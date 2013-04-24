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
import com.android.mms.R;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.drm.mobile1.DrmException;

import com.android.mms.model.AudioModel;
import com.android.mms.model.ImageModel;
import com.android.mms.model.LayoutModel;
import com.android.mms.model.MediaModel;
import com.android.mms.model.MediaModel.MediaAction;
import com.android.mms.model.Model;
import com.android.mms.model.RegionMediaModel;
import com.android.mms.model.RegionModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.model.VideoModel;
import com.android.mms.model.VcardModel;
import com.android.mms.ui.AdaptableSlideViewInterface.OnSizeChangedListener;
import com.android.mms.util.ItemLoadedCallback;

import android.drm.mobile1.DrmException;
import android.widget.Toast;
/**
 * A basic presenter of slides.
 */
public class SlideshowPresenter extends Presenter {
    private static final String TAG = "SlideshowPresenter";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    protected int mLocation;
    protected final int mSlideNumber;

    protected float mWidthTransformRatio = 1.0f;
    protected float mHeightTransformRatio = 1.0f;
    private SlideModel mSlideModel;//yinqi add 2010-1-5
    private int mWidth = -1;
    private int mHeight = -1;
    //private static Bitmap mImageIcon;
    // Since only the original thread that created a view hierarchy can touch
    // its views, we have to use Handler to manage the views in the some
    // callbacks such as onModelChanged().
    protected final Handler mHandler = new Handler();

    public SlideshowPresenter(Context context, ViewInterface view, Model model) {
        super(context, view, model);
        mLocation = 0;
        mSlideNumber = ((SlideshowModel) mModel).size();

        if (view instanceof AdaptableSlideViewInterface) {
            ((AdaptableSlideViewInterface) view).setOnSizeChangedListener(
                    mViewSizeChangedListener);
        }
    }

    private final OnSizeChangedListener mViewSizeChangedListener =
        new OnSizeChangedListener() {
        public void onSizeChanged(int width, int height) {
            LayoutModel layout = ((SlideshowModel) mModel).getLayout();
            mWidthTransformRatio = getWidthTransformRatio(
                    width, layout.getLayoutWidth());
            mHeightTransformRatio = getHeightTransformRatio(
                    height, layout.getLayoutHeight());
            // The ratio indicates how to reduce the source to match the View,
            // so the larger one should be used.
            float ratio = mWidthTransformRatio > mHeightTransformRatio ?
                    mWidthTransformRatio : mHeightTransformRatio;
            mWidthTransformRatio = ratio;
            mHeightTransformRatio = ratio;
            if (LOCAL_LOGV) {
                Log.v(TAG, "ratio_w = " + mWidthTransformRatio
                        + ", ratio_h = " + mHeightTransformRatio);
            }

            mWidth = width;
            mHeight = height;
        }
    };

    private float getWidthTransformRatio(int width, int layoutWidth) {
        if (width > 0) {
            return (float) layoutWidth / (float) width;
        }
        return 1.0f;
    }

    private float getHeightTransformRatio(int height, int layoutHeight) {
        if (height > 0) {
            return (float) layoutHeight / (float) height;
        }
        return 1.0f;
    }

    private int transformWidth(int width) {
        return (int) (width / mWidthTransformRatio);
    }

    private int transformHeight(int height) {
        return (int) (height / mHeightTransformRatio);
    }

    @Override
    public void present(ItemLoadedCallback callback) {
        // This is called to show a full-screen slideshow. Presently, all parts of
        // a slideshow (images, sounds, etc.) are loaded and displayed on the UI thread.
        presentSlide((SlideViewInterface) mView, ((SlideshowModel) mModel).get(mLocation));
    }
		
    public void present() {
        presentSlide((SlideViewInterface) mView, ((SlideshowModel) mModel).get(mLocation));
    }

