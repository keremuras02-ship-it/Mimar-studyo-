package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.ChatMessageRecord
import com.example.data.repository.CloudProjectRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val CHAT_VM_TAG = "ArchChatViewModel"

sealed interface ConversationalUiState {
    data object Idle : ConversationalUiState
    data object Loading : ConversationalUiState
    data class Streaming(val partialText: String) : ConversationalUiState
    data class GeneratingDesign(val statusText: String) : ConversationalUiState
    data class Success(val lastMessageId: String) : ConversationalUiState
    data class Error(val message: String, val failedPrompt: String? = null) : ConversationalUiState
}

data class ArchitecturalChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val thinkingUsed: Boolean = false,
    val renderPromptSuggestion: String? = null,
    val generatedDesignBitmap: Bitmap? = null,
    val attachedUserPhoto: Bitmap? = null
)

class ArchitecturalChatViewModel(
    private val cloudRepo: CloudProjectRepository,
    private val currentUserId: String
) : ViewModel() {

    private val architectSystemInstruction by lazy {
        content {
            text(
                """
                Sen MimarStüdyo uygulamasının Baş Mimarı ve Kıdemli İç Mimarısın.
                Kullanıcılarla akıcı, uzman ve yaratıcı bir Türkçe ile iletişim kur.
                İç mekan, dış cephe, kat planı, ışıklandırma, ergonomi, peyzaj ve yapı malzemeleri hakkında somut mimari tasarımlar geliştir.
                Kullanıcı bir mekan veya ev tasarımı istediğinde:
                1. Konsepti, mekan kurgusunu, malzeme paletini ve ışık senaryosunu maddeler halinde netçe öner.
                2. Yanıtının en son satırında tek dokunuşla fotogerçekçi mimari görsel üretilebilmesi için şu etiketi ekle:
                [RENDER_PROMPT: <Mekan tipi, mimari stil, malzemeler, ışık ve kamera açısını özetleyen tek paragraf detaylı tasarım komutu>]
                """.trimIndent()
            )
        }
    }

    // Lazy Firebase AI Logic models — never evaluated eagerly at construction time
    private val standardChatModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = architectSystemInstruction
        )
    }

    private val deepThinkingChatModel: GenerativeModel by lazy {
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

    private val inlineDesignImageModel: GenerativeModel by lazy {
        val imageGenConfig = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = imageGenConfig
        )
    }

    private val _uiState = MutableStateFlow<ConversationalUiState>(ConversationalUiState.Idle)
    val uiState: StateFlow<ConversationalUiState> = _uiState.asStateFlow()

    private val _highThinkingEnabled = MutableStateFlow(false)
    val highThinkingEnabled: StateFlow<Boolean> = _highThinkingEnabled.asStateFlow()

    private val _directDesignMode = MutableStateFlow(false)
    val directDesignMode: StateFlow<Boolean> = _directDesignMode.asStateFlow()

    private val _attachedPhoto = MutableStateFlow<Bitmap?>(null)
    val attachedPhoto: StateFlow<Bitmap?> = _attachedPhoto.asStateFlow()

    // In-memory map linking session-generated architectural renders & user thumbnails to message IDs
    private val _localDesignBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    private val _localFallbackMessages = MutableStateFlow<List<ArchitecturalChatMessage>>(emptyList())

    private val cloudMessagesFlow = cloudRepo.observeChatMessages(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(CHAT_VM_TAG, "Error observing cloud chat messages", rootError)
            _uiState.value = ConversationalUiState.Error(
                rootError.message ?: "Sohbet geçmişi senkronize edilemedi."
            )
            emit(emptyList())
        }

    val messages: StateFlow<List<ArchitecturalChatMessage>> = combine(
        cloudMessagesFlow,
        _localDesignBitmaps,
        _localFallbackMessages
    ) { cloudRecords, designBitmaps, localExtras ->
        val mappedCloud = cloudRecords.map { rec ->
            ArchitecturalChatMessage(
                id = rec.id,
                role = rec.role,
                text = rec.text,
                thinkingUsed = rec.thinkingUsed,
                renderPromptSuggestion = rec.attachedPrompt,
                generatedDesignBitmap = designBitmaps[rec.attachedPrompt ?: rec.id]
            )
        }
        mappedCloud + localExtras
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun toggleHighThinking(enabled: Boolean) {
        _highThinkingEnabled.value = enabled
    }

    fun toggleDirectDesignMode(enabled: Boolean) {
        _directDesignMode.value = enabled
    }

    fun setAttachedPhoto(bitmap: Bitmap?) {
        _attachedPhoto.value = bitmap
    }

    fun dismissError() {
        if (_uiState.value is ConversationalUiState.Error) {
            _uiState.value = ConversationalUiState.Idle
        }
    }

    fun submitPrompt(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return
        if (_uiState.value is ConversationalUiState.Loading ||
            _uiState.value is ConversationalUiState.Streaming ||
            _uiState.value is ConversationalUiState.GeneratingDesign
        ) {
            return
        }

        if (_directDesignMode.value) {
            generateInlineArchitecturalDesign(prompt, _attachedPhoto.value)
            _attachedPhoto.value = null
        } else {
            sendConversationalPrompt(prompt)
        }
    }

    private fun sendConversationalPrompt(prompt: String) {
        val highThinking = _highThinkingEnabled.value
        val photo = _attachedPhoto.value
        val currentHistory = messages.value

        viewModelScope.launch {
            _attachedPhoto.value = null
            _uiState.value = ConversationalUiState.Loading

            cloudRepo.saveChatMessage(
                role = "USER",
                text = if (photo != null) "[Fotoğraf Eklendi] $prompt" else prompt,
                thinkingUsed = highThinking
            )

            val accumulated = StringBuilder()
            try {
                val activeModel = if (highThinking) deepThinkingChatModel else standardChatModel
                val sdkHistory = currentHistory
                    .filter { it.text.isNotBlank() }
                    .takeLast(14)
                    .map { msg ->
                        content(role = if (msg.role == "USER") "user" else "model") {
                            text(msg.text)
                        }
                    }

                val chat = activeModel.startChat(history = sdkHistory)
                val outgoing = content("user") {
                    if (photo != null) {
                        image(photo)
                    }
                    text(prompt)
                }

                val timeoutMs = if (highThinking) 90_000L else 60_000L
                withTimeout(timeoutMs) {
                    chat.sendMessageStream(outgoing).collect { chunk ->
                        val textChunk = chunk.text.orEmpty()
                        if (textChunk.isNotEmpty()) {
                            accumulated.append(textChunk)
                            _uiState.value = ConversationalUiState.Streaming(accumulated.toString())
                        }
                    }
                }

                val fullReply = accumulated.toString().trim()
                if (fullReply.isNotEmpty()) {
                    val extractedRenderPrompt = extractRenderPromptTag(fullReply)
                    val cleanDisplayText = stripRenderPromptTag(fullReply).ifBlank { fullReply }
                    cloudRepo.saveChatMessage(
                        role = "MODEL",
                        text = cleanDisplayText,
                        thinkingUsed = highThinking,
                        attachedPrompt = extractedRenderPrompt
                    )
                    _uiState.value = ConversationalUiState.Success("reply_${System.currentTimeMillis()}")
                } else {
                    _uiState.value = ConversationalUiState.Error(
                        message = "Model boş bir yanıt döndürdü. Lütfen mimari isteğinizi biraz daha detaylandırın.",
                        failedPrompt = prompt
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(CHAT_VM_TAG, "Architectural chat timed out", e)
                _uiState.value = ConversationalUiState.Error(
                    message = "Yanıt zaman aşımına uğradı. Lütfen tekrar deneyin.",
                    failedPrompt = prompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(CHAT_VM_TAG, "Firebase AI chat generation failed", e)
                _uiState.value = ConversationalUiState.Error(
                    message = e.localizedMessage ?: "AI Mimar yanıt üretirken bir hata oluştu.",
                    failedPrompt = prompt
                )
            }
        }
    }

    /**
     * Generates a photorealistic architectural design image directly inside the conversational thread
     * via Firebase AI Logic (gemini-nano-banana-2.1).
     */
    fun generateInlineArchitecturalDesign(
        designPrompt: String,
        referenceBitmap: Bitmap? = null
    ) {
        val cleanPrompt = designPrompt.trim()
        if (cleanPrompt.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = ConversationalUiState.GeneratingDesign(
                "Mimari tasarım görseli üretiliyor (16:9 • 1K)..."
            )

            // Record the request in cloud history if triggered directly from composer
            if (_directDesignMode.value) {
                cloudRepo.saveChatMessage(
                    role = "USER",
                    text = "[Tasarım Üret] $cleanPrompt",
                    thinkingUsed = false
                )
            }

            try {
                val architecturalDirective = buildString {
                    append("Photorealistic architectural design visualization render. ")
                    if (referenceBitmap != null) {
                        append("Redesign the attached architectural photograph while preserving core spatial geometry. ")
                    }
                    append("Design brief: $cleanPrompt. ")
                    append("Physically accurate materials, architectural daylighting, editorial photography quality, no text or watermarks.")
                }

                val generatedBitmap = withTimeout(60_000L) {
                    val response = if (referenceBitmap != null) {
                        inlineDesignImageModel.generateContent(
                            content {
                                image(referenceBitmap)
                                text(architecturalDirective)
                            }
                        )
                    } else {
                        inlineDesignImageModel.generateContent(architecturalDirective)
                    }
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (generatedBitmap != null) {
                    val renderKey = "inline_render_${System.currentTimeMillis()}"
                    _localDesignBitmaps.value = _localDesignBitmaps.value + (cleanPrompt to generatedBitmap) + (renderKey to generatedBitmap)

                    val saveRes = cloudRepo.saveChatMessage(
                        role = "MODEL",
                        text = "Mimari tasarımınız oluşturuldu: $cleanPrompt",
                        thinkingUsed = false,
                        attachedPrompt = cleanPrompt
                    )
                    if (saveRes.isFailure) {
                        // Still show generated render locally in thread if cloud log write failed
                        _localFallbackMessages.value = _localFallbackMessages.value + ArchitecturalChatMessage(
                            id = renderKey,
                            role = "MODEL",
                            text = "Mimari tasarımınız oluşturuldu: $cleanPrompt",
                            renderPromptSuggestion = cleanPrompt,
                            generatedDesignBitmap = generatedBitmap
                        )
                    }
                    _uiState.value = ConversationalUiState.Success(renderKey)
                } else {
                    _uiState.value = ConversationalUiState.Error(
                        message = "Model görsel üretemedi. Lütfen mimari tarifi netleştirip tekrar deneyin.",
                        failedPrompt = cleanPrompt
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(CHAT_VM_TAG, "Inline design generation timed out", e)
                _uiState.value = ConversationalUiState.Error(
                    message = "Mimari görsel üretimi 60 saniyede zaman aşımına uğradı. Lütfen tekrar deneyin.",
                    failedPrompt = cleanPrompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(CHAT_VM_TAG, "Inline architectural design generation failed", e)
                _uiState.value = ConversationalUiState.Error(
                    message = e.localizedMessage ?: "Mimari tasarım görseli oluşturulamadı.",
                    failedPrompt = cleanPrompt
                )
            }
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            _localDesignBitmaps.value = emptyMap()
            _localFallbackMessages.value = emptyList()
            _uiState.value = ConversationalUiState.Idle
            val res = cloudRepo.clearChatHistory()
            if (res.isFailure) {
                _uiState.value = ConversationalUiState.Error(
                    res.exceptionOrNull()?.localizedMessage ?: "Sohbet geçmişi temizlenemedi."
                )
            }
        }
    }

    private fun extractRenderPromptTag(text: String): String? {
        val regex = Regex("\\[RENDER_PROMPT:\\s*(.+?)\\]", setOf(RegexOption.DOT_MATCHES_ALL))
        return regex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun stripRenderPromptTag(text: String): String {
        val regex = Regex("\\[RENDER_PROMPT:\\s*.+?\\]", setOf(RegexOption.DOT_MATCHES_ALL))
        return text.replace(regex, "").trim()
    }

    companion object {
        fun provideFactory(
            cloudRepo: CloudProjectRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ArchitecturalChatViewModel(cloudRepo, currentUserId)
            }
        }
    }
}
