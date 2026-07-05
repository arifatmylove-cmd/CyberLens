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
import com.cyberlens.app.domain.model.ThreatInfo
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.ThreatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatIntelScreen(navController: NavController, vm: ThreatViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val input by vm.inputText.collectAsStateWithLifecycle()
    val consent by vm.consentGiven.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Threat Intelligence", fontFamily = FontFamily.Monospace, color = CyberRed) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberRed) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(CyberBg).padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanTextField(value = input, onValueChange = vm::onInputChange, placeholder = "8.8.8.8 or example.com", label = "IP or Domain")
            ConsentCheckbox(checked = consent, onCheckedChange = vm::onConsentChange)
            ScanButton("CHECK THREAT", vm::check, consent && input.isNotBlank() && uiState !is UiState.Loading, CyberRed)

            CyberCard(glowColor = CyberGray) {
                Text("Powered by VirusTotal. Add a free API key in the source code (AppModule.kt) for full results. Without a key, basic reputation data is shown.", color = CyberGray, style = MaterialTheme.typography.bodyMedium)
            }

            when (val s = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Error -> ErrorCard(s.message)
                is UiState.Success -> ThreatResultCard(s.data)
                else -> {}
            }
        }
    }
}

@Composable
fun ThreatResultCard(info: ThreatInfo) {
    CyberCard(glowColor = CyberRed) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(info.target, color = CyberWhite, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
            RiskBadge(info.riskLevel)
        }
        Spacer(Modifier.height(12.dp))
        SectionHeader("REPUTATION", CyberRed)
        InfoRow("Type", if (info.isIp) "IP Address" else "Domain")
        InfoRow("Malicious", info.malicious.toString(), if (info.malicious > 0) CyberRed else CyberGreen)
        InfoRow("Suspicious", info.suspicious.toString(), if (info.suspicious > 0) CyberOrange else CyberGreen)
        InfoRow("Harmless", info.harmless.toString(), CyberGreen)
        if (info.country != null) InfoRow("Country", info.country)
        if (info.categories.isNotEmpty()) {
            SectionHeader("CATEGORIES", CyberRed)
            info.categories.entries.take(5).forEach { (vendor, cat) -> InfoRow(vendor, cat) }
        }
        if (info.tags.isNotEmpty()) {
            SectionHeader("TAGS")
            TerminalText(info.tags.joinToString(", "))
        }
    }
}
