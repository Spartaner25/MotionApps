/*******************************************************************************
 * Copyright (c) 2008 InvenSense Corporation, All Rights Reserved.
 ******************************************************************************/

/*******************************************************************************
 *
 * File: Global.java
 *
 * Date: 2009/10/15 14:00:00
 *
 * Revision: 1.0
 * 
 *******************************************************************************/

/* Global.java */

// modify this for different package
package com.invensense.app.RollDiceSM;

import android.view.Display;

public class Global {
    public static float mRoll, mPitch, mYaw;
    public static boolean enterSelfTest;
    public static float[] q = new float[4];
    public static float mQuat[] = new float[4];
    public static boolean mPortrait = true;
    public static Display mDisplay;    
    static {
        q[0] = (float) 1.0;
        q[1] = (float) 0.0;
        q[2] = (float) 0.0;
        q[3] = (float) 0.0;
    }
}