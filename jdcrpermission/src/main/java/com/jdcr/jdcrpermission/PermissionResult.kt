package com.jdcr.jdcrpermission

data class PermissionResult(
    val allGranted: Boolean,
    val granted: List<String>,
    val denied: List<String>,
    val permanentlyDenied: List<String>
)