package com.shendu.util;


import com.shendu.launcher.Launcher;
import com.shendu.launcher.Launcher.PreViewDateAdapter;
import com.shendu.launcher.Workspace;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;

public class DragGrid extends GridView {

	private int dragPosition;
	private int dropPosition;
	private int holdPosition;
	private int startPosition;
	private int specialPosition = -1;
	private int leftBottomPosition = -1;
	
	private int nColumns = 3;
	private int nRows;
	private int Remainder;
	
	private int itemTotalCount;	
	private int halfItemWidth;	

	private ImageView dragImageView = null;
	private ViewGroup dragItemView = null;

	private WindowManager windowManager = null;
	private WindowManager.LayoutParams windowParams = null;
	
	private int mLastX,xtox;
	private int mLastY,ytoy;
	private int specialItemY;
	private int leftBtmItemY;
	
	private String LastAnimationID;
	
	private boolean isCountXY = false;	
	private boolean isMoving = false;
	private  Workspace  mWorkspace ;
	private int offsetX=0;
	private int offsetY=0;
	

	public DragGrid(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}

	public DragGrid(Context context, Workspace  workspace ) {
		super(context);
		mWorkspace = workspace;
	}

	
	public boolean setOnItemLongClickListener(final MotionEvent ev,final float f , final float g) {
	
		this.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				int x = (int) f;
				int y = (int) g;
				
				mLastX=(int)x;
				mLastY=(int)y;
				
				startPosition = dragPosition = dropPosition = arg2;
				
				ViewGroup itemView = (ViewGroup) getChildAt(dragPosition
						- getFirstVisiblePosition());
				if(!isCountXY){
					halfItemWidth = itemView.getWidth()/2;
				    int rows;
				    itemTotalCount = getCount();
				    rows = itemTotalCount/nColumns;
				    
				    Remainder = itemTotalCount%nColumns;
				    nRows =  Remainder == 0 ?  rows : rows + 1;
				    specialPosition = itemTotalCount - 1 - Remainder;
				    if(Remainder!=1)
				    	leftBottomPosition = nColumns*(nRows-1);
				    if(Remainder == 0 || nRows == 1)
				    	specialPosition = -1;			    
				   // isCountXY = true;
				}
			    if(specialPosition>=0&&specialPosition != dragPosition && dragPosition != -1){
			        specialItemY = getChildAt(specialPosition).getTop();
			    }else{
			    	specialItemY = -1;
			    }
			    if(leftBottomPosition>=0&&leftBottomPosition != dragPosition && dragPosition != -1){
			    	leftBtmItemY = getChildAt(leftBottomPosition).getTop();
			    }else{
			    	leftBtmItemY = -1;
			    }
				dragItemView = itemView;
				itemView.destroyDrawingCache();
				itemView.setDrawingCacheEnabled(true);
				itemView.setDrawingCacheBackgroundColor(0x000000);
				Bitmap bm = Bitmap.createBitmap(itemView.getDrawingCache(true));
				Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
				startDrag(bitmap, x, y,itemView.getX(), itemView.getY());
				hideDropItem();
				itemView.setVisibility(View.INVISIBLE);				
				isMoving = false;
				return true;
			};
		});
		
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			return setOnItemLongClickListener(ev ,ev.getX() ,ev.getY());
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		if (dragImageView != null
				&& dragPosition != AdapterView.INVALID_POSITION) {
			int x = (int) ev.getX();
			int y = (int) ev.getY();
			switch (ev.getAction()) {
			case MotionEvent.ACTION_MOVE:
				if(!isCountXY) {
					xtox = x-mLastX;
				    ytoy = y-mLastY;
				 //   isCountXY= true;
				}
				onDrag(x, y);
				if(!isMoving )
				    OnMove(x,y);			
				break;
			case MotionEvent.ACTION_UP:
				stopDrag();
				onDrop(x, y);
				break;
			}
		}
		return super.onTouchEvent(ev);
	}

	private void startDrag(Bitmap bm, int x, int y,float imagex, float imagey) {
		stopDrag();

		windowParams = new WindowManager.LayoutParams();
		windowParams.gravity = Gravity.TOP | Gravity.LEFT;
		
		offsetX = (int) (x-imagex);
		offsetY =(int) (y-imagey);
		
		windowParams.x =x- offsetX ;
		windowParams.y = y-offsetY;
		windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		windowParams.alpha = 0.8f;

		ImageView iv = new ImageView(getContext());
		iv.setImageBitmap(bm);
		windowManager = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
		windowManager.addView(iv, windowParams);
		dragImageView = iv;
	}
	
	public  void OnMove(int x, int y){
		int TempPosition = pointToPosition(x,y);
		int sOffsetY = specialItemY == -1 ? y - mLastY : y - specialItemY - halfItemWidth;
		int lOffsetY = leftBtmItemY == -1 ? y - mLastY : y - leftBtmItemY - halfItemWidth;
		if(TempPosition != AdapterView.INVALID_POSITION && TempPosition != dragPosition){
			dropPosition = TempPosition;
		}else if(specialPosition != -1 && dragPosition == specialPosition && sOffsetY >= halfItemWidth){
			dropPosition = (itemTotalCount - 1);
		}else if(leftBottomPosition != -1 && dragPosition == leftBottomPosition && lOffsetY >= halfItemWidth){
			dropPosition = (itemTotalCount - 1);
		}	
		if(dragPosition != startPosition)
			dragPosition = startPosition;
		int MoveNum = dropPosition - dragPosition;
		if(dragPosition != startPosition && dragPosition == dropPosition)
			MoveNum = 0;
		if(MoveNum != 0){
			int itemMoveNum = Math.abs(MoveNum);
			float Xoffset,Yoffset;
			for (int i = 0;i < itemMoveNum;i++){
			if(MoveNum > 0){
				holdPosition = dragPosition + 1;
				Xoffset = (dragPosition/nColumns == holdPosition/nColumns) ? (-1) : (nColumns -1);
				Yoffset = (dragPosition/nColumns == holdPosition/nColumns) ? 0 : (-1);
			}else{
				holdPosition = dragPosition - 1;
				Xoffset = (dragPosition/nColumns == holdPosition/nColumns) ? 1 : (-(nColumns-1));
				Yoffset = (dragPosition/nColumns == holdPosition/nColumns) ? 0 : 1;
			}
		    ViewGroup moveView = (ViewGroup)getChildAt(holdPosition);				
			Animation animation = getMoveAnimation(Xoffset,Yoffset);
			moveView.startAnimation(animation);
			dragPosition = holdPosition;
			if(dragPosition == dropPosition)
				LastAnimationID = animation.toString();
			final PreViewDateAdapter adapter = (PreViewDateAdapter)this.getAdapter();
			animation.setAnimationListener(new Animation.AnimationListener() {
				
				public void onAnimationStart(Animation animation) {
						// TODO Auto-generated method stub
					isMoving = true;
				}
				public void onAnimationRepeat(Animation animation) {
						// TODO Auto-generated method stub
				}
				public void onAnimationEnd(Animation animation) {
						// TODO Auto-generated method stub
					String animaionID = animation.toString();
					if(animaionID.equalsIgnoreCase(LastAnimationID)){
						adapter.exchange(startPosition, dropPosition);
						startPosition = dropPosition;
						isMoving = false;
					}					
				}
			});	
		  }
	   }
	}
	
	private void onDrop(int x,int y){
	    final PreViewDateAdapter adapter = (PreViewDateAdapter) this.getAdapter();
		adapter.showDropItem(true);
		adapter.notifyDataSetChanged();	
		adapter.notifyScreenSetChanged();
		
	}

	private void onDrag(int x, int y) {
		if (dragImageView != null) {
			windowParams.alpha = 0.8f;
			windowParams.x =x- offsetX ;
			windowParams.y = y-offsetY;
			windowManager.updateViewLayout(dragImageView, windowParams);
		}
	}
	
	private void stopDrag() {
		if (dragImageView != null) {
			windowManager.removeView(dragImageView);
			dragImageView = null;
		}
	}
	
	private void hideDropItem(){
		final PreViewDateAdapter adapter = (PreViewDateAdapter)this.getAdapter();
		adapter.showDropItem(false);
	}
	
	public Animation getMoveAnimation(float x,float y){
		TranslateAnimation go = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, x, 
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, y);
		go.setFillAfter(true);
		go.setDuration(300);	
		return go;
	}


}















