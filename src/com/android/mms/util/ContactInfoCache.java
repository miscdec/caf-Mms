/*
 * Copyright (C) 2008 Esmertec AG.
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

package com.android.mms.util;

import com.android.mms.ui.MessageUtils;
import com.google.android.mms.util.SqliteWrapper;
   
import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import com.android.mms.data.Contact;
   
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

   
import java.util.ArrayList;
import java.util.List;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;
import android.provider.Telephony.Sms;
import com.android.mms.MmsConfig;

   
/**
 * This class caches query results of contact database and provides convenient
 * methods to return contact display name, etc.
 *
 * TODO: To improve performance, we should make contacts query by ourselves instead of
 *       doing it one by one calling the CallerInfo API. In the long term, the contacts
 *       database could have a caching layer to ease the work for all apps.
 */
public class ContactInfoCache {
    private static final String TAG = "Mms/cache";

    private static final boolean LOCAL_DEBUG = true;

    public static final String SEPARATOR = ";";

    // query params for caller id lookup
    // TODO this query uses non-public API. Figure out a way to expose this functionality
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
                    + "(SELECT raw_contact_id "
                    + " FROM phone_lookup"
                    + " WHERE normalized_number GLOB('+*'))";

    // Utilizing private API
    private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;

    private static final String[] CALLER_ID_PROJECTION = new String[] {
            Phone.NUMBER,                   // 0
            Phone.LABEL,                    // 1
            Phone.DISPLAY_NAME,             // 2
            Phone.CONTACT_ID,               // 3
            Phone.CONTACT_PRESENCE,         // 4
            Phone.CONTACT_STATUS,           // 5
    };
    private static final int PHONE_NUMBER_COLUMN = 0;
    private static final int PHONE_LABEL_COLUMN = 1;
    private static final int CONTACT_NAME_COLUMN = 2;
    private static final int CONTACT_ID_COLUMN = 3;
    private static final int CONTACT_PRESENCE_COLUMN = 4;
    private static final int CONTACT_STATUS_COLUMN = 5;
   

    private final Uri mRawUri = Uri.withAppendedPath(Sms.CONTENT_URI, "raw");    
   
    // query params for contact lookup by email
    private static final Uri EMAIL_WITH_PRESENCE_URI = Data.CONTENT_URI;

    private static final String EMAIL_SELECTION = Email.DATA + "=? AND " + Data.MIMETYPE + "='"
            + Email.CONTENT_ITEM_TYPE + "'";

    private static final String[] EMAIL_PROJECTION = new String[] {
            Email.DISPLAY_NAME,           // 0
            Email.CONTACT_PRESENCE,       // 1
            Email.CONTACT_ID,             // 2
            Phone.DISPLAY_NAME,           //
    };
    private static final int EMAIL_NAME_COLUMN = 0;
    private static final int EMAIL_STATUS_COLUMN = 1;
    private static final int EMAIL_ID_COLUMN = 2;
    private static final int EMAIL_CONTACT_NAME_COLUMN = 3;

    private static ContactInfoCache sInstance;

    private final Context mContext;

    // cached contact info
    private final HashMap<String, CacheEntry> mCache = new HashMap<String, CacheEntry>();
       
    private static final int CONTACT_METHOD_NAME_COLUMN = 0;
    private static final int CONTACT_METHOD_STATUS_COLUMN = 1;
    private static final int CONTACT_METHOD_ID_COLUMN = 2;


    private String[] mContactInfoSelectionArgs = new String[1];
    // for background cache rebuilding
    private Thread mCacheRebuilder = null;
    private Object mCacheRebuildLock = new Object();
    private boolean mPhoneCacheInvalidated = false;
    private boolean mEmailCacheInvalidated = false;

