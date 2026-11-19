package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ArchStylePreset
import com.example.data.model.CameraLensPreset
import com.example.data.model.ChatMessageRecord
import com.example.data.model.DesignProject
import com.example.data.model.DraftMaterialInput
import com.example.data.model.LightingPreset
import com.example.data.model.MaterialCategory
import com.example.data.model.MaterialPalettePreset
import com.example.data.model.RenderAspectRatio
import com.example.data.model.RenderResolution
import com.example.data.model.SpaceType
import com.example.data.model.VirtualTourAspectRatio
import com.example.data.model.VirtualTourCameraMove
import com.example.data.model.VirtualTourFrame
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "GeminiStudioRepo"

class GeminiStudioRepository {

    private val architectSystemInstruction by lazy {
        content {
            text(
                """
                Sen MimarStüdyo uygulamasının Baş Mimarı ve Kıdemli İç Mimarısın.
                Kullanıcılarla her zaman akıcı, uzman ve ilham verici bir Türkçe ile konuş.
                İç mekan, dış cephe, peyzaj, ışıklandırma, ergonomi, metrekare yerleşimi, yapı malzemeleri ve yaklaşık bütçe optimizasyonu konularında somut mimari öneriler ver.
                Kullanıcı bir ev, oda, cephe, mutfak, banyo veya bahçe tasarımı istediğinde:
                1. Önce mimari konsepti, mekan kurgusunu, malzeme paletini ve aydınlatma senaryosunu net maddeler halinde açıkla.
                2. Yanıtının en son satırında, kullanıcının tek dokunuşla fotogerçekçi render alabilmesi için şu formatta net bir tasarım özeti ekle:
                [RENDER_PROMPT: <İç/Dış mekan, stil, malzemeler, ışık ve atmosferi özetleyen tek paragraf Türkçe render komutu>]
                """.trimIndent()
            )
        }
    }

