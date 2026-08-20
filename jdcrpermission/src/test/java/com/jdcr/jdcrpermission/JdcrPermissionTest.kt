package com.jdcr.jdcrpermission

import android.Manifest
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
class JdcrPermissionTest {

    private lateinit var controller: ActivityController<PermissionTestActivity>
    private lateinit var activity: PermissionTestActivity

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(PermissionTestActivity::class.java).setup()
        activity = controller.get()
    }

    @After
    fun tearDown() {
        controller.destroy()
    }

    @Test
    fun `reused builder does not retain permissions from previous request`() {
        val firstPermission = Manifest.permission.CAMERA
        val secondPermission = Manifest.permission.RECORD_AUDIO
        activity.grantedPermissions += setOf(firstPermission, secondPermission)
        val manager = JdcrPermission.with(activity)
        var firstResult = emptyList<String>()
        var secondResult = emptyList<String>()

        manager.permissions(firstPermission).request {
            firstResult = it.details.map { detail -> detail.permission }
        }
        manager.permissions(secondPermission).request {
            secondResult = it.details.map { detail -> detail.permission }
        }

        assertEquals(listOf(firstPermission), firstResult)
        assertEquals(listOf(secondPermission), secondResult)
    }
}
