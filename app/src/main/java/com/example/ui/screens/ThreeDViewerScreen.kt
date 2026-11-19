package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BitmapUtils
import com.example.core.ProEntitlementState
import com.example.data.model.Model3DPreset
import com.example.data.model.VirtualTourAspectRatio
import com.example.data.model.VirtualTourCameraMove
import com.example.ui.viewmodel.VirtualTourUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)
private data class ArchBox3D(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val baseColor: Color,
    val layer: String,
    val alpha: Float = 1f
)

private data class TourWaypoint3D(
    val labelTr: String,
    val yawDeg: Float,
    val pitchDeg: Float,
    val zoom: Float
)

private val WalkWaypoints3D = listOf(
    TourWaypoint3D("Dış Cephe", 38f, 24f, 1.0f),
    TourWaypoint3D("Giriş Avlusu", -25f, 18f, 1.25f),
    TourWaypoint3D("Salon Galeri", 12f, 12f, 1.48f),
    TourWaypoint3D("Havuz & Teras", 128f, 22f, 1.15f),
    TourWaypoint3D("Kuş Bakışı Plan", 45f, 56f, 0.92f)
)

@Composable
fun ThreeDViewerScreen(
    entitlement: ProEntitlementState,
    selectedModel: Model3DPreset,
    tourSourceBitmap: Bitmap?,
    tourAspectRatio: VirtualTourAspectRatio,
    tourCameraMove: VirtualTourCameraMove,
    virtualTourState: VirtualTourUiState,
    initialSubTab: Int = 0,
    onSelectModel: (Model3DPreset) -> Unit,
    onSetTourSourceBitmap: (Bitmap?) -> Unit,
    onSetTourAspectRatio: (VirtualTourAspectRatio) -> Unit,
    onSetTourCameraMove: (VirtualTourCameraMove) -> Unit,
    onGenerateAiTour: () -> Unit,
    onOpenPaywall: (String?) -> Unit
) {
    var activeSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "3D MİMARİ MOTOR & SANAL TUR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "3D İnceleme ve Sanal Tur",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("tab_3d_viewer")
                ) {
                    Text("İnteraktif 3D Model")
                }
                SegmentedButton(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("tab_ai_virtual_tour")
                ) {
                    Text("AI Sanal Tur Videosu")
                }
            }
        }

        if (activeSubTab == 0) {
            item {
                Interactive3DModelSection(
                    selectedModel = selectedModel,
                    onSelectModel = onSelectModel
                )
            }
        } else {
            item {
                AiVirtualTourStudioSection(
                    entitlement = entitlement,
                    tourSourceBitmap = tourSourceBitmap,
                    tourAspectRatio = tourAspectRatio,
                    tourCameraMove = tourCameraMove,
                    virtualTourState = virtualTourState,
                    onSetTourSourceBitmap = onSetTourSourceBitmap,
                    onSetTourAspectRatio = onSetTourAspectRatio,
                    onSetTourCameraMove = onSetTourCameraMove,
                    onGenerateAiTour = onGenerateAiTour,
                    onOpenPaywall = onOpenPaywall
                )
            }
        }
    }
}

