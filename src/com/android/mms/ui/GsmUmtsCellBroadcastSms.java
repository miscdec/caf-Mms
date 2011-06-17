/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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

package com.android.mms.ui;

import com.android.mms.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

public class GsmUmtsCellBroadcastSms extends PreferenceActivity {
    // debug data
    private static final String LOG_TAG = "GsmUmtsCellBroadcastSms";

    // Message IDs.
    private static final int AREA_INFO_MSG_ID = 50;
    private static final int [] supportedMsgIds = {AREA_INFO_MSG_ID};

    // String keys for preference lookup
    private static final String AREA_INFO_PREFERENCE_KEY = "area_info_msgs_key";

    // String keys for shared preference lookup
    private static final String SP_FILE_NAME = "GsmUmtsSharedPref";
    private static final String AREA_INFO_ENABLED = "area_info_enabled";

    // Preference instance variables.
    private CheckBoxPreference mAreaInfoPreference;

    // Instance variables
    boolean mAreaInfoEnabled = false;
    private int mSubscription = 0;

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAreaInfoPreference) {
            Log.d(LOG_TAG, "onPreferenceTreeClick: AreaInfo - " + mAreaInfoPreference.isChecked());
            return true;
        }

        return false;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_cell_broadcast_sms);
        mAreaInfoPreference = (CheckBoxPreference) findPreference(AREA_INFO_PREFERENCE_KEY);
        mSubscription = getIntent().getIntExtra(MessagingPreferenceActivity.SUBSCRIPTION, 0);
        Log.d(LOG_TAG, "onCreate: mSubscription is: " + mSubscription);

        String spKey = AREA_INFO_ENABLED + mSubscription;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        enableOrDisableCbMsg(AREA_INFO_MSG_ID, sp.getBoolean(spKey, false));
        Log.d(LOG_TAG, "onCreate, mAreaInfoEnabled, spKey values : " + mAreaInfoEnabled
              + ", " + spKey);
        mAreaInfoPreference.setChecked(mAreaInfoEnabled);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        enableOrDisableCbMsg(AREA_INFO_MSG_ID, mAreaInfoPreference.isChecked());

        String spKey = AREA_INFO_ENABLED + mSubscription;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(spKey, mAreaInfoPreference.isChecked());
        spe.commit();
    }

    public void enableOrDisableCbMsg(int msgId, boolean enable) {
        if (mAreaInfoEnabled != enable) {
            mAreaInfoEnabled = enable;
            SmsManager sm = SmsManager.getDefault();
            if (enable) {
                sm.enableCellBroadcastOnSubscription(msgId, mSubscription);
            } else {
                sm.disableCellBroadcastOnSubscription(msgId, mSubscription);
            }
       }
    }

    public static boolean isMsgIdSupported(int msgId) {
        int length = supportedMsgIds.length;

        for (int i = 0; i < length; i++) {
            if(msgId == supportedMsgIds[i]) {
               return true;
            }
        }

        return false;
    }
}
