/*******************************************************************************
 * Copyright (c) 2008 InvenSense Corporation, All Rights Reserved.
 ******************************************************************************/

/*******************************************************************************
 *
 * File: Cube.java
 *
 * Date: 2009/10/15 14:00:00
 *
 * Revision: 1.0
 * 
 *******************************************************************************/

/* Cube.java */

package com.invensense.app.RollDiceSM;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * A vertex shaded cube. 
 */
class Cube
{
    public Cube(Context context, GL10 gl)
    {
        float box[] = {
                // FRONT
                -0.5f, -0.5f,  0.5f,
                0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                // BACK
                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                // LEFT
                -0.5f, -0.5f,  0.5f,
                -0.5f,  0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f,  0.5f, -0.5f,
                // RIGHT
                0.5f, -0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                // TOP
                -0.5f,  0.5f,  0.5f,
                0.5f,  0.5f,  0.5f,
                -0.5f,  0.5f, -0.5f,
                0.5f,  0.5f, -0.5f,
                // BOTTOM
                -0.5f, -0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f,  0.5f,
                0.5f, -0.5f, -0.5f,
            };

        float texCoords[] = {
                // FRONT
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                // BACK
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                // LEFT
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                // RIGHT
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                // TOP
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                // BOTTOM
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                // MIDDLE
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
            };

        /*
         * Create our texture. This has to be done each time the
         * surface is created.
         */

        int mTextureID;

        gl.glGenTextures(mTextures.length, mTextures, 0);

        for (int i = 0; i < mTextures.length; ++i) {
            mTextureID = mTextures[i];

            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureID);

            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_NEAREST);

            gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_LINEAR);

            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_CLAMP_TO_EDGE);

            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                    GL10.GL_REPLACE);

            InputStream is = null;

            switch (i) {
                case 0:
                    is = context.getResources().openRawResource(R.drawable.dice1);
                    break;

                case 1:
                    is = context.getResources().openRawResource(R.drawable.dice2);
                    break;

                case 2:
                    is = context.getResources().openRawResource(R.drawable.dice3);
                    break;

                case 3:
                    is = context.getResources().openRawResource(R.drawable.dice4);
                    break;

                case 4:
                    is = context.getResources().openRawResource(R.drawable.dice5);
                    break;

                case 5:
                    is = context.getResources().openRawResource(R.drawable.dice6);
                    break;
            }

            Bitmap bitmap;

            try {
                bitmap = BitmapFactory.decodeStream(is);
            } finally {
                try {
                    is.close();
                } catch(IOException e) {
                    // ignore exception
                }
            }

            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }

        // Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(box.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();
        mFVertexBuffer.put(box);
        mFVertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();
        mTexBuffer.put(texCoords);
        mTexBuffer.position(0);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
    }

    public void draw(GL10 gl)
    {
        // get current orientation
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        synchronized(Global.q) {
            gl.glRotatef((float)(2 * Math.acos(Global.q[0]) * 180 / 3.1415), -Global.q[1], -Global.q[2], -Global.q[3]); //orig
            //gl.glRotatef((float)(2 * Math.acos(Global.q[0]) * 180 / 3.1415), Global.q[1], Global.q[3], -Global.q[2]); //NexusOne?
            //gl.glRotatef((float)(2 * Math.acos(Global.q[0]) * 180 / 3.1415), Global.q[1], -Global.q[2], -Global.q[3]); // landscape klp imported from DS fix
        }
        
        // for texture
        gl.glEnable(GL10.GL_CULL_FACE);

        gl.glActiveTexture(GL10.GL_TEXTURE0);

        gl.glScalef(1.2f, 1.2f, 1.2f);

        for (int i = 0; i < 6; ++i) {
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextures[i]);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, i * 4, 4);
        }
    }

    private int mTextures[] = new int[6];
    private FloatBuffer mFVertexBuffer;
    private FloatBuffer mTexBuffer;
}
