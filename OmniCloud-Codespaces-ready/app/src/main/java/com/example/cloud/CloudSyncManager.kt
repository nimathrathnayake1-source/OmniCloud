package com.example.cloud

import android.util.Base64
import com.example.data.OmniCloudRepository
import com.example.model.CloudProviderEntity
import com.example.model.CloudProviderType
import com.example.model.FileCloudSyncEntity
import com.example.model.SyncLogEntity
import com.example.model.SyncStatus
import com.example.model.VaultFileEntity
import com.example.security.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/**
 * Real sync implementation for WebDAV/Nextcloud/self-hosted endpoints.
 * Unsupported providers are reported as unsupported instead of being marked SYNCED.
 */
data class ActiveTransfer(
    val transferId: String,
    val fileId: Long,
    val fileName: String,
    val providerId: String,
    val providerName: String,
    val progress: Float,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val status: SyncStatus
)

class CloudSyncManager(
    private val repository: OmniCloudRepository,
    private val credentialStore: CredentialStore
) {
    private val client = OkHttpClient.Builder().build()
    private val _activeTransfers = MutableStateFlow<List<ActiveTransfer>>(emptyList())
    val activeTransfers: StateFlow<List<ActiveTransfer>> = _activeTransfers.asStateFlow()
    private val _isGlobalSyncing = MutableStateFlow(false)
    val isGlobalSyncing: StateFlow<Boolean> = _isGlobalSyncing.asStateFlow()

    suspend fun syncFileToProviders(
        file: VaultFileEntity,
        targetProviderIds: Set<String>? = null,
        onProgress: ((providerId: String, progress: Float) -> Unit)? = null
    ) {
        val connected = repository.getConnectedProvidersList()
        val providers = if (targetProviderIds == null || targetProviderIds.isEmpty()) connected
        else connected.filter { it.id in targetProviderIds }
        if (providers.isEmpty()) {
            repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName="None", action="SYNC_SKIPPED", status="WARNING", details="No connected provider selected."))
            return
        }
        for (provider in providers) uploadSingleFileToProvider(file, provider, onProgress)
    }

    private suspend fun uploadSingleFileToProvider(
        file: VaultFileEntity,
        provider: CloudProviderEntity,
        onProgress: ((String, Float) -> Unit)?
    ) = withContext(Dispatchers.IO) {
        val supported = provider.type == CloudProviderType.WEBDAV ||
                provider.type == CloudProviderType.NEXTCLOUD ||
                provider.type == CloudProviderType.CUSTOM_SERVER
        if (!supported) {
            repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName=provider.name, action="SYNC_UNSUPPORTED", status="WARNING", details="${provider.type.brandName} requires its provider-specific OAuth/API integration and is not simulated."))
            return@withContext
        }
        if (provider.onlySyncEncrypted && !file.isEncrypted) {
            repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName=provider.name, action="SYNC_BLOCKED", status="WARNING", details="Provider only accepts encrypted files."))
            return@withContext
        }
        val endpoint = provider.endpointUrl.trimEnd('/')
        if (endpoint.isBlank() || !endpoint.startsWith("http")) {
            fail(file, provider, "Invalid WebDAV endpoint URL")
            return@withContext
        }
        val password = credentialStore.getPassword(provider.id)
        if (password.isNullOrBlank()) {
            fail(file, provider, "No password/app-password saved for this provider")
            return@withContext
        }

        val transferId = UUID.randomUUID().toString()
        val remoteDir = provider.bucketName.trim('/').ifBlank { "OmniCloud/Vault" }
        val remotePath = "$endpoint/${remoteDir.split('/').joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }}/${java.net.URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")}"
        val sync = FileCloudSyncEntity(fileId=file.id, providerId=provider.id, providerType=provider.type, syncStatus=SyncStatus.SYNCING, remotePath=remotePath, lastSyncedAt=System.currentTimeMillis(), remoteVersionId="", bytesTransferred=0L)
        repository.upsertSync(sync)
        setTransfer(ActiveTransfer(transferId, file.id, file.name, provider.id, provider.name, 0f, 0L, file.sizeBytes, SyncStatus.SYNCING))

        try {
            val rawPayload = Base64.decode(file.contentData, Base64.NO_WRAP)
            ensureWebDavDirectories(endpoint, remoteDir, provider.accountEmail, password)
            val body = rawPayload.toRequestBody(providerMime(file.mimeType).toMediaTypeOrNull())
            val request = Request.Builder().url(remotePath)
                .put(body)
                .header("Authorization", Credentials.basic(provider.accountEmail, password))
                .header("X-OmniCloud-Checksum", file.sha256Checksum)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("WebDAV upload failed: HTTP ${response.code}")
                val completed = sync.copy(syncStatus=SyncStatus.SYNCED, lastSyncedAt=System.currentTimeMillis(), remoteVersionId=response.header("ETag").orEmpty(), bytesTransferred=rawPayload.size.toLong())
                repository.upsertSync(completed)
                val providerBytes = (provider.storageUsedBytes + rawPayload.size).coerceAtMost(provider.storageTotalBytes)
                repository.updateProvider(provider.copy(storageUsedBytes=providerBytes, lastSyncTime=System.currentTimeMillis()))
                repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName=provider.name, action="SYNC_COMPLETE", status="SUCCESS", details="Uploaded encrypted payload to WebDAV. HTTP ${response.code}."))
                setTransfer(ActiveTransfer(transferId, file.id, file.name, provider.id, provider.name, 1f, rawPayload.size.toLong(), rawPayload.size.toLong(), SyncStatus.SYNCED))
            }
        } catch (e: Exception) {
            repository.upsertSync(sync.copy(syncStatus=SyncStatus.ERROR, lastSyncedAt=System.currentTimeMillis()))
            repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName=provider.name, action="SYNC_ERROR", status="ERROR", details=e.message ?: "WebDAV upload failed"))
            setTransfer(ActiveTransfer(transferId, file.id, file.name, provider.id, provider.name, 0f, 0L, file.sizeBytes, SyncStatus.ERROR))
        }
        _activeTransfers.value = _activeTransfers.value.filterNot { it.transferId == transferId }
        onProgress?.invoke(provider.id, 1f)
    }


    private fun ensureWebDavDirectories(endpoint: String, remoteDir: String, username: String, password: String) {
        var current = endpoint
        val parts = remoteDir.trim('/').split('/').filter { it.isNotBlank() }
        for (part in parts) {
            current += "/" + java.net.URLEncoder.encode(part, "UTF-8").replace("+", "%20")
            val request = Request.Builder().url(current).method("MKCOL", null)
                .header("Authorization", Credentials.basic(username, password)).build()
            client.newCall(request).execute().use { response ->
                // 201 = created, 405 = already exists. Some servers return 301/409 for existing/nested paths.
                if (!response.isSuccessful && response.code !in setOf(301, 302, 405, 409)) {
                    throw IllegalStateException("WebDAV directory creation failed: HTTP ${response.code}")
                }
            }
        }
    }

    private fun providerMime(mime: String): String = mime.ifBlank { "application/octet-stream" }

    private suspend fun fail(file: VaultFileEntity, provider: CloudProviderEntity, message: String) {
        repository.insertLog(SyncLogEntity(fileId=file.id, fileName=file.name, providerName=provider.name, action="SYNC_ERROR", status="ERROR", details=message))
    }

    private fun setTransfer(transfer: ActiveTransfer) {
        _activeTransfers.value = _activeTransfers.value.filterNot { it.transferId == transfer.transferId } + transfer
    }

    suspend fun syncAllFilesAcrossAllClouds(files: List<VaultFileEntity>) {
        if (_isGlobalSyncing.value) return
        _isGlobalSyncing.value = true
        try {
            val providers = repository.getConnectedProvidersList().filter { it.autoSyncEnabled }.map { it.id }.toSet()
            for (file in files) syncFileToProviders(file, providers)
        } finally { _isGlobalSyncing.value = false }
    }
}
