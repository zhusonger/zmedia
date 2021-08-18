/*
 * Copyright (c) 2003 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * @file
 * libavformat API example.
 *
 * Output a media file in any supported libavformat format. The default
 * codecs are used.
 * @example muxing.c
 */

#include "muxing.h"
#include "../util/helper.h"
#include <libavutil/avassert.h>
#include <libavutil/channel_layout.h>
#include <libavutil/opt.h>
#include <libavutil/mathematics.h>
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <libavutil/imgutils.h>
#include <libavutil/parseutils.h>

// a wrapper around a single output AVStream
typedef struct OutputStream {
    AVStream *st;
    AVCodecContext *enc; // codec context

    /* pts of the next frame that will be generated */
    int64_t next_pts;
    int samples_count;

    AVFrame *frame;
    AVFrame *tmp_frame;

    float t, tincr, tincr2;

    struct SwsContext *sws_ctx; // 视频转换
    struct SwrContext *swr_ctx; // 音频重采样
    enum AVPixelFormat src_pix_fmt;
    int src_width;
    int src_height;
    int eof;
} OutputStream;

// a muxer
typedef struct ZMuxContext
{
    AVFormatContext *oc;
    char *output;
//    char *format;

    /**
     * Whether or not avformat_init_output fully initialized streams
     */
    int streams_initialized;

    OutputStream *video_st, *audio_st;
} ZMuxContext;
//=======Private=========//
// 生成一个存储帧数据的空间, 用于复用
int p_alloc_re_usable_picture(AVFrame **picture, enum AVPixelFormat pix_fmt, int width, int height) {
    int ret = 0;
    *picture = av_frame_alloc();
    if (!picture) {
        LOGE("Could not alloc video frame\n");
        ret = ERROR_OPEN_VIDEO_CODEC;
        goto end;
    }

    (*picture)->format = pix_fmt;
    (*picture)->width  = width;
    (*picture)->height = height;

    /* allocate the buffers for the frame data */
    ret = av_frame_get_buffer(*picture, 0);
    if (ret < 0) {
        LOGE("Could not allocate frame data.\n");
        ret = ERROR_OPEN_VIDEO_CODEC;
        goto end;
    }

end:
    return ret;
}

// 关闭流并释放相关数据
void p_close_stream(ZMuxContext *ctx, OutputStream *stream)
{
    if (NULL != stream)
    {
        enum AVMediaType type = AVMEDIA_TYPE_UNKNOWN;
        // 获取类型
        if (stream->enc && stream->enc->codec)
            type = stream->enc->codec->type;
        avcodec_free_context(&stream->enc);
        av_frame_free(&stream->frame);
        av_frame_free(&stream->tmp_frame);
        sws_freeContext(stream->sws_ctx);
        swr_free(&stream->swr_ctx);
        // stream->st在oc中释放, 这里释放会导致调用avformat_free_context异常
        // av_freep(&stream->st);
        // codec 在 avcodec_free_context已经释放
        if (type == AVMEDIA_TYPE_AUDIO) {
            av_freep(&ctx->audio_st);
        } else if (type == AVMEDIA_TYPE_VIDEO) {
            av_freep(&ctx->video_st);
        }
    }
}

static void p_log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt)
{
    return;
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;

    LOGD("pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
                        av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
                        av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
                        av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
                        pkt->stream_index);
}

//=======Public=========//
long init(char *output/*, char *format*/)
{
    ZMuxContext *ctx = (ZMuxContext *)calloc(sizeof(ZMuxContext), 1);
    long handle = (long)ctx;
    ctx->output = output;
//    ctx->format = format;
    ctx->streams_initialized = -1;
    AVFormatContext *oc;
    /* allocate the output media context */
    avformat_alloc_output_context2(&oc, NULL, NULL, output);
    if (!oc) {
        LOGE("Could not deduce output format from file extension: using MP4.\n");
        avformat_alloc_output_context2(&oc, NULL, "mp4", output);
    }
    if (!oc) {
        stop(handle);
        return 0;
    }
    ctx->oc = oc;
    return handle;
}

