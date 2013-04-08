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

import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.telephony.MSimSmsManager;
import android.telephony.MSimTelephonyManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.StatFs;
import com.android.mms.R;
import com.google.android.mms.util.SqliteWrapper;


/**
 * Show a list of memory status used in MMS 
 */
public class MemoryStatusActivity extends Activity
{
    private static final String TAG = "MemoryStatusView";
    private AsyncQueryHandler mQueryHandler = null;
    private static final Uri MAILBOX_URI = Uri.parse("content://mms-sms/mailboxs"); 

    private int m_Card1SmsCountUsed = -1;
    private int m_Card1SmsCountAll = -1;
    private int m_Card2SmsCountUsed = -1;
    private int m_Card2SmsCountAll = -1;

    private static final int SHOW_LIST = 0;
    private static final int SHOW_EMPTY = 1;
    private static final int SHOW_BUSY = 2;

    private static final int ICC_QUERY_ID = 111;
    private static final int ICC2_QUERY_ID = 112;
    private static final int MAILBOX_QUERY_ID = 113;

    private int mState = SHOW_LIST;

    TextView CardLabel;
    TextView m_Card1Detail;
    TextView m_Card1Label;
    TextView m_Card2Detail;
    TextView m_Card2Label;
    TextView phoneAll;
    TextView usedDetail;
    TextView unusedDetail;
    TextView countView;
    TextView usedView;
    TextView unusedView;

    private static int mIccCount = 0;
    private static int mIcc2Count = 0;

