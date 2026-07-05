package com.cyberlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.domain.model.UsernameOsintResult
import com.cyberlens.app.domain.model.UsernameResult
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.UsernameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameOsintScreen(navController: NavController, vm: UsernameViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val input by vm.inputText.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Username OSINT", fontFamily = FontFamily.Monospace, color = CyberPurple) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberPurple) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBg)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().background(CyberBg).padding(padding)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScanTextField(value = input, onValueChange = vm::onInputChange, placeholder = "johndoe", label = "Username to search")
                ScanButton("SEARCH USERNAME", vm::search, input.isNotBlank() && uiState !is UiState.Loading, CyberPurple)
            }

            when (val s = uiState) {
                is UiState.Loading -> LoadingOverlay()
                is UiState.Error -> Box(Modifier.padding(16.dp)) { ErrorCard(s.message) }
                is UiState.Success -> UsernameResultList(s.data)
                else -> {}
            }
        }
    }
}

@Composable
fun UsernameResultList(result: UsernameOsintResult) {
    Column {
        // Summary header
        CyberCard(modifier = Modifier.padding(horizontal = 16.dp), glowColor = CyberPurple) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("@${result.username}", color = CyberPurple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("${result.foundCount} of ${result.totalChecked} platforms", color = CyberLightGray, fontSize = 12.sp)
                }
                Text("${result.foundCount}", color = CyberGreen, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(result.results.sortedByDescending { it.found }) { r ->
                UsernameResultRow(r)
            }
        }
    }
}

@Composable
fun UsernameResultRow(result: UsernameResult) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (result.found) CyberGreen.copy(0.4f) else CyberGray.copy(0.2f))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(result.platform, color = CyberWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(result.url, color = CyberGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                color = if (result.found) CyberGreen.copy(0.15f) else CyberGray.copy(0.15f)
            ) {
                Text(
                    if (result.found) "FOUND" else "NOT FOUND",
                    color = if (result.found) CyberGreen else CyberGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
