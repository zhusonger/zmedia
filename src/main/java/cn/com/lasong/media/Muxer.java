package cn.com.lasong.media;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/2
 * Description:
 */
public class Muxer {
    static {
        System.loadLibrary("avformat");
        System.loadLibrary("avcodec");
        System.loadLibrary("mux");
    }

    // 禁止修改, 由native赋值
    private long nativeZMuxerContext = 0;
    /**
     * 重新合成视频
     * @param input 输入的视频
     * @param output 输出的视频
     * @param start 开始的时间, 单位秒, <=0 表示从头开始
     * @param end 结束的时候, 单位秒, <=0 表示到结尾
     * @param metadata 是否保留metadata
     * @param rotate 是否保留旋转信息
     * @param startKeyFrame 是否从关键帧开始, 关键帧可能不在准确的时间, 但是画面可以确切的解析, 非关键帧在准确的时间, 但是画面会不全
     * @return 返回值, 0表示成功, 其他值表示失败
     */
    public static native int remux(String input, String output, double start, double end, boolean metadata, boolean rotate, boolean startKeyFrame);
    public static int remux(String input, String output, double start, double end) {
        return remux(input, output, start, end, true, true, true);
    }

    /**
     * 初始化合成器
     * 重复初始化只会生效第一次, 除非stop之后再次调用init
     * @param output 输出文件路径
     * @param format 文件的格式, 如mp4, 不传从路径猜测, 猜测失败默认使用mp4
     * @return
     */
    public native long init(long handle, String output, String format);
    public long init(String output) {
        return init(nativeZMuxerContext, output, null);
    }
    public long init(String output, String format) {
        return init(nativeZMuxerContext, output, format);
    }

    /**
     * 添加视频流
     * @param handle init方法返回的处理上下文句柄地址
     * @param bit_rate 码率
     * @param width 输出宽
     * @param height 输出高
     * @param frame_rate 输出帧率
     * @param gop_size 输出GOP
     * @return 成功0, 小于0 失败
     */
    public native int add_video_stream(long handle, long bit_rate, int width, int height, int frame_rate, int gop_size);
    public int add_video_stream(long bit_rate, int width, int height, int frame_rate, int gop_size) {
        return add_video_stream(nativeZMuxerContext, bit_rate, width, height, frame_rate, gop_size);
    }
    public int add_video_stream(long bit_rate, int width, int height) {
        return add_video_stream(bit_rate, width, height, 30, 10);
    }

    /**
     * 视频数据转换配置, 不配置表示输入的帧数据跟编码器要求的帧数据格式与大小一样
     * 格式/大小不一样在write_video_frame写入帧数据时会进行转换
     * @param handle init方法返回的处理上下文句柄地址
     * @param src_fmt 源输入格式
     * @param src_width 源宽
     * @param src_height 源高
     * @return 成功0, 小于0 失败
     */
    public native int scale_video(long handle, int src_fmt, int src_width, int src_height);
    public int scale_video(int src_fmt, int src_width, int src_height) {
        return scale_video(nativeZMuxerContext, src_fmt, src_width, src_height);
    }

    /**
     * 开始合成
     * @param handle init方法返回的处理上下文句柄地址
     * @return 成功0, 小于0 失败
     */
    public native int start(long handle);
    public int start() {
        return start(nativeZMuxerContext);
    }

    /**
     * 写入视频帧
     * @param handle init方法返回的处理上下文句柄地址
     * @param data 源未压缩数据, 如RGBA, YUV420P, 调用过scale_video就认为是src_fmt, 否则认为是编码器格式
     *             传入null表示结尾
     * @return 成功0, 小于0 失败
     */
    public native int write_video_frame(long handle, byte[] data);
    public int write_video_frame(byte[] data) {
        return write_video_frame(nativeZMuxerContext, data);
    }

    /**
     * 停止合成
     * @param handle init方法返回的处理上下文句柄地址
     * @return 成功0, 小于0 失败
     */
    public native int stop(long handle);
    public int stop() {
        return stop(nativeZMuxerContext);
    }
}
