package com.cyberlens.app.ui.screens.redteam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cyberlens.app.domain.model.*
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.redteam.RedTeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedTeamDashboardScreen(navController: NavController, vm: RedTeamViewModel = hiltViewModel()) {
    val consent by vm.consentGiven.collectAsStateWithLifecycle()
    val nmapState by vm.nmapState.collectAsStateWithLifecycle()
    val portState by vm.portState.collectAsStateWithLifecycle()
    val bannerState by vm.bannerState.collectAsStateWithLifecycle()
    val wafState by vm.wafState.collectAsStateWithLifecycle()
    val cveState by vm.cveState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Nmap", "Port Scan", "Banner", "WAF", "CVE")

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Red Team Tools", fontFamily = FontFamily.Monospace, color = CyberRed) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberRed) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(CyberBg).padding(padding)) {
            // Legal Warning Banner
            Surface(color = CyberRed.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = CyberRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AUTHORIZED USE ONLY — These tools are for authorized penetration testing and network administration only.",
                        color = CyberRed,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                ConsentCheckbox(checked = consent, onCheckedChange = vm::onConsentChange)
            }

            Spacer(Modifier.height(8.dp))

            // Tab selector
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberBg,
                contentColor = CyberRed,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(tab, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    )
                }
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> NmapTab(vm, consent, nmapState)
                    1 -> PortScanTab(vm, consent, portState)
                    2 -> BannerGrabTab(vm, consent, bannerState)
                    3 -> WafDetectTab(vm, consent, wafState)
                    4 -> CveLookupTab(vm, cveState)
                }
            }
        }
    }
}

@Composable
fun NmapTab(vm: RedTeamViewModel, consent: Boolean, state: UiState<NmapScanResult>) {
    var target by remember { mutableStateOf("") }
    ScanTextField(value = target, onValueChange = { target = it }, placeholder = "192.168.1.1 or example.com", label = "Target")
    ScanButton("RUN NMAP SCAN", onClick = { vm.nmapScan(target) }, enabled = consent && target.isNotBlank() && state !is UiState.Loading, color = CyberRed)
    CyberCard(glowColor = CyberGray) {
        Text("Uses HackerTarget free Nmap API. Performs TCP SYN scan on top 100 ports. Results show open ports, services, and banners.", color = CyberGray, fontSize = 12.sp)
    }
    when (state) {
        is UiState.Loading -> LoadingOverlay()
        is UiState.Error -> ErrorCard(state.message)
        is UiState.Success -> NmapResultCard(state.data)
        else -> {}
    }
}

@Composable
fun NmapResultCard(result: NmapScanResult) {
    CyberCard(glowColor = CyberRed) {
        SectionHeader("SCAN RESULTS — ${result.target}", CyberRed)
        InfoRow("Scan Type", result.scanType)
        InfoRow("Duration", "${result.scanDuration}ms")
        InfoRow("Open Ports", result.ports.count { it.open }.toString())
        Spacer(Modifier.height(8.dp))
        SectionHeader("PORT DETAILS", CyberOrange)
        result.ports.filter { it.open }.forEach { port ->
            InfoRow("${port.port}/${if (port.open) "OPEN" else "closed"}", "${port.service ?: "unknown"} ${port.banner ?: ""}".trim(), CyberGreen)
        }
        Spacer(Modifier.height(8.dp))
        SectionHeader("RAW OUTPUT", CyberGray)
        TerminalText(result.rawOutput.take(1500))
    }
}

@Composable
fun PortScanTab(vm: RedTeamViewModel, consent: Boolean, state: UiState<NmapScanResult>) {
    var target by remember { mutableStateOf("") }
    var ports by remember { mutableStateOf("22,80,443,3389,3306,5432,8080,8443") }
    ScanTextField(value = target, onValueChange = { target = it }, placeholder = "192.168.1.1", label = "Target Host")
    ScanTextField(value = ports, onValueChange = { ports = it }, placeholder = "22,80,443 or 1-1000", label = "Ports (comma or range, max 200)")
    ScanButton("SCAN PORTS", onClick = { vm.portScan(target, ports) }, enabled = consent && target.isNotBlank() && state !is UiState.Loading, color = CyberOrange)
    when (state) {
        is UiState.Loading -> LoadingOverlay()
        is UiState.Error -> ErrorCard(state.message)
        is UiState.Success -> NmapResultCard(state.data)
        else -> {}
    }
}

