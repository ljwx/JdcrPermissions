package com.jdcr.jdcrpermissioncommon

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.jdcr.jdcrpermission.JdcrPermission
import com.jdcr.jdcrpermission.util.JdcrPermissionLog
import com.jdcr.jdcrpermissioncommon.ui.theme.JdcrPermissionCommonTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        JdcrPermissionLog.enable(true, cacheDir.absolutePath + "/log.txt")

        JdcrPermission.with(this).onExplainBeforeRequest {

        }

//        JdcrPermissionUtils.openLocationSettings(this) {
//
//        }
        setContent {
            JdcrPermissionCommonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column() {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = {

        }) { }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JdcrPermissionCommonTheme {
        Greeting("Android")
    }
}