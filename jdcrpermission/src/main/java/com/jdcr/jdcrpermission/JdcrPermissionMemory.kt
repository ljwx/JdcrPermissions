package com.jdcr.jdcrpermission

import android.content.Context

internal object JdcrPermissionMemory {
    private const val SP = "jdcr_permission_memory"
    private const val KEY = "requested"
    fun markRequested(context: Context, permissions: Collection<String>) {
        val sp = context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY, emptySet())!!.toMutableSet().apply { addAll(permissions) }
        sp.edit().putStringSet(KEY, set).apply()
    }
    fun hasRequested(context: Context, permission: String): Boolean =
        context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())!!.contains(permission)
}