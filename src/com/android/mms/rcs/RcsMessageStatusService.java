/*
 * Copyright (c) 2014 pci-suntektech Technologies, Inc.  All Rights Reserved.
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

import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatApi;
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
        if (Actions.MessageAction.ACTION_MESSAGE_NOTIFY.equals(action)){
            long threadId = intent.getLongExtra(Parameter.EXTRA_THREAD_ID, DEFAULT_THREAD_ID);
            disposeGroupChatNewMessage(threadId);
        } else if (Actions.MessageAction.ACTION_MESSAGE_STATUS_CHANGED.equals(action)) {
            long id = intent.getLongExtra(Parameter.EXTRA_THREAD_ID, DEFAULT_THREAD_ID);
            int status = intent.getIntExtra(Parameter.EXTRA_STATUS, DEFAULT_STATUS);
            if (status == RcsUtils.MESSAGE_FAIL) {
                RcsNotifyManager.sendMessageFailNotif(MmsApp.getApplication(), status, id, true);
            }
        }
        pool.execute(new Runnable() {
            public void run() {
                runningCount++;
                RcsLog.i("pool.execute: runningId=" + currentRunningId + ", countOfRunning=" + runningCount
                        + ", taskCount=" + taskCount + ", Begin");

                String action = intent.getAction();
                if (Actions.MessageAction.ACTION_MESSAGE_FILE_TRANSFER_PROGRESS.equals(action)) {
                    long msgId = intent.getLongExtra(Parameter.EXTRA_ID, DEFAULT_THREAD_ID);
                    long currentSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_CURRENT_SIZE,
                            DEFAULT_THREAD_SIZE);
                    long totalSize = intent.getLongExtra(Parameter.EXTRA_TRANSFER_TOTAL_SIZE,
                            DEFAULT_THREAD_SIZE);
                    if (totalSize > 0 && currentSize == totalSize) {
                        RcsUtils.updateFileDownloadState(RcsMessageStatusService.this,
                                msgId, RcsUtils.RCS_IS_DOWNLOAD_OK);
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
                .getCurrentlyDisplayedThreadId()) {
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
