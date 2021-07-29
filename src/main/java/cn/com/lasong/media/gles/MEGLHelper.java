package cn.com.lasong.media.gles;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Locale;

import cn.com.lasong.media.MediaLog;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2021/7/20
 * Description:
 * EGL14 辅助类
 */
public class MEGLHelper {

    private static final String TAG = "MEGLHelper";

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

    static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;
    static final int INT_SIZE = Integer.SIZE / Byte.SIZE;
    static final int FLOAT_SIZE = Float.SIZE / Byte.SIZE;
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
            MediaLog.e(TAG, e);
            helper.release();
        }

        try {
            helper.init(2, true);
            return helper;
        } catch (Exception e) {
            MediaLog.e(TAG, e);
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
        if (!isInitialized) {
            return false;
        }
        // 销毁之前的surface
        if (null != mEglSurface && null != mEglDisplay) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
        // 创建surface
        int[] attrib_list = { EGL14.EGL_NONE };
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface,
                attrib_list, 0);
        if (null == mEglSurface || mEglSurface == EGL14.EGL_NO_SURFACE) {
            checkError();
            return false;
        }
        if(!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            checkError();
            return false;
        }
        return true;
    }

    /*设置时间戳(纳秒)并交换缓冲*/
    public void swapBuffer(long presentationTime) {
        if (null != mEglDisplay && null != mEglSurface) {
            EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, presentationTime);
            EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
        }
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

    public int getGlVersion() {
        return glVersion;
    }

    /*创建OES纹理*/
    public static @NonNull int[] glGenOesTexture(int count) {
        int[] textures = new int[count];
        GLES20.glGenTextures(count, textures, 0);
        for (int i = 0; i < count; i++) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[i]);
            checkError("glBindTexture OES");
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        return textures;
    }

    /*创建2D纹理*/
    public static @NonNull int[] glGen2DTexture(int count) {
        int[] textures = new int[count];
        GLES20.glGenTextures(count, textures, 0);
        for (int i = 0; i < count; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            checkError("glBindTexture 2D");
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        return textures;
    }

    /**
     * 绑定2D纹理
     * @param texture
     * @param watermark
     */
    public static void glBindTexture2D(int texture, Bitmap watermark) {
        if (null == watermark) {
            return;
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        checkError("glBindTexture " + texture);
        GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, watermark, 0);
        checkError("texImage2D " + watermark);
    }

    /**
     * 加载shader
     * 这个函数的输出是一个非负数字
     * 用于指定返回的 shader object
     * 当创建失败的话,返回 0
     * */
    public static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        if (shader != 0) {
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                shader = 0;
                MediaLog.e(TAG, String.format(Locale.getDefault(), "Could not compile shader %d : %s", type, info));
            }
        }
        return shader;
    }

    /**
     * 创建program
     * 这个函数的输出是一个非负整数
     * 用于指定返回的 program object
     * 当创建失败的话,返回 0
     * */
    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if (vertexShader == 0) {
            return 0;
        }
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if (fragmentShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkError("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] != GLES20.GL_TRUE) {
                String info = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                program = 0;
                MediaLog.e(TAG, "Could not create program : " + info);
            }
        }
        return program;
    }

    /**
     * 删除程序
     * @param program
     */
    public static void glDeleteProgram(int program) {
        if (program != 0) {
            GLES20.glDeleteProgram(program);
            checkError("glDeleteProgram : " + program);
        }
    }

    /**
     * 删除buffer
     * @param buffers
     */
    public static void glDeleteBuffers(int[] buffers) {
        if (null != buffers) {
            GLES20.glDeleteBuffers(buffers.length, buffers, 0);
        }
    }

    /**
     * 删除纹理
     * @param textures
     */
    public static void glDeleteTextures(int[] textures) {
        if (null == textures || textures.length == 0) {
            return;
        }
        GLES20.glDeleteTextures(textures.length, textures, 0);
    }

    /**
     * 生成本地堆内存Float数组
     * @param data
     * @return
     */
    public static FloatBuffer allocateFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    /**
     * 生成本地堆内存Int数组
     * @param data
     * @return
     */
    public static IntBuffer allocateIntBuffer(int[] data) {
        IntBuffer buffer = ByteBuffer.allocateDirect(data.length * INT_SIZE)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    /**
     * 生成本地堆内存Short数组
     * @param data
     * @return
     */
    public static ShortBuffer allocateShortBuffer(short[] data) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(data.length * SHORT_SIZE)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    /**
     * 绑定VBO/EBO数据
     * @param buffer
     * @param data
     * @param usage
     */
    public static void glBufferData(int target, int buffer, Buffer data, int sizeBytes, int usage) {
        // 1 绑定到顶点坐标数据缓冲
        GLES20.glBindBuffer(target, buffer);
        // 2.向顶点坐标数据缓冲送入数据
        GLES30.glBufferData(target, data.capacity() * sizeBytes,
                data, usage);
        // 清除data释放空间
        data.limit(0);
    }

    /**
     * 绑定顶点数据
     * @param buffer
     * @param data
     * @param usage
     */
    public static void glBindVertexBufferData(int buffer, Buffer data, int usage) {
        if (!(data instanceof FloatBuffer)) {
            throw new IllegalArgumentException("vertex data is float array");
        }
        glBufferData(GLES20.GL_ARRAY_BUFFER, buffer, data, FLOAT_SIZE, usage);
    }

    /**
     * 绑定索引数据
     * @param buffer
     * @param data
     * @param usage
     */
    public static void glBindElementBufferData(int buffer, Buffer data, int usage) {
        if (!(data instanceof ShortBuffer)) {
            throw new IllegalArgumentException("element data is short array");
        }
        glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffer, data, SHORT_SIZE, usage);
    }

    /*打印错误*/
    public static void checkError() {
        checkError(null);
    }
    public static void checkError(String message) {
        int errorCode = EGL14.eglGetError();
        if (errorCode != EGL14.EGL_SUCCESS && null != message) {
            MediaLog.e(message);
        }
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
