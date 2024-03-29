/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.ui.MessageUtils;
import com.android.mmswrapper.SubscriptionManagerWrapper;
import com.android.mmswrapper.ConstantsWrapper;
import com.android.mmswrapper.TelephonyManagerWrapper;
import com.android.mmswrapper.compat.PhoneUtils;

import android.content.res.Configuration;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import android.os.Process;
import android.os.UserHandle;
import android.os.Binder;
import  android.app.role.RoleManager;

public class MmsConfig {
    private static final String TAG = LogTag.TAG;
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = false;

    private static final String DEFAULT_HTTP_KEY_X_WAP_PROFILE = "x-wap-profile";
    private static final String DEFAULT_USER_AGENT = "Android-Mms/2.0";

    private static final String MMS_APP_PACKAGE = "com.android.mms";

    private static final String SMS_PROMO_DISMISSED_KEY = "sms_promo_dismissed_key";

    private static final int MAX_IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGE_WIDTH = 640;
    private static final int MAX_TEXT_LENGTH = 2000;

    public static final int CREATIONMODE_RESTRICTED = 1;
    public static final int CREATIONMODE_WARNING = 2;
    public static final int CREATIONMODE_FREE = 3;

    public static final String EXTRA_URI = "uri";
    public static final String KILO_BYTE = "KB";
    public static final String MEGA_BYTE = "MB";
    public static final int KB_IN_BYTES = 1024;
    public static final float DEFAULT_FONT_SIZE = 14.0f;

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean mTransIdEnabled = false;
    private static int mMmsEnabled = 1;                         // default to true
    private static int mMaxMessageSize = 300 * 1024;            // default to 300k max size
    private static int mMaxRestrictedMessageSize = 600 * 1024;// restricted mode message size 600k
    private static String mUserAgent = DEFAULT_USER_AGENT;
    private static String mUaProfTagName = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String mUaProfUrl = null;
    private static String mHttpParams = null;
    private static String mHttpParamsLine1Key = null;
    private static String mEmailGateway = null;
    private static int mMaxImageHeight = MAX_IMAGE_HEIGHT;      // default value
    private static int mMaxImageWidth = MAX_IMAGE_WIDTH;        // default value
    private static int mRecipientLimit = Integer.MAX_VALUE;     // default value
    private static int mDefaultSMSMessagesPerThread = 1000;     // default value
    private static int mDefaultMMSMessagesPerThread = 1000;     // default value
    private static int mMinMessageCountPerThread = 2;           // default value
    private static int mMaxMessageCountPerThread = 5000;        // default value
    private static int mHttpSocketTimeout = 60*1000;            // default to 1 min
    private static int mMinimumSlideElementDuration = 7;        // default to 7 sec
    private static boolean mNotifyWapMMSC = false;
    private static boolean mAllowAttachAudio = true;
    private static boolean mZoomMessage = true;

    // If mEnableMultipartSMS is true, long sms messages are always sent as multi-part sms
    // messages, with no checked limit on the number of segments.
    // If mEnableMultipartSMS is false, then as soon as the user types a message longer
    // than a single segment (i.e. 140 chars), then the message will turn into and be sent
    // as an mms message. This feature exists for carriers that don't support multi-part sms's.
    private static boolean mEnableMultipartSMS = true;

    // If mEnableMultipartSMS is true and mSmsToMmsTextThreshold > 1, then multi-part SMS messages
    // will be converted into a single mms message. For example, if the mms_config.xml file
    // specifies <int name="smsToMmsTextThreshold">4</int>, then on the 5th sms segment, the
    // message will be converted to an mms.
    private static int mSmsToMmsTextThreshold = -1;

    private static boolean mEnableSlideDuration = true;
    private static boolean mEnableMMSReadReports = false;        // key: "enableMMSReadReports"
    private static boolean mEnableSMSDeliveryReports = true;    // key: "enableSMSDeliveryReports"
    private static boolean mEnableMMSDeliveryReports = true;    // key: "enableMMSDeliveryReports"
    private static int mMaxTextLength = -1;

    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int mMaxSizeScaleForPendingMmsAllowed = 4;       // default value

    // Email gateway alias support, including the master switch and different rules
    private static boolean mAliasEnabled = false;
    private static int mAliasRuleMinChars = 2;
    private static int mAliasRuleMaxChars = 48;

