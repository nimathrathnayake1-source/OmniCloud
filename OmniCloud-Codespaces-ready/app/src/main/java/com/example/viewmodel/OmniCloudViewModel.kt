package com.example.viewmodel

import android.app.Application
import com.example.security.CredentialStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cloud.ActiveTransfer
import com.example.cloud.CloudSyncManager
import com.example.data.OmniCloudDatabase
import com.example.data.OmniCloudRepository
import com.example.model.CloudProviderEntity
import com.example.model.CloudProviderType
import com.example.model.FileCloudSyncEntity
import com.example.model.FileTypeCategory
import com.example.model.ProjectFolderEntity
import com.example.model.SyncLogEntity
import com.example.model.VaultFileEntity
import com.example.security.CryptoEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OmniCloudUiState(
    val files: List<VaultFileEntity> = emptyList(),
    val filteredFiles: List<VaultFileEntity> = emptyList(),
    val projects: List<ProjectFolderEntity> = emptyList(),
    val providers: List<CloudProviderEntity> = emptyList(),
    val syncsByFile: Map<Long, List<FileCloudSyncEntity>> = emptyMap(),
    val syncLogs: List<SyncLogEntity> = emptyList(),
    val activeTransfers: List<ActiveTransfer> = emptyList(),
    val isGlobalSyncing: Boolean = false,
    val selectedCategory: FileTypeCategory? = null,
    val selectedProjectId: Long? = null,
    val searchQuery: String = "",
    val isVaultLocked: Boolean = false,
    val masterPassphrase: String = "omnicloud-secure-master-2026",
    val isAppLocked: Boolean = false,
    val isBioSecurityOnAppOpenEnabled: Boolean = false,
    val isBioForMasterKeyEnabled: Boolean = true,
    val isBioForPersonalVaultEnabled: Boolean = true,
    val totalVaultSizeBytes: Long = 0L,
    val totalCloudQuotaBytes: Long = 0L,
    val totalCloudUsedBytes: Long = 0L,
    val encryptedFilesCount: Int = 0,
    val connectedCloudsCount: Int = 0
)

private data class RepoDataBundle(
    val files: List<VaultFileEntity>,
    val projects: List<ProjectFolderEntity>,
    val providers: List<CloudProviderEntity>,
    val syncs: List<FileCloudSyncEntity>,
    val logs: List<SyncLogEntity>
)

private data class FilterBundle(
    val query: String,
    val category: FileTypeCategory?,
    val projectId: Long?,
    val isLocked: Boolean,
    val passphrase: String
)

private data class BioSecurityBundle(
    val isAppLocked: Boolean,
    val isBioSecurityOnAppOpen: Boolean,
    val isBioForMasterKey: Boolean,
    val isBioForPersonalVault: Boolean
)

private data class ControlBundle(
    val filter: FilterBundle,
    val bio: BioSecurityBundle,
    val transfers: List<ActiveTransfer>,
    val isGlobalSync: Boolean
)

