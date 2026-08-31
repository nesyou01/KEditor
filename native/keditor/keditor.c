#include "include/keditor.h"


static void app_context_init(AppContext *app) {
    memset(app, 0, sizeof(*app));

    app->video_stream_idx = -1;
    app->audio_stream_idx = -1;

    app->out_video_stream_idx = -1;
    app->out_audio_stream_idx = -1;
}


/*
 * Report progress based on video packets.
 */
static void report_progress(
    const AppContext *app,
    const AVPacket *pkt
) {
    if (!app->progress_cb || !app->in_fmt_ctx)
        return;

    if (pkt->stream_index != app->video_stream_idx)
        return;

    if (pkt->pts == AV_NOPTS_VALUE)
        return;

    const AVStream *stream =
        app->in_fmt_ctx->streams[pkt->stream_index];

    const int64_t pts_us = av_rescale_q(
        pkt->pts,
        stream->time_base,
        AV_TIME_BASE_Q
    );

    const int64_t range_start = app->trim_start_us;

    const int64_t range_end =
        app->trim_end_us > 0
            ? app->trim_end_us
            : app->in_fmt_ctx->duration;

    if (range_end <= range_start)
        return;

    float fraction =
        (float)(pts_us - range_start) /
        (float)(range_end - range_start);

    if (fraction < 0.0f)
        fraction = 0.0f;

    if (fraction > 1.0f)
        fraction = 1.0f;

    app->progress_cb(
        app->progress_user_data,
        fraction
    );
}


/*
 * Cleanup.
 */
static void app_context_cleanup(AppContext *app) {
    if (app->filter_graph)
        avfilter_graph_free(&app->filter_graph);

    if (app->dec_ctx)
        avcodec_free_context(&app->dec_ctx);

    if (app->enc_ctx)
        avcodec_free_context(&app->enc_ctx);

    if (app->in_fmt_ctx)
        avformat_close_input(&app->in_fmt_ctx);

    if (app->out_fmt_ctx) {
        if (!(app->out_fmt_ctx->oformat->flags & AVFMT_NOFILE) &&
            app->out_fmt_ctx->pb) {

            avio_closep(&app->out_fmt_ctx->pb);
        }

        avformat_free_context(app->out_fmt_ctx);
        app->out_fmt_ctx = NULL;
    }
}


/*
 * Open input and find video/audio streams.
 */
static int open_input(
    AppContext *app,
    const char *filename
) {
    int ret = avformat_open_input(
        &app->in_fmt_ctx,
        filename,
        NULL,
        NULL
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot open input file '%s'\n",
            filename
        );

        return ret;
    }

    ret = avformat_find_stream_info(
        app->in_fmt_ctx,
        NULL
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot find stream information\n"
        );

        return ret;
    }


    /*
     * Find video stream.
     */
    app->video_stream_idx = av_find_best_stream(
        app->in_fmt_ctx,
        AVMEDIA_TYPE_VIDEO,
        -1,
        -1,
        NULL,
        0
    );


    /*
     * Find audio stream.
     */
    app->audio_stream_idx = av_find_best_stream(
        app->in_fmt_ctx,
        AVMEDIA_TYPE_AUDIO,
        -1,
        -1,
        NULL,
        0
    );


    /*
     * At least one stream must exist.
     */
    if (app->video_stream_idx < 0 &&
        app->audio_stream_idx < 0) {

        fprintf(
            stderr,
            "Input contains neither video nor audio\n"
        );

        return AVERROR_STREAM_NOT_FOUND;
    }


    /*
     * Open video decoder only when video is required.
     */
    if (!app->remove_video) {

        if (app->video_stream_idx < 0) {
            fprintf(
                stderr,
                "Input does not contain a video stream\n"
            );

            return AVERROR_STREAM_NOT_FOUND;
        }

        AVStream *stream =
            app->in_fmt_ctx->streams[
                app->video_stream_idx
            ];

        const AVCodec *decoder =
            avcodec_find_decoder(
                stream->codecpar->codec_id
            );

        if (!decoder) {
            fprintf(
                stderr,
                "Failed to find video decoder\n"
            );

            return AVERROR_DECODER_NOT_FOUND;
        }

        app->dec_ctx =
            avcodec_alloc_context3(decoder);

        if (!app->dec_ctx)
            return AVERROR(ENOMEM);

        ret = avcodec_parameters_to_context(
            app->dec_ctx,
            stream->codecpar
        );

        if (ret < 0)
            return ret;

        app->dec_ctx->pkt_timebase =
            stream->time_base;

        ret = avcodec_open2(
            app->dec_ctx,
            decoder,
            NULL
        );

        if (ret < 0) {
            fprintf(
                stderr,
                "Failed to open video decoder\n"
            );

            return ret;
        }
    }

    return 0;
}


