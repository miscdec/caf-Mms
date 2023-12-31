/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.rcs.RcsChatMessageUtils;
import com.android.mms.rcs.RcsDualSimMananger;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessageUtils;

import com.google.android.mms.MmsException;

import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.support.SupportApi;
import com.suntek.rcs.ui.common.RcsLog;

public class MailBoxMessageContent extends Activity {
    private static final String TAG = "MessageDetailActivity";
    private Uri mMessageUri;
    private int mMsgId;
    private long mMsgThreadId;// threadid of message
    private String mMsgText;// Text of message
    private String mMsgFrom;
    private int mMsgstatus;
    private int mRead;
    private int mMailboxId;
    private int mMsgType = Sms.MESSAGE_TYPE_INBOX;
    private boolean mLock = false;
    private boolean mIsConvMode;

    private int mSubID = MessageUtils.SUB_INVALID;
    private Cursor mCursor = null;

    private ViewPager mContentPager;
    private MessageDetailAdapter mPagerAdapter;
    /*Operations for gesture to scale the current text fontsize of content*/
    private float mScaleFactor = 1;
    private  ScaleGestureDetector mScaleDetector;

    private static final int MENU_CALL_RECIPIENT = Menu.FIRST;
    private static final int MENU_DELETE = Menu.FIRST + 1;
    private static final int MENU_FORWARD = Menu.FIRST + 2;
    private static final int MENU_REPLY = Menu.FIRST + 3;
    private static final int MENU_RESEND = Menu.FIRST + 4;
    private static final int MENU_SAVE_TO_CONTACT = Menu.FIRST + 5;
    private static final int MENU_LOCK = Menu.FIRST + 6;
    private static final int MENU_FAVORITED = Menu.FIRST + 7;
    private static final int MENU_UNFAVORITED = Menu.FIRST + 8;

    private BackgroundHandler mBackgroundHandler;
    private static final int DELETE_MESSAGE_TOKEN = 6701;
    private static final int QUERY_MESSAGE_TOKEN = 6702;

    private static final int OPERATE_DEL_SINGLE_OVER = 1;
    private static final int UPDATE_UI = 2;
    private static final int SHOW_TOAST = 3;

    private ContentResolver mContentResolver;
    private static final String[] SMS_LOCK_PROJECTION = {
        Sms._ID,
        Sms.LOCKED
    };
    private static final String[] SMS_DETAIL_PROJECTION_DEFAULT = new String[] {
        Sms.THREAD_ID,
        Sms.DATE,
        Sms.ADDRESS,
        Sms.BODY,
        Sms.SUBSCRIPTION_ID,
        Sms.LOCKED,
        Sms.DATE_SENT,
        Sms.TYPE,
        Sms.ERROR_CODE,
        Sms._ID,
        Sms.STATUS,
        Sms.READ
    };

    private static final String[] SMS_DETAIL_PROJECTION_RCS = new String[] {
        Sms.THREAD_ID,
        Sms.DATE,
        Sms.ADDRESS,
        Sms.BODY,
        Sms.SUBSCRIPTION_ID,
        Sms.LOCKED,
        Sms.DATE_SENT,
        Sms.TYPE,
        Sms.ERROR_CODE,
        Sms._ID,
        Sms.STATUS,
        Sms.READ,
        RcsColumns.SmsRcsColumns.RCS_MSG_STATE,
        RcsColumns.SmsRcsColumns.RCS_CHAT_TYPE,
        RcsColumns.SmsRcsColumns.RCS_MSG_TYPE,
        RcsColumns.SmsRcsColumns.RCS_THUMB_PATH,
        RcsColumns.SmsRcsColumns.RCS_FILENAME,
        RcsColumns.SmsRcsColumns.RCS_FILE_SIZE,
        RcsColumns.SmsRcsColumns.RCS_MIME_TYPE
    };

