/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shendu.launcher.screenshot;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.animation.AnimatorSet;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

public class TakeScreenshotService extends Service {
	
    private static final String TAG = "TakeScreenshotService";
    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private NotificationManager mNotificationManager;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
    private Bitmap mScreenBitmap;
    private View mScreenshotLayout;
    private ImageView mBackgroundView;
    private ImageView mScreenshotView;
    private ImageView mScreenshotFlash;
    private AnimatorSet mScreenshotAnimation;
    private int mNotificationIconSize;
    private float mBgPadding;
    private float mBgPaddingScale;
    private final IBinder mBinder = new LocalBinder();  

   
    
    public class LocalBinder extends Binder {  
    	 public TakeScreenshotService getService() {  
            return TakeScreenshotService.this;  
        }  
    }
    
    
    @Override  
    public IBinder onBind(Intent intent) {  
        return mBinder;  
    }  
    
	public void onCreate() {
		super.onCreate();
		   initScreenshot(TakeScreenshotService.this);
		   
		   
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}



	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
	}
	
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	
	}



	public void initScreenshot(Context context) {
        Resources r = context.getResources();
        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Inflate the screenshot layout
        mDisplayMatrix = new Matrix();


        // Setup the window that we are going to use
        mWindowLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        mWindowLayoutParams.setTitle("ScreenshotAnimation");
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        // Get the various target sizes
        mNotificationIconSize =
            r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        // Scale has to account for both sides of the bg
        mBgPadding = (float) (74);
        mBgPaddingScale = mBgPadding /  mDisplayMetrics.widthPixels;

        // Setup the Camera shutter sound

    }
    
    /**
     * @return the current display rotation in degrees
     */
    private float getDegreesForRotation(int value) {
        switch (value) {
        case Surface.ROTATION_90:
            return 360f - 90f;
        case Surface.ROTATION_180:
            return 360f - 180f;
        case Surface.ROTATION_270:
            return 360f - 270f;
        }
        return 0f;
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
   public Bitmap takeScreenshot(int startBarHeight) {
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)
 
        
        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
        int rot = mDisplay.getRotation();
        // Allow for abnormal hardware orientation
        rot = (rot + (android.os.SystemProperties.getInt("ro.sf.hwrotation",0) / 90 )) % 4;
        float degrees = getDegreesForRotation(rot);
        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }
        
        // Take the screenshot
        mScreenBitmap = Surface.screenshot((int) dims[0], (int) dims[1]);
        if (mScreenBitmap == null) {
           // notifyScreenshotError(mContext, mNotificationManager);
        }
        mScreenBitmap= Bitmap.createBitmap(mScreenBitmap, 0, startBarHeight, (int) dims[0], (int) dims[1] - startBarHeight);
        if (requiresRotation) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(mScreenBitmap, 0, 0, null);
            c.setBitmap(null);
            mScreenBitmap = ss;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

//        long time =SystemClock.currentThreadTimeMillis();
//        Log.i("zlf", "......................savePic)"+time);
//		savePic(mScreenBitmap,"sdcard/"+String.valueOf(time)+".png");
        return mScreenBitmap;
    }


    private void savePic(Bitmap b,String strFileName){ 
        FileOutputStream fos = null; 
        try { 
            fos = new FileOutputStream(strFileName); 
            if (null != fos) 
            { 
            	b.compress(Bitmap.CompressFormat.PNG, 90, fos); 
                fos.flush(); 
                fos.close(); 
            } 
        } catch (FileNotFoundException e) { 
            e.printStackTrace(); 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    }  
    
    
    
    
    
}
