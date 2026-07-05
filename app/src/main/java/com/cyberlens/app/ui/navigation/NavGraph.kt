package com.cyberlens.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyberlens.app.ui.screens.*
import com.cyberlens.app.ui.screens.redteam.RedTeamDashboardScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val IP_INTEL = "ip_intel"
    const val DOMAIN = "domain"
    const val USERNAME = "username"
    const val IMAGE_REVERSE = "image_reverse"
    const val WEB_SCANNER = "web_scanner"
    const val THREAT_INTEL = "threat_intel"
    const val HISTORY = "history"
    const val RED_TEAM = "red_team"
}

@Composable
fun CyberLensNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(Routes.IP_INTEL) {
            IpIntelScreen(navController = navController)
        }
        composable(Routes.DOMAIN) {
            DomainAnalysisScreen(navController = navController)
        }
        composable(Routes.USERNAME) {
            UsernameOsintScreen(navController = navController)
        }
        composable(Routes.IMAGE_REVERSE) {
            ImageReverseScreen(navController = navController)
        }
        composable(Routes.WEB_SCANNER) {
            WebScannerScreen(navController = navController)
        }
        composable(Routes.THREAT_INTEL) {
            ThreatIntelScreen(navController = navController)
        }
        composable(Routes.HISTORY) {
            ScanHistoryScreen(navController = navController)
        }
        composable(Routes.RED_TEAM) {
            RedTeamDashboardScreen(navController = navController)
        }
    }
}
