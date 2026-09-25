package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CloudProviderEntity
import com.example.model.VaultFileEntity
import com.example.ui.components.AddCloudProviderDialog
import com.example.ui.components.AddCustomServerDialog
import com.example.ui.components.AppLockGateScreen
import com.example.ui.components.BiometricVerificationDialog
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.FileDetailDialog
import com.example.ui.components.MasterKeyDialog
import com.example.ui.components.ProviderConfigDialog
import com.example.ui.components.UploadDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.ProvidersScreen
import com.example.ui.screens.SecurityVaultScreen
import com.example.ui.screens.SyncLogsScreen
import com.example.viewmodel.OmniCloudViewModel

enum class NavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    FILES("Files", Icons.Default.Folder),
    PROVIDERS("Clouds", Icons.Default.CloudSync),
    SECURITY("Security", Icons.Default.Security),
    LOGS("Logs", Icons.Default.ListAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniCloudApp(
    viewModel: OmniCloudViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    // Dialog state
    var showUploadDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showAddCloudProviderDialog by remember { mutableStateOf(false) }
    var showAddCustomServerDialog by remember { mutableStateOf(false) }
    var selectedFileForDetail by remember { mutableStateOf<VaultFileEntity?>(null) }
    var selectedProviderForConfig by remember { mutableStateOf<CloudProviderEntity?>(null) }
    var showMasterKeyDialog by remember { mutableStateOf(false) }
    var isMasterKeyUnlockMode by remember { mutableStateOf(false) }
    var showBioPromptForMasterKey by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Biometric Shield on App Open
    if (state.isAppLocked && state.isBioSecurityOnAppOpenEnabled) {
        AppLockGateScreen(
            masterPassphrase = state.masterPassphrase,
            onUnlock = { viewModel.unlockApp() }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("omnicloud_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OmniCloud",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Lock / Unlock toggle
                    IconButton(
                        onClick = {
                            if (state.isVaultLocked) {
                                isMasterKeyUnlockMode = true
                                showMasterKeyDialog = true
                            } else {
                                viewModel.lockVault()
                            }
                        },
                        modifier = Modifier.testTag("topbar_lock_button")
                    ) {
                        Icon(
                            imageVector = if (state.isVaultLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Status",
                            tint = if (state.isVaultLocked) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }

                    // Global sync icon
                    IconButton(
                        onClick = { viewModel.syncAllAcrossClouds() },
                        modifier = Modifier.testTag("topbar_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync All Clouds",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == NavigationTab.LOGS && state.activeTransfers.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${state.activeTransfers.size}") } }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == NavigationTab.DASHBOARD || selectedTab == NavigationTab.FILES) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_upload")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload Data")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    onUploadClick = { showUploadDialog = true },
                    onCreateProjectClick = { showCreateProjectDialog = true },
                    onSyncAllClick = { viewModel.syncAllAcrossClouds() },
                    onFileClick = { selectedFileForDetail = it },
                    onViewAllFilesClick = { selectedTab = NavigationTab.FILES },
                    onViewProvidersClick = { selectedTab = NavigationTab.PROVIDERS },
                    onLockToggleClick = {
                        if (state.isVaultLocked) {
                            isMasterKeyUnlockMode = true
                            showMasterKeyDialog = true
                        } else {
                            viewModel.lockVault()
                        }
                    },
                    onToggleConnection = { viewModel.toggleProviderConnection(it) }
                )

                NavigationTab.FILES -> FilesScreen(
                    state = state,
                    onFileClick = { selectedFileForDetail = it },
                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                    onProjectSelect = { viewModel.setSelectedProject(it) },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCreateProjectClick = { showCreateProjectDialog = true },
                    onPinToggle = { viewModel.togglePin(it) }
                )

                NavigationTab.PROVIDERS -> ProvidersScreen(
                    state = state,
                    onToggleConnection = { viewModel.toggleProviderConnection(it) },
                    onToggleAutoSync = { viewModel.toggleProviderAutoSync(it) },
                    onConfigureProvider = { selectedProviderForConfig = it },
                    onSyncAll = { viewModel.syncAllAcrossClouds() },
                    onAddProviderClick = { showAddCloudProviderDialog = true },
                    onDeleteProvider = { viewModel.deleteProvider(it.id, it.name) }
                )

                NavigationTab.SECURITY -> SecurityVaultScreen(
                    state = state,
                    onChangeMasterKeyClick = {
                        if (state.isBioForMasterKeyEnabled) {
                            showBioPromptForMasterKey = true
                        } else {
                            isMasterKeyUnlockMode = false
                            showMasterKeyDialog = true
                        }
                    },
                    onLockToggleClick = {
                        if (state.isVaultLocked) {
                            isMasterKeyUnlockMode = true
                            showMasterKeyDialog = true
                        } else {
                            viewModel.lockVault()
                        }
                    },
                    onLockAppNow = { viewModel.lockApp() },
                    onToggleBioAppOpen = { viewModel.setBioSecurityOnAppOpen(it) },
                    onToggleBioMasterKey = { viewModel.setBioForMasterKey(it) },
                    onToggleBioPersonalVault = { viewModel.setBioForPersonalVault(it) }
                )

                NavigationTab.LOGS -> SyncLogsScreen(
                    state = state,
                    onClearLogs = { viewModel.clearSyncLogs() }
                )
            }
        }
    }

    // Dialogs
    if (showUploadDialog) {
        UploadDialog(
            projects = state.projects,
            connectedProviders = state.providers.filter { it.isConnected },
            onDismiss = { showUploadDialog = false },
            onUpload = { name, original, mime, content, size, cat, projId, encrypt, targetIds ->
                viewModel.uploadFile(
                    name = name,
                    originalName = original,
                    mimeType = mime,
                    content = content,
                    sizeBytes = size,
                    category = cat,
                    projectId = projId,
                    enableEncryption = encrypt,
                    targetProviderIds = targetIds
                )
            }
        )
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { name, desc, color, icon ->
                viewModel.createProject(name, desc, color, icon)
            }
        )
    }

    if (showAddCloudProviderDialog) {
        AddCloudProviderDialog(
            existingProviderIds = state.providers.map { it.id }.toSet(),
            onDismiss = { showAddCloudProviderDialog = false },
            onConnectProvider = { providerEntity ->
                viewModel.connectProviderWithAuth(providerEntity)
            }
        )
    }

    if (showAddCustomServerDialog) {
        AddCustomServerDialog(
            onDismiss = { showAddCustomServerDialog = false },
            onAddServer = { name, type, endpoint, bucket, email, password, quotaGb, autoSync, encryptedOnly ->
                viewModel.addCustomCloudServer(
                    name = name,
                    type = type,
                    endpointUrl = endpoint,
                    bucketOrPath = bucket,
                    accountEmail = email,
                    password = password,
                    quotaGb = quotaGb,
                    autoSync = autoSync,
                    onlyEncrypted = encryptedOnly
                )
            }
        )
    }

    selectedFileForDetail?.let { file ->
        val syncs = state.syncsByFile[file.id] ?: emptyList()
        val project = state.projects.firstOrNull { it.id == file.projectId }
        FileDetailDialog(
            file = file,
            project = project,
            providers = state.providers,
            syncs = syncs,
            requireBioAuth = state.isBioForPersonalVaultEnabled,
            masterPassphrase = state.masterPassphrase,
            onDismiss = { selectedFileForDetail = null },
            onDecrypt = { pass -> viewModel.decryptFile(file, pass) },
            onSyncToProvider = { provId -> viewModel.syncFileToProviders(file, setOf(provId)) },
            onDesyncFromProvider = { provId, provName -> viewModel.desyncFileFromProvider(file, provId, provName) },
            onSyncToAll = {
                val allProvIds = state.providers.filter { it.isConnected }.map { it.id }.toSet()
                viewModel.syncFileToProviders(file, allProvIds)
            },
            onDelete = {
                viewModel.deleteFile(file.id)
                selectedFileForDetail = null
            }
        )
    }

    selectedProviderForConfig?.let { provider ->
        ProviderConfigDialog(
            provider = provider,
            onDismiss = { selectedProviderForConfig = null },
            onSave = { updated ->
                viewModel.updateProviderSettings(updated)
            }
        )
    }

    if (showBioPromptForMasterKey) {
        BiometricVerificationDialog(
            title = "Master Key Security Guard",
            subtitle = "Verify biometric identity to view or edit Master Passphrase",
            masterPassphrase = state.masterPassphrase,
            onDismiss = { showBioPromptForMasterKey = false },
            onAuthenticated = {
                showBioPromptForMasterKey = false
                isMasterKeyUnlockMode = false
                showMasterKeyDialog = true
            }
        )
    }

    if (showMasterKeyDialog) {
        MasterKeyDialog(
            currentKey = state.masterPassphrase,
            isUnlockMode = isMasterKeyUnlockMode,
            onDismiss = { showMasterKeyDialog = false },
            onConfirm = { pass ->
                if (isMasterKeyUnlockMode) {
                    viewModel.unlockVault(pass)
                } else {
                    viewModel.setMasterPassphrase(pass)
                }
            }
        )
    }
}
