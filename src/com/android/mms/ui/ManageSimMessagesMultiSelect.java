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

    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
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
    ArrayList<Integer> mSelectedPositions = new ArrayList<Integer>();
    ArrayList<String> mSelectedUris = new ArrayList<String>();
    
    private static final int MENU_DELETE_SELECT = 0;
    private static final int MENU_COPY_SELECT   = 1;    
       
    private int ACTION_NONE = 0;
    private int ACTION_COPY = 1;
    private int ACTION_DELETE = 2;
    private int mAction = ACTION_NONE;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sim_list);

        mMsgListView = (ListView) findViewById(R.id.messages);
        mMessage = (TextView) findViewById(R.id.empty_message);

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver); 

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        startMsgListQuery();
    }

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
                mSelectedPositions.add(position);
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
            confirmCopyDialog(l);
        }
    }

    private class MultiMessagesListener implements OnClickListener
    {
        public void onClick(DialogInterface dialog, int whichButton)
        {
            if(ACTION_DELETE == mAction)
            {
                deleteSelectedMessages();
            }
            else if(ACTION_COPY == mAction)
            {
                copySelectedMessages(); 
            }
        }
    }

    private void deleteSelectedMessages()
    {
        setTitle(getString(R.string.refreshing));
        setProgressBarIndeterminateVisibility(true);

        for (String uri : mSelectedUris)
        { 
            SqliteWrapper.delete(ManageSimMessagesMultiSelect.this, mContentResolver, Uri.parse(uri), null, null);
        }

        Toast.makeText(ManageSimMessagesMultiSelect.this, getString(R.string.operate_success),
            Toast.LENGTH_LONG).show();
        finish();
    }

    private void copySelectedMessages()
    {
        for (Integer position : mSelectedPositions)
        { 
            Cursor c = (Cursor) mMsgListAdapter.getItem(position);
            if (c == null)
            {
                return;
            }
            copyToPhoneMemory(c);
        }

        Toast.makeText(ManageSimMessagesMultiSelect.this, getString(R.string.operate_success),
            Toast.LENGTH_LONG).show();
        finish();
    }

    private String getUriStrByCursor(Cursor cursor)
    {
        String messageIndexString =
                cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        Uri simUri = ICC_URI.buildUpon().appendPath(messageIndexString).build();

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
    
    private void copyToPhoneMemory(Cursor cursor) {
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
                cursor.getColumnIndexOrThrow("status"));

        return (messageStatus == SmsManager.STATUS_ON_ICC_READ) ||
               (messageStatus == SmsManager.STATUS_ON_ICC_UNREAD);
    }

    private void startMsgListQuery() {
        try {
            mMsgListView.setVisibility(View.GONE);
            mMessage.setVisibility(View.GONE);
            setTitle(getString(R.string.refreshing));
            setProgressBarIndeterminateVisibility(true);
            mBackgroundQueryHandler.startQuery(0, null, ICC_URI, null, null, null, null);
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
        menu.add(0, MENU_COPY_SELECT,  0, R.string.menu_copy_to)
            .setIcon(R.drawable.ic_menu_copy)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);  
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
