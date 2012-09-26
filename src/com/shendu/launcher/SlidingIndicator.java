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
    public static final int BAR_COLOR = 0xbbADADAD;  
    public static final int HIGHLIGHT_COLOR = 0Xff0072E3;  
    public static final int FADE_DELAY = 2000;  
    public static final int FADE_DURATION = 0;  
  
    private int amount, currentPage;  
    private Paint barPaint, highlightPaint;  
    private int fadeDelay, fadeDuration;  
    private float ovalRadius;  
    private Animation animFadeout;  
   
    private RectF rectFBody, rectFIndicator;  
    private Handler handler;
    
    
   
	private  Bitmap defaultPoint, highLightPoint ,flashPoint;

  
    public SlidingIndicator(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
    
        int barColor = BAR_COLOR, highlightColor = HIGHLIGHT_COLOR;  
        fadeDelay = FADE_DELAY;  
        fadeDuration = FADE_DURATION;  
        if (attrs != null) {  
            TypedArray typedArr = context.obtainStyledAttributes(attrs, R.styleable.sliding_SlidingIndicator);  
            barColor = typedArr.getColor(R.styleable.sliding_SlidingIndicator_barColor, BAR_COLOR);  
            highlightColor = typedArr.getColor(R.styleable.sliding_SlidingIndicator_highlightColor, HIGHLIGHT_COLOR);  
            fadeDelay = typedArr.getInteger(R.styleable.sliding_SlidingIndicator_fadeDelay, FADE_DELAY);  
            fadeDuration = typedArr.getInteger(R.styleable.sliding_SlidingIndicator_fadeDuration, FADE_DURATION);  
            ovalRadius = typedArr.getDimension(R.styleable.sliding_SlidingIndicator_roundRectRadius, 2.0f);  
            typedArr.recycle();  
        }  
        initialization(barColor, highlightColor, fadeDuration);  
    }  
  
    public SlidingIndicator(Context context, AttributeSet attrs) {  
        this(context, attrs, 0);  
  
    }  
  
    public SlidingIndicator(Context context) {  
        super(context);  
    }  
  
    private void initialization(int barColor, int highlightColor, int fadeDuration) {  
    	
    	
    	
//        barPaint = new Paint();  
//        barPaint.setColor(barColor);  
//        barPaint.setAntiAlias(true);  
//        highlightPaint = new Paint();  
//        highlightPaint.setColor(highlightColor);  
//        highlightPaint.setAntiAlias(true); 
        
//        animFadeout = new AlphaAnimation(1f, 0f);  
//        animFadeout.setDuration(fadeDuration);  
//        animFadeout.setRepeatCount(0);  
//        animFadeout.setInterpolator(new LinearInterpolator());  
//       
//        animFadeout.setFillEnabled(true);  
//        animFadeout.setFillAfter(true);  
  
        rectFBody = new RectF();  
        rectFIndicator = new RectF();  
        
        Resources res = getResources(); 
        defaultPoint   = BitmapFactory.decodeResource(res, R.drawable.default_point1);  ;
        
        highLightPoint = BitmapFactory.decodeResource(res,R.drawable.high_lightpoint1);
        flashPoint     =  BitmapFactory.decodeResource(res,R.drawable.flash_point);
    }  
  
    public void setPageAmount(int num) {  
        if (num < 0) {  
            throw new IllegalArgumentException("num must be positive.");  
        }  
        amount = num;  
        invalidate();  
      //  fadeOut();  
    }  
  
//    private void fadeOut() {  
//        if (fadeDuration > 0) {  
//            clearAnimation();  
//        
//            animFadeout.setStartTime(AnimationUtils.currentAnimationTimeMillis() + fadeDelay);  
//            setAnimation(animFadeout);  
//        }  
//    }  
  
    public int getCurrentPage() {  
        return currentPage;  
    }  
  
    public void setCurrentPage(int idx) {  
        if (currentPage < 0) {  

            throw new IllegalArgumentException("currentPage parameter out of bounds");  
        }  

            this.currentPage = idx;
          //  this.position = currentPage * getPageWidth(); 
            invalidate();  
          //  fadeOut();  
        
    }  
  
//    public void setPosition(int position) {  
//        if (this.position != position) {  
//
//            this.position = position;  
//            invalidate();  
//            fadeOut();  
//        }  
//    }  
  
//    public int getPosition(){
//    	return this.position;
//    }
    
    public int getPageWidth() {  
    		return getWidth() / amount;  
    }  
  
    protected void onDraw(Canvas canvas) {  
    
        // getWidth()/2-(amount-1)*20/2-8（图片的半径）;  
    	int position=getWidth()/2-(amount-1)*15-8;
    	int positionY =getHeight()-13;
    	for(int i=0;i<amount;i++){
    	
    		  //  rectFBody.set(position+40*i,0, position+40*i+20, 20);  
    		
    		if(currentPage == i){
    			canvas.drawBitmap(highLightPoint, position+30*i, positionY, null);
    		//	canvas.drawCircle(position+40*i, positionY-6, 6, highlightPaint);  
    		}else{
    			canvas.drawBitmap(defaultPoint, position+30*i, positionY+1, null);
    		//	canvas.drawCircle(position+40*i, positionY-6, 6, barPaint);  
    		}
    	        
    	}
       
       // rectFIndicator.set(position+currentPage*40, 0, position+currentPage*40+20, getHeight());  
      //  canvas.drawRoundRect(rectFIndicator, ovalRadius, ovalRadius, highlightPaint);  
        
       // canvas.drawCircle(position+40*currentPage, positionY, 7, highlightPaint);  
    }  
    

    
  
}  