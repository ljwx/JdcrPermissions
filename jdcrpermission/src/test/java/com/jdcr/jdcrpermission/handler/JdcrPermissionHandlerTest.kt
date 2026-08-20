package com.jdcr.jdcrpermission.handler

import android.Manifest
import android.content.Context
import com.jdcr.jdcrpermission.BeforePermissionRequestScope
import com.jdcr.jdcrpermission.PermanentlyDeniedScope
import com.jdcr.jdcrpermission.PermissionTestActivity
import com.jdcr.jdcrpermission.RecordingActivityResultRegistry
import com.jdcr.jdcrpermission.result.JdcrPermissionResult
import com.jdcr.jdcrpermission.result.JdcrPermissionState
import com.jdcr.jdcrpermission.util.JdcrPermissionUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        handler(registry, listOf(permission)) { result = it }.start()

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

        handler(registry, listOf(permission)) { result = it }.start()

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

        handler(registry, listOf(permission)) { result = it }.start()

        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NO_RATIONALE, stateBefore)
            assertTrue(requestLaunched)
            assertEquals(false, systemGranted)
            assertEquals(JdcrPermissionState.DENIED_NO_RATIONALE, stateAfter)
        }
    }

    @Test
    fun `canceling explanation does not count as system request`() {
        val permission = Manifest.permission.CAMERA
        val registry = RecordingActivityResultRegistry { error("request must not be launched") }
        var result: JdcrPermissionResult? = null
        val before: BeforePermissionRequestScope.() -> Unit = { cancel() }

        handler(registry, listOf(permission), before = before) { result = it }.start()

        assertTrue(registry.launchedPermissions.isEmpty())
        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateBefore)
            assertFalse(requestLaunched)
            assertNull(systemGranted)
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateAfter)
        }
    }

    @Test
    fun `system denial and final granted state are recorded separately`() {
        val permission = Manifest.permission.CAMERA
        val registry = RecordingActivityResultRegistry { permissions ->
            permissions.associateWith { false }
        }
        var result: JdcrPermissionResult? = null
        val after: PermanentlyDeniedScope.() -> Unit = {
            activity.grantedPermissions += permissions
            cancel()
        }

        handler(registry, listOf(permission), after = after) { result = it }.start()

        with(result!!.details.single()) {
            assertEquals(JdcrPermissionState.DENIED_NOT_REQUESTED, stateBefore)
            assertTrue(requestLaunched)
            assertEquals(false, systemGranted)
            assertEquals(JdcrPermissionState.GRANTED, stateAfter)
        }
    }

    private fun handler(
        registry: RecordingActivityResultRegistry,
        requested: List<String>,
        before: (BeforePermissionRequestScope.() -> Unit)? = null,
        after: (PermanentlyDeniedScope.() -> Unit)? = null,
        callback: (JdcrPermissionResult) -> Unit
    ) = JdcrPermissionHandler(
        activity = activity,
        lifecycleOwner = activity,
        registry = registry,
        aliveCheck = { true },
        requested = requested,
        before = before,
        permanentlyDenied = after,
        callback = callback
    )
}
