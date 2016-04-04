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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.R;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.ui.LetterTileDrawable;
import com.android.mms.util.MaterialColorMapUtils;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.aidl.common.RcsColumns;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class manages the view for given conversation.
 */
public class ConversationListItem extends RelativeLayout implements Contact.UpdateListener,
            Checkable {
    private static final String TAG = LogTag.TAG;
    private static final boolean DEBUG = false;

    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private TextView mContentView;
    private View mAttachmentView;
    private View mErrorIndicator;
    private QuickContactBadge mAvatarView;

    static private Drawable sDefaultContactImage;
    private static Drawable sDefaultGroupChatImage; // The RCS Group Chat photo.
    private static Drawable sDefaultToPcChatImage;
    private static Drawable sDefaultCheckedImageDrawable;

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();

    private Conversation mConversation;

    public static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    public ConversationListItem(Context context) {
        super(context);
    }

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.stranger);
        }

        if (MmsConfig.isRcsVersion() && sDefaultGroupChatImage == null) {
            sDefaultGroupChatImage = context.getResources().getDrawable(
                    R.drawable.rcs_ic_group_chat_photo);
        }
        if (MmsConfig.isRcsVersion() && sDefaultToPcChatImage == null) {
            sDefaultToPcChatImage = context.getResources().getDrawable(
                    R.drawable.rcs_ic_topc_chat_photo);
        }
        if (sDefaultCheckedImageDrawable == null) {
            sDefaultCheckedImageDrawable = context.getResources().getDrawable(R.drawable.selected);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFromView = (TextView) findViewById(R.id.from);
        mSubjectView = (TextView) findViewById(R.id.subject);

        mDateView = (TextView) findViewById(R.id.date);
        mContentView = (TextView)findViewById(R.id.content);
        mAttachmentView = findViewById(R.id.attachment);
        mErrorIndicator = findViewById(R.id.error);
        mAvatarView = (QuickContactBadge) findViewById(R.id.avatar);
        mAvatarView.setOverlay(null);
    }

    public Conversation getConversation() {
        return mConversation;
    }

    /**
     * Only used for header binding.
     */
    public void bind(String title, String explain) {
        mFromView.setText(title);
        mSubjectView.setText(explain);
    }

    private CharSequence formatMessage() {
        final int color = android.R.styleable.Theme_textColorSecondary;
        String from;
        if (MmsConfig.isRcsVersion()) {
            if (mConversation.isPcChat()) {
                from = mContext.getResources().getString(R.string.rcs_to_pc_conversion);
            } else if (mConversation.isGroupChat()) {
                GroupChat groupChat = mConversation.getGroupChat();
                if (groupChat != null) {
                    from = RcsUtils.getDisplayName(groupChat);
                } else {
                    from = mContext.getResources().getString(R.string.group_chat);
                }
            } else {
                from = mConversation.getRecipients().formatNames(", ");
            }
        } else {
            from = mConversation.getRecipients().formatNames(", ");
        }
        if (MessageUtils.isWapPushNumber(from)) {
            String[] mAddresses = from.split(":");
            from = mAddresses[mContext.getResources().getInteger(
                    R.integer.wap_push_address_index)];
        }

        /**
         * Add boolean to know that the "from" haven't the Arabic and '+'.
         * Make sure the "from" display normally for RTL.
         */
        Boolean isEnName = false;
        Boolean isLayoutRtl = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL);
        if (isLayoutRtl && from != null) {
            if (from.length() >= 1) {
                Pattern pattern = Pattern.compile("[^أ-ي]+");
                Matcher matcher = pattern.matcher(from);
                isEnName = matcher.matches();
                if (from.charAt(0) != '\u202D') {
                    if (isEnName) {
                        from = '\u202D' + from + '\u202C';
                    }
                }
            }
        }

        SpannableStringBuilder buf = new SpannableStringBuilder(from);

        if (mConversation.getMessageCount() > 1) {
            int before = buf.length();
            if (isLayoutRtl) {
                if (isEnName) {
                    buf.insert(1, mConversation.getMessageCount() + " ");
                    buf.setSpan(new ForegroundColorSpan(
                            mContext.getResources().getColor(R.color.message_count_color)),
                            1, buf.length() - before, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                } else {
                    buf.append(" " + mConversation.getMessageCount());
                    buf.setSpan(new ForegroundColorSpan(
                            mContext.getResources().getColor(R.color.message_count_color)),
                            before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            } else {
                buf.append(mContext.getResources().getString(R.string.message_count_format,
                        mConversation.getMessageCount()));
                buf.setSpan(new ForegroundColorSpan(
                        mContext.getResources().getColor(R.color.message_count_color)),
                        before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
           }
        }
        if (mConversation.hasDraft()) {
            if (isLayoutRtl && isEnName) {
                int before = buf.length();
                buf.insert(1,'\u202E'
                        + mContext.getResources().getString(R.string.draft_separator)
                        + '\u202C');
                buf.setSpan(new ForegroundColorSpan(
                        mContext.getResources().getColor(R.drawable.text_color_black)),
                        1, buf.length() - before + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                before = buf.length();
                int size;
                buf.insert(1,mContext.getResources().getString(R.string.has_draft));
                size = android.R.style.TextAppearance_Small;
                buf.setSpan(new TextAppearanceSpan(mContext, size), 1,
                        buf.length() - before + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                buf.setSpan(new ForegroundColorSpan(
                        mContext.getResources().getColor(R.drawable.text_color_red)),
                        1, buf.length() - before + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                mContentView.setVisibility(View.VISIBLE);
                mContentView.setText(mContext.getResources().getString(R.string.has_draft));
                mDateView.setVisibility(GONE);
              }
        } else {
            mContentView.setVisibility(View.GONE);
            mDateView.setVisibility(VISIBLE);
        }

        // Unread messages are shown in bold
        if (mConversation.hasUnreadMessages()) {
            mSubjectView.setSingleLine(false);
            mSubjectView.setMaxLines(mContext.getResources().getInteger(
                    R.integer.max_unread_message_lines));
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            getLayoutParams().height = mContext.getResources().getDimensionPixelSize(
                    R.dimen.conversation_list_itme_height_unread);
        } else {
            mSubjectView.setSingleLine(true);
            getLayoutParams().height = mContext.getResources().getDimensionPixelSize(
                    R.dimen.conversation_list_itme_height);
        }
        return buf;
    }

    private void updateAvatarView() {
        if (MmsConfig.isRcsVersion()) {
            if (mConversation.isGroupChat()) {
                mAvatarView.assignContactUri(null);
                mAvatarView.setImageDrawable(sDefaultGroupChatImage);
                mAvatarView.setVisibility(View.VISIBLE);
                return;
            }
            if (mConversation.isPcChat()) {
                mAvatarView.assignContactUri(null);
                mAvatarView.setImageDrawable(sDefaultToPcChatImage);
                mAvatarView.setVisibility(View.VISIBLE);
                return;
            }
        }
        mAvatarView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        Drawable avatarDrawable;
        Drawable backgroundDrawable = null;
        if (mConversation.isChecked()) {
            avatarDrawable = sDefaultCheckedImageDrawable;
            mAvatarView.setBackgroundResource(R.drawable.selected_icon_background);
        } else {
            if (mConversation.getRecipients().size() == 1) {
                Contact contact = mConversation.getRecipients().get(0);
                avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);

                if (contact.existsInDatabase()) {
                    // Contact already exist in phonebook
                    // Check whether there is user-defined photo for this contact
                    if (avatarDrawable.equals(sDefaultContactImage)) {
                        // Do not have user-defined photo for this contact
                        // If contact name start with English letter, use the first letter as avatar
                        // Otherwise, use default avatar.
                        if (LetterTileDrawable.isEnglishLetterString(contact.getNameForAvatar())) {
                            avatarDrawable = MaterialColorMapUtils
                                    .getLetterTitleDraw(mContext, contact);
                        } else {
                            backgroundDrawable = MaterialColorMapUtils.getLetterTitleDraw(mContext,
                                    contact);
                            mAvatarView.setBackgroundDrawable(backgroundDrawable);
                        }
                    } else {
                        // Have user-defined photo for this contact, just use it
                        mAvatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        mAvatarView.setBackgroundDrawable(null);
                    }

                    mAvatarView.assignContactUri(contact.getUri());
                } else {
                    // identify it is phone number or email address,handle it respectively
                    if (Telephony.Mms.isEmailAddress(contact.getNumber())) {
                        mAvatarView.assignContactFromEmail(contact.getNumber(), true);
                    } else if (MessageUtils.isWapPushNumber(contact.getNumber())) {
                        mAvatarView.assignContactFromPhone(
                                MessageUtils.getWapPushNumber(contact.getNumber()), true);
                    } else {
                        mAvatarView.assignContactFromPhone(contact.getNumber(), true);
                    }
                    contact.setContactColor(mContext.getResources().getColor(
                            R.color.avatar_default_color));
                    backgroundDrawable = MaterialColorMapUtils
                            .getLetterTitleDraw(mContext, contact);
                    mAvatarView.setBackgroundDrawable(backgroundDrawable);
                }
            } else {
                // TODO: Need to implement this group function (TS)
                avatarDrawable = sDefaultContactImage;
                backgroundDrawable = MaterialColorMapUtils.getLetterTitleDraw(mContext, null);
                mAvatarView.setBackgroundDrawable(backgroundDrawable);
            }
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    private void updateFromView() {
        mFromView.setText(formatMessage());
        updateAvatarView();
    }

    public void onUpdate(Contact updated) {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "onUpdate: " + this + " contact: " + updated);
        }
        mHandler.post(new Runnable() {
            public void run() {
                updateFromView();
            }
        });
    }

    public final void bind(Context context, final Conversation conversation) {
        //if (DEBUG) Log.v(TAG, "bind()");

        mConversation = conversation;

        updateBackground();

        boolean hasError = conversation.hasError();

        boolean hasAttachment = conversation.hasAttachment();
        mAttachmentView.setVisibility(hasAttachment ? VISIBLE : GONE);

        // Date
        mDateView.setText(formateUnreadToBold(MessageUtils.formatTimeStampString(context,
                conversation.getDate())));

        // From.
        mFromView.setText(formatMessage());

        // Register for updates in changes of any of the contacts in this conversation.
        ContactList contacts = conversation.getRecipients();

        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "bind: contacts.addListeners " + this);
        }
        Contact.addListener(this);
        if (MmsConfig.isRcsVersion()) {
            int messageID = conversation.getRcsLastMsgId();
                // Date
                mDateView.setText(formateUnreadToBold(MessageUtils.formatTimeStampString(context,
                        conversation.getDate())));
                // Subject
                String snippet = RcsUtils.formatConversationSnippet(getContext(),
                        conversation.getSnippet(), conversation.getRcsLastMsgType());
                // TODO judge the latest message is notification message.
                if (conversation.isGroupChat()) {
                    snippet = RcsUtils.getStringOfNotificationBody(context, snippet);
                    mSubjectView.setText(snippet);
                } else if (mConversation.hasUnreadMessages()) {
                    SpannableStringBuilder buf = new SpannableStringBuilder(snippet);
                    buf.setSpan(STYLE_BOLD, 0, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    mSubjectView.setText(buf);
                } else {
                    mSubjectView.setText(snippet);
                }
        } else {
            mSubjectView.setText(formateUnreadToBold(conversation.getSnippet()));
        }

        // Transmission error indicator.
        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);

        updateAvatarView();
    }

    private CharSequence formateUnreadToBold(String content) {
        SpannableStringBuilder buf = new SpannableStringBuilder(content);
        if (mConversation.hasUnreadMessages()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return buf;
    }

    private void updateBackground() {
        int backgroundId;
        if (mConversation != null && mConversation.isChecked()) {
            backgroundId = R.color.conversation_item_selected;
        } else if (mConversation != null && mConversation.hasUnreadMessages()) {
            backgroundId = R.drawable.conversation_item_background_unread;
        } else {
            backgroundId = R.drawable.conversation_item_background_read;
        }
        Drawable background = mContext.getResources().getDrawable(backgroundId);
        setBackground(background);
    }

    public final void unbind() {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "unbind: contacts.removeListeners " + this);
        }
        // Unregister contact update callbacks.
        Contact.removeListener(this);
    }

    public void setChecked(boolean checked) {
        try {
            if(mConversation != null){
                mConversation.setIsChecked(checked);
                updateBackground();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    public boolean isChecked() {
        return mConversation != null && mConversation.isChecked();
    }

    public void toggle() {
        mConversation.setIsChecked(!mConversation.isChecked());
    }

    /* Begin add for RCS */
    public void setGroupChatImage(Drawable drawable){
        this.sDefaultGroupChatImage = drawable;
    }

    public void bindAvatar(Drawable drawable){
        mAvatarView.assignContactUri(null);
        mAvatarView.setImageDrawable(drawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    public boolean isBurnMsg(int messageID){
        boolean isBurnMsg = false;
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://sms/");
            cursor = mContext.getContentResolver().query (uri, null, Sms._ID + " = ? and "
                    + Sms.TYPE + " != 0", new String[] {String.valueOf(messageID)}, null);
            if (cursor != null && cursor.moveToFirst()) {
                isBurnMsg = (cursor.getInt(cursor.getColumnIndex(
                        RcsColumns.SmsRcsColumns.RCS_BURN))> RcsUtils.RCS_NOT_A_BURN_MESSAGE);
           }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isBurnMsg;
    }
    /* End add for RCS */

}
