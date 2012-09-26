package com.shendu.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ShenduPrograme {

	public boolean isEffectCurrent() {
		return effectCurrent;
	}
	public void setEffectCurrent(boolean effectCurrent) {
		this.effectCurrent = effectCurrent;
	}
	public int getEffectStrId() {
		return effectStrId;
	}
	public void setEffectStrId(int effectStrId) {
		this.effectStrId = effectStrId;
	}
	public int getEffectDrawableId() {
		return effectDrawableId;
	}
	public void setEffectDrawableId(int effectDrawableId) {
		this.effectDrawableId = effectDrawableId;
	}
	public boolean isLiveWallpaper() {
		return liveWallpaper;
	}
	public void setLiveWallpaper(boolean liveWallpaper) {
		this.liveWallpaper = liveWallpaper;
	}
	public int getChoice() {
		return choice;
	}
	public void setChoice(int choice) {
		this.choice = choice;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Drawable getResDrawable() {
		return resDrawable;
	}
	public void setResDrawable(Drawable resDrawable) {
		this.resDrawable = resDrawable;
	}
	public int getResId() {
		return resId;
	}
	public void setResId(int resId) {
		this.resId = resId;
	}
	public int getResSmallId() {
		return resSmallId;
	}
	public void setResSmallId(int resSmallId) {
		this.resSmallId = resSmallId;
	}
	public Intent getIntent() {
		return intent;
	}
	public void setIntent(Intent intent) {
		this.intent = intent;
	}
	public ComponentName getComponentname() {
		return componentname;
	}
	public void setComponentname(ComponentName componentname) {
		this.componentname = componentname;
	}
	public static final int CHOICE_WALLPAPER_LAUNCHER = 1;
	public static final int CHOICE_WALLPAPER_CURRENT = 2;
	public static final int CHOICE_WALLPAPER_MORE = 3;
	private int resId;
	private int resSmallId;
	private Intent intent;
	private Drawable resDrawable;
	private String name;
	private int choice;
	private boolean liveWallpaper;
	private ComponentName componentname;
	private int effectStrId;
	private int effectDrawableId;
	private boolean effectCurrent;

}
