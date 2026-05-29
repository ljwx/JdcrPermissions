package com.jdcr.jdcrpermission

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object JdcrPermissionUtils {

    fun check(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context.applicationContext, permission) == PackageManager.PERMISSION_GRANTED
    fun request(
        activity: FragmentActivity,
        permissions: Array<String>,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) {
        JdcrPermission.with(activity).permissions(*permissions).request { r ->
            callback?.invoke(r.allGranted, r.granted.associateWith { true } + r.denied.associateWith { false })
        }
    }
    fun checkAndRequest(
        activity: FragmentActivity,
        permission: String,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) = request(activity, arrayOf(permission), callback)
    fun openAppSettings(activity: FragmentActivity, callback: () -> Unit) =
        JdcrOpenPageFragment.open(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, callback)

}