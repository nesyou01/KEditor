#include <jni.h>
#include <android/log.h>

#include <cstdio>
#include <cstring>

extern "C" {
#include <keditor.h>
}

#define LOG_TAG "CropSample"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void logError(const char* message, int ret)
{
    char error[AV_ERROR_MAX_STRING_SIZE];

    av_strerror(
            ret,
            error,
            sizeof(error)
    );

    LOGE("%s: %s", message, error);
}

extern "C"
JNIEXPORT jint JNICALL
Java_dev_nesyou_keditor_ffmpeg_VideoEditor_nativeApplyFilter(
        JNIEnv* env,
        jobject /* thiz */,
        jstring inputPath,
        jstring outputPath,
        jstring filterDescr)
{
    if (inputPath == nullptr || outputPath == nullptr || filterDescr == nullptr) {
        LOGE("nativeCropVideo: null argument");
        return AVERROR(EINVAL);
    }

    const char* inputCStr  = env->GetStringUTFChars(inputPath, nullptr);
    const char* outputCStr = env->GetStringUTFChars(outputPath, nullptr);
    const char* filterCStr = env->GetStringUTFChars(filterDescr, nullptr);

    if (inputCStr == nullptr || outputCStr == nullptr || filterCStr == nullptr) {
        LOGE("nativeCropVideo: failed to read Java strings");
        if (inputCStr)  env->ReleaseStringUTFChars(inputPath, inputCStr);
        if (outputCStr) env->ReleaseStringUTFChars(outputPath, outputCStr);
        if (filterCStr) env->ReleaseStringUTFChars(filterDescr, filterCStr);
        return AVERROR(ENOMEM);
    }

    LOGI("Applying filter \"%s\" to %s -> %s", filterCStr, inputCStr, outputCStr);

    int ret = apply_video_filter(inputCStr, outputCStr, filterCStr);

    if (ret < 0) {
        LOGI("apply_video_filter not succeeded \"%d\"", ret);

        logError("apply_video_filter failed", ret);
    } else {
        LOGI("apply_video_filter succeeded");
    }

    env->ReleaseStringUTFChars(inputPath, inputCStr);
    env->ReleaseStringUTFChars(outputPath, outputCStr);
    env->ReleaseStringUTFChars(filterDescr, filterCStr);

    return ret;
}