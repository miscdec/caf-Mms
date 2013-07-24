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
public class MemoryStatusActivity extends Activity {
    private static final String TAG = "MemoryStatusView";
    private AsyncQueryHandler mQueryHandler = null;
    private static final Uri MAILBOX_URI = Uri.parse("content://mms-sms/mailboxs");

    private static final int SHOW_LIST = 0;
    private static final int SHOW_BUSY = 1;

    private static final int ICC_QUERY_ID = 111;
    private static final int ICC2_QUERY_ID = 112;
    private static final int MAILBOX_QUERY_ID = 113;

    // inbox, outbox, sendbox, draftbox
    private final int MAILBOX_COUNT = 4;
    private final int MAILBOX_COUNT_COLUMN_INDEX = 3;

    TextView mCardLabel;
    TextView mCard1Detail;
    TextView mCard1Label;
    TextView mCard2Detail;
    TextView mCard2Label;
    TextView mPhoneAllView;
    TextView mCountView;
    TextView mMmsUsedView;
    TextView mElseUsedView;
    TextView mMemoryUnusedView;
    TextView mMemoryAllView;
    TextView mMmsUsedDetail;
    TextView mElseUsedDetail;
    TextView mMemoryUnusedDetail;
    TextView mMemoryAllDetail;

    private static int mIccCount = 0;
    private static int mIcc2Count = 0;

