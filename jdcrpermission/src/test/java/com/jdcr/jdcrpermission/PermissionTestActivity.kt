package com.jdcr.jdcrpermission

import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity

internal class PermissionTestActivity : FragmentActivity() {

    val grantedPermissions = mutableSetOf<String>()
    val rationalePermissions = mutableSetOf<String>()

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
        permissionResult(permission)

    override fun checkSelfPermission(permission: String): Int =
        permissionResult(permission)

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean =
        permission in rationalePermissions

    private fun permissionResult(permission: String): Int =
        if (permission in grantedPermissions) {
            PackageManager.PERMISSION_GRANTED
        } else {
            PackageManager.PERMISSION_DENIED
        }
}
