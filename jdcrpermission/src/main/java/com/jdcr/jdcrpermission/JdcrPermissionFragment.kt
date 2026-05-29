package com.jdcr.jdcrpermission

import android.app.Fragment
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class JdcrPermissionFragment : Fragment() {

    companion object {
        private const val TAG = "JdcrPermissionFragment"
        fun start(
            owner: JdcrPermission,
            permissions: List<String>,
            before: (ExplainScope.(List<String>) -> Unit)?,
            after: (ExplainScope.(List<String>) -> Unit)?,
            callback: (PermissionResult) -> Unit
        ) {
            if (permissions.isEmpty() || !owner.aliveCheck()) return
            // 状态机: 仅在 STARTED 且状态未保存时操作 FragmentManager, 否则延后到 onStart
            runWhenStarted(owner) {
                val fm = owner.fragmentManager
                if (fm.isStateSaved || !owner.aliveCheck()) return@runWhenStarted
                val fragment = (fm.findFragmentByTag(TAG) as? JdcrPermissionFragment)
                    ?: JdcrPermissionFragment().also {
                        fm.beginTransaction().add(it, TAG).commitNow()
                    }
                fragment.enqueue(permissions, before, after, callback)
            }
        }
        private fun runWhenStarted(owner: JdcrPermission, action: () -> Unit) {
            val lifecycle = owner.lifecycleOwner.lifecycle
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !owner.fragmentManager.isStateSaved) {
                action()
            } else {
                lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onStart(o: LifecycleOwner) { lifecycle.removeObserver(this); action() }
                    override fun onDestroy(o: LifecycleOwner) = lifecycle.removeObserver(this)
                })
            }
        }
    }
    private var before: (ExplainScope.(List<String>) -> Unit)? = null
    private var after: (ExplainScope.(List<String>) -> Unit)? = null
    private var callback: ((PermissionResult) -> Unit)? = null
    private var requested: List<String> = emptyList()
    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { handleResult() }
    }
    fun enqueue(
        permissions: List<String>,
        before: (ExplainScope.(List<String>) -> Unit)?,
        after: (ExplainScope.(List<String>) -> Unit)?,
        callback: (PermissionResult) -> Unit
    ) {
        if (this.callback != null) return // 已有进行中的请求, 忽略并发请求
        this.before = before; this.after = after
        this.callback = callback; this.requested = permissions
        val denied = permissions.filterNot { isGranted(it) }
        if (denied.isEmpty()) { deliver(); return }
        if (before != null) before.invoke(scope(denied) { launch(denied) }, denied) // 请求前解释
        else launch(denied)
    }
    private fun launch(permissions: List<String>) {
        context?.let { JdcrPermissionMemory.markRequested(it, permissions) }
        launcher.launch(permissions.toTypedArray())
    }
    private fun handleResult() {
        if (callback == null) return
        val denied = requested.filterNot { isGranted(it) }
        if (denied.isEmpty()) { deliver(); return }
        val permanent = denied.filter { isPermanentlyDenied(it) }
        val after = after
        if (after != null && permanent.isNotEmpty()) after.invoke(scope(permanent) { openSettings() }, permanent) // 被拒后解释
        else deliver()
    }
    private fun openSettings() {
        val activity = activity ?: return deliver()
        JdcrOpenPageFragment.open(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS) { deliver() } // 回来后重新计算
    }
    private fun deliver() {
        val cb = callback ?: return
        val granted = requested.filter { isGranted(it) }
        val denied = requested.filterNot { isGranted(it) }
        val permanent = denied.filter { isPermanentlyDenied(it) }
        callback = null; before = null; after = null // 完成即清空, 断开对宿主的引用
        cb(PermissionResult(denied.isEmpty(), granted, denied, permanent))
    }
    private fun isGranted(p: String) =
        ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED
    // 请求过 + 系统不再展示解释 ⇒ 永久拒绝(不再询问)
    private fun isPermanentlyDenied(p: String): Boolean {
        val act = activity ?: return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(act, p) &&
                JdcrPermissionMemory.hasRequested(act, p)
    }
    private fun scope(perms: List<String>, onProceed: () -> Unit) = object : ExplainScope {
        override val permissions = perms
        override fun proceed() = onProceed()
        override fun cancel() = deliver()
    }

}