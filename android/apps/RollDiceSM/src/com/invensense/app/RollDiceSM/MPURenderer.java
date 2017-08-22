/*******************************************************************************
 * Copyright (c) 2008 InvenSense Corporation, All Rights Reserved.
 ******************************************************************************/

/*******************************************************************************
 *
 * File: MPURenderer.java
 *
 * Date: 2009/10/15 14:00:00
 *
 * Revision: 1.0
 * 
 *******************************************************************************/

/* MPURenderer.java */

package com.invensense.app.RollDiceSM;

import android.content.Context;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;

public class MPURenderer implements GLSurfaceView.Renderer{
    public MPURenderer(Context context) {
        mContext = context;
    }

    public int[] getConfigSpec() {
        // We don't need a depth buffer, and don't care about our
        // color depth.
        int[] configSpec = {
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_NONE
        };
        return configSpec;
    }

    public void surfaceCreated(GL10 gl) {
        mCube = new Cube(mContext, gl);

        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glShadeModel(GL10.GL_SMOOTH);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    public void drawFrame(GL10 gl) {
        /*
         * Usually, the first thing one might want to do is to clear
         * the screen. The most efficient way of doing this is to use
         * glClear().
         */

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        /*
         * Now we're ready to draw some 3D objects
         */

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        GLU.gluLookAt(gl, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        mCube.draw(gl);
   }

    public void sizeChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);

        /*
        * Set our projection matrix. This doesn't have to be done
        * each time we draw, but usually a new projection needs to
        * be set when the viewport is resized.
        */

        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1.0f, 1.0f, 3.0f, 7.0f);
    }

    private Context mContext;
    private Cube mCube;
}