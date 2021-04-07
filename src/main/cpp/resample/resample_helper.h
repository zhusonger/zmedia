//
// Created by YouXi on 2020/7/14.
//

#ifndef ANDROIDZ_RESAMPLE_HELPER_H
#define ANDROIDZ_RESAMPLE_HELPER_H

#include <android/log.h>
#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
#include <libswresample/swresample.h>
// 默认使用1024个采样点, 一般来说够用了
// 双声道 s16le的音频就有4096个字节
// 单声道 s16le的音频有2048个字节

#define DEFAULT_NB_SAMPLES 1024

#define MAX_AUDIO_SIZE 32767
#define MIN_AUDIO_SIZE -32768

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "NDK_LOG", __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "NDK_LOG", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NDK_LOG", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NDK_LOG", __VA_ARGS__)
#define ASSERT(cond, fmt, ...)                                \
  if (!(cond)) {                                              \
    __android_log_assert(#cond, "AG_APM", fmt, ##__VA_ARGS__); \
  }

struct SwrContextExt {
    struct SwrContext *swr_ctx;
    uint8_t **src_buffers;
    uint8_t **dst_buffers;

    enum AVSampleFormat src_sample_fmt;
    enum AVSampleFormat dst_sample_fmt;

    int src_sample_rate;
    int dst_sample_rate;

    int src_nb_samples;         // ffmpeg默认每次采样数为1024
    int dst_nb_samples;
    int max_dst_nb_samples;     // 用于记录最大的输出采样数，防止数组越界

    int src_linesize;
    int dst_linesize;

    // 声道数
    int src_nb_channels;
    int dst_nb_channels;

    // buffer数组个数, packed是1, planner根据声道数
    int src_nb_buffers;
    int dst_nb_buffers;

    // 每个采样点字节数
    int src_bytes_per_sample;
    int dst_bytes_per_sample;
};
typedef struct SwrContextExt SwrContextExt;
typedef enum AVSampleFormat AVSampleFormat;
void swr_ext_free(SwrContextExt **ss);
int convert_samples(int bytes_len, int bytes_per_sample, int nb_channels);
#endif //ANDROIDZ_RESAMPLE_HELPER_H
