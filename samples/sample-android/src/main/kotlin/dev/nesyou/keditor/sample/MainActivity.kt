package dev.nesyou.keditor.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.nesyou.keditor.VideoEditor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val editor = VideoEditor()

        println(editor.ffmpegVersion())
        println(editor.ffmpegMajorVersion())

        setContent {


        }
    }

}