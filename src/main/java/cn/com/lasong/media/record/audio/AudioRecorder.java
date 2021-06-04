package cn.com.lasong.media.record.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.com.lasong.media.MediaLog;

import static android.media.MediaCodecList.REGULAR_CODECS;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/1
 * Description:
 * 音频录音器, 根据Android系统有不同的行为模式
 *
 * 1. 在 Android 10 之前，输入音频流一次只能由一个应用捕获。
 * 如果已有应用在录制或侦听音频，则您的应用可以创建一个 AudioRecord 对象，
 * 但系统会在您调用 AudioRecord.startRecording() 时返回错误，并且不会开始录制。
 *
 * 2. Android 9 中还添加了一项更改：只有在前台运行的应用（或前台服务）才能捕获音频输入。
 *
 * 3. Android 10 (API 级别 29) 或更高版本 采用优先级方案，可以在运行的应用之间切换输入音频流。
 *  使用和共享音频输入的优先级规则如下：
 *
 *  特权应用的优先级高于普通应用。
 *  具有可见前台界面的应用比后台应用具有更高的优先级。
 *  相较于从非隐私敏感源捕获音频的应用，从隐私敏感源捕获音频的应用有着更高的优先级。
 *  两个普通应用永远无法同时捕获音频。
 *  在某些情况下，特权应用可以与其他应用共享音频输入。
 *  如果两个优先级相同的后台应用都在捕获音频，则后开始的那个优先级更高。
 *
 *
 *  实现功能:
 *  使用系统的AudioRecord实现音频的录制、暂停、恢复、停止
 */
public class AudioRecorder {

    /** end successfully **/
    public static final int EVENT_SUCCESS = 0;
    /** audio record is initialized **/
    public static final int EVENT_INITIALIZED = 1;
    /** fail at create AudioRecord **/
    public static final int EVENT_INIT_FAIL = 2;
    /** interrupt by another record task **/
    public static final int EVENT_INTERRUPT = 3;
    /** start audio record fail, maybe is recording by other process **/
    public static final int EVENT_START_FAIL = 4;
    /** pause audio record **/
    public static final int EVENT_PAUSE = 5;
    /** resume audio record **/
    public static final int EVENT_RESUME = 6;

    private AudioRecorder(){}
    static class Holder {
        static final AudioRecorder INSTANCE = new AudioRecorder();
    }
    public static AudioRecorder getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 录音参数构造器
     */
    public static class Builder {
        private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION; // VoIP通话录音
        private int sampleRateInHz = 44100; //44.1KHz
        private int channelConfig = AudioFormat.CHANNEL_IN_MONO; // mono 单声道
        private int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 位深 16位/2byte/-32768 ~ 32767
        private IAudioListener audioListener = null;
        public Builder setAudioSource(int audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        public Builder setSampleRateInHz(int sampleRateInHz) {
            this.sampleRateInHz = sampleRateInHz;
            return this;
        }

        public Builder setChannelConfig(int channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public Builder setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        public Builder registerListener(IAudioListener listener) {
            this.audioListener = listener;
            return this;
        }

        public AudioRecord build() {
            AudioRecord recorder = null;
            try {
                int minBuffSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    recorder = new AudioRecord.Builder()
                            .setAudioSource(audioSource)
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(audioFormat)
                                    .setSampleRate(sampleRateInHz)
                                    .setChannelMask(channelConfig)
                                    .build())
                            .setBufferSizeInBytes(2 * minBuffSize)
                            .build();
                } else {
                    recorder = new AudioRecord(audioSource,
                            sampleRateInHz,
                            channelConfig,
                            audioFormat,
                            2 * minBuffSize);
                }
            } catch (Exception e) {
                MediaLog.e(e);
            }
            return recorder;
        }
    }

