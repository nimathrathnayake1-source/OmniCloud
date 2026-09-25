package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CloudProviderType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomServerDialog(
    onDismiss: () -> Unit,
    onAddServer: (
        name: String,
        type: CloudProviderType,
        endpointUrl: String,
        bucketOrPath: String,
        accountEmail: String,
        password: String,
        quotaGb: Long,
        autoSync: Boolean,
        onlyEncrypted: Boolean
    ) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CloudProviderType.CUSTOM_SERVER) }
    var endpointUrl by remember { mutableStateOf("https://") }
    var bucketOrPath by remember { mutableStateOf("/OmniCloud/Vault") }
    var accountEmail by remember { mutableStateOf("admin@server.local") }
    var password by remember { mutableStateOf("") }
    var quotaGbText by remember { mutableStateOf("100") }
    var autoSync by remember { mutableStateOf(false) }
    var onlyEncrypted by remember { mutableStateOf(false) }

    val presetTypes = listOf(
        Pair(CloudProviderType.CUSTOM_SERVER, "Self-Hosted Cloud"),
        Pair(CloudProviderType.WEBDAV, "WebDAV / NAS (Synology, QNAP)"),
        Pair(CloudProviderType.NEXTCLOUD, "Private Nextcloud / ownCloud"),
        Pair(CloudProviderType.AWS_S3, "MinIO / S3-Compatible Storage")
    )

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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_server_dialog_card")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add Your Own Cloud Server",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect self-hosted NAS, Nextcloud, WebDAV, or MinIO",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Server Architecture & Protocol:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTypes.forEach { (type, label) ->
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedType = type
                                    if (serverName.isBlank() || presetTypes.any { it.second == serverName }) {
                                        serverName = label
                                    }
                                    if (type == CloudProviderType.NEXTCLOUD && endpointUrl == "https://") {
                                        endpointUrl = "https://cloud.myserver.com/remote.php/webdav/"
                                    } else if (type == CloudProviderType.AWS_S3 && endpointUrl == "https://") {
                                        endpointUrl = "https://s3.mycompany.com:9000"
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Display Name") },
                        placeholder = { Text("e.g. Home Synology NAS, Private MinIO") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_server_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = endpointUrl,
                        onValueChange = { endpointUrl = it },
                        label = { Text("Server Endpoint / Host URL") },
                        placeholder = { Text("https://nas.local:5006 or https://nextcloud.domain.com") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_server_url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bucketOrPath,
                        onValueChange = { bucketOrPath = it },
                        label = { Text("Target Remote Directory / S3 Bucket") },
                        placeholder = { Text("/OmniCloud/Vault or my-secure-bucket") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = accountEmail,
                        onValueChange = { accountEmail = it },
                        label = { Text("Account Identifier / Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password / App Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = quotaGbText,
                        onValueChange = { quotaGbText = it },
                        label = { Text("Allocated Storage Quota (GB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Sync Replications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Automatically backup newly uploaded records here", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict E2EE Protection", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Only sync AES-256 encrypted records to this server", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = onlyEncrypted, onCheckedChange = { onlyEncrypted = it })
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val name = if (serverName.isNotBlank()) serverName.trim() else "Private Cloud Server"
                                val quotaGb = quotaGbText.toLongOrNull() ?: 100L
                                onAddServer(
                                    name,
                                    selectedType,
                                    endpointUrl.trim(),
                                    bucketOrPath.trim(),
                                    accountEmail.trim(),
                                    password,
                                    quotaGb,
                                    autoSync,
                                    onlyEncrypted
                                )
                                onDismiss()
                            },
                            modifier = Modifier.testTag("confirm_add_server_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect Server")
                        }
                    }
                }
            }
        }
    }
}
