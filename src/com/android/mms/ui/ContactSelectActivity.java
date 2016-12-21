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
/*
 * BORQS Software Solutions Pvt Ltd. CONFIDENTIAL
 * Copyright (c) 2016 All rights reserved.
 *
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by BORQS Software
 * Solutions Pvt Ltd. No part of the Material may be used,copied,
 * reproduced, modified, published, uploaded,posted, transmitted,
 * distributed, or disclosed in any way without BORQS Software
 * Solutions Pvt Ltd. prior written permission.
 *
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by BORQS Software Solutions Pvt Ltd. in writing.
 *
 */
package com.android.mms.ui;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AlphabetIndexer;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.mms.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ContactSelectActivity extends Activity implements
    TextWatcher {
        private static final String TAG = "ContactSelectActivity";

        private static final String ITEM_SEP = ", ";
        private static final String CONTACT_SEP_LEFT = "[";
        private static final String CONTACT_SEP_RIGHT = "]";

        public static final String MODE = "mode";
        public static final int MODE_INFO = 1;
        public static final int MODE_VCARD = 2;

        public static final String EXTRA_INFO = "info";
        public static final String EXTRA_VCARD = "vcard";

        private int mMode = -1;

        private ListView mList = null;
        private ContactsAdapter mAdapter = null;
        private Cursor mContactCursor = null;

        private EditText mSearchText;

        private int mContactIdIndex = -1;
        private int mContactLookupIndex = -1;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // get the mode from the intent or saved instance.
            mMode = getIntent().getIntExtra(MODE, MODE_INFO);

            setContentView(R.layout.contact_select_activity);

            mSearchText = (EditText) findViewById(R.id.search_field);
            mSearchText.addTextChangedListener(this);

            mList = (ListView) findViewById(R.id.list);

            if (mAdapter == null) {
                mAdapter = new ContactsAdapter(getApplication(), this , null);
                mList.setAdapter(mAdapter);
                getContactsCursor(mAdapter.getQueryHandler(), null);
            }

            mList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                    mContactCursor.moveToPosition(position);
                    Intent intent = new Intent();
                        if (mMode == MODE_INFO) {
                            long contactId = mContactCursor.getLong(mContactIdIndex);
                            String info = getSelectedAsText(contactId);
                            Log.d(TAG, info);
                            intent.putExtra(EXTRA_INFO, info);
                        } else if (mMode == MODE_VCARD) {
                            String lookupKey = mContactCursor.getString(mContactLookupIndex);
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI,
                            lookupKey);
                            Log.d(TAG, uri.toString());
                            intent.putExtra(EXTRA_VCARD, uri.toString());
                        }
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        @Override
        protected void onDestroy() {
            if (mAdapter != null) {
                mAdapter.changeCursor(null);
                mList.setAdapter(null);
                mAdapter = null;
            }
            super.onDestroy();
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mAdapter != null) {
                Cursor cursor = mAdapter.runQueryOnBackgroundThread(s);
                init(cursor);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // do nothing
        }

        private void init(Cursor c) {
            if (mAdapter == null) {
                return;
            }
            mAdapter.changeCursor(c);
        }

        private Cursor getContactsCursor(AsyncQueryHandler async, String filter) {
            String[] cols = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.SORT_KEY_PRIMARY
            };

            Uri uri = ContactsContract.Contacts.CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,
                        Uri.encode(filter.toString()));
            }

            Cursor ret = null;
            if (async != null) {
                async.startQuery(0, null, uri,
                        cols, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
            } else {
                ret = getContentResolver().query(uri, cols, null, null,
                        ContactsContract.Contacts.SORT_KEY_PRIMARY);
            }
            return ret;
        }

        private Cursor getContactsDetailCursor(String contactId) {
            StringBuilder selection = new StringBuilder();
            selection.append(ContactsContract.Data.CONTACT_ID + "=" + contactId)
                    .append(" AND (")
                    .append(ContactsContract.Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'")
                    .append(" OR ")
                    .append(ContactsContract.Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "')");

            Cursor cursor = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI, null, selection.toString(), null, null);

            return cursor;
        }

        private class ContactsAdapter extends CursorAdapter {
            private ContactSelectActivity mActivity;
            private AsyncQueryHandler mQueryHandler;
            private String mConstraint = null;
            private boolean mConstraintIsValid = false;

            private int mDisplayNameIndex = -1;
            private int mSortedIndex = -1;

            class QueryHandler extends AsyncQueryHandler {

                public QueryHandler(ContentResolver res) {
                    super(res);
                }

                @Override
                protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                    mActivity.init(cursor);
                }
            }

            public ContactsAdapter(Context context, ContactSelectActivity activity  ,Cursor cursor) {
                super(context, cursor, 0);
                mActivity = activity;
                mQueryHandler = new QueryHandler(context.getContentResolver());
                getColumnIndex(cursor);
            }

            public AsyncQueryHandler getQueryHandler() {
                return mQueryHandler;
            }
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.contact_select_item,
                    parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor){
                TextView textView = (TextView) view.findViewById(R.id.pick_item_detail);
                String displayName = cursor.getString(mDisplayNameIndex);
                textView.setText(displayName);
                textView.setTextColor(Color.BLACK);
            }

            @Override
            public void changeCursor(Cursor cursor) {
                if (mActivity.isFinishing() && cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                if (cursor != mActivity.mContactCursor) {
                    mActivity.mContactCursor = cursor;
                    getColumnIndex(cursor);
                    super.changeCursor(cursor);
                }
            }

            @Override
            public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
                String s = constraint.toString();
                if (mConstraintIsValid && (
                        (s == null && mConstraint == null) ||
                        (s != null && s.equals(mConstraint)))) {
                    return getCursor();
                }
                Cursor c = mActivity.getContactsCursor(null, s);
                mConstraint = s;
                mConstraintIsValid = true;
                return c;
            }

            private void getColumnIndex(Cursor cursor) {
                if (cursor == null) {
                    Log.w(TAG, "getColumnsIndex, the cursor is null, couldn't get the index.");
                    return;
                }
                mContactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
                mContactLookupIndex = cursor
                        .getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY);
                mDisplayNameIndex = cursor
                        .getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
                mSortedIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.SORT_KEY_PRIMARY);

            }
        }

        public String getSelectedAsText(Long contactId) {
            StringBuilder result = new StringBuilder();
            result.append(CONTACT_SEP_LEFT);
            result.append(getString(R.string.contact_info_text_as_name));
            Cursor contactCursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME
                },
                ContactsContract.Contacts._ID + "=" + contactId, null, null);
            try {
                    if (contactCursor != null && contactCursor.moveToFirst()) {
                        result.append(contactCursor.getString(0));
                    }
            } finally {
                if (contactCursor != null) {
                    contactCursor.close();
                    contactCursor = null;
                }
            }
            Cursor cursor = getContactsDetailCursor(Long.toString(contactId));
                try {
                    if (cursor != null) {
                        int mimeIndex = cursor
                                .getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
                        int phoneIndex = cursor.getColumnIndexOrThrow(Phone.NUMBER);
                        int emailIndex = cursor.getColumnIndexOrThrow(Email.ADDRESS);
                        if(cursor.moveToFirst()) {
                            result.append(ITEM_SEP);
                                String mimeType = cursor.getString(mimeIndex);
                                if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                                    result.append(getString(R.string.contact_info_text_as_phone));
                                    result.append(cursor.getString(phoneIndex));
                                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                                    result.append(getString(R.string.contact_info_text_as_email));
                                    result.append(cursor.getString(emailIndex));
                                }
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                }
                result.append(CONTACT_SEP_RIGHT);
            return result.toString();
        }
}