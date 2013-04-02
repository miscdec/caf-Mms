package com.android.mms.ui;

import com.android.mms.R;
import java.io.InputStream;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.net.Uri;
import android.content.res.AssetManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.database.Cursor;
import android.widget.ImageView;
import java.io.File;
import java.io.FileInputStream;
import android.content.ContentResolver;
import com.android.mms.layout.LayoutManager;

public class GIFView extends ImageView implements GifAction{

    private static final String TAG = "GIFView";
    private GifDecoder gifDecoder = null;

    private Bitmap currentImage = null;
    private boolean isRun = false;
    private boolean pause = true;

    private final int W;

    private final int H;

    static final int MSG_REDRAW    = 1;
    static final int MSG_SET_IMAGE = 2;
    
    private DrawThread drawThread = null;

    Uri mUri;
    public GIFView(Context context) {
        super(context);
        Log.v(TAG," gifview constructor.");
        //W = 240;
        //H = 320;
        if(LayoutManager.getInstance().getLayoutParameters().getTypeDescription().equals("HVGA-P"))
            {
            W = 480;
            H = 800;
            }
        else
            {
            W = 800;
            H = 480;
            }
        //W = LayoutManager.getInstance().getLayoutParameters().getWidth();
       // H = LayoutManager.getInstance().getLayoutParameters().getHeight();        
    }

    public boolean setDrawable(Uri uri){
        if (null == uri){
            return false;
        }
        isRun = true;
        pause = false;
        mUri = uri;
        int mSize = 0;
        ContentResolver cr = mContext.getContentResolver();
        InputStream input = null;
        try {
            input = cr.openInputStream(uri);
            
            if (input instanceof FileInputStream) {
                FileInputStream f = (FileInputStream) input;
                mSize = (int) f.getChannel().size();
            } else {
                while (-1 != input.read()) {
                    mSize++;
                }
            }

        } catch (IOException e) {
            
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    
                }
            }
        }

        if (mSize > 260*1024)
        {
            Log.v(TAG," GIF size too large mSize = " + mSize);
            return false;
        }
        android.content.ContentResolver resolver = mContext.getContentResolver();
        Cursor c = resolver.query(uri, new String[]{"_data"}, null, null, null);
        if ( c != null && 1 == c.getCount()){
            c.moveToFirst();        
            Log.v(TAG, "file=" + c.getString(0));
            AssetManager am = new AssetManager();
            am.addAssetPath("/data/data/com.android.providers.telephony/app_parts/");
            try{                        
                setGifDecoderImage(am.open(c.getString(0), AssetManager.ACCESS_RANDOM));
            }catch(FileNotFoundException e){
                Log.v(TAG, "e:" + e);
            }catch(IOException e){
                Log.v(TAG, "e:" + e);
            }finally{
                am.close();
                c.close();
            }
            
            return true;
        }

        return false;
    }
    
    private void setGifDecoderImage(InputStream is){
    if(gifDecoder != null){
            gifDecoder.free();
            gifDecoder= null;
        }
    gifDecoder = new GifDecoder(is, this);
    gifDecoder.start();
    }
    
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gifDecoder == null)
        {
            Log.v(TAG," gifDecoder is null.");
            return;
        }
        if (currentImage == null)
        {        
            currentImage = gifDecoder.getImage();
        }
        if (currentImage == null)
        {
            Log.v(TAG," currentImage is null.");        
            //setImageURI(mUri);
            return;
        }
        setImageURI(null);
        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
