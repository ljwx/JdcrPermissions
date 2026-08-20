package com.jdcr.jdcrpermission.handler

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.ExplainScope
import com.jdcr.jdcrpermission.result.JdcrPermissionDetail
import com.jdcr.jdcrpermission.result.JdcrPermissionResult
import com.jdcr.jdcrpermission.result.JdcrPermissionState
import com.jdcr.jdcrpermission.util.JdcrPermissionLog
import com.jdcr.jdcrpermission.util.JdcrPermissionUtils
import java.util.concurrent.atomic.AtomicLong

internal class JdcrPermissionHandler(
    private val activity: FragmentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val registry: ActivityResultRegistry,
    private val aliveCheck: () -> Boolean,
    private val requested: List<String>,
    private val before: (ExplainScope.(List<String>) -> Unit)?,
    private val after: (ExplainScope.(List<String>) -> Unit)?,
    private val callback: (JdcrPermissionResult) -> Unit
) : DefaultLifecycleObserver {

    private companion object {
        val SEQ = AtomicLong(0)
    }

    private val id = SEQ.incrementAndGet()
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var actionController = JdcrOpenActionHandler(
        lifecycleOwner,
        registry,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }) {
        JdcrPermissionLog.i("跳转App详情设置页后回来")
        deliver()
    }

    private var finished = false
    private var completed = false
    internal var completeListener: (() -> Unit)? = null

    private val statesBefore = LinkedHashMap<String, JdcrPermissionState>()
    private val launchedPermissions = LinkedHashSet<String>()
    private val systemResults = LinkedHashMap<String, Boolean>()

    fun start() {
        JdcrPermissionLog.i("触发权限请求逻辑")
        if (requested.isEmpty()) {
            deliver(); return
        }
        if (!aliveCheck()) {
            onComplete(); return
        }

        permissionLauncher = registry.register(
            "jdcr_permission_$id",
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            systemResults.putAll(results)
            onPermissionResult()
        }

        lifecycleOwner.lifecycle.addObserver(this)

        requested.distinct().forEach { permission ->
            val state = JdcrPermissionUtils.getState(activity, permission)
            statesBefore[permission] = state
            JdcrPermissionLog.i("请求前的权限状态,$permission:$${state.name}")
        }

        val denied = requested.filterNot { JdcrPermissionUtils.isGranted(activity, it) }
        if (denied.isEmpty()) {
            JdcrPermissionLog.i("不用处理,已全部授予:${requested.toTypedArray().contentToString()}")
            deliver(); return
        }
        val deniedContent = denied.toTypedArray().contentToString()
        val before = before
        if (before != null) {
            JdcrPermissionLog.w("触发前置解释:${deniedContent}")
            before.invoke(scope(denied) {
                JdcrPermissionLog.i("解释通过,发起权限请求:${deniedContent}")
                launch(denied)
            }, denied)
        } else {
            JdcrPermissionLog.i("不用解释,发起权限请求:${deniedContent}")
            launch(denied)
        }

    }

    private fun launch(permissions: List<String>) {
        if (!aliveCheck()) {
            onRelease(); onComplete(); return
        }
        launchedPermissions.addAll(permissions)
        JdcrPermissionUtils.markRequested(activity, permissions)
        permissionLauncher?.launch(permissions.toTypedArray())
    }

    private fun onPermissionResult() {
        if (finished) return
        val denied = requested.filterNot { JdcrPermissionUtils.isGranted(activity, it) }
        JdcrPermissionLog.i("请求后被拒绝的权限:${denied.toTypedArray().contentToString()}")
        if (denied.isEmpty()) {
            deliver(); return
        }
        val after = after
        val forever = denied.filter { JdcrPermissionUtils.isForeverDenied(activity, it) }
        if (after != null && forever.isNotEmpty()) {
            val foreverContent = forever.toTypedArray().contentToString()
            JdcrPermissionLog.i("被永久拒绝的权限:${foreverContent}")
            after.invoke(scope(forever) { openSettings() }, forever)
        } else deliver()
    }

    private fun deliver() {
        if (finished) return
        finished = true
        val details = requested.distinct().map { permission ->
            JdcrPermissionDetail(
                permission = permission,
                stateBefore = statesBefore.getValue(permission),
                requestLaunched = permission in launchedPermissions,
                systemGranted = systemResults[permission],
                stateAfter = JdcrPermissionUtils.getState(activity, permission)
            )
        }

        onRelease()
        val result = JdcrPermissionResult(details)
        JdcrPermissionLog.i("回调调用方最终结果:${result}")
        callback(result)
        onComplete()
    }

    private fun openSettings() {
        if (!aliveCheck()) {
            deliver(); return
        }
        actionController.start()
    }

    fun onComplete() {
        JdcrPermissionLog.i("触发权限请求完成")
        if (completed) return
        completed = true
        completeListener?.invoke()
    }

    private fun onRelease() {
        permissionLauncher?.unregister(); permissionLauncher = null
        actionController.onRelease()
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    private fun scope(permissions: List<String>, onProceed: () -> Unit) = object : ExplainScope {
        override val permissions = permissions
        override fun proceed() = onProceed()
        override fun cancel() = deliver()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onRelease()
        onComplete()
        super.onDestroy(owner)
    }

}