//
// Created by Zhusong on 2021/6/2.
//

#include "muxer.h"
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <jni.h>
#include "../util/helper.h"

JNIEXPORT int JNICALL
Java_cn_com_lasong_media_Muxer_remux(JNIEnv* env,jobject object, jstring input, jstring output, jdouble start, jdouble end, jboolean metadata, jboolean rotate) {

    if (start < 0 && end <=0) {
        LOGE("timestamp is invalid");
        return -1;
    }

    if (start >= end) {
        LOGE("start timestamp must less than end");
        return -1;
    }

    if (start < 0) {
        start = 0;
    }

    const char *inputC = (*env)->GetStringUTFChars(env, input, 0);
    const char *outputC = (*env)->GetStringUTFChars(env, output, 0);

    const AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;
    const char *in_filename, *out_filename;
    int ret, i;
    int stream_index = 0;
    int *stream_mapping = NULL;
    int stream_mapping_size = 0;

    in_filename  = (*env)->GetStringUTFChars(env, input, 0);
    out_filename = (*env)->GetStringUTFChars(env, output, 0);

    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        LOGE("Could not open input file '%s'", in_filename);
        goto end;
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        LOGE("Failed to retrieve input stream information");
        goto end;
    }

//    av_dump_format(ifmt_ctx, 0, in_filename, 0);

    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        LOGE("Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        goto end;
    }

    stream_mapping_size = ifmt_ctx->nb_streams;
    stream_mapping = av_mallocz_array(stream_mapping_size, sizeof(*stream_mapping));
    if (!stream_mapping) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    ofmt = ofmt_ctx->oformat;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO &&
            in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO &&
            in_codecpar->codec_type != AVMEDIA_TYPE_SUBTITLE) {
            stream_mapping[i] = -1;
            continue;
        }

        stream_mapping[i] = stream_index++;

        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            LOGE("Failed allocating output stream\n");
            ret = AVERROR_UNKNOWN;
            goto end;
        }

        ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
        if (ret < 0) {
            LOGE("Failed to copy codec parameters\n");
            goto end;
        }
        out_stream->codecpar->codec_tag = 0;

        // 拷贝metadata
        if (metadata) {
            av_dict_copy(&out_stream->metadata, in_stream->metadata, AV_DICT_DONT_OVERWRITE);
        }
        // 保留旋转信息
        if (rotate && in_codecpar->codec_type == AVMEDIA_TYPE_VIDEO
            && in_stream->side_data != NULL) {
            int *sd_size=malloc(sizeof(int*));
            uint8_t* display_sd = av_stream_get_side_data(in_stream ,
                                                          AV_PKT_DATA_DISPLAYMATRIX, (size_t *) sd_size);
            if (display_sd != NULL) {
                in_stream->side_data->data = (uint8_t*) av_mallocz(
                        in_stream->side_data->size*sizeof(uint8_t*)
                );
                av_stream_add_side_data(out_stream, AV_PKT_DATA_DISPLAYMATRIX, display_sd, *sd_size);
            }
        }
    }
//    av_dump_format(ofmt_ctx, 0, out_filename, 1);

    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("Could not open output file '%s'", out_filename);
            goto end;
        }
    }

    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        LOGE("Error occurred when opening output file\n");
        goto end;
    }

    if (start > 0) {
        ret = av_seek_frame(ifmt_ctx, -1, start * AV_TIME_BASE, AVSEEK_FLAG_ANY);
        if (ret < 0) {
            LOGE("Error seek\n");
            goto end;
        }
    }

    // 记录跳帧的其实时间, 用于后面的帧调整时间戳
    int64_t *dts_start_from = malloc(sizeof(int64_t) * ifmt_ctx->nb_streams);
    memset(dts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);
    int64_t *pts_start_from = malloc(sizeof(int64_t) * ifmt_ctx->nb_streams);
    memset(pts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);

    while (1) {
        AVStream *in_stream, *out_stream;

        ret = av_read_frame(ifmt_ctx, &pkt);
        // 没有数据, 退出
        if (ret < 0)
            break;

        in_stream  = ifmt_ctx->streams[pkt.stream_index];
        if (pkt.stream_index >= stream_mapping_size ||
            stream_mapping[pkt.stream_index] < 0) {
            av_packet_unref(&pkt);
            continue;
        }

        if (dts_start_from[pkt.stream_index] == 0) {
            dts_start_from[pkt.stream_index] = pkt.dts;
        }
        if (pts_start_from[pkt.stream_index] == 0) {
            pts_start_from[pkt.stream_index] = pkt.pts;
        }

        pkt.stream_index = stream_mapping[pkt.stream_index];
        out_stream = ofmt_ctx->streams[pkt.stream_index];

        // 到结束时间, 退出
        if (end > 0 && av_q2d(in_stream->time_base) * pkt.pts > end) {
            av_packet_unref(&pkt);
            break;
        }
        /* copy packet */
        pkt.pts = av_rescale_q_rnd(pkt.pts - pts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
        pkt.dts = av_rescale_q_rnd(pkt.dts - dts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
        if (pkt.pts < 0) {
            pkt.pts = 0;
        }
        if (pkt.dts < 0) {
            pkt.dts = 0;
        }
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            LOGE("Error muxing packet\n");
            break;
        }
        av_packet_unref(&pkt);
    }
    free(dts_start_from);
    free(pts_start_from);

    av_write_trailer(ofmt_ctx);
    end:

    avformat_close_input(&ifmt_ctx);

    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);

    av_freep(&stream_mapping);

    if (ret < 0 && ret != AVERROR_EOF) {
        LOGE("Error occurred: %s\n", av_err2str(ret));
    }

    (*env)->ReleaseStringUTFChars(env, input, inputC);
    (*env)->ReleaseStringUTFChars(env, output, outputC);
    return ret;
}