package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.core.ProEntitlementState

private data class PlanFeatureRow(
    val icon: ImageVector,
    val title: String,
    val freeLabel: String,
    val proLabel: String
)

private val PlanComparisonItems = listOf(
    PlanFeatureRow(
        icon = Icons.Rounded.Block,
        title = "Reklamsız Stüdyo Deneyimi",
        freeLabel = "Banner & Geçiş Reklamlı",
        proLabel = "%100 Reklamsız"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.HighQuality,
        title = "Render Çözünürlüğü",
        freeLabel = "Yalnızca 1K",
        proLabel = "1K, 2K & 4K Stüdyo Pro"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.AutoAwesome,
        title = "Günlük AI Render & Düzenleme",
        freeLabel = "Günde 3 Hak",
        proLabel = "Sınırsız"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.CloudDone,
        title = "Bulut Tasarım Projeleri",
        freeLabel = "En Fazla 2 Proje",
        proLabel = "Sınırsız Bulut Projesi"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.Psychology,
        title = "AI Mimar & Derin Düşünme",
        freeLabel = "Günde 10 Mesaj (Standart)",
        proLabel = "Sınırsız + High Thinking"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.MovieCreation,
        title = "AI Sanal Tur Videosu",
        freeLabel = "Kilitli (Yalnızca 3D Model)",
        proLabel = "Sınırsız 16:9 & 9:16 Tur"
    ),
    PlanFeatureRow(
        icon = Icons.Rounded.Inventory2,
        title = "AI Metraj & Malzeme Listesi",
        freeLabel = "Yalnızca Manuel Giriş",
        proLabel = "Otomatik AI Keşif + Paylaşım"
    )
)

/**
 * Full-featured Material 3 Paywall & Pro Membership dialog for MimarStüdyo Pro (99,99 TL).
 */
@Composable
fun ProPaywallDialog(
    entitlement: ProEntitlementState,
    lockedFeatureReason: String?,
    onDismiss: () -> Unit,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDebugTogglePro: (Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("pro_paywall_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .testTag("close_paywall_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Pencereyi kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 44.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (entitlement.isPro) "MİMARSTÜDYO PRO AKTİF" else "MİMARSTÜDYO PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Text(
                            text = if (entitlement.isPro) {
                                "Pro Üyeliğiniz Aktif"
                            } else {
                                "Reklamsız ve Sınırsız Mimari Stüdyo"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (entitlement.isPro) {
                                "4K render motoru, AI Sanal Tur, Derin Düşünme ve tüm bulut özellikleri reklamsız olarak açık."
                            } else {
                                "Tek seferlik ödeme ile reklamları tamamen kaldırın, 2K/4K render ve tüm profesyonel AI araçlarının kilidini açın."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contextual lock reason banner if opened from a restricted feature
                    if (!entitlement.isPro && !lockedFeatureReason.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = lockedFeatureReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Price Highlight Card
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "PRO MOD • ÖMÜR BOYU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tek seferlik Google Play ödemesi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = entitlement.formattedPrice,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Normal vs Pro Comparison List
                    Text(
                        text = "Normal Mod ve Pro Mod Karşılaştırması",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PlanComparisonItems.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Normal: ${item.freeLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "Pro: ${item.proLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (entitlement.isPro) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (idx < PlanComparisonItems.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    // Billing Status Feedback
                    if (!entitlement.statusMessage.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entitlement.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Primary Purchase CTA or Active Status
                    if (!entitlement.isPro) {
                        Button(
                            onClick = onPurchaseClick,
                            enabled = !entitlement.isProcessingPurchase,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("purchase_pro_button")
                        ) {
                            if (entitlement.isProcessingPurchase) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Google Play Bağlanıyor...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pro'ya Geç — ${entitlement.formattedPrice}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pro Stüdyoya Devam Et")
                        }
                    }

                    // Restore Purchases
                    TextButton(
                        onClick = onRestoreClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_purchase_button")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Satın Alımları Geri Yükle (Google Play)")
                    }

                    // Debug QA switcher when Play Store SKU is not published yet (only compiled in DEBUG builds)
                    if (BuildConfig.DEBUG && !entitlement.isStoreProductAvailable) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Science,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Önizleme Test Modu (Debug)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Play Console ürünü yayınlanmadan Pro/Normal geçişini test edin.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = entitlement.isPro,
                                    onCheckedChange = onDebugTogglePro,
                                    modifier = Modifier.testTag("debug_pro_toggle_switch")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
