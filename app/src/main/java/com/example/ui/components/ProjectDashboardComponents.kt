package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.BitmapUtils
import com.example.core.ProEntitlementState
import com.example.data.model.ArchStylePreset
import com.example.data.model.DesignProject
import com.example.data.model.Model3DPreset
import com.example.data.model.ProjectPhase
import com.example.data.model.ShowcaseTemplate
import com.example.data.model.SpaceType
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardMetricsBanner(
    activeCount: Int,
    deliveredCount: Int,
    totalActiveAreaSqm: Double,
    formattedActiveBudget: String,
    totalProjectsCount: Int,
    entitlement: ProEntitlementState,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val accentGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_metrics_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle architectural drafting blueprint grid
                    val step = 28.dp.toPx()
                    var x = 0f
                    while (x <= size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y <= size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accentGlow, Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.12f),
                            radius = size.width * 0.65f
                        )
                    )
                }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "CANLI FIRESTORE PORTFÖYÜ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$deliveredCount teslim",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardKpiTile(
                    label = "AKTİF PROJE",
                    value = "$activeCount",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiTile(
                    label = "AKTİF ALAN",
                    value = "${totalActiveAreaSqm.toInt()} m²",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                DashboardKpiTile(
                    label = "BÜTÇE TOPLAMI",
                    value = formattedActiveBudget,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1.25f)
                )
            }

            if (!entitlement.isPro) {
                val quotaRatio = (totalProjectsCount.toFloat() / entitlement.maxFreeProjects.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Normal Mod Bulut Kotası: $totalProjectsCount / ${entitlement.maxFreeProjects}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (totalProjectsCount >= entitlement.maxFreeProjects) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onUpgradeClick)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sınırsız Pro • ${entitlement.formattedPrice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { quotaRatio },
                        color = if (totalProjectsCount >= entitlement.maxFreeProjects) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashboardProjectCard(
    project: DesignProject,
    isSelected: Boolean,
    formattedBudget: String,
    onClick: () -> Unit,
    onOpenRender: () -> Unit,
    onOpen3D: () -> Unit,
    onOpenMaterials: () -> Unit,
    onAdvancePhase: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val phase = ProjectPhase.fromRaw(project.phase)
    val space = SpaceType.fromRaw(project.spaceType)
    val thumbBitmap = remember(project.thumbnailBase64) {
        BitmapUtils.base64ToBitmap(project.thumbnailBase64)
    }
    val updatedLabel = remember(project.updatedAt) {
        formatFirestoreTimestamp(project.updatedAt)
    }
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "project_card_border"
    )
    val blueprintLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}")
    ) {
        Column {
            if (thumbBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                ) {
                    Image(
                        bitmap = thumbBitmap.asImageBitmap(),
                        contentDescription = "${project.title} render önizlemesi",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x110E1217), Color(0xC80E1217))
                                )
                            )
                    )
                    PhaseBadgePill(
                        phase = phase,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${space.labelTr.uppercase()} • ${project.style}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Architectural blueprint header band when no render thumbnail exists yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .drawBehind {
                            val step = 18.dp.toPx()
                            var x = 0f
                            while (x <= size.width) {
                                drawLine(
                                    color = blueprintLineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f
                                )
                                x += step
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Architecture,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${space.labelTr.uppercase()} • ${project.style}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PhaseBadgePill(phase = phase)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = project.roomCategory,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (updatedLabel != null) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = updatedLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (project.promptSummary.isNotBlank()) {
                    Text(
                        text = project.promptSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3-Stage Architectural Phase Tracker
                PhaseProgressTrack(phase = phase)

                // Technical Telemetry Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ALAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${project.areaSqm.toInt()} m²",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = "TAHMİNİ MALİYET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedBudget,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "3D MODEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Model3DPreset.fromRaw(project.architecturalModelType).name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Quick Workflow Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = onOpenRender,
                                label = { Text("Render") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("open_render_button_${project.id}")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpen3D,
                                label = { Text("3D") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.ViewInAr,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("open_3d_button_${project.id}")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpenMaterials,
                                label = { Text("Malzeme") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("open_materials_button_${project.id}")
                            )
                        }
                    }

                    if (phase != ProjectPhase.TESLIM) {
                        IconButton(
                            onClick = onAdvancePhase,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("advance_phase_button_${project.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Aşamayı ilerlet",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_project_button_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Projeyi sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseBadgePill(
    phase: ProjectPhase,
    modifier: Modifier = Modifier
) {
    val isDelivered = phase == ProjectPhase.TESLIM
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDelivered) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        modifier = modifier
    ) {
        Text(
            text = phase.labelTr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isDelivered) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PhaseProgressTrack(phase: ProjectPhase) {
    val stepIndex = when (phase) {
        ProjectPhase.KONSEPT -> 0
        ProjectPhase.UYGULAMA -> 1
        ProjectPhase.TESLIM -> 2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProjectPhase.entries.forEachIndexed { idx, item ->
            val reached = idx <= stepIndex
            val activeColor = if (phase == ProjectPhase.TESLIM) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (reached) activeColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Text(
                    text = item.labelTr,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == stepIndex) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == stepIndex) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DashboardShowcaseCard(
    template: ShowcaseTemplate,
    onUseStyle: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(268.dp)
            .height(196.dp)
            .clickable(onClick = onUseStyle)
            .testTag("showcase_card_${template.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = template.drawableRes),
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x220E1217), Color(0xEA0E1217))
                        )
                    )
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = template.spaceType.labelTr.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = template.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD6DCE5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Bu Stilde Render Üret →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDashboardStateCard(
    isFilteredEmpty: Boolean,
    title: String,
    subtitle: String,
    onCreateClick: () -> Unit,
    onResetFilters: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isFilteredEmpty) Icons.Rounded.SearchOff else Icons.Rounded.HomeWork,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isFilteredEmpty) {
                    OutlinedButton(
                        onClick = onResetFilters,
                        modifier = Modifier.testTag("reset_dashboard_filters_button")
                    ) {
                        Text("Filtreleri Temizle")
                    }
                }
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier.testTag("empty_create_project_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni Proje Ekle")
                }
            }
        }
    }
}

@Composable
fun CreateArchitecturalProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, SpaceType, String, String, Double, String, Model3DPreset) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var spaceType by remember { mutableStateOf(SpaceType.BOTH) }
    var style by remember { mutableStateOf(ArchStylePreset.MEDITERRANEAN) }
    var roomCategory by remember { mutableStateOf("Villa & Salon") }
    var areaText by remember { mutableStateOf("220") }
    var brief by remember { mutableStateOf("") }
    var model3D by remember { mutableStateOf(Model3DPreset.VILLA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Yeni Mimari Proje Ekle",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Proje Adı *") },
                    placeholder = { Text("Örn: Bodrum Taş Villa") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_title_input")
                )

                Text("Mekan Kapsamı", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpaceType.entries.forEach { sp ->
                        FilterChip(
                            selected = spaceType == sp,
                            onClick = { spaceType = sp },
                            label = { Text(sp.labelTr, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roomCategory,
                        onValueChange = { roomCategory = it },
                        label = { Text("Bölüm / Oda") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = areaText,
                        onValueChange = { areaText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Alan (m²)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Text("Mimari Stil", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArchStylePreset.entries) { preset ->
                        FilterChip(
                            selected = style == preset,
                            onClick = { style = preset },
                            label = { Text(preset.labelTr) }
                        )
                    }
                }

                Text("3D Model Tipi", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Model3DPreset.entries) { preset ->
                        FilterChip(
                            selected = model3D == preset,
                            onClick = { model3D = preset },
                            label = { Text(preset.labelTr) }
                        )
                    }
                }

                OutlinedTextField(
                    value = brief,
                    onValueChange = { brief = it },
                    label = { Text("Kısa Mimari Not") },
                    placeholder = { Text("Geniş cam cepheli, havuzlu, sıcak ahşap detaylı...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val area = areaText.toDoubleOrNull()?.coerceIn(10.0, 50000.0) ?: 160.0
                    onConfirm(
                        title.trim().ifEmpty { "${style.labelTr} Ev Projesi" },
                        spaceType,
                        style.labelTr,
                        roomCategory.trim().ifEmpty { "Genel Mekan" },
                        area,
                        brief.trim(),
                        model3D
                    )
                },
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Buluta Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun ConfirmDeleteProjectDialog(
    projectTitle: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Projeyi Sil", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Text(
                text = "\"$projectTitle\" projesi ve bağlı tüm malzeme kayıtları Firestore bulut deposundan kalıcı olarak silinecek.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_project_button")
            ) {
                Text("Kalıcı Olarak Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

private fun formatFirestoreTimestamp(ts: Timestamp?): String? {
    val date = ts?.toDate() ?: return null
    return SimpleDateFormat("d MMM, HH:mm", Locale("tr", "TR")).format(date)
}
