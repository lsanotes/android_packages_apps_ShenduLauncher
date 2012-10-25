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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shendu.launcher.DropTarget.DragObject;
import com.shendu.launcher.FolderInfo.FolderListener;
import com.shendu.launcher.Workspace.FolderStyle;
import com.shendu.launcher.preference.PreferencesProvider;

import java.util.ArrayList;

/**
 * An icon that can appear on in the workspace representing an {@link UserFolder}.
 */
public class FolderIcon extends LinearLayout implements FolderListener {
    private Launcher mLauncher;
    Folder mFolder;
    FolderInfo mInfo;
    private static boolean sStaticValuesDirty = true;

    // The number of icons to display in the
    private static final int NUM_ITEMS_IN_PREVIEW = 3;
    private static final int CONSUMPTION_ANIMATION_DURATION = 100;
    private static final int DROP_IN_ANIMATION_DURATION = 400;
    private static final int INITIAL_ITEM_ANIMATION_DURATION = 350;

    // The degree to which the inner ring grows when accepting drop
    private static final float INNER_RING_GROWTH_FACTOR = 0.15f;

    // The degree to which the outer ring is scaled in its natural state
    private static final float OUTER_RING_GROWTH_FACTOR = 0.3f;

    // The amount of vertical spread between items in the stack [0...1]
    private static final float PERSPECTIVE_SHIFT_FACTOR = 0.24f;

    // The degree to which the item in the back of the stack is scaled [0...1]
    // (0 means it's not scaled at all, 1 means it's scaled to nothing)
    private static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;

    public static Drawable sSharedFolderLeaveBehind = null;

    private ImageView mPreviewBackground;
    private BubbleTextView mFolderName;

    FolderRingAnimator mFolderRingAnimator = null;

