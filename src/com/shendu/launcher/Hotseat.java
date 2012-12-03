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

package com.shendu.launcher;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.shendu.launcher.R;

public class Hotseat extends FrameLayout {
    @SuppressWarnings("unused")
    private static final String TAG = "Hotseat";

    private Launcher mLauncher;
    private CellLayout mContent;

    public int mCellCountX;
    private int mCellCountY;
    //private int mAllAppsButtonRank;
    private boolean mIsLandscape;
    private ShortcutAndWidgetContainer mShortcutAndWidgetContainer ;

    private static final int DEFAULT_CELL_COUNT_X = 5;
    private static final int DEFAULT_CELL_COUNT_Y = 1;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Hotseat, defStyle, 0);
        //mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, -1);
        mCellCountX=0;
        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, -1);
        //mAllAppsButtonRank = context.getResources().getInteger(R.integer.hotseat_all_apps_index);
        mIsLandscape = context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
        mShortcutAndWidgetContainer=(ShortcutAndWidgetContainer)mContent.getShortcutsAndWidgets();
    }

    CellLayout getLayout() {
        return mContent;
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    int getOrderInHotseat(int x, int y) {
        return mIsLandscape ? (mContent.getCountY() - y - 1) : x;
    }
    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return mIsLandscape ? 0 : rank;
    }
    int getCellYFromOrder(int rank) {
        return mIsLandscape ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mCellCountX < 0) mCellCountX = DEFAULT_CELL_COUNT_X;
        if (mCellCountY < 0) mCellCountY = DEFAULT_CELL_COUNT_Y;
        mContent = (CellLayout) findViewById(R.id.layout);
        //mContent.setGridSize(mCellCountX, mCellCountY);
        mContent.setGridSize(1,1);
        mContent.setIsHotseat(true);

        resetLayout();
    }
    
	int mCount =0;
    
	public void setGridSize(int cellCount,boolean isAdd,boolean initState){ //used to update hotseat count
		
		
		mContent.setUseTempCoords(false);
		mCount =mShortcutAndWidgetContainer.getChildCount();
		if(isAdd&&mCellCountX<=mCount){
			mCellCountX=cellCount;
			mContent.setGridSize(mCellCountX, mCellCountY);	
		}else if(!isAdd&&mCellCountX>mCount){	
			mCellCountX=cellCount;
			mContent.setGridSize(mCellCountX, mCellCountY);	
		}
		View view = null;
		for(int i = 0 ,j=0;i < mCount; i++,j++){
			view =mShortcutAndWidgetContainer.getChildAt(j,0);
			if(view!=null){
				mContent.animateChildToPosition(view,i,0,150,0,true,true);
			}else{
				if(initState){
				}else{
					i--;
				}
			}
			if(j-i>20){
				return;
			}
		}
	}

    void resetLayout() {
        mContent.removeAllViewsInLayout();

    }
}
