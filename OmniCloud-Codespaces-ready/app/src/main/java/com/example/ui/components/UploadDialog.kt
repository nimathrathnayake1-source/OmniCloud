package com.example.ui.components

import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CloudProviderEntity
import com.example.model.FileTypeCategory
import com.example.model.ProjectFolderEntity
import com.example.security.CryptoEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadDialog(
    projects: List<ProjectFolderEntity>,
    connectedProviders: List<CloudProviderEntity>,
    onDismiss: () -> Unit,
    onUpload: (
        fileName: String,
        originalName: String,
        mimeType: String,
        content: String,
        sizeBytes: Long,
        category: FileTypeCategory,
        projectId: Long?,
        enableEncryption: Boolean,
        targetProviderIds: Set<String>?
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pick from Device, 1 = Write Encrypted Note
    var fileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("text/plain") }
    var fileSizeBytes by remember { mutableLongStateOf(0L) }
    var hasPickedFile by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf(FileTypeCategory.DOCUMENTS) }
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var enableEncryption by remember { mutableStateOf(true) }
    var selectedProviders by remember {
        mutableStateOf(connectedProviders.map { it.id }.toSet())
    }

    // System File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                var resolvedName = "file_${System.currentTimeMillis()}"
                var resolvedSize = 0L

                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (it.moveToFirst()) {
                        if (nameIndex != -1) {
                            resolvedName = it.getString(nameIndex) ?: resolvedName
                        }
                        if (sizeIndex != -1) {
                            resolvedSize = it.getLong(sizeIndex)
                        }
                    }
                }

                val resolvedMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                fileName = resolvedName
                mimeType = resolvedMime
                selectedCategory = FileTypeCategory.fromMimeOrFilename(resolvedMime, resolvedName)

                // Read bytes
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    fileSizeBytes = if (resolvedSize > 0) resolvedSize else bytes.size.toLong()
                    fileContent = if (resolvedMime.startsWith("text/") ||
                        resolvedMime.contains("json") ||
                        resolvedMime.contains("xml") ||
                        resolvedMime.contains("yaml") ||
                        resolvedMime.contains("csv")
                    ) {
                        String(bytes, Charsets.UTF_8)
                    } else {
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
                hasPickedFile = true
            } catch (e: Exception) {
                // Fallback for empty or unreadable uri
                hasPickedFile = false
            }
        }
    }

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
                    .testTag("upload_dialog_card"),
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Upload to Multi-Cloud",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero-knowledge encryption & cross-cloud sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Device File") },
                            icon = { Icon(Icons.Default.FileOpen, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Secure Note / Key") },
                            icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        // Device File Selection Mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!hasPickedFile) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select any file from device storage",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Documents, images, backups, keys, or code",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("browse_files_button")
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browse Device Files")
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fileName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${CryptoEngine.formatBytes(fileSizeBytes)} • $mimeType",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { filePickerLauncher.launch("*/*") },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Change", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (hasPickedFile) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = fileName,
                                onValueChange = {
                                    fileName = it
                                    selectedCategory = FileTypeCategory.fromMimeOrFilename(mimeType, it)
                                },
                                label = { Text("Display Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("file_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    } else {
                        // Direct Secure Note / Key Entry
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = {
                                fileName = it
                                selectedCategory = FileTypeCategory.fromMimeOrFilename("text/plain", it)
                            },
                            label = { Text("Title / File Name") },
                            placeholder = { Text("e.g. master_credentials.txt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("file_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = fileContent,
                            onValueChange = {
                                fileContent = it
                                fileSizeBytes = it.toByteArray(Charsets.UTF_8).size.toLong()
                            },
                            label = { Text("Confidential Content / Key / Note") },
                            placeholder = { Text("Type or paste confidential notes, recovery seed, or tokens here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("file_content_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (connectedProviders.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No cloud providers connected yet. File will be encrypted and saved to your Local Vault. You can connect cloud providers in the Clouds tab anytime.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Cloud Storage Destinations
                        val allSelected = connectedProviders.isNotEmpty() && selectedProviders.size == connectedProviders.size

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cloud Storage Destinations:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (allSelected) "All ${connectedProviders.size} clouds selected"
                                    else "${selectedProviders.size} of ${connectedProviders.size} clouds selected",
                                    fontSize = 11.sp,
                                    color = if (selectedProviders.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            FilterChip(
                                selected = allSelected,
                                onClick = {
                                    selectedProviders = if (allSelected) {
                                        emptySet()
                                    } else {
                                        connectedProviders.map { it.id }.toSet()
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (allSelected) "Deselect All" else "Select All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("select_all_clouds_chip"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            connectedProviders.forEach { prov ->
                                val isChecked = selectedProviders.contains(prov.id)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedProviders = if (isChecked) {
                                            selectedProviders - prov.id
                                        } else {
                                            selectedProviders + prov.id
                                        }
                                    },
                                    label = { Text(prov.name, fontSize = 11.sp) },
                                    leadingIcon = {
                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(prov.type.brandColorHex)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(prov.type.brandColorHex).copy(alpha = 0.5f))
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(prov.type.brandColorHex).copy(alpha = 0.18f),
                                        selectedLabelColor = Color(prov.type.brandColorHex)
                                    ),
                                    modifier = Modifier.testTag("cloud_option_${prov.id}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Project Folder Selector
                    Text(
                        text = "Project Folder Organization:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId == null,
                            onClick = { selectedProjectId = null },
                            label = { Text("Uncategorized") },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        projects.forEach { proj ->
                            FilterChip(
                                selected = selectedProjectId == proj.id,
                                onClick = { selectedProjectId = proj.id },
                                label = { Text(proj.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(proj.colorHex).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(proj.colorHex)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // File Type Category
                    Text(
                        text = "Category:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FileTypeCategory.values().forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // End-to-End Encryption Toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (enableEncryption) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (enableEncryption) Icons.Default.Security else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (enableEncryption) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "End-to-End Encryption (AES-256-GCM)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (enableEncryption) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Client-side cipher. Clouds only store encrypted ciphertext.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableEncryption,
                                onCheckedChange = { enableEncryption = it },
                                modifier = Modifier.testTag("encryption_toggle"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom actions
                    val canUpload = fileName.isNotBlank() && (hasPickedFile || fileContent.isNotBlank()) && (connectedProviders.isEmpty() || selectedProviders.isNotEmpty())

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
                                if (canUpload) {
                                    val size = if (fileSizeBytes > 0) fileSizeBytes else fileContent.toByteArray(Charsets.UTF_8).size.toLong()
                                    onUpload(
                                        fileName.trim(),
                                        fileName.trim(),
                                        mimeType,
                                        fileContent,
                                        size,
                                        selectedCategory,
                                        selectedProjectId,
                                        enableEncryption,
                                        selectedProviders
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = canUpload,
                            modifier = Modifier.testTag("confirm_upload_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (connectedProviders.isEmpty()) "Encrypt & Save to Local Vault"
                                else if (selectedProviders.isEmpty()) "Select a Cloud"
                                else "Encrypt & Upload (${selectedProviders.size} clouds)"
                            )
                        }
                    }
                }
            }
        }
    }
}
