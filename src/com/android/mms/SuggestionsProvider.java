/*
 * Copyright (C) 2009 Google Inc.
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

package com.android.mms;

import java.util.ArrayList;
import java.util.HashSet;
import android.util.Log;
import java.util.Map;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.content.Context;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.provider.ContactsContract.PhoneLookup;

/**
 * Suggestions provider for mms.  Queries the "words" table to provide possible word suggestions.
 */
public class SuggestionsProvider extends android.content.ContentProvider {

    final static String AUTHORITY = "com.android.mms.SuggestionsProvider";
    final static String TAG = "SuggestionsProvider";
//    final static int MODE = DATABASE_MODE_QUERIES + DATABASE_MODE_2LINES;

    public SuggestionsProvider() {
        super();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String number = getAddressByName(getContext(), selectionArgs[0]);
        Log.v(TAG, "query : the number:" + number);
        if(TextUtils.isEmpty(number)){
            Log.v(TAG, "query : the selectionArgs[0]" + selectionArgs[0]);
            Uri u = Uri.parse(String.format(
                    "content://mms-sms/searchSuggest?pattern=%s",
                    selectionArgs[0]));
            Cursor c = getContext().getContentResolver().query(
                    u,
                    null,
                    null,
                    null,
                    null);
            if (c == null) {
                return null;
            }

            return new SuggestionsCursor(c, selectionArgs[0]);
        }else{
            Uri u = Uri.parse(String.format(
                "content://mms-sms/searchSuggest?pattern=%s",
                number));
            Cursor c = getContext().getContentResolver().query(
                u,
                null,
                null,
                null,
                null);
            if (c == null) {
                return null;
            }
            return new SuggestionsCursor(c, number);
        }

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public static boolean isNumber(String str){
        if(str == null)
            return false;
        for(int i=0;i<str.length();i++){
            if(!Character.isDigit(str.charAt(i))){
                return false;
            }
        }
        
        return true;
    }
    
    public static String getAddressByName(Context context, String name)
    {    
        Log.d(TAG, "getAddressByName : name = " + name);
        String resultAddr = "";
        Uri nameUri = null;
        if (TextUtils.isEmpty(name)) 
        {
            return resultAddr;
        }

        Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
            new String[] {ContactsContract.Data.RAW_CONTACT_ID},
                ContactsContract.Data.MIMETYPE + " =? AND " + StructuredName.DISPLAY_NAME + " =? "  ,                    
            new String[] {StructuredName.CONTENT_ITEM_TYPE, name}, null);

        if (c == null) 
        {              
            return resultAddr;
        }
        
        if (!c.moveToFirst())
        {
            c.close();
            return resultAddr;
        }
        final int SUMMARY_ID_COLUMN_INDEX = 0;
        final long raw_contact_id = c.getLong(SUMMARY_ID_COLUMN_INDEX);
        Log.v(TAG, "getAddressByName : raw_contact_id = " + raw_contact_id);        
        c.close();        

        resultAddr = queryPhoneNumbersWithRaw(context, raw_contact_id);        
        Log.v(TAG, "getAddressByName : resultAddr = " + resultAddr);
        return resultAddr;        
    }
    
