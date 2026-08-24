#include "include/keditor.h"


static void app_context_init(AppContext *app)
{
    memset(app, 0, sizeof(*app));
    app->video_stream_idx = -1;
    app->out_video_stream_idx = -1;
}

/* Single teardown path used regardless of how far setup got. Every
 * pointer is checked before freeing, so it's safe to call this after a
 * partial/failed initialization. */
static void app_context_cleanup(AppContext *app)
{
    if (app->filter_graph)
        avfilter_graph_free(&app->filter_graph);
    if (app->dec_ctx)
        avcodec_free_context(&app->dec_ctx);
    if (app->enc_ctx)
        avcodec_free_context(&app->enc_ctx);
    if (app->in_fmt_ctx)
        avformat_close_input(&app->in_fmt_ctx);
    if (app->out_fmt_ctx) {
        if (!(app->out_fmt_ctx->oformat->flags & AVFMT_NOFILE) && app->out_fmt_ctx->pb)
            avio_closep(&app->out_fmt_ctx->pb);
        avformat_free_context(app->out_fmt_ctx);
        app->out_fmt_ctx = NULL;
    }
}

/* Open the input file, find the video stream, open its decoder. */
static int open_input(AppContext *app, const char *filename)
{
    int ret = avformat_open_input(&app->in_fmt_ctx, filename, NULL, NULL);
    if (ret < 0) {
        fprintf(stderr, "Cannot open input file '%s'\n", filename);
        return ret;
    }

    ret = avformat_find_stream_info(app->in_fmt_ctx, NULL);
    if (ret < 0) {
        fprintf(stderr, "Cannot find stream information\n");
        return ret;
    }

    ret = av_find_best_stream(app->in_fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
    if (ret < 0) {
        fprintf(stderr, "Cannot find a video stream in the input\n");
        return ret;
    }
    app->video_stream_idx = ret;

    AVStream *stream = app->in_fmt_ctx->streams[app->video_stream_idx];
    const AVCodec *decoder = avcodec_find_decoder(stream->codecpar->codec_id);
    if (!decoder) {
        fprintf(stderr, "Failed to find decoder\n");
        return AVERROR_DECODER_NOT_FOUND;
    }

    app->dec_ctx = avcodec_alloc_context3(decoder);
    if (!app->dec_ctx)
        return AVERROR(ENOMEM);

    ret = avcodec_parameters_to_context(app->dec_ctx, stream->codecpar);
    if (ret < 0)
        return ret;

    app->dec_ctx->pkt_timebase = stream->time_base;

    ret = avcodec_open2(app->dec_ctx, decoder, NULL);
    if (ret < 0) {
        fprintf(stderr, "Failed to open decoder\n");
        return ret;
    }

    return 0;
}

/* Pick the encoder we'll use for output. Done up front (before the
 * filter graph is built) so init_filters() can constrain the graph's
 * output pixel format to whatever this encoder actually supports. */
static int choose_encoder(AppContext *app)
{
    app->encoder = avcodec_find_encoder(app->dec_ctx->codec_id);
    if (!app->encoder)
        app->encoder = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!app->encoder) {
        fprintf(stderr, "Necessary encoder not found\n");
        return AVERROR_ENCODER_NOT_FOUND;
    }
    return 0;
}

/* Build the final filter chain to parse: the user's filter_descr with
 * an explicit "format=..." filter appended, listing every pixel format
 * the chosen encoder supports (pipe-separated, as the "format" filter
 * expects). This is just a plain filtergraph feature -- no AVOption
 * API needed -- so it avoids av_opt_set_int_list entirely. libavfilter
 * inserts whatever conversion is needed to reach one of those formats.
 *
 * If querying the encoder's supported formats fails or returns none
 * (e.g. an encoder like rawvideo that accepts anything), the filter
 * chain is left unmodified. */
