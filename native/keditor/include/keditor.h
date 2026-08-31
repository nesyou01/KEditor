//
// Created by Youness Lagmah on 8/23/26.
//

#ifndef KEDITOR_H
#define KEDITOR_H

#include <stdio.h>
#include <string.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>
#include <libavutil/pixdesc.h>

typedef void (*ProgressCallback)(void *env, float progress);

typedef struct AppContext {
    AVFormatContext *in_fmt_ctx;
    AVFormatContext *out_fmt_ctx;
    AVCodecContext *dec_ctx;
    AVCodecContext *enc_ctx;
    AVFilterContext *buffersrc_ctx;
    AVFilterContext *buffersink_ctx;
    AVFilterGraph *filter_graph;
    const AVCodec *encoder;
    int video_stream_idx;
    int out_video_stream_idx;
    ProgressCallback progress_cb;
    void *progress_user_data;
    int64_t trim_start_us;
    int64_t trim_end_us;
} AppContext;


int apply_video_filter(
    void *env,
    const char *in_filename,
    const char *out_filename,
    const char *filter_descr,
    long       start,
    long       end,
    ProgressCallback progress_callback
);

#endif
