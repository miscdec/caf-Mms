/*
 * Copyright (c) 2015 pci-suntektech Technologies, Inc.  All Rights Reserved.
 * pci-suntektech Technologies Proprietary and Confidential.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */


package com.android.mms.rcs;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Telephony.Sms;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mms.LogTag;
import com.android.mms.data.Contact;
import com.android.mms.R;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.BoxMessageItem;
import android.widget.QuickContactBadge;
import com.suntek.mway.rcs.client.aidl.constant.Constants.FavoriteMessageProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class FavoriteMessageListAdapter extends CursorAdapter implements Contact.UpdateListener {
    private LayoutInflater mInflater;
    private static final String TAG = "FavoriteMessageListAdapter";

    private OnListContentChangedListener mListChangedListener;
    private final LinkedHashMap<String, BoxMessageItem> mMessageItemCache;
    private static final int CACHE_SIZE = 50;
    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();
    private ListView mListView;
    QuickContactBadge mAvatarView;
    TextView mNameView;
    TextView mBodyView;
    TextView mDateView;
    ImageView mErrorIndicator;
    ImageView mImageViewLock;
    Drawable mBgSelectedDrawable;
    Drawable mBgUnReadDrawable;
    Drawable mBgReadDrawable;

    private int mSubscription = MessageUtils.SUB_INVALID;
    private String mAddress;
    private String mName;
    private int mWapPushAddressIndex;

    public FavoriteMessageListAdapter(Context context, OnListContentChangedListener changedListener,
            Cursor cursor) {
        super(context, cursor);
        mListView = ((ListActivity) context).getListView();
        mInflater = LayoutInflater.from(context);
        mListChangedListener = changedListener;
        mMessageItemCache = new LinkedHashMap<String, BoxMessageItem>(10, 1.0f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CACHE_SIZE;
            }
        };
        mWapPushAddressIndex = context.getResources().getInteger(R.integer.wap_push_address_index);
        mBgSelectedDrawable = context.getResources().getDrawable(
                R.drawable.list_selected_holo_light);
        mBgUnReadDrawable = context.getResources().getDrawable(
                R.drawable.conversation_item_background_unread);
        mBgReadDrawable = context.getResources().getDrawable(
                R.drawable.conversation_item_background_read);
    }

    public BoxMessageItem getCachedMessageItem(String type, long msgId, Cursor c) {
        BoxMessageItem item = mMessageItemCache.get(getKey(type, msgId));
        if (item == null) {
            item = new BoxMessageItem(mContext, type, msgId, c);
            mMessageItemCache.put(getKey(type, item.getMessageId()), item);
        }
        return item;
    }

    private static String getKey(String type, long id) {
        return type + String.valueOf(id);
    }

    private void updateAvatarView() {
        Resources res = mContext.getResources();
        Drawable avatarDrawable = null;

        Contact contact = Contact.get(mAddress, true);
        if (contact.existsInDatabase()) {
            mAvatarView.assignContactUri(contact.getUri());
        } else if (MessageUtils.isWapPushNumber(contact.getNumber())) {
            mAvatarView.assignContactFromPhone(
                    MessageUtils.getWapPushNumber(contact.getNumber()), true);
        } else {
            mAvatarView.assignContactFromPhone(contact.getNumber(), true);
        }

        mAvatarView.setOverlay(avatarDrawable);
        //contact.bindAvatar(mAvatarView);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    public void onUpdate(Contact updated) {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "onUpdate: " + this + " contact: " + updated);
        }

        mHandler.post(new Runnable() {
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.mailbox_msg_list, parent, false);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateItemBackgroud(int position) {
        Cursor cursor = (Cursor)getItem(position);
        View view = mListView.getChildAt(position);
        if (cursor == null || view == null) {
            return;
        }

        if (mListView.isItemChecked(position)) {
            view.setBackgroundDrawable(mBgSelectedDrawable);
        } else {
            view.setBackgroundDrawable(null);
        }
    }

    public void bindView(View view, Context context, Cursor cursor) {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "bind: contacts.addListeners " + this);
        }
        Contact.addListener(this);
        cleanItemCache();

        long msgId = cursor.getLong(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.MSG_ID));
        long threadId = cursor.getLong(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.THREAD_ID));
        String addr = cursor.getString(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.NUMBER));
        String body = cursor.getString(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.CONTENT));
        String nameContact="";
        if (addr.contains(",")) {
            String[] strarray = addr.split(",");
            for (int i = 0; i < strarray.length; i++) {
                String name = Contact.get(strarray[i], false).getName();
                if (null == name || name == "") {
                    name = strarray[i];
                }
                if (i != strarray.length - 1) {
                    name += ",";
                }
                nameContact += name;
            }
        }else{
            nameContact = Contact.get(addr, false).getName();
        }
        long date = cursor.getLong(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.DATE));
        int rcsMsgType = cursor.getInt(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.MSG_TYPE));
        String dateStr = MessageUtils.formatTimeStampString(context, date, false);;
        String bodyStr = RcsUtils.formatConversationSnippet(context, body, rcsMsgType);
        if (mListView.isItemChecked(cursor.getPosition())) {
            view.setBackgroundDrawable(mBgSelectedDrawable);
        } else {
            view.setBackgroundDrawable(null);
        }

        mBodyView = (TextView) view.findViewById(R.id.msgBody);
        mDateView = (TextView) view.findViewById(R.id.textViewDate);
        mErrorIndicator = (ImageView)view.findViewById(R.id.error);
        mImageViewLock = (ImageView) view.findViewById(R.id.imageViewLock);
        mNameView = (TextView) view.findViewById(R.id.textName);
        mAvatarView = (QuickContactBadge) view.findViewById(R.id.avatar);
        mAddress = addr;
        mName = nameContact;

        if (MessageUtils.isWapPushNumber(addr) && MessageUtils.isWapPushNumber(nameContact)) {
            String[] mMailBoxAddresses = addr.split(":");
            String[] mMailBoxName = nameContact.split(":");
            formatNameView(
                    mMailBoxAddresses[mWapPushAddressIndex],
                    mMailBoxName[mWapPushAddressIndex]);
        } else if (MessageUtils.isWapPushNumber(addr)) {
            String[] mailBoxAddresses = addr.split(":");
            addr = mailBoxAddresses[mWapPushAddressIndex];
            formatNameView(addr, mName);
        } else if (MessageUtils.isWapPushNumber(nameContact)) {
            String[] mailBoxName = nameContact.split(":");
            nameContact = mailBoxName[mWapPushAddressIndex];
            formatNameView(mAddress, nameContact);
        } else {
            formatNameView(mAddress, mName);
        }
        updateAvatarView();

        Long lastMsgId = (Long) mAvatarView.getTag();
        boolean sameItem = lastMsgId != null && lastMsgId.equals(msgId);
       // mAvatarView.setChecked(mListView.isItemChecked(cursor.getPosition()), sameItem);
        mAvatarView.setTag(Long.valueOf(msgId));

        mImageViewLock.setVisibility(View.GONE);
        mErrorIndicator.setVisibility(View.GONE);
        mDateView.setText(dateStr);
        mBodyView.setText(bodyStr);
    }

    public void formatNameView(String address, String name) {
        SpannableStringBuilder buf = null;
        if (TextUtils.isEmpty(name)) {
            if (TextUtils.isEmpty(address)) {
                SpannableStringBuilder builder = new SpannableStringBuilder(" ");
                buf = builder;
            } else {
                buf = new SpannableStringBuilder(address);
            }
        } else {
            buf = new SpannableStringBuilder(name);
        }
        mNameView.setText(buf);
    }

    public void cleanItemCache() {
        mMessageItemCache.clear();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mMessageItemCache.clear();
    }

    /**
     * Callback on the UI thread when the content observer on the backing cursor
     * fires. Instead of calling requery we need to do an async query so that
     * the requery doesn't block the UI thread for a long time.
     */
    @Override
    protected void onContentChanged() {
        mListChangedListener.onListContentChanged();
    }

    public interface OnListContentChangedListener {
        void onListContentChanged();
    }

}
