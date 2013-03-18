/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.shendu.launcher;


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;

import java.util.HashMap;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
    }

    private final Bitmap mDefaultIcon;
    private final LauncherApplication mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private int mIconDpi;

    public IconCache(LauncherApplication context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();

        // need to set mIconDpi before getting default icon
        mDefaultIcon = makeDefaultIcon();
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ShortcutInfo application, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache);

            application.title = entry.title;
            application.iconBitmap = entry.icon;
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, null);
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, labelCache);
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

            ComponentName key = LauncherModel.getComponentNameFromResolveInfo(info);
            if (labelCache != null && labelCache.containsKey(key)) {
                entry.title = labelCache.get(key).toString();
            } else {
                entry.title = info.loadLabel(mPackageManager).toString();
                if (labelCache != null) {
                    labelCache.put(key, entry.title);
                }
            }
            if (entry.title == null) {
                entry.title = info.activityInfo.name;
            }
            //entry.icon = Utilities.createIconBitmap(
                 //   getFullResIcon(info), mContext);

//            entry.icon = Utilities.createIconBitmap( //moditify,for theme
//                    info.activityInfo.loadIcon(mPackageManager), mContext);
            boolean isSystemApp=false;

    		if (info!=null && (
    			(info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0 ||
              (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0)){
    			isSystemApp=true;
    		}else{
    			isSystemApp=false;
    		}
    
    		bitmap = Utilities.createIconBitmap( //moditify,for theme
                    info.activityInfo.loadShenduIcon(mPackageManager), mContext);
    		
    	//	appBgSizeBg = (int) mContext.getResources().getDimension(R.dimen.app_icon_bg_size);
    		if(appBgSize!=0){
    			appBgSize = (int) mContext.getResources().getDimension(R.dimen.app_icon_size);
    		}
    		
            if(!isSystemApp&&appBgSize>0&&bitmap.getWidth()>appBgSize){
            	bitmap=  Bitmap.createScaledBitmap(bitmap, appBgSize, appBgSize, true);
            }
            entry.icon = bitmap;
           //  bitmap=getShenduBitmap(bitmap);
           // entry.icon= bitmap;
            
        }
        return entry;
    }
   Bitmap bitmap;
   int appBgSize,appBgSizeBg;