@Composable
private fun Interactive3DModelSection(
    selectedModel: Model3DPreset,
    onSelectModel: (Model3DPreset) -> Unit
) {
    var yawDeg by remember { mutableFloatStateOf(38f) }
    var pitchDeg by remember { mutableFloatStateOf(24f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var sunAngleDeg by remember { mutableFloatStateOf(42f) }
    var isAutoTourRunning by remember { mutableStateOf(false) }
    var activeWaypointIndex by remember { mutableIntStateOf(0) }

    var showStructure by remember { mutableStateOf(true) }
    var showGlass by remember { mutableStateOf(true) }
    var showRoof by remember { mutableStateOf(true) }
    var showLandscape by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }

    LaunchedEffect(isAutoTourRunning) {
        if (isAutoTourRunning) {
            while (isAutoTourRunning) {
                yawDeg = (yawDeg + 0.9f) % 360f
                delay(16L)
            }
        }
    }

    val boxes = remember(selectedModel) { buildArchitectureGeometry(selectedModel) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 3D Viewport Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A0F15),
                                    Color(0xFF15202B),
                                    Color(0xFF0F161E)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                isAutoTourRunning = false
                                yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                                pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(6f, 76f)
                            }
                        }
                        .testTag("interactive_3d_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w * 0.5f
                        val cy = h * 0.54f
                        val scale = (minOf(w, h) * 0.135f) * zoom

                        val yaw = (yawDeg * PI / 180.0).toFloat()
                        val pitch = (pitchDeg * PI / 180.0).toFloat()
                        val cosY = cos(yaw)
                        val sinY = sin(yaw)
                        val cosP = cos(pitch)
                        val sinP = sin(pitch)

                        fun toCamera(v: Vec3): Vec3 {
                            val rx = v.x * cosY - v.z * sinY
                            val rz = v.x * sinY + v.z * cosY
                            val ry = v.y * cosP - rz * sinP
                            val fz = v.y * sinP + rz * cosP
                            return Vec3(rx, ry, fz)
                        }

                        fun project(cam: Vec3): Offset {
                            val dist = 12f
                            val persp = dist / (dist + cam.z * 0.45f).coerceAtLeast(2.5f)
                            return Offset(
                                x = cx + cam.x * scale * persp,
                                y = cy - cam.y * scale * persp
                            )
                        }

                        // 1. Blueprint Coordinate Ground Grid
                        if (showGrid) {
                            val gridColor = Color(0x336C859F)
                            for (i in -5..5) {
                                val p1 = project(toCamera(Vec3(i.toFloat(), 0f, -5f)))
                                val p2 = project(toCamera(Vec3(i.toFloat(), 0f, 5f)))
                                drawLine(gridColor, p1, p2, strokeWidth = 1f)

                                val q1 = project(toCamera(Vec3(-5f, 0f, i.toFloat())))
                                val q2 = project(toCamera(Vec3(5f, 0f, i.toFloat())))
                                drawLine(gridColor, q1, q2, strokeWidth = 1f)
                            }
                        }

                        // Directional sunlight vector controlled by user slider
                        val sunRad = (sunAngleDeg * PI / 180.0).toFloat()
                        val sunVec = normalize3(Vec3(cos(sunRad) * 0.85f, 0.72f, sin(sunRad) * 0.85f))

                        // Collect all visible faces from active architectural layers
                        val activeBoxes = boxes.filter { b ->
                            when (b.layer) {
                                "STRUCTURE" -> showStructure
                                "GLASS" -> showGlass
                                "ROOF" -> showRoof
                                "LANDSCAPE" -> showLandscape
                                else -> true
                            }
                        }

                        data class RenderFace(
                            val pts: List<Offset>,
                            val avgZ: Float,
                            val fillColor: Color,
                            val strokeColor: Color
                        )

                        val facesToDraw = ArrayList<RenderFace>(activeBoxes.size * 6)

                        val faceIndices = arrayOf(
                            intArrayOf(4, 5, 6, 7), // top (+y)
                            intArrayOf(0, 3, 2, 1), // bottom (-y)
                            intArrayOf(1, 2, 6, 5), // right (+x)
                            intArrayOf(0, 4, 7, 3), // left (-x)
                            intArrayOf(3, 7, 6, 2), // front (+z)
                            intArrayOf(0, 1, 5, 4)  // back (-z)
                        )
                        val faceNormals = arrayOf(
                            Vec3(0f, 1f, 0f),
                            Vec3(0f, -1f, 0f),
                            Vec3(1f, 0f, 0f),
                            Vec3(-1f, 0f, 0f),
                            Vec3(0f, 0f, 1f),
                            Vec3(0f, 0f, -1f)
                        )

                        for (b in activeBoxes) {
                            val hx = b.sx * 0.5f
                            val hy = b.sy * 0.5f
                            val hz = b.sz * 0.5f
                            val verts = arrayOf(
                                Vec3(b.cx - hx, b.cy - hy, b.cz - hz),
                                Vec3(b.cx + hx, b.cy - hy, b.cz - hz),
                                Vec3(b.cx + hx, b.cy - hy, b.cz + hz),
                                Vec3(b.cx - hx, b.cy - hy, b.cz + hz),
                                Vec3(b.cx - hx, b.cy + hy, b.cz - hz),
                                Vec3(b.cx + hx, b.cy + hy, b.cz - hz),
                                Vec3(b.cx + hx, b.cy + hy, b.cz + hz),
                                Vec3(b.cx - hx, b.cy + hy, b.cz + hz)
                            )
                            val camVerts = verts.map { toCamera(it) }
                            val screenVerts = camVerts.map { project(it) }

                            for (fi in 0 until 6) {
                                val idx = faceIndices[fi]
                                val normal = faceNormals[fi]
                                val diffuse = max(0f, dot3(normal, sunVec))
                                val lightFactor = (0.48f + diffuse * 0.56f).coerceIn(0.35f, 1.15f)
                                val shaded = Color(
                                    red = (b.baseColor.red * lightFactor).coerceIn(0f, 1f),
                                    green = (b.baseColor.green * lightFactor).coerceIn(0f, 1f),
                                    blue = (b.baseColor.blue * lightFactor).coerceIn(0f, 1f),
                                    alpha = b.alpha
                                )
                                val avgZ = (camVerts[idx[0]].z + camVerts[idx[1]].z + camVerts[idx[2]].z + camVerts[idx[3]].z) * 0.25f
                                facesToDraw.add(
                                    RenderFace(
                                        pts = listOf(
                                            screenVerts[idx[0]],
                                            screenVerts[idx[1]],
                                            screenVerts[idx[2]],
                                            screenVerts[idx[3]]
                                        ),
                                        avgZ = avgZ,
                                        fillColor = shaded,
                                        strokeColor = Color(0x550D1218)
                                    )
                                )
                            }
                        }

                        // Painter's algorithm: farthest first (largest camera Z first)
                        facesToDraw.sortByDescending { it.avgZ }
                        for (face in facesToDraw) {
                            val path = Path().apply {
                                moveTo(face.pts[0].x, face.pts[0].y)
                                lineTo(face.pts[1].x, face.pts[1].y)
                                lineTo(face.pts[2].x, face.pts[2].y)
                                lineTo(face.pts[3].x, face.pts[3].y)
                                close()
                            }
                            drawPath(path = path, color = face.fillColor)
                            drawPath(path = path, color = face.strokeColor, style = Stroke(width = 1.2f))
                        }
                    }

                    // Top-Left 3D Telemetry HUD
                    Surface(
                        color = Color(0xCC0E1217),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "YAW ${yawDeg.toInt()}° • PITCH ${pitchDeg.toInt()}° • ${(zoom * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Top-Right Orbit Play/Reset buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { isAutoTourRunning = !isAutoTourRunning },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC151B22))
                                .testTag("toggle_3d_orbit_button")
                        ) {
                            Icon(
                                imageVector = if (isAutoTourRunning) Icons.Rounded.Pause else Icons.Rounded.Autorenew,
                                contentDescription = "Otomatik 360 derece döndürme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                isAutoTourRunning = false
                                yawDeg = 38f
                                pitchDeg = 24f
                                zoom = 1.0f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC151B22))
                                .testTag("reset_3d_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = "Kamerayı sıfırla",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Bottom interaction hint
                    Surface(
                        color = Color(0xB80E1217),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "360° döndürmek için parmağınızla sürükleyin",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6DCE5),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3D Walkthrough Waypoint Bar
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sanal Gezinti Kamera Noktaları",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WalkWaypoints3D.indices.toList()) { idx ->
                            val wp = WalkWaypoints3D[idx]
                            FilterChip(
                                selected = activeWaypointIndex == idx,
                                onClick = {
                                    isAutoTourRunning = false
                                    activeWaypointIndex = idx
                                    yawDeg = wp.yawDeg
                                    pitchDeg = wp.pitchDeg
                                    zoom = wp.zoom
                                },
                                label = { Text(wp.labelTr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3D Model Selector
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ViewInAr, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3D Mimari Tipoloji", style = MaterialTheme.typography.titleMedium)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Model3DPreset.entries) { preset ->
                        FilterChip(
                            selected = selectedModel == preset,
                            onClick = { onSelectModel(preset) },
                            label = { Text(preset.labelTr) },
                            modifier = Modifier.testTag("model_3d_${preset.name}")
                        )
                    }
                }
                Text(
                    text = selectedModel.subtitleTr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Zoom & Sun Light Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kamera Yakınlaştırma", style = MaterialTheme.typography.bodySmall)
                    Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 0.65f..1.85f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Güneş Azimut & Gölge Açısı", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${sunAngleDeg.toInt()}°", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                }
                Slider(
                    value = sunAngleDeg,
                    onValueChange = { sunAngleDeg = it },
                    valueRange = 0f..360f
                )

                // Architectural Layer Toggles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mimari Katmanlar", style = MaterialTheme.typography.titleSmall)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = showStructure, onClick = { showStructure = !showStructure }, label = { Text("Kaba Yapı") }) }
                    item { FilterChip(selected = showGlass, onClick = { showGlass = !showGlass }, label = { Text("Cam Cephe") }) }
                    item { FilterChip(selected = showRoof, onClick = { showRoof = !showRoof }, label = { Text("Çatı & Pergola") }) }
                    item { FilterChip(selected = showLandscape, onClick = { showLandscape = !showLandscape }, label = { Text("Havuz & Peyzaj") }) }
                    item { FilterChip(selected = showGrid, onClick = { showGrid = !showGrid }, label = { Text("Plan Izgarası") }) }
                }
            }
        }
    }
}

