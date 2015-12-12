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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.ui.MessageListAdapter;
import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.rcs.ui.common.RcsLog;
import com.suntek.rcs.ui.common.mms.RcsFileTransferCache;

import java.util.HashMap;
import java.util.Iterator;

public class ComposeMessageFileTransferReceiver extends BroadcastReceiver {

    private MessageListAdapter mMsgListAdapter;

    public ComposeMessageFileTransferReceiver(MessageListAdapter msgListAdapter) {
        this.mMsgListAdapter = msgListAdapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = null;
        if (MmsConfig.isRcsVersion()) {
            action = intent.getAction();
        } else {
            return;
        }
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (!gprs.isConnected() && !wifi.isConnected()) {
                new updateFileDownloadTask(context, mMsgListAdapter).execute();
                return;
            }
        } else if (Actions.ACTION_ERROR.equals(action)) {
            int errorType = intent.getIntExtra(Parameter.EXTRA_ERROR_EVENT, 0);
            if (errorType == Constants.CONST_ERROR_EVENT_DOWNLOAD_FILE) {
                new updateFileDownloadTask(context, mMsgListAdapter).execute();
                return;
            }
        } else if (Actions.MessageAction.ACTION_MESSAGE_FILE_TRANSFER_PROGRESS.equals(action)) {
            long notifyMessageId = intent.getLongExtra(Parameter.EXTRA_ID, -1);
            long currentSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_CURRENT_SIZE, -1);
            long totalSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_TOTAL_SIZE, -1);
            if (notifyMessageId > 0 && currentSize == totalSize) {
                RcsFileTransferCache.getInstance().removeFileTransferPercent(notifyMessageId);
                if (RcsUtils.queryRcsMsgDownLoadState(context, notifyMessageId) !=
                        RcsUtils.RCS_IS_DOWNLOAD_OK){
                    RcsUtils.updateFileDownloadState(context, notifyMessageId,
                            RcsUtils.RCS_IS_DOWNLOAD_OK);
                }
                mMsgListAdapter.notifyDataSetChanged();
                return;
            }
            if (totalSize > 0) {
                long temp = currentSize * 100 / totalSize;
                if (temp == 100) {
                    RcsFileTransferCache.getInstance().removeFileTransferPercent(notifyMessageId);
                    if (RcsUtils.queryRcsMsgDownLoadState(context, notifyMessageId) !=
                            RcsUtils.RCS_IS_DOWNLOAD_OK){
                        RcsUtils.updateFileDownloadState(context, notifyMessageId,
                                RcsUtils.RCS_IS_DOWNLOAD_OK);
                    }
                    return;
                }
                if (notifyMessageId > 0 && currentSize < totalSize) {
                    RcsFileTransferCache.getInstance()
                            .addFileTransferPercent(notifyMessageId, Long.valueOf(temp));
                    if (RcsUtils.queryRcsMsgDownLoadState(context, notifyMessageId) !=
                            RcsUtils.RCS_IS_DOWNLOADING){
                        RcsUtils.updateFileDownloadState(context, notifyMessageId,
                                RcsUtils.RCS_IS_DOWNLOADING);
                    }
                    mMsgListAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private class updateFileDownloadTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        private MessageListAdapter mMessageListAdapter;

        public updateFileDownloadTask(Context context, MessageListAdapter msgListAdapter) {
            mMessageListAdapter = msgListAdapter;
            mContext = MmsApp.getApplication().getApplicationContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Iterator<Long> iter = RcsFileTransferCache.getInstance()
                    .getFileTransferPercentKeys();
            while (iter.hasNext()) {
                long key = iter.next();
                RcsUtils.updateFileDownloadState(mContext, key);
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            if (mMessageListAdapter != null) {
                mMessageListAdapter.notifyDataSetChanged();
            }
        }
    }
}

