package com.shendu.launcher;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public class SlidingIndicator extends View {  
	
	private int amount, currentPage;  
	private  Bitmap defaultPoint, highLightPoint ;//,flashPoint;
	private int defaultPointW,highLightPointW;
  
	public SlidingIndicator(Context context, AttributeSet attrs, int defStyle) {  
		super(context, attrs, defStyle);  
		if (attrs != null) {  
			TypedArray typedArr = context.obtainStyledAttributes(attrs, R.styleable.sliding_SlidingIndicator);  
			typedArr.recycle();  
		}  
		initialization();  
	}  
  
	public SlidingIndicator(Context context, AttributeSet attrs) {  
		this(context, attrs, 0);  
  
	}  
  
	public SlidingIndicator(Context context) {  
    	super(context);  
    }  
  
	private void initialization() {  
    	
		Resources res = getResources(); 
		defaultPoint   = BitmapFactory.decodeResource(res, R.drawable.default_point1);  ;
		highLightPoint = BitmapFactory.decodeResource(res,R.drawable.high_lightpoint1);
		defaultPointW = defaultPoint.getWidth();
		highLightPointW = highLightPoint.getWidth();
	}  
  
    public void setPageAmount(int num) {  
    	if (num < 0) {  
    		throw new IllegalArgumentException("num must be positive.");  
    	}  
    	amount = num;  
    	invalidate();  
	}  
  
  
    public int getCurrentPage() {  
        return currentPage;  
    }  
  
    public void setCurrentPage(int idx) {  
    	if (currentPage < 0) {
    		 currentPage =0;
        }  
    	this.currentPage = idx;
    	invalidate();  
        
	}  
  
	public int getPageWidth() {  
    	return getWidth() / amount;  
    }  
  
	protected void onDraw(Canvas canvas) {  
    	int position=getWidth()/2-(amount-1)*20/2;
    	int positionY =getHeight()-highLightPointW;
    	for(int i=0;i<amount;i++){
    		if(currentPage == i){
    			canvas.drawBitmap(highLightPoint, position-highLightPointW/2+20*i, positionY, null);
    		}else{
    			canvas.drawBitmap(defaultPoint, position-defaultPointW/2+20*i, positionY+1, null);
    		}
    	}
	}  
  
}  