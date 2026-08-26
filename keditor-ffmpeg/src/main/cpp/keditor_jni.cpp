#include <jni.h>
#include <android/log.h>

extern "C" {
#include <keditor.h>
}

#define LOG_TAG "KEditor"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static void logError(const char *message, int ret) {
    char error[AV_ERROR_MAX_STRING_SIZE];

    av_strerror(
            ret,
            error,
            sizeof(error)
    );

    LOGE("%s: %s", message, error);
}


struct ProgressContext {
    JNIEnv *env;
    jobject callback;
    jmethodID invokeMethod;
    jclass floatClass;
    jmethodID floatConstructor;
    float lastProgress;
};


static void progressHandler(
        void *userData,
        float progress
) {
    auto *context = static_cast<ProgressContext *>(userData);

    if (context == nullptr ||
            context->env == nullptr ||
            context->callback == nullptr) {
        return;
    }

    // Report approximately every 1%.
    if (progress < 1.0f &&
            progress - context->lastProgress < 0.01f) {
        return;
    }

    context->lastProgress = progress;

    JNIEnv *env = context->env;

    jobject value = env->NewObject(
            context->floatClass,
            context->floatConstructor,
            progress
    );

    if (value == nullptr) {
        return;
    }

    env->CallObjectMethod(
            context->callback,
            context->invokeMethod,
            value
    );

    env->DeleteLocalRef(value);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}


extern "C"
JNIEXPORT jint JNICALL
Java_dev_nesyou_keditor_ffmpeg_AndroidMediaEditor_nativeApplyFilter(
        JNIEnv *env,
        jobject /* thiz */,
        jstring inputPath,
        jstring outputPath,
        jstring filterDescr,
        jobject progress
) {
    if (inputPath == nullptr ||
            outputPath == nullptr ||
            filterDescr == nullptr ||
            progress == nullptr) {

        LOGE("nativeApplyFilter: null argument");
        return AVERROR(EINVAL);
    }

    const char *inputCStr =
            env->GetStringUTFChars(inputPath, nullptr);

    const char *outputCStr =
            env->GetStringUTFChars(outputPath, nullptr);

    const char *filterCStr =
            env->GetStringUTFChars(filterDescr, nullptr);

    if (inputCStr == nullptr ||
            outputCStr == nullptr ||
            filterCStr == nullptr) {

        LOGE("nativeApplyFilter: failed to read Java strings");

        if (inputCStr != nullptr) {
            env->ReleaseStringUTFChars(
                    inputPath,
                    inputCStr
            );
        }

        if (outputCStr != nullptr) {
            env->ReleaseStringUTFChars(
                    outputPath,
                    outputCStr
            );
        }

        if (filterCStr != nullptr) {
            env->ReleaseStringUTFChars(
                    filterDescr,
                    filterCStr
            );
        }

        return AVERROR(ENOMEM);
    }

    LOGI(
            "Applying filter \"%s\" to %s -> %s",
            filterCStr,
            inputCStr,
            outputCStr
    );


    jclass functionClass =
            env->GetObjectClass(progress);

    if (functionClass == nullptr) {
        LOGE("Failed to get progress callback class");

        env->ReleaseStringUTFChars(inputPath, inputCStr);
        env->ReleaseStringUTFChars(outputPath, outputCStr);
        env->ReleaseStringUTFChars(filterDescr, filterCStr);

        return AVERROR(EINVAL);
    }


    jmethodID invokeMethod =
            env->GetMethodID(
                    functionClass,
                    "invoke",
                    "(Ljava/lang/Object;)Ljava/lang/Object;"
            );

    if (invokeMethod == nullptr) {
        LOGE("Failed to find Function1.invoke()");

        env->DeleteLocalRef(functionClass);

        env->ReleaseStringUTFChars(inputPath, inputCStr);
        env->ReleaseStringUTFChars(outputPath, outputCStr);
        env->ReleaseStringUTFChars(filterDescr, filterCStr);

        return AVERROR(EINVAL);
    }


    jclass floatClass =
            env->FindClass("java/lang/Float");

    if (floatClass == nullptr) {
        LOGE("Failed to find java.lang.Float");

        env->DeleteLocalRef(functionClass);

        env->ReleaseStringUTFChars(inputPath, inputCStr);
        env->ReleaseStringUTFChars(outputPath, outputCStr);
        env->ReleaseStringUTFChars(filterDescr, filterCStr);

        return AVERROR(EINVAL);
    }


    jmethodID floatConstructor =
            env->GetMethodID(
                    floatClass,
                    "<init>",
                    "(F)V"
            );

    if (floatConstructor == nullptr) {
        LOGE("Failed to find Float constructor");

        env->DeleteLocalRef(functionClass);
        env->DeleteLocalRef(floatClass);

        env->ReleaseStringUTFChars(inputPath, inputCStr);
        env->ReleaseStringUTFChars(outputPath, outputCStr);
        env->ReleaseStringUTFChars(filterDescr, filterCStr);

        return AVERROR(EINVAL);
    }


    ProgressContext context{
            env,
            progress,
            invokeMethod,
            floatClass,
            floatConstructor,
            -1.0f
    };


    int ret = apply_video_filter(
            &context,
            inputCStr,
            outputCStr,
            filterCStr,
            progressHandler
    );


    if (ret >= 0) {
        progressHandler(
                &context,
                1.0f
        );

        LOGI("apply_video_filter succeeded");
    } else {
        logError(
                "apply_video_filter failed",
                ret
        );
    }


    env->DeleteLocalRef(functionClass);
    env->DeleteLocalRef(floatClass);

    env->ReleaseStringUTFChars(
            inputPath,
            inputCStr
    );

    env->ReleaseStringUTFChars(
            outputPath,
            outputCStr
    );

    env->ReleaseStringUTFChars(
            filterDescr,
            filterCStr
    );

    return ret;
}