    // These variables are all associated with the drawing of the preview; they are stored
    // as member variables for shared usage and to avoid computation on each frame
    private int mIntrinsicIconSize;
    private float mBaselineIconScale;
    private int mBaselineIconSize;
    private int mAvailableSpaceInPreview;
    private int mTotalWidth = -1;
    private int mPreviewOffsetX;
    private int mPreviewOffsetY;
    private float mMaxPerspectiveShift;
    boolean mAnimating = false;
    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    private PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0, 0, 0, 0);

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderIcon(Context context) {
        super(context);
    }

    public boolean isDropEnabled() {
        final ViewGroup cellLayoutChildren = (ViewGroup) getParent();
        final ViewGroup cellLayout = (ViewGroup) cellLayoutChildren.getParent();
        final Workspace workspace = (Workspace) cellLayout.getParent();
        return !workspace.isSmall();
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            FolderInfo folderInfo, IconCache iconCache) {

        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);

        icon.mFolderName = (BubbleTextView) icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.title);
        icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.preview_background);

        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        //Log.i("hhl", "****FolderIcon.java...fromXml()==="+folderInfo.contents.size()+"==="+folderInfo.title);
        Workspace.FolderStyle folderStyle = launcher.getWorkspace().getFolderStyle();
        if(folderStyle == Workspace.FolderStyle.Square){
            icon.mPreviewBackground.setImageDrawable(icon.shenduCreateFolderThumBitmap(folderInfo));
        }
        icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format),
                folderInfo.title));
        Folder folder = Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(icon);
        folder.bind(folderInfo);
        icon.mFolder = folder;

        icon.mFolderRingAnimator = new FolderRingAnimator(launcher, icon);
        folderInfo.addListener(icon);

        return icon;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public static class FolderRingAnimator {
        public int mCellX;
        public int mCellY;
        private CellLayout mCellLayout;
        public float mOuterRingSize;
        public float mInnerRingSize;
        public FolderIcon mFolderIcon = null;
        //public Drawable mOuterRingDrawable = null;
        //public Drawable mInnerRingDrawable = null;
        public static Drawable sSharedOuterRingDrawable = null;
        public static Drawable sSharedInnerRingDrawable = null;
        public static int sPreviewSize = -1;
        public static int sPreviewPadding = -1;

        private ValueAnimator mAcceptAnimator;
        private ValueAnimator mNeutralAnimator;

        public FolderRingAnimator(Launcher launcher, FolderIcon folderIcon) {
            mFolderIcon = folderIcon;
            Resources res = launcher.getResources();
            //mOuterRingDrawable = res.getDrawable(R.drawable.portal_ring_outer_holo);
            //mInnerRingDrawable = res.getDrawable(R.drawable.portal_ring_inner_holo);

            // We need to reload the static values when configuration changes in case they are
            // different in another configuration
            if (sStaticValuesDirty) {
                sPreviewSize = res.getDimensionPixelSize(R.dimen.app_icon_size);
                //sPreviewSize = res.getDimensionPixelSize(R.dimen.folder_preview_size);
                sPreviewPadding = res.getDimensionPixelSize(R.dimen.folder_preview_padding);
                Workspace.FolderStyle folderStyle = launcher.getWorkspace().getFolderStyle();
                if(folderStyle == Workspace.FolderStyle.Ring){
                    sSharedOuterRingDrawable = res.getDrawable(R.drawable.portal_ring_outer_holo);
                	//sSharedInnerRingDrawable = res.getDrawable(R.drawable.portal_ring_inner_holo);
                }else{
                    sSharedOuterRingDrawable = res.getDrawable(R.drawable.portal_square_outer_holo);
                	//sSharedInnerRingDrawable = res.getDrawable(R.drawable.portal_square_inner_holo);
                }
                sSharedFolderLeaveBehind = res.getDrawable(R.drawable.portal_ring_rest);
                sStaticValuesDirty = false;
            }
        }

        public void animateToAcceptState() {
            if (mNeutralAnimator != null) {
                mNeutralAnimator.cancel();
            }
            mAcceptAnimator = ValueAnimator.ofFloat(0f, 1f);
            mAcceptAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);
            mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + percent * OUTER_RING_GROWTH_FACTOR) * sPreviewSize;
                    mInnerRingSize = (1 + percent * INNER_RING_GROWTH_FACTOR) * sPreviewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                	//Log.i("hhl", ">>>>>>>>>>>>>>>>FolderIcon.java...animateToAcceptState==="+(mFolderIcon != null));
                    if (mFolderIcon != null) {
                        //mFolderIcon.mPreviewBackground.setVisibility(View.INVISIBLE);
                    }
                }
            });
            mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (mAcceptAnimator != null) {
                mAcceptAnimator.cancel();
            }
            mNeutralAnimator = ValueAnimator.ofFloat(0f, 1f);
            mNeutralAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);
            mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + (1 - percent) * OUTER_RING_GROWTH_FACTOR) * sPreviewSize;
                    mInnerRingSize = (1 + (1 - percent) * INNER_RING_GROWTH_FACTOR) * sPreviewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mCellLayout != null) {
                        mCellLayout.hideFolderAccept(FolderRingAnimator.this);
                    }
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(VISIBLE);
                    }
                }
            });
            mNeutralAnimator.start();
        }

        // Location is expressed in window coordinates
        public void getCell(int[] loc) {
            loc[0] = mCellX;
            loc[1] = mCellY;
        }

        // Location is expressed in window coordinates
        public void setCell(int x, int y) {
            mCellX = x;
            mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            mCellLayout = layout;
        }

        public float getOuterRingSize() {
            return mOuterRingSize;
        }

        public float getInnerRingSize() {
            return mInnerRingSize;
        }
    }

    private boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        int itemCount = 0;
        boolean resultFlag = false;
        switch(itemType){
	        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
	        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
	        case LauncherSettings.Favorites.ITEM_TYPE_DELETESHOETCUT:
	        	itemCount = 1;
	        	break;
	        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
	        	itemCount = ((FolderInfo)item).contents.size();
	        	break;
	        default:break;
        }
       
        if(itemCount>0 && !mFolder.isFull(itemCount) && item != mInfo && !mInfo.opened){
        	resultFlag = true;
        }
       // return resultFlag;
       return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DELETESHOETCUT) &&
                resultFlag);
    }

    public boolean acceptDrop(Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
    	//Log.i("hhl", "*************FolderIcon.java....acceptDrop==="+getClass().getName()+"===="+item);
        return willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        //Log.i("hhl", "^^^FolderIcon.java...addItem()....."+item.title+"=="+item.itemType);
        mInfo.add(item);
        Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        if(folderStyle == Workspace.FolderStyle.Square){
            mFolder.mFolderIcon.mPreviewBackground.setImageDrawable(shenduCreateFolderThumBitmap(mInfo));
        }
        LauncherModel.addOrMoveItemInDatabase(mLauncher, item, mInfo.id, 0, item.cellX, item.cellY);
    }

    public void onDragEnter(Object dragInfo) {
    	//Log.i("hhl", "FolderIcon.java....onDragEnter==111==="+dragInfo+"==="+mFolder);
    	if (!willAcceptItem((ItemInfo) dragInfo)) return;
    	shenduDisplayFolderBg(mFolder.mFolderIcon.mPreviewBackground,false);
        //Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        //if(folderStyle == Workspace.FolderStyle.Ring){
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
            CellLayout layout = (CellLayout) getParent().getParent();
            mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
            mFolderRingAnimator.setCellLayout(layout);
            mFolderRingAnimator.animateToAcceptState();
            layout.showFolderAccept(mFolderRingAnimator);
        //}
    }

    public void onDragOver(Object dragInfo) {
    }

    public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
            final ShortcutInfo srcInfo, final View srcView, Rect dstRect,
            float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {

        //Drawable animateDrawable = ((TextView) destView).getCompoundDrawables()[1];
        //Drawable animateDrawable = ((ImageView)destView.findViewById(R.id.app_shortcutinfo_icon_id)).getDrawable();
        Drawable animateDrawable = ((TextView)destView.findViewById(R.id.app_shortcutinfo_icon_id)).getBackground();
    	//Log.i("hhl", "^^^^^^^FolderIcon.java...performCreateAnimation before computer 333===");
    	
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), destView.getMeasuredWidth());

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable);

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION);

        postDelayed(new Runnable() {
            public void run() {
                addItem(destInfo);
            }
        }, INITIAL_ITEM_ANIMATION_DURATION);
    }

    public void onDragExit(Object dragInfo) {
    	//Log.i("hhl", "FolderIcon.java...onDragExit====="+dragInfo.toString()+"==="+
    			//(willAcceptItem((ItemInfo) dragInfo)));
        if (!willAcceptItem((ItemInfo) dragInfo)) return;
        //Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        //if(folderStyle == Workspace.FolderStyle.Ring){
            mFolderRingAnimator.animateToNaturalState();
        //}
        shenduDisplayFolderBg(mFolder.mFolderIcon.mPreviewBackground,true);
    }
    
    public Drawable shenduCreateFolderThumBitmap(FolderInfo folderInfo){
    	
    	//Log.i("hhl", "+++++FolderIcon.java...shenduCreateFolderThumBitmap==="+folderInfo.contents.size());
    	//final Resources resource = mLauncher.getResources();
		//Bitmap oldBitmap = BitmapFactory.decodeResource(resource,R.drawable.folder_bg);
    	int folderSize = (int)mLauncher.getResources().getDimension(R.dimen.app_icon_size);
		Bitmap oldBitmap = Bitmap.createBitmap(folderSize,folderSize,Config.ARGB_8888);
		Bitmap mutableBitmap = Bitmap.createScaledBitmap(
								oldBitmap,
								(oldBitmap.getWidth())*3/2,
								(oldBitmap.getHeight())*3/2, 
								false); 
		int count = folderInfo.contents.size();
		count = count>4 ? 4:count;
		Canvas canvas = new Canvas(mutableBitmap); 
		for(int i=0; i<count; i++) {
			ItemInfo info = folderInfo.contents.get(i); 
			Bitmap orgbmp=null;
			if(folderInfo.contents.get(i) instanceof ShortcutInfo){
				orgbmp = ((ShortcutInfo)info).iconBitmap; 
				if(orgbmp==null){
					orgbmp = ((ShortcutInfo)info).mIcon;
				}
			}else{
				orgbmp=((ApplicationInfo)info).iconBitmap;
			}
			int oldWidth  = orgbmp.getWidth();
			int oldHeight = orgbmp.getHeight();
			int newWidth  = 50;
			int newHeight = 50;
			float scaleW  = ((float)newWidth) / oldWidth;
			float scaleH  = ((float)newHeight) / oldHeight;
			Matrix matrix = new Matrix();
			matrix.postScale(scaleW, scaleH); 
			Bitmap thumbmp = Bitmap.createBitmap(orgbmp,0,0,oldWidth,oldHeight,matrix,true); 
			if (i == 3)
				canvas.drawBitmap(thumbmp, 57, 57, null);
			if (i == 2)
				canvas.drawBitmap(thumbmp, 7, 57, null);
			if (i == 1)
				canvas.drawBitmap(thumbmp, 57, 7, null);
			if (i == 0)
				canvas.drawBitmap(thumbmp, 7, 7, null);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			canvas.restore();
		}
		BitmapDrawable closeIcon = new BitmapDrawable(mutableBitmap); 
		return closeIcon; 
    }

    private void onDrop(final ShortcutInfo item, View animateView, Rect finalRect,
            float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable) {
        item.cellX = -1;
        item.cellY = -1;
        Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        
    	//Log.i("hhl", "FolderIcon.java...onDrop 6====="+item.title+"==="+(animateView != null)+"==="+(finalRect==null));
        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null && (folderStyle == Workspace.FolderStyle.Ring)) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                // Set cellLayout and this to it's final state to compute final animation locations
                workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform((CellLayout) getParent().getParent());
            }

            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, center);
            center[0] = Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                    center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < NUM_ITEMS_IN_PREVIEW ? 0.5f : 0f;

            dragLayer.animateView(animateView, from, to, finalAlpha,
                    scale * scaleRelativeToDragLayer, DROP_IN_ANIMATION_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    postAnimationRunnable, false);
            postDelayed(new Runnable() {
                public void run() {
                    addItem(item);
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }

    public void onDrop(DragObject d) {
        ShortcutInfo item = null;
    	//Log.i("hhl", "FolderIcon.java...onDrop 1====="+getClass().getName()+"==="+d.dragInfo);
    	
    	
        if (d.dragInfo instanceof ApplicationInfo) {
            // Came from all apps -- make a copy
            item = ((ApplicationInfo) d.dragInfo).makeShortcut();
        } else if (d.dragInfo instanceof FolderInfo) {
            FolderInfo folder = (FolderInfo) d.dragInfo;
            mFolder.notifyDrop();
            for (ShortcutInfo fItem : folder.contents) {
                onDrop(fItem, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable);
            }
            mLauncher.removeFolder(folder);
            LauncherModel.deleteItemFromDatabase(mLauncher, folder);
            return;
        } else {
        
        		   item = (ShortcutInfo) d.dragInfo;
        
        }
        mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable);
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }

    private void computePreviewDrawingParams(int drawableSize, int totalSize) {
    	//Log.i("hhl", "^^^^^^^FolderIcon.java...computePreviewDrawingParams two===");
    	Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        if(folderStyle == Workspace.FolderStyle.Ring){
        	if (mIntrinsicIconSize != drawableSize || mTotalWidth != totalSize) {
                mIntrinsicIconSize = drawableSize;
                mTotalWidth = totalSize;

                final int previewSize = FolderRingAnimator.sPreviewSize;
                final int previewPadding = FolderRingAnimator.sPreviewPadding;

                mAvailableSpaceInPreview = (previewSize - 2 * previewPadding);
                // cos(45) = 0.707  + ~= 0.1) = 0.8f
                int adjustedAvailableSpace = (int) ((mAvailableSpaceInPreview / 2) * (1 + 0.8f));

                int unscaledHeight = (int) (mIntrinsicIconSize * (1 + PERSPECTIVE_SHIFT_FACTOR));
                mBaselineIconScale = (1.0f * adjustedAvailableSpace / unscaledHeight);

                mBaselineIconSize = (int) (mIntrinsicIconSize * mBaselineIconScale);
                mMaxPerspectiveShift = mBaselineIconSize * PERSPECTIVE_SHIFT_FACTOR;

                mPreviewOffsetX = (mTotalWidth - mAvailableSpaceInPreview) / 2;
                mPreviewOffsetY = previewPadding;
            }
        }
    }

    private void computePreviewDrawingParams(Drawable d) {
    	//Log.i("hhl", "^^^^^^^FolderIcon.java...computePreviewDrawingParams one===");
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    class PreviewItemDrawingParams {
        PreviewItemDrawingParams(float transX, float transY, float scale, int overlayAlpha) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
            this.overlayAlpha = overlayAlpha;
        }
        float transX;
        float transY;
        float scale;
        int overlayAlpha;
        Drawable drawable;
    }

    private float getLocalCenterForIndex(int index, int[] center) {
        mParams = computePreviewItemDrawingParams(Math.min(NUM_ITEMS_IN_PREVIEW, index), mParams);

        mParams.transX += mPreviewOffsetX;
        mParams.transY += mPreviewOffsetY;
        float offsetX = mParams.transX + (mParams.scale * mIntrinsicIconSize) / 2;
        float offsetY = mParams.transY + (mParams.scale * mIntrinsicIconSize) / 2;

        center[0] = Math.round(offsetX);
        center[1] = Math.round(offsetY);
        return mParams.scale;
    }

    private PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
            PreviewItemDrawingParams params) {
        index = NUM_ITEMS_IN_PREVIEW - index - 1;
        float r = (index * 1.0f) / (NUM_ITEMS_IN_PREVIEW - 1);
        float scale = (1 - PERSPECTIVE_SCALE_FACTOR * (1 - r));

        float offset = (1 - r) * mMaxPerspectiveShift;
        float scaledSize = scale * mBaselineIconSize;
        float scaleOffsetCorrection = (1 - scale) * mBaselineIconSize;

        // We want to imagine our coordinates from the bottom left, growing up and to the
        // right. This is natural for the x-axis, but for the y-axis, we have to invert things.
        float transY = mAvailableSpaceInPreview - (offset + scaledSize + scaleOffsetCorrection);
        float transX = offset + scaleOffsetCorrection;
        float totalScale = mBaselineIconScale * scale;
        final int overlayAlpha = (int) (80 * (1 - r));

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        } else {
            params.transX = transX;
            params.transY = transY;
            params.scale = totalScale;
            params.overlayAlpha = overlayAlpha;
        }
        return params;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY+15);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;

        if (d != null) {
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            d.setFilterBitmap(true);
            d.setColorFilter(Color.argb(params.overlayAlpha, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            d.draw(canvas);
            d.clearColorFilter();
            d.setFilterBitmap(false);
        }
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mFolder == null) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        ArrayList<View> items = mFolder.getItemsInReadingOrder(false);
        
        TextView app_icon;
        
        Drawable d;
        //TextView v;

        // Update our drawing parameters if necessary
        //FolderStyle folderStyle = PreferencesProvider.Interface.Homescreen.getScreenFolderStyle(getContext(), 
        		//getResources().getString(R.string.config_folder_style_default));
        Workspace.FolderStyle folderStyle = mLauncher.getWorkspace().getFolderStyle();
        //Log.i("hhl", "&&&&FolderIcon.java...dispatchDraw().before computer 111.."+mAnimating+"==="+
        		//(mFolder.mInfo==null)+"==="+folderStyle);
        //if(folderStyle == Workspace.FolderStyle.Ring){
        	if (mAnimating) {
                computePreviewDrawingParams(mAnimParams.drawable);
            } else {
            	//app_icon = (ImageView)items.get(0).findViewById(R.id.app_shortcutinfo_icon_id);
            	app_icon = (TextView)items.get(0).findViewById(R.id.app_shortcutinfo_icon_id);
            	//d = app_icon.getDrawable();
            	d = app_icon.getBackground();
                //v = (TextView) items.get(0);
                //d = v.getCompoundDrawables()[1];
                computePreviewDrawingParams(d);
            }
        //}else
        	if(folderStyle == Workspace.FolderStyle.Square){
        	mFolder.mFolderIcon.mPreviewBackground.setImageDrawable(shenduCreateFolderThumBitmap(mFolder.mInfo));
        }
        
        int nItemsInPreview = Math.min(items.size(), NUM_ITEMS_IN_PREVIEW);
        if (!mAnimating) {
            for (int i = nItemsInPreview - 1; i >= 0; i--) {
                //v = (TextView) items.get(i);
                //d = v.getCompoundDrawables()[1];
            	//Log.i("hhl", "===FolderIcon.java==drawPreviewItem==="+mIntrinsicIconSize+"**"+i);
            	app_icon = (TextView)items.get(i).findViewById(R.id.app_shortcutinfo_icon_id);
            	d = app_icon.getBackground();
                mParams = computePreviewItemDrawingParams(i, mParams);
                mParams.drawable = d;
                drawPreviewItem(canvas, mParams);
            }
        } else {
            drawPreviewItem(canvas, mAnimParams);
        }
    }

    private void animateFirstItem(final Drawable d, int duration) {
    	//Log.i("hhl", "^^^^^^^FolderIcon.java...animateFirstItem before computer 222===");
        computePreviewDrawingParams(d);
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);

        final float scale0 = 1.0f;
        final float transX0 = (mAvailableSpaceInPreview - d.getIntrinsicWidth()) / 2;
        final float transY0 = (mAvailableSpaceInPreview - d.getIntrinsicHeight()) / 2;
        mAnimParams.drawable = d;

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1.0f);
        va.addUpdateListener(new AnimatorUpdateListener(){
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (Float) animation.getAnimatedValue();

                mAnimParams.transX = transX0 + progress * (finalParams.transX - transX0);
                mAnimParams.transY = transY0 + progress * (finalParams.transY - transY0);
                mAnimParams.scale = scale0 + progress * (finalParams.scale - scale0);
                invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
            }
        });
        va.setDuration(duration);
        va.start();
    }

    public void setTextVisible(boolean visible) {
        if (visible) {
            mFolderName.setVisibility(VISIBLE);
        } else {
            mFolderName.setVisibility(INVISIBLE);
        }
    }

    public boolean getTextVisible() {
        return mFolderName.getVisibility() == VISIBLE;
    }

    public void onItemsChanged() {
        invalidate();
        requestLayout();
    }

    public void onAdd(ShortcutInfo item) {
        invalidate();
        requestLayout();
    }

    public void onRemove(ShortcutInfo item) {
        invalidate();
        requestLayout();
    }

    public void onTitleChanged(CharSequence title) {
        mFolderName.setText(title.toString());
        setContentDescription(String.format(mContext.getString(R.string.folder_name_format),
                title));
    }
    
    /**
     * 2012-9-26 hhl
     * @param imageView: need to dispaly or hide background object
     * @param flag: dispaly or hide mark
     * TODO: used to dispaly or hide the object background
     */
    private void shenduDisplayFolderBg(ImageView imageView,boolean flag){
    	if(imageView!=null){
        	if(flag){
        		imageView.setBackgroundDrawable(mLauncher.getResources().getDrawable(R.drawable.folder_bg));
        	}else{
        		imageView.setBackgroundDrawable(null);
        	}
    	}
    }
}