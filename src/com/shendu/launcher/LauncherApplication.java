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

import android.app.Application;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog.Calls;

import com.shendu.launcher.R;

import java.lang.ref.WeakReference;

public class LauncherApplication extends Application {
    public LauncherModel mModel;
    public IconCache mIconCache;
    private static boolean sIsScreenLarge;
    private static float sScreenDensity;
    public static final ComponentName sMMSComponentName = 
    		new ComponentName("com.android.mms","com.android.mms.ui.ConversationList");
    public static final ComponentName sCallComponentName = 
    		new ComponentName("com.android.contacts","com.android.contacts.activities.DialtactsActivity");
    public static final int MMS_MARK = 1;
    public static final int CALL_MARK = 2;
    private static int sLongPressTimeout = 300;
    private static final String sSharedPreferencesKey = "com.shendu.launcher.prefs";
    WeakReference<LauncherProvider> mLauncherProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        // set sIsScreenXLarge and sScreenDensity *before* creating icon cache
        sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        sScreenDensity = getResources().getDisplayMetrics().density;

        mIconCache = new IconCache(this);
        mModel = new LauncherModel(this, mIconCache);

        // Register intent receivers
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        registerReceiver(mModel, filter);

        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);
        
        // Register for changes to the call info
        resolver.registerContentObserver(Calls.CONTENT_URI, true,mCallInfoObserver);
        
        // Register for changes to the sms and mms info
        resolver.registerContentObserver(Uri.parse("content://mms-sms/conversations"),true,mSMSObserver);
        
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();

        unregisterReceiver(mModel);

        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
        resolver.unregisterContentObserver(mCallInfoObserver);
        resolver.unregisterContentObserver(mSMSObserver);
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    /**
     * 2012-9-10 hhl
     * LauncherApplication.java
     * Trebuchet
     * TODO: the listener of call info is changed
     */
    private final ContentObserver mCallInfoObserver = new ContentObserver(new Handler()) {
    	public void onChange(boolean selfChange) {
            //Log.i("hhl", "==LauncherApplications.java==mCallInfoObserver==");
    		Intent intent = new Intent();
    		intent.setAction(Intent.ACTION_MAIN);
    		intent.setComponent(sCallComponentName);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		mModel.shenduUpdateAppMarkFromRegister(CALL_MARK,intent);
        }
    };
    
    /**
     * 2012-9-10 hhl
     * LauncherApplication.java
     * Trebuchet
     * TODO: the listener of sms and mms info is changed
     */
    private final ContentObserver mSMSObserver = new ContentObserver(new Handler()) {
    	public void onChange(boolean selfChange) {
            //Log.i("hhl", "==LauncherApplications.java==mSMSObserver==");
    		Intent intent = new Intent();
    		intent.setAction(Intent.ACTION_MAIN);
    		intent.setComponent(sMMSComponentName);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		mModel.shenduUpdateAppMarkFromRegister(MMS_MARK,intent);
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        mModel.initialize(launcher);
        return mModel;
    }

    IconCache getIconCache() {
        return mIconCache;
    }

    LauncherModel getModel() {
        return mModel;
    }

    void setLauncherProvider(LauncherProvider provider) {
        mLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    LauncherProvider getLauncherProvider() {
        return mLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return sSharedPreferencesKey;
    }

    public static boolean isScreenLarge() {
      //  return sIsScreenLarge;
    	return false;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }

    public static int getLongPressTimeout() {
        return sLongPressTimeout;
    }
}