int add_video_stream(long handle, int64_t bit_rate, int width, int height, int frame_rate, int gop_size) {
    if (handle == 0)
        return ERROR_NO_INIT;

    ZMuxContext *ctx = (ZMuxContext *)handle;
    if (ctx->video_st != NULL) {
        return ERROR_STEAM_INITED;
    }

    int ret = 0;
    OutputStream *video_st = av_mallocz(sizeof(OutputStream));
    ctx->video_st = video_st;
    AVFormatContext *oc = ctx->oc;
    AVOutputFormat *fmt = oc->oformat;

    /* Add the audio and video streams using the default format codecs
     * and initialize the codecs. */
    if (fmt->video_codec != AV_CODEC_ID_NONE) {
        enum AVCodecID codec_id = fmt->video_codec;
        /* find the encoder */
        AVCodec *codec = avcodec_find_encoder(codec_id);
        if (!(codec)) {
            LOGE("Could not find encoder for '%s'\n",
                    avcodec_get_name(codec_id));
            ret = ERROR_NO_CODEC;
            goto error;
        }
        video_st->st = avformat_new_stream(oc, NULL);
        if (!video_st->st) {
            LOGE("Could not allocate stream\n");
            ret = ERROR_NEW_STREAM;
            goto error;
        }
        video_st->st->id = oc->nb_streams-1;
        AVCodecContext *codec_context = avcodec_alloc_context3(codec);
        if (!codec_context) {
            LOGE("Could not alloc an encoding context\n");
            ret = ERROR_ALLOC_CODEC;
            goto error;
        }
        
        // 取编码器第一个像素格式, 否则就用默认值
        enum AVPixelFormat pix_fmt = codec->pix_fmts ?
            codec->pix_fmts[0] : STREAM_PIX_FMT;
        /* allocate and init a re-usable frame */
        AVFrame *picture = NULL;
        if(p_alloc_re_usable_picture(&picture, pix_fmt, width, height) < 0) {
            LOGE("Could not allocate video frame\n");
            ret = ERROR_OPEN_VIDEO_CODEC;
            goto error;
        }
        video_st->frame = picture;
        // codec在AVCodecContext中, 释放AVCodecContext就已经释放了codec
        video_st->enc = codec_context;
        codec_context->codec_id = codec_id;
        codec_context->bit_rate = bit_rate;
        codec_context->width = width;
        codec_context->height = height;
        video_st->st->time_base = (AVRational){ 1, frame_rate };
        codec_context->time_base = video_st->st->time_base;
        codec_context->gop_size = gop_size; /* emit one intra frame every twelve frames at most */
        
        codec_context->pix_fmt = pix_fmt;
        /* Some formats want stream headers to be separate. */
        if (fmt->flags & AVFMT_GLOBALHEADER)
            codec_context->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    }

error:
    // 出错就释放stream
    if(ret != 0) {
        p_close_stream(ctx, video_st);
        ctx->video_st = NULL;
    }

    return ret;
}

