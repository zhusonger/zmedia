package cn.com.lasong.media;

import java.nio.ByteBuffer;

public class Resample {
    // 禁止修改, 由native赋值
    private long nativeSwrContext = 0;
    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("resample");
    }

    /**
     * 将dst数据更新为混音后的数据
     * mix还是直接只支持默认均一化合成, 用ffmpeg加入的库比较大
     * @param dst  原音频
     * @param mix  混音音频
     */
    public static native void mix(/*DirectByteBuffer*/ByteBuffer dst, byte[] mix);

    /**
     * 重采样
     * @param src_data   解码源音频字节数据
     * @param src_len  解码源音频字节数据有效数据长度
     * @return  返回实际重采样后的长度
     */
    private native int resample(long nativeSwrContext, byte[] src_data, int src_len);
    public int resample(byte[] src_data, int src_len) {
        return resample(nativeSwrContext, src_data, src_len);
    }

    /**
     * 读取重采样后的数据
     * @param nativeSwrContext
     * @param dst_data 大于等于resample返回长度的字节数组
     * @param dst_len resample返回的字节数组长度
     * @return 返回读取的字节数组长度
     */
    private native int read(long nativeSwrContext, byte[] dst_data, int dst_len);
    public int read(byte[] dst_data, int dst_len) {
        return read(nativeSwrContext, dst_data, dst_len);
    }

    /**
     * 初始化重采样工具类
     * @param src_channel_layout 源音频声道排布 {@link cn.com.lasong.media.AVChannelLayout}
     * @param src_fmt   源音频格式 {@link cn.com.lasong.media.AVSampleFormat}
     * @param src_rate  源音频采样率
     * @param dst_channel_layout 目标音频声道排布
     * @param dst_fmt   目标音频格式
     * @param dst_rate 目标音频采样率
     * @return
     */
    private native int init(long nativeSwrContext, long src_channel_layout, int src_fmt, int src_rate,
                           long dst_channel_layout, int dst_fmt, int dst_rate);
    public int init(long src_channel_layout, int src_fmt, int src_rate,
                    long dst_channel_layout, int dst_fmt, int dst_rate) {
        return init(nativeSwrContext, src_channel_layout, src_fmt, src_rate, dst_channel_layout, dst_fmt, dst_rate);
    }
    /**
     * 销毁重采样工具类
     */
    private native void destroy(long nativeSwrContext);
    public void release() {
        destroy(nativeSwrContext);
    }
}