    private Object mlongSmsLock = new Object();    
    public static final HashMap<LongSmsTracker, Uri> s_needUpdateMessageList = new HashMap<LongSmsTracker, Uri>();
    public static class LongSmsTracker implements Serializable {
        public String mSenderAddress = "";
        public int mMessageRef = -1;
        public int mCountAll = -1;        
        public int mSubID = -1;
        public LongSmsTracker(int messageRef, int countAll, String sender, int subID) 
        {
            mMessageRef = messageRef;
            mCountAll = countAll;
            mSenderAddress = (sender == null) ? "" : sender;
            mSubID = subID;
        }      
        
        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof LongSmsTracker) {
                LongSmsTracker s = (LongSmsTracker) object;
                if (mMessageRef == s.mMessageRef
                    && mCountAll == s.mCountAll
                    && mSenderAddress.equals((s.mSenderAddress == null) ? "" : s.mSenderAddress)
                    && mSubID == s.mSubID)
                {
                    Log.w(TAG,"z274 true");
                    return true;
                }
            }
            Log.w(TAG,"z278 false");
            return false;
        }   
        @Override
        public int hashCode() {
            String all = mSenderAddress 
                            + String.valueOf(mCountAll)
                            + String.valueOf(mMessageRef)
                            + String.valueOf(mSubID);
            return all.hashCode();
        }        
    }


    public void removeLongSmsTracker(LongSmsTracker tracker) {
        synchronized (mlongSmsLock) {        
            s_needUpdateMessageList.remove(tracker);
        }
    }
    
    public void addLongSmsTracker(LongSmsTracker tracker, Uri uri) {
        synchronized (mlongSmsLock) {    
            s_needUpdateMessageList.put(tracker, uri);
        }
    }

    public void clearLongSmsTracker(Context context) {    
        synchronized (mlongSmsLock) {    
            s_needUpdateMessageList.clear();
        }
        clearLongSmsDatabase(context);
    }

    public int clearLongSmsDatabase(Context context) {
        //int result = context.getContentResolver().delete(mRawUri, null, null);
        StringBuilder where = new StringBuilder("is_stale = 1");
        int result = context.getContentResolver().delete(mRawUri, 
                                                where.toString(), null);        
        Log.w("MMS:", "z189 result = " + result);        
        return result;
    }


    public void removeLongSmsTrackerByUri(Context context, Uri uri) {
        if (s_needUpdateMessageList.size() <= 0
            || !s_needUpdateMessageList.containsValue(uri))
        {
            return;
        }
        synchronized (mlongSmsLock) 
        {                
            //ArrayList<LongSmsTracker> smsTrackerList = new ArrayList<LongSmsTracker>();
            Set<Entry<LongSmsTracker, Uri>> entrySet = s_needUpdateMessageList.entrySet();
            for (Entry<LongSmsTracker, Uri> entry : entrySet) 
            {
                if (uri.equals(entry.getValue()))
                {
                    LongSmsTracker tracker = entry.getKey();  
                    deleteAllReceivedParts(context, tracker);
                    //smsTrackerList.add(tracker);
                    s_needUpdateMessageList.remove(tracker);
                    return;
                    //removeLongSmsTracker(tracker);
                }
            }
            /*
            int i = 0;
            int count = 0;
            for (count = smsTrackerList.size(); i < count; i++) 
            {
                LongSmsTracker tempTracker = smsTrackerList.get(i);
                s_needUpdateMessageList.remove(tempTracker);
            } 
            */
        }
    }

    public void validateLongSmsTracker(Context context) {
        if (s_needUpdateMessageList.size() <= 0)
        {
            return;
        }
        Log.w("MMS:", "z229 validateLongSmsTracker " + s_needUpdateMessageList.size());  
        synchronized (mlongSmsLock) 
        {                    
            ArrayList<LongSmsTracker> smsTrackerList = new ArrayList<LongSmsTracker>();        
            Set<Entry<LongSmsTracker, Uri>> entrySet = s_needUpdateMessageList.entrySet();        
            for (Entry<LongSmsTracker, Uri> entry : entrySet) 
            {
                Uri uri = entry.getValue();
                if (!isValidUri(context, uri))
                {
                    //if this is a invalid uri then del it.
                    LongSmsTracker tracker = entry.getKey();  
                    deleteAllReceivedParts(context, tracker);
                    smsTrackerList.add(tracker);
                    //s_needUpdateMessageList.remove(tracker);
                    //removeLongSmsTracker(tracker);
                }
            }
            int i = 0;
            int count = 0;
            for (count = smsTrackerList.size(); i < count; i++) 
            {
                LongSmsTracker tempTracker = smsTrackerList.get(i);
                s_needUpdateMessageList.remove(tempTracker);
            }            
        }
    }

    private int deleteAllReceivedParts(Context context, 
                                    LongSmsTracker tracker)
    {
        int messageRef = tracker.mMessageRef;
        int countAll = tracker.mCountAll;
        int subID = tracker.mSubID;
        String senderAddress = tracker.mSenderAddress;
        if (TextUtils.isEmpty(senderAddress) || messageRef < 0)
        {
            return 0;
        }        
        StringBuilder where = new StringBuilder("reference_number =");
        where.append(messageRef);
        where.append(" AND count =");
        where.append(countAll);
        where.append(" AND sub_id =");
        where.append(subID);
        where.append(" AND address = ?");
        String[] whereArgs = new String[] {senderAddress};        
        int result = context.getContentResolver().delete(mRawUri, where.toString(), whereArgs);
        Log.w("MMS:", "z260 result = " + result);
        return result;
    }


    private boolean isValidUri(Context context, Uri uri)
    {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                uri, null, null, null, null);
        if (cursor != null)
        {
            try 
            {
                if (cursor.getCount() > 0)
                {
                    return true;
                }
            }
            finally 
            {
                cursor.close();
            }
        }
        return false;            
    }
    
    public Uri getLongSmsUri(LongSmsTracker tracker) {
        synchronized (mlongSmsLock) 
        {             
            return s_needUpdateMessageList.get(tracker);
        }
    }

       

    /**
     * CacheEntry stores the caller id or email lookup info.
     */
    public class CacheEntry {
        /**
         * phone number
         */
        public String phoneNumber;
        /**
         * phone label
         */
        public String phoneLabel;
        /**
         * name of the contact
         */
        public String name;
        /**
         * the contact id in the contacts people table
         */
        public long person_id;
        /**
         * the presence icon resource id
         */
        public int presenceResId;
        /*
         * custom presence
         */
        public String presenceText;
        /**
         * Avatar image for this contact.
         */
        public BitmapDrawable mAvatar;

        /**
         * If true, it indicates the CacheEntry has old info. We want to give the user of this
         * class a chance to use the old info, as it can still be useful for displaying something
         * rather than nothing in the UI. But this flag indicates that the CacheEntry needs to be
         * updated.
         */
        private boolean isStale;

        /**
         * Returns true if this CacheEntry needs to be updated. However, cache may still contain
         * the old information.
         *
         */
        public boolean isStale() {
            return isStale;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("name=" + name);
            buf.append(", phone=" + phoneNumber);
            buf.append(", pid=" + person_id);
            buf.append(", presence=" + presenceResId);
            buf.append(", stale=" + isStale);
            return buf.toString();
        }
    };

    private ContactInfoCache(Context context) {
        mContext = context;
    }

    /**
     * Initialize the global instance. Should call only once.
     */
    public static void init(Context context) {
        sInstance = new ContactInfoCache(context);
    }

    /**
     * Get the global instance.
     */
    public static ContactInfoCache getInstance() {
        return sInstance;
    }

    public void dump() {
            Log.i(TAG, "ContactInfoCache.dump");
    }

    /**
     * Returns the caller info in CacheEntry.
     */
    public CacheEntry getContactInfo(String numberOrEmail, boolean allowQuery) {
        if (Mms.isEmailAddress(numberOrEmail)) {
            return getContactInfoForEmailAddress(numberOrEmail, allowQuery);
        } else {
            return getContactInfoForPhoneNumber(numberOrEmail, allowQuery);
        }
    }

    public CacheEntry getContactInfo(String numberOrEmail) {
        return getContactInfo(numberOrEmail, true);
    }

    /**
     * Returns the caller info in a CacheEntry. If 'noQuery' is set to true, then this
     * method only checks in the cache and makes no content provider query.
     *
     * @param number the phone number for the contact.
     * @param allowQuery allow (potentially blocking) query the content provider if true.
     * @return the CacheEntry containing the contact info.
     */
    public CacheEntry getContactInfoForPhoneNumber(String number, boolean allowQuery) {
        // TODO: numbers like "6501234567" and "+16501234567" are equivalent.
        // we should convert them into a uniform format so that we don't cache
        // them twice.
        number = PhoneNumberUtils.stripSeparators(number);
        synchronized (mCache) {
            if (mCache.containsKey(number)) {
                CacheEntry entry = mCache.get(number);
                if (LOCAL_DEBUG) {
                    log("getContactInfo: number=" + number + ", name=" + entry.name +
                            ", presence=" + entry.presenceResId);
                }
                if (!allowQuery || !entry.isStale()) {
                    return entry;
                }
            } else if (!allowQuery) {
                return null;
            }
        }
        CacheEntry entry = queryContactInfoByNumber_EX(number);
        if (entry == null)
        {
            entry = new CacheEntry();
            entry.phoneNumber = number;
            return entry;
        }
        synchronized (mCache) {
            mCache.put(number, entry);
        }
        return entry;
    }
	
    private CacheEntry queryContactInfoByNumber_EX(String number) {
        CacheEntry entry = new CacheEntry();
        entry.phoneNumber = number;

        //if (LOCAL_DEBUG) log("queryContactInfoByNumber: number=" + number);

        String contactInfoSelectionArgs[] = new String[1];
        contactInfoSelectionArgs[0] = number;

        // We need to include the phone number in the selection string itself rather then
        // selection arguments, because SQLite needs to see the exact pattern of GLOB
        // to generate the correct query plan
        
        String selection;
		if(MessageUtils.isFetionNumber(number)){
			selection = CALLER_ID_SELECTION.replace("+",
				PhoneNumberUtils.getStrippedReversed(number));
		}else{
		    selection = CALLER_ID_SELECTION.replace("+",
				PhoneNumberUtils.toCallerIDMinMatch(number));
		}
        
        Cursor cursor = mContext.getContentResolver().query(
                PHONES_WITH_PRESENCE_URI,
                CALLER_ID_PROJECTION,
                selection,
                contactInfoSelectionArgs,
                null);

        if (cursor == null) {
            Log.w(TAG, "queryContactInfoByNumber_EX(" + number + ") returned NULL cursor!" +
                    " contact uri used " + PHONES_WITH_PRESENCE_URI);
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                entry.phoneLabel = cursor.getString(PHONE_LABEL_COLUMN);
                entry.name = cursor.getString(CONTACT_NAME_COLUMN);
                entry.person_id = cursor.getLong(CONTACT_ID_COLUMN);
                entry.presenceResId = getPresenceIconResourceId(
                        cursor.getInt(CONTACT_PRESENCE_COLUMN));
                entry.presenceText = cursor.getString(CONTACT_STATUS_COLUMN);
                if (LOCAL_DEBUG) {
                    log("queryContactInfoByNumber_EX: name=" + entry.name + ", number=" + number +
                            ", presence=" + entry.presenceResId);
                }

                loadAvatar(entry, cursor);
            }
            else
            {
                return null;
            }
        } finally {
            cursor.close();
        }

        return entry;
    }

    private void loadAvatar(CacheEntry entry, Cursor cursor) {
        if (entry.person_id == 0 || entry.mAvatar != null) {
            return;
        }

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, entry.person_id);

        InputStream avatarDataStream =
            Contacts.openContactPhotoInputStream(
                    mContext.getContentResolver(),
                    contactUri);
        if (avatarDataStream != null) {
            Bitmap b = BitmapFactory.decodeStream(avatarDataStream);

            BitmapDrawable bd =
                new BitmapDrawable(mContext.getResources(), b);

            entry.mAvatar = bd;
            try {
                avatarDataStream.close();
            } catch (IOException e) {
                entry.mAvatar = null;
            }
        }
    }



    /**
     * Get the display names of contacts. Contacts can be either email address or
     * phone number.
     *
     * @param address the addresses to lookup, separated by ";"
     * @return a nicely formatted version of the contact names to display
     */
    public String getContactName(String address) {
        if (TextUtils.isEmpty(address))
        {
            return "";
        }
        if (address.contains(SEPARATOR))
        {
            return getContactNameWithMulti(address, SEPARATOR);
        }
        String name = Contact.get(address, true).getName();
        if (TextUtils.isEmpty(name))
        {
            name = address;
        }
        return name;
    }

    /**
     * Get the display names of contacts. Contacts can be either email address or
     * phone number.
     *
     * @param context the context to use
     * @param address the addresses to lookup, separated by ";"
     * @return a nicely formatted version of the contact names to display
     */
        public String getContactNameWithMulti(String address, String seperator) {
            if (TextUtils.isEmpty(address)) {
                return "";
            }
                        
            StringBuilder result = new StringBuilder();
            for (String value : address.split(seperator)) 
            {
                if (value.length() > 0) 
                {
                    result.append(seperator);
                    String name = Contact.get(value, true).getName();
                    if (TextUtils.isEmpty(name))
                    {
                        name = value;
                    }
                    if (!TextUtils.isEmpty(name))
                    {
                        result.append(name);
                    }
                }
            }
    
            if (result.length() > 0) {
                // Skip the first ";"
                return result.substring(1);
            }
    
            return "";
        }
    

    

       
    /**
     * Get the display names of contacts. Contacts can be either email address or
     * phone number.
     *
     * @param context the context to use
     * @param address the addresses to lookup, separated by ";"
     * @return a nicely formatted version of the contact names to display
     */
    public String getContactNameWithNull(Context context, String address) {
        if (TextUtils.isEmpty(address)) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String value : address.split(SEPARATOR)) {
            if (value.length() > 0) {
                result.append(SEPARATOR);
                if (Mms.isEmailAddress(value)) {
                    result.append(getDisplayName(value));
                } else {
                    result.append(getCallerIdWithNull(context, value));
                }
            }
        }

        if (result.length() > 0) {
            // Skip the first ";"
            return result.substring(1);
        }

        return "";
    }