@Composable
private fun AiVirtualTourStudioSection(
    entitlement: ProEntitlementState,
    tourSourceBitmap: Bitmap?,
    tourAspectRatio: VirtualTourAspectRatio,
    tourCameraMove: VirtualTourCameraMove,
    virtualTourState: VirtualTourUiState,
    onSetTourSourceBitmap: (Bitmap?) -> Unit,
    onSetTourAspectRatio: (VirtualTourAspectRatio) -> Unit,
    onSetTourCameraMove: (VirtualTourCameraMove) -> Unit,
    onGenerateAiTour: () -> Unit,
    onOpenPaywall: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapUtils.decodeScaledBitmap(context, uri, 1280)
                onSetTourSourceBitmap(bmp)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Virtual Tour Video Player / Source Preview
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            when (virtualTourState) {
                is VirtualTourUiState.Ready -> {
                    AnimatedVirtualTourPlayer(
                        state = virtualTourState
                    )
                }
                is VirtualTourUiState.Generating -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = virtualTourState.progressText,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Çoklu kamera açıları ve mimar anlatımı sentezleniyor...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is VirtualTourUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = virtualTourState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is VirtualTourUiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(tourAspectRatio.ratioValue.coerceIn(0.62f, 1.85f))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tourSourceBitmap != null) {
                            Image(
                                bitmap = tourSourceBitmap.asImageBitmap(),
                                contentDescription = "Sanal tur kaynak fotoğrafı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xCC0E1217),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "SANAL TUR KAYNAĞI HAZIR (${tourAspectRatio.labelTr})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MovieCreation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Fotoğraftan AI Sanal Tur Oluşturun",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Bir oda/cephe fotoğrafı seçin veya Render sekmesinden aktarın; Gemini çoklu kamera açıları ve mimar anlatımıyla sinematik sanal tur üretsin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Virtual Tour Controls Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sanal Tur Ayarları", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("tour_pick_photo_button")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (tourSourceBitmap == null) "Fotoğraf Yükle" else "Değiştir")
                    }
                }

                Text("Video En-Boy Oranı", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VirtualTourAspectRatio.entries.forEach { ratio ->
                        FilterChip(
                            selected = tourAspectRatio == ratio,
                            onClick = { onSetTourAspectRatio(ratio) },
                            label = { Text(ratio.labelTr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("tour_ratio_${ratio.name}")
                        )
                    }
                }

                Text("Sinematik Kamera Hareketi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VirtualTourCameraMove.entries) { move ->
                        FilterChip(
                            selected = tourCameraMove == move,
                            onClick = { onSetTourCameraMove(move) },
                            label = { Text(move.labelTr) }
                        )
                    }
                }

                val isGenerating = virtualTourState is VirtualTourUiState.Generating
                Button(
                    onClick = {
                        if (!entitlement.isPro) {
                            onOpenPaywall(
                                "AI Sanal Tur Videosu üretimi Pro moduna özeldir (${entitlement.formattedPrice}). İnteraktif 3D mimari modeli ücretsiz olarak incelemeye devam edebilirsiniz."
                            )
                        } else {
                            onGenerateAiTour()
                        }
                    },
                    enabled = !isGenerating,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_virtual_tour_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sanal Tur Hazırlanıyor...")
                    } else if (!entitlement.isPro) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Sanal Tur Videosu Oluştur (PRO)", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Sanal Tur Videosu Oluştur", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedVirtualTourPlayer(state: VirtualTourUiState.Ready) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    val frameCount = state.frames.size.coerceAtLeast(1)
    LaunchedEffect(isPlaying, frameCount) {
        while (isPlaying) {
            delay(32L)
            progress = (progress + 0.0045f) % 1f
        }
    }

    val scaledIdx = (progress * frameCount).toInt().coerceIn(0, frameCount - 1)
    val localFrac = (progress * frameCount) - scaledIdx
    val currentFrame = state.frames[scaledIdx]

    val animatedZoom by animateFloatAsState(
        targetValue = 1.0f + localFrac * 0.14f,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "tour_ken_burns_zoom"
    )
    val panOffset by animateFloatAsState(
        targetValue = (localFrac - 0.5f) * 36f * if (scaledIdx % 2 == 0) 1f else -1f,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "tour_ken_burns_pan"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.aspectRatio.ratioValue.coerceIn(0.62f, 1.85f))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.Black)
        ) {
            Image(
                bitmap = currentFrame.bitmap.asImageBitmap(),
                contentDescription = currentFrame.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedZoom
                        scaleY = animatedZoom
                        translationX = panOffset
                    }
            )

            // Cinematic bottom vignette & architect narration subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE60B0F14))
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "SAHNE ${scaledIdx + 1}/$frameCount • ${currentFrame.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentFrame.narrationTr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Surface(
                color = Color(0xCC0E1217),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "${state.aspectRatio.labelTr} • ${state.cameraMove.labelTr}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Playback Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("tour_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = progress,
                onValueChange = {
                    isPlaying = false
                    progress = it
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun dot3(a: Vec3, b: Vec3): Float = a.x * b.x + a.y * b.y + a.z * b.z

private fun normalize3(v: Vec3): Vec3 {
    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z).coerceAtLeast(0.0001f)
    return Vec3(v.x / len, v.y / len, v.z / len)
}

