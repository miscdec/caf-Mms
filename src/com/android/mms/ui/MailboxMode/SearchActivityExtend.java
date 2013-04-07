/*
 * Copyright (c) 2012-2013, Code Aurora Forum. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained 
 * for attribution purposes only.
 * Copyright (C) 2012 The Android Open Source Project. 
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

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.util.Recycler;
import com.google.android.mms.pdu.PduHeaders;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;


/**
 * This activity provides extend search mode by content ,number and name in mailbox mode.
 */
public class SearchActivityExtend extends Activity 
{
    private static final String TAG = "SearchActivityExtend";
    private EditText mSearchStringEdit;
    private Spinner  mSpinSearchMode;
    private final static int MENU_SEARCH = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_dialog); 

        initUi();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);                
    }

    private void initUi()
    {
        mSpinSearchMode = (Spinner) findViewById(R.id.search_mode);
        mSpinSearchMode.setPromptId(R.string.search_mode);        
        mSearchStringEdit = (EditText) findViewById(R.id.search_key_edit);         
        mSearchStringEdit.requestFocus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
        case MENU_SEARCH:
            doSearch();
            break;
        case android.R.id.home:
            finish();
            break;
        default:
            return true;
        }
   
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        menu.add(0, MENU_SEARCH, 0, R.string.menu_search)
            .setIcon(R.drawable.ic_menu_search_holo_dark)
            .setAlphabeticShortcut(android.app.SearchManager.MENU_KEY)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);        
        return true;
    }
    
    private void doSearch()
    {
        String keyStr = mSearchStringEdit.getText().toString();
        if (TextUtils.isEmpty(keyStr))
        {       
            return;
        }

        int modePosition = mSpinSearchMode.getSelectedItemPosition();      
        int orgModePosition = modePosition;
        int matchWhole = 0;                         

        if (modePosition == MessageUtils.SEARCH_MODE_NAME)
        {
            keyStr = MessageUtils.getAddressByName(this, keyStr);
            if (TextUtils.isEmpty(keyStr))
            {
                Toast.makeText(SearchActivityExtend.this, getString(R.string.invalid_name_toast), 
                                    Toast.LENGTH_LONG).show();
                return;
            }
            modePosition = MessageUtils.SEARCH_MODE_NUMBER;
            matchWhole = 1;
        }

        Intent i = new Intent(this, MailBoxMessageList.class);
        int mailboxId = Sms.MESSAGE_TYPE_SEARCH;            
        i.putExtra("title", getString(R.string.search_title));
        i.putExtra("mailboxId", mailboxId);  
        i.putExtra("mode_position", modePosition);
        i.putExtra("key_str", keyStr);
        i.putExtra("match_whole", matchWhole);        
        startActivity(i);  
        
        Log.d(TAG, "doSearch : keyStr = " + keyStr + " modePosition = " + modePosition
                                + " matchWhole = " + matchWhole +" mailboxId = "+ mailboxId);   
        finish();
    }
}

