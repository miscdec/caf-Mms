/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.mms.data.Contact;
import com.android.mms.data.Conversation;
import android.provider.Telephony.Sms;
import android.text.TextUtils;

/**
 * The Welcome activity initializes the application and decides what Activity
 * the user should start with.
 */
public class ModeChooser extends Activity {
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SharedPreferences prefsms = PreferenceManager.getDefaultSharedPreferences(this);
        int viewMode = prefsms.getInt("current_view", MessageUtils.CHAT_MODE);
        Log.d("ModeChooser", "onCreate : viewMode = " + viewMode);
      
        if (viewMode == MessageUtils.MAILBOX_MODE) 
        {
            Intent intent = new Intent(this, MailBoxMessageList.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);            
            intent.putExtra("mailboxId", Sms.MESSAGE_TYPE_INBOX);
            
            startActivity(intent);
        }
        else 
        {
            Intent intent = new Intent(this, ConversationList.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
               | Intent.FLAG_ACTIVITY_SINGLE_TOP
               | Intent.FLAG_ACTIVITY_CLEAR_TOP);
               
            startActivity(intent);
        }        
        finish();
    }
    
}
