package com.cyberlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cyberlens.app.domain.model.IpInfo
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.IpIntelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpIntelScreen(navController: NavController, vm: IpIntelViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val input by vm.inputText.collectAsStateWithLifecycle()
    val consent by vm.consentGiven.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("IP Intelligence", fontFamily = FontFamily.Monospace, color = CyberBlue) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberBlue)
                    }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanTextField(
                value = input,
                onValueChange = vm::onInputChange,
                placeholder = "8.8.8.8",
                label = "Target IP Address"
            )
            ConsentCheckbox(checked = consent, onCheckedChange = vm::onConsentChange)
            ScanButton(
                text = "ANALYZE IP",
                onClick = vm::analyze,
                enabled = consent && input.isNotBlank() && uiState !is UiState.Loading
            )

            when (val s = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Error -> ErrorCard(s.message)
                is UiState.Success -> IpResultCard(s.data)
                else -> {}
            }
        }
    }
}

@Composable
fun IpResultCard(info: IpInfo) {
    CyberCard(glowColor = CyberBlue) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(info.ip, color = CyberBlue, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            RiskBadge(info.riskLevel)
        }
        Spacer(Modifier.height(12.dp))
        SectionHeader("GEOLOCATION")
        InfoRow("City", info.city ?: "Unknown")
        InfoRow("Region", info.region ?: "Unknown")
        InfoRow("Country", info.country ?: "Unknown")
        InfoRow("Timezone", info.timezone ?: "Unknown")
        InfoRow("Coordinates", if (info.latitude != null) "${info.latitude}, ${info.longitude}" else "N/A")
        SectionHeader("NETWORK")
        InfoRow("ISP", info.isp ?: info.org ?: "Unknown")
        InfoRow("ASN", info.asn ?: "Unknown")
        InfoRow("Organization", info.org ?: "Unknown")
        InfoRow("Hostname", info.hostname ?: "N/A")
        if (info.openPorts.isNotEmpty()) {
            SectionHeader("OPEN PORTS", CyberOrange)
            TerminalText(info.openPorts.joinToString(", "))
        }
        if (info.tags.isNotEmpty()) {
            SectionHeader("TAGS", CyberOrange)
            TerminalText(info.tags.joinToString(", "))
        }
        if (info.vulns.isNotEmpty()) {
            SectionHeader("VULNERABILITIES", CyberRed)
            info.vulns.take(5).forEach { vuln ->
                Text("• $vuln", color = CyberRed, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
