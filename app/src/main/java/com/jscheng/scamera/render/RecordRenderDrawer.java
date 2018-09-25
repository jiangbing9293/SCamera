package com.jscheng.scamera.render;

import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.jscheng.scamera.record.VideoEncoder;
import com.jscheng.scamera.util.EGLHelper;
import com.jscheng.scamera.util.GlesUtil;
import com.jscheng.scamera.util.StorageUtil;
import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGL;

/**
 * Created By Chengjunsen on 2018/9/21
 */
public class RecordRenderDrawer extends BaseRenderDrawer implements Runnable{
    // 绘制的纹理 ID
    private int mTextureId;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private Handler mMsgHandler;
    private EGLHelper mEglHelper;
    private EGLSurface mEglSurface;

    private int av_Position;
    private int af_Position;
    private int s_Texture;


    public RecordRenderDrawer() {
        this.mVideoEncoder = null;
        this.mEglHelper = null;
        this.mTextureId = -1;
        new Thread(this).start();
    }

    @Override
    public void setInputTextureId(int textureId) {
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_UPDATE_TEXTUREID, textureId);
        mMsgHandler.sendMessage(msg);
    }

    @Override
    public int getOutputTextureId() {
        return mTextureId;
    }

    @Override
    public void create() {
    }

    @Override
    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_START_RECORD, width, height, EGL14.eglGetCurrentContext());
        mMsgHandler.sendMessage(msg);
    }

    @Override
    public void draw() {
        Message msg = mMsgHandler.obtainMessage(MsgHandler.MSG_FRAME, System.currentTimeMillis());
        mMsgHandler.sendMessage(msg);
    }

    @Override
    public void run() {
        Looper.prepare();
        mMsgHandler = new MsgHandler();
        Looper.loop();
    }

    private class MsgHandler extends Handler {
        public static final int MSG_START_RECORD = 1;
        public static final int MSG_STOP_RECORD = 2;
        public static final int MSG_UPDATE_CONTEXT = 3;
        public static final int MSG_UPDATE_TEXTUREID = 4;
        public static final int MSG_UPDATE_SIZE = 5;
        public static final int MSG_FRAME = 6;
        public static final int MSG_QUIT = 7;

        public MsgHandler() {

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_RECORD:
                    prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_STOP_RECORD:
                    break;
                case MSG_UPDATE_CONTEXT:
                    updateEglContext((EGLContext) msg.obj);
                    break;
                case MSG_UPDATE_SIZE:
                    updateChangedSize(msg.arg1, msg.arg2);
                    break;
                case MSG_UPDATE_TEXTUREID:
                    updateTextureId((int)msg.obj);
                    break;
                case MSG_FRAME:
                    drawFrame(msg.arg1);
                    break;
                case MSG_QUIT:
                    release();
                    break;
                default:
                    break;
            }
        }
    }

    private void prepareVideoEncoder(EGLContext context, int width, int height) {
        try {
            mVideoPath = StorageUtil.getVedioPath(true) + "glvideo.mp4";
            mVideoEncoder = new VideoEncoder(width, height, 1000000, new File(mVideoPath));
            mEglHelper = new EGLHelper();
            mEglHelper.createGL(context);
            mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
            mEglHelper.makeCurrent(mEglSurface);
            onCreated();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateEglContext(EGLContext context) {
        mEglSurface = EGL14.EGL_NO_SURFACE;
        mEglHelper.destroyGL();
        mEglHelper.createGL(context);
        mEglSurface = mEglHelper.createWindowSurface(mVideoEncoder.getInputSurface());
        mEglHelper.makeCurrent(mEglSurface);
    }

    private void updateTextureId(int mTextureId) {
        this.mTextureId = mTextureId;
    }

    private void drawFrame(long timeStamp) {
        mVideoEncoder.drainEncoder(false);
        mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapBuffers(mEglSurface);
        onDraw();
    }

    public void updateChangedSize(int width, int height) {
        onChanged(width, height);
    }

    public void release() {
        mEglHelper.destroySurface(mEglSurface);
        mEglHelper.destroyGL();
        mEglSurface = EGL14.EGL_NO_SURFACE;
        mVideoEncoder.release();
        mEglHelper = null;
        mVideoEncoder = null;
        Looper.myLooper().quit();
    }

    @Override
    protected void onCreated() {
        mProgram = GlesUtil.createProgram(getVertexSource(), getFragmentSource());
    }

    @Override
    protected void onChanged(int width, int height) {
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onDraw() {
        clear();
        useProgram();
        viewPort(0, 0, width, height);

        GLES30.glEnableVertexAttribArray(av_Position);
        GLES30.glEnableVertexAttribArray(af_Position);
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, VertexStride, mVertexBuffer);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, TextureStride, mDisplayTextureBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glUniform1i(s_Texture, 0);
        // 绘制 GLES30.GL_TRIANGLE_STRIP:复用坐标
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES30.glDisableVertexAttribArray(av_Position);
        GLES30.glDisableVertexAttribArray(af_Position);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    @Override
    protected String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    @Override
    protected String getFragmentSource() {
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                //"    gl_FragColor = vec4(color, color, color, 1);\n" +
                "}";
        return source;
    }
}
