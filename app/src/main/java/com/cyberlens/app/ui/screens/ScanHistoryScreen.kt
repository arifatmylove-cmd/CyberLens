package com.cyberlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cyberlens.app.data.local.ScanEntity
import com.cyberlens.app.domain.model.ScanType
import com.cyberlens.app.ui.components.ScanTextField
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(navController: NavController, vm: HistoryViewModel = hiltViewModel()) {
    val scans by vm.scans.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History", color = CyberWhite) },
            text = { Text("Delete all scan history? This cannot be undone.", color = CyberLightGray) },
            confirmButton = {
                Button(onClick = { vm.clearAll(); showClearDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = CyberRed)) {
                    Text("Delete All")
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel", color = CyberBlue) } },
            containerColor = CyberSurface
        )
    }

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Scan History", fontFamily = FontFamily.Monospace, color = CyberLightGray) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberLightGray) } },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, null, tint = CyberRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(CyberBg).padding(padding)) {
            Box(Modifier.padding(16.dp)) {
                ScanTextField(value = query, onValueChange = vm::onSearchChange, placeholder = "Search scans...")
            }
            if (scans.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, tint = CyberGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No scans yet", color = CyberGray, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scans, key = { it.id }) { scan ->
                        ScanHistoryRow(scan, onDelete = { vm.deleteScan(scan.id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ScanHistoryRow(scan: ScanEntity, onDelete: () -> Unit) {
    val typeColor = when (scan.scanType) {
        ScanType.IP_INTEL -> CyberBlue
        ScanType.DOMAIN_ANALYSIS -> CyberGreen
        ScanType.USERNAME_OSINT -> CyberPurple
        ScanType.THREAT_INTEL -> CyberRed
        ScanType.NMAP, ScanType.PORT_SCAN, ScanType.BANNER_GRAB, ScanType.WAF_DETECT -> CyberOrange
        else -> CyberGray
    }
    val riskColor = when (scan.riskLevel) {
        "SAFE" -> CyberGreen; "SUSPICIOUS" -> CyberOrange; "DANGEROUS" -> CyberRed
        else -> CyberGray
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, typeColor.copy(0.3f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(scan.scanType.name.replace("_", " "), color = typeColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Text(scan.target, color = CyberWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(scan.timestamp)),
                    color = CyberGray, fontSize = 10.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("● ${scan.riskLevel}", color = riskColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = CyberGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
