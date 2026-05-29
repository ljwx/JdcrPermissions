package com.jdcr.jdcrpermission

import android.app.Fragment
import android.app.FragmentManager
import androidx.lifecycle.LifecycleOwner

interface ExplainScope {
    val permissions: List<String>
    fun proceed()   // before: 继续发起系统请求; after: 跳转应用设置页
    fun cancel()    // 放弃, 直接回调当前结果
}
class JdcrPermission private constructor(
    internal val fragmentManager: FragmentManager,
    internal val lifecycleOwner: LifecycleOwner,
    internal val aliveCheck: () -> Boolean
) {
    companion object {
        fun with(activity: FragmentActivity) = JdcrPermission(
            activity.supportFragmentManager, activity
        ) { !activity.isFinishing && !activity.isDestroyed }
        fun with(fragment: Fragment) = JdcrPermission(
            fragment.childFragmentManager, fragment
        ) { fragment.isAdded && fragment.activity?.let { !it.isFinishing && !it.isDestroyed } == true }
    }
    private val permissions = LinkedHashSet<String>()
    private var before: (ExplainScope.(List<String>) -> Unit)? = null
    private var after: (ExplainScope.(List<String>) -> Unit)? = null
    fun permissions(vararg p: String) = apply { permissions += p }
    fun permissions(p: Collection<String>) = apply { permissions += p }
    fun onExplainBeforeRequest(block: ExplainScope.(deniedList: List<String>) -> Unit) =
        apply { before = block }
    fun onExplainAfterDenied(block: ExplainScope.(permanentlyDeniedList: List<String>) -> Unit) =
        apply { after = block }
    fun request(callback: (PermissionResult) -> Unit) {
        JdcrPermissionFragment.start(this, permissions.toList(), before, after, callback)
    }
}