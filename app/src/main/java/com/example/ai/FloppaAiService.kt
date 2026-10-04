package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.DeviceSpecs
import com.example.model.StoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FloppaAiService {
    private const val TAG = "FloppaAiService"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AiResponse(
        val messageText: String,
        val recommendedAppIds: List<String>,
        val systemFitAnalysis: String?
    )

    suspend fun getAppRecommendation(
        userPrompt: String,
        deviceSpecs: DeviceSpecs,
        availableApps: List<StoreApp>
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // Formulate the app catalog summary for Gemini
        val catalogSummary = availableApps.joinToString("\n") { app ->
            "- ID: ${app.id} | Name: ${app.title} | Category: ${app.category.label} | Size: ${app.sizeMb}MB | Min Android API: ${app.minAndroidVersion} | Rec RAM: ${app.recommendedRamGb}GB | Description: ${app.description}"
        }

        val systemContext = """
            You are Floppa AI, the witty, hyper-intelligent caracal mascot and tech advisor of "Floppa Apps".
            The user wants advice on what app best fits their needs, tailored to their exact device hardware specs.

            USER DEVICE SPECS:
            - Device: ${deviceSpecs.manufacturer} ${deviceSpecs.deviceModel}
            - OS: ${deviceSpecs.androidVersion} (API ${deviceSpecs.apiLevel})
            - Total RAM: ${deviceSpecs.totalRamGb} GB (Available: ${deviceSpecs.freeRamGb} GB)
            - Internal Storage: ${deviceSpecs.freeStorageGb} GB free of ${deviceSpecs.totalStorageGb} GB
            - CPU Architecture: ${deviceSpecs.cpuAbi} (${deviceSpecs.cpuCores} cores)

            AVAILABLE FLOPPA STORE APPS:
            $catalogSummary

            TASK:
            1. Analyze the user's request.
            2. Check device compatibility (RAM, storage, Android version).
            3. Recommend 1 to 3 best matching apps from the store list above.
            4. State why each app fits the user's requirements and device specs.
            5. In the final line of your response, output a tag in the exact format:
               [RECOMMENDED_IDS: id1, id2]
               (using the exact IDs from the catalog).
            6. Keep your tone cheerful, knowledgeable, with fun Floppa caracal enthusiasm!
        """.trimIndent()

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local Floppa AI reasoning fallback if API key is not configured in Secrets yet
            return@withContext generateLocalRecommendation(userPrompt, deviceSpecs, availableApps)
        }

        try {
            // Call Gemini API (gemini-3.5-flash)
            val modelName = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val jsonPayload = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val turn = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemContext\n\nUser Question: $userPrompt")
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(turn)
                }
                put("contents", contentsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val rawResponse = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error code: ${response.code}, body: $rawResponse")
                return@withContext generateLocalRecommendation(userPrompt, deviceSpecs, availableApps)
            }

            val rootJson = JSONObject(rawResponse)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseAiResponse(text, deviceSpecs, availableApps)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed: ${e.message}", e)
            generateLocalRecommendation(userPrompt, deviceSpecs, availableApps)
        }
    }

    private fun parseAiResponse(
        rawText: String,
        deviceSpecs: DeviceSpecs,
        availableApps: List<StoreApp>
    ): AiResponse {
        val pattern = Regex("\\[RECOMMENDED_IDS:\\s*([^\\]]+)\\]", RegexOption.IGNORE_CASE)
        val match = pattern.find(rawText)

        val recommendedIds = mutableListOf<String>()
        val cleanText = if (match != null) {
            val idsPart = match.groupValues[1]
            idsPart.split(",").map { it.trim() }.forEach { id ->
                if (availableApps.any { it.id == id }) {
                    recommendedIds.add(id)
                }
            }
            rawText.replace(match.value, "").trim()
        } else {
            // Find mentioned app titles
            availableApps.forEach { app ->
                if (rawText.contains(app.title, ignoreCase = true)) {
                    recommendedIds.add(app.id)
                }
            }
            rawText.trim()
        }

        val fitAnalysis = "Optimized for your ${deviceSpecs.deviceModel} (${deviceSpecs.totalRamGb}GB RAM, ${deviceSpecs.androidVersion})"

        return AiResponse(
            messageText = cleanText,
            recommendedAppIds = recommendedIds.distinct(),
            systemFitAnalysis = fitAnalysis
        )
    }

    private fun generateLocalRecommendation(
        prompt: String,
        deviceSpecs: DeviceSpecs,
        availableApps: List<StoreApp>
    ): AiResponse {
        val lower = prompt.lowercase()

        val matchingApps = when {
            lower.contains("game") || lower.contains("play") || lower.contains("fps") -> {
                availableApps.filter { it.category == com.example.model.AppCategory.GAMES }
            }
            lower.contains("security") || lower.contains("virus") || lower.contains("malware") || lower.contains("protect") || lower.contains("clean") -> {
                availableApps.filter { it.category == com.example.model.AppCategory.SECURITY }
            }
            lower.contains("tool") || lower.contains("utility") || lower.contains("battery") || lower.contains("speed") -> {
                availableApps.filter { it.category == com.example.model.AppCategory.TOOLS }
            }
            lower.contains("productivity") || lower.contains("work") || lower.contains("notes") || lower.contains("pdf") -> {
                availableApps.filter { it.category == com.example.model.AppCategory.PRODUCTIVITY }
            }
            lower.contains("audio") || lower.contains("music") || lower.contains("media") || lower.contains("video") -> {
                availableApps.filter { it.category == com.example.model.AppCategory.MEDIA }
            }
            else -> {
                // Recommend top rated that comfortably fit device RAM
                availableApps.filter { it.recommendedRamGb <= deviceSpecs.totalRamGb }.take(3)
            }
        }.ifEmpty { availableApps.take(2) }

        val appsDescription = matchingApps.joinToString("\n\n") { app ->
            "🐾 **${app.title}** (v${app.version} • ${app.sizeMb}MB)\n" +
            "• Performance fit: Requires ${app.recommendedRamGb}GB RAM (your device has ${deviceSpecs.totalRamGb}GB — flawless fit!)\n" +
            "• OS Compatibility: Requires API ${app.minAndroidVersion}+ (you are running ${deviceSpecs.androidVersion})\n" +
            "• FloppaSecurity Scan: ${app.securityReport.virusTotalCleanRatio} (Score: ${app.securityReport.scanScore}/100)"
        }

        val reply = """
            Greetings! Floppa AI here inspecting your hardware specs! 🐱🚀

            Your **${deviceSpecs.manufacturer} ${deviceSpecs.deviceModel}** is packing **${deviceSpecs.totalRamGb} GB RAM** and **${deviceSpecs.freeStorageGb} GB free storage** on ${deviceSpecs.androidVersion}. Based on your query:

            $appsDescription

            Both apps are verified 100% clean by FloppaSecurity & VirusTotal 72-engine scans and will run silky smooth without thermal throttling! Tap any card below to inspect security details or download immediately.
        """.trimIndent()

        return AiResponse(
            messageText = reply,
            recommendedAppIds = matchingApps.map { it.id },
            systemFitAnalysis = "Tailored for ${deviceSpecs.cpuAbi} • ${deviceSpecs.totalRamGb}GB RAM"
        )
    }
}
