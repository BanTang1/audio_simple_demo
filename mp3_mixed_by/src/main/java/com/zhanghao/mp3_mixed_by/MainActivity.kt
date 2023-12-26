package com.zhanghao.mp3_mixed_by

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zhanghao.mp3_mixed_by.ui.theme.Music_ClipTheme

/**
 * 将两个mp3 文件混音， 前提是两个mp3文件的 采样率 通道数 采样深度一致;
 * 两个音频通过 FFmpeg 将其 采样率 通道数 采样深度 保持一致，也使用FFmpeg成功混音;
 * 此处简单编写理解原理即可
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Music_ClipTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }

        // copy to sdcard
        FileUtil.copyAssetFilesToExternalStorage(this, "music1.mp3", "music2.mp3")
        // start
        Thread(MusicMixedByHandle(this)).start()

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Music_ClipTheme {
        Greeting("Android")
    }
}