package dev.nesyou.keditor.sample.kmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nesyou.keditor.KEditor
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch

//
// Created by Youness Lagmah on 8/24/26.
//

var nmb = 0

@Composable
fun SharedSample() {
    val scope = rememberCoroutineScope()

    var inputFile by remember { mutableStateOf<PlatformFile?>(null) }
    var outputFile by remember { mutableStateOf<PlatformFile?>(null) }

    var progress by remember { mutableFloatStateOf(0f) }
    var processing by remember { mutableStateOf(false) }

    var removeAudio by remember { mutableStateOf(false) }
    var removeVideo by remember { mutableStateOf(false) }

    var status by remember {
        mutableStateOf("Select a video to start")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "KEditor Test",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = inputFile?.name ?: "No video selected"
        )

        Button(
            enabled = !processing,
            onClick = {
                scope.launch {
                    val file = FileKit.openFilePicker(
                        type = FileKitType.Video
                    )

                    if (file != null) {
                        inputFile = file
                        outputFile = null
                        progress = 0f
                        status = "Video selected"
                    }
                }
            }
        ) {
            Text("Select Video")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Audio / Video"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                enabled = !processing,
                onClick = {
                    removeAudio = !removeAudio
                    if (removeAudio) {
                        removeVideo = false
                    }
                }
            ) {
                Text(
                    if (removeAudio)
                        "Audio Removed"
                    else
                        "Keep Audio"
                )
            }

            OutlinedButton(
                enabled = !processing,
                onClick = {
                    removeVideo = !removeVideo
                    if (removeVideo) {
                        removeAudio = false
                    }
                }
            ) {
                Text(
                    if (removeVideo)
                        "Video Removed"
                    else
                        "Keep Video"
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = inputFile != null && !processing,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val input = inputFile ?: return@Button

                scope.launch {
                    processing = true
                    progress = 0f
                    status = "Processing..."

                    try {
                        val output = FileKit.filesDir.resolve("output_" + input.name)
                        val cacheInput = FileKit.filesDir.resolve(input.name)

                        outputFile = output

                        val editor = KEditor {
                            if (removeAudio) {
                                removeAudio()
                            }

                            if (removeVideo) {
                                removeVideo()
                            }
                        }

                        cacheInput.write(input)

                        editor.process(
                            input = cacheInput.path,
                            output = output.path
                        ) { currentProgress ->
                            progress = currentProgress
                        }

                        progress = 1f
                        status = "Finished"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    } finally {
                        processing = false
                    }
                }
            }
        ) {
            Text("Process Video")
        }

        if (processing) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${(progress * 100).toInt()}%"
            )
        }

        Text(status)

        outputFile?.let { output ->
            OutlinedButton(
                onClick = {
                    scope.launch {
                        FileKit.openFileWithDefaultApplication(output)
                    }
                }
            ) {
                Text("Open Output")
            }
        }
    }
}