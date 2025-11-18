package moe.fuqiuluo.mamu.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.fuqiuluo.mamu.data.DriverStatus
import moe.fuqiuluo.mamu.data.SeLinuxMode
import moe.fuqiuluo.mamu.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mamu") },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 驱动状态卡片
                DriverStatusCard(
                    status = uiState.driverInfo?.status,
                    isProcessBound = uiState.driverInfo?.isProcessBound ?: false,
                    boundPid = uiState.driverInfo?.boundPid ?: -1,
                    errorMessage = uiState.driverInfo?.errorMessage
                )

                // Root权限状态卡片
                RootStatusCard(hasRoot = uiState.hasRootAccess)

                // SELinux状态卡片
                SeLinuxStatusCard(
                    mode = uiState.seLinuxStatus?.mode,
                    modeString = uiState.seLinuxStatus?.modeString
                )

                // 系统信息卡片
                SystemInfoCard(
                    systemInfo = uiState.systemInfo
                )

                // 错误信息
                uiState.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "错误",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverStatusCard(
    status: DriverStatus?,
    isProcessBound: Boolean,
    boundPid: Int,
    errorMessage: String?
) {
    StatusCard(
        title = "驱动状态",
        icon = Icons.Default.Settings
    ) {
        when (status) {
            DriverStatus.LOADED -> {
                StatusItem(
                    label = "状态",
                    value = "已加载",
                    color = MaterialTheme.colorScheme.primary
                )
                if (isProcessBound && boundPid > 0) {
                    StatusItem(
                        label = "绑定进程",
                        value = "PID: $boundPid",
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    StatusItem(
                        label = "绑定进程",
                        value = "未绑定",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DriverStatus.NOT_LOADED -> {
                StatusItem(
                    label = "状态",
                    value = "未加载",
                    color = MaterialTheme.colorScheme.error
                )
            }
            DriverStatus.ERROR -> {
                StatusItem(
                    label = "状态",
                    value = "错误",
                    color = MaterialTheme.colorScheme.error
                )
                errorMessage?.let {
                    StatusItem(
                        label = "错误信息",
                        value = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            null -> {
                StatusItem(
                    label = "状态",
                    value = "未知",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RootStatusCard(hasRoot: Boolean) {
    StatusCard(
        title = "Root权限",
        icon = Icons.Default.AdminPanelSettings
    ) {
        StatusItem(
            label = "状态",
            value = if (hasRoot) "已获取" else "未获取",
            color = if (hasRoot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun SeLinuxStatusCard(mode: SeLinuxMode?, modeString: String?) {
    StatusCard(
        title = "SELinux状态",
        icon = Icons.Default.Security
    ) {
        val (statusText, statusColor) = when (mode) {
            SeLinuxMode.ENFORCING -> "强制模式" to MaterialTheme.colorScheme.error
            SeLinuxMode.PERMISSIVE -> "宽容模式" to MaterialTheme.colorScheme.tertiary
            SeLinuxMode.DISABLED -> "已禁用" to MaterialTheme.colorScheme.primary
            SeLinuxMode.UNKNOWN, null -> "未知" to MaterialTheme.colorScheme.onSurfaceVariant
        }

        StatusItem(
            label = "模式",
            value = modeString ?: statusText,
            color = statusColor
        )
    }
}

@Composable
fun SystemInfoCard(
    systemInfo: moe.fuqiuluo.mamu.data.SystemInfo
) {
    StatusCard(
        title = "系统信息",
        icon = Icons.Default.Info
    ) {
        StatusItem(label = "设备型号", value = systemInfo.deviceModel)
        StatusItem(label = "设备制造商", value = systemInfo.deviceManufacturer)
        StatusItem(label = "设备品牌", value = systemInfo.deviceBrand)
        StatusItem(label = "Android版本", value = systemInfo.androidVersion)
        StatusItem(label = "SDK版本", value = systemInfo.sdkVersion.toString())
        StatusItem(label = "内核版本", value = systemInfo.kernelVersion)
        StatusItem(label = "CPU架构", value = systemInfo.cpuAbi)
        StatusItem(
            label = "支持的ABI",
            value = systemInfo.allCpuAbis.joinToString(", ")
        )
    }
}

@Composable
fun StatusCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}