int scale_video(long handle, int src_fmt, int src_width, int src_height)
{
    if (handle == 0)
        return ERROR_NO_INIT;

    ZMuxContext *ctx = (ZMuxContext *)handle;
    if (!ctx->video_st)
    {
        return ERROR_NO_INIT;
    }
    OutputStream *video_st = ctx->video_st;
    // 已经配置过, 释放之前的
    if (ctx->video_st->sws_ctx)
    {
        sws_freeContext(video_st->sws_ctx);
    }
    enum AVPixelFormat dst_pix_fmt = video_st->enc->pix_fmt;
    enum AVPixelFormat src_pix_fmt = (enum AVPixelFormat)src_fmt;
    int dst_width = video_st->enc->width;
    int dst_height = video_st->enc->height;
    // 参数都相同, 不需要配置
    if (src_pix_fmt == dst_pix_fmt && dst_width == src_width && dst_height == src_height)
    {
        return 0;
    }
    /* allocate and init a re-usable frame */
    AVFrame *picture = NULL;
    if(p_alloc_re_usable_picture(&picture, src_pix_fmt, src_width, src_height) < 0) {
        LOGE("Could not allocate video frame\n");
        return ERROR_SCALE_VIDEO;
    }
    // 配置转换上下文
    struct SwsContext *sws_ctx = sws_getContext(src_width, src_height,
                                       src_pix_fmt,
                                       dst_width, dst_height,
                                       dst_pix_fmt,
                                       SCALE_FLAGS, NULL, NULL, NULL);
    if (!sws_ctx)
    {
        LOGE("Impossible to create scale context for the conversion "
                "fmt:%s s:%dx%d -> fmt:%s s:%dx%d\n",
                av_get_pix_fmt_name(src_pix_fmt), src_width, src_height,
                av_get_pix_fmt_name(dst_pix_fmt), dst_width, dst_height);
        return ERROR_SCALE_VIDEO;
    }
    video_st->sws_ctx = sws_ctx;
    video_st->tmp_frame = picture;
    video_st->src_pix_fmt = src_pix_fmt;
    video_st->src_width = src_width;
    video_st->src_height = src_height;
    return 0;
}

int add_audio_stream(long handle, int sample_fmt, int64_t bit_rate, int sample_rate, uint64_t channel_layout) {
    if (handle == 0)
        return ERROR_NO_INIT;
    
    ZMuxContext *ctx = (ZMuxContext *)handle;
    if (ctx->audio_st != NULL) {
        return ERROR_STEAM_INITED;
    }
    int ret = 0;
    OutputStream *audio_st = av_mallocz(sizeof(OutputStream));
    ctx->audio_st = audio_st;
    AVFormatContext *oc = ctx->oc;
    AVOutputFormat *fmt = oc->oformat;

    if (fmt->audio_codec != AV_CODEC_ID_NONE) {
        int i;
        enum AVCodecID codec_id = fmt->audio_codec;
        /* find the encoder */
        AVCodec *codec = avcodec_find_encoder(codec_id);
        if (!(codec)) {
            LOGE("Could not find encoder for '%s'\n",
                    avcodec_get_name(codec_id));
            ret = ERROR_NO_CODEC;
            goto error;
        }

        audio_st->st = avformat_new_stream(oc, NULL);
        if (!audio_st->st) {
            LOGE("Could not allocate stream\n");
            ret = ERROR_NEW_STREAM;
            goto error;
        }
        // 递增0,1,2
        audio_st->st->id = oc->nb_streams-1;
        AVCodecContext *codec_context = avcodec_alloc_context3(codec);
        if (!codec_context) {
            LOGE("Could not alloc an encoding context\n");
            ret = ERROR_ALLOC_CODEC;
            goto error;
        }
        // codec在AVCodecContext中, 释放AVCodecContext就已经释放了codec
        audio_st->enc = codec_context;
        codec_context->sample_fmt  = codec->sample_fmts ?
            codec->sample_fmts[0] : AV_SAMPLE_FMT_FLTP;
        codec_context->bit_rate = bit_rate;
        // 搜索编码器是否支持该采样率
        if (codec->supported_samplerates) {
            for (i = 0; codec->supported_samplerates[i]; i++) {
                if (codec->supported_samplerates[i] == sample_rate) {
                    codec_context->sample_rate = sample_rate;
                    break;
                }
            }
        } else {
            codec_context->sample_rate = sample_rate;
        }
        if (codec_context->sample_rate <= 0) {
            LOGE("Could not support sample_rate : %d\n", sample_rate);
            ret = ERROR_SAMPLE_RATE_INVALID;
            goto error;
        }

        // 搜索编码器是否支持该声道
        if (codec->channel_layouts) {
            for (i = 0; codec->channel_layouts[i]; i++) {
                if (codec->channel_layouts[i] == channel_layout) {
                    codec_context->channels = av_get_channel_layout_nb_channels(channel_layout);
                    codec_context->channel_layout = channel_layout;
                    break;
                }
            }
        } else {
            codec_context->channels = av_get_channel_layout_nb_channels(channel_layout);
            codec_context->channel_layout = channel_layout;
        }

        if (codec_context->channels <= 0) {
            LOGE("Could not support channel_layout : %lu\n", channel_layout);
            ret = ERROR_SAMPLE_RATE_INVALID;
            goto error;
        }
        audio_st->st->time_base = (AVRational){ 1, sample_rate };
    }

    error:
    // 出错就释放stream
    if(ret != 0) {
        p_close_stream(ctx, audio_st);
        ctx->audio_st = NULL;
    }
    return ret;
}

