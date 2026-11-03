package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ResearchEntity
import com.example.ui.MouseUiState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
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
fun ResearchScreen(
    uiState: MouseUiState,
    onConductResearch: (String) -> Unit,
    onToggleVerification: (id: String, currentStatus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var researchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Independent Research Lab",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = if (uiState.permissionState.isInternetEnabled) "Online Multi-Source Research Engine active" else "Offline Archive Search (Live Web is OFF)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Network Status Advisory
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (uiState.permissionState.isInternetEnabled) Icons.Default.Public else Icons.Default.PublicOff,
                    contentDescription = null,
                    tint = if (uiState.permissionState.isInternetEnabled) AccentGreen else AmberSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.permissionState.isInternetEnabled) {
                        "Internet ON: MOUSE searches multiple sources, compares data, and validates confidence."
                    } else {
                        "Internet OFF: MOUSE synthesizes records strictly from local verified archives without calling external web APIs."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = researchQuery,
                onValueChange = { researchQuery = it },
                placeholder = { Text("Enter research topic or question...", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("research_query_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (researchQuery.isNotBlank()) {
                        onConductResearch(researchQuery)
                        researchQuery = ""
                    }
                },
                enabled = researchQuery.isNotBlank() && !uiState.isGenerating,
                modifier = Modifier.height(52.dp).testTag("conduct_research_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = SurfaceDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SurfaceDark, strokeWidth = 2.dp)
                } else {
                    Text("Investigate", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Research Archive List
        Text(
            text = "SYNTHESIZED RESEARCH ARCHIVES (${uiState.researchList.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.researchList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No research records compiled yet.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.researchList, key = { it.id }) { item ->
                    ResearchCard(
                        research = item,
                        onToggleStatus = { onToggleVerification(item.id, item.verificationStatus) }
                    )
                }
            }
        }
    }
}

@Composable
fun ResearchCard(
    research: ResearchEntity,
    onToggleStatus: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("research_card_${research.id.take(8)}"),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceVariantDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = research.publisher,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = CyanPrimary
                )

                // Verification Pill Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleStatus() }
                        .testTag("toggle_verification_${research.id.take(8)}"),
                    color = when (research.verificationStatus) {
                        "VERIFIED" -> AccentGreen.copy(alpha = 0.15f)
                        "UNVERIFIED" -> AmberSecondary.copy(alpha = 0.15f)
                        else -> AccentRed.copy(alpha = 0.15f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (research.verificationStatus) {
                            "VERIFIED" -> AccentGreen
                            "UNVERIFIED" -> AmberSecondary
                            else -> AccentRed
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = when (research.verificationStatus) {
                                "VERIFIED" -> AccentGreen
                                "UNVERIFIED" -> AmberSecondary
                                else -> AccentRed
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = research.verificationStatus,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = when (research.verificationStatus) {
                                "VERIFIED" -> AccentGreen
                                "UNVERIFIED" -> AmberSecondary
                                else -> AccentRed
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = research.question,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = research.answer,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Citations / Sources: ${research.sourcesJson}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confidence: ${(research.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = AmberSecondary
            )
        }
    }
}
