package com.example.model

enum class AppPublishState {
    PLAYTEST_PRIVATE, // Playtester private beta only (accessible via private link / code)
    PUBLIC_RELEASE    // Fully published to all players in Floppa Store
}

data class PlaytestFeedback(
    val id: String,
    val testerName: String,
    val testerEmail: String,
    val rating: Int,
    val feedbackText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceModel: String
)

data class StoreApp(
    val id: String,
    val title: String,
    val packageName: String,
    val developerName: String,
    val developerEmail: String = "dev@floppaapps.com",
    val category: AppCategory,
    val iconEmoji: String = "📦",
    val iconColorHex: String = "#6366F1",
    val bannerGradient: List<String> = listOf("#4338CA", "#1E1B4B"),
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val downloadCount: Int = 0,
    val sizeMb: Float,
    val minAndroidVersion: Int = 24, // Android 7.0+
    val recommendedRamGb: Float = 2.0f,
    val description: String,
    val whatsNew: String,
    val permissionsRequired: List<String>,
    val apkDownloadUrl: String = "",
    val apkSha256: String,
    val version: String,
    val versionCode: Int = 1,
    val releaseDate: String,
    val securityReport: SecurityReport,
    val isEditorChoice: Boolean = false,
    val isFloppaOriginal: Boolean = false,
    val publishState: AppPublishState = AppPublishState.PLAYTEST_PRIVATE,
    val playtestCode: String = "",
    val playtestPrivateLink: String = "",
    val playtestFeedbackList: List<PlaytestFeedback> = emptyList(),
    val uploadedApkName: String = "",
    val uploadedApkPath: String? = null
)

enum class AppCategory(val label: String, val icon: String) {
    ALL("All", "✨"),
    FEATURED("Featured", "🔥"),
    GAMES("Games", "🎮"),
    TOOLS("Tools & Utilities", "🛠️"),
    SECURITY("Security & Privacy", "🛡️"),
    PRODUCTIVITY("Productivity", "⚡"),
    MEDIA("Media & Audio", "🎧"),
    FLOPPA_SPECIALS("Floppa Labs", "🐱")
}

data class SecurityReport(
    val scanScore: Int, // 0 - 100
    val virusTotalCleanRatio: String, // e.g. "0/72 Clean"
    val floppaSecurityStatus: FloppaSecurityStatus,
    val scannedVendors: List<VendorScanResult>,
    val malwareFound: Boolean = false,
    val spywareFound: Boolean = false,
    val trojanFound: Boolean = false,
    val adwareFound: Boolean = false,
    val suspiciousPermissions: List<String> = emptyList(),
    val sha256Checksum: String,
    val certificateSigned: Boolean = true,
    val certFingerprint: String = "SHA256: 7A:B3:9E:C1:4F:2D:6B:8A:1E:50:C3:92:DF:01:84:67",
    val lastScannedTime: String = "Just now",
    val heuristicEngineAnalysis: String = "Clean. FloppaGuard behavioral heuristic found zero anomalous dynamic DEX loads.",
    val isSafeForPublicDistribution: Boolean = true,
    val threatDetails: String = ""
)

data class UserProfile(
    val id: String = "user_alex_431562",
    val displayName: String = "Alex",
    val email: String = "alex431562@gmail.com",
    val avatarEmoji: String = "🐱",
    val memberSince: String = "October 2026",
    val accountType: String = "Standard User",
    val isWifiOnlyEnabled: Boolean = true,
    val autoScanEnabled: Boolean = true,
    val feedbackSubmittedCount: Int = 0
)

enum class FloppaSecurityStatus {
    CLEAN_VERIFIED,
    CAUTION,
    FLAGGED
}

data class VendorScanResult(
    val vendorName: String,
    val isClean: Boolean,
    val result: String = "Clean / Undetected"
)

data class DeveloperProfile(
    val id: String,
    val googleUid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val developerHandle: String = "@floppadev",
    val registeredDate: String = "2026-10-03",
    val isVerified: Boolean = true,
    val publishedAppIds: List<String> = emptyList()
)

data class AppAnalytics(
    val appId: String = "",
    val appTitle: String = "Overall Portfolio",
    val totalDownloads: Int = 0,
    val activeInstalls: Int = 0,
    val dailyActiveUsers: Int = 0,
    val monthlyActiveUsers: Int = 0,
    val crashFreeRate: Float = 100.0f,
    val avgSessionMinutes: Float = 0.0f,
    val ratingAverage: Float = 0.0f,
    val impressions: Int = 0,
    val productPageViews: Int = 0,
    val conversionRate: Float = 0.0f,
    val dailyDownloads: List<DailyMetric> = emptyList(),
    val topCountries: List<CountryMetric> = emptyList()
)

data class DailyMetric(
    val dayLabel: String,
    val downloads: Int,
    val activeUsers: Int
)

data class CountryMetric(
    val countryCode: String,
    val countryName: String,
    val percentage: Int
)

data class DownloadItem(
    val appId: String,
    val packageName: String,
    val appTitle: String,
    val version: String,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val localFilePath: String? = null,
    val error: String? = null
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    VERIFYING_SECURITY,
    COMPLETED,
    INSTALLING,
    INSTALLED,
    FAILED
}

data class DeviceSpecs(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalRamGb: Float,
    val freeRamGb: Float,
    val totalStorageGb: Float,
    val freeStorageGb: Float,
    val cpuAbi: String,
    val cpuCores: Int,
    val isInstallUnknownSourcesAllowed: Boolean = true
)

data class FloppaAiMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedApps: List<StoreApp> = emptyList(),
    val systemFitAnalysis: String? = null
)

data class UserReview(
    val id: String,
    val userName: String,
    val userAvatar: String,
    val rating: Int,
    val date: String,
    val comment: String,
    val helpfulCount: Int
)
