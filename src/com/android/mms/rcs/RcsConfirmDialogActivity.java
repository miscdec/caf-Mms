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
import java.util.Iterator;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.google.android.mms.MmsException;
import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.mway.rcs.client.aidl.constant.Constants.MessageConstants;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.rcs.ui.common.mms.RcsMessageForwardToSmsCache;
import com.suntek.rcs.ui.common.RcsLog;


public class RcsConfirmDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setFinishOnTouchOutside(false);
        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        showPolicyDialog();
    }


    private void showPolicyDialog() {
        if (RcsMessageForwardToSmsCache.getInstance().getCacheMessage().size() > 1) {
            finish();
            return;
        }
        AlertDialog alert = new AlertDialog.Builder(this)
                .setMessage(R.string.rcs_message_send_fail_send_by_policy)
                .setPositiveButton(R.string.send_confirm_ok, new SendListener())
                .setNegativeButton(R.string.set_poliy, new GoSettingListener())
                .create();
        alert.setCanceledOnTouchOutside(false);
        alert.setCancelable(false);
        alert.show();
    }

    private class SendListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            try {
                MessageApi.getInstance().setSendPolicy(
                        MessageConstants.CONST_SEND_POLICY_FORWARD_SMS);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (ServiceDisconnectedException e) {
                e.printStackTrace();
            }
            sendMessage();
            dialog.dismiss();
            finish();
        }
    }

    private class GoSettingListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            Intent intent = new Intent(RcsConfirmDialogActivity.this,
                    MessagingPreferenceActivity.class);
            updateMessageState();
            startActivity(intent);
            RcsMessageForwardToSmsCache.getInstance().clearCacheMessage();
            dialog.dismiss();
            finish();
        }
    }

    private void deleteMessage(long id) {
        Context context = MmsApp.getApplication().getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(Sms.CONTENT_URI, Sms._ID + " = " + String.valueOf(id), null);
    }

    private void updateMessageState() {
        Iterator<Long[]> iter = RcsMessageForwardToSmsCache.getInstance().getChachIterator();
        while (iter.hasNext()) {
            Long[] key = iter.next();
            Context context = MmsApp.getApplication().getApplicationContext();
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(RcsColumns.SmsRcsColumns.RCS_MSG_STATE,
                    MessageConstants.CONST_STATUS_SEND_FAIL);
            resolver.update(Sms.CONTENT_URI, values, Sms._ID + " = " +
                    String.valueOf(key[0]), null);
        }
    }

    private void sendMessage() {
        try {
            Context context = MmsApp.getApplication().getApplicationContext();
            Iterator<Long[]> iter = RcsMessageForwardToSmsCache
                    .getInstance().getChachIterator();
            while (iter.hasNext()) {
                Long[] key = iter.next();
                String[] value = RcsMessageForwardToSmsCache
                        .getInstance().getCacheVaule(key);
                String[] numberList = value[0].split(",");
                MessageSender sender = new SmsMessageSender(context,
                        numberList, value[1], key[1], 0/*Integer.valueOf(value[2])*/);
                sender.sendMessage((long)key[1]);
                deleteMessage(key[0]);
            }
        } catch (Throwable e) {
            RcsLog.w(e);
        }
        RcsMessageForwardToSmsCache.getInstance().clearCacheMessage();
    }
}
