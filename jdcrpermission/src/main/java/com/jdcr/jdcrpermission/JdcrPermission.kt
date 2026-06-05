package com.jdcr.jdcrpermission

import android.view.View
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.handler.JdcrPermissionDispatcher
import com.jdcr.jdcrpermission.handler.JdcrPermissionHandler
import com.jdcr.jdcrpermission.result.JdcrPermissionResult
import com.jdcr.jdcrpermission.util.JdcrPermissionUtils

interface ExplainScope {
    val permissions: List<String>
    fun proceed()   // before: 继续发起系统请求; after: 跳转应用设置页
    fun cancel()    // 放弃, 直接回调当前结果
}

class JdcrPermission private constructor(
    internal val activity: FragmentActivity,
    internal val lifecycleOwner: LifecycleOwner,
    internal val registry: ActivityResultRegistry,
    internal val aliveCheck: () -> Boolean
) {
    companion object {
        fun with(activity: FragmentActivity) = JdcrPermission(
            activity, activity, activity.activityResultRegistry
        ) { !activity.isFinishing && !activity.isDestroyed }

        fun with(fragment: Fragment) = JdcrPermission(
            fragment.requireActivity(),
            fragment,
            fragment.requireActivity().activityResultRegistry
        ) {
            fragment.isAdded &&
                    fragment.activity?.let { !it.isFinishing && !it.isDestroyed } == true
        }

        @Throws(IllegalStateException::class)
        fun withView(view: View): JdcrPermission {
            val activity = JdcrPermissionUtils.context2Activity(view.context) as? FragmentActivity
                ?: throw IllegalStateException("context无法转换为Activity")
            return JdcrPermission(activity, activity, activity.activityResultRegistry) {
                !activity.isFinishing && !activity.isDestroyed
            }
        }

    }

    private val permissions = LinkedHashSet<String>()
    private var before: (ExplainScope.(List<String>) -> Unit)? = null
    private var after: (ExplainScope.(List<String>) -> Unit)? = null
    fun permissions(vararg permission: String) = apply { permissions += permission }
    fun permissions(p: Collection<String>) = apply { permissions += p }
    fun onExplainBeforeRequest(block: ExplainScope.(deniedList: List<String>) -> Unit) =
        apply { before = block }

    fun onExplainAfterDenied(block: ExplainScope.(permanentlyDeniedList: List<String>) -> Unit) =
        apply { after = block }

    fun request(callback: (JdcrPermissionResult) -> Unit) {
        val handler = JdcrPermissionHandler(
            activity, lifecycleOwner, registry, aliveCheck,
            permissions.toList(), before, after, callback
        )
        JdcrPermissionDispatcher.enqueue(lifecycleOwner, handler)
    }
}