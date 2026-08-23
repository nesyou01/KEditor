#include <jni.h>
#include <android/log.h>

#include <cstdio>
#include <cstring>

extern "C" {

#include <libavutil/avutil.h>
#include <libavutil/error.h>
#include <libavutil/frame.h>
#include <libavutil/imgutils.h>
#include <libavutil/pixdesc.h>
#include <libavutil/rational.h>

#include <libavcodec/avcodec.h>

#include <libavformat/avformat.h>

#include <libavfilter/avfilter.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
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
Java_dev_nesyou_keditor_ffmpeg_VideoEditor_testCrop(
        JNIEnv* env,
        jobject /* thiz */,
        jstring inputPath_,
        jstring outputPath_)
{
    LOGE("Input or output path is null");


    if (!inputPath_ || !outputPath_) {
        LOGE("Input or output path is null");
        return AVERROR(EINVAL);
    }

    const char* inputPath =
            env->GetStringUTFChars(inputPath_, nullptr);

    const char* outputPath =
            env->GetStringUTFChars(outputPath_, nullptr);

    AVFormatContext* inputFormatCtx = nullptr;
    AVFormatContext* outputFormatCtx = nullptr;

    AVCodecContext* decoderCtx = nullptr;
    AVCodecContext* encoderCtx = nullptr;

    AVStream* inputStream = nullptr;
    AVStream* outputStream = nullptr;

    AVFilterGraph* filterGraph = nullptr;
    AVFilterContext* bufferSrc = nullptr;
    AVFilterContext* cropCtx = nullptr;
    AVFilterContext* bufferSink = nullptr;

    AVPacket* inputPacket = nullptr;
    AVPacket* outputPacket = nullptr;

    AVFrame* decodedFrame = nullptr;
    AVFrame* filteredFrame = nullptr;

    int ret = 0;

    int videoStreamIndex = -1;

    // ---------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------

    auto cleanup = [&]() {

        av_packet_free(&inputPacket);
        av_packet_free(&outputPacket);

        av_frame_free(&decodedFrame);
        av_frame_free(&filteredFrame);

        avfilter_graph_free(&filterGraph);

        avcodec_free_context(&decoderCtx);
        avcodec_free_context(&encoderCtx);

        if (inputFormatCtx) {
            avformat_close_input(&inputFormatCtx);
        }

        if (outputFormatCtx) {

            if (outputFormatCtx->pb) {
                avio_closep(&outputFormatCtx->pb);
            }

            avformat_free_context(outputFormatCtx);
            outputFormatCtx = nullptr;
        }

        if (inputPath_) {
            env->ReleaseStringUTFChars(
                    inputPath_,
                    inputPath
            );
        }

        if (outputPath_) {
            env->ReleaseStringUTFChars(
                    outputPath_,
                    outputPath
            );
        }
    };

    // ---------------------------------------------------------
    // Open input
    // ---------------------------------------------------------

    ret = avformat_open_input(
            &inputFormatCtx,
            inputPath,
            nullptr,
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to open input",
                ret
        );

        cleanup();
        return ret;
    }

    ret = avformat_find_stream_info(
            inputFormatCtx,
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to find stream information",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Find video stream
    // ---------------------------------------------------------

    for (unsigned int i = 0;
         i < inputFormatCtx->nb_streams;
         i++) {

        if (inputFormatCtx->streams[i]->codecpar->codec_type
                == AVMEDIA_TYPE_VIDEO) {

            videoStreamIndex = static_cast<int>(i);
            break;
        }
    }

    if (videoStreamIndex < 0) {
        LOGE("No video stream found");

        cleanup();
        return AVERROR_STREAM_NOT_FOUND;
    }

    inputStream =
            inputFormatCtx->streams[videoStreamIndex];

    // ---------------------------------------------------------
    // Decoder
    // ---------------------------------------------------------

    const AVCodec* decoder =
            avcodec_find_decoder(
                    inputStream->codecpar->codec_id
            );

    if (!decoder) {
        LOGE("Decoder not found");

        cleanup();
        return AVERROR_DECODER_NOT_FOUND;
    }

    decoderCtx =
            avcodec_alloc_context3(decoder);

    if (!decoderCtx) {
        cleanup();
        return AVERROR(ENOMEM);
    }

    ret = avcodec_parameters_to_context(
            decoderCtx,
            inputStream->codecpar
    );

    if (ret < 0) {
        logError(
                "Failed to copy decoder parameters",
                ret
        );

        cleanup();
        return ret;
    }

    ret = avcodec_open2(
            decoderCtx,
            decoder,
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to open decoder",
                ret
        );

        cleanup();
        return ret;
    }

    LOGI(
            "Input video: %dx%d",
            decoderCtx->width,
            decoderCtx->height
    );

    // ---------------------------------------------------------
    // Allocate filter graph
    // ---------------------------------------------------------

    filterGraph =
            avfilter_graph_alloc();

    if (!filterGraph) {
        cleanup();
        return AVERROR(ENOMEM);
    }

    // ---------------------------------------------------------
    // Buffer source
    // ---------------------------------------------------------

    const AVFilter* buffer =
            avfilter_get_by_name("buffer");

    if (!buffer) {
        LOGE("buffer filter not found");

        cleanup();
        return AVERROR_FILTER_NOT_FOUND;
    }

    char bufferArgs[512];

    AVRational timeBase = inputStream->time_base;

    if (timeBase.num <= 0 || timeBase.den <= 0) {
        timeBase = AVRational{1, 30};
    }

    AVRational pixelAspect =
            decoderCtx->sample_aspect_ratio;

    if (pixelAspect.num == 0 ||
            pixelAspect.den == 0) {

        pixelAspect =
                AVRational{1, 1};
    }


    snprintf(
            bufferArgs,
            sizeof(bufferArgs),
            "video_size=%dx%d:"
            "pix_fmt=%d:"
            "time_base=%d/%d:"
            "pixel_aspect=1/1",
            decoderCtx->width,
            decoderCtx->height,
            decoderCtx->pix_fmt,
            timeBase.num,
            timeBase.den
    );

    LOGI("Buffer args: %s", bufferArgs);

    ret = avfilter_graph_create_filter(
            &bufferSrc,
            buffer,
            "in",
            bufferArgs,
            nullptr,
            filterGraph
    );

    if (ret < 0) {
        logError(
                "Failed to create buffer source",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Crop
    // ---------------------------------------------------------

    const AVFilter* crop =
            avfilter_get_by_name("crop");

    if (!crop) {
        LOGE("crop filter not found");

        cleanup();
        return AVERROR_FILTER_NOT_FOUND;
    }

    /*
     * Crop:
     *
     * width  = 640
     * height = 640
     * x      = 100
     * y      = 50
     */

    ret = avfilter_graph_create_filter(
            &cropCtx,
            crop,
            "crop",
            "w=640:h=640:x=100:y=50",
            nullptr,
            filterGraph
    );

    if (ret < 0) {
        logError(
                "Failed to create crop filter",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Buffer sink
    // ---------------------------------------------------------

    const AVFilter* sink =
            avfilter_get_by_name("buffersink");

    if (!sink) {
        LOGE("buffersink filter not found");

        cleanup();
        return AVERROR_FILTER_NOT_FOUND;
    }

    ret = avfilter_graph_create_filter(
            &bufferSink,
            sink,
            "out",
            nullptr,
            nullptr,
            filterGraph
    );

    if (ret < 0) {
        logError(
                "Failed to create buffer sink",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Link filters
    // ---------------------------------------------------------

    ret = avfilter_link(
            bufferSrc,
            0,
            cropCtx,
            0
    );

    if (ret < 0) {
        logError(
                "Failed to link buffer -> crop",
                ret
        );

        cleanup();
        return ret;
    }

    ret = avfilter_link(
            cropCtx,
            0,
            bufferSink,
            0
    );

    if (ret < 0) {
        logError(
                "Failed to link crop -> sink",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Configure graph
    // ---------------------------------------------------------

    ret = avfilter_graph_config(
            filterGraph,
            nullptr
    );

    ret = avfilter_graph_config(
            filterGraph,
            nullptr
    );

    if (ret < 0) {
        logError("Failed to configure filter graph", ret);

        char dump[4096];
        avfilter_graph_dump(filterGraph, nullptr);

        LOGE(
                "Filter graph args: %s",
                bufferArgs
        );

        cleanup();
        return ret;
    }

    LOGI("Crop filter graph configured");

    // ---------------------------------------------------------
    // Create output
    // ---------------------------------------------------------

    ret = avformat_alloc_output_context2(
            &outputFormatCtx,
            nullptr,
            "mp4",
            outputPath
    );

    if (ret < 0 || !outputFormatCtx) {
        logError(
                "Failed to create output context",
                ret
        );

        cleanup();
        return ret < 0 ? ret : AVERROR_UNKNOWN;
    }

    // ---------------------------------------------------------
    // Encoder
    // ---------------------------------------------------------

    const AVCodec* encoder =
            avcodec_find_encoder(
                    AV_CODEC_ID_MPEG4
            );

    if (!encoder) {
        LOGE("MPEG4 encoder not found");

        cleanup();
        return AVERROR_ENCODER_NOT_FOUND;
    }

    outputStream =
            avformat_new_stream(
                    outputFormatCtx,
                    encoder
            );

    if (!outputStream) {
        cleanup();
        return AVERROR(ENOMEM);
    }

    encoderCtx =
            avcodec_alloc_context3(encoder);

    if (!encoderCtx) {
        cleanup();
        return AVERROR(ENOMEM);
    }

    encoderCtx->codec_id =
            AV_CODEC_ID_MPEG4;

    encoderCtx->codec_type =
            AVMEDIA_TYPE_VIDEO;

    encoderCtx->width = 640;
    encoderCtx->height = 640;

    encoderCtx->pix_fmt =
            AV_PIX_FMT_YUV420P;

    /*
     * Use the original frame rate.
     */

    AVRational frameRate =
            av_guess_frame_rate(
                    inputFormatCtx,
                    inputStream,
                    nullptr
            );

    if (frameRate.num <= 0 ||
            frameRate.den <= 0) {

        frameRate =
                AVRational{30, 1};
    }

    encoderCtx->time_base =
            av_inv_q(frameRate);

    encoderCtx->framerate =
            frameRate;

    encoderCtx->bit_rate =
            4 * 1000 * 1000;

    encoderCtx->gop_size = 30;

    encoderCtx->max_b_frames = 0;

    if (outputFormatCtx->oformat->flags &
            AVFMT_GLOBALHEADER) {

        encoderCtx->flags |=
                AV_CODEC_FLAG_GLOBAL_HEADER;
    }

    ret = avcodec_open2(
            encoderCtx,
            encoder,
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to open encoder",
                ret
        );

        cleanup();
        return ret;
    }

    ret = avcodec_parameters_from_context(
            outputStream->codecpar,
            encoderCtx
    );

    if (ret < 0) {
        logError(
                "Failed to copy encoder parameters",
                ret
        );

        cleanup();
        return ret;
    }

    outputStream->time_base =
            encoderCtx->time_base;

    // ---------------------------------------------------------
    // Open output file
    // ---------------------------------------------------------

    if (!(outputFormatCtx->oformat->flags &
            AVFMT_NOFILE)) {

        ret = avio_open(
                &outputFormatCtx->pb,
                outputPath,
                AVIO_FLAG_WRITE
        );

        if (ret < 0) {
            logError(
                    "Failed to open output file",
                    ret
            );

            cleanup();
            return ret;
        }
    }

    // ---------------------------------------------------------
    // Write MP4 header
    // ---------------------------------------------------------

    ret = avformat_write_header(
            outputFormatCtx,
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to write output header",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Allocate packets/frames
    // ---------------------------------------------------------

    inputPacket =
            av_packet_alloc();

    outputPacket =
            av_packet_alloc();

    decodedFrame =
            av_frame_alloc();

    filteredFrame =
            av_frame_alloc();

    if (!inputPacket ||
            !outputPacket ||
            !decodedFrame ||
            !filteredFrame) {

        cleanup();
        return AVERROR(ENOMEM);
    }

    // ---------------------------------------------------------
    // Encode helper
    // ---------------------------------------------------------

    auto encodeFrame =
            [&](AVFrame* frame) -> int {

                int result =
                        avcodec_send_frame(
                                encoderCtx,
                                frame
                        );

                if (result < 0) {
                    return result;
                }

                while (true) {

                    result =
                            avcodec_receive_packet(
                                    encoderCtx,
                                    outputPacket
                            );

                    if (result == AVERROR(EAGAIN) ||
                            result == AVERROR_EOF) {

                        return 0;
                    }

                    if (result < 0) {
                        return result;
                    }

                    av_packet_rescale_ts(
                            outputPacket,
                            encoderCtx->time_base,
                            outputStream->time_base
                    );

                    outputPacket->stream_index =
                            outputStream->index;

                    result =
                            av_interleaved_write_frame(
                                    outputFormatCtx,
                                    outputPacket
                            );

                    av_packet_unref(outputPacket);

                    if (result < 0) {
                        return result;
                    }
                }
            };

    // ---------------------------------------------------------
    // Decode -> Crop -> Encode
    // ---------------------------------------------------------

    while (true) {

        ret = av_read_frame(
                inputFormatCtx,
                inputPacket
        );

        if (ret == AVERROR_EOF) {
            break;
        }

        if (ret < 0) {
            logError(
                    "Failed to read input packet",
                    ret
            );

            cleanup();
            return ret;
        }

        if (inputPacket->stream_index !=
                videoStreamIndex) {

            av_packet_unref(inputPacket);
            continue;
        }

        ret = avcodec_send_packet(
                decoderCtx,
                inputPacket
        );

        av_packet_unref(inputPacket);

        if (ret < 0) {
            logError(
                    "Failed to send packet to decoder",
                    ret
            );

            cleanup();
            return ret;
        }

        while (true) {

            ret = avcodec_receive_frame(
                    decoderCtx,
                    decodedFrame
            );

            if (ret == AVERROR(EAGAIN) ||
                    ret == AVERROR_EOF) {

                break;
            }

            if (ret < 0) {
                logError(
                        "Failed to decode frame",
                        ret
                );

                cleanup();
                return ret;
            }

            // -------------------------------------------------
            // Push decoded frame into crop filter
            // -------------------------------------------------

            ret = av_buffersrc_add_frame_flags(
                    bufferSrc,
                    decodedFrame,
                    AV_BUFFERSRC_FLAG_KEEP_REF
            );

            if (ret < 0) {
                logError(
                        "Failed to push frame into filter",
                        ret
                );

                cleanup();
                return ret;
            }

            // -------------------------------------------------
            // Receive all cropped frames
            // -------------------------------------------------

            while (true) {

                ret = av_buffersink_get_frame(
                        bufferSink,
                        filteredFrame
                );

                if (ret == AVERROR(EAGAIN) ||
                        ret == AVERROR_EOF) {

                    break;
                }

                if (ret < 0) {
                    logError(
                            "Failed to receive filtered frame",
                            ret
                    );

                    cleanup();
                    return ret;
                }

                /*
                 * Convert PTS from filter time base
                 * to encoder time base.
                 */

                filteredFrame->pts =
                        av_rescale_q(
                                filteredFrame->pts,
                                bufferSink->inputs[0]->time_base,
                                encoderCtx->time_base
                        );

                ret = encodeFrame(
                        filteredFrame
                );

                av_frame_unref(
                        filteredFrame
                );

                if (ret < 0) {
                    logError(
                            "Failed to encode frame",
                            ret
                    );

                    cleanup();
                    return ret;
                }
            }

            av_frame_unref(
                    decodedFrame
            );
        }
    }

    // ---------------------------------------------------------
    // Flush decoder
    // ---------------------------------------------------------

    ret = avcodec_send_packet(
            decoderCtx,
            nullptr
    );

    if (ret < 0 && ret != AVERROR_EOF) {
        logError(
                "Failed to flush decoder",
                ret
        );

        cleanup();
        return ret;
    }

    while (true) {

        ret = avcodec_receive_frame(
                decoderCtx,
                decodedFrame
        );

        if (ret == AVERROR_EOF ||
                ret == AVERROR(EAGAIN)) {

            break;
        }

        if (ret < 0) {
            logError(
                    "Failed during decoder flush",
                    ret
            );

            cleanup();
            return ret;
        }

        ret = av_buffersrc_add_frame_flags(
                bufferSrc,
                decodedFrame,
                AV_BUFFERSRC_FLAG_KEEP_REF
        );

        av_frame_unref(
                decodedFrame
        );

        if (ret < 0) {
            cleanup();
            return ret;
        }

        while (true) {

            ret = av_buffersink_get_frame(
                    bufferSink,
                    filteredFrame
            );

            if (ret == AVERROR(EAGAIN) ||
                    ret == AVERROR_EOF) {

                break;
            }

            if (ret < 0) {
                cleanup();
                return ret;
            }

            filteredFrame->pts =
                    av_rescale_q(
                            filteredFrame->pts,
                            bufferSink->inputs[0]->time_base,
                            encoderCtx->time_base
                    );

            ret = encodeFrame(
                    filteredFrame
            );

            av_frame_unref(
                    filteredFrame
            );

            if (ret < 0) {
                cleanup();
                return ret;
            }
        }
    }

    // ---------------------------------------------------------
    // Flush filter
    // ---------------------------------------------------------

    ret = av_buffersrc_add_frame_flags(
            bufferSrc,
            nullptr,
            0
    );

    if (ret < 0 && ret != AVERROR_EOF) {
        logError(
                "Failed to flush filter",
                ret
        );

        cleanup();
        return ret;
    }

    while (true) {

        ret = av_buffersink_get_frame(
                bufferSink,
                filteredFrame
        );

        if (ret == AVERROR(EAGAIN) ||
                ret == AVERROR_EOF) {

            break;
        }

        if (ret < 0) {
            cleanup();
            return ret;
        }

        filteredFrame->pts =
                av_rescale_q(
                        filteredFrame->pts,
                        bufferSink->inputs[0]->time_base,
                        encoderCtx->time_base
                );

        ret = encodeFrame(
                filteredFrame
        );

        av_frame_unref(
                filteredFrame
        );

        if (ret < 0) {
            cleanup();
            return ret;
        }
    }

    // ---------------------------------------------------------
    // Flush encoder
    // ---------------------------------------------------------

    ret = encodeFrame(
            nullptr
    );

    if (ret < 0) {
        logError(
                "Failed to flush encoder",
                ret
        );

        cleanup();
        return ret;
    }

    // ---------------------------------------------------------
    // Write trailer
    // ---------------------------------------------------------

    ret = av_write_trailer(
            outputFormatCtx
    );

    if (ret < 0) {
        logError(
                "Failed to write trailer",
                ret
        );

        cleanup();
        return ret;
    }

    LOGI(
            "Crop completed successfully"
    );

    LOGI(
            "Input: %s",
            inputPath
    );

    LOGI(
            "Output: %s",
            outputPath
    );

    cleanup();

    return 0;
}