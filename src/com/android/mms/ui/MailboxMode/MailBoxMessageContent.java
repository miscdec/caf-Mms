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

import android.app.Activity;
import android.os.Bundle;
import com.android.mms.R;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;
import android.util.Log;
import com.android.mms.LogTag;
import android.net.Uri;
import android.database.sqlite.SqliteWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.SendingProgressTokenManager;
import com.google.android.mms.MmsException;
import android.content.ContentValues;
import android.database.Cursor;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.content.ContentUris;
import android.content.DialogInterface.OnClickListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;
import android.database.sqlite.SQLiteException;
import android.text.format.Time;
import android.telephony.SmsMessage;
import com.android.internal.telephony.uicc.IccUtils;
import android.view.Window;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View.OnCreateContextMenuListener;
import android.text.method.HideReturnsTransformationMethod;
import android.content.ActivityNotFoundException;
import java.util.ArrayList;
import com.android.mms.util.SmileyParser;
import java.util.Arrays;
import java.util.HashSet;
import android.provider.Telephony.Threads;
import android.content.AsyncQueryHandler;
import android.widget.Toast;
import com.android.mms.util.DraftCache;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View.OnLongClickListener;
import android.view.animation.AlphaAnimation;
import android.content.Context;
import android.util.AttributeSet;
import java.io.File;
import java.text.SimpleDateFormat;
import android.text.format.Time;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.widget.Button;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.text.Spannable;
import android.view.Gravity;
import com.android.mms.MmsConfig;
import android.app.ProgressDialog;
import android.widget.AbsoluteLayout;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.text.style.UnderlineSpan;
import android.text.method.LinkMovementMethod;
import android.app.ActionBar;
import android.util.TypedValue;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import com.android.mms.data.Contact;
import android.telephony.TelephonyManager;
import android.os.AsyncTask;

public class MailBoxMessageContent extends Activity 
{
    private static final String TAG = "MailBoxMessageContent";
    private Uri     mMessageUri;    
    private int     mMsgId;
    private long    mMsgThreadId;// threadid of message
    private String  mMsgText;// Text of message
    private String  mMsgFromto;
    private String  mFromtoLabel;
    private String  mSendLabel;
    private String  mDisplayName;
    private String  mMsgTime;// Date of message
    private Long    mDateLongFormat;
    private int     mMsgstatus;
    private int     mRead;
    private int     mMailboxId;
    private int     mMsgType = Sms.MESSAGE_TYPE_INBOX;    
    private String  mTitle; 
    private boolean mLock = false;
    
    private int mSubID = MessageUtils.SUB_INVALID;
    private ArrayList<String> m_AllIdList = null;    
    private String mMsgUriStr = null;
    private Cursor mCursor = null;

    private TextView mBodyTextView;
    private TextView mFromTextView;
    private TextView mTimeTextView;  
    private TextView mTimeDetailTextView;    
    private TextView mNumberView;   
    private TextView mSlotTypeView; 

    private static final int MENU_CALL_RECIPIENT    = Menu.FIRST;
    private static final int MENU_DELETE            = Menu.FIRST + 1;
    private static final int MENU_FORWARD           = Menu.FIRST + 2;
    private static final int MENU_REPLY             = Menu.FIRST + 3;
    private static final int MENU_RESEND            = Menu.FIRST + 4;
    private static final int MENU_SAVE_TO_CONTACT   = Menu.FIRST + 5;
    private static final int MENU_COPY              = Menu.FIRST + 6; 
    private static final int MENU_LOCK              = Menu.FIRST + 7;     
    private static final int MENU_LOAD              = Menu.FIRST + 8;  
    
    private BackgroundQueryHandler mBackgroundQueryHandler; 
    private static final int DELETE_MESSAGE_TOKEN  = 6701;
    
    private static final int OPERATE_DEL_SINGLE_OVER = 1;
    private static final int UPDATE_TITLE            = 2;
    private static final int SHOW_TOAST              = 3;

