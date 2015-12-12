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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.telephony.SubscriptionManager;

import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.internal.telephony.IExtTelephony;

import com.suntek.mway.rcs.client.api.support.SupportApi;
import com.suntek.rcs.ui.common.RcsLog;


import java.io.UnsupportedEncodingException;

public class RcsDualSimMananger {

    // default sending sms slot ID.
    public static final int DEFAULT_SENDING_SIM_SLOT1 = 0;

    public static final int DEFAULT_SENDING_SIM_SLOT2 =1;

    public static final int SMS_PROMPT_ENABLED = 2;

    // rcs attachment state
    public static final int RCS_ATTACHMENT = 0;

    public static final int DEFAULT_ATTACHMENT = 1;

    public static final int RCS_OFFLINE_AND_RCS_MESSAGE_ONLY = 2;

    public static final String ENABLE_RCS_MESSAGE_POLICY =
            "pref_key_rcs_immediately_message_policy";

    public static final String DEFAULT_SENDING_CONFIRM_VALUE =
            "default_sending_confirm_value";

    public static final String CONST_ENCODING_UTF8 = "UTF-8";
    /**
     * @return
     * 0 default sending sim is slot 1.
     * 1 default sending sim is slot 2.
     * 2 SMS prompt enalbed.
     */
    private static int getSendingDefaultSim() {
        IExtTelephony mExtTelephony = IExtTelephony.Stub.asInterface(ServiceManager
                .getService("extphone"));
        try {
            if (mExtTelephony.isSMSPromptEnabled()) {
                return SMS_PROMPT_ENABLED;
            } else {
                int subId = SubscriptionManager.getDefaultSmsSubId();
                int phoneId = SubscriptionManager.getPhoneId(subId);
                return phoneId;
            }
        } catch (NullPointerException ex) {
        } catch (RemoteException ex) {
        }
        return 0;
    }

    public static int getCurrentRcsOnlineSlot() {
        SupportApi supportApi = SupportApi.getInstance();
        int defaultDataSlot = supportApi.getDefaultDataSlotId();
        boolean isDdfaultDataSlotOnline = supportApi.isRcsSupported(defaultDataSlot);
        if (isDdfaultDataSlotOnline) {
            return defaultDataSlot;
        } else {
            return -1;
        }
    }

    public static boolean getUserIsUseRcsPolicy(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(ENABLE_RCS_MESSAGE_POLICY, true);
    }

    public static boolean getDefaultSendingConfirmValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(DEFAULT_SENDING_CONFIRM_VALUE, false);
    }

    public static void setDefaultSendingConfirmValue(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(DEFAULT_SENDING_CONFIRM_VALUE, true);
        editor.apply();
    }

    public static boolean shouldSendMessageWithRcsPolicy(Context context,int defaultSubId,
            boolean requiresMms, boolean isGroupChat) {
        if (!RcsUtils.isRcsOnline()) {
            return false;
        }
        if (isGroupChat) {
            return getUserIsUseRcsPolicy(context);
        }
        int phoneId = SubscriptionManager.getPhoneId(defaultSubId);
        int rcsOnlineSlot = getCurrentRcsOnlineSlot();
        if (phoneId == rcsOnlineSlot) {
            if (requiresMms) {
                return false;
            }
            return getUserIsUseRcsPolicy(context);
        }
        return false;
    }

    /**
     * @return
     * 0 should require rcs attachment.
     * 1 should require default attatchment.
     * 2 rcs offlie and only rcs message, do not support attachment.
     */
    public static int getAttachmentState(Context context) {
        int result = DEFAULT_ATTACHMENT;
        int defaultSlot = getSendingDefaultSim();
        int registeredSlot = getCurrentRcsOnlineSlot();
        switch (defaultSlot) {
            case DEFAULT_SENDING_SIM_SLOT1:
            case DEFAULT_SENDING_SIM_SLOT2: {
                if (defaultSlot != registeredSlot) {
                    result = RcsDualSimMananger.DEFAULT_ATTACHMENT;
                    break;
                }
                if (!getUserIsUseRcsPolicy(context)) {
                    result = DEFAULT_ATTACHMENT;
                } else {
                    if (RcsUtils.isRcsOnline()) {
                        result = RCS_ATTACHMENT;
                    } else {
                        result = DEFAULT_ATTACHMENT;
                    }
                }
                break;
            }
            case SMS_PROMPT_ENABLED: {
                if (!getUserIsUseRcsPolicy(context)) {
                    result = DEFAULT_ATTACHMENT;
                } else {
                    if (RcsUtils.isRcsOnline()) {
                        result = RCS_ATTACHMENT;
                    } else {
                        result = DEFAULT_ATTACHMENT;
                    }
                }
                break;
            }
            default:
                break;
        }
        return result;
    }

    public static boolean isContentExceedLimit(String content, int limit) {
        try {
            content = Base64.encodeToString(content.getBytes(CONST_ENCODING_UTF8), Base64.NO_WRAP);
            if (content.length() <= limit) {
                return false;
            } else {
                return true;
            }
        } catch (UnsupportedEncodingException e) {
            RcsLog.e("decode error", e);
            return false;
        }

    }
}
