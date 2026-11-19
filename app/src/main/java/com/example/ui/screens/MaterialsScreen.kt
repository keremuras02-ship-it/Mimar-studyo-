package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.BitmapUtils
import com.example.core.ProEntitlementState
import com.example.data.model.DesignProject
import com.example.data.model.DraftMaterialInput
import com.example.data.model.MaterialCategory
import com.example.data.model.MaterialItem
import com.example.ui.viewmodel.CloudUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MaterialsScreen(
    entitlement: ProEntitlementState,
    projects: List<DesignProject>,
    selectedProjectId: String?,
    materialsState: CloudUiState<List<MaterialItem>>,
    isGeneratingBom: Boolean,
    onSelectProject: (String) -> Unit,
    onGenerateAiBom: () -> Unit,
    onAddMaterial: (DraftMaterialInput) -> Unit,
    onTogglePurchased: (MaterialItem) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onGoToProjects: () -> Unit,
    onOpenPaywall: (String?) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<MaterialCategory?>(null) }

    val activeProject = projects.firstOrNull { it.id == selectedProjectId }
    val tryFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("tr", "TR")).apply {
            maximumFractionDigits = 0
        }
    }

    val materials = (materialsState as? CloudUiState.Success)?.data.orEmpty()
    val totalBudget = materials.sumOf { it.lineTotalTry }
    val purchasedBudget = materials.filter { it.purchased }.sumOf { it.lineTotalTry }
    val purchasedCount = materials.count { it.purchased }
    val completionRatio = if (materials.isEmpty()) 0f else purchasedCount.toFloat() / materials.size.toFloat()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "METRAJ & KEŞİF LİSTESİ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Malzeme & Maliyet",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (activeProject != null && materials.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (!entitlement.isPro) {
                                    onOpenPaywall(
                                        "Malzeme & metraj raporunu dışa aktarma ve paylaşma Pro moduna özeldir (${entitlement.formattedPrice})."
                                    )
                                } else {
                                    val report = buildBomShareReport(activeProject, materials, tryFormat)
                                    BitmapUtils.shareTextReport(
                                        context = context,
                                        subject = "${activeProject.title} - Malzeme Listesi",
                                        body = report
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("share_bom_button")
                        ) {
                            Icon(
                                imageVector = if (entitlement.isPro) Icons.Rounded.Share else Icons.Rounded.Lock,
                                contentDescription = "Malzeme listesini paylaş",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Text("Önce Bir Bulut Projesi Oluşturun", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Malzeme listeleri projelerinize bağlı olarak bulutta saklanır. Projeler sekmesinden ilk projenizi oluşturun.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onGoToProjects) {
                                Text("Projelere Git")
                            }
                        }
                    }
                }
            } else {
                // Project Switcher Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projects, key = { it.id }) { proj ->
                            FilterChip(
                                selected = proj.id == selectedProjectId,
                                onClick = { onSelectProject(proj.id) },
                                label = { Text("${proj.title} (${proj.areaSqm.toInt()} m²)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Cost Summary & AI Generator Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "TOPLAM TAHMİNİ MALİYET",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = tryFormat.format(totalBudget),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TEMİN EDİLEN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$purchasedCount / ${materials.size} kalem",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { completionRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Text(
                                text = "Tedarik Tutarı: ${tryFormat.format(purchasedBudget)} • Kalan: ${tryFormat.format((totalBudget - purchasedBudget).coerceAtLeast(0.0))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    if (!entitlement.isPro) {
                                        onOpenPaywall(
                                            "AI ile Otomatik Malzeme & Maliyet Listesi oluşturma Pro moduna özeldir (${entitlement.formattedPrice}). Normal modda + butonu ile manuel malzeme ekleyebilirsiniz."
                                        )
                                    } else {
                                        onGenerateAiBom()
                                    }
                                },
                                enabled = !isGeneratingBom && activeProject != null,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("generate_ai_bom_button")
                            ) {
                                if (isGeneratingBom) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("AI Metraj & Malzeme Listesi Hazırlıyor...")
                                } else if (!entitlement.isPro) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Malzeme Listesi Oluştur (PRO)", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI ile Otomatik Malzeme Listesi Oluştur", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Category Filter Bar
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("Tümü (${materials.size})") }
                            )
                        }
                        items(MaterialCategory.entries) { cat ->
                            val count = materials.count { MaterialCategory.fromRaw(it.category) == cat }
                            if (count > 0) {
                                FilterChip(
                                    selected = selectedCategoryFilter == cat,
                                    onClick = { selectedCategoryFilter = cat },
                                    label = { Text("${cat.labelTr} ($count)") }
                                )
                            }
                        }
                    }
                }

                // Materials List
                when (materialsState) {
                    is CloudUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is CloudUiState.Error -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = materialsState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    is CloudUiState.Success -> {
                        val filtered = materials.filter { item ->
                            selectedCategoryFilter == null || MaterialCategory.fromRaw(item.category) == selectedCategoryFilter
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Bu projede henüz malzeme kalemi yok",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Yukarıdaki AI butonuna dokunarak projeye özel metraj ve fiyatlı malzeme listesi oluşturabilir veya + ile manuel kalem ekleyebilirsiniz.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { mat ->
                                MaterialLineItemCard(
                                    item = mat,
                                    formattedUnit = tryFormat.format(mat.unitPriceTry),
                                    formattedTotal = tryFormat.format(mat.lineTotalTry),
                                    onTogglePurchased = { onTogglePurchased(mat) },
                                    onDelete = { onDeleteMaterial(mat.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeProject != null) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_material_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Manuel malzeme ekle")
            }
        }
    }

    if (showAddDialog) {
        AddMaterialDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { draft ->
                onAddMaterial(draft)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MaterialLineItemCard(
    item: MaterialItem,
    formattedUnit: String,
    formattedTotal: String,
    onTogglePurchased: () -> Unit,
    onDelete: () -> Unit
) {
    val category = MaterialCategory.fromRaw(item.category)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.purchased,
                onCheckedChange = { onTogglePurchased() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("material_check_${item.id}")
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = category.labelTr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${formatQty(item.quantity)} ${item.unit} × $formattedUnit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.purchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (item.specification.isNotBlank()) {
                    Text(
                        text = item.specification,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Malzemeyi sil",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onAdd: (DraftMaterialInput) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MaterialCategory.ZEMIN) }
    var spec by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("50") }
    var unit by remember { mutableStateOf("m²") }
    var priceText by remember { mutableStateOf("1200") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Malzeme Kalemi", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Malzeme Adı *") },
                    placeholder = { Text("Örn: Honlu Traverten Plaka") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("material_name_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MaterialCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelTr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = spec,
                    onValueChange = { spec = it },
                    label = { Text("Teknik Özellik") },
                    placeholder = { Text("60x120cm, 2cm kalınlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Miktar") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Birim") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Birim Fiyat (₺)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isNotEmpty()) {
                        onAdd(
                            DraftMaterialInput(
                                name = cleanName,
                                category = category,
                                specification = spec.trim(),
                                quantity = qtyText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0,
                                unit = unit.trim().ifEmpty { "adet" },
                                unitPriceTry = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_material_button")
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

private fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("tr", "TR"), "%.1f", value)
}

private fun buildBomShareReport(
    project: DesignProject,
    items: List<MaterialItem>,
    tryFormat: NumberFormat
): String = buildString {
    appendLine("MİMARSTÜDYO — MALZEME & METRAJ RAPORU")
    appendLine("Proje: ${project.title} (${project.areaSqm.toInt()} m² • ${project.style})")
    appendLine("--------------------------------------------------")
    items.forEachIndexed { idx, item ->
        val status = if (item.purchased) "[✓]" else "[ ]"
        appendLine("${idx + 1}. $status ${item.name} (${MaterialCategory.fromRaw(item.category).labelTr})")
        if (item.specification.isNotBlank()) {
            appendLine("   Özellik: ${item.specification}")
        }
        appendLine("   Miktar: ${formatQty(item.quantity)} ${item.unit} × ${tryFormat.format(item.unitPriceTry)} = ${tryFormat.format(item.lineTotalTry)}")
    }
    appendLine("--------------------------------------------------")
    appendLine("TOPLAM TAHMİNİ BÜTÇE: ${tryFormat.format(items.sumOf { it.lineTotalTry })}")
}
