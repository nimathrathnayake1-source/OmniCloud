package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.CloudProviderEntity
import com.example.model.FileCloudSyncEntity
import com.example.model.FileTypeCategory
import com.example.model.ProjectFolderEntity
import com.example.model.SyncLogEntity
import com.example.model.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM vault_files ORDER BY isPinned DESC, createdAt DESC")
    fun getAllFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE category = :category ORDER BY isPinned DESC, createdAt DESC")
    fun getFilesByCategory(category: FileTypeCategory): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE projectId = :projectId ORDER BY isPinned DESC, createdAt DESC")
    fun getFilesByProject(projectId: Long): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    fun getFileById(id: Long): Flow<VaultFileEntity?>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getFileByIdDirect(id: Long): VaultFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFileEntity): Long

    @Update
    suspend fun updateFile(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("SELECT * FROM vault_files WHERE name LIKE '%' || :query || '%' OR originalName LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<VaultFileEntity>>
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project_folders ORDER BY isDefault DESC, name ASC")
    fun getAllProjects(): Flow<List<ProjectFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectFolderEntity): Long

    @Query("DELETE FROM project_folders WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface CloudProviderDao {
    @Query("SELECT * FROM cloud_providers ORDER BY isConnected DESC, name ASC")
    fun getAllProviders(): Flow<List<CloudProviderEntity>>

    @Query("SELECT * FROM cloud_providers WHERE isConnected = 1")
    fun getConnectedProviders(): Flow<List<CloudProviderEntity>>

    @Query("SELECT * FROM cloud_providers WHERE isConnected = 1")
    suspend fun getConnectedProvidersList(): List<CloudProviderEntity>

    @Query("SELECT * FROM cloud_providers WHERE id = :id")
    suspend fun getProviderById(id: String): CloudProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProviders(providers: List<CloudProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: CloudProviderEntity)

    @Update
    suspend fun updateProvider(provider: CloudProviderEntity)

    @Query("DELETE FROM cloud_providers WHERE id = :id")
    suspend fun deleteProvider(id: String)
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM file_cloud_syncs")
    fun getAllSyncs(): Flow<List<FileCloudSyncEntity>>

    @Query("SELECT * FROM file_cloud_syncs WHERE fileId = :fileId")
    fun getSyncsForFile(fileId: Long): Flow<List<FileCloudSyncEntity>>

    @Query("SELECT * FROM file_cloud_syncs WHERE fileId = :fileId")
    suspend fun getSyncsForFileDirect(fileId: Long): List<FileCloudSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceSync(sync: FileCloudSyncEntity): Long

    @Query("DELETE FROM file_cloud_syncs WHERE fileId = :fileId")
    suspend fun deleteSyncsForFile(fileId: Long)

    @Query("DELETE FROM file_cloud_syncs WHERE fileId = :fileId AND providerId = :providerId")
    suspend fun deleteSync(fileId: Long, providerId: String)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearSyncLogs()
}