private fun buildArchitectureGeometry(preset: Model3DPreset): List<ArchBox3D> {
    val travertine = Color(0xFFD9CDB8)
    val warmConcrete = Color(0xFF8F969E)
    val cedar = Color(0xFFA86A3D)
    val charcoalSteel = Color(0xFF2B3440)
    val glassBlue = Color(0xFF66B6C9)
    val poolTeal = Color(0xFF29889B)
    val lawnGreen = Color(0xFF3E6B53)
    val deckStone = Color(0xFFB5A996)

    return when (preset) {
        Model3DPreset.VILLA -> listOf(
            // Ground plinth & infinity pool
            ArchBox3D(0f, 0.1f, 0f, 7.6f, 0.2f, 6.2f, deckStone, "LANDSCAPE"),
            ArchBox3D(0.4f, 0.14f, 2.0f, 4.4f, 0.18f, 1.8f, poolTeal, "LANDSCAPE", 0.92f),
            ArchBox3D(-2.7f, 0.22f, 2.0f, 1.6f, 0.26f, 1.7f, lawnGreen, "LANDSCAPE"),
            // Ground floor main mass
            ArchBox3D(-0.8f, 1.0f, -0.8f, 4.8f, 1.6f, 3.0f, travertine, "STRUCTURE"),
            ArchBox3D(2.0f, 1.0f, -1.2f, 1.6f, 1.6f, 2.2f, warmConcrete, "STRUCTURE"),
            // Floor-to-ceiling glass salon front
            ArchBox3D(-0.8f, 1.0f, 0.72f, 3.8f, 1.45f, 0.12f, glassBlue, "GLASS", 0.78f),
            // Cantilevered upper floor
            ArchBox3D(-1.3f, 2.55f, -0.5f, 4.2f, 1.4f, 3.4f, travertine, "STRUCTURE"),
            ArchBox3D(-1.3f, 2.55f, 1.22f, 3.5f, 1.15f, 0.12f, glassBlue, "GLASS", 0.78f),
            // Cedar soffit & cantilever roof fascia
            ArchBox3D(-1.1f, 3.35f, -0.3f, 5.2f, 0.22f, 4.1f, charcoalSteel, "ROOF"),
            ArchBox3D(-1.1f, 3.22f, -0.3f, 4.9f, 0.10f, 3.8f, cedar, "ROOF"),
            // Terrace pergola ribs
            ArchBox3D(1.8f, 2.25f, 0.2f, 2.0f, 0.12f, 2.0f, cedar, "ROOF")
        )
        Model3DPreset.LOFT -> listOf(
            ArchBox3D(0f, 0.1f, 0f, 6.4f, 0.2f, 5.2f, warmConcrete, "LANDSCAPE"),
            ArchBox3D(-1.1f, 1.6f, -0.4f, 3.8f, 3.0f, 3.6f, warmConcrete, "STRUCTURE"),
            ArchBox3D(1.6f, 1.1f, -0.2f, 2.0f, 2.0f, 3.2f, travertine, "STRUCTURE"),
            // Double-height curtain wall glass
            ArchBox3D(-1.1f, 1.6f, 1.45f, 3.3f, 2.65f, 0.14f, glassBlue, "GLASS", 0.8f),
            ArchBox3D(1.6f, 1.1f, 1.45f, 1.6f, 1.65f, 0.14f, glassBlue, "GLASS", 0.8f),
            // Steel roof & skylight frame
            ArchBox3D(-1.1f, 3.2f, -0.4f, 4.2f, 0.24f, 4.0f, charcoalSteel, "ROOF"),
            ArchBox3D(1.6f, 2.2f, -0.2f, 2.3f, 0.18f, 3.5f, cedar, "ROOF")
        )
        Model3DPreset.COURTYARD -> listOf(
            // Courtyard base & garden
            ArchBox3D(0f, 0.1f, 0f, 7.2f, 0.2f, 6.4f, deckStone, "LANDSCAPE"),
            ArchBox3D(0f, 0.18f, 0.6f, 3.0f, 0.18f, 2.8f, lawnGreen, "LANDSCAPE"),
            // U-shape stone wings
            ArchBox3D(0f, 1.1f, -1.7f, 6.2f, 1.8f, 1.8f, travertine, "STRUCTURE"),
            ArchBox3D(-2.2f, 1.0f, 0.6f, 1.8f, 1.6f, 3.2f, travertine, "STRUCTURE"),
            ArchBox3D(2.2f, 1.0f, 0.6f, 1.8f, 1.6f, 3.2f, travertine, "STRUCTURE"),
            // Courtyard glass openings
            ArchBox3D(0f, 1.05f, -0.78f, 3.4f, 1.4f, 0.12f, glassBlue, "GLASS", 0.78f),
            // Timber slatted pergola over courtyard
            ArchBox3D(0f, 2.15f, -1.7f, 6.6f, 0.22f, 2.1f, charcoalSteel, "ROOF"),
            ArchBox3D(0f, 2.0f, 0.5f, 2.8f, 0.14f, 2.4f, cedar, "ROOF")
        )
        Model3DPreset.DUPLEX -> listOf(
            ArchBox3D(0f, 0.1f, 0f, 6.8f, 0.2f, 5.6f, deckStone, "LANDSCAPE"),
            ArchBox3D(1.8f, 0.16f, 1.6f, 2.6f, 0.16f, 1.6f, poolTeal, "LANDSCAPE", 0.9f),
            // Stepped 3-tier modernist volumes
            ArchBox3D(-0.5f, 0.9f, -0.5f, 5.0f, 1.4f, 3.4f, warmConcrete, "STRUCTURE"),
            ArchBox3D(-0.9f, 2.25f, -0.7f, 3.8f, 1.3f, 2.9f, travertine, "STRUCTURE"),
            ArchBox3D(-1.3f, 3.45f, -0.9f, 2.6f, 1.1f, 2.2f, cedar, "STRUCTURE"),
            ArchBox3D(-0.5f, 0.9f, 1.24f, 4.2f, 1.15f, 0.12f, glassBlue, "GLASS", 0.8f),
            ArchBox3D(-0.9f, 2.25f, 0.78f, 3.2f, 1.05f, 0.12f, glassBlue, "GLASS", 0.8f),
            ArchBox3D(-1.3f, 4.1f, -0.9f, 3.0f, 0.18f, 2.6f, charcoalSteel, "ROOF")
        )
    }
}
