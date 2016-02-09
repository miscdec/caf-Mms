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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.android.mms.R;
import com.android.mms.MmsApp;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.util.Recycler;
import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatApi;
import com.suntek.rcs.ui.common.mms.RcsFileTransferCache;
import com.suntek.rcs.ui.common.mms.RcsMessageForwardToSmsCache;
import com.suntek.rcs.ui.common.RcsLog;

public class RcsMessageStatusService extends IntentService {

    private static ThreadPoolExecutor pool;
    private static final int NUMBER_OF_CORES; // Number of cores.
    private static final int MAXIMUM_POOL_SIZE; // Max size of the thread pool.
    private static int runningCount = 0;
    private static int runningId = 0;
    private static long taskCount = 0;
    private static long DEFAULT_THREAD_ID = -1;
    private static long DEFAULT_THREAD_SIZE = -1;
    private static int DEFAULT_STATUS = -11;
    private static int DEFAULT_SILENCE = 0;
    private static int RCS_SILENCE_STATUS = 1;
    private static final String ACTION_START_DIALOG =
            "com.suntek.rcs.action.ACTION_LUNCHER_CONFRMDIALOG";

    static {
        NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        MAXIMUM_POOL_SIZE = Math.max(NUMBER_OF_CORES, 16);

        pool = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                MAXIMUM_POOL_SIZE,
                1,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public RcsMessageStatusService() {
        // Class name will be the thread name.
        super(RcsMessageStatusService.class.getName());

        // Intent should be redelivered if the process gets killed before
        // completing the job.
        setIntentRedelivery(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        taskCount++;
        RcsLog.i("RcsMessageStatusService.onStartCommand: taskCount=" + taskCount);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final int currentRunningId = ++runningId;

        String action = intent.getAction();
        if (Actions.MessageAction.ACTION_MESSAGE_NOTIFY.equals(action)) {
            long threadId = intent.getLongExtra(Parameter.EXTRA_THREAD_ID, DEFAULT_THREAD_ID);
            int silence = intent.getIntExtra(Parameter.EXTRA_SILENCE, DEFAULT_SILENCE);
            if (silence != RCS_SILENCE_STATUS) {
                disposeGroupChatNewMessage(threadId);
            }
            Recycler.getSmsRecycler().deleteOldMessagesByThreadId(RcsMessageStatusService.this,
                    threadId);
        } else if (Actions.MessageAction.ACTION_MESSAGE_STATUS_CHANGED.equals(action)) {
            long dataId = intent.getLongExtra(Parameter.EXTRA_ID, DEFAULT_THREAD_ID);
            long id = intent.getLongExtra(Parameter.EXTRA_THREAD_ID, DEFAULT_THREAD_ID);
            int status = intent.getIntExtra(Parameter.EXTRA_STATUS, DEFAULT_STATUS);
            if (status == RcsUtils.MESSAGE_FAIL) {
                RcsUtils.updateFaildMessageType(MmsApp.getApplication(), dataId);
                if (MessagingNotification.getCurrentlyDisplayedThreadId() != id) {
                    RcsNotifyManager
                            .sendMessageFailNotif(MmsApp.getApplication(), status, id, true);
                }
            }
        } else if (Actions.MessageAction.ACTION_MESSAGE_SMS_POLICY_NOT_SET.equals(action)) {
            long messageId = intent.getLongExtra(Parameter.EXTRA_ID, -1);
            long threadId = intent.getLongExtra(Parameter.EXTRA_THREAD_ID, -1);
            int phoneId = intent.getIntExtra("phoneId", -1);
            String numbers = intent.getStringExtra(Parameter.EXTRA_NUMBER);
            String content = intent.getStringExtra(Parameter.EXTRA_CONTENT);
            RcsMessageForwardToSmsCache.getInstance().addSendMessage(
                    new Long[]{messageId, threadId},
                    new String[] {numbers, content, String.valueOf(phoneId)});
            Intent startIntent = new Intent();
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.setAction(ACTION_START_DIALOG);
            startActivity(startIntent);
        }
        pool.execute(new Runnable() {
            public void run() {
                runningCount++;
                RcsLog.i("pool.execute: runningId=" + currentRunningId + "," + " countOfRunning="
                        + runningCount + ", taskCount=" + taskCount + ", Begin");

                String action = intent.getAction();
                if (Actions.MessageAction.ACTION_MESSAGE_FILE_TRANSFER_PROGRESS.equals(action)) {
                    long msgId = intent.getLongExtra(Parameter.EXTRA_ID, DEFAULT_THREAD_ID);
                    long currentSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_CURRENT_SIZE,
                            DEFAULT_THREAD_SIZE);
                    long totalSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_TOTAL_SIZE,
                            DEFAULT_THREAD_SIZE);
                    long temp = currentSize * 100 / totalSize;
                    if (totalSize > 0 && currentSize == totalSize || temp == 100) {
                        RcsFileTransferCache.getInstance().removeFileTransferPercent(msgId);
                        RcsUtils.updateFileDownloadState(RcsMessageStatusService.this,
                                msgId, RcsUtils.RCS_IS_DOWNLOAD_OK);
                    } else {
                        RcsFileTransferCache.getInstance()
                                .addFileTransferPercent(msgId, Long.valueOf(temp));
                    }
                }

                RcsLog.i("pool.execute: runningId=" + currentRunningId + ", countOfRunning="
                        + runningCount + ", taskCount=" + taskCount + ", End");
                runningCount--;
                taskCount--;
            };
        });
    }

    private void notifyNewMessage(long threadId) {
        if (threadId != DEFAULT_THREAD_ID && threadId != MessagingNotification
                .getCurrentlyDisplayedThreadId() && !RcsChatMessageUtils
                .isPublicAccountMessage(RcsMessageStatusService.this, threadId)) {
            MessagingNotification.blockingUpdateNewMessageIndicator(
                    RcsMessageStatusService.this, threadId, true);
        }
    }

    private void disposeGroupChatNewMessage(long threadId){
        GroupChatApi groupChatApi = GroupChatApi.getInstance();
        try {
            GroupChat model = groupChatApi.getGroupChatByThreadId(threadId);
            if (model != null) {
                int msgNotifyType = model.getPolicy();
                if (msgNotifyType == GroupChat.MESSAGE_RECEIVE_AND_REMIND
                        && threadId != MessagingNotification.getCurrentlyDisplayedThreadId()) {
                    MessagingNotification.blockingUpdateNewMessageIndicator(
                                RcsMessageStatusService.this, threadId, true);
                }
            } else {
                notifyNewMessage(threadId);
            }
        } catch (ServiceDisconnectedException e) {
            RcsLog.e(e);
        } catch (RemoteException e) {
            RcsLog.e(e);
        }
    }
}