    /**
     * @param view
     * @param model
     */
    public synchronized void presentSlide(SlideViewInterface view, SlideModel model) {
        setLocation(((SlideshowModel) mModel).indexOf(model));
        try {
            //validate region(added by yinqi 2010-1-12)
            RegionModel textRm = null;
            RegionModel iRm = null;
            boolean textBottom = true;
            mSlideModel = model;
			
            if (model.hasText() && (model.hasImage() || model.hasVideo())){
                for (MediaModel media : model) {
                    if (media instanceof RegionMediaModel) {
                        if (media.isText()){
                            textRm = ((RegionMediaModel)media).getRegion();
                        }else if(media.isImage()){
                            iRm = ((RegionMediaModel)media).getRegion();
                        }else if(media.isVideo()){
                            iRm = ((RegionMediaModel)media).getRegion();
                        }
                    }
                }

                if (textRm != null && iRm != null){
                    //text is on the top of the image
                    if (textRm.getTop() < iRm.getTop()){
                        Log.v(TAG,"texttop    region interlaces, reassign the coordination!");
                        textBottom = false;
                        //interlaced
                        if ((textRm.getTop() + textRm.getHeight()) > iRm.getTop()){
                            Log.v(TAG,"region interlaces, reassign the coordination!");
                            //textRm.setHeight(iRm.getTop() - textRm.getTop());
                            iRm.setTop(textRm.getTop() + textRm.getHeight() + 10);
                        }
                    }else if (textRm.getTop() == iRm.getTop()){
                        Log.v(TAG,"display region is same, reassign the coordination!");
                        final int W = mWidth;
                        final int H = mHeight;                    

                        iRm.setTop(0);
                        iRm.setLeft(0);
                        iRm.setHeight((int)((float)H * mHeightTransformRatio / 2));
                        textRm.setTop((int)((float)H * mHeightTransformRatio / 2));
                        textRm.setLeft(0);
                        textRm.setHeight((int)((float)H * mHeightTransformRatio / 2));
                    }else{
                        Log.v(TAG,"text bottom    region interlaces, reassign the coordination!");
                        if ((iRm.getTop() + iRm.getHeight()) > textRm.getTop()){
                            Log.w(TAG,"region interlaces, reassign the coordination!");
                            //iRm.setHeight(textRm.getTop() - iRm.getTop());
                            textRm.setTop(iRm.getTop() + iRm.getHeight());
                        }
                    }
                }
                if(textBottom){
                    view.setImage(null, null);

                    view.setVideo(null,  null);

                    view.setText(null, "");
                } else{
                    view.setText(null, "");
                    view.setImage(null, null);

                    view.setVideo(null,  null);
                }   
            }
            view.reset();
            for (MediaModel media : model) {
                if (media instanceof RegionMediaModel) {
                    presentRegionMedia(view, (RegionMediaModel) media, true);
                } 
            }
         if(model.hasAudio()){
                MediaModel media = model.getAudio();
                presentAudio(view, (AudioModel) media, true);
            }                  
        } catch (DrmException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(mContext,
                    mContext.getString(R.string.insufficient_drm_rights),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @param view
     */
    protected void presentRegionMedia(SlideViewInterface view,
            RegionMediaModel rMedia, boolean dataChanged)
            throws DrmException {
        RegionModel r = (rMedia).getRegion();
        if (rMedia.isText()) {
            presentText(view, (TextModel) rMedia, r, dataChanged);
        } else if (rMedia.isImage()) {
            presentImage(view, (ImageModel) rMedia, r, dataChanged);
        } else if (rMedia.isVideo()) {
            presentVideo(view, (VideoModel) rMedia, r, dataChanged);
        }
    }

    protected void presentAudio(SlideViewInterface view, AudioModel audio,
            boolean dataChanged) throws DrmException {
        // Set audio only when data changed.
        if (dataChanged) {
            view.setAudio(audio.getUri(), audio.getSrc(), audio.getExtras());
        }

        MediaAction action = audio.getCurrentAction();
        if (action == MediaAction.START) {
            view.startAudio();
        } else if (action == MediaAction.PAUSE) {
            view.pauseAudio();
        } else if (action == MediaAction.STOP) {
            view.stopAudio();
        } else if (action == MediaAction.SEEK) {
            view.seekAudio(audio.getSeekTo());
        }
    }

    protected void presentText(SlideViewInterface view, TextModel text,
            RegionModel r, boolean dataChanged) {
        if (dataChanged) {
            view.setText(text.getSrc(), text.getText());
        }

            if (view instanceof AdaptableSlideViewInterface) {
                if (mSlideModel == null)
                {
                    return;
                }
                //yinqi add, if there is no image media show text from the top left corner 2010-1-5
                if ( !mSlideModel.hasImage() && !mSlideModel.hasVideo()){
                    Log.v(TAG, "   view.getWidth() = "+view.getWidth()+"   view.getHeight() = "+view.getHeight());
                    ((AdaptableSlideViewInterface) view).setTextRegion(
                            transformWidth(0),
                            transformHeight(0),
                            transformWidth(view.getWidth()),
                            transformHeight(view.getHeight()));
                }else{
                    ((AdaptableSlideViewInterface) view).setTextRegion(
                            transformWidth(r.getLeft()),
                            transformHeight(r.getTop()),
                            transformWidth(r.getWidth()),
                            transformHeight(r.getHeight()));
                }
            }         
            view.setTextVisibility(text.isVisible());        
    }


    /**
     * @param view
     * @param image
     * @param r
     */
    protected void presentImage(SlideViewInterface view, ImageModel image,
            RegionModel r, boolean dataChanged) throws DrmException {
        if (dataChanged) {      
            String ct = image.getContentType();
            if (ct != null && ct.contains("gif") 
                && view instanceof SlideView){
                Log.v(TAG,"z280 setgif will be called.");
                /*
                MessageUtils.printMmsLog("z293 image.getBitmapWithDrmCheck() "
                    + image.getBitmapWithDrmCheck().getHeight() + " "
                    + image.getBitmapWithDrmCheck().getWidth());
                    */
                if (false == ((SlideView)view).setGIF(image.getUri(), image.getBitmap(r.getWidth(), r.getHeight()))){
                    view.setImage(image.getSrc(), image.getBitmap(r.getWidth(), r.getHeight()));
                }
            }else{         
                view.setImage(image.getSrc(), image.getBitmap(r.getWidth(), r.getHeight()));
            }
          //  view.setImage(image.getSrc(), image.getBitmap(r.getWidth(), r.getHeight()));
        }

        if (view instanceof AdaptableSlideViewInterface) {
            ((AdaptableSlideViewInterface) view).setImageRegion(
                    transformWidth(r.getLeft()),
                    transformHeight(r.getTop()),
                    transformWidth(r.getWidth()),
                    transformHeight(r.getHeight()));
        }
        view.setImageRegionFit(r.getFit());
        view.setImageVisibility(image.isVisible());
    }

    /**
     * @param view
     * @param video
     * @param r
     */
    protected void presentVideo(SlideViewInterface view, VideoModel video,
            RegionModel r, boolean dataChanged) throws DrmException {
        if (dataChanged) {
            view.setVideo(video.getSrc(), video.getUri());
        }

            if (view instanceof AdaptableSlideViewInterface) {
            ((AdaptableSlideViewInterface) view).setVideoRegion(
                    transformWidth(r.getLeft()),
                    transformHeight(r.getTop()),
                    transformWidth(r.getWidth()),
                    transformHeight(r.getHeight()));
        }
        view.setVideoVisibility(video.isVisible());

        MediaAction action = video.getCurrentAction();
        if (action == MediaAction.START) {
            view.startVideo();
        } else if (action == MediaAction.PAUSE) {
            view.pauseVideo();
        } else if (action == MediaAction.STOP) {
            view.stopVideo();
        } else if (action == MediaAction.SEEK) {
            view.seekVideo(video.getSeekTo());
        }
    }

    public void setLocation(int location) {
        mLocation = location;
    }

    public int getLocation() {
        return mLocation;
    }

    public void goBackward() {
        if (mLocation > 0) {
            mLocation--;
        }
    }

    public void goForward() {
        if (mLocation < (mSlideNumber - 1)) {
            mLocation++;
        }
    }

    public void onModelChanged(final Model model, final boolean dataChanged) {
        final SlideViewInterface view = (SlideViewInterface) mView;

        // FIXME: Should be optimized.
        if (model instanceof SlideshowModel) {
            // TODO:
        } else if (model instanceof SlideModel) {
            if (((SlideModel) model).isVisible()) {
                mSlideModel = (SlideModel) model;//yinqi 2010-1-5
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(new Runnable() {
                    public void run() {
                        presentSlide(view, (SlideModel) model);
                    }
                });
            } else {
                mHandler.post(new Runnable() {
                    public void run() {
                        goForward();
                    }
                });
            }
        } else if (model instanceof MediaModel) {
            if (model instanceof RegionMediaModel) {
                mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            presentRegionMedia(view, (RegionMediaModel) model, dataChanged);
                        } catch (DrmException e) {
                            Log.e(TAG, e.getMessage(), e);
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.insufficient_drm_rights),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else if (((MediaModel) model).isAudio()) {
                mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            presentAudio(view, (AudioModel) model, dataChanged);
                        } catch (DrmException e) {
                            Log.e(TAG, e.getMessage(), e);
                            Toast.makeText(mContext,
                                    mContext.getString(R.string.insufficient_drm_rights),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } else if (model instanceof RegionModel) {
            // TODO:
        }
    }

    @Override
    public void cancelBackgroundLoading() {
        // For now, the SlideshowPresenter does no background loading so there is nothing to cancel.
    }
}
