package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppPublishState
import com.example.model.SecurityReport
import com.example.model.StoreApp
import com.example.ui.components.StoreHeader
import com.example.ui.components.WifiRequiredBanner
import com.example.ui.dialogs.AppDetailDialog
import com.example.ui.dialogs.EditDeveloperProfileDialog
import com.example.ui.dialogs.JoinPlaytestDialog
import com.example.ui.dialogs.VirusTotalReportDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NetworkHelper
import com.example.viewmodel.FloppaStoreViewModel
import com.example.viewmodel.StoreTab

class MainActivity : ComponentActivity() {

    private val viewModel: FloppaStoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                FloppaAppsApp(
                    viewModel = viewModel,
                    onOpenWifiSettings = {
                        try {
                            startActivity(NetworkHelper.openWifiSettingsIntent())
                        } catch (e: Exception) {
                            // Handled safely
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FloppaAppsApp(
    viewModel: FloppaStoreViewModel,
    onOpenWifiSettings: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val inspectingReport by viewModel.inspectingReport.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val installedPackages by viewModel.installedPackages.collectAsStateWithLifecycle()
    val deviceSpecs by viewModel.deviceSpecs.collectAsStateWithLifecycle()

    // Wi-Fi Status
    val isWifiConnected by viewModel.isWifiConnected.collectAsStateWithLifecycle()

    // Normal User Profile
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Security Scanner State
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()
    val deviceScanSummary by viewModel.deviceScanSummary.collectAsStateWithLifecycle()

    // Floppa AI State
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()

    // Developer & APK Upload State
    val developerProfile by viewModel.developerProfile.collectAsStateWithLifecycle()
    val developerAnalytics by viewModel.developerAnalytics.collectAsStateWithLifecycle()
    val isUploadingApk by viewModel.isUploadingApk.collectAsStateWithLifecycle()
    val uploadedApkName by viewModel.uploadedApkName.collectAsStateWithLifecycle()
    val uploadedApkSizeMb by viewModel.uploadedApkSizeMb.collectAsStateWithLifecycle()
    val uploadedApkSha256 by viewModel.uploadedApkSha256.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val publishPreflightReport by viewModel.publishPreflightReport.collectAsStateWithLifecycle()
    val publishSecurityError by viewModel.publishSecurityError.collectAsStateWithLifecycle()

    var showJoinPlaytestDialog by remember { mutableStateOf(false) }
    var showEditDeveloperProfileDialog by remember { mutableStateOf(false) }
    var customReportTitle by remember { mutableStateOf("Security Audit") }

    // Back handler
    BackHandler(enabled = selectedApp != null || inspectingReport != null || showJoinPlaytestDialog || showEditDeveloperProfileDialog || currentTab != StoreTab.DISCOVER) {
        when {
            showEditDeveloperProfileDialog -> showEditDeveloperProfileDialog = false
            showJoinPlaytestDialog -> showJoinPlaytestDialog = false
            inspectingReport != null -> viewModel.inspectReport(null)
            selectedApp != null -> viewModel.selectApp(null)
            currentTab != StoreTab.DISCOVER -> viewModel.selectTab(StoreTab.DISCOVER)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // Persistent Wi-Fi Required Banner across the Store
                if (!isWifiConnected) {
                    WifiRequiredBanner(
                        onConnectWifiClick = onOpenWifiSettings,
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                if (currentTab == StoreTab.DISCOVER) {
                    StoreHeader(
                        searchQuery = searchQuery,
                        isWifiConnected = isWifiConnected,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSecurityShieldClick = { viewModel.selectTab(StoreTab.SECURITY) },
                        onProfileClick = { viewModel.selectTab(StoreTab.PROFILE) },
                        onWifiStatusClick = onOpenWifiSettings,
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("store_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                StoreTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Text(text = tab.iconEmoji, fontSize = if (isSelected) 20.sp else 17.sp)
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                StoreTab.DISCOVER -> {
                    val publicApps = allApps.filter { it.publishState == AppPublishState.PUBLIC_RELEASE }
                    val filteredBySearch = if (searchQuery.isBlank()) {
                        publicApps
                    } else {
                        publicApps.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.category.label.contains(searchQuery, ignoreCase = true) ||
                            it.packageName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    DiscoverScreen(
                        apps = filteredBySearch,
                        downloads = downloads,
                        installedPackages = installedPackages,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onAppClick = { viewModel.selectApp(it) },
                        onDownloadClick = { viewModel.startDownload(it) },
                        onSecurityReportClick = { app ->
                            customReportTitle = app.title
                            viewModel.inspectReport(app.securityReport)
                        },
                        onOpenAiBannerClick = { viewModel.selectTab(StoreTab.FLOPPA_AI) },
                        onGoToDeveloperHubClick = { viewModel.selectTab(StoreTab.DEVELOPER) },
                        onJoinPlaytestClick = { showJoinPlaytestDialog = true }
                    )
                }

                StoreTab.SECURITY -> {
                    SecurityScannerScreen(
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        statusMessage = scanStatusMessage,
                        scanSummary = deviceScanSummary,
                        onStartScan = { viewModel.runDeviceSecurityScan() },
                        onInspectReport = { report, title ->
                            customReportTitle = title
                            viewModel.inspectReport(report)
                        },
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }

                StoreTab.FLOPPA_AI -> {
                    FloppaAiScreen(
                        deviceSpecs = deviceSpecs,
                        messages = aiMessages,
                        isThinking = isAiThinking,
                        onSendMessage = { viewModel.sendAiPrompt(it) },
                        onAppClick = { viewModel.selectApp(it) },
                        onDownloadClick = { viewModel.startDownload(it) },
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }

                StoreTab.DOWNLOADS -> {
                    DownloadsScreen(
                        downloads = downloads,
                        apps = allApps,
                        installedPackages = installedPackages,
                        onInstallApk = { viewModel.startDownload(it) },
                        onAppClick = { viewModel.selectApp(it) },
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }

                StoreTab.PROFILE -> {
                    // Normal Non-Developer Profile Page
                    ProfileScreen(
                        userProfile = userProfile,
                        developerProfile = developerProfile,
                        isWifiConnected = isWifiConnected,
                        installedAppsCount = installedPackages.size,
                        playtestsCount = allApps.count { it.publishState == AppPublishState.PLAYTEST_PRIVATE },
                        onOpenWifiSettings = onOpenWifiSettings,
                        onSwitchToDeveloperHub = { viewModel.selectTab(StoreTab.DEVELOPER) },
                        onEditDeveloperProfileClick = { showEditDeveloperProfileDialog = true },
                        onJoinPlaytestClick = { showJoinPlaytestDialog = true },
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }

                StoreTab.DEVELOPER -> {
                    DeveloperScreen(
                        developerProfile = developerProfile,
                        analytics = developerAnalytics,
                        myApps = allApps,
                        isUploadingApk = isUploadingApk,
                        uploadedApkName = uploadedApkName,
                        uploadedApkSizeMb = uploadedApkSizeMb,
                        uploadedApkSha256 = uploadedApkSha256,
                        isPublishing = isPublishing,
                        preflightReport = publishPreflightReport,
                        publishSecurityError = publishSecurityError,
                        onSignInGoogle = { viewModel.signInDeveloperGoogle() },
                        onEditDeveloperProfile = { newName, newHandle, photoUri ->
                            viewModel.updateDeveloperProfile(newName, newHandle, photoUri)
                        },
                        onUploadApk = { name, size, uri -> viewModel.handleApkUpload(name, size, uri) },
                        onCreatePlaytest = { title, pkg, cat, desc, ver, perms ->
                            viewModel.createPlaytestApp(title, pkg, cat, desc, ver, perms)
                        },
                        onPublishToAllUsers = { appId -> viewModel.publishAppToAllUsers(appId) },
                        onUnpublishApp = { appId -> viewModel.unpublishApp(appId) },
                        onDeleteApp = { appId -> viewModel.deleteApp(appId) },
                        onAppClick = { viewModel.selectApp(it) },
                        modifier = if (isWifiConnected) Modifier.statusBarsPadding() else Modifier
                    )
                }
            }

            // App Detail Modal Dialog
            selectedApp?.let { app ->
                AppDetailDialog(
                    app = app,
                    deviceSpecs = deviceSpecs,
                    downloadItem = downloads[app.id],
                    isInstalled = installedPackages.contains(app.packageName),
                    onDownloadClick = { viewModel.startDownload(app) },
                    onViewSecurityReport = {
                        customReportTitle = app.title
                        viewModel.inspectReport(app.securityReport)
                    },
                    onSubmitFeedback = { rating, comment ->
                        viewModel.submitPlaytestFeedback(app.id, userProfile.displayName, rating, comment)
                    },
                    onPublishToAllUsers = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE) {
                        { viewModel.publishAppToAllUsers(app.id) }
                    } else null,
                    onUnpublishApp = if (app.publishState == AppPublishState.PUBLIC_RELEASE) {
                        { viewModel.unpublishApp(app.id) }
                    } else null,
                    onDeleteApp = { viewModel.deleteApp(app.id) },
                    onDismiss = { viewModel.selectApp(null) }
                )
            }

            // Join Playtest Dialog (Input Beta Code or Link)
            if (showJoinPlaytestDialog) {
                JoinPlaytestDialog(
                    onDismiss = { showJoinPlaytestDialog = false },
                    onJoin = { code -> viewModel.joinPlaytestByCode(code) }
                )
            }

            // Edit Developer Profile & Upload Picture Dialog
            if (showEditDeveloperProfileDialog) {
                EditDeveloperProfileDialog(
                    currentProfile = developerProfile,
                    onDismiss = { showEditDeveloperProfileDialog = false },
                    onSaveProfile = { newName, newHandle, photoUri ->
                        viewModel.updateDeveloperProfile(newName, newHandle, photoUri)
                    }
                )
            }

            // VirusTotal & FloppaSecurity Deep Audit Dialog
            inspectingReport?.let { report ->
                VirusTotalReportDialog(
                    report = report,
                    appTitle = customReportTitle,
                    onDismiss = { viewModel.inspectReport(null) }
                )
            }
        }
    }
}