    private static final String[] SMS_DETAIL_PROJECTION = MmsConfig.isRcsVersion() ?
            SMS_DETAIL_PROJECTION_RCS : SMS_DETAIL_PROJECTION_DEFAULT;
    private static final int COLUMN_THREAD_ID = 0;
    private static final int COLUMN_DATE = 1;
    private static final int COLUMN_SMS_ADDRESS = 2;
    private static final int COLUMN_SMS_BODY = 3;
    private static final int COLUMN_SMS_SUBID = 4;
    private static final int COLUMN_SMS_LOCKED = 5;
    private static final int COLUMN_DATE_SENT = 6;
    private static final int COLUMN_SMS_TYPE = 7;
    private static final int COLUMN_SMS_ERROR_CODE = 8;
    private static final int COLUMN_ID = 9;
    private static final int COLUMN_STATUS = 10;
    private static final int COLUMN_SMS_READ = 11;
    private static final int COLUMN_RCS_MSG_STATE = 12;
    private static final int COLUMN_RCS_CHAT_TYPE = 13;
    private static final int COLUMN_RCS_MSG_TYPE = 14;
    private static final int COLUMN_RCS_THUMB_PATH = 15;
    private static final int COLUMN_RCS_FILENAME = 16;
    private static final int COLUMN_RCS_FILE_SIZE = 17;
    private static final int COLUMN_RCS_MIME_TYPE = 18;

    private static final int SMS_ADDRESS_INDEX = 0;
    private static final int SMS_BODY_INDEX = 1;
    private static final int SMS_SUB_ID_INDEX = 2;

    /* Begin add for RCS */
    private int mRcsChatType;
    private int mRcsMsgState;
    // RCS Message API
    private MessageApi mMessageApi = MessageApi.getInstance();
    private SupportApi mSupportApi = SupportApi.getInstance();
    private static final int FORWARD_INPUT_NUMBER = 0;
    private static final int FORWARD_CONTACTS = 1;
    private static final int FORWARD_CONVERSATION = 2;
    private static final int FORWARD_GROUP = 3;

    private static final int REQUEST_CODE_PICK = 109;
    private static final int REQUEST_CODE_RCS_PICK = 115;
    private static final int REQUEST_SELECT_CONV = 116;
    private static final int REQUEST_SELECT_GROUP = 117;

    /* End add for RCS */

    private float mFontSizeForSave = MessageUtils.FONT_SIZE_DEFAULT;

