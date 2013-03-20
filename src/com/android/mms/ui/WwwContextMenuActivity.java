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

import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.view.Window;
import android.provider.ContactsContract.Contacts;

import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View.OnCreateContextMenuListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.text.util.Linkify;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.content.DialogInterface.OnClickListener;

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

public class WwwContextMenuActivity extends Activity
{
    private static final int MENU_LOAD_URL       = Menu.FIRST;
    private static final int MENU_SAVE_TO_LABEL  = Menu.FIRST + 1;
    private static final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");  

    private String mUrlString = "";
    private AlertDialog mMenuDialog = null;
    
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
        mUrlString = uri.getSchemeSpecificPart();   
        showMenu();
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

    private void loadURL()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_connect_url);
        //builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setMessage(getString(R.string.loadurlinfo_str));
        builder.setCancelable(true);                     
        builder.setPositiveButton(R.string.yes, new OnClickListener(){
              final public void onClick(DialogInterface dialog, int which)
              {
                  loadUrl(mUrlString);
                  WwwContextMenuActivity.this.finish();                      
              }
        });
        builder.setNegativeButton(R.string.no, new OnClickListener(){
              final public void onClick(DialogInterface dialog, int which)
              {
                  WwwContextMenuActivity.this.finish();                      
              }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                WwwContextMenuActivity.this.finish();
            }
        });            
        builder.show();
        
    }

    private void loadUrl(String url)
    {
        if (!url.regionMatches(true, 0, "http://", 0, 7) 
                && !url.regionMatches(true, 0, "https://", 0, 8))
        {       
            url = "http://" + url;
        }
        url = url.replace("Http://","http://");
        url = url.replace("Https://","https://");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        
        if((url.substring(url.length()-4).compareToIgnoreCase(".mp4")==0) || (url.substring(url.length()-4).compareToIgnoreCase(".3gp")==0) ){
            intent.setDataAndType(Uri.parse(url),"video/*");
        }
        startActivity(intent);       
    }

    private void addToLabel()
    {
        Intent i = new Intent(Intent.ACTION_INSERT, BOOKMARKS_URI);
        i.putExtra("title", "");
        i.putExtra("url", mUrlString);
        i.putExtra("extend", "outside");
        startActivity(i);  
    }

    private void addToContact()
    {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra("website", mUrlString);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }


    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private void showMenu()
    {
        final String[] texts = new String[] {
                            getString(R.string.menu_connect_url),
                            getString(R.string.menu_add_to_label),
                            getString(R.string.menu_save_to_contact)                            
                            };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.message_options));
        builder.setItems(texts, new DialogInterface.OnClickListener()
            {
             public final void onClick(DialogInterface dialog, int which)
             {
                if (which == 0)
                {
                    loadURL();
                }
                else if (which == 1)
                {
                    addToLabel();
                    WwwContextMenuActivity.this.finish();                    
                }                                           
                else if (which == 2)
                {
                    addToContact();
                    WwwContextMenuActivity.this.finish();                    
                }                
             }
            }
        );
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                WwwContextMenuActivity.this.finish();
            }
        });
        builder.setCancelable(true);
        mMenuDialog = builder.create();
        mMenuDialog.show();
    }    
}
