

#include "resample.h"
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include "resample_helper.h"


int16_t TPMixSamples(int16_t a, int16_t b) {
    return
        // If both samples are negative, mixed signal must have an amplitude between the lesser of A and B, and the minimum permissible negative amplitude
            (int16_t) (a < 0 && b < 0 ?
                       ((int) a + (int) b) - (((int) a * (int) b) / INT16_MIN) :

                       // If both samples are positive, mixed signal must have an amplitude between the greater of A and B, and the maximum permissible positive amplitude
                       (a > 0 && b > 0 ?
                        ((int) a + (int) b) - (((int) a * (int) b) / INT16_MAX)

                        // If samples are on opposite sides of the 0-crossing, mixed signal should reflect that samples cancel each other out somewhat
                                       :
                        a + b));
}

JNIEXPORT void JNICALL Java_cn_com_lasong_media_Resample_mix
        (JNIEnv *env, jclass clz, jobject dst, jbyteArray mix) {
    if (!mix || !dst) {
        return;
    }

    // Step1:准备更新的数组
    jbyte *dst_buffer = (*env)->GetDirectBufferAddress(env, dst);
    jlong dst_len = (*env)->GetDirectBufferCapacity(env, dst);

	// 转换成short数组
	int short_dst_len = (int) (dst_len / 2);
	jshortArray *array_short_dst = (*env)->NewShortArray(env, short_dst_len);
	jshort *short_dst = (*env)->GetShortArrayElements(env, array_short_dst, 0);
	// 转换byte到short, 2个byte对应1个short
	for (int i = 0; i < short_dst_len; i++) {
		short low = dst_buffer[i * 2];
		short high = dst_buffer[i * 2 + 1];
		short_dst[i] = (short) ((low & 0xff) | (high << 8));
	}

	// Step2:需要混音的数据
	jbyte *mix_buffer = (*env)->GetByteArrayElements(env, mix, 0);
	jsize len_mix = (*env)->GetArrayLength(env, mix);
	int short_mix_len = len_mix / 2;
	jshortArray *array_short_mix = (*env)->NewShortArray(env, short_mix_len);
	jshort *short_mix = (*env)->GetShortArrayElements(env, array_short_mix, 0);
	// 转换byte到short, 2个byte对应1个short
	for (int i = 0; i < short_mix_len; i++) {
		short low = mix_buffer[i * 2];
		short high = mix_buffer[i * 2 + 1];
		short_mix[i] = (short) ((low & 0xff) | (high << 8));
	}

	// 转换byte到short, 2个byte对应1个short
	for (int i = 0; i < short_dst_len; i++) {
		if (i < short_mix_len) {
			short dst_sample = short_dst[i];
			short mix_sample = short_mix[i];
			short final_sample = TPMixSamples(dst_sample, mix_sample);
			short_dst[i] = final_sample;
		}
	}

	// Step3:更新result
	// 转换short为byte数组
	for (int i = 0; i < short_dst_len; i++) {
		short value = short_dst[i];
		if (value > MAX_AUDIO_SIZE) {
			value = MAX_AUDIO_SIZE;
		} else if (value < MIN_AUDIO_SIZE) {
			value = MAX_AUDIO_SIZE;
		}
		short low = (short) (value & 0xff);
		short high = (short) ((value >> 8) & 0xff);
		dst_buffer[i * 2] = (jbyte) low;
		dst_buffer[i * 2 + 1] = (jbyte) high;
	}
	// 索引重置为0
    jclass cls = (*env)->GetObjectClass(env, dst);
    jmethodID mid = (*env)->GetMethodID(env, cls, "position", "(I)Ljava/nio/Buffer;");
    (*env)->CallObjectMethod(env, dst, mid, 0);

	// 释放dst相关
	// 释放新建的short数组
	(*env)->ReleaseShortArrayElements(env, array_short_dst, short_dst, 0);
	(*env)->DeleteLocalRef(env, array_short_dst);
    (*env)->DeleteLocalRef(env, cls);
	// 释放获取的byte引用
//	(*env)->ReleaseByteArrayElements(env, dst, dst_buffer, JNI_ABORT);

    // 释放mix相关
	// 释放新建的short数组
	(*env)->ReleaseShortArrayElements(env, array_short_mix, short_mix, 0);
	(*env)->DeleteLocalRef(env, array_short_mix);
	// 释放获取的byte引用
	(*env)->ReleaseByteArrayElements(env, mix, mix_buffer, JNI_ABORT);
}

/*
 * Class:     net_sourceforge_resample_Resample
 * Method:    init
 * Signature: (II)V
 */