//        canvas.drawBitmap(currentImage, (W - currentImage.getWidth()) / 2, (W - currentImage.getHeight() - 40)/8, null);
        
        Rect sRect = null;        
        Rect dRect = null;
        Log.v(TAG," W = " + W
                + "H = " + H
                + "currentImage.getWidth() = " + currentImage.getWidth()
                + "currentImage.getHeight() = " + currentImage.getHeight());
        
        int imageHeight = currentImage.getHeight();
        int imageWidth = currentImage.getWidth();

        //int newHeight = H/2;
        int newHeight = H;        
        int newWidth = W;
        
        if (newWidth < imageWidth)
        {
            if (newHeight < imageHeight)
            {
                //h big, w big;                            
                Log.v(TAG," h big, w big");
                if (imageHeight*W > imageWidth*H)
                {
                   //too height                
                   //newHeight = H/2;
                   newWidth = (imageWidth * newHeight)/imageHeight;    
                   Log.v(TAG," h big, w big");                
                }
                else
                {
                    newWidth = W;
                    newHeight = (imageHeight * newWidth)/imageWidth;                   
                }                
                
                //sRect = new Rect(0, 0, currentImage.getWidth(), currentImage.getHeight());                
                dRect = new Rect((W - newWidth) / 2, 0, (W + newWidth) / 2, newHeight);
            }
            else
            {
                //h small, w big;
                newHeight = (imageHeight * newWidth)/imageWidth;
                dRect = new Rect(0, 0, newWidth, newHeight);
            }
            canvas.drawBitmap(currentImage, sRect, dRect, null);
            
        }
        else if (newHeight < imageHeight)
        {
            //h big, w small;        
            newWidth = (imageWidth * newHeight)/imageHeight;
            dRect = new Rect((W - newWidth) / 2, 0, 
                (W + newWidth) / 2, newHeight);    
            canvas.drawBitmap(currentImage, sRect, dRect, null);                
        }
        else
        {
            //h small, w small;
            canvas.drawBitmap(currentImage, (W - imageWidth) / 2, 0, null);
        }
        canvas.restoreToCount(saveCount);
    }
 
    public void parseOk(boolean parseStatus,int frameIndex){
        Log.v(TAG," parseStatus = " + parseStatus
                    + "frameIndex = " + frameIndex);        
        if(parseStatus){
            if(gifDecoder != null){
                if(frameIndex == -1){
                    if (gifDecoder.getFrameCount() > 1)
                    {  
                        if (drawThread == null)
                        {
                            drawThread = new DrawThread();
                        }
                        else
                        {
                            drawThread.isStop = true;
                            isRun = false;
                            drawThread = null;
                            drawThread = new DrawThread();
                        }
                        drawThread.start();
                    }
                    else if (gifDecoder.getFrameCount() == 1)
                    {
                        GifFrame frame = gifDecoder.next();
                        currentImage = frame.image;
                        Message msg = redrawHandler.obtainMessage();
                        redrawHandler.sendMessage(msg);
                    }
                }
            }
        }else{
            Log.e("gif","parse error");
        }
    }

    private Handler redrawHandler = new Handler(){
        public void handleMessage(Message msg) {
           invalidate();
        }
    };
    
    private class DrawThread extends Thread{
        public boolean isStop = false;
        public void run(){
            if (gifDecoder == null || isStop == true){
                Log.v(TAG," drawthread stop.");            
                return;
            }
            while(isRun){
                if(pause == false && isStop == false){
                    if(!isShown())
                    {
                        Log.v(TAG," isShown false.getVisibility() = " + getVisibility());
                        isRun = false;
                        pause = true;
                        break;
                    }
                    GifFrame frame = gifDecoder.next();
                    if (frame == null)
                    {
                        continue;
                    }
                    currentImage = frame.image;
                    long sp = frame.delay;
                    Log.v(TAG,"____________________-sp   =  " +sp);
                    if (sp == 0)
                    {
                        sp = 200;
                    }
                    if (redrawHandler != null)
                    {
                        Message msg = redrawHandler.obtainMessage();
                        redrawHandler.sendMessage(msg);
                        try
                        {
                            Thread.sleep(sp);
                        }
                        catch(InterruptedException e)
                        {}
                    }
                    else
                    {
                        Log.v(TAG," redrawHandler is null.");                    
                        break;
                    }
                } else{
                    break;
                }
            }
            isRun = true;
            pause = false;
        }
    }

    public boolean restartGif()
    {            
        if (gifDecoder == null)
        {
            return false;
        }
        Log.v(TAG," gifDecoder.status = " 
            + gifDecoder.getStatus());        
        int status = gifDecoder.getStatus();
        if (GifDecoder.STATUS_PARSING == status)
        {
            return false;
        }
        if (drawThread == null)
        {
            drawThread = new DrawThread();
        }
        else
        {
            drawThread.isStop = true;
            isRun = false;  
            pause = true;
            drawThread = null;
            drawThread = new DrawThread();
        }
        isRun = true;
        pause = false;        
        drawThread.start();
        return true;
    }

    public void freeMemory()
    {
        Log.v(TAG," freeMemory");
        isRun = false;
        pause = true;
        if (drawThread != null)
        {
            drawThread.isStop = true;
            drawThread = null;
        }
        if (gifDecoder != null)
        {            
            gifDecoder.free();
            gifDecoder = null;
        }
    }
}



