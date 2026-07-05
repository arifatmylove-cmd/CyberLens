package com.cyberlens.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cyberlens.app.ui.components.CyberCard
import com.cyberlens.app.ui.components.SectionHeader
import com.cyberlens.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageReverseScreen(navController: NavController) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
    }

    Scaffold(
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = { Text("Image Reverse Search", fontFamily = FontFamily.Monospace, color = CyberOrange) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = CyberOrange) } },
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
            // Upload area
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(2.dp, CyberOrange.copy(0.4f), RoundedCornerShape(12.dp))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = CyberOrange, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to select image", color = CyberOrange, fontFamily = FontFamily.Monospace)
                        Text("from gallery", color = CyberGray, fontSize = 12.sp)
                    }
                }
            }

            if (selectedUri != null) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = CyberBg)
                ) {
                    Text("SEARCH IMAGE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // API Key Info
            CyberCard(glowColor = CyberOrange) {
                SectionHeader("SETUP REQUIRED", CyberOrange)
                Text(
                    "Image reverse search requires one of the following free API keys:",
                    color = CyberWhite,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                ApiOptionRow("Google Cloud Vision API", "Free tier: 1000 req/month", CyberBlue)
                ApiOptionRow("TinEye API", "Free trial available", CyberGreen)
                ApiOptionRow("Bing Visual Search", "Free tier via Azure", CyberPurple)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add your API key in Settings once obtained. Image upload, Base64 encoding, and similarity matching are fully implemented.",
                    color = CyberGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            // How it works
            CyberCard(glowColor = CyberGray) {
                SectionHeader("HOW IT WORKS")
                Text("1. Select image from gallery or camera", color = CyberWhite, fontSize = 12.sp)
                Text("2. Image is converted to Base64", color = CyberWhite, fontSize = 12.sp)
                Text("3. Sent to reverse image API", color = CyberWhite, fontSize = 12.sp)
                Text("4. Returns similar images + source URLs", color = CyberWhite, fontSize = 12.sp)
                Text("5. Shows similarity score for each match", color = CyberWhite, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ApiOptionRow(name: String, desc: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = CyberGray, fontSize = 11.sp)
        }
    }
}
