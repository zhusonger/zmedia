package cn.com.lasong.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-05
 * Description: MP3播放器
 */
public class MP3Player implements IMP3DecodeCallback{

    //===========播放相关============//
    // 播放器
    private AudioTrack mAudioTrack = null;
    // 采样率
    private int mSampleRate;
    @Override
    public void onFormat(MediaFormat format) {
        if (null == format) {
            return;
        }
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int audioFormat = channelCount > 1 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        mSampleRate = sampleRate;
        // 获取最小buffer大小
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                audioFormat, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, audioFormat,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    @Override
    public void onDrain(ByteBuffer buffer, long presentationTimeUs) {
        if (null == mAudioTrack) {
            return;
        }
        if (null == buffer || buffer.remaining() <= 0) {
            return;
        }
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        mAudioTrack.write(data, 0, data.length);
    }

    @Override
    public void onEndOfStream(int err) {
        if (null != mAudioTrack) {
            try {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            } catch (Exception e) {
                MediaLog.e("onEndOfStream : " + err, e);
            }
        }
    }

    /**
     * 获取当前播放时间戳(ms)
     * @return
     */
    public long getCurrentPosition() {
        if (null == mAudioTrack || mSampleRate <= 0) {
            return 0;
        }
        int numFramesPlayed = mAudioTrack.getPlaybackHeadPosition();
        return (numFramesPlayed * 1000L) / mSampleRate;
    }
}
