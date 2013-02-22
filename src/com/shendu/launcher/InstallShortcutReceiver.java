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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.widget.Toast;

import com.shendu.launcher.LauncherModel.Callbacks;
import com.shendu.launcher.R;
import com.shendu.launcher.preference.PreferencesProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InstallShortcutReceiver extends BroadcastReceiver {
    public static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String NEW_APPS_PAGE_KEY = "apps.new.page";
    public static final String NEW_APPS_LIST_KEY = "apps.new.list";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 75;

    private static final int INSTALL_SHORTCUT_SUCCESSFUL = 0;
    private static final int INSTALL_SHORTCUT_IS_DUPLICATE = -1;
    private static final int INSTALL_SHORTCUT_NO_SPACE = -2;

    // A mime-type representing shortcut data
    public static final String SHORTCUT_MIMETYPE =
            "com.shendu.launcher/shortcut";

    // The set of shortcuts that are pending install
    private static ArrayList<PendingInstallShortcutInfo> mInstallQueue =
            new ArrayList<PendingInstallShortcutInfo>();

    // Determines whether to defer installing shortcuts immediately until
    // processAllPendingInstalls() is called.
    private static boolean mUseInstallQueue = false;
    
    private static class PendingInstallShortcutInfo {
        Intent data;
        Intent launchIntent;
        String name;

        public PendingInstallShortcutInfo(Intent rawData, String shortcutName,
                Intent shortcutIntent) {
            data = rawData;
            name = shortcutName;
            launchIntent = shortcutIntent;
        }
    }

    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (intent == null) {
            return;
        }
        //used to filter some unwanted shortcuts
        

        Bundle bundle = intent.getExtras();
       // Log.i(Launcher.TAG," InstallShortcutReceiver.java  onReceive().....intent:"+intent+"=="+bundle);
        if(bundle==null){
            Log.i(Launcher.TAG,"InstallShortcutReceiver.java onReceive() bundle is null, do not create shortcut  ");
        	return;
        }
        
 //       Set<String> set = intent.getCategories();
//        if(set!=null){
//           if(set.contains(Intent.CATEGORY_LAUNCHER) && intent.getExtras()==null){
//                Log.i(Launcher.TAG,"  InstallShortcutReceiver.java  onReceive()    do not create shortcut  ");
//            	return;
//            }
//         }
        
        // This name is only used for comparisons and notifications, so fall back to activity name
        // if not supplied
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ActivityInfo info = pm.getActivityInfo(intent.getComponent(), 0);
                name = info.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException nnfe) {
                return;
            }
        }
        // Queue the item up for adding if launcher has not loaded properly yet
        boolean launcherNotLoaded = LauncherModel.getCellCountX() <= 0 ||
                LauncherModel.getCellCountY() <= 0;

        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, name, intent);
        if (mUseInstallQueue || launcherNotLoaded) {
            mInstallQueue.add(info);
        } else {
            processInstallShortcut(context, info);
        }
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }
    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }
    static void flushInstallQueue(Context context) {
        Iterator<PendingInstallShortcutInfo> iter = mInstallQueue.iterator();
        while (iter.hasNext()) {
            processInstallShortcut(context, iter.next());
            iter.remove();
        }
    }

    private static void processInstallShortcut(Context context,
            PendingInstallShortcutInfo pendingInfo) {
        //String spKey = PreferencesProvider.PREFERENCES_KEY;//moditify
        //SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);

        final Intent data = pendingInfo.data;
        //final Intent intent = pendingInfo.launchIntent;
        //final String name = pendingInfo.name;

        // Lock on the app so that we don't try and get the items while apps are being added
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        //add,used to add the mark shortcut
        //boolean exists = LauncherModel.shortcutExists(context, name, intent);
        //if(!exists){
        	LauncherModel launcherModel = app.getModel();
            if(launcherModel!=null){
            	Callbacks callbacks = launcherModel.mCallbacks.get();
            	ShortcutInfo shortcutInfo = launcherModel.infoFromShortcutIntent(context, data,null);
            	if(shortcutInfo!=null){
                	
                	ArrayList<ShortcutInfo> list = new ArrayList<ShortcutInfo>();
                	list.add(shortcutInfo);
                	if(callbacks!=null){
                		callbacks.bindAllApplications(list);
                	}
            	}
            }
        //}else{
        	//Toast.makeText(context,context.getString(R.string.shortcut_exist_toast_message),Toast.LENGTH_SHORT).show();
        //}
        
    }

    /*private static boolean installShortcut(Context context, Intent data, ArrayList<ItemInfo> items,
            String name, Intent intent, final int screen, boolean shortcutExists,
            final SharedPreferences sharedPrefs, int[] result) {
        int[] tmpCoordinates = new int[2];
        if (findEmptyCell(context, items, tmpCoordinates, screen)) {
            if (intent != null) {
                if (intent.getAction() == null) {
                    intent.setAction(Intent.ACTION_VIEW);
                } else if (intent.getAction().equals(Intent.ACTION_MAIN) &&
                        intent.getCategories() != null &&
                        intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                }

                // By default, we allow for duplicate entries (located in
                // different places)
                boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
                if (duplicate || !shortcutExists) {
                    // If the new app is going to fall into the same page as before, then just
                    // continue adding to the current page
                    int newAppsScreen = sharedPrefs.getInt(NEW_APPS_PAGE_KEY, screen);
                    Set<String> newApps = new HashSet<String>();
                    if (newAppsScreen == screen) {
                        newApps = sharedPrefs.getStringSet(NEW_APPS_LIST_KEY, newApps);
                    }
                    synchronized (newApps) {
                        newApps.add(intent.toUri(0).toString());
                    }
                    final Set<String> savedNewApps = newApps;
                    new Thread("setNewAppsThread") {
                        public void run() {
                            synchronized (savedNewApps) {
                                sharedPrefs.edit()
                                           .putInt(NEW_APPS_PAGE_KEY, screen)
                                           .putStringSet(NEW_APPS_LIST_KEY, savedNewApps)
                                           .commit();
                            }
                        }
                    }.start();

                    // Update the Launcher db
                    LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                    ShortcutInfo info = app.getModel().addShortcut(context, data,
                            LauncherSettings.Favorites.CONTAINER_DESKTOP, screen,
                            tmpCoordinates[0], tmpCoordinates[1], true);
                    if (info == null) {
                        return false;
                    }
                } else {
                    result[0] = INSTALL_SHORTCUT_IS_DUPLICATE;
                }

                return true;
            }
        } else {
            result[0] = INSTALL_SHORTCUT_NO_SPACE;
        }

        return false;
    }*/

    /*private static boolean findEmptyCell(Context context, ArrayList<ItemInfo> items, int[] xy,
            int screen) {
        final int xCount = LauncherModel.getCellCountX();
        final int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = new boolean[xCount][yCount];

        ItemInfo item = null;
        int cellX, cellY, spanX, spanY;
        for (int i = 0; i < items.size(); ++i) {
            item = items.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (item.screen == screen) {
                    cellX = item.cellX;
                    cellY = item.cellY;
                    spanX = item.spanX;
                    spanY = item.spanY;
                    for (int x = cellX; 0 <= x && x < cellX + spanX && x < xCount; x++) {
                        for (int y = cellY; 0 <= y && y < cellY + spanY && y < yCount; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }
        }

        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }*/
}