/*
 * Select video encoder.
 */
static int choose_encoder(AppContext *app) {
    if (!app->encoder) {
        app->encoder =
            avcodec_find_encoder_by_name("libx264");
    }

    if (!app->encoder) {
        fprintf(
            stderr,
            "Necessary encoder not found\n"
        );

        return AVERROR_ENCODER_NOT_FOUND;
    }

    return 0;
}


/*
 * Build final filter chain.
 */
static int build_filter_chain(
    AppContext *app,
    const char *filter_descr,
    char *out,
    size_t out_size
) {
    const enum AVPixelFormat *pix_fmts = NULL;
    int nb_pix_fmts = 0;

    char fmt_list[256];

    size_t used = 0;

    int ret =
        avcodec_get_supported_config(
            NULL,
            app->encoder,
            AV_CODEC_CONFIG_PIX_FORMAT,
            0,
            (const void **)&pix_fmts,
            &nb_pix_fmts
        );

    if (ret < 0)
        return ret;


    /*
     * Encoder accepts any pixel format.
     */
    if (!pix_fmts || nb_pix_fmts <= 0) {

        if (snprintf(
                out,
                out_size,
                "%s",
                filter_descr
            ) >= (int)out_size) {

            return AVERROR(ENOSPC);
        }

        return 0;
    }


    fmt_list[0] = '\0';


    for (int i = 0; i < nb_pix_fmts; i++) {

        const char *name =
            av_get_pix_fmt_name(
                pix_fmts[i]
            );

        if (!name)
            continue;

        int written =
            snprintf(
                fmt_list + used,
                sizeof(fmt_list) - used,
                "%s%s",
                used ? "|" : "",
                name
            );

        if (written < 0 ||
            used + (size_t)written >= sizeof(fmt_list)) {

            break;
        }

        used += (size_t)written;
    }


    if (used == 0) {

        if (snprintf(
                out,
                out_size,
                "%s",
                filter_descr
            ) >= (int)out_size) {

            return AVERROR(ENOSPC);
        }

        return 0;
    }


    if (snprintf(
            out,
            out_size,
            "%s,format=%s",
            filter_descr,
            fmt_list
        ) >= (int)out_size) {

        return AVERROR(ENOSPC);
    }

    return 0;
}


/*
 * Initialize video filter graph.
 */
