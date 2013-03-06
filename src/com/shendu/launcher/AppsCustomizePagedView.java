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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.TableMaskFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.shendu.launcher.R;
import com.shendu.launcher.DropTarget.DragObject;
import com.shendu.launcher.Workspace.State;
import com.shendu.launcher.Workspace.TransitionEffect;
import com.shendu.launcher.preference.PreferencesProvider;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.lang.ref.WeakReference;

/**
 * A simple callback interface which also provides the results of the task.
 */
interface AsyncTaskCallback {
    void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data);
}

/**
 * The data needed to perform either of the custom AsyncTasks.
 */
class AsyncTaskPageData {
    enum Type {
        LoadWidgetPreviewData
    }

    AsyncTaskPageData(int p, ArrayList<Object> l, ArrayList<Bitmap> si, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        sourceImages = si;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = maxImageHeight = -1;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    AsyncTaskPageData(int p, ArrayList<Object> l, int cw, int ch, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (sourceImages != null) {
            if (cancelled) {
                for (Bitmap b : sourceImages) {
                    b.recycle();
                }
            }
            sourceImages.clear();
        }
        if (generatedImages != null) {
            if (cancelled) {
                for (Bitmap b : generatedImages) {
                    b.recycle();
                }
            }
            generatedImages.clear();
        }
    }
    int page;
    ArrayList<Object> items;
    ArrayList<Bitmap> sourceImages;
    ArrayList<Bitmap> generatedImages;
    int maxImageWidth;
    int maxImageHeight;
    AsyncTaskCallback doInBackgroundCallback;
    AsyncTaskCallback postExecuteCallback;
}

/**
 * A generic template for an async task used in AppsCustomize.
 */
class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AppsCustomizeAsyncTask(int p, AppsCustomizePagedView.ContentType t, AsyncTaskPageData.Type ty) {
        page = p;
        pageContentType = t;
        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
        dataType = ty;
    }
    @Override
    protected AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1) return null;
        // Load each of the widget previews in the background
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }
    @Override
    protected void onPostExecute(AsyncTaskPageData result) {
        // All the widget previews are loaded, so we can just callback to inflate the page
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        threadPriority = p;
    }
    void syncThreadPriority() {
        Process.setThreadPriority(threadPriority);
    }

    // The page that this async task is associated with
    AsyncTaskPageData.Type dataType;
    int page;
    AppsCustomizePagedView.ContentType pageContentType;
    int threadPriority;

}

abstract class WeakReferenceThreadLocal<T> {
    private ThreadLocal<WeakReference<T>> mThreadLocal;
    public WeakReferenceThreadLocal() {
        mThreadLocal = new ThreadLocal<WeakReference<T>>();
    }

    abstract T initialValue();

    public void set(T t) {
        mThreadLocal.set(new WeakReference<T>(t));
    }

    public T get() {
        WeakReference<T> reference = mThreadLocal.get();
        T obj;
        if (reference == null) {
            obj = initialValue();
            mThreadLocal.set(new WeakReference<T>(obj));
            return obj;
        } else {
            obj = reference.get();
            if (obj == null) {
                obj = initialValue();
                mThreadLocal.set(new WeakReference<T>(obj));
            }
            return obj;
        }
    }
}

class CanvasCache extends WeakReferenceThreadLocal<Canvas> {
    @Override
    protected Canvas initialValue() {
        return new Canvas();
    }
}

class PaintCache extends WeakReferenceThreadLocal<Paint> {
    @Override
    protected Paint initialValue() {
        return null;
    }
}

class BitmapCache extends WeakReferenceThreadLocal<Bitmap> {
    @Override
    protected Bitmap initialValue() {
        return null;
    }
}

class RectCache extends WeakReferenceThreadLocal<Rect> {
    @Override
    protected Rect initialValue() {
        return new Rect();
    }
}

/**
 * The Apps/Customize page that displays all the applications, widgets, and shortcuts.
 * PagedViewIcon.PressedCallback, 
 */
