package moe.fuqiuluo.mamu.ui.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.fuqiuluo.mamu.data.model.DriverInfo
import moe.fuqiuluo.mamu.ui.viewmodel.DriverInstallViewModel
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverInstallScreen(
    onNavigateBack: () -> Unit,
    viewModel: DriverInstallViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 监听重启标志
    LaunchedEffect(uiState.shouldRestartApp) {
        if (uiState.shouldRestartApp) {
            restartApp(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("驱动安装") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDrivers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.drivers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "无可用驱动",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 驱动列表
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.drivers) { driver ->
                                DriverCard(
                                    driver = driver,
                                    isSelected = uiState.selectedDriver == driver,
                                    onSelect = { viewModel.selectDriver(driver) }
                                )
                            }
                        }

                        // 底部安装按钮
                        if (uiState.selectedDriver != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                tonalElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.isInstalling) {
                                        Text(
                                            text = "正在下载并安装...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Button(
                                            onClick = { showConfirmDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "下载并安装"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 成功提示 Snackbar
            uiState.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }

    // 确认对话框
    if (showConfirmDialog && uiState.selectedDriver != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认安装") },
            text = {
                Text("确定要安装驱动 ${uiState.selectedDriver!!.displayName} 吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.downloadAndInstallDriver()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误日志对话框
    uiState.error?.let { errorLog ->
        ErrorLogDialog(
            errorLog = errorLog,
            onDismiss = { viewModel.clearMessages() }
        )
    }
}

@Composable
fun DriverCard(
    driver: DriverInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder()
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = driver.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = driver.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态标签
            if (driver.installed) {
                AssistChip(
                    onClick = { },
                    label = { Text("已安装") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // 选中指示器
            if (isSelected && !driver.installed) {
                Icon(
                    Icons.Default.RadioButtonChecked,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (!driver.installed) {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = "未选中",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorLogDialog(
    errorLog: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "驱动安装失败",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "错误详情：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = errorLog,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "您可以复制日志内容并反馈给开发者",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    // 复制到剪贴板
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("驱动错误日志", errorLog)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("复制日志")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 重启应用
 */
private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(it)
    }

    // 结束当前Activity
    if (context is Activity) {
        context.finish()
    }

    // 延迟后杀掉进程，确保重启
    Handler(Looper.getMainLooper()).postDelayed({
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }, 300)
}
