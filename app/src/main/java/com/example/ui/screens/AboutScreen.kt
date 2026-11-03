package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletTertiary

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("about_screen")
    ) {
        // Hero Emblem
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyanPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MOUSE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = CyanPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "My Own Ultimate Smart Entity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Complete AI Personal Assistant (v1.0 Native Android)",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Core Operating Principles
        Text(
            text = "CORE OPERATING PRINCIPLES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        PrincipleCard(
            title = "1. Offline-First by Default",
            body = "Internet access is strictly OFF by default. MOUSE never makes unauthorized background network requests without explicit owner consent.",
            accentColor = AmberSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        PrincipleCard(
            title = "2. One Central Identity",
            body = "MOUSE is a single continuous entity with unified memory, personality, and context across all underlying local or cloud AI models.",
            accentColor = CyanPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        PrincipleCard(
            title = "3. Strict Honesty & Non-Hallucination",
            body = "MOUSE never pretends a web search occurred when it did not, never claims an offline answer was from an online model, and communicates all technical limitations honestly.",
            accentColor = AccentGreen
        )

        Spacer(modifier = Modifier.height(10.dp))

        PrincipleCard(
            title = "4. Hardware-Backed Owner Security",
            body = "Sensitive credentials are saved in the AndroidKeyStore using AES-256-GCM hardware encryption. Operating modes ensure sensitive tasks always require user confirmation.",
            accentColor = VioletTertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Technology Architecture
        Text(
            text = "TECHNICAL ARCHITECTURE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariantDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TechRow("Language", "100% Native Kotlin (Coroutines & Flow)")
                TechRow("UI Toolkit", "Jetpack Compose (Material 3 Dynamic System)")
                TechRow("Local Persistence", "Room SQLite Database with Reactive DAOs")
                TechRow("Credential Security", "AndroidKeyStore (AES-256-GCM Hardware-Backed)")
                TechRow("Modular AI Engine", "MouseEngine (Layer A Retrieval + Layer B Hardware/Local AI + Layer C Routing)")
                TechRow("Online Adapters", "Google Gemini 1.5 Flash & OpenAI-Compatible REST")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PrincipleCard(
    title: String,
    body: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp), color = TextSecondary)
        }
    }
}

@Composable
fun TechRow(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = key, style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}
