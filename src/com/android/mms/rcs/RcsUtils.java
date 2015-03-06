/*
 * Copyright (c) 2014 pci-suntektech Technologies, Inc.  All Rights Reserved.
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
import com.android.mms.ui.MessageItem;
import com.android.mms.ui.MessageListItem;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.exception.VCardException;
import com.suntek.mway.rcs.client.aidl.constant.BroadcastConstants;
import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatModel;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatUser;
import com.suntek.mway.rcs.client.api.specialnumber.impl.SpecialServiceNumApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.provider.ContactsContract.Groups;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Outbox;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Sent;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.FileInputStream;

import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class RcsUtils {
    public static final String TAG = "RcsUtils";
    public static final int IS_RCS_TRUE = 1;
    public static final int IS_RCS_FALSE = 0;
    public static final int RCS_IS_BURN_TRUE = 1;
    public static final int RCS_IS_BURN_FALSE = 0;
    public static final int RCS_IS_DOWNLOAD_FALSE = 0;
    public static final int RCS_IS_DOWNLOAD_OK = 1;
    public static final int SMS_DEFAULT_RCS_ID = -1;
    public static final int SMS_DEFAULT_RCS_GROUP_ID = 0;
    public static final int RCS_MSG_TYPE_TEXT = SuntekMessageData.MSG_TYPE_TEXT;
    public static final int RCS_MSG_TYPE_IMAGE = SuntekMessageData.MSG_TYPE_IMAGE;
    public static final int RCS_MSG_TYPE_VIDEO = SuntekMessageData.MSG_TYPE_VIDEO;
    public static final int RCS_MSG_TYPE_AUDIO = SuntekMessageData.MSG_TYPE_AUDIO;
    public static final int RCS_MSG_TYPE_MAP = SuntekMessageData.MSG_TYPE_MAP;
    public static final int RCS_MSG_TYPE_VCARD = SuntekMessageData.MSG_TYPE_CONTACT;
    public static final int RCS_MSG_TYPE_NOTIFICATION = SuntekMessageData.MSG_TYPE_NOTIFICATION;
    public static final int RCS_MSG_TYPE_CAIYUNFILE = SuntekMessageData.MSG_TYPE_CLOUD_FILE;
    public static final int RCS_MSG_TYPE_PAID_EMO = SuntekMessageData.MSG_TYPE_PAID_EMO;

    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_CREATED = "create_not_active";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_ACTIVE = "create";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_JOIN = "join";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_SUBJECT = "subject";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_ALIAS = "alias";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_REMARK = "remark";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_CHAIRMAN = "chairman";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_TICK = "tick";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_QUIT = "quit";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_DISBAND = "disband";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_POLICY = "policy";
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_GONE = "gone";

    public static final String PUBLIC_ACCOUNT_PACKAGE_NAME = "com.suntek.mway.rcs.publicaccount";
    public static final String NATIVE_UI_PACKAGE_NAME = "com.suntek.mway.rcs.nativeui";

    public static final int MSG_RECEIVE =   SuntekMessageData.MSG_RECEIVE;
    public static final String IM_ONLY     = "1";
    public static final String SMS_ONLY     ="2";
    public static final String RCS_MMS_VCARD_PATH="sdcard/rcs/" + "mms.vcf";
    public static final String SMS_URI_ALL = "content://sms/";
    private static final int NEED_GET_PROFILE_PHOTO_CHAT_COUNT =1;
    static String contentType  = "text/x-vCard";

    // message status
    public static final int MESSAGE_SENDING = 64;
    public static final int MESSAGE_HAS_SENDED = 32;
    public static final int MESSAGE_SENDED = -1;
    public static final int MESSAGE_FAIL = 128;
    public static final int MESSAGE_HAS_BURNED = 2;
    public static final int MESSAGE_SEND_RECEIVE = 99;//delivered
    public static final int MESSAGE_HAS_READ = 100;//displayed
    public static final int MESSAGE_HAS_SEND_SERVER = 0;//send to server

    public static final String RCS_NATIVE_UI_ACTION_GROUP_CHAT_DETAIL =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCH_RCS_GROUPCHATDETAIL";
    public static final String RCS_NATIVE_UI_ACTION_NOTIFICATION_LIST =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCHER_RCS_NOTIFICATION_LIST";

    private static final String LOG_TAG = "RCS_UI";

    public static boolean isSupportRcs() {
        return RcsApiManager.isRcsServiceInstalled()
                && RcsApiManager.isRcsOnline();
    }

    public static GeoLocation readMapXml(String filepath) {
        GeoLocation geo = null;
        try {
            GeoLocationParser handler = new GeoLocationParser(
                    new FileInputStream(new File(filepath)));
            geo = handler.getGeoLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return geo;
    }

    public static void burnMessageAtLocal(final Context context, final long id) {
        String smsId = String.valueOf(id);
        ContentValues values = new ContentValues();
        values.put("rcs_is_burn", 1);
        values.put("rcs_burn_body","");
        context.getContentResolver().update(Uri.parse("content://sms/"), values, " _id = ? ",
                new String[] {
                    smsId
                });
    }

    public static void burnAllMessageAtLocal(final Context context) {
        ContentValues values = new ContentValues();
        values.put("rcs_burn_body", "");
        values.put("rcs_is_burn", 1);
        context.getContentResolver().update(Uri.parse("content://sms/"), values, "rcs_burn_flag = ?",
                new String[] {
                    "1"
                });
    }

    public static void deleteMessageById(Context context, long id) {
        String smsId = String.valueOf(id);
        ContentValues values = new ContentValues();
        context.getContentResolver().delete(Uri.parse("content://sms/"), "rcs_id=?", new String[] {
            smsId
        });
    }

    public static void updateState(Context context, String rcs_id, int rcsMsgState) {
        ContentValues values = new ContentValues();

        switch (rcsMsgState) {
            case SuntekMessageData.MSG_STATE_SEND_FAIL:
                values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
                break;
            case SuntekMessageData.MSG_STATE_SEND_ING:
                values.put(Sms.TYPE, Sms.MESSAGE_TYPE_OUTBOX);
                break;
            case SuntekMessageData.MSG_STATE_SENDED:
                values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
                break;
            case SuntekMessageData.MSG_STATE_DOWNLOAD_FAIL:
                values.put(Sms.TYPE, Sms.MESSAGE_TYPE_INBOX);
                break;
        }

        String selection;
        String[] selectionArgs;

        values.put("rcs_msg_state", rcsMsgState);

        if (rcsMsgState == -1) {
            selection = "rcs_id=? and rcs_chat_type=?";
            selectionArgs = new String[] {
                    rcs_id, "1"
            };
        } else {
            selection = "rcs_id=?";
            selectionArgs = new String[] {
                rcs_id
            };
        }

        ContentResolver resolver = context.getContentResolver();
        int result = resolver.update(Sms.CONTENT_URI, values, selection, selectionArgs);
        if (result == 0) {
            try {
                Thread.sleep(3000);
                int reresult = resolver.update(Sms.CONTENT_URI, values, selection, selectionArgs);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public static void updateManyState(Context context, String rcs_id,
            String number, int rcs_msg_state) {
        ContentValues values = new ContentValues();

        values.put("rcs_msg_state", rcs_msg_state);

        number = number.replaceAll(" ", "");
        String numberW86;
        if (!number.startsWith("+86")) {
            numberW86 = "+86" + number;
        } else {
            numberW86 = number;
            number = number.substring(3);
        }
        String formatNumber = getAndroidFormatNumber(number);
        ContentResolver resolver = context.getContentResolver();
        String selection = "rcs_message_id = ? and ( address = ? OR address = ? OR address = ? )";
        String[] selectionArgs = new String[] {
                rcs_id, number, numberW86, formatNumber
        };
        resolver.update(Sms.CONTENT_URI, values, selection, selectionArgs);
    }

    public static String getAndroidFormatNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }

        number = number.replaceAll(" ", "");

        if (number.startsWith("+86")) {
            number = number.substring(3);
        }

        if (number.length() != 11) {
            return number;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("+86 ");
        builder.append(number.substring(0, 3));
        builder.append(" ");
        builder.append(number.substring(3, 7));
        builder.append(" ");
        builder.append(number.substring(7));
        return builder.toString();
    }

    public static void topConversion(Context context, long mThreadId) {
        ContentValues values = new ContentValues();
        values.put("top", 1);
        values.put("top_time", System.currentTimeMillis());
        final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/update-top");
        Uri uri = ContentUris.withAppendedId(THREAD_ID_CONTENT_URI, mThreadId);
        context.getContentResolver().update(THREAD_ID_CONTENT_URI, values, "_id=?", new String[] {
            mThreadId + ""
        });
    }

    public static void cancelTopConversion(Context context, long mThreadId) {
        ContentValues values = new ContentValues();
        values.put("top", 0);
        values.put("top_time", 0);
        final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/update-top");
        Uri uri = ContentUris.withAppendedId(THREAD_ID_CONTENT_URI, mThreadId);
        context.getContentResolver().update(THREAD_ID_CONTENT_URI, values, "_id=?", new String[] {
            mThreadId + ""
        });
    }

    public static void updateFileDownloadState(Context context, String rcs_message_id) {
        ContentValues values = new ContentValues();
        values.put("rcs_is_download", 1);
        context.getContentResolver().update(Sms.CONTENT_URI, values, "rcs_message_id=?",
                new String[] {
                    rcs_message_id
                });
    }

    public static String getFilePath(int id, String str) {
        try {
            ChatMessage cMsg = RcsApiManager.getMessageApi()
                    .getMessageById(String.valueOf(id));
            String imagePath = RcsApiManager.getMessageApi().getFilepath(cMsg);
            if (imagePath != null && new File(imagePath).exists()) {
                return imagePath;
            } else {
                String path = RcsApiManager.getMessageApi().getFilepath(cMsg);
                if (path != null && path.lastIndexOf("/") != -1) {
                    path = path.substring(0, path.lastIndexOf("/") + 1);
                    return path + cMsg.getFilename();
                } else {
                    return str;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return str;

    }

    /**
     * @param context
     * @param body
     * @param address
     * @param is_rcs
     * @param rcs_msg_type
     * @param rcs_mime_type
     * @param rcs_have_attach
     * @param rcs_path
     */
    public static void rcsInsertInbox(Context context, String body, String address, int is_rcs,
            int rcs_msg_type, String rcs_mime_type, int rcs_have_attach, String rcs_path) {
        ContentValues values = new ContentValues();
        values.put(Inbox.BODY, body);
        values.put(Inbox.ADDRESS, address);
        values.put("is_rcs", is_rcs);
        values.put("rcs_msg_type", rcs_msg_type); //text or image  //0 text,1 image ,2 video ,3 audio ,4 map,5 vcard
        values.put("rcs_mime_type",rcs_mime_type); //text or image
        values.put("rcs_have_attach", rcs_have_attach);
        values.put("rcs_path", rcs_path);
        Long threadId = Conversation.getOrCreateThreadId(context,address);
        ContentResolver resolver = context.getContentResolver();
        Uri insertedUri = SqliteWrapper.insert(context, resolver,
                Inbox.CONTENT_URI, values);
        // Now make sure we're not over the limit in stored messages
        Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
        MmsWidgetProvider.notifyDatasetChanged(context);

    }

    public static void rcsInsertMany(Context context, List<ChatMessage> cMsgList)
            throws ServiceDisconnectedException {
        if(cMsgList.size() == 0){
            Log.i("RCS_UI","RETURN");
            return;
        }
        for(ChatMessage cMsg:cMsgList){
            if (cMsg != null && !isMessageExist(context, cMsg)) {
                rcsInsert(context, cMsg);
            }
        }
    }

    public static boolean isMessageExist(Context context, ChatMessage chatMessage) {
        Log.i("RCS_UI", "chatMessage.getMessageId()=" + chatMessage.getMessageId());
        String id = chatMessage.getMessageId();
        if (id == null) {
            id = "";
        }
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI, null,
                "rcs_message_id = ?", new String[] {
                    chatMessage.getMessageId()
                }, null);
        try {
            if (cursor != null && cursor.moveToNext()) {
                dumpCursorRows(cursor);
                return true;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }

    public static long rcsInsert(Context context, ChatMessage chatMessage)
            throws ServiceDisconnectedException {
        MessageApi messageApi = RcsApiManager.getMessageApi();

        int id = chatMessage.getId();
        String contact = chatMessage.getContact();
        String data = chatMessage.getData();
        int msgType = chatMessage.getMsgType();
        int sendReceive = chatMessage.getSendReceive();
        String mimeType = chatMessage.getMimeType();
        String messageId = chatMessage.getMessageId();
        int msgBurnAfterReadFlag = chatMessage.getMsgBurnAfterReadFlag();
        int chatType = chatMessage.getChatType();
        long threadIdTmp = chatMessage.getThreadId();
        long filesize = chatMessage.getFilesize();
        int msgState = chatMessage.getMsgState();
        String filename = chatMessage.getFilename();
        long time = chatMessage.getTime();
        int isRead = chatMessage.getIsRead();
        String conversationId = chatMessage.getConversationId();;
        String contributionId = chatMessage.getContributionId();
        String fileSelector = chatMessage.getFileSelector();
        String fileTransferExt = chatMessage.getFileTransferExt();
        String fileTransferId = chatMessage.getFileTransferId();
        String fileIcon = chatMessage.getFileIcon();
        int barCycle = chatMessage.getBarCycle();
        int msgBlackFlag = chatMessage.getMsgBlackFlag();
        int continueFlag = chatMessage.getContinueFlag();

        String address = chatMessage.getContact();
        String body = chatMessage.getData();
        int rcs_msg_type = chatMessage.getMsgType();
        int send_receive = chatMessage.getSendReceive();
        String rcs_mime_type = chatMessage.getMimeType();
        int rcs_have_attach = 1;
        if (SuntekMessageData.MSG_TYPE_IMAGE == rcs_msg_type && body != null) {
            if (body.endsWith("gif")) {
                rcs_mime_type = "image/gif";
            } else if (body.endsWith("bmp")) {
                rcs_mime_type = "image/bmp";
            } else if (body.endsWith("jpg")) {
                rcs_mime_type = "image/*";
            } else if (body.endsWith("jpeg")) {
                rcs_mime_type = "image/*";
            } else if (body.endsWith("png")) {
                rcs_mime_type = "image/png";
            }
        }
        String rcs_message_id = chatMessage.getMessageId();
        int rcs_burn_flag = chatMessage.getMsgBurnAfterReadFlag();
        int rcs_chat_type = chatMessage.getChatType();
        long rcsThreadId = chatMessage.getThreadId();
        long fileSize = chatMessage.getFilesize();

        int playTime = 0;
        String rcs_path = "";
        String rcs_thumb_path = "";
        switch (rcs_msg_type) {
            case SuntekMessageData.MSG_TYPE_TEXT:
                break;
            case SuntekMessageData.MSG_TYPE_VIDEO:
            case SuntekMessageData.MSG_TYPE_AUDIO:
                playTime = messageApi.getPlayTime(rcs_msg_type, chatMessage.getData());
            case SuntekMessageData.MSG_TYPE_IMAGE:
            case SuntekMessageData.MSG_TYPE_GIF:
            case SuntekMessageData.MSG_TYPE_CONTACT:
                rcs_path = getFilePath(chatMessage);
                rcs_thumb_path = messageApi.getThumbFilepath(chatMessage);
                break;
            case SuntekMessageData.MSG_TYPE_PAID_EMO:
                body = chatMessage.getData() + "," + chatMessage.getFilename();
                break;
        }

        int rcs_msg_state = chatMessage.getMsgState();

        if (send_receive == 2 && rcs_msg_type == RcsUtils.RCS_MSG_TYPE_IMAGE) {
            rcs_thumb_path = rcs_path;
        }

        if (rcs_msg_type == SuntekMessageData.MSG_TYPE_NOTIFICATION && TextUtils.isEmpty(address)) {
            address = String.valueOf(rcsThreadId);
        }

        Uri uri;
        if (send_receive == 1) {
            uri = Inbox.CONTENT_URI;
        } else if (rcs_msg_type == SuntekMessageData.MSG_TYPE_NOTIFICATION) {
            // Group chatMessage notification message.
            uri = Inbox.CONTENT_URI;
        } else {
            uri = Outbox.CONTENT_URI;
        }

        if (address != null && address.contains(",")) {
            String[] addresslist = address.split(",");

            HashSet<String> recipients = new HashSet<String>();
            for (int i = 0; i < addresslist.length; i++) {
                recipients.add(addresslist[i]);
            }
            Long threadId = Threads.getOrCreateThreadId(context, recipients);

            ContentResolver resolver = context.getContentResolver();
            for (int i = 0; i < addresslist.length; i++) {
                ContentValues values = new ContentValues();
                if (rcs_burn_flag == SuntekMessageData.MSG_BURN_AFTER_READ_FLAG) {
                    values.put(Sms.BODY, "burnMessage");
                    values.put("rcs_burn_body", body);
                } else {
                    values.put(Sms.BODY, body);
                }
                values.put(Sms.ADDRESS, addresslist[i]);
                if (send_receive == 2) {
                    values.put("type",2);
                }
                values.put("is_rcs", 1);
                values.put("rcs_msg_type", rcs_msg_type); // text or image //0 text,1 image ,2 video ,3 audio ,4 map,5 vcard
                values.put("rcs_mime_type", rcs_mime_type); // text or image
                values.put("rcs_have_attach", rcs_have_attach);
                values.put("rcs_path", rcs_path);
                values.put("rcs_thumb_path", rcs_thumb_path);
                values.put("thread_id", threadId);
                values.put("rcs_id", chatMessage.getId());
                values.put("rcs_burn_flag", rcs_burn_flag);
                values.put("rcs_message_id", rcs_message_id);
                values.put("rcs_chat_type", rcs_chat_type);
                values.put("rcs_file_size", fileSize);
                values.put("rcs_msg_state", rcs_msg_state);
                values.put("rcs_play_time", playTime);
                if (rcs_msg_type == SuntekMessageData.MSG_TYPE_NOTIFICATION) {
                    // Group chatMessage notification message.
                    values.put("seen", 1);
                }

                Uri insertedUri = SqliteWrapper.insert(context, resolver, uri, values);
                // Now make sure we're not over the limit in stored messages
                Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
                MmsWidgetProvider.notifyDatasetChanged(context);

            }

            return threadId;
        } else {
            ContentValues values = new ContentValues();
            if (rcs_burn_flag == SuntekMessageData.MSG_BURN_AFTER_READ_FLAG) {
                values.put(Sms.BODY, "burnMessage");
                values.put("rcs_burn_body", body);
            } else {
                values.put(Sms.BODY, body);
            }
            values.put(Sms.ADDRESS, address);
            //values.put("type",2); //send sucsess;
            values.put("is_rcs", 1);
            values.put("rcs_msg_type", rcs_msg_type); //text or image  //0 text,1 image ,2 video ,3 audio ,4 map,5 vcard
            values.put("rcs_mime_type", rcs_mime_type); //text or image
            values.put("rcs_have_attach", rcs_have_attach);
            values.put("rcs_path", rcs_path);
            values.put("rcs_thumb_path", rcs_thumb_path);
            values.put("rcs_id", chatMessage.getId());
            values.put("rcs_burn_flag", rcs_burn_flag);
            values.put("rcs_message_id", rcs_message_id);
            values.put("rcs_chat_type", rcs_chat_type);
            values.put("rcs_file_size", fileSize);
            values.put("rcs_play_time", playTime);
            values.put("rcs_msg_state", rcs_msg_state);
            if (send_receive == 2) {
                values.put("type", 2);
            }
            if (rcs_msg_type == SuntekMessageData.MSG_TYPE_NOTIFICATION) {
                // Group chatMessage notification message.
                values.put("seen", 1);
            }

            long t0, t1;
            t0 = System.currentTimeMillis();
            long threadId;
            if (rcs_chat_type == SuntekMessageData.CHAT_TYPE_GROUP) {
                HashSet<String> recipients = new HashSet<String>();
                recipients.add(String.valueOf(rcsThreadId));
                threadId = getOrCreateThreadId(context, recipients);
            } else {
                ArrayList<String> numbers = new ArrayList<String>();
                numbers.add(String.valueOf(address));
                ContactList recipients = ContactList.getByNumbers(numbers, false);
                Conversation conversation = Conversation.get(context, recipients, true);
                if (conversation == null) {
                    threadId = Conversation.getOrCreateThreadId(context, address);
                } else {
                    threadId = conversation.getThreadId();
                }
            }
            values.put("thread_id", threadId);
            t1 = System.currentTimeMillis();
            Log.d("Demo", "getOrCreateThreadId, threadId=" + threadId + ", cost: " + (t1 - t0));

            t0 = System.currentTimeMillis();
            ContentResolver resolver = context.getContentResolver();
            Uri insertedUri = SqliteWrapper.insert(context, resolver, uri, values);
            t1 = System.currentTimeMillis();
            Log.d("Demo", "insert cost: " + (t1 - t0));
            // Now make sure we're not over the limit in stored messages
            Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
            MmsWidgetProvider.notifyDatasetChanged(context);
            return threadId;
        }
    }

    public static String getFilePath(ChatMessage cMsg) throws ServiceDisconnectedException {
        String imagePath = RcsApiManager.getMessageApi().getFilepath(cMsg);
        if (imagePath != null && new File(imagePath).exists()) {
            return imagePath;
        } else {
            String path = RcsApiManager.getMessageApi().getFilepath(cMsg);
            if (path != null && path.lastIndexOf("/") != -1) {
                path = path.substring(0, path.lastIndexOf("/") + 1);
                return path + cMsg.getFilename();
            } else {
                return null;
            }
        }
    }

    public static long getThreadIdByRcsMesssageId(Context context, long rcs_id) {
        long threadId = 0;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI, new String[] {
            Sms.THREAD_ID
        }, "rcs_id=?", new String[] {
            String.valueOf(rcs_id)
        }, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    threadId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        Log.d(LOG_TAG, "getThreadIdByRcsMessageId(): rcs_id=" + rcs_id + ", threadId=" + threadId);

        return threadId;
    }

    public static long getRcsThreadIdByThreadId(Context context, long threadId) {
        long rcsThreadId = 0;

        ContentResolver resolver = context.getContentResolver();
        Uri uri = Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
        Cursor cursor = SqliteWrapper.query(context, resolver, uri, new String[] {
            Telephony.Threads.RECIPIENT_IDS
        }, Telephony.Threads._ID + "=?", new String[] {
            String.valueOf(threadId)
        }, null);

        String recipientId = "";
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    recipientId = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }

        Log.d(LOG_TAG, "getRcsThreadIdByThreadId(): threadId=" + threadId + ", recipientId="
                + recipientId);

        if (TextUtils.isEmpty(recipientId)) {
            return rcsThreadId;
        }

        uri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "canonical-addresses");
        cursor = SqliteWrapper.query(context, resolver, uri, new String[] {
                CanonicalAddressesColumns._ID, CanonicalAddressesColumns.ADDRESS
        }, Telephony.CanonicalAddressesColumns._ID + "=?", new String[] {
            String.valueOf(recipientId)
        }, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    rcsThreadId = cursor.getLong(1);
                }
            } catch (Exception e) {
                // Just let the exception to if it's not an rcsThreadId.
            } finally {
                cursor.close();
            }
        }

        Log.d(LOG_TAG, "getRcsThreadIdByThreadId(): threadId=" + threadId + ", recipientId="
                + recipientId + ", rcsThreadId=" + rcsThreadId);

        return rcsThreadId;
    }

    public static long getThreadIdByGroupId(Context context, String groupId) {
        long threadId = 0;

        if (groupId == null) {
            return threadId;
        }
        GroupChatModel groupChat = null;
        try {
            groupChat = RcsApiManager.getMessageApi().getGroupChatById(groupId);
        } catch (ServiceDisconnectedException e) {
            Log.w(LOG_TAG, e);
        }

        if (groupChat == null) {
            return threadId;
        }

        long rcsThreadId = groupChat.getThreadId();

        ContentResolver resolver = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "canonical-addresses");
        Cursor cursor = SqliteWrapper.query(context, resolver, uri, new String[] {
                Telephony.CanonicalAddressesColumns._ID
        }, Telephony.CanonicalAddressesColumns.ADDRESS + "=?", new String[] {
            String.valueOf(rcsThreadId)
        }, null);

        int recipientId = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    recipientId = cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }

        Log.d(LOG_TAG, "getThreadIdByRcsMessageId(): groupId=" + groupId + ", recipientId="
                + recipientId);

        if (recipientId > 0) {
            uri = Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
            cursor = SqliteWrapper.query(context, resolver, uri, new String[] {
                Telephony.Threads._ID
            }, Telephony.Threads.RECIPIENT_IDS + "=?", new String[] {
                String.valueOf(recipientId)
            }, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        threadId = cursor.getLong(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        Log.d(LOG_TAG, "getThreadIdByRcsMessageId(): groupId=" + groupId + ", recipientId="
                + recipientId + ", threadId=" + threadId);

        return threadId;
    }

    public static int getDuration(final Context context, final Uri uri) {
        MediaPlayer mPlayer = MediaPlayer.create(context, uri);
        if (mPlayer == null) {
            return 0;
        }
        int duration = mPlayer.getDuration() / 1000;
        return duration;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    /**
     * Launch the RCS group chat detail activity.
     */
    public static void startGroupChatDetailActivity(Context context, String groupId) {
        Intent intent = new Intent(RCS_NATIVE_UI_ACTION_GROUP_CHAT_DETAIL);
        intent.putExtra("groupId", groupId);
        if (isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent);
        }
    }

    /**
     * Launch the RCS notify list activity.
     */
    public static void startNotificationListActivity(Context context) {
        Intent intent = new Intent(RCS_NATIVE_UI_ACTION_NOTIFICATION_LIST);
        if (isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent);
        }
    }

    /**
     * This method is temporally copied from /framework/opt/telephone for RCS group chat debug purpose.
     * @hide
     */
    public static long getOrCreateThreadId(
            Context context, Set<String> recipients) {
        Uri.Builder uriBuilder = Uri.parse("content://mms-sms/threadID").buildUpon();

        for (String recipient : recipients) {
            if (Mms.isEmailAddress(recipient)) {
                recipient = Mms.extractAddrSpec(recipient);
            }

            uriBuilder.appendQueryParameter("recipient", recipient);
        }
        Log.d(LOG_TAG, "uriBuilder.appendQueryParameter(\"isGroupChat\", \"1\");");
        uriBuilder.appendQueryParameter("isGroupChat", "1");

        Uri uri = uriBuilder.build();
        //if (DEBUG) Rlog.v(TAG, "getOrCreateThreadId uri: " + uri);

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                uri, new String[] { BaseColumns._ID }, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {
                    Log.e(LOG_TAG, "getOrCreateThreadId returned no rows!");
                }
            } finally {
                cursor.close();
            }
        }

        Log.e(LOG_TAG, "getOrCreateThreadId failed with uri " + uri.toString());
        throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
    }

    public static boolean isActivityIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static void onShowConferenceCallStartScreen(Context context) {
        onShowConferenceCallStartScreen(context, null);
    }

    public static void onShowConferenceCallStartScreen(Context context, String number) {
        Intent intent = new Intent("android.intent.action.ADDPARTICIPANT");
        if (!TextUtils.isEmpty(number)) {
            intent.putExtra("confernece_number_key", number);
        }
        if (isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Activity not found.", Toast.LENGTH_LONG).show();
        }
    }

    public static void dumpCursorRows(Cursor cursor) {
        int count = cursor.getColumnCount();
        Log.d(LOG_TAG, "------ dump cursor row ------");
        for (int i = 0; i < count; i++) {
            Log.d(LOG_TAG, cursor.getColumnName(i) + "=" + cursor.getString(i));
        }
    }

    public static void dumpIntent(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        Log.d(LOG_TAG, "============ onReceive ============");
        Log.d(LOG_TAG, "action=" + action);
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(LOG_TAG, key + "=" + extras.get(key));
            }
        }
    }

    /**
     * Get the chat group name for display. Return 'subject' if the 'remark' is empry.
     */
    public static String getDisplayName(GroupChatModel groupChat) {
        if (groupChat == null) {
            return "";
        }

        String remark = groupChat.getRemark();
        if (!TextUtils.isEmpty(remark)) {
            return remark;
        } else {
            String subject = groupChat.getSubject();
            if (!TextUtils.isEmpty(subject)) {
                return subject;
            } else {
                return "";
            }
        }
    }

    /**
     * Launch the activity for creating rcs group chat.
     * @param context
     * @param number numbers, split by ";". For example: 13800138000;10086
     * @param message
     */
    public static void startCreateGroupChatActivity(Context context, String number, String message) {
        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        if (!TextUtils.isEmpty(number)) {
            sendIntent.putExtra("address", number);
        }
        if (!TextUtils.isEmpty(message)) {
            sendIntent.putExtra("sms_body", message);
        }
        sendIntent.putExtra("isGroupChat", true);
        sendIntent.setType("vnd.android-dir/mms-sms");
        context.startActivity(sendIntent);
    }

    public static String getStringOfNotificationBody(Context context, String body) {
        if (body != null) {
            if (body.equals(GROUP_CHAT_NOTIFICATION_KEY_WORDS_CREATED)) {
                body = context.getString(R.string.group_chat_created);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_CHAIRMAN)) {
                String chairmanNumber = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_update_chairman, chairmanNumber);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_JOIN)) {
                String joinNumber = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_join, joinNumber);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_SUBJECT)) {
                String subject = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_subject, subject);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_REMARK)) {
                String remark = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_remark, remark);
            } else if (body.equals(GROUP_CHAT_NOTIFICATION_KEY_WORDS_ACTIVE)) {
                body = context.getString(R.string.group_chat_active);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_ALIAS)) {
                String[] params = body.split(",");
                if (params.length == 3) {
                    body = context.getString(R.string.group_chat_alias, params[1], params[2]);
                }
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_TICK)) {
                String number = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_kick, number);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_QUIT)) {
                String number = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_quit, number);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_DISBAND)) {
                body = context.getString(R.string.group_chat_disbanded);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_POLICY)) {
                body = context.getString(R.string.group_chat_policy);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_GONE)) {
                body = context.getString(R.string.group_chat_gone);
            }
        }

        return body;
    }

    public static void updateGroupChatSubject(Context context, GroupChatModel groupChatModel) {
        if(context == null) return;
        ContentResolver resolver = context.getContentResolver();
        String thread_id = String.valueOf(groupChatModel.getThreadId());
        String group_id = String.valueOf(groupChatModel.getId());
        String groupTitle = TextUtils.isEmpty(groupChatModel
                .getRemark()) ? groupChatModel.getSubject()
                : groupChatModel.getRemark();

        StringBuilder where = new StringBuilder();
        where.append(Groups.SYSTEM_ID);
        where.append("=" + group_id);

        ContentValues values = new ContentValues();
        values.put(Groups.TITLE, groupTitle);
        values.put(Groups.SYSTEM_ID,group_id);
        values.put(Groups.SOURCE_ID,"RCS");

        try{
            Log.d(TAG," update group: title= "+groupTitle+" id= "+group_id);
            resolver.update(Groups.CONTENT_URI, values, where.toString(), null);
        } catch(Exception ex) {
            //
        }
    }

    public static void createGroupChat(Context context,GroupChatModel groupChatModel){
        if(context == null) return;
        ContentResolver resolver = context.getContentResolver();
        String thread_id = String.valueOf(groupChatModel.getThreadId());
        String group_id = String.valueOf(groupChatModel.getId());
        String groupTitle = TextUtils.isEmpty(groupChatModel
                .getRemark()) ? groupChatModel.getSubject()
                : groupChatModel.getRemark();

        ContentValues values = new ContentValues();
        values.put(Groups.TITLE, groupTitle);
        values.put(Groups.SYSTEM_ID,group_id);
        values.put(Groups.SOURCE_ID,"RCS");

        try{
            Log.d(TAG," create group: title= "+groupTitle+" id= "+group_id);
            resolver.insert(Groups.CONTENT_URI, values);
        } catch(Exception ex) {
            //
        }
    }

    public static void disBandGroupChat(Context context,GroupChatModel groupChatModel){
        if(context == null) return;
        ContentResolver resolver = context.getContentResolver();
        String thread_id = String.valueOf(groupChatModel.getThreadId());
        String group_id = String.valueOf(groupChatModel.getId());
        StringBuilder where = new StringBuilder();
        where.append(Groups.SYSTEM_ID);
        where.append("="+group_id);

        try{
            Log.d(TAG," disband group:  id= "+group_id);
            resolver.delete(Groups.CONTENT_URI, where.toString(), null);
        } catch(Exception ex) {
            //
        }
    }
    /**
     * Make sure the bytes length of <b>src</b> is less than <b>bytesLength</b>.
     */
    public static String trimToSpecificBytesLength(String src, int bytesLength) {
        String dst = "";
        if (src != null) {
            int subjectBytesLength = src.getBytes().length;
            if (subjectBytesLength > bytesLength) {
                int subjectCharCount = src.length();
                for (int i = 0; i < subjectCharCount; i++) {
                    char c = src.charAt(i);
                    if ((dst + c).getBytes().length > bytesLength) {
                        break;
                    } else {
                        dst = dst + c;
                    }
                }

                src = dst;
            } else {
                dst = src;
            }
        } else {
            dst = src;
        }

        return dst;
    }

    public  static boolean setVcard(final Context context,Uri uri) {
        InputStream instream = null;

        FileOutputStream fout = null;
        try {

            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            instream = fd.createInputStream();
            File file = new File(RCS_MMS_VCARD_PATH);

            fout = new FileOutputStream(file);

            byte[] buffer = new byte[8000];
            int size = 0;
            while ((size = instream.read(buffer)) != -1) {
                fout.write(buffer, 0, size);
            }

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        } catch (IOException e) {

        } finally {
            if (null != instream) {
                try {
                    instream.close();
                } catch (IOException e) {

                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {

                    return false;
                }
            }
            return true;
        }
    }

    public static Bitmap createBitmap_Compress(String absFilePath) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inPurgeable = true;
            BitmapFactory.decodeFile(absFilePath, options);

            options.inSampleSize = calculateInSampleSize(options, 480, 800);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(absFilePath, options);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    @SuppressWarnings("deprecation")
    public static Drawable createDrawable(Context context, Bitmap bitmap) {
        if (bitmap == null)
            return null;

        byte[] ninePatch = bitmap.getNinePatchChunk();
        if (ninePatch != null && ninePatch.length > 0) {
            NinePatch np = new NinePatch(bitmap, ninePatch, null);
            return new NinePatchDrawable(context.getResources(), np);
        }
        return new BitmapDrawable(bitmap);
    }

    public static long getAudioMaxTime(){
        try {
            return RcsApiManager.getMessageApi().getAudioMaxTime();
        } catch (ServiceDisconnectedException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static long getVideoMaxTime(){
        try {
            return RcsApiManager.getMessageApi().getVideoMaxTime();
        } catch (ServiceDisconnectedException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static long getVideoFtMaxSize(){
        try {
            return RcsApiManager.getMessageApi().getVideoFtMaxSize();
        } catch (ServiceDisconnectedException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static boolean isLoading(String filePath,long fileSize){
        if(TextUtils.isEmpty(filePath)){
            return false;
        }
        File file = new File(filePath);
        if (file.exists() && file.length() < fileSize){
            return true;
        } else {
            return false;
        }
    }

    public static int getMessageCountByGroupIdAndNumber(Context context, String groupId,
            String number) {
        long threadId = getThreadIdByGroupId(context, groupId);
        ContentResolver cr = context.getContentResolver();
        String[] projection = new String[] {
                "count(distinct address)"
        };
        Uri uri = Uri.parse(SMS_URI_ALL);
        Cursor cursor = cr.query(uri, projection,
                "address = ? AND type = ? AND thread_id =? AND 1=1) GROUP BY (address",
                new String[] {
                        number, "1", String.valueOf(threadId)
                }, "date DESC");
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                } else {
                    Log.e("RCS_UI", "getMessageCountByGroupIdAndNumber returned no rows!");
                }
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    public static synchronized HashMap<String, Integer> loadGroupMessageCountByGroupId(Context context,
            String groupId) {
        Looper.prepare();
        HashMap<String, Integer> groupChatCount = new HashMap<String, Integer>();
        int count = 0;
        GroupChatModel groupModel = null;
        try{
            groupModel = RcsApiManager.getMessageApi().getGroupChatById(groupId);
        }catch(ServiceDisconnectedException ex){
            ex.printStackTrace();
        }
        if (groupModel != null) {
            List<GroupChatUser> userList = groupModel.getUserList();
            if (userList == null || userList.size() == 0)
                return null;
            for (GroupChatUser groupChatUser : userList) {
                if (groupChatUser != null) {
                    count = getMessageCountByGroupIdAndNumber(context, groupId, groupChatUser.getNumber());
                    Log.i("RCS_UI", "groudId = " + groupId +" number = " + groupChatUser.getNumber() + " count = "+count);
                    if(count == NEED_GET_PROFILE_PHOTO_CHAT_COUNT){
                        RcsContactsUtils.updateContactPhotosByNumber(context, groupChatUser.getNumber());
                    }
                    groupChatCount.put(groupId+groupChatUser.getNumber(),Integer.valueOf(count));
                }
            }
        }
        Looper.loop();
        return groupChatCount;
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo info : installedApps) {
            if (packageName.equals(info.packageName)) {
                return true;
            }
        }
        return false;
    }

    public static void startEmojiStore(Activity activity, int requestCode) {
        if (RcsUtils.isPackageInstalled(activity, "com.temobi.dm.emoji.store")) {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName("com.temobi.dm.emoji.store",
                    "com.temobi.dm.emoji.store.activity.EmojiActivity");
            intent.setComponent(comp);
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(activity, R.string.install_emoj_store, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("static-access")
    public static void closeKB(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            ((InputMethodManager)activity.getSystemService(activity.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void openKB(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager)context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    public static void openPopupWindow(Context context, View view, byte[] data) {
        LinearLayout.LayoutParams mGifParam = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if(bitmap == null){
            return;
        }
        int windowWidth = bitmap.getWidth() + RcsUtils.dip2px(context, 40);
        int windowHeight = bitmap.getHeight() + RcsUtils.dip2px(context, 40);
        ColorDrawable transparent = new ColorDrawable(Color.TRANSPARENT);
        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout
                .setLayoutParams(new LinearLayout.LayoutParams(windowWidth, windowHeight));
        relativeLayout.setBackgroundResource(R.drawable.rcs_emoji_popup_bg);
        relativeLayout.setGravity(Gravity.CENTER);
        RcsEmojiGifView emojiGifView = new RcsEmojiGifView(context);
        emojiGifView.setLayoutParams(mGifParam);
        emojiGifView.setBackground(transparent);
        emojiGifView.setMonieByteData(data);
        relativeLayout.addView(emojiGifView);
        PopupWindow popupWindow = new PopupWindow(view, windowWidth, windowHeight);
        popupWindow.setBackgroundDrawable(transparent);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setContentView(relativeLayout);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        popupWindow.update();
    }

    public static boolean isDeletePrefixSpecailNumberAvailable(Context context){
        boolean isDeleSpecailNumber =context.getResources()
            .getBoolean(R.bool.config_mms_delete_prefix_special_number);
            SpecialServiceNumApi specailNumApi = RcsApiManager
                 .getSpecialServiceNumApi();
        try{
            if(!isDeleSpecailNumber){
                specailNumApi.closeFunction();
            } else {
                specailNumApi.openFunction();
                List<String> specailNum = new ArrayList<String>();
                specailNum = specailNumApi.getList();
                Log.i("RCS_UI", "specailNum:" + specailNum.toString());
                if(0 == specailNum.size()) {
                    String[] specialNumberItems = context.getResources()
                        .getStringArray(R.array.special_prefix_number);
                    for (int i = 0; i < specialNumberItems.length; i++)
                        specailNumApi.add(specialNumberItems[i]);
                }
            }
        } catch (ServiceDisconnectedException e){
            Log.i("RCS_UI","delete Special Number funtion error");
        }
        return isDeleSpecailNumber;
    }

    public static void showOpenRcsVcardDialog(final Context context,final MessageListItem messageListItem){
        final String[] openVcardItems = new String[] {
            context.getString(R.string.vcard_detail_info),
            context.getString(R.string.vcard_import)
        };
        final MessageItem messageItem = messageListItem.getMessageItem();
        AlertDialog.Builder builder = new AlertDialog.Builder(messageListItem.getContext());
        builder.setItems(openVcardItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        String vcardFilePath = RcsUtils.getFilePath(messageItem.mRcsId, messageItem.mRcsPath);
                        ArrayList<PropertyNode> propList = openRcsVcardDetail(context,vcardFilePath);
                        showDetailVcard(context,propList);
                        break;
                    case 1:
                        try {
                          String filePath = RcsUtils.getFilePath(messageItem.mRcsId, messageItem.mRcsPath);
                          File file = new File(filePath);
                          Intent intent = new Intent(Intent.ACTION_VIEW);
                          intent.setDataAndType(Uri.fromFile(file), contentType.toLowerCase());
                          intent.putExtra("VIEW_VCARD_FROM_MMS", true);
                          messageListItem.getContext().startActivity(intent);
                      } catch (Exception e) {
                          Log.w("RCS_UI", e);
                      }
                        break;
                    default:
                        break;
                }
            }
        });
        builder.create().show();
    }

    public static ArrayList<PropertyNode> openRcsVcardDetail(Context context,String filePath){
        if (TextUtils.isEmpty(filePath)){
            return null;
        }
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);

            VNodeBuilder builder = new VNodeBuilder();
            VCardParser parser = new VCardParser_V21();
            parser.addInterpreter(builder);
            parser.parse(fis);
            List<VNode> vNodeList = builder.getVNodeList();
            ArrayList<PropertyNode> propList = vNodeList.get(0).propList;
            return propList;
        } catch (Exception e) {
            Log.w("RCS_UI",e);
            return null;
        }
    }

    private static void showDetailVcard(Context context,ArrayList<PropertyNode> propList){
        AlertDialog.Builder builder = new Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View vcardView = inflater.inflate(R.layout.rcs_vcard_detail, null);

        ImageView photoView = (ImageView)vcardView.findViewById(R.id.vcard_photo);
        TextView nameView, priNumber, addrText,comName, positionText;
        nameView = (TextView)vcardView.findViewById(R.id.vcard_name);
        priNumber = (TextView)vcardView.findViewById(R.id.vcard_number);
       addrText = (TextView) vcardView.findViewById(R.id.vcard_addre);
       positionText = (TextView)vcardView.findViewById(R.id.vcard_position);
       comName = (TextView) vcardView.findViewById(R.id.vcard_com_name);

       ArrayList<String> numberList = new ArrayList<String>();
        for (PropertyNode propertyNode : propList) {
            if ("FN".equals(propertyNode.propName)) {
                if(!TextUtils.isEmpty(propertyNode.propValue)){
                    nameView.setText(context.getString(
                            R.string.vcard_name) + propertyNode.propValue);
                }
            } else if ("TEL".equals(propertyNode.propName)) {
                if(!TextUtils.isEmpty(propertyNode.propValue)){
                    getPhoneNumberMap(context, numberList, propertyNode);
                }
            } else if("ADR".equals(propertyNode.propName)){
                if(!TextUtils.isEmpty(propertyNode.propValue)){
                    String address = propertyNode.propValue;
                    address = address.replaceAll(";", "");
                    addrText.setText(context.getString(
                        R.string.vcard_compony_addre)+":" + address);
                }
            } else if("ORG".equals(propertyNode.propName)){
                if(!TextUtils.isEmpty(propertyNode.propValue)){
                    comName.setText(context.getString(R.string.vcard_compony_name)
                        + ":" + propertyNode.propValue);
                }
            } else if("TITLE".equals(propertyNode.propName)){
                if(!TextUtils.isEmpty(propertyNode.propValue)){
                    positionText.setText(context.getString(
                            R.string.vcard_compony_position) + ":" + propertyNode.propValue);
                }
            } else if("PHOTO".equals(propertyNode.propName)){
                if(propertyNode.propValue_bytes != null){
                    byte[] bytes = propertyNode.propValue_bytes;
                    final Bitmap vcardBitmap = BitmapFactory
                            .decodeByteArray(bytes, 0, bytes.length);
                    photoView.setImageBitmap(vcardBitmap) ;
                }
            }
        }
        vcardView.findViewById(R.id.vcard_middle).setVisibility(View.GONE);
        if (numberList.size() > 0) {
            priNumber.setText(numberList.get(0));
            numberList.remove(0);
        }
        if (numberList.size() > 0) {
            vcardView.findViewById(R.id.vcard_middle).setVisibility(
                    View.VISIBLE);
            LinearLayout linearLayout = (LinearLayout)vcardView.findViewById(R.id.other_number_layout);
            addNumberTextView(context, numberList, linearLayout);
        }
        builder.setTitle(R.string.vcard_detail_info);
        builder.setView(vcardView);
        builder.create();
        builder.show();
    }

    private static void addNumberTextView(Context context,
            ArrayList<String> numberList, LinearLayout linearLayout) {
        for (int i = 0; i < numberList.size(); i++) {
            TextView textView = new TextView(context);
            textView.setText(numberList.get(i));
            linearLayout.addView(textView);
        }
    }

    private static void getPhoneNumberMap(Context context,
            ArrayList<String> numberList, PropertyNode propertyNode) {
        if (null == propertyNode.paramMap_TYPE
                || propertyNode.paramMap_TYPE.size() == 0) {
            return;
        }
        String number = propertyNode.propValue;
        if (propertyNode.paramMap_TYPE.size() == 2) {
            if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("HOME")) {
                numberList.add(context
                        .getString(R.string.vcard_number_fax_home) + number);
            } else if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberList.add(context
                        .getString(R.string.vcard_number_fax_work) + number);
            } else if (propertyNode.paramMap_TYPE.contains("PREF")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberList.add(context
                        .getString(R.string.vcard_number_pref_work) + number);
            } else if (propertyNode.paramMap_TYPE.contains("CELL")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberList.add(context
                        .getString(R.string.vcard_number_call_work) + number);
            } else if (propertyNode.paramMap_TYPE.contains("WORK")
                    && propertyNode.paramMap_TYPE.contains("PAGER")) {
                numberList.add(context
                        .getString(R.string.vcard_number_work_pager) + number);
            } else {
                numberList.add(context.getString(R.string.vcard_number_other)
                        + number);
            }
        } else {
            if (propertyNode.paramMap_TYPE.contains("CELL")) {
                numberList.add(context.getString(R.string.vcard_number)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("HOME")) {
                numberList.add(context.getString(R.string.vcard_number_home)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("WORK")) {
                numberList.add(context.getString(R.string.vcard_number_work)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("PAGER")) {
                numberList.add(context.getString(R.string.vcard_number_pager)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("VOICE")) {
                numberList.add(context.getString(R.string.vcard_number_other)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("CAR")) {
                numberList.add(context.getString(R.string.vcard_number_car)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("ISDN")) {
                numberList.add(context.getString(R.string.vcard_number_isdn)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("PREF")) {
                numberList.add(context.getString(R.string.vcard_number_pref)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("FAX")) {
                numberList.add(context.getString(R.string.vcard_number_fax)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("TLX")) {
                numberList.add(context.getString(R.string.vcard_number_tlx)
                        + number);
            } else if (propertyNode.paramMap_TYPE.contains("MSG")) {
                numberList.add(context.getString(R.string.vcard_number_msg)
                        + number);
            } else {
                numberList.add(context.getString(R.string.vcard_number_other)
                        + number);
            }
        }
    }

    public static Intent OpenFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        String end = file.getName()
                .substring(file.getName().lastIndexOf(".") + 1, file.getName().length())
                .toLowerCase();
        Log.i("RCS_UI", "END=" + end);
        if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") || end.equals("xmf")
                || end.equals("ogg") || end.equals("wav") || end.equals("amr")) {
            return getAudioFileIntent(filePath);
        } else if (end.equals("3gp") || end.equals("mp4")) {
            return getVideoFileIntent(filePath);
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png")
                || end.equals("jpeg") || end.equals("bmp")) {
            return getImageFileIntent(filePath);
        } else if(end.equals("apk")){
            return getApkFileIntent(filePath);
        }else if(end.equals("ppt")){
            return getPptFileIntent(filePath);
        }else if(end.equals("xls")){
            return getExcelFileIntent(filePath);
        }else if(end.equals("doc")){
            return getWordFileIntent(filePath);
        }else if(end.equals("pdf")){
            return getPdfFileIntent(filePath);
        }else if(end.equals("chm")){
            return getChmFileIntent(filePath);
        }else if(end.equals("txt")){
            return getTextFileIntent(filePath,false);
        }else{
            return getAllIntent(filePath);
        }

    }

    private static Intent getVideoFileIntent(String param) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "video/*");
        return intent;
    }

    private static Intent getAudioFileIntent(String param) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "audio/*");
        return intent;
    }

    private static Intent getImageFileIntent(String param) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "image/*");
        return intent;
    }

    public static Intent getAllIntent( String param) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "*/*");
        return intent;
    }

    public static Intent getApkFileIntent( String param) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        return intent;
    }

    public static Intent getHtmlFileIntent( String param){

        Uri uri = Uri.parse(param ).buildUpon().
                encodedAuthority("com.android.htmlfileprovider").
                scheme("content").encodedPath(param).build();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        return intent;
    }

    private static Intent getFileIntent( String param, String datatype){

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, datatype);
        return intent;
    }

    public static Intent getPptFileIntent( String param){

        return getFileIntent(param, "application/vnd.ms-powerpoint");
    }

    public static Intent getExcelFileIntent( String param){

        return getFileIntent(param, "application/vnd.ms-excel");
    }

    public static Intent getWordFileIntent( String param){

        return getFileIntent(param, "application/msword");
    }

    public static Intent getChmFileIntent( String param){

        return getFileIntent(param, "application/x-chm");
    }

    public static Intent getPdfFileIntent( String param){

        return getFileIntent(param, "application/pdf");
    }

    public static Intent getTextFileIntent( String param, boolean paramBoolean){

        if (paramBoolean){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.parse(param);
            intent.setDataAndType(uri, "text/plain");
            return intent;

        }else{
            return getFileIntent(param, "text/plain");
        }
    }

    public static String getRcsMessageStatusText(Context context, MessageItem mMessageItem) {
        String text;

        switch (mMessageItem.mRcsMsgState) {
            case MESSAGE_SENDING:
                if ((mMessageItem.mRcsType == RCS_MSG_TYPE_IMAGE
                        || mMessageItem.mRcsType == RCS_MSG_TYPE_VIDEO)) {
                    if (MessageListItem.sFileTrasnfer != null) {
                        Long percent = MessageListItem.sFileTrasnfer
                                .get(mMessageItem.mRcsMessageId);
                        if (percent != null) {
                            text = context.getString(R.string.uploading_percent,
                                    percent.intValue());
                        } else {
                            text = context.getString(R.string.message_adapte_sening);
                        }
                    } else {
                        text = context.getString(R.string.message_adapte_sening);
                    }
                } else {
                    text = context.getString(R.string.message_adapte_sening);
                }
                break;
            case MESSAGE_HAS_SENDED:
                text = context.getString(R.string.message_adapter_has_send)
                        + "  " + mMessageItem.mTimestamp;
                break;
            case MESSAGE_SENDED:
                text = context.getString(R.string.message_received)
                        + "  " + mMessageItem.mTimestamp;
                break;
            case MESSAGE_FAIL:
                if (mMessageItem.mRcsType == RCS_MSG_TYPE_TEXT) {
                    text = context.getString(R.string.message_send_fail);
                } else {
                    text = context.getString(R.string.message_send_fail_resend);
                }
                break;
            case MESSAGE_SEND_RECEIVE:
                text = context.getString(R.string.message_received) + "  "
                        + mMessageItem.mTimestamp;
                break;
            case MESSAGE_HAS_BURNED:
                text = context.getString(R.string.message_received)
                        + "  " + mMessageItem.mTimestamp;
                if (mMessageItem.mRcsIsBurn != 1)
                    burnMessageAtLocal(context, mMessageItem.mMsgId);
                break;
            default:
                text = context.getString(R.string.message_adapte_sening);
                break;
        }

        return text;
    }

}