JNIEXPORT int JNICALL Java_cn_com_lasong_media_Resample_init
        (JNIEnv *env, jobject thiz, jlong nativeSwrContext, jlong src_channel_layout, jint src_fmt,
         jint src_rate,
         jlong dst_channel_layout, jint dst_fmt, jint dst_rate) {
    SwrContext *swr_ctx;
    SwrContextExt *ctx;
    AVSampleFormat src_sample_fmt = (AVSampleFormat) src_fmt;
    AVSampleFormat dst_sample_fmt = (AVSampleFormat) dst_fmt;
    if (nativeSwrContext == 0) {
        ctx = (SwrContextExt *) calloc(sizeof(SwrContextExt), 1);
        swr_ctx = swr_alloc_set_opts(NULL, dst_channel_layout, dst_sample_fmt, dst_rate,
                                     src_channel_layout,
                                     src_sample_fmt, src_rate, 0, NULL);
        ctx->swr_ctx = swr_ctx;
    } else {
        ctx = (SwrContextExt *) nativeSwrContext;
        swr_ctx = swr_alloc_set_opts(ctx->swr_ctx, dst_channel_layout, dst_sample_fmt, dst_rate,
                                     src_channel_layout,
                                     src_sample_fmt, src_rate, 0, NULL);
    }
    ctx->src_sample_fmt = src_sample_fmt;
    ctx->src_sample_rate = src_rate;
    ctx->src_nb_channels = av_get_channel_layout_nb_channels(src_channel_layout);
    ctx->src_nb_buffers = av_sample_fmt_is_planar(src_sample_fmt) ? ctx->src_nb_channels : 1;
    ctx->src_bytes_per_sample = av_get_bytes_per_sample(src_sample_fmt);

    ctx->dst_sample_fmt = dst_sample_fmt;
    ctx->dst_sample_rate = dst_rate;
    ctx->dst_nb_channels = av_get_channel_layout_nb_channels(dst_channel_layout);
    ctx->dst_nb_buffers = av_sample_fmt_is_planar(dst_sample_fmt) ? ctx->dst_nb_channels : 1;
    ctx->dst_bytes_per_sample = av_get_bytes_per_sample(src_sample_fmt);

    int ret = 0;
    if (!swr_ctx) {
        LOGE("Could not allocate resampler context\n");
        goto error;
    }

    /* initialize the resampling context */
    if ((ret = swr_init(swr_ctx)) < 0) {
        LOGE("Failed to initialize the resampling context\n");
        goto error;
    }

    // 重新分配输出buffer
    if (ctx->src_buffers) {
        av_freep(&ctx->src_buffers[0]);
    }
    ret = av_samples_alloc_array_and_samples(&ctx->src_buffers, &ctx->src_linesize,
                                             ctx->src_nb_channels,
                                             DEFAULT_NB_SAMPLES, ctx->src_sample_fmt, 0);
    if (ret < 0) {
        LOGE("Could not allocate source samples\n");
        goto error;
    }
    ctx->src_nb_samples = DEFAULT_NB_SAMPLES;

    // 重新分配输出buffer
    if (ctx->dst_buffers) {
        av_freep(&ctx->dst_buffers[0]);
    }
    ret = av_samples_alloc_array_and_samples(&ctx->dst_buffers, &ctx->dst_linesize,
                                             ctx->dst_nb_channels,
                                             DEFAULT_NB_SAMPLES, ctx->dst_sample_fmt, 0);
    if (ret < 0) {
        LOGE("Could not allocate destination samples\n");
        goto error;
    }
    ctx->dst_nb_samples = DEFAULT_NB_SAMPLES;

    if (nativeSwrContext == 0) {
        jclass clz = (*env)->GetObjectClass(env, thiz);
        jfieldID fieldId = (*env)->GetFieldID(env, clz, "nativeSwrContext", "J");
        // 初始化成功
        (*env)->SetLongField(env, thiz, fieldId, (jlong) ctx);
    }
    return 0;

error:
    ret = AVERROR(ENOMEM);
    // 将nativeSWContext设置为0，防止重复调用close导致崩溃
    jclass clz = (*env)->GetObjectClass(env, thiz);
    jfieldID fieldId = (*env)->GetFieldID(env, clz, "nativeSwrContext", "J");
    (*env)->SetLongField(env, thiz, fieldId, (jlong) 0);
    swr_ext_free(&ctx);
    return ret;
}

