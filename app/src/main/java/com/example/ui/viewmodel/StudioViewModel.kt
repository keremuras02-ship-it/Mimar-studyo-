package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.BitmapUtils
import com.example.data.model.ArchStylePreset
import com.example.data.model.CameraLensPreset
import com.example.data.model.ChatMessageRecord
import com.example.data.model.DesignProject
import com.example.data.model.DraftMaterialInput
import com.example.data.model.LightingPreset
import com.example.data.model.MaterialItem
import com.example.data.model.MaterialPalettePreset
import com.example.data.model.Model3DPreset
import com.example.data.model.ProjectPhase
import com.example.data.model.RenderAspectRatio
import com.example.data.model.RenderResolution
import com.example.data.model.ShowcaseTemplate
import com.example.data.model.SpaceType
import com.example.data.model.VirtualTourAspectRatio
import com.example.data.model.VirtualTourCameraMove
import com.example.data.model.VirtualTourFrame
import com.example.data.repository.CloudProjectRepository
import com.example.data.repository.GeminiStudioRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val VM_TAG = "StudioViewModel"

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

sealed interface RenderUiState {
    data object Idle : RenderUiState
    data class Loading(val statusText: String) : RenderUiState
    data class Success(val bitmap: Bitmap, val summary: String, val settingsLabel: String) : RenderUiState
    data class Error(val message: String) : RenderUiState
}

sealed interface VirtualTourUiState {
    data object Idle : VirtualTourUiState
    data class Generating(val progressText: String) : VirtualTourUiState
    data class Ready(
        val frames: List<VirtualTourFrame>,
        val aspectRatio: VirtualTourAspectRatio,
        val cameraMove: VirtualTourCameraMove
    ) : VirtualTourUiState
    data class Error(val message: String) : VirtualTourUiState
}

data class RenderConfigState(
    val isPhotoEditMode: Boolean = false,
    val prompt: String = "",
    val spaceType: SpaceType = SpaceType.EXTERIOR,
    val style: ArchStylePreset = ArchStylePreset.MEDITERRANEAN,
    val lighting: LightingPreset = LightingPreset.GOLDEN_HOUR,
    val cameraLens: CameraLensPreset = CameraLensPreset.WIDE_24,
    val materialPalette: MaterialPalettePreset = MaterialPalettePreset.TRAVERTINE_WALNUT,
    val aspectRatio: RenderAspectRatio = RenderAspectRatio.RATIO_16_9,
    val resolution: RenderResolution = RenderResolution.SIZE_1K,
    val uploadedSourceBitmap: Bitmap? = null
)

