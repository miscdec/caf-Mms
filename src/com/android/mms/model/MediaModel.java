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

package com.android.mms.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.w3c.dom.events.EventListener;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import android.text.TextUtils;
// TODO: remove dependency for SDK build

public abstract class MediaModel extends Model implements EventListener {
    protected static final String TAG = LogTag.TAG;

    protected Context mContext;
    protected int mBegin;
    protected int mDuration;
    protected String mTag;
    protected String mSrc;
    protected String mContentType;
    private Uri mUri;
    private byte[] mData;
    protected short mFill;
    protected int mSize;
    protected int mSeekTo;
    protected boolean mMediaResizeable;
    protected static final int MAX_SRC_LENGTH = 30;
    private final ArrayList<MediaAction> mMediaActions;
    public static enum MediaAction {
        NO_ACTIVE_ACTION,
        START,
        STOP,
        PAUSE,
        SEEK,
    }

    public MediaModel(Context context, String tag, String contentType,
            String src, Uri uri) throws MmsException {
        mContext = context;
        mTag = tag;
        mContentType = contentType;
        mSrc = src;
        mUri = uri;
        initMediaSize();
        mMediaActions = new ArrayList<MediaAction>();
    }

    public MediaModel(Context context, String tag, String contentType,
            String src, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data may not be null.");
        }

        mContext = context;
        mTag = tag;
        mContentType = contentType;
        mSrc = src;
        mData = data;
        mSize = data.length;
        mMediaActions = new ArrayList<MediaAction>();
    }

    public int getBegin() {
        return mBegin;
    }

    public void setBegin(int begin) {
        mBegin = begin;
        notifyModelChanged(true);
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        if (isPlayable() && (duration < 0)) {
            // 'indefinite' duration, we should try to find its exact value;
            try {
                initMediaDuration();
            } catch (MmsException e) {
                // On error, keep default duration.
                Log.e(TAG, e.getMessage(), e);
                return;
            }
        } else {
            mDuration = duration;
        }
        notifyModelChanged(true);
    }

    public String getTag() {
        return mTag;
    }

    public String getContentType() {
        return mContentType;
    }

    /**
     * Get the URI of the media.
     *
     * @return The URI of the media.
     */
    public Uri getUri() {
        return mUri;
    }

    public byte[] getData() {
        if (mData != null) {
            byte[] data = new byte[mData.length];
            System.arraycopy(mData, 0, data, 0, mData.length);
            return data;
        }
        return null;
    }

    /**
     * @param uri the mUri to set
     */
    void setUri(Uri uri) {
        mUri = uri;
    }

    /**
     * @return the mSrc
     */
    public String getSrc() {
        return mSrc;
    }

    /**
     * @return the mFill
     */
    public short getFill() {
        return mFill;
    }

    /**
     * @param fill the mFill to set
     */
    public void setFill(short fill) {
        mFill = fill;
        notifyModelChanged(true);
    }

    /**
     * @return whether the media is resizable or not. For instance, a picture can be resized
     * to smaller dimensions or lower resolution. Other media, such as video and sounds, aren't
     * currently able to be resized.
     */
    public boolean getMediaResizable() {
        return mMediaResizeable;
    }

    /**
     * @return the size of the attached media
     */
    public int getMediaSize() {
        return mSize;
    }

    public boolean isText() {
        return mTag.equals(SmilHelper.ELEMENT_TAG_TEXT);
    }

    public boolean isImage() {
        return mTag.equals(SmilHelper.ELEMENT_TAG_IMAGE);
    }

    public boolean isVideo() {
        return mTag.equals(SmilHelper.ELEMENT_TAG_VIDEO);
    }

    public boolean isAudio() {
        return mTag.equals(SmilHelper.ELEMENT_TAG_AUDIO);
    }

    public boolean isVcard() {
        return mTag.equals(SmilHelper.ELEMENT_TAG_REF)
                && mContentType.toLowerCase().equals(ContentType.TEXT_VCARD.toLowerCase());
    }

    protected void initMediaDuration() throws MmsException {
        if (mUri == null) {
            throw new IllegalArgumentException("Uri may not be null.");
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int duration = 0;
        try {
            retriever.setDataSource(mContext, mUri);
            String dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null) {
                duration = Integer.parseInt(dur);
            }
            mDuration = duration;
        } catch (Exception ex) {
            Log.e(TAG, "MediaMetadataRetriever failed to get duration for " + mUri.getPath(), ex);
            throw new MmsException(ex);
        } finally {
            try {
                retriever.release();
            } catch (IOException ex) {
                throw new MmsException(ex);
            }
        }
    }

    private void initMediaSize() throws MmsException {
        ContentResolver cr = mContext.getContentResolver();
        InputStream input = null;
        try {
            input = cr.openInputStream(mUri);
            if (input instanceof FileInputStream) {
                // avoid reading the whole stream to get its length
                FileInputStream f = (FileInputStream) input;
                mSize = (int) f.getChannel().size();
                // sometimes mSize will be zero here. It's tempting to count the bytes as the code
                // does below, but that turns out to be very slow. We'll deal with a zero size
                // when we resize the media.

                if (isVideo() && mSize > MmsConfig.getMaxMessageSize()) {
                    Log.w(TAG, "initMediaSize: Video size: f.getChannel().size(): " + mSize +
                            " larger than max message size: " + MmsConfig.getMaxMessageSize());
                }

                // after attaching vCard into attachment, mSize will be zero, should get it's
                // size by calling available().
                if (isVcard() && mSize <= 0) {
                    mSize = input.available();
                    if (mSize <= 0) {
                        throw new MmsException("Bad URI");
                    }
                }
            } else {
                while (-1 != input.read()) {
                    mSize++;
                }
            }

        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
            if (e instanceof FileNotFoundException) {
                throw new MmsException(e.getMessage());
            }
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
    }

    public static boolean isMmsUri(Uri uri) {
        return uri != null && uri.getAuthority().startsWith("mms");
    }

    public static boolean isFileUri(final Uri uri) {
        return uri != null && TextUtils.equals(uri.getScheme(), ContentResolver.SCHEME_FILE);
    }

    public int getSeekTo() {
        return mSeekTo;
    }

    public void appendAction(MediaAction action) {
        mMediaActions.add(action);
    }

    public MediaAction getCurrentAction() {
        if (0 == mMediaActions.size()) {
            return MediaAction.NO_ACTIVE_ACTION;
        }
        return mMediaActions.remove(0);
    }

    protected boolean isPlayable() {
        return false;
    }

    protected void pauseMusicPlayer() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.d(TAG, "pauseMusicPlayer");
        }

        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    /**
     * If the attached media is resizeable, resize it to fit within the byteLimit. Save the
     * new part in the pdu.
     * @param byteLimit the max size of the media attachment
     * @throws MmsException
     */
    protected void resizeMedia(int byteLimit, long messageId) throws MmsException {
    }

    public String reSetAttachmentName() {
        Log.d("Mms","reSetAttachmentName src is "+mSrc);
        if (TextUtils.isEmpty(mSrc)) {
            return mSrc;
        }
        int dotPos = mSrc.lastIndexOf('.');
        String tempName = mSrc;
        if (0 <= dotPos && dotPos < mSrc.length()) {
            String subName = mSrc.substring(0, dotPos);
            if (!TextUtils.isEmpty(subName) && subName.length() > MAX_SRC_LENGTH) {
                subName = mSrc.substring(0, MAX_SRC_LENGTH);
                tempName = subName + mSrc.substring(dotPos - 1);
            }
        }
        Log.d("Mms","reSetAttachmentName final src is "+tempName);
        return tempName;
    }
}
