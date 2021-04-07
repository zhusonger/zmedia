package cn.com.lasong.media;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import java.nio.ByteBuffer;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-05
 * Description: 解码MP3线程
 */
public class MP3DecodeThread extends Thread {

    public static final int ERR_INTERRUPT = -1;
    public static final int ERR_UNEXPECTED_END = -2;

    private String mPath;
    private AssetFileDescriptor mAFD;

    private IMP3DecodeCallback mCallback;
    private volatile boolean mDone = false;
    public MP3DecodeThread(String path) {
        super("DecodeThread");
        this.mPath = path;
    }

    public MP3DecodeThread(AssetFileDescriptor afd) {
        super("DecodeThread");
        this.mAFD = afd;
    }

    public void setCallback(IMP3DecodeCallback callback) {
        mCallback = callback;
    }

    private void setDone() {
        mDone = true;
    }

    @Override
    public void run() {

        if (TextUtils.isEmpty(mPath) && null == mAFD) {
            setDone();
            return;
        }

        MediaLog.d("Run extractor MP3");
        // 1. 解码文件
        MediaExtractor extractor = new MediaExtractor();
        try {
            if (!TextUtils.isEmpty(mPath)) {
                extractor.setDataSource(mPath);
            } else if (null != mAFD) {
                extractor.setDataSource(mAFD.getFileDescriptor(), mAFD.getStartOffset(), mAFD.getLength());
            }
        } catch (Exception e) {
            MediaLog.e(e);
            extractor.release();
            extractor = null;
        }
        if (null == extractor) {
            MediaLog.e("extractor is null");
            setDone();
            return;
        }
        MediaFormat audioFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (!TextUtils.isEmpty(mime) && mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                audioFormat = format;
                break;
            }
        }

        if (null == audioFormat) {
            MediaLog.e("audioFormat is null");
            setDone();
            return;
        }
        String mime = audioFormat.getString(MediaFormat.KEY_MIME);
        if (TextUtils.isEmpty(mime)) {
            MediaLog.e("mime is null");
            setDone();
            return;
        }

        // 2. 开启解码器
        MediaCodec decoder = null;
        try {
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(audioFormat, null, null, 0);
            decoder.start();
        } catch (Exception e) {
            MediaLog.e(e);
            if (null != decoder) {
                decoder.release();
                decoder = null;
            }
        }
        if (null == decoder) {
            MediaLog.e("decoder is null");
            setDone();
            return;
        }

        if (null != mCallback) {
            try {
                mCallback.onFormat(audioFormat);
            } catch (Exception e) {
                MediaLog.e(e);
            }
        }

        // 3. 解码PCM
        boolean endOfStream = false;
        int err = 0;
        while (!endOfStream && !isInterrupted()) {
            // 读取一次样本 写入 解码器
            try {
                int bufferIndex = decoder.dequeueInputBuffer(10);
                if (bufferIndex >= 0) {
                    ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                    ByteBuffer bufferCache = inputBuffers[bufferIndex];
                    bufferCache.clear();
                    int audioSize = extractor.readSampleData(bufferCache, 0);

                    // 得到buffer之后, dequeueInputBuffer与queueInputBuffer必须一对一调用
                    // 否则解码器输入buffer会不够
                    if (audioSize < 0) {
                        endOfStream = true;
                        MediaLog.w("readSampleData : error audioSize = " + audioSize);
                        decoder.queueInputBuffer(bufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        decoder.queueInputBuffer(bufferIndex, 0, audioSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            } catch (Exception e) {
                MediaLog.e(e);
            }

            // 读取器解码器数据
            while (!isInterrupted()) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int bufferIndex = decoder.dequeueOutputBuffer(info, 10);
                ByteBuffer[] buffers = decoder.getOutputBuffers();
                if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat format = decoder.getOutputFormat();
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    buffers = decoder.getOutputBuffers();
                } else if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no available data, break
                    if (!endOfStream) {
                        break;
                    } else {
                        // wait to end
                        MediaLog.d("drainEncoder : wait for eos");
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (bufferIndex < 0) {
                    MediaLog.w("drainEncoder : bufferIndex < 0 ");
                } else {
                    boolean isReachEnd = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    ByteBuffer data = buffers[bufferIndex]; // data after decode(PCM)

                    // 注意这个buffer要及时使用归还, 不然解码器的可用buffer会不够
                    if (null != mCallback) {
                        try {
                            mCallback.onDrain(data, info.presentationTimeUs);
                        } catch (Exception e) {
                            MediaLog.e(e);
                        }
                    }
                    // 每次都要释放, 与dequeueOutputBuffer必须一对一调用, 不然解码器的可用buffer会不够
                    decoder.releaseOutputBuffer(bufferIndex, false);

                    if (isReachEnd) {
                        if (!endOfStream) {
                            err = ERR_UNEXPECTED_END;
                            MediaLog.w("Audio drainEncoder : reached end of stream unexpectedly");
                        } else {
                            MediaLog.d("Audio drainEncoder : end of stream reached");
                        }
                        break;
                    }
                }
            }
        }

        // 4. 释放相关资源
        // 释放MediaCodec
        try {
            decoder.stop();
            decoder.release();
        } catch (Exception e) {
            MediaLog.e(e);
        }
        // 释放MediaExtractor
        try {
            extractor.release();
        } catch (Exception e) {
            MediaLog.e(e);
        }

        err = err >= 0 && isInterrupted() ? ERR_INTERRUPT : err;
        if (null != mCallback) {
            try {
                mCallback.onEndOfStream(err);
            } catch (Exception e) {
                MediaLog.e(e);
            }
        }
        setDone();
    }

    /**
     * 返回线程是否结束
     * @return
     */
    public boolean isDone() {
        return mDone;
    }
}
