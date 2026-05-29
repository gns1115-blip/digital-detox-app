package com.example.instagramdetector.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.instagramdetector.overlay.UsageDuration
import com.example.instagramdetector.util.AppInfo
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DetectorTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToApps = { navController.navigate("apps") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        composable("apps") {
            AppsScreen(
                onBack = { navController.popBackStack() },
                onLaunchRequest = { appInfo ->
                    val encodedName = URLEncoder.encode(appInfo.name, StandardCharsets.UTF_8.toString())
                    navController.navigate("intentional_launch/${appInfo.packageName}/$encodedName")
                }
            )
        }
        
        composable(
            route = "intentional_launch/{packageName}/{appName}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val encodedName = backStackEntry.arguments?.getString("appName") ?: ""
            val appName = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString())
            
            val context = androidx.compose.ui.platform.LocalContext.current
            
            IntentionalLaunchScreen(
                appName = appName,
                onCancel = { navController.popBackStack() },
                onConfirm = { duration ->
                    // Launch logic
                    launchTargetApp(context, packageName, duration)
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToInfo = { navController.navigate("info") }
            )
        }

        composable("history") {
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.instagramdetector.InstagramDetectorApplication
            val recentRecords by app.overlaySelectionRepository.recentRecords
                .collectAsStateWithLifecycle(initialValue = emptyList())
            HistoryScreen(
                records = recentRecords,
                onBack = { navController.popBackStack() }
            )
        }

        composable("info") {
            InstructionScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun launchTargetApp(
    context: android.content.Context, 
    packageName: String, 
    duration: UsageDuration
) {
    // 1. Grant Session
    com.example.instagramdetector.overlay.AppSessionGate.grantSession(
        packageName, 
        com.example.instagramdetector.overlay.OverlayReason.BOREDOM, 
        duration
    )
    
    // 2. Start Timer
    com.example.instagramdetector.service.UsageTimerController.start(context, packageName, duration.minutes)
    
    // 3. Launch Intent
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }
}
