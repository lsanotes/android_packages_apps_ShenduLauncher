/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.shendu.launcher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.shendu.launcher.LauncherApplication;

import com.shendu.launcher.R;

public class Preferences extends PreferenceActivity {

    private static final String TAG = "Launcher.Preferences";
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mSharedPreferences = getSharedPreferences(PreferencesProvider.PREFERENCES_KEY, Context.MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        
        /*SharedPreferences prefs = getSharedPreferences(PreferencesProvider.PREFERENCES_KEY, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PreferencesProvider.PREFERENCES_CHANGED, true);
        editor.commit();*/
                

        // Remove some preferences on large screens
        if (LauncherApplication.isScreenLarge()) {
            PreferenceGroup homescreen = (PreferenceGroup) findPreference("ui_homescreen");
            homescreen.removePreference(findPreference("ui_homescreen_grid"));
            homescreen.removePreference(findPreference("ui_homescreen_screen_padding_vertical"));
            homescreen.removePreference(findPreference("ui_homescreen_screen_padding_horizontal"));
            homescreen.removePreference(findPreference("ui_homescreen_indicator"));

            PreferenceGroup drawer = (PreferenceGroup) findPreference("ui_drawer");
            drawer.removePreference(findPreference("ui_drawer_indicator"));
        }

        //Preference version = findPreference("application_version");
        //version.setTitle(getString(R.string.application_name));
    }
    
    protected void onDestroy() {
    	super.onDestroy();
    	mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }
    
    /**
     * add by hhl, used to listener the launcher settings changed
     */
    OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener(){
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String str) {
		//	Log.i(TAG, "!!!!!!!!!!!!!!!Preferences.java...OnSharedPreferenceChangeListener==="+str);
			if(!str.equals(PreferencesProvider.PREFERENCES_CHANGED)){
				SharedPreferences.Editor editor = mSharedPreferences.edit();
		        editor.putBoolean(PreferencesProvider.PREFERENCES_CHANGED, true);
		        editor.commit();
			}
		}
    };
    
}