static int init_filters(
    AppContext *app,
    const char *filter_descr
) {
    char args[512];
    char full_filter_descr[768];

    int ret;

    AVFilterInOut *outputs = NULL;
    AVFilterInOut *inputs = NULL;

    const AVFilter *buffersrc =
        avfilter_get_by_name("buffer");

    const AVFilter *buffersink =
        avfilter_get_by_name("buffersink");

    AVRational time_base =
        app->in_fmt_ctx
            ->streams[app->video_stream_idx]
            ->time_base;


    outputs = avfilter_inout_alloc();
    inputs = avfilter_inout_alloc();

    app->filter_graph =
        avfilter_graph_alloc();


    if (!outputs ||
        !inputs ||
        !app->filter_graph) {

        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);

        return AVERROR(ENOMEM);
    }


    snprintf(
        args,
        sizeof(args),
        "video_size=%dx%d:"
        "pix_fmt=%d:"
        "time_base=%d/%d:"
        "pixel_aspect=%d/%d",

        app->dec_ctx->width,
        app->dec_ctx->height,
        app->dec_ctx->pix_fmt,

        time_base.num,
        time_base.den,

        app->dec_ctx->sample_aspect_ratio.num,

        app->dec_ctx->sample_aspect_ratio.den
            ? app->dec_ctx->sample_aspect_ratio.den
            : 1
    );


    ret = avfilter_graph_create_filter(
        &app->buffersrc_ctx,
        buffersrc,
        "in",
        args,
        NULL,
        app->filter_graph
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot create buffer source\n"
        );

        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);

        return ret;
    }


    ret = avfilter_graph_create_filter(
        &app->buffersink_ctx,
        buffersink,
        "out",
        NULL,
        NULL,
        app->filter_graph
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot create buffer sink\n"
        );

        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);

        return ret;
    }


    ret = build_filter_chain(
        app,
        filter_descr,
        full_filter_descr,
        sizeof(full_filter_descr)
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot build filter chain\n"
        );

        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);

        return ret;
    }


    outputs->name =
        av_strdup("in");

    outputs->filter_ctx =
        app->buffersrc_ctx;

    outputs->pad_idx = 0;
    outputs->next = NULL;


    inputs->name =
        av_strdup("out");

    inputs->filter_ctx =
        app->buffersink_ctx;

    inputs->pad_idx = 0;
    inputs->next = NULL;


    if (!outputs->name ||
        !inputs->name) {

        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);

        return AVERROR(ENOMEM);
    }


    ret = avfilter_graph_parse_ptr(
        app->filter_graph,
        full_filter_descr,
        &inputs,
        &outputs,
        NULL
    );

    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);


    if (ret < 0)
        return ret;


    ret = avfilter_graph_config(
        app->filter_graph,
        NULL
    );

    if (ret < 0)
        return ret;

    return 0;
}


/*
 * Open output file.
 *
 * Video is encoded.
 * Audio is stream-copied.
 */