class OmniCloudViewModel(
    application: Application,
    private val repository: OmniCloudRepository,
    private val cloudSyncManager: CloudSyncManager
) : AndroidViewModel(application) {

    private val credentialStore = CredentialStore(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileTypeCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    private val _isVaultLocked = MutableStateFlow(false)
    val isVaultLocked = _isVaultLocked.asStateFlow()

    private val _masterPassphrase = MutableStateFlow("omnicloud-master-key-2026")
    val masterPassphrase = _masterPassphrase.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked = _isAppLocked.asStateFlow()

    private val _isBioSecurityOnAppOpen = MutableStateFlow(false)
    val isBioSecurityOnAppOpen = _isBioSecurityOnAppOpen.asStateFlow()

    private val _isBioForMasterKey = MutableStateFlow(true)
    val isBioForMasterKey = _isBioForMasterKey.asStateFlow()

    private val _isBioForPersonalVault = MutableStateFlow(true)
    val isBioForPersonalVault = _isBioForPersonalVault.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // 1. Group repository flows (5 flows)
    private val repoDataFlow: Flow<RepoDataBundle> = combine(
        repository.allFiles,
        repository.allProjects,
        repository.allProviders,
        repository.allSyncs,
        repository.allSyncLogs
    ) { files, projects, providers, syncs, logs ->
        RepoDataBundle(files, projects, providers, syncs, logs)
    }

    // 2. Group UI filter flows (5 flows)
    private val filterBundleFlow: Flow<FilterBundle> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedProjectId,
        _isVaultLocked,
        _masterPassphrase
    ) { q, cat, proj, locked, pass ->
        FilterBundle(q, cat, proj, locked, pass)
    }

    // 3. Group Bio-security flows (4 flows)
    private val bioSecurityBundleFlow: Flow<BioSecurityBundle> = combine(
        _isAppLocked,
        _isBioSecurityOnAppOpen,
        _isBioForMasterKey,
        _isBioForPersonalVault
    ) { appLocked, onAppOpen, masterKey, vault ->
        BioSecurityBundle(appLocked, onAppOpen, masterKey, vault)
    }

    // 4. Group control flows (4 flows)
    private val controlBundleFlow: Flow<ControlBundle> = combine(
        filterBundleFlow,
        bioSecurityBundleFlow,
        cloudSyncManager.activeTransfers,
        cloudSyncManager.isGlobalSyncing
    ) { filter, bio, transfers, isGlobalSync ->
        ControlBundle(filter, bio, transfers, isGlobalSync)
    }

    // 5. Combine into final UI state (2 flows)
    val uiState: StateFlow<OmniCloudUiState> = combine(
        repoDataFlow,
        controlBundleFlow
    ) { repo, control ->
        val filter = control.filter
        val bio = control.bio
        val syncsMap = repo.syncs.groupBy { it.fileId }

        val filtered = repo.files.filter { file ->
            val matchesQuery = filter.query.isBlank() ||
                    file.name.contains(filter.query, ignoreCase = true) ||
                    file.originalName.contains(filter.query, ignoreCase = true)

            val matchesCategory = filter.category == null || file.category == filter.category
            val matchesProject = filter.projectId == null || file.projectId == filter.projectId

            matchesQuery && matchesCategory && matchesProject
        }

        val totalSize = repo.files.sumOf { it.sizeBytes }
        val cloudQuota = repo.providers.filter { it.isConnected }.sumOf { it.storageTotalBytes }
        val cloudUsed = repo.providers.filter { it.isConnected }.sumOf { it.storageUsedBytes }
        val encryptedCount = repo.files.count { it.isEncrypted }
        val connectedCount = repo.providers.count { it.isConnected }

        OmniCloudUiState(
            files = repo.files,
            filteredFiles = filtered,
            projects = repo.projects,
            providers = repo.providers,
            syncsByFile = syncsMap,
            syncLogs = repo.logs,
            activeTransfers = control.transfers,
            isGlobalSyncing = control.isGlobalSync,
            selectedCategory = filter.category,
            selectedProjectId = filter.projectId,
            searchQuery = filter.query,
            isVaultLocked = filter.isLocked,
            masterPassphrase = filter.passphrase,
            isAppLocked = bio.isAppLocked,
            isBioSecurityOnAppOpenEnabled = bio.isBioSecurityOnAppOpen,
            isBioForMasterKeyEnabled = bio.isBioForMasterKey,
            isBioForPersonalVaultEnabled = bio.isBioForPersonalVault,
            totalVaultSizeBytes = totalSize,
            totalCloudQuotaBytes = cloudQuota,
            totalCloudUsedBytes = cloudUsed,
            encryptedFilesCount = encryptedCount,
            connectedCloudsCount = connectedCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OmniCloudUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: FileTypeCategory?) {
        _selectedCategory.value = category
    }

    fun setSelectedProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
        viewModelScope.launch {
            _toastEvent.emit("App secured with Biometric Shield")
        }
    }

    fun setBioSecurityOnAppOpen(enabled: Boolean) {
        _isBioSecurityOnAppOpen.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
        viewModelScope.launch {
            _toastEvent.emit(if (enabled) "Biometric App Lock enabled" else "Biometric App Lock disabled")
        }
    }

    fun setBioForMasterKey(enabled: Boolean) {
        _isBioForMasterKey.value = enabled
        viewModelScope.launch {
            _toastEvent.emit(if (enabled) "Biometrics required for Master Key" else "Master Key biometrics disabled")
        }
    }

    fun setBioForPersonalVault(enabled: Boolean) {
        _isBioForPersonalVault.value = enabled
        viewModelScope.launch {
            _toastEvent.emit(if (enabled) "Biometrics required for Vault Decryption" else "Vault Decryption biometrics disabled")
        }
    }

    fun setMasterPassphrase(passphrase: String) {
        _masterPassphrase.value = passphrase
        viewModelScope.launch {
            _toastEvent.emit("Master encryption passphrase updated successfully")
        }
    }

    fun lockVault() {
        _isVaultLocked.value = true
        viewModelScope.launch {
            _toastEvent.emit("Vault locked. Decryption requires Master Passphrase.")
        }
    }

    fun unlockVault(passphrase: String): Boolean {
        return if (passphrase == _masterPassphrase.value) {
            _isVaultLocked.value = false
            viewModelScope.launch {
                _toastEvent.emit("Vault unlocked successfully.")
            }
            true
        } else {
            viewModelScope.launch {
                _toastEvent.emit("Incorrect passphrase!")
            }
            false
        }
    }

    /**
     * Uploads and distributes any data into cloud service storage providers.
     * Features AES-256-GCM End-to-End Encryption if requested.
     * If user targets specific providers, only uploads to them ("only if needed").
     */
    fun uploadFile(
        name: String,
        originalName: String,
        mimeType: String,
        content: String,
        sizeBytes: Long,
        category: FileTypeCategory,
        projectId: Long?,
        enableEncryption: Boolean,
        targetProviderIds: Set<String>? // null or empty means all connected clouds
    ) {
        viewModelScope.launch {
            val finalName = if (enableEncryption && !name.endsWith(".enc")) "$name.enc" else name
            val finalContent: String
            val saltBase64: String
            val ivBase64: String
            val checksum: String
            val finalSize: Long

            if (enableEncryption) {
                // Real AES-256-GCM encryption with master key
                val encrypted = CryptoEngine.encrypt(content, _masterPassphrase.value)
                finalContent = encrypted.cipherTextBase64
                saltBase64 = encrypted.saltBase64
                ivBase64 = encrypted.ivBase64
                checksum = encrypted.sha256Checksum
                finalSize = finalContent.toByteArray(Charsets.UTF_8).size.toLong()

                repository.insertLog(
                    SyncLogEntity(
                        fileName = finalName,
                        providerName = "Client Crypto",
                        action = "ENCRYPT",
                        status = "SUCCESS",
                        details = "AES-256-GCM envelope generated. SHA-256: ${checksum.take(16)}..."
                    )
                )
            } else {
                finalContent = content
                saltBase64 = ""
                ivBase64 = ""
                checksum = CryptoEngine.calculateSha256(content)
                finalSize = sizeBytes.coerceAtLeast(content.toByteArray(Charsets.UTF_8).size.toLong())
            }

            val entity = VaultFileEntity(
                name = finalName,
                originalName = originalName,
                mimeType = mimeType,
                sizeBytes = finalSize,
                category = category,
                projectId = projectId,
                isEncrypted = enableEncryption,
                encryptionAlgorithm = if (enableEncryption) "AES-256-GCM (PBKDF2)" else "None (Plaintext)",
                sha256Checksum = checksum,
                cipherSaltBase64 = saltBase64,
                cipherIvBase64 = ivBase64,
                contentData = finalContent,
                isPinned = false
            )

            val fileId = repository.insertFile(entity)
            val savedEntity = entity.copy(id = fileId)

            repository.insertLog(
                SyncLogEntity(
                    fileId = fileId,
                    fileName = finalName,
                    providerName = "OmniVault",
                    action = "LOCAL_STORE",
                    status = "SUCCESS",
                    details = "Stored in project folder (ID: ${projectId ?: "Uncategorized"}). Size: ${CryptoEngine.formatBytes(finalSize)}"
                )
            )

            // Multi-cloud sync dispatch
            cloudSyncManager.syncFileToProviders(savedEntity, targetProviderIds)
            _toastEvent.emit("Uploaded '$finalName' to ${targetProviderIds?.size ?: "all"} cloud providers")
        }
    }

    fun syncFileToProviders(file: VaultFileEntity, targetProviderIds: Set<String>) {
        viewModelScope.launch {
            cloudSyncManager.syncFileToProviders(file, targetProviderIds)
            _toastEvent.emit("Sync initiated for '${file.name}'")
        }
    }

    fun desyncFileFromProvider(file: VaultFileEntity, providerId: String, providerName: String) {
        viewModelScope.launch {
            repository.deleteSync(file.id, providerId)
            // Adjust provider used bytes down
            val provider = repository.getProviderById(providerId)
            if (provider != null) {
                val updatedUsedBytes = (provider.storageUsedBytes - file.sizeBytes).coerceAtLeast(0L)
                repository.updateProvider(provider.copy(storageUsedBytes = updatedUsedBytes))
            }
            repository.insertLog(
                SyncLogEntity(
                    fileId = file.id,
                    fileName = file.name,
                    providerName = providerName,
                    action = "DE_SYNC",
                    status = "SUCCESS",
                    details = "Removed cloud replica from $providerName."
                )
            )
            _toastEvent.emit("De-synced '${file.name}' from $providerName")
        }
    }

    fun syncAllAcrossClouds() {
        viewModelScope.launch {
            val allFiles = uiState.value.files
            cloudSyncManager.syncAllFilesAcrossAllClouds(allFiles)
            _toastEvent.emit("Full cloud synchronization completed.")
        }
    }

    fun togglePin(file: VaultFileEntity) {
        viewModelScope.launch {
            repository.updateFile(file.copy(isPinned = !file.isPinned))
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            repository.deleteFile(fileId)
            _toastEvent.emit("File removed from vault and cloud replicas")
        }
    }

    fun createProject(name: String, description: String, colorHex: Long, iconName: String) {
        viewModelScope.launch {
            val id = repository.insertProject(
                ProjectFolderEntity(
                    name = name,
                    description = description,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
            _toastEvent.emit("Project folder '$name' created")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _toastEvent.emit("Project folder removed")
        }
    }

    fun toggleProviderConnection(provider: CloudProviderEntity) {
        viewModelScope.launch {
            val updated = provider.copy(isConnected = !provider.isConnected)
            repository.updateProvider(updated)
            val stateLabel = if (updated.isConnected) "Connected" else "Disconnected"
            repository.insertLog(
                SyncLogEntity(
                    fileName = "Provider Settings",
                    providerName = provider.name,
                    action = "CONFIG_CHANGE",
                    status = "SUCCESS",
                    details = "Cloud provider is now $stateLabel."
                )
            )
            _toastEvent.emit("${provider.name} $stateLabel")
        }
    }

    fun toggleProviderAutoSync(provider: CloudProviderEntity) {
        viewModelScope.launch {
            val updated = provider.copy(autoSyncEnabled = !provider.autoSyncEnabled)
            repository.updateProvider(updated)
            val label = if (updated.autoSyncEnabled) "enabled" else "disabled"
            _toastEvent.emit("Auto-sync $label for ${provider.name}")
        }
    }

    fun updateProviderSettings(provider: CloudProviderEntity) {
        viewModelScope.launch {
            repository.updateProvider(provider)
            val stateLabel = if (provider.isConnected) "Connected (${provider.accountEmail})" else "Disconnected"
            repository.insertLog(
                SyncLogEntity(
                    fileName = "Provider Settings",
                    providerName = provider.name,
                    action = "CONFIG_UPDATED",
                    status = "SUCCESS",
                    details = "$stateLabel • Quota: ${CryptoEngine.formatBytes(provider.storageTotalBytes)} • Target: ${provider.bucketName}"
                )
            )
            _toastEvent.emit("${provider.name} configured & connected successfully!")
        }
    }

    fun addCustomCloudServer(
        name: String,
        type: CloudProviderType,
        endpointUrl: String,
        bucketOrPath: String,
        accountEmail: String,
        password: String,
        quotaGb: Long,
        autoSync: Boolean,
        onlyEncrypted: Boolean
    ) {
        viewModelScope.launch {
            val id = "custom_${System.currentTimeMillis()}"
            val totalBytes = quotaGb * 1024L * 1024L * 1024L
            val server = CloudProviderEntity(
                id = id,
                name = name,
                type = type,
                isConnected = true,
                accountEmail = accountEmail,
                storageTotalBytes = totalBytes,
                storageUsedBytes = 0L,
                autoSyncEnabled = autoSync,
                endpointUrl = endpointUrl,
                bucketName = bucketOrPath,
                onlySyncEncrypted = onlyEncrypted
            )
            credentialStore.savePassword(id, password)
            repository.insertProvider(server)
            repository.insertLog(
                SyncLogEntity(
                    fileName = "Server Configuration",
                    providerName = name,
                    action = "SERVER_ADDED",
                    status = "SUCCESS",
                    details = "Custom server added ($endpointUrl). Target: $bucketOrPath"
                )
            )
            _toastEvent.emit("Cloud server '$name' added successfully")
        }
    }

    fun connectProviderWithAuth(provider: CloudProviderEntity) {
        viewModelScope.launch {
            val realIntegration = provider.type == CloudProviderType.WEBDAV ||
                    provider.type == CloudProviderType.NEXTCLOUD ||
                    provider.type == CloudProviderType.CUSTOM_SERVER
            if (!realIntegration) {
                repository.insertLog(
                    SyncLogEntity(fileName = "Cloud Authorization", providerName = provider.name, action = "AUTH_NOT_CONFIGURED", status = "WARNING", details = "${provider.type.brandName} is not connected because this build does not fake OAuth/API authorization. Use Add Your Own Cloud Server for WebDAV/Nextcloud/S3-compatible endpoints." )
                )
                _toastEvent.emit("${provider.name}: provider authentication is not configured in this build")
                return@launch
            }
            repository.insertProvider(provider)
            repository.insertLog(SyncLogEntity(fileName = "Cloud Authorization", providerName = provider.name, action = "AUTH_CONNECTED", status = "SUCCESS", details = "Provider configuration saved. Uploads use the configured endpoint."))
            _toastEvent.emit("${provider.name} configuration saved")
        }
    }

    fun deleteProvider(providerId: String, name: String) {
        viewModelScope.launch {
            credentialStore.delete(providerId)
            repository.deleteProvider(providerId)
            repository.insertLog(
                SyncLogEntity(
                    fileName = "Cloud Disconnected",
                    providerName = name,
                    action = "PROVIDER_REMOVED",
                    status = "WARNING",
                    details = "Cloud service '$name' was disconnected and removed from vault."
                )
            )
            _toastEvent.emit("Removed $name")
        }
    }

    fun removeCustomCloudServer(providerId: String, name: String) {
        deleteProvider(providerId, name)
    }

    fun decryptFile(file: VaultFileEntity, customPassphrase: String? = null): Result<String> {
        if (!file.isEncrypted) {
            return Result.success(file.contentData)
        }
        val key = customPassphrase ?: _masterPassphrase.value
        return CryptoEngine.decrypt(
            cipherTextBase64 = file.contentData,
            saltBase64 = file.cipherSaltBase64,
            ivBase64 = file.cipherIvBase64,
            passphrase = key
        )
    }

    fun clearSyncLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _toastEvent.emit("Audit logs cleared")
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = OmniCloudDatabase.getDatabase(
                        application,
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    )
                    val repository = OmniCloudRepository(
                        fileDao = db.fileDao(),
                        projectDao = db.projectDao(),
                        cloudProviderDao = db.cloudProviderDao(),
                        syncDao = db.syncDao()
                    )
                    val credentialStore = CredentialStore(application)
                    val syncManager = CloudSyncManager(repository, credentialStore)
                    return OmniCloudViewModel(application, repository, syncManager) as T
                }
            }
    }
}
