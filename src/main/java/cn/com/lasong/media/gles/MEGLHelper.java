package cn.com.lasong.media.gles;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import cn.com.lasong.media.MediaLog;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/20
 * Description:
 * EGL14 辅助类
 */
public class MEGLHelper {

    static final int EGL_RECORDABLE_ANDROID = 0x3142;

    int glVersion = 3;
    boolean recordable = true;

    int[] mConfigSpec;

    int[] mValue;
    int mRedSize;
    int mGreenSize;
    int mBlueSize;
    int mAlphaSize;
    int mDepthSize;
    int mStencilSize;


    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLConfig mEglConfig;
    EGLContext mEglContext;

    boolean isInitialized = false;

    /**
     * 获取EGLHelper实例
     * @return 不成功返回null
     */
    public static MEGLHelper newInstance() {
        MEGLHelper helper = new MEGLHelper();
        try {
            helper.init(3, true);
            return helper;
        } catch (Exception e) {
            MediaLog.e(e);
            helper.release();
        }

        try {
            helper.init(2, true);
            return helper;
        } catch (Exception e) {
            MediaLog.e(e);
            helper.release();
        }
        return null;
    }

    private MEGLHelper() {}
    /*初始化*/
    public void init(int glVersion, boolean recordable) {
        this.glVersion = glVersion;
        this.recordable = recordable;
        int redSize = 8;
        int greenSize = 8;
        int blueSize = 8;
        int alphaSize = 8;
        int depthSize = 16;
        int stencilSize = 0;
        mConfigSpec = new int[] {
                EGL14.EGL_RED_SIZE, redSize,
                EGL14.EGL_GREEN_SIZE, greenSize,
                EGL14.EGL_BLUE_SIZE, blueSize,
                EGL14.EGL_ALPHA_SIZE, alphaSize,
                EGL14.EGL_DEPTH_SIZE, depthSize,
                EGL14.EGL_STENCIL_SIZE, stencilSize,
                EGL14.EGL_RENDERABLE_TYPE, glVersion == 2 ? EGL14.EGL_OPENGL_ES2_BIT : EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL_RECORDABLE_ANDROID, recordable ? 1 : 0,
                EGL14.EGL_NONE};

        mValue = new int[1];
        mRedSize = redSize;
        mGreenSize = greenSize;
        mBlueSize = blueSize;
        mAlphaSize = alphaSize;
        mDepthSize = depthSize;
        mStencilSize = stencilSize;
        /*
         * 1. Get to the default display.
         */
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            checkError();
            throw new RuntimeException("eglGetDisplay failed");
        }

        /*
         * 2. We can now initialize EGL for that display
         */
        int[] version = new int[2];
        if(!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            checkError();
            throw new RuntimeException("eglInitialize failed");
        }