/*
 * Class:     net_sourceforge_resample_Resample
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_cn_com_lasong_media_Resample_destroy
        (JNIEnv *env, jobject thiz, jlong nativeSwrContext) {
    if (nativeSwrContext == 0) {
        return;
    }
    // 将nativeSWContext设置为0，防止重复调用close导致崩溃
    jclass clz = (*env)->GetObjectClass(env, thiz);
    jfieldID fieldId = (*env)->GetFieldID(env, clz, "nativeSwrContext", "J");
    (*env)->SetLongField(env, thiz, fieldId, (jlong) 0);

    SwrContextExt *ctx = (SwrContextExt *) nativeSwrContext;
    swr_ext_free(&ctx);
}

JNIEXPORT jint JNICALL Java_cn_com_lasong_media_Resample_resample
        (JNIEnv *env, jobject thiz, jlong nativeSwrContext, jbyteArray src_data, jint src_len) {
    if (nativeSwrContext == 0) {
        return -1;
    }
    SwrContextExt *ctx = (SwrContextExt *) nativeSwrContext;

    jbyte *data = (*env)->GetByteArrayElements(env, src_data, NULL);

    int src_nb_samples = convert_samples(src_len, ctx->src_bytes_per_sample, ctx->src_nb_channels);
    // 源数据大于预设的输入buffer大小, 重新分配
    if (src_nb_samples > ctx->src_nb_samples) {
        // 重新分配输出buffer
        if (ctx->src_buffers) {
            av_freep(&ctx->src_buffers[0]);
        }

        int ret = av_samples_alloc(ctx->src_buffers, &ctx->src_linesize,
                                   ctx->src_nb_channels,
                                   src_nb_samples, ctx->src_sample_fmt, 1);
        if (ret < 0) {
            LOGE("Could not allocate source samples\n");
            goto error;
        }
        LOGE("resize src_nb_samples:%d, ctx->src_nb_samples:%d ret:%d\n", src_nb_samples, ctx->src_nb_samples, ret);
        ctx->src_nb_samples = src_nb_samples;
    }
    // packed类型的音频数据只需要传入第一个即可, swr_convert参数注释有说明
    // 如果是planner, 这里就是分割成每个声道的数据
    int single_len = src_len / ctx->src_nb_buffers;
    for (int i = 0; i < ctx->src_nb_buffers; i++) {
        memcpy(ctx->src_buffers[i], (uint8_t *) data + single_len * i, single_len);
    }

    /* compute destination number of samples */
    int dst_nb_samples = av_rescale_rnd(
            swr_get_delay(ctx->swr_ctx, ctx->src_sample_rate) +
            src_nb_samples, ctx->dst_sample_rate, ctx->src_sample_rate,
            AV_ROUND_UP);
    if (dst_nb_samples > ctx->dst_nb_samples) {
        // 重新分配输出buffer
        if (ctx->dst_buffers) {
            av_freep(&ctx->dst_buffers[0]);
        }
        int ret = av_samples_alloc(ctx->dst_buffers, &ctx->dst_linesize,
                                   ctx->dst_nb_channels,
                                   dst_nb_samples, ctx->dst_sample_fmt, 1);
        if (ret < 0) {
            LOGE("Could not allocate destination samples\n");
            goto error;
        }
        LOGE("resize dst_nb_samples:%d, ctx->dst_nb_samples:%d ret:%d\n", dst_nb_samples, ctx->dst_nb_samples, ret);
        ctx->dst_nb_samples = dst_nb_samples;
    }

    int ret = swr_convert(ctx->swr_ctx, ctx->dst_buffers, dst_nb_samples, (const uint8_t **) ctx->src_buffers, src_nb_samples);
    if (ret < 0) {
        LOGE("Error while converting\n");
        goto error;
    }
    int dst_buffer_size = av_samples_get_buffer_size(&ctx->dst_linesize, ctx->dst_nb_channels,
                                                     ret, ctx->dst_sample_fmt, 1);
    if (dst_buffer_size < 0) {
        LOGE("Could not get sample buffer size\n");
        goto error;
    }
    (*env)->ReleaseByteArrayElements(env, src_data, data, 0);

    return dst_buffer_size;

error:
    (*env)->ReleaseByteArrayElements(env, src_data, data, 0);
    return -1;
}

/*
 * Class:     cn_com_lasong_media_Resample
 * Method:    read
 * Signature: (J[BI)I
 */
JNIEXPORT jint JNICALL Java_cn_com_lasong_media_Resample_read
        (JNIEnv *env, jobject thiz, jlong nativeSwrContext, jbyteArray dst_data, jint dst_len) {
    if (nativeSwrContext == 0) {
        return -1;
    }
    SwrContextExt *ctx = (SwrContextExt *) nativeSwrContext;

    jbyte *data = (*env)->GetByteArrayElements(env, dst_data, NULL);

    int ret;
    if (ctx->dst_linesize >= dst_len) {
        memcpy(data, ctx->dst_buffers[0], dst_len);
        ret = dst_len;
    } else {
        memcpy(data, ctx->dst_buffers[0], ctx->dst_linesize);
        ret = ctx->dst_linesize;
    }

    (*env)->ReleaseByteArrayElements(env, dst_data, data, 0);

    return ret;
}


