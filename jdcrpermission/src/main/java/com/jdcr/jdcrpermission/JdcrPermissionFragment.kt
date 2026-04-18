package com.jdcr.jdcrpermission

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class JdcrPermissionFragment : Fragment() {

    companion object {

        private val hasLaunchedKey = "permission_has_launched"

        private fun getTag(permissions: Array<String>): String {
            return "JdcrPermission:${permissions.contentToString()}"
        }

        fun requestPermission(
            activity: FragmentActivity,
            permissions: Array<String>,
            callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
        ) {
            JdcrPermissionLog.i("发起权限请求:${permissions.contentToString()}")
            if (permissions.isEmpty()) {
                JdcrPermissionLog.w("权限为空,不继续执行")
                return
            }
            val tag = getTag(permissions)
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                JdcrPermissionLog.w("相同权限正在请求,不继续执行")
                return
            }
            val fragment = JdcrPermissionFragment().apply {
                arguments = Bundle().apply { putStringArray("permissions", permissions) }
            }
            activity.supportFragmentManager.setFragmentResultListener(
                getTag(permissions),
                activity
            ) { _, bundle ->
                val allGranted = bundle.getBoolean("allGranted")
                val perms = bundle.getStringArray("permissions")!!
                val granted = bundle.getBooleanArray("granted")!!
                val map = perms.zip(granted.toList()).toMap()
                JdcrPermissionLog.i("触发权限结果回调")
                callback?.invoke(allGranted, map)
                activity.supportFragmentManager.clearFragmentResultListener(tag)
            }
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, tag)
                .commitNow()
            JdcrPermissionLog.i("添加权限fragment")
        }
    }

    private var permissions: Array<String>? = null

    private var launcher: ActivityResultLauncher<Array<String>>? = null

    private var hasLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLaunched = savedInstanceState?.getBoolean(hasLaunchedKey) ?: false
        permissions = arguments?.getStringArray("permissions")
        JdcrPermissionLog.i("fragment收到的权限参数:${permissions?.contentToString()}")
        launcher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                JdcrPermissionLog.i("系统权限请求结果:$result")
                if (activity?.isFinishing == false) {
                    val allGranted = result.values.all { it }
                    permissions?.let {
                        parentFragmentManager.setFragmentResult(
                            getTag(it),
                            Bundle().apply {
                                putBoolean("allGranted", allGranted)
                                putStringArray("permissions", result.keys.toTypedArray())
                                putBooleanArray("granted", result.values.toBooleanArray())
                            }
                        )
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
        val perms = permissions ?: return
        val l = launcher ?: return
        hasLaunched = true
        JdcrPermissionLog.i("调起系统权限请求")
        l.launch(perms)
    }

    override fun onDestroy() {
        super.onDestroy()
        JdcrPermissionLog.w("权限fragment被销毁:$tag")
    }

}