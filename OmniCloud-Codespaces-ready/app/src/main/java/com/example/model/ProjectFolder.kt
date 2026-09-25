package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_folders")
data class ProjectFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val colorHex: Long,
    val iconName: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