public class AppsCustomizePagedView extends PagedViewWithDraggableItems implements
        AllAppsView, View.OnClickListener, View.OnKeyListener, DragSource,
        DropTarget ,DragController.DragListener,
        PagedViewWidget.ShortPressListener, LauncherTransitionable {
    static final String TAG = "AppsCustomizePagedView";

    /**
     * The different content types that this paged view can show.
     */
    public enum ContentType {
        //Applications,
        Widgets,
        Wallpapers,
        Themes,
        Effects
    }

    /**
     * The sorting mode of the apps.
     */
    /*public enum SortMode {
        Title,
        InstallDate
    }*/

    // Refs
    private Launcher mLauncher;
    private DragController mDragController;
    private final LayoutInflater mLayoutInflater;
    private final PackageManager mPackageManager;

    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;
    //private PagedViewIcon mPressedIcon;
    private int mRestorePage = -1;

    // Content
    private ContentType mContentType;
    //private SortMode mSortMode = SortMode.Title;
    //private ArrayList<ShortcutInfo> mApps;
    private ArrayList<Object> mWidgets;
    private ArrayList<ShenduPrograme> mWallpapersList;
    private ArrayList<ShenduPrograme> mEffectsList;
    private ArrayList<ShenduPrograme> mThemesList;
    private ImageView mEditStateLeftArrow,mEditStateRightArrow;


    // Caching
    private Canvas mCanvas;
    private Drawable mDefaultWidgetBackground;
    private IconCache mIconCache;

    // Dimens
    private int mContentWidth;
    private int mAppIconSize;
    private int mMaxAppCellCountX, mMaxAppCellCountY;
    private int mMaxWidgetSpan, mMinWidgetSpan;
    private int mWidgetCountX, mWidgetCountY;
    private int mWidgetWidthGap, mWidgetHeightGap;
    private final float sWidgetPreviewIconPaddingPercentage = 0.25f;
    private PagedViewCellLayout mWidgetSpacingLayout;
    //private int mNumAppsPages = 0;
    //private int mNumWidgetPages = 0;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);
    private SharedPreferences mSharedPreferences;

    // Previews & outlines
    ArrayList<AppsCustomizeAsyncTask> mRunningTasks;
    private static final int sPageSleepDelay = 200;

    private Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;
    static final int WIDGET_NO_CLEANUP_REQUIRED = -1;
    static final int WIDGET_PRELOAD_PENDING = 0;
    static final int WIDGET_BOUND = 1;
    static final int WIDGET_INFLATED = 2;
    int mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
    int mWidgetLoadingId = -1;
    PendingAddWidgetInfo mCreateWidgetInfo = null;
    private boolean mDraggingWidget = false;

    // Deferral of loading widget previews during launcher transitions
    private boolean mInTransition;
    private ArrayList<AsyncTaskPageData> mDeferredSyncWidgetPageItems =
        new ArrayList<AsyncTaskPageData>();
    private ArrayList<Runnable> mDeferredPrepareLoadWidgetPreviewsTasks =
        new ArrayList<Runnable>();
    
    
    
    public  ThemeBroadcastReceiver  mThemeBroadcastReceiver ;

    // Used for drawing shortcut previews
    BitmapCache mCachedShortcutPreviewBitmap = new BitmapCache();
    PaintCache mCachedShortcutPreviewPaint = new PaintCache();
    CanvasCache mCachedShortcutPreviewCanvas = new CanvasCache();

    // Used for drawing widget previews
    CanvasCache mCachedAppWidgetPreviewCanvas = new CanvasCache();
    RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();
    RectCache mCachedAppWidgetPreviewDestRect = new RectCache();
    PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();


    public AppsCustomizePagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        mContentType = ContentType.Wallpapers;
        //mApps = new ArrayList<ShortcutInfo>();
        mWallpapersList = new ArrayList<ShenduPrograme>();
        mEffectsList = new ArrayList<ShenduPrograme>();
        mThemesList  = new ArrayList<ShenduPrograme>();
        mWidgets = new ArrayList<Object>();
        mIconCache = ((LauncherApplication) context.getApplicationContext()).getIconCache();
        mCanvas = new Canvas();
        mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();


        // Save the default widget preview background
        Resources resources = context.getResources();
        mDefaultWidgetBackground = resources.getDrawable(R.drawable.default_widget_preview_holo);
        mAppIconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mWidgetWidthGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellWidthGap, 0);
        mWidgetHeightGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellHeightGap, 0);
        mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 2);
        mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
        a.recycle();
        mWidgetSpacingLayout = new PagedViewCellLayout(getContext());

        // The max widget span is the length N, such that NxN is the largest bounds that the widget
        // preview can be before applying the widget scaling
        mMinWidgetSpan = 1;
        mMaxWidgetSpan = 3;

        // The padding on the non-matched dimension for the default widget preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

    }

    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;

        Context context = getContext();
        Resources r = context.getResources();
        setDragSlopeThreshold(r.getInteger(R.integer.config_appsCustomizeDragSlopeThreshold)/100f);
    }

    @Override
    protected void onUnhandledTap(MotionEvent ev) {
        if (LauncherApplication.isScreenLarge()) {
            // Dismiss AppsCustomize if we tap
            mLauncher.showWorkspace(true);
        }
    }

    /** Returns the item index of the center item on this page so that we can restore to this
     *  item index when we rotate. */
    private int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
                switch (mContentType) {
                case Widgets: {
                    PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(currentPage);
                    int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                    int childCount = layout.getChildCount();
                    if (childCount > 0) {
                        i = (currentPage * numItemsPerPage) + (childCount / 2);
                    }}
                    break;
                default:
                	break;
                }
        }
        return i;
    }

    /** Get the index of the item to restore to if we need to restore the current page. */
    int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }

    /** Returns the page in the current orientation which is expected to contain the specified
     *  item index. */
    int getPageForComponent(int index) {
            switch (mContentType) {
            case Widgets: {
                int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                return (index / numItemsPerPage);
            }}
            return -1;
    }

    /** Restores the page for an item at the specified index */
    void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }

    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a similar layout
        boolean isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (LauncherApplication.isScreenLarge()) {
            maxCellCountX = (isLandscape ? LauncherModel.getCellCountX() :
                LauncherModel.getCellCountY());
            maxCellCountY = (isLandscape ? LauncherModel.getCellCountY() :
                LauncherModel.getCellCountX());
        }
        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }
        if (mMaxAppCellCountY > -1) {
            maxCellCountY = Math.min(maxCellCountY, mMaxAppCellCountY);
        }

        // Now that the data is ready, we can calculate the content width, the number of cells to
        // use for each page
        mWidgetSpacingLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        //updatePageCounts();

        // Force a measure to update recalculate the gaps
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        mWidgetSpacingLayout.measure(widthSpec, heightSpec);
        mContentWidth = mWidgetSpacingLayout.getContentWidth();

        AppsCustomizeTabHost host = (AppsCustomizeTabHost) getTabHost();
        final boolean hostIsTransitioning = host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        invalidatePageData(Math.max(0, page), hostIsTransitioning);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            boolean isReady = false;
            if (mContentType == AppsCustomizePagedView.ContentType.Widgets ){//|| mJoinWidgetsApps) {
                isReady = !mWidgets.isEmpty(); //(!mApps.isEmpty() && !mWidgets.isEmpty());
            } else if(mContentType == AppsCustomizePagedView.ContentType.Wallpapers ){
            	isReady = !mWallpapersList.isEmpty();
            }else if(mContentType == AppsCustomizePagedView.ContentType.Effects ){
            	isReady = !mEffectsList.isEmpty();
            }

            if (isReady) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onPackagesUpdated() {
        // TODO: this isn't ideal, but we actually need to delay here. This call is triggered
        // by a broadcast receiver, and in order for it to work correctly, we need to know that
        // the AppWidgetService has already received and processed the same broadcast. Since there
        // is no guarantee about ordering of broadcast receipt, we just delay here. This is a
        // workaround until we add a callback from AppWidgetService to AppWidgetHost when widget
        // packages are added, updated or removed.
        postDelayed(new Runnable() {
           public void run() {
               updatePackages();
           }
        },4000);
    }

	/** 
	 * 2012-8-30 hhl
     * TODO: userd to init wallpaperList data when listener wallpaper is changed or first open launcher
	 */
	public void onWallpaperChanged() {
		postDelayed(new Runnable() {
			public void run() {
				shenduFindWallpapers();
			}
	    }, 100);
	}
	
	/** 
	 * 2012-9-19 hhl
     * TODO: userd to init effectList data when listener effect is changed or first open launcher
	 */
	public void onEffectChanged() {
		postDelayed(new Runnable() {
			public void run() {
				shenduFindEffects();
			}
	    }, 100);
	}
	
	
	/** 
	 * 2012-9-19 hhl
     * TODO: userd to init effectList data when listener effect is changed or first open launcher
	 */
	public void onThemeChanged() {
		
		if(mThemeBroadcastReceiver ==null){
			mThemeBroadcastReceiver = new ThemeBroadcastReceiver();
		}
		
		postDelayed(new Runnable() {
			public void run() {
				shenduFindTheme();
			}
	    }, 100);
	}

    public void updatePackages() {
        mWidgets.clear();
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(mLauncher).getInstalledProviders();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.minWidth > 0 && widget.minHeight > 0) {
                // Ensure that all widgets we show can be added on a workspace of this size
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, widget);
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, widget);
                int minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                int minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                if (minSpanX <= LauncherModel.getCellCountX() &&
                        minSpanY <= LauncherModel.getCellCountY()) {
                    mWidgets.add(widget);
                } else {
                    Log.e(TAG, "Widget " + widget.provider + " can not fit on this device (" +
                            widget.minWidth + ", " + widget.minHeight + ")");
                }
            } else {
                Log.e(TAG, "Widget " + widget.provider + " has invalid dimensions (" +
                        widget.minWidth + ", " + widget.minHeight + ")");
            }
        }
        mWidgets.addAll(shortcuts);
        Collections.sort(mWidgets,
                new LauncherModel.WidgetAndShortcutNameComparator(mPackageManager));
        //updatePageCounts();
        invalidateOnDataChange();
    }
    
    /**
     * 2012-9-12 hhl
     * TODO: userd to find the effects style in current launcher and init mEffectsList data
     */
    private void shenduFindEffects(){
    	boolean wasEmpty = mEffectsList.isEmpty();
    	int effectStrId,effectDrawableId;
    	//TransitionEffect mTransitionEffect;
    	mEffectsList.clear();
    	Resources resources = getResources();
       String currentEffect = mSharedPreferences.getString(PreferencesProvider.PREFERENCES_EFFECT,"Standard");
    	String[] extras = resources.getStringArray(R.array.effects);
    	for (String extra:extras) {
    		ShenduPrograme shendParograme = new ShenduPrograme();
    		effectStrId = getResources().getIdentifier("editstate_effect_string_"+extra.toLowerCase(),
    				"string","com.shendu.launcher");
    		effectDrawableId = getResources().getIdentifier("editstate_effect_drawable_"+extra.toLowerCase(),
    				"drawable","com.shendu.launcher");
    		//mTransitionEffect = TransitionEffect.valueOf(extra);
    		shendParograme.setName(extra);
    		shendParograme.setEffectStrId(effectStrId);
    		shendParograme.setEffectDrawableId(effectDrawableId);
    		if(currentEffect.equals(extra)){
        		shendParograme.setEffectCurrent(true);
    		}else{
        		shendParograme.setEffectCurrent(false);
    		}
    		mEffectsList.add(shendParograme);
    	}
    	if (wasEmpty) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so request
            // a layout to do this test and invalidate the page data when ready.
            //if (testDataReady()) requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }
    
    /**
     * 2012-8-26 hhl
     * TODO: userd to find the wallpapers data in current launcher and init mWallpapersList data
     */
    private void shenduFindWallpapers() {
    	boolean wasEmpty = mWallpapersList.isEmpty();
    	mWallpapersList.clear();
    	Resources resources = getResources();
    	String packageName = resources.getResourcePackageName(R.array.wallpapers);
    	String[] extras = resources.getStringArray(R.array.wallpapers);
    	for (String extra:extras) {
            int resId = resources.getIdentifier(extra, "drawable", packageName);
            ShenduPrograme widgetPrograme = new ShenduPrograme();
            if (resId != 0) {
                int smallResId = resources.getIdentifier(extra+"_small","drawable",packageName);
                widgetPrograme.setResId(resId);
                widgetPrograme.setChoice(ShenduPrograme.CHOICE_WALLPAPER_LAUNCHER);
                if (smallResId != 0) {
                    widgetPrograme.setResSmallId(smallResId);
                }
                mWallpapersList.add(widgetPrograme);
            }
        }
    	ShenduPrograme widgetPrograme = new ShenduPrograme();
    	WallpaperManager wallpaperManager = WallpaperManager.getInstance(mLauncher);
    	WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
    	if(wallpaperInfo==null){
    		widgetPrograme.setLiveWallpaper(false);
    		widgetPrograme.setResDrawable(wallpaperManager.getDrawable());
    	}else{
    		widgetPrograme.setLiveWallpaper(true);
    		widgetPrograme.setResDrawable(wallpaperInfo.loadThumbnail(mPackageManager));
    		ComponentName component = new ComponentName(wallpaperInfo.getPackageName(),wallpaperInfo.getServiceName()); 
    		widgetPrograme.setComponentname(component);
    		component=null;
    	}
        widgetPrograme.setChoice(ShenduPrograme.CHOICE_WALLPAPER_CURRENT);
    	mWallpapersList.add(widgetPrograme);
    	widgetPrograme = new ShenduPrograme();
    	Intent wallpaperIntent = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooserWallpaper = Intent.createChooser(
        		new Intent(Intent.ACTION_SET_WALLPAPER),
        		mLauncher.getString(R.string.chooser_wallpaper));
    	widgetPrograme.setIntent(chooserWallpaper);
    	widgetPrograme.setName(mLauncher.getString(R.string.group_wallpapers).toString());
    	widgetPrograme.setResSmallId(R.drawable.editstate_more_wallpaper_bg);
        widgetPrograme.setChoice(ShenduPrograme.CHOICE_WALLPAPER_MORE);
    	mWallpapersList.add(widgetPrograme);
    	if (wasEmpty) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so request
            // a layout to do this test and invalidate the page data when ready.
            //if (testDataReady()) requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }
    
    /**
     * 2012-8-26 hhl
     * TODO: userd to find the wallpapers data in current launcher and init mWallpapersList data
     */
    private void shenduFindTheme() {
    	boolean wasEmpty = mThemesList.isEmpty();
    	mThemesList.clear();
		Intent intent = new Intent(AppsCustomizePagedView.LAUNCH_THEME_SEND);
		mLauncher.sendBroadcast(intent);
		
	 	if (wasEmpty) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so request
            // a layout to do this test and invalidate the page data when ready.
            //if (testDataReady()) requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    	
    }

    public class ThemeBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			boolean resuilt = intent.getBooleanExtra("ischanged",false);
	
			if(resuilt || firstSendToTheme ){
				try {
					getThemeResources( intent.getStringExtra("path") );
					if(!firstSendToTheme){
						syncThemesPageItems(0,true);
					}
					firstSendToTheme =false;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static String LAUNCH_THEME_SEND  ="com.shendu.theme.LauncherBroadcast_parser_perview_Action";
    public static String THEME_RECEIVER ="com.shendu.launcher_receive";  
    public static String CHANGE_THEME_SEND  ="com.shendu.theme.LauncherBroadcast_setting_theme_Action";
	private static boolean firstSendToTheme= true;
	
	public void getThemeResources(String path ) throws Exception {

		mThemesList.clear();
		String suffix = ".jpg";
		File stringPath = Environment.getDataDirectory();
		File systemThemePaht = new File(stringPath.getPath() + path);

		if (systemThemePaht.exists()) {
			File[] files = systemThemePaht.listFiles();
		
			for (File file : files) {
		
				if (file.isFile() && file.getName().endsWith(suffix)) {
					
					ShenduPrograme shenduTheme =new ShenduPrograme();
					String cruThemePath = file.getAbsolutePath();
					Bitmap bm = BitmapFactory.decodeFile(cruThemePath);

					shenduTheme.mThemeBitmap=bm;
					shenduTheme.mThemePath=cruThemePath;
					mThemesList.add(shenduTheme);
				}
			}
		}
  
		ShenduPrograme	moreThemePrograme = new ShenduPrograme();
		
    	Intent thmemIntent =  new Intent(Intent.ACTION_MAIN);
    	ComponentName componentName = new ComponentName("com.shendu.theme","com.shendu.theme.ShenDu_MainActivity");
    	thmemIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    	thmemIntent.setComponent(componentName);
    	thmemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
   
        moreThemePrograme.setIntent(thmemIntent);
        //moreThemePrograme.mThemeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.editstate_tabhost_tabcontent_wallpaper_more_normal);
        moreThemePrograme.setResSmallId(R.drawable.editstate_more_wallpaper_bg);
        moreThemePrograme.setChoice(ShenduPrograme.CHOICE_WALLPAPER_MORE);
        mThemesList.add(moreThemePrograme);
        
	}
	

    @Override
    public void onClick(final View view) {
        // When we have exited all apps or are in transition, disregard clicks
        //if (!mLauncher.isAllAppsCustomizeOpen() ||
    	if (mLauncher.getWorkspace().isSwitchingState()) return;
        if (view instanceof PagedViewWallpaper) {
            // Animate some feedback to the click
            //final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();
            final ShenduPrograme shenduPrograme = (ShenduPrograme) view.getTag();
            animateClickFeedback(view, new Runnable() {
                public void run() {
                	try {
                    	WallpaperManager wallpaperManager = WallpaperManager.getInstance(mLauncher);
                		switch(shenduPrograme.getChoice()){
                    	case ShenduPrograme.CHOICE_WALLPAPER_LAUNCHER:
        					wallpaperManager.setResource(shenduPrograme.getResId());
                    		break;
                    	case ShenduPrograme.CHOICE_WALLPAPER_CURRENT:
                    		break;
                    	case ShenduPrograme.CHOICE_WALLPAPER_MORE:
                    		mLauncher.startActivity(shenduPrograme.getIntent());
                    		break;
                    	default:break;
                    	}
                		wallpaperManager=null;
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
            });
        } else if (view instanceof PagedViewEffect){
        	ShenduPrograme shenduPrograme = (ShenduPrograme) view.getTag();
        	TransitionEffect oldTransitionEffect = mLauncher.getWorkspace().getmTransitionEffect();
        	TransitionEffect newTransitionEffect = Workspace.TransitionEffect.valueOf(shenduPrograme.getName());
        	if(!oldTransitionEffect.equals(newTransitionEffect)){
            	mLauncher.getWorkspace().setmTransitionEffect(newTransitionEffect);
            	mLauncher.getWorkspace().recoveryState(State.NORMAL,State.SMALL,true);
            	mLauncher.getWorkspace().transitionEffectDemonstration();
            	SharedPreferences.Editor editor = mSharedPreferences.edit();
            	editor.putString(PreferencesProvider.PREFERENCES_EFFECT, shenduPrograme.getName());
            	editor.commit();
        	}
        }else if (view instanceof PagedViewWidget) {
            // Let the user know that they have to long press to add a widget
            Toast.makeText(getContext(), R.string.long_press_widget_to_add,
                    Toast.LENGTH_SHORT).show();
            // Create a little animation to show that the widget can move
            float offsetY = getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
            final ImageView p = (ImageView) view.findViewById(R.id.widget_preview);
            AnimatorSet bounce = new AnimatorSet();
            ValueAnimator tyuAnim = ObjectAnimator.ofFloat(p, "translationY", offsetY);
            tyuAnim.setDuration(125);
            ValueAnimator tydAnim = ObjectAnimator.ofFloat(p, "translationY", 0f);
            tydAnim.setDuration(100);
            bounce.play(tyuAnim).before(tydAnim);
            bounce.setInterpolator(new AccelerateInterpolator());
            bounce.start();
        }else if (view instanceof PagedViewTheme) {
        	 mLauncher.backFromEditMode();
        	 ShenduPrograme shenduPrograme = (ShenduPrograme) view.getTag();
        	 Intent intent = shenduPrograme.getIntent();
        	 if(intent ==null){
        		  // add by zlf for Theme
             	intent = new Intent(AppsCustomizePagedView.CHANGE_THEME_SEND);
 				intent.putExtra("path", shenduPrograme.mThemePath);
 				mLauncher.sendBroadcast(intent);
 				mLauncher.shenduShowProgressDialog(mLauncher.getResources().getString(R.string.luancher_changed_theme));
        	 }else{
 	    	    mLauncher.startActivity(intent);
        	 }
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v,  keyCode, event);
    }

    /*
     * PagedViewWithDraggableItems implementation
     */
    @Override
    protected void determineDraggingStart(android.view.MotionEvent ev) {
        // Disable dragging by pulling an app down for now.
    }


    private void preloadWidget(final PendingAddWidgetInfo info) {
        final AppWidgetProviderInfo pInfo = info.info;
        if (pInfo.configure != null) {
            return;
        }

        mWidgetCleanupState = WIDGET_PRELOAD_PENDING;
        mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                mWidgetLoadingId = mLauncher.getAppWidgetHost().allocateAppWidgetId();
                if (AppWidgetManager.getInstance(mLauncher)
                            .bindAppWidgetIdIfAllowed(mWidgetLoadingId, info.componentName)) {
                    mWidgetCleanupState = WIDGET_BOUND;
                }
            }
        };
        post(mBindWidgetRunnable);

        mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                AppWidgetHostView hostView = mLauncher.
                        getAppWidgetHost().createView(getContext(), mWidgetLoadingId, pInfo);
                info.boundWidget = hostView;
                mWidgetCleanupState = WIDGET_INFLATED;
                hostView.setVisibility(INVISIBLE);
                int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(info.spanX,
                        info.spanY, info, false);

                // We want the first widget layout to be the correct size. This will be important
                // for width size reporting to the AppWidgetManager.
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0],
                        unScaledSize[1]);
                lp.x = lp.y = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                mLauncher.getDragLayer().addView(hostView);
            }
        };
        post(mInflateWidgetRunnable);
    }

    @Override
    public void onShortPress(View v) {
        // We are anticipating a long press, and we use this time to load bind and instantiate
        // the widget. This will need to be cleaned up if it turns out no long press occurs.
        if (mCreateWidgetInfo != null) {
            // Just in case the cleanup process wasn't properly executed. This shouldn't happen.
            cleanupWidgetPreloading(false);
        }
        mCreateWidgetInfo = new PendingAddWidgetInfo((PendingAddWidgetInfo) v.getTag());
        preloadWidget(mCreateWidgetInfo);
    }

    private void cleanupWidgetPreloading(boolean widgetWasAdded) {
        if (!widgetWasAdded) {
            // If the widget was not added, we may need to do further cleanup.
            PendingAddWidgetInfo info = mCreateWidgetInfo;
            mCreateWidgetInfo = null;

            if (mWidgetCleanupState == WIDGET_PRELOAD_PENDING) {
                // We never did any preloading, so just remove pending callbacks to do so
                removeCallbacks(mBindWidgetRunnable);
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_BOUND) {
                 // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // We never got around to inflating the widget, so remove the callback to do so.
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_INFLATED) {
                // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // The widget was inflated and added to the DragLayer -- remove it.
                AppWidgetHostView widget = info.boundWidget;
                mLauncher.getDragLayer().removeView(widget);
            }
        }
        mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
        mWidgetLoadingId = -1;
        mCreateWidgetInfo = null;
        PagedViewWidget.resetShortPressTarget();
    }

    @Override
    public void cleanUpShortPress(View v) {
        if (!mDraggingWidget) {
            cleanupWidgetPreloading(false);
        }
    }

    private boolean beginDraggingWidget(View v) {
        mDraggingWidget = true;
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        // If the ImageView doesn't have a drawable yet, the widget preview hasn't been loaded and
        // we abort the drag.
        if (image.getDrawable() == null) {
            mDraggingWidget = false;
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        float scale = 1f;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            // This can happen in some weird cases involving multi-touch. We can't start dragging
            // the widget if this is null, so we break out.
            if (mCreateWidgetInfo == null) {
                return false;
            }
            PendingAddWidgetInfo createWidgetInfo = mCreateWidgetInfo;
            createItemInfo = createWidgetInfo;
            int spanX = createItemInfo.spanX;
            int spanY = createItemInfo.spanY;
            int[] size = mLauncher.getWorkspace().estimateItemSize(spanX, spanY,
                    createWidgetInfo, true);

            FastBitmapDrawable previewDrawable = (FastBitmapDrawable) image.getDrawable();
            float minScale = 1.25f;
            int maxWidth, maxHeight;
            maxWidth = Math.min((int) (previewDrawable.getIntrinsicWidth() * minScale), size[0]);
            maxHeight = Math.min((int) (previewDrawable.getIntrinsicHeight() * minScale), size[1]);
            preview = getWidgetPreview(createWidgetInfo.componentName, createWidgetInfo.previewImage,
                    createWidgetInfo.icon, spanX, spanY, maxWidth, maxHeight);

            // Determine the image view drawable scale relative to the preview
            float[] mv = new float[9];
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0f, 0f, (float) preview.getWidth(), (float) preview.getHeight()),
                    new RectF(0f, 0f, (float) previewDrawable.getIntrinsicWidth(),
                            (float) previewDrawable.getIntrinsicHeight()),
                    Matrix.ScaleToFit.START);
            m.getValues(mv);
            scale = (float) mv[0];
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            Drawable icon = mIconCache.getFullResIcon(createShortcutInfo.shortcutActivityInfo);
            preview = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                    icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            mCanvas.setBitmap(preview);
            mCanvas.save();
            renderDrawableToBitmap(icon, preview, 0, 0,
                    icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            mCanvas.restore();
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = 1;
        }

        // We use a custom alpha clip table for the default widget previews
        Paint alphaClipPaint = null;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            if (((PendingAddWidgetInfo) createItemInfo).previewImage != 0) {
                MaskFilter alphaClipTable = TableMaskFilter.CreateClipTable(0, 255);
                alphaClipPaint = new Paint();
                alphaClipPaint.setMaskFilter(alphaClipTable);
            }
        }

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(), false);
        // Start the drag
        alphaClipPaint = null;
        mLauncher.lockScreenOrientation();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, alphaClipPaint);
        mDragController.startDrag(image, preview, this, createItemInfo,
                DragController.DRAG_ACTION_COPY, null, scale);
        outline.recycle();
        preview.recycle();
        return true;
    }

    @Override
    protected boolean beginDragging(final View v) {
        if (!super.beginDragging(v)) return false;

        /*if (v instanceof PagedViewIcon) {
            beginDraggingApplication(v);
        } else */ 
		if (v instanceof PagedViewWidget) {
            if (!beginDraggingWidget(v)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public View getContent() {
        return null;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = true;
        if (toWorkspace) {
            cancelAllTasks();
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = false;
        for (AsyncTaskPageData d : mDeferredSyncWidgetPageItems) {
            onSyncWidgetPageItems(d);
        }
        mDeferredSyncWidgetPageItems.clear();
        for (Runnable r : mDeferredPrepareLoadWidgetPreviewsTasks) {
            r.run();
        }
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
        mForceDrawAllChildrenNextFrame = !toWorkspace;
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
            boolean success) {
        // Return early and wait for onFlingToDeleteCompleted if this was the result of a fling
        if (isFlingToDelete) return;

        //endDragging(target, false, success);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
        cleanupWidgetPreloading(success);
        mDraggingWidget = false;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // We just dismiss the drag when we fling, so cleanup here
        //endDragging(null, true, true);
        cleanupWidgetPreloading(false);
        mDraggingWidget = false;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllTasks();
    }

    public void clearAllWidgetPages() {
    	
    	if(mContentType==ContentType.Widgets){
    		return ;
    	}
        cancelAllTasks();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }

    private void cancelAllTasks() {
        // Clean up all the async tasks
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
            mDirtyPageContent.set(task.page, true);

            // We've already preallocated the views for the data to load into, so clear them as well
            View v = getPageAt(task.page);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
            }
        }
        mDeferredSyncWidgetPageItems.clear();
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
    }

    public void setContentType(ContentType type) {
            mContentType = type;
            invalidatePageData(0,true);
    }
    
    /**
     * 2012-8-29 hhl
     * @param currentPage: which page of the bottom view in edit state
     * TODO: userd to update the left、right arrow of the bottom view in edit state display or not
     */
    public void shenduUpdateTheArrowImageView(int currentPage){
    	if((mEditStateLeftArrow!=null)&&(mEditStateRightArrow!=null)){
    		int pageCount = getChildCount();
    		if(pageCount<=1){
    			mEditStateLeftArrow.setVisibility(View.GONE);
            	mEditStateRightArrow.setVisibility(View.GONE);
    		}else{
	    		if(currentPage==0){
	    			mEditStateLeftArrow.setVisibility(View.GONE);
	    			mEditStateRightArrow.setVisibility(View.VISIBLE);
	    		}else if(currentPage==(getChildCount()-1)){
	    			mEditStateLeftArrow.setVisibility(View.VISIBLE);
	    			mEditStateRightArrow.setVisibility(View.GONE);
	    		}else{
	    			mEditStateRightArrow.setVisibility(View.VISIBLE);
	    			mEditStateLeftArrow.setVisibility(View.VISIBLE);
	    		}
    		}
    	}
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        shenduUpdateTheArrowImageView(whichPage);
        shenduUpdateTheArrowImageView(whichPage);
            // Update the thread priorities given the direction lookahead
            Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
            while (iter.hasNext()) {
                AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
                int pageIndex = task.page;
                if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) ||
                        (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                    task.setThreadPriority(getThreadPriorityForPage(pageIndex));
                } else {
                    task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                }
            }
    }

    public boolean isContentType(ContentType type) {
        return (mContentType == type);
    }

    public void setCurrentPageToWidgets() {
        invalidatePageData(0);
    }

    /*
     * Apps PagedView implementation
     */
    private void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }
    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.  That said, we already know the
        // expected page width, so we can actually optimize by hiding all the TextView-based
        // children that are expensive to measure, and let that happen naturally later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
        setVisibilityOnChildren(layout, View.VISIBLE);
    }

    /**
     * A helper to return the priority for loading of the specified widget page.
     */
    private int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of priority, but if there
        // are no pages of higher priority than the page specified, then bump up the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }
    /**
     * Return the appropriate thread priority for loading for a given page (we give the current
     * page much higher priority)
     */
    private int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }
    private int getSleepForPage(int page) {
        int pageDiff = getWidgetPageLoadPriority(page);
        return Math.max(0, pageDiff * sPageSleepDelay);
    }
    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    private void prepareLoadWidgetPreviewsTask(int page, ArrayList<Object> widgets,
            int cellWidth, int cellHeight, int cellCountX) {

        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if (taskPage < getAssociatedLowerPageBound(mCurrentPage) ||
                    taskPage > getAssociatedUpperPageBound(mCurrentPage)) {
                task.cancel(false);
                iter.remove();
            } else {
                task.setThreadPriority(getThreadPriorityForPage(taskPage));
            }
        }

        // We introduce a slight delay to order the loading of side pages so that we don't thrash
        final int sleepMs = getSleepForPage(page);
        AsyncTaskPageData pageData = new AsyncTaskPageData(page, widgets, cellWidth, cellHeight,
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (Exception e) {}
                        loadWidgetPreviewsInBackground(task, data);
                    } finally {
                        if (task.isCancelled()) {
                            data.cleanup(true);
                        }
                    }
                }
            },
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    mRunningTasks.remove(task);
                    if (task.isCancelled()) return;
                    // do cleanup inside onSyncWidgetPageItems
                    onSyncWidgetPageItems(data);
                }
            });

        // Ensure that the task is appropriately prioritized and runs in parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(page, mContentType,
                AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(getThreadPriorityForPage(page));
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    /*
     * Widgets PagedView implementation
     */
    private void setupPage(PagedViewGridLayout layout) {
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f);
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h,
            float scale) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            c.setBitmap(null);
        }
    }

    private Bitmap getShortcutPreview(ResolveInfo info, int maxWidth, int maxHeight) {
        Bitmap tempBitmap = mCachedShortcutPreviewBitmap.get();
        final Canvas c = mCachedShortcutPreviewCanvas.get();
        if (tempBitmap == null ||
                tempBitmap.getWidth() != maxWidth ||
                tempBitmap.getHeight() != maxHeight) {
            tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
            mCachedShortcutPreviewBitmap.set(tempBitmap);
        } else {
            c.setBitmap(tempBitmap);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            c.setBitmap(null);
        }
        // Render the icon
        Drawable icon = mIconCache.getFullResIcon(info);

        int paddingTop =
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft =
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int paddingRight =
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);

        int scaledIconWidth = (maxWidth - paddingLeft - paddingRight);
        float scaleSize = scaledIconWidth / (float) mAppIconSize;

        renderDrawableToBitmap(
                icon, tempBitmap, paddingLeft, paddingTop, scaledIconWidth, scaledIconWidth);

        Bitmap preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
        c.setBitmap(preview);
        Paint p = mCachedShortcutPreviewPaint.get();
        if (p == null) {
            p = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            p.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            p.setAlpha((int) (255 * 0.06f));
            //float density = 1f;
            //p.setMaskFilter(new BlurMaskFilter(15*density, BlurMaskFilter.Blur.NORMAL));
            mCachedShortcutPreviewPaint.set(p);
        }
        c.drawBitmap(tempBitmap, 0, 0, p);
        c.setBitmap(null);

        renderDrawableToBitmap(icon, preview, 0, 0, mAppIconSize, mAppIconSize);

        return preview;
    }

    private Bitmap getWidgetPreview(ComponentName provider, int previewImage,
            int iconId, int cellHSpan, int cellVSpan, int maxWidth,
            int maxHeight) {
        // Load the preview image if possible
        String packageName = provider.getPackageName();
        if (maxWidth < 0) maxWidth = Integer.MAX_VALUE;
        if (maxHeight < 0) maxHeight = Integer.MAX_VALUE;

        Drawable drawable = null;
        if (previewImage != 0) {
            drawable = mPackageManager.getDrawable(packageName, previewImage, null);
            if (drawable == null) {
                Log.w(TAG, "Can't load widget preview drawable 0x" +
                        Integer.toHexString(previewImage) + " for provider: " + provider);
            }
        }

        int bitmapWidth;
        int bitmapHeight;
        Bitmap defaultPreview = null;
        boolean widgetPreviewExists = (drawable != null);
        if (widgetPreviewExists) {
            bitmapWidth = drawable.getIntrinsicWidth();
            bitmapHeight = drawable.getIntrinsicHeight();
        } else {
            // Generate a preview image if we couldn't load one
            //if (cellHSpan < 1) 
            	cellHSpan = 1;
            //if (cellVSpan < 1) 
            	cellVSpan = 1;

            BitmapDrawable previewDrawable = (BitmapDrawable) getResources()
                    .getDrawable(R.drawable.widget_preview_tile);
            final int previewDrawableWidth = previewDrawable
                    .getIntrinsicWidth();
            final int previewDrawableHeight = previewDrawable
                    .getIntrinsicHeight();
            bitmapWidth = previewDrawableWidth * cellHSpan; // subtract 2 dips
            bitmapHeight = previewDrawableHeight * cellVSpan;

            defaultPreview = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                    Config.ARGB_8888);
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            c.setBitmap(defaultPreview);
            previewDrawable.setBounds(0, 0, bitmapWidth, bitmapHeight);
            previewDrawable.setTileModeXY(Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT);
            previewDrawable.draw(c);
            c.setBitmap(null);

            // Draw the icon in the top left corner
            int minOffset = (int) (mAppIconSize * sWidgetPreviewIconPaddingPercentage);
            int smallestSide = Math.min(bitmapWidth, bitmapHeight);
            float iconScale = Math.min((float) smallestSide
                    / (mAppIconSize + 2 * minOffset), 1f);

            try {
                Drawable icon = null;
                int hoffset =
                        (int) ((previewDrawableWidth - mAppIconSize * iconScale) / 2);
                int yoffset =
                        (int) ((previewDrawableHeight - mAppIconSize * iconScale) / 2);
                if (iconId > 0)
                    icon = mIconCache.getFullResIcon(packageName, iconId);
                Resources resources = mLauncher.getResources();
                if (icon != null) {
                    renderDrawableToBitmap(icon, defaultPreview, hoffset,
                            yoffset, (int) (mAppIconSize * iconScale),
                            (int) (mAppIconSize * iconScale));
                }
            } catch (Resources.NotFoundException e) {
            }
        }

        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        float scale = 1f;
        if (bitmapWidth > maxWidth) {
            scale = maxWidth / (float) bitmapWidth;
        }
        if (scale != 1f) {
            bitmapWidth = (int) (scale * bitmapWidth);
            bitmapHeight = (int) (scale * bitmapHeight);
        }

        Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                Config.ARGB_8888);

        // Draw the scaled preview into the final bitmap
        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, 0, 0, bitmapWidth,
                    bitmapHeight);
        } else {
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            final Rect src = mCachedAppWidgetPreviewSrcRect.get();
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            c.setBitmap(preview);
            src.set(0, 0, defaultPreview.getWidth(), defaultPreview.getHeight());
            dest.set(0, 0, preview.getWidth(), preview.getHeight());

            Paint p = mCachedAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setFilterBitmap(true);
                mCachedAppWidgetPreviewPaint.set(p);
            }
            c.drawBitmap(defaultPreview, src, dest, p);
            c.setBitmap(null);
        }
        return preview;
    }


    /**
     * 2012-9-12 hhl
     * TODO: used sync the effect tabwidget content page view data
     */
    public void syncEffectsPages(){
    	
    	Context context = getContext();
        int numPages = (int) Math.ceil((float)mEffectsList.size()/(mCellCountX * mCellCountY));
        for (int j = 0; j < numPages; ++j) {
        	PagedViewCellLayout layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
    }
    
    /**
     * 2012-8-29 hhl
     * TODO: used sync the wallpaper tabwidget content page view data
     */
    public void syncWallpapersPages(){
    	Context context = getContext();
        int numPages = (int) Math.ceil((float)mWallpapersList.size()/(mCellCountX * mCellCountY));
        for (int j = 0; j < numPages; ++j) {
        	PagedViewCellLayout layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
    }
    
    public void syncWidgetPages() {
    	
        // Ensure that we have the right number of pages
        Context context = getContext();
        int numPages = (int) Math.ceil(mWidgets.size() /
                (float) (mWidgetCountX * mWidgetCountY));
        for (int j = 0; j < numPages; ++j) {
            PagedViewGridLayout layout = new PagedViewGridLayout(context, mWidgetCountX,
                    mWidgetCountY);
            setupPage(layout);
            addView(layout, new PagedViewGridLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
        }
    }
    
    
    public void syncThemesPages() {
    	removeAllViews();
        // Ensure that we have the right number of pages
      	Context context = getContext();
        int numPages = Math.max((int) Math.ceil((float)mThemesList.size()/(mCellCountX * mCellCountY)), 1);
        for (int j = 0; j < numPages; ++j) {
        	PagedViewCellLayout layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
        }
    }

    /**
     * 2012-8-29 hhl
     * @param page: which page item used to sync
     * @param immediate: immediate sync data or not
     * TODO: used to sync the wallpaper tabwidget content of the page items data
     */
    public void syncWallpaperPageItems(final int page, final boolean immediate){
    	
    	int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mWallpapersList.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);

        layout.removeAllViewsOnPage();
        for (int i = startIndex; i < endIndex; ++i) {
        	ShenduPrograme info = mWallpapersList.get(i);
        	PagedViewWallpaper wallpaperView = (PagedViewWallpaper)mLayoutInflater.inflate(
                    R.layout.apps_customize_wallpaper, layout, false);
        	wallpaperView.applyFromShenduPrograme(info);
        	wallpaperView.setOnClickListener(this);
            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayoutWallpapger(wallpaperView, -1, i, new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
        }

        layout.createHardwareLayers();
    }
    
    /**
     * 2012-9-12 hhl
     * @param page: which page item used to sync
     * @param immediate: immediate sync data or not
     * TODO: used to sync the effect tabwidget content of the page items data
     */
    public void syncEffectPageItems(final int page, final boolean immediate){
    	
    	
    	int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mEffectsList.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);
        layout.removeAllViewsOnPage();
        for (int i = startIndex; i < endIndex; ++i) {
        	ShenduPrograme info = mEffectsList.get(i);
        	PagedViewEffect effectView = (PagedViewEffect)mLayoutInflater.inflate(
                    R.layout.apps_customize_effect, layout, false);
        	effectView.applyFromShenduPrograme(info);
        	effectView.setOnClickListener(this);
            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayoutWallpapger(effectView, -1, i, new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
        }

        layout.createHardwareLayers();
    }
    
    
    public void  syncThemesPageItems(final int page, final boolean immediate){
    	
    	int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mThemesList.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);
        layout.removeAllViewsOnPage();
        
        for (int i = startIndex; i < endIndex; ++i) {
        	ShenduPrograme info = mThemesList.get(i);
        	PagedViewTheme pagedViewTheme = (PagedViewTheme)mLayoutInflater.inflate(
                    R.layout.apps_customize_theme, layout, false);
        	pagedViewTheme.applyFromShenduPrograme(info);
        	pagedViewTheme.setOnClickListener(this);
            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayoutWallpapger(pagedViewTheme, -1, i, new PagedViewCellLayout.LayoutParams(x, y, 1, 1));
        }


        layout.createHardwareLayers();
    }

    
    
    public void syncWidgetPageItems(final int page, final boolean immediate) {
    	
        int numItemsPerPage = mWidgetCountX * mWidgetCountY;
        // Calculate the dimensions of each cell we are giving to each widget
        final ArrayList<Object> items = new ArrayList<Object>();
        int contentWidth = mWidgetSpacingLayout.getContentWidth();
        final int cellWidth = ((contentWidth - mPageLayoutPaddingLeft - mPageLayoutPaddingRight
                - ((mWidgetCountX - 1) * mWidgetWidthGap)) / mWidgetCountX);
        int contentHeight = mWidgetSpacingLayout.getContentHeight();
        final int cellHeight = ((contentHeight - mPageLayoutPaddingTop - mPageLayoutPaddingBottom
                - ((mWidgetCountY - 1) * mWidgetHeightGap)) / mWidgetCountY);

        // Prepare the set of widgets to load previews for in the background
        int offset = (page) * numItemsPerPage;
        for (int i = offset; i < Math.min(offset + numItemsPerPage, mWidgets.size()); ++i) {
            items.add(mWidgets.get(i));
        }

        // Prepopulate the pages with the other widget info, and fill in the previews later
        final PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);
        layout.setColumnCount(layout.getCellCountX());
        for (int i = 0; i < items.size(); ++i) {
            Object rawInfo = items.get(i);
            PendingAddItemInfo createItemInfo = null;
            PagedViewWidget widget = (PagedViewWidget) mLayoutInflater.inflate(
                    R.layout.apps_customize_widget, layout, false);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                // Fill in the widget information
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                createItemInfo = new PendingAddWidgetInfo(info, null, null);

                // Determine the widget spans and min resize spans.
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, info);
                createItemInfo.spanX = spanXY[0];
                createItemInfo.spanY = spanXY[1];
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, info);
                createItemInfo.minSpanX = minSpanXY[0];
                createItemInfo.minSpanY = minSpanXY[1];

                widget.applyFromAppWidgetProviderInfo(info, -1, spanXY);
                widget.setTag(createItemInfo);
                widget.setShortPressListener(this);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                createItemInfo = new PendingAddShortcutInfo(info.activityInfo);
                createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_DELETESHOETCUT;
                createItemInfo.componentName = new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name);
                widget.applyFromResolveInfo(mPackageManager, info);
                widget.setTag(createItemInfo);
            }
            widget.setOnClickListener(this);
            widget.setOnLongClickListener(this);
            widget.setOnTouchListener(this);
            widget.setOnKeyListener(this);

            // Layout each widget
            int ix = i % mWidgetCountX;
            int iy = i / mWidgetCountX;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(iy, GridLayout.LEFT),
                    GridLayout.spec(ix, GridLayout.TOP));
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.setGravity(Gravity.TOP | Gravity.LEFT);
            if (ix > 0) lp.leftMargin = mWidgetWidthGap;
            if (iy > 0) lp.topMargin = mWidgetHeightGap;
            layout.addView(widget, lp);
        }

        // wait until a call on onLayout to start loading, because
        // PagedViewWidget.getPreviewSize() will return 0 if it hasn't been laid out
        // TODO: can we do a measure/layout immediately?
        layout.setOnLayoutListener(new Runnable() {
            public void run() {
                // Load the widget previews
                int maxPreviewWidth = cellWidth;
                int maxPreviewHeight = cellHeight;
                if (layout.getChildCount() > 0) {
                    PagedViewWidget w = (PagedViewWidget) layout.getChildAt(0);
                    int[] maxSize = w.getPreviewSize();
                    maxPreviewWidth = maxSize[0];
                    maxPreviewHeight = maxSize[1];
                }
                if (immediate) {
                    AsyncTaskPageData data = new AsyncTaskPageData(page, items,
                            maxPreviewWidth, maxPreviewHeight, null, null);
                    loadWidgetPreviewsInBackground(null, data);
                    onSyncWidgetPageItems(data);
                } else {
                    if (mInTransition) {
                        mDeferredPrepareLoadWidgetPreviewsTasks.add(this);
                    } else {
                        prepareLoadWidgetPreviewsTask(page, items,
                                maxPreviewWidth, maxPreviewHeight, mWidgetCountX);
                    }
                }
            }
        });
    }
    private void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task,
            AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }

        // Load each of the widget/shortcut previews
        ArrayList<Object> items = data.items;
        ArrayList<Bitmap> images = data.generatedImages;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            if (task != null) {
                // Ensure we haven't been cancelled yet
                if (task.isCancelled()) break;
                // Before work on each item, ensure that this task is running at the correct
                // priority
                task.syncThreadPriority();
            }

            Object rawInfo = items.get(i);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                int[] cellSpans = Launcher.getSpanForWidget(mLauncher, info);

                int maxWidth = Math.min(data.maxImageWidth,
                        mWidgetSpacingLayout.estimateCellWidth(cellSpans[0]));
                int maxHeight = Math.min(data.maxImageHeight,
                        mWidgetSpacingLayout.estimateCellHeight(cellSpans[1]));
                Bitmap b = getWidgetPreview(info.provider, info.previewImage, info.icon,
                        cellSpans[0], cellSpans[1], maxWidth, maxHeight);
                images.add(b);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                images.add(getShortcutPreview(info, data.maxImageWidth, data.maxImageHeight));
            }
        }
    }

    private void onSyncWidgetPageItems(AsyncTaskPageData data) {
        if (mInTransition) {
            mDeferredSyncWidgetPageItems.add(data);
            return;
        }
        try {
            int page = data.page;
            PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);

            ArrayList<Object> items = data.items;
            int count = items.size();
            for (int i = 0; i < count; ++i) {
                PagedViewWidget widget = (PagedViewWidget) layout.getChildAt(i);
                if (widget != null) {
                    Bitmap preview = data.generatedImages.get(i);
                    widget.applyPreview(new FastBitmapDrawable(preview), i);
                }
            }

            layout.createHardwareLayer();
            invalidate();

            // Update all thread priorities
            Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
            while (iter.hasNext()) {
                AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
                int pageIndex = task.page;
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            }
        } finally {
            data.cleanup(false);
        }
    }

    @Override
    public void syncPages() {
        removeAllViews();
        cancelAllTasks();

            switch (mContentType) {
            case Widgets:
                syncWidgetPages();
                break;
            case Wallpapers:
                syncWallpapersPages();
                break;
            case Effects:
            	syncEffectsPages();
            	break;
            case Themes:
            	syncThemesPages();
            	break;
            default:
            	break;
            }
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
            switch (mContentType) {
            case Widgets:
                syncWidgetPageItems(page, immediate);
                break;
            case Wallpapers:
                syncWallpaperPageItems(page, immediate);
                break;
            case Effects:
            	syncEffectPageItems(page, immediate);
            	break;
            	
            case Themes:
            	syncThemesPageItems(page, immediate);
            	break;
            default:
            	break;
            }
    }

    // We want our pages to be z-ordered such that the further a page is to the left, the higher
    // it is in the z-order. This is important to insure touch events are handled correctly.
    View getPageAt(int index) {
        return getChildAt(indexToPage(index));
    }

    @Override
    protected int indexToPage(int index) {
        return getChildCount() - index - 1;
    }

    // In apps customize, we have a scrolling effect which emulates pulling cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float interpolatedProgress =
                        mZInterpolator.getInterpolation(Math.abs(Math.min(scrollProgress, 0)));
                float scale = (1 - interpolatedProgress) +
                        interpolatedProgress * TRANSITION_SCALE_FACTOR;
                float translationX = Math.min(0, scrollProgress) * v.getMeasuredWidth();

                float alpha;

                if (scrollProgress < 0) {
                    alpha = scrollProgress < 0 ? mAlphaInterpolator.getInterpolation(
                        1 - Math.abs(scrollProgress)) : 1.0f;
                } else {
                    // On large screens we need to fade the page as it nears its leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
                int pageWidth = v.getMeasuredWidth();
                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
                    if (i == 0 && scrollProgress < 0) {
                        // Overscroll to the left
                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any lateral motion.
                        translationX = 0;
                    } else {
                        v.setPivotY(pageHeight / 2.0f);
                        v.setPivotX(pageWidth / 2.0f);
                        v.setRotationY(0f);
                    }
                }

                v.setTranslationX(translationX);
                v.setScaleX(scale);
                v.setScaleY(scale);
                v.setAlpha(alpha);

                // If the view has 0 alpha, we set it to be invisible so as to prevent
                // it from accepting touches
                if (alpha == 0) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }
    }

    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    /**
     * Used by the parent to get the content width to set the tab bar to
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        mForceDrawAllChildrenNextFrame = true;

        // We reset the save index when we change pages so that it will be recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }

    /*
     * AllAppsView implementation
     */
    @Override
    public void setup(Launcher launcher, DragController dragController) {
    	
        mLauncher = launcher;
        mContentType=ContentType.Wallpapers;
        
        mSharedPreferences = mLauncher.getSharedPreferences(PreferencesProvider.PREFERENCES_KEY, Context.MODE_PRIVATE);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        mDragController = dragController;

        mEditStateLeftArrow = (ImageView)mLauncher.findViewById(R.id.apps_customize_pane_content_left_arrow_id);
        mEditStateRightArrow = (ImageView)mLauncher.findViewById(R.id.apps_customize_pane_content_right_arrow_id);
        
    }
    
    /**
     * add by hhl, used to listener the launcher settings changed
     */
    OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener(){
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String str) {
			if(str.equals(PreferencesProvider.PREFERENCES_EFFECT)){
				onEffectChanged();
			}
		}
    };
	
    @Override
    public void zoom(float zoom, boolean animate) {
        // TODO-APPS_CUSTOMIZE: Call back to mLauncher.zoomed()
    }
    @Override
    public boolean isVisible() {
        return (getVisibility() == VISIBLE);
    }
    @Override
    public boolean isAnimating() {
        return false;
    }

    /**
     * We should call thise method whenever the core data changes (mApps, mWidgets) so that we can
     * appropriately determine when to invalidate the PagedView page data.  In cases where the data
     * has yet to be set, we can requestLayout() and wait for onDataReady() to be called in the
     * next onMeasure() pass, which will trigger an invalidatePageData() itself.
     */
    private void invalidateOnDataChange() {
        if (!isDataReady()) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so
            // request a layout to trigger the page data when ready.
            requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }

    @Override
    public void setApps(ArrayList<ShortcutInfo> list) {
        invalidateOnDataChange();
    }
    @Override
    public void addApps(ArrayList<ShortcutInfo> list) {
        //addAppsWithoutInvalidate(list);
        //updatePageCounts();
        invalidateOnDataChange();
    }
    @Override
    public void removeApps(ArrayList<ShortcutInfo> list) {
        //removeAppsWithoutInvalidate(list);
        //updatePageCounts();
        invalidateOnDataChange();
    }
    @Override
    public void updateApps(ArrayList<ShortcutInfo> list) {
        // We remove and re-add the updated applications list because it's properties may have
        // changed (ie. the title), and this will ensure that the items will be in their proper
        // place in the list.
        invalidateOnDataChange();
    }

    @Override
    public void reset() {
        // If we have reset, then we should not continue to restore the previous state
        mSaveInstanceStateItemIndex = -1;

                // Reset to the first page of the Apps pane
                AppsCustomizeTabHost tabs = (AppsCustomizeTabHost)
                    mLauncher.findViewById(R.id.apps_customize_pane);
            	 tabs.selectWallpapersTab();

        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }

    private AppsCustomizeTabHost getTabHost() {
        return (AppsCustomizeTabHost) mLauncher.findViewById(R.id.apps_customize_pane);
    }

    @Override
    public void dumpState() {
        // TODO: Dump information related to current list of Applications, Widgets, etc.
        //ApplicationInfo.dumpApplicationInfoList(TAG, "mApps", mApps);
        dumpAppWidgetProviderInfoList(TAG, "mWidgets", mWidgets);
    }

    private void dumpAppWidgetProviderInfoList(String tag, String label,
            ArrayList<Object> list) {
        Log.d(tag, label + " size=" + list.size());
        for (Object i: list) {
            if (i instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) i;
                Log.d(tag, "   label=\"" + info.label + "\" previewImage=" + info.previewImage
                        + " resizeMode=" + info.resizeMode + " configure=" + info.configure
                        + " initialLayout=" + info.initialLayout
                        + " minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
            } else if (i instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) i;
                Log.d(tag, "   label=\"" + info.loadLabel(mPackageManager) + "\" icon="
                        + info.icon);
            }
        }
    }

    @Override
    public void surrender() {
        // TODO: If we are in the middle of any process (ie. for holographic outlines, etc) we
        // should stop this now.

        // Stop all background tasks
        cancelAllTasks();
    }

    /*
     * We load an extra page on each side to prevent flashes from scrolling and loading of the
     * widget previews in the background with the AsyncTasks.
     */
