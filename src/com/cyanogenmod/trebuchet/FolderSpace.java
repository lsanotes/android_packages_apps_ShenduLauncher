package com.cyanogenmod.trebuchet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class FolderSpace extends PagedView{

	
	private Context mContext;
	LayoutInflater mInflater;
	
	
	public FolderSpace(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mContext =context;
	    mInflater = LayoutInflater.from(context);
	    
	    initSpace();
	    
	}
	
	private void initSpace(){

        View view = (CellLayout)mInflater.inflate(R.layout.folderspace_screen, null);
 
      addView(view,0);
      CellLayout cellLayout =(CellLayout)getChildAt(0);
    
      cellLayout.setGridSize(3, 1);
      cellLayout.getChildrenLayout().setMotionEventSplittingEnabled(false);
       
	}

	@Override
	public void syncPages() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void syncPageItems(int page, boolean immediate) {
		// TODO Auto-generated method stub
		
	}

}
