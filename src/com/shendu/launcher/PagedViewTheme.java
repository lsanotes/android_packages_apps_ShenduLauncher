package com.shendu.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 2012-8-31 hhl
 * PagedViewWallpaper.java
 * Trebuchet
 * TODO: editstate bottom layout choice wallpaper display item view
 */
public class PagedViewTheme extends FrameLayout {
	
    public PagedViewTheme(Context context) {
        this(context, null);
    }

    public PagedViewTheme(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewTheme(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void applyFromShenduPrograme(ShenduPrograme info) {
        int bgResId = 0;
        ImageView wallpaperIcon = (ImageView)findViewById(R.id.editstate_tabhost_tabcontent_theme_id);
        TextView wallpaperMark = (TextView)findViewById(R.id.editstate_tabhost_tabcontent_theme_mark_id);
//        if(info.getChoice()==ShenduPrograme.CHOICE_WALLPAPER_LAUNCHER){
//        	bgResId = R.drawable.editstate_tabhost_tabcontent_wallpaper_bg;
//        	wallpaperIcon.setImageBitmap(info.mThemeBitmap);
//        }else if(info.getChoice()==ShenduPrograme.CHOICE_WALLPAPER_CURRENT){
//        	bgResId = R.drawable.editstate_tabhost_tabcontent_wallpaper_bg;
//        	wallpaperIcon.setImageBitmap(info.mThemeBitmap);
//        	wallpaperMark.setVisibility(View.VISIBLE);
//        }else if(info.getChoice()==ShenduPrograme.CHOICE_WALLPAPER_MORE){
//        	bgResId = info.getResSmallId();
//        }
        wallpaperIcon.setImageBitmap(info.mThemeBitmap);
        
        setBackgroundResource(bgResId);
        setTag(info);
    }

}