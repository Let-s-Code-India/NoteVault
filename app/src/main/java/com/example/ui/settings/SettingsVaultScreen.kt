package com.example.ui.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.NoteVaultTopBar
import com.example.ui.components.PinEntryDialog
import com.example.ui.viewmodel.NoteVaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsVaultScreen(viewModel: NoteVaultViewModel) {
    val notes by viewModel.notes.collectAsState()
    val diagrams by viewModel.diagrams.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val context = LocalContext.current
    var showBackupModal by remember { mutableStateOf(false) }
    var showRestoreModal by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var restoreInputText by remember { mutableStateOf("") }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    viewModel.restoreFullZipBackup(context, stream)
                }
                showRestoreModal = false
            } catch (e: Exception) {
                viewModel.userNotification.value = "Error opening backup zip: ${e.localizedMessage}"
            }
        }
    }

    Scaffold(
        topBar = {
            NoteVaultTopBar(
                title = "Vault & Settings",
                syncStatus = syncStatus
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Storage Health Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Local Database Statistics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatBadge("Notes", "${notes.size}")
                        StatBadge("Diagrams", "${diagrams.size}")
                        StatBadge("Tasks", "${tasks.size}")
                        StatBadge("Folders", "${folders.size}")
                    }
                }
            }

            // Sync Readiness Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Offline-First Sync Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "100% Offline-First. Data is stored directly in your device's encrypted Room SQLite database. Background sync queue automatically pushes local changes when online.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Security & Encryption Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Active Security & Encryption", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Database at rest: Encrypted via SQLCipher (256-bit AES, KeyStore-managed key)\n" +
                        "• PIN Protection: Salted SHA-256 hashing (stored in EncryptedSharedPreferences)\n" +
                        "• Biometric Unlock: Fingerprint / Face ID via AndroidX BiometricPrompt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isBioEnabled by viewModel.isBiometricEnabled.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = isBioEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showPinSetupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Vault Master PIN (Salted Hash)")
                    }
                }
            }

            // Backup & Restore Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Full App Backup & Restore (.zip)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Export complete app archive (notes, folders, diagrams, tasks, attached images) as a single .zip file for complete local offline backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportFullZipBackup(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export ZIP")
                        }
                        OutlinedButton(
                            onClick = { showRestoreModal = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Backup")
                        }
                    }
                }
            }

            // Legal & Compliance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Legal, Privacy & Licenses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(
                        "Review our 100% on-device privacy guarantee, terms of service, open-source library attributions, and developer contact information.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.openLegalScreen(com.example.ui.legal.LegalTab.PRIVACY) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Privacy")
                        }
                        OutlinedButton(
                            onClick = { viewModel.openLegalScreen(com.example.ui.legal.LegalTab.TERMS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Terms")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.openLegalScreen(com.example.ui.legal.LegalTab.DISCLAIMER) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disclaimer")
                        }
                        OutlinedButton(
                            onClick = { viewModel.openLegalScreen(com.example.ui.legal.LegalTab.LICENSES) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Licenses")
                        }
                    }

                    Button(
                        onClick = { viewModel.openLegalScreen(com.example.ui.legal.LegalTab.ABOUT) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("About NoteVault & Contact Info")
                    }
                }
            }
        }
    }

    if (showBackupModal) {
        AlertDialog(
            onDismissRequest = { showBackupModal = false },
            title = { Text("Backup Archive Ready") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Whole-app JSON snapshot:")
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, backupJsonText)
                        type = "application/json"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Save NoteVault Backup Snapshot"))
                    showBackupModal = false
                }) {
                    Text("Share Backup File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupModal = false }) { Text("Close") }
            }
        )
    }

    if (showRestoreModal) {
        AlertDialog(
            onDismissRequest = { showRestoreModal = false },
            title = { Text("Restore NoteVault Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            zipPickerLauncher.launch("application/zip")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick .ZIP Backup File")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Text("Paste backup JSON snapshot:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("{\"version\":1, ...}") },
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = restoreInputText.isNotBlank(),
                    onClick = {
                        if (restoreInputText.isNotBlank()) {
                            viewModel.restoreFullBackup(restoreInputText)
                            showRestoreModal = false
                        }
                    }
                ) {
                    Text("Restore JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreModal = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinSetupDialog) {
        PinEntryDialog(
            title = "Set App Security PIN",
            onPinSubmitted = { pin ->
                viewModel.saveAppPin(pin)
                showPinSetupDialog = false
            },
            onDismiss = { showPinSetupDialog = false }
        )
    }
}

@Composable
fun StatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
