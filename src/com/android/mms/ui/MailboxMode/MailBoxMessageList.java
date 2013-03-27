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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.android.mms.LogTag;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.view.Window;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.content.ContentValues;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.view.KeyEvent;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.telephony.SmsManager;
import android.provider.Telephony.Threads;
import java.util.HashMap;
import com.google.android.mms.pdu.PduComposer;
import java.io.IOException;
import java.util.ArrayList;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.android.mms.LogTag;
import android.view.Gravity;

import static com.android.mms.ui.MessageListAdapter.MAILBOX_PROJECTION;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_THREAD_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_ADDRESS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_BODY;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SUB_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_STATUS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE_SENT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_SUBJECT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_BOX;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_DELIVERY_REPORT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import static com.android.mms.transaction.TransactionService.*;
import android.content.DialogInterface;

import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.util.SparseBooleanArray;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.google.android.mms.MmsException;
import android.content.ContentUris;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.EncodedStringValue;
import com.android.mms.ui.MessageListAdapter;
import android.provider.Telephony.Threads;
import com.google.android.mms.pdu.PduPersister;
import android.content.DialogInterface.OnClickListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.PowerManager;
import java.util.HashSet;
import java.util.Arrays;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.DeliveryReportActivity;
import com.google.android.mms.pdu.PduHeaders;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.content.Context;
import android.widget.Button;
import com.android.mms.util.SmileyParser;
import com.android.mms.transaction.Transaction;
import com.google.android.mms.pdu.NotificationInd;
import com.android.mms.transaction.TransactionService;
import com.android.mms.transaction.TransactionBundle;
import com.google.android.mms.MmsException;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.IBinder;
import com.android.mms.transaction.TransactionState;
import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.util.AddressUtils;
import com.android.mms.data.Conversation;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import android.text.format.Time;
import android.widget.Toast;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.telephony.SmsMessage;
import com.android.mms.model.SlideshowModel;
import android.widget.AbsListView;
import com.android.mms.data.Contact;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.RetrieveConf;
import java.sql.Timestamp;
import com.android.mms.MmsConfig;
import android.view.ActionMode;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.graphics.drawable.Drawable;
import android.app.ActionBar;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.content.res.Configuration;
import android.widget.CheckBox;
import com.android.mms.MmsApp;


/**
 * This activity provides a list view of MailBox-Mode.
 */
