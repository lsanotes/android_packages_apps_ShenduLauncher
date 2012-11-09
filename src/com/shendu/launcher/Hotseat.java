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

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class Hotseat extends FrameLayout {
  //  private static final int sAllAppsButtonRank = 4; // In the middle of the dock

    private Launcher mLauncher;
    private CellLayout mContent;

    public int mCellCountX;
    private int mCellCountY;
    private boolean mIsLandscape;
    private CellLayoutChildren cellLayoutChildren ;

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
      //  mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, -1);
        
        mCellCountX=0;
        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, -1);
        mIsLandscape = context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
        
        cellLayoutChildren=(CellLayoutChildren)mContent.getChildrenLayout();
    
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
//    public static boolean isAllAppsButtonRank(int rank) {
//        return rank == sAllAppsButtonRank;
//    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mCellCountX < 0) mCellCountX = DEFAULT_CELL_COUNT_X;
        if (mCellCountY < 0) mCellCountY = DEFAULT_CELL_COUNT_Y;
        mContent = (CellLayout) findViewById(R.id.layout);
        mContent.setGridSize(1, 1);

        resetLayout();
    }
    int count =0; 
    public void setGridSize(int cellCount,boolean isAdd,boolean initState){
   
     	 count =cellLayoutChildren.getChildCount();
     	
        
     	if(isAdd&&mCellCountX<cellCount){
     		
    		mCellCountX=cellCount;
     		mContent.setGridSize(mCellCountX, mCellCountY);	
        
     	}else if(!isAdd&&mCellCountX>count){	
     		mCellCountX=cellCount;
     		mContent.setGridSize(mCellCountX, mCellCountY);	

     	}

    	View view = null;

    	for(int i = 0 ,j=0;i < count; i++,j++){

    		view =cellLayoutChildren.getChildAt(j,0);
    		
    		if(view!=null){
       		 mContent.animateChildToPosition(view,i,0,230,30);
    	
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
    

    public void viewMatchingCellInfo(){
    	
    	CellLayoutChildren clc=	(CellLayoutChildren)mContent.getChildrenLayout();
    	
       int  count =clc.getChildCount();
    	mContent.setGridSize(count, mCellCountY);
    }
    

    void resetLayout() {
        mContent.removeAllViewsInLayout();

    }


}
