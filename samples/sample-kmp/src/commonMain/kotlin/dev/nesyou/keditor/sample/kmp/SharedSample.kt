package dev.nesyou.keditor.sample.kmp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nesyou.keditor.KEditor
import dev.nesyou.keditor.filters.CropFilter
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

//
// Created by Youness Lagmah on 8/24/26.
//

var nmb = 0

@Composable
fun SharedSample() {
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Button(
                onClick = {
                    scope.launch {
                        nmb++
                        val imageFile = FileKit.openFilePicker(type = FileKitType.Video)
                            ?: return@launch

                        val inputFile = FileKit.filesDir.resolve("input${nmb}.mp4")

                        inputFile.write(imageFile.readBytes())

                        val output = FileKit.cacheDir.resolve("output${nmb}.mp4")

                        val keditor = KEditor {
                            trim(0.seconds, 300.seconds)

                            filter(
                                CropFilter(
                                    width = 200,
                                    height = 200,
                                    startX = 0.0,
                                    startY = 0.0
                                )
                            )
                        }

                        keditor.process(
                            input = inputFile.absolutePath(),
                            output = output.absolutePath(),
                            progress = {
                                println("UNESS PROGRESS $it")
                            }
                        )

                        println("UNESS DONE ${output.absolutePath()}")
                    }
                }
            ) {
                Text("Select File")
            }

        }
    }
}