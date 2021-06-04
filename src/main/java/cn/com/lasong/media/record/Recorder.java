package cn.com.lasong.media.record;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/6/1
 * Description:
 *
 *
 * RFC-1359:
 * 1. 无法察觉：音频和视频的时间戳差值在：-100ms ~ +25ms 之间
 * 2. 能够察觉：音频滞后了 100ms 以上，或者超前了 25ms 以上
 * 3. 无法接受：音频滞后了 185ms 以上，或者超前了 90ms 以上
 */
public class Recorder {

    private Recorder(){}
    static class Holder {
        final static Recorder INSTANCE = new Recorder();
    }
    public static Recorder getInstance() {
        return Holder.INSTANCE;
    }


}
