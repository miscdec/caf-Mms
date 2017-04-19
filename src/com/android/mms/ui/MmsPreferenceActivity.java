/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import java.util.List;
import java.util.Map;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.telephony.OperatorSimInfo;
import com.android.internal.telephony.PhoneConstants;

import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.mms.transaction.MmsMessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.transaction.TransactionService;

import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;

public class MmsPreferenceActivity extends PreferenceActivity {
    private static String TAG = "MmsPreferenceActivity";

    public static final String MMS_DELIVERY_REPORT_MODE_SIM = "pref_key_mms_delivery_reports";
    public static final String MMS_DELIVERY_REPORT_SS = "pref_key_mms_delivery_reports_ss";

    public static final String MMS_DELIVERY_REPORT_SUB1 = "pref_key_mms_delivery_reports_card1";

    public static final String MMS_DELIVERY_REPORT_SUB2 = "pref_key_mms_delivery_reports_card2";

    public static final String MMS_READ_REPORT_MODE = "pref_key_mms_read_reports";

    public static final String MMS_READ_REPORT_SS = "pref_key_mms_read_reports_ss";

    public static final String MMS_READ_REPORT_SUB1 = "pref_key_mms_read_reports_card1";

    public static final String MMS_READ_REPORT_SUB2 = "pref_key_mms_read_reports_card2";

    public static final String AUTO_RETRIEVAL = "pref_key_mms_auto_retrieval";

    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";

    public static final String MMS_VALIDITY_PERIOD = "pref_key_mms_expiry";

    public static final String MMS_VALIDITY_PERIOD_NO_MULTI = "pref_key_mms_expiry_no_multi";

    public static final String MMS_VALIDITY_SIM1 = "pref_key_mms_expiry_sim1";

    public static final String MMS_VALIDITY_SIM2 = "pref_key_mms_expiry_sim2";

    private static String MMS_READ_REPORTS = "mms_read_reports";

    private static String MMS_DELIVERY_REPORTS = "mms_delivery_reports";

    private static String TYPE_MMS = "mms";

    private static String MSG_TYPE = "msg_type";

