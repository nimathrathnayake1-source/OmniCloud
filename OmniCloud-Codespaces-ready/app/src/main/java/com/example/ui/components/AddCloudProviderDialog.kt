package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CloudProviderEntity
import com.example.model.CloudProviderType

enum class CloudCatalogItem(
    val id: String,
    val brandName: String,
    val type: CloudProviderType,
    val subtitle: String,
    val freeQuotaBadge: String,
    val brandColorHex: Long,
    val authTypeDesc: String
) {
    GOOGLE_DRIVE(
        id = "google_drive",
        brandName = "Google Drive",
        type = CloudProviderType.GOOGLE_DRIVE,
        subtitle = "Google Cloud Storage with OAuth2 Authorization",
        freeQuotaBadge = "15 GB Free",
        brandColorHex = 0xFF4285F4,
        authTypeDesc = "Google Account Sign-In & Drive Scope"
    ),
    DROPBOX(
        id = "dropbox",
        brandName = "Dropbox",
        type = CloudProviderType.DROPBOX,
        subtitle = "Scoped App Folder storage & backup",
        freeQuotaBadge = "2 GB Free",
        brandColorHex = 0xFF0061FF,
        authTypeDesc = "Dropbox OAuth2 Authorization"
    ),
    MICROSOFT_ONEDRIVE(
        id = "onedrive",
        brandName = "Microsoft OneDrive",
        type = CloudProviderType.MICROSOFT_ONEDRIVE,
        subtitle = "Microsoft 365 Cloud Graph Storage",
        freeQuotaBadge = "5 GB Free",
        brandColorHex = 0xFF0078D4,
        authTypeDesc = "Microsoft Account Sign-In"
    ),
    AWS_S3(
        id = "aws_s3",
        brandName = "Amazon AWS S3",
        type = CloudProviderType.AWS_S3,
        subtitle = "Scalable Enterprise Object Storage",
        freeQuotaBadge = "Scalable S3",
        brandColorHex = 0xFFFF9900,
        authTypeDesc = "Bucket & IAM Access Keys"
    ),
    NEXTCLOUD(
        id = "nextcloud",
        brandName = "Nextcloud / ownCloud",
        type = CloudProviderType.NEXTCLOUD,
        subtitle = "Private Self-Hosted Cloud Storage",
        freeQuotaBadge = "Self-Hosted",
        brandColorHex = 0xFF0082C9,
        authTypeDesc = "WebDAV & App Password"
    ),
    BOX(
        id = "box",
        brandName = "Box Cloud",
        type = CloudProviderType.BOX,
        subtitle = "Secure Enterprise Content Management",
        freeQuotaBadge = "10 GB Free",
        brandColorHex = 0xFF0061D5,
        authTypeDesc = "Box API Token Authorization"
    ),
    PCLOUD(
        id = "pcloud",
        brandName = "pCloud Crypto",
        type = CloudProviderType.PCLOUD,
        subtitle = "Swiss Zero-Knowledge Cloud Storage",
        freeQuotaBadge = "10 GB Free",
        brandColorHex = 0xFF00B0FF,
        authTypeDesc = "pCloud API Authorization"
    ),
    CUSTOM_OWN_CLOUD(
        id = "custom_server",
        brandName = "Add Own Cloud / Server",
        type = CloudProviderType.CUSTOM_SERVER,
        subtitle = "Synology NAS, QNAP, MinIO, or Custom WebDAV",
        freeQuotaBadge = "Private NAS",
        brandColorHex = 0xFF8B5CF6,
        authTypeDesc = "WebDAV / S3 / NAS Endpoint"
    )
}

