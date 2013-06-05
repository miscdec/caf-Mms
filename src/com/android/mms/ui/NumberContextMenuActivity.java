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

import com.android.mms.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.database.Cursor;
import android.widget.CursorAdapter;
import android.util.Log;
import android.provider.Telephony.Sms;
import android.content.DialogInterface;
import android.os.Looper;

import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.view.Window;

import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View.OnCreateContextMenuListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.text.util.Linkify;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import com.android.mms.data.Contact;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Demonstrates how to write an efficient list adapter. The adapter used in this example binds
 * to an ImageView and to a TextView for each row in the list.
 *
 * To work efficiently the adapter implemented here uses two techniques:
 * - It reuses the convertView passed to getView() to avoid inflating View when it is not necessary
 * - It uses the ViewHolder pattern to avoid calling findViewById() when it is not necessary
 *
 * The ViewHolder pattern consists in storing a data structure in the tag of the view returned by
 * getView(). This data structures contains references to the views we want to bind data to, thus
 * avoiding calls to findViewById() every time getView() is invoked.
 */

public class NumberContextMenuActivity extends Activity
{
    private static final String TAG = "NumberContextMenuActivity";
    private static final int MENU_CALL            = Menu.FIRST;
    private static final int MENU_EDIT_TO_CALL    = Menu.FIRST + 1;
    private static final int MENU_ADD_TO_CONTACTS = Menu.FIRST + 2;
    private static final int MENU_SEND_MESSAGE    = Menu.FIRST + 3;

    private String mNumber = "";
    private AlertDialog mMenuDialog = null;
    boolean mExitInDB = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
       
        initUi(getIntent());
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        return super.onContextItemSelected(item);
    }

    private void initUi(Intent intent)
    {
        Uri uri = intent.getData();
        mNumber = uri.getSchemeSpecificPart();
        
        Contact contact = Contact.get(mNumber, true);
        mExitInDB = contact.existsInDatabase();
        
        if(MessageUtils.isHasCard())
        {
            showMenuWithCall(mExitInDB);
        }
        else
        {
            showMenu(mExitInDB);
        } 
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    } 

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
    
    private void call()
    {
        if(MessageUtils.isMultiSimEnabledMms())
        {
            if(MessageUtils.getActivatedIccCardCount() > 1)
            {
                showCallSelectDialog();
            }
            else
            {
                if(MessageUtils.isIccCardActivated(MessageUtils.SUB1))
                {
                    MessageUtils.dialRecipient(this, mNumber, MessageUtils.SUB1);
                }
                else if(MessageUtils.isIccCardActivated(MessageUtils.SUB2))
                {
                    MessageUtils.dialRecipient(this, mNumber, MessageUtils.SUB2);
                }
                NumberContextMenuActivity.this.finish();
            }
        }
        else
        {
            Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mNumber));
            startActivity(dialIntent);
            NumberContextMenuActivity.this.finish();
        }
    }

    private void showCallSelectDialog(){
        String[] items = new String[MessageUtils.getActivatedIccCardCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = MessageUtils.getMultiSimName(this, i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_call));
        builder.setCancelable(true);
        builder.setItems(items, new DialogInterface.OnClickListener()
        {
            public final void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(NumberContextMenuActivity.this, mNumber, MessageUtils.SUB1);
                         Looper.loop();
                        }
                    }).start();
                    NumberContextMenuActivity.this.finish();
                }
                else
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(NumberContextMenuActivity.this, mNumber, MessageUtils.SUB2);
                         Looper.loop();
                        }
                    }).start();
                    NumberContextMenuActivity.this.finish();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                NumberContextMenuActivity.this.finish();
            }
        });  
        builder.show();
        
    }

    private void editCall()
    {
        Intent editCallIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mNumber));
        startActivity(editCallIntent);
    }

    private void addToContact()
    {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, mNumber);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        this.startActivity(intent); 
    }

    private void sendMessage()
    {
        Intent sendIntent = new Intent(this, ComposeMessageActivity.class);
        sendIntent.putExtra("address", mNumber);
        sendIntent.putExtra("msg_reply", true);
        sendIntent.putExtra("exit_on_sent", true);
        this.startActivity(sendIntent);
    }
    
    private void showMenu(boolean exit)
    {
        String[] Texts = new String[] {
                            getString(R.string.menu_send_message),
                            getString(R.string.menu_save_to_contact)
                            };
        String[] texts;
        if(exit){
            texts = Arrays.copyOf(Texts, 3);
        }
        else
        {
            texts = Texts;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.message_options));
        builder.setItems(texts, new DialogInterface.OnClickListener()
            {
             public final void onClick(DialogInterface dialog, int which)
             {
                if (which == 0)
                {
                    sendMessage();
                }
                else if (which == 1)
                {
                    addToContact();
                }  
                dialog.dismiss(); 
                NumberContextMenuActivity.this.finish();
             }
            }
        );
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                NumberContextMenuActivity.this.finish();
            }
        });
        builder.setCancelable(true);
        mMenuDialog = builder.create();
        mMenuDialog.show();
    }

    private void showMenuWithCall(boolean exit)
    {
        String[] Texts = new String[] {
                            getString(R.string.menu_send_message),
                            getString(R.string.menu_call),
                            getString(R.string.menu_edit_call),
                            getString(R.string.menu_save_to_contact)
                            };
        String[] texts;
        if(exit){
            texts = Arrays.copyOf(Texts, 3);
        }
        else
        {
            texts = Texts;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.message_options));
        builder.setItems(texts, new DialogInterface.OnClickListener()
            {
             public final void onClick(DialogInterface dialog, int which)
             {
                if (which == 0)
                {
                    sendMessage();
                    NumberContextMenuActivity.this.finish();
                }
                else if (which == 1)
                {
                    call();
                }  
                else if (which == 2)
                {
                    editCall();
                    NumberContextMenuActivity.this.finish();
                }
                else if (which == 3)
                {
                    addToContact();
                    NumberContextMenuActivity.this.finish();
                }
             }
            }
        );
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                NumberContextMenuActivity.this.finish();
            }
        });
        builder.setCancelable(true);
        mMenuDialog = builder.create();
        mMenuDialog.show();
    }  
    
}
