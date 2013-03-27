/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
//import android.provider.Contacts.RecordsPhones;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.widget.Button;
import android.widget.ImageButton;
import android.content.DialogInterface;
import android.app.ProgressDialog;
import android.widget.Toast;
import android.os.Looper;
import android.widget.LinearLayout;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.CallerInfo;

import java.util.HashMap;
import java.util.LinkedList;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.ArrayList;
import android.provider.Telephony.Sms;
import com.android.mms.util.ContactInfoCache;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.LocalGroup;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.provider.LocalGroups;
import android.provider.LocalGroups.GroupColumns;
/**
 * Displays a list of recent contacts of msg entries.
 */
public class ContactGroup extends ListActivity
    implements View.OnCreateContextMenuListener {
    private static final String TAG = "ContactGroup";
    public static int mSelectedCount = 0;
    public static int mMultiselMaxitems = 0 ;
    /** The projection to use when querying the call log table */
    private final static String[] COLUMNS = new String[]{
            GroupColumns._ID,
            GroupColumns.TITLE,
            GroupColumns.COUNT
        };
    private static final Uri GROUP_LIST_URI = Groups.CONTENT_SUMMARY_URI;
    private static final String[] ADDRESS_PROJECTION = new String[] {
                Sms.ADDRESS,                
                Sms._ID
            };
    //static final Uri inboxUri = Uri.parse("content://sms/inbox?distinct=1");            
    //static final Uri inboxUri = Uri.parse("content://sms/inbox?groupby=address");
    private static final Uri recentUri = Uri.parse("content://mms-sms/recent");                        

    static final int NUMBER_COLUMN_INDEX = 0;
    static final int CALLER_NAME_COLUMN_INDEX = 1;

    /** The projection to use when querying the phones table */
    /*
    static final String[] RECORDSPHONES_PROJECTION = new String[] {
                Contacts.RecordsPhones.NUMBER,
                Contacts.RecordsPhones.DISPLAY_NAME,
            };
    *///ztemp deled
    static final int MATCHED_NUMBER_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;

    private static final int MENU_ITEM_DELETE_MULTI_SELECT_CONFIRM = 1;
    private static final int MENU_ITEM_DELETE_SELECT_ALL = 2;
    private static final int MENU_ITEM_DELETE_DESELECT_ALL = 3;

    private static final int QUERY_TOKEN = 1;
    private static final int UPDATE_TOKEN = 2;

    RecentCallsAdapter mAdapter;
    private QueryHandler mQueryHandler;
    String mVoiceMailNumber;

    private ListView mListView;
    //private ImageButton mOkButton;
    //private ImageButton mAllButton;
    private Button mOkButton;  
    private Button mAllButton;
      private ImageButton mNullButton;
    private boolean mHasSelectAll = false;
//	    private Button mClearButton;
    

    static final class ContactInfo {
        public String name;
        public String number;
        public static ContactInfo EMPTY = new ContactInfo();
    }

    public static final class RecentCallsListItemViews {
//	        CheckedTextView selectView;
        TextView line1View;
        TextView numberView;
        int count = 0;
        String name = "";
        String number = "";
    }

    static final class CallerInfoQuery {
        String number;
        String name;
    }
    private static final String DATA_JOIN_MIMETYPES = "data "
        + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)";
    private static final String PHONES_IN_GROUP_SELECT_BYGROUPID =
                         "raw_contact_id" + " IN "
                        + "(SELECT " + "data.raw_contact_id"
                        + " FROM " + DATA_JOIN_MIMETYPES
                        + " WHERE " + Data.MIMETYPE + "='" + LocalGroup.CONTENT_ITEM_TYPE
                                + "' AND " + GroupMembership.GROUP_ROW_ID
                                + "=?)"
                                + " AND "+ Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE+"'";

    /** Adapter class to fill in data for the Call Log */
    final class RecentCallsAdapter extends ResourceCursorAdapter
        implements Runnable, ViewTreeObserver.OnPreDrawListener {
        HashMap<String,ContactInfo> mContactInfo;
        private final LinkedList<CallerInfoQuery> mRequests;
        private volatile boolean mDone;
        private boolean mLoading = true;
        ViewTreeObserver.OnPreDrawListener mPreDrawListener;
        private static final int REDRAW = 1;
        private static final int START_THREAD = 2;
        private boolean mFirst;
        private Thread mCallerIdThread;

        private CharSequence[] mLabelArray;

        public boolean onPreDraw() {
            if (mFirst) {
                mHandler.sendEmptyMessageDelayed(START_THREAD, 1000);
                mFirst = false;
            }return true;
        }

        private Handler mHandler = new Handler() {
                                       @Override
                                       public void handleMessage(Message msg) {
                                           switch (msg.what) {
                                           case REDRAW:
                                               notifyDataSetChanged();
                                               break;
                                           case START_THREAD:
                                               startRequestProcessing();
                                               break;
                                           }
                                       }
                                   };

        public RecentCallsAdapter() {
            super(ContactGroup.this, R.layout.recent_calls_list, null);

            mContactInfo = new HashMap<String,ContactInfo>();
            mRequests = new LinkedList<CallerInfoQuery>();
            mPreDrawListener = null;
        }

        void setLoading(boolean loading) {
            mLoading = loading;
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }

        public void startRequestProcessing() {
            mDone = false;
            mCallerIdThread = new Thread(this);
            mCallerIdThread.setPriority(Thread.MIN_PRIORITY);
            mCallerIdThread.start();
        }

        public void stopRequestProcessing() {
            mDone = true;
            if (mCallerIdThread != null)
                mCallerIdThread.interrupt();
        }

        private void queryContactInfo(CallerInfoQuery ciq) {
            // First check if there was a prior request for the same number
            // that was already satisfied
            ContactInfo info = mContactInfo.get(ciq.number);
            if (info != null && info != ContactInfo.EMPTY) {
                synchronized (mRequests) {
                    if (mRequests.isEmpty()) {
                        mHandler.sendEmptyMessage(REDRAW);
                    }
                }
            } else {
                String number = ciq.number;//ztemp
                //number = CallerInfo.bestNumMatch(ciq.number, ContactGroup.this);//ztemp
                if(null == number) {
                    Log.i(TAG, " **number**: " + number);
                    number = ciq.number;
                }
                Log.i(TAG, "number: " + number);
                Cursor phonesCursor = null;
                if (phonesCursor != null) {
                    if (phonesCursor.moveToFirst()) {
                        info = new ContactInfo();
                        info.name = phonesCursor.getString(NAME_COLUMN_INDEX);
                        info.number = phonesCursor.getString(MATCHED_NUMBER_COLUMN_INDEX);
                        mContactInfo.put(ciq.number, info);
                        // Inform list to update this item, if in view
                        synchronized (mRequests) {
                            if (mRequests.isEmpty()) {
                                mHandler.sendEmptyMessage(REDRAW);
                            }
                        }
                    }
                    phonesCursor.close();
                }
            }
            if (info != null) {
                updateCallLog(ciq, info);
            }
        }

        /*
         * Handles requests for contact name and number type
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while (!mDone) {
                CallerInfoQuery ciq = null;
                synchronized (mRequests) {
                    if (!mRequests.isEmpty()) {
                        ciq = mRequests.removeFirst();
                    } else {
                        try {
                            mRequests.wait(1000);
                        } catch (InterruptedException ie) {
                            // Ignore and continue processing requests
                        }
                    }
                }
                if (ciq != null) {
                    queryContactInfo(ciq);
                }
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);

            // Get the views to bind to
            RecentCallsListItemViews views = new RecentCallsListItemViews();
            views.line1View = (TextView) view.findViewById(R.id.line1);
            views.numberView = (TextView) view.findViewById(R.id.number);
            views.count = 0;

            view.setTag(views);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {

            final RecentCallsListItemViews views = (RecentCallsListItemViews) view.getTag();
            String count = c.getString(2);
            String text = c.getString(1);

            if(text.equals("Family")){
                text = getString(R.string.family);
            }
            else if(text.equals("Colleagues")){
                text = getString(R.string.colleagues);
            }
            else if(text.equals("Friends")){
                text = getString(R.string.friends);
            }
            else if(text.equals("Classmates")){
                text = getString(R.string.classmates);
            }
            else if(text.equals("VIP")){
                text = getString(R.string.vip);
            }
            else if(text.equals("Partners")){
                text = getString(R.string.partners);
            }
            text += "(" + count + ")";


            views.line1View.setText(text);
            views.numberView.setVisibility(View.VISIBLE);

            if(mListView.isItemChecked(c.getPosition()))
            {
                if(view != null)
                {
                    view.setBackgroundDrawable( context.getResources().getDrawable(R.drawable.list_selected_holo_light));
                }
            }
            else
            {
                view.setBackgroundDrawable(null);
            }   
        }
    }

    private String getNameByAddress(String addr)
    {
        if (TextUtils.isEmpty(addr))
        {
            return "";
        }
        return ContactInfoCache.getInstance().getContactNameWithNull(
                ContactGroup.this, addr);
    } 

    private static final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<ContactGroup> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ContactGroup>(
                            (ContactGroup) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final ContactGroup activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                final ContactGroup.RecentCallsAdapter callsAdapter = activity.mAdapter;
                callsAdapter.setLoading(false);
                callsAdapter.changeCursor(cursor);
            } else {
                cursor.close();
            }
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        final Intent intent = getIntent();

        setContentView(R.layout.recent_calls);
        LinearLayout bt_layout = (LinearLayout)findViewById(R.id.bottom_panel);
      //  bt_layout.setBackgroundColor(this.getResources().getThemeColor("background"));
        mOkButton = (Button)findViewById(R.id.ok_button);
        mAllButton = (Button)findViewById(R.id.all_button);
        mOkButton.setOnLongClickListener(new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(ContactGroup.this, R.string.execute_menu, Toast.LENGTH_SHORT).show();
            return true;
        }
        });
        mOkButton.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                ArrayList<ContentValues> results = getResults();
                if(null == results) {
                    Toast.makeText(ContactGroup.this, R.string.no_select,
                                   Toast.LENGTH_SHORT).show();
                } else {
                    setResult(RESULT_OK, new Intent().putParcelableArrayListExtra("com.android.contacts.action.ACTION_GET_CONTENTS", results));
                    finish();
                }

             }
         });
        mAllButton.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                if(mAdapter == null || mAdapter.getCursor() == null)
                {
                    return;
                }
                if(mHasSelectAll)
                {
                     for(int index = 0; index < mAdapter.getCursor().getCount(); index++) {
                        if(mListView.isItemChecked(index)) {
                                mListView.setItemChecked(index, false);
                            }
                        }
                    mSelectedCount = 0;
                    mListView.invalidate();
                    mHasSelectAll = false;
                    //mAllButton.setImageResource(R.drawable.add_recipients_select_all);
                    mAllButton.setText(R.string.menu_selectall);
                    return;
                }
                 for(int index = 0; index < mAdapter.getCursor().getCount(); index++) {
                    if(!mListView.isItemChecked(index)) {
                        if(mSelectedCount < mMultiselMaxitems) {
                            mListView.setItemChecked(index, true);
                            mSelectedCount++;
                        }
                    }
                    if(((index + 1) < mAdapter.getCount())) {
                        Toast.makeText(ContactGroup.this, getString(R.string.operation_exceed, mMultiselMaxitems),
                                       Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                mHasSelectAll = true;
                mAllButton.setText(R.string.menu_clearall);
                mListView.invalidate();
                if(!checkResults())
                {
                     for(int index = 0; index < mAdapter.getCursor().getCount(); index++) {
                        if(mListView.isItemChecked(index)) {
                                mListView.setItemChecked(index, false);
                            }
                        }
                    mSelectedCount = 0;
                    mListView.invalidate();
                    mHasSelectAll = false;
                    mAllButton.setText(R.string.menu_selectall);
                    return;
                }
             }
         });

        mAdapter = new RecentCallsAdapter();
        mListView = getListView();
        mListView.setOnCreateContextMenuListener(this);
        setListAdapter(mAdapter);

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mQueryHandler = new QueryHandler(this);
    }

    @Override
    protected void onResume() {
        startQuery();
        super.onResume();
        mAdapter.mPreDrawListener = null; // Let it restart the thread after next draw
    }

    @Override
    protected void onPause() {
    
        super.onPause();

        // Kill the requests thread
        mAdapter.stopRequestProcessing();
    }

    @Override
    protected void onDestroy() {
        
        ArrayList<ContentValues> results = getResults();
        if(null != results) {
            Intent intent = new Intent("com.android.mms.selectedrecipients");
            intent.putParcelableArrayListExtra("com.android.contacts.action.ACTION_GET_CONTENTS", results);
            intent.putExtra("table", 1);
            sendBroadcast(intent);
        }
        super.onDestroy();
        mAdapter.stopRequestProcessing();
        Cursor cursor = mAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

    }
    private void updateCallLog(CallerInfoQuery ciq, ContactInfo ci) {
        // Check if they are different. If not, don't update.
        if (TextUtils.equals(ciq.name, ci.name)) {
            return;
        }
        ContentValues values = new ContentValues(1);
        values.put(Calls.CACHED_NAME, ci.name);
        ContactGroup.this.getContentResolver().update(
            Calls.CONTENT_URI,
            values, Calls.NUMBER + "='" + ciq.number + "'", null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                ArrayList<ContentValues> results = getResults();
                setResult(RESULT_OK, 
                    new Intent().putParcelableArrayListExtra(
                        "com.android.contacts.action.ACTION_GET_CONTENTS", results));
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void startQuery() {
        mAdapter.setLoading(true);
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        mQueryHandler.startQuery(QUERY_TOKEN, null, LocalGroups.CONTENT_URI,
                                 COLUMNS, null, null,null);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE_MULTI_SELECT_CONFIRM: {
                ArrayList<ContentValues> results = getResults();
                setResult(RESULT_OK, 
                    new Intent().putParcelableArrayListExtra(
                        "com.android.contacts.action.ACTION_GET_CONTENTS", results));
                finish();
                return true;
            }
        case MENU_ITEM_DELETE_SELECT_ALL: {
                Cursor mcursor = mAdapter.getCursor();
                int mTotalItem=mcursor.getCount();
                int position;

                for(int index = 0; index < mAdapter.getCount(); index++) {
                    if(!mListView.isItemChecked(index)) {
                        if(mSelectedCount < mMultiselMaxitems) {
                            mListView.setItemChecked(index, true);
                            mSelectedCount++;
                        }
                    }
                    if((mSelectedCount == mMultiselMaxitems) && ((index + 1) < mAdapter.getCount())) {
                        Toast.makeText(ContactGroup.this, getString(R.string.operation_exceed, mMultiselMaxitems),
                                       Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                onResume();
                return true;
            }
        case MENU_ITEM_DELETE_DESELECT_ALL: {
                mListView.clearChoices();
                mSelectedCount = 0;
                onResume();
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final RecentCallsListItemViews views = (RecentCallsListItemViews) v.getTag();
        int groupCount =  views.count;
        Log.d(TAG,"views.count= " + views.count);

        mListView.invalidateViews();
        if ( mListView.isItemChecked(position) && mSelectedCount +  groupCount > mMultiselMaxitems) 
        {
            mListView.setItemChecked(position, false);
            Toast.makeText(ContactGroup.this, getString(R.string.operation_exceed, mMultiselMaxitems),
                           Toast.LENGTH_SHORT).show();
        }
        else if(mListView.isItemChecked(position))
        {
            mSelectedCount += groupCount;
        }
    }
    
    public ArrayList<ContentValues> getResults() 
    {
        SparseBooleanArray bArrary = mListView.getCheckedItemPositions();
        ArrayList<ContentValues> results = new ArrayList<ContentValues>();
        ContentValues values;
        ArrayList<String> item;        
        int mSelectCount = 0;
        Cursor cursor = mAdapter.getCursor();         

        int countSize = bArrary.size();
        
        Log.w(TAG,"z615 countSize = " + countSize);
        Log.w(TAG,"z641 cursor = " + cursor);
        
        for(int index = 0; index < countSize; index++) 
        {
            if (bArrary.valueAt(index)) 
            {
                Log.w(TAG,"z620 bArrary.keyAt(index) = " 
                    + bArrary.keyAt(index) + ";index = " + index); 
                item = new ArrayList<String>();
                {
                    if (null != cursor) 
                    {
                        if (cursor.moveToPosition(bArrary.keyAt(index)))
                        {
                            String groupid = cursor.getString(0);           
                            String name = null;
                            String number = null;
                            String personID = null;
                            boolean number_Not_null = false;
                            Cursor membercursor = getContentResolver().query(Phone.CONTENT_URI,
                                                new String[]{Phone.RAW_CONTACT_ID, 
                                                Phone.DISPLAY_NAME, 
                                                Phone.NUMBER}, //Which columns to return. 
                                                PHONES_IN_GROUP_SELECT_BYGROUPID,       //WHERE clause--we won't specify.
                                                new String[]{groupid},  //Selection
                                                null);
                            if( membercursor != null ){
                                if(membercursor.getCount() != 0){
                                     try {
                                        while (membercursor.moveToNext()) {
                                            ArrayList<String> peopleitem = new ArrayList<String>();
                                            personID = membercursor.getString(0); 
                                            name = membercursor.getString(1);
                                            number = membercursor.getString(2);
                                            if(number != null && TextUtils.isGraphic(number) ){
                                                number_Not_null = true;
                                            }
                                            peopleitem.add(name);
                                            peopleitem.add(number);
                                            values = new ContentValues();
                                            values.putStringArrayList("com.android.contacts.action.ACTION_GET_CONTENTS" , peopleitem);
                                            results.add(values);
                                            mSelectCount++;  
                                        }
                                    } finally {
                                        membercursor.close();
                                    }
                                }

                            }
                        }
                        else
                        {
                            continue;
                        }
                    }
                    else
                    {
                        continue;
                    }
                }
            }
        }

        if (0 == mSelectCount) 
        {
            return null;
        }
     return results;
    }

    private boolean checkResults() 
    {
        SparseBooleanArray bArrary = mListView.getCheckedItemPositions();
        ArrayList<ContentValues> results = new ArrayList<ContentValues>();
        ContentValues values;
        ArrayList<String> item; 	   
        int mSelectCount = 0;
        Cursor cursor = mAdapter.getCursor();
        int countSize = bArrary.size();
        for(int index = 0; index < countSize; index++) 
        {
            if (bArrary.valueAt(index)) 
            {
                item = new ArrayList<String>();
                if (null != cursor) 
                {
                    if (cursor.moveToPosition(bArrary.keyAt(index)))
                    {
                        String groupid = cursor.getString(0);
                        String name = null;
                        String number = null;
                        String personID = null;
                        boolean number_Not_null = false;
                        Cursor membercursor = getContentResolver().query(Phone.CONTENT_URI,
                        new String[]{Phone.RAW_CONTACT_ID, 
                        Phone.DISPLAY_NAME, 
                        Phone.NUMBER}, //Which columns to return. 
                        PHONES_IN_GROUP_SELECT_BYGROUPID,//WHERE clause--we won't specify.
                        new String[]{groupid},//Selection
                        null);
                        if( membercursor != null ){
                            if(membercursor.getCount() != 0){
                                try {
                                        while (membercursor.moveToNext()) {
                                            ArrayList<String> peopleitem = new ArrayList<String>();
                                            personID = membercursor.getString(0); 
                                            name = membercursor.getString(1);
                                            number = membercursor.getString(2);
                                            if(number != null && TextUtils.isGraphic(number) ){
                                                number_Not_null = true;
                                            }
                                            peopleitem.add(name);
                                            peopleitem.add(number);
                                            values = new ContentValues();
                                            values.putStringArrayList("com.android.contacts.action.ACTION_GET_CONTENTS" , peopleitem);
                                            results.add(values);
                                            mSelectCount++;  
                                        }
                                } finally {
                                    membercursor.close();
                                    }
                            }
                        }
                    }
                    else
                    {
                        continue;
                    }
                }
                else
                {
                    continue;
                }
            }
        }
        if((mSelectCount>=mMultiselMaxitems))
        {
            Toast.makeText(ContactGroup.this, getString(R.string.operation_exceed, mMultiselMaxitems),
            Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}



