const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_architect_1";
const BOB_UID = "bob_architect_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    title: "Ege Taş Villa",
    spaceType: "BOTH",
    style: "Modern Akdeniz",
    roomCategory: "Dış Cephe & Salon",
    areaSqm: 280.0,
    promptSummary: "Havuzlu traverten taş cepheli modern villa",
    renderSettings: "16:9 • 2K • Altın Saat",
    architecturalModelType: "VILLA",
    estimatedTotalTry: 1250000.0,
    phase: "KONSEPT",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validMaterialPayload(uid, projectId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    projectId,
    name: "Honlu Denizli Traverten Plaka",
    category: "ZEMIN",
    specification: "60x120cm, 2cm kalınlık, mat yüzey",
    quantity: 140.0,
    unit: "m²",
    unitPriceTry: 1450.0,
    purchased: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write projects, materials, or chat", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(validProjectPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/chat_messages`).get());
});

test("Authenticated owner: valid project create, list, partial update, budget sync, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projectRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1");
  const created = new Date(Date.now() - 5000);

  // Create without @DocumentId 'id' key
  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID, { createdAt: created, updatedAt: created })));

  // List projects under owner subcollection
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());

  // Partial update: details + thumbnailBase64
  await assertSucceeds(
    projectRef.update({
      title: "Ege Taş Villa Rev2",
      thumbnailBase64: "abc123base64",
      phase: "UYGULAMA",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Partial update: budget total sync
  await assertSucceeds(
    projectRef.update({
      estimatedTotalTry: 1480000.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    projectRef.update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject shadow ghost field on create
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_ghost").set(
      validProjectPayload(ALICE_UID, { isVerified: true })
    )
  );

  // Reject @DocumentId 'id' field in create payload
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_with_id").set(
      validProjectPayload(ALICE_UID, { id: "proj_with_id" })
    )
  );

  // Transition to terminal phase TESLIM, then verify subsequent updates are locked
  await assertSucceeds(
    projectRef.update({
      phase: "TESLIM",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    projectRef.update({
      title: "Cannot edit finalized",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_a").set(validProjectPayload(ALICE_UID))
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).doc("proj_a").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/projects`).doc("proj_a").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/projects`).doc("proj_a").delete());
});

test("Materials subcollection: requires existing parent project and enforces schema + partial updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Orphan material fails when parent project does not exist
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/projects/missing_proj/materials`)
      .doc("mat_1")
      .set(validMaterialPayload(ALICE_UID, "missing_proj"))
  );

  // Create parent project first
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(validProjectPayload(ALICE_UID))
  );

  const matRef = aliceDb.collection(`users/${ALICE_UID}/projects/proj_1/materials`).doc("mat_1");
  await assertSucceeds(matRef.set(validMaterialPayload(ALICE_UID, "proj_1")));

  // List materials
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects/proj_1/materials`).get());

  // Toggle purchased status
  await assertSucceeds(
    matRef.update({
      purchased: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    matRef.update({
      purchased: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Delete material succeeds for owner
  await assertSucceeds(matRef.delete());
});

test("Chat messages: create, list, delete succeed for owner; update is forbidden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const msgRef = aliceDb.collection(`users/${ALICE_UID}/chat_messages`).doc("msg_1");

  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "MODEL",
      text: "Salonunuz için doğal traverten ve sıcak ceviz öneriyorum.",
      thinkingUsed: true,
      attachedPrompt: "Modern traverten ve ceviz salon iç mekan render",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/chat_messages`).get());

  // Chat messages are append-only (no updates)
  await assertFails(msgRef.update({ text: "Tampered" }));
  await assertSucceeds(msgRef.delete());
});
