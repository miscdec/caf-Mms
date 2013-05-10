
package com.android.mms.ui;

import com.android.mms.R;
import com.google.android.mms.pdu.PduHeaders;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.preference.ListPreference;
import android.preference.Preference;
import android.content.SharedPreferences;
import android.preference.PreferenceScreen;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.app.Activity;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.preference.DialogPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference;
import android.util.Log;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.app.Dialog;
import android.widget.Button;
import android.view.Gravity;
import android.content.Intent;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;
import android.widget.AdapterView.OnItemLongClickListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.content.res.Resources;
import android.app.ActionBar;
/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class SMSTemplateActivity extends Activity 
        implements OnItemClickListener, DialogInterface.OnClickListener
{
    public  static final int ADD_ID = Menu.FIRST;
    public  static final int DEL_ID = Menu.FIRST + 1;
    private static final int MAXIMUM = 20;
    private static final int LENGTH_MAXIMUM = 50;

    private ListView lv;
    private ArrayAdapter<String> adapter;
    private String[] templates;
    private int tCount;
    private int whichItem;
    private EditText edt;
    private String templatesStr;
    private String division;
    private String space;
    public SharedPreferences tempatespre;
    private int whichItemdel;
    private TextView emptyView;

    private DialogInterface.OnClickListener mMsgUpdateDialogClickListener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            if (which == DialogInterface.BUTTON_POSITIVE)
            {
                int index = 0;
                String newStr = edt.getText().toString();
                if (TextUtils.isEmpty(newStr))
                {
                    Toast.makeText(SMSTemplateActivity.this, R.string.invalid_template, 
                        Toast.LENGTH_LONG).show();
                    return;
                }                  
                templatesStr = new String();
                templates[whichItem] = newStr;
                while (index < templates.length)
                {
                    templatesStr += templates[index];
                    templatesStr += division;
                    index++;
                }
                templatesStr = templatesStr.substring(0,templatesStr.length()-1);
                templates = templatesStr.split(division);
                //update the view
                lv = new ListView(SMSTemplateActivity.this);
                adapter = new ArrayAdapter<String>(SMSTemplateActivity.this,
                                android.R.layout.simple_list_item_1, templates);
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(SMSTemplateActivity.this);
                lv.setOnItemLongClickListener(mMsgListLongClickListener);
                lv.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
                setContentView(lv);
            }
        }
    };

    private OnItemLongClickListener mMsgListLongClickListener =new OnItemLongClickListener()
    {
        public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id)
        {
            whichItemdel = position;
            return false;
        }

    };
    
    private final OnCreateContextMenuListener mMsgListMenuCreateListener =
        new OnCreateContextMenuListener()
        {
            public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo)
            {
                menu.clear();
                menu.setHeaderTitle(R.string.option_title);
                menu.add(0, DEL_ID, 0, R.string.menu_delete_msg);
            }
        };

    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setTitle(getResources().getString(R.string.sms_template_title));
        int index = 0;
        tempatespre = getSharedPreferences("SMSTemplate",0);
        
        tCount = tempatespre.getInt("templatecount", 10);
        lv = new ListView(this);
        char newChar = 0x01;
        char sp = 0x20;
        division = "\t";
        space = String.valueOf(sp);
        if (tCount > 0)
        {
            if (tempatespre.getBoolean("init", true))
            {
                templatesStr = getResources().getString(R.string.sms_template);
                templates = templatesStr.split("~");
                templatesStr = "";
                for (int strindex = 0; strindex < templates.length; strindex++)
                {
                    templatesStr += templates[strindex];
                    if (strindex != templates.length - 1)
                    {
                        templatesStr += division;
                    }
                }
                tempatespre.edit().putString("templates", templatesStr).commit();
                tempatespre.edit().putBoolean("init", false).commit();
            }
            else
            {
                templatesStr = tempatespre.getString("templates", "");
                templates = templatesStr.split(division);
            }
            tCount = templates.length;
            adapter = new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1, templates);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(this);
            lv.setOnItemLongClickListener(mMsgListLongClickListener);
            lv.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
            setContentView(lv);
        }
        else
        {
            templatesStr = null;
            emptyView = new TextView(this);
            emptyView.setText(R.string.empty_template);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setTextSize(22);            
            setContentView(emptyView);
        }

       ActionBar actionBar = getActionBar();
       actionBar.setDisplayHomeAsUpEnabled(true); 
    }

    public void onItemClick(AdapterView<?> arg,View arg1,int arg2,long arg3)
    {
        AlertDialog.Builder b = new AlertDialog.Builder(this);

        b = new AlertDialog.Builder(this)
            .setTitle(R.string.sms_template_title)
            .setPositiveButton(R.string.button_ok, mMsgUpdateDialogClickListener)
            .setNegativeButton(R.string.cancel, mMsgUpdateDialogClickListener);

        AlertDialog mTemplateDialog = b.create();
        edt = new EditText(mTemplateDialog.getContext());
        edt.setFilters(new InputFilter[] { new LengthFilter(LENGTH_MAXIMUM)});
        FrameLayout outLayout = new FrameLayout(mTemplateDialog.getContext());
        LinearLayout inLayout1 = new LinearLayout(mTemplateDialog.getContext());
        Button addBtn = new Button(mTemplateDialog.getContext());
        Button delBtn = new Button(mTemplateDialog.getContext());
        inLayout1.setOrientation(LinearLayout.HORIZONTAL);
        inLayout1.setGravity(Gravity.CENTER_HORIZONTAL);
        inLayout1.addView(addBtn);
        inLayout1.addView(delBtn);
        outLayout.addView(edt);
        if (tCount >= arg2 && templates[arg2] != null)
        {
            edt.setText(templates[arg2]);
            edt.setSelection(templates[arg2].length());
        }
        whichItem = arg2;

        mTemplateDialog.setView(outLayout);
        mTemplateDialog.show();
    }
    
    public void onClick(DialogInterface dialog, int which)
    {
        if (which == DialogInterface.BUTTON_POSITIVE)
        {
            String tmp = edt.getText().toString();
            if (TextUtils.isEmpty(tmp))
            {
                Toast.makeText(SMSTemplateActivity.this, R.string.invalid_template, 
                    Toast.LENGTH_LONG).show();
                return;
            }  

            if (TextUtils.isEmpty(templatesStr))
            {
                templates = new String[1];
                whichItem = 0;
            }    
            else
            {
                if (!templatesStr.endsWith(division))
                {
                    templatesStr += division;
                }
                templatesStr += space;
                templates = templatesStr.split(division);
                if (templates != null)
                {
                    whichItem = templates.length - 1;
                }
            }

            templates[whichItem] = tmp;

            if (!TextUtils.isEmpty(templatesStr))
            {
                templatesStr = templatesStr.substring(0, templatesStr.length()-1);
                templatesStr += templates[whichItem];
            }
            else
            {                
                templatesStr = templates[whichItem];
            }
            tCount++;
        }
        else
        {
            return;
        }      

        lv = new ListView(this);
        adapter = new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1, templates);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(mMsgListLongClickListener);
        lv.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
        setContentView(lv);
    }

    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, ADD_ID, 0, R.string.menu_create);
        return true;
    }
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case ADD_ID:
                if (tCount >= MAXIMUM)
                {
                    Toast.makeText(this, 
                        getString(R.string.max_template, MAXIMUM), 
                        Toast.LENGTH_LONG).show();
                    return true;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b = new AlertDialog.Builder(this)
                    .setTitle(R.string.new_template)
                    .setPositiveButton(R.string.button_ok, this)
                    .setNegativeButton(R.string.cancel, this);
                AlertDialog mTemplateDialog = b.create();
                edt = new EditText(mTemplateDialog.getContext());
                edt.setFilters(new InputFilter[] {new LengthFilter(LENGTH_MAXIMUM)});
                FrameLayout outLayout = new FrameLayout(mTemplateDialog.getContext());
                outLayout.addView(edt);
                mTemplateDialog.setView(outLayout);
                mTemplateDialog.show();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case DEL_ID:
                tCount--;
                int index = 0;
                templatesStr = new String();                
                while (index < templates.length-1)
                {
                    if (index == whichItemdel)
                    {
                        index++;
                        continue;
                    }
                    templatesStr += templates[index];
                    if (index != whichItemdel)
                    {
                        templatesStr += division;
                    }
                    index ++;
                }
                
                if (index != whichItemdel)
                {
                    templatesStr += templates[index];
                }
                
                if (!TextUtils.isEmpty(templatesStr))
                {
                    templates = templatesStr.split(division);
                }
                else
                {
                    templates = null;
                }

                if (templates != null)
                {
                    //update the view
                    lv = new ListView(SMSTemplateActivity.this);                    
                    adapter = new ArrayAdapter<String>(SMSTemplateActivity.this,
                                android.R.layout.simple_list_item_1,templates);
                    lv.setAdapter(adapter);
                    lv.setOnItemClickListener(SMSTemplateActivity.this);
                    lv.setOnItemLongClickListener(mMsgListLongClickListener);
                    lv.setOnCreateContextMenuListener(mMsgListMenuCreateListener);
                    setContentView(lv);
                }
                else
                {
                    emptyView = new TextView(this);
                    emptyView.setText(R.string.empty_template);
                    emptyView.setGravity(Gravity.CENTER);                
                    emptyView.setTextSize(22);
                    setContentView(emptyView);
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override 
    protected void onPause()
    {
        super.onPause();        
        int index = 0;
        templatesStr = new String();
        if (templates != null)
        {
            while (index < templates.length-1)
            {
                templatesStr += templates[index];
                templatesStr += division;
                index ++;
            }
            templatesStr += templates[index];
        }
        tempatespre.edit().putString("templates", templatesStr).commit();
        tempatespre.edit().putInt("templatecount", tCount).commit();
        tempatespre.edit().putBoolean("init", false).commit();
    }
}
