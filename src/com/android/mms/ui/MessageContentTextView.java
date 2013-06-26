//zhangchao add
package com.android.mms.ui;
import android.view.View;
import android.widget.TextView;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.util.Log;
import android.content.Context;
import android.text.style.ClickableSpan;
import android.text.Spannable;
import android.text.Selection;
import android.text.Layout;

public class MessageContentTextView extends TextView
{
    private final static String TAG =  "MessageTextView";
    private static int mMoveCount = 0;
    public MessageContentTextView(Context context) {
        super(context, null);
    }

    public MessageContentTextView(Context context,
                    AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.textViewStyle);
//	            setFocusable(false);
//	            setClickable(false);
//	            setLongClickable(false);        
    }    

//	    @Override
//	    public boolean performLongClick() {
//	        Log.d(TAG,"+++++++++performLongClick++++++++");
//	        return false;
//	    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Log.d(TAG,"dispatchTouchEvent action: " + action);
        boolean result =  super.dispatchTouchEvent(event);
        Log.d(TAG,"dispatchTouchEvent result: " + result);
        return result;
    }
//	    @Override
//	    boolean interceptTouchEvent(MotionEvent event) {
//	        int action = event.getAction();
//	        Log.d(TAG,"interceptTouchEvent action: " + action);
//	        boolean result =  super.interceptTouchEvent(event);
//	        Log.d(TAG,"interceptTouchEvent result: " + result);
//	        return result;
//	    }    

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE) {
            mMoveCount++;
            if(mMoveCount > 5)
            {
                Log.d(TAG,"-------------onTouchEvent: cancelLongPress");
                cancelLongPress();
                mMoveCount = 0;
            }
        }
        else if(action == MotionEvent.ACTION_DOWN)
        {
            mMoveCount = 0;
        }

        return super.onTouchEvent( event);
    }

}

