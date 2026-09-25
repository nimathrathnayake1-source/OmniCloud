package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val fileId: Long? = null,
    val fileName: String,
    val providerName: String,
    val action: String, // "UPLOAD", "ENCRYPT", "SYNC", "VERIFY", "CONNECT"
    val status: String, // "SUCCESS", "IN_PROGRESS", "ERROR"
    val details: String
)
