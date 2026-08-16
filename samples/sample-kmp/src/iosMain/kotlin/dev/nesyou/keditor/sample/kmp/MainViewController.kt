package dev.nesyou.keditor.sample.kmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import dev.nesyou.keditor.VideoEditor

fun MainViewController() = ComposeUIViewController {
    val editor = VideoEditor()

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            Text(editor.ffmpegVersion())
            Text(editor.ffmpegMajorVersion().toString())
        }
    }

}