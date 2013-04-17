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
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ActionMode;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.graphics.drawable.Drawable;
import android.app.ActionBar;
import java.util.HashSet;
import java.util.regex.Pattern;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.LogTag;
import android.widget.ListView;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.app.Activity;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.os.PowerManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import android.text.format.Time;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.util.SparseBooleanArray;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.widget.ImageView;
import android.view.Window;
import android.content.res.Configuration;
import android.telephony.TelephonyManager;

/**
 * Displays a list of the SMS messages stored on the ICC.
 */

public class ManageSimMessagesMultiSelect extends Activity
{
    private static final String TAG = "ManageSimMessagesMultiSelect";

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int SHOW_SUCCESS_TOAST_TIMER = 2*1000;
    private Cursor mCursor;
    private ListView mMsgListView;
    private TextView mMessage;
    public MessageListAdapter mMsgListAdapter; 
    private ContentResolver mContentResolver;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private AsyncQueryHandler mQueryHandler = null;
    private TextView mSelectedConvCount;
    private ImageView mSelectedAll;
    private boolean mHasSelectAll = false;    
    PowerManager.WakeLock mWakeLock;
    private int mSubscription; // add for DSDS
    private Uri mIccUri;
    private ProgressDialog mProgressDialog = null;
    private OperateThread mOperateThread = null;
    private boolean mShowSuccessToast = true;
    ArrayList<Cursor> mSelectedCursors = new ArrayList<Cursor>();
    ArrayList<String> mSelectedUris = new ArrayList<String>();
    
    private static final int MENU_DELETE_SELECT = 0;
    private static final int MENU_COPY_SELECT   = 1;    
    private static final int SUB_INVALID = -1;
    private static final int SUB1 = 0;
    private static final int SUB2 = 1;  
    private int ACTION_NONE = 0;
    private int ACTION_COPY = 1;
    private int ACTION_DELETE = 2;
    private static final int SHOW_TOAST = 1;
    