int start(long handle) 
{
    if (handle == 0)
        return ERROR_NO_INIT;

    int ret = 0;
    ZMuxContext *ctx = (ZMuxContext *)handle;
    if (ctx->video_st) {
        /* open the codec */
        OutputStream *video_st = ctx->video_st;
        ret = avcodec_open2(video_st->enc, video_st->enc->codec, NULL);
        if (ret < 0) {
            LOGE("Could not open video codec: %s\n", av_err2str(ret));
            ret = ERROR_OPEN_VIDEO_CODEC;
            goto end;
        }


        /* copy the stream parameters to the muxer */
        ret = avcodec_parameters_from_context(video_st->st->codecpar, video_st->enc);
        if (ret < 0) {
            LOGE("Could not copy the stream parameters\n");
            ret = ERROR_OPEN_VIDEO_CODEC;
            goto end;
        }
    }

    // TODO: 先不管音频
    if (ctx->audio_st) {
        /* open the codec */
        OutputStream *audio_st = ctx->audio_st;
        ret = avcodec_open2(audio_st->enc, audio_st->enc->codec, NULL);
        if (ret < 0) {
            LOGE("Could not open audio codec: %s\n", av_err2str(ret));
            ret = ERROR_OPEN_AUDIO_CODEC;
            goto end;
        }

        AVCodecContext *codec_context = audio_st->enc;
        /* init signal generator */
        audio_st->t     = 0;
        audio_st->tincr = 2 * M_PI * 110.0 / codec_context->sample_rate;
        /* increment frequency by 110 Hz per second */
        audio_st->tincr2 = 2 * M_PI * 110.0 / codec_context->sample_rate / codec_context->sample_rate;
    }

    AVFormatContext *oc = ctx->oc;
    AVOutputFormat *fmt = oc->oformat;
    const char* filename = ctx->output;
    /* open the output file, if needed */
    if (!(fmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&oc->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("Could not open '%s': %s\n", filename,
                    av_err2str(ret));
            goto end;
        }
    }

    /* Write the stream header, if any. */
    ret = avformat_write_header(oc, NULL);
    ctx->streams_initialized = ret;
    if (ret < 0) {
        LOGE("Error occurred when opening output file: %s\n",
                av_err2str(ret));
        goto end;
    } else 

end:
    return ret;
}


