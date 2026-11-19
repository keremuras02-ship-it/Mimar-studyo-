package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BitmapUtils
import com.example.core.ProEntitlementState
import com.example.core.rememberRoomCameraCapture
import com.example.data.model.ArchStylePreset
import com.example.data.model.CameraLensPreset
import com.example.data.model.DesignProject
import com.example.data.model.LightingPreset
import com.example.data.model.MaterialPalettePreset
import com.example.data.model.RenderAspectRatio
import com.example.data.model.RenderResolution
import com.example.data.model.SpaceType
import com.example.ui.viewmodel.RenderConfigState
import com.example.ui.viewmodel.RenderUiState
import kotlinx.coroutines.launch

@Composable
fun RenderStudioScreen(
    config: RenderConfigState,
    renderState: RenderUiState,
    entitlement: ProEntitlementState,
    projects: List<DesignProject>,
    selectedProjectId: String?,
    onUpdateConfig: ((RenderConfigState) -> RenderConfigState) -> Unit,
    onPhotoPicked: (Bitmap?) -> Unit,
    onGenerateClick: () -> Unit,
    onSaveRenderToCloud: (String, Double, String?) -> Unit,
    onSendToVirtualTour: (Bitmap) -> Unit,
    onGenerateBomFromRender: () -> Unit,
    onOpenPaywall: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    // Ensure Normal mode cannot stay on 2K/4K resolution
    LaunchedEffect(entitlement.isPro, config.resolution) {
        if (!entitlement.isPro && config.resolution != RenderResolution.SIZE_1K) {
            onUpdateConfig { it.copy(resolution = RenderResolution.SIZE_1K) }
        }
    }

    val launchRoomCamera = rememberRoomCameraCapture(
        maxDimension = 1280,
        onPhotoCaptured = { bmp ->
            cameraPermissionDenied = false
            onPhotoPicked(bmp)
        },
        onPermissionDenied = {
            cameraPermissionDenied = true
        }
    )

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapUtils.decodeScaledBitmap(context, uri, 1280)
                onPhotoPicked(bmp)
            }
        }
    }

    val quickPhotoEditChips = remember {
        listOf(
            "Modern minimalist ahşap & traverten dönüşüm",
            "Akşam mavi saat dış cephe LED aydınlatması ekle",
            "Lüks İtalyan mobilyalar ve gizli tavan ışığı yerleştir",
            "Doğal taş cephe kaplama ve modern peyzaj ekle"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Screen Title & Free Quota Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PROFESYONEL RENDER & FOTOĞRAF STÜDYOSU",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "İç & Dış Mekan Tasarla",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (!entitlement.isPro) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenPaywall(
                                    "Normal modda günde ${entitlement.dailyRenderLimit} adet 1K render üretebilirsiniz. 2K/4K ve sınırsız render için Pro'ya geçin (${entitlement.formattedPrice})."
                                )
                            }
                            .testTag("render_free_quota_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NORMAL MOD • Kalan Günlük Hak: ${entitlement.remainingFreeRenders}/${entitlement.dailyRenderLimit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (entitlement.remainingFreeRenders == 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = "2K / 4K çözünürlük ve sınırsız üretim Pro'da açık",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entitlement.formattedPrice,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Studio Mode Segmented Toggle: Text-to-Render vs Photo Upload & Edit
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !config.isPhotoEditMode,
                    onClick = { onUpdateConfig { it.copy(isPhotoEditMode = false) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("mode_new_render_tab")
                ) {
                    Text("Sıfırdan Render")
                }
                SegmentedButton(
                    selected = config.isPhotoEditMode,
                    onClick = { onUpdateConfig { it.copy(isPhotoEditMode = true) } },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("mode_photo_edit_tab")
                ) {
                    Text("Fotoğraf Yükle & Düzenle")
                }
            }
        }

        // Viewport Preview Card (Render Result / Before-After Slider / Photo Upload Box)
        item {
            RenderViewportCard(
                config = config,
                renderState = renderState,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSaveClick = { showSaveDialog = true },
                onAnimateClick = { bmp -> onSendToVirtualTour(bmp) },
                onBomClick = onGenerateBomFromRender
            )
        }

        // Photo Edit Quick Presets when in Photo Edit Mode
        if (config.isPhotoEditMode) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cameraPermissionDenied) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Oda fotoğrafı çekmek için kamera izni gereklidir. Galeriden fotoğraf seçebilir veya kamera izni verebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = launchRoomCamera,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("render_take_photo_button")
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Oda Fotoğrafı Çek")
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (config.uploadedSourceBitmap == null) "Galeriden Seç" else "Değiştir")
                        }
                    }

                    Text("Hızlı Dönüşüm Komutları", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPhotoEditChips) { chip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    onUpdateConfig { it.copy(prompt = chip) }
                                }
                            ) {
                                Text(
                                    text = chip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Prompt Input Card
        item {
            OutlinedTextField(
                value = config.prompt,
                onValueChange = { text -> onUpdateConfig { it.copy(prompt = text) } },
                label = {
                    Text(
                        if (config.isPhotoEditMode) {
                            "Fotoğrafta ne değiştirmek istiyorsunuz?"
                        } else {
                            "Mimari tasarım detaylarınızı yazın"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (config.isPhotoEditMode) {
                            "Örn: Duvarları sıcak ceviz ahşap kapla, zemini honlu traverten yap, tavana gizli LED ekle..."
                        } else {
                            "Örn: Deniz manzaralı iki katlı traverten villa, konsol çatı, cam galeri ve sonsuzluk havuzu..."
                        }
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("render_prompt_input")
            )
        }

        // Primary Generate Button
        item {
            val isLoading = renderState is RenderUiState.Loading
            val hasQuota = entitlement.canRunRender()
            Button(
                onClick = {
                    if (!hasQuota) {
                        onOpenPaywall(
                            "Normal modda günlük ${entitlement.dailyRenderLimit} ücretsiz render hakkınız doldu. Sınırsız ve reklamsız render için Pro'ya geçin (${entitlement.formattedPrice})."
                        )
                    } else {
                        onGenerateClick()
                    }
                },
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_render_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Render Motoru Çalışıyor...")
                } else if (!hasQuota) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Günlük Limit Doldu • Pro'ya Geç (${entitlement.formattedPrice})",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (config.isPhotoEditMode) {
                            "Fotoğrafı Yeniden Tasarla (${config.resolution.label} • ${config.aspectRatio.label})"
                        } else {
                            "Render Üret (${config.resolution.label} • ${config.aspectRatio.label})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Studio Control Deck: Space Type, Aspect Ratio (ALL 8), Resolution (1K / 2K / 4K)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profesyonel Render Parametreleri", style = MaterialTheme.typography.titleMedium)
                    }

                    // Mekan Tipi
                    ControlSectionHeader("Mekan Tipi")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceType.entries.forEach { sp ->
                            FilterChip(
                                selected = config.spaceType == sp,
                                onClick = { onUpdateConfig { it.copy(spaceType = sp) } },
                                label = { Text(sp.labelTr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Çözünürlük (1K, 2K, 4K — 2K & 4K Pro-only in Normal mode)
                    ControlSectionHeader("Çözünürlük & Render Motoru", Icons.Rounded.HighQuality)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RenderResolution.entries.forEach { res ->
                            val isLocked = !entitlement.isPro && res != RenderResolution.SIZE_1K
                            val selected = config.resolution == res && !isLocked
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (isLocked) {
                                            onOpenPaywall(
                                                "${res.label} yüksek çözünürlüklü render motoru Pro moduna özeldir. Pro'ya geçerek 2K ve 4K renderların kilidini açın (${entitlement.formattedPrice})."
                                            )
                                        } else {
                                            onUpdateConfig { it.copy(resolution = res) }
                                        }
                                    }
                                    .testTag("resolution_${res.label}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = res.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isLocked) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = "Pro özelliği",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = when {
                                            res == RenderResolution.SIZE_1K -> "Ücretsiz"
                                            isLocked -> "PRO Kilitli"
                                            else -> "Stüdyo Pro"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // En-Boy Oranı (ALL 8 ratios: 1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 21:9)
                    ControlSectionHeader("En-Boy Oranı (8 Format)")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RenderAspectRatio.entries) { ratio ->
                            FilterChip(
                                selected = config.aspectRatio == ratio,
                                onClick = { onUpdateConfig { it.copy(aspectRatio = ratio) } },
                                label = {
                                    Text(
                                        text = ratio.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("aspect_ratio_${ratio.label.replace(":", "_")}")
                            )
                        }
                    }

                    // Mimari Stil
                    ControlSectionHeader("Mimari Stil")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ArchStylePreset.entries) { preset ->
                            FilterChip(
                                selected = config.style == preset,
                                onClick = { onUpdateConfig { it.copy(style = preset) } },
                                label = { Text(preset.labelTr) }
                            )
                        }
                    }

                    // Işık & Atmosfer
                    ControlSectionHeader("Işık & Gün Saati", Icons.Rounded.WbSunny)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LightingPreset.entries) { light ->
                            FilterChip(
                                selected = config.lighting == light,
                                onClick = { onUpdateConfig { it.copy(lighting = light) } },
                                label = { Text(light.labelTr) }
                            )
                        }
                    }

                    // Kamera Lensi
                    ControlSectionHeader("Kamera & Optik Lens", Icons.Rounded.CameraAlt)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CameraLensPreset.entries) { lens ->
                            FilterChip(
                                selected = config.cameraLens == lens,
                                onClick = { onUpdateConfig { it.copy(cameraLens = lens) } },
                                label = { Text(lens.labelTr) }
                            )
                        }
                    }

                    // Malzeme Paleti
                    ControlSectionHeader("Malzeme Paleti", Icons.Rounded.Palette)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MaterialPalettePreset.entries) { pal ->
                            FilterChip(
                                selected = config.materialPalette == pal,
                                onClick = { onUpdateConfig { it.copy(materialPalette = pal) } },
                                label = { Text(pal.labelTr) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveRenderToCloudDialog(
            projects = projects,
            defaultProjectId = selectedProjectId,
            defaultTitle = "${config.style.labelTr} ${config.spaceType.labelTr}",
            onDismiss = { showSaveDialog = false },
            onSave = { title, area, existingId ->
                onSaveRenderToCloud(title, area, existingId)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun ControlSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RenderViewportCard(
    config: RenderConfigState,
    renderState: RenderUiState,
    onPickPhoto: () -> Unit,
    onSaveClick: () -> Unit,
    onAnimateClick: (Bitmap) -> Unit,
    onBomClick: () -> Unit
) {
    val clampedAspect = config.aspectRatio.ratioValue.coerceIn(0.62f, 2.2f)
    var showBeforeAfter by remember { mutableStateOf(true) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(clampedAspect)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (renderState) {
                    is RenderUiState.Loading -> {
                        ShimmerRenderPlaceholder(statusText = renderState.statusText)
                    }
                    is RenderUiState.Success -> {
                        if (config.isPhotoEditMode && config.uploadedSourceBitmap != null && showBeforeAfter) {
                            BeforeAfterSlider(
                                beforeBitmap = config.uploadedSourceBitmap,
                                afterBitmap = renderState.bitmap,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                bitmap = renderState.bitmap.asImageBitmap(),
                                contentDescription = "Üretilen mimari render",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Telemetry badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xCC0E1217),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = renderState.settingsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is RenderUiState.Error -> {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Render Hatası",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = renderState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is RenderUiState.Idle -> {
                        if (config.isPhotoEditMode && config.uploadedSourceBitmap != null) {
                            Image(
                                bitmap = config.uploadedSourceBitmap.asImageBitmap(),
                                contentDescription = "Yüklenen kaynak fotoğraf",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xCC0E1217),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "KAYNAK FOTOĞRAF HAZIR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(enabled = config.isPhotoEditMode, onClick = onPickPhoto)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (config.isPhotoEditMode) Icons.Rounded.AddPhotoAlternate else Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (config.isPhotoEditMode) {
                                        "Galeriden Oda veya Cephe Fotoğrafı Yükleyin"
                                    } else {
                                        "Stüdyo Render Önizleme Alanı"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (config.isPhotoEditMode) {
                                        "Fotoğrafınızı seçin, stil ve değişiklik komutunuzu yazın."
                                    } else {
                                        "${config.aspectRatio.label} • ${config.resolution.label} • ${config.style.labelTr}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action strip when a render is ready
            if (renderState is RenderUiState.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_render_cloud_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Buluta Kaydet", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { onAnimateClick(renderState.bitmap) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("animate_render_tour_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Sanal Tur Yap", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBomClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bom_from_render_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Bu Render'dan Malzeme Listesi", style = MaterialTheme.typography.labelLarge)
                    }
                    if (config.isPhotoEditMode && config.uploadedSourceBitmap != null) {
                        FilterChip(
                            selected = showBeforeAfter,
                            onClick = { showBeforeAfter = !showBeforeAfter },
                            label = { Text("Önce/Sonra") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val x = change.position.x
                    splitFraction = (x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                }
            }
    ) {
        // Full After Render (Right side)
        Image(
            bitmap = afterBitmap.asImageBitmap(),
            contentDescription = "Yeniden tasarlanmış render",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Clipped Before Photo (Left side)
        Image(
            bitmap = beforeBitmap.asImageBitmap(),
            contentDescription = "Orijinal fotoğraf",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * splitFraction) {
                        this@drawWithContent.drawContent()
                    }
                }
        )
        // Vertical Divider Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(splitFraction)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(2.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(30.dp)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Compare,
                        contentDescription = "Önce ve sonra kaydırıcı",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Surface(
            color = Color(0xBB0E1217),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text("ÖNCE", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Surface(
            color = Color(0xBB0E1217),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            Text("SONRA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ShimmerRenderPlaceholder(statusText: String) {
    val transition = rememberInfiniteTransition(label = "render_shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_shift"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                    start = androidx.compose.ui.geometry.Offset(shift - 400f, 0f),
                    end = androidx.compose.ui.geometry.Offset(shift, 400f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Yüksek kaliteli mimari render 20–60 saniye sürebilir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SaveRenderToCloudDialog(
    projects: List<DesignProject>,
    defaultProjectId: String?,
    defaultTitle: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String?) -> Unit
) {
    var createNew by remember { mutableStateOf(projects.isEmpty()) }
    var chosenProjectId by remember { mutableStateOf(defaultProjectId ?: projects.firstOrNull()?.id) }
    var newTitle by remember { mutableStateOf(defaultTitle) }
    var areaText by remember { mutableStateOf("180") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Render'ı Buluta Kaydet", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (projects.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !createNew,
                            onClick = { createNew = false },
                            label = { Text("Mevcut Proje") }
                        )
                        FilterChip(
                            selected = createNew,
                            onClick = { createNew = true },
                            label = { Text("Yeni Proje") }
                        )
                    }
                }
                if (createNew || projects.isEmpty()) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Proje Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = { areaText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tahmini Alan (m²)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        projects.take(6).forEach { proj ->
                            FilterChip(
                                selected = chosenProjectId == proj.id,
                                onClick = { chosenProjectId = proj.id },
                                label = { Text("${proj.title} (${proj.areaSqm.toInt()} m²)") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = areaText.toDoubleOrNull()?.coerceIn(10.0, 50000.0) ?: 180.0
                    onSave(newTitle, area, if (createNew) null else chosenProjectId)
                },
                modifier = Modifier.testTag("confirm_save_render_button")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
