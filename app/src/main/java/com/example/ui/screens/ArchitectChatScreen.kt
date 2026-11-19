package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BitmapUtils
import com.example.data.model.ChatMessageRecord
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.CloudUiState
import kotlinx.coroutines.launch

@Composable
fun ArchitectChatScreen(
    historyState: CloudUiState<List<ChatMessageRecord>>,
    chatUiState: ChatUiState,
    onToggleHighThinking: (Boolean) -> Unit,
    onAttachPhoto: (Bitmap?) -> Unit,
    onSendMessage: (String) -> Unit,
    onRenderPromptFromChat: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapUtils.decodeScaledBitmap(context, uri, 1024)
                onAttachPhoto(bmp)
            }
        }
    }

    val starterPrompts = remember {
        listOf(
            "200 m² havuzlu modern taş villa tasarla, cephe ve kat planı öner",
            "Küçük 28 m² salonu lüks Japandi tarzda nasıl ferah gösteririz?",
            "Eğimli arsa için güneş yönelimli dış mekan ve teras analizi yap",
            "Açık mutfak ve yemek odası için aydınlatma ve malzeme paleti öner"
        )
    }

    val messages = (historyState as? CloudUiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size, chatUiState.streamingBuffer.length) {
        val totalCount = messages.size + if (chatUiState.isStreaming) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header + High Thinking Mode Toggle
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
                            text = "BAŞ MİMAR AI ASİSTANI",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hayalindeki Evi Yaz, Üretelim",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Sohbeti temizle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // High Thinking Toggle (Gemini 4 Argon + ThinkingLevel.HIGH)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = chatUiState.highThinkingEnabled,
                        onClick = { onToggleHighThinking(!chatUiState.highThinkingEnabled) },
                        label = {
                            Text(
                                text = if (chatUiState.highThinkingEnabled) {
                                    "Derin Düşünme Aktif (High Thinking)"
                                } else {
                                    "Derin Mimari Analiz Modu"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("high_thinking_toggle")
                    )
                }
            }
        }

        // Scrollable Message Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && !chatUiState.isStreaming) {
                item {
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
                                    text = "MimarStüdyo Baş Mimar AI",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "İç mekan, dış cephe, oda yerleşimi, aydınlatma veya peyzaj hakkında istediğinizi yazın ya da fotoğraf ekleyin. Size mimari çözümü sunup tek dokunuşla fotogerçekçi render üreteyim.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Örnek Sorular",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            starterPrompts.forEach { suggestion ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSendMessage(suggestion) }
                                ) {
                                    Text(
                                        text = "“$suggestion”",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }) { msg ->
                ChatBubble(
                    message = msg,
                    onRenderProposal = onRenderPromptFromChat
                )
            }

            if (chatUiState.isStreaming) {
                item(key = "streaming_bubble") {
                    Surface(
                        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (chatUiState.highThinkingEnabled) {
                                        "Derin mimari muhakeme yürütülüyor..."
                                    } else {
                                        "Baş Mimar yazıyor..."
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (chatUiState.streamingBuffer.isNotBlank()) {
                                Text(
                                    text = chatUiState.streamingBuffer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (chatUiState.errorMessage != null) {
                item(key = "chat_error") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chatUiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (chatUiState.attachedPhoto != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        Image(
                            bitmap = chatUiState.attachedPhoto.asImageBitmap(),
                            contentDescription = "Sohbete eklenen fotoğraf",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                        IconButton(
                            onClick = { onAttachPhoto(null) },
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Fotoğraf ekle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nasıl bir ev veya oda tasarlayalım?") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    IconButton(
                        onClick = {
                            val toSend = inputText.trim()
                            if (toSend.isNotEmpty()) {
                                inputText = ""
                                onSendMessage(toSend)
                            }
                        },
                        enabled = !chatUiState.isStreaming && inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !chatUiState.isStreaming) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Gönder",
                            tint = if (inputText.isNotBlank() && !chatUiState.isStreaming) {
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
private fun ChatBubble(
    message: ChatMessageRecord,
    onRenderProposal: (String) -> Unit
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
                color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = if (message.thinkingUsed) "BAŞ MİMAR AI • DERİN ANALİZ" else "BAŞ MİMAR AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )

                val renderPrompt = message.attachedPrompt
                if (!isUser && !renderPrompt.isNullOrBlank()) {
                    Button(
                        onClick = { onRenderProposal(renderPrompt) },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("render_from_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bu Tasarımı Render Stüdyosunda Üret",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
