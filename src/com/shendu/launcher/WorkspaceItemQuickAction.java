package com.shendu.launcher;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.shendu.launcher.R;

/**
 * 2012-12-27 hhl
 * WorkspaceItemQuickAction.java
 * CM10_Launcher
 * TODO: A PopupWindow of Change Icon、Change Name、Delete、Uninstall for Long Click the cellInfo 
 */
public class WorkspaceItemQuickAction {
	
	private String TAG = "WorkspaceItemQuickAction";
	private LayoutInflater mLayoutInflater;
	private PopupWindow mPopupWindow;
	private View mPopupWindowView;
	private WindowManager mWindowManager;
	private OnActionItemClickListener mItemClickListener;
	private TextView mArrowUp,mArrowDown;
	private RelativeLayout mArrowParent;
	private int mItemChild;
	private ViewGroup mTrack;
	private Drawable mDrawableUp,mDrawableDown;

	public interface OnActionItemClickListener {
		public abstract void onItemClick(int child,int flag);
	}
	
	public WorkspaceItemQuickAction(Context context) {
		
		mPopupWindow = new PopupWindow(context);
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		mPopupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		mPopupWindow.setTouchable(true);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setAnimationStyle(R.style.QuickActionAnimation);
		mPopupWindow.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					mPopupWindow.dismiss();
					return true;
				}
				return false;
			}
		});
		
		mDrawableDown = context.getResources().getDrawable(R.drawable.quick_action_arrow_down);
		mDrawableUp = context.getResources().getDrawable(R.drawable.quick_action_arrow_up);
		
		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mItemChild = 0;

		mPopupWindowView = (ViewGroup) mLayoutInflater.inflate(R.layout.workspace_quick_action_layout, null);
		mArrowParent = (RelativeLayout) mPopupWindowView.findViewById(R.id.quick_action_layout_arrow_parent_id);
		mTrack = (ViewGroup) mPopupWindowView.findViewById(R.id.quick_action_layout_tracks_id);
		mArrowDown = (TextView) mPopupWindowView.findViewById(R.id.quick_action_layout_arrow_down_id);
		mArrowUp = (TextView) mPopupWindowView.findViewById(R.id.quick_action_layout_arrow_up_id);
		mPopupWindow.setContentView(mPopupWindowView);
	}
	
	/**
	 * TODO: add menu for the workspace item popup 
	 */
	public void addActionItem(final ShenduPrograme actionItem) {
		final int child = mItemChild;
		String title = actionItem.getName();
		int iconResId = actionItem.getIconResId();
		View container = (View) mLayoutInflater.inflate(R.layout.workspace_quick_action_item, null);
		ImageView img = (ImageView) container.findViewById(R.id.quick_action_item_icon_id);
		TextView text = (TextView) container.findViewById(R.id.quick_action_item_name_id);

		if (iconResId != 0) img.setImageResource(iconResId);
		else img.setVisibility(View.GONE);
		if (title != null) text.setText(title);
		else text.setVisibility(View.GONE);

		container.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mItemClickListener != null)
					mItemClickListener.onItemClick(child,actionItem.getActionOP());
			}
		});
		container.setFocusable(true);
		container.setClickable(true);
		mTrack.addView(container, mItemChild);
		mItemChild ++;
	}

	public void removeActionItem(){
		mTrack.removeViews(0, mTrack.getChildCount());
		mItemChild = 0;
	}

	public void setOnActionItemClickListener(OnActionItemClickListener itemClickListener) {
		mItemClickListener = itemClickListener;
	}
	
	/**
	 * TODO: show the workspace item popup menu
	 */
	public void show(boolean hotSeatOne,boolean widgetFlag,boolean hotSeatFlag,View view,int cellX, int cellY,int spanY) {
		mPopupWindow.setContentView(mPopupWindowView);
		
		int viewWidth = view.getWidth();
		int viewHeight = view.getHeight();
		Point screenPoint = new Point();
		mWindowManager.getDefaultDisplay().getSize(screenPoint);
		int screenWidth = screenPoint.x;
		int screenHeight = screenPoint.y;
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		Rect anchorRect = new Rect(location[0],location[1],location[0]+viewWidth,location[1]+viewHeight);
		
		int upGravity,leftGravity;
		boolean upFlag,leftFlag;
		int xPos;
		int yPos;
		if(!hotSeatFlag && cellY<1){
			upFlag = true;
			if(widgetFlag){
				yPos = anchorRect.bottom-viewHeight/2;
			}else{
				yPos = anchorRect.bottom;
			}
			upGravity = Gravity.TOP;
		}else{
			upFlag = false;
			if(widgetFlag){
				yPos = screenHeight-anchorRect.top-viewHeight/2;
			}else{
				yPos = screenHeight-anchorRect.top;
			}
			upGravity = Gravity.BOTTOM;
		}
		if(cellX>1){
			leftFlag = false;
			leftGravity = Gravity.RIGHT;
			if(widgetFlag){
				xPos = screenWidth - anchorRect.right+viewWidth/2;
			}else{
				xPos = screenWidth - anchorRect.right;
			}
		}else{
			leftFlag = true;
			leftGravity = Gravity.LEFT;
			if(widgetFlag){
				xPos = anchorRect.left+viewWidth/2;
			}else{
				xPos = anchorRect.left;
			}
		}
		if(hotSeatOne){
			xPos = screenWidth/2-2*mDrawableDown.getIntrinsicWidth();
			viewWidth = 4*mDrawableDown.getIntrinsicWidth();
		}
		showArrow(widgetFlag,upFlag,leftFlag,viewWidth);
		mPopupWindow.showAtLocation(view,leftGravity|upGravity, xPos, yPos);
	}

	public void dismiss(){
		if(mPopupWindow!=null && mPopupWindow.isShowing()){
			mPopupWindow.dismiss();
		}
	}
	
	/**
	 * @param upFlag: the arrow display up or down
	 * @param leftFlag: the arrow display left or right
	 * @param requestedX: the arrow margin size
	 * TODO: show the up or down arrow view
	 */
	private void showArrow(boolean widgetFlag,boolean upFlag,boolean leftFlag,int requestedX) {
		int halfofArrowW = upFlag?mDrawableDown.getIntrinsicWidth()/2:mDrawableUp.getIntrinsicWidth()/2;
		TextView showArrowView = upFlag?mArrowUp:mArrowDown;
		TextView hideArrowView = upFlag?mArrowDown:mArrowUp;
		RelativeLayout.LayoutParams showParam = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams hideParam = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		hideParam.leftMargin = 0;
		hideParam.rightMargin = 0;
		hideParam.addRule(RelativeLayout.ALIGN_LEFT,0);
		hideParam.addRule(RelativeLayout.ALIGN_RIGHT,0);
		hideParam.addRule(RelativeLayout.BELOW,0);
		if (leftFlag) {
			if(widgetFlag){
				showParam.leftMargin = halfofArrowW+10;
			}else{
				showParam.leftMargin = requestedX / 2 - halfofArrowW;
			}
			showParam.addRule(RelativeLayout.ALIGN_LEFT,R.id.quick_action_layout_tracks_id);
		} else {
			if(widgetFlag){
				showParam.rightMargin = halfofArrowW+10;
			}else{
				showParam.rightMargin = requestedX / 2 - halfofArrowW;
			}
			showParam.addRule(RelativeLayout.ALIGN_RIGHT,R.id.quick_action_layout_tracks_id);
		}
		if (!upFlag) {
			showParam.addRule(RelativeLayout.BELOW,R.id.quick_action_layout_tracks_id);
		}
		showArrowView.setLayoutParams(showParam);
		showArrowView.setVisibility(View.VISIBLE);
		hideArrowView.setLayoutParams(hideParam);
		hideArrowView.setVisibility(View.GONE);
	}
	
}
