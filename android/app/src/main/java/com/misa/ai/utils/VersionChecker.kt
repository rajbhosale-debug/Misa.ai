package com.misa.ai.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for version checking and comparison
 */
@Singleton
class VersionChecker @Inject constructor(
    private val context: Context
) {

    /**
     * Get current app version
     */
    fun getCurrentVersion(): AppVersion {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            AppVersion(
                versionName = packageInfo.versionName ?: "1.0.0",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current version")
            AppVersion("1.0.0", 1)
        }
    }

    /**
     * Compare two version strings
     */
    fun compareVersions(version1: String, version2: String): Int {
        try {
            val v1Parts = version1.split(".").map { it.trim() }
            val v2Parts = version2.split(".").map { it.trim() }

            val maxLength = maxOf(v1Parts.size, v2Parts.size)

            for (i in 0 until maxLength) {
                val v1Part = if (i < v1Parts.size) parseVersionPart(v1Parts[i]) else 0
                val v2Part = if (i < v2Parts.size) parseVersionPart(v2Parts[i]) else 0

                when {
                    v1Part > v2Part -> return 1
                    v1Part < v2Part -> return -1
                }
            }

            return 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to compare versions: $version1 vs $version2")
            return 0
        }
    }

    /**
     * Check if version 1 is newer than version 2
     */
    fun isNewer(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) > 0
    }

    /**
     * Check if update is available
     */
    fun isUpdateAvailable(currentVersion: String, availableVersion: String): Boolean {
        return isNewer(availableVersion, currentVersion)
    }

    /**
     * Check if version is compatible with minimum required version
     */
    fun isCompatible(currentVersion: String, minimumVersion: String): Boolean {
        return compareVersions(currentVersion, minimumVersion) >= 0
    }

    /**
     * Extract version number from string
     */
    fun extractVersion(input: String): String {
        // Look for version patterns like v1.0.0, version 1.0.0, 1.0.0
        val versionRegex = Regex("""(?:v|version\s*)?(\d+(?:\.\d+)*(?:\.\d+)?)""")
        val match = versionRegex.find(input.lowercase())
        return match?.groupValues?.get(1) ?: input
    }

    /**
     * Get version categories for UI display
     */
    fun getVersionCategory(version: String): VersionCategory {
        return when {
            version.contains("alpha") -> VersionCategory.ALPHA
            version.contains("beta") -> VersionCategory.BETA
            version.contains("rc") || version.contains("release.candidate") -> VersionCategory.RC
            version.contains("dev") || version.contains("development") -> VersionCategory.DEVELOPMENT
            else -> VersionCategory.STABLE
        }
    }

    /**
     * Format version for display
     */
    fun formatVersionForDisplay(version: String, includeCategory: Boolean = true): String {
        val cleanVersion = extractVersion(version)
        val category = if (includeCategory) getVersionCategory(version) else VersionCategory.STABLE

        return when (category) {
            VersionCategory.ALPHA -> "$cleanVersion (Alpha)"
            VersionCategory.BETA -> "$cleanVersion (Beta)"
            VersionCategory.RC -> "$cleanVersion (RC)"
            VersionCategory.DEVELOPMENT -> "$cleanVersion (Dev)"
            VersionCategory.STABLE -> cleanVersion
        }
    }

    /**
     * Parse version part to integer
     */
    private fun parseVersionPart(part: String): Int {
        return try {
            // Remove any non-numeric characters and convert to int
            val numericPart = part.replace(Regex("[^0-9]"), "")
            if (numericPart.isNotEmpty()) numericPart.toInt() else 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get architecture-specific version code
     */
    fun getArchitectureVersionCode(baseCode: Int): Int {
        val abi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }

        // Add architecture suffix to version code
        return when {
            abi.contains("arm64") -> baseCode + 1000
            abi.contains("arm") -> baseCode + 2000
            abi.contains("x86_64") -> baseCode + 3000
            abi.contains("x86") -> baseCode + 4000
            else -> baseCode
        }
    }
}

/**
 * App version data class
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Int
) {
    /**
     * Check if this version is newer than another
     */
    fun isNewerThan(other: AppVersion): Boolean {
        return this.versionCode > other.versionCode
    }

    /**
     * Check if this version is older than another
     */
    fun isOlderThan(other: AppVersion): Boolean {
        return this.versionCode < other.versionCode
    }

    /**
     * Check if this is the same version as another
     */
    fun isSameAs(other: AppVersion): Boolean {
        return this.versionCode == other.versionCode
    }

    override fun toString(): String = "v$versionName ($versionCode)"
}

/**
 * Version category enum
 */
enum class VersionCategory {
    ALPHA,
    BETA,
    RC,
    DEVELOPMENT,
    STABLE
}

/**
 * Version information data class
 */
data class VersionInfo(
    val currentVersion: AppVersion,
    val latestVersion: AppVersion?,
    val updateAvailable: Boolean,
    val mandatoryUpdate: Boolean = false,
    val minimumSupportedVersion: AppVersion? = null
) {
    /**
     * Check if current version is supported
     */
    fun isCurrentVersionSupported(): Boolean {
        return minimumSupportedVersion?.let { minVersion ->
            currentVersion.versionCode >= minVersion.versionCode
        } ?: true
    }

    /**
     * Get update priority
     */
    fun getUpdatePriority(): UpdatePriority {
        return when {
            !isCurrentVersionSupported() -> UpdatePriority.CRITICAL
            mandatoryUpdate -> UpdatePriority.HIGH
            updateAvailable -> UpdatePriority.MEDIUM
            else -> UpdatePriority.LOW
        }
    }
}

/**
 * Update priority enum
 */
enum class UpdatePriority {
    CRITICAL,   // Must update now
    HIGH,       // Should update soon
    MEDIUM,     // Optional update
    LOW         // No update needed
}