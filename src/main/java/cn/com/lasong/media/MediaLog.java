package cn.com.lasong.media;

import android.util.Log;

import java.util.Collection;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020-03-07
 * Description:
 */
public class MediaLog {
    private static final String TAG = "MediaLog";
    private static int sLogLevel = Log.ERROR;

    public static void setLogLevel(int level) {
        sLogLevel = level;
    }

    /**
     * Debug
     */
    public static void d(String msg) {
        d(TAG, msg, null);
    }
    public static void d(Collection collection) {
        d(TAG, collection);
    }
    public static void d(String tag, Collection collection) {
        d(tag, "", collection);
    }
    public static void d(String tag, String msg, Collection collection) {
        if (sLogLevel <= Log.DEBUG) {
            Log.d(tag, msg + formatCollection(collection));
        }
    }
    private static String formatCollection(Collection collection) {
        if (null != collection && !collection.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (Object item : collection) {
                sb.append(item).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
            return sb.toString();
        }

        return "";
    }

    /**
     * Error
     */
    public static void e(String msg) {
        e(TAG, msg, null);
    }
    public static void e(Throwable tr) {
        e(TAG, tr);
    }
    public static void e(String tag, Throwable tr) {
        e(tag, "", tr);
    }
    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }
    public static void e(String tag, String msg, Throwable tr) {
        if (sLogLevel <= Log.ERROR) {
            Log.e(tag, msg, tr);
        }
    }

    /**
     * Warn
     */
    public static void w(String msg) {
        w(TAG, msg);
    }
    public static void w(String tag, String msg) {
        if (sLogLevel <= Log.WARN) {
            Log.w(tag, msg);
        }
    }
}
