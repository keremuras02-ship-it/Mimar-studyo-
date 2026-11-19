package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessageRecord
import com.example.data.model.DesignProject
import com.example.data.model.DraftMaterialInput
import com.example.data.model.MaterialItem
import com.example.data.model.Model3DPreset
import com.example.data.model.ProjectPhase
import com.example.data.model.SpaceType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudProjectRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Bulut verilerine erişmek için Google ile giriş yapmalısınız.")
    }

    fun observeProjects(userId: String): Flow<List<DesignProject>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Oturum kullanıcı kimliği eşleşmiyor." }
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DesignProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMaterials(userId: String, projectId: String): Flow<List<MaterialItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Oturum kullanıcı kimliği eşleşmiyor." }
        require(projectId.isNotBlank()) { "Proje ID boş olamaz." }
        val path = "users/$uid/projects/$projectId/materials"
        emitAll(
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("materials")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MaterialItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChatMessages(userId: String): Flow<List<ChatMessageRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Oturum kullanıcı kimliği eşleşmiyor." }
        val path = "users/$uid/chat_messages"
        emitAll(
            db.collection("users").document(uid)
                .collection("chat_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessageRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProject(
        title: String,
        spaceType: SpaceType,
        style: String,
        roomCategory: String,
        areaSqm: Double,
        promptSummary: String,
        renderSettings: String,
        modelType: Model3DPreset,
        thumbnailBase64: String? = null,
        notes: String? = null
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Proje adı boş olamaz." }
            val cleanStyle = style.trim().ifEmpty { "Modern Akdeniz" }.take(60)
            val cleanRoom = roomCategory.trim().ifEmpty { "Genel Mekan" }.take(60)
            val validArea = areaSqm.coerceIn(1.0, 50000.0)
            val cleanPrompt = promptSummary.trim().take(2000)
            val cleanSettings = renderSettings.trim().take(300)
            val cleanThumb = thumbnailBase64?.takeIf { it.isNotBlank() && it.length <= 350000 }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(4000)

            val docRef = db.collection("users").document(uid).collection("projects").document()
            docPath = docRef.path

            val payload = linkedMapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "spaceType" to spaceType.name,
                "style" to cleanStyle,
                "roomCategory" to cleanRoom,
                "areaSqm" to validArea,
                "promptSummary" to cleanPrompt,
                "renderSettings" to cleanSettings,
                "architecturalModelType" to modelType.name,
                "thumbnailBase64" to cleanThumb,
                "notes" to cleanNotes,
                "estimatedTotalTry" to 0.0,
                "phase" to ProjectPhase.KONSEPT.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProjectRender(
        projectId: String,
        promptSummary: String,
        renderSettings: String,
        thumbnailBase64: String?
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje ID geçersiz." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path

            val updates = linkedMapOf<String, Any?>(
                "promptSummary" to promptSummary.trim().take(2000),
                "renderSettings" to renderSettings.trim().take(300),
                "thumbnailBase64" to thumbnailBase64?.takeIf { it.isNotBlank() && it.length <= 350000 },
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProjectPhase(projectId: String, phase: ProjectPhase): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje ID geçersiz." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "phase" to phase.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncProjectBudgetTotal(projectId: String, totalTry: Double): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje ID geçersiz." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "estimatedTotalTry" to totalTry.coerceIn(0.0, 1_000_000_000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje ID geçersiz." }
            val projectRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = projectRef.path

            // Delete subcollection materials first
            val mats = projectRef.collection("materials").get().await()
            for (doc in mats.documents) {
                doc.reference.delete().await()
            }
            projectRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMaterial(projectId: String, item: DraftMaterialInput): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/materials"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje seçilmedi." }
            val cleanName = item.name.trim().take(120)
            require(cleanName.isNotEmpty()) { "Malzeme adı boş olamaz." }
            val cleanUnit = item.unit.trim().ifEmpty { "adet" }.take(20)
            val docRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("materials").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "projectId" to projectId,
                "name" to cleanName,
                "category" to item.category.name,
                "specification" to item.specification.trim().take(300),
                "quantity" to item.quantity.coerceIn(0.01, 1_000_000.0),
                "unit" to cleanUnit,
                "unitPriceTry" to item.unitPriceTry.coerceIn(0.0, 100_000_000.0),
                "purchased" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMaterialsBatch(projectId: String, items: List<DraftMaterialInput>): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/materials"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank()) { "Proje seçilmedi." }
            val batch = db.batch()
            val collection = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("materials")
            for (item in items) {
                val cleanName = item.name.trim().take(120)
                if (cleanName.isEmpty()) continue
                val docRef = collection.document()
                val payload = mapOf(
                    "userId" to uid,
                    "projectId" to projectId,
                    "name" to cleanName,
                    "category" to item.category.name,
                    "specification" to item.specification.trim().take(300),
                    "quantity" to item.quantity.coerceIn(0.01, 1_000_000.0),
                    "unit" to item.unit.trim().ifEmpty { "adet" }.take(20),
                    "unitPriceTry" to item.unitPriceTry.coerceIn(0.0, 100_000_000.0),
                    "purchased" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMaterialPurchased(
        projectId: String,
        materialId: String,
        purchased: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/materials/$materialId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank() && materialId.isNotBlank()) { "Geçersiz malzeme referansı." }
            val docRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("materials").document(materialId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "purchased" to purchased,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMaterial(projectId: String, materialId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId/materials/$materialId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank() && materialId.isNotBlank()) { "Geçersiz malzeme referansı." }
            val docRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("materials").document(materialId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveChatMessage(
        role: String,
        text: String,
        thinkingUsed: Boolean,
        attachedPrompt: String? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chat_messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(12000)
            require(cleanText.isNotEmpty()) { "Mesaj boş olamaz." }
            val docRef = db.collection("users").document(uid).collection("chat_messages").document()
            docPath = docRef.path

            val payload = linkedMapOf<String, Any?>(
                "userId" to uid,
                "role" to if (role == "USER") "USER" else "MODEL",
                "text" to cleanText,
                "thinkingUsed" to thinkingUsed,
                "attachedPrompt" to attachedPrompt?.trim()?.takeIf { it.isNotEmpty() }?.take(1500),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(): Result<Unit> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/chat_messages"
        return try {
            val uid = requireUserId()
            val snap = db.collection("users").document(uid).collection("chat_messages").get().await()
            val batch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
