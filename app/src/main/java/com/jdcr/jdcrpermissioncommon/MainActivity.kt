package com.jdcr.jdcrpermissioncommon

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.jdcr.jdcrpermission.BeforePermissionRequestScope
import com.jdcr.jdcrpermission.DeniedNoRationaleScope
import com.jdcr.jdcrpermission.JdcrPermission
import com.jdcr.jdcrpermission.result.JdcrPermissionDetail
import com.jdcr.jdcrpermission.result.JdcrPermissionResult
import com.jdcr.jdcrpermission.result.JdcrPermissionState
import com.jdcr.jdcrpermission.util.JdcrPermissionLog
import com.jdcr.jdcrpermission.util.JdcrPermissionUtils
import com.jdcr.jdcrpermissioncommon.ui.theme.JdcrPermissionCommonTheme

class MainActivity : FragmentActivity() {
    private var activeScenario by mutableStateOf<String?>(null)
    private var lastScenario by mutableStateOf<String?>(null)
    private var lastResult by mutableStateOf<JdcrPermissionResult?>(null)
    private var errorMessage by mutableStateOf<String?>(null)
    private var beforeScope by mutableStateOf<BeforePermissionRequestScope?>(null)
    private var deniedScope by mutableStateOf<DeniedNoRationaleScope?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        JdcrPermissionLog.enable(true)
        setContent {
            JdcrPermissionCommonTheme {
                PermissionExamples()
            }
        }
    }

    private fun requestExample(
        title: String,
        configure: JdcrPermission.() -> Unit
    ) {
        if (activeScenario != null) return
        activeScenario = title
        lastScenario = null
        lastResult = null
        errorMessage = null
        deniedScope = null
        JdcrPermission.with(this).apply(configure).request { result ->
            lastScenario = title
            lastResult = result
            activeScenario = null
        }
    }

    private fun finishExplanation(continueRequest: Boolean) {
        val scope = beforeScope ?: return
        beforeScope = null
        if (continueRequest) scope.proceed() else scope.cancel()
    }

    private fun finishDeniedHandling() {
        val scope = deniedScope ?: return
        deniedScope = null
        scope.finish()
    }

    private fun openSettingsForNoRationale() {
        val scope = deniedScope ?: return
        deniedScope = null
        runCatching {
            JdcrPermissionUtils.openAppSettings(this) {
                scope.finish()
            }
        }.onFailure {
            errorMessage = "无法打开应用设置"
            scope.finish()
        }
    }

    @Composable
    private fun PermissionExamples() {
        val enabled = activeScenario == null
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("权限请求", style = MaterialTheme.typography.headlineSmall)
                Text("请求场景", style = MaterialTheme.typography.titleMedium)
                ExampleButton("请求相机权限", enabled) {
                    requestExample("相机") {
                        permissions(Manifest.permission.CAMERA)
                    }
                }
                ExampleButton("请求相机权限 · 请求前说明", enabled) {
                    requestExample("相机 · 请求前说明") {
                        permissions(Manifest.permission.CAMERA)
                        onExplainBeforeRequest { beforeScope = this }
                    }
                }
                ExampleButton("请求相机权限 · 无说明提示处理", enabled) {
                    requestExample("相机 · 无说明提示处理") {
                        permissions(Manifest.permission.CAMERA)
                        onDeniedNoRationale { deniedScope = this }
                    }
                }
                ExampleButton("请求前台定位", enabled) {
                    requestExample("前台定位") {
                        permissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    }
                }
                ExampleButton(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "请求附近设备权限"
                    else "请求蓝牙扫描所需定位权限",
                    enabled
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestExample("附近设备") {
                            permissions(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        }
                    } else {
                        requestExample("蓝牙扫描所需定位") {
                            permissions(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }

                HorizontalDivider()
                Text("最近一次结果", style = MaterialTheme.typography.titleMedium)
                activeScenario?.let { Text("正在请求：$it") }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                lastResult?.let { result ->
                    Text("$lastScenario：${if (result.allGranted) "全部已授权" else "仍有未授权权限"}")
                    result.details.forEach { detail -> PermissionDetailRow(detail) }
                }
            }
        }

        beforeScope?.let { scope ->
            AlertDialog(
                onDismissRequest = { finishExplanation(false) },
                title = { Text("相机权限") },
                text = { Text("是否继续请求${scope.permissions.joinToString { permissionLabel(it) }}？") },
                confirmButton = {
                    TextButton(onClick = { finishExplanation(true) }) { Text("继续") }
                },
                dismissButton = {
                    TextButton(onClick = { finishExplanation(false) }) { Text("取消") }
                }
            )
        }

        deniedScope?.let { scope ->
            AlertDialog(
                onDismissRequest = { finishDeniedHandling() },
                title = { Text("权限未授予") },
                text = {
                    Text("${scope.permissions.joinToString { permissionLabel(it) }}未授权。可稍后处理，或前往设置查看。")
                },
                confirmButton = {
                    TextButton(onClick = { openSettingsForNoRationale() }) { Text("去设置") }
                },
                dismissButton = {
                    TextButton(onClick = { finishDeniedHandling() }) { Text("稍后") }
                }
            )
        }
    }
}

@Composable
private fun ExampleButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

@Composable
private fun PermissionDetailRow(detail: JdcrPermissionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(permissionLabel(detail.permission), style = MaterialTheme.typography.titleSmall)
        Text(
            "请求前：${stateLabel(detail.stateBefore)} · 当前：${stateLabel(detail.stateAfter)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "系统返回：${when (detail.systemGranted) {
                true -> "同意"
                false -> "拒绝"
                null -> "未请求"
            }} · 发起请求：${if (detail.requestLaunched) "是" else "否"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun permissionLabel(permission: String): String = when (permission) {
    Manifest.permission.CAMERA -> "相机"
    Manifest.permission.ACCESS_COARSE_LOCATION -> "大致位置"
    Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置"
    Manifest.permission.BLUETOOTH_SCAN -> "扫描附近设备"
    Manifest.permission.BLUETOOTH_CONNECT -> "连接附近设备"
    else -> permission
}

private fun stateLabel(state: JdcrPermissionState): String = when (state) {
    JdcrPermissionState.GRANTED -> "已授权"
    JdcrPermissionState.DENIED_NOT_REQUESTED -> "未请求"
    JdcrPermissionState.DENIED_SHOW_RATIONALE -> "已拒绝，可展示说明"
    JdcrPermissionState.DENIED_NO_RATIONALE -> "已拒绝，无说明提示"
}
