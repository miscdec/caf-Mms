/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.mms.rcs;

import com.android.mms.R;
import com.android.mms.RcsApiManager;
import com.android.mms.transaction.MessagingNotification;
import com.suntek.mway.rcs.client.aidl.constant.BroadcastConstants;
import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatModel;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Looper;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RcsMessageStatusService extends IntentService {
    public static final String ACTION_UI_SHOW_GROUP_MESSAGE_NOTIFY = "ACTION_UI_SHOW_GROUP_MESSAGE_NOTIFY";
    private static ThreadPoolExecutor pool;
    private static final int NUMBER_OF_CORES; // Number of cores.
    private static final int MAXIMUM_POOL_SIZE; // Max size of the thread pool.
    private static int runningCount = 0;
    private static int runningId = 0;
    private static long taskCount = 0;
    private TelephonyManager mTeleManager;

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
        Log.w("RCS_UI", "onStartCommand: taskCount=" + taskCount);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final int currentRunningId = ++runningId;
        mTeleManager = (TelephonyManager) RcsMessageStatusService.this.getSystemService(TELEPHONY_SERVICE);
        pool.execute(new Runnable() {
            public void run() {
                runningCount++;
                Log.w("RCS_UI", "runningId=" + currentRunningId + ", countOfRunning="
                        + runningCount + ", taskCount=" + taskCount + ", Begin");

                String action = intent.getAction();
                RcsUtils.dumpIntent(intent);
                if (BroadcastConstants.UI_MESSAGE_ADD_DATABASE.equals(action)) {
                    final ChatMessage chatMessage = intent.getParcelableExtra("chatMessage");
                    if(chatMessage.getChatType() == SuntekMessageData.CHAT_TYPE_PUBLIC)
                        return;

                    long threadId = copyRcsMsgToSmsProvider(
                            RcsMessageStatusService.this, chatMessage);

                    boolean notify;
                    int chatType = chatMessage.getChatType();
                    int sendReceive = chatMessage.getSendReceive();
                    if (chatMessage == null) {
                        notify = false;
                    } else if (chatType == SuntekMessageData.CHAT_TYPE_PUBLIC) {
                        notify = false;
                    } else {
                        notify = ((chatType != SuntekMessageData.CHAT_TYPE_GROUP) && (sendReceive == SuntekMessageData.MSG_RECEIVE));
                    }
                    if (notify) {
                        MessagingNotification.blockingUpdateNewMessageIndicator(
                                RcsMessageStatusService.this, threadId, true);
                    }
                    if((chatType == SuntekMessageData.CHAT_TYPE_GROUP) && (sendReceive == SuntekMessageData.MSG_RECEIVE)&&          chatMessage.getMsgType() != SuntekMessageData.MSG_TYPE_NOTIFICATION){
                        Intent groupNotifyIntent = new Intent(ACTION_UI_SHOW_GROUP_MESSAGE_NOTIFY);
                        groupNotifyIntent.putExtra("id", (long)chatMessage.getId());
                        groupNotifyIntent.putExtra("threadId", chatMessage.getThreadId());
                        sendBroadcast(groupNotifyIntent);
                    }
                } else if (ACTION_UI_SHOW_GROUP_MESSAGE_NOTIFY.equals(action)) {
                    long id = intent.getLongExtra("id", 0);
                    long rcsThreadId = intent.getLongExtra("threadId", -1);
                    MessageApi messageApi = RcsApiManager.getMessageApi();
                    long threadId = RcsUtils.getThreadIdByRcsMesssageId(
                            RcsMessageStatusService.this, id);
                    try {
                        GroupChatModel model = messageApi.getGroupChatByThreadId(rcsThreadId);
                        if (model != null) {
                            int msgNotifyType = model.getRemindPolicy();
                            if (msgNotifyType == 0) {
                                MessagingNotification.blockingUpdateNewMessageIndicator(
                                        RcsMessageStatusService.this, threadId, true);
                            } else {
                                MessagingNotification.blockingUpdateNewMessageIndicator(
                                        RcsMessageStatusService.this, threadId, false);
                            }
                        }
                    } catch (ServiceDisconnectedException e) {
                        Log.i("RCS_UI", "GroupChatMessage" + e);
                    }
                } else if (BroadcastConstants.UI_MESSAGE_STATUS_CHANGE_NOTIFY.equals(action)) {
                    String id = intent.getStringExtra("id");
                    int status = intent.getIntExtra("status", -11);
                    Log.i("RCS_UI", "com.suntek.mway.rcs.ACTION_UI_MESSAGE_STATUS_CHANGE_NOTIFY"
                            + id + status);
                    RcsUtils.updateState(RcsMessageStatusService.this, id, status);
                    RcsNotifyManager.sendMessageFailNotif(RcsMessageStatusService.this,
                            status, id, true);
                } else if (BroadcastConstants.UI_DOWNLOADING_FILE_CHANGE.equals(action)) {
                    String rcs_message_id = intent
                            .getStringExtra(BroadcastConstants.BC_VAR_TRANSFER_PRG_MESSAGE_ID);
                    long start = intent.getLongExtra(BroadcastConstants.BC_VAR_TRANSFER_PRG_START,
                            -1);
                    long end = intent.getLongExtra(BroadcastConstants.BC_VAR_TRANSFER_PRG_END, -1);
                    if (start == end) {
                        RcsUtils.updateFileDownloadState(RcsMessageStatusService.this,
                                rcs_message_id);
                    }
                } else if (BroadcastConstants.UI_SHOW_RECV_REPORT_INFO.equals(action)) {
                    String id = intent.getStringExtra("messageId");
                    String statusString = intent.getStringExtra("status");
                    int status = 99;
                    if ("delivered".equals(statusString)) {
                        status = 99;
                    } else if ("displayed".equals(statusString)) {
                        status = 100;
                    }
                    String number = intent.getStringExtra("original-recipient");
                    RcsUtils.updateManyState(RcsMessageStatusService.this, id, number, status);
                } else if("com.suntek.mway.rcs.ACTION_UI_MESSAGE_TRANSFER_SMS".equals(action)){
                    Log.i("RCS_UI","rcs message to sms="+action);
                    long messageId = intent.getLongExtra("id",-1);
                    RcsUtils.deleteMessageById(RcsMessageStatusService.this, messageId);
                } else if("android.intent.action.SIM_STATE_CHANGED".equals(action)){
                    if(TelephonyManager.SIM_STATE_ABSENT == mTeleManager.getSimState()){
                        Looper.prepare();
                        Toast.makeText(RcsMessageStatusService.this, "burn all", 0).show();
                        Looper.loop();
                        try {
                         RcsApiManager.getMessageApi().burnAllMsgAtOnce();
                         RcsUtils.burnAllMessageAtLocal(RcsMessageStatusService.this);
                     } catch (ServiceDisconnectedException e) {
                         e.printStackTrace();
                     }
                    }
                }

                Log.w("RCS_UI", "runningId=" + currentRunningId + ", countOfRunning="
                        + runningCount + ", taskCount=" + taskCount + ", End");
                runningCount--;
                taskCount--;
            };
        });
    }

    public static long copyRcsMsgToSmsProvider(Context context, ChatMessage chatMessage) {
        try {
            return RcsUtils.rcsInsert(context, chatMessage);
        } catch (ServiceDisconnectedException e) {
            Log.w("RCS_UI", e);
            return 0;
        }
    }
}
