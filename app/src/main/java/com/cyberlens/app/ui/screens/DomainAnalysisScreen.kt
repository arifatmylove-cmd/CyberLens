package com.cyberlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cyberlens.app.domain.model.DomainInfo
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.DomainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainAnalysisScreen(navController: NavController, vm: DomainViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val input by vm.inputText.collectAsStateWithLifecycle()
    val consent by vm.consentGiven.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Domain Analysis", fontFamily = FontFamily.Monospace, color = CyberGreen) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberGreen)
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
            ScanTextField(value = input, onValueChange = vm::onInputChange, placeholder = "example.com", label = "Domain or URL")
            ConsentCheckbox(checked = consent, onCheckedChange = vm::onConsentChange)
            ScanButton("ANALYZE DOMAIN", vm::analyze, consent && input.isNotBlank() && uiState !is UiState.Loading, CyberGreen)

            when (val s = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Error -> ErrorCard(s.message)
                is UiState.Success -> DomainResultCard(s.data)
                else -> {}
            }
        }
    }
}

@Composable
fun DomainResultCard(info: DomainInfo) {
    CyberCard(glowColor = CyberGreen) {
        Text(info.domain, color = CyberGreen, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        SecurityScoreBar(info.securityScore)
        Spacer(Modifier.height(12.dp))
        SectionHeader("DOMAIN INFO", CyberGreen)
        InfoRow("IP Address", info.ipAddress ?: "N/A")
        InfoRow("Server", info.serverInfo ?: "Unknown")
        if (info.technologies.isNotEmpty()) InfoRow("Stack", info.technologies.joinToString(", "))
        SectionHeader("SECURITY CHECKS", CyberGreen)
        SecurityCheck("HTTPS Enabled", info.hasHttps)
        SecurityCheck("HSTS Header", info.hasHsts)
        SecurityCheck("Content-Security-Policy", info.hasCsp)
        SecurityCheck("X-Frame-Options", info.hasXFrameOptions)
        SecurityCheck("SSL Valid", info.sslValid ?: false)
        if (info.sslExpiry != null) InfoRow("SSL Expiry", info.sslExpiry)
        if (info.dnsRecords.isNotEmpty()) {
            SectionHeader("DNS RECORDS", CyberGreen)
            info.dnsRecords.take(10).forEach { record ->
                InfoRow(record.type, record.value)
            }
        }
        if (!info.whoisData.isNullOrBlank()) {
            SectionHeader("WHOIS (excerpt)", CyberGreen)
            TerminalText(info.whoisData.take(600))
        }
    }
}

@Composable
fun SecurityCheck(label: String, pass: Boolean) {
    InfoRow(
        label,
        if (pass) "✓ PASS" else "✗ FAIL",
        if (pass) CyberGreen else CyberRed
    )
}