    private static String queryPhoneNumbersWithRaw(Context context, long rawContactId) 
    {
        Cursor c = null;        
        String addrs = "";        
        try
        {
            c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {Phone.NUMBER}, Phone.RAW_CONTACT_ID + " = " + rawContactId,
                    null, null);

            if (c != null && c.moveToFirst()) 
            {
                int i = 0;
                while (!c.isAfterLast())
                {
                    String addrValue = c.getString(0);
                    if (!TextUtils.isEmpty(addrValue))
                    {
                        if (i == 0)
                        {
                            addrs = addrValue;
                        }
                        else
                        {
                            addrs = addrs + "," + addrValue;
                        }                        
                        i++;
                    }
                    c.moveToNext();
                }                
            } 
        }
        finally 
        {
            if (c != null) {
                c.close();
            }
        }  
        return addrs;        
    }

    private class SuggestionsCursor implements CrossProcessCursor {
        Cursor mDatabaseCursor;
        int mColumnCount;
        int mCurrentRow;
        ArrayList<Row> mRows = new ArrayList<Row>();
        String mQuery;

        public SuggestionsCursor(Cursor cursor, String query) {
            mDatabaseCursor = cursor;
            mQuery = query;

            mColumnCount = cursor.getColumnCount();
            try {
                computeRows();
            } catch (SQLiteException ex) {
                // This can happen if the user enters -n (anything starting with -).
                // sqlite3/fts3 can't handle it.  Google for "logic error or missing database fts3"
                // for commentary on it.
                mRows.clear(); // assume no results
            }
        }

        public int getType(int columnIndex)
        {
            return mDatabaseCursor.getType(columnIndex);
        }        

        public int getCount() {
            return mRows.size();
        }

        private class Row {
            public Row(int row, String text, int startOffset, int endOffset) {
                mText = text;
                mRowNumber = row;
                mStartOffset = startOffset;
                mEndOffset = endOffset;
            }
            String mText;
            int mRowNumber;
            int mStartOffset;
            int mEndOffset;

            public String getWord() {
                return mText;
            }
        }

        /*
         * Compute rows for rows in the cursor.  The cursor can contain duplicates which
         * are filtered out in the while loop.  Using DISTINCT on the result of the
         * FTS3 snippet function does not work so we do it here in the code.
         */
        private void computeRows() {
            HashSet<String> got = new HashSet<String>();
            int textColumn = mDatabaseCursor.getColumnIndex("body");

            int count = mDatabaseCursor.getCount();
            for (int i = 0; i < count; i++) {
                mDatabaseCursor.moveToPosition(i);
                String message = mDatabaseCursor.getString(textColumn);
				mRows.add(new Row(i, message, 0, 0));
            }
                mDatabaseCursor.moveToPosition(-1);            
        }

        public void fillWindow(int position, CursorWindow window) {
            int count = getCount();
            if (position < 0 || position > count + 1) {
                return;
            }
            window.acquireReference();
            try {
                int oldpos = getPosition();
                int pos = position;
                window.clear();
                window.setStartPosition(position);
                int columnNum = getColumnCount();
                window.setNumColumns(columnNum);
                while (moveToPosition(pos) && window.allocRow()) {
                    for (int i = 0; i < columnNum; i++) {
                        String field = getString(i);
                        if (field != null) {
                            if (!window.putString(field, pos, i)) {
                                window.freeLastRow();
                                break;
                            }
                        } else {
                            if (!window.putNull(pos, i)) {
                                window.freeLastRow();
                                break;
                            }
                        }
                    }
                    ++pos;
                }
                moveToPosition(oldpos);
            } catch (IllegalStateException e){
                // simply ignore it
            } finally {
                window.releaseReference();
            }
        }

        public CursorWindow getWindow() {
            return null;
        }

        public boolean onMove(int oldPosition, int newPosition) {
            return ((CrossProcessCursor)mDatabaseCursor).onMove(oldPosition, newPosition);
        }

        /*
         * These "virtual columns" are columns which don't exist in the underlying
         * database cursor but are exported by this cursor.  For example, we compute
         * a "word" by taking the substring of the full row text in the words table
         * using the provided offsets.
         */
        private String [] mVirtualColumns = new String [] {
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
            };

        // Cursor column offsets for the above virtual columns.
        // These columns exist after the natural columns in the
        // database cursor.  So, for example, the column called
        // SUGGEST_COLUMN_TEXT_1 comes 3 after mDatabaseCursor.getColumnCount().
        private final int INTENT_DATA_COLUMN = 0;
        private final int INTENT_ACTION_COLUMN = 1;
        private final int INTENT_EXTRA_DATA_COLUMN = 2;
        private final int INTENT_TEXT_COLUMN = 3;


        public int getColumnCount() {
            return mColumnCount + mVirtualColumns.length;
        }

        public int getColumnIndex(String columnName) {
            for (int i = 0; i < mVirtualColumns.length; i++) {
                if (mVirtualColumns[i].equals(columnName)) {
                    return mColumnCount + i;
                }
            }
            return mDatabaseCursor.getColumnIndex(columnName);
        }

        public String [] getColumnNames() {
            String [] x = mDatabaseCursor.getColumnNames();
            String [] y = new String [x.length + mVirtualColumns.length];

            for (int i = 0; i < x.length; i++) {
                y[i] = x[i];
            }

            for (int i = 0; i < mVirtualColumns.length; i++) {
                y[x.length + i] = mVirtualColumns[i];
            }

            return y;
        }

        public boolean moveToPosition(int position) {
            if (position >= 0 && position < mRows.size()) {
                mCurrentRow = position;
                mDatabaseCursor.moveToPosition(mRows.get(position).mRowNumber);
                return true;
            } else {
                return false;
            }
        }

        public boolean move(int offset) {
            return moveToPosition(mCurrentRow + offset);
        }

        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        public boolean moveToLast() {
            return moveToPosition(mRows.size() - 1);
        }

        public boolean moveToNext() {
            return moveToPosition(mCurrentRow + 1);
        }

        public boolean moveToPrevious() {
            return moveToPosition(mCurrentRow - 1);
        }

        public String getString(int column) {
            // if we're returning one of the columns in the underlying database column
            // then do so here
            if (column < mColumnCount) {
                return mDatabaseCursor.getString(column);
            }

            // otherwise we're returning one of the synthetic columns.
            // the constants like INTENT_DATA_COLUMN are offsets relative to
            // mColumnCount.
            Row row = mRows.get(mCurrentRow);
            switch (column - mColumnCount) {
                case INTENT_DATA_COLUMN:
                    Uri u = Uri.parse("content://mms-sms/search").buildUpon().appendQueryParameter("pattern", row.getWord()).build();
                    return u.toString();
                case INTENT_ACTION_COLUMN:
                    return Intent.ACTION_SEARCH;
                case INTENT_EXTRA_DATA_COLUMN:
                    return getString(getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                case INTENT_TEXT_COLUMN:
                    return row.getWord();
                default:
                    return null;
            }
        }

        public void abortUpdates() {
        }

        public void close() {
            mDatabaseCursor.close();
        }

        public boolean commitUpdates() {
            return false;
        }

        public boolean commitUpdates(Map<? extends Long, ? extends Map<String, Object>> values) {
            return false;
        }

        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            mDatabaseCursor.copyStringToBuffer(columnIndex, buffer);
        }

        public void deactivate() {
            mDatabaseCursor.deactivate();
        }

        public boolean deleteRow() {
            return false;
        }

        public byte[] getBlob(int columnIndex) {
            return null;
        }

        public int getColumnIndexOrThrow(String columnName)
                throws IllegalArgumentException {
            return 0;
        }

        public String getColumnName(int columnIndex) {
            return null;
        }

        public double getDouble(int columnIndex) {
            return 0;
        }

        public Bundle getExtras() {
            return Bundle.EMPTY;
        }

        public float getFloat(int columnIndex) {
            return 0;
        }

        public int getInt(int columnIndex) {
            return 0;
        }

        public long getLong(int columnIndex) {
            return 0;
        }

        public int getPosition() {
            return mCurrentRow;
        }

        public short getShort(int columnIndex) {
            return 0;
        }

        public boolean getWantsAllOnMoveCalls() {
            return false;
        }

        public boolean hasUpdates() {
            return false;
        }

        public boolean isAfterLast() {
            return mCurrentRow >= mRows.size();
        }

        public boolean isBeforeFirst() {
            return mCurrentRow < 0;
        }

        public boolean isClosed() {
            return mDatabaseCursor.isClosed();
        }

        public boolean isFirst() {
            return mCurrentRow == 0;
        }

        public boolean isLast() {
            return mCurrentRow == mRows.size() - 1;
        }

        public boolean isNull(int columnIndex) {
            return false;  // TODO revisit
        }

        public void registerContentObserver(ContentObserver observer) {
            mDatabaseCursor.registerContentObserver(observer);
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mDatabaseCursor.registerDataSetObserver(observer);
        }

        public boolean requery() {
            return false;
        }

        public Bundle respond(Bundle extras) {
            return mDatabaseCursor.respond(extras);
        }

        public void setNotificationUri(ContentResolver cr, Uri uri) {
            mDatabaseCursor.setNotificationUri(cr, uri);
        }

        public boolean supportsUpdates() {
            return false;
        }

        public void unregisterContentObserver(ContentObserver observer) {
            mDatabaseCursor.unregisterContentObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDatabaseCursor.unregisterDataSetObserver(observer);
        }

        public boolean updateBlob(int columnIndex, byte[] value) {
            return false;
        }

        public boolean updateDouble(int columnIndex, double value) {
            return false;
        }

        public boolean updateFloat(int columnIndex, float value) {
            return false;
        }

        public boolean updateInt(int columnIndex, int value) {
            return false;
        }

        public boolean updateLong(int columnIndex, long value) {
            return false;
        }

        public boolean updateShort(int columnIndex, short value) {
            return false;
        }

        public boolean updateString(int columnIndex, String value) {
            return false;
        }

        public boolean updateToNull(int columnIndex) {
            return false;
        }
    }
    
}