    ProgressDialog mProgressDialog = null; 
    private SetReadThread mSetReadThread = null;//new SetReadThread();
    private ContentResolver mContentResolver;
    private static final String[] SMS_LOCK_PROJECTION = { Sms._ID, Sms.LOCKED };
    private final Object mCursorLock = new Object();
    private static final String[] SMS_DETAIL_PROJECTION = new String[]
        {
            Sms.THREAD_ID,
            Sms.DATE,
            Sms.ADDRESS,
            Sms.BODY,                
            Sms.SUB_ID,
            Sms.LOCKED,                
            Sms.DATE_SENT
        };     
   
    private static final int COLUMN_THREAD_ID    = 0;
    private static final int COLUMN_DATE         = 1;
    private static final int COLUMN_SMS_ADDRESS  = 2;
    private static final int COLUMN_SMS_BODY     = 3;
    private static final int COLUMN_SMS_SUBID    = 4;
    private static final int COLUMN_SMS_LOCKED   = 5;
    private static final int COLUMN_DATE_SENT    = 6;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        mTitle = getString(R.string.message_detail);
        setTitle(mTitle);
        setContentView(R.layout.mailbox_msg_detail);
        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);

        initUi(getIntent());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);              
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private void refreshUi()
    {
        if (!TextUtils.isEmpty(mMsgFromto))
        {
            String address = mMsgFromto;
            mNumberView.setText(mMsgFromto);                
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();

        if("Browser Information".equals(mMsgFromto))
        {
            menu.add(0, MENU_LOAD, 0, R.string.menu_load_push);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
            return true;
        }

        if(MessageUtils.isHasCard())
        {
            menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                .setIcon(R.drawable.ic_menu_call)
                .setTitle(R.string.menu_call)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        if (mMsgType == Sms.MESSAGE_TYPE_INBOX)
        {
            menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);     
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);           
        }
        else if (mMsgType == Sms.MESSAGE_TYPE_FAILED || mMsgType == Sms.MESSAGE_TYPE_OUTBOX)
        {           
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);  
            menu.add(0, MENU_RESEND, 0, R.string.menu_resend);   
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);   
        }
        else if (mMsgType == Sms.MESSAGE_TYPE_SENT)
        {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);    
            menu.add(0, MENU_RESEND, 0, R.string.menu_resend);         
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
        }    
        else if (mMsgType == Sms.MESSAGE_TYPE_QUEUED)
        {
            menu.add(0, MENU_FORWARD, 0, R.string.menu_forward);    
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);         
        }            


        if (MessageUtils.isHasCard() &&
            (mMailboxId == Sms.MESSAGE_TYPE_INBOX
                || mMailboxId == Sms.MESSAGE_TYPE_DRAFT
                || mMailboxId == Sms.MESSAGE_TYPE_SENT))
        {
            menu.add(0, MENU_COPY, 0, R.string.menu_copy_to);
        }

        int isLocked = getLockAttr();
        if (isLocked == 1)
        {
            menu.add(0, MENU_LOCK, 0, R.string.menu_unlock);

        }
        else
        {
            menu.add(0, MENU_LOCK, 0, R.string.menu_lock);           
        }

        if(!Contact.get(mMsgFromto, false).existsInDatabase())
        {
            menu.add(0, MENU_SAVE_TO_CONTACT, 0, R.string.menu_add_to_contacts);  
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case MENU_CALL_RECIPIENT:
                if(MessageUtils.isMultiSimEnabledMms())
                {
                    if(MessageUtils.getActivatedIccCardCount() > 1)
                    {
                        showCallSelectDialog();
                    }
                    else
                    {
                        if(MessageUtils.isIccCardActivated(MessageUtils.SUB1))
                        {
                            MessageUtils.dialRecipient(this, mMsgFromto, MessageUtils.SUB1);
                        }
                        else if(MessageUtils.isIccCardActivated(MessageUtils.SUB2))
                        {
                            MessageUtils.dialRecipient(this, mMsgFromto, MessageUtils.SUB2);
                        }                         
                    }
                }
                else
                {
                    MessageUtils.dialRecipient(this, mMsgFromto, MessageUtils.SUB_INVALID);
                }
                break;
            case MENU_DELETE:
                delete();
                break;
            case MENU_FORWARD:
                forward();
                break;             
            case MENU_REPLY:
                reply();
                break;
            case MENU_COPY:
                if(MessageUtils.isMultiSimEnabledMms())
                {
                    if(MessageUtils.getActivatedIccCardCount() > 1)
                    {
                        showCopySelectDialog();
                    }
                    else
                    {
                        if(MessageUtils.isIccCardActivated(MessageUtils.SUB1))
                        {
                            copy(MessageUtils.SUB1);
                        }
                        else if(MessageUtils.isIccCardActivated(MessageUtils.SUB2))
                        {
                            copy(MessageUtils.SUB2);
                        }                         
                    }                  
                }
                else
                {
                    copy(MessageUtils.SUB_INVALID);
                }
                break;
            case MENU_LOCK:
                lockUnlockMessage();
                break;            
            case MENU_RESEND:
                resend();
                break;
            case MENU_LOAD:
                loadUrl();
                break;
            case MENU_SAVE_TO_CONTACT:
                saveToContact();
                break;
            case android.R.id.home:
                finish();
                break;            
            default:
                return true;
        }

        return true;
    }
    
    private void setMessageRead(Context context)
    {
        ContentValues values = new ContentValues(1);
        values.put(Sms.READ, 1);
        SqliteWrapper.update(context, getContentResolver(),
                             mMessageUri, values, null, null);
    }  

    private void confirmDeleteDialog(OnClickListener listener, boolean locked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(locked ? R.string.confirm_delete_locked_message :
                    R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void showCopySelectDialog(){
        final String[] texts = new String[] {getString(R.string.type_slot1), getString(R.string.type_slot2)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_copy_to));
        builder.setCancelable(true);
        builder.setItems(texts, new DialogInterface.OnClickListener()
        {
            public final void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         copy(MessageUtils.SUB1);
                         Looper.loop();
                        }
                    }).start();
                }
                else
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         copy(MessageUtils.SUB2);
                         Looper.loop();
                        }
                    }).start();
                }
                dialog.dismiss();
            }
        });
        builder.show();    
    }

    private void showCallSelectDialog(){
        final String[] texts = new String[] {getString(R.string.type_slot1), getString(R.string.type_slot2)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_call));
        builder.setCancelable(true);
        builder.setItems(texts, new DialogInterface.OnClickListener()
        {
            public final void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(MailBoxMessageContent.this, mMsgFromto, MessageUtils.SUB1);
                         Looper.loop();
                        }
                    }).start();
                }
                else
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(MailBoxMessageContent.this, mMsgFromto, MessageUtils.SUB2);
                         Looper.loop();
                        }
                    }).start();
                }
                dialog.dismiss();
            }
        });
        builder.show();    
    }
    
    public void saveToContact()
    {    
        String address = mMsgFromto;
        if (TextUtils.isEmpty(address)) {
             if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                 Log.v(TAG,"  saveToContact fail for null address! ");                     
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

    private void delete()
    {
        mLock = getLockAttr() != 0; 
        DeleteMessageListener l = new DeleteMessageListener();
        confirmDeleteDialog(l, mLock);
    }

    private void copy(int subscription)
    {
        copyToCard(mDateLongFormat, mMsgText, mMsgFromto, mMailboxId, MessageUtils.MESSAGE_READ, subscription);
    }
        
    private void copyToCard(final Long date, String body, final String address,
                                       final int boxId, final int read, final int subscription)
    {
        SmsManager smsManager = SmsManager.getDefault();
        final ArrayList<String> messages = smsManager.divideMessage(body);
        {
            final int messageCount = messages.size();

            new Thread(new Runnable() {
                public void run() {
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
                        values.put(Sms.READ, read);
                        values.put(Sms.SUB_ID, subscription);  // -1 for MessageUtils.SUB_INVALID , 0 for MessageUtils.SUB1, 1 for MessageUtils.SUB2                 
                        Uri uriStr = MessageUtils.getIccUriBySubscription(subscription);
                        
                        Uri retUri = SqliteWrapper.insert(MailBoxMessageContent.this, getContentResolver(),
                                                          uriStr, values);
                        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                            Log.e(TAG, "copyToCard : uriStr = " + uriStr.toString() 
                                + ", retUri = " + retUri.toString());
                        }
                        
                        if (retUri == null)
                        {
                            msg.obj = getString(R.string.operate_failure);
                            break;
                        }
                        else if (MessageUtils.COPY_SUCCESS_FULL.equals(retUri.toString()))
                        {
                            msg.obj = getString(R.string.copy_success_full);
                            break;
                        }
                        else if (MessageUtils.COPY_FAILURE_FULL.equals(retUri.toString()))
                        {
                            msg.obj = getString(R.string.copy_failure_full);
                            break;
                        }
                    }
                          
                    uihandler.sendMessage(msg);
                }
            }).start();                  
        }
    }
    private void reply()
    {
        Intent intent = new Intent(this, ComposeMessageActivity.class);
        intent.putExtra("address", mMsgFromto);
        intent.putExtra("msg_reply", true);
        intent.putExtra("exit_on_sent", true);
        this.startActivity(intent);
    }

    private void forward()
    {
        Intent intent = new Intent(this, ComposeMessageActivity.class);
        intent.putExtra("sms_body", mMsgText);
        intent.putExtra("exit_on_sent", true);
        intent.putExtra("forwarded_message", true);
        this.startActivity(intent);
    }

    private void resend()
    {
        resendShortMessage(mMsgThreadId, mMessageUri);
        finish();
    }

    private void resendShortMessage(long threadId, Uri uri)
    {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                                            uri, new String[] { Sms.ADDRESS, Sms.BODY, Sms.SUB_ID }, null, null, null);

        if (cursor != null)
        {
            try
            {
                if ((cursor.getCount() == 1) && cursor.moveToFirst())
                {
                    MessageSender sender = new SmsMessageSender(
                                               this, new String[] { cursor.getString(0) },
                                               cursor.getString(1), threadId, cursor.getInt(2));
                    sender.sendMessage(threadId);

                    // Delete the undelivered message since the sender will
                    // save a new one into database.
                    SqliteWrapper.delete(this, getContentResolver(), uri, null, null);
                }
            }
            catch (MmsException e)
            {
                Log.e(TAG, e.getMessage());
            }
            finally
            {
                cursor.close();
            }
        }
        else
        {
            Toast.makeText(MailBoxMessageContent.this, R.string.send_failure, Toast.LENGTH_SHORT).show();
        }
    }

    private void lockUnlockMessage()
    {
        int lockValue;
        //1, lock; 0, unlock
        mLock = getLockAttr() != 0;            
        final Uri lockUri = mMessageUri;
        lockValue = mLock ? 0 : 1;        
        final ContentValues values = new ContentValues(1);
        values.put("locked", lockValue);

        new Thread(new Runnable() {
            public void run() {
                getContentResolver().update(lockUri,
                        values, null, null);
                Message msg = Message.obtain();
                msg.what = SHOW_TOAST;
                msg.obj = getString(R.string.operate_success);
                uihandler.sendMessage(msg);                        
            }
        }).start();
    }

    private int getLockAttr()
    {
        int locked = 0;
        
        Cursor c = SqliteWrapper.query(MailBoxMessageContent.this, mContentResolver,
                   mMessageUri, SMS_LOCK_PROJECTION, null, null, null);
        if (c == null)
        {
            return 0;
        }
        try
        {
            if (c.moveToFirst())
            {
                locked = c.getInt(1);
            }
            else
            {
                return 0;
            }
        }
        finally
        {
            c.close();
        }    
        return locked;
    }

    private void initUi(Intent intent)
    {
        setProgressBarIndeterminateVisibility(true);
        mBodyTextView = (TextView) findViewById(R.id.TextViewBody);        
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        mBodyTextView.setTextIsSelectable(true);
        mBodyTextView.setTelUrl("tels:");
        mBodyTextView.setWebUrl("www_custom:");
        mFromTextView = (TextView) findViewById(R.id.TextViewFrom);
        mNumberView = (TextView) findViewById(R.id.TextViewNumber); 
        mNumberView.setTelUrl("tels:"); 
        mTimeTextView = (TextView) findViewById(R.id.TextViewTime);
        mTimeDetailTextView = (TextView) findViewById(R.id.TextViewTimeDetail);  
        mSlotTypeView = (TextView) findViewById(R.id.TextViewSlotType);

        if (null != intent.getAction())
        {
            String type = intent.getType();
            String uriString = intent.getStringExtra("message_uri");
            mMessageUri = Uri.parse(uriString);
            if (mMessageUri == null)
            {
                finish();
                return;
            }
            
            Cursor cursor = null;
            cursor = SqliteWrapper.query(this, getContentResolver(),
                        mMessageUri, SMS_DETAIL_PROJECTION, null, null, null);

            if (cursor != null)
            {
                if ((cursor.getCount() == 1) && cursor.moveToFirst())
                {
                    mSubID = cursor.getInt(COLUMN_SMS_SUBID);  
                    mMsgThreadId = cursor.getLong(COLUMN_THREAD_ID);
                    
                    mMsgText = cursor.getString(COLUMN_SMS_BODY);
                    mMsgFromto = cursor.getString(COLUMN_SMS_ADDRESS);
                    mDateLongFormat = cursor.getLong(COLUMN_DATE);                 
                    mLock = cursor.getInt(COLUMN_SMS_LOCKED) == 0 ? false :true;
                }
                else
                {
                    cursor.close();   
                    finish();
                    return;
                }
                cursor.close();
            }
            else
            {              
                finish();
                return;
            }

            mMsgTime= MessageUtils.formatTimeStampString(this, mDateLongFormat);
            mMsgstatus = -1;
            mRead = 0;
            mMailboxId = Sms.MESSAGE_TYPE_INBOX;
        }
        else
        {
            mMessageUri = (Uri) intent.getParcelableExtra("msg_uri");
            mMsgUriStr = intent.getStringExtra("sms_uri_str");
            if ((mMessageUri == null))
            {
                // If we haven't been given a thread id or a URI in the extras,
                // get it out of the intent.
                Uri uri = intent.getData();
            }
            mMsgThreadId = intent.getLongExtra("sms_threadid", -1);
            mMsgText = intent.getStringExtra("sms_body");
            mMsgFromto = intent.getStringExtra("sms_fromto");
            mFromtoLabel = intent.getStringExtra("sms_fromtolabel");
            mSendLabel = intent.getStringExtra("sms_sendlabel");
            mDisplayName = intent.getStringExtra("sms_displayname");
            mDateLongFormat = intent.getLongExtra("sms_datelongformat", -1);
            mMsgstatus = intent.getIntExtra("sms_status", -1);
            mRead = intent.getIntExtra("sms_read", 0);
            mMailboxId = intent.getIntExtra("mailboxId", 1);
            mLock = intent.getIntExtra("sms_locked", 0) == 0 ? false : true; 
            mSubID = intent.getIntExtra("sms_subid", MessageUtils.SUB_INVALID);
            mMsgTime= MessageUtils.formatTimeStampString(this, mDateLongFormat);
            mMsgType = intent.getIntExtra("sms_type", Sms.MESSAGE_TYPE_INBOX);           

            m_AllIdList = intent.getStringArrayListExtra("sms_id_list");
        }

        mBodyTextView.setText(formatMessage(mMsgText));
        mNumberView.setText(mMsgFromto);   
        mFromTextView.setText(mFromtoLabel); 
        mTimeTextView.setText(mSendLabel);
        mTimeDetailTextView.setText(mMsgTime);  
        if(MessageUtils.isMultiSimEnabledMms())
        {
            mSlotTypeView.setVisibility(View.VISIBLE);
            mSlotTypeView.setText(mSubID == 0 ? getString(R.string.slot_type, getString(R.string.type_slot1))
                : getString(R.string.slot_type, getString(R.string.type_slot2)));
        }


        if (!TextUtils.isEmpty(mDisplayName) && !mDisplayName.equals(mMsgFromto))
        {
            mFromTextView.setText(mFromtoLabel);   
            String numberStr = mDisplayName + " <" + mMsgFromto + ">";
            mNumberView.setText(numberStr);               
        }
        else
        {
            mFromTextView.setText(mFromtoLabel);
            mNumberView.setText(mMsgFromto);                
        }

        if (mRead == 0)
        {
            if (mSetReadThread == null)
            {
                mSetReadThread = new SetReadThread();
            }
            mSetReadThread.start();
        }
        else
        {
            setTitle(mTitle);
            setProgressBarIndeterminateVisibility(false);
        }
    }

    private void loadUrl()
    {
        String body = mBodyTextView.getText().toString();
        String url = body.substring(body.indexOf("http"));
        if (TextUtils.isEmpty(url))
        {
            return;
        }
        if (!url.regionMatches(true, 0, "http://", 0, 7) 
                && !url.regionMatches(true, 0, "https://", 0, 8))
        {        
            url = "http://" + url;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra("subscription", mSubID);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        try
        {
            startActivity(intent);
        }        
        catch (ActivityNotFoundException e)        
        {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.e(TAG, "loadUrl: error url = " + url);                    
            }
        }
    }


    private class DeleteMessageListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();

            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... none) {
                    mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                            null, mMessageUri,
                            mLock ? null : "locked=0", null);
                    return null;
                }
            }.execute();
        }
    }

    private final class BackgroundQueryHandler extends AsyncQueryHandler
    {
        public BackgroundQueryHandler(ContentResolver contentResolver)
        {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursor)
        {
            synchronized (mCursorLock)
            {
                    return;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result)
        {
            switch(token)
            {
                case DELETE_MESSAGE_TOKEN:
                    Message msg = Message.obtain();
                    msg.what = OPERATE_DEL_SINGLE_OVER;
                    msg.arg1 = result;
                    uihandler.sendMessage(msg);
                    break;                        
            }            
        }
    }

    private Handler uihandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UPDATE_TITLE:
                {
                    setTitle(mTitle);
                    setProgressBarIndeterminateVisibility(false);
                    break;
                }
                case SHOW_TOAST:
                {                
                    if(mProgressDialog != null && mProgressDialog.isShowing())
                    {   
                       mProgressDialog.dismiss();
                    }
                    
                    String toastStr = (String) msg.obj;
                    Toast.makeText(MailBoxMessageContent.this, toastStr, 
                                    Toast.LENGTH_LONG).show();

                    break; 
                }
                case OPERATE_DEL_SINGLE_OVER:
                {
                    int result = msg.arg1;
                    if (result > 0)
                    {
                        Toast.makeText(MailBoxMessageContent.this, R.string.operate_success,
                                       Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(MailBoxMessageContent.this, R.string.operate_failure,
                                       Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    break;
                }
                default:
                    break;
            }
        }
    };

    private CharSequence formatMessage(String body)
    {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(body))
        {
            SmileyParser parser = SmileyParser.getInstance();
            buf.append(parser.addSmileySpans(body));
        }
        return buf;
    }

    private class SetReadThread extends Thread
    {
        public SetReadThread()
        {
            super("SetReadThread");
        }
        public void run()
        {
            try
            {
                Thread.sleep(500);
                setMessageRead(MailBoxMessageContent.this);
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(MailBoxMessageContent.this, MessagingNotification.THREAD_NONE, false);  
            }
            catch(Exception e)
            {}
            Message msg = Message.obtain();
            msg.what = UPDATE_TITLE;
            uihandler.sendMessage(msg);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        uihandler.removeCallbacksAndMessages(null);
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }    
}
