package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.dialogs.EditDeveloperProfileDialog
import java.io.File

@Composable
fun DeveloperScreen(
    developerProfile: DeveloperProfile?,
    analytics: AppAnalytics,
    myApps: List<StoreApp>,
    isUploadingApk: Boolean,
    uploadedApkName: String,
    uploadedApkSizeMb: Float,
    uploadedApkSha256: String,
    isPublishing: Boolean,
    preflightReport: SecurityReport?,
    publishSecurityError: String?,
    onSignInGoogle: () -> Unit,
    onEditDeveloperProfile: (displayName: String, handle: String, photoUri: Uri?) -> Unit,
    onUploadApk: (fileName: String, sizeMb: Float, uri: Uri?) -> Unit,
    onCreatePlaytest: (
        title: String,
        packageName: String,
        category: AppCategory,
        description: String,
        version: String,
        permissions: List<String>
    ) -> Unit,
    onPublishToAllUsers: (appId: String) -> Unit,
    onUnpublishApp: (appId: String) -> Unit,
    onDeleteApp: (appId: String) -> Unit,
    onAppClick: (StoreApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    var appTitle by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("1.0.0") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.GAMES) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    var appToDelete by remember { mutableStateOf<StoreApp?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Comprehensive permissions grouped by functionality
    val permissionGroups = remember {
        listOf(
            "Network & Web" to listOf(
                "INTERNET",
                "ACCESS_NETWORK_STATE",
                "ACCESS_WIFI_STATE",
                "CHANGE_WIFI_STATE"
            ),
            "Camera & Audio" to listOf(
                "CAMERA",
                "RECORD_AUDIO",
                "MODIFY_AUDIO_SETTINGS",
                "FLASHLIGHT"
            ),
            "Location & Sensors" to listOf(
                "ACCESS_FINE_LOCATION",
                "ACCESS_COARSE_LOCATION",
                "BODY_SENSORS",
                "ACTIVITY_RECOGNITION"
            ),
            "Bluetooth & NFC" to listOf(
                "BLUETOOTH",
                "BLUETOOTH_CONNECT",
                "BLUETOOTH_SCAN",
                "NFC"
            ),
            "System & Background" to listOf(
                "POST_NOTIFICATIONS",
                "VIBRATE",
                "WAKE_LOCK",
                "FOREGROUND_SERVICE",
                "RECEIVE_BOOT_COMPLETED",
                "REQUEST_INSTALL_PACKAGES",
                "SYSTEM_ALERT_WINDOW"
            ),
            "Media & Files" to listOf(
                "READ_MEDIA_IMAGES",
                "READ_MEDIA_VIDEO",
                "READ_MEDIA_AUDIO"
            )
        )
    }

    val selectedPermissions = remember { mutableStateListOf("INTERNET", "ACCESS_NETWORK_STATE") }
    var selectedPermissionTab by remember { mutableStateOf("All") }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "my_game.apk"
            onUploadApk(fileName, 14.5f, uri)
            if (appTitle.isBlank()) {
                val inferredTitle = fileName.removeSuffix(".apk").replace("_", " ").replace("-", " ")
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                appTitle = inferredTitle
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("developer_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PROMINENT DEVELOPER PROFILE CARD
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "FLOOPA DEVELOPER PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Developer Profile Picture with Camera Badge
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFF0284C7))
                                        )
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { showEditProfileDialog = true }
                                    .testTag("developer_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                val photoUrl = developerProfile?.photoUrl
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = if (photoUrl.startsWith("/")) File(photoUrl) else photoUrl,
                                        contentDescription = "Developer Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(text = "🐱‍💻", fontSize = 34.sp)
                                }

                                // Camera overlay icon
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload Picture",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = developerProfile?.displayName ?: "Floppa Creator",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        modifier = Modifier.testTag("developer_display_name_text")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Developer",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = developerProfile?.email ?: "alex431562@gmail.com",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Handle: ${developerProfile?.developerHandle ?: "@alex_floppa"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // PROMINENT BUTTON: EDIT DEVELOPER NAME & UPLOAD PROFILE PICTURE
                    Button(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("prominent_edit_developer_profile_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHANGE DEVELOPER NAME & UPLOAD PICTURE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Real Developer Performance Analytics (starts at 0)
        item {
            Text(
                text = "Real Performance Analytics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "Total Downloads",
                    value = "${analytics.totalDownloads}",
                    sub = if (analytics.totalDownloads == 0) "Zero fake metrics" else "Live user installs",
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Active Installs",
                    value = "${analytics.activeInstalls}",
                    sub = "Verified devices",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsKpiCard(
                    title = "My Apps & Betas",
                    value = "${myApps.size}",
                    sub = "${myApps.count { it.publishState == AppPublishState.PLAYTEST_PRIVATE }} in playtesting",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsKpiCard(
                    title = "Playtester Feedback",
                    value = "${myApps.sumOf { it.playtestFeedbackList.size }}",
                    sub = "Reviews & bug notes",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Playtests & Beta Builds Section
        if (myApps.isNotEmpty()) {
            item {
                Text(
                    text = "My Uploaded Apps & Playtests (${myApps.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(myApps, key = { it.id }) { app ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE)
                            Color(0xFF1E1B4B)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(android.graphics.Color.parseColor(app.iconColorHex))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = app.iconEmoji, fontSize = 24.sp)
                                }
                                Column {
                                    Text(
                                        text = app.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "by ${app.developerName} • v${app.version} • ${app.sizeMb}MB",
                                        fontSize = 11.sp,
                                        color = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE)
                                    Color(0xFFF59E0B)
                                else
                                    Color(0xFF10B981)
                            ) {
                                Text(
                                    text = if (app.publishState == AppPublishState.PLAYTEST_PRIVATE) "PLAYTEST (PRIVATE)" else "PUBLIC RELEASE",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // FloppaSecurity Threat Database verification pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (app.securityReport.isSafeForPublicDistribution)
                                Color(0xFF10B981).copy(alpha = 0.15f)
                            else
                                Color(0xFFEF4444).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (app.securityReport.isSafeForPublicDistribution) Icons.Default.Shield else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (app.securityReport.isSafeForPublicDistribution) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (app.securityReport.isSafeForPublicDistribution)
                                        "FloppaSecurity Threat DB: Passed (SHA-256 Verified Safe)"
                                    else
                                        "FloppaSecurity Threat DB: Flagged (${app.securityReport.threatDetails})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (app.securityReport.isSafeForPublicDistribution) Color(0xFF047857) else Color(0xFFDC2626)
                                )
                            }
                        }

                        // If unlisted playtest: Display link and feedback
                        if (app.publishState == AppPublishState.PLAYTEST_PRIVATE) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Private Beta Code: ${app.playtestCode}",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(app.playtestCode))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Copy Code", fontSize = 10.sp)
                                        }
                                    }

                                    Text(
                                        text = "Link: ${app.playtestPrivateLink}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Feedback counter & review summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Playtester Feedback: ${app.playtestFeedbackList.size} submissions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                                TextButton(onClick = { onAppClick(app) }) {
                                    Text("View Feedback Details", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                }
                            }

                            // Publish App to all Users
                            Button(
                                onClick = { onPublishToAllUsers(app.id) },
                                enabled = app.securityReport.isSafeForPublicDistribution,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("publish_app_to_all_users_btn_${app.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish App to all Users", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            // Already Public: Show Unpublish button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live on Store • ${app.downloadCount} downloads",
                                    fontSize = 12.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Button(
                                    onClick = { onUnpublishApp(app.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("unpublish_app_btn_${app.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unpublish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Button to Delete the app
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { appToDelete = app },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("delete_app_btn_${app.id}")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // New App Upload & Publishing Studio (Requires APK upload first!)
        item {
            Text(
                text = "Publish New App (APK Upload Required)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // STEP 1: UPLOAD APK FILE
                    Text(
                        text = "Step 1: Upload APK File (Required)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (uploadedApkName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (preflightReport?.isSafeForPublicDistribution != false)
                                Color(0xFF10B981).copy(alpha = 0.15f)
                            else
                                Color(0xFFEF4444).copy(alpha = 0.2f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (preflightReport?.isSafeForPublicDistribution != false) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (preflightReport?.isSafeForPublicDistribution != false) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "APK Uploaded: $uploadedApkName ($uploadedApkSizeMb MB)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (preflightReport?.isSafeForPublicDistribution != false) Color(0xFF047857) else Color(0xFFDC2626)
                                    )
                                }
                                Text(
                                    text = "SHA-256: ${uploadedApkSha256.take(24)}...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "FloppaSecurity Threat DB: ${preflightReport?.threatDetails ?: "Verified Clean"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (preflightReport?.isSafeForPublicDistribution != false) Color(0xFF047857) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select APK File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val demoName = if (appTitle.isNotBlank()) "${appTitle.lowercase().replace(" ", "_")}.apk" else "floppa_app_v1.apk"
                                onUploadApk(demoName, 16.4f, null)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Package Sample APK", fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider()

                    // STEP 2: METADATA & PERMISSIONS
                    Text(
                        text = "Step 2: App Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = appTitle,
                        onValueChange = { appTitle = it },
                        label = { Text("Application Title") },
                        placeholder = { Text("e.g. My Amazing Game") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_app_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // PACKAGE NAME IS STRICTLY OPTIONAL
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name (Optional)") },
                        placeholder = { Text("Leave blank to auto-generate (e.g. com.developer.myapp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_pkg_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Box {
                        OutlinedButton(
                            onClick = { showCategoryDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Category: ${selectedCategory.label} (${selectedCategory.icon})")
                        }
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            AppCategory.values().filter { it != AppCategory.ALL && it != AppCategory.FEATURED }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.icon} ${cat.label}") },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Beta Testing Notes") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // PERMISSIONS SELECTION (Comprehensive Categorized Selector)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select App Permissions (${selectedPermissions.size} selected):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            TextButton(
                                onClick = {
                                    if (selectedPermissions.size == permissionGroups.sumOf { it.second.size }) {
                                        selectedPermissions.clear()
                                        selectedPermissions.addAll(listOf("INTERNET", "ACCESS_NETWORK_STATE"))
                                    } else {
                                        selectedPermissions.clear()
                                        permissionGroups.forEach { selectedPermissions.addAll(it.second) }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedPermissions.size == permissionGroups.sumOf { it.second.size }) "Reset Default" else "Select All",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Category Tab Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("All") + permissionGroups.map { it.first } .forEach { tabName ->
                                val isSelected = selectedPermissionTab == tabName
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPermissionTab = tabName },
                                    label = { Text(tabName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Grouped Permissions Display
                        val groupsToShow = if (selectedPermissionTab == "All") {
                            permissionGroups
                        } else {
                            permissionGroups.filter { it.first == selectedPermissionTab }
                        }

                        groupsToShow.forEach { (groupName, perms) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = groupName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    perms.forEach { perm ->
                                        val isChecked = selectedPermissions.contains(perm)
                                        FilterChip(
                                            selected = isChecked,
                                            onClick = {
                                                if (isChecked) selectedPermissions.remove(perm)
                                                else selectedPermissions.add(perm)
                                            },
                                            label = {
                                                Text(
                                                    text = perm,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (publishSecurityError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Text(
                                    text = publishSecurityError,
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Button: Create Private Playtest (Package Name is completely optional!)
                    Button(
                        onClick = {
                            val resolvedPackageName = if (packageName.isNotBlank()) {
                                packageName.trim()
                            } else {
                                "com.developer." + appTitle.lowercase().replace(Regex("[^a-z0-9]"), "")
                            }

                            onCreatePlaytest(
                                appTitle,
                                resolvedPackageName,
                                selectedCategory,
                                description,
                                version,
                                selectedPermissions.toList()
                            )
                        },
                        enabled = !isPublishing && appTitle.isNotBlank() && uploadedApkName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("publish_app_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running FloppaSecurity Threat DB Check...")
                        } else {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uploadedApkName.isBlank()) "UPLOAD APK FIRST TO CONTINUE" else "CREATE PRIVATE PLAYTEST BUILD",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog to Delete App
    appToDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete App: ${app.title}?") },
            text = {
                Text("Are you sure you want to permanently delete this app? It will be removed from Firestore, local storage, and the store catalog.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteApp(app.id)
                        appToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Developer Profile Dialog
    if (showEditProfileDialog) {
        EditDeveloperProfileDialog(
            currentProfile = developerProfile,
            onDismiss = { showEditProfileDialog = false },
            onSaveProfile = { newName, newHandle, photoUri ->
                onEditDeveloperProfile(newName, newHandle, photoUri)
            }
        )
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}
