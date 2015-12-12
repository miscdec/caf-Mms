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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.LogTag;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.R;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.MultiPickContactGroups;
import com.suntek.mway.rcs.client.aidl.constant.Constants.FavoriteMessageProvider;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.message.MessageApi;




public class FavoriteDetailActivity  extends Activity {
    private static final String TAG = "FavoriteDetailActivity";
    private final static String MESSAGE_ID = "message_id";
    private Uri mMessageUri;
    private long mMsgId;
    private String mMsgText;// Text of message
    private String mMsgFrom;
    private int mRcsChatType;
    // RCS Message API
    private MessageApi mMessageApi = MessageApi.getInstance();

    private Cursor mCursor = null;

    private ViewPager mContentPager;
    private FavoriteDetailAdapter mPagerAdapter;
    /*Operations for gesture to scale the current text fontsize of content*/
    private float mScaleFactor = 1;
    private  ScaleGestureDetector mScaleDetector;

    private static final int MENU_FORWARD = Menu.FIRST;
    private static final int MENU_SAVE_TO_CONTACT = Menu.FIRST + 1;
    private static final int MENU_UNFAVORITED = Menu.FIRST + 2;

    private BackgroundHandler mBackgroundHandler;
    private static final int DELETE_MESSAGE_TOKEN = 6701;
    private static final int QUERY_MESSAGE_TOKEN = 6702;

    private static final int FORWARD_INPUT_NUMBER = 0;
    private static final int FORWARD_CONTACTS = 1;
    private static final int FORWARD_CONVERSATION = 2;
    private static final int FORWARD_GROUP = 3;

    public static final int REQUEST_CODE_RCS_PICK = 115;
    public static final int REQUEST_SELECT_CONV = 116;
    public static final int REQUEST_SELECT_GROUP = 117;
    private static final String INTENT_MULTI_PICK = "com.android.contacts.action.MULTI_PICK";

