package dev.nesyou.keditor.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.nesyou.keditor.ffmpeg.VideoEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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
                    VideoPicker()
                }
            }
        }
    }
    val videoEditor = VideoEditor()

    @Composable
    fun VideoPicker(
    ) {
        val scope = rememberCoroutineScope()

        val picker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch(Dispatchers.IO) {

                val inputFile = File(
                    cacheDir,
                    "input.mp4"
                )

                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->
                        inputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                val outputFile = File(
                    cacheDir,
                    "cropped.mp4"
                )

                val result = videoEditor.testCropaa(
                    inputFile.absolutePath,
                    outputFile.absolutePath
                )

                Log.d(
                    "VideoEditor",
                    "Result=$result output=${outputFile.absolutePath}"
                )
            }
        }

        Button(
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )
            }
        ) {
            Text("Pick Video")
        }
    }

}