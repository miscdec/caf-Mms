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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class VCalModel extends MediaModel {
    private static final String TAG = "VCalModel";

    private CharSequence mText;
    private final int mCharset;    

    public VCalModel(Context context, String contentType, String src) {
        this(context, contentType, src, CharacterSets.UTF_8, new byte[0]);
    }

    public VCalModel(Context context, String contentType, String src,
            int charset, byte[] data ) {
        super(context, VCALENDAR, contentType, src, data);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
        mText = extractTextFromData(data);
    }

    private CharSequence extractTextFromData(byte[] data) {
        if (data != null) {
            try {
                if (CharacterSets.ANY_CHARSET == mCharset) {
                    return new String(data); // system default encoding.
                } else {
                    String name = CharacterSets.getMimeName(mCharset);
                    return new String(data, name);
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding: " + mCharset, e);
                return new String(data); // system default encoding.
            }
        }
        return "";
    }

    public String getText() {
        if (mText == null) {
                mText = extractTextFromData(getData());
        }
        
        // If our internal CharSequence is not already a String,
        // re-save it as a String so subsequent calls to getText will
        // be less expensive.
        if (!(mText instanceof String)) {
            mText = mText.toString();
        }
        
        return mText.toString();
    }

    public void setText(CharSequence text) {
        mText = text;
        notifyModelChanged(true);
    }

    public void cloneText() {
        mText = new String(mText.toString());
    }

    public int getCharset() {
        return mCharset;
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
    }
}


