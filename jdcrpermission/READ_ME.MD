```kotlin
JdcrPermission.with(this)              // Activity 或 Fragment 都行
    .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    .onExplainBeforeRequest { denied ->
        // 弹你自己的 UI, 想继续就 proceed(), 否则 cancel()
        proceed()
    }
    .onExplainAfterDenied { permanentlyDenied ->
        // 被永久拒绝, 引导去设置; proceed()=跳设置页并回来复查, cancel()=直接结束
        proceed()
    }
    .request { result ->
        if (result.allGranted) { /* ... */ }
        else { result.permanentlyDenied /* 需引导设置的 */ }
    }
```