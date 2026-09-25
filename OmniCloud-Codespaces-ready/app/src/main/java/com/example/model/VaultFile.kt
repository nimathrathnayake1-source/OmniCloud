package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val category: FileTypeCategory,
    val projectId: Long?,
    val isEncrypted: Boolean,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val sha256Checksum: String,
    val cipherSaltBase64: String = "",
    val cipherIvBase64: String = "",
    val contentData: String = "", // Base64 payload or raw text or cached content
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
