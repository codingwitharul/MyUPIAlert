package com.codingwitharul.myupialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwitharul.myupialert.R
import com.codingwitharul.myupialert.ui.MainIntent
import com.codingwitharul.myupialert.ui.MainState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawerContent(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Language Dropdown
        Text(stringResource(R.string.language), color = Color.Gray, fontSize = 14.sp)
        var expandedLanguage by remember { mutableStateOf(false) }
        val languages = listOf("English", "Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Marathi", "Gujarati", "Bengali", "Punjabi", "Odia", "Urdu", "Assamese")
        ExposedDropdownMenuBox(
            expanded = expandedLanguage,
            onExpandedChange = { expandedLanguage = !expandedLanguage },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = state.language,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            ExposedDropdownMenu(
                expanded = expandedLanguage,
                onDismissRequest = { expandedLanguage = false }
            ) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
                        onClick = {
                            onIntent(MainIntent.ChangeLanguage(lang))
                            expandedLanguage = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Mute Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.mute_announcements), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.mute_subtitle), fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = state.isMuted,
                onCheckedChange = { onIntent(MainIntent.ToggleMute(it)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Volume
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.volume), color = Color.Gray, fontSize = 14.sp)
            Text("${state.volume}%", color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(
            value = state.volume.toFloat(),
            onValueChange = { onIntent(MainIntent.ChangeVolume(it.toInt())) },
            valueRange = 0f..200f,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.speech_speed), color = Color.Gray, fontSize = 14.sp)
            Text(String.format(java.util.Locale.US, "%.1fx", state.speedFloat), color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(
            value = state.speedInt.toFloat(),
            onValueChange = { 
                val newFloat = 0.5f + (it / 10f)
                onIntent(MainIntent.ChangeSpeed(it.toInt(), newFloat)) 
            },
            valueRange = 0f..10f,
            modifier = Modifier.padding(top = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.DarkGray)

        // Announce Sender Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.announce_sender), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.announce_sender_subtitle), fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = state.announceSenderName,
                onCheckedChange = { onIntent(MainIntent.ToggleAnnounceSender(it)) }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.DarkGray)

        // SMS Forwarder Settings
        Text(stringResource(R.string.sms_forwarder), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.forwarderNumber,
                onValueChange = { onIntent(MainIntent.ChangeForwarderNumber(it)) },
                placeholder = { Text(stringResource(R.string.forward_to_number)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { /* trigger save action if needed, or already saved by state */ }) {
                Text(stringResource(R.string.set))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.forward_payment_type), color = Color.Gray, fontSize = 14.sp)
        
        var expandedForwardType by remember { mutableStateOf(false) }
        val forwardTypes = listOf("Both", "Credited", "Debited")
        ExposedDropdownMenuBox(
            expanded = expandedForwardType,
            onExpandedChange = { expandedForwardType = !expandedForwardType },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            OutlinedTextField(
                value = state.forwarderTypeFilter,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForwardType) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedForwardType,
                onDismissRequest = { expandedForwardType = false }
            ) {
                forwardTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onIntent(MainIntent.ChangeForwarderType(type))
                            expandedForwardType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.forward_app), color = Color.Gray, fontSize = 14.sp)
        
        var expandedForwardApp by remember { mutableStateOf(false) }
        // Hardcoding standard ones, or could pass from state
        val forwardApps = listOf("all", "Google Pay", "PhonePe", "Paytm")
        ExposedDropdownMenuBox(
            expanded = expandedForwardApp,
            onExpandedChange = { expandedForwardApp = !expandedForwardApp },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            OutlinedTextField(
                value = state.forwarderAppFilter,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForwardApp) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedForwardApp,
                onDismissRequest = { expandedForwardApp = false }
            ) {
                forwardApps.forEach { app ->
                    DropdownMenuItem(
                        text = { Text(if(app == "all") "All Apps" else app) },
                        onClick = {
                            onIntent(MainIntent.ChangeForwarderApp(app))
                            expandedForwardApp = false
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.DarkGray)

        // Test Sounds
        Text(stringResource(R.string.test_sounds), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Button(
            onClick = { onIntent(MainIntent.TestSound(amount = null)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.test_default_sound), color = MaterialTheme.colorScheme.onPrimary)
        }

        var testAmount by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = testAmount,
                onValueChange = { testAmount = it },
                placeholder = { Text(stringResource(R.string.amount)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if(testAmount.isNotBlank()) onIntent(MainIntent.TestSound(amount = testAmount))
                    else onIntent(MainIntent.TestSound(amount = "1500.00")) // Provide a default if empty
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.test_amount), color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = Color.DarkGray)

        Button(
            onClick = { onIntent(MainIntent.ExportTransactions) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Transactions")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
