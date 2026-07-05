package com.cyberlens.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberlens.app.domain.model.RiskLevel
import com.cyberlens.app.ui.theme.*

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CyberBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, glowColor.copy(alpha = 0.3f)),
        content = { Column(Modifier.padding(16.dp), content = content) }
    )
}

@Composable
fun RiskBadge(riskLevel: RiskLevel) {
    val (color, label) = when (riskLevel) {
        RiskLevel.SAFE -> Pair(CyberGreen, "SAFE")
        RiskLevel.SUSPICIOUS -> Pair(CyberOrange, "SUSPICIOUS")
        RiskLevel.DANGEROUS -> Pair(CyberRed, "DANGEROUS")
        RiskLevel.UNKNOWN -> Pair(CyberGray, "UNKNOWN")
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = "● $label",
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ScanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace, color = CyberGray) },
        label = label?.let { { Text(it, color = CyberLightGray) } },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberBlue,
            unfocusedBorderColor = CyberGray,
            focusedTextColor = CyberWhite,
            unfocusedTextColor = CyberWhite,
            cursorColor = CyberBlue,
            focusedContainerColor = CyberSurface,
            unfocusedContainerColor = CyberSurface
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun ConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberRed.copy(alpha = 0.08f))
            .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CyberGreen,
                uncheckedColor = CyberRed,
                checkmarkColor = CyberBg
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "I confirm I am authorized to scan this target and accept full legal responsibility.",
            color = CyberWhite,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ScanButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = CyberBlue,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = CyberBg,
            disabledContainerColor = CyberGray.copy(alpha = 0.3f),
            disabledContentColor = CyberGray
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = CyberWhite) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CyberLightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
    HorizontalDivider(color = CyberSurfaceVariant, thickness = 0.5.dp)
}

@Composable
fun SectionHeader(title: String, color: Color = CyberBlue) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.width(3.dp).height(18.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LoadingOverlay() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CyberBlue, strokeWidth = 2.dp)
            Spacer(Modifier.height(12.dp))
            Text("Scanning...", color = CyberBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    CyberCard(glowColor = CyberRed) {
        Text("ERROR", color = CyberRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(message, color = CyberWhite, fontSize = 13.sp)
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = CyberWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = CyberLightGray, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SecurityScoreBar(score: Int) {
    val color = when {
        score >= 70 -> CyberGreen
        score >= 40 -> CyberOrange
        else -> CyberRed
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Security Score", color = CyberLightGray, fontSize = 12.sp)
            Text("$score/100", color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CyberSurfaceVariant
        )
    }
}

@Composable
fun TerminalText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = CyberGreen,
        lineHeight = 16.sp,
        modifier = modifier
    )
}
