package cn.com.lasong.media.record.audio;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/1
 * Description:
 * 录音回调处理
 */
public interface IAudioListener {
    void onInitialized();
    void onInitFail();
    void onInterrupted();
    void onStartFail();
    void onPause();
    void onResume();
    void onSuccess();
    /** record stream finish (fail & success) will call it **/
    boolean onFinish();
}
