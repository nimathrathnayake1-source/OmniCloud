package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CloudProviderEntity
import com.example.model.FileCloudSyncEntity
import com.example.model.ProjectFolderEntity
import com.example.model.SyncStatus
import com.example.model.VaultFileEntity
import com.example.security.CryptoEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileDetailDialog(
    file: VaultFileEntity,
    project: ProjectFolderEntity?,
    providers: List<CloudProviderEntity>,
    syncs: List<FileCloudSyncEntity>,
    requireBioAuth: Boolean = true,
    masterPassphrase: String = "",
    onDismiss: () -> Unit,
    onDecrypt: (passphrase: String?) -> Result<String>,
    onSyncToProvider: (providerId: String) -> Unit,
    onDesyncFromProvider: (providerId: String, providerName: String) -> Unit = { _, _ -> },
    onSyncToAll: () -> Unit,
    onDelete: () -> Unit
) {
    var decryptedContent by remember { mutableStateOf<String?>(null) }
    var decryptionError by remember { mutableStateOf<String?>(null) }
    var customPassphrase by remember { mutableStateOf("") }
    var showCustomPassField by remember { mutableStateOf(false) }
    var showBioPrompt by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_detail_dialog_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(file.category.colorHex).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (file.isEncrypted) Icons.Default.Security else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(file.category.colorHex),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Text(
                            text = "${file.category.displayName} • ${CryptoEngine.formatBytes(file.sizeBytes)} • ${dateFormat.format(Date(file.createdAt))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.testTag("delete_file_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Organization tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Project Folder: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (project != null) Color(project.colorHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = project?.name ?: "Uncategorized",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (project != null) Color(project.colorHex) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                // Cryptographic & E2EE Info
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (file.isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (file.isEncrypted) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (file.isEncrypted) "E2EE AES-256-GCM Protected" else "Standard File (Plaintext)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (file.isEncrypted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SHA-256 Checksum:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = file.sha256Checksum,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (file.isEncrypted) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cipher Salt (128-bit): ${file.cipherSaltBase64.take(14)}... • IV (96-bit GCM): ${file.cipherIvBase64.take(14)}...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Decryption Actions
                            if (decryptedContent == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (requireBioAuth) {
                                                showBioPrompt = true
                                            } else {
                                                val res = onDecrypt(null)
                                                if (res.isSuccess) {
                                                    decryptedContent = res.getOrNull()
                                                    decryptionError = null
                                                } else {
                                                    decryptionError = res.exceptionOrNull()?.message ?: "Decryption failed"
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("decrypt_with_master_key_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (requireBioAuth) "Verify Bio & Decrypt" else "Decrypt with Master Key", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showCustomPassField = !showCustomPassField }
                                    ) {
                                        Text("Custom Key", fontSize = 12.sp)
                                    }
                                }

                                if (showCustomPassField) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = customPassphrase,
                                            onValueChange = { customPassphrase = it },
                                            placeholder = { Text("Enter passphrase", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                val res = onDecrypt(customPassphrase)
                                                if (res.isSuccess) {
                                                    decryptedContent = res.getOrNull()
                                                    decryptionError = null
                                                } else {
                                                    decryptionError = "Invalid passphrase"
                                                }
                                            }
                                        ) {
                                            Text("Try", fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (decryptionError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = decryptionError!!,
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                // Decrypted content preview
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Decrypted Payload Preview:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = decryptedContent!!,
                                        modifier = Modifier.padding(10.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Multi-Cloud Sync Status Matrix
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multi-Cloud Synchronization Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Replication status across storage providers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onSyncToAll) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync All", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val syncsMap = syncs.associateBy { it.providerId }
                providers.forEach { provider ->
                    val syncEntry = syncsMap[provider.id]
                    val isSynced = syncEntry?.syncStatus == SyncStatus.SYNCED

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProviderAvatar(type = provider.type, size = 30)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (provider.isConnected) {
                                        if (isSynced) "Synced to remote" else "Pending / Out of sync"
                                    } else "Provider Disconnected",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (syncEntry != null) {
                                SyncStatusBadge(status = syncEntry.syncStatus)
                            } else {
                                Text(
                                    text = "Not Synced",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (provider.isConnected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isSynced) {
                                    // Button to de-sync from this cloud provider
                                    IconButton(
                                        onClick = { onDesyncFromProvider(provider.id, provider.name) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = "De-sync from ${provider.name}",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { onSyncToProvider(provider.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Sync",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete File?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Do you really want to delete the file '${file.name}'? This action cannot be undone and will permanently remove the record from your vault and synced cloud replicas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("confirm_delete_dialog")
        )
    }

    if (showBioPrompt) {
        BiometricVerificationDialog(
            title = "Personal Vault Decryption",
            subtitle = "Verify biometric identity to decrypt '${file.name}'",
            masterPassphrase = masterPassphrase,
            onDismiss = { showBioPrompt = false },
            onAuthenticated = {
                showBioPrompt = false
                val res = onDecrypt(null)
                if (res.isSuccess) {
                    decryptedContent = res.getOrNull()
                    decryptionError = null
                } else {
                    decryptionError = res.exceptionOrNull()?.message ?: "Decryption failed"
                }
            }
        )
    }
}