/**
 * Get the display names of contacts. Contacts can be either email address or
 * phone number.
 *
 * @param context the context to use
 * @param address the addresses to lookup, separated by ";"
 * @return a nicely formatted version of the contact names to display
 */
    public String getContactNameWithSeperator(Context context, String address, String seperator) {
        if (TextUtils.isEmpty(address)) {
            return "";
        }
                    
        StringBuilder result = new StringBuilder();
        for (String value : address.split(seperator)) {
            if (value.length() > 0) {
                result.append(seperator);
                if (Mms.isEmailAddress(value)) {
                    String name = getDisplayName(value);
                    if (TextUtils.isEmpty(name))
                    {
                        name = value;
                    }
                    result.append(name);                
                } else {
                    String name = getCallerIdWithNull(context, value);
                    if (TextUtils.isEmpty(name))
                    {
                        name = value;
                    }
                    result.append(name);
                }
            }
        }

        if (result.length() > 0) {
            // Skip the first ";"
            return result.substring(1);
        }

        return "";
    }

    private String getCallerIdWithNull(Context context, String number) {
        ContactInfoCache.CacheEntry entry = getContactInfo(number);
        if (entry != null && !TextUtils.isEmpty(entry.name)) {
            return entry.name;
        }
        return "";
    }

    /**
     * Get the person id of contacts. Contacts can be either email address or
     * phone number.
     *
     * @param context the context to use
     * @param number the numbers to lookup, separated by ";"
     * @return a nicely formatted version of the contact names to display
     */
    public long getPersonId(String number) {
        return Contact.get(number, true).getPersonId();
    }

       
    /**
     * Get the display name of an email address. If the address already contains
     * the name, parse and return it. Otherwise, query the contact database. Cache
     * query results for repeated queries.
     */
    public String getDisplayName(String email) {
        Matcher match = Mms.NAME_ADDR_EMAIL_PATTERN.matcher(email);
        if (match.matches()) {
            // email has display name
            return getEmailDisplayName(match.group(1));
        }

        CacheEntry entry = getContactInfoForEmailAddress(email, true /* allow query */);
        if (entry != null && entry.name != null) {
            return entry.name;
        }

        return email;
    }

    /**
     * Returns the contact info for a given email address
     *
     * @param email the email address.
     * @param allowQuery allow making (potentially blocking) content provider queries if true.
     * @return a CacheEntry if the contact is found.
     */
    public CacheEntry getContactInfoForEmailAddress(String email, boolean allowQuery) {
        synchronized (mCache) {
            if (mCache.containsKey(email)) {
                CacheEntry entry = mCache.get(email);
                if (!allowQuery || !entry.isStale()) {
                    return entry;
                }
            } else if (!allowQuery) {
                return null;
            }
        }
        CacheEntry entry = queryEmailDisplayName(email);
        synchronized (mCache) {
            mCache.put(email, entry);

            return entry;
        }
    }


    private static String getEmailDisplayName(String displayString) {
        Matcher match = Mms.QUOTED_STRING_PATTERN.matcher(displayString);
        if (match.matches()) {
            return match.group(1);
        }

        return displayString;
    }

    private int getPresenceIconResourceId(int presence) {
        if (presence != Presence.OFFLINE) {
            return Presence.getPresenceIconResourceId(presence);
        }

        return 0;
    }

    /**
     * Query the contact email table to get the name of an email address.
     */
    private CacheEntry queryEmailDisplayName(String email) {
        CacheEntry entry = new CacheEntry();

        String contactInfoSelectionArgs[] = new String[1];
        contactInfoSelectionArgs[0] = email;

        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                EMAIL_WITH_PRESENCE_URI,
                EMAIL_PROJECTION,
                EMAIL_SELECTION,
                contactInfoSelectionArgs,
                null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    entry.presenceResId = getPresenceIconResourceId(
                            cursor.getInt(EMAIL_STATUS_COLUMN));
                    entry.person_id = cursor.getLong(EMAIL_ID_COLUMN);

                    String name = cursor.getString(EMAIL_NAME_COLUMN);
                    if (TextUtils.isEmpty(name)) {
                        name = cursor.getString(EMAIL_CONTACT_NAME_COLUMN);
                    }
                    if (!TextUtils.isEmpty(name)) {
                        entry.name = name;
                        loadAvatar(entry, cursor);
                        if (LOCAL_DEBUG) {
                            log("queryEmailDisplayName: name=" + entry.name + ", email=" + email +
                                    ", presence=" + entry.presenceResId);
                        }
                        break;
                    }

                }
            } finally {
                cursor.close();
            }
        }
        return entry;
    }

    private void log(String msg) {
        Log.d(TAG, "[ContactInfoCache] " + msg);
    }
}
