package com.jdcr.jdcrpermission.result

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JdcrPermissionResultTest {

    @Test
    fun `empty details are treated as all granted`() {
        val result = JdcrPermissionResult(emptyList())

        assertTrue(result.allGranted)
        assertTrue(result.granted.isEmpty())
        assertTrue(result.denied.isEmpty())
    }

    @Test
    fun `derived collections use final permission state`() {
        val result = JdcrPermissionResult(
            listOf(
                detail(Manifest.permission.CAMERA, JdcrPermissionState.GRANTED),
                detail(Manifest.permission.RECORD_AUDIO, JdcrPermissionState.DENIED_NO_RATIONALE)
            )
        )

        assertFalse(result.allGranted)
        assertEquals(listOf(Manifest.permission.CAMERA), result.granted)
        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), result.denied)
    }

    private fun detail(permission: String, stateAfter: JdcrPermissionState) =
        JdcrPermissionDetail(
            permission = permission,
            stateBefore = JdcrPermissionState.DENIED_NOT_REQUESTED,
            requestLaunched = true,
            systemGranted = stateAfter == JdcrPermissionState.GRANTED,
            stateAfter = stateAfter
        )
}
