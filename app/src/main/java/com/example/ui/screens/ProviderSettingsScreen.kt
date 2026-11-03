package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MouseUiState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryContainer
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProviderSettingsScreen(
    uiState: MouseUiState,
    onSelectProvider: (String) -> Unit,
    onSaveKey: (providerId: String, key: String) -> Unit,
    getMaskedKey: (providerId: String) -> String,
    modifier: Modifier = Modifier
) {
    var geminiKeyInput by remember { mutableStateOf("") }
    var openAiKeyInput by remember { mutableStateOf("") }
    var openAiEndpointInput by remember { mutableStateOf("https://api.openai.com/v1") }
    var openAiModelInput by remember { mutableStateOf("gpt-4o-mini") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "AI Provider Architecture",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Select active intelligence backend & manage Keystore credentials",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Security Notice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Credentials are encrypted on-device with AndroidKeyStore (AES-256-GCM). Plaintext keys are never logged or exported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "ACTIVE INTELLIGENCE PROVIDER",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Provider Options
        ProviderChoiceCard(
            id = "local_core",
            title = "MOUSE Local Core",
            subtitle = "Zero network. 100% on-device Layer A/B/C reasoning with local memory & knowledge.",
            isOnline = false,
            isSelected = uiState.activeProviderId == "local_core",
            onSelect = { onSelectProvider("local_core") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProviderChoiceCard(
            id = "gemini",
            title = "Google Gemini 1.5 Flash",
            subtitle = "Cloud inference with speed and reasoning capabilities. Requires internet permission.",
            isOnline = true,
            isSelected = uiState.activeProviderId == "gemini",
            onSelect = { onSelectProvider("gemini") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProviderChoiceCard(
            id = "openai_compatible",
            title = "OpenAI-Compatible Custom Endpoint",
            subtitle = "Connects to any standard API (OpenAI, Ollama, vLLM, self-hosted models).",
            isOnline = true,
            isSelected = uiState.activeProviderId == "openai_compatible",
            onSelect = { onSelectProvider("openai_compatible") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gemini Key Section
        Text(
            text = "GEMINI CREDENTIALS",
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stored Key:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(getMaskedKey("gemini"), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CyanPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = { geminiKeyInput = it },
                    placeholder = { Text("Paste new Gemini API key...", fontSize = 13.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (geminiKeyInput.isNotBlank()) {
                            onSaveKey("gemini", geminiKeyInput)
                            geminiKeyInput = ""
                        }
                    },
                    enabled = geminiKeyInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.End).testTag("save_gemini_key_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SurfaceDark)
                ) {
                    Text("Encrypt & Save Key", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // OpenAI Compatible Section
        Text(
            text = "OPENAI-COMPATIBLE CONFIGURATION",
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
                OutlinedTextField(
                    value = openAiEndpointInput,
                    onValueChange = { openAiEndpointInput = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("e.g. https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = openAiModelInput,
                    onValueChange = { openAiModelInput = it },
                    label = { Text("Model Name") },
                    placeholder = { Text("e.g. gpt-4o-mini or mistral") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stored Token/Key:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(getMaskedKey("openai_compatible"), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = CyanPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = openAiKeyInput,
                    onValueChange = { openAiKeyInput = it },
                    placeholder = { Text("Paste endpoint bearer token/API key...", fontSize = 13.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (openAiKeyInput.isNotBlank()) {
                            onSaveKey("openai_compatible", openAiKeyInput)
                            openAiKeyInput = ""
                        }
                    },
                    enabled = openAiKeyInput.isNotBlank(),
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SurfaceDark)
                ) {
                    Text("Save Custom Key", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProviderChoiceCard(
    id: String,
    title: String,
    subtitle: String,
    isOnline: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("provider_choice_$id"),
        color = if (isSelected) CyanPrimaryContainer else SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanPrimary else BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) CyanPrimary else TextMuted
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOnline) AccentGreen.copy(alpha = 0.2f) else AmberSecondary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = if (isOnline) AccentGreen else AmberSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
