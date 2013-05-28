/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.method.HideReturnsTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.mms.R;
import com.android.mms.layout.LayoutManager;
import android.widget.FrameLayout;

import java.io.InputStream;
import android.content.ContentResolver;
import android.text.util.Linkify;
import android.text.TextUtils;
import com.android.mms.MmsConfig;
/**
 * A basic view to show the contents of a slide.
 */
public class SlideView extends LinearLayout implements
        AdaptableSlideViewInterface {
    private static final String TAG = "SlideView";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;
    // FIXME: Need getHeight from mAudioInfoView instead of constant AUDIO_INFO_HEIGHT.
    private static final int AUDIO_INFO_HEIGHT = 82;

    private View mAudioInfoView;
    private Uri mGifUri = null;
    private ImageView mImageView;
    private VideoView mVideoView;
    private ScrollView mScrollText;
    private TextView mTextView;
    private OnSizeChangedListener mSizeChangedListener;
    private MediaPlayer mAudioPlayer;
    private boolean mIsPrepared = true;
    private boolean mStartWhenPrepared;
    private int     mSeekWhenPrepared;
    private boolean mStopWhenPrepared;
    private ScrollView mScrollViewPort;
    private LinearLayout mViewPort;
    // Indicates whether the view is in MMS conformance mode.
    private boolean mConformanceMode;
    private MediaController mMediaController;
    private boolean mPauseState = false;
    private boolean mIsAudioError = false;
    
    private boolean mShowAsImage = false;
    private int mVideoPosition = -1;
    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            if (mSeekWhenPrepared > 0) {
                mAudioPlayer.seekTo(mSeekWhenPrepared);
                mSeekWhenPrepared = 0;

                // When reloading, display audio information
                displayAudioInfo();
                if (mMediaController != null) {
                    mMediaController.show();
                }
            }
            if (mStartWhenPrepared) {
                mAudioPlayer.start();
                mPauseState = false;
                mStartWhenPrepared = false;
                displayAudioInfo();
            }
            else if (mStopWhenPrepared) {//szh mod
                mAudioPlayer.stop();
                mAudioPlayer.release();
                mAudioPlayer = null;
                mStopWhenPrepared = false;
                hideAudioInfo();
            }
        }
    };

    public SlideView(Context context) {
        super(context);
    }

    public SlideView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setImage(String name, Bitmap bitmap) {   
        if(null == name && null == bitmap){
            removeView(mImageView);
            if (mImageView != null)
            {
                if (mImageView instanceof GIFView)
                {
                    ((GIFView)mImageView).freeMemory();            
                }
            }
            mImageView = null;
        }
        
        if (mImageView != null && mImageView instanceof GIFView){
            int position = indexOfChild(mImageView);
            removeView(mImageView);
            ((GIFView)mImageView).freeMemory();            
            mImageView = null;
            mImageView = new ImageView(mContext);
            addView(mImageView, position, new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
        
        if (mImageView == null) {
            mImageView = new ImageView(mContext);
            addView(mImageView, new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            if (DEBUG) {
                //mImageView.setBackgroundColor(0xFFFF0000);
            }
        }

        if (null == bitmap) {
            bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
        }
        mImageView.setImageBitmap(bitmap);
        //mShowAsImage = true;
    }  

    public boolean setGIF(Uri uri, Bitmap bitmap) {
        if (bitmap == null || !isGif(uri))
        {
            Log.v(TAG,"uri = " + uri);
            return false;
        }
        int imageHeight = bitmap.getHeight();
        int imageWidth = bitmap.getWidth();        
        int newWidth = LayoutManager.getInstance().getLayoutParameters().getWidth();
        int screenWidth  = newWidth;
        int newHeight = LayoutManager.getInstance().getLayoutParameters().getHeight();   
        int screenHeight = newHeight;
        Log.v(TAG,"screenWidth = " + screenWidth
            + ";screenHeight = " + screenHeight);

        if (newWidth < imageWidth)
        {
            if (newHeight < imageHeight)
            {
                //h big, w big;                            
                Log.v(TAG," h big, w big");
                if (imageHeight*screenWidth > imageWidth*screenHeight)
                {
                   //too height                
                   newWidth = (imageWidth * newHeight)/imageHeight;    
                   Log.v(TAG," h big, w big");                
                }
                else
                {
                    newHeight = (imageHeight * newWidth)/imageWidth;                   
                }
            }
            else
            {
                //h small, w big;
                newHeight = (imageHeight * newWidth)/imageWidth;
            }           
        }
        else if (newHeight < imageHeight)
        {
            //h big, w small;        
            newWidth = (imageWidth * newHeight)/imageHeight;   
        }
        else
        {
            newHeight = imageHeight;
        }
        Log.v(TAG," newHeight = " + newHeight
            + ";newWidth = " + newWidth
            + ";screenWidth = " + screenWidth);            
        
        if (mImageView != null 
            && (mImageView instanceof GIFView)
            && mGifUri != null && mGifUri == uri)
        {
            if (!mImageView.isShown())
            {
                mImageView.setVisibility(View.VISIBLE);
                if (((GIFView)mImageView).restartGif())
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
            Log.v(TAG," gif is same.");        
            //return true;
        }
        mGifUri = uri;
        if (mImageView == null) {
            mImageView = new GIFView(mContext);
            /*
            addView(mImageView, new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    */
            addView(mImageView, new LayoutParams(
                    screenWidth, newHeight));                    
            if (DEBUG) 
            {
                //mImageView.setBackgroundColor(0xFFFF0000);
            }
        } else if (mImageView != null){
            int position = indexOfChild(mImageView);
            removeView(mImageView);
            if (mImageView instanceof GIFView)
            {
                ((GIFView)mImageView).freeMemory();
            }
            mImageView = null;
            mImageView = new GIFView(mContext);
            addView(mImageView, position, new LayoutParams(
                        screenWidth, newHeight));

        }
        return ((GIFView)mImageView).setDrawable(uri);
    }

    private boolean isGif(Uri uri)
    {
        InputStream input = null;
        ContentResolver cr = mContext.getContentResolver();        
        try 
        {
            input = cr.openInputStream(uri);
            return isGifHeader(input);

        } catch (IOException e) {
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    
                }
            }
        }
        return false;
    }

    private boolean isGifHeader(InputStream input) {
        String id = "";
        try {
            for (int i = 0; i < 6; i++) {        
                id += (char) input.read();
            }
        }                
        catch (Exception e) {
            Log.v(TAG," e = " + e);
            return false;
        }        
        Log.v(TAG," id = " + id);
        if (!id.startsWith("GIF")) {
            return false;
        }
        return true;
    }

    public void setImageRegion(int left, int top, int width, int height) {
        if (mImageView != null) {
            int W = LayoutManager.getInstance().getLayoutParameters().getWidth();
            int H = LayoutManager.getInstance().getLayoutParameters().getHeight();

            if (mImageView instanceof GIFView){
                return;
                
            } else{
                mImageView.setScrollY(-1);
               // mImageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                mImageView.setLayoutParams(new LayoutParams(W, H/3));
            }
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        if (mVideoView == null) {
            mVideoView = new VideoView(mContext);
            addView(mVideoView, mVideoPosition, new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            mVideoView.setWillNotDraw(false); 
                    
            if (DEBUG) 
            {
                //mVideoView.setBackgroundColor(0xFFFF0000);
            }
        }

        if (LOCAL_LOGV) {
            Log.v(TAG, " video source to " + video);
        }
        mVideoView.setVisibility(View.VISIBLE);
        mVideoView.setVideoURI(video);
    }

    public void setMediaController(MediaController mediaController) {
        mMediaController = mediaController;
    }

    private void initAudioInfoView(String name) {
        LayoutInflater factory = LayoutInflater.from(getContext());
        mAudioInfoView = factory.inflate(R.layout.playing_audio_info, null);
        int height = mAudioInfoView.getHeight();
        TextView audioName = (TextView) mAudioInfoView.findViewById(R.id.name);
        audioName.setText(name);
        addView(mAudioInfoView, new LayoutParams(
                LayoutParams.FILL_PARENT, AUDIO_INFO_HEIGHT+20));
        if (DEBUG) {
            //mAudioInfoView.setBackgroundColor(0xFFFF0000);
        }

        mAudioInfoView.setVisibility(View.GONE);
    }

    private void displayAudioInfo() {
        if (null != mAudioInfoView) {
            mAudioInfoView.setVisibility(View.VISIBLE);
        }
    }

    private void hideAudioInfo() {
        if (null != mAudioInfoView) {
            mAudioInfoView.setVisibility(View.GONE);
        }
    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        if (audio == null) {
            throw new IllegalArgumentException("Audio URI may not be null.");
        }

        if (LOCAL_LOGV) {
            Log.v(TAG, " audio source to " + audio);
        }

        if (mAudioPlayer != null) {
            mAudioPlayer.reset();
            mAudioPlayer.release();
            mAudioPlayer = null;
            mStopWhenPrepared = false;
        }

        // Reset state variables
        mIsPrepared = false;
        mStartWhenPrepared = false;
        mSeekWhenPrepared = 0;
        mStopWhenPrepared = false;

        try {
            mAudioPlayer = new MediaPlayer();
            mAudioPlayer.setOnPreparedListener(mPreparedListener);
            mAudioPlayer.setDataSource(mContext, audio);
            //mAudioPlayer.setDataSource(mContext, audio, true);            
            mAudioPlayer.prepare();
            mIsAudioError = false;
        } catch (IOException e) {
            Log.e(TAG, "Unexpected IOException.", e);
            mAudioPlayer.reset();            
            mAudioPlayer.release();
            mAudioPlayer = null;
            mIsAudioError = true;
        }
        initAudioInfoView(name);
    }

    public void setText(String name, String text) {
        if (null == name && (null == text || TextUtils.isEmpty(text))){
            removeView(mTextView);
            mTextView = null;
        }
        if (null == mTextView) {

            mTextView = new TextView(mContext);
            mTextView.setTextIsSelectable(true);
            addView(mTextView);

        }

        mTextView.setAutoLinkMask(Linkify.ALL);
        mTextView.setLinksClickable(true);
        mTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        
        mTextView.setTelUrl("tels:");
        mTextView.setWebUrl("www_custom:");            
        mTextView.requestFocus();
        mTextView.setTextExt(text + "\n");
    }

    public void setTextRegion(int left, int top, int width, int height) {
        if (mTextView != null) {
         int W = LayoutManager.getInstance().getLayoutParameters().getWidth();
            if (left >= W){
                left = 0;
            }
            
            if (width > W){
                width = W;                
            }
            mTextView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            mTextView.setTextIsSelectable(true);
            mTextView.setTextSize(18);              
        }
    }

    public void setVideoRegion(int left, int top, int width, int height) {
        if (mVideoView != null) {
            int W = LayoutManager.getInstance().getLayoutParameters().getWidth();
            int H = LayoutManager.getInstance().getLayoutParameters().getHeight();
            Log.v(TAG,"setVideoRegion " + left + " " + top + " " + width + " " + height + " Height=" + H + "w=" +W );

            height = H / 2;
            Log.v(TAG,"w = "+W+"height ="+height);
            if(W>=MmsConfig.MAX_IMAGE_WIDTH)
                W=MmsConfig.MAX_IMAGE_WIDTH-20;
            mVideoView.setLayoutParams(new LayoutParams(W, height));
        }
    }

    public void setImageVisibility(boolean visible) {
        if (mImageView != null) {
            mImageView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setTextVisibility(boolean visible) {
        if (mTextView != null) {
            mTextView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setVideoVisibility(boolean visible) {
        if (mVideoView != null) {
            mVideoView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void startAudio() {
        if (mIsAudioError)
        {
            mPauseState = false;
        }
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.start();
            mPauseState = false;
            mStartWhenPrepared = false;
            displayAudioInfo();
        } else {
            if (mIsAudioError)
            {
                mStartWhenPrepared = false;
            }
            else
            {
                mStartWhenPrepared = true;
            }
        }
    }

    public void stopAudio() {
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            hideAudioInfo();
        } else {
           // mStopWhenPrepared = true;
        }
    }

    public void pauseAudio() {
        if ((mAudioPlayer != null) && mIsPrepared) {
            if (mAudioPlayer.isPlaying()) {
                mAudioPlayer.pause();
            }
        }
        mPauseState = true;
        mStartWhenPrepared = false;
    }

    public void seekAudio(int seekTo) {
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.seekTo(seekTo);
            displayAudioInfo();//szh add
        } else {
            mSeekWhenPrepared = seekTo;
        }
    }

    public void startVideo() {
        if (mVideoView != null) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Starting video playback.");
            }
            mVideoView.start();
        }
    }

    public void stopVideo() {
        if ((mVideoView != null)) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Stopping video playback.");
            }
            mVideoView.stopPlayback();
        }
    }

    public void pauseVideo() {
        if (mVideoView != null) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "Pausing video playback.");
            }
            mVideoView.pause();
        }
    }

    public void seekVideo(int seekTo) {
        if (mVideoView != null) {
            if (seekTo > 0) {
                if (LOCAL_LOGV) {
                    Log.v(TAG, "Seeking video playback to " + seekTo);
                }
                mVideoView.seekTo(seekTo);
            }
        }
    }

    public boolean isPrepared(){
        return mIsPrepared;
    }
	
    public void reset() {
        ((FrameLayout)getParent()).scrollTo(0, 0);
        ((FrameLayout)getParent()).scrollBy(0, 1);
        setOrientation(VERTICAL);
        setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        if (null != mTextView) {
            //removeView(mTextView);
            //mTextView = null;
            mTextView.setVisibility(View.GONE);
        }

        if (null != mImageView) {
            mImageView.setVisibility(View.GONE);
        }

        if (null != mAudioPlayer) {
            stopAudio();
        }

        if (null != mVideoView) {
            stopVideo();
            mVideoPosition = indexOfChild(mVideoView);
            mVideoView.setVisibility(View.GONE);
            mVideoView = null;
        }
        
    }

    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mSizeChangedListener != null) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "new size=" + w + "x" + h);
            }
            mSizeChangedListener.onSizeChanged(w, h - AUDIO_INFO_HEIGHT);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener l) {
        mSizeChangedListener = l;
    }
    @Override
    public void setVideoThumbnail(String name, Bitmap bitmap) {
    }

    @Override
    public void setVcard(Uri lookupUri, String name) {
    }

    @Override
    public void setVcardVisibility(boolean visible) {
    }
}