static int build_filter_chain(AppContext *app, const char *filter_descr,
                               char *out, size_t out_size)
{
    const enum AVPixelFormat *pix_fmts = NULL;
    int nb_pix_fmts = 0;
    char fmt_list[256];
    size_t used = 0;
    int ret;

    ret = avcodec_get_supported_config(NULL, app->encoder, AV_CODEC_CONFIG_PIX_FORMAT, 0,
                                        (const void **)&pix_fmts, &nb_pix_fmts);
    if (ret < 0)
        return ret;

    if (!pix_fmts || nb_pix_fmts <= 0) {
        if (snprintf(out, out_size, "%s", filter_descr) >= (int)out_size)
            return AVERROR(ENOSPC);
        return 0;
    }

    fmt_list[0] = '\0';
    for (int i = 0; i < nb_pix_fmts; i++) {
        const char *name = av_get_pix_fmt_name(pix_fmts[i]);
        int written;

        if (!name)
            continue;

        written = snprintf(fmt_list + used, sizeof(fmt_list) - used,
                            "%s%s", used ? "|" : "", name);
        if (written < 0 || used + (size_t)written >= sizeof(fmt_list))
            break; /* stop rather than overflow; formats found so far are enough */
        used += (size_t)written;
    }

    if (used == 0) {
        if (snprintf(out, out_size, "%s", filter_descr) >= (int)out_size)
            return AVERROR(ENOSPC);
        return 0;
    }

    if (snprintf(out, out_size, "%s,format=%s", filter_descr, fmt_list) >= (int)out_size)
        return AVERROR(ENOSPC);

    return 0;
}

/* Build a simple filter graph: buffer -> <filter_descr> -> buffersink */
static int init_filters(AppContext *app, const char *filter_descr)
{
    char args[512];
    char full_filter_descr[768];
    int ret;
    AVFilterInOut *outputs = NULL;
    AVFilterInOut *inputs  = NULL;
    const AVFilter *buffersrc  = avfilter_get_by_name("buffer");
    const AVFilter *buffersink = avfilter_get_by_name("buffersink");
    AVRational time_base = app->in_fmt_ctx->streams[app->video_stream_idx]->time_base;

    outputs = avfilter_inout_alloc();
    inputs  = avfilter_inout_alloc();
    app->filter_graph = avfilter_graph_alloc();

    if (!outputs || !inputs || !app->filter_graph) {
        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);
        return AVERROR(ENOMEM);
    }

    snprintf(args, sizeof(args),
             "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
             app->dec_ctx->width, app->dec_ctx->height, app->dec_ctx->pix_fmt,
             time_base.num, time_base.den,
             app->dec_ctx->sample_aspect_ratio.num,
             app->dec_ctx->sample_aspect_ratio.den ? app->dec_ctx->sample_aspect_ratio.den : 1);

    ret = avfilter_graph_create_filter(&app->buffersrc_ctx, buffersrc, "in",
                                        args, NULL, app->filter_graph);
    if (ret < 0) {
        fprintf(stderr, "Cannot create buffer source\n");
        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);
        return ret;
    }

    ret = avfilter_graph_create_filter(&app->buffersink_ctx, buffersink, "out",
                                        NULL, NULL, app->filter_graph);
    if (ret < 0) {
        fprintf(stderr, "Cannot create buffer sink\n");
        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);
        return ret;
    }

    /* Append a "format" filter listing the encoder's supported pixel
     * formats onto the end of the user's filter chain, so the encoder
     * always receives a format it can actually accept. */
    ret = build_filter_chain(app, filter_descr, full_filter_descr, sizeof(full_filter_descr));
    if (ret < 0) {
        fprintf(stderr, "Cannot build filter chain\n");
        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);
        return ret;
    }

    outputs->name       = av_strdup("in");
    outputs->filter_ctx = app->buffersrc_ctx;
    outputs->pad_idx    = 0;
    outputs->next       = NULL;

    inputs->name       = av_strdup("out");
    inputs->filter_ctx = app->buffersink_ctx;
    inputs->pad_idx    = 0;
    inputs->next       = NULL;

    if (!outputs->name || !inputs->name) {
        avfilter_inout_free(&inputs);
        avfilter_inout_free(&outputs);
        return AVERROR(ENOMEM);
    }

    ret = avfilter_graph_parse_ptr(app->filter_graph, full_filter_descr,
                                    &inputs, &outputs, NULL);
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);
    if (ret < 0)
        return ret;

    ret = avfilter_graph_config(app->filter_graph, NULL);
    if (ret < 0)
        return ret;

    return 0;
}

