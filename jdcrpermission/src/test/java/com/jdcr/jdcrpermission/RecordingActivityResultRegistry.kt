package com.jdcr.jdcrpermission

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat

internal class RecordingActivityResultRegistry(
    private val resultProvider: (List<String>) -> Map<String, Boolean>
) : ActivityResultRegistry() {

    val launchedPermissions = mutableListOf<List<String>>()

    override fun <I : Any?, O : Any?> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        check(contract is ActivityResultContracts.RequestMultiplePermissions)
        @Suppress("UNCHECKED_CAST")
        val permissions = (input as Array<String>).toList()
        launchedPermissions += permissions

        @Suppress("UNCHECKED_CAST")
        dispatchResult(requestCode, resultProvider(permissions) as O)
    }
}
