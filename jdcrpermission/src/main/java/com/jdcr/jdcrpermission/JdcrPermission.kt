package com.jdcr.jdcrpermission

import android.content.Context
import android.view.View
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrbase.page.toActivity
import com.jdcr.jdcrpermission.handler.JdcrPermissionDispatcher
import com.jdcr.jdcrpermission.handler.JdcrPermissionHandler
import com.jdcr.jdcrpermission.result.JdcrPermissionResult

interface BeforePermissionRequestScope {
    val permissions: List<String>
    fun proceed()   // before: 继续发起系统请求
    fun cancel()    // 放弃, 直接回调当前结果
}

interface DeniedNoRationaleScope {
    val permissions: List<String>
    fun finish()
}

class JdcrPermission private constructor(
    internal val activity: FragmentActivity,
    internal val lifecycleOwner: LifecycleOwner,
    internal val registry: ActivityResultRegistry,
    internal val aliveCheck: () -> Boolean
) {
    companion object {
        fun with(activity: FragmentActivity): JdcrPermission =
            JdcrPermission(
                activity, activity, activity.activityResultRegistry
            ) { !activity.isFinishing && !activity.isDestroyed }

        fun with(fragment: Fragment): JdcrPermission =
            JdcrPermission(
                fragment.requireActivity(),
                fragment,
                fragment.requireActivity().activityResultRegistry
            ) {
                fragment.isAdded &&
                        fragment.activity?.let { !it.isFinishing && !it.isDestroyed } == true
            }

        fun with(view: View): Result<JdcrPermission> {
            return runCatching {
                val activity = view.context.toActivity() as FragmentActivity
                JdcrPermission(activity, activity, activity.activityResultRegistry) {
                    !activity.isFinishing && !activity.isDestroyed
                }
            }
        }

        fun with(context: Context): Result<JdcrPermission> {
            return runCatching {
                val activity = context.toActivity() as FragmentActivity
                JdcrPermission(activity, activity, activity.activityResultRegistry) {
                    !activity.isFinishing && !activity.isDestroyed
                }
            }
        }

    }

    private val permissions = LinkedHashSet<String>()
    private var beforeRequest: (BeforePermissionRequestScope.() -> Unit)? = null
    private var deniedNoRationale: (DeniedNoRationaleScope.() -> Unit)? = null
    fun permissions(vararg permission: String) = apply { permissions += permission }
    fun permissions(p: Collection<String>) = apply { permissions += p }
    fun onExplainBeforeRequest(block: BeforePermissionRequestScope.() -> Unit) = apply { beforeRequest = block }
    fun onDeniedNoRationale(block: DeniedNoRationaleScope.() -> Unit) = apply { deniedNoRationale = block }

    fun request(callback: (JdcrPermissionResult) -> Unit) {
        val currentPermissions = permissions.toList()
        val currentBefore = beforeRequest
        val currentDeniedNoRationale = deniedNoRationale

        permissions.clear()
        beforeRequest = null
        deniedNoRationale = null

        val handler = JdcrPermissionHandler(
            activity, lifecycleOwner, registry, aliveCheck,
            currentPermissions, currentBefore, currentDeniedNoRationale, callback
        )
        JdcrPermissionDispatcher.enqueue(lifecycleOwner, handler)
    }
}