public class MailBoxMessageList extends ListActivity
    implements MailBoxMessageListAdapter.OnListContentChangedListener,
    MailBoxMessageListAdapter.ActivityCanPaused
{
    private Cursor mCursor;
    private static final String TAG = "MailBoxMessageList";
    private BoxMsgListQueryHandler mQueryHandler;
    private static String MAILBOX_URI ="content://mms-sms/mailbox/";
    private String mMailboxUri;
    
    private static final int MESSAGE_LIST_QUERY_TOKEN             = 9001;

    // IDs of the spinner items for the box type.
    public static final int TYPE_INBOX    = 1;
    public static final int TYPE_SENTBOX  = 2;
    public static final int TYPE_OUTBOX   = 4;
    public static final int TYPE_DRAFTBOX = 3;
    // IDs of the spinner items for the slot type in DSDS 
    public static final int TYPE_ALL_SLOT    = 0;
    public static final int TYPE_SLOT_ONE   = 1;
    public static final int TYPE_SLOT_TWO = 2;

    private boolean mHasPause = false;
    private boolean mHasQueryOver = true;            
    private int mQueryBoxType = 1;
    private int mQuerySlotType = 0;
    private int mMailboxId;      
    private String mSmsWhereDelete = "";
    private String mMmsWhereDelete = "";    
    private boolean mHasLocked = false;
    
    private MailBoxMessageListAdapter mListAdapter = null;
    private final Object mCursorLock = new Object();
    private ListView mListView;
    private TextView mCountTextView;
    private View mSpinners;
    private Spinner boxSpinner = null;
    private Spinner slotSpinner = null;
    private ModeCallback mModeCallback = null;
    // mark whether comes into MultiChoiceMode or not.
    private boolean mMultiChoiceMode = false;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private CharSequence mQueryText;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());

        String box = Integer.toString(mMailboxId);
        mQueryHandler = new BoxMsgListQueryHandler(getContentResolver());
        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.mailbox_list_screen);
        mSpinners = (View) findViewById(R.id.spinners);
        boxSpinner = (Spinner) findViewById(R.id.box_spinner);
        slotSpinner = (Spinner) findViewById(R.id.slot_spinner);
        initSpinner();

        mListView = getListView(); 
        getListView().setItemsCanFocus(true);
        mModeCallback = new ModeCallback();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(mModeCallback);
        
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true); 
        setupActionBar();
    }
    
    public boolean onSearchRequested() {
        // if comes into multiChoiceMode,do not continue to enter search mode ;
        if (mSearchItem != null && !mMultiChoiceMode) {
            mSearchItem.expandActionView();
        }
        return true;
    }     

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.
        closeContextMenu(); 
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.d(TAG,"+++++++++++onListItemClick+++++++++++++");               
        }

        if (!mHasQueryOver)
        {
            return;
        }
        Cursor c = (Cursor) l.getAdapter().getItem(position);
        if (c == null)
        {
            return;
        }
        showMessageContent(c);
    }

    private void showMessageContent(Cursor c)
    {
        final String type = c.getString(COLUMN_MSG_TYPE);
        long msgId = c.getLong(COLUMN_ID);

        if (type.equals("mms"))
        {
            int mmsType = c.getInt(MessageListAdapter.COLUMN_MMS_MESSAGE_BOX);
            String ct = com.google.android.mms.ContentType.MULTIPART_MIXED;
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {                  
                Log.v(TAG,"showMessageContent : contenttype = " + ct);
            }

            if ( null != ct 
                && ct.equals(com.google.android.mms.ContentType.MULTIPART_MIXED)
                && Mms.MESSAGE_BOX_DRAFTS != mmsType)
            {
                int read = c.getInt(MessageListAdapter.COLUMN_MMS_READ);
                Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);

                Intent in = new Intent(this, SlideshowActivity.class);        
                String report = c.getString(COLUMN_MMS_DELIVERY_REPORT);
                boolean readReport = false;
                if ((report == null)) {
                    readReport = false;
                } else {
                    int reportInt;
                    try {
                        reportInt = Integer.parseInt(report);
                        readReport = (reportInt == PduHeaders.VALUE_YES);
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value for read report was invalid.");
                        readReport = false;
                    }
                }                
                in.putExtra("mms_report", readReport);
                in.putExtra("msg_id", msgId);
                in.putExtra("show", true);
                in.setData(uri);

                startActivity(in);
                return;
            }
            return;
        }
        else if (!type.equals("sms"))
        {
            return;
        }

        Intent i = new Intent(this, MailBoxMessageContent.class);
        String body = c.getString(COLUMN_SMS_BODY);
        int subID = c.getInt(COLUMN_SUB_ID);
        String fromto = "";
        String displayName;
        String fromtoLabel;
        String sendLabel;
        long person_id = -1;

        // Set Time Stamp
        Long date = c.getLong(COLUMN_SMS_DATE);
        Long dateSent = c.getLong(COLUMN_SMS_DATE_SENT);
        String dateStr = MessageUtils.formatTimeStampString(this, date, true);
        
        String msgid = c.getString(COLUMN_ID);
        String msgUriStr = "content://" + type + "/" + msgid;
        long threadId = c.getLong(COLUMN_THREAD_ID);
        int smsType = mCursor.getInt(COLUMN_SMS_TYPE);

        String addr = "";
        addr = c.getString(COLUMN_SMS_ADDRESS);
        displayName = Contact.get(addr, true).getName(); 
        fromto = addr;
        int smsLocked = c.getInt(COLUMN_SMS_LOCKED);            
        
        if (smsType == Sms.MESSAGE_TYPE_DRAFT)
        {
            Intent intent = new Intent(this, ComposeMessageActivity.class);
            //intent.putExtra("thread_id", threadId);
            intent.putExtra("sms_body", body);
            intent.putExtra("address", fromto);
            intent.putExtra("edit_draft", true);
            intent.putExtra("sms_id", msgId);
            intent.putExtra("sms_locked", smsLocked);            
            intent.putExtra("mailboxId", Sms.MESSAGE_TYPE_DRAFT);
            startActivity(intent);
            return;
        }        
        else if (smsType == Sms.MESSAGE_TYPE_INBOX)
        {
            fromtoLabel = this.getString(R.string.from_label);
            sendLabel = this.getString(R.string.received_label);
        }
        else
        {
            fromtoLabel = this.getString(R.string.to_address_label);
            sendLabel = this.getString(R.string.sent_label);
        }

        Uri msgUri = Uri.parse(msgUriStr);
        int smstatus = c.getInt(COLUMN_SMS_STATUS);
        int msgRead = c.getInt(COLUMN_SMS_READ);

        i.putExtra("sms_datelongformat", date);
        i.putExtra("sms_datesentlongformat", dateSent);
        i.putExtra("sms_body", body);
        i.putExtra("sms_fromto", fromto);
        i.putExtra("sms_displayname", displayName);
        i.putExtra("sms_fromtolabel", fromtoLabel);
        i.putExtra("sms_sendlabel", sendLabel);
        i.putExtra("sms_date", dateStr);
        i.putExtra("msg_uri", msgUri);
        i.putExtra("sms_threadid", threadId);
        i.putExtra("sms_status", smstatus);
        i.putExtra("sms_read", msgRead);
        i.putExtra("mailboxId", smsType);
        i.putExtra("sms_personId", person_id);
        i.putExtra("sms_id", c.getInt(COLUMN_ID));
        i.putExtra("sms_uri_str", msgUriStr);
        i.putExtra("sms_on_uim", false);
        i.putExtra("sms_type", smsType);       
        i.putExtra("sms_locked", smsLocked);            
        i.putExtra("sms_subid", subID);
        ArrayList<String> allIdList = getAllMsgId();
        i.putStringArrayListExtra("sms_id_list", allIdList);
        startActivity(i);
    }

    private ArrayList<String> getAllMsgId()
    {
        ArrayList<String> ids = new ArrayList<String>();
        int size = mListAdapter.getCount();
        for (int j = 0; j < size; j++)
        {
            Cursor c = (Cursor) mListAdapter.getItem(j);
            if (c != null)
            {
                ids.add(getUriStrByCursor(c));
            }
        }
        return ids;
    }

    private String getUriStrByCursor(Cursor c)
    {
        if (c == null)
        {
            return "";
        }
        String msgid = c.getString(COLUMN_ID);
        String msgtype = c.getString(COLUMN_MSG_TYPE);
        String uriString = "content://" + msgtype + "/" + msgid;

        return uriString;
    }
    private void handleIntent(Intent intent)
    {
        String subject = intent.getStringExtra("subject");
        mMailboxId = intent.getIntExtra("mailboxId", Sms.MESSAGE_TYPE_INBOX);
        
        if (mMailboxId < 0)
        {
            mMailboxId = Sms.MESSAGE_TYPE_INBOX;
        }
    }
    
    @Override
    public void onStart()
    {
        super.onStart();    
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mHasPause = false;
        startAsyncQuery(); 
        getListView().invalidateViews();
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHasPause = true;        
    } 

    private void initSpinner()
    {
        boxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                       int position, long id){
                int oldQueryType = mQueryBoxType;
                switch (position) {
                    case 0:
                        mQueryBoxType = TYPE_INBOX;
                        break;
                    case 1:
                        mQueryBoxType = TYPE_SENTBOX;
                        break;
                    case 2:
                        mQueryBoxType = TYPE_OUTBOX;
                        break;
                    case 3:
                        mQueryBoxType = TYPE_DRAFTBOX;
                        break;
                    default:
                        return;

                }
                startAsyncQuery();
                if (oldQueryType != mQueryBoxType){
                    unCheckAll();
                    onResume();
                }
            }
    
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            //do nothing
            }
        });
        
        if (MessageUtils.isMultiSimEnabledMms()){
            slotSpinner.setPrompt(getResources().getString(R.string.slot_type_select));
            ArrayAdapter<CharSequence> slotAdapter =  ArrayAdapter.createFromResource(
                this ,R.array.slot_type, android.R.layout.simple_spinner_item);
            slotAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            slotSpinner.setAdapter(slotAdapter);
            slotSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){  
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                    switch (position) {
                        case 0:
                            mQuerySlotType = TYPE_ALL_SLOT;
                            break;
                        case 1:
                            mQuerySlotType = TYPE_SLOT_ONE;
                            break;
                        case 2:
                            mQuerySlotType = TYPE_SLOT_TWO;
                            break;
                        default:
                            return;
                        }
                    onResume();
                }
    
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    //do nothing
                }
            });
        } else {
            slotSpinner.setVisibility(View.GONE);
        }

    }
    
    public boolean isHasPaused()
    {
        return mHasPause;
    }
    
    private void startAsyncQuery()
    {   
        try
        {
            synchronized (mCursorLock)
            {
                setProgressBarIndeterminateVisibility(true);
                // FIXME: I have to pass the mQueryToken as cookie since the
                // AsyncQueryHandler.onQueryComplete() method doesn't provide
                // the same token as what I input here.
                mHasQueryOver = false;
                String selStr = null;
                if (mQuerySlotType == TYPE_SLOT_ONE)
                {
                    selStr = "sub_id = " + MessageUtils.SUB1;
                }
                else if (mQuerySlotType == TYPE_SLOT_TWO)
                {
                    selStr = "sub_id = " + MessageUtils.SUB2;
                }
                mMailboxUri = MAILBOX_URI + mQueryBoxType;
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {                     
                    Log.d(TAG,"startAsyncQuery : mMailboxUri = " + mMailboxUri);
                }

                mQueryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, 0, 
                    Uri.parse(mMailboxUri),
                    MAILBOX_PROJECTION, selStr, 
                    null, "normalized_date DESC");
            }
        }
        catch (SQLiteException e)
        {        
            mHasQueryOver = true;
            SqliteWrapper.checkSQLiteException(this, e);            
            mListView.setVisibility(View.VISIBLE);
        }
    }
    
    private final class BoxMsgListQueryHandler extends AsyncQueryHandler
    {
        public BoxMsgListQueryHandler(ContentResolver contentResolver)
        {
            super(contentResolver);
        }
        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursor)
        {
            synchronized (mCursorLock)
            {       
                if (cursor != null)
                {
                    if (mCursor != null)
                    {
                        mCursor.close();
                    }
                    mCursor = cursor;
                    if (mListAdapter == null)
                    {
                        mListAdapter = new MailBoxMessageListAdapter(
                                           MailBoxMessageList.this,                                           
                                           MailBoxMessageList.this,
                                           MailBoxMessageList.this, cursor);
                        invalidateOptionsMenu();
                        MailBoxMessageList.this.setListAdapter(mListAdapter);
                        View emptyView = (View) findViewById(R.id.emptyview);
                        mListView.setEmptyView(emptyView);
                    }
                    else
                    {  
                        mListAdapter.changeCursor(mCursor);
                        if(cursor.getCount() > 0)
                        {
                            mCountTextView.setVisibility(View.VISIBLE);
                            if(mQueryBoxType == TYPE_INBOX)
                            {
                                int count = 0;
                                while (cursor.moveToNext()) {
                                    if (cursor.getInt(COLUMN_SMS_READ) == 0 
                                        || cursor.getInt(COLUMN_MMS_READ) == 0 ) {
                                        count++;
                                    }
                                }
                                mCountTextView.setText(""+count+"/"+cursor.getCount());
                            }
                            else
                            {
                                mCountTextView.setText(""+cursor.getCount());
                            }
                        }
                        else
                        {
                            mCountTextView.setVisibility(View.INVISIBLE);
                        }
                    }                    
                }
                else
                {
                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        Log.e(TAG, "Cannot init the cursor for the thread list.");                    
                    }

                    finish();
                }
            }
            setProgressBarIndeterminateVisibility(false);
            mHasQueryOver = true;
        }

    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(MailBoxMessageList.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_list_menu, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        if(mSearchView != null)
        {
            mSearchView.setOnQueryTextListener(mQueryTextListener);
            mSearchView.setQueryHint(getString(R.string.search_hint));
            mSearchView.setIconifiedByDefault(true);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            if (searchManager != null) {
                SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
                mSearchView.setSearchableInfo(info);
            }
        }

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.action_delete_all);
        if (item != null) {
            item.setVisible(false);
        }

        if (!MessageUtils.isHasCard()){
            item = menu.findItem(R.id.action_sim_card);
            if (item != null) {
                item.setVisible(false);
            }
        }

        if (!LogTag.DEBUG_DUMP) {
            item = menu.findItem(R.id.action_debug_dump);
            if (item != null) {
                item.setVisible(false);
            }
        }
        // if mQueryText is not null,so restore it.
        if (mQueryText != null && mSearchView != null) {
            mSearchView.setQuery(mQueryText,false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_compose_new:
                createNewMessage();
                break;
            case R.id.action_settings:
                Intent intent = new Intent(this, MessagingPreferenceActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            case R.id.action_change_mode:
                Intent modeIntent = new Intent(this, ConversationList.class);
                startActivityIfNeeded(modeIntent, -1);
                break;
            case R.id.action_sim_card:
                if (!MessageUtils.isMultiSimEnabledMms()) {
                    startActivity(new Intent(this, ManageSimMessages.class));
                } else {
                    if(MessageUtils.getActivatedIccCardCount() > 1)
                    {
                        Intent subIntent = new Intent(this, SelectSubscription.class);
                        subIntent.putExtra(SelectSubscription.PACKAGE, "com.android.mms");
                        subIntent.putExtra(SelectSubscription.TARGET_CLASS, "com.android.mms.ui.ManageSimMessages");
                        startActivity(subIntent);
                    }
                    else
                    {
                        Intent simintent = new Intent(this, ManageSimMessages.class);
                        simintent.putExtra(MessageUtils.SUB_KEY,
                            MessageUtils.isIccCardActivated(MessageUtils.SUB1) ? MessageUtils.SUB1 : MessageUtils.SUB2);
                        startActivity(simintent);
                    }
                }
                break;
            case R.id.action_debug_dump:
                LogTag.dumpInternalTables(this);
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }       
        return true;
    }    

    private void createNewMessage() {
        startActivity(ComposeMessageActivity.createIntent(this, 0));
    }

    private void lockUnlockMessage(Cursor c)
    {
        final String type = c.getString(COLUMN_MSG_TYPE);
        long msgId = c.getLong(COLUMN_ID);
        boolean locked = false;
        //1, lock; 0, unlock
        Uri uri;
        if ("sms".equals(type)) {
            uri = Sms.CONTENT_URI;
            locked = c.getInt(COLUMN_SMS_LOCKED) != 0;            
        } else {
            uri = Mms.CONTENT_URI;
            locked = c.getInt(COLUMN_MMS_LOCKED) != 0;                        
        }
        final Uri lockUri = ContentUris.withAppendedId(uri, msgId);
        final ContentValues values = new ContentValues(1);
        values.put("locked", locked ? 0 : 1);

        Toast.makeText(this, getString(R.string.operate_success),
                                       Toast.LENGTH_LONG).show();
    }
                        

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    
        mHasLocked = false;               
        mHasPause = true;
        if (mCursor != null)
        {
            mCursor.close();
        }
        if (mListAdapter != null)
        {
            mListAdapter.changeCursor(null);
        }
    }

    
    private void confirmDeleteMessages()
    {
        calcuteSelect();
        DeleteMessagesListener l = new DeleteMessagesListener(this);
        confirmDeleteDialog(l, mHasLocked);
    }

    private class DeleteMessagesListener implements OnClickListener
    {
        private final Context mContext;	
        private boolean mDeleteLockedMessages;

        public DeleteMessagesListener(Context context)
        {
             mContext = context;
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessage) {
            mDeleteLockedMessages = deleteLockedMessage;
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton)
        {
            deleteMessages(mDeleteLockedMessages);
        }
    }
    
    private void confirmDeleteDialog(final DeleteMessagesListener listener, boolean locked)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        View contents = View.inflate(this, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_delete_selected_messages));
        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setChecked(false);
        if (!mHasLocked) {
            checkbox.setVisibility(View.GONE);
        } else {
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        }
        builder.setView(contents);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void deleteMessages(boolean deleteLocked)
    {
        String whereClause;
        String smsWhereDelete = mSmsWhereDelete;
        String mmsWhereDelete = mMmsWhereDelete;       

        if (!TextUtils.isEmpty(mSmsWhereDelete))
        {
            smsWhereDelete = smsWhereDelete.substring(0, smsWhereDelete.length()-1);
            smsWhereDelete = "_id in (" + smsWhereDelete + ")";
            whereClause = smsWhereDelete;
            if (!deleteLocked) {
                whereClause = smsWhereDelete == null ? " locked=0 " : smsWhereDelete + " AND locked=0 ";
            }

            if (!TextUtils.isEmpty(whereClause))
            {
                int delSmsCount = SqliteWrapper.delete(this, getContentResolver(),
                    Uri.parse("content://sms"), whereClause, null);
                if(delSmsCount > 0)
                {
                    Toast.makeText(MailBoxMessageList.this, getString(R.string.operate_success),
                        Toast.LENGTH_LONG).show();
                }
            }
        }
        
        if (!TextUtils.isEmpty(mmsWhereDelete))
        {
            mmsWhereDelete = mmsWhereDelete.substring(0, mmsWhereDelete.length()-1);
            mmsWhereDelete = "_id in (" + mmsWhereDelete + ")";
            whereClause = mmsWhereDelete;
            if (!deleteLocked) {
                whereClause = mmsWhereDelete == null ? " locked=0 " : mmsWhereDelete + " AND locked=0 ";
            }

            if (!TextUtils.isEmpty(whereClause))
            {
                int delMmsCount = SqliteWrapper.delete(this, getContentResolver(),
                                     Uri.parse("content://mms"), whereClause, null);
                if(delMmsCount > 0)
                {
                    Toast.makeText(MailBoxMessageList.this, getString(R.string.operate_success),
                        Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    private void calcuteSelect()
    {
        int count = mListAdapter.getCount();
        SparseBooleanArray booleanArray = mListView.getCheckedItemPositions();
        int size = booleanArray.size();

        if (count == 0 ||size == 0)
        {
            return;
        }
        String smsWhereDelete = "";
        String mmsWhereDelete = "";       
        boolean hasLocked = false;

        for (int j = 0; j < size; j++)
        {
            int position = booleanArray.keyAt(j);

            if (!mListView.isItemChecked(position))
            {
                continue;
            }
            Cursor c = (Cursor) mListAdapter.getItem(position);
            if (c == null)
            {
                return;
            }

            String msgtype = "sms";            
            try
            {
                msgtype = c.getString(COLUMN_MSG_TYPE);
            }
            catch (Exception ex)
            {
                continue;
            }
            if (msgtype.equals("sms"))
            {
                String msgId = c.getString(COLUMN_ID);
                int lockValue = c.getInt(COLUMN_SMS_LOCKED);
                if (lockValue == 1)
                {
                    hasLocked = true;                   
                }
                smsWhereDelete += msgId + ",";   
            }
            else if (msgtype.equals("mms"))
            {
                int lockValue = c.getInt(COLUMN_MMS_LOCKED);     
                if (lockValue == 1)
                {
                    hasLocked = true;                   
                }                
                String msgId = c.getString(COLUMN_ID);
                mmsWhereDelete += msgId + ",";
            }                        
        }
        mSmsWhereDelete = smsWhereDelete;
        mMmsWhereDelete = mmsWhereDelete;       
        mHasLocked = hasLocked;
    }
    
    public void onListContentChanged() 
    {  
        if (!mHasPause)
        {
            startAsyncQuery();        
        }
    }    

    public void CheckAll() {
        int count = getListView().getCount();

        for (int i = 0; i < count; i++) {
            getListView().setItemChecked(i, true);
        }
        mListAdapter.notifyDataSetChanged();
    }

    public void unCheckAll() {
        int count = getListView().getCount();

        for (int i = 0; i < count; i++) {
            getListView().setItemChecked(i, false);
        }
        mListAdapter.notifyDataSetChanged();
    }

    private void setupActionBar()
    {
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup)LayoutInflater.from(this)
            .inflate(R.layout.mailbox_list_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        mCountTextView = (TextView)v.findViewById(R.id.message_count);
        return;
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private ImageView mSelectedAll; 
        //used in MultiChoiceMode
        private boolean mHasSelectAll = false;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {  
            // comes into MultiChoiceMode
            mMultiChoiceMode = true;
            mSpinners.setVisibility(View.GONE);
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.conversation_multi_select_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = (ViewGroup)LayoutInflater.from(MailBoxMessageList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar, null);

                mSelectedConvCount =
                    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);
            }

            if (mSelectedConvCount != null) {
                mSelectedConvCount.setText("0");
            }

            mode.setCustomView(mMultiSelectActionBarView);
            ((TextView)mMultiSelectActionBarView.findViewById(R.id.title))
                .setText(R.string.select_messages);   

            mSelectedAll = (ImageView)mMultiSelectActionBarView.findViewById(R.id.selecte_all);
            mSelectedAll.setImageResource(R.drawable.ic_menu_select_all);           
            mSelectedAll.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {                       
                        if(mHasSelectAll)
                        {
                            mHasSelectAll = false;
                            unCheckAll();
                            mSelectedAll.setImageResource(R.drawable.ic_menu_select_all);
                        }
                        else
                        {
                            mHasSelectAll = true;
                            CheckAll();
                            mSelectedAll.setImageResource(R.drawable.ic_menu_unselect_all);
                        }
                    }
                });         

            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(MailBoxMessageList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar, null);
                mode.setCustomView(v);

                mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);
            }
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ListView listView = getListView();
            final int checkedCount = listView.getCheckedItemCount();            
            switch (item.getItemId()) {
                case R.id.delete:
                    confirmDeleteMessages();
                    break;
                default:
                    break;
            }
            mode.finish();
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            // leave MultiChoiceMode
            mMultiChoiceMode = false;
            mHasSelectAll = false;
            getListView().clearChoices();
            mListAdapter.notifyDataSetChanged();
            mSpinners.setVisibility(View.VISIBLE);
        }

        public void onItemCheckedStateChanged(ActionMode mode,
                int position, long id, boolean checked) {
            ListView listView = getListView();
            listView.invalidateViews();
            final int checkedCount = listView.getCheckedItemCount();
            mSelectedConvCount.setText(Integer.toString(checkedCount));
        }
    }    
}
