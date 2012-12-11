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

import com.shendu.launcher.Launcher;
import android.animation.AnimatorSet;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import com.shendu.launcher.R;
public class TakeScreenshotService extends Service {
	
    private static final String TAG = "TakeScreenshotService";
    private WindowManager mWindowManager;
    private Display mDisplay;
    private Bitmap mScreenBitmap;
    private final IBinder mBinder = new LocalBinder();  
    private Context mContext;
    private Drawable mMaskBitmap;
    float[] mDims = new float[2];
    
    public class LocalBinder extends Binder {  
    	 public TakeScreenshotService getService() {  
            return TakeScreenshotService.this;  
        }  
    }
    
    public IBinder onBind(Intent intent) {  
        return mBinder;  
    }  
    
	public void onCreate() {
		super.onCreate();
		initScreenshot(TakeScreenshotService.this);
	}

	public void initScreenshot(Context context) {
        mContext = context;
        mMaskBitmap = mContext.getResources().getDrawable(R.drawable.open_folder_top_botom_mask);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDims[0] = mDisplay.getWidth();
        mDims[1] = mDisplay.getHeight();
    }
    
    /**
     * Takes a screenshot of the current display and shows an animation.
     */
   public Bitmap takeScreenshot(final int startBarHeight) {
        mScreenBitmap = Surface.screenshot((int)mDims[0],(int)mDims[1]);
        mScreenBitmap= Bitmap.createBitmap(mScreenBitmap, 0, startBarHeight, 
        		 (int)mDims[0], (int)mDims[1]-startBarHeight);

        		Drawable[] array = new Drawable[2];
        		array[0] = new BitmapDrawable(mScreenBitmap);
        		array[1] = mMaskBitmap;
        		LayerDrawable layoutDrawable = new LayerDrawable(array); 
        		layoutDrawable.setLayerInset(1,0,0,0,0);
        		Drawable drawable = layoutDrawable.mutate();
        		mScreenBitmap = Bitmap.createBitmap(
        				(int)mDims[0],(int)mDims[1]-startBarHeight,
        				drawable.getOpacity() != PixelFormat.OPAQUE ?
        						Bitmap.Config.ARGB_8888: Bitmap.Config.RGB_565
        				);
        		Canvas canvas = new Canvas(mScreenBitmap);
        		drawable.setBounds(0, 0,(int)mDims[0],(int)mDims[1]-startBarHeight);  
        		drawable.draw(canvas);
        		
        		
        return mScreenBitmap;
    }

    
}