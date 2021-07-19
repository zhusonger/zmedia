package cn.com.lasong.media.record.audio;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/1
 * Description:
 * 适配回调, 避免实现太多的接口, 代码显的冗余
 */
public class AdapterAudioListener implements IAudioListener {

    @Override
    public void onInitialized() {

    }

    @Override
    public void onInitFail() {

    }

    @Override
    public void onInterrupted() {

    }

    @Override
    public void onStartFail() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onSuccess() {

    }

    @Override
    public boolean onFinish() {
        return false;
    }
}
