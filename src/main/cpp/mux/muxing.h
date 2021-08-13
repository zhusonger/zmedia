#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

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

#define STREAM_DURATION   10.0
#define STREAM_FRAME_RATE 25 /* 25 images/s */
#define STREAM_PIX_FMT    AV_PIX_FMT_YUV420P /* default pix_fmt */

#define SCALE_FLAGS SWS_BICUBIC


#define ERROR_NO_INIT -1
#define ERROR_STEAM_INITED -2
#define ERROR_NO_CODEC -3
#define ERROR_NEW_STREAM -4
#define ERROR_ALLOC_CODEC -5
#define ERROR_SAMPLE_RATE_INVALID -6
#define ERROR_CHANNEL_LAYOUT_INVALID -7
#define ERROR_OPEN_VIDEO_CODEC -8
#define ERROR_OPEN_AUDIO_CODEC -9
#define ERROR_SCALE_VIDEO -10
#define ERROR_WRITE_VIDEO_FRAME -11
#define ERROR_WRITE_AUDIO_FRAME -12

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
    char *format;
    
    /**
     * Whether or not avformat_init_output fully initialized streams
     */
    int streams_initialized;
    
    OutputStream *video_st, *audio_st;
} ZMuxContext;

// output 输出文件路径
// format 输出文件类型, 传NULL默认从output文件名猜测, 如果失败就默认为mp4, 可以传入指定的输出格式, 如"mp4"
// 返回ZMuxContext句柄地址
long init(char *output, char *format);

// 添加视频流
// handle ZMuxContext句柄地址
// bit_rate 码率
// width/height 视频宽高
// frame_rate 帧率
// gop_size 关键帧间隔帧数
int add_video_stream(long handle, int64_t bit_rate, int width, int height, int frame_rate, int gop_size);


// 配置转换参数, 不配置的话, 默认输入跟编码器输出配置一致
// 在添加add_video_stream之后设置
// src_fmt AVPixelFormat枚举值, 输入数据的格式, 如果src_fmt跟编码器的输入格式不一致, 需要进行转换
int scale_video(long handle, int src_fmt, int src_width, int src_height);

// 添加音频流
// handle ZMuxContext句柄地址
// sample_fmt 采样的格式, AVSampleFormat枚举对应的值, 如AV_SAMPLE_FMT_S16
// bit_rate 码率
// sample_rate 采样率
// channel_layout 声道布局
int add_audio_stream(long handle, int sample_fmt, int64_t bit_rate, int sample_rate, uint64_t channel_layout);

// 开始合成
// handle ZMuxContext句柄地址
int start(long handle);

// 写入视频帧数据
// handle ZMuxContext句柄地址
// data 写入的视频数据, 如果scale_video就是配置的格式, 否则就是编码器输入格式, NULL表示结束EOF
int write_video_frame(long handle, const uint8_t *data);

// 停止合成
// handle ZMuxContext句柄地址
int stop(long handle);
