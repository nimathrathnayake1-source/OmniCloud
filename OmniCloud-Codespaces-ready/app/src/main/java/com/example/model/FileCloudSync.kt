package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncStatus {
    SYNCED,
    SYNCING,
    PENDING,
    ERROR,
    PAUSED
}

@Entity(
    tableName = "file_cloud_syncs",
    indices = [
        Index(value = ["fileId", "providerId"], unique = true)
    ]
)
data class FileCloudSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileId: Long,
    val providerId: String,
    val providerType: CloudProviderType,
    val syncStatus: SyncStatus,
    val remotePath: String,
    val lastSyncedAt: Long = 0L,
    val remoteVersionId: String = "",
    val errorMessage: String? = null,
    val bytesTransferred: Long = 0L
)
