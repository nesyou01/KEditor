#include "include/keditor.h"
#include <libavutil/avutil.h>

const char *test_uness() {
    return av_version_info();
}