    // 系统录制类
    private volatile AudioRecord record;
    // 录制参数构造对象, 便于暂停恢复的处理
    private volatile Builder builder;
    // 系统自带编码器
    private MediaCodec encoder;
    // 线程
    private HandlerThread handlerThread;
    /**
     * 开始录制音频
     * @param record 音频录制系统类
     */
    private void start(AudioRecord record) {
        stop(EVENT_INTERRUPT);
        this.record = record;
        // Android 10(API 29)开始优先级原则, 可能会被静默处理, 添加回调监听
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            record.registerAudioRecordingCallback(null, new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    super.onRecordingConfigChanged(configs);
                    if (configs != null && !configs.isEmpty()) {
                        AudioRecordingConfiguration configuration = configs.get(0);
                        MediaLog.d(configs.size()
                                + ",\n " + configuration.isClientSilenced()
                                + ",\n " + configuration.getAudioSource()
                                + ",\n " + configuration.getClientAudioSource()
                                + ",\n " + configuration.getClientAudioSessionId()
                                + ",\n " + configuration.getFormat().getSampleRate()
                                + ",\n " + configuration.getFormat().getFrameSizeInBytes()
                                + ",\n " + configuration.getFormat().getChannelCount()
                                + ",\n " + configuration.getFormat().getChannelMask()
                                + ",\n " + configuration.getFormat().getChannelIndexMask());
                    }
                }
            });
        }

        try {
            record.startRecording();
        } catch (Exception e) {
            MediaLog.e(e);
            stop(EVENT_START_FAIL);
            return;
        }

        stopThread();
        handlerThread = new HandlerThread("AudioRecorder");
        handlerThread.start();

        handlerThread.getLooper();
    }

    public void start(Builder builder) {
        if (null == builder) {
            return;
        }
        registerListener(builder.audioListener);
        AudioRecord record = builder.build();
        if (null == record) {
            notifyListeners(EVENT_INIT_FAIL);
            return;
        }
        // encoder start
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, builder.sampleRateInHz,
                builder.audioFormat == AudioFormat.CHANNEL_IN_MONO ? 1 : 2);
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        String codeName = codecList.findEncoderForFormat(format);
        try {
            encoder = MediaCodec.createByCodecName(codeName);
            encoder.start();
        } catch (IOException e) {
            MediaLog.e(e);
            notifyListeners(EVENT_INIT_FAIL);
            return;
        }

        // 初始化成功
        notifyListeners(EVENT_INITIALIZED);
        this.builder = builder;
        start(record);


    }
    /**
     * 关闭录音
     * @param error 错误码
     */
    public void stop(int error) {
        if (null == record) {
            return;
        }
        record.release();
        record = null;

        notifyListeners(error);
    }
    public void stop() {
        stop(EVENT_SUCCESS);
    }

    /**
     * 暂停录音
     */
    public void pause() {
        if (isRecording()) {
            stop(EVENT_PAUSE);
        }
    }
    /**
     * 恢复录音
     */
    public void resume() {
        if (null != builder) {
            AudioRecord record = builder.build();
            start(record);
            if (isRecording()) {
                notifyListeners(EVENT_RESUME);
            }
        }
    }
    /**
     * 记录是否正在录制
     * @return true or false
     */
    public boolean isRecording() {
        return null != record
                && record.getState() == AudioRecord.STATE_INITIALIZED
                && record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    /**
     * 关闭线程
     */
    private void stopThread() {
        if (null != handlerThread) {
            handlerThread.quit();
            handlerThread = null;
        }
    }


    /** register & unregister listeners
     *  handle events from recorder
     * **/
    private Map<IAudioListener, Integer> listeners = new LinkedHashMap<>();
    public void registerListener(IAudioListener listener) {
        listeners.put(listener, 0);
    }
    public void unregisterListener(IAudioListener listener) {
        listeners.remove(listener);
    }
    private void notifyListeners(int event) {
        try {
            Iterator<IAudioListener> iterator = listeners.keySet().iterator();
            while(iterator.hasNext()) {
                IAudioListener listener = iterator.next();
                if (listener == null) {
                    iterator.remove();
                } else {
                    boolean remove = handleEvent(event, listener);
                    if (remove) {
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e) {
            MediaLog.e("notifyListeners", e);
        }
    }

    /**
     * 处理事件
     * @param event 事件
     * @param listener 回调
     * @return true or false, 表示是否需要删除回调接口
     */
    private boolean handleEvent(int event, IAudioListener listener) {
        if (event == EVENT_INITIALIZED) {
            listener.onInitialized();
        } else if (event == EVENT_INTERRUPT) {
            listener.onInterrupted();
        } else if (event == EVENT_SUCCESS) {
            listener.onSuccess();
        } else if (event == EVENT_INIT_FAIL) {
            listener.onInitFail();
        } else if (event == EVENT_START_FAIL) {
            listener.onStartFail();
        } else if (event == EVENT_PAUSE) {
            listener.onPause();
        } else if (event == EVENT_RESUME) {
            listener.onResume();
        }

        if (event == EVENT_SUCCESS
                || event == EVENT_INTERRUPT
                || event == EVENT_INIT_FAIL
                || event == EVENT_START_FAIL) {
            return listener.onFinish();
        }
        return false;
    }
}