    private static int mMaxSubjectLength = 40;  // maximum number of characters allowed for mms
                                                // subject

    // If mEnableGroupMms is true, a message with multiple recipients, regardless of contents,
    // will be sent as a single MMS message with multiple "TO" fields set for each recipient.
    // If mEnableGroupMms is false, the group MMS setting/preference will be hidden in the settings
    // activity.
    private static boolean mEnableGroupMms = false;

    private static int MAX_SLIDE_NUM = 10;

    private static boolean mIsEnabledCreationmode  = false;

    private static final String MMS_DESTINATION = "9798";

    private static final String[] mMdnInfoArray = new String[MessageUtils.getPhoneCount()];
    public static void init(Context context) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.init()");
        }
        // Always put the mnc/mcc in the log so we can tell which mms_config.xml was loaded.
        Log.v(TAG, "mnc/mcc: " +
                android.os.SystemProperties.get(ConstantsWrapper.TelephonyProperty.PROPERTY_ICC_OPERATOR_NUMERIC));

        loadMmsSettings(context);
        loadCarrierHttpSettings(context);
        MAX_SLIDE_NUM = context.getResources().getInteger(R.integer.max_slide_num);
    }

    public static int getMaxSlideNumber() {
        return MAX_SLIDE_NUM;
    }

    public static boolean isSmsEnabled(Context context) {
        String defaultSMSApp  =  getDefaultSMSApp(context);
        if (defaultSMSApp != null && defaultSMSApp.equals(MMS_APP_PACKAGE)) {
            return true;
        }
        return false;
    }


    private static int getIncomingUserId(Context context) {
        int contextUserId = UserHandle.myUserId();
        final int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) < Process.FIRST_APPLICATION_UID) {
            return contextUserId;
        } else {
            return UserHandle.getUserHandleForUid(callingUid).getIdentifier();
        }
    }

    public static String getDefaultSMSApp(Context context) {
        int userId = getIncomingUserId(context);
        try {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            String defaultSMSApp = roleManager.getSmsRoleHolder(userId);
            Log.d("Mms","default SMS App: " + defaultSMSApp);
            return  defaultSMSApp;
        }catch (Exception | Error e){
            Log.d("Mms","this app does not have system permission!");
            return getDefaultSmsPackageName(context);
        }
    }

    public static String getDefaultSmsPackageName(Context context) {
        String defaultSmsPackage;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//            defaultSmsPackage = Settings.Secure.getString(context.getContentResolver(),
//                    "sms_default_application");
//        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            defaultSmsPackage = intent.resolveActivity(context.getPackageManager()).getPackageName();
//        }
        Log.d("Mms","default SMS PackageName: " + defaultSmsPackage);
        return defaultSmsPackage;
    }