    private val standardChatModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = architectSystemInstruction
        )
    }

    private val highThinkingChatModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            systemInstruction = architectSystemInstruction,
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    private val bomModel: GenerativeModel by lazy {
        val itemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Malzeme veya ürün adı (Türkçe)"),
                "category" to Schema.enumeration(
                    values = listOf("YAPI", "ZEMIN", "CEPHE", "AYDINLATMA", "MOBILYA", "PEYZAJ"),
                    description = "Malzeme kategorisi"
                ),
                "specification" to Schema.string(description = "Teknik özellik, ölçü ve yüzey detayı"),
                "quantity" to Schema.double(description = "Gerekli tahmini miktar (>0)"),
                "unit" to Schema.string(description = "Ölçü birimi (m², adet, m³, mt, kg)"),
                "unitPriceTry" to Schema.double(description = "Güncel tahmini birim fiyat (Türk Lirası)")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "items" to Schema.array(
                    items = itemSchema,
                    description = "Projeye özel 8 ila 12 kalemlik detaylı malzeme listesi"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private val tourScriptModel: GenerativeModel by lazy {
        val shotSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Kısa sahne başlığı (Türkçe, örn: Giriş Cephesi)"),
                "narrationTr" to Schema.string(description = "Mimar rehber anlatımı (1-2 cümle Türkçe)"),
                "visualDirectiveEn" to Schema.string(description = "Detailed English camera angle and lighting directive to synthesize this tour frame from the reference photo while keeping the exact same house architecture")
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "shots" to Schema.array(
                    items = shotSchema,
                    description = "Exactly 3 sequential architectural virtual tour camera shots"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private fun toSdkAspectRatio(ratio: RenderAspectRatio): AspectRatio = when (ratio) {
        RenderAspectRatio.RATIO_1_1 -> AspectRatio.SQUARE_1x1
        RenderAspectRatio.RATIO_2_3 -> AspectRatio.PORTRAIT_2x3
        RenderAspectRatio.RATIO_3_2 -> AspectRatio.LANDSCAPE_3x2
        RenderAspectRatio.RATIO_3_4 -> AspectRatio.PORTRAIT_3x4
        RenderAspectRatio.RATIO_4_3 -> AspectRatio.LANDSCAPE_4x3
        RenderAspectRatio.RATIO_9_16 -> AspectRatio.PORTRAIT_9x16
        RenderAspectRatio.RATIO_16_9 -> AspectRatio.LANDSCAPE_16x9
        RenderAspectRatio.RATIO_21_9 -> AspectRatio.LANDSCAPE_21x9
    }

    private fun toSdkImageSize(resolution: RenderResolution): ImageSize = when (resolution) {
        RenderResolution.SIZE_1K -> ImageSize.SIZE_1K
        RenderResolution.SIZE_2K -> ImageSize.SIZE_2K
        RenderResolution.SIZE_4K -> ImageSize.SIZE_4K
    }

    private fun createImageModel(
        resolution: RenderResolution,
        aspectRatio: RenderAspectRatio
    ): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(toSdkAspectRatio(aspectRatio))
                .setImageSize(toSdkImageSize(resolution))
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = resolution.modelName,
            generationConfig = config
        )
    }

    suspend fun generateArchitecturalRender(
        userPrompt: String,
        spaceType: SpaceType,
        style: ArchStylePreset,
        lighting: LightingPreset,
        cameraLens: CameraLensPreset,
        materialPalette: MaterialPalettePreset,
        aspectRatio: RenderAspectRatio,
        resolution: RenderResolution
    ): Bitmap {
        val fullPrompt = buildString {
            append("Photorealistic award-winning architectural visualization render. ")
            append("Space type: ${spaceType.promptHint}. ")
            append("Architectural style: ${style.promptDesc}. ")
            append("Primary material palette: ${materialPalette.promptDesc}. ")
            append("Lighting & atmosphere: ${lighting.promptDesc}. ")
            append("Camera & composition: ${cameraLens.promptDesc}. ")
            append("Client brief: ${userPrompt.trim()}. ")
            append("Ultra-crisp architectural photography, physically accurate reflections, global illumination, realistic textures, magazine editorial quality, no text or watermarks.")
        }

        val model = createImageModel(resolution, aspectRatio)
        val timeoutMs = if (resolution == RenderResolution.SIZE_4K) 90_000L else 60_000L

        return withTimeout(timeoutMs) {
            val response = model.generateContent(fullPrompt)
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
                ?: throw IllegalStateException("Render motoru görsel döndürmedi. Lütfen açıklamanızı düzenleyip tekrar deneyin.")
        }
    }

    suspend fun editArchitecturalPhoto(
        sourceBitmap: Bitmap,
        editPrompt: String,
        spaceType: SpaceType,
        style: ArchStylePreset,
        lighting: LightingPreset,
        materialPalette: MaterialPalettePreset,
        aspectRatio: RenderAspectRatio,
        resolution: RenderResolution
    ): Bitmap {
        val instruction = buildString {
            append("Redesign and professionally render this uploaded ${spaceType.promptHint} photograph while preserving the core room/building geometry and perspective. ")
            append("Apply style: ${style.promptDesc}. ")
            append("Materials: ${materialPalette.promptDesc}. ")
            append("Lighting: ${lighting.promptDesc}. ")
            append("Specific redesign instructions from user: ${editPrompt.trim()}. ")
            append("Deliver a photorealistic architectural photography result with realistic shadows, materials, and clean lines. No text or watermarks.")
        }

        val model = createImageModel(resolution, aspectRatio)
        val timeoutMs = if (resolution == RenderResolution.SIZE_4K) 90_000L else 60_000L

        return withTimeout(timeoutMs) {
            val response = model.generateContent(
                content {
                    image(sourceBitmap)
                    text(instruction)
                }
            )
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
                ?: throw IllegalStateException("Fotoğraf yeniden tasarlanamadı. Lütfen talimatınızı netleştirip tekrar deneyin.")
        }
    }

    fun streamArchitectChat(
        history: List<ChatMessageRecord>,
        userMessage: String,
        attachedPhoto: Bitmap?,
        highThinking: Boolean
    ): Flow<String> = flow {
        val model = if (highThinking) highThinkingChatModel else standardChatModel
        val sdkHistory = history.takeLast(14).map { msg ->
            content(role = if (msg.role == "USER") "user" else "model") {
                text(msg.text)
            }
        }
        val chat = model.startChat(history = sdkHistory)
        val outgoing = content("user") {
            if (attachedPhoto != null) {
                image(attachedPhoto)
            }
            text(userMessage)
        }
        val timeoutMs = if (highThinking) 90_000L else 60_000L
        withTimeout(timeoutMs) {
            chat.sendMessageStream(outgoing).collect { chunk ->
                val textChunk = chunk.text
                if (!textChunk.isNullOrEmpty()) {
                    emit(textChunk)
                }
            }
        }
    }

    suspend fun generateMaterialList(
        project: DesignProject,
        referenceBitmap: Bitmap?
    ): List<DraftMaterialInput> {
        val prompt = buildString {
            append("Aşağıdaki ev tasarımı projesi için Türkiye inşaat/mimari piyasasına uygun gerçekçi bir Malzeme Listesi (Metraj & Keşif) oluştur:\n")
            append("Proje: ${project.title}\n")
            append("Alan: ${project.areaSqm} m²\n")
            append("Mekan: ${SpaceType.fromRaw(project.spaceType).labelTr} (${project.roomCategory})\n")
            append("Mimari Stil: ${project.style}\n")
            append("Tasarım Özeti: ${project.promptSummary.ifBlank { "Modern lüks konut tasarımı" }}\n")
            append("En az 8, en fazla 12 farklı kalem üret (YAPI, ZEMIN, CEPHE, AYDINLATMA, MOBILYA, PEYZAJ kategorilerini dengeli dağıt). Tüm birim fiyatları gerçekçi Türk Lirası (TRY) cinsinden yaz.")
        }

        val response = withTimeout(60_000L) {
            if (referenceBitmap != null) {
                bomModel.generateContent(
                    content {
                        image(referenceBitmap)
                        text(prompt)
                    }
                )
            } else {
                bomModel.generateContent(prompt)
            }
        }

        val jsonText = response.text
            ?: throw IllegalStateException("Malzeme listesi yanıtı boş döndü.")
        val root = JSONObject(jsonText)
        val itemsArr = root.getJSONArray("items")
        val results = mutableListOf<DraftMaterialInput>()
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.getJSONObject(i)
            val name = obj.optString("name").trim()
            if (name.isEmpty()) continue
            results.add(
                DraftMaterialInput(
                    name = name,
                    category = MaterialCategory.fromRaw(obj.optString("category")),
                    specification = obj.optString("specification").trim(),
                    quantity = obj.optDouble("quantity", 1.0).coerceAtLeast(0.1),
                    unit = obj.optString("unit", "m²").ifBlank { "adet" },
                    unitPriceTry = obj.optDouble("unitPriceTry", 0.0).coerceAtLeast(0.0)
                )
            )
        }
        if (results.isEmpty()) {
            throw IllegalStateException("Geçerli malzeme kalemi üretilemedi.")
        }
        return results
    }

    /**
     * Generates a coordinated multi-angle AI Virtual Tour sequence from a user photo or render,
     * adhering strictly to 16:9 or 9:16 aspect ratio with Turkish architectural tour narration.
     */
    suspend fun generateVirtualTourSequence(
        sourceBitmap: Bitmap,
        aspectRatio: VirtualTourAspectRatio,
        cameraMove: VirtualTourCameraMove,
        spaceHint: String,
        onProgress: (String) -> Unit
    ): List<VirtualTourFrame> {
        onProgress("Mimari kamera senaryosu hazırlanıyor...")
        val scriptResponse = withTimeout(60_000L) {
            tourScriptModel.generateContent(
                content {
                    image(sourceBitmap)
                    text(
                        """
                        Bu mimari fotoğraf/render için 3 aşamalı sinematik sanal tur senaryosu oluştur.
                        Kamera hareketi stili: ${cameraMove.labelTr} (${cameraMove.instructionEn}).
                        Mekan bilgisi: $spaceHint.
                        Her sahne aynı evin/mekanın mimarisini koruyarak farklı bir kamera açısından devamlılık sağlamalı.
                        """.trimIndent()
                    )
                }
            )
        }

        val jsonText = scriptResponse.text
            ?: throw IllegalStateException("Sanal tur senaryosu oluşturulamadı.")
        val shotsArray = JSONObject(jsonText).getJSONArray("shots")
        val frames = mutableListOf<VirtualTourFrame>()
        val imageModel = createImageModel(RenderResolution.SIZE_1K, aspectRatio.apiAspect)

        // Frame 0 is the anchor establishing view from the source photo
        val firstShot = if (shotsArray.length() > 0) shotsArray.getJSONObject(0) else null
        frames.add(
            VirtualTourFrame(
                title = firstShot?.optString("title")?.ifBlank { "Ana Perspektif" } ?: "Ana Perspektif",
                narrationTr = firstShot?.optString("narrationTr")?.ifBlank {
                    "Mimari sanal tur başlıyor: ana cephe ve mekan oranlarını inceliyoruz."
                } ?: "Mimari sanal tur başlıyor.",
                bitmap = sourceBitmap
            )
        )

        // Synthesize coordinated AI tour keyframes for shots 1..2 (total 3 smooth cinematic shots)
        val totalToGen = minOf(shotsArray.length(), 3)
        for (i in 1 until totalToGen) {
            val shot = shotsArray.getJSONObject(i)
            val title = shot.optString("title", "Sahne ${i + 1}")
            val narration = shot.optString("narrationTr", "")
            val directive = shot.optString("visualDirectiveEn", cameraMove.instructionEn)

            onProgress("Sanal tur karesi ${i + 1}/$totalToGen render ediliyor ($title)...")
            try {
                val frameBitmap = withTimeout(60_000L) {
                    val res = imageModel.generateContent(
                        content {
                            image(sourceBitmap)
                            text(
                                "Create the next cinematic architectural virtual tour camera keyframe of this EXACT SAME building/room. " +
                                    "Maintain identical architecture, materials, colors, and proportions. " +
                                    "Camera movement: ${cameraMove.instructionEn}. " +
                                    "Shot directive: $directive. Photorealistic architectural cinematography, no text or watermarks."
                            )
                        }
                    )
                    res.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (frameBitmap != null) {
                    frames.add(
                        VirtualTourFrame(
                            title = title,
                            narrationTr = narration,
                            bitmap = frameBitmap
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AI_TAG, "Virtual tour frame $i generation failed", e)
                if (frames.size <= 1) {
                    throw e
                }
            }
        }
        return frames
    }
}
