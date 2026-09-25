package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CloudProviderType(
    val brandName: String,
    val brandColorHex: Long,
    val defaultQuotaBytes: Long,
    val defaultQuotaLabel: String,
    val iconKey: String
) {
    GOOGLE_DRIVE("Google Drive", 0xFF4285F4, 15L * 1024 * 1024 * 1024, "15 GB Free", "google_drive"),
    DROPBOX("Dropbox", 0xFF0061FE, 2L * 1024 * 1024 * 1024, "2 GB Basic", "dropbox"),
    MICROSOFT_ONEDRIVE("OneDrive", 0xFF0078D4, 5L * 1024 * 1024 * 1024, "5 GB Personal", "onedrive"),
    AWS_S3("Amazon AWS S3", 0xFFFF9900, 100L * 1024 * 1024 * 1024, "100 GB Bucket", "aws_s3"),
    BOX("Box Cloud", 0xFF0061D5, 10L * 1024 * 1024 * 1024, "10 GB Standard", "box"),
    NEXTCLOUD("Nextcloud Vault", 0xFF0082C9, 50L * 1024 * 1024 * 1024, "50 GB Private", "nextcloud"),
    PCLOUD("pCloud Crypto", 0xFF17C4A5, 10L * 1024 * 1024 * 1024, "10 GB Swiss", "pcloud"),
    CUSTOM_SERVER("Custom / Self-Hosted", 0xFF6366F1, 200L * 1024 * 1024 * 1024, "200 GB Private", "custom_server"),
    WEBDAV("WebDAV / NAS", 0xFF0D9488, 500L * 1024 * 1024 * 1024, "500 GB NAS", "webdav")
}

@Entity(tableName = "cloud_providers")
data class CloudProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CloudProviderType,
    val isConnected: Boolean,
    val accountEmail: String,
    val storageTotalBytes: Long,
    val storageUsedBytes: Long,
    val autoSyncEnabled: Boolean,
    val endpointUrl: String = "",
    val bucketName: String = "",
    val lastSyncTime: Long = 0L,
    val onlySyncEncrypted: Boolean = false,
    val maxFileSizeBytes: Long = 500L * 1024 * 1024 // 500 MB default max file size
) {
    val usedPercentage: Float
        get() = if (storageTotalBytes > 0) {
            (storageUsedBytes.toFloat() / storageTotalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val availableBytes: Long
        get() = (storageTotalBytes - storageUsedBytes).coerceAtLeast(0L)
}
