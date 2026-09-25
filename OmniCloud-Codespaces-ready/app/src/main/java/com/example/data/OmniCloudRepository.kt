package com.example.data

import com.example.model.CloudProviderEntity
import com.example.model.FileCloudSyncEntity
import com.example.model.FileTypeCategory
import com.example.model.ProjectFolderEntity
import com.example.model.SyncLogEntity
import com.example.model.VaultFileEntity
import kotlinx.coroutines.flow.Flow

class OmniCloudRepository(
    private val fileDao: FileDao,
    private val projectDao: ProjectDao,
    private val cloudProviderDao: CloudProviderDao,
    private val syncDao: SyncDao
) {
    val allFiles: Flow<List<VaultFileEntity>> = fileDao.getAllFiles()
    val allProjects: Flow<List<ProjectFolderEntity>> = projectDao.getAllProjects()
    val allProviders: Flow<List<CloudProviderEntity>> = cloudProviderDao.getAllProviders()
    val connectedProviders: Flow<List<CloudProviderEntity>> = cloudProviderDao.getConnectedProviders()
    val allSyncs: Flow<List<FileCloudSyncEntity>> = syncDao.getAllSyncs()
    val allSyncLogs: Flow<List<SyncLogEntity>> = syncDao.getAllSyncLogs()

    fun getFilesByCategory(category: FileTypeCategory): Flow<List<VaultFileEntity>> =
        fileDao.getFilesByCategory(category)

    fun getFilesByProject(projectId: Long): Flow<List<VaultFileEntity>> =
        fileDao.getFilesByProject(projectId)

    fun searchFiles(query: String): Flow<List<VaultFileEntity>> =
        fileDao.searchFiles(query)

    fun getFileById(id: Long): Flow<VaultFileEntity?> =
        fileDao.getFileById(id)

    suspend fun getFileByIdDirect(id: Long): VaultFileEntity? =
        fileDao.getFileByIdDirect(id)

    suspend fun insertFile(file: VaultFileEntity): Long =
        fileDao.insertFile(file)

    suspend fun updateFile(file: VaultFileEntity) =
        fileDao.updateFile(file)

    suspend fun deleteFile(fileId: Long) {
        syncDao.deleteSyncsForFile(fileId)
        fileDao.deleteFileById(fileId)
    }

    suspend fun insertProject(project: ProjectFolderEntity): Long =
        projectDao.insertProject(project)

    suspend fun deleteProject(projectId: Long) =
        projectDao.deleteProjectById(projectId)

    suspend fun updateProvider(provider: CloudProviderEntity) =
        cloudProviderDao.updateProvider(provider)

    suspend fun insertProvider(provider: CloudProviderEntity) =
        cloudProviderDao.insertProvider(provider)

    suspend fun deleteProvider(providerId: String) =
        cloudProviderDao.deleteProvider(providerId)

    suspend fun getConnectedProvidersList(): List<CloudProviderEntity> =
        cloudProviderDao.getConnectedProvidersList()

    suspend fun getProviderById(id: String): CloudProviderEntity? =
        cloudProviderDao.getProviderById(id)

    fun getSyncsForFile(fileId: Long): Flow<List<FileCloudSyncEntity>> =
        syncDao.getSyncsForFile(fileId)

    suspend fun getSyncsForFileDirect(fileId: Long): List<FileCloudSyncEntity> =
        syncDao.getSyncsForFileDirect(fileId)

    suspend fun upsertSync(sync: FileCloudSyncEntity): Long =
        syncDao.insertOrReplaceSync(sync)

    suspend fun deleteSync(fileId: Long, providerId: String) =
        syncDao.deleteSync(fileId, providerId)

    suspend fun insertLog(log: SyncLogEntity): Long =
        syncDao.insertSyncLog(log)

    suspend fun clearLogs() =
        syncDao.clearSyncLogs()
}