    private ContentResolver mContentResolver;

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
        if (RcsDualSimMananger.getUserIsUseRcsPolicy(FavoriteDetailActivity.this)) {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);
        }
        if (!Contact.get(mMsgFrom, false).existsInDatabase()) {
            menu.add(0, MENU_SAVE_TO_CONTACT, 0, R.string.menu_add_to_contacts);
        }
        menu.add(0, MENU_UNFAVORITED, 0, R.string.unfavorited);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FORWARD:
                if (isRcsMessage()) {
                    RcsChatMessageUtils.forwardContactOrConversation(this,
                            new ForwardClickListener());
                } else {
                    Intent intentForward = new Intent(this, ComposeMessageActivity.class);
                    intentForward.putExtra("sms_body", mMsgText);
                    intentForward.putExtra("exit_on_sent", true);
                    intentForward.putExtra("forwarded_message", true);
                    this.startActivity(intentForward);
                }
                break;
            case MENU_SAVE_TO_CONTACT:
                saveToContact();
                break;
            case MENU_UNFAVORITED:
                RcsChatMessageUtils.favoritedOneMessage(this, mMsgId, isRcsMessage(), false);
                finish();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }

        return true;
    }

    public class ForwardClickListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            switch (whichButton) {
                case FORWARD_INPUT_NUMBER:
                    inputNumberForwarMessage();
                    break;
                case FORWARD_CONTACTS:
                    launchRcsPhonePicker();
                    break;
                case FORWARD_CONVERSATION:
                    Intent intent = new Intent(FavoriteDetailActivity.this,ConversationList.class);
                    intent.putExtra("select_conversation",true);
                    MessageUtils.setMailboxMode(false);
                    startActivityForResult(intent, REQUEST_SELECT_CONV);
                    break;
                case FORWARD_GROUP:
                    launchRcsContactGroupPicker(REQUEST_SELECT_GROUP);
                    break;
                default:
                    break;
            }
        }
    }

    private void inputNumberForwarMessage(){
        final EditText editText = new EditText(FavoriteDetailActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(lp);
        editText.setInputType(InputType.TYPE_CLASS_PHONE);
        editText.setHint(R.string.forward_input_number_hint);
        new AlertDialog.Builder(FavoriteDetailActivity.this)
        .setTitle(R.string.forward_input_number_title)
        .setView(editText)
        .setPositiveButton(android.R.string.ok,  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String input = editText.getText().toString();
                if (TextUtils.isEmpty(input)) {
                    Toast.makeText(FavoriteDetailActivity.this, R.string.forward_input_number_title,
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
        }).setNegativeButton(android.R.string.cancel, null)
        .show();
    }

    private void forwardRcsMessage(ArrayList<String> numbers) {
        ContactList list = ContactList.getByNumbers(numbers, true);
        boolean success = false;
        try {
            success = RcsChatMessageUtils.sendRcsForwardMessage(
                    FavoriteDetailActivity.this, numbers,
                    null, mMsgId);
            if (success) {
                Toast.makeText(FavoriteDetailActivity.this,
                        R.string.forward_message_success,Toast.LENGTH_SHORT ).show();
            } else {
                Toast.makeText(FavoriteDetailActivity.this,
                        R.string.forward_message_fail,Toast.LENGTH_SHORT ).show();
            }
        } catch (Exception e) {
            Toast.makeText(FavoriteDetailActivity.this,
                    R.string.forward_message_fail,Toast.LENGTH_SHORT ).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("RCS_UI","requestCode="+requestCode+",resultCode="+resultCode+",data="+data);
        if (resultCode != RESULT_OK){
            return;
        }
        boolean success = false;
        switch (requestCode) {
            case REQUEST_CODE_RCS_PICK:
            case REQUEST_SELECT_GROUP:
                if (data != null) {
                    Bundle bundle = data.getExtras().getBundle("result");
                    final Set<String> keySet = bundle.keySet();
                    final int recipientCount = (keySet != null) ? keySet.size() : 0;
                    final ContactList list;
                    list = ContactList.blockingGetByUris(buildUris(keySet, recipientCount));
                    String[] numbers = list.getNumbers(false);
                    success = RcsChatMessageUtils.sendRcsForwardMessage(
                            FavoriteDetailActivity.this, Arrays.asList(list.getNumbers()), null,
                            mMsgId);
                    if (success) {
                        toast(R.string.forward_message_success);
                    } else {
                        toast(R.string.forward_message_fail);
                    }
                }
                break;
            case REQUEST_SELECT_CONV:
                success = RcsChatMessageUtils.sendRcsForwardMessage(
                        FavoriteDetailActivity.this, null, data, mMsgId);
                if (success) {
                    toast(R.string.forward_message_success);
                } else {
                    toast(R.string.forward_message_fail);
                }
                break;
            default:
                break;
        }
    }


    private void launchRcsPhonePicker() {
        Intent intent = new Intent(INTENT_MULTI_PICK, Contacts.CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_CODE_RCS_PICK);
        } catch (Exception ex) {
            Toast.makeText(this, R.string.contact_app_not_found, Toast.LENGTH_SHORT).show();
        }
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
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        this.startActivity(intent);
    }



    private void getCurosrData(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        mMsgFrom = cursor.getString(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.NUMBER));
        mMsgText = cursor.getString(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.CONTENT));
        mMsgId = cursor.getLong(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.MSG_ID));
        mRcsChatType = cursor.getInt(cursor.getColumnIndex(
                FavoriteMessageProvider.FavoriteMessage.CHAT_TYPE));
    }

    private void startQuerySmsContent() {
        mMessageUri = getIntent().getData();
        mMsgId = getIntent().getLongExtra(MESSAGE_ID, -1);
        mBackgroundHandler.startQuery(QUERY_MESSAGE_TOKEN, 0,
                mMessageUri, null, "msg_id = " + mMsgId, null, "_id ASC");
    }

    private void initUi() {
        setProgressBarIndeterminateVisibility(true);

        mScaleDetector = new ScaleGestureDetector(this, new MyScaleListener());

        if (mCursor != null && mCursor.moveToFirst()) {
            mPagerAdapter = new FavoriteDetailAdapter(this, mCursor);
            mPagerAdapter.setScaleTextList(mSlidePaperItemTextViews);
            mContentPager = (ViewPager) findViewById(R.id.details_view_pager);
            mContentPager.setAdapter(mPagerAdapter);
            mContentPager.setCurrentItem(mCursor.getPosition());
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void updateUi() {
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
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
                    initUi();

                    if (cursor != null && cursor.getCount() == 1) {
                        try {
                            if (cursor.moveToFirst()) {
                                getCurosrData(cursor);
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
    }

    private boolean isRcsMessage() {
        return mRcsChatType > RcsUtils.RCS_CHAT_TYPE_DEFAULT
                && mRcsChatType < RcsUtils.RCS_CHAT_TYPE_PUBLIC_MESSAGE;
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

    private Uri[] buildUris(final Set<String> keySet, final int newPickRecipientsCount) {
        Uri[] newUris = new Uri[newPickRecipientsCount];
        Iterator<String> it = keySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            String id = it.next();
            newUris[i++] = ContentUris.withAppendedId(Phone.CONTENT_URI, Integer.parseInt(id));
            if (i == newPickRecipientsCount) {
                break;
            }
        }
        return newUris;
    }

    private void launchRcsContactGroupPicker(int requestCode) {
        Intent intent = new Intent(this, MultiPickContactGroups.class);
        startActivityForResult(intent, requestCode);
    }
}