        int[] num_config = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, mConfigSpec,
                0, null, 0, 0,
                num_config, 0)) {
            checkError();
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        int numConfigs = num_config[0];

        if (numConfigs <= 0) {
            checkError();
            throw new IllegalArgumentException(
                    "No configs match configSpec");
        }

        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!EGL14.eglChooseConfig(mEglDisplay, mConfigSpec, 0, configs, 0, numConfigs,
                num_config, 0)) {
            checkError();
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }


        for (EGLConfig config : configs) {
            int d = findConfigAttrib(mEglDisplay, config,
                    EGL14.EGL_DEPTH_SIZE);
            int s = findConfigAttrib(mEglDisplay, config,
                    EGL14.EGL_STENCIL_SIZE);
            if ((d >= mDepthSize) && (s >= mStencilSize)) {
                int r = findConfigAttrib(mEglDisplay, config,
                        EGL14.EGL_RED_SIZE);
                int g = findConfigAttrib(mEglDisplay, config,
                        EGL14.EGL_GREEN_SIZE);
                int b = findConfigAttrib(mEglDisplay, config,
                        EGL14.EGL_BLUE_SIZE);
                int a = findConfigAttrib(mEglDisplay, config,
                        EGL14.EGL_ALPHA_SIZE);
                if ((r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)) {
                    mEglConfig = config;
                    break;
                }
            }
        }

        if (mEglConfig == null) {
            checkError();
            throw new IllegalArgumentException("No config chosen");
        }

        int[] attrib_list = { EGL14.EGL_CONTEXT_CLIENT_VERSION, glVersion,
                EGL14.EGL_NONE };
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig,
                EGL14.EGL_NO_CONTEXT, attrib_list, 0);
        if(mEglContext == EGL14.EGL_NO_CONTEXT) {
            checkError();
            throw new IllegalStateException("No EGLContext could be made");
        }
        isInitialized = true;
    }
    private int findConfigAttrib(EGLDisplay display, EGLConfig config, int attribute) {
        if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
            return mValue[0];
        }
        return 0;
    }

    /*设置窗口surface*/
    public boolean setSurface(Surface surface) {
        if (isInitialized) {
            return false;
        }
        int[] attrib_list = { EGL14.EGL_NONE };
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface,
                attrib_list, 0);
        if (mEglSurface == EGL14.EGL_NO_SURFACE) {
            checkError();
            return false;
        }
        if(!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            checkError();
            return false;
        }
        return true;
    }

    /*释放资源*/
    public void release() {
        if (null != mEglDisplay && null != mEglSurface) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
        if (null != mEglDisplay && null != mEglContext) {
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (null != mEglDisplay) {
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
        isInitialized = false;
    }

    /*打印错误*/
    private void checkError() {
        int errorCode = EGL14.eglGetError();
        switch (errorCode) {
            case EGL14.EGL_NOT_INITIALIZED:
                MediaLog.e("对于特定的 Display, EGL 未初始化，或者不能初始化---没有初始化");
                break;
            case EGL14.EGL_BAD_ACCESS:
                MediaLog.e("EGL 无法访问资源(如 Context 绑定在了其他线程)---访问失败");
                break;
            case EGL14.EGL_BAD_ALLOC:
                MediaLog.e("对于请求的操作，EGL 分配资源失败---分配失败");
                break;
            case EGL14.EGL_BAD_ATTRIBUTE:
                MediaLog.e("未知的属性，或者属性已失效---错误的属性");
                break;
            case EGL14.EGL_BAD_CONTEXT:
                MediaLog.e("EGLContext(上下文) 错误或无效---错误的上下文");
                break;
            case EGL14.EGL_BAD_CONFIG:
                MediaLog.e("EGLConfig(配置) 错误或无效---错误的配置");
                break;
            case EGL14.EGL_BAD_DISPLAY:
                MediaLog.e("EGLDisplay(显示) 错误或无效---错误的显示设备对象");
                break;
            case EGL14.EGL_BAD_SURFACE:
                MediaLog.e("未知的属性，或者属性已失效---错误的Surface对象");
                break;
            case EGL14.EGL_BAD_CURRENT_SURFACE:
                MediaLog.e("窗口，缓冲和像素图(三种 Surface)的调用线程的 Surface 错误或无效---当前Surface对象错误");
                break;
            case EGL14.EGL_BAD_MATCH:
                MediaLog.e("参数不符(如有效的 Context 申请缓冲，但缓冲不是有效的 Surface 提供)---无法匹配");
                break;
            case EGL14.EGL_BAD_PARAMETER:
                MediaLog.e("错误的参数");
                break;
            case EGL14.EGL_BAD_NATIVE_PIXMAP:
                MediaLog.e("NativePixmapType 对象未指向有效的本地像素图对象---错误的像素图");
                break;
            case EGL14.EGL_BAD_NATIVE_WINDOW:
                MediaLog.e("NativeWindowType 对象未指向有效的本地窗口对象---错误的本地窗口对象");
                break;
            case EGL14.EGL_CONTEXT_LOST:
                MediaLog.e("电源错误事件发生，Open GL重新初始化，上下文等状态重置---上下文丢失");
                break;
        }
    }
}
