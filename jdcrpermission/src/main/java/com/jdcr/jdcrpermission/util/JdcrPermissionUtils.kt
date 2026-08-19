package com.jdcr.jdcrpermission.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.jdcr.jdcrpermission.handler.JdcrOpenActionHandler
import com.jdcr.jdcrpermission.result.JdcrPermissionState

object JdcrPermissionUtils {

    private const val SP = "jdcr_permission_memory"
    private const val KEY = "requested"

    internal fun markRequested(context: Context, permissions: Collection<String>) {
        val sp = context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY, emptySet())!!.toMutableSet().apply { addAll(permissions) }
        sp.edit().putStringSet(KEY, set).apply()
    }

    internal fun hasRequested(context: Context, permission: String): Boolean =
        context.applicationContext
            .getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            .orEmpty()
            .contains(permission)

    internal fun getState(
        activity: Activity,
        permission: String
    ): JdcrPermissionState {
        if (isGranted(activity, permission)) {
            return JdcrPermissionState.GRANTED
        }

        if (!hasRequested(activity, permission)) {
            return JdcrPermissionState.DENIED_NOT_REQUESTED
        }

        return if (
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        ) {
            JdcrPermissionState.DENIED_SHOW_RATIONALE
        } else {
            JdcrPermissionState.DENIED_NO_RATIONALE
        }
    }

    fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isForeverDenied(activity: Activity, permission: String): Boolean =
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                hasRequested(activity, permission)

    fun openAppSettings(activity: FragmentActivity, callback: () -> Unit) {
        JdcrOpenActionHandler(
            activity, activity.activityResultRegistry,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }, callback
        ).start()
    }

    fun openIntent(activity: FragmentActivity, intent: Intent, callback: () -> Unit) {
        JdcrOpenActionHandler(
            activity, activity.activityResultRegistry,
            intent, callback
        ).start()
    }

}