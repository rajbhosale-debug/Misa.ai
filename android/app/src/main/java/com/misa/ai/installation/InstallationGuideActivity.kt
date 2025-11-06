package com.misa.ai.installation

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.misa.ai.R
import com.misa.ai.installation.service.AutoInstallerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Interactive installation guide with settings navigation
 * Guides users through enabling "install from unknown sources" with visual overlays
 */
class InstallationGuideActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var stepIndicator: TextView
    private lateinit var guideImage: ImageView
    private lateinit var actionButton: Button
    private lateinit var skipButton: Button

    private var currentStep = InstallationStep.UNKNOWN_SOURCES
    private var coordinatorUrl: String? = null
    private var apkUrl: String? = null
    private var pairingToken: String? = null

    // Activity result launchers
    private val unknownSourcesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleUnknownSourcesResult(result.resultCode)
    }

    private val installLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleInstallResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up overlay window for guidance
        setupOverlayWindow()

        setContentView(R.layout.activity_installation_guide)

        // Extract coordinator data from intent
        coordinatorUrl = intent.getStringExtra(EXTRA_COORDINATOR_URL)
        apkUrl = intent.getStringExtra(EXTRA_APK_URL)
        pairingToken = intent.getStringExtra(EXTRA_PAIRING_TOKEN)

        initializeViews()
        startInstallationFlow()
    }

    private fun setupOverlayWindow() {
        // Set overlay permissions for guidance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                       WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        stepIndicator = findViewById(R.id.stepIndicator)
        guideImage = findViewById(R.id.guideImage)
        actionButton = findViewById(R.id.actionButton)
        skipButton = findViewById(R.id.skipButton)

        actionButton.setOnClickListener { handleActionButtonClick() }
        skipButton.setOnClickListener { skipCurrentStep() }

        updateUI()
    }

    private fun startInstallationFlow() {
        lifecycleScope.launch {
            if (checkUnknownSourcesPermission()) {
                // Permission already granted, proceed to download
                currentStep = InstallationStep.DOWNLOAD_APK
                updateUI()
                downloadApk()
            } else {
                // Need to guide user to enable unknown sources
                showUnknownSourcesGuide()
            }
        }
    }

    private fun showUnknownSourcesGuide() {
        currentStep = InstallationStep.UNKNOWN_SOURCES
        updateUI()

        // Show visual overlay highlighting where to tap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showOverlayGuide()
        } else {
            showLegacyGuide()
        }
    }

    private fun showOverlayGuide() {
        stepIndicator.text = getString(R.string.install_step_unknown_sources)
        statusText.text = getString(R.string.unknown_sources_guide_description)
        guideImage.setImageResource(R.drawable.ic_unknown_sources_guide)

        actionButton.text = getString(R.string.open_settings)
        actionButton.visibility = View.VISIBLE

        // Create visual overlay guide
        showVisualOverlay()
    }

    private fun showLegacyGuide() {
        stepIndicator.text = getString(R.string.install_step_legacy)
        statusText.text = getString(R.string.legacy_install_guide)
        guideImage.setImageResource(R.drawable.ic_legacy_install)

        actionButton.text = getString(R.string.open_security_settings)
        actionButton.visibility = View.VISIBLE
    }

    private fun showVisualOverlay() {
        // This would show a translucent overlay with arrows and highlights
        // pointing to the settings that need to be enabled

        // Implementation would depend on Android version
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                showAndroid11PlusGuide()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                showAndroid8PlusGuide()
            }
            else -> {
                showLegacyGuide()
            }
        }
    }

    private fun showAndroid11PlusGuide() {
        // Guide for Android 11+ where unknown sources is per-app
        stepIndicator.text = getString(R.string.install_step_android11)
        statusText.text = getString(R.string.android11_install_description)
        guideImage.setImageResource(R.drawable.ic_android11_guide)
    }

    private fun showAndroid8PlusGuide() {
        // Guide for Android 8-10
        stepIndicator.text = getString(R.string.install_step_android8)
        statusText.text = getString(R.string.android8_install_description)
        guideImage.setImageResource(R.drawable.ic_android8_guide)
    }

    private fun checkUnknownSourcesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8+ - check install packages permission
            packageManager.canRequestPackageInstalls()
        } else {
            // Android 7 and below - check legacy unknown sources setting
            Settings.Global.getInt(contentResolver, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    private fun handleActionButtonClick() {
        when (currentStep) {
            InstallationStep.UNKNOWN_SOURCES -> {
                openUnknownSourcesSettings()
            }
            InstallationStep.DOWNLOAD_APK -> {
                downloadApk()
            }
            InstallationStep.INSTALL_APK -> {
                installApk()
            }
            InstallationStep.PAIRING -> {
                startPairing()
            }
            InstallationStep.COMPLETE -> {
                finishInstallation()
            }
        }
    }

    private fun openUnknownSourcesSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        unknownSourcesLauncher.launch(intent)
    }

    private fun handleUnknownSourcesResult(resultCode: Int) {
        if (checkUnknownSourcesPermission()) {
            // Permission granted
            currentStep = InstallationStep.DOWNLOAD_APK
            updateUI()
            downloadApk()
        } else {
            // Permission denied
            statusText.text = getString(R.string.unknown_sources_denied)
            actionButton.text = getString(R.string.try_again)
            showRetryOption()
        }
    }

    private fun downloadApk() {
        currentStep = InstallationStep.DOWNLOAD_APK
        updateUI()

        val url = apkUrl ?: return

        lifecycleScope.launch {
            try {
                stepIndicator.text = getString(R.string.install_step_download)
                statusText.text = getString(R.string.downloading_apk)
                progressBar.visibility = View.VISIBLE

                // Start download service
                val downloadIntent = Intent(this@InstallationGuideActivity, AutoInstallerService::class.java).apply {
                    action = AutoInstallerService.ACTION_DOWNLOAD_APK
                    putExtra(AutoInstallerService.EXTRA_APK_URL, url)
                    putExtra(AutoInstallerService.EXTRA_COORDINATOR_URL, coordinatorUrl)
                    putExtra(AutoInstallerService.EXTRA_PAIRING_TOKEN, pairingToken)
                }

                startService(downloadIntent)

                // Simulate download progress (real implementation would use actual download progress)
                for (progress in 0..100 step 10) {
                    progressBar.progress = progress
                    statusText.text = getString(R.string.download_progress, progress)
                    delay(200)
                }

                currentStep = InstallationStep.INSTALL_APK
                updateUI()
                installApk()

            } catch (e: Exception) {
                statusText.text = getString(R.string.download_failed, e.message)
                showRetryOption()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun installApk() {
        currentStep = InstallationStep.INSTALL_APK
        updateUI()

        lifecycleScope.launch {
            try {
                val apkFile = File(getExternalFilesDir(null), "misa-latest.apk")

                if (!apkFile.exists()) {
                    throw Exception("APK file not found")
                }

                stepIndicator.text = getString(R.string.install_step_install)
                statusText.text = getString(R.string.installing_apk)
                progressBar.visibility = View.VISIBLE

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                installLauncher.launch(installIntent)

            } catch (e: Exception) {
                statusText.text = getString(R.string.install_failed, e.message)
                showRetryOption()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun handleInstallResult(resultCode: Int) {
        if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
            // Installation successful
            currentStep = InstallationStep.PAIRING
            updateUI()
            startPairing()
        } else {
            // Installation failed
            statusText.text = getString(R.string.installation_cancelled)
            showRetryOption()
        }
    }

    private fun startPairing() {
        currentStep = InstallationStep.PAIRING
        updateUI()

        stepIndicator.text = getString(R.string.install_step_pairing)
        statusText.text = getString(R.string.pairing_with_coordinator)
        guideImage.setImageResource(R.drawable.ic_pairing_success)

        // Start pairing process
        val pairingIntent = Intent(this, AutoInstallerService::class.java).apply {
            action = AutoInstallerService.ACTION_START_PAIRING
            putExtra(AutoInstallerService.EXTRA_COORDINATOR_URL, coordinatorUrl)
            putExtra(AutoInstallerService.EXTRA_PAIRING_TOKEN, pairingToken)
        }

        startService(pairingIntent)

        // Auto-advance after successful pairing
        lifecycleScope.launch {
            delay(3000)
            currentStep = InstallationStep.COMPLETE
            updateUI()
        }
    }

    private fun finishInstallation() {
        currentStep = InstallationStep.COMPLETE
        updateUI()

        stepIndicator.text = getString(R.string.install_step_complete)
        statusText.text = getString(R.string.installation_complete)
        guideImage.setImageResource(R.drawable.ic_installation_complete)

        actionButton.text = getString(R.string.launch_app)
        actionButton.setOnClickListener {
            launchMainActivity()
        }
    }

    private fun launchMainActivity() {
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (mainIntent != null) {
            startActivity(mainIntent)
        }
        finish()
    }

    private fun skipCurrentStep() {
        when (currentStep) {
            InstallationStep.UNKNOWN_SOURCES -> {
                if (checkUnknownSourcesPermission()) {
                    currentStep = InstallationStep.DOWNLOAD_APK
                    updateUI()
                    downloadApk()
                } else {
                    finish() // Can't proceed without permission
                }
            }
            InstallationStep.DOWNLOAD_APK -> {
                // Skip download, try to install existing APK
                currentStep = InstallationStep.INSTALL_APK
                updateUI()
                installApk()
            }
            InstallationStep.PAIRING -> {
                // Skip pairing, go to completion
                currentStep = InstallationStep.COMPLETE
                updateUI()
            }
            else -> {
                finish()
            }
        }
    }

    private fun showRetryOption() {
        actionButton.text = getString(R.string.retry)
        actionButton.visibility = View.VISIBLE
        skipButton.visibility = View.VISIBLE
    }

    private fun updateUI() {
        when (currentStep) {
            InstallationStep.UNKNOWN_SOURCES -> {
                progressBar.visibility = View.GONE
                guideImage.visibility = View.VISIBLE
                actionButton.visibility = View.VISIBLE
                skipButton.visibility = View.GONE
            }
            InstallationStep.DOWNLOAD_APK -> {
                progressBar.visibility = View.VISIBLE
                guideImage.visibility = View.GONE
                actionButton.visibility = View.GONE
                skipButton.visibility = View.VISIBLE
            }
            InstallationStep.INSTALL_APK -> {
                progressBar.visibility = View.VISIBLE
                guideImage.visibility = View.GONE
                actionButton.visibility = View.GONE
                skipButton.visibility = View.VISIBLE
            }
            InstallationStep.PAIRING -> {
                progressBar.visibility = View.GONE
                guideImage.visibility = View.VISIBLE
                actionButton.visibility = View.GONE
                skipButton.visibility = View.VISIBLE
            }
            InstallationStep.COMPLETE -> {
                progressBar.visibility = View.GONE
                guideImage.visibility = View.VISIBLE
                actionButton.visibility = View.VISIBLE
                skipButton.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if unknown sources permission was granted while activity was in background
        if (currentStep == InstallationStep.UNKNOWN_SOURCES && checkUnknownSourcesPermission()) {
            currentStep = InstallationStep.DOWNLOAD_APK
            updateUI()
            downloadApk()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up any overlay views
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.clearFlags(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }
    }

    companion object {
        private const val TAG = "InstallationGuideActivity"

        const val EXTRA_COORDINATOR_URL = "coordinator_url"
        const val EXTRA_APK_URL = "apk_url"
        const val EXTRA_PAIRING_TOKEN = "pairing_token"

        enum class InstallationStep {
            UNKNOWN_SOURCES,
            DOWNLOAD_APK,
            INSTALL_APK,
            PAIRING,
            COMPLETE
        }

        fun createIntent(
            context: Context,
            coordinatorUrl: String? = null,
            apkUrl: String? = null,
            pairingToken: String? = null
        ): Intent {
            return Intent(context, InstallationGuideActivity::class.java).apply {
                putExtra(EXTRA_COORDINATOR_URL, coordinatorUrl)
                putExtra(EXTRA_APK_URL, apkUrl)
                putExtra(EXTRA_PAIRING_TOKEN, pairingToken)
            }
        }
    }
}