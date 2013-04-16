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
    private boolean mIsPrepared;
    private boolean mStartWhenPrepared;
    private int     mSeekWhenPrepared;
    private boolean mStopWhenPrepared;
    private ScrollView mScrollViewPort;
    private LinearLayout mViewPort;
    // Indicates whether the view is in MMS conformance mode.
    private boolean mConformanceMode;
    private MediaController mMediaController;

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            if (mSeekWhenPrepared > 0) {
                mAudioPlayer.seekTo(mSeekWhenPrepared);
                mSeekWhenPrepared = 0;
            }
            if (mStartWhenPrepared) {
                mAudioPlayer.start();
                mStartWhenPrepared = false;
                displayAudioInfo();
            }
            if (mStopWhenPrepared) {
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
            mImageView.setPadding(0, 5, 0, 5);
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
/*Start of  zhuzhongwei 2011.4.28*/
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
/*End   of  zhuzhongwei 2011.4.28*/
        
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
                mImageView.setScrollY(-7);
                mImageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        if (mVideoView == null) {
            mVideoView = new VideoView(mContext);
            addView(mVideoView, new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            if (DEBUG) {
                mVideoView.setBackgroundColor(0xFFFF0000);
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
        if (null == mAudioInfoView) {
            LayoutInflater factory = LayoutInflater.from(getContext());
            mAudioInfoView = factory.inflate(R.layout.playing_audio_info, null);
            int height = mAudioInfoView.getHeight();
            if (mConformanceMode) {
                mViewPort.addView(mAudioInfoView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        AUDIO_INFO_HEIGHT));
            } else {
                addView(mAudioInfoView, new LayoutParams(
                        LayoutParams.MATCH_PARENT, AUDIO_INFO_HEIGHT+20));
                if (DEBUG) {
                    mAudioInfoView.setBackgroundColor(0xFFFF0000);
                }
            }
        }
        TextView audioName = (TextView) mAudioInfoView.findViewById(R.id.name);
        audioName.setText(name);
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
            mAudioPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Unexpected IOException.", e);
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        initAudioInfoView(name);
    }

    public void setText(String name, String text) {
        if (!mConformanceMode) {
            if (null == mScrollText) {
                mScrollText = new ScrollView(mContext);
                mScrollText.setScrollBarStyle(SCROLLBARS_OUTSIDE_INSET);
                addView(mScrollText, new LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                if (DEBUG) {
                    mScrollText.setBackgroundColor(0xFF00FF00);
                }
            }
            if (null == mTextView) {
                mTextView = new TextView(mContext);
                mTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                mScrollText.addView(mTextView);
            }
            mScrollText.requestFocus();
        }
        mTextView.setAutoLinkMask(Linkify.ALL);
        mTextView.setLinksClickable(true);
        mTextView.setTelUrl("tels:");
        mTextView.setWebUrl("www_custom:");
        mTextView.setVisibility(View.VISIBLE);
        mTextView.setTextExt(text);
        // Let the text in Mms can be selected.
        mTextView.setTextIsSelectable(true);
    }

    public void setTextRegion(int left, int top, int width, int height) {
        // Ignore any requirement of layout change once we are in MMS conformance mode.
        if (mScrollText != null && !mConformanceMode) {
            mScrollText.setLayoutParams(new LayoutParams(width, height));
        }
    }

    public void setVideoRegion(int left, int top, int width, int height) {
        if (mVideoView != null && !mConformanceMode) {
            mVideoView.setLayoutParams(new LayoutParams(width, height));
        }
    }

    public void setImageVisibility(boolean visible) {
        if (mImageView != null) {
            if (mConformanceMode) {
                mImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
            } else {
                mImageView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    public void setTextVisibility(boolean visible) {
        if (mConformanceMode) {
            if (mTextView != null) {
                mTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } else if (mScrollText != null) {
            mScrollText.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setVideoVisibility(boolean visible) {
        if (mVideoView != null) {
            if (mConformanceMode) {
                mVideoView.setVisibility(visible ? View.VISIBLE : View.GONE);
            } else {
                mVideoView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    public void startAudio() {
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.start();
            mStartWhenPrepared = false;
            displayAudioInfo();
        } else {
            mStartWhenPrepared = true;
        }
    }

    public void stopAudio() {
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            hideAudioInfo();
        } else {
            mStopWhenPrepared = true;
        }
    }

    public void pauseAudio() {
        if ((mAudioPlayer != null) && mIsPrepared) {
            if (mAudioPlayer.isPlaying()) {
                mAudioPlayer.pause();
            }
        }
        mStartWhenPrepared = false;
    }

    public void seekAudio(int seekTo) {
        if ((mAudioPlayer != null) && mIsPrepared) {
            mAudioPlayer.seekTo(seekTo);
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

    public void reset() {
        if (null != mScrollText) {
            mScrollText.setVisibility(View.GONE);
        }

        if (null != mImageView) {
            mImageView.setVisibility(View.GONE);
        }

        if (null != mAudioPlayer) {
            stopAudio();
        }

        if (null != mVideoView) {
            stopVideo();
            mVideoView.setVisibility(View.GONE);
          //  mVideoView = null;
        }

        if (null != mTextView) {
            mTextView.setVisibility(View.GONE);
        }

        if (mScrollViewPort != null) {
            mScrollViewPort.scrollTo(0, 0);
            mScrollViewPort.setLayoutParams(
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
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

    private class Position {
        public Position(int left, int top) {
            mTop = top;
            mLeft = left;
        }
        public int mTop;
        public int mLeft;
    }

    /**
     * Makes the SlideView working on  MMSConformance Mode. The view will be
     * re-layout to the linear view.
     * <p>
     * This is Chinese requirement about mms conformance.
     * The most popular Mms service in China is newspaper which is MMS conformance,
     * normally it mixes the image and text and has a number of slides. The
     * AbsoluteLayout doesn't have good user experience for this kind of message,
     * for example,
     *
     * 1. AbsoluteLayout exactly follows the smil's layout which is not optimized,
     * and actually, no other MMS applications follow the smil's layout, they adjust
     * the layout according their screen size. MMS conformance doc also allows the
     * implementation to adjust the layout.
     *
     * 2. The TextView is fixed in the small area of screen, and other part of screen
     * is empty once there is no image in the current slide.
     *
     * 3. The TextView is scrollable in a small area of screen and the font size is
     * small which make the user experience bad.
     *
     * The better UI for the MMS conformance message could be putting the image/video
     * and text in a linear layout view and making them scrollable together.
     *
     * Another reason for only applying the LinearLayout to the MMS conformance message
     * is that the AbsoluteLayout has ability to play image and video in a same screen.
     * which shouldn't be broken.
     */
    public void enableMMSConformanceMode(int textLeft, int textTop,
            int imageLeft, int imageTop) {
        mConformanceMode = true;
        if (mScrollViewPort == null) {
            mScrollViewPort = new ScrollView(mContext) {
                private int mBottomY;
                @Override
                protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                    super.onLayout(changed, left, top, right, bottom);
                    if (getChildCount() > 0) {
                        int childHeight = getChildAt(0).getHeight();
                        int height = getHeight();
                        mBottomY = height < childHeight ? childHeight - height : 0;
                    }
                }
                @Override
                protected void onScrollChanged(int l, int t, int oldl, int oldt) {
                    // Shows MediaController when the view is scrolled to the top/bottom of itself.
                    if (t == 0 || t >= mBottomY){
                        if (mMediaController != null
                                && !((SlideshowActivity) mContext).isFinishing()) {
                            mMediaController.show();
                        }
                    }
                }
            };
            mScrollViewPort.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
            mViewPort = new LinearLayout(mContext);
            mViewPort.setOrientation(LinearLayout.VERTICAL);
            mViewPort.setGravity(Gravity.CENTER);
            mViewPort.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mMediaController != null) {
                        mMediaController.show();
                    }
                }
            });
            mScrollViewPort.addView(mViewPort, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            addView(mScrollViewPort);
        }
        // Layout views to fit the LinearLayout from left to right, then top to
        // bottom.
        TreeMap<Position, View> viewsByPosition = new TreeMap<Position, View>(new Comparator<Position>() {
            public int compare(Position p1, Position p2) {
                int l1 = p1.mLeft;
                int t1 = p1.mTop;
                int l2 = p2.mLeft;
                int t2 = p2.mTop;
                int res = t1 - t2;
                if (res == 0) {
                    res = l1 - l2;
                }
                if (res == 0) {
                    // A view will be lost if return 0.
                    return -1;
                }
                return res;
            }
        });
        if (textLeft >=0 && textTop >=0) {
            mTextView = new TextView(mContext);
            mTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mTextView.setTextSize(18);
            mTextView.setPadding(5, 5, 5, 5);
            viewsByPosition.put(new Position(textLeft, textTop), mTextView);
        }

        if (imageLeft >=0 && imageTop >=0) {
            mImageView = new ImageView(mContext);
            mImageView.setPadding(0, 5, 0, 5);
            viewsByPosition.put(new Position(imageLeft, imageTop), mImageView);
            // According MMS Conformance Document, the image and video should use the same
            // region. So, put the VideoView below the ImageView.
            mVideoView = new VideoView(mContext);
            viewsByPosition.put(new Position(imageLeft + 1, imageTop), mVideoView);
        }
        for (View view : viewsByPosition.values()) {
            if (view instanceof VideoView) {
                mViewPort.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutManager.getInstance().getLayoutParameters().getHeight()));
            } else {
                mViewPort.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
            }
            view.setVisibility(View.GONE);
        }
    }

    public void setVideoThumbnail(String name, Bitmap bitmap) {
    }
    
    @Override
    public void setVcard(Uri lookupUri, String name) {
    }

    @Override
    public void setVcardVisibility(boolean visible) {
    }
}
