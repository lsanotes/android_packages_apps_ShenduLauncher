package com.shendu.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * 2012-8-31 hhl
 * PagedViewTheme.java
 * Trebuchet
 * TODO: editstate bottom layout choice theme display item view
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
		ImageView wallpaperIcon = (ImageView) findViewById(R.id.editstate_tabhost_tabcontent_theme_id);
		if (info.getChoice() == ShenduPrograme.CHOICE_WALLPAPER_MORE) {
			bgResId = info.getResSmallId();
		} else {
			bgResId = R.drawable.editstate_tabhost_tabcontent_wallpaper_bg;
			wallpaperIcon.setImageBitmap(info.mThemeBitmap);
		}
		setBackgroundResource(bgResId);
		setTag(info);
	}

}