    private boolean mIsIccOver = false;
    private boolean mIsIcc2Over = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memory_layout);
        
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mQueryHandler = new QueryHandler(getContentResolver());
        initUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }
   
        return true;
    }
    
    private void initUi()
    {
        phoneAll = (TextView) findViewById(R.id.phone);
        usedDetail = (TextView) findViewById(R.id.useddetail);
        unusedDetail = (TextView) findViewById(R.id.unuseddetail);
        countView = (TextView) findViewById(R.id.phonecount);
        usedView = (TextView) findViewById(R.id.phoneused);
        unusedView = (TextView) findViewById(R.id.phoneunused);

        String slotOneStr = getString(R.string.sim_card1);
        String slottwoStr = getString(R.string.sim_card2);
        CardLabel = (TextView) findViewById(R.id.cardlabel);
        m_Card1Detail = (TextView) findViewById(R.id.carddetail);
        m_Card1Label = (TextView) findViewById(R.id.card);
        m_Card2Detail = (TextView) findViewById(R.id.cardtwodetail);
        m_Card2Label = (TextView) findViewById(R.id.cardtwo);
        if(!MessageUtils.isMultiSimEnabledMms())
        {
            slotOneStr = getString(R.string.sim_card);
            m_Card2Detail.setVisibility(View.GONE);
            m_Card2Label.setVisibility(View.GONE);
            if(MessageUtils.isHasCard()){
                updateState(SHOW_BUSY, MessageUtils.SUB1);
                startQueryIccUsedCount();
            } else {
                CardLabel.setVisibility(View.GONE);
                m_Card1Detail.setVisibility(View.GONE);
                m_Card1Label.setVisibility(View.GONE);
                mIsIccOver = true;
            }
        }
        else
        {
            if(!MessageUtils.isHasCard())
            {
                CardLabel.setVisibility(View.GONE);
                m_Card1Detail.setVisibility(View.GONE);
                m_Card1Label.setVisibility(View.GONE);
                m_Card2Detail.setVisibility(View.GONE);
                m_Card2Label.setVisibility(View.GONE);
            }
            else
            {
                if(MessageUtils.isHasCard(MessageUtils.SUB1)){
                    updateState(SHOW_BUSY, MessageUtils.SUB1);
                    startQueryIccUsedCount();
                } else {
                    m_Card1Detail.setVisibility(View.GONE);
                    m_Card1Label.setVisibility(View.GONE);
                    mIsIccOver = true;
                }
                
                if(MessageUtils.isHasCard(MessageUtils.SUB2)){
                    updateState(SHOW_BUSY, MessageUtils.SUB2);
                    startQueryIcc2UsedCount();
                } else {
                    m_Card2Detail.setVisibility(View.GONE);
                    m_Card2Label.setVisibility(View.GONE);
                    mIsIcc2Over = true;
                }
            } 
        }

        m_Card1Label.setText(slotOneStr);
        m_Card2Label.setText(slottwoStr);
   
        String unusedStr = MessageUtils.formatMemorySize(MessageUtils.getStoreUnused());
        String usedStr = MessageUtils.formatMemorySize(MessageUtils.getStoreUsed());

        unusedDetail.setText(unusedStr);
        usedDetail.setText(usedStr);
        startQueryMailboxCount();
    }
    
    private void startQueryIccUsedCount()
    {
        Uri uri = MessageUtils.ICC1_URI;
        if(!MessageUtils.isMultiSimEnabledMms())
        {
            uri = MessageUtils.ICC_URI;
        }
        
        try
        {
            mQueryHandler.startQuery(ICC_QUERY_ID, null, uri, null, null, null, null);
        }
        catch (SQLiteException e)
        {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startQueryIcc2UsedCount()
    {
        Uri uri = MessageUtils.ICC2_URI;
        try
        {
            mQueryHandler.startQuery(ICC2_QUERY_ID, null, uri, null, null, null, null);
        }
        catch (SQLiteException e)
        {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startQueryMailboxCount()
    {
        try
        {
            mQueryHandler.startQuery(MAILBOX_QUERY_ID, null, MAILBOX_URI, 
                null, null, null, "boxtype");
        }
        catch (SQLiteException e)
        {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }
    private class QueryHandler extends AsyncQueryHandler
    {

        public QueryHandler(ContentResolver contentResolver)
        {
            super(contentResolver);
        }
        @Override
        protected void onQueryComplete(
            int token, Object cookie, Cursor cursor)
        {
            switch (token)
            {     
                 case MAILBOX_QUERY_ID:
                    int inboxCount = 0;
                    int sentCount = 0;
                    int outboxCount = 0;
                    int draftsCount = 0;
                    int phoneAllCount = 0;
                    
                    if ((cursor != null) && cursor.getCount() >= 4)
                    {
                        if (cursor.moveToFirst())
                        {
                            inboxCount = cursor.getInt(3);      
                            if (cursor.moveToPosition(1))
                            {
                                sentCount = cursor.getInt(3); 
                            }
                            if (cursor.moveToPosition(2))
                            {
                                outboxCount = cursor.getInt(3); 
                            }
                            if (cursor.moveToPosition(3))
                            {
                                draftsCount = cursor.getInt(3); 
                            }
                        }
                    }
                    phoneAllCount = inboxCount + sentCount + outboxCount + draftsCount;
                    
                    String phoneCountStr = getString(R.string.type_inbox)+" : "+Integer.toString(inboxCount)+"\n"
                        +getString(R.string.type_sent)+" : "+Integer.toString(sentCount)+"\n"
                        +getString(R.string.type_outbox)+" : "+Integer.toString(outboxCount)+"\n"
                        +getString(R.string.type_draft)+" : "+Integer.toString(draftsCount)+"\n"
                        +getString(R.string.phonecount)+" : "+Integer.toString(phoneAllCount);
                    
                    countView.setText(phoneCountStr);

                    if(cursor != null)
                    {
                        cursor.close();
                    }
                    
                    break;
                 case ICC_QUERY_ID:
                    if ((cursor != null) && !cursor.moveToFirst())
                    {
                        mIccCount = 0;
                        updateState(SHOW_LIST, MessageUtils.SUB1);
                        cursor.close();
                    }
                    else
                    {
                        mIccCount = cursor.getCount();
                        updateState(SHOW_LIST, MessageUtils.SUB1);
                        cursor.close();
                    }
                    
                    break;            
                   case ICC2_QUERY_ID:
                    if ((cursor != null) && !cursor.moveToFirst())
                    {
                        mIcc2Count = 0;
                        updateState(SHOW_LIST, MessageUtils.SUB2);
                        cursor.close();
                    }
                    else
                    {
                        mIcc2Count = cursor.getCount();
                        updateState(SHOW_LIST, MessageUtils.SUB2);
                        cursor.close();
                    }
                    break;                   
                default:
                    return;
            }
        }
    }

    private void updateState(int state, int subscription)
    {
        switch(state) 
        {
            case SHOW_LIST:
                phoneAll.setVisibility(View.VISIBLE);
                usedDetail.setVisibility(View.VISIBLE);
                unusedDetail.setVisibility(View.VISIBLE);
                countView.setVisibility(View.VISIBLE);
                usedView.setVisibility(View.VISIBLE);
                unusedView.setVisibility(View.VISIBLE);
                CardLabel.setVisibility(View.VISIBLE);
                
                int allCount = -1;

                if(MessageUtils.SUB1 == subscription)
                {
                    m_Card1Label.setVisibility(View.VISIBLE);
  
                    if(!MessageUtils.isMultiSimEnabledMms())
                    {
                        allCount = SmsManager.getDefault().getSmsCapCountOnIcc();
                    }
                    else
                    {
                        allCount = MSimSmsManager.getDefault().getSmsCapCountOnIcc(subscription);
                    }
                
                    Log.d(TAG, "liutao allCount:" + allCount);
                    String tempStr =Integer.toString(mIccCount) + "/" + Integer.toString(allCount);
                    m_Card1Detail.setText(tempStr);
                    mIsIccOver = true;
                }
                else
                {
                    m_Card2Label.setVisibility(View.VISIBLE);
                    allCount = MSimSmsManager.getDefault().getSmsCapCountOnIcc(subscription);
                    Log.d(TAG, "liutao allCount:" + allCount);
                    String tempStr =Integer.toString(mIcc2Count) + "/" + Integer.toString(allCount);
                    m_Card2Detail.setText(tempStr);
                    mIsIcc2Over = true;
                }
                 
                if((MessageUtils.isMultiSimEnabledMms() && mIsIccOver && mIsIcc2Over)
                    || (!MessageUtils.isMultiSimEnabledMms() && mIsIccOver))
                {
                    setTitle(getString(R.string.memory_status_title));
                    setProgressBarIndeterminateVisibility(false);
                }
                 
                break;
                
            case SHOW_BUSY:
                phoneAll.setVisibility(View.GONE);
                usedDetail.setVisibility(View.GONE);
                unusedDetail.setVisibility(View.GONE);
                countView.setVisibility(View.GONE);
                usedView.setVisibility(View.GONE);
                unusedView.setVisibility(View.GONE);
                CardLabel.setVisibility(View.GONE);
                m_Card1Label.setVisibility(View.GONE);
                m_Card2Label.setVisibility(View.GONE);
                mIsIccOver = false;
                mIsIcc2Over = false;
                
                setTitle(getString(R.string.refreshing));
                setProgressBarIndeterminateVisibility(true);
                break;
            default:
                Log.e(TAG, "Invalid State");
        }
    }
}
