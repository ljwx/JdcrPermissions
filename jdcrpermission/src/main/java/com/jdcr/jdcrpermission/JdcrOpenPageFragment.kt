package com.jdcr.jdcrpermission

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class JdcrOpenPageFragment : Fragment() {

    companion object {

        private val hasLaunchedKey = "page_has_launched"

        private fun getTag(action: String): String {
            return "JdcrPermission:${action}"
        }

        fun open(activity: FragmentActivity, action: String, callback: () -> Unit) {
            JdcrPermissionLog.i("发起页面跳转请求:$action")
            val tag = getTag(action)
            if (action.isEmpty()) {
                JdcrPermissionLog.w("页面为空,不继续执行")
                return
            }
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                JdcrPermissionLog.w("相同页面已跳转,不继续执行")
                return
            }
            val fragment = JdcrOpenPageFragment().apply {
                arguments = Bundle().apply { putString("action", action) }
            }
            activity.supportFragmentManager.setFragmentResultListener(
                tag,
                activity
            ) { _, bundle ->
                callback.invoke()
                activity.supportFragmentManager.clearFragmentResultListener(tag)
            }
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, tag)
                .commitNow()
            JdcrPermissionLog.i("添加页面跳转fragment")
        }

    }

    private var action: String? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLaunched = savedInstanceState?.getBoolean(hasLaunchedKey) ?: false
        action = arguments?.getString("action")
        JdcrPermissionLog.i("fragment收到的页面:$action")
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            JdcrPermissionLog.i("页面跳转结果:${it.resultCode}")
            if (activity?.isFinishing == false) {
                action?.let {
                    parentFragmentManager.setFragmentResult(getTag(it), Bundle())
                }
            }
            JdcrPermissionLog.i("移除fragment")
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(hasLaunchedKey, hasLaunched)
    }

    override fun onStart() {
        super.onStart()
        if (hasLaunched) return
        val act = action ?: return
        val l = launcher ?: return
        hasLaunched = true
        JdcrPermissionLog.i("执行系统跳转页面")
        l.launch(buildIntent(act))
    }

    private fun buildIntent(act: String): Intent {
        return if (act == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
            Intent(act).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
        } else {
            Intent(act)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        JdcrPermissionLog.w("页面跳转fragment被销毁:$tag")
    }

}