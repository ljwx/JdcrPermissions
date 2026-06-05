package com.jdcr.jdcrpermission.handler

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.util.JdcrPermissionLog
import java.util.concurrent.atomic.AtomicLong

class JdcrOpenActionHandler(
    private val lifecycleOwner: LifecycleOwner,
    private val registry: ActivityResultRegistry,
    private val intent: Intent,
    private val callback: () -> Unit
) : DefaultLifecycleObserver {

    private companion object {
        private val SEQ = AtomicLong(0)
    }

    private val id = SEQ.incrementAndGet()
    private var settingsLauncher: ActivityResultLauncher<Intent>? = null

    private fun create() {
        if (settingsLauncher == null) {
            synchronized(this) {
                settingsLauncher ?: registry.register(
                    "jdcr_permission_action_$id",
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    JdcrPermissionLog.i("从意图页回来:${intent.action}")
                    onRelease()
                    callback()
                }.also { settingsLauncher = it }
            }
        }
    }

    fun start() {
        create()
        lifecycleOwner.lifecycle.addObserver(this)
        JdcrPermissionLog.i("跳转意图页:${intent.action}")
        settingsLauncher?.launch(intent)
    }

    fun onRelease() {
        JdcrPermissionLog.i("清除跳转意图页的监听器:${intent.action}")
        settingsLauncher?.unregister(); settingsLauncher = null
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onRelease()
        super.onDestroy(owner)
    }

}