@Composable
fun AddCloudProviderDialog(
    existingProviderIds: Set<String>,
    onDismiss: () -> Unit,
    onConnectProvider: (CloudProviderEntity) -> Unit
) {
    var selectedItem by remember { mutableStateOf<CloudCatalogItem?>(null) }

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
                    .testTag("add_cloud_provider_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                AnimatedContent(
                    targetState = selectedItem,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "catalog_transition"
                ) { item ->
                    if (item == null) {
                        // View 1: Catalog of Available Cloud Providers
                        CloudProviderCatalogView(
                            existingProviderIds = existingProviderIds,
                            onSelectItem = { selectedItem = it },
                            onDismiss = onDismiss
                        )
                    } else {
                        // View 2: Dedicated Cloud Provider Authorization & Setup View
                        CloudProviderAuthorizationView(
                            item = item,
                            onBack = { selectedItem = null },
                            onConnect = { entity ->
                                onConnectProvider(entity)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudProviderCatalogView(
    existingProviderIds: Set<String>,
    onSelectItem: (CloudCatalogItem) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connect Cloud Storage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a provider to authorize access & sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Catalog List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CloudCatalogItem.values().forEach { item ->
                val isAlreadyConnected = existingProviderIds.contains(item.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAlreadyConnected) { onSelectItem(item) }
                        .testTag("catalog_item_${item.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlreadyConnected)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProviderAvatar(type = item.type, size = 42)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.brandName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(item.brandColorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.freeQuotaBadge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(item.brandColorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isAlreadyConnected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Connected",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Connect",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloudProviderAuthorizationView(
    item: CloudCatalogItem,
    onBack: () -> Unit,
    onConnect: (CloudProviderEntity) -> Unit
) {
    // Specific state per provider type
    var userEmail by remember {
        mutableStateOf(
            when (item) {
                CloudCatalogItem.GOOGLE_DRIVE -> "nimathrathnayake1@gmail.com"
                CloudCatalogItem.DROPBOX -> "user.dropbox@gmail.com"
                CloudCatalogItem.MICROSOFT_ONEDRIVE -> "user.vault@outlook.com"
                CloudCatalogItem.AWS_S3 -> "s3-admin@vault-cloud"
                CloudCatalogItem.NEXTCLOUD -> "admin@nextcloud.lan"
                CloudCatalogItem.BOX -> "user@box-enterprise.com"
                CloudCatalogItem.PCLOUD -> "security@pcloud.ch"
                CloudCatalogItem.CUSTOM_OWN_CLOUD -> "admin@nas.local"
            }
        )
    }

    var serverName by remember { mutableStateOf(if (item == CloudCatalogItem.CUSTOM_OWN_CLOUD) "Home NAS / WebDAV" else item.brandName) }
    var endpointUrl by remember {
        mutableStateOf(
            when (item) {
                CloudCatalogItem.GOOGLE_DRIVE -> "https://www.googleapis.com/drive/v3"
                CloudCatalogItem.DROPBOX -> "https://api.dropboxapi.com/2"
                CloudCatalogItem.MICROSOFT_ONEDRIVE -> "https://graph.microsoft.com/v1.0/me/drive"
                CloudCatalogItem.AWS_S3 -> "https://s3.us-east-1.amazonaws.com"
                CloudCatalogItem.NEXTCLOUD -> "https://nextcloud.lan/remote.php/dav/files"
                CloudCatalogItem.BOX -> "https://api.box.com/2.0"
                CloudCatalogItem.PCLOUD -> "https://api.pcloud.com"
                CloudCatalogItem.CUSTOM_OWN_CLOUD -> "https://nas.local:5005/webdav"
            }
        )
    }

    var bucketOrFolder by remember {
        mutableStateOf(
            when (item) {
                CloudCatalogItem.GOOGLE_DRIVE -> "omnicloud-secure-drive"
                CloudCatalogItem.DROPBOX -> "Apps/OmniCloudSync"
                CloudCatalogItem.MICROSOFT_ONEDRIVE -> "VaultSync"
                CloudCatalogItem.AWS_S3 -> "omnicloud-encrypted-vault"
                CloudCatalogItem.NEXTCLOUD -> "/SecureVault/Storage"
                CloudCatalogItem.BOX -> "OmniBoxRoot"
                CloudCatalogItem.PCLOUD -> "CryptoFolder"
                CloudCatalogItem.CUSTOM_OWN_CLOUD -> "/OmniVault/Storage"
            }
        )
    }

    var quotaGbText by remember {
        mutableStateOf(
            when (item) {
                CloudCatalogItem.GOOGLE_DRIVE -> "15"
                CloudCatalogItem.DROPBOX -> "2"
                CloudCatalogItem.MICROSOFT_ONEDRIVE -> "5"
                CloudCatalogItem.AWS_S3 -> "100"
                CloudCatalogItem.NEXTCLOUD -> "50"
                CloudCatalogItem.BOX -> "10"
                CloudCatalogItem.PCLOUD -> "10"
                CloudCatalogItem.CUSTOM_OWN_CLOUD -> "250"
            }
        )
    }

    var awsRegion by remember { mutableStateOf("us-east-1") }
    var accessKeyId by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }
    var appPassword by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(true) }
    var onlyEncrypted by remember { mutableStateOf(true) }
    var customArchitecture by remember { mutableStateOf(CloudProviderType.CUSTOM_SERVER) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Navigation & Provider Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Catalog")
            }
            Spacer(modifier = Modifier.width(4.dp))
            ProviderAvatar(type = item.type, size = 36)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.brandName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.authTypeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Provider Specific Authorization & Permission Card
        when (item) {
            CloudCatalogItem.GOOGLE_DRIVE -> {
                // Official Google Drive OAuth / Permission Screen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sign in with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "to continue to OmniCloud Vault",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Google Account Email") },
                            modifier = Modifier.fillMaxWidth().testTag("google_email_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "OmniCloud will be granted access to:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "See, edit, create, and delete only the specific Google Drive files you use with this app (https://www.googleapis.com/auth/drive.file)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "All uploaded payloads are protected with client-side AES-256-GCM Zero-Knowledge encryption.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CloudCatalogItem.DROPBOX -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Authorize Dropbox App Folder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Grants scoped access to store encrypted sync files in /Apps/OmniCloudSync",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Dropbox Account Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            CloudCatalogItem.MICROSOFT_ONEDRIVE -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sign in with Microsoft Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Grants permission to create and sync your files to OneDrive (Files.ReadWrite)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Microsoft / Outlook Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            CloudCatalogItem.AWS_S3 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AWS S3 Credentials & Bucket Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bucketOrFolder,
                            onValueChange = { bucketOrFolder = it },
                            label = { Text("S3 Bucket Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = awsRegion,
                            onValueChange = { awsRegion = it },
                            label = { Text("AWS Region (e.g. us-east-1, eu-west-1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accessKeyId,
                            onValueChange = { accessKeyId = it },
                            label = { Text("AWS Access Key ID") },
                            placeholder = { Text("AKIAIOSFODNN7EXAMPLE") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = secretKey,
                            onValueChange = { secretKey = it },
                            label = { Text("AWS Secret Access Key") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            CloudCatalogItem.NEXTCLOUD -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nextcloud / ownCloud Instance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endpointUrl,
                            onValueChange = { endpointUrl = it },
                            label = { Text("Nextcloud WebDAV URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Nextcloud Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = appPassword,
                            onValueChange = { appPassword = it },
                            label = { Text("App Password / Token") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            CloudCatalogItem.BOX, CloudCatalogItem.PCLOUD -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${item.brandName} Authorization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("Account Identifier / Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bucketOrFolder,
                            onValueChange = { bucketOrFolder = it },
                            label = { Text("Target Storage Folder") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            CloudCatalogItem.CUSTOM_OWN_CLOUD -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Custom Cloud Storage / NAS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CloudProviderType.CUSTOM_SERVER to "WebDAV / NAS",
                                CloudProviderType.NEXTCLOUD to "Nextcloud",
                                CloudProviderType.AWS_S3 to "MinIO / S3"
                            ).forEach { (arch, label) ->
                                FilterChip(
                                    selected = customArchitecture == arch,
                                    onClick = { customArchitecture = arch },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = serverName,
                            onValueChange = { serverName = it },
                            label = { Text("Server Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endpointUrl,
                            onValueChange = { endpointUrl = it },
                            label = { Text("Endpoint URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bucketOrFolder,
                            onValueChange = { bucketOrFolder = it },
                            label = { Text("Target Bucket / Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { userEmail = it },
                            label = { Text("User / Email ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quota & Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = quotaGbText,
                onValueChange = { quotaGbText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Storage Quota (GB)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = bucketOrFolder,
                onValueChange = { bucketOrFolder = it },
                label = { Text("Remote Folder / Bucket") },
                modifier = Modifier.weight(1.3f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Security Toggles
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Sync New Uploads", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically replicate uploaded files to this provider", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce Client-Side E2EE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Only transmit zero-knowledge encrypted payloads", fontSize = 10.sp, color = Color(0xFF10B981))
                    }
                    Switch(
                        checked = onlyEncrypted,
                        onCheckedChange = { onlyEncrypted = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onBack) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val parsedQuota = quotaGbText.toLongOrNull() ?: 10L
                    val totalBytes = parsedQuota * 1024L * 1024L * 1024L
                    val providerId = if (item == CloudCatalogItem.CUSTOM_OWN_CLOUD) {
                        "custom_${System.currentTimeMillis()}"
                    } else {
                        item.id
                    }

                    val providerType = if (item == CloudCatalogItem.CUSTOM_OWN_CLOUD) customArchitecture else item.type
                    val finalEndpoint = if (item == CloudCatalogItem.AWS_S3) {
                        "https://s3.$awsRegion.amazonaws.com"
                    } else {
                        endpointUrl.trim()
                    }

                    val entity = CloudProviderEntity(
                        id = providerId,
                        name = serverName.trim(),
                        type = providerType,
                        isConnected = true,
                        accountEmail = userEmail.trim().ifEmpty { "user@vault.storage" },
                        storageTotalBytes = totalBytes,
                        storageUsedBytes = 0L,
                        autoSyncEnabled = autoSync,
                        endpointUrl = finalEndpoint,
                        bucketName = bucketOrFolder.trim(),
                        onlySyncEncrypted = onlyEncrypted,
                        lastSyncTime = System.currentTimeMillis()
                    )
                    onConnect(entity)
                },
                modifier = Modifier.testTag("confirm_authorize_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(item.brandColorHex)
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (item) {
                        CloudCatalogItem.GOOGLE_DRIVE -> "Allow & Connect Google Drive"
                        CloudCatalogItem.DROPBOX -> "Authorize & Connect Dropbox"
                        CloudCatalogItem.MICROSOFT_ONEDRIVE -> "Sign In & Connect OneDrive"
                        CloudCatalogItem.AWS_S3 -> "Verify & Connect S3 Bucket"
                        CloudCatalogItem.NEXTCLOUD -> "Connect Nextcloud Instance"
                        else -> "Authorize & Connect Cloud"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
