package com.example.data

import com.example.model.*

object StoreCatalog {
    // Clean initial state: zero fake apps in the store!
    // Apps are added only when creators upload an APK and publish them.
    val sampleApps: List<StoreApp> = emptyList()

    // Real clean baseline analytics (0 downloads, 0 fake numbers)
    val sampleAnalytics: AppAnalytics = AppAnalytics(
        appId = "creator_portfolio",
        appTitle = "Developer Portfolio",
        totalDownloads = 0,
        activeInstalls = 0,
        dailyActiveUsers = 0,
        monthlyActiveUsers = 0,
        crashFreeRate = 100.0f,
        avgSessionMinutes = 0.0f,
        ratingAverage = 0.0f,
        impressions = 0,
        productPageViews = 0,
        conversionRate = 0.0f,
        dailyDownloads = listOf(
            DailyMetric("Mon", 0, 0),
            DailyMetric("Tue", 0, 0),
            DailyMetric("Wed", 0, 0),
            DailyMetric("Thu", 0, 0),
            DailyMetric("Fri", 0, 0),
            DailyMetric("Sat", 0, 0),
            DailyMetric("Sun", 0, 0)
        ),
        topCountries = emptyList()
    )
}
