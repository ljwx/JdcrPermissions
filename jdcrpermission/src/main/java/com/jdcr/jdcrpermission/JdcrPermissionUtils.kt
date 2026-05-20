package com.jdcr.jdcrpermission

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
        activity: AppCompatActivity,
        permissions: Array<String>,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) {
        JdcrPermissionFragment.requestPermission(activity, permissions, callback)
    }

    fun checkAndRequest(
        activity: AppCompatActivity,
        permission: String,
        callback: ((allGranted: Boolean, Map<String, Boolean>) -> Unit)?
    ) {
        if (check(activity, permission)) {
            callback?.invoke(true, mapOf(permission to true))
        } else {
            request(activity, arrayOf(permission), callback)
        }
    }

    fun openAppSettings(
        activity: AppCompatActivity,
        callback: (() -> Unit)
    ) {
        JdcrOpenPageFragment.open(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, callback)
    }

    fun openBluetoothSettings(
        activity: AppCompatActivity,
        callback: (() -> Unit)
    ) {
        JdcrOpenPageFragment.open(activity, Settings.ACTION_BLUETOOTH_SETTINGS, callback)
    }

    fun openLocationSettings(
        activity: AppCompatActivity,
        callback: (() -> Unit)
    ) {
        JdcrOpenPageFragment.open(activity, Settings.ACTION_LOCATION_SOURCE_SETTINGS, callback)
    }

}