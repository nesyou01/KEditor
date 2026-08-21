#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include <x264.h>
#include <x264_config.h>
#include <libavutil/avutil.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavutil/avutil.h>
}

#define LOG_TAG "KVideoKit"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_dev_nesyou_keditor_ffmpeg_VideoEditor_nativeGetFFmpegVersion(
        JNIEnv* env,
        jobject /* thiz */) {

    const AVCodec* x264 = avcodec_find_encoder_by_name("libx264");
//
//    if (x264 == nullptr) {
//        LOGE("❌ libx264 NOT found");
//    } else {
//        LOGI("✅ libx264 found!");
//
//        LOGI("Encoder: %s", x264->name ? x264->name : "unknown");
//
//        LOGI("Long name: %s",
//                x264->long_name ? x264->long_name : "unknown");
//
//        LOGI("ID: %d", x264->id);
//    }

    const char* version = av_version_info();

    return env->NewStringUTF(version);
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_nesyou_keditor_ffmpeg_VideoEditor_nativeGetFFmpegMajorVersion(
        JNIEnv* env,
        jobject /* thiz */) {

    return AV_VERSION_MAJOR(LIBAVUTIL_VERSION_INT);
}