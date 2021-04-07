package cn.com.lasong.media;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-05
 * Description: MP3解码器回调
 */
public interface IMP3DecodeCallback {

    void onFormat(MediaFormat format);

    void onDrain(ByteBuffer buffer, long presentationTimeUs);

    void onEndOfStream(int err);
}