static int open_output(
    AppContext *app,
    const char *filename
) {
    int ret;

    ret = avformat_alloc_output_context2(
        &app->out_fmt_ctx,
        NULL,
        NULL,
        filename
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Cannot create output context\n"
        );

        return ret;
    }


    /*
     * VIDEO OUTPUT
     */
    if (!app->remove_video) {

        AVStream *out_video =
            avformat_new_stream(
                app->out_fmt_ctx,
                NULL
            );

        if (!out_video)
            return AVERROR(ENOMEM);

        app->out_video_stream_idx =
            out_video->index;


        const AVCodec *encoder =
            app->encoder;


        app->enc_ctx =
            avcodec_alloc_context3(
                encoder
            );

        if (!app->enc_ctx)
            return AVERROR(ENOMEM);


        app->enc_ctx->height =
            av_buffersink_get_h(
                app->buffersink_ctx
            );

        app->enc_ctx->width =
            av_buffersink_get_w(
                app->buffersink_ctx
            );

        app->enc_ctx->sample_aspect_ratio =
            av_buffersink_get_sample_aspect_ratio(
                app->buffersink_ctx
            );

        app->enc_ctx->pix_fmt =
            av_buffersink_get_format(
                app->buffersink_ctx
            );

        app->enc_ctx->time_base =
            av_buffersink_get_time_base(
                app->buffersink_ctx
            );

        app->enc_ctx->framerate =
            av_buffersink_get_frame_rate(
                app->buffersink_ctx
            );

        app->enc_ctx->bit_rate =
            2 * 1000 * 1000;

        app->enc_ctx->gop_size = 12;


        if (app->out_fmt_ctx->oformat->flags &
            AVFMT_GLOBALHEADER) {

            app->enc_ctx->flags |=
                AV_CODEC_FLAG_GLOBAL_HEADER;
        }


        ret = avcodec_open2(
            app->enc_ctx,
            encoder,
            NULL
        );

        if (ret < 0) {
            fprintf(
                stderr,
                "Cannot open video encoder\n"
            );

            return ret;
        }


        ret = avcodec_parameters_from_context(
            out_video->codecpar,
            app->enc_ctx
        );

        if (ret < 0)
            return ret;


        out_video->time_base =
            app->enc_ctx->time_base;
    }


    /*
     * AUDIO OUTPUT
     *
     * No decoding.
     * No encoding.
     * Packets are copied directly.
     */
    if (!app->remove_audio &&
        app->audio_stream_idx >= 0) {

        AVStream *in_audio =
            app->in_fmt_ctx->streams[
                app->audio_stream_idx
            ];

        AVStream *out_audio =
            avformat_new_stream(
                app->out_fmt_ctx,
                NULL
            );

        if (!out_audio)
            return AVERROR(ENOMEM);


        app->out_audio_stream_idx =
            out_audio->index;


        ret = avcodec_parameters_copy(
            out_audio->codecpar,
            in_audio->codecpar
        );

        if (ret < 0)
            return ret;


        /*
         * Let the muxer decide the correct codec tag.
         */
        out_audio->codecpar->codec_tag = 0;

        out_audio->time_base =
            in_audio->time_base;
    }


    /*
     * Open output file.
     */
    if (!(app->out_fmt_ctx->oformat->flags &
          AVFMT_NOFILE)) {

        ret = avio_open(
            &app->out_fmt_ctx->pb,
            filename,
            AVIO_FLAG_WRITE
        );

        if (ret < 0) {
            fprintf(
                stderr,
                "Could not open output file '%s'\n",
                filename
            );

            return ret;
        }
    }


    ret = avformat_write_header(
        app->out_fmt_ctx,
        NULL
    );

    if (ret < 0) {
        fprintf(
            stderr,
            "Error writing header\n"
        );

        return ret;
    }

    return 0;
}


/*
 * Check whether video frame is inside trim range.
 */
static int frame_in_trim_range(
    const AppContext *app,
    const AVFrame *frame
) {
    if (frame->pts == AV_NOPTS_VALUE)
        return 1;

    const AVStream *stream =
        app->in_fmt_ctx->streams[
            app->video_stream_idx
        ];

    const int64_t pts_us =
        av_rescale_q(
            frame->pts,
            stream->time_base,
            AV_TIME_BASE_Q
        );


    if (app->trim_start_us > 0 &&
        pts_us < app->trim_start_us) {

        return 0;
    }


    if (app->trim_end_us > 0 &&
        pts_us > app->trim_end_us) {

        return 0;
    }


    return 1;
}


/*
 * Offset video PTS after trimming.
 */
static void offset_frame_pts(
    const AppContext *app,
    AVFrame *frame
) {
    if (app->trim_start_us <= 0 ||
        frame->pts == AV_NOPTS_VALUE) {

        return;
    }


    const AVStream *stream =
        app->in_fmt_ctx->streams[
            app->video_stream_idx
        ];


    const int64_t start_pts =
        av_rescale_q(
            app->trim_start_us,
            AV_TIME_BASE_Q,
            stream->time_base
        );


    frame->pts -= start_pts;


    if (frame->pts < 0)
        frame->pts = 0;
}


/*
 * Write packets produced by encoder.
 */
