//
// Created by Youness Lagmah on 8/15/26.
//

#include "keditor.h"

extern "C" {
    #include "libavutil/avutil.h"
}

const char* kffmpeg_get_version() {
    return av_version_info();
}

int kffmpeg_get_major_version() {
    return AV_VERSION_MAJOR(LIBAVUTIL_VERSION_INT);
}