package com.zhanghao.music_clip

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
import com.zhanghao.music_clip.ui.theme.Music_ClipTheme

/**
 * mp3 裁剪
 */
class MainActivity : ComponentActivity() {

    private lateinit var musicHandle: MusicHandle

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

        // copy to sd card
        FileUtil.copyAssetFileToExternalStorage(this, "music.mp3")
        musicHandle = MusicHandle(this, 10 * 1000 * 1000, 20 * 1000 * 1000)
        Thread(musicHandle).start()
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