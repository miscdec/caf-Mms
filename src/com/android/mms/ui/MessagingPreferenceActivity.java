/*
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.Recycler;
import com.qrd.plugin.feature_query.FeatureQuery;
import android.telephony.SmsManager;
import android.telephony.MSimSmsManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import android.preference.Preference.OnPreferenceClickListener;

/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
            implements OnPreferenceChangeListener {
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_VIBRATE     = "pref_key_vibrate";
    public static final String NOTIFICATION_VIBRATE_WHEN= "pref_key_vibrateWhen";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";
    public static final String CONVERT_LONG_SMS_TO_MMS  = "pref_key_longsms_convert_mms";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;

    private Preference mSmsLimitPref;
    private Preference mSmsDeliveryReportPref;
    private Preference mMmsLimitPref;
    private Preference mMmsDeliveryReportPref;
    private Preference mMmsGroupMmsPref;
    private Preference mMmsReadReportPref;
    private Preference mManageSimPref;
    private Preference mClearHistoryPref;
    private CheckBoxPreference mVibratePref;
    private CheckBoxPreference mEnableNotificationsPref;
    private CheckBoxPreference mMmsAutoRetrievialPref;
    private RingtonePreference mRingtonePref;
    private ListPreference mSmsStorePref;
    private ListPreference mSmsStoreCard1Pref;
    private ListPreference mSmsStoreCard2Pref;
    private ListPreference mMmsExpiryPref;
    private EditTextPreference mSmsCenterPref;
    private EditTextPreference mSmsCenterCard1Pref;
    private EditTextPreference mSmsCenterCard2Pref;
    private Preference mSmsTemplatePref;

    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;
    public static String s_smsCenter_sub1 = null;
    public static String s_smsCenter_sub2 = null;
    public static final String GET_GSM_SMS_CENTER_ACTION = "com.android.mms.GET_GSM_SMS_CENTER_OVER";
    private boolean mShowToast = false;
    

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GET_GSM_SMS_CENTER_ACTION)) { 
                String center = intent.getStringExtra("GSM_SMS_CENTER");
                String subId = intent.getStringExtra(MessageUtils.SUB_KEY);
                if (TextUtils.isEmpty(center)) {
                    center = "";
                }
                if (center.indexOf(",") > 0) {
                    String[] centerArr = center.split(",");
                    center = centerArr[0];
                    center = center.replace("\"", "");
                }
                if (MessageUtils.CARD_SUB1 == Integer.parseInt(subId)) {
                    s_smsCenter_sub1 = center; 
                    mSmsCenterPref.setSummary(s_smsCenter_sub1);
                    mSmsCenterPref.setText(s_smsCenter_sub1);  
                    mSmsCenterCard1Pref.setSummary(s_smsCenter_sub1);
                    mSmsCenterCard1Pref.setText(s_smsCenter_sub1);
                } else {
                    s_smsCenter_sub2 = center; 
                    mSmsCenterCard2Pref.setSummary(s_smsCenter_sub2);
                    mSmsCenterCard2Pref.setText(s_smsCenter_sub2);
                }           
            }             
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        loadPrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        registerListeners();
        resume();
    }

    @Override
    protected void onPause() {
        super.onPause();    
        unregisterReceiver(mReceiver);
    }

    private void loadPrefs() {
        addPreferencesFromResource(R.xml.preferences);

        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mSmsDeliveryReportPref = findPreference("pref_key_sms_delivery_reports");
        mMmsDeliveryReportPref = findPreference("pref_key_mms_delivery_reports");
        mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        mMmsReadReportPref = findPreference("pref_key_mms_read_reports");
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mMmsAutoRetrievialPref = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);

        mSmsStorePref = (ListPreference) findPreference("pref_key_sms_store"); 
        mSmsStoreCard1Pref = (ListPreference) findPreference("pref_key_sms_store_card1");
        mSmsStoreCard2Pref = (ListPreference) findPreference("pref_key_sms_store_card2");
        mSmsCenterPref = (EditTextPreference) findPreference ("pref_key_sms_center");
        mSmsCenterCard1Pref = (EditTextPreference) findPreference ("pref_key_sms_center_card1");
        mSmsCenterCard2Pref = (EditTextPreference) findPreference ("pref_key_sms_center_card2");
        mMmsExpiryPref = (ListPreference) findPreference("pref_key_mms_expiry"); 
        mSmsTemplatePref = (Preference)findPreference("pref_key_sms_template");
        
        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        unregisterReceiver(mReceiver);
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        loadPrefs();
        resume();
        if (!MessageUtils.isMultiSimEnabledMms()) {
            if (MessageUtils.isHasCard()) {
                setPreferStore(MessageUtils.STORE_ME);
            }   
        } else {
            if (MessageUtils.isHasCard(MessageUtils.CARD_SUB1)) {
                setPreferStore(MessageUtils.STORE_ME,MessageUtils.CARD_SUB1);
            }  
            if (MessageUtils.isHasCard(MessageUtils.CARD_SUB2)) {
                setPreferStore(MessageUtils.STORE_ME,MessageUtils.CARD_SUB2);
            }   
        }
        restoreSmsTemplatepref();

        // NOTE: After restoring preferences, the auto delete function (i.e. message recycler)
        // will be turned off by default. However, we really want the default to be turned on.
        // Because all the prefs are cleared, that'll cause:
        // ConversationList.runOneTimeStorageLimitCheckForLegacyMessages to get executed the
        // next time the user runs the Messaging app and it will either turn on the setting
        // by default, or if the user is over the limits, encourage them to turn on the setting
        // manually.
    }

    private void setMessagePreferences() {
        //if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
        //remove this preference for menu item of SIM card is added in conversation list view and mailbox list view
        if(true){
            // No SIM card, remove the SIM-related prefs
            PreferenceCategory smsCategory =
                (PreferenceCategory)findPreference("pref_key_sms_settings");
            smsCategory.removePreference(mManageSimPref);
        }

        if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            PreferenceCategory smsCategory =
                (PreferenceCategory)findPreference("pref_key_sms_settings");
            smsCategory.removePreference(mSmsDeliveryReportPref);
            if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                getPreferenceScreen().removePreference(smsCategory);
            }
        }

        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            PreferenceCategory mmsOptions =
                (PreferenceCategory)findPreference("pref_key_mms_settings");
            getPreferenceScreen().removePreference(mmsOptions);

            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(findPreference("pref_key_mms_delete_limit"));
        } else {
            PreferenceCategory mmsOptions =
                    (PreferenceCategory)findPreference("pref_key_mms_settings");
            if (!MmsConfig.getMMSDeliveryReportsEnabled()) {
                mmsOptions.removePreference(mMmsDeliveryReportPref);
            }
            if (!MmsConfig.getMMSReadReportsEnabled()) {
                mmsOptions.removePreference(mMmsReadReportPref);
            }
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled() ||
                    TextUtils.isEmpty(MessageUtils.getLocalNumber())) {
                mmsOptions.removePreference(mMmsGroupMmsPref);
            }
        }

        if (MessageUtils.isMultiSimEnabledMms()) {
            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            PreferenceCategory smsCategory =
                (PreferenceCategory)findPreference("pref_key_sms_settings");  
            storageOptions.removePreference(mSmsStorePref);
            smsCategory.removePreference(mSmsCenterPref);
            
            if (!MessageUtils.isHasCard(MessageUtils.CARD_SUB1)) {
                storageOptions.removePreference(mSmsStoreCard1Pref);
                smsCategory.removePreference(mSmsCenterCard1Pref);
            } else {
                setSmsStoreSummary(MessageUtils.CARD_SUB1);
                setSmsCenterNumber(MessageUtils.CARD_SUB1);
            }
            if (!MessageUtils.isHasCard(MessageUtils.CARD_SUB2)) {
                storageOptions.removePreference(mSmsStoreCard2Pref);
                smsCategory.removePreference(mSmsCenterCard2Pref);
            } else {
                setSmsStoreSummary(MessageUtils.CARD_SUB2);
                setSmsCenterNumber(MessageUtils.CARD_SUB2);
            }
        } else {
            PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_storage_settings");
            PreferenceCategory smsCategory =
                (PreferenceCategory)findPreference("pref_key_sms_settings"); 
            storageOptions.removePreference(mSmsStoreCard1Pref);
            storageOptions.removePreference(mSmsStoreCard2Pref);

            smsCategory.removePreference(mSmsCenterCard1Pref);
            smsCategory.removePreference(mSmsCenterCard2Pref);
            
            if (!MessageUtils.isHasCard()) {
                storageOptions.removePreference(mSmsStorePref);
                smsCategory.removePreference(mSmsCenterPref);
            } else {
                setSmsStoreSummary();
                setSmsCenterNumber();
            }
        }
        
        setEnabledNotificationsPref();

        // If needed, migrate vibration setting from the previous tri-state setting stored in
        // NOTIFICATION_VIBRATE_WHEN to the boolean setting stored in NOTIFICATION_VIBRATE.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            String vibrateWhen = sharedPreferences.
                    getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            boolean vibrate = "always".equals(vibrateWhen);
            SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
            prefsEditor.putBoolean(NOTIFICATION_VIBRATE, vibrate);
            prefsEditor.remove(NOTIFICATION_VIBRATE_WHEN);  // remove obsolete setting
            prefsEditor.apply();
            mVibratePref.setChecked(vibrate);
        }

        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();
        setMmsExpirySummary();
        setSmsTemplatePref();

        String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        setRingtoneSummary(soundValue);
    }

    private void setRingtoneSummary(String soundValue) {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    private void setSmsStoreSummary() {
        mSmsStorePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSmsStorePref.findIndexOfValue(summary);
                mSmsStorePref.setSummary(mSmsStorePref.getEntries()[index]);
                mSmsStorePref.setValue(summary);
                setPreferStore(Integer.parseInt(summary));
                return true;
            }
        }); 
        mSmsStorePref.setSummary(mSmsStorePref.getEntry());
    }

    private void setSmsStoreSummary(int subscription) {
        if(MessageUtils.CARD_SUB1 == subscription) {
            mSmsStoreCard1Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreCard1Pref.findIndexOfValue(summary);
                    mSmsStoreCard1Pref.setSummary(mSmsStoreCard1Pref.getEntries()[index]);
                    mSmsStoreCard1Pref.setValue(summary);
                    setPreferStore(Integer.parseInt(summary),MessageUtils.CARD_SUB1);
                    return false;
                }
            });   
            mSmsStoreCard1Pref.setSummary(mSmsStoreCard1Pref.getEntry());
        } else {
            mSmsStoreCard2Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreCard2Pref.findIndexOfValue(summary);
                    mSmsStoreCard2Pref.setSummary(mSmsStoreCard2Pref.getEntries()[index]);
                    mSmsStoreCard2Pref.setValue(summary);
                    setPreferStore(Integer.parseInt(summary),MessageUtils.CARD_SUB2);
                    return false;
                }
            }); 
            mSmsStoreCard2Pref.setSummary(mSmsStoreCard2Pref.getEntry());
        }       
    }
    
    private void getSmsCenterNumber() {
        String s_smsCenter = null;
        s_smsCenter = s_smsCenter_sub1;
        if (TextUtils.isEmpty(s_smsCenter)) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.getGsmSmsCenter();
            String title = "";
            mSmsCenterPref.setSummary(title);
        } else {
            mSmsCenterPref.setSummary(s_smsCenter);
            mSmsCenterPref.setText(s_smsCenter);
        }       
    }
    
    private void getSmsCenterNumber(int subscription) {
        String s_smsCenter = null;
        if (MessageUtils.CARD_SUB1 == subscription) {
            s_smsCenter = s_smsCenter_sub1;
            if (TextUtils.isEmpty(s_smsCenter)) {
                MSimSmsManager smsManager = MSimSmsManager.getDefault();
                smsManager.getGsmSmsCenter(subscription);   
                String title = "";
                mSmsCenterCard1Pref.setSummary(title);
            } else {  
                mSmsCenterCard1Pref.setSummary(s_smsCenter);
                mSmsCenterCard1Pref.setText(s_smsCenter);
            }
        } else {
            s_smsCenter = s_smsCenter_sub2;
            if (TextUtils.isEmpty(s_smsCenter)) {
                MSimSmsManager smsManager = MSimSmsManager.getDefault();
                smsManager.getGsmSmsCenter(subscription);
                String title = "";
                mSmsCenterCard2Pref.setSummary(title);
            } else {
                mSmsCenterCard2Pref.setSummary(s_smsCenter);
                mSmsCenterCard2Pref.setText(s_smsCenter);
            }
        }  
    }

    private void setSmsCenterNumber() {
        mSmsCenterPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                boolean result = setNumberResult(value);
                if (result) {
                    mSmsCenterPref.setSummary(value);
                    s_smsCenter_sub1 = value; 
                    Toast.makeText(MessagingPreferenceActivity.this,
                        R.string.operate_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MessagingPreferenceActivity.this,
                        R.string.operate_failure, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        }); 
        mSmsCenterPref.setSummary(mSmsCenterPref.getText());
    }

    private void setSmsCenterNumber(int subscription) {
        if (MessageUtils.CARD_SUB1 == subscription){
            mSmsCenterCard1Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String value = newValue.toString();
                    boolean result = setNumberResult(value,MessageUtils.CARD_SUB1);
                    if (result) {
                        mSmsCenterCard1Pref.setSummary(value);
                        s_smsCenter_sub1 = value; 
                        Toast.makeText(MessagingPreferenceActivity.this,
                            R.string.operate_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MessagingPreferenceActivity.this,
                            R.string.operate_failure, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }); 
            mSmsCenterCard1Pref.setSummary(mSmsCenterCard1Pref.getText());
        } else {
            mSmsCenterCard2Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String value = newValue.toString();
                    boolean result = setNumberResult(value,MessageUtils.CARD_SUB2);
                    if (result) {
                        mSmsCenterCard2Pref.setSummary(value);
                        s_smsCenter_sub2 = value; 
                        Toast.makeText(MessagingPreferenceActivity.this,
                            R.string.operate_success, Toast.LENGTH_SHORT).show();   
                    } else {
                        Toast.makeText(MessagingPreferenceActivity.this,
                            R.string.operate_failure, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }); 
            mSmsCenterCard2Pref.setSummary(mSmsCenterCard2Pref.getText());
        }   
    }

    private boolean setNumberResult(String value) {
        int numberType = 129;
        if (value.startsWith("+")) {
            numberType = 145;
        }
        String center = "\"" + value + "\"," + String.valueOf(numberType);
        boolean result = false;
        SmsManager smsManager = SmsManager.getDefault();
        result = smsManager.setGsmSmsCenter(center); 
        return result; 
    }
    
    private boolean setNumberResult(String value,int subscription) {
        int numberType = 129;
        if (value.startsWith("+")) {
            numberType = 145;
        }
        String center = "\"" + value + "\"," + String.valueOf(numberType);
        boolean result = false;
        MSimSmsManager smsManager = MSimSmsManager.getDefault();
        result = smsManager.setGsmSmsCenter(center,subscription);
        return result; 
    }

    public void resume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GET_GSM_SMS_CENTER_ACTION);
        registerReceiver(mReceiver, filter);
        if (MessageUtils.isMultiSimEnabledMms()) {
            if (MessageUtils.isHasCard(MessageUtils.CARD_SUB1)) {
                getSmsCenterNumber(MessageUtils.CARD_SUB1);
            }
            if (MessageUtils.isHasCard(MessageUtils.CARD_SUB2)) {
                getSmsCenterNumber(MessageUtils.CARD_SUB2);
            }
        } else {
            if (MessageUtils.isHasCard()) {
                getSmsCenterNumber();
            }
        }
    }

    private void setPreferStore(int store) {
        SmsManager smsmanager = SmsManager.getDefault();
        if (MessageUtils.isIccCardActivated()) {
            if (store == MessageUtils.STORE_ME) {
                smsmanager.setSmsPreStore(MessageUtils.STORE_ME,true);
            } else if (store == MessageUtils.STORE_SM) {
                smsmanager.setSmsPreStore(MessageUtils.STORE_SM,true);
            }
        }
    }

    private void setPreferStore(int store,int subscription) {
        MSimSmsManager smsmanager = MSimSmsManager.getDefault();
        if (MessageUtils.CARD_SUB1 == subscription){
            if (MessageUtils.isIccCardActivated(MessageUtils.CARD_SUB1)) {
                if (store == MessageUtils.STORE_ME) {
                    smsmanager.setSmsPreStore(MessageUtils.STORE_ME,true,MessageUtils.CARD_SUB1);
                } else if (store == MessageUtils.STORE_SM) {
                    smsmanager.setSmsPreStore(MessageUtils.STORE_SM,true,MessageUtils.CARD_SUB1);
                }
            }
        } else{
            if (MessageUtils.isIccCardActivated(MessageUtils.CARD_SUB2)) {
                if (store == MessageUtils.STORE_ME) {
                    smsmanager.setSmsPreStore(MessageUtils.STORE_ME,true,MessageUtils.CARD_SUB2);
                } else if (store == MessageUtils.STORE_SM) {
                    smsmanager.setSmsPreStore(MessageUtils.STORE_SM,true,MessageUtils.CARD_SUB2);
                }
            }
        }
    }

    private void setMmsExpirySummary() {
        mMmsExpiryPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String value = newValue.toString();
                mMmsExpiryPref.setValue(value);

                if (value.equals("604800") ) {
                    mMmsExpiryPref.setSummary(getString(R.string.mms_one_week));
                } else if (value.equals("172800")) {
                    mMmsExpiryPref.setSummary(getString(R.string.mms_two_days));
                } else {
                    mMmsExpiryPref.setSummary(getString(R.string.mms_max));
                }
                return false;
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String expiry = prefs.getString("pref_key_mms_expiry", "");  
        
        if (expiry.equals("604800")) {
            mMmsExpiryPref.setSummary(getString(R.string.mms_one_week));
        } else if ( expiry.equals("172800")) {
            mMmsExpiryPref.setSummary(getString(R.string.mms_two_days));
        } else {
            mMmsExpiryPref.setSummary(getString(R.string.mms_max));
        }
    }

    private void setSmsTemplatePref() {
        OnPreferenceClickListener preListener = new OnPreferenceClickListener(){
            public boolean onPreferenceClick(Preference preference){
                Intent intent = new Intent();
                intent.setClass(MessagingPreferenceActivity.this,SMSTemplateActivity.class);
                startActivity(intent);
                return true;
            }
        };
        mSmsTemplatePref.setOnPreferenceClickListener(preListener);
    }

    private void restoreSmsTemplatepref(){
        SharedPreferences tempatespre;
        tempatespre = getSharedPreferences("SMSTemplate",0);
        tempatespre.edit().clear().commit();
        tempatespre.edit().putInt("templatecount",10).commit();
        tempatespre.edit().putBoolean("init",true).commit();
        Preference smsTemplatePre = (Preference)findPreference("smstemplate");
        setSmsTemplatePref();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mSmsLimitPref) {
            new NumberPickerDialog(this,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete).show();
        } else if (preference == mMmsLimitPref) {
            new NumberPickerDialog(this,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete).show();
        } else if (preference == mManageSimPref) {
            startActivity(new Intent(this, ManageSimMessages.class));
        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;
        } else if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE, null, this,
                TransactionService.class));
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit();
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, enabled);

        editor.apply();
    }

    private void registerListeners() {
        mRingtonePref.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mRingtonePref) {
            setRingtoneSummary((String)newValue);
            result = true;
        }
        return result;
    }

    // For the group mms feature to be enabled, the following must be true:
    //  1. the feature is enabled in mms_config.xml (currently on by default)
    //  2. the feature is enabled in the mms settings page
    //  3. the SIM knows its own phone number
    public static boolean getIsGroupMmsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean groupMmsPrefOn = prefs.getBoolean(
                MessagingPreferenceActivity.GROUP_MMS_MODE, true);
        return MmsConfig.getGroupMmsEnabled() &&
                groupMmsPrefOn &&
                !TextUtils.isEmpty(MessageUtils.getLocalNumber());
    }
}
