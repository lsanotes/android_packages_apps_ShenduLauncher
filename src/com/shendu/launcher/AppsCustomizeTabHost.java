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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.shendu.launcher.R;
import com.shendu.launcher.preference.PreferencesProvider;

import java.util.ArrayList;

public class AppsCustomizeTabHost extends TabHost implements LauncherTransitionable,
        TabHost.OnTabChangeListener  {
    static final String LOG_TAG = "AppsCustomizeTabHost";

    //private static final String APPS_TAB_TAG = "APPS";
    private static final String WALLPAPERS_TAB_TAG = "WALLPAPERS";
    private static final String WIDGETS_TAB_TAG = "WIDGETS";
    private static final String THEMES_TAB_TAG = "THEMES";
    private static final String EFFECTS_TAB_TAG = "EFFECTS";

    private final LayoutInflater mLayoutInflater;
    private ViewGroup mTabs;
    private ViewGroup mTabsContainer;
    private AppsCustomizePagedView mAppsCustomizePane;
    private boolean mSuppressContentCallback = false;
    //private FrameLayout mAnimationBuffer;
    public LinearLayout mContent;

    private boolean mInTransition;
    private boolean mTransitioningToWorkspace;
    private boolean mResetAfterTransition;
    private Animator mLauncherTransition;
    
    private FrameLayout mTabViewLayout;
    private TextView mTabLabel;
    private ImageView mTabLabelIndicator;
    private Runnable mRelayoutAndMakeVisible;

    //private Launcher mLauncher;

    // Preferences
    //private boolean mJoinWidgetsApps;
    //private boolean mFadeScrollingIndicator;

    public AppsCustomizeTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mRelayoutAndMakeVisible = new Runnable() {
                public void run() {
                    mTabs.requestLayout();
                    mTabsContainer.setAlpha(1f);
                }
            };
    }

    public void setup(Launcher launcher) {
        //mLauncher = launcher;
    }

    /**
     * Convenience methods to select specific tabs.  We want to set the content type immediately
     * in these cases, but we note that we still call setCurrentTabByTag() so that the tab view
     * reflects the new content (but doesn't do the animation and logic associated with changing
     * tabs manually).
     */
    private void setContentTypeImmediate(AppsCustomizePagedView.ContentType type) {
        onTabChangedStart();
        onTabChangedEnd(type);
        mAppsCustomizePane.shenduUpdateTheArrowImageView(0);
        
    }
    void selectWidgetsTab() {
        setContentTypeImmediate(AppsCustomizePagedView.ContentType.Widgets);
        //mAppsCustomizePane.setCurrentPageToWidgets();
        setCurrentTabByTag(WIDGETS_TAB_TAG);
    }
    
    void selectWallpapersTab() {//add by hhl,used to set current tab
        setContentTypeImmediate(AppsCustomizePagedView.ContentType.Wallpapers);
        //mAppsCustomizePane.setCurrentPageToWallpapers();
        setCurrentTabByTag(WALLPAPERS_TAB_TAG);
    }

    /**
     * Setup the tab host and create all necessary tabs.
     */
    @Override
    protected void onFinishInflate() {
        // Setup the tab host
        setup();

        final ViewGroup tabsContainer = (ViewGroup) findViewById(R.id.tabs_container);
        final TabWidget tabs = getTabWidget();
        final AppsCustomizePagedView appsCustomizePane = (AppsCustomizePagedView)
                findViewById(R.id.apps_customize_pane_content);
        mTabs = tabs;
        mTabsContainer = tabsContainer;
        mAppsCustomizePane = appsCustomizePane;
        //mAnimationBuffer = (FrameLayout) findViewById(R.id.animation_buffer);
        mContent = (LinearLayout) findViewById(R.id.apps_customize_content);
        if (tabs == null || mAppsCustomizePane == null) throw new Resources.NotFoundException();

        // Configure the tabs content factory to return the same paged view (that we change the
        // content filter on)
        TabContentFactory contentFactory = new TabContentFactory() {
            public View createTabContent(String tag) {
                return appsCustomizePane;
            }
        };

        String label;
        
        //choice widget
        label = mContext.getString(R.string.editstate_choice_widget);
        mTabViewLayout = (FrameLayout) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        mTabLabel = (TextView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_textview_id);
        mTabLabel.setText(label);
        mTabViewLayout.setContentDescription(label);
        addTab(newTabSpec(WIDGETS_TAB_TAG).setIndicator(mTabViewLayout).setContent(contentFactory));
        
        
        //choice wallpaper
        label = mContext.getString(R.string.editstate_choice_wallpaper);
        mTabViewLayout = (FrameLayout) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        mTabLabel = (TextView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_textview_id);
        mTabLabel.setText(label);
        mTabViewLayout.setContentDescription(label);
        addTab(newTabSpec(WALLPAPERS_TAB_TAG).setIndicator(mTabViewLayout).setContent(contentFactory));
        setCurrentTabByTag(WALLPAPERS_TAB_TAG);
        onTabChanged(WALLPAPERS_TAB_TAG);
        //choice specially effect
        label = mContext.getString(R.string.editstate_choice_specially_effect);
        mTabViewLayout = (FrameLayout) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        mTabLabel = (TextView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_textview_id);
        mTabLabel.setText(label);
        mTabViewLayout.setContentDescription(label);
        addTab(newTabSpec(EFFECTS_TAB_TAG).setIndicator(mTabViewLayout).setContent(contentFactory));
        
        
        
        //choice specially theme
        label = mContext.getString(R.string.editstate_choice_theme);
        mTabViewLayout = (FrameLayout) mLayoutInflater.inflate(R.layout.tab_widget_indicator, tabs, false);
        mTabLabel = (TextView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_textview_id);
        mTabLabel.setText(label);
        mTabViewLayout.setContentDescription(label);
        addTab(newTabSpec(THEMES_TAB_TAG).setIndicator(mTabViewLayout).setContent(contentFactory));
        
        
        setOnTabChangedListener(this);


        // Hide the tab bar until we measure
        mTabsContainer.setAlpha(0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean remeasureTabWidth = (mTabs.getLayoutParams().width <= 0);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set the width of the tab list to the content width
        if (remeasureTabWidth) {
            int contentWidth = mAppsCustomizePane.getPageContentWidth();
            if (contentWidth > 0 && mTabs.getLayoutParams().width != contentWidth) {
                // Set the width and show the tab bar
                mTabs.getLayoutParams().width = contentWidth;
                post(mRelayoutAndMakeVisible);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

     public boolean onInterceptTouchEvent(MotionEvent ev) {
         // If we are mid transitioning to the workspace, then intercept touch events here so we
         // can ignore them, otherwise we just let all apps handle the touch events.
         if (mInTransition && mTransitioningToWorkspace) {
             return true;
         }
         return super.onInterceptTouchEvent(ev);
     };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Allow touch events to fall through to the workspace if we are transitioning there
        if (mInTransition && mTransitioningToWorkspace) {
            return super.onTouchEvent(event);
        }

        // Intercept all touch events up to the bottom of the AppsCustomizePane so they do not fall
        // through to the workspace and trigger showWorkspace()
        if (event.getY() < mAppsCustomizePane.getBottom()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void onTabChangedStart() {
        //mAppsCustomizePane.hideScrollingIndicator(false);
    }

    private void reloadCurrentPage() {
        mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());
        mAppsCustomizePane.requestFocus();
    }

    private void onTabChangedEnd(AppsCustomizePagedView.ContentType type) {
    }

    @Override
    public void onTabChanged(String tabId) {
        final AppsCustomizePagedView.ContentType type = getContentTypeForTabTag(tabId);
        if (mSuppressContentCallback) {
            mSuppressContentCallback = false;
            return;
        }

        int size = getTabWidget().getChildCount();
        for(int i=0;i<size;i++){ 
        	mTabViewLayout = (FrameLayout)getTabWidget().getChildAt(i);
            mTabLabel = (TextView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_textview_id);
            mTabLabelIndicator = (ImageView)mTabViewLayout.findViewById(R.id.editstate_tabhost_tabwidget_label_indicator_id);
        	if(getCurrentTab()==i){
        		//mTabLabel.setTextColor(Color.rgb(12, 141, 234));
        		mTabLabel.setTextColor(getResources().getColor(R.color.tab_text_press));
        		mTabLabelIndicator.setVisibility(View.VISIBLE);
        	}else{
        		//mTabLabel.setTextColor(Color.BLACK);
        		mTabLabel.setTextColor(getResources().getColor(R.color.tab_text_unpress));
        		mTabLabelIndicator.setVisibility(View.GONE);
        	}
        }
        
        if (!mAppsCustomizePane.isContentType(type)) {
           	// Animate the changing of the tab content by fading pages in and out
        	   //final Resources res = getResources();
              //final int duration = res.getInteger(R.integer.config_tabTransitionDuration);
              mAppsCustomizePane.shenduUpdateTheArrowImageView(0);
              if(type.equals(AppsCustomizePagedView.ContentType.Wallpapers) || 
            		  type.equals(AppsCustomizePagedView.ContentType.Widgets) ||
            		  type.equals(AppsCustomizePagedView.ContentType.Effects) ||
            		  type.equals(AppsCustomizePagedView.ContentType.Themes)){
                // We post a runnable here because there is a delay while the first page is loading and
                // the feedback from having changed the tab almost feels better than having it stick
                post(new Runnable() {
                	@Override
                  public void run() {
                  //getMeasuredHeight());
                		if (getMeasuredWidth() <= 0 || getMeasuredHeight() <= 0) {
                			reloadCurrentPage();
                         return;
                        }
                		// Take the visible pages and re-parent them temporarily to mAnimatorBuffer
                		// and then cross fade to the new pages
                		int[] visiblePageRange = new int[2];
                		mAppsCustomizePane.getVisiblePages(visiblePageRange);
                		if (visiblePageRange[0] == -1 && visiblePageRange[1] == -1) {
                			// If we can't get the visible page ranges, then just skip the animation
                         reloadCurrentPage();
                         return;
                        }
                		mAppsCustomizePane.setContentType(type);
                		reloadCurrentPage();
                	}
                });
              }
           }

    }

    public void setCurrentTabFromContent(AppsCustomizePagedView.ContentType type) {
        mSuppressContentCallback = true;
        setCurrentTabByTag(getTabTagForContentType(type));
    }

    /**
     * Returns the content type for the specified tab tag.
     */
    public AppsCustomizePagedView.ContentType getContentTypeForTabTag(String tag) {
        /*if (tag.equals(APPS_TAB_TAG)) {
            return AppsCustomizeView.ContentType.Apps;
        } else*/ 
    	//Log.i(Launcher.TAG, "=AppsCustomizaTabHost.java=getContentTypeForTabTag=="+tag);
        if (tag.equals(WIDGETS_TAB_TAG)) {
            return AppsCustomizePagedView.ContentType.Widgets;
        } else if(tag.equals(WALLPAPERS_TAB_TAG)){
            return AppsCustomizePagedView.ContentType.Wallpapers;
        } else if(tag.equals(THEMES_TAB_TAG)){
            return AppsCustomizePagedView.ContentType.Themes;
        } else if(tag.equals(EFFECTS_TAB_TAG)){
            return AppsCustomizePagedView.ContentType.Effects;
        }
        return AppsCustomizePagedView.ContentType.Widgets;
    }

    /**
     * Returns the tab tag for a given content type.
     */
    public String getTabTagForContentType(AppsCustomizePagedView.ContentType type) {
        /*if (type == AppsCustomizePagedView.ContentType.Applications) {
            return APPS_TAB_TAG;
        } else */
    	//Log.i(Launcher.TAG, "=AppsCustomizaTabHost.java=getTabTagForContentType=="+type);
		if (type == AppsCustomizePagedView.ContentType.Widgets) {
            return WIDGETS_TAB_TAG;
        }else if(type == AppsCustomizePagedView.ContentType.Wallpapers){
        	return WALLPAPERS_TAB_TAG;
        }else if(type == AppsCustomizePagedView.ContentType.Effects){
        	return EFFECTS_TAB_TAG;
        } else if(type == AppsCustomizePagedView.ContentType.Themes){
        	return THEMES_TAB_TAG;
        }
        return WIDGETS_TAB_TAG;
    }

    /**
     * Disable focus on anything under this view in the hierarchy if we are not visible.
     */
    @Override
    public int getDescendantFocusability() {
        if (getVisibility() != View.VISIBLE) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    void reset() {
        if (mInTransition) {
            // Defer to after the transition to reset
            mResetAfterTransition = true;
        } else {
            // Reset immediately
            mAppsCustomizePane.reset();
        }
    }

    private void enableAndBuildHardwareLayer() {
        // isHardwareAccelerated() checks if we're attached to a window and if that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (isHardwareAccelerated()) {
            // Turn on hardware layers for performance
            setLayerType(LAYER_TYPE_HARDWARE, null);

            // force building the layer, so you don't get a blip early in an animation
            // when the layer is created layer
            buildLayer();

            // Let the GC system know that now is a good time to do any garbage
            // collection; makes it less likely we'll get a GC during the all apps
            // to workspace animation
            System.gc();
        }
    }

    @Override
    public View getContent() {
        return mContent;
    }

    /* LauncherTransitionable overrides */
    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mAppsCustomizePane.onLauncherTransitionPrepare(l, animated, toWorkspace);
        mInTransition = true;
        mTransitioningToWorkspace = toWorkspace;

        if (toWorkspace) {
            // Going from All Apps -> Workspace
            setVisibilityOfSiblingsWithLowerZOrder(VISIBLE);
            // Stop the scrolling indicator - we don't want All Apps to be invalidating itself
            // during the transition, especially since it has a hardware layer set on it
            //mAppsCustomizePane.cancelScrollingIndicatorAnimations();
        } else {
            // Going from Workspace -> All Apps
            mContent.setVisibility(VISIBLE);

            // Make sure the current page is loaded (we start loading the side pages after the
            // transition to prevent slowing down the animation)
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage(), true);

           /* if (!LauncherApplication.isScreenLarge()) {
                mAppsCustomizePane.showScrollingIndicator(true);
            }*/
        }

        if (mResetAfterTransition) {
            mAppsCustomizePane.reset();
            mResetAfterTransition = false;
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
        if (animated) {
            enableAndBuildHardwareLayer();
        }
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        // Do nothing
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mAppsCustomizePane.onLauncherTransitionEnd(l, animated, toWorkspace);
        mInTransition = false;
        if (animated) {
            setLayerType(LAYER_TYPE_NONE, null);
        }

        if (!toWorkspace) {
            // Going from Workspace -> All Apps
        	  //remove by hhl
            //setVisibilityOfSiblingsWithLowerZOrder(INVISIBLE);

            // Dismiss the workspace cling and show the all apps cling (if not already shown)
            //l.dismissWorkspaceCling(null);
            //mAppsCustomizePane.showAllAppsCling();
            // Make sure adjacent pages are loaded (we wait until after the transition to
            // prevent slowing down the animation)
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());

            /*if (!LauncherApplication.isScreenLarge() && mFadeScrollingIndicator) {
                mAppsCustomizePane.hideIndicator(false);
            }*/
        }
    }

    private void setVisibilityOfSiblingsWithLowerZOrder(int visibility) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;

        final int count = parent.getChildCount();
        if (!isChildrenDrawingOrderEnabled()) {
            for (int i = 0; i < count; i++) {
                final View child = parent.getChildAt(i);
                if (child == this) {
                    break;
                } else {
                    if (child.getVisibility() == GONE) {
                        continue;
                    }
                    child.setVisibility(visibility);
                }
            }
        } else {
            throw new RuntimeException("Failed; can't get z-order of views");
        }
    }

    public void onWindowVisible() {
        if (getVisibility() == VISIBLE) {
            mContent.setVisibility(VISIBLE);
            // We unload the widget previews when the UI is hidden, so need to reload pages
            // Load the current page synchronously, and the neighboring pages asynchronously
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage(), true);
            mAppsCustomizePane.loadAssociatedPages(mAppsCustomizePane.getCurrentPage());
        }
    }

    public void onTrimMemory() {
        mContent.setVisibility(GONE);
        // Clear the widget pages of all their subviews - this will trigger the widget previews
        // to delete their bitmaps
        mAppsCustomizePane.clearAllWidgetPages();
    }

    boolean isTransitioning() {
        return mInTransition;
    }
}
