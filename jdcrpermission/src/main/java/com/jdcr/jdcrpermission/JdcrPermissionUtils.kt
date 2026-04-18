package com.jdcr.jdcrpermission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object JdcrPermissionUtils {

    fun check(context: Context, permission: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context.applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        JdcrPermissionLog.w("权限检查结果,$permission:$granted")
        return granted
    }

    fun request(
        activity: FragmentActivity,
        permissions: Array<String>,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) {
        JdcrPermissionFragment.requestPermission(activity, permissions, callback)
    }

    fun checkAndRequest(
        activity: FragmentActivity,
        permission: String,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) {
        if (check(activity, permission)) {
            callback?.invoke(true, mapOf(permission to true))
        } else {
            request(activity, arrayOf(permission), callback)
        }
    }

}