/* Open the output file and set up an encoder matching the filtered frames. */
static int open_output(AppContext *app, const char *filename)
{
    int ret;
    /* app->encoder was already chosen by choose_encoder() before the
     * filter graph was built -- reuse it rather than searching again,
     * so the encoder we open here is guaranteed to be the same one
     * the buffersink's pix_fmts were constrained against. */
    const AVCodec *encoder = app->encoder;

    ret = avformat_alloc_output_context2(&app->out_fmt_ctx, NULL, NULL, filename);
    if (ret < 0) {
        fprintf(stderr, "Cannot create output context\n");
        return ret;
    }

    AVStream *out_stream = avformat_new_stream(app->out_fmt_ctx, NULL);
    if (!out_stream)
        return AVERROR(ENOMEM);
    app->out_video_stream_idx = out_stream->index;

    app->enc_ctx = avcodec_alloc_context3(encoder);
    if (!app->enc_ctx)
        return AVERROR(ENOMEM);

    app->enc_ctx->height    = av_buffersink_get_h(app->buffersink_ctx);
    app->enc_ctx->width     = av_buffersink_get_w(app->buffersink_ctx);
    app->enc_ctx->sample_aspect_ratio = av_buffersink_get_sample_aspect_ratio(app->buffersink_ctx);
    app->enc_ctx->pix_fmt   = av_buffersink_get_format(app->buffersink_ctx);
    app->enc_ctx->time_base = av_buffersink_get_time_base(app->buffersink_ctx);
    app->enc_ctx->framerate = av_buffersink_get_frame_rate(app->buffersink_ctx);
    app->enc_ctx->bit_rate  = 2 * 1000 * 1000; /* 2 Mbps, adjust to taste */
    app->enc_ctx->gop_size  = 12;

    if (app->out_fmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
        app->enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

    ret = avcodec_open2(app->enc_ctx, encoder, NULL);
    if (ret < 0) {
        fprintf(stderr, "Cannot open video encoder\n");
        return ret;
    }

    ret = avcodec_parameters_from_context(out_stream->codecpar, app->enc_ctx);
    if (ret < 0)
        return ret;
    out_stream->time_base = app->enc_ctx->time_base;

    if (!(app->out_fmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&app->out_fmt_ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            fprintf(stderr, "Could not open output file '%s'\n", filename);
            return ret;
        }
    }

    ret = avformat_write_header(app->out_fmt_ctx, NULL);
    if (ret < 0) {
        fprintf(stderr, "Error writing header\n");
        return ret;
    }

    return 0;
}

/* Drain every packet currently buffered in the encoder and write it out.
 * Returns 0 on success (including the "nothing available yet" case),
 * or a negative error on real failure. */
static int drain_encoder(AppContext *app, AVPacket *pkt)
{
    int ret = 0;

    while (ret >= 0) {
        ret = avcodec_receive_packet(app->enc_ctx, pkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            ret = 0;
            break;
        }
        if (ret < 0)
            break;

        pkt->stream_index = app->out_video_stream_idx;
        av_packet_rescale_ts(pkt, app->enc_ctx->time_base,
                              app->out_fmt_ctx->streams[app->out_video_stream_idx]->time_base);
        ret = av_interleaved_write_frame(app->out_fmt_ctx, pkt);
        av_packet_unref(pkt);
    }

    return ret;
}

static int encode_and_write(AppContext *app, AVFrame *frame)
{
    AVPacket *pkt = av_packet_alloc();
    if (!pkt)
        return AVERROR(ENOMEM);

    /* avcodec_send_frame() can return EAGAIN to mean "my internal
     * buffer is full -- drain packets with receive_packet() first,
     * then retry the same send_frame() call". Loop until it actually
     * succeeds or fails for a real reason. */
    int send_ret;
    while (1) {
        send_ret = avcodec_send_frame(app->enc_ctx, frame);
        if (send_ret != AVERROR(EAGAIN))
            break;

        int drain_ret = drain_encoder(app, pkt);
        if (drain_ret < 0) {
            av_packet_free(&pkt);
            return drain_ret;
        }
    }

    if (send_ret < 0 && send_ret != AVERROR_EOF) {
        av_packet_free(&pkt);
        return send_ret;
    }

    int ret = drain_encoder(app, pkt);

    av_packet_free(&pkt);
    return ret;
}

/* Push a decoded frame through the filter graph, then encode every
 * filtered frame that comes out the other side. Pass frame == NULL to
 * flush the filter graph at end of stream. */
static int filter_encode_frame(AppContext *app, AVFrame *frame)
{
    int ret;
    AVFrame *filt_frame = av_frame_alloc();
    if (!filt_frame)
        return AVERROR(ENOMEM);

    ret = av_buffersrc_add_frame_flags(app->buffersrc_ctx, frame, AV_BUFFERSRC_FLAG_KEEP_REF);
    if (ret < 0) {
        av_frame_free(&filt_frame);
        return ret;
    }

    while (ret >= 0) {
        ret = av_buffersink_get_frame(app->buffersink_ctx, filt_frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            ret = 0;
            break;
        }
        if (ret < 0)
            break;

        filt_frame->pict_type = AV_PICTURE_TYPE_NONE;
        ret = encode_and_write(app, filt_frame);
        av_frame_unref(filt_frame);
    }

    av_frame_free(&filt_frame);
    return ret;
}

/* Decode every packet belonging to the video stream. Pass pkt == NULL to
 * flush the decoder at end of stream. */
static int decode_packet(AppContext *app, AVPacket *pkt, AVFrame *frame)
{
    int ret = avcodec_send_packet(app->dec_ctx, pkt);
    if (ret < 0)
        return ret;

    while (ret >= 0) {
        ret = avcodec_receive_frame(app->dec_ctx, frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            ret = 0;
            break;
        }
        if (ret < 0)
            return ret;

        frame->pts = frame->best_effort_timestamp;
        ret = filter_encode_frame(app, frame);
        av_frame_unref(frame);
        if (ret < 0)
            return ret;
    }

    return 0;
}

static int run(AppContext *app, const char *in_filename,
                const char *out_filename, const char *filter_descr)
{
    int ret = open_input(app, in_filename);
    if (ret < 0)
        return ret;

    ret = choose_encoder(app);
    if (ret < 0)
        return ret;

    ret = init_filters(app, filter_descr);
    if (ret < 0)
        return ret;

    ret = open_output(app, out_filename);
    if (ret < 0)
        return ret;

    AVPacket *pkt = av_packet_alloc();
    AVFrame *frame = av_frame_alloc();
    if (!pkt || !frame) {
        av_packet_free(&pkt);
        av_frame_free(&frame);
        return AVERROR(ENOMEM);
    }

    while (1) {
        ret = av_read_frame(app->in_fmt_ctx, pkt);

        if (ret < 0)
            break;

        if (pkt->stream_index == app->video_stream_idx) {
            ret = decode_packet(app, pkt, frame);

            av_packet_unref(pkt);

            if (ret < 0) {
                break;
            }
        } else {
            av_packet_unref(pkt);
        }
    }

    if (ret >= 0 || ret == AVERROR_EOF)
        ret = decode_packet(app, NULL, frame);          /* flush decoder */

    if (ret >= 0)
        ret = filter_encode_frame(app, NULL);           /* flush filter graph */

    if (ret >= 0)
        ret = encode_and_write(app, NULL);               /* flush encoder */

    if (ret >= 0 || ret == AVERROR_EOF)
        av_write_trailer(app->out_fmt_ctx);

    av_frame_free(&frame);
    av_packet_free(&pkt);

    return (ret == AVERROR_EOF) ? 0 : ret;
}

/*
 * apply_video_filter - apply an ffmpeg filter graph to a video file.
 *
 * @in_filename:    path to the input media file.
 * @out_filename:   path to the file to create/overwrite.
 * @filter_descr:   any valid ffmpeg video filter string, e.g.
 *                  "crop=640:480:100:50", "scale=1280:720", "hflip".
 *
 * Returns 0 on success, or a negative AVERROR code on failure. On
 * failure the caller can format the error with av_strerror().
 */
int apply_video_filter(const char *in_filename, const char *out_filename,
                        const char *filter_descr)
{
    AppContext app;
    app_context_init(&app);

    const int ret = run(&app, in_filename, out_filename, filter_descr);

    if (ret < 0) {
        char errbuf[128];
        av_strerror(ret, errbuf, sizeof(errbuf));
        fprintf(stderr, "apply_video_filter failed: %s\n", errbuf);
    }

    app_context_cleanup(&app);
    return ret;
}