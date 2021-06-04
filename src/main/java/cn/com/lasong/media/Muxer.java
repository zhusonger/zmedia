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

    public static native int remux(String input, String output);
}
