/*
 * Copyright (c) 2012-2013, Code Aurora Forum. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained 
 * for attribution purposes only.
 * Copyright (C) 2012 The Android Open Source Project. 
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
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.android.mms.data.Contact;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.util.Log;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_ADDRESS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_BODY;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SUB_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_STATUS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE_SENT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_THREAD_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import android.provider.Settings;
import android.content.ContentResolver;

/**
 * Mostly immutable model for an SMS/MMS message.
 *
 * <p>The only mutable field is the cached formatted message member,
 * the formatting of which is done outside this model in MessageListItem.
 */
public class BoxMessageItem {
    private static String TAG = "BoxMessageItem";

    final Context mContext;
    final String mType;
    final long mMsgId;
    int mSubID;
    long mDate;    
    long mDateSent;    
    String mAddress;
    String mContact;
    String mBody; // Body of SMS, first text of MMS.

    // The only non-immutable field.  Not synchronized, as access will
    // only be from the main GUI thread.  Worst case if accessed from
    // another thread is it'll return null and be set again from that
    // thread.
    CharSequence mCachedFormattedMessage;

    // Fields for MMS only.
    int mRead;
    int mStatus;
    int mSmsType;//if icc sms then StatusOnIcc
    int mLocked;    
    String mDateStr;
    String mName;
    long mThreadId = 0;  

    BoxMessageItem(Context context, String type, long msgId, Cursor cursor)
    {
        mContext = context;
        mType = type;   
        mMsgId = msgId;  

        if ("sms".equals(type)) 
        {
            mBody = cursor.getString(COLUMN_SMS_BODY);
            mAddress = cursor.getString(COLUMN_SMS_ADDRESS);
            mDate = cursor.getLong(COLUMN_SMS_DATE); 
            mStatus = cursor.getInt(COLUMN_SMS_STATUS);                            
            mRead = cursor.getInt(COLUMN_SMS_READ);
            mSubID = cursor.getInt(COLUMN_SUB_ID);
            mDateSent = cursor.getLong(COLUMN_SMS_DATE_SENT);  
            mSmsType = cursor.getInt(COLUMN_SMS_TYPE);
            mLocked = cursor.getInt(COLUMN_SMS_LOCKED);
            mThreadId = cursor.getInt(COLUMN_THREAD_ID);          
            // For incoming messages, the ADDRESS field contains the sender.
            mName = Contact.get(mAddress, true).getName();                         
        }                   
        
        mDateStr = MessageUtils.formatTimeStampString(context,
                                                        mDate, false);
    }

    public boolean isMms() {
        return mType.equals("mms");
    }

    public boolean isSms() {
        return (mType.equals("sms"));
    }

    public void setBody(String body) 
    {
        mBody = body;
    }    

    // Note: This is the only mutable field in this class.  Think of
    // mCachedFormattedMessage as a C++ 'mutable' field on a const
    // object, with this being a lazy accessor whose logic to set it
    // is outside the class for model/view separation reasons.  In any
    // case, please keep this class conceptually immutable.
    public void setCachedFormattedMessage(CharSequence formattedMessage) {
        mCachedFormattedMessage = formattedMessage;
    }

    public CharSequence getCachedFormattedMessage() {
        return mCachedFormattedMessage;
    }
}
