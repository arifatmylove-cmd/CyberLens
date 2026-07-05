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
import com.cyberlens.app.domain.model.DomainInfo
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.DomainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScannerScreen(navController: NavController, vm: DomainViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val input by vm.inputText.collectAsStateWithLifecycle()
    val consent by vm.consentGiven.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Website Scanner", fontFamily = FontFamily.Monospace, color = CyberYellow) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberYellow) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(CyberBg).padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanTextField(value = input, onValueChange = vm::onInputChange, placeholder = "https://example.com", label = "Target Website")
            ConsentCheckbox(checked = consent, onCheckedChange = vm::onConsentChange)
            ScanButton("SCAN WEBSITE", vm::analyze, consent && input.isNotBlank() && uiState !is UiState.Loading, CyberYellow)

            when (val s = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Error -> ErrorCard(s.message)
                is UiState.Success -> WebScanResult(s.data)
                else -> {}
            }
        }
    }
}

@Composable
fun WebScanResult(info: DomainInfo) {
    CyberCard(glowColor = CyberYellow) {
        Text(info.domain, color = CyberYellow, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        SecurityScoreBar(info.securityScore)
        Spacer(Modifier.height(12.dp))
        SectionHeader("SECURITY HEADERS", CyberYellow)
        SecurityCheck("HTTPS/TLS", info.hasHttps)
        SecurityCheck("HSTS (HTTP Strict Transport Security)", info.hasHsts)
        SecurityCheck("Content-Security-Policy", info.hasCsp)
        SecurityCheck("X-Frame-Options (Clickjacking protection)", info.hasXFrameOptions)
        SectionHeader("SSL CERTIFICATE", CyberYellow)
        InfoRow("Valid", if (info.sslValid == true) "Yes" else "No/Unknown", if (info.sslValid == true) CyberGreen else CyberRed)
        InfoRow("Expiry", info.sslExpiry ?: "N/A")
        SectionHeader("SERVER INFO", CyberYellow)
        InfoRow("Server", info.serverInfo ?: "Not disclosed")
        InfoRow("IP Address", info.ipAddress ?: "N/A")
        if (info.technologies.isNotEmpty()) InfoRow("Technologies", info.technologies.joinToString(", "))
        SectionHeader("RESPONSE HEADERS", CyberYellow)
        info.httpHeaders.entries.take(10).forEach { (k, v) ->
            InfoRow(k, v.take(80))
        }
        Spacer(Modifier.height(8.dp))
        val score = info.securityScore
        val recommendation = when {
            score >= 70 -> "Good security posture. Consider adding remaining missing headers."
            score >= 40 -> "Moderate risk. Enable HTTPS, HSTS and CSP headers to improve."
            else -> "Poor security. Immediately enable HTTPS and security headers."
        }
        Text("Recommendation: $recommendation", color = if (score >= 70) CyberGreen else if (score >= 40) CyberOrange else CyberRed, style = MaterialTheme.typography.bodyMedium)
    }
}
