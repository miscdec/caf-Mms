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
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.app.ListActivity;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduPersister;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_THREAD_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_ADDRESS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_BODY;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_STATUS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_SUBJECT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_BOX;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_SUBJECT_CHARSET;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_ERROR_TYPE; 
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_SUB_ID; 
import static com.android.mms.ui.MessageListAdapter.COLUMN_RECIPIENT_IDS; 

import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.mms.ui.MessageUtils;
import java.util.LinkedHashMap;
import android.provider.Telephony.Threads;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import com.google.android.mms.MmsException;
import java.util.Map;
import android.net.Uri;
import android.text.TextUtils;
import com.android.mms.util.AddressUtils;
import com.google.android.mms.util.SqliteWrapper;
import android.content.ContentUris;
import java.sql.Timestamp;
import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;
import android.telephony.SmsManager;
import android.text.format.Time;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.Intents;
import android.content.Intent;
import android.widget.QuickContactBadge;
import com.android.mms.data.Contact;
import com.android.mms.LogTag;
import android.os.Handler;

import android.content.res.Resources;
import android.util.SparseBooleanArray;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.graphics.Typeface;

public class MailBoxMessageListAdapter extends CursorAdapter
    implements Contact.UpdateListener
{
    private LayoutInflater mInflater;
    private static final String TAG = "MailBoxMessageListAdapter";

    private OnListContentChangedListener mListChangedListener;
    private final LinkedHashMap<String, BoxMessageItem> mMessageItemCache;
    private static final int CACHE_SIZE = 50;
    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    
    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();
    private ActivityCanPaused mMailBoxMessageList;
    private ListView mListView;
    QuickContactBadge mAvatarView;
    TextView mAddressView;
    TextView mNameView;
    TextView mBodyView;
    TextView mDateView;
    ImageView mImageViewLock;
    private int mSubscription = MessageUtils.SUB_INVALID;
    private String mMsgType;  // "sms" or "mms"
    private String mAddress;
    private String mName;
    private int mScreenWidth;
    
    public MailBoxMessageListAdapter(Context context,
                                     ActivityCanPaused mailBoxMessageList,
                                     OnListContentChangedListener changedListener,
                                     Cursor cursor)
    {
        super(context, cursor);
        mListView = ((ListActivity)context).getListView();
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);
        mMailBoxMessageList = mailBoxMessageList;
        mListChangedListener = changedListener;
        mScreenWidth = ((ListActivity)context).getWindowManager().getDefaultDisplay().getWidth();
        mMessageItemCache = new LinkedHashMap<String, BoxMessageItem>(
                                10, 1.0f, true)
                            {
                                @Override
                                protected boolean removeEldestEntry(Map.Entry eldest)
                                {
                                    return size() > CACHE_SIZE;
                                }
                            };
                            
    }
    
    public BoxMessageItem getCachedMessageItem(String type, long msgId, Cursor c)
    {
        BoxMessageItem item = mMessageItemCache.get(getKey(type, msgId));
        if (item == null)
        {
            item = new BoxMessageItem(mContext, type, msgId, c);
            mMessageItemCache.put(getKey(item.mType, item.mMsgId), item);
        }
        return item;
    }

    private static String getKey(String type, long id)
    {
        if (type.equals("mms"))
        {
            return "";
        }
        else
        {
            return type + String.valueOf(id);
        }
    }

    private void updateAvatarView() {
        Drawable avatarDrawable;
        Drawable sDefaultContactImage = mContext.getResources().getDrawable(R.drawable.ic_contact_picture);
        Drawable sDefaultContactImageMms = mContext.getResources().getDrawable(R.drawable.ic_contact_picture_mms);
        if(MessageUtils.isMultiSimEnabledMms())
        {
            sDefaultContactImage = (mSubscription == MessageUtils.SUB1) ? 
                mContext.getResources().getDrawable(R.drawable.ic_contact_picture_card1) : 
                mContext.getResources().getDrawable(R.drawable.ic_contact_picture_card2);
            sDefaultContactImageMms = (mSubscription == MessageUtils.SUB1) ? 
                mContext.getResources().getDrawable(R.drawable.ic_contact_picture_mms_card1) : 
                mContext.getResources().getDrawable(R.drawable.ic_contact_picture_mms_card2);
        }
        
        Contact contact = Contact.get(mAddress, true);
        if(mMsgType.equals("mms"))
        {
            avatarDrawable = sDefaultContactImageMms;
        }
        else
        {
            avatarDrawable = sDefaultContactImage;
        }

        if (contact.existsInDatabase()) {
            mAvatarView.assignContactUri(contact.getUri());
        } else {
            mAvatarView.assignContactFromPhone(contact.getNumber(), true);
        }

        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    public void onUpdate(Contact updated) {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "onUpdate: " + this + " contact: " + updated);
        }
        mHandler.post(new Runnable() {
            public void run() {
                updateAvatarView();
                mName = Contact.get(mAddress, true).getName();
                formatNameView(mAddress, mName);
            }
        });
    }
    
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        /* FIXME: this is called 3+x times too many by the ListView */
        View ret = mInflater.inflate(R.layout.mailbox_msg_list, parent, false);
        return ret;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void bindView(View view, Context context, Cursor cursor)
    {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "bindView: contacts.addListeners " + this);
        }
        Contact.addListener(this);
        cleanItemCache();
        
        final String type = cursor.getString(COLUMN_MSG_TYPE);
        mMsgType = type;
        final long msgId = cursor.getLong(COLUMN_ID);
        final String mstrMsgId = cursor.getString(COLUMN_ID);       
        final long threadId = cursor.getLong(COLUMN_THREAD_ID);
        String tmp = "";
        
        if (null != cursor.getString(COLUMN_MMS_SUBJECT))
        {
            tmp = new EncodedStringValue(cursor.getInt(COLUMN_MMS_SUBJECT_CHARSET), 
                PduPersister.getBytes(cursor.getString(COLUMN_MMS_SUBJECT))).getString();
        }
        final String subject = context.getString(R.string.forward_prefix) + tmp;       
        String addr = "";
        String bodyStr = "";
        String nameContact = "";
        String dateStr = "";
        String recipientIds = "";
        // Set time stamp
        long date = 0;
        Drawable sendTypeIcon = null;
        int isLocked = 0;
        int msgBox = Sms.MESSAGE_TYPE_INBOX;
        boolean isUnread=false;  

        if (type.equals("sms"))
        {
            BoxMessageItem item = getCachedMessageItem(type, msgId, cursor);
            int status = item.mStatus;
            msgBox = item.mSmsType;
            int smsRead = item.mRead;
            isUnread=(smsRead == 0 ? true : false);
            mSubscription = item.mSubID;
            addr = item.mAddress;
            isLocked = item.mLocked;
            bodyStr = item.mBody;
            dateStr = item.mDateStr;
            nameContact = item.mName;
        }
        else if (type.equals("mms"))
        {        
            final int mmsRead = cursor.getInt(COLUMN_MMS_READ);
            mSubscription = cursor.getInt(COLUMN_MMS_SUB_ID);
            int messageType = cursor.getInt(COLUMN_MMS_MESSAGE_TYPE);
            msgBox = cursor.getInt(COLUMN_MMS_MESSAGE_BOX);
            isLocked = cursor.getInt(COLUMN_MMS_LOCKED);
            recipientIds = cursor.getString(COLUMN_RECIPIENT_IDS);

            if(0 == mmsRead && msgBox == Mms.MESSAGE_BOX_INBOX )
            {
                isUnread=true;
            }

            bodyStr = MessageUtils.extractEncStrFromCursor(cursor,
                      COLUMN_MMS_SUBJECT, COLUMN_MMS_SUBJECT_CHARSET );
            if (bodyStr.equals(""))
            {
                bodyStr = mContext.getString(R.string.no_subject_view);
            }

            date = cursor.getLong(COLUMN_MMS_DATE) * 1000;
            dateStr = MessageUtils.formatTimeStampString(context,
                      date, false);

            //get address and name of MMS from recipientIds
            addr = recipientIds;
            if (!TextUtils.isEmpty(recipientIds))
            {
                addr = MessageUtils.getRecipientsByIds(
                        context, recipientIds, true);
                nameContact = Contact.get(addr, true).getName();
            }
            else if (threadId > 0)
            {
                addr = MessageUtils.getAddressByThreadId(context, threadId);
                nameContact = Contact.get(addr, true).getName();
            }
            else
            {
                addr = "";
                nameContact = "";
            }
        }

        int backgroundId;
        if (mListView.isItemChecked(cursor.getPosition())) {
             backgroundId = R.drawable.list_selected_holo_light;
        } 
        else if(isUnread){
             backgroundId = R.drawable.conversation_item_background_unread;  //conversation_item_background_unread;
        }
        else
        {
             backgroundId = R.drawable.conversation_item_background_read; 
        }      
        Drawable background = context.getResources().getDrawable(backgroundId);
        if(view != null)
        {
            view.setBackgroundDrawable(background);
        }

        mBodyView = (TextView) view.findViewById(R.id.MsgBody);
        mDateView = (TextView) view.findViewById(R.id.TextViewDate);
        mImageViewLock = (ImageView) view.findViewById(R.id.imageViewLock);
        mAddressView = (TextView) view.findViewById(R.id.MsgAddress);
        mNameView = (TextView) view.findViewById(R.id.TextName);
        mAvatarView = (QuickContactBadge) view.findViewById(R.id.avatar);
        mAddress = addr;
        mName = nameContact;
        formatNameView(mAddress, mName);
        updateAvatarView();
        
        if (isLocked == 1)
        {
            mImageViewLock.setVisibility(View.VISIBLE);
        }
        else
        {
            mImageViewLock.setVisibility(View.GONE);
        }
        
        mDateView.setText(dateStr);
        
        if(bodyStr != null && bodyStr.length() > 120)
        {
            String bodyPart = bodyStr.substring(0,120) + "...";
            mBodyView.setText(bodyPart);
        }
        else
        {
            mBodyView.setText(bodyStr);
        }
    }

    public void formatNameView(String address, String name)
    {
        if (TextUtils.isEmpty(name) || name.equals(address))
        {
            mNameView.setText(address);
            mNameView.setMaxWidth(mScreenWidth/2);
            mNameView.setMinWidth(mScreenWidth/2);
            mAddressView.setText("");
        }
        else
        {
            mNameView.setText(name);
            mNameView.setMaxWidth(130);
            mNameView.setMinWidth(100);
            mAddressView.setVisibility(View.VISIBLE);
            mAddressView.setText(address);
        }
    }
        
    public void cleanItemCache()
    {
        mMessageItemCache.clear();
    }

    @Override
    public void notifyDataSetChanged()
    {
        super.notifyDataSetChanged();
        mMessageItemCache.clear();
    }

    /**
     * Callback on the UI thread when the content observer on the backing cursor fires.
     * Instead of calling requery we need to do an async query so that the requery doesn't
     * block the UI thread for a long time. 
     */
    @Override
    protected void onContentChanged()
    {        
        mListChangedListener.onListContentChanged();     
    }

    public interface OnListContentChangedListener
    {
        void onListContentChanged();
    }

    public interface ActivityCanPaused
    {
        boolean isHasPaused();
    }        
}
