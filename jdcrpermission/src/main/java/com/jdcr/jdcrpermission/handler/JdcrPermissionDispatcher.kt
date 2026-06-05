package com.jdcr.jdcrpermission.handler

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.jdcr.jdcrpermission.util.JdcrPermissionLog

internal object JdcrPermissionDispatcher {

    private val queues = HashMap<LifecycleOwner, ArrayDeque<JdcrPermissionHandler>>()

    fun enqueue(lifecycleOwner: LifecycleOwner, handler: JdcrPermissionHandler) {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        val queue = queues.getOrPut(lifecycleOwner) {
            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    queues.remove(lifecycleOwner)
                    owner.lifecycle.removeObserver(this)
                }
            })
            ArrayDeque()
        }
        handler.completeListener = { onFinished(lifecycleOwner, handler) }
        queue.addLast(handler)
        if (queue.size == 1) {
            JdcrPermissionLog.i("当前队列就一条,立马执行")
            handler.start()
        } else {
            JdcrPermissionLog.i("当前队列不止一条,等待执行")
        }
    }

    private fun onFinished(owner: LifecycleOwner, handler: JdcrPermissionHandler) {
        val queue = queues[owner] ?: return
        queue.remove(handler)
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            queues.remove(owner); return
        }
        val next = queue.firstOrNull()
        if (next != null) next.start() else queues.remove(owner)
    }

}