int write_video_frame(long handle, const uint8_t *data) {
    if (handle == 0)
        return ERROR_NO_INIT;

    ZMuxContext *ctx = (ZMuxContext *)handle;
    if (ctx->streams_initialized < 0) {
        LOGE("streams_initialized is negative : %s\n",
                av_err2str(ctx->streams_initialized));
        return ctx->streams_initialized;
    }

    OutputStream *video_st = ctx->video_st;
    if (video_st->eof == AVERROR_EOF) {
        LOGE("video_st is reach end : %s\n",
                av_err2str(video_st->eof));
        return video_st->eof;
    }
    int ret = 0;
    AVCodecContext *codec_context = video_st->enc;

    // 有数据才进行更新frame
    if (data)
    {
        // av_compare_ts
        // return ts_a == ts_b ? 0 : ts_a < ts_b ? -1 : 1
        /* when we pass a frame to the encoder, it may keep a reference to it
        * internally; make sure we do not overwrite it here */
        // 拷贝一份数据, 因为编码器可能会持有这个frame
        if (av_frame_make_writable(video_st->frame) < 0)
        {
            ret = ERROR_WRITE_VIDEO_FRAME;
            goto end;
        }

        // 需要转换的数据
        if (video_st->sws_ctx)
        {
            AVFrame *frame = video_st->tmp_frame;
            // align 对齐值, 1表示保持原宽高
            if (av_image_fill_arrays(frame->data, frame->linesize, data,
                                     video_st->src_pix_fmt, video_st->src_width, video_st->src_height, 1) < 0)
            {
                av_buffer_unref(&frame->buf[0]);
                ret = ERROR_WRITE_VIDEO_FRAME;
                goto end;
            }

            sws_scale(video_st->sws_ctx, (const uint8_t *const *)frame->data,
                      frame->linesize, 0, video_st->src_height, video_st->frame->data,
                      video_st->frame->linesize);
        }
    }

    video_st->frame->pts = video_st->next_pts++;

    // send the frame to the encoder
    ret = avcodec_send_frame(codec_context, data ? video_st->frame : NULL);
    if (ret < 0) {
        LOGE("Error sending a frame to the encoder: %s\n",
                av_err2str(ret));
        ret = ERROR_WRITE_VIDEO_FRAME;
        goto end;
    }

    while (ret >= 0) {
        AVPacket pkt = { 0 };

        ret = avcodec_receive_packet(codec_context, &pkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            if (ret == AVERROR(EAGAIN)) {
                ret = 0;
            }
            break;
        }
        else if (ret < 0) {
            LOGE("Error encoding a frame: %s\n", av_err2str(ret));
            ret = ERROR_WRITE_VIDEO_FRAME;
            goto end;
        }

        /* rescale output packet timestamp values from codec to stream timebase */
        av_packet_rescale_ts(&pkt, codec_context->time_base, video_st->st->time_base);
        pkt.stream_index = video_st->st->index;

        /* Write the compressed frame to the media file. */
        p_log_packet(ctx->oc, &pkt);
        ret = av_interleaved_write_frame(ctx->oc, &pkt);
        av_packet_unref(&pkt);
        if (ret < 0) {
            LOGE("Error while writing output packet: %s\n", av_err2str(ret));
            ret = ERROR_WRITE_VIDEO_FRAME;
            goto end;
        }
    }
end:
    video_st->eof = ret;
    return ret;    
}

int stop(long handle)
{
    if (handle == 0)
        return ERROR_NO_INIT;

    ZMuxContext *ctx = (ZMuxContext *)handle;
    int ret = ctx->streams_initialized;
    AVFormatContext *oc = ctx->oc;
    AVOutputFormat *fmt = oc->oformat;
    if (ctx->streams_initialized == 0)
    {
        /* Write the trailer, if any. The trailer must be written before you
        * close the CodecContexts open when you wrote the header; otherwise
        * av_write_trailer() may try to use memory that was freed on
        * av_codec_close(). */
        av_write_trailer(oc);
    } else {
        LOGE("Error occurred when opening output");
    }
    p_close_stream(ctx, ctx->video_st);
    p_close_stream(ctx, ctx->audio_st);
    if (NULL != fmt && !(fmt->flags & AVFMT_NOFILE))
        /* Close the output file. */
        avio_closep(&oc->pb);
    /* free the stream */
    avformat_free_context(oc);
    av_freep(&ctx->output);
//    av_freep(&ctx->format);
    av_freep(&ctx);
    return ret;
}


