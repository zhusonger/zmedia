package cn.com.lasong.media;

import java.nio.ByteBuffer;

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

    /**
     * 重新合成视频
     * @param input 输入的视频
     * @param output 输出的视频
     * @param start 开始的时间, 单位秒, <=0 表示从头开始
     * @param end 结束的时候, 单位秒, <=0 表示到结尾
     * @param metadata 是否保留metadata
     * @param rotate 是否保留旋转信息
     * @return 返回值, 0表示成功, 其他值表示失败
     */
    public native int remux(String input, String output, double start, double end, boolean metadata, boolean rotate);
    public int remux(String input, String output, double start, double end) {
        return remux(input, output, start, end, true, true);
    }
}