//    private static Drawable ICON_BACKGROUND = null;
//    private static Drawable ICON_BORDER = null;
//    private static Drawable ICON_MASK = null;
//    private static Drawable ICON_PATTERN = null;
//    
//    public Bitmap getShenduBitmap( Bitmap apkbitmap){
//    	if(ICON_BACKGROUND == null){
//            ICON_BACKGROUND=mContext.getResources().getDrawable(R.drawable.icon_bg);
//    	}
//    	
//    	if(ICON_MASK == null){
//            ICON_MASK =ICON_BACKGROUND;
//    	}
//        
//      
//        if(ICON_BACKGROUND!=null  && ICON_MASK!=null ){
//        	apkbitmap = shenduSynthesDrawable(ICON_BACKGROUND, ICON_MASK,apkbitmap);
//        }else{
//        	apkbitmap = apkbitmap;
//        }
//        
//        return apkbitmap;
//    }
//    
//    
//    private final static class MaskPaint {
//        
//        private static Paint dstInPaint;
//
//        public static final Paint dstInPaint(int mAlpha)
//        {
//          if (dstInPaint != null){
//              dstInPaint.setAlpha(mAlpha);
//              dstInPaint.setFilterBitmap(false);
//              dstInPaint.setAntiAlias(true);
//              dstInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//          }else{
//              Paint newDstInPaint = new Paint();
//              dstInPaint = newDstInPaint;
//              newDstInPaint.setAlpha(mAlpha);
//              dstInPaint.setFilterBitmap(false);
//              dstInPaint.setAntiAlias(true);
//              dstInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//          }
//          return dstInPaint;
//        }
//    }
//    
//    /**
//     * d1 and d2 make dr 
//     * @param dr1
//     * @param dr2
//     * @return
//     */
//    /**@hide*/
//    public Bitmap shenduSynthesDrawable(Drawable background,Drawable mask,Bitmap apkbitmap) {
//    	//Log.i("LYS-TEST", ".0000000..............mask = "  + mask.getIntrinsicWidth() + " , " + mask.getIntrinsicHeight() + " , background = " +  background.getIntrinsicWidth() + " , " + background.getIntrinsicHeight() );
//    	Drawable dr;
//        Drawable[] array = new Drawable[2];
//        array[0] = background;
//        
//        Bitmap maskBitmap = Bitmap.createBitmap(
//                background.getIntrinsicWidth(),
//                background.getIntrinsicHeight(),
//                Bitmap.Config.ARGB_8888
//                ); 
//        
//        Canvas maskCanvas = new Canvas(maskBitmap); 
//        int height = apkbitmap.getHeight();
//        int width = apkbitmap.getWidth();
//        
//        Log.i(TAG, "......cacheLocked..@@@@@@................bitmap:"+width+"....");
//        if(width>120){
//        		
//        	apkbitmap = Bitmap.createBitmap(apkbitmap, (width-120)/2,(height-120)/2, 120, 120); 
//        }
//        Log.i(TAG, "......cacheLocked..@@@@@@2222................bitmap:"+apkbitmap.getHeight()+"....");
//        //Log.i("LYS-TEST", ".11111..............mask = "  + mask.getIntrinsicWidth() + " , " + mask.getIntrinsicHeight() + "    apkdrawable = " + apkBitmap.getWidth() + " , " + apkBitmap.getHeight());
//        //maskCanvas.drawBitmap(apkbitmap, (float)(background.getIntrinsicWidth() - apkbitmap.getWidth())/2, (float)(background.getIntrinsicHeight() - apkbitmap.getHeight())/2, null);
//        maskCanvas.drawBitmap(apkbitmap, 0, 0, null);
//        //Log.i("LYS-TEST", ".222222222..............mask = "  + mask.getIntrinsicWidth() + " , " + mask.getIntrinsicHeight());
//        maskCanvas.drawBitmap(shenduDrawableToBitmap(mask), 0.0F, 0.0F, MaskPaint.dstInPaint(255));
//        
//        array[1] = new BitmapDrawable(maskBitmap);
//        
//        LayerDrawable la = new LayerDrawable(array);
//
//        dr = la.mutate();
//        Bitmap bitmap2 = Bitmap.createBitmap(
//                background.getIntrinsicWidth(),
//                background.getIntrinsicHeight(),
//                dr.getOpacity() != PixelFormat.OPAQUE ?
//                Bitmap.Config.ARGB_8888: Bitmap.Config.RGB_565
//                ); 
//        Canvas canvas = new Canvas(bitmap2);  
//        dr.setBounds(0, 0, bitmap2.getWidth(),bitmap2.getHeight());  
//        dr.draw(canvas);
//        //dr = new BitmapDrawable(this,bitmap2);
//        return bitmap2;
//    }
//    
//    /**
//     * drawable to bitmap
//     * @author liuyongsheng
//     * @param drawable
//     * @return
//     */
//    /**@hide*/
//    private Bitmap shenduDrawableToBitmap(Drawable drawable) {  
//        int width = drawable.getIntrinsicWidth();  
//        int height = drawable.getIntrinsicHeight();  
//        //Log.i("LYS-TEST", "=====*****************==="+width + " , " + height);
//        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
//                : Bitmap.Config.RGB_565;  
//        Bitmap bitmap = Bitmap.createBitmap(width, height, config);  
//        Canvas canvas = new Canvas(bitmap);  
//        drawable.setBounds(0, 0, width, height);  
//        drawable.draw(canvas); 
//        Log.i(TAG, "......cacheLocked.....................width:"+width+"...height."+height);
//        if(width >120 || height > 120){
//        	int newWidth = 120;
//        	int newHeight = 120;
//        	float scaleWidth = (float)newWidth/width;
//        	float scaleHeight = (float)newHeight/height;
//        	Matrix matrix =  new Matrix();
//        	matrix.postScale(scaleWidth, scaleHeight); 
//        	return Bitmap.createBitmap(bitmap, 0, 0, 
//        	                        width, height,matrix,true); 
//        }
//        //Log.i("LYS-TEST", "========"+bitmap.getWidth() + " , " + bitmap.getHeight());
//        return bitmap;  
//    }  
//    

    public HashMap<ComponentName,Bitmap> getAllIcons() {
        synchronized (mCache) {
            HashMap<ComponentName,Bitmap> set = new HashMap<ComponentName,Bitmap>();
            for (ComponentName cn : mCache.keySet()) {
                final CacheEntry e = mCache.get(cn);
                set.put(cn, e.icon);
            }
            return set;
        }
    }
}