data class ChatUiState(
    val isStreaming: Boolean = false,
    val streamingBuffer: String = "",
    val highThinkingEnabled: Boolean = false,
    val attachedPhoto: Bitmap? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
    private val cloudRepo: CloudProjectRepository,
    private val aiRepo: GeminiStudioRepository,
    val currentUserId: String
) : ViewModel() {

    // --- CLOUD PROJECTS ---
    val projectsState: StateFlow<CloudUiState<List<DesignProject>>> =
        cloudRepo.observeProjects(currentUserId)
            .map<List<DesignProject>, CloudUiState<List<DesignProject>>> { list ->
                if (_selectedProjectId.value == null && list.isNotEmpty()) {
                    _selectedProjectId.value = list.first().id
                }
                CloudUiState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(VM_TAG, "Error observing projects", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Bulut projeleri yüklenemedi."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    val materialsState: StateFlow<CloudUiState<List<MaterialItem>>> =
        _selectedProjectId.flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(CloudUiState.Success(emptyList()))
            } else {
                cloudRepo.observeMaterials(currentUserId, projectId)
                    .map<List<MaterialItem>, CloudUiState<List<MaterialItem>>> { CloudUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(VM_TAG, "Error observing materials", rootError)
                        emit(CloudUiState.Error(rootError.message ?: "Malzeme listesi yüklenemedi."))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudUiState.Loading
        )

    val chatHistoryState: StateFlow<CloudUiState<List<ChatMessageRecord>>> =
        cloudRepo.observeChatMessages(currentUserId)
            .map<List<ChatMessageRecord>, CloudUiState<List<ChatMessageRecord>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(VM_TAG, "Error observing chat messages", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Sohbet geçmişi yüklenemedi."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    // --- RENDER & PHOTO EDIT STATE ---
    private val _renderConfig = MutableStateFlow(RenderConfigState())
    val renderConfig: StateFlow<RenderConfigState> = _renderConfig.asStateFlow()

    private val _renderUiState = MutableStateFlow<RenderUiState>(RenderUiState.Idle)
    val renderUiState: StateFlow<RenderUiState> = _renderUiState.asStateFlow()

    // --- CHAT STATE ---
    private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    // --- 3D & VIRTUAL TOUR STATE ---
    private val _selected3DModel = MutableStateFlow(Model3DPreset.VILLA)
    val selected3DModel: StateFlow<Model3DPreset> = _selected3DModel.asStateFlow()

    private val _tourSourceBitmap = MutableStateFlow<Bitmap?>(null)
    val tourSourceBitmap: StateFlow<Bitmap?> = _tourSourceBitmap.asStateFlow()

    private val _tourAspectRatio = MutableStateFlow(VirtualTourAspectRatio.LANDSCAPE_16_9)
    val tourAspectRatio: StateFlow<VirtualTourAspectRatio> = _tourAspectRatio.asStateFlow()

    private val _tourCameraMove = MutableStateFlow(VirtualTourCameraMove.CINEMATIC_PAN)
    val tourCameraMove: StateFlow<VirtualTourCameraMove> = _tourCameraMove.asStateFlow()

    private val _virtualTourState = MutableStateFlow<VirtualTourUiState>(VirtualTourUiState.Idle)
    val virtualTourState: StateFlow<VirtualTourUiState> = _virtualTourState.asStateFlow()

    // --- BOM AI GENERATION STATE ---
    private val _isGeneratingBom = MutableStateFlow(false)
    val isGeneratingBom: StateFlow<Boolean> = _isGeneratingBom.asStateFlow()

    // --- GLOBAL SNACKBAR / ACTION MESSAGES ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        val project = (projectsState.value as? CloudUiState.Success)?.data?.firstOrNull { it.id == projectId }
        if (project != null) {
            _selected3DModel.value = Model3DPreset.fromRaw(project.architecturalModelType)
        }
    }

    fun select3DModel(preset: Model3DPreset) {
        _selected3DModel.value = preset
    }

    fun applyShowcaseTemplate(template: ShowcaseTemplate) {
        _renderConfig.update {
            it.copy(
                isPhotoEditMode = false,
                spaceType = template.spaceType,
                style = template.style,
                prompt = template.samplePrompt
            )
        }
        _selected3DModel.value = template.model3D
    }

    fun prepareRenderFromChatPrompt(promptText: String) {
        _renderConfig.update {
            it.copy(
                isPhotoEditMode = false,
                prompt = promptText
            )
        }
    }

    fun prepareRenderForProject(project: DesignProject) {
        selectProject(project.id)
        val matchedStyle = ArchStylePreset.entries.firstOrNull {
            it.labelTr.equals(project.style, ignoreCase = true) ||
                it.name.equals(project.style, ignoreCase = true)
        } ?: ArchStylePreset.MEDITERRANEAN
        _renderConfig.update {
            it.copy(
                isPhotoEditMode = false,
                spaceType = SpaceType.fromRaw(project.spaceType),
                style = matchedStyle,
                prompt = project.promptSummary.ifBlank {
                    "${project.title} — ${project.roomCategory}, ${project.style} tarzında ${project.areaSqm.toInt()} m² mimari tasarım"
                }
            )
        }
    }

    fun updateRenderConfig(transform: (RenderConfigState) -> RenderConfigState) {
        _renderConfig.update(transform)
    }

    fun setUploadedPhotoForEdit(bitmap: Bitmap?) {
        _renderConfig.update {
            it.copy(
                uploadedSourceBitmap = bitmap,
                isPhotoEditMode = bitmap != null
            )
        }
    }

    fun runRenderOrPhotoEdit() {
        val cfg = _renderConfig.value
        val cleanPrompt = cfg.prompt.trim()
        if (cleanPrompt.isEmpty()) {
            _renderUiState.value = RenderUiState.Error("Lütfen tasarım veya düzenleme açıklamanızı yazın.")
            return
        }
        if (cfg.isPhotoEditMode && cfg.uploadedSourceBitmap == null) {
            _renderUiState.value = RenderUiState.Error("Lütfen önce galeriden bir oda veya cephe fotoğrafı yükleyin.")
            return
        }

        viewModelScope.launch {
            val statusLabel = if (cfg.isPhotoEditMode) {
                "Fotoğrafınız ${cfg.resolution.label} (${cfg.aspectRatio.label}) kalitesinde yeniden tasarlanıyor..."
            } else {
                "Profesyonel ${cfg.resolution.label} (${cfg.aspectRatio.label}) mimari render üretiliyor..."
            }
            _renderUiState.value = RenderUiState.Loading(statusLabel)
            try {
                val bitmap = if (cfg.isPhotoEditMode && cfg.uploadedSourceBitmap != null) {
                    aiRepo.editArchitecturalPhoto(
                        sourceBitmap = cfg.uploadedSourceBitmap,
                        editPrompt = cleanPrompt,
                        spaceType = cfg.spaceType,
                        style = cfg.style,
                        lighting = cfg.lighting,
                        materialPalette = cfg.materialPalette,
                        aspectRatio = cfg.aspectRatio,
                        resolution = cfg.resolution
                    )
                } else {
                    aiRepo.generateArchitecturalRender(
                        userPrompt = cleanPrompt,
                        spaceType = cfg.spaceType,
                        style = cfg.style,
                        lighting = cfg.lighting,
                        cameraLens = cfg.cameraLens,
                        materialPalette = cfg.materialPalette,
                        aspectRatio = cfg.aspectRatio,
                        resolution = cfg.resolution
                    )
                }
                val settingsSummary = "${cfg.aspectRatio.label} • ${cfg.resolution.label} • ${cfg.lighting.labelTr} • ${cfg.cameraLens.labelTr}"
                _renderUiState.value = RenderUiState.Success(bitmap, cleanPrompt, settingsSummary)
                _tourSourceBitmap.value = bitmap
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Render timed out", e)
                _renderUiState.value = RenderUiState.Error("Render işlemi zaman aşımına uğradı. Lütfen tekrar deneyin.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Render failed", e)
                _renderUiState.value = RenderUiState.Error(e.localizedMessage ?: "Render oluşturulamadı.")
            }
        }
    }

    fun saveCurrentRenderToCloudProject(title: String, areaSqm: Double, existingProjectId: String?) {
        val state = _renderUiState.value
        val cfg = _renderConfig.value
        if (state !is RenderUiState.Success) {
            _snackbarMessage.value = "Önce bir render oluşturun."
            return
        }
        viewModelScope.launch {
            val thumbBase64 = BitmapUtils.bitmapToFirestoreThumbnail(state.bitmap)
            if (existingProjectId != null) {
                val res = cloudRepo.updateProjectRender(
                    projectId = existingProjectId,
                    promptSummary = state.summary,
                    renderSettings = state.settingsLabel,
                    thumbnailBase64 = thumbBase64
                )
                _snackbarMessage.value = if (res.isSuccess) {
                    "Render bulut projesine kaydedildi."
                } else {
                    res.exceptionOrNull()?.localizedMessage ?: "Proje güncellenemedi."
                }
            } else {
                val res = cloudRepo.createProject(
                    title = title.ifBlank { "${cfg.style.labelTr} Tasarım" },
                    spaceType = cfg.spaceType,
                    style = cfg.style.labelTr,
                    roomCategory = if (cfg.spaceType == SpaceType.EXTERIOR) "Dış Cephe" else "İç Mekan",
                    areaSqm = areaSqm,
                    promptSummary = state.summary,
                    renderSettings = state.settingsLabel,
                    modelType = _selected3DModel.value,
                    thumbnailBase64 = thumbBase64
                )
                if (res.isSuccess) {
                    _selectedProjectId.value = res.getOrNull()
                    _snackbarMessage.value = "Yeni proje buluta kaydedildi."
                } else {
                    _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Buluta kaydedilemedi."
                }
            }
        }
    }

    fun createNewProject(
        title: String,
        spaceType: SpaceType,
        style: String,
        roomCategory: String,
        areaSqm: Double,
        promptSummary: String,
        modelType: Model3DPreset
    ) {
        viewModelScope.launch {
            val res = cloudRepo.createProject(
                title = title,
                spaceType = spaceType,
                style = style,
                roomCategory = roomCategory,
                areaSqm = areaSqm,
                promptSummary = promptSummary,
                renderSettings = "16:9 • 2K • Altın Saat",
                modelType = modelType
            )
            if (res.isSuccess) {
                _selectedProjectId.value = res.getOrNull()
                _selected3DModel.value = modelType
                _snackbarMessage.value = "Proje bulut deposunda oluşturuldu."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Proje oluşturulamadı."
            }
        }
    }

    fun updateProjectPhase(projectId: String, phase: ProjectPhase) {
        viewModelScope.launch {
            val res = cloudRepo.updateProjectPhase(projectId, phase)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Proje durumu güncellenemedi."
            } else {
                _snackbarMessage.value = "Proje aşaması güncellendi: ${phase.labelTr}"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val res = cloudRepo.deleteProject(projectId)
            if (res.isSuccess) {
                if (_selectedProjectId.value == projectId) {
                    _selectedProjectId.value = null
                }
                _snackbarMessage.value = "Proje buluttan silindi."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Proje silinemedi."
            }
        }
    }

    // --- MATERIALS / BOM ---
    fun addMaterialToSelectedProject(item: DraftMaterialInput) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val res = cloudRepo.addMaterial(projectId, item)
            if (res.isSuccess) {
                recalculateAndSyncBudget(projectId)
                _snackbarMessage.value = "Malzeme bulut listesine eklendi."
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Malzeme eklenemedi."
            }
        }
    }

    fun toggleMaterialPurchased(material: MaterialItem) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val res = cloudRepo.toggleMaterialPurchased(projectId, material.id, !material.purchased)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Durum güncellenemedi."
            }
        }
    }

    fun deleteMaterial(materialId: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val res = cloudRepo.deleteMaterial(projectId, materialId)
            if (res.isSuccess) {
                recalculateAndSyncBudget(projectId)
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Malzeme silinemedi."
            }
        }
    }

    fun generateAiMaterialsForSelectedProject() {
        val projectId = _selectedProjectId.value
        val project = (projectsState.value as? CloudUiState.Success)?.data?.firstOrNull { it.id == projectId }
        if (project == null) {
            _snackbarMessage.value = "Önce bir bulut projesi seçin veya oluşturun."
            return
        }
        viewModelScope.launch {
            _isGeneratingBom.value = true
            try {
                val refBitmap = (_renderUiState.value as? RenderUiState.Success)?.bitmap
                    ?: BitmapUtils.base64ToBitmap(project.thumbnailBase64)
                val items = aiRepo.generateMaterialList(project, refBitmap)
                val saveRes = cloudRepo.addMaterialsBatch(project.id, items)
                if (saveRes.isSuccess) {
                    val existingTotal = (materialsState.value as? CloudUiState.Success)?.data
                        ?.sumOf { it.lineTotalTry } ?: 0.0
                    val addedTotal = items.sumOf { it.quantity * it.unitPriceTry }
                    cloudRepo.syncProjectBudgetTotal(project.id, existingTotal + addedTotal)
                    _snackbarMessage.value = "${items.size} kalemlik AI malzeme listesi buluta kaydedildi."
                } else {
                    _snackbarMessage.value = saveRes.exceptionOrNull()?.localizedMessage ?: "Malzemeler kaydedilemedi."
                }
            } catch (e: TimeoutCancellationException) {
                _snackbarMessage.value = "Malzeme analizi zaman aşımına uğradı. Tekrar deneyin."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "AI BOM generation failed", e)
                _snackbarMessage.value = e.localizedMessage ?: "Malzeme listesi üretilemedi."
            } finally {
                _isGeneratingBom.value = false
            }
        }
    }

    private suspend fun recalculateAndSyncBudget(projectId: String) {
        val currentList = (materialsState.value as? CloudUiState.Success)?.data ?: return
        val total = currentList.sumOf { it.lineTotalTry }
        cloudRepo.syncProjectBudgetTotal(projectId, total)
    }

    // --- CHATBOT ---
    fun toggleHighThinking(enabled: Boolean) {
        _chatUiState.update { it.copy(highThinkingEnabled = enabled) }
    }

    fun setChatAttachedPhoto(bitmap: Bitmap?) {
        _chatUiState.update { it.copy(attachedPhoto = bitmap) }
    }

    fun sendChatMessage(rawInput: String) {
        val text = rawInput.trim()
        if (text.isEmpty() || _chatUiState.value.isStreaming) return

        val highThinking = _chatUiState.value.highThinkingEnabled
        val photo = _chatUiState.value.attachedPhoto
        val history = (chatHistoryState.value as? CloudUiState.Success)?.data.orEmpty()

        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    isStreaming = true,
                    streamingBuffer = "",
                    attachedPhoto = null,
                    errorMessage = null
                )
            }
            cloudRepo.saveChatMessage(
                role = "USER",
                text = if (photo != null) "[Fotoğraf Eklendi] $text" else text,
                thinkingUsed = highThinking
            )

            val accumulated = StringBuilder()
            try {
                aiRepo.streamArchitectChat(
                    history = history,
                    userMessage = text,
                    attachedPhoto = photo,
                    highThinking = highThinking
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _chatUiState.update { it.copy(streamingBuffer = accumulated.toString()) }
                }

                val fullReply = accumulated.toString().trim()
                if (fullReply.isNotEmpty()) {
                    val extractedPrompt = extractRenderPromptTag(fullReply)
                    val cleanDisplayText = stripRenderPromptTag(fullReply)
                    cloudRepo.saveChatMessage(
                        role = "MODEL",
                        text = cleanDisplayText.ifBlank { fullReply },
                        thinkingUsed = highThinking,
                        attachedPrompt = extractedPrompt
                    )
                } else {
                    _chatUiState.update { it.copy(errorMessage = "AI Mimar boş yanıt döndürdü.") }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Chat timed out", e)
                _chatUiState.update { it.copy(errorMessage = "Yanıt zaman aşımına uğradı. Lütfen tekrar deneyin.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Chat stream failed", e)
                _chatUiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "AI Mimar yanıt veremedi.")
                }
            } finally {
                _chatUiState.update { it.copy(isStreaming = false, streamingBuffer = "") }
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            val res = cloudRepo.clearChatHistory()
            _snackbarMessage.value = if (res.isSuccess) {
                "Sohbet geçmişi temizlendi."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Geçmiş temizlenemedi."
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

    // --- VIRTUAL TOUR ---
    fun setTourSourceBitmap(bitmap: Bitmap?) {
        _tourSourceBitmap.value = bitmap
    }

    fun setTourAspectRatio(ratio: VirtualTourAspectRatio) {
        _tourAspectRatio.value = ratio
    }

    fun setTourCameraMove(move: VirtualTourCameraMove) {
        _tourCameraMove.value = move
    }

    fun generateAiVirtualTour() {
        val source = _tourSourceBitmap.value
            ?: (_renderUiState.value as? RenderUiState.Success)?.bitmap
        if (source == null) {
            _virtualTourState.value = VirtualTourUiState.Error(
                "Sanal tur oluşturmak için önce bir fotoğraf yükleyin veya Render sekmesinde görsel üretin."
            )
            return
        }
        val aspect = _tourAspectRatio.value
        val move = _tourCameraMove.value
        val spaceHint = "${_renderConfig.value.spaceType.labelTr} - ${_renderConfig.value.style.labelTr}"

        viewModelScope.launch {
            _virtualTourState.value = VirtualTourUiState.Generating("Sanal tur sahneleri planlanıyor...")
            try {
                val frames = aiRepo.generateVirtualTourSequence(
                    sourceBitmap = source,
                    aspectRatio = aspect,
                    cameraMove = move,
                    spaceHint = spaceHint,
                    onProgress = { status ->
                        _virtualTourState.value = VirtualTourUiState.Generating(status)
                    }
                )
                _virtualTourState.value = VirtualTourUiState.Ready(frames, aspect, move)
            } catch (e: TimeoutCancellationException) {
                _virtualTourState.value = VirtualTourUiState.Error("Sanal tur oluşturma zaman aşımına uğradı.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Virtual tour generation failed", e)
                _virtualTourState.value = VirtualTourUiState.Error(
                    e.localizedMessage ?: "Sanal tur oluşturulamadı."
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            cloudRepo: CloudProjectRepository,
            aiRepo: GeminiStudioRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StudioViewModel(cloudRepo, aiRepo, currentUserId)
            }
        }
    }
}