    private ArrayList<TextView> mSlidePaperItemTextViews;

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mFontSizeForSave = MessageUtils.onFontSizeScale(mSlidePaperItemTextViews,
                    detector.getScaleFactor(), mFontSizeForSave);
            mPagerAdapter.setBodyFontSize(mFontSizeForSave);
            mPagerAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        setContentView(R.layout.message_detail_viewpaper);
        mContentResolver = getContentResolver();
        mBackgroundHandler = new BackgroundHandler(mContentResolver);
        mSlidePaperItemTextViews = new ArrayList<TextView>();
        handleIntent();
        startQuerySmsContent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MessageUtils.saveTextFontSize(this, mFontSizeForSave);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUiHandler.removeCallbacksAndMessages(null);
        if (mContentPager != null) {
            mContentPager.setAdapter(null);
        }
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            mScaleDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        if (MessageUtils.hasIccCard()) {
            menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                    .setIcon(R.drawable.ic_menu_call)
                    .setTitle(R.string.menu_call)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (MmsConfig.isRcsVersion() && mRcsMsgState == RcsUtils.MESSAGE_HAS_BURNED) {
            menu.clear();
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
            return true;
        }
        if (mMsgType == Sms.MESSAGE_TYPE_INBOX) {
            menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
        } else if (mMsgType == Sms.MESSAGE_TYPE_FAILED
                || mMsgType == Sms.MESSAGE_TYPE_OUTBOX) {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
            if (!isRcsMessage()) {
                menu.add(0, MENU_RESEND, 0, R.string.menu_resend);
            }
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
        } else if (mMsgType == Sms.MESSAGE_TYPE_SENT) {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
        } else if (mMsgType == Sms.MESSAGE_TYPE_QUEUED) {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
        }

        if (isLockMessage()) {
            menu.add(0, MENU_LOCK, 0, R.string.menu_unlock);
        } else {
            menu.add(0, MENU_LOCK, 0, R.string.menu_lock);
        }

        if (!Contact.get(mMsgFrom, false).existsInDatabase()) {
            menu.add(0, MENU_SAVE_TO_CONTACT, 0, R.string.menu_add_to_contacts);
        }

        if (MmsConfig.isRcsEnabled()) {
            if (!RcsChatMessageUtils.isFavoritedMessage(this, mMsgId)) {
                menu.add(0, MENU_FAVORITED, 0, R.string.favorited);
            } else {
                menu.add(0, MENU_UNFAVORITED, 0, R.string.unfavorited);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_CALL_RECIPIENT:
                MessageUtils.dialNumber(this,mMsgFrom);
                break;
            case MENU_DELETE:
                mLock = isLockMessage();
                DeleteMessageListener l = new DeleteMessageListener();
                confirmDeleteDialog(l, mLock);
                break;
            case MENU_FORWARD:
                if (isRcsMessage()) {
                    boolean isEnableRcsSendingPolicy = RcsDualSimMananger
                            .getUserIsUseRcsPolicy(MailBoxMessageContent.this);
                    if (isEnableRcsSendingPolicy) {
                        RcsChatMessageUtils.forwardContactOrConversation(this,
                                new ForwardClickListener());
                    } else {
                        Toast.makeText(MailBoxMessageContent.this,
                                R.string.rcs_sending_policy_not_support_forwarding,
                                Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Intent intentForward = new Intent(this, ComposeMessageActivity.class);
                    intentForward.putExtra("sms_body", mMsgText);
                    intentForward.putExtra("exit_on_sent", true);
                    intentForward.putExtra("forwarded_message", true);
                    this.startActivity(intentForward);
                }
                break;
            case MENU_REPLY:
                Intent intentReplay = new Intent(this, ComposeMessageActivity.class);
                intentReplay.putExtra("reply_message", true);
                intentReplay.putExtra("address", mMsgFrom);
                intentReplay.putExtra("exit_on_sent", true);
                this.startActivity(intentReplay);
                break;
            case MENU_LOCK:
                lockUnlockMessage();
                break;
            case MENU_RESEND:
                resendShortMessage(mMsgThreadId, mMessageUri);
                finish();
                break;
            case MENU_SAVE_TO_CONTACT:
                saveToContact();
                break;
            case MENU_FAVORITED:
                RcsChatMessageUtils.favoritedOneMessage(this, mMsgId, isRcsMessage(), true);
                break;
            case MENU_UNFAVORITED:
                RcsChatMessageUtils.favoritedOneMessage(this, mMsgId, isRcsMessage(), false);
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }

        return true;
    }

    private void confirmDeleteDialog(OnClickListener listener, boolean locked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(locked ? R.string.confirm_delete_locked_message
                : R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    public void saveToContact() {
        String address = mMsgFrom;
        if (TextUtils.isEmpty(address)) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG, "  saveToContact fail for null address! ");
            }
            return;
        }

        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        this.startActivity(intent);
    }

    private void resendShortMessage(long threadId, Uri uri) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(), uri, new String[] {
                Sms.ADDRESS, Sms.BODY, Sms.SUBSCRIPTION_ID
        }, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    MessageSender sender = new SmsMessageSender(this,
                            new String[] {cursor.getString(SMS_ADDRESS_INDEX)},
                            cursor.getString(SMS_BODY_INDEX),
                            threadId,
                            cursor.getInt(SMS_SUB_ID_INDEX));
                    sender.sendMessage(threadId);

                    // Delete the undelivered message since the sender will
                    // save a new one into database.
                    SqliteWrapper.delete(this, getContentResolver(), uri, null, null);
                }
            } catch (MmsException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                cursor.close();
            }
        } else {
            Toast.makeText(MailBoxMessageContent.this, R.string.send_failure, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void lockUnlockMessage() {
        int lockValue;
        // 1, lock; 0, unlock
        mLock = isLockMessage();
        final Uri lockUri = mMessageUri;
        lockValue = mLock ? 0 : 1;
        final ContentValues values = new ContentValues(1);
        values.put("locked", lockValue);

        new Thread(new Runnable() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = SHOW_TOAST;
                if (getContentResolver().update(lockUri, values, null, null) > 0) {
                    msg.obj = getString(R.string.operate_success);
                } else {
                    msg.obj = getString(R.string.operate_failure);
                }
                mUiHandler.sendMessage(msg);
            }
        }).start();
    }

    private boolean isLockMessage() {
        boolean locked = false;

        Cursor c = SqliteWrapper.query(MailBoxMessageContent.this, mContentResolver, mMessageUri,
                SMS_LOCK_PROJECTION, null, null, null);

        try {
            if (c != null && c.moveToFirst()) {
                locked = c.getInt(1) != 0;
            }
        } finally {
            if (c != null) c.close();
        }
        return locked;
    }

    private void getCurosrData(Cursor cursor) {
        if (cursor == null) {
            return;
        }

        mMsgThreadId = cursor.getLong(COLUMN_THREAD_ID);
        mMsgFrom = cursor.getString(COLUMN_SMS_ADDRESS);
        mMsgText = cursor.getString(COLUMN_SMS_BODY);
        mRead = cursor.getInt(COLUMN_SMS_READ);
        mMsgType = cursor.getInt(COLUMN_SMS_TYPE);
        mLock = cursor.getInt(COLUMN_SMS_LOCKED) != 0;
        mMsgstatus = cursor.getInt(COLUMN_STATUS);
        mSubID = cursor.getInt(COLUMN_SMS_SUBID);
        mMsgId = cursor.getInt(COLUMN_ID);
        if (MmsConfig.isRcsVersion()) {
            mRcsMsgState = cursor.getInt(COLUMN_RCS_MSG_STATE);
            mRcsChatType = cursor.getInt(COLUMN_RCS_CHAT_TYPE);
        }
    }

    private void startQuerySmsContent() {
        mMsgId = Integer.parseInt(mMessageUri.getLastPathSegment());
        mBackgroundHandler.startQuery(QUERY_MESSAGE_TOKEN, 0,
                Sms.CONTENT_URI,
                SMS_DETAIL_PROJECTION,
                getSwapSmsSetection(),
                null, "_id ASC");
    }

    private String getSwapSmsSetection() {
        String selection;
        if (mIsConvMode) {
            selection = Sms.THREAD_ID + "=" + mMsgThreadId;
        } else {
            selection = Sms.TYPE + "=" + mMailboxId;
        }
        return selection;
    }

    private void initUi() {
        setProgressBarIndeterminateVisibility(true);

        mScaleDetector = new ScaleGestureDetector(this, new MyScaleListener());

        Cursor cursor = moveCursorToCurrentMsg(mCursor, mMsgId);
        if (cursor != null) {
            mPagerAdapter = new MessageDetailAdapter(this, cursor);
            mPagerAdapter.setScaleTextList(mSlidePaperItemTextViews);
            mContentPager = (ViewPager) findViewById(R.id.details_view_pager);
            mContentPager.setAdapter(mPagerAdapter);
            mContentPager.setCurrentItem(cursor.getPosition());
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void updateUi() {
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

    private Cursor moveCursorToCurrentMsg(Cursor cursor, int id) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (id == cursor.getInt(COLUMN_ID)) {
                    return cursor;
                }
            } while (cursor.moveToNext());
        }
        return null;
    }

    private class DeleteMessageListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();

            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... none) {
                    mBackgroundHandler.startDelete(DELETE_MESSAGE_TOKEN, null, mMessageUri,
                            mLock ? null : "locked=0", null);
                    return null;
                }
            }.execute();
        }
    }

    private final class BackgroundHandler extends AsyncQueryHandler {
        public BackgroundHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case QUERY_MESSAGE_TOKEN:
                    if (cursor == null) {
                        Log.e(TAG, "onQueryComplete: cursor is null!");
                        return;
                    }

                    mCursor = cursor;
                    mCursor.moveToFirst();
                    initUi();

                    if (cursor != null && cursor.getCount() == 1) {
                        try {
                            if (cursor.moveToFirst()) {
                                getCurosrData(cursor);
                                if (mRead == 0) {
                                    MessageUtils.markAsRead(MailBoxMessageContent.this,
                                            ContentUris.withAppendedId(Sms.CONTENT_URI, mMsgId));
                                }
                                Message msg = Message.obtain();
                                msg.what = UPDATE_UI;
                                mUiHandler.sendMessage(msg);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Get sms content failed : " + e);
                        }
                    } else {
                        Log.e(TAG, "Can't find this SMS. URI: " + mMessageUri);
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown query token :" + token);
                    break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
                case DELETE_MESSAGE_TOKEN:
                    Message msg = Message.obtain();
                    msg.what = OPERATE_DEL_SINGLE_OVER;
                    msg.arg1 = result;
                    mUiHandler.sendMessage(msg);
                    break;
            }
        }
    }

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_UI:
                    updateUi();
                    break;
                case SHOW_TOAST:
                    String toastStr = (String) msg.obj;
                    Toast.makeText(MailBoxMessageContent.this, toastStr,
                            Toast.LENGTH_SHORT).show();
                    break;
                case OPERATE_DEL_SINGLE_OVER:
                    int result = msg.arg1;
                    if (result > 0) {
                        Toast.makeText(MailBoxMessageContent.this,
                                R.string.operate_success, Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(MailBoxMessageContent.this,
                                R.string.operate_failure, Toast.LENGTH_SHORT)
                                .show();
                    }
                    finish();
                default:
                    break;
            }
        }
    };

    private void startAsyncQuery() {
        try {
            mBackgroundHandler.startQuery(QUERY_MESSAGE_TOKEN,
                    0, mMessageUri, SMS_DETAIL_PROJECTION,
                    null, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Query sms content failed : " + e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        mMessageUri = intent.getData();

        // Cancel failed notification.
        MessageUtils.cancelFailedToDeliverNotification(intent, this);
        MessageUtils.cancelFailedDownloadNotification(intent, this);

        if (mMessageUri != null) {
            startAsyncQuery();
        } else {
            Log.e(TAG, "There's no sms uri!");
            finish();
        }
    }

    /* Begin add for RCS */
    public class ForwardClickListener implements OnClickListener{
        public void onClick(DialogInterface dialog, int whichButton) {
            switch (whichButton) {
                case FORWARD_INPUT_NUMBER:
                    inputNumberForwarMessage();
                    break;
                case FORWARD_CONVERSATION:
                    Intent intent = new Intent(MailBoxMessageContent.this,ConversationList.class);
                    intent.putExtra("select_conversation",true);
                    MessageUtils.setMailboxMode(false);
                    startActivityForResult(intent, REQUEST_SELECT_CONV);
                    break;
                default:
                    break;
            }
        }
    }

    private void inputNumberForwarMessage(){
        final EditText editText = new EditText(MailBoxMessageContent.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(lp);
        editText.setInputType(InputType.TYPE_CLASS_PHONE);
        editText.setHint(R.string.forward_input_number_hint);
        new AlertDialog.Builder(MailBoxMessageContent.this)
                .setTitle(R.string.forward_input_number_title).setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString();
                        if (TextUtils.isEmpty(input)) {
                            Toast.makeText(MailBoxMessageContent.this,
                                    R.string.forward_input_number_title,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            String[] numbers = input.split(";");
                            if (numbers != null && numbers.length > 0) {
                                ArrayList<String> numberList = new ArrayList<String>();
                                for (int i = 0; i < numbers.length; i++) {
                                    numberList.add(numbers[i]);
                                }
                                forwardRcsMessage(numberList);
                            }
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void forwardRcsMessage(ArrayList<String> numbers) {
        ContactList list = ContactList.getByNumbers(numbers, true);
        boolean success = false;
        try {
            success = RcsChatMessageUtils.sendRcsForwardMessage(
                    MailBoxMessageContent.this, numbers,
                    null, mMsgId);
            if (success) {
                Toast.makeText(MailBoxMessageContent.this,
                        R.string.forward_message_success,Toast.LENGTH_SHORT ).show();
            } else {
                Toast.makeText(MailBoxMessageContent.this,
                        R.string.forward_message_fail,Toast.LENGTH_SHORT ).show();
            }
        } catch (Exception e) {
            Toast.makeText(MailBoxMessageContent.this,
                    R.string.forward_message_fail,Toast.LENGTH_SHORT ).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        RcsLog.i("requestCode="+requestCode+",resultCode="+resultCode+",data="+data);
        if (resultCode != RESULT_OK){
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_RCS_PICK:
                RcsChatMessageUtils.sendRcsForwardMessage(
                        MailBoxMessageContent.this, null, data, mMsgId);
                break;
            case REQUEST_SELECT_CONV:
                RcsChatMessageUtils.sendRcsForwardMessage(
                        MailBoxMessageContent.this, null, data, mMsgId);
                break;
            case REQUEST_SELECT_GROUP:
                RcsChatMessageUtils.sendRcsForwardMessage(
                        MailBoxMessageContent.this, null, data, mMsgId);
                break;
            default:
                break;
        }
    }

    private boolean isRcsMessage() {
        return MmsConfig.isRcsVersion() && mRcsChatType > RcsUtils.RCS_CHAT_TYPE_DEFAULT
                && mRcsChatType < RcsUtils.RCS_CHAT_TYPE_PUBLIC_MESSAGE;
    }
    /* End add for RCS */

}
