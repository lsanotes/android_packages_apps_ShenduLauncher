package com.cyanogenmod.trebuchet;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class FolderItemsPageView extends PagedView{
	
	private Context mContext;
    private LayoutInflater mLayoutInflater;
    private View mCellLayout;
    private CellLayout mContent;
	public FolderItemsPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
    }
	
	
	public FolderItemsPageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext =context;
		Log.i("hhl", "^^^^^^^^^^FolderItemsPageView.java==FolderItemsPageView==="+getClass().getName());
		 
		 mContent = (CellLayout)findViewById(R.id.folder_content);
		 
		 
		 
		//addScreen();
		//addScreenTextView();
	}
	
	public void addScreen(View view){
		view.setMinimumHeight(120);
        view.setMinimumWidth(120);
        view.setBackgroundColor(Color.YELLOW);
		addView(view); 
	}
	
	protected void onLayout(boolean changed, int left, int top, int right,int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		Log.i("hhl", "^^^^^^^^^^FolderItemsPageView.java==onLayout==="+getClass().getName());
	}
	
	/*protected void onFinishInflate(){

        //mLayoutInflater = LayoutInflater.from(mContext);
        //mCellLayout = mLayoutInflater.inflate(R.layout.workspace_screen_add,null);
        //addScreen();
		
		Log.i("hhl", "^^^^^^^^^^FolderItemsPageView.java==onFinishInflate==="+getClass().getName());
	}*/
	
	public void addScreenTextView(){
		TextView text = new TextView(mContext);
		LayoutParams params = new LayoutParams(100, 100);
		text.setLayoutParams(params);
		text.setText(R.string.workspace_cling_title);
		text.setBackgroundColor(Color.RED);
		addView(text); 
	}
	
	public void addScreen(){
        if(mLayoutInflater ==null){
        	mLayoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
        }
        View screen = mLayoutInflater.inflate(R.layout.workspace_screen_add, null);
        screen.setBackgroundColor(Color.BLUE);
        screen.setMinimumHeight(120);
        screen.setMinimumWidth(120);
        addView(screen); 
        ((CellLayout)getChildAt((getChildCount()-1))).setGridSize(3,3);
        //((CellLayout)getChildAt((getChildCount()-1))).
	}
	
	public int addViewToPageCellLayout(int screen,View child, int index, int childId, 
			CellLayout.LayoutParams params, boolean markCells){
    	Log.i("hhl", "===FolderItemsPageView.java...addViewToPageCellLayout()==111=="+childId);
		int newScreen = -1;
		CellLayout cellLayout = null;
		if(screen>=0){
			newScreen = screen;
			cellLayout = (CellLayout)getChildAt(screen);
			cellLayout.addViewToCellLayout(child,index,childId,params,markCells);
		}else{
			cellLayout = (CellLayout)getChildAt(getChildCount()-1);
			if(cellLayout.getChildrenLayout().getChildCount()==12){
				addScreen();
			}
			cellLayout = (CellLayout)getChildAt(getChildCount()-1);
	    	Log.i("hhl", "===FolderItemsPageView.java...addViewToPageCellLayout()==222=="+getChildCount()+
	    			params.cellX+"==="+params.cellY);
			cellLayout.addViewToCellLayout(child,index,childId,params,markCells);
			newScreen = getChildCount();
		}

    	Log.i("hhl", "===FolderItemsPageView.java...addViewToPageCellLayout()==3333=="+newScreen);
		return newScreen;
				
	}
	
	
	
	public void removeAllViewsInLayout() {
		int size = getChildCount();
		CellLayout celllayout = null;
		for(int i=0;i<size;i++){
			celllayout = (CellLayout)getChildAt(i);
			celllayout.removeAllViewsInLayout();
			if(i>0){
				removeView(celllayout);
			}
		}
		//super.removeAllViewsInLayout();
	}
	
	public View getChildAt(int page,int x, int y) {
		CellLayout celllayout = (CellLayout)getChildAt(page);
        return celllayout.getChildAt(x, y);
    }
	
	public void removeItemView(View view) {
		//super.removeView(view);
		CellLayout celllayout = getParentCellLayoutForView(view);
		if(celllayout!=null){
			celllayout.removeView(view);
		}
	}
	
    /**
     * Returns a specific CellLayout
     */
    CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getChildrenLayout().indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }
    
    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getPageAt(screen)));
        }
        return layouts;
    }
	
	public void syncPages() {
		
	}

	public void syncPageItems(int page, boolean immediate) {
		
	}

}