    private PreferenceScreen mMmsSettingsPref;
    private Preference mMmsDeliveryReportPref;
    private Preference mMmsReadReportPref;
    private SwitchPreference mMmsDeliveryReportPrefSS;
    private SwitchPreference mMmsDeliveryReportPrefSub1;
    private SwitchPreference mMmsDeliveryReportPrefSub2;
    private SwitchPreference mMmsReadReportPrefSS;
    private SwitchPreference mMmsReadReportPrefSub1;
    private SwitchPreference mMmsReadReportPrefSub2;
    private SwitchPreference mMmsAutoRetrievialPref;
    private SwitchPreference mMmsDuringPref;
    private Preference mMmsValidityPref;
    private ListPreference mMmsExpiryNoMultiPref;
    private ListPreference mMmsExpiryCard1Pref;
    private ListPreference mMmsExpiryCard2Pref;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        createResource();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE
                | ActionBar.DISPLAY_HOME_AS_UP);
    }

    private void createResource() {
        addPreferencesFromResource(R.xml.mms_preferences);
        mMmsSettingsPref = (PreferenceScreen) findPreference("pref_key_mms_settings");
        mMmsDeliveryReportPref = (Preference) findPreference("pref_key_mms_delivery_reports");
        mMmsDeliveryReportPrefSS = (SwitchPreference) findPreference(MMS_DELIVERY_REPORT_SS);
        mMmsDeliveryReportPrefSub1 = (SwitchPreference)
                findPreference(MMS_DELIVERY_REPORT_SUB1);
        mMmsDeliveryReportPrefSub2 = (SwitchPreference)
                findPreference(MMS_DELIVERY_REPORT_SUB2);
        mMmsReadReportPref = (Preference) findPreference("pref_key_mms_read_reports");
        mMmsReadReportPrefSS = (SwitchPreference) findPreference(MMS_READ_REPORT_SS);
        mMmsReadReportPrefSub1 = (SwitchPreference)
                findPreference(MMS_READ_REPORT_SUB1);
        mMmsReadReportPrefSub2 = (SwitchPreference)
                findPreference(MMS_READ_REPORT_SUB2);
        mMmsAutoRetrievialPref = (SwitchPreference) findPreference("pref_key_mms_auto_retrieval");
        mMmsDuringPref = (SwitchPreference) findPreference("pref_key_mms_retrieval_during_roaming");
        mMmsValidityPref = (Preference) findPreference("pref_key_mms_expiry");
        mMmsExpiryNoMultiPref = (ListPreference) findPreference("pref_key_mms_expiry_no_multi");
        mMmsExpiryCard1Pref = (ListPreference) findPreference("pref_key_mms_expiry_sim1");
        mMmsExpiryCard2Pref = (ListPreference) findPreference("pref_key_mms_expiry_sim2");
        setMmsExpiryPref();
        if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            mMmsSettingsPref.removePreference(mMmsDeliveryReportPref);
            mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSS);
            mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSub1);
            mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSub2);
        } else {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSS);
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mMmsSettingsPref.removePreference(mMmsDeliveryReportPref);
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref.removePreference(mMmsDeliveryReportPref);
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub2);
                }
                if (MessageUtils.isMsimIccCardActive()) {
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub1);
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub2);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)
                        && !MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub1);
                    mMmsSettingsPref
                            .removePreference(mMmsDeliveryReportPrefSub2);
                    mMmsSettingsPref.addPreference(mMmsDeliveryReportPref);
                    mMmsDeliveryReportPref.setEnabled(false);
                }
            } else {
                mMmsSettingsPref.removePreference(mMmsDeliveryReportPref);
                mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSub1);
                mMmsSettingsPref.removePreference(mMmsDeliveryReportPrefSub2);
            }
        }
        if (!MmsConfig.getMMSReadReportsEnabled()) {
            mMmsSettingsPref.removePreference(mMmsReadReportPref);
            mMmsSettingsPref.removePreference(mMmsReadReportPrefSS);
            mMmsSettingsPref.removePreference(mMmsReadReportPrefSub1);
            mMmsSettingsPref.removePreference(mMmsReadReportPrefSub2);
        } else {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                mMmsSettingsPref.removePreference(mMmsReadReportPrefSS);
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)) {
                    mMmsSettingsPref.removePreference(mMmsReadReportPref);
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub1);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref.removePreference(mMmsReadReportPref);
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub2);
                }
                if (MessageUtils.isMsimIccCardActive()) {
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub1);
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub2);
                }
                if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)
                        && !MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub1);
                    mMmsSettingsPref.removePreference(mMmsReadReportPrefSub2);
                    mMmsSettingsPref.addPreference(mMmsReadReportPref);
                    mMmsReadReportPref.setEnabled(false);
                }
            } else {
                mMmsSettingsPref.removePreference(mMmsReadReportPref);
                mMmsSettingsPref.removePreference(mMmsReadReportPrefSub1);
                mMmsSettingsPref.removePreference(mMmsReadReportPrefSub2);
            }
        }
    }

    private void setMmsExpiryPref() {
        if (getResources().getBoolean(R.bool.config_mms_validity)) {
            if (MessageUtils.isMultiSimEnabledMms()) {
                mMmsSettingsPref.removePreference(mMmsExpiryNoMultiPref);
                if (MessageUtils.isMsimIccCardActive()) {
                    mMmsSettingsPref.removePreference(mMmsExpiryCard1Pref);
                    mMmsSettingsPref.removePreference(mMmsExpiryCard2Pref);
                } else if (MessageUtils.isIccCardActivated(MessageUtils.SUB1)
                        && !MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref.removePreference(mMmsExpiryCard2Pref);
                    mMmsSettingsPref.removePreference(mMmsValidityPref);
                    setMmsExpirySummary(PhoneConstants.SUB1);
                    if(checkForOperatorCustomFeature()) {
                        mMmsExpiryCard1Pref.setDialogTitle(getResources().
                                getString(R.string.pref_title_mms_save_time));
                    }
                } else if (!MessageUtils.isIccCardActivated(MessageUtils.SUB1)
                        && MessageUtils.isIccCardActivated(MessageUtils.SUB2)) {
                    mMmsSettingsPref.removePreference(mMmsExpiryCard1Pref);
                    mMmsSettingsPref.removePreference(mMmsValidityPref);
                    setMmsExpirySummary(PhoneConstants.SUB2);
                    if(checkForOperatorCustomFeature()) {
                        mMmsExpiryCard2Pref.setDialogTitle(getResources().
                                getString(R.string.pref_title_mms_save_time));
                    }
                } else {
                    mMmsSettingsPref.removePreference(mMmsExpiryCard1Pref);
                    mMmsSettingsPref.removePreference(mMmsExpiryCard2Pref);
                    mMmsValidityPref.setEnabled(false);
                }
            } else {
                mMmsSettingsPref.removePreference(mMmsExpiryCard1Pref);
                mMmsSettingsPref.removePreference(mMmsExpiryCard2Pref);
                mMmsSettingsPref.removePreference(mMmsValidityPref);
                setMmsExpirySummary(MessageUtils.SUB_INVALID);
            }
        } else {
            mMmsSettingsPref.removePreference(mMmsExpiryCard1Pref);
            mMmsSettingsPref.removePreference(mMmsExpiryCard2Pref);
            mMmsSettingsPref.removePreference(mMmsExpiryNoMultiPref);
            mMmsSettingsPref.removePreference(mMmsValidityPref);
        }
    }

    private boolean checkForOperatorCustomFeature() {
        boolean isFeatureExists = false;
        OperatorSimInfo operatorSimInfo = new OperatorSimInfo(getApplicationContext());
        if (operatorSimInfo.isOperatorFeatureEnabled()) {
            isFeatureExists = true;
        }
        return isFeatureExists;
    }

    private void setMmsExpirySummary(int subscription) {
        switch (subscription) {
            case MessageUtils.SUB_INVALID:
                mMmsExpiryNoMultiPref
                        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                final String summary = newValue.toString();
                                int index = mMmsExpiryNoMultiPref
                                        .findIndexOfValue(summary);
                                mMmsExpiryNoMultiPref
                                        .setSummary(mMmsExpiryNoMultiPref
                                                .getEntries()[index]);
                                mMmsExpiryNoMultiPref.setValue(summary);
                                return true;
                            }
                        });
                mMmsExpiryNoMultiPref.setSummary(mMmsExpiryNoMultiPref.getEntry());
                break;
            case PhoneConstants.SUB1:
                mMmsExpiryCard1Pref
                        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                final String value = newValue.toString();
                                int index = mMmsExpiryCard1Pref
                                        .findIndexOfValue(value);
                                mMmsExpiryCard1Pref.setValue(value);
                                mMmsExpiryCard1Pref.setSummary(mMmsExpiryCard1Pref
                                        .getEntries()[index]);
                                return false;
                            }
                        });
                mMmsExpiryCard1Pref.setSummary(mMmsExpiryCard1Pref.getEntry());
                break;
            case PhoneConstants.SUB2:
                mMmsExpiryCard2Pref
                        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                final String value = newValue.toString();
                                int index = mMmsExpiryCard2Pref
                                        .findIndexOfValue(value);
                                mMmsExpiryCard2Pref.setValue(value);
                                mMmsExpiryCard2Pref.setSummary(mMmsExpiryCard2Pref
                                        .getEntries()[index]);
                                return false;
                            }
                        });
                mMmsExpiryCard2Pref.setSummary(mMmsExpiryCard2Pref.getEntry());
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar.
                // Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mMmsDeliveryReportPref) {
            Intent intent = new Intent(this, MessagingReportsPreferenceActivity.class);
            intent.putExtra(MSG_TYPE, MMS_DELIVERY_REPORTS);
            startActivity(intent);
        } else if (preference == mMmsReadReportPref) {
            Intent intent = new Intent(this, MessagingReportsPreferenceActivity.class);
            intent.putExtra(MSG_TYPE, MMS_READ_REPORTS);
            startActivity(intent);
        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        } else if (preference == mMmsValidityPref) {
            Intent intent = new Intent(this, MessagingExpiryPreferenceActivity.class);
            intent.putExtra(MSG_TYPE, TYPE_MMS);
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE,
                null, this, TransactionService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
