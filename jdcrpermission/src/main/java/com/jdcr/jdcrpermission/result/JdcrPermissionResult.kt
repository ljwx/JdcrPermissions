package com.jdcr.jdcrpermission.result

enum class JdcrPermissionState {
    GRANTED,

    // App安装后还没有请求过
    DENIED_NOT_REQUESTED,

    // Android建议再次请求前展示权限说明
    DENIED_SHOW_RATIONALE,

    // 已请求过，但系统不建议展示权限说明
    DENIED_NO_RATIONALE
}

data class JdcrPermissionDetail(
    val permission: String,
    val stateBefore: JdcrPermissionState,
    val requestLaunched: Boolean,
    val systemGranted: Boolean?,
    val stateAfter: JdcrPermissionState
)

data class JdcrPermissionResult(
    val details: List<JdcrPermissionDetail>
) {
    val allGranted: Boolean
        get() = details.all { it.stateAfter == JdcrPermissionState.GRANTED }

    val granted: List<String>
        get() = details.filter {
            it.stateAfter == JdcrPermissionState.GRANTED
        }.map { it.permission }

    val denied: List<String>
        get() = details.filter {
            it.stateAfter != JdcrPermissionState.GRANTED
        }.map { it.permission }
}