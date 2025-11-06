package com.misa.ai

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * MISA.AI Application Class
 *
 * Main application class that initializes core services, dependency injection,
 * and sets up the application-wide configuration.
 */
@HiltAndroidApp
class MisaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        initializeLogging()

        // Initialize core services
        initializeServices()

        // Initialize crash reporting
        initializeCrashReporting()

        Timber.i("MISA.AI Application started")
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        // Initialize any base context requirements here
        Timber.d("Application base context attached")
    }

    /**
     * Provide WorkManager configuration
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()

    /**
     * Initialize logging infrastructure
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, you might want to use a crash reporting tree
            Timber.plant(ReleaseTree())
        }
    }

    /**
     * Initialize core application services
     */
    private fun initializeServices() {
        applicationScope.launch {
            try {
                // Initialize notification channels
                initializeNotificationChannels()

                // Initialize security settings
                initializeSecuritySettings()

                // Initialize device services
                initializeDeviceServices()

                // Initialize kernel connection
                initializeKernelConnection()

                Timber.i("All core services initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize core services")
            }
        }
    }

    /**
     * Initialize notification channels for Android O and above
     */
    private fun initializeNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Implementation would create notification channels
            Timber.d("Notification channels initialized")
        }
    }

    /**
     * Initialize security and encryption settings
     */
    private fun initializeSecuritySettings() {
        // Implementation would initialize security manager
        Timber.d("Security settings initialized")
    }

    /**
     * Initialize device-specific services
     */
    private fun initializeDeviceServices() {
        // Implementation would initialize device services
        Timber.d("Device services initialized")
    }

    /**
     * Initialize connection to MISA kernel
     */
    private fun initializeKernelConnection() {
        // Implementation would establish WebSocket connection to kernel
        Timber.d("Kernel connection initialized")
    }

    /**
     * Initialize crash reporting and analytics
     */
    private fun initializeCrashReporting() {
        // Implementation would initialize crash reporting service
        if (!BuildConfig.DEBUG) {
            // Initialize production crash reporting
            Timber.d("Crash reporting initialized")
        }
    }

    /**
     * Custom release tree that only logs to crash reporting
     */
    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // Only log WARN, ERROR, and WTF to crash reporting
            return !(priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG || priority == android.util.Log.INFO)
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Send to crash reporting service
            if (t != null) {
                // Report exception to crash reporting service
            }
        }
    }
}

/**
 * Application companion object for global access
 */
object MisaApp {
    lateinit var instance: MisaApplication
        private set

    val context: Context
        get() = instance.applicationContext

    val isDebug: Boolean
        get() = BuildConfig.DEBUG

    val versionName: String
        get() = BuildConfig.VERSION_NAME

    val versionCode: Int
        get() = BuildConfig.VERSION_CODE
}