@Composable
fun BannerGrabTab(vm: RedTeamViewModel, consent: Boolean, state: UiState<PortResult>) {
    var target by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    ScanTextField(value = target, onValueChange = { target = it }, placeholder = "192.168.1.1", label = "Target Host")
    ScanTextField(value = port, onValueChange = { port = it }, placeholder = "22", label = "Port Number")
    ScanButton("GRAB BANNER", onClick = { vm.bannerGrab(target, port.toIntOrNull() ?: 80) }, enabled = consent && target.isNotBlank() && port.isNotBlank() && state !is UiState.Loading, color = CyberPurple)
    CyberCard(glowColor = CyberGray) {
        Text("Banner grabbing retrieves service version information from open ports. Useful for fingerprinting services to identify software versions.", color = CyberGray, fontSize = 12.sp)
    }
    when (state) {
        is UiState.Loading -> LoadingOverlay()
        is UiState.Error -> ErrorCard(state.message)
        is UiState.Success -> CyberCard(glowColor = CyberPurple) {
            SectionHeader("BANNER RESULT", CyberPurple)
            InfoRow("Port", state.data.port.toString())
            InfoRow("Service", state.data.service ?: "Unknown")
            SectionHeader("BANNER DATA", CyberPurple)
            TerminalText(state.data.banner ?: "No banner received")
        }
        else -> {}
    }
}

@Composable
fun WafDetectTab(vm: RedTeamViewModel, consent: Boolean, state: UiState<WafDetectResult>) {
    var target by remember { mutableStateOf("") }
    ScanTextField(value = target, onValueChange = { target = it }, placeholder = "example.com", label = "Target Domain/URL")
    ScanButton("DETECT WAF", onClick = { vm.detectWaf(target) }, enabled = consent && target.isNotBlank() && state !is UiState.Loading, color = CyberBlue)
    CyberCard(glowColor = CyberGray) {
        Text("WAF detection analyzes HTTP response headers for signatures of Cloudflare, AWS WAF, Akamai, Imperva, Sucuri, F5, and more.", color = CyberGray, fontSize = 12.sp)
    }
    when (state) {
        is UiState.Loading -> LoadingOverlay()
        is UiState.Error -> ErrorCard(state.message)
        is UiState.Success -> CyberCard(glowColor = if (state.data.wafDetected) CyberOrange else CyberGreen) {
            SectionHeader("WAF DETECTION RESULT", if (state.data.wafDetected) CyberOrange else CyberGreen)
            InfoRow("WAF Detected", if (state.data.wafDetected) "YES" else "No", if (state.data.wafDetected) CyberOrange else CyberGreen)
            if (state.data.wafName != null) InfoRow("WAF Name", state.data.wafName)
            InfoRow("Confidence", "${state.data.confidence}%")
            if (state.data.fingerprints.isNotEmpty()) {
                SectionHeader("FINGERPRINTS", CyberOrange)
                state.data.fingerprints.forEach { fp ->
                    Text("• $fp", color = CyberWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        else -> {}
    }
}

@Composable
fun CveLookupTab(vm: RedTeamViewModel, state: UiState<List<CveInfo>>) {
    var vendor by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    ScanTextField(value = vendor, onValueChange = { vendor = it }, placeholder = "apache", label = "Vendor")
    ScanTextField(value = product, onValueChange = { product = it }, placeholder = "httpd", label = "Product")
    ScanButton("SEARCH CVE", onClick = { vm.lookupCve(vendor, product) }, enabled = vendor.isNotBlank() && product.isNotBlank() && state !is UiState.Loading, color = CyberGreen)
    CyberCard(glowColor = CyberGray) {
        Text("Powered by CIRCL CVE Search (free, no API key). Searches known vulnerabilities for a vendor/product pair.", color = CyberGray, fontSize = 12.sp)
    }
    when (state) {
        is UiState.Loading -> LoadingOverlay()
        is UiState.Error -> ErrorCard(state.message)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                CyberCard(glowColor = CyberGray) { Text("No CVEs found for $vendor/$product", color = CyberGray, fontSize = 13.sp) }
            } else {
                state.data.take(10).forEach { cve -> CveCard(cve) }
            }
        }
        else -> {}
    }
}

@Composable
fun CveCard(cve: CveInfo) {
    val cvssColor = when {
        (cve.cvss ?: 0.0) >= 9.0 -> CyberRed
        (cve.cvss ?: 0.0) >= 7.0 -> CyberOrange
        (cve.cvss ?: 0.0) >= 4.0 -> CyberYellow
        else -> CyberGreen
    }
    CyberCard(glowColor = cvssColor) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(cve.id, color = cvssColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (cve.cvss != null) {
                Surface(color = cvssColor.copy(0.15f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) {
                    Text("CVSS ${cve.cvss}", color = cvssColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(cve.summary, color = CyberWhite, fontSize = 12.sp, lineHeight = 17.sp)
        if (cve.publishedDate != null) {
            Spacer(Modifier.height(4.dp))
            Text("Published: ${cve.publishedDate}", color = CyberGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
