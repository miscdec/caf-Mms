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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.MmsConfig;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageListAdapter;
import com.android.mms.ui.MessageListItem;
import com.android.mms.R;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.rcs.ui.common.mms.RcsFileTransferCache;

import java.util.HashMap;

public class ComposeMessageCloudFileReceiver extends BroadcastReceiver {

    private MessageListAdapter mMsgListAdapter;
    private ListView mListView;

    public ComposeMessageCloudFileReceiver(MessageListAdapter msgListAdapter,
            ListView listView) {
        this.mMsgListAdapter = msgListAdapter;
        this.mListView = listView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!MmsConfig.isRcsVersion()) {
            return;
        }
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        long messageId = intent.getLongExtra(
                Parameter.EXTRA_MCLOUD_CHATMESSAGE_ID, -1);
        NetworkInfo gprs = manager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!gprs.isConnected() && !wifi.isConnected()) {
            if (RcsUtils.queryRcsMsgDownLoadState(context, messageId)
                    != RcsUtils.RCS_IS_DOWNLOAD_FAIL) {
                RcsUtils.updateFileDownloadState(context, messageId,
                        RcsUtils.RCS_IS_DOWNLOAD_FAIL);
            }
            mMsgListAdapter.notifyDataSetChanged();
            return;
        }
        String eventType = intent.getStringExtra(Parameter.EXTRA_MCLOUD_ENENTTYPE);
        if (!TextUtils.isEmpty(eventType) && eventType.
                equals(Constants.PluginConstants.CONST_MCLOUD_EVENT_ERROR)) {
            Toast.makeText(context, R.string.download_mcloud_file_fail,
                    Toast.LENGTH_SHORT).show();
            RcsFileTransferCache.getInstance().removeFileTransferPercent(messageId);
            if (RcsUtils.queryRcsMsgDownLoadState(context, messageId)
                    != RcsUtils.RCS_IS_DOWNLOAD_FAIL) {
                RcsUtils.updateFileDownloadState(context, messageId,
                        RcsUtils.RCS_IS_DOWNLOAD_FAIL);
            }
            mMsgListAdapter.notifyDataSetChanged();
        } else if (!TextUtils.isEmpty(eventType) && eventType
                .equals(Constants.PluginConstants.CONST_MCLOUD_EVENT_PROGRESS)) {
            float process = (int) intent.getLongExtra(Parameter.EXTRA_MCLOUD_PROCESS_SIZE, 0);
            float total = (int) intent.getLongExtra(Parameter.EXTRA_MCLOUD_TOTAL_SIZE, 0);
            long percent = (long) ((process / total) * 100);
            RcsFileTransferCache.getInstance().addFileTransferPercent(messageId, percent);
            if (RcsUtils.queryRcsMsgDownLoadState(context, messageId) !=
                    RcsUtils.RCS_IS_DOWNLOADING){
                RcsUtils.updateFileDownloadState(context, messageId,
                        RcsUtils.RCS_IS_DOWNLOADING);
            }
            mMsgListAdapter.notifyDataSetChanged();
        } else if (!TextUtils.isEmpty(eventType) && eventType
                .equals(Constants.PluginConstants.CONST_MCLOUD_EVENT_SUCCESS)) {
            RcsFileTransferCache.getInstance().removeFileTransferPercent(messageId);
            if (RcsUtils.queryRcsMsgDownLoadState(context, messageId) !=
                    RcsUtils.RCS_IS_DOWNLOAD_OK){
                RcsUtils.updateFileDownloadState(context, messageId,
                        RcsUtils.RCS_IS_DOWNLOAD_OK);
            }
            mMsgListAdapter.notifyDataSetChanged();
        } else if(!TextUtils.isEmpty(eventType) && eventType
                .equals(Constants.PluginConstants.CONST_MCLOUD_EVENT_FILE_TOO_LARGE)){
          Toast.makeText(context,R.string.file_is_too_larger,
                  Toast.LENGTH_LONG).show();
        } else if(!TextUtils.isEmpty(eventType) && eventType.
                equals(Constants.PluginConstants.CONST_MCLOUD_EVENT_SUFFIX_NOT_ALLOWED)){
            Toast.makeText(context,R.string.name_not_fix,
                    Toast.LENGTH_LONG).show();
        }
    }

}
