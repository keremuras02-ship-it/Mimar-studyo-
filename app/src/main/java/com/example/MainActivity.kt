package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AdMobBannerBar
import com.example.core.InterstitialAdController
import com.example.core.ProPlanManager
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.repository.CloudProjectRepository
import com.example.data.repository.GeminiStudioRepository
import com.example.ui.components.ArchitecturalDesignChatComponent
import com.example.ui.components.ProPaywallDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MaterialsScreen
import com.example.ui.screens.ProjectDashboardScreen
import com.example.ui.screens.RenderStudioScreen
import com.example.ui.screens.ThreeDViewerScreen
import com.example.ui.theme.MimarStudyoTheme
import com.example.ui.viewmodel.ArchitecturalChatViewModel
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.RenderUiState
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class StudioTab(
    val route: String,
    val labelTr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PROJECTS("projects", "Projeler", Icons.Rounded.HomeWork, Icons.Outlined.HomeWork),
    RENDER("render", "Render", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    CHAT("chat", "AI Mimar", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    VIEW_3D("three_d", "3D & Tur", Icons.Rounded.ViewInAr, Icons.Outlined.ViewInAr),
    MATERIALS("materials", "Malzeme", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MimarStudyoTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
    } else {
        key(user.uid) {
            AuthenticatedStudioShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Mimar"
            )
        }
    }
}

@Composable
private fun AuthenticatedStudioShell(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Play Billing + Free-tier daily quota manager
    val proPlanManager = remember(currentUserId) { ProPlanManager(context, currentUserId) }
    val interstitialAdController = remember(context) { InterstitialAdController(context) }

    DisposableEffect(proPlanManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                proPlanManager.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            proPlanManager.dispose()
            interstitialAdController.clear()
        }
    }

    val entitlement by proPlanManager.state.collectAsStateWithLifecycle()

    // Keep interstitial preloaded only when user is on Normal (Free) mode
    LaunchedEffect(entitlement.isPro) {
        interstitialAdController.preloadIfFree(entitlement.isPro)
    }

    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var paywallReason by rememberSaveable { mutableStateOf<String?>(null) }
    val openPaywall: (String?) -> Unit = { reason ->
        proPlanManager.clearStatusMessage()
        paywallReason = reason
        showPaywall = true
    }

    // Resolve custom named Firestore database ID at the DI boundary (never parameterless getInstance())
    val cloudRepo = remember(currentUserId) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        CloudProjectRepository(db)
    }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = remember(currentUserId) {
            StudioViewModel.provideFactory(
                cloudRepo = cloudRepo,
                aiRepo = GeminiStudioRepository(),
                currentUserId = currentUserId
            )
        }
    )

    val architecturalChatViewModel: ArchitecturalChatViewModel = viewModel(
        key = "arch_chat_$currentUserId",
        factory = remember(currentUserId) {
            ArchitecturalChatViewModel.provideFactory(
                cloudRepo = cloudRepo,
                currentUserId = currentUserId
            )
        }
    )

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.PROJECTS) }
    var threeDInitialSubTab by rememberSaveable { mutableIntStateOf(0) }

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val materialsState by viewModel.materialsState.collectAsStateWithLifecycle()
    val renderConfig by viewModel.renderConfig.collectAsStateWithLifecycle()
    val renderUiState by viewModel.renderUiState.collectAsStateWithLifecycle()
    val selected3DModel by viewModel.selected3DModel.collectAsStateWithLifecycle()
    val tourSourceBitmap by viewModel.tourSourceBitmap.collectAsStateWithLifecycle()
    val tourAspectRatio by viewModel.tourAspectRatio.collectAsStateWithLifecycle()
    val tourCameraMove by viewModel.tourCameraMove.collectAsStateWithLifecycle()
    val virtualTourState by viewModel.virtualTourState.collectAsStateWithLifecycle()
    val isGeneratingBom by viewModel.isGeneratingBom.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Trigger non-intrusive interstitial ad after a completed render in Normal mode
    var lastShownRenderRef by remember { mutableStateOf<RenderUiState?>(null) }
    LaunchedEffect(renderUiState, entitlement.isPro) {
        if (!entitlement.isPro && renderUiState is RenderUiState.Success && renderUiState !== lastShownRenderRef) {
            lastShownRenderRef = renderUiState
            interstitialAdController.showIfEligible(activity = activity, isPro = false)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back press from secondary tabs returns to Projects home tab
    BackHandler(enabled = selectedTab != StudioTab.PROJECTS) {
        selectedTab = StudioTab.PROJECTS
    }

    val projectsList = (projectsState as? CloudUiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!entitlement.isPro) {
                            AdMobBannerBar(
                                priceLabel = entitlement.formattedPrice,
                                onUpgradeToProClick = {
                                    openPaywall("Pro Mod (${entitlement.formattedPrice}) ile tüm banner ve geçiş reklamlarını kalıcı olarak kaldırın.")
                                }
                            )
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            StudioTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelTr
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelTr,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelTr
                                    )
                                },
                                label = { Text(tab.labelTr, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 820.dp)
                        ) {
                            when (selectedTab) {
                                StudioTab.PROJECTS -> {
                                    ProjectDashboardScreen(
                                        userName = userDisplayName,
                                        entitlement = entitlement,
                                        projectsState = projectsState,
                                        selectedProjectId = selectedProjectId,
                                        onSelectProject = viewModel::selectProject,
                                        onCreateProject = viewModel::createNewProject,
                                        onUpdatePhase = viewModel::updateProjectPhase,
                                        onDeleteProject = viewModel::deleteProject,
                                        onApplyTemplateToRender = { template ->
                                            viewModel.applyShowcaseTemplate(template)
                                            selectedTab = StudioTab.RENDER
                                        },
                                        onOpenRenderForProject = { proj ->
                                            viewModel.prepareRenderForProject(proj)
                                            selectedTab = StudioTab.RENDER
                                        },
                                        onOpen3DForProject = { proj ->
                                            viewModel.selectProject(proj.id)
                                            threeDInitialSubTab = 0
                                            selectedTab = StudioTab.VIEW_3D
                                        },
                                        onOpenMaterialsForProject = { proj ->
                                            viewModel.selectProject(proj.id)
                                            selectedTab = StudioTab.MATERIALS
                                        },
                                        onOpenPaywall = openPaywall,
                                        onSignOut = {
                                            signOutUser(credentialManager, scope)
                                        }
                                    )
                                }

                                StudioTab.RENDER -> {
                                    RenderStudioScreen(
                                        config = renderConfig,
                                        renderState = renderUiState,
                                        entitlement = entitlement,
                                        projects = projectsList,
                                        selectedProjectId = selectedProjectId,
                                        onUpdateConfig = viewModel::updateRenderConfig,
                                        onPhotoPicked = viewModel::setUploadedPhotoForEdit,
                                        onGenerateClick = {
                                            proPlanManager.recordRenderUsed()
                                            viewModel.runRenderOrPhotoEdit()
                                        },
                                        onSaveRenderToCloud = { title, area, existingId ->
                                            if (existingId == null && !entitlement.canCreateProject(projectsList.size)) {
                                                openPaywall(
                                                    "Normal modda en fazla ${entitlement.maxFreeProjects} bulut projesi saklayabilirsiniz. Mevcut bir projenin üzerine kaydedebilir veya Pro'ya geçebilirsiniz (${entitlement.formattedPrice})."
                                                )
                                            } else {
                                                viewModel.saveCurrentRenderToCloudProject(title, area, existingId)
                                            }
                                        },
                                        onSendToVirtualTour = { bmp ->
                                            viewModel.setTourSourceBitmap(bmp)
                                            threeDInitialSubTab = 1
                                            selectedTab = StudioTab.VIEW_3D
                                        },
                                        onGenerateBomFromRender = {
                                            if (!entitlement.isPro) {
                                                openPaywall(
                                                    "AI ile otomatik malzeme & metraj listesi oluşturma Pro moduna özeldir (${entitlement.formattedPrice})."
                                                )
                                            } else {
                                                selectedTab = StudioTab.MATERIALS
                                                viewModel.generateAiMaterialsForSelectedProject()
                                            }
                                        },
                                        onOpenPaywall = openPaywall
                                    )
                                }

                                StudioTab.CHAT -> {
                                    ArchitecturalDesignChatComponent(
                                        viewModel = architecturalChatViewModel,
                                        entitlement = entitlement,
                                        onOpenInProRenderStudio = { prompt ->
                                            if (!entitlement.canRunRender()) {
                                                openPaywall(
                                                    "Normal modda günlük ${entitlement.dailyRenderLimit} ücretsiz render hakkınız doldu. Sınırsız render için Pro'ya geçin (${entitlement.formattedPrice})."
                                                )
                                            } else {
                                                proPlanManager.recordRenderUsed()
                                                viewModel.prepareRenderFromChatPrompt(prompt)
                                                selectedTab = StudioTab.RENDER
                                                viewModel.runRenderOrPhotoEdit()
                                            }
                                        },
                                        onSendRenderToVirtualTour = { bmp ->
                                            viewModel.setTourSourceBitmap(bmp)
                                            threeDInitialSubTab = 1
                                            selectedTab = StudioTab.VIEW_3D
                                        },
                                        onChatPromptSubmitted = {
                                            proPlanManager.recordChatUsed()
                                        },
                                        onInlineRenderSubmitted = {
                                            proPlanManager.recordRenderUsed()
                                        },
                                        onOpenPaywall = openPaywall
                                    )
                                }

                                StudioTab.VIEW_3D -> {
                                    ThreeDViewerScreen(
                                        entitlement = entitlement,
                                        selectedModel = selected3DModel,
                                        tourSourceBitmap = tourSourceBitmap,
                                        tourAspectRatio = tourAspectRatio,
                                        tourCameraMove = tourCameraMove,
                                        virtualTourState = virtualTourState,
                                        initialSubTab = threeDInitialSubTab,
                                        onSelectModel = viewModel::select3DModel,
                                        onSetTourSourceBitmap = viewModel::setTourSourceBitmap,
                                        onSetTourAspectRatio = viewModel::setTourAspectRatio,
                                        onSetTourCameraMove = viewModel::setTourCameraMove,
                                        onGenerateAiTour = viewModel::generateAiVirtualTour,
                                        onOpenPaywall = openPaywall
                                    )
                                }

                                StudioTab.MATERIALS -> {
                                    MaterialsScreen(
                                        entitlement = entitlement,
                                        projects = projectsList,
                                        selectedProjectId = selectedProjectId,
                                        materialsState = materialsState,
                                        isGeneratingBom = isGeneratingBom,
                                        onSelectProject = viewModel::selectProject,
                                        onGenerateAiBom = viewModel::generateAiMaterialsForSelectedProject,
                                        onAddMaterial = viewModel::addMaterialToSelectedProject,
                                        onTogglePurchased = viewModel::toggleMaterialPurchased,
                                        onDeleteMaterial = viewModel::deleteMaterial,
                                        onGoToProjects = { selectedTab = StudioTab.PROJECTS },
                                        onOpenPaywall = openPaywall
                                    )
                                }
                            }
                        }
                    }

                    // Show Banner Ad at bottom of main content on wide/tablet NavigationRail layouts in Normal mode
                    if (useNavigationRail && !entitlement.isPro) {
                        AdMobBannerBar(
                            priceLabel = entitlement.formattedPrice,
                            onUpgradeToProClick = {
                                openPaywall("Pro Mod (${entitlement.formattedPrice}) ile tüm reklamları kalıcı olarak kaldırın.")
                            },
                            modifier = Modifier.widthIn(max = 820.dp)
                        )
                    }
                }
            }
        }
    }

    if (showPaywall) {
        ProPaywallDialog(
            entitlement = entitlement,
            lockedFeatureReason = paywallReason,
            onDismiss = { showPaywall = false },
            onPurchaseClick = {
                if (activity != null) {
                    proPlanManager.launchPurchase(activity)
                }
            },
            onRestoreClick = { proPlanManager.restorePurchases() },
            onDebugTogglePro = { enabled -> proPlanManager.setDebugProPreview(enabled) }
        )
    }
}