static int drain_encoder(
    AppContext *app,
    AVPacket *pkt
) {
    int ret = 0;


    while (1) {

        ret = avcodec_receive_packet(
            app->enc_ctx,
            pkt
        );


        if (ret == AVERROR(EAGAIN) ||
            ret == AVERROR_EOF) {

            return 0;
        }


        if (ret < 0)
            return ret;


        pkt->stream_index =
            app->out_video_stream_idx;


        av_packet_rescale_ts(
            pkt,
            app->enc_ctx->time_base,
            app->out_fmt_ctx
                ->streams[
                    app->out_video_stream_idx
                ]
                ->time_base
        );


        ret = av_interleaved_write_frame(
            app->out_fmt_ctx,
            pkt
        );


        av_packet_unref(pkt);


        if (ret < 0)
            return ret;
    }
}


/*
 * Encode one video frame.
 */
static int encode_and_write(
    AppContext *app,
    AVFrame *frame
) {
    AVPacket *pkt =
        av_packet_alloc();

    if (!pkt)
        return AVERROR(ENOMEM);


    int ret =
        avcodec_send_frame(
            app->enc_ctx,
            frame
        );


    if (ret < 0 &&
        ret != AVERROR(EAGAIN) &&
        ret != AVERROR_EOF) {

        av_packet_free(&pkt);
        return ret;
    }


    ret = drain_encoder(
        app,
        pkt
    );


    av_packet_free(&pkt);

    return ret;
}


/*
 * Filter + encode one video frame.
 */
static int filter_encode_frame(
    AppContext *app,
    AVFrame *frame
) {
    int ret;

    AVFrame *filt_frame =
        av_frame_alloc();

    if (!filt_frame)
        return AVERROR(ENOMEM);


    ret = av_buffersrc_add_frame_flags(
        app->buffersrc_ctx,
        frame,
        frame
            ? AV_BUFFERSRC_FLAG_KEEP_REF
            : 0
    );

    if (ret < 0) {
        av_frame_free(&filt_frame);
        return ret;
    }


    while (1) {

        ret =
            av_buffersink_get_frame(
                app->buffersink_ctx,
                filt_frame
            );


        if (ret == AVERROR(EAGAIN) ||
            ret == AVERROR_EOF) {

            ret = 0;
            break;
        }


        if (ret < 0)
            break;


        filt_frame->pict_type =
            AV_PICTURE_TYPE_NONE;


        ret = encode_and_write(
            app,
            filt_frame
        );


        av_frame_unref(
            filt_frame
        );


        if (ret < 0)
            break;
    }


    av_frame_free(&filt_frame);

    return ret;
}


/*
 * Decode video packet.
 */
static int decode_packet(
    AppContext *app,
    AVPacket *pkt,
    AVFrame *frame
) {
    int ret =
        avcodec_send_packet(
            app->dec_ctx,
            pkt
        );


    if (ret < 0)
        return ret;


    while (1) {

        ret =
            avcodec_receive_frame(
                app->dec_ctx,
                frame
            );


        if (ret == AVERROR(EAGAIN) ||
            ret == AVERROR_EOF) {

            return 0;
        }


        if (ret < 0)
            return ret;


        frame->pts =
            frame->best_effort_timestamp;


        if (!frame_in_trim_range(
                app,
                frame
            )) {

            av_frame_unref(frame);
            continue;
        }


        offset_frame_pts(
            app,
            frame
        );


        ret = filter_encode_frame(
            app,
            frame
        );


        av_frame_unref(frame);


        if (ret < 0)
            return ret;
    }
}


/*
 * Copy one audio packet.
 *
 * Audio is NOT decoded/re-encoded.
 */
