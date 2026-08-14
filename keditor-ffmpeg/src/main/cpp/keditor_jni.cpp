#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
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