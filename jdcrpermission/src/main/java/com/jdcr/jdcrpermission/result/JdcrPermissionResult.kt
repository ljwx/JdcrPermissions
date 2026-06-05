package com.jdcr.jdcrpermission.result

data class JdcrPermissionResult(
    val allGranted: Boolean,
    val granted: List<String>,
    val denied: List<String>,
    val foreverDenied: List<String>
)