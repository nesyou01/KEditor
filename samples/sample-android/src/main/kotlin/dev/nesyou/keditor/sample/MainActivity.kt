package dev.nesyou.keditor.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.nesyou.keditor.ffmpeg.VideoEditor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val editor = VideoEditor()

        setContent {

            Scaffold {padding->
                Column(
                    modifier = Modifier.padding(padding)
                ) {
                    Text(editor.ffmpegVersion())
                    Text(editor.ffmpegMajorVersion().toString())
                }
            }
        }
    }

}