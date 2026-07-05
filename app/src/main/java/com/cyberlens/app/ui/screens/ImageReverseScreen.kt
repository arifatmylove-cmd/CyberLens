package com.cyberlens.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberlens.app.domain.model.ImageMatch
import com.cyberlens.app.domain.model.ImageReverseResult
import com.cyberlens.app.domain.model.UiState
import com.cyberlens.app.ui.components.*
import com.cyberlens.app.ui.theme.*
import com.cyberlens.app.ui.viewmodel.ImageReverseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageReverseScreen(
    navController: NavController,
    vm: ImageReverseViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val selectedUri by vm.selectedUri.collectAsStateWithLifecycle()
    val urlInput by vm.urlInput.collectAsStateWithLifecycle()
    var searchMode by remember { mutableStateOf(0) } // 0 = file, 1 = url
    val uriHandler = LocalUriHandler.current

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.onUriSelected(it) } }

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Image Reverse Search", fontFamily = FontFamily.Monospace, color = CyberOrange) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = CyberOrange)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberGreen.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            "SauceNAO",
                            color = CyberGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
        ) {
            // Mode toggle
            TabRow(
                selectedTabIndex = searchMode,
                containerColor = CyberBg,
                contentColor = CyberOrange
            ) {
                Tab(selected = searchMode == 0, onClick = { searchMode = 0 }) {
                    Text("Upload Image", fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = searchMode == 1, onClick = { searchMode = 1 }) {
                    Text("Search by URL", fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            when (val s = uiState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingOverlay() }
                is UiState.Success -> ImageResultList(s.data, uriHandler) { vm.reset() }
                is UiState.Error -> Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorCard(s.message)
                    ScanButton("TRY AGAIN", onClick = vm::reset, color = CyberOrange)
                }
                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (searchMode == 0) {
                            // Image upload mode
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberSurface)
                                    .border(2.dp, CyberOrange.copy(0.4f), RoundedCornerShape(12.dp))
                                    .clickable { imagePicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedUri != null) {
                                    AsyncImage(
                                        model = selectedUri,
                                        contentDescription = "Selected image",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(CyberBg.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Tap to change", color = CyberOrange, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = CyberOrange, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tap to select image", color = CyberOrange, fontFamily = FontFamily.Monospace)
                                        Text("JPEG / PNG / WebP", color = CyberGray, fontSize = 11.sp)
                                    }
                                }
                            }
                            ScanButton(
                                text = "REVERSE SEARCH IMAGE",
                                onClick = vm::searchByFile,
                                enabled = selectedUri != null,
                                color = CyberOrange
                            )
                        } else {
                            // URL mode
                            ScanTextField(
                                value = urlInput,
                                onValueChange = vm::onUrlChange,
                                placeholder = "https://example.com/image.jpg",
                                label = "Image URL"
                            )
                            ScanButton(
                                text = "SEARCH BY URL",
                                onClick = vm::searchByUrl,
                                enabled = urlInput.isNotBlank(),
                                color = CyberOrange
                            )
                        }

                        // Info card
                        CyberCard(glowColor = CyberGray) {
                            SectionHeader("POWERED BY SAUCENAO", CyberOrange)
                            Text(
                                "SauceNAO is a free reverse image search engine with 300 API calls/day. " +
                                "It searches across anime, manga, art, stock photos, and general web content.",
                                color = CyberLightGray, fontSize = 12.sp, lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Free API key at: saucenao.com/user.php?page=search-api",
                                color = CyberBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageResultList(
    result: ImageReverseResult,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Summary bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${result.totalMatches} matches found",
                    color = CyberOrange,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (result.apiLimitRemaining != null) {
                    Text(
                        "${result.apiLimitRemaining} API calls remaining today",
                        color = CyberGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            TextButton(onClick = onBack) {
                Text("New Search", color = CyberOrange, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        if (result.matches.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ImageNotSupported, null, tint = CyberGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No matches found", color = CyberGray, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(result.matches) { match ->
                    ImageMatchCard(match, uriHandler)
                }
            }
        }
    }
}

@Composable
fun ImageMatchCard(match: ImageMatch, uriHandler: androidx.compose.ui.platform.UriHandler) {
    val simColor = when {
        match.similarity >= 90 -> CyberGreen
        match.similarity >= 70 -> CyberOrange
        else -> CyberGray
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, simColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            // Thumbnail
            if (match.thumbnail != null) {
                AsyncImage(
                    model = match.thumbnail,
                    contentDescription = "Match thumbnail",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                // Similarity badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = simColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "${match.similarity.toInt()}% match",
                            color = simColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (match.indexName != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(match.indexName, color = CyberGray, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (match.title != null) {
                    Text(match.title, color = CyberWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2)
                }
                if (match.authorName != null) {
                    Text("by ${match.authorName}", color = CyberLightGray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
                // Clickable URL
                Text(
                    match.sourceUrl,
                    color = CyberBlue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri(match.sourceUrl) }
                    }
                )
            }
        }
    }
}
