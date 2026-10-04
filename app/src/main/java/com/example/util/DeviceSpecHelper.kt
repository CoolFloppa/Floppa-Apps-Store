package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.model.DeviceSpecs
import java.io.File
import kotlin.math.roundToInt

object DeviceSpecHelper {

    fun getDeviceSpecs(context: Context): DeviceSpecs {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamBytes = memInfo.totalMem
        val availRamBytes = memInfo.availMem

        val totalRamGb = ((totalRamBytes / (1024f * 1024f * 1024f)) * 10).roundToInt() / 10f
        val freeRamGb = ((availRamBytes / (1024f * 1024f * 1024f)) * 10).roundToInt() / 10f

        val (totalStorageGb, freeStorageGb) = getStorageInfo()

        val cores = Runtime.getRuntime().availableProcessors()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

        return DeviceSpecs(
            deviceModel = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT,
            totalRamGb = if (totalRamGb <= 0f) 6.0f else totalRamGb,
            freeRamGb = if (freeRamGb <= 0f) 3.2f else freeRamGb,
            totalStorageGb = totalStorageGb,
            freeStorageGb = freeStorageGb,
            cpuAbi = abi,
            cpuCores = cores,
            isInstallUnknownSourcesAllowed = canInstall
        )
    }

    private fun getStorageInfo(): Pair<Float, Float> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availBlocks * blockSize

            val totalGb = ((totalBytes / (1024f * 1024f * 1024f)) * 10).roundToInt() / 10f
            val freeGb = ((freeBytes / (1024f * 1024f * 1024f)) * 10).roundToInt() / 10f
            Pair(if (totalGb <= 0f) 64.0f else totalGb, if (freeGb <= 0f) 32.5f else freeGb)
        } catch (e: Exception) {
            Pair(64.0f, 32.5f)
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getInstalledVersionCode(context: Context, packageName: String): Int {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (info.longVersionCode and 0xFFFFFFFFL).toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        } catch (e: Exception) {
            -1
        }
    }
}
