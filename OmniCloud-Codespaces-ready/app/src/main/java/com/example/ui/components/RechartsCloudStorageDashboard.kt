package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CloudProviderEntity
import com.example.model.CloudProviderType
import java.util.Locale
import kotlin.math.max

data class ProviderGbMetric(
    val id: String,
    val shortName: String,
    val fullName: String,
    val type: CloudProviderType,
    val isOnline: Boolean,
    val usedGb: Float,
    val quotaGb: Float,
    val brandColorHex: Long,
    val entity: CloudProviderEntity
) {
    val usagePercentage: Float
        get() = if (quotaGb > 0f) (usedGb / quotaGb).coerceIn(0f, 1f) else 0f

    val freeGb: Float
        get() = max(0f, quotaGb - usedGb)
}

fun bytesToGb(bytes: Long): Float {
    return (bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat()
}

fun formatGbNumber(gb: Float): String {
    return if (gb < 0.1f && gb > 0f) {
        String.format(Locale.US, "%.2f GB", gb)
    } else if (gb >= 10f) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        String.format(Locale.US, "%.2f GB", gb)
    }
}

enum class DashboardViewMode {
    RECHARTS_CHART,
    RECHARTS_WEB,
    GRID_METRICS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsCloudStorageDashboard(
    providers: List<CloudProviderEntity>,
    onToggleConnection: (CloudProviderEntity) -> Unit,
    onSyncAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(DashboardViewMode.RECHARTS_CHART) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var filterOnlineOnly by remember { mutableStateOf(false) }

    // Map providers to standard GB metrics, ensuring Drive, Dropbox, OneDrive, S3, Box, Nextcloud are represented
    val metrics = remember(providers, filterOnlineOnly) {
        val targetOrder = listOf(
            CloudProviderType.GOOGLE_DRIVE,
            CloudProviderType.DROPBOX,
            CloudProviderType.MICROSOFT_ONEDRIVE,
            CloudProviderType.AWS_S3,
            CloudProviderType.BOX,
            CloudProviderType.NEXTCLOUD
        )

        val mapped = providers.map { p ->
            val short = when (p.type) {
                CloudProviderType.GOOGLE_DRIVE -> "Drive"
                CloudProviderType.DROPBOX -> "Dropbox"
                CloudProviderType.MICROSOFT_ONEDRIVE -> "OneDrive"
                CloudProviderType.AWS_S3 -> "S3"
                CloudProviderType.BOX -> "Box"
                CloudProviderType.NEXTCLOUD -> "Nextcloud"
                CloudProviderType.PCLOUD -> "pCloud"
                CloudProviderType.CUSTOM_SERVER -> "Custom"
                CloudProviderType.WEBDAV -> "WebDAV"
            }
            ProviderGbMetric(
                id = p.id,
                shortName = short,
                fullName = p.name,
                type = p.type,
                isOnline = p.isConnected,
                usedGb = bytesToGb(p.storageUsedBytes),
                quotaGb = bytesToGb(p.storageTotalBytes),
                brandColorHex = p.type.brandColorHex,
                entity = p
            )
        }.sortedBy { metric ->
            val idx = targetOrder.indexOf(metric.type)
            if (idx >= 0) idx else 99
        }

        if (filterOnlineOnly) {
            mapped.filter { it.isOnline }
        } else {
            mapped
        }
    }

    val onlineCloudsCount = providers.count { it.isConnected }
    val totalCloudsCount = providers.size
    val totalUsedGb = metrics.map { it.usedGb }.sum()
    val totalQuotaGb = metrics.map { it.quotaGb }.sum()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_storage_dashboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .animateContentSize()
        ) {
            // Header Row: Title & Online Cloud No.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cloud Storage Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Recharts Usage Telemetry (in GB)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Online Cloud Count Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (onlineCloudsCount > 0) Color(0xFF10B981).copy(alpha = 0.16f) else MaterialTheme.colorScheme.errorContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (onlineCloudsCount > 0) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("online_clouds_counter_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (onlineCloudsCount > 0) Color(0xFF10B981).copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.error
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$onlineCloudsCount / $totalCloudsCount Online",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (onlineCloudsCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick KPI Cards: Total Used GB, Quota GB, Online Cloud No.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online Clouds metric card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Online Clouds",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$onlineCloudsCount Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Total Used GB
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total Used",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f GB", totalUsedGb),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Total Quota GB
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total Quota",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.0f GB", totalQuotaGb),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // View Mode Selector Chips & Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = viewMode == DashboardViewMode.RECHARTS_CHART,
                        onClick = { viewMode = DashboardViewMode.RECHARTS_CHART },
                        label = { Text("Recharts Bar", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                    FilterChip(
                        selected = viewMode == DashboardViewMode.GRID_METRICS,
                        onClick = { viewMode = DashboardViewMode.GRID_METRICS },
                        label = { Text("GB Breakdown", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                    FilterChip(
                        selected = viewMode == DashboardViewMode.RECHARTS_WEB,
                        onClick = { viewMode = DashboardViewMode.RECHARTS_WEB },
                        label = { Text("Web Engine", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (viewMode) {
                DashboardViewMode.RECHARTS_CHART -> {
                    // Native Recharts-styled Cartesian Canvas Bar Chart
                    RechartsCartesianBarChart(
                        metrics = metrics,
                        selectedIndex = selectedIndex,
                        onSelectIndex = { selectedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recharts Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Used Storage (GB)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Total Quota (GB)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Interactive Tooltip Box
                    if (metrics.isNotEmpty() && selectedIndex in metrics.indices) {
                        val sel = metrics[selectedIndex]
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(sel.brandColorHex).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recharts_tooltip_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ProviderAvatar(type = sel.type, size = 26)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sel.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sel.isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (sel.isOnline) "● Online" else "○ Offline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sel.isOnline) Color(0xFF10B981) else Color.Gray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Used Storage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            formatGbNumber(sel.usedGb),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(sel.brandColorHex)
                                        )
                                    }
                                    Column {
                                        Text("Quota Limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            formatGbNumber(sel.quotaGb),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column {
                                        Text("Free Space", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            formatGbNumber(sel.freeGb),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column {
                                        Text("Usage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            String.format(Locale.US, "%.1f%%", sel.usagePercentage * 100f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardViewMode.GRID_METRICS -> {
                    // GB metrics breakdown list for Drive, Dropbox, OneDrive, S3, Box, Nextcloud
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        metrics.forEach { metric ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("provider_gb_row_${metric.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ProviderAvatar(type = metric.type, size = 30)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = metric.fullName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${formatGbNumber(metric.usedGb)} of ${formatGbNumber(metric.quotaGb)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Online toggle button
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (metric.isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                                modifier = Modifier.clickable { onToggleConnection(metric.entity) }
                                            ) {
                                                Text(
                                                    text = if (metric.isOnline) "● Online" else "○ Offline",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (metric.isOnline) Color(0xFF10B981) else Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { metric.usagePercentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(metric.brandColorHex),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${formatGbNumber(metric.freeGb)} free",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f%% used", metric.usagePercentage * 100f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(metric.brandColorHex)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardViewMode.RECHARTS_WEB -> {
                    // HTML5 / SVG Recharts rendered directly in an offline WebView
                    RechartsWebView(
                        metrics = metrics,
                        onlineCount = onlineCloudsCount,
                        totalCount = totalCloudsCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom action: Quick Sync All & Online status summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Syncs across Drive, Dropbox, OneDrive, S3, Box, Nextcloud",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSyncAll,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sync_all_clouds_button")
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Online", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Native Canvas Recharts-style Bar Chart
 * Features:
 * - Cartesian grid with dashed horizontal gridlines (Recharts strokeDasharray="3 3")
 * - Y-Axis labeled in GB (e.g. 0 GB, 25 GB, 50 GB, 75 GB, 100 GB)
 * - X-Axis labeled with provider short names (Drive, Dropbox, OneDrive, S3, Box, Nextcloud)
 * - Dual bars: Quota Limit (subtle backdrop) & Used Storage in GB (brand colors)
 * - Online / Offline indicator dot above each bar
 * - Touch detection to select bar
 */
@Composable
fun RechartsCartesianBarChart(
    metrics: List<ProviderGbMetric>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val quotaBarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    // Determine max GB scale for Y-axis (round up nicely to 10, 20, 50, 100, etc.)
    val maxQuotaGb = metrics.map { it.quotaGb }.maxOrNull() ?: 100f
    val yAxisMaxGb = when {
        maxQuotaGb <= 10f -> 10f
        maxQuotaGb <= 20f -> 20f
        maxQuotaGb <= 50f -> 50f
        maxQuotaGb <= 100f -> 100f
        else -> ((maxQuotaGb / 50).toInt() + 1) * 50f
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("recharts_cartesian_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(metrics) {
                    detectTapGestures { offset ->
                        val leftPadding = 48.dp.toPx()
                        val rightPadding = 12.dp.toPx()
                        val chartWidth = size.width - leftPadding - rightPadding
                        val barCount = metrics.size
                        if (barCount > 0 && offset.x >= leftPadding && offset.x <= size.width - rightPadding) {
                            val slotWidth = chartWidth / barCount
                            val tappedIdx = ((offset.x - leftPadding) / slotWidth).toInt().coerceIn(0, barCount - 1)
                            onSelectIndex(tappedIdx)
                        }
                    }
                }
        ) {
            val leftPadding = 48.dp.toPx()
            val bottomPadding = 32.dp.toPx()
            val topPadding = 20.dp.toPx()
            val rightPadding = 12.dp.toPx()

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            // 1. Draw Cartesian Grid Lines (Recharts signature horizontal dashed lines)
            val gridSteps = 4
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            for (i in 0..gridSteps) {
                val ratio = i.toFloat() / gridSteps.toFloat()
                val y = topPadding + chartHeight * (1f - ratio)
                val gbValue = yAxisMaxGb * ratio

                // Grid line
                drawLine(
                    color = gridLineColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashedEffect
                )

                // Y-Axis GB Label
                val labelText = if (gbValue >= 10f) {
                    "${gbValue.toInt()} GB"
                } else if (gbValue == 0f) {
                    "0 GB"
                } else {
                    String.format(Locale.US, "%.1f GB", gbValue)
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    drawText(labelText, leftPadding - 6.dp.toPx(), y + 4.dp.toPx(), paint)
                }
            }

            // 2. Draw Bars for Each Provider
            val barCount = metrics.size
            if (barCount > 0) {
                val slotWidth = chartWidth / barCount
                val barWidth = (slotWidth * 0.55f).coerceAtMost(32.dp.toPx())

                metrics.forEachIndexed { index, metric ->
                    val centerX = leftPadding + (index + 0.5f) * slotWidth
                    val barLeft = centerX - barWidth / 2f

                    // Quota Height
                    val quotaRatio = (metric.quotaGb / yAxisMaxGb).coerceIn(0f, 1f)
                    val quotaHeight = chartHeight * quotaRatio
                    val quotaTop = topPadding + chartHeight - quotaHeight

                    // Used Height
                    val usedRatio = (metric.usedGb / yAxisMaxGb).coerceIn(0f, 1f)
                    val usedHeight = chartHeight * usedRatio
                    val usedTop = topPadding + chartHeight - usedHeight

                    // Background Quota Bar (Light translucent)
                    drawRoundRect(
                        color = quotaBarColor,
                        topLeft = Offset(barLeft, quotaTop),
                        size = Size(barWidth, quotaHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Foreground Used Bar (Vibrant brand gradient)
                    val brandColor = Color(metric.brandColorHex)
                    val isSelected = index == selectedIndex
                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            brandColor,
                            brandColor.copy(alpha = if (metric.isOnline) 0.85f else 0.4f)
                        ),
                        startY = usedTop,
                        endY = topPadding + chartHeight
                    )

                    if (usedHeight > 0) {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barLeft, usedTop),
                            size = Size(barWidth, usedHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Selection highlight outline
                    if (isSelected) {
                        drawRoundRect(
                            color = brandColor,
                            topLeft = Offset(barLeft - 2.dp.toPx(), minOf(quotaTop, usedTop) - 2.dp.toPx()),
                            size = Size(barWidth + 4.dp.toPx(), (topPadding + chartHeight - minOf(quotaTop, usedTop)) + 2.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Online indicator dot above bar
                    val dotColor = if (metric.isOnline) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.5f)
                    drawCircle(
                        color = dotColor,
                        radius = 3.5.dp.toPx(),
                        center = Offset(centerX, minOf(quotaTop, usedTop) - 8.dp.toPx())
                    )

                    // X-Axis Provider Name
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            isFakeBoldText = isSelected
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            metric.shortName,
                            centerX,
                            size.height - bottomPadding + 16.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure SVG + HTML5 Recharts Web Engine rendered in an offline Android WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebView(
    metrics: List<ProviderGbMetric>,
    onlineCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    // Generate inline HTML page with CSS and SVG chart that renders like Recharts
    val htmlContent = remember(metrics, onlineCount, totalCount) {
        val barsHtml = StringBuilder()
        val labelsHtml = StringBuilder()
        val maxQuota = metrics.map { it.quotaGb }.maxOrNull() ?: 100f
        val chartHeight = 180

        metrics.forEachIndexed { i, m ->
            val usedHeight = if (maxQuota > 0) ((m.usedGb / maxQuota) * chartHeight).toInt() else 0
            val quotaHeight = if (maxQuota > 0) ((m.quotaGb / maxQuota) * chartHeight).toInt() else 0
            val colorHex = String.format("#%06X", 0xFFFFFF and m.brandColorHex.toInt())
            val statusColor = if (m.isOnline) "#10B981" else "#9CA3AF"

            barsHtml.append(
                """
                <div class="bar-col" onclick="showTip('${m.fullName}', '${m.isOnline}', '${String.format(Locale.US, "%.2f", m.usedGb)}', '${String.format(Locale.US, "%.2f", m.quotaGb)}')">
                    <div class="dot" style="background:${statusColor}"></div>
                    <div class="bar-stack">
                        <div class="bar-quota" style="height:${quotaHeight}px;"></div>
                        <div class="bar-used" style="height:${usedHeight}px; background:${colorHex};"></div>
                    </div>
                    <div class="bar-label">${m.shortName}</div>
                </div>
                """.trimIndent()
            )
        }

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; margin:0; padding:0; font-family: -apple-system, Roboto, sans-serif; }
                body { background: #121826; color: #F3F4F6; padding: 12px; }
                .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
                .title { font-size: 13px; font-weight: bold; color: #38BDF8; }
                .badge { background: rgba(16, 185, 129, 0.15); color: #10B981; border: 1px solid rgba(16, 185, 129, 0.4); padding: 3px 8px; border-radius: 10px; font-size: 11px; font-weight: bold; }
                .chart-container { display: flex; height: 180px; align-items: flex-end; justify-content: space-around; border-bottom: 1px dashed rgba(255,255,255,0.15); padding-bottom: 4px; }
                .bar-col { display: flex; flex-direction: column; align-items: center; cursor: pointer; flex: 1; position: relative; }
                .dot { width: 7px; height: 7px; border-radius: 50%; margin-bottom: 4px; }
                .bar-stack { width: 26px; height: 140px; display: flex; align-items: flex-end; position: relative; background: rgba(255,255,255,0.03); border-radius: 6px 6px 0 0; }
                .bar-quota { width: 100%; position: absolute; bottom: 0; background: rgba(255,255,255,0.08); border-radius: 6px 6px 0 0; }
                .bar-used { width: 100%; position: absolute; bottom: 0; border-radius: 6px 6px 0 0; z-index: 2; transition: all 0.3s; }
                .bar-label { font-size: 10px; color: #9CA3AF; margin-top: 6px; text-align: center; }
                .tooltip { background: #1E293B; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; padding: 8px 12px; margin-top: 10px; font-size: 11px; display: flex; justify-content: space-between; }
                .legend { display: flex; justify-content: center; gap: 14px; margin-top: 8px; font-size: 10px; color: #9CA3AF; }
                .leg-item { display: flex; align-items: center; gap: 4px; }
                .leg-box { width: 8px; height: 8px; border-radius: 2px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="title">Recharts Multi-Cloud Engine</div>
                <div class="badge">● ${onlineCount} / ${totalCount} Online</div>
            </div>
            <div class="chart-container">
                $barsHtml
            </div>
            <div id="tipBox" class="tooltip">
                <span id="tipName" style="font-weight:bold; color:#38BDF8;">Tap any bar for live metrics</span>
                <span id="tipVal" style="color:#9CA3AF;">Storage in GB</span>
            </div>
            <div class="legend">
                <div class="leg-item"><div class="leg-box" style="background:#38BDF8;"></div> Used (GB)</div>
                <div class="leg-item"><div class="leg-box" style="background:rgba(255,255,255,0.15);"></div> Quota (GB)</div>
                <div class="leg-item"><div class="leg-box" style="background:#10B981; border-radius:50%;"></div> Online</div>
            </div>

            <script>
                function showTip(name, online, used, quota) {
                    const status = online === 'true' ? '● Online' : '○ Offline';
                    document.getElementById('tipName').innerHTML = name + ' (' + status + ')';
                    document.getElementById('tipVal').innerHTML = used + ' GB / ' + quota + ' GB';
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.testTag("recharts_webview"),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(0xFF121826.toInt())
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}
