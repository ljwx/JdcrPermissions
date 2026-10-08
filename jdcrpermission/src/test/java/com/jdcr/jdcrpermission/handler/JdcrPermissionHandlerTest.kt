package com.jdcr.jdcrpermission.handler

import android.Manifest
import android.content.Context
import com.jdcr.jdcrpermission.BeforePermissionRequestScope
import com.jdcr.jdcrpermission.DeniedNoRationaleScope
import com.jdcr.jdcrpermission.PermissionTestActivity
import com.jdcr.jdcrpermission.RecordingActivityResultRegistry
import com.jdcr.jdcrpermission.result.JdcrPermissionResult
import com.jdcr.jdcrpermission.result.JdcrPermissionState
import com.jdcr.jdcrpermission.util.JdcrPermissionUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JdcrPermissionHandlerTest {

    private lateinit var controller: ActivityController<PermissionTestActivity>
    private lateinit var activity: PermissionTestActivity

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(PermissionTestActivity::class.java).setup()
        activity = controller.get()
        activity.getSharedPreferences("jdcr_permission_memory", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun `empty permission list returns success without launching request`() {
        val registry = RecordingActivityResultRegistry {
            error("request must not be launched")
        }
        var callbackCount = 0
        var result: JdcrPermissionResult? = null

        handler(registry, emptyList()) {
            callbackCount++
            result = it
        }.start()

        assertEquals(1, callbackCount)
        assertTrue(result!!.allGranted)
        assertTrue(result!!.details.isEmpty())
        assertTrue(registry.launchedPermissions.isEmpty())
    }

    @Test
    fun `already granted permission returns details without launching request`() {
        val permission = Manifest.permission.CAMERA
        activity.grantedPermissions += permission
        val registry = RecordingActivityResultRegistry { error("request must not be launched") }
        var result: JdcrPermissionResult? = null

        handler(registry, listOf(permission), after = {
            error("no-rationale callback must not run for a granted permission")
        }) { result = it }.start()

        assertTrue(result!!.allGranted)
        assertTrue(registry.launchedPermissions.isEmpty())
        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.GRANTED, stateBefore)
            assertFalse(requestLaunched)
            assertNull(systemGranted)
            assertEquals(JdcrPermissionState.GRANTED, stateAfter)
        }
    }

    @Test
    fun `first denial records system result and state transition`() {
        val permission = Manifest.permission.CAMERA
        val registry = RecordingActivityResultRegistry { permissions ->
            activity.rationalePermissions += permissions
            permissions.associateWith { false }
        }
        var result: JdcrPermissionResult? = null

        handler(registry, listOf(permission), after = {
            error("no-rationale callback must not run when rationale is available")
        }) { result = it }.start()

        assertEquals(listOf(listOf(permission)), registry.launchedPermissions)
        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateBefore)
            assertTrue(requestLaunched)
            assertEquals(false, systemGranted)
            assertEquals(JdcrPermissionState.DENIED_SHOW_RATIONALE, stateAfter)
        }
    }

    @Test
    fun `previous no-rationale denial remains distinguishable`() {
        val permission = Manifest.permission.CAMERA
        JdcrPermissionUtils.markRequested(activity, listOf(permission))
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        var result: JdcrPermissionResult? = null
        var completionCount = 0

        val request = handler(registry, listOf(permission)) { result = it }
        request.completeListener = { completionCount++ }
        request.start()

        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NO_RATIONALE, stateBefore)
            assertTrue(requestLaunched)
            assertEquals(false, systemGranted)
            assertEquals(JdcrPermissionState.DENIED_NO_RATIONALE, stateAfter)
        }
        assertEquals(listOf(permission), result!!.deniedNoRationale)
        assertEquals(1, completionCount)
    }

    @Test
    fun `no-rationale handler waits for finish and reports state after external handling`() {
        val permission = Manifest.permission.CAMERA
        JdcrPermissionUtils.markRequested(activity, listOf(permission))
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        val events = mutableListOf<String>()
        var scope: DeniedNoRationaleScope? = null
        var result: JdcrPermissionResult? = null
        val after: DeniedNoRationaleScope.() -> Unit = {
            events += "denied"
            scope = this
        }
        val request = handler(registry, listOf(permission), after = after) {
            events += "result"
            result = it
        }
        request.completeListener = { events += "complete" }

        request.start()

        assertEquals(listOf("denied"), events)
        assertEquals(listOf(permission), scope!!.permissions)
        assertNull(result)

        activity.grantedPermissions += permission
        scope!!.finish()
        scope!!.finish()

        assertEquals(listOf("denied", "result", "complete"), events)
        with(result!!.details.single()) {
            assertEquals(false, systemGranted)
            assertEquals(JdcrPermissionState.GRANTED, stateAfter)
        }
        assertTrue(result!!.allGranted)
    }

    @Test
    fun `next queued request starts only after no-rationale handling finishes`() {
        val deniedPermission = Manifest.permission.CAMERA
        val grantedPermission = Manifest.permission.RECORD_AUDIO
        JdcrPermissionUtils.markRequested(activity, listOf(deniedPermission))
        activity.grantedPermissions += grantedPermission
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        var scope: DeniedNoRationaleScope? = null
        val results = mutableListOf<String>()
        val first = handler(registry, listOf(deniedPermission), after = { scope = this }) {
            results += "first"
        }
        val second = handler(registry, listOf(grantedPermission)) {
            results += "second"
        }

        JdcrPermissionDispatcher.enqueue(activity, first)
        JdcrPermissionDispatcher.enqueue(activity, second)

        assertTrue(results.isEmpty())
        scope!!.finish()
        assertEquals(listOf("first", "second"), results)
    }

    @Test
    fun `first automatic denial without rationale is reported without claiming permanence`() {
        val permission = Manifest.permission.CAMERA
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        var scope: DeniedNoRationaleScope? = null
        var result: JdcrPermissionResult? = null

        handler(registry, listOf(permission), after = { scope = this }) {
            result = it
        }.start()

        assertNull(result)
        scope!!.finish()
        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateBefore)
            assertEquals(JdcrPermissionState.DENIED_NO_RATIONALE, stateAfter)
        }
    }

    @Test
    fun `exception in no-rationale callback still completes request and queue`() {
        val permission = Manifest.permission.CAMERA
        val failure = IllegalStateException("dialog failed")
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        var resultCount = 0
        var completionCount = 0
        val request = handler(registry, listOf(permission), after = { throw failure }) {
            resultCount++
        }
        request.completeListener = { completionCount++ }

        val thrown = runCatching { request.start() }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(1, resultCount)
        assertEquals(1, completionCount)
    }

    @Test
    fun `canceling explanation does not count as system request`() {
        val permission = Manifest.permission.CAMERA
        val registry = RecordingActivityResultRegistry { error("request must not be launched") }
        var result: JdcrPermissionResult? = null
        val before: BeforePermissionRequestScope.() -> Unit = { cancel() }

        handler(registry, listOf(permission), before = before, after = {
            error("no-rationale callback must not run when system request is canceled")
        }) { result = it }.start()

        assertTrue(registry.launchedPermissions.isEmpty())
        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateBefore)
            assertFalse(requestLaunched)
            assertNull(systemGranted)
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateAfter)
        }
    }

    private fun handler(
        registry: RecordingActivityResultRegistry,
        requested: List<String>,
        before: (BeforePermissionRequestScope.() -> Unit)? = null,
        after: (DeniedNoRationaleScope.() -> Unit)? = null,
        callback: (JdcrPermissionResult) -> Unit
    ) = JdcrPermissionHandler(
        activity = activity,
        lifecycleOwner = activity,
        registry = registry,
        aliveCheck = { true },
        requested = requested,
        before = before,
        deniedNoRationale = after,
        callback = callback
    )
}
