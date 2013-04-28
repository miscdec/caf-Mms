/*
 * yinqi add this file for vcard attachment 2009-4-28
 */

package com.android.mms.model;

import com.android.mms.dom.smil.SmilMediaElementImpl;
import com.google.android.mms.pdu.CharacterSets;

import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementTime;

import android.content.Context;
import android.drm.mobile1.DrmException;
import android.util.Log;
import android.net.Uri;
import com.google.android.mms.MmsException;
import com.google.android.mms.ContentType;
import com.android.mms.ui.MessageUtils;


import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class FileModel extends MediaModel {
    private static final String TAG = "FileModel";   
    public static final String OCT_STREAM         = "application/octet-stream";

    public FileModel(Context context, Uri uri) throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_FILE, null, null, uri);
        initModelFromUri(uri);
    }

    public FileModel(Context context, Uri uri, String src) throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_FILE, null, null, uri);
        initModelFromUri(uri, src);
    }

    private void initModelFromUri(Uri uri) throws MmsException {
        String scheme = uri.getScheme();
        String path = uri.getPath();
        if (uri.getScheme().equals("file")) {           
            mContentType = OCT_STREAM;
            mSrc = path.substring(path.lastIndexOf('/') + 1);
        
            // Some MMSCs appear to have problems with filenames
            // containing a space.  So just replace them with
            // underscores in the name, which is typically not
            // visible to the user anyway.
            mSrc = mSrc.replace(' ', '_');
            return;
        };
    }

    private void initModelFromUri(Uri uri, String src) throws MmsException {
        String scheme = uri.getScheme();
        String path = uri.getPath();

        if(null != src){
            mSrc = src;
        }
        mContentType = OCT_STREAM;
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
    }
}
