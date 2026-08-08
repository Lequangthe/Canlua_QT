package com.quangthe.canluav3

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quangthe.canluav3.ui.screens.*
import com.quangthe.canluav3.ui.theme.CANLUAV3Theme
import com.quangthe.canluav3.ui.theme.provider
import com.quangthe.canluav3.viewmodel.RiceViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RiceViewModel = viewModel()
            val appSettings by viewModel.appSettings.collectAsState()
            val context = LocalContext.current
            
            // Log to check if provider is available
            LaunchedEffect(Unit) {
                try {
                    val available = provider.isAvailableOnDevice(context)
                    Log.d("FontDebug", "Google Font Provider available: $available")
                } catch (e: Exception) {
                    Log.e("FontDebug", "Error checking provider availability", e)
                }
            }

            val handler = CoroutineExceptionHandler { _, throwable ->
                Log.e("FontDebug", "Font loading error: ${throwable.message}", throwable)
            }

            CompositionLocalProvider(
                LocalFontFamilyResolver provides createFontFamilyResolver(LocalContext.current, handler)
            ) {
                CANLUAV3Theme(
                    fontScale = appSettings.globalFontScale,
                    fontFamilyName = appSettings.fontFamilyName
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToTrash = { navController.navigate("trash") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onTicketClick = { id -> navController.navigate("detail/$id") }
                            )
                        }
                        composable("detail/{ticketId}") { backStackEntry ->
                            val ticketId = backStackEntry.arguments?.getString("ticketId")?.toInt() ?: 0
                            TicketDetailScreen(
                                viewModel = viewModel,
                                ticketId = ticketId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("trash") {
                            TrashScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
