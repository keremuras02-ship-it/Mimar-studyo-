package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.BitmapUtils
import com.example.core.ProEntitlementState
import com.example.core.rememberRoomCameraCapture
import com.example.ui.viewmodel.ArchitecturalChatMessage
import com.example.ui.viewmodel.ArchitecturalChatViewModel
import com.example.ui.viewmodel.ConversationalUiState
import kotlinx.coroutines.launch

/**
 * Reusable conversational architectural design UI component powered by [ArchitecturalChatViewModel]
 * and Firebase AI Logic (streaming Gemini chat, High Thinking analysis, and inline render generation).
 */
@Composable
fun ArchitecturalDesignChatComponent(
    viewModel: ArchitecturalChatViewModel,
    entitlement: ProEntitlementState,
    onOpenInProRenderStudio: (String) -> Unit,
    onSendRenderToVirtualTour: (Bitmap) -> Unit,
    onChatPromptSubmitted: () -> Unit,
    onInlineRenderSubmitted: () -> Unit,
    onOpenPaywall: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val highThinkingEnabled by viewModel.highThinkingEnabled.collectAsStateWithLifecycle()
    val directDesignMode by viewModel.directDesignMode.collectAsStateWithLifecycle()
    val attachedPhoto by viewModel.attachedPhoto.collectAsStateWithLifecycle()

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    LaunchedEffect(entitlement.isPro, highThinkingEnabled) {
        if (!entitlement.isPro && highThinkingEnabled) {
            viewModel.toggleHighThinking(false)
        }
    }

    val submitWithEntitlementCheck: (String) -> Unit = { rawText ->
        val clean = rawText.trim()
        if (clean.isNotEmpty()) {
            if (directDesignMode) {
                if (!entitlement.canRunRender()) {
                    onOpenPaywall(
                        "Normal modda günlük ${entitlement.dailyRenderLimit} görsel üretim hakkınız doldu. Sınırsız üretim için Pro'ya geçin (${entitlement.formattedPrice})."
                    )
                } else {
                    onInlineRenderSubmitted()
                    viewModel.submitPrompt(clean)
                }
            } else {
                if (!entitlement.canSendChat()) {
                    onOpenPaywall(
                        "Normal modda günlük ${entitlement.dailyChatLimit} AI Mimar mesaj hakkınız doldu. Sınırsız sohbet ve Derin Düşünme için Pro'ya geçin (${entitlement.formattedPrice})."
                    )
                } else {
                    onChatPromptSubmitted()
                    viewModel.submitPrompt(clean)
                }
            }
        }
    }

    val isBusy = uiState is ConversationalUiState.Loading ||
        uiState is ConversationalUiState.Streaming ||
        uiState is ConversationalUiState.GeneratingDesign

    val launchRoomCamera = rememberRoomCameraCapture(
        maxDimension = 1024,
        onPhotoCaptured = { bitmap ->
            cameraPermissionDenied = false
            viewModel.setAttachedPhoto(bitmap)
            if (inputPrompt.isBlank()) {
                inputPrompt = "Bu mevcut odanın fotoğrafını analiz et; modern mimari tasarım ve dekorasyon önerileri sun."
            }
        },
        onPermissionDenied = {
            cameraPermissionDenied = true
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapUtils.decodeScaledBitmap(context, uri, 1024)
                viewModel.setAttachedPhoto(bmp)
            }
        }
    }

    val quickPrompts = remember {
        listOf(
            "200 m² havuzlu modern traverten taş villa tasarla",
            "Çift kat yüksekliğinde sıcak ceviz & cam salon öner",
            "Minimalist Japandi açık mutfak ve ışık planı oluştur",
            "Eğimli arsa için güneş yönelimli dış cephe analizi yap"
        )
    }

    LaunchedEffect(messages.size, uiState) {
        val count = messages.size + if (isBusy) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Studio Conversation Header & Mode Switches
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (entitlement.isPro) {
                                "BAŞ MİMAR AI • PRO STÜDYO"
                            } else {
                                "BAŞ MİMAR AI • KALAN MESAJ: ${entitlement.remainingFreeChats}/${entitlement.dailyChatLimit}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Mimari Tasarım Sohbeti",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!entitlement.isPro) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clickable { onOpenPaywall(null) }
                                    .testTag("chat_header_pro_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = "Pro'ya geç",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = viewModel::clearConversation,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("architect_chat_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Sohbet geçmişini temizle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = directDesignMode,
                            onClick = { viewModel.toggleDirectDesignMode(!directDesignMode) },
                            label = {
                                Text(
                                    if (directDesignMode) "Doğrudan Görsel Üret: AÇIK" else "Doğrudan Görsel Üret"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("architect_direct_render_toggle")
                        )
                    }
                    item {
                        val isThinkingLocked = !entitlement.isPro
                        FilterChip(
                            selected = highThinkingEnabled && !directDesignMode && !isThinkingLocked,
                            enabled = !directDesignMode,
                            onClick = {
                                if (isThinkingLocked) {
                                    onOpenPaywall(
                                        "Derin Düşünme (High Thinking) mimari analiz modu Pro kullanıcılarına özeldir (${entitlement.formattedPrice})."
                                    )
                                } else {
                                    viewModel.toggleHighThinking(!highThinkingEnabled)
                                }
                            },
                            label = {
                                Text(
                                    when {
                                        isThinkingLocked -> "Derin Düşünme (PRO)"
                                        highThinkingEnabled -> "Derin Düşünme: AÇIK"
                                        else -> "Derin Mimari Analiz"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isThinkingLocked) Icons.Rounded.Lock else Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("architect_high_thinking_toggle")
                        )
                    }
                }
            }
        }

        // Scrollable Architectural Conversation Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && !isBusy) {
                item(key = "welcome_card") {
                    ArchitectWelcomeCard(
                        prompts = quickPrompts,
                        onPromptSelected = { selected -> submitWithEntitlementCheck(selected) },
                        onTakeRoomPhoto = launchRoomCamera
                    )
                }
            }

            items(
                items = messages,
                key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }
            ) { msg ->
                ArchitecturalMessageBubble(
                    message = msg,
                    isBusy = isBusy,
                    onGenerateInlineRender = { prompt ->
                        if (!entitlement.canRunRender()) {
                            onOpenPaywall(
                                "Normal modda günlük ${entitlement.dailyRenderLimit} görsel üretim hakkınız doldu. Sınırsız üretim için Pro'ya geçin (${entitlement.formattedPrice})."
                            )
                        } else {
                            onInlineRenderSubmitted()
                            viewModel.generateInlineArchitecturalDesign(prompt)
                        }
                    },
                    onOpenInProStudio = onOpenInProRenderStudio,
                    onAnimateInTour = onSendRenderToVirtualTour
                )
            }

            // Active Streaming / Loading / Inline Design Generation Indicator
            when (val activeState = uiState) {
                is ConversationalUiState.Loading -> {
                    item(key = "loading_indicator") {
                        ActiveGenerationBubble(
                            title = if (highThinkingEnabled) {
                                "Derin mimari muhakeme başlatılıyor..."
                            } else {
                                "Baş Mimar AI düşünüyor..."
                            },
                            bodyText = null
                        )
                    }
                }
                is ConversationalUiState.Streaming -> {
                    item(key = "streaming_indicator") {
                        ActiveGenerationBubble(
                            title = if (highThinkingEnabled) {
                                "Baş Mimar AI • Derin Analiz Akışı"
                            } else {
                                "Baş Mimar AI yazıyor..."
                            },
                            bodyText = activeState.partialText
                        )
                    }
                }
                is ConversationalUiState.GeneratingDesign -> {
                    item(key = "design_gen_indicator") {
                        ActiveGenerationBubble(
                            title = activeState.statusText,
                            bodyText = "Fotogerçekçi mimari görsel 20–60 saniye içinde sohbet akışına eklenecektir."
                        )
                    }
                }
                is ConversationalUiState.Error -> {
                    item(key = "error_card") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = activeState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!activeState.failedPrompt.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { viewModel.submitPrompt(activeState.failedPrompt) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Tekrar dene",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                AnimatedVisibility(visible = cameraPermissionDenied) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Oda fotoğrafı çekmek için kamera izni gereklidir. Ayarlardan izin verebilir veya galeriden fotoğraf seçebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { cameraPermissionDenied = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Uyarıyı kapat",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = attachedPhoto != null) {
                    val bmp = attachedPhoto
                    if (bmp != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Eklenen referans fotoğraf",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            IconButton(
                                onClick = { viewModel.setAttachedPhoto(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Fotoğrafı kaldır",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = launchRoomCamera,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("architect_chat_take_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Mevcut odanın fotoğrafını çek",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("architect_chat_attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Galeriden referans fotoğraf ekle",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = {
                            inputPrompt = it
                            viewModel.dismissError()
                        },
                        placeholder = {
                            Text(
                                if (directDesignMode) {
                                    "Üretilecek mimari tasarımı tarif edin..."
                                } else {
                                    "Hayalinizdeki evi veya odayı yazın..."
                                }
                            )
                        },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val text = inputPrompt.trim()
                                if (text.isNotEmpty() && !isBusy) {
                                    inputPrompt = ""
                                    submitWithEntitlementCheck(text)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("architect_chat_prompt_input")
                    )

                    IconButton(
                        onClick = {
                            val text = inputPrompt.trim()
                            if (text.isNotEmpty() && !isBusy) {
                                inputPrompt = ""
                                submitWithEntitlementCheck(text)
                            }
                        },
                        enabled = !isBusy && inputPrompt.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputPrompt.isNotBlank() && !isBusy) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .testTag("architect_chat_send_button")
                    ) {
                        Icon(
                            imageVector = if (directDesignMode) Icons.Rounded.AutoAwesome else Icons.AutoMirrored.Rounded.Send,
                            contentDescription = if (directDesignMode) "Tasarım üret" else "Mesaj gönder",
                            tint = if (inputPrompt.isNotBlank() && !isBusy) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitectWelcomeCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    onTakeRoomPhoto: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Architecture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Baş Mimar AI Tasarım Asistanı",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "İç mekan, dış cephe, kat planı veya malzeme önerisi için metin yazın ya da mevcut odanızın fotoğrafını çekerek AI tasarım önerisi alın.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onTakeRoomPhoto,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_take_room_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mevcut Odanın Fotoğrafını Çek ve Öneri Al")
            }
            Text(
                text = "Hızlı Tasarım Komutları",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            prompts.forEachIndexed { idx, prompt ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPromptSelected(prompt) }
                        .testTag("quick_architect_prompt_$idx")
                ) {
                    Text(
                        text = "“$prompt”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveGenerationBubble(
    title: String,
    bodyText: String?
) {
    Surface(
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(0.92f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!bodyText.isNullOrBlank()) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ArchitecturalMessageBubble(
    message: ArchitecturalChatMessage,
    isBusy: Boolean,
    onGenerateInlineRender: (String) -> Unit,
    onOpenInProStudio: (String) -> Unit,
    onAnimateInTour: (Bitmap) -> Unit
) {
    val isUser = message.role == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = if (isUser) {
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            } else {
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            },
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (message.thinkingUsed) Icons.Rounded.Psychology else Icons.Rounded.Architecture,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (message.thinkingUsed) {
                                "BAŞ MİMAR AI • DERİN ANALİZ"
                            } else {
                                "BAŞ MİMAR AI"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Inline generated architectural render preview
                val designBitmap = message.generatedDesignBitmap
                if (designBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black)
                    ) {
                        Image(
                            bitmap = designBitmap.asImageBitmap(),
                            contentDescription = "Sohbette üretilen mimari tasarım renderı",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = Color(0xCC0E1217),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "AI MİMARİ RENDER • 16:9",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAnimateInTour(designBitmap) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_render_to_tour_button")
                        ) {
                            Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sanal Tur Yap", style = MaterialTheme.typography.labelMedium)
                        }
                        if (!message.renderPromptSuggestion.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onOpenInProStudio(message.renderPromptSuggestion) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_render_to_studio_button")
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4K Stüdyoda Aç", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // One-tap inline render or Pro Render Studio launch for text proposals
                val renderPrompt = message.renderPromptSuggestion
                if (!isUser && !renderPrompt.isNullOrBlank() && designBitmap == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGenerateInlineRender(renderPrompt) },
                            enabled = !isBusy,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inline_generate_design_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Sohbette Render Et",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenInProStudio(renderPrompt) },
                            enabled = !isBusy,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_pro_studio_from_chat_button")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("4K Stüdyo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
