# KEditor

**KEditor** is a Kotlin Multiplatform video editing library powered by **FFmpeg**.

It provides a simple Kotlin API for performing common video editing operations without requiring users to interact directly with FFmpeg's command-line interface.

KEditor is designed for **Android and iOS** applications and can be integrated into Kotlin Multiplatform projects.

## ✨ Features

* 🎬 Video editing powered by FFmpeg
* ✂️ Trim videos
* 🔄 Rotate videos
* 📐 Scale videos
* ✂️ Crop videos
* 🔃 Horizontal and vertical flip
* 🎨 Video filters
* 🌫️ Blur effects
* ⚫ Grayscale and color adjustments
* 🎞️ Fade effects
* 🐇 Change video speed
* 🔇 Remove audio
* 🎥 Remove video
* 🌍 Kotlin Multiplatform
* 🤖 Android support
* 🍎 iOS support

## 🤖 Supported Architectures

KEditor currently supports the following CPU architectures:

* **Android**

    * `arm64-v8a`
    * `x86_64`
* **iOS**

    * `arm64`
* **iOS Simulator**

    * `arm64` (Apple Silicon)



## 🚀 Getting Started

Add the required KEditor dependencies to your project.

```kotlin
dependencies {
    implementation("dev.nesyou:keditor-core:<version>")
}
```

> Replace `<version>` with the latest KEditor version.

## 🎬 Basic Usage

A simple video editing operation can be created using the KEditor API:

```kotlin
val editor = KEditor {
    removeAudio()
    
    filter(CropFilter(0.0, 0.0, 200, 200))
}

editor.process(
    input = inputPath,
    output = outputPath
)
```

The exact configuration can be extended with editing options such as trimming, filters, compress, and audio/video settings.

## ✂️ Trim a Video

KEditor supports trimming videos using Kotlin's `Duration` API.

```kotlin
KEditor {
    trim(
        start = 10.seconds,
        end = 30.seconds
    )
}
```

For example, this extracts the section between 10 and 30 seconds.

## 🎨 Filters

KEditor provides a filter-based API for applying video effects.

Filters implement the `Filter` interface:

```kotlin
interface Filter {
    fun build(): String
}
```

Multiple filters can be composed and passed to the editor.

### Rotate

```kotlin
RotateFilter(
    angle = 90.0
)
```

A custom fill color can also be specified:

```kotlin
RotateFilter(
    angle = 90.0,
    fillColorHex = "#000000"
)
```

### Scale

```kotlin
ScaleFilter(
    width = 1080,
    height = 1920
)
```

### Blur

```kotlin
GBlurFilter(
    sigma = 10.0
)
```

## 🐇 Change Video Speed

KEditor provides a `Speed` configuration for modifying playback speed.

```kotlin
Speed(
    value = 2.0
)
```

For example:

```text
0.5x → Slow motion
1.0x → Original speed
2.0x → Double speed
```

## 🔇 Remove Audio/Video

Audio/Video can be removed from using:

```kotlin
removeAudio()
removeVideo()
```

This can be useful when converting a video file into an audio-only output.

## 📊 Progress

Long-running video operations can report their progress through `ProgressCallback`.

```kotlin
val callback = object : ProgressCallback {
    override fun onProgress(progress: Int) {
        println("Progress: $progress%")
    }
}
```

This allows applications to display a progress indicator while FFmpeg processes the media.

## 🧩 Custom Filters

KEditor is designed to make adding new FFmpeg filters straightforward.

Implement the `Filter` interface:

```kotlin
class MyFilter : Filter {

    override fun build(): String {
        return "my_ffmpeg_filter"
    }
}
```

Then add the filter to your editing pipeline.

This makes it possible to expose additional FFmpeg filters without changing the core editing architecture.

## 🏗️ Platform Support

| Platform | Support |
| -------- | ------- |
| Android  | ✅       |
| iOS      | ✅       |
| JVM      | 🚧      |
| macOS    | 🚧      |
| Linux    | 🚧      |
| Windows  | 🚧      |

KEditor is built with **Kotlin Multiplatform**, while FFmpeg provides the native media-processing layer.

## 🛠️ Technologies

KEditor is built using:

* **Kotlin**
* **Kotlin Multiplatform**
* **FFmpeg**
* **Android NDK**
* **JNI**
* **Native C/C++**
* **Coroutines**

## 🗺️ Roadmap

* [x] Video trimming
* [x] Scaling
* [x] Cropping
* [x] Rotation
* [x] Horizontal flip
* [x] Vertical flip
* [x] Blur
* [x] Box blur
* [x] EQ adjustments
* [x] Hue
* [x] Fade
* [x] Text overlay
* [x] Video speed
* [x] Remove audio
* [x] Remove video
* [x] Progress reporting
* [ ] Video concatenation
* [ ] Image overlays
* [ ] Watermarks
* [ ] Audio filters
* [ ] Audio replacement
* [ ] Hardware acceleration
* [ ] More FFmpeg filters

## 🤝 Contributing

Contributions are welcome.

If you would like to contribute:

1. Fork the repository.
2. Create a new branch.
3. Make your changes.
4. Add tests where appropriate.
5. Open a pull request.


---

Built with ❤️ using Kotlin and FFmpeg.
