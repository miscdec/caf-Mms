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

import com.android.mms.R;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageItem;
//import com.android.mms.ui.SelectRecipientsList;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Profile;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.aidl.service.entity.SimpleMessage;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.rcs.ui.common.mms.GeoLocation;
import com.suntek.rcs.ui.common.mms.GeoLocationParser;
import com.suntek.rcs.ui.common.RcsLog;

public class RcsChatMessageUtils {

    private static final String EXTRA_MESSAGE_ID = "_id";
    private static final String ACTION_LUNCH_BURN_FLAG_MESSAGE =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCH_BURN_FLAG_MESSAGE";

    public static GeoLocation readMapXml(String filepath) {
        GeoLocation geo = null;
        try {
            GeoLocationParser handler = new GeoLocationParser(new FileInputStream(
                    new File(filepath)));
            geo = handler.getGeoLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return geo;
    }

    public static boolean renameFile(String oldFilePath, String newFilePath) {
        if (TextUtils.isEmpty(oldFilePath) || TextUtils.isEmpty(newFilePath)) {
            return false;
        }
        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);
        return oldFile.renameTo(newFile);
    }

    public static String[] toStringArray(List<String> strList) {
        String[] array = new String[strList.size()];
        strList.toArray(array);
        return array;
    }

    public static boolean isFileDownload(String filePath, long fileSize) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        if (fileSize == 0) {
            return false;
        }
        boolean isDownload = false;
        File file = new File(filePath);
        if (file != null) {
            RcsLog.i("RcsChatMessageUtils.isFileDownload: filePath = " + filePath +
                    " ; thisFileSize = " + file.length() + " ; fileSize = " + fileSize);
            if (file.exists() && file.length() >= fileSize) {
                isDownload = true;
            }
        }
        return isDownload;
    }

    public static void startBurnMessageActivity(Context context, long msgId, int burnMessageState) {
        if (burnMessageState == RcsUtils.MESSAGE_HAS_BURNED) {
            Toast.makeText(context, R.string.message_is_burnd, Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(ACTION_LUNCH_BURN_FLAG_MESSAGE);
            intent.putExtra(EXTRA_MESSAGE_ID, msgId);
            RcsUtils.startSafeActivity(context, intent);
        }
    }

    public static boolean sendRcsForwardMessage(Context context, List<String> numberList,
            Intent intent, long msgId) {
        long groupChatId = -1;
        long groupThreadId = -1;
        MessageApi messageApi = MessageApi.getInstance();
        try {
            HashSet<String> numbers = new HashSet<String>();
            for (String number : numberList) {
                numbers.add(number);
            }
            long threadId = Threads.getOrCreateThreadId(context, numbers);
            messageApi.forward(msgId, threadId, numberList, -1);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } catch (ServiceDisconnectedException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean sendRcsForwardMessage(Context context, ArrayList<String> numbers,
            Intent intent, long msgId) {
        if (numbers == null) {
            numbers = new ArrayList<String>();
        }
        long groupChatId = -1;
        long groupThreadId = -1;
        MessageApi messageApi = MessageApi.getInstance();
        if (intent != null) {
            groupChatId = intent.getLongExtra("groupChatId", -1);
            groupThreadId = intent.getLongExtra("selectThreadId", -1);
            if (intent.hasExtra("recipients")) {
                numbers = intent.getStringArrayListExtra(
                        "SelectRecipientsList.EXTRA_RECIPIENTS");//SelectRecipientsList lost
            } else if (intent.hasExtra("numbers")) {
                   String[] extraNumber = intent.getStringArrayExtra("numbers");
                   if (extraNumber == null || extraNumber.length == 0) {
                       return false;
                   }
                   for (String number : extraNumber) {
                       numbers.add(number);
                   }
            }
        }

        try {
            if (groupChatId != -1 && groupThreadId != -1) {
                messageApi.forwardToGroupChat(msgId, groupThreadId, groupChatId);
                return true;
            } else if (numbers.size() > 0) {
                List<String> numberList = numbers;
                HashSet<String> recipients = new HashSet<String>();
                for (String number : numberList) {
                    recipients.add(number);
                }
                long threadId = Threads.getOrCreateThreadId(context, recipients);
                if (numberList != null) {
                    if (numberList.size() == 1) {
                        String forwardNumber = numberList.get(0);
                        if (forwardNumber.contains(" ")) {
                            forwardNumber = forwardNumber.replaceAll(" ", "");
                        }

                        messageApi.forward(msgId, threadId, forwardNumber, -1);
                        return true;
                    } else {
                        List<String> forwardNumberList = new ArrayList<String>();
                        for (String number : numberList) {
                            if (number.contains(" ")) {
                                number = number.replaceAll(" ", "");
                            }

                            forwardNumberList.add(number);
                        }
                        messageApi.forward(msgId, threadId, numberList, -1);
                        return true;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        } catch (ServiceDisconnectedException ex) {
            ex.printStackTrace();
            return false;
        }
        return false;
    }

    public static void favoritedOneMessage (Context context,
            long messageId, boolean isRcsMessage, boolean favourite){
       ArrayList<SimpleMessage> simpleMsgs = new ArrayList<SimpleMessage>();
       SimpleMessage sm = new SimpleMessage();
       sm.setMessageRowId(messageId);
       if (!isRcsMessage) {
           sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_SMS);
       } else {
           sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_IM);
       }
       simpleMsgs.add(sm);
       try {
           MessageApi messageApi = MessageApi.getInstance();
           if (favourite) {
               messageApi.collect(simpleMsgs);
           } else {
               messageApi.cancelCollect(simpleMsgs);
           }
       } catch (ServiceDisconnectedException e) {
           e.printStackTrace();
       } catch (RemoteException ex){
           ex.printStackTrace();
       }
    }

    public static boolean isFavoritedMessage(Context context, long messageId) {
        boolean isFavorited = false;
        Uri uri = Uri.parse("content://sms/");
        Cursor cursor = context.getContentResolver()
                .query(uri, new String[] {
                        RcsColumns.SmsRcsColumns.RCS_FAVOURITE
                }, "_id = ?", new String[] {
                        String.valueOf(messageId)
                }, null);
        if (cursor != null && cursor.moveToFirst()) {
            if (!cursor.isAfterLast()) {
                int favorited = cursor.getInt(
                        cursor.getColumnIndex(RcsColumns.SmsRcsColumns.RCS_FAVOURITE));
                if (favorited == 1) {
                    isFavorited = true;
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return isFavorited;
    }

    private static Uri[] buildUris(final Set<String> keySet, final int newPickRecipientsCount) {
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

    public static void forwardContactOrConversation(Context context,OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(R.string.select_contact_conversation);
        builder.setItems(new String[] {
                context.getString(R.string.forward_input_number),
                context.getString(R.string.forward_contact),
                context.getString(R.string.forward_conversation),
                context.getString(R.string.forward_contact_group)
        },listener);
        builder.show();
    }

    public static boolean isPublicAccountMessage(Context context, long threadId) {
        boolean isPAMessage = false;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(Constants.ThreadProvider.CONST_THREAD_URI, new String[]{
                "msg_chat_type" }, "_id = ?", new String[]{ String.valueOf(threadId) }, null);
        try {
            if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) == 4) {
                isPAMessage = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isPAMessage;
    }
}
