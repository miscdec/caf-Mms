/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.mms.rcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Base64;

import com.android.mms.RcsApiManager;
import com.android.mms.data.Conversation;
import com.suntek.mway.rcs.client.aidl.contacts.RCSContact;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Profile;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.TelephoneModel;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatModel;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatUser;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.aidl.contacts.RCSContact;

public class RcsContactsUtils {
    public static final String MIMETYPE_RCS = "vnd.android.cursor.item/rcs";
    public static final String PHONE_PRE_CODE = "+86";

    public static String getMyRcsRawContactId(Context context){
        String rawContactId = null;
        Uri uri = Uri.parse("content://com.android.contacts/profile/data/");
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] {"raw_contact_id"}, null,null, null);
        if(cursor != null){
            if(cursor.moveToNext()){
                rawContactId = cursor.getString(0);
                cursor.close();
                cursor = null;
            }
        }
       return rawContactId;
    }

	public static String getRawContactId(Context context,String contactId) {
        String rawContactId = null;
        Cursor cursor = context.getContentResolver()
                .query(RawContacts.CONTENT_URI,
                        new String[] { RawContacts._ID },
                        RawContacts.CONTACT_ID + "=?",
                        new String[] { contactId }, null);
        if (null != cursor) {
            if (cursor.moveToNext())
                rawContactId = cursor.getString(0);
            cursor.close();
            cursor = null;
        }
        return rawContactId;
    }

    public static RCSContact getMyRcsContact(Context context, String rawContactId){
        if(TextUtils.isEmpty(rawContactId))
            return null;
        RCSContact rcsContact = null;
        ArrayList<TelephoneModel> teleList = null;
        Uri uri = Uri.parse("content://com.android.contacts/profile/data/");
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] {
                        "_id", "mimetype", "data1", "data2", "data3",
                        "data4", "data15"
                }, " raw_contact_id = ?  ",
                new String[] {
                    rawContactId
                }, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                rcsContact = new RCSContact();
                teleList = new ArrayList<TelephoneModel>();
                while (!cursor.isAfterLast()) {
                    String mimetype = cursor.getString(cursor
                            .getColumnIndexOrThrow("mimetype"));
                    String data1 = cursor.getString(cursor
                            .getColumnIndexOrThrow("data1"));
                    if ("vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                        String numberType = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        if ("4".equals(numberType)) {
                            rcsContact.setCompanyFax(data1);
                        } else if ("17".equals(numberType)) {
                            rcsContact.setCompanyTel(data1);
                        }  else if ("2".equals(numberType)) {
                            rcsContact.setAccount(data1);
                        }else {

                            TelephoneModel model = new TelephoneModel();
                            model.setTelephone(data1);
                            model.setType(Integer.parseInt(numberType));
                            teleList.add(model);

                        }
                    } else if ("vnd.android.cursor.item/postal-address_v2"
                            .equals(mimetype)) {
                        String data2 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));

                        if ("1".equals(data2)) {
                            rcsContact.setHomeAddress(data1);
                        } else if ("2".equals(data2)) {
                            rcsContact.setCompanyAddress(data1);
                        }
                    } else if ("vnd.android.cursor.item/name".equals(mimetype)) {
                        String fristName = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        String lastName = cursor.getString(cursor
                                .getColumnIndexOrThrow("data3"));
                        rcsContact.setFirstName(fristName);
                        rcsContact.setLastName(lastName);
                    } else if ("vnd.android.cursor.item/email_v2"
                            .equals(mimetype)) {
                        rcsContact.setEmail(data1);
                    } else if ("vnd.android.cursor.item/organization"
                            .equals(mimetype)) {
                        String data4 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data4"));
                        rcsContact.setCompanyName(data1);
                        rcsContact.setCompanyDuty(data4);
                    } else if (MIMETYPE_RCS.equals(mimetype)) {
                        String data2 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        rcsContact.setEtag(data1);
                        rcsContact.setBirthday(data2);
                    }
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (rcsContact != null) {
            rcsContact.setOtherTels(teleList);
        }
        return rcsContact;
    }

    public  static RCSContact getContactProfileOnDbByRawContactId(Context context,
            String rawContactId) {
        if(TextUtils.isEmpty(rawContactId))
            return null;
        RCSContact rcsContact = null;
        ArrayList<TelephoneModel> teleList = null;
        Uri uri = Uri.parse("content://com.android.contacts/data/");
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[] {
                        "_id", "mimetype", "data1", "data2", "data3",
                        "data4", "data15"
                }, " raw_contact_id = ?  ",
                new String[] {
                    rawContactId
                }, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                rcsContact = new RCSContact();
                teleList = new ArrayList<TelephoneModel>();
                while (!cursor.isAfterLast()) {
                    String mimetype = cursor.getString(cursor
                            .getColumnIndexOrThrow("mimetype"));
                    String data1 = cursor.getString(cursor
                            .getColumnIndexOrThrow("data1"));
                    if ("vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                        String numberType = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        if ("4".equals(numberType)) {
                            rcsContact.setCompanyFax(data1);
                        } else if ("17".equals(numberType)) {
                            rcsContact.setCompanyTel(data1);
                        }  else if ("2".equals(numberType)) {
                            rcsContact.setAccount(data1);
                        }else {

                            TelephoneModel model = new TelephoneModel();
                            model.setTelephone(data1);
                            model.setType(Integer.parseInt(numberType));
                            teleList.add(model);

                        }
                    } else if ("vnd.android.cursor.item/postal-address_v2"
                            .equals(mimetype)) {
                        String data2 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));

                        if ("1".equals(data2)) {
                            rcsContact.setHomeAddress(data1);
                        } else if ("2".equals(data2)) {
                            rcsContact.setCompanyAddress(data1);
                        }
                    } else if ("vnd.android.cursor.item/name".equals(mimetype)) {
                        String fristName = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        String lastName = cursor.getString(cursor
                                .getColumnIndexOrThrow("data3"));
                        rcsContact.setFirstName(fristName);
                        rcsContact.setLastName(lastName);
                    } else if ("vnd.android.cursor.item/email_v2"
                            .equals(mimetype)) {
                        rcsContact.setEmail(data1);
                    } else if ("vnd.android.cursor.item/organization"
                            .equals(mimetype)) {
                        String data4 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data4"));
                        rcsContact.setCompanyName(data1);
                        rcsContact.setCompanyDuty(data4);
                    } else if (MIMETYPE_RCS.equals(mimetype)) {
                        String data2 = cursor.getString(cursor
                                .getColumnIndexOrThrow("data2"));
                        rcsContact.setEtag(data1);
                        rcsContact.setBirthday(data2);
                    }
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (rcsContact != null) {
            rcsContact.setOtherTels(teleList);
        }
        return rcsContact;
    }

    public static String getGroupChatMemberDisplayName(Context context, int groupId,
            String number) {
        GroupChatModel model = null;
        try {
            model = RcsApiManager.getMessageApi().getGroupChatById(String.valueOf(groupId));
        } catch (ServiceDisconnectedException e) {
            e.printStackTrace();
        }
        if (model == null)
            return number;
        List<GroupChatUser> list = model.getUserList();
        if (list == null || list.size() == 0)
            return number;
        for (GroupChatUser groupChatUser : list) {
            if (groupChatUser.getNumber().equals(number)) {
                if (!TextUtils.isEmpty(groupChatUser.getAlias())) {
                    return groupChatUser.getAlias();
                } else {
                    return getContactNameFromPhoneBook(context, number);
                }
            }
        }
        return number;
    }

    public static String getContactNameFromPhoneBook(Context context, String phoneNum) {
        String contactName = phoneNum;
        String numberW86;
        if (!phoneNum.startsWith(PHONE_PRE_CODE)) {
            numberW86 = PHONE_PRE_CODE + phoneNum;
        } else {
            numberW86 = phoneNum;
            phoneNum = phoneNum.substring(3);
        }
        String formatNumber = getAndroidFormatNumber(phoneNum);

        ContentResolver cr = context.getContentResolver();
        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                },
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? OR "
                        + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? OR "
                        + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? ",
                new String[] {
                        phoneNum, numberW86, formatNumber
                }, null);
        try {
            if (pCur != null && pCur.moveToFirst()) {
                contactName = pCur
                        .getString(pCur
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

            }
        } finally {
            if (pCur != null)
                pCur.close();
        }
        return contactName;
    }

    public static String getAndroidFormatNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }

        number = number.replaceAll(" ", "");

        if (number.startsWith(PHONE_PRE_CODE)) {
            number = number.substring(3);
        }

        if (number.length() != 11) {
            return number;
        }

        StringBuilder builder = new StringBuilder();
        // builder.append("+86 ");
        builder.append(number.substring(0, 3));
        builder.append(" ");
        builder.append(number.substring(3, 7));
        builder.append(" ");
        builder.append(number.substring(7));
        return builder.toString();
    }

}