//    final static int sLookBehindPageCount = 3;
//    final static int sLookAheadPageCount = 3;
    protected int getAssociatedLowerPageBound(int page) {
//        final int count = getChildCount();
//        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
//        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
//        return windowMinIndex;
        return Math.max(0, page-2);
    	
    }
    protected int getAssociatedUpperPageBound(int page) {
//        final int count = getChildCount();
//        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
//        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1),
//                count - 1);
//        return windowMaxIndex;
    	return Math.min(page+2, getChildCount() - 1);
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;

            switch (mContentType) {
            case Widgets:
                stringId = R.string.apps_customize_widgets_scroll_format;
                break;
            case Wallpapers:
                stringId = R.string.apps_customize_wallpapers_scroll_format;
                break;
            case Effects:
                stringId = R.string.apps_customize_effects_scroll_format;
                break;
                
            case Themes:
                stringId = R.string.apps_customize_themes_scroll_format;
                break;
            }
            return String.format(mContext.getString(stringId), page + 1, getChildCount());
    }

	@Override
	public boolean isDropEnabled() {
		// TODO Auto-generated method stub
		if(CellLayout.mIsEditstate){
			return true;
		}else{
			return false;	
		}
		
	}

	@Override
	public void onDrop(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragEnter(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragOver(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragExit(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DropTarget getDropTargetDelegate(DragObject dragObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean acceptDrop(DragObject dragObject) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void getLocationInDragLayer(int[] loc) {
		//Log.i(Launcher.TAG, TAG+"######################  getLocationInDragLayer=="+loc);
		mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragStart(DragSource source, Object info, int dragAction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragEnd() {
		// TODO Auto-generated method stub
		
	}
	

    public void getHitRect(Rect outRect){
    //	super.getHitRect(outRect);
    	/*outRect.left=0;
    	outRect.right=mLauncher.mscreenwidth;
    	
    	outRect.top=mLauncher.mscreenHeight-250;
    	outRect.bottom=mLauncher.mscreenHeight;*/
    	outRect.set(0,0,mLauncher.mscreenwidth,mLauncher.mscreenHeight);
    	
	 }

	@Override
	public void onFlingToDelete(DragObject dragObject, int x, int y, PointF vec) {
		// TODO Auto-generated method stub
		
	}
}
