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

import com.suntek.mway.rcs.client.aidl.constant.BroadcastConstants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GroupChatManagerReceiver extends BroadcastReceiver {

    private GroupChatNotifyCallback callback;

    public GroupChatManagerReceiver(GroupChatNotifyCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context arg0, Intent intent) {
        if (BroadcastConstants.UI_GROUP_MANAGE_NOTIFY.equals(intent.getAction())) {
            String groupId = intent
                    .getStringExtra(BroadcastConstants.BC_VAR_MSG_GROUP_ID);
            String actionType = intent.getStringExtra(BroadcastConstants.BC_VAR_MSG_ACTION_TYPE);
            if (BroadcastConstants.ACTION_TYPE_CREATE.equals(actionType)) {
                String newSubject = intent.getStringExtra(BroadcastConstants.BC_VAR_GROUP_SUBJECT);
                if (callback != null) {
                    callback.onNewSubject(groupId, newSubject);
                }
            } else if (BroadcastConstants.ACTION_TYPE_UPDATE_ALIAS.equals(actionType)) {
                if (callback != null) {
                    callback.onMemberAliasChange(groupId);
                }
            } else if (BroadcastConstants.ACTION_TYPE_DELETED.equals(actionType)) {
                if (callback != null) {
                    callback.onDisband(groupId);
                }
            }
        }
    }

    public interface GroupChatNotifyCallback {
        void onNewSubject(String groupId, String subject);

        void onMemberAliasChange(String groupId);

        void onDisband(String groupId);
    }

}
