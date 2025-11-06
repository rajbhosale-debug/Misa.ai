package com.misa.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.misa.ai.ui.auth.AuthScreen
import com.misa.ai.ui.auth.AuthViewModel
import com.misa.ai.ui.home.HomeScreen
import com.misa.ai.ui.home.HomeViewModel
import com.misa.ai.ui.navigation.MisaNavigation
import com.misa.ai.ui.navigation.Route
import com.misa.ai.ui.theme.MisaAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Activity for MISA.AI Android Application
 *
 * This is the entry point activity that sets up the Compose navigation,
 * handles authentication state, and manages the overall app UI structure.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var misaNavigation: MisaNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set content with Compose UI
        setContent {
            MisaApp()
        }

        // Initialize app in background
        initializeApp()

        Timber.d("MainActivity created")
    }

    /**
     * Initialize application components
     */
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                // Initialize any app-level components
                Timber.i("App initialization completed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize app")
            }
        }
    }
}

/**
 * Main Compose App
 */
@Composable
fun MisaApp() {
    val navController = rememberNavController()
    val systemInDarkTheme = isSystemInDarkTheme()

    MisaAITheme(
        darkTheme = systemInDarkTheme
    ) {
        Surface(
            modifier = Modifier,
            color = MaterialTheme.colorScheme.background
        ) {
            // Navigation setup
            NavHost(
                navController = navController,
                startDestination = Route.Splash.route
            ) {
                // Splash screen
                composable(Route.Splash.route) {
                    // Splash screen that checks authentication state
                    SplashScreen(navController = navController)
                }

                // Authentication flow
                composable(Route.Auth.route) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = {
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Main app screens
                composable(Route.Home.route) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToApp = { appRoute ->
                            navController.navigate(appRoute)
                        },
                        onLogout = {
                            navController.navigate(Route.Auth.route) {
                                popUpTo(Route.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Calendar app
                composable(Route.Calendar.route) {
                    CalendarScreen()
                }

                // Notes app
                composable(Route.Notes.route) {
                    NotesScreen()
                }

                // TaskFlow app
                composable(Route.TaskFlow.route) {
                    TaskFlowScreen()
                }

                // FileHub app
                composable(Route.FileHub.route) {
                    FileHubScreen()
                }

                // Focus app
                composable(Route.Focus.route) {
                    FocusScreen()
                }

                // Persona Studio app
                composable(Route.Persona.route) {
                    PersonaScreen()
                }

                // WebIQ app
                composable(Route.WebIQ.route) {
                    WebIQScreen()
                }

                // ChatSync app
                composable(Route.ChatSync.route) {
                    ChatSyncScreen()
                }

                // Meet app
                composable(Route.Meet.route) {
                    MeetScreen()
                }

                // Home IoT app
                composable(Route.IotHome.route) {
                    IotHomeScreen()
                }

                // PowerSense app
                composable(Route.PowerSense.route) {
                    PowerSenseScreen()
                }

                // WorkSuite app
                composable(Route.WorkSuite.route) {
                    WorkSuiteScreen()
                }

                // DevHub app
                composable(Route.DevHub.route) {
                    DevHubScreen()
                }

                // Store app
                composable(Route.Store.route) {
                    StoreScreen()
                }

                // Vault app
                composable(Route.Vault.route) {
                    VaultScreen()
                }

                // BioLink app
                composable(Route.BioLink.route) {
                    BioLinkScreen()
                }

                // Workflow AI app
                composable(Route.Workflow.route) {
                    WorkflowScreen()
                }

                // Settings
                composable(Route.Settings.route) {
                    SettingsScreen()
                }

                // Device management
                composable(Route.Devices.route) {
                    DevicesScreen()
                }

                // Remote desktop
                composable(Route.RemoteDesktop.route) {
                    RemoteDesktopScreen()
                }

                // Profile
                composable(Route.Profile.route) {
                    ProfileScreen()
                }
            }
        }
    }
}

/**
 * Splash Screen
 */
@Composable
fun SplashScreen(navController: androidx.navigation.NavController) {
    // Check authentication state and navigate accordingly
    // This is a placeholder - implementation would check actual auth state

    androidx.compose.foundation.layout.Box(
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }

    // Simulate splash screen delay
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)

        // Check if user is authenticated
        val isAuthenticated = false // Replace with actual auth check

        if (isAuthenticated) {
            navController.navigate(Route.Home.route) {
                popUpTo(Route.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Route.Auth.route) {
                popUpTo(Route.Splash.route) { inclusive = true }
            }
        }
    }
}

// Placeholder screens for navigation
@Composable
fun CalendarScreen() {
    androidx.compose.material3.Text("Calendar App")
}

@Composable
fun NotesScreen() {
    androidx.compose.material3.Text("Notes App")
}

@Composable
fun TaskFlowScreen() {
    androidx.compose.material3.Text("TaskFlow App")
}

@Composable
fun FileHubScreen() {
    androidx.compose.material3.Text("FileHub App")
}

@Composable
fun FocusScreen() {
    androidx.compose.material3.Text("Focus App")
}

@Composable
fun PersonaScreen() {
    androidx.compose.material3.Text("Persona Studio App")
}

@Composable
fun WebIQScreen() {
    androidx.compose.material3.Text("WebIQ App")
}

@Composable
fun ChatSyncScreen() {
    androidx.compose.material3.Text("ChatSync App")
}

@Composable
fun MeetScreen() {
    androidx.compose.material3.Text("Meet App")
}

@Composable
fun IotHomeScreen() {
    androidx.compose.material3.Text("Home IoT App")
}

@Composable
fun PowerSenseScreen() {
    androidx.compose.material3.Text("PowerSense App")
}

@Composable
fun WorkSuiteScreen() {
    androidx.compose.material3.Text("WorkSuite App")
}

@Composable
fun DevHubScreen() {
    androidx.compose.material3.Text("DevHub App")
}

@Composable
fun StoreScreen() {
    androidx.compose.material3.Text("Store App")
}

@Composable
fun VaultScreen() {
    androidx.compose.material3.Text("Vault App")
}

@Composable
fun BioLinkScreen() {
    androidx.compose.material3.Text("BioLink App")
}

@Composable
fun WorkflowScreen() {
    androidx.compose.material3.Text("Workflow AI App")
}

@Composable
fun SettingsScreen() {
    androidx.compose.material3.Text("Settings")
}

@Composable
fun DevicesScreen() {
    androidx.compose.material3.Text("Device Management")
}

@Composable
fun RemoteDesktopScreen() {
    androidx.compose.material3.Text("Remote Desktop")
}

@Composable
fun ProfileScreen() {
    androidx.compose.material3.Text("Profile")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MisaAITheme {
        MisaApp()
    }
}