static int copy_audio_packet(
    AppContext *app,
    AVPacket *pkt
) {
    AVStream *in_stream =
        app->in_fmt_ctx->streams[
            app->audio_stream_idx
        ];

    AVStream *out_stream =
        app->out_fmt_ctx->streams[
            app->out_audio_stream_idx
        ];


    /*
     * Check packet timestamp against trim range.
     */
    if (pkt->pts != AV_NOPTS_VALUE) {

        const int64_t pts_us =
            av_rescale_q(
                pkt->pts,
                in_stream->time_base,
                AV_TIME_BASE_Q
            );


        if (app->trim_start_us > 0 &&
            pts_us < app->trim_start_us) {

            return 0;
        }


        if (app->trim_end_us > 0 &&
            pts_us > app->trim_end_us) {

            return AVERROR_EOF;
        }
    }


    /*
     * Shift timestamps so the output starts at zero.
     */
    if (app->trim_start_us > 0) {

        const int64_t start_pts =
            av_rescale_q(
                app->trim_start_us,
                AV_TIME_BASE_Q,
                in_stream->time_base
            );


        if (pkt->pts != AV_NOPTS_VALUE)
            pkt->pts -= start_pts;

        if (pkt->dts != AV_NOPTS_VALUE)
            pkt->dts -= start_pts;
    }


    pkt->stream_index =
        app->out_audio_stream_idx;


    av_packet_rescale_ts(
        pkt,
        in_stream->time_base,
        out_stream->time_base
    );


    return av_interleaved_write_frame(
        app->out_fmt_ctx,
        pkt
    );
}


/*
 * Main processing function.
 */