    private int mAction = ACTION_NONE;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sim_list);
        
        mSubscription = getIntent().getIntExtra(MessageUtils.SUB_KEY, SUB_INVALID);
        mIccUri = MessageUtils.getIccUriBySubscription(mSubscription);

        mMsgListView = (ListView) findViewById(R.id.messages);
        mMessage = (TextView) findViewById(R.id.empty_message);

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver); 

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        startMsgListQuery();
    }

    private Handler uihandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case SHOW_TOAST:
                {                
                    String toastStr = (String) msg.obj;
                    Toast.makeText(ManageSimMessagesMultiSelect.this, toastStr, 
                                    Toast.LENGTH_LONG).show();
                    
                    //Update the notification for text message memory may not be full, add for cmcc test
                    MessageUtils.checkIsPhoneMessageFull(ManageSimMessagesMultiSelect.this);
                    clearSelect();
                    
                    if (mProgressDialog != null)
                    {
                        mProgressDialog.dismiss();
                    }
                    
                    finish();
                    break; 
                }
                default:
                    break;
            }
        }
    };
    

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mCursor != null)
        {
            mCursor.close();
        }
    }    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setupActionBarMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
        case MENU_DELETE_SELECT:
            mAction = ACTION_DELETE;
            confirmMultiAction();
            break;
        case MENU_COPY_SELECT:
            mAction = ACTION_COPY;
            confirmMultiAction();
            break;
        case android.R.id.home:
            finish();
            break;
        default:
            return true;
        }

        return true;
    }

    private void confirmMultiAction()
    {
        SparseBooleanArray booleanArray = mMsgListView.getCheckedItemPositions();
        int size = booleanArray.size();

        if(size > 0)
        {
            for (int j = 0; j < size; j++)
            {
                int position = booleanArray.keyAt(j);
                if (!mMsgListView.isItemChecked(position))
                {
                    continue;
                }
                Cursor c = (Cursor) mMsgListAdapter.getItem(position);
                if (c == null)
                {
                    return;
                }
                mSelectedCursors.add(c);
                mSelectedUris.add(getUriStrByCursor(c)); 
            }
        }

        MultiMessagesListener l = new MultiMessagesListener();
        if(ACTION_DELETE == mAction)
        {
            confirmDeleteDialog(l);
        }
        else if(ACTION_COPY == mAction)
        {
            if(MessageUtils.isMultiSimEnabledMms())
            {
                if(MessageUtils.getActivatedIccCardCount() > 1)
                {
                    showCopySelectDialog();
                }
                else
                {
                    confirmCopyDialog(l);                     
                }                  
            }
            else
            {
                confirmCopyDialog(l);
            }          
        }
    }

    private void showCopySelectDialog(){
        String targetCard = mSubscription == SUB1 ? MessageUtils.getMultiSimName(this, SUB2) : MessageUtils.getMultiSimName(this, SUB1);
        String[] items = new String[] {getString(R.string.view_setting_phone), targetCard};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_copy_to));
        builder.setCancelable(true);
        builder.setItems(items, new DialogInterface.OnClickListener()
        {
            public final void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                     confirmCopyDialog(new MultiMessagesListener(true));  // copy to phone memory 
                }
                else
                {
                     confirmCopyDialog(new MultiMessagesListener(false));  //copy to the other SIM card
                }
                dialog.dismiss();
            }
        });
        builder.show();    
    }
        
    private class MultiMessagesListener implements OnClickListener
    {
        private boolean mIsCopyToPhone = true;

        public MultiMessagesListener() 
        {
            //do nothing
        }
        
        public MultiMessagesListener(boolean isCopyToPhone) 
        {
            mIsCopyToPhone = isCopyToPhone;
        }
                
        public void onClick(DialogInterface dialog, int whichButton)
        {
            if(ACTION_DELETE == mAction)
            {
                deleteSelectedMessages();
            }
            else if(ACTION_COPY == mAction)
            {
                copySelectedMessages(mIsCopyToPhone); 
            }
        }
    }

    private void deleteSelectedMessages()
    {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.deleting_title);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.show();
        if (mOperateThread == null)
        {
            mOperateThread = new OperateThread();
        }
        Thread thread = new Thread(mOperateThread);
        thread.setPriority(Thread.MAX_PRIORITY);            
        thread.start();
        return;
    }

    private void deleteMessages()
    {        
        for (String uri : mSelectedUris)
        { 
            SqliteWrapper.delete(ManageSimMessagesMultiSelect.this, mContentResolver, Uri.parse(uri), null, null);
        }

        Message msg = Message.obtain();
        msg.what = SHOW_TOAST;
        msg.obj = getString(R.string.operate_success);   
        uihandler.sendMessage(msg);
    }
    
    private void copySelectedMessages(boolean isCopyToPhone)
    {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.copying_title);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.show();
        if (mOperateThread == null)
        {
            mOperateThread = new OperateThread(isCopyToPhone);
        }        
        Thread thread = new Thread(mOperateThread);
        thread.start();
    }

    private void copyMessages(boolean isCopyToPhone)
    {
        SparseBooleanArray booleanArray = mMsgListView.getCheckedItemPositions();
        int size = booleanArray.size();

        if(size > 0)
        {
            for (int j = 0; j < size; j++)
            {
                int position = booleanArray.keyAt(j);
                if (!mMsgListView.isItemChecked(position))
                {
                    continue;
                }
                Cursor c = (Cursor) mMsgListAdapter.getItem(position);
                if (c == null)
                {
                    return;
                }
                
                if(isCopyToPhone)
                {
                    copyToPhoneMemory(c);       
                }
                else
                {
                    copyToCard(c, mSubscription == SUB1 ? SUB2 : SUB1);
                } 
                
                if(!mShowSuccessToast)
                {
                    break;
                }  

            }
        }

        if(mShowSuccessToast)
        {
            Message msg = Message.obtain();
            msg.what = SHOW_TOAST;
            msg.obj = getString(R.string.operate_success);
            uihandler.sendMessage(msg);
        }
    }
    
    private String getUriStrByCursor(Cursor cursor)
    {
        String messageIndexString =
                cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        Uri simUri = mIccUri.buildUpon().appendPath(messageIndexString).build();

        return simUri.toString();
    }

    private void confirmDeleteDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.setMessage(R.string.confirm_delete_selected_messages);
        builder.show();
    }

    private void confirmCopyDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_copy_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.setMessage(R.string.confirm_copy_selected_messages);
        builder.show();
    }

    private void copyToCard(Cursor cursor, int subscription)
    {
        final String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        final String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        final Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        final int boxId = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
        
        SmsManager smsManager = SmsManager.getDefault();
        final ArrayList<String> messages = smsManager.divideMessage(body);
        final int messageCount = messages.size();

        Message msg = Message.obtain();
        msg.what = SHOW_TOAST;
        msg.obj = getString(R.string.operate_success);

        for (int j = 0; j < messageCount; j++)
        {
            String content = messages.get(j);
            if (TextUtils.isEmpty(content))
            {
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.e(TAG, "copyToCard : Copy error for empty content!");
                }
                continue;
            }
            ContentValues values = new ContentValues(6);
            values.put(Sms.DATE, date);
            values.put(Sms.BODY, content);
            values.put(Sms.TYPE, boxId);
            values.put(Sms.ADDRESS, address);
            values.put(Sms.READ, MessageUtils.MESSAGE_READ);
            values.put(Sms.SUB_ID, subscription);  // -1 for MessageUtils.SUB_INVALID , 0 for MessageUtils.SUB1, 1 for MessageUtils.SUB2                 
            Uri uriStr = MessageUtils.getIccUriBySubscription(subscription);
            
            Uri retUri = SqliteWrapper.insert(ManageSimMessagesMultiSelect.this, getContentResolver(),
                                              uriStr, values);
            if (uriStr != null && retUri != null) {
                Log.e(TAG, "copyToCard : uriStr = " + uriStr.toString() 
                    + ", retUri = " + retUri.toString());
            }
            
            if (retUri == null)
            {
                msg.obj = getString(R.string.operate_failure);
                uihandler.sendMessage(msg);
                mShowSuccessToast = false;
                break;
            }
            else if (MessageUtils.COPY_SUCCESS_FULL.equals(retUri.toString()))
            {
                msg.obj = getString(R.string.copy_success_full);
                uihandler.sendMessage(msg);
                mShowSuccessToast = false;
                break;
            }
            else if (MessageUtils.COPY_FAILURE_FULL.equals(retUri.toString()))
            {
                msg.obj = getString(R.string.copy_failure_full);
                uihandler.sendMessage(msg);
                mShowSuccessToast = false;
                break;
            }
        }
    }
    
    private void copyToPhoneMemory(Cursor cursor) {
        if(MessageUtils.isSmsMessageJustFull(this))
        {
            Message msg = Message.obtain();
            msg.what = SHOW_TOAST;
            msg.obj = getString(R.string.exceed_message_size_limitation);
            uihandler.sendMessage(msg);
            mShowSuccessToast = false;
            return;
        }
        String address = cursor.getString(
                cursor.getColumnIndexOrThrow("address"));
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));

        try {
            if (isIncomingMessage(cursor)) {
                Sms.Inbox.addMessage(mContentResolver, address, body, null, date, true /* read */);
            } else {
                Sms.Sent.addMessage(mContentResolver, address, body, null, date);
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private boolean isIncomingMessage(Cursor cursor) {
        int messageStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow("status_on_icc"));
        
        return (messageStatus == SmsManager.STATUS_ON_ICC_READ) ||
               (messageStatus == SmsManager.STATUS_ON_ICC_UNREAD);
    }

    private void startMsgListQuery() {
        try {
            mMsgListView.setVisibility(View.GONE);
            mMessage.setVisibility(View.GONE);
            setTitle(getString(R.string.refreshing));
            setProgressBarIndeterminateVisibility(true);
            mBackgroundQueryHandler.startQuery(0, null, mIccUri, null, null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void setupActionBarMenu(Menu menu)
    {
        menu.clear();
    
        menu.add(0, MENU_DELETE_SELECT,  0, R.string.delete)
            .setIcon(R.drawable.ic_menu_trash_holo_dark)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        if(MessageUtils.isMultiSimEnabledMms())
        {
            menu.add(0, MENU_COPY_SELECT,  0, R.string.menu_copy_to)
                .setIcon(R.drawable.ic_menu_copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); 

        }
        else
        {
            menu.add(0, MENU_COPY_SELECT,  0, R.string.sim_copy_to_phone_memory)
            .setIcon(R.drawable.ic_menu_copy)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); 
        }
 
    }

    private void setupActionBar()
    {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                     | ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setTitle("");
            actionBar.setCustomView(R.layout.conversation_list_multi_select_actionbar);
            mSelectedConvCount = (TextView)findViewById(R.id.selected_conv_count);
            mSelectedConvCount.setText("0");
            mSelectedAll = (ImageView)findViewById(R.id.selecte_all);
            mSelectedAll.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if(mHasSelectAll)
                    {
                        clearSelect();
                        mHasSelectAll = false;
                        mSelectedAll.setImageResource(R.drawable.ic_menu_select_all);
                    }
                    else
                    {
                        allSelect();
                        mHasSelectAll = true;
                        mSelectedAll.setImageResource(R.drawable.ic_menu_unselect_all);
                    }
                }
            });                
            ((TextView)findViewById(R.id.title)).setText(R.string.select_messages);        
        }
    }

    private void clearSelect()
    {
        mMsgListView.clearChoices();
        final int checkedCount = mMsgListView.getCheckedItemCount();
        mSelectedConvCount.setText(Integer.toString(checkedCount));        
        mMsgListView.invalidateViews();                        
    }

    private void allSelect()
    {
        int count = mMsgListAdapter.getCount();
        for (int i = 0; i < count; i++)
        {
            mMsgListView.setItemChecked(i, true);
        }
        final int checkedCount = mMsgListView.getCheckedItemCount();
        mSelectedConvCount.setText(Integer.toString(checkedCount));        
        mMsgListView.invalidateViews();                        
    }

    private void disenableSleep(Context context) {
        if (mWakeLock == null) {
            PowerManager pm =
                (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MmsOperating");
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }

    private void enalbeSleep() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    private int deleteMsgByUri(String deleteString)
    {
        Uri deleteUri = Uri.parse(deleteString);
        return SqliteWrapper.delete(this, getContentResolver(),
                                    deleteUri, null, null);
    }  

    private class OperateThread extends Thread
    {
        boolean mIsCopyToPhone;

        public OperateThread()
        {
            super("OperateThread");
        }
        
        public OperateThread(boolean isCopyToPhone)
        {
            mIsCopyToPhone = isCopyToPhone;
        }

        public void run()
        {
            if (ACTION_DELETE == mAction)
            {
                deleteMessages();             
            }
            else if (ACTION_COPY == mAction)
            {
                //disenableSleep(ManageSimMessagesMultiSelect.this);                
                copyMessages(mIsCopyToPhone);                
                //enalbeSleep();   
            }
        }
    }
    
    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            setProgressBarVisibility(false);
            mCursor = cursor;

            if (mCursor != null) {
                if (!mCursor.moveToFirst()) {
                    // Let user know the SIM is empty
                    finish();
                } else if (mMsgListAdapter == null) {
                    // Note that the MessageListAdapter doesn't support auto-requeries. If we
                    // want to respond to changes we'd need to add a line like:
                    //   mListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
                    // See ComposeMessageActivity for an example.
                    mMsgListAdapter = new MessageListAdapter(
                            ManageSimMessagesMultiSelect.this, mCursor, mMsgListView, false, null, true);
                    mMsgListView.setAdapter(mMsgListAdapter);
                    mMsgListView.setVisibility(View.VISIBLE);
                    mMessage.setVisibility(View.GONE);
                    setupActionBar();
                    mMsgListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);               
                    mMsgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mMsgListView.invalidateViews();
                            final int checkedCount = mMsgListView.getCheckedItemCount();
                            mSelectedConvCount.setText(Integer.toString(checkedCount));
                            long[] ids = mMsgListView.getCheckedItemIds();
                            for(int index = 0; index < ids.length;index++)
                            {
                                Log.d(TAG,"ids" +index+" = " + ids[index]);
                            }
                        }
                    }); 
                    mMsgListView.requestFocus();  
                    setProgressBarIndeterminateVisibility(false);
                } else {
                    mMsgListAdapter.changeCursor(mCursor);
                }
                startManagingCursor(mCursor);
            } else {
                // Let user know the SIM is empty
                finish();
            }

            return;
        }
    }    
}
