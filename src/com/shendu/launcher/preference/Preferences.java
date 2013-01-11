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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.shendu.launcher.Launcher;
import com.shendu.launcher.LauncherApplication;
import com.shendu.launcher.R;

public class Preferences extends PreferenceActivity {

    private static final String TAG = "Launcher.Preferences";
    private SharedPreferences mSharedPreferences;
    private boolean mSearchBarExist;
    private CheckBoxPreference mSearchCheckBoxPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mSharedPreferences = getSharedPreferences(PreferencesProvider.PREFERENCES_KEY, Context.MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        mSearchCheckBoxPreference=(CheckBoxPreference)findPreference(PreferencesProvider.PREFERENES_SEARCH);
        mSearchBarExist = mSharedPreferences.getBoolean(PreferencesProvider.SEARCHBAR_EXIST,true);
        mSearchCheckBoxPreference.setEnabled(mSearchBarExist);
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
