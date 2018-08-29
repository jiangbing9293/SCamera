package com.jscheng.scamera.render;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.jscheng.scamera.render.BaseRenderDrawer;
import com.jscheng.scamera.util.GlesUtil;

/**
 * Created By Chengjunsen on 2018/8/27
 */
public class OriginalRenderDrawer extends BaseRenderDrawer {
    private int av_Position;
    private int af_Position;
    private int s_Texture;
    private int mInputTextureId;
    private int mOutputTextureId;
    private int mFrameBuffer;

    @Override
    protected void onCreated() {

    }

    @Override
    protected void onChanged(int width, int height) {
        mOutputTextureId = GlesUtil.createFrameTexture(width, height);
        mFrameBuffer = GlesUtil.createFrameBuffer();
        GlesUtil.bindFrameTexture(mFrameBuffer, mOutputTextureId);

        av_Position = GLES20.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES20.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES20.glGetUniformLocation(mProgram, "s_Texture");
    }

    @Override
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(av_Position);
        GLES20.glEnableVertexAttribArray(af_Position);
        GLES20.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);
        if (isBackCamera) {
            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mBackTextureBuffer);
        } else {
            GLES20.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES20.GL_FLOAT, false, TextureStride, mFrontTextureBuffer);
        }
        bindTexture(mInputTextureId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VertexCount);
        unBindTexure();
        GLES20.glDisableVertexAttribArray(av_Position);
        GLES20.glDisableVertexAttribArray(af_Position);
    }

    private void bindTexture(int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(s_Texture, 0);
    }

    private void unBindTexure() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void bindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOutputTextureId, 0);
    }

    public void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void deleteFrameBuffer() {
//        GLES20.glDeleteRenderbuffers(1, new int[]{mFrameRender}, 0);
        GLES20.glDeleteFramebuffers(1, new int[]{mFrameBuffer}, 0);
        GLES20.glDeleteTextures(1, new int[]{mOutputTextureId}, 0);
    }

    @Override
    public void setInputTextureId(int textureId) {
        mInputTextureId = textureId;
    }

    @Override
    public int getOutputTextureId() {
        return mOutputTextureId;
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
        final String source = "#extension GL_OES_EGL_image_external : require \n" +
                "precision mediump float; " +
                "varying vec2 v_texPo; " +
                "uniform samplerExternalOES s_Texture; " +
                "void main() { " +
                "   gl_FragColor = texture2D(s_Texture, v_texPo); " +
                "} ";
        return source;
    }
}