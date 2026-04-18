package com.jdcr.jdcrpermissioncommon

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jdcr.jdcrpermission.JdcrOpenPageFragment
import com.jdcr.jdcrpermission.JdcrPermissionFragment
import com.jdcr.jdcrpermission.JdcrPermissionLog
import com.jdcr.jdcrpermission.JdcrPermissionUtils
import com.jdcr.jdcrpermissioncommon.ui.theme.JdcrPermissionCommonTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        JdcrPermissionLog.enable(true, cacheDir.absolutePath + "/log.txt")
        JdcrPermissionUtils.checkAndRequest(this, Manifest.permission.CAMERA) { allGranted, map ->

        }
        JdcrPermissionFragment.requestPermission(
            this@MainActivity,
            arrayOf(Manifest.permission.CAMERA)
        ) { allGranted, map ->

        }
        JdcrOpenPageFragment.open(this@MainActivity, Settings.ACTION_SETTINGS) {

        }
        JdcrOpenPageFragment.open(this@MainActivity, Settings.ACTION_SETTINGS) {

        }
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