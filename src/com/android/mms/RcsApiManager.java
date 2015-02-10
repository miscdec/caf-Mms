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

package com.android.mms;

import com.suntek.mway.rcs.client.api.RCSServiceListener;
import com.suntek.mway.rcs.client.api.autoconfig.RcsAccountApi;
import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.api.impl.groupchat.ConfApi;
import com.suntek.mway.rcs.client.api.mcloud.McloudFileApi;
import com.suntek.mway.rcs.client.api.support.RcsSupportApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.capability.impl.CapabilityApi;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class RcsApiManager {
    private static boolean mIsRcsServiceInstalled;

    private static ConfApi mConfApi = new ConfApi();
    private static MessageApi mMessageApi = new MessageApi();
    private static RcsAccountApi mRcsAccountApi = new RcsAccountApi();
    private static CapabilityApi mCapabilityApi = new CapabilityApi();
    private static McloudFileApi mMcloudFileApi = new McloudFileApi();

    public static void init(Context context) {
        mIsRcsServiceInstalled = RcsSupportApi.isRcsServiceInstalled(context);
        if (!mIsRcsServiceInstalled) {
            return;
        }

        mMessageApi.init(context, new RCSServiceListener() {
            @Override
            public void onServiceDisconnected() throws RemoteException {
                Log.d("RCS_UI", "MessageApi disconnected");
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d("RCS_UI", "MessageApi connected");
            }
        });

        mRcsAccountApi.init(context, new RCSServiceListener() {
            @Override
            public void onServiceDisconnected() throws RemoteException {
                Log.d("RCS_UI", "RcsAccountApi disconnected");
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d("RCS_UI", "RcsAccountApi connected");
            }
        });

        mConfApi.init(context, new RCSServiceListener() {
            public void onServiceDisconnected() throws RemoteException {
                Log.d("RCS_UI", "ConfApi connected");
            }

            public void onServiceConnected() throws RemoteException {
                Log.d("RCS_UI", "ConfApi connected");
            }
        });

        mCapabilityApi.init(context, null);
        mMcloudFileApi.init(context,null);
    }
    public static McloudFileApi getMcloudFileApi(){
        return mMcloudFileApi;
    }
    public static MessageApi getMessageApi() {
        return mMessageApi;
    }

    public static RcsAccountApi getRcsAccountApi() {
        return mRcsAccountApi;
    }

    public static ConfApi getConfApi() {
        return mConfApi;
    }

    public static boolean isRcsServiceInstalled() {
        return mIsRcsServiceInstalled;
    }

    public static boolean isRcsOnline() {
        try {
            return mRcsAccountApi.isOnline();
        } catch (ServiceDisconnectedException e) {
            return false;
        }
    }

    public static CapabilityApi getCapabilityApi() {
        return mCapabilityApi;
    }
}
