package com.shendu.launcher;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.Scroller;

public class FolderCoverView extends View {
	private Bitmap  mCurrentPageBitmap;
	private Bitmap mFolderBitmap;
	private int mWidth;
	private int mHeight;
	private int mTop;
	private int mFolderLeft;
	private int mFolderTop;
	private Scroller mScroller;
	private ArrayList<Bitmap> mRecycleBitmaps;
	private int mDeltay;
	public static final int SCROLL_CLOSE_DURATION =300;
	
	int upOrDown;

	/**
	 * @param context
	 * @param currentPageBitmap   
	 * @param top
	 * @param width
	 * @param height
	 * @param currentFolderBitmap 
	 * @param iconLeft  
	 * @param iconTop
	 */
	public FolderCoverView(Context context, Bitmap currentPageBitmap, int top,
			int width, int height, Bitmap currentFolderBitmap, int iconLeft,
			int iconTop, int wallpaperTop,int upOrDown) {
		super(context);
		
		mCurrentPageBitmap = currentPageBitmap;
		mFolderBitmap = currentFolderBitmap;
		mWidth = width;
		mHeight = height;
		mTop = top;
		mScroller = new Scroller(context);
		mFolderLeft = iconLeft;
		mFolderTop = iconTop;
		mRecycleBitmaps = new ArrayList<Bitmap>();
		mRecycleBitmaps.add(currentPageBitmap);
		this.upOrDown =upOrDown;
		
		if(arrow==null){
			 arrow = createArrowBitmap();	
		}
		
		if(upOrDown==-1){
			Matrix matrix = new Matrix();		
			matrix.postRotate(180);
			arrow = Bitmap.createBitmap(arrow, 0, 0, arrow.getWidth(),
					 arrow.getHeight(), matrix, true); 
		}
		
	}
	
	Bitmap arrow;
	
	public void draw(Canvas canvas) {
		canvas.drawBitmap(createCurrentWallpaper(mCurrentPageBitmap), 0, mTop,null);
		if (mFolderBitmap != null) {
			canvas.drawBitmap(mFolderBitmap, mFolderLeft, mFolderTop, null);
			canvas.drawBitmap(arrow, 
					mFolderLeft+ (mFolderBitmap.getWidth()-arrow.getWidth())/ 2,
					upOrDown==1? mHeight- arrow.getHeight()+5:mTop-5 ,null);
		}
	}

	/**
	 * @param startx
	 *            Starting horizontal scroll offset in pixels. Positive numbers
	 *            will scroll the content to the left.
	 * @param starty
	 *            Starting vertical scroll offset in pixels. Positive numbers
	 *            will scroll the content up.
	 * @param deltax
	 *            Horizontal distance to travel. Positive numbers will scroll
	 *            the content to the left.
	 * @param deltay
	 *            Vertical distance to travel. Positive numbers will scroll the
	 *            content up.
	 * @param Duration
	 *            of the scroll in milliseconds.
	 */
	public void slideBy(int startx, int starty, int deltax, int deltay,
			int duration) {
		mDeltay = deltay;
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}

		mScroller.startScroll(startx, starty, deltax, deltay, duration);
		this.invalidate();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			int desY = mScroller.getCurrY();
			this.scrollTo(0, desY);

			postInvalidate();

		} else {
			super.computeScroll();
		}
	}

	public Bitmap createCurrentWallpaper(Bitmap currentPageBitmap) {
		Bitmap coverBg = Bitmap.createBitmap(currentPageBitmap, 0, mTop,mWidth, mHeight);
		return coverBg;
	}

	public Bitmap createArrowBitmap() {
		Bitmap arrow = FolderCoverView.createBitmapThumbnail(
				BitmapFactory.decodeResource(getResources(),
						R.drawable.saf_folder_indicator), (int) getResources()
						.getDimension(R.dimen.saf_folder_cover_arrow_width),
				(int) getResources().getDimension(
						R.dimen.saf_folder_cover_arrow_height));
		
		return arrow;
	}

	public void recycleAll() {
		for (Bitmap garbage : mRecycleBitmaps) {
			garbage.recycle();
		}

		mRecycleBitmaps.clear();
		mRecycleBitmaps = null;
	}

	public int getDeltay() {
		return mDeltay;
	}
	public static Bitmap view2Bitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        

        return bitmap;
    }
	
	
	/**
	 * through the rectangle zooming images
	 * @param bitmap expect being resized original picture
	 * @param width expected width for bitmap
	 * @param height expected height for bitmap
	 * @return 
	 */
	public static Bitmap createBitmapThumbnail(Bitmap bitmap, int width,
            int height) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if (width > 0 && height > 0) {

            final Bitmap.Config c = Bitmap.Config.ARGB_8888;
            final Bitmap thumb = Bitmap.createBitmap(width, height, c);
            final Canvas canvas = new Canvas();
            final Paint paint = new Paint();

            canvas.setBitmap(thumb);
            paint.setDither(false);
            paint.setFilterBitmap(true);
            Rect sBounds = new Rect();
            sBounds.set(0, 0, width, height);
            Rect sOldBounds = new Rect();
            sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
            canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
            
            return thumb;
        }

        return bitmap;
    }
	

}