//int main(int argc, char **argv) {
//    if (argc < 3) {
//        printf("usage: %s output_file intput_size\n"
//               "Example: %s mac-build/muxing_video.mp4 500x500\n"
//               "intput_size is raw data size\n"
//               "\n", argv[0], argv[0]);
//        return 1;
//    }
//    const char *filename = argv[1];
//    const char *src_size = argv[2];
//    int src_w, src_h, dst_w, dst_h;
//    if (av_parse_video_size(&src_w, &src_h, src_size) < 0) {
//        LOGE(
//                "Invalid size '%s', must be in the form WxH or a valid size abbreviation\n",
//                src_size);
//        exit(1);
//    }
//    dst_w = src_w;
//    dst_h = src_h;
//
//    char* rgba_1 = "/Users/youxi/Documents/Dev/git/ffmpeg/mac-build/rgba_7.raw";
//    char* rgba_2 = "/Users/youxi/Documents/Dev/git/ffmpeg/mac-build/rgba_8.raw";
//    const int nmemb = src_w * src_h * 4;
//    uint8_t* rgba_1_data = av_mallocz(nmemb * sizeof(uint8_t));
//    uint8_t* rgba_2_data = av_mallocz(nmemb * sizeof(uint8_t));
//    //1 打开RGB和YUV文件
//	FILE *fpin = fopen(rgba_1, "rb");
//	if (!fpin)
//	{
//        printf("fopen %s fail\n", rgba_1);
//		exit(1);
//	} else {
//        int len = fread(rgba_1_data, nmemb, 1, fpin);
//        if (len <= 0)
//		{
//			printf("rgba_1_data len is %d\n", len);
//		    exit(1);
//		}
//        fclose(fpin);
//    }
//
//    FILE *fpin2 = fopen(rgba_2, "rb");
//	if (!fpin2)
//	{
//        printf("fopen %s fail\n", rgba_2);
//		exit(1);
//	} else {
//        int len = fread(rgba_2_data, nmemb, 1, fpin2);
//        if (len <= 0)
//		{
//			printf("rgba_2_data len is %d\n", len);
//		    exit(1);
//		}
//        fclose(fpin2);
//    }
//
//    long handle = init(filename, NULL);
//    ZMuxContext *ctx = (ZMuxContext *)handle;
//    printf("init : %ld\n", handle);
//    int ret = add_video_stream(handle, 4000000, dst_w, dst_h, 30, 10);
//    printf("add_video_stream result : %d, handle = %ld\n", ret, handle);
//    ret = scale_video(handle, (int)AV_PIX_FMT_RGBA, src_w, src_h);
//    printf("scale_video result : %d, handle = %ld\n", ret, handle);
//    // ret = add_audio_stream(handle, (int)AV_SAMPLE_FMT_S16, 64000, 44100, AV_CH_LAYOUT_STEREO);
//    // printf("add_audio_stream result : %d, handle = %ld\n", ret, handle);
//    ret = start(handle);
//    if (ret == 0) {
//        av_dump_format(ctx->oc, 0, filename, 1);
//    }
//    printf("start result : %d, handle = %ld\n", ret, handle);
//
//    OutputStream *video_st = ctx->video_st;
//    int index = 0;
//    const uint8_t* rgba = rgba_1_data;
//    while (video_st)
//    {
//        if (av_compare_ts(video_st->next_pts, video_st->enc->time_base,
//                      STREAM_DURATION, (AVRational){ 1, 1 }) > 0) {
//                          break;
//                      }
//        if (index >= 60) {
//            if (rgba == rgba_1_data) {
//                rgba = rgba_2_data;
//            } else {
//                rgba = rgba_1_data;
//            }
//            index = 0;
//        }
//        write_video_frame(handle, rgba);
//        index++;
//    }
//
//    write_video_frame(handle, NULL);
//
//    ret = stop(handle);
//    printf("stop result : %d, handle = %ld\n", ret, handle);
//
//    av_freep(&rgba_1_data);
//    av_freep(&rgba_2_data);
//}