    private boolean isIcc1Unavailable = false;
    private boolean isIcc2Unavailable = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memory_layout);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mQueryHandler = new QueryHandler(getContentResolver());
        initUi();
        startQueryMailboxCount();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }
        return true;
    }

    private void initUi() {
        mPhoneAllView = (TextView) findViewById(R.id.phone);
        mCountView = (TextView) findViewById(R.id.phonecount);
        mMmsUsedView = (TextView) findViewById(R.id.mms_memory_used);
        mElseUsedView = (TextView) findViewById(R.id.else_memory_used);
        mMemoryUnusedView = (TextView) findViewById(R.id.unused_memory);
        mMemoryAllView = (TextView) findViewById(R.id.memory_all);
        mMmsUsedDetail = (TextView) findViewById(R.id.mms_memory_useddetail);
        mElseUsedDetail = (TextView) findViewById(R.id.else_memory_useddetail);
        mMemoryUnusedDetail = (TextView) findViewById(R.id.unused_memory_detail);
        mMemoryAllDetail = (TextView) findViewById(R.id.memory_all_detail);

        String slotOneStr = getString(R.string.sim_card1);
        String slottwoStr = getString(R.string.sim_card2);
        mCardLabel = (TextView) findViewById(R.id.cardlabel);
        mCard1Detail = (TextView) findViewById(R.id.carddetail);
        mCard1Label = (TextView) findViewById(R.id.card);
        mCard2Detail = (TextView) findViewById(R.id.cardtwodetail);
        mCard2Label = (TextView) findViewById(R.id.cardtwo);

        if (!MessageUtils.isMultiSimEnabledMms()) {
            slotOneStr = getString(R.string.sim_card);
            mCard2Detail.setVisibility(View.GONE);
            mCard2Label.setVisibility(View.GONE);
            if (MessageUtils.hasIccCard()) {
                updateState(SHOW_BUSY, MessageUtils.SUB1);
                startQueryIccUsedCount();
            } else {
                mCardLabel.setVisibility(View.GONE);
                mCard1Detail.setVisibility(View.GONE);
                mCard1Label.setVisibility(View.GONE);
                isIcc1Unavailable = true;
            }
        } else {
            if (!MessageUtils.hasIccCard()) {
                mCardLabel.setVisibility(View.GONE);
                mCard1Detail.setVisibility(View.GONE);
                mCard1Label.setVisibility(View.GONE);
                mCard2Detail.setVisibility(View.GONE);
                mCard2Label.setVisibility(View.GONE);
            } else {
                if (MessageUtils.hasIccCard(MessageUtils.SUB1)) {
                    updateState(SHOW_BUSY, MessageUtils.SUB1);
                    startQueryIccUsedCount();
                } else {
                    mCard1Detail.setVisibility(View.GONE);
                    mCard1Label.setVisibility(View.GONE);
                    isIcc1Unavailable = true;
                }

                if (MessageUtils.hasIccCard(MessageUtils.SUB2)) {
                    updateState(SHOW_BUSY, MessageUtils.SUB2);
                    startQueryIcc2UsedCount();
                } else {
                    mCard2Detail.setVisibility(View.GONE);
                    mCard2Label.setVisibility(View.GONE);
                    isIcc2Unavailable = true;
                }
            }
        }

        mCard1Label.setText(slotOneStr);
        mCard2Label.setText(slottwoStr);

        String mmsusedStr = MessageUtils.formatMemorySize(MessageUtils.getMmsUsed(this));
        String elseusedStr = MessageUtils.formatMemorySize(MessageUtils.getStoreUsed()
                - MessageUtils.getMmsUsed(this));
        String unusedStr = MessageUtils.formatMemorySize(MessageUtils.getStoreUnused());
        String allStr = MessageUtils.formatMemorySize(MessageUtils.getStoreAll());

        mMmsUsedDetail.setText(mmsusedStr);
        mElseUsedDetail.setText(elseusedStr);
        mMemoryUnusedDetail.setText(unusedStr);
        mMemoryAllDetail.setText(allStr);
    }

    private void startQueryIccUsedCount() {
        Uri uri = MessageUtils.ICC1_URI;
        if (!MessageUtils.isMultiSimEnabledMms()) {
            uri = MessageUtils.ICC_URI;
        }

        try {
            mQueryHandler.startQuery(ICC_QUERY_ID, null, uri, null, null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startQueryIcc2UsedCount() {
        Uri uri = MessageUtils.ICC2_URI;
        try {
            mQueryHandler.startQuery(ICC2_QUERY_ID, null, uri, null, null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void startQueryMailboxCount() {
        try {
            mQueryHandler.startQuery(MAILBOX_QUERY_ID, null, MAILBOX_URI,
                    null, null, null, null);

        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case MAILBOX_QUERY_ID:
                    int inboxCount = 0;
                    int sentCount = 0;
                    int outboxCount = 0;
                    int draftsCount = 0;
                    int phoneAllCount = 0;

                    if ((cursor != null) && cursor.getCount() >= MAILBOX_COUNT) {
                        if (cursor.moveToFirst()) {
                            inboxCount = cursor.getInt(MAILBOX_COUNT_COLUMN_INDEX);
                            if (cursor.moveToNext()) {
                                sentCount = cursor.getInt(MAILBOX_COUNT_COLUMN_INDEX);
                            }
                            if (cursor.moveToNext()) {
                                outboxCount = cursor.getInt(MAILBOX_COUNT_COLUMN_INDEX);
                            }
                            if (cursor.moveToNext()) {
                                draftsCount = cursor.getInt(MAILBOX_COUNT_COLUMN_INDEX);
                            }
                        }
                    }
                    phoneAllCount = inboxCount + sentCount + outboxCount + draftsCount;

                    String phoneCountStr = getString(R.string.type_inbox) + " : "
                            + Integer.toString(inboxCount) + "\n"
                            + getString(R.string.type_sent) + " : " + Integer.toString(sentCount)
                            + "\n"
                            + getString(R.string.type_outbox) + " : "
                            + Integer.toString(outboxCount) + "\n"
                            + getString(R.string.type_draft) + " : "
                            + Integer.toString(draftsCount) + "\n"
                            + getString(R.string.phonecount) + " : "
                            + Integer.toString(phoneAllCount);

                    mCountView.setText(phoneCountStr);

                    if (cursor != null) {
                        cursor.close();
                    }

                    break;
                case ICC_QUERY_ID:
                    if ((cursor != null) && !cursor.moveToFirst()) {
                        mIccCount = 0;
                        updateState(SHOW_LIST, MessageUtils.SUB1);
                        cursor.close();
                    } else {
                        if (cursor != null) {
                            mIccCount = cursor.getCount();
                            updateState(SHOW_LIST, MessageUtils.SUB1);
                            cursor.close();
                        }
                    }

                    break;
                case ICC2_QUERY_ID:
                    if ((cursor != null) && !cursor.moveToFirst()) {
                        mIcc2Count = 0;
                        updateState(SHOW_LIST, MessageUtils.SUB2);
                        cursor.close();
                    } else {
                        if (cursor != null) {
                            mIcc2Count = cursor.getCount();
                            updateState(SHOW_LIST, MessageUtils.SUB2);
                            cursor.close();
                        }
                    }

                    break;
                default:
                    return;
            }
        }
    }

    private void updateState(int state, int subscription) {
        switch (state) {
            case SHOW_LIST:
                mPhoneAllView.setVisibility(View.VISIBLE);
                mMmsUsedDetail.setVisibility(View.VISIBLE);
                mElseUsedDetail.setVisibility(View.VISIBLE);
                mMemoryUnusedDetail.setVisibility(View.VISIBLE);
                mMemoryAllDetail.setVisibility(View.VISIBLE);
                mCountView.setVisibility(View.VISIBLE);
                mMmsUsedView.setVisibility(View.VISIBLE);
                mElseUsedView.setVisibility(View.VISIBLE);
                mMemoryUnusedView.setVisibility(View.VISIBLE);
                mMemoryAllView.setVisibility(View.VISIBLE);
                mCardLabel.setVisibility(View.VISIBLE);

                int allCount = -1;

                if (MessageUtils.SUB1 == subscription) {
                    mCard1Label.setVisibility(View.VISIBLE);

                    if (!MessageUtils.isMultiSimEnabledMms()) {
                        allCount = SmsManager.getDefault().getSmsCapacityOnIcc();
                    } else {
                        allCount = MSimSmsManager.getDefault().getSmsCapacityOnIcc(subscription);
                    }

                    if (allCount > 0) {
                        String tempStr = Integer.toString(mIccCount) + "/"
                                + Integer.toString(allCount);
                        mCard1Detail.setText(tempStr);
                    } else {
                        mCard1Detail.setText(getString(R.string.please_wait));
                    }

                    isIcc1Unavailable = true;
                } else {
                    mCard2Label.setVisibility(View.VISIBLE);
                    allCount = MSimSmsManager.getDefault().getSmsCapacityOnIcc(subscription);

                    if (allCount > 0) {
                        String tempStr = Integer.toString(mIcc2Count) + "/"
                                + Integer.toString(allCount);
                        mCard2Detail.setText(tempStr);
                    } else {
                        mCard2Detail.setText(getString(R.string.please_wait));
                    }

                    isIcc2Unavailable = true;
                }

                setTitle(getString(R.string.memory_status_title));
                setProgressBarIndeterminateVisibility(false);

                break;

            case SHOW_BUSY:
                mPhoneAllView.setVisibility(View.GONE);
                mMmsUsedDetail.setVisibility(View.GONE);
                mElseUsedDetail.setVisibility(View.GONE);
                mMemoryUnusedDetail.setVisibility(View.GONE);
                mMemoryAllDetail.setVisibility(View.GONE);
                mCountView.setVisibility(View.GONE);
                mCardLabel.setVisibility(View.GONE);
                mMmsUsedView.setVisibility(View.GONE);
                mElseUsedView.setVisibility(View.GONE);
                mMemoryUnusedView.setVisibility(View.GONE);
                mMemoryAllView.setVisibility(View.GONE);
                mCard1Label.setVisibility(View.GONE);
                mCard2Label.setVisibility(View.GONE);

                isIcc1Unavailable = false;
                isIcc2Unavailable = false;

                setTitle(getString(R.string.refreshing));
                setProgressBarIndeterminateVisibility(true);
                break;
            default:
                Log.e(TAG, "Invalid State");
        }
    }
}
