/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shendu.launcher.R;

/**
 * 2012-9-12 hhl
 * PagedViewEffect.java
 * Trebuchet
 * TODO: editstate bottom layout choice effect display item view
 */
public class PagedViewEffect extends RelativeLayout {
	
	
    public PagedViewEffect(Context context) {
        this(context, null);
    }

    public PagedViewEffect(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewEffect(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void applyFromShenduPrograme(ShenduPrograme info) {
    	
    	TextView effectName = (TextView)findViewById(R.id.editstate_tabhost_tabcontent_effect_name_id);
    	TextView effectIcon = (TextView)findViewById(R.id.editstate_tabhost_tabcontent_effect_icon_id);
    	TextView effectMark = (TextView)findViewById(R.id.editstate_tabhost_tabcontent_effect_mark_id);
    	effectName.setText(info.getEffectStrId());
    	effectIcon.setBackgroundResource(info.getEffectDrawableId());
    	if(info.isEffectCurrent()){
        	effectMark.setVisibility(View.VISIBLE);
    	}
    	setTag(info);
        /*setCompoundDrawablesWithIntrinsicBounds(0,info.getEffectDrawableId(),info.getEffectMarkDrawableId(),0);
        setText(info.getEffectStrId());
        setTag(info);*/
    }

}
