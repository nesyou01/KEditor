cmake_minimum_required(VERSION 3.22.1)

project("keditor")

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(FFMPEG_DIR
        ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native/ffmpeg/android/${ANDROID_ABI}
)

set(X264_DIR
        ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native/x264/android/${ANDROID_ABI}
)

add_library(
        keditor
        SHARED
        keditor_jni.cpp
)

target_include_directories(
        keditor
        PRIVATE
        ${FFMPEG_DIR}/include
        ${X264_DIR}/include
)

target_link_libraries(
        keditor

        # FFmpeg
        ${FFMPEG_DIR}/lib/libavutil.a
        ${FFMPEG_DIR}/lib/libavcodec.a
        ${FFMPEG_DIR}/lib/libavfilter.a
        ${FFMPEG_DIR}/lib/libavdevice.a
        ${FFMPEG_DIR}/lib/libavformat.a
        ${FFMPEG_DIR}/lib/libswscale.a
        ${FFMPEG_DIR}/lib/libswresample.a

        # x264
        ${X264_DIR}/lib/libx264.a

        z
        # Android
        mediandk
        log
        android
)