package com.jdcr.jdcrpermission.handler

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.BeforePermissionRequestScope
import com.jdcr.jdcrpermission.DeniedNoRationaleScope
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
    private val before: (BeforePermissionRequestScope.() -> Unit)?,
    private val deniedNoRationale: (DeniedNoRationaleScope.() -> Unit)?,
    private val callback: (JdcrPermissionResult) -> Unit
) : DefaultLifecycleObserver {

    private companion object {
        val SEQ = AtomicLong(0)
    }

    private val id = SEQ.incrementAndGet()
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var finished = false
    private var completed = false
    internal var completeListener: (() -> Unit)? = null

    private val statesBefore = LinkedHashMap<String, JdcrPermissionState>()
    private val launchedPermissions = LinkedHashSet<String>()
    private val systemResults = LinkedHashMap<String, Boolean>()

    fun start() {
        JdcrPermissionLog.i("权限请求流程起点")
        if (requested.isEmpty()) {
            deliver(); return
        }
        if (!aliveCheck()) {
            complete(); return
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
            before.invoke(explainScope(denied) {
                JdcrPermissionLog.i("解释通过,发起权限请求:${deniedContent}")
                launch(denied)
            })
        } else {
            JdcrPermissionLog.i("不用解释,发起权限请求:${deniedContent}")
            launch(denied)
        }

    }

    private fun launch(permissions: List<String>) {
        if (!aliveCheck()) {
            release(); complete(); return
        }
        launchedPermissions.addAll(permissions)
        JdcrPermissionUtils.markRequested(activity, permissions)
        permissionLauncher?.launch(permissions.toTypedArray())
    }

    private fun onPermissionResult() {
        if (finished || completed) return
        val denied = requested.distinct().filter { permission ->
            systemResults[permission] == false &&
                    JdcrPermissionUtils.getState(activity, permission) ==
                    JdcrPermissionState.DENIED_NO_RATIONALE
        }
        val after = deniedNoRationale
        if (after == null || denied.isEmpty()) {
            deliver()
            return
        }

        JdcrPermissionLog.i("请求后无说明提示的权限:${denied.toTypedArray().contentToString()}")
        try {
            after.invoke(object : DeniedNoRationaleScope {
                override val permissions = denied
                override fun finish() = deliver()
            })
        } catch (failure: Throwable) {
            try {
                deliver()
            } catch (deliveryFailure: Throwable) {
                failure.addSuppressed(deliveryFailure)
            }
            throw failure
        }
    }

    private fun deliver() {
        JdcrPermissionLog.i("触发权限结果交付")
        if (finished || completed) return
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

        release()
        val result = JdcrPermissionResult(details)
        JdcrPermissionLog.i("回调最终交付结果:${result}")
        try {
            callback(result)
        } finally {
            complete()
        }
    }

    private fun complete() {
        if (completed) return
        completed = true
        JdcrPermissionLog.i("结束一次权限请求任务")
        completeListener?.invoke()
    }

    private fun release() {
        JdcrPermissionLog.i("释放权限请求资源")
        permissionLauncher?.unregister(); permissionLauncher = null
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    private fun explainScope(permissions: List<String>, onProceed: () -> Unit) =
        object : BeforePermissionRequestScope {
            override val permissions = permissions
            override fun proceed() = onProceed()
            override fun cancel() = deliver()
        }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
        complete()
        super.onDestroy(owner)
    }

}