static int run(
    AppContext *app,
    const char *in_filename,
    const char *out_filename,
    const char *filter_descr,
    long start_ms,
    long end_ms,
    unsigned char remove_audio,
    unsigned char remove_video
) {
    int ret;


    app->remove_audio =
        remove_audio != 0;

    app->remove_video =
        remove_video != 0;


    /*
     * Can't remove both.
     */
    if (app->remove_audio &&
        app->remove_video) {

        fprintf(
            stderr,
            "Cannot remove both audio and video\n"
        );

        return AVERROR(EINVAL);
    }


    /*
     * Open input.
     */
    ret = open_input(
        app,
        in_filename
    );

    if (ret < 0)
        return ret;


    /*
     * Validate requested streams.
     */
    if (!app->remove_video &&
        app->video_stream_idx < 0) {

        fprintf(
            stderr,
            "Video requested but input has no video\n"
        );

        return AVERROR_STREAM_NOT_FOUND;
    }


    if (!app->remove_audio &&
        app->audio_stream_idx < 0) {

        /*
         * This is not necessarily an error.
         *
         * Example:
         * video-only input + removeAudio=false.
         *
         * We simply produce video.
         */
        fprintf(
            stderr,
            "Input has no audio stream; continuing without audio\n"
        );
    }


    /*
     * Convert milliseconds to microseconds.
     */
    app->trim_start_us =
        start_ms > 0
            ? av_rescale(
                start_ms,
                AV_TIME_BASE,
                1000
            )
            : 0;


    app->trim_end_us =
        end_ms > 0
            ? av_rescale(
                end_ms,
                AV_TIME_BASE,
                1000
            )
            : 0;


    if (app->trim_end_us > 0 &&
        app->trim_end_us <=
            app->trim_start_us) {

        fprintf(
            stderr,
            "Invalid trim range: "
            "end (%ld ms) must be greater "
            "than start (%ld ms)\n",
            end_ms,
            start_ms
        );

        return AVERROR(EINVAL);
    }


    /*
     * Video processing setup.
     */
    if (!app->remove_video) {

        ret = choose_encoder(app);

        if (ret < 0)
            return ret;


        ret = init_filters(
            app,
            filter_descr
        );

        if (ret < 0)
            return ret;
    }


    /*
     * Create output.
     */
    ret = open_output(
        app,
        out_filename
    );

    if (ret < 0)
        return ret;


    /*
     * Seek only when processing video.
     *
     * For audio-only output we should not seek to a video keyframe,
     * because we are copying audio packets directly.
     */
    if (!app->remove_video &&
        app->trim_start_us > 0) {

        ret = avformat_seek_file(
            app->in_fmt_ctx,
            -1,
            INT64_MIN,
            app->trim_start_us,
            app->trim_start_us,
            0
        );


        if (ret < 0) {
            fprintf(
                stderr,
                "Cannot seek to trim start\n"
            );

            return ret;
        }


        avcodec_flush_buffers(
            app->dec_ctx
        );
    }


    AVPacket *pkt =
        av_packet_alloc();

    AVFrame *frame =
        av_frame_alloc();


    if (!pkt || !frame) {

        av_packet_free(&pkt);
        av_frame_free(&frame);

        return AVERROR(ENOMEM);
    }


    /*
     * Read packets.
     */
    while (1) {

        ret =
            av_read_frame(
                app->in_fmt_ctx,
                pkt
            );


        if (ret < 0)
            break;


        report_progress(
            app,
            pkt
        );


        /*
         * VIDEO
         */
        if (pkt->stream_index ==
            app->video_stream_idx) {

            if (app->remove_video) {

                av_packet_unref(pkt);
                continue;
            }


            /*
             * Stop when video reaches trim end.
             */
            if (app->trim_end_us > 0 &&
                pkt->pts != AV_NOPTS_VALUE) {

                const AVStream *stream =
                    app->in_fmt_ctx->streams[
                        pkt->stream_index
                    ];


                const int64_t pts_us =
                    av_rescale_q(
                        pkt->pts,
                        stream->time_base,
                        AV_TIME_BASE_Q
                    );


                if (pts_us >
                    app->trim_end_us) {

                    av_packet_unref(pkt);

                    ret = AVERROR_EOF;

                    break;
                }
            }


            ret = decode_packet(
                app,
                pkt,
                frame
            );


            av_packet_unref(pkt);


            if (ret < 0)
                break;


            continue;
        }


        /*
         * AUDIO
         */
        if (pkt->stream_index ==
            app->audio_stream_idx) {

            if (app->remove_audio ||
                app->audio_stream_idx < 0) {

                av_packet_unref(pkt);
                continue;
            }


            ret = copy_audio_packet(
                app,
                pkt
            );


            av_packet_unref(pkt);


            if (ret == AVERROR_EOF) {

                ret = 0;
                break;
            }


            if (ret < 0)
                break;


            continue;
        }


        /*
         * Ignore other streams.
         */
        av_packet_unref(pkt);
    }


    /*
     * Flush video decoder.
     */
    if (!app->remove_video &&
        (ret >= 0 ||
         ret == AVERROR_EOF)) {

        ret = decode_packet(
            app,
            NULL,
            frame
        );
    }


    /*
     * Flush filter graph.
     */
    if (!app->remove_video &&
        ret >= 0) {

        ret = filter_encode_frame(
            app,
            NULL
        );
    }


    /*
     * Flush encoder.
     */
    if (!app->remove_video &&
        ret >= 0) {

        ret = encode_and_write(
            app,
            NULL
        );
    }


    /*
     * Write trailer.
     */
    if (ret >= 0 ||
        ret == AVERROR_EOF) {

        av_write_trailer(
            app->out_fmt_ctx
        );
    }


    av_frame_free(&frame);
    av_packet_free(&pkt);


    return ret == AVERROR_EOF
        ? 0
        : ret;
}


/*
 * Public API.
 */
int apply_video_filter(
    void *env,
    const char *in_filename,
    const char *out_filename,
    const char *filter_descr,
    long start,
    long end,
    unsigned char removeAudio,
    unsigned char removeVideo,
    const ProgressCallback progress_callback
) {
    AppContext app;

    app_context_init(&app);

    app.progress_cb = progress_callback;
    app.progress_user_data = env;

    const int ret =
        run(
            &app,
            in_filename,
            out_filename,
            filter_descr,
            start,
            end,
            removeAudio,
            removeVideo
        );


    if (ret < 0) {
        char errbuf[128];
        av_strerror(ret, errbuf, sizeof(errbuf));
        fprintf(stderr, "apply_video_filter failed: %s\n", errbuf);
    }

    app_context_cleanup(&app);

    return ret;
}