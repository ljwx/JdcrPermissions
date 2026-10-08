package com.jdcr.jdcrpermission.handler

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.util.JdcrPermissionLog
import java.util.concurrent.atomic.AtomicLong

class JdcrIntentLauncher(
    private val lifecycleOwner: LifecycleOwner,
    private val registry: ActivityResultRegistry,
    private val intent: Intent,
    private val onReturned: () -> Unit
) : DefaultLifecycleObserver {

    private companion object {
        private val SEQ = AtomicLong(0)
    }

    private val id = SEQ.incrementAndGet()
    private var launcher: ActivityResultLauncher<Intent>? = null

    private fun ensureLauncher() {
        if (launcher == null) {
            synchronized(this) {
                launcher ?: registry.register(
                    "jdcr_permission_action_$id",
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    JdcrPermissionLog.i("从意图页面返回:${intent.action}")
                    release()
                    onReturned()
                }.also { launcher = it }
            }
        }
    }

    fun start() {
        ensureLauncher()
        lifecycleOwner.lifecycle.addObserver(this)
        JdcrPermissionLog.i("跳转意图页面:${intent.action}")
        launcher?.launch(intent)
    }

    fun release() {
        JdcrPermissionLog.i("释放意图页面监听器:${intent.action}")
        launcher?.unregister(); launcher = null
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
        super.onDestroy(owner)
    }

}