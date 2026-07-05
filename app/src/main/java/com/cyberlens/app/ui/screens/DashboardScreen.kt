package com.cyberlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cyberlens.app.ui.components.DashboardCard
import com.cyberlens.app.ui.navigation.Routes
import com.cyberlens.app.ui.theme.*

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val items = listOf(
        DashboardItem("IP Intelligence", "Geo, ASN, ISP & risk", Icons.Default.Language, CyberBlue, Routes.IP_INTEL),
        DashboardItem("Domain Analysis", "WHOIS, DNS & SSL", Icons.Default.Dns, CyberGreen, Routes.DOMAIN),
        DashboardItem("Username OSINT", "20+ platform lookup", Icons.Default.Person, CyberPurple, Routes.USERNAME),
        DashboardItem("Image Reverse", "Reverse image search", Icons.Default.ImageSearch, CyberOrange, Routes.IMAGE_REVERSE),
        DashboardItem("Web Scanner", "Security headers & SSL", Icons.Default.Security, CyberYellow, Routes.WEB_SCANNER),
        DashboardItem("Threat Intel", "Reputation & blacklist", Icons.Default.BugReport, CyberRed, Routes.THREAT_INTEL),
        DashboardItem("Scan History", "View previous scans", Icons.Default.History, CyberLightGray, Routes.HISTORY),
        DashboardItem("Red Team", "Nmap, ports, WAF, CVE", Icons.Default.Terminal, CyberRed, Routes.RED_TEAM)
    )

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "CYBERLENS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = CyberBlue
                        )
                        Text(
                            "OSINT Intelligence Platform",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberGray
                        )
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Secure",
                        tint = CyberGreen,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(CyberBg)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Status Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip("OSINT", CyberBlue)
                StatusChip("DEFENSIVE", CyberGreen)
                StatusChip("v1.0", CyberGray)
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    DashboardCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        accentColor = item.color,
                        onClick = { navController.navigate(item.route) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legal disclaimer
            Text(
                "⚠ For authorized use only. Always obtain written permission before scanning external targets.",
                color = CyberGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun StatusChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
