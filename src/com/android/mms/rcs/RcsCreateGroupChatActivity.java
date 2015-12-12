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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.R.array;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.text.InputFilter;
import android.text.TextUtils;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.LogTag;
import com.android.mms.ui.ChipsRecipientAdapter;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.RecipientsEditor;

import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.api.basic.BasicApi;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatApi;
import com.suntek.mway.rcs.client.api.exception.InviteTooManyUserException;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.rcs.ui.common.mms.GroupChatManagerReceiver;
import com.suntek.rcs.ui.common.mms.GroupChatManagerReceiver.GroupChatNotifyCallback;
import com.suntek.rcs.ui.common.mms.RcsEditTextInputFilter;

public class RcsCreateGroupChatActivity extends Activity implements
        View.OnClickListener {

    private static final String RCS_TAG = "RCS_UI";
    public static final String EXTRA_RECIPIENTS = "recipients";
    private final static String MULTI_SELECT_CONV = "select_conversation";
    private static final int MENU_DONE = 0;
    public static final int REQUEST_CODE_CONTACTS_PICK = 100;
    public static final int REQUEST_CODE_ADD_CONVERSATION = 124;
    private EditText mSubjectEdit;
    private RecipientsEditor mRecipientsEditor;
    private ContactList mRecipientList = new ContactList();
    private ComposeMessageCreateGroupChatCallback mCreateGroupChatCallback;
    private static final String INTENT_MULTI_PICK = "com.android.contacts.action.MULTI_PICK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_new_group_chat_activity);
        getIntentData();
        initView();
        registerReceiver(mRcsGroupChatReceiver,
                new IntentFilter(Actions.GroupChatAction.ACTION_GROUP_CHAT_MANAGE_NOTIFY));
    }

    private void getIntentData(){
        String numbers = getIntent().getStringExtra(EXTRA_RECIPIENTS);
        if(!TextUtils.isEmpty(numbers)){
            String[] numberList = numbers.split(";");
            ContactList list = ContactList.getByNumbers(Arrays.asList(numberList), true);
            mRecipientList.clear();
            mRecipientList.addAll(list);
        }
    }

    private void initView() {
        mSubjectEdit = (EditText) findViewById(R.id.group_chat_subject);
        InputFilter[] filters = { new RcsEditTextInputFilter(30) };
        mSubjectEdit.setFilters(filters);
        findViewById(R.id.recipients_selector).setOnClickListener(this);
        findViewById(R.id.create_group_chat).setOnClickListener(this);

        mRecipientsEditor = (RecipientsEditor) findViewById(R.id.recipients_editor);
        mRecipientsEditor.setAdapter(new ChipsRecipientAdapter(this));
        mRecipientsEditor.populate(mRecipientList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_CONTACTS_PICK:
                if (data != null) {
                    Bundle bundle = data.getExtras().getBundle("result");
                    final Set<String> keySet = bundle.keySet();
                    final int recipientCount = (keySet != null) ? keySet.size() : 0;
                    final ContactList list;

                    list = ContactList.blockingGetByUris(buildUris(keySet, recipientCount));

                    String[] numbers = list.getNumbers(false);
                    insertNumbersIntoRecipientsEditor(list);
                }
                break;
        default:
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void insertNumbersIntoRecipientsEditor(ContactList list) {
        ContactList existing = mRecipientsEditor
                .constructContactsFromInput(true);
        for (Contact contact : existing) {
            list.add(contact);
        }
        mRecipientsEditor.setText(null);
        mRecipientsEditor.populate(list);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
        case R.id.recipients_selector:
            launchMultiplePhonePicker();
            break;
        case R.id.create_group_chat:
            tryCreateGroupChat();
            break;
        default:
            break;
        }
    }

    private void launchMultiplePhonePicker() {
        Intent intent = new Intent(INTENT_MULTI_PICK, Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_CONTACTS_PICK);
    }

    private void tryCreateGroupChat() {
        try {
            confirmCreateGroupChat();
        } catch (ServiceDisconnectedException e) {
            toast(R.string.rcs_service_is_not_available);
            Log.w(RCS_TAG, e);
        }catch (InviteTooManyUserException e) {
            Log.w(RCS_TAG, e);
        } catch (RemoteException e){
            Log.w(RCS_TAG,e);
        }
    }

    private void confirmCreateGroupChat() throws ServiceDisconnectedException, RemoteException,
            InviteTooManyUserException {
        if (RcsUtils.isRcsOnline()) {
            createGroupChat();
        } else {
            toast(R.string.rcs_service_is_not_available);
        }
    }

    private void createGroupChat() throws ServiceDisconnectedException, RemoteException,
            InviteTooManyUserException {
        List<String> list = mRecipientsEditor.getNumbers();
        if (list != null && list.size() > 0) {
            if (list.size() == 1) {
                String number = list.get(0).trim().replace(" ", "");
                String account = RcsUtils.getAccount().replace("+86", "");
                if (number.endsWith(account)) {
                    Toast.makeText(RcsCreateGroupChatActivity.this,
                            R.string.can_not_recipient_only_me, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            String subject = mSubjectEdit.getText().toString();
            if (!TextUtils.isEmpty(subject) && subject.length() > 0
                    && subject.replaceAll(" ", "").length() == 0) {
                Toast.makeText(RcsCreateGroupChatActivity.this, R.string.Group_name_not_fit,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(subject)) {
                subject = getString(R.string.temp_group_chat);
            }
            if (mCreateGroupChatCallback == null) {
                mCreateGroupChatCallback = new ComposeMessageCreateGroupChatCallback(this);
            }
            mCreateGroupChatCallback.onBegin();
            // TODO: Need to implement later
            long result = GroupChatApi.getInstance().create(subject, list);
            if (result == -1) {
                if (mCreateGroupChatCallback != null) {
                    mCreateGroupChatCallback.onDone(true);
                    mCreateGroupChatCallback.onEnd();
                }
            }
        }
    }

    private GroupChatManagerReceiver mRcsGroupChatReceiver = new GroupChatManagerReceiver(
            new GroupChatNotifyCallback() {

                @Override
                public void onGroupChatCreate(Bundle extras) {
                    handleRcsGroupChatCreateNotActive(extras);
                }

                @Override
                public void onMemberAliasChange(Bundle extras) {
                }

                @Override
                public void onDisband(Bundle extras) {
                }

                @Override
                public void onDeparted(Bundle extras) {
                }

                @Override
                public void onUpdateSubject(Bundle extras) {
                }

                @Override
                public void onUpdateRemark(Bundle extras) {
                }

                @Override
                public void onCreateNotActive(Bundle extras) {

                }

                @Override
                public void onBootMe(Bundle extras) {
                }

                @Override
                public void onGroupGone(Bundle extras) {
                }

                @Override
                public void onGroupInviteExpired(Bundle extras) {
                }

            });

    private void handleRcsGroupChatCreateNotActive(final Bundle extras) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mCreateGroupChatCallback != null){
                    mCreateGroupChatCallback.onDone(true);
                    mCreateGroupChatCallback.onEnd();
                }
                long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
                long threadId = extras.getLong(Parameter.EXTRA_THREAD_ID);
                startActivity(ComposeMessageActivity.createIntent(
                        RcsCreateGroupChatActivity.this, threadId));
                RcsCreateGroupChatActivity.this.finish();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mRcsGroupChatReceiver);
        super.onDestroy();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private Uri[] buildUris(final Set<String> keySet, final int newPickRecipientsCount) {
        Uri[] newUris = new Uri[newPickRecipientsCount];
        Iterator<String> it = keySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            String id = it.next();
            newUris[i++] = ContentUris.withAppendedId(Phone.CONTENT_URI, Integer.parseInt(id));
            if (i == newPickRecipientsCount) {
                break;
            }
        }
        return newUris;
    }

}
