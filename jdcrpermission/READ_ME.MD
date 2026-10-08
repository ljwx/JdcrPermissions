```kotlin
val activity = this
JdcrPermission.with(activity)
    .permissions(Manifest.permission.CAMERA)
    .onDeniedNoRationale {
        val scope = this
        AlertDialog.Builder(activity)
            .setMessage("相机权限未授予，是否去设置中查看？")
            .setPositiveButton("去设置") { _, _ ->
                runCatching {
                    JdcrPermissionUtils.openAppSettings(activity) {
                        scope.finish()
                    }
                }.onFailure { scope.finish() }
            }
            .setNegativeButton("稍后") { _, _ -> scope.finish() }
            .setOnCancelListener { scope.finish() }
            .show()
    }
    .request { result ->
        if (result.allGranted) {
            // 使用相机能力
        } else {
            // 处理未授权状态
        }
    }
```

`onDeniedNoRationale` 仅在本次系统请求返回拒绝、且系统不建议展示权限说明时触发。
这不等于可以确定为“永久拒绝”；是否提示或打开设置页由调用方决定。
调用 `finish()` 后才会重新检查权限、交付 `request` 结果并执行队列里的下一个请求。
弹窗取消、选择稍后及设置页打开失败等路径也必须调用 `finish()`；重复调用只生效一次。
未配置该回调时，请求结果会直接交付。`result.deniedNoRationale` 表示最终仍被拒绝且无 rationale 的权限；
`systemGranted` 保留系统请求的原始回答，`stateAfter` 是 `finish()` 时的状态。
Activity 或 Fragment 销毁时会结束等待并释放资源，但不会交付结果。
