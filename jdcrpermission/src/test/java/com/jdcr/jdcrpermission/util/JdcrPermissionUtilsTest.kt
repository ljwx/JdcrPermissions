package com.jdcr.jdcrpermission.util

import android.Manifest
import android.content.Context
import com.jdcr.jdcrpermission.PermissionTestActivity
import com.jdcr.jdcrpermission.result.JdcrPermissionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JdcrPermissionUtilsTest {

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
    fun `granted permission reports granted`() {
        activity.grantedPermissions += Manifest.permission.CAMERA

        assertEquals(
            JdcrPermissionState.GRANTED,
            JdcrPermissionUtils.getState(activity, Manifest.permission.CAMERA)
        )
    }

    @Test
    fun `permission without request history reports not requested`() {
        assertEquals(
            JdcrPermissionState.DENIED_NOT_REQUESTED,
            JdcrPermissionUtils.getState(activity, Manifest.permission.CAMERA)
        )
    }

    @Test
    fun `requested permission with rationale reports show rationale`() {
        val permission = Manifest.permission.CAMERA
        JdcrPermissionUtils.markRequested(activity, listOf(permission))
        activity.rationalePermissions += permission

        assertEquals(
            JdcrPermissionState.DENIED_SHOW_RATIONALE,
            JdcrPermissionUtils.getState(activity, permission)
        )
    }

    @Test
    fun `requested permission without rationale reports no rationale`() {
        val permission = Manifest.permission.CAMERA
        JdcrPermissionUtils.markRequested(activity, listOf(permission))

        assertEquals(
            JdcrPermissionState.DENIED_NO_RATIONALE,
            JdcrPermissionUtils.getState(activity, permission)
        )
    }
}
