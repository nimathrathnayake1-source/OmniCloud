package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CloudProviderType
import com.example.model.SyncStatus

fun getProviderIcon(type: CloudProviderType): ImageVector {
    return when (type) {
        CloudProviderType.GOOGLE_DRIVE -> Icons.Default.CloudDone
        CloudProviderType.DROPBOX -> Icons.Default.Folder
        CloudProviderType.MICROSOFT_ONEDRIVE -> Icons.Default.Cloud
        CloudProviderType.AWS_S3 -> Icons.Default.Storage
        CloudProviderType.BOX -> Icons.Default.Folder
        CloudProviderType.NEXTCLOUD -> Icons.Default.CloudSync
        CloudProviderType.PCLOUD -> Icons.Default.CloudQueue
        CloudProviderType.CUSTOM_SERVER -> Icons.Default.Dns
        CloudProviderType.WEBDAV -> Icons.Default.Storage
    }
}

@Composable
fun ProviderAvatar(
    type: CloudProviderType,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val brandColor = Color(type.brandColorHex)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(brandColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getProviderIcon(type),
            contentDescription = type.brandName,
            tint = brandColor,
            modifier = Modifier.size((size * 0.58).dp)
        )
    }
}

@Composable
fun ProviderChip(
    type: CloudProviderType,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val brandColor = Color(type.brandColorHex)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) brandColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, brandColor) else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = type.brandName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val (color, label, icon) = when (status) {
        SyncStatus.SYNCED -> Triple(Color(0xFF10B981), "Synced", Icons.Default.CloudDone)
        SyncStatus.SYNCING -> Triple(Color(0xFF3B82F6), "Syncing", Icons.Default.Sync)
        SyncStatus.PENDING -> Triple(Color(0xFFF59E0B), "Pending", Icons.Default.CloudQueue)
        SyncStatus.ERROR -> Triple(Color(0xFFEF4444), "Error", Icons.Default.Warning)
        SyncStatus.PAUSED -> Triple(Color(0xFF94A3B8), "Paused", Icons.Default.CloudQueue)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
