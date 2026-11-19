package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ProEntitlementState
import com.example.data.model.DesignProject
import com.example.data.model.Model3DPreset
import com.example.data.model.ProjectPhase
import com.example.data.model.ShowcaseTemplate
import com.example.data.model.ShowcaseTemplates
import com.example.data.model.SpaceType
import com.example.ui.components.ConfirmDeleteProjectDialog
import com.example.ui.components.CreateArchitecturalProjectDialog
import com.example.ui.components.DashboardMetricsBanner
import com.example.ui.components.DashboardProjectCard
import com.example.ui.components.DashboardShowcaseCard
import com.example.ui.components.EmptyDashboardStateCard
import com.example.ui.viewmodel.CloudUiState
import java.text.NumberFormat
import java.util.Locale

private enum class DashboardStatusFilter(val labelTr: String) {
    ACTIVE("Aktif Projeler"),
    DELIVERED("Teslim Edilen"),
    ALL("Tümü")
}

@Composable
fun ProjectDashboardScreen(
    userName: String,
    entitlement: ProEntitlementState,
    projectsState: CloudUiState<List<DesignProject>>,
    selectedProjectId: String?,
    onSelectProject: (String) -> Unit,
    onCreateProject: (String, SpaceType, String, String, Double, String, Model3DPreset) -> Unit,
    onUpdatePhase: (String, ProjectPhase) -> Unit,
    onDeleteProject: (String) -> Unit,
    onApplyTemplateToRender: (ShowcaseTemplate) -> Unit,
    onOpenRenderForProject: (DesignProject) -> Unit,
    onOpen3DForProject: (DesignProject) -> Unit,
    onOpenMaterialsForProject: (DesignProject) -> Unit,
    onOpenPaywall: (String?) -> Unit,
    onSignOut: () -> Unit
) {
    var statusFilter by rememberSaveable { mutableStateOf(DashboardStatusFilter.ACTIVE) }
    var selectedSpaceFilter by rememberSaveable { mutableStateOf<SpaceType?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var projectPendingDelete by remember { mutableStateOf<DesignProject?>(null) }

    val listState = rememberLazyListState()
    val expandFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val allProjects = (projectsState as? CloudUiState.Success)?.data.orEmpty()
    val activeProjects = remember(allProjects) {
        allProjects.filter { ProjectPhase.fromRaw(it.phase) != ProjectPhase.TESLIM }
    }
    val deliveredProjects = remember(allProjects) {
        allProjects.filter { ProjectPhase.fromRaw(it.phase) == ProjectPhase.TESLIM }
    }

    val totalActiveArea = remember(activeProjects) { activeProjects.sumOf { it.areaSqm } }
    val totalActiveBudget = remember(activeProjects) { activeProjects.sumOf { it.estimatedTotalTry } }

    val tryFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("tr", "TR")).apply {
            maximumFractionDigits = 0
        }
    }

    val canCreateProject = entitlement.canCreateProject(allProjects.size)
    val openCreateOrPaywall: () -> Unit = {
        if (canCreateProject) {
            showCreateDialog = true
        } else {
            onOpenPaywall(
                "Normal modda en fazla ${entitlement.maxFreeProjects} bulut projesi oluşturabilirsiniz. Sınırsız proje ve reklamsız deneyim için Pro'ya geçin (${entitlement.formattedPrice})."
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = openCreateOrPaywall,
                expanded = expandFab,
                icon = {
                    Icon(
                        imageVector = if (canCreateProject) Icons.Rounded.Add else Icons.Rounded.Lock,
                        contentDescription = "Yeni proje ekle"
                    )
                },
                text = {
                    Text(if (canCreateProject) "Yeni Proje" else "Pro ile Ekle")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_project_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("project_dashboard_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Atelier Header Bar
            item(key = "dashboard_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = "Firestore bulut senkronizasyonu aktif",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PROJE PANOSU • $userName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MimarStüdyo",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (entitlement.isPro) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                if (entitlement.isPro) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier
                                .clickable { onOpenPaywall(null) }
                                .testTag("header_pro_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WorkspacePremium,
                                    contentDescription = if (entitlement.isPro) "Pro Mod Aktif" else "Pro'ya Geç",
                                    tint = if (entitlement.isPro) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (entitlement.isPro) "PRO" else "PRO • ${entitlement.formattedPrice}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entitlement.isPro) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Çıkış Yap",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Live Firestore Portfolio KPI Banner
            item(key = "dashboard_metrics") {
                DashboardMetricsBanner(
                    activeCount = activeProjects.size,
                    deliveredCount = deliveredProjects.size,
                    totalActiveAreaSqm = totalActiveArea,
                    formattedActiveBudget = tryFormat.format(totalActiveBudget),
                    totalProjectsCount = allProjects.size,
                    entitlement = entitlement,
                    onUpgradeClick = { onOpenPaywall(null) }
                )
            }

            // Status Switcher, Search Bar & Space Filter Chips
            item(key = "dashboard_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardStatusFilter.entries.forEach { tab ->
                            val count = when (tab) {
                                DashboardStatusFilter.ACTIVE -> activeProjects.size
                                DashboardStatusFilter.DELIVERED -> deliveredProjects.size
                                DashboardStatusFilter.ALL -> allProjects.size
                            }
                            val selected = statusFilter == tab
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { statusFilter = tab }
                                    .testTag("status_filter_${tab.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${tab.labelTr} ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Proje adı, stil veya mekan ara...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Ara",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Aramayı temizle"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSpaceFilter == null,
                                onClick = { selectedSpaceFilter = null },
                                label = { Text("Tüm Mekanlar") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("space_filter_all")
                            )
                        }
                        items(SpaceType.entries) { space ->
                            FilterChip(
                                selected = selectedSpaceFilter == space,
                                onClick = { selectedSpaceFilter = space },
                                label = { Text(space.labelTr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("space_filter_${space.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Active Architectural Projects List from Firestore
            when (projectsState) {
                is CloudUiState.Loading -> {
                    item(key = "dashboard_loading") {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Firestore projeleri yükleniyor...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is CloudUiState.Error -> {
                    item(key = "dashboard_error") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_error_banner")
                        ) {
                            Text(
                                text = projectsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is CloudUiState.Success -> {
                    val baseByStatus = when (statusFilter) {
                        DashboardStatusFilter.ACTIVE -> activeProjects
                        DashboardStatusFilter.DELIVERED -> deliveredProjects
                        DashboardStatusFilter.ALL -> allProjects
                    }
                    val trimmedQuery = searchQuery.trim().lowercase(Locale("tr", "TR"))
                    val filteredProjects = baseByStatus.filter { proj ->
                        val matchesSpace = selectedSpaceFilter == null ||
                            SpaceType.fromRaw(proj.spaceType) == selectedSpaceFilter
                        val matchesQuery = trimmedQuery.isEmpty() ||
                            proj.title.lowercase(Locale("tr", "TR")).contains(trimmedQuery) ||
                            proj.style.lowercase(Locale("tr", "TR")).contains(trimmedQuery) ||
                            proj.roomCategory.lowercase(Locale("tr", "TR")).contains(trimmedQuery)
                        matchesSpace && matchesQuery
                    }

                    if (filteredProjects.isEmpty()) {
                        item(key = "dashboard_empty") {
                            val hasActiveFilters = selectedSpaceFilter != null ||
                                trimmedQuery.isNotEmpty() ||
                                (statusFilter != DashboardStatusFilter.ALL && allProjects.isNotEmpty())
                            EmptyDashboardStateCard(
                                isFilteredEmpty = hasActiveFilters,
                                title = when {
                                    allProjects.isEmpty() -> "Henüz Mimari Projeniz Yok"
                                    statusFilter == DashboardStatusFilter.ACTIVE && activeProjects.isEmpty() ->
                                        "Aktif Proje Bulunmuyor"
                                    statusFilter == DashboardStatusFilter.DELIVERED && deliveredProjects.isEmpty() ->
                                        "Henüz Teslim Edilen Proje Yok"
                                    else -> "Filtreyle Eşleşen Proje Yok"
                                },
                                subtitle = when {
                                    allProjects.isEmpty() ->
                                        "Sağ alttaki + Yeni Proje düğmesine dokunarak ilk iç/dış mekan projenizi Firestore bulutuna ekleyin."
                                    hasActiveFilters ->
                                        "Farklı bir durum sekmesi seçin, arama filtresini temizleyin veya yeni bir mimari proje başlatın."
                                    else ->
                                        "Yeni bir mimari proje ekleyerek bulut senkronizasyonunu başlatın."
                                },
                                onCreateClick = openCreateOrPaywall,
                                onResetFilters = {
                                    statusFilter = DashboardStatusFilter.ALL
                                    selectedSpaceFilter = null
                                    searchQuery = ""
                                }
                            )
                        }
                    } else {
                        items(filteredProjects, key = { it.id }) { project ->
                            DashboardProjectCard(
                                project = project,
                                isSelected = project.id == selectedProjectId,
                                formattedBudget = tryFormat.format(project.estimatedTotalTry),
                                onClick = { onSelectProject(project.id) },
                                onOpenRender = { onOpenRenderForProject(project) },
                                onOpen3D = { onOpen3DForProject(project) },
                                onOpenMaterials = { onOpenMaterialsForProject(project) },
                                onAdvancePhase = {
                                    val next = when (ProjectPhase.fromRaw(project.phase)) {
                                        ProjectPhase.KONSEPT -> ProjectPhase.UYGULAMA
                                        ProjectPhase.UYGULAMA -> ProjectPhase.TESLIM
                                        ProjectPhase.TESLIM -> null
                                    }
                                    if (next != null) onUpdatePhase(project.id, next)
                                },
                                onRequestDelete = { projectPendingDelete = project }
                            )
                        }
                    }
                }
            }

            // Curated Architectural Inspiration Carousel
            item(key = "dashboard_showcase") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "İlham & Hızlı Başlangıç",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "İÇ & DIŞ MEKAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(ShowcaseTemplates, key = { it.id }) { tpl ->
                            DashboardShowcaseCard(
                                template = tpl,
                                onUseStyle = { onApplyTemplateToRender(tpl) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateArchitecturalProjectDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, space, style, room, area, brief, model ->
                onCreateProject(title, space, style, room, area, brief, model)
                statusFilter = DashboardStatusFilter.ACTIVE
                showCreateDialog = false
            }
        )
    }

    val pendingDelete = projectPendingDelete
    if (pendingDelete != null) {
        ConfirmDeleteProjectDialog(
            projectTitle = pendingDelete.title,
            onDismiss = { projectPendingDelete = null },
            onConfirmDelete = {
                onDeleteProject(pendingDelete.id)
                projectPendingDelete = null
            }
        )
    }
}