public static boolean isSmsPromoDismissed(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(SMS_PROMO_DISMISSED_KEY, false);
    }

    public static void setSmsPromoDismissed(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SMS_PROMO_DISMISSED_KEY, true);
        editor.apply();
    }

    public static Intent getRequestDefaultSmsAppActivity() {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, MMS_APP_PACKAGE);
        return intent;
    }

    public static int getSmsToMmsTextThreshold(Context context) {
        int limitCount = context.getResources().getInteger(R.integer.limit_count);
        if (limitCount != 0) {
            return limitCount;
        }
        return mSmsToMmsTextThreshold;
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getMaxMessageSize() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.getMaxMessageSize(): " + mMaxMessageSize);
        }
       return mMaxMessageSize;
    }

    /**
     * This function returns the value of "enabledTransID" present in mms_config file.
     * In case of single segment wap push message, this "enabledTransID" indicates whether
     * TransactionID should be appended to URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        return mUserAgent;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        return mUaProfUrl;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getRecipientLimit() {
        return mRecipientLimit;
    }

    public static int getMaxTextLimit() {
        return mMaxTextLength > -1 ? mMaxTextLength : MAX_TEXT_LENGTH;
    }

    public static int getDefaultSMSMessagesPerThread() {
        return mDefaultSMSMessagesPerThread;
    }

    public static int getDefaultMMSMessagesPerThread() {
        return mDefaultMMSMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }

    public static int getHttpSocketTimeout() {
        return mHttpSocketTimeout;
    }

    public static void setHttpSocketTimeout(int timeOut) {
        Log.v(TAG, "setHttpSocketTimeout: " + timeOut);
        mHttpSocketTimeout = timeOut;
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static boolean getMultipartSmsEnabled() {
        return mEnableMultipartSMS;
    }

    public static boolean getSlideDurationEnabled() {
        return mEnableSlideDuration;
    }

    public static boolean getMMSReadReportsEnabled() {
        return mEnableMMSReadReports;
    }

    public static boolean getSMSDeliveryReportsEnabled() {
        return mEnableSMSDeliveryReports;
    }

    public static boolean getMMSDeliveryReportsEnabled() {
        return mEnableMMSDeliveryReports;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }

    public static int getMaxSubjectLength() {
        return mMaxSubjectLength;
    }

    public static boolean getGroupMmsEnabled() {
        return mEnableGroupMms;
    }

    public static boolean isCreationModeEnabled() {
        return mIsEnabledCreationmode;
    }

    public static int getMaxRestrictedMessageSize() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "MmsConfig.mMaxRestrictedMessageSize(): "
                    + mMaxRestrictedMessageSize);
        }
        return mMaxRestrictedMessageSize;
    }

    //Configuration client
    public static boolean isOMACPEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.enableOMACP);
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG
                   && type != XmlPullParser.END_DOCUMENT) {
            ;
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG
                   && type != XmlPullParser.END_DOCUMENT) {
            ;
        }
    }

    private static void loadMmsSettings(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.mms_config);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                if (DEBUG) {
                    Log.v(TAG, "tag: " + tag + " value: " + value + " - " +
                            text);
                }
                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        } else if ("enableMultipartSMS".equalsIgnoreCase(value)) {
                            mEnableMultipartSMS = "true".equalsIgnoreCase(text);
                        } else if ("enableSlideDuration".equalsIgnoreCase(value)) {
                            mEnableSlideDuration = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSReadReports".equalsIgnoreCase(value)) {
                            mEnableMMSReadReports = "true".equalsIgnoreCase(text);
                        } else if ("enableSMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableSMSDeliveryReports = "true".equalsIgnoreCase(text);
                        } else if ("enableMMSDeliveryReports".equalsIgnoreCase(value)) {
                            mEnableMMSDeliveryReports = "true".equalsIgnoreCase(text);
                        } else if ("enableGroupMms".equalsIgnoreCase(value)) {
                            mEnableGroupMms = "true".equalsIgnoreCase(text);
                        } else if("enableCreationMode".equalsIgnoreCase(value)) {
                            mIsEnabledCreationmode = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        } else if ("defaultSMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultSMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("defaultMMSMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMMSMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        } else if ("recipientLimit".equalsIgnoreCase(value)) {
                            mRecipientLimit = Integer.parseInt(text);
                            if (mRecipientLimit < 0) {
                                mRecipientLimit = Integer.MAX_VALUE;
                            }
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mHttpSocketTimeout = Integer.parseInt(text);
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        } else if ("smsToMmsTextThreshold".equalsIgnoreCase(value)) {
                            int maxSmstomms = context.getResources().getInteger(R.integer
                                    .config_max_smstomms);
                            mSmsToMmsTextThreshold = (maxSmstomms == 0) ? Integer.parseInt(text)
                                    : maxSmstomms;
                        } else if ("maxMessageTextSize".equalsIgnoreCase(value)) {
                            mMaxTextLength = Integer.parseInt(text);
                        } else if ("maxSubjectLength".equalsIgnoreCase(value)) {
                            mMaxSubjectLength = Integer.parseInt(text);
                        } else if ("maxRestrictedMessageSize".equalsIgnoreCase(value)) {
                            mMaxRestrictedMessageSize = Integer.parseInt(text);
                        }

                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            mUaProfUrl = text;
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err =
                String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                        errorStr);
            Log.e(TAG, err);
        }
    }

    public static String getHttpParaBySubId(int subId) {
        int phoneId = PhoneUtils.getPhoneId(subId,MmsApp.getApplication().getApplicationContext());
        if ((phoneId >= 0) && (phoneId < mMdnInfoArray.length)) {
            return mMdnInfoArray[phoneId];
        } else {
            return null;
        }

    }

    private static void loadCarrierHttpSettings(Context context) {
        for (int i= 0; i< mMdnInfoArray.length; i++) {
            if (MessageUtils.isIccCardActivated(i)) {
                Log.d(TAG, "MDN: loadCarrierHttpSettings phone i " + i + " is active");
                loadCarrierHttpSetting(context, i);
            } else {
                Log.d(TAG, "MDN: loadCarrierHttpSettings phone i " + i + " isn't active");
            }
        }
    }

    public static void loadCarrierHttpSetting(final Context context, final int phoneId) {
        int subId = SubscriptionManagerWrapper.getSubIdBySlotId(phoneId);
        Context subContext = getSubContext(context, subId);
        Log.d(TAG, "MDN: loadCarrierHttpSetting: context " + context
                + "; subContext: " + subContext
                + "; phoneId: " + phoneId
                + "; subId: " + subId);
        if (subContext == null) {
            Log.d(TAG, "MDN: loadCarrierHttpSetting subId " + subId + " context is null  " );
            return;
        }
        XmlResourceParser parser = subContext.getResources().getXml(R.xml.sub_mms_config);
        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

                Log.v(TAG, "MDN: tag: " + tag + " value: " + value + " - " +
                            text);

                if ("name".equalsIgnoreCase(name)) {
                    if ("string".equals(tag)) {
                        if ("httpParams".equalsIgnoreCase(value)) {
                            Log.d(TAG, "MDN: loadCarrierHttpSetting httpParams is   " + text);
                            if ((phoneId >= 0) && (phoneId < mMdnInfoArray.length)) {
                                mMdnInfoArray[phoneId] = text;
                            }
                        }
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } catch (IOException e) {
            Log.e(TAG, "loadMmsSettings caught ", e);
        } finally {
            parser.close();
        }
    }

    private static Context getSubContext(final Context context, final int subId) {
        final String[] mccMnc = getMccMnc(context, subId);
        if (TextUtils.isEmpty(mccMnc[0]) || TextUtils.isEmpty(mccMnc[1])) {
            return null;
        }
        final Configuration subConfig = new Configuration();
        subConfig.mcc = Integer.parseInt(mccMnc[0]);
        subConfig.mnc = Integer.parseInt(mccMnc[1]);
        return context.createConfigurationContext(subConfig);
    }

    static String[] getMccMnc(final Context context, final int subId) {
        String[] mccMnc =  new String[2];
        String mcc;
        String mnc;
        StringBuilder sb =  new StringBuilder();
        if (MessageUtils.isMultiSimEnabledMms()) {
            Log.d(TAG,"MDN: getMccMnc: Support multi sim.");
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
            final SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
            if (subInfo != null) {
                mcc = subInfo.getMccString();
                mnc = subInfo.getMncString();
                Log.d(TAG,"MDN: getMccMnc: Mcc is: " + mcc
                        + "; Mnc is: " + mnc);
                mccMnc[0] = mcc;
                mccMnc[1] = mnc;
            }
        } else {
            Log.d(TAG,"MDN: getMccMnc: Do't support multi sim.");
            final String mccMncString = TelephonyManagerWrapper.getMccMnc(context);
            Log.d(TAG,"MDN: getMccMnc: MccMnc is: " + mccMncString);
            try {
                mcc = mccMncString.substring(0, 3);
                mnc = mccMncString.substring(3);
                Log.d(TAG,"MDN: getMccMnc: Mcc is: " + mcc
                        + "; Mnc is: " + mnc);
                mccMnc[0] = mcc;
                mccMnc[1] = mnc;
            } catch (Exception e) {
                Log.w(TAG, "MDN: Invalid mcc/mnc from system " + mccMncString + ": " + e);
            }
        }
        Log.d(TAG,"MDN: getMccMnc: final Mcc_mnc is: "
                + mccMnc[0] + "_" + mccMnc[1]);
        return mccMnc;
    }

    public static String getMmsDestination() {
        return MMS_DESTINATION;
    }

    public static void setZoomMessage(boolean zoom) {
        mZoomMessage = zoom;
    }

    public static boolean getZoomMessage() {
        return mZoomMessage;
    }

    public static int getDefaultSlideDuration() {
        return MmsApp.getApplication().getApplicationContext().getResources()
                .getInteger(R.integer.def_mms_slide_duration);
    }
}
