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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.WorkingMessage;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.ConversationListItem;
import com.android.mms.ui.MessageItem;
import com.android.mms.ui.MessageListItem;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.Recycler;
import com.android.mms.widget.MmsWidgetProvider;
import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.constant.Constants.GroupChatConstants;
import com.suntek.mway.rcs.client.aidl.constant.Constants.MessageConstants;
import com.suntek.mway.rcs.client.aidl.plugin.entity.cloudfile.CloudFileMessage;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonConstant;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChatMember;
import com.suntek.mway.rcs.client.api.basic.BasicApi;
import com.suntek.mway.rcs.client.api.cloudfile.CloudFileApi;
import com.suntek.mway.rcs.client.api.exception.FileDurationException;
import com.suntek.mway.rcs.client.api.exception.FileNotExistsException;
import com.suntek.mway.rcs.client.api.exception.FileSuffixException;
import com.suntek.mway.rcs.client.api.exception.FileTooLargeException;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatApi;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.support.SupportApi;
import com.suntek.mway.rcs.client.api.specialnumber.SpecialServiceNumApi;
import com.suntek.rcs.ui.common.mms.RcsFileTransferCache;
import com.suntek.rcs.ui.common.mms.ImageUtils;
import com.suntek.rcs.ui.common.mms.GeoLocation;
import com.suntek.rcs.ui.common.mms.GeoLocationParser;
import com.suntek.rcs.ui.common.PropertyNode;
import com.suntek.rcs.ui.common.RcsEmojiStoreUtil;
import com.suntek.rcs.ui.common.RcsLog;
import com.suntek.rcs.ui.common.VNode;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;

public class RcsUtils {
    public static final int RCS_NOT_A_BURN_MESSAGE = -1;
    public static final int RCS_IS_DOWNLOAD_FALSE = 0;//unDownload
    public static final int RCS_IS_DOWNLOAD_OK = 1;//DownLoaded
    public static final int RCS_IS_DOWNLOAD_PAUSE = 2;// pause DownLoaded
    public static final int RCS_IS_DOWNLOAD_FAIL = 3;//DownLoad fail
    public static final int RCS_IS_DOWNLOADING = 4;// DownLoading
    public static final int SMS_DEFAULT_RCS_ID = -1;
    public static final int RCS_MESSAGE_ID = 1;
    public static final int CONVERSATION_IS_TOP = 1;
    public static final long SMS_DEFAULT_RCS_GROUP_ID = 0;
    public static final int RCS_MSG_TYPE_TEXT =
            Constants.MessageConstants.CONST_MESSAGE_TEXT;
    public static final int RCS_MSG_TYPE_IMAGE =
            Constants.MessageConstants.CONST_MESSAGE_IMAGE;
    public static final int RCS_MSG_TYPE_VIDEO =
            Constants.MessageConstants.CONST_MESSAGE_VIDEO;
    public static final int RCS_MSG_TYPE_AUDIO =
            Constants.MessageConstants.CONST_MESSAGE_AUDIO;
    public static final int RCS_MSG_TYPE_MAP =
            Constants.MessageConstants.CONST_MESSAGE_MAP;
    public static final int RCS_MSG_TYPE_VCARD =
            Constants.MessageConstants.CONST_MESSAGE_CONTACT;
    public static final int RCS_MSG_TYPE_NOTIFICATION =
            Constants.MessageConstants.CONST_MESSAGE_NOTIFICATION;
    public static final int RCS_MSG_TYPE_CAIYUNFILE =
            Constants.MessageConstants.CONST_MESSAGE_CLOUD_FILE;
    public static final int RCS_MSG_TYPE_PAID_EMO =
            Constants.MessageConstants.CONST_MESSAGE_PAID_EMOTICON;
    public static final int RCS_MSG_TYPE_OTHER_FILE =
            Constants.MessageConstants.CONST_MESSAGE_OTHER_FILE;

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
    public static final String GROUP_CHAT_NOTIFICATION_KEY_WORDS_ACCEPT = "accept";

    public static final String RCS_MSG_IMAGE_TYPE_BMP = "image/bmp";
    public static final String RCS_MSG_IMAGE_TYPE_GIF = "image/gif";
    public static final String RCS_MSG_IMAGE_TYPE_ALL = "image/*";
    public static final String RCS_MSG_VIDEO_TYPE_ALL = "video/*";
    public static final String RCS_MSG_AUDO_TYPE_ALL  = "audio/*";
    public static final String RCS_MSG_MAP_TYPE_ALL   = "map/*";
    public static final String RCS_MSG_VCARD_TYPE     = "text/x-vCard";

    // rcs_chat_type
    // Default message, not a rcs message
    public static final int RCS_CHAT_TYPE_DEFAULT = 0;
    // 1-1 rcs message
    public static final int RCS_CHAT_TYPE_ONE_TO_ONE = 1;
     // 1-N rcs message
    public static final int RCS_CHAT_TYPE_ONE_TO_N = 2;
    // Group chat rcs Message,it means this message is a group chat message.
    public static final int RCS_CHAT_TYPE_GROUP_CHAT = 3;
    // public account message
    public static final int RCS_CHAT_TYPE_PUBLIC_MESSAGE = 4;
    public static final int RCS_CHAT_TYPE_TO_PC = 5;

    public static final String RCS_NATIVE_UI_ACTION_GROUP_CHAT_DETAIL =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCH_RCS_GROUPCHATDETAIL";

    public static final String RCS_NATIVE_UI_ACTION_NOTIFICATION_LIST =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCHER_RCS_NOTIFICATION_LIST";

    public static final String RCS_NATIVE_UI_ACTION_CONVERSATION_LIST =
            "com.suntek.mway.rcs.publicaccount.ACTION_LUNCHER_RCS_CONVERSATION_LIST";

    public static final String NATIVE_UI_PACKAGE_NAME = "com.suntek.mway.rcs.nativeui";

    // message status

    public static final int MESSAGE_SENDING = MessageConstants.CONST_STATUS_SENDING;

    public static final int MESSAGE_HAS_SENDED = MessageConstants.CONST_STATUS_SENDED;

    public static final int MESSAGE_SENDED = MessageConstants.CONST_STATUS_SEND_RECEIVED;

    public static final int MESSAGE_FAIL = MessageConstants.CONST_STATUS_SEND_FAIL;

    public static final int MESSAGE_HAS_BURNED = MessageConstants.CONST_STATUS_BURNED;
    //delivered
    public static final int MESSAGE_SEND_RECEIVE = MessageConstants.CONST_STATUS_SEND_RECEIVED;
    //displayed
    public static final int MESSAGE_HAS_READ = MessageConstants.CONST_STATUS_ALREADY_READ;

    public static final int MESSAGE_HAS_SENT_TO_SERVER = 0;//send to server

    private static final String FIREWALL_APK_NAME = "com.android.firewall";

    public static final Uri WHITELIST_CONTENT_URI = Uri
            .parse("content://com.android.firewall/whitelistitems");

    public static final Uri BLACKLIST_CONTENT_URI = Uri
            .parse("content://com.android.firewall/blacklistitems");

    public static final int MSG_RECEIVE = 1;

    public static final String IM_ONLY = "1";

    public static final String SMS_ONLY = "2";

    private static final int DEFAULT_THUMBNAIL_SIZE_IN_DP = 150;

    private static final int FILE_NAME_NOT_FIT = 1;

    private static final int FILE_DURATION_TOO_LONG = 2;

    private static final int FILE_SIZE_TOO_LARGE = 3;

    private static final int FILE_NOT_EXIST = 4;

    // Vcard saved path
    public static final String RCS_MMS_VCARD_PATH = Environment.getExternalStorageDirectory()
            +"/rcs/";

    private static Handler exceptionHandler;

    public static final int RCS_MAX_SMS_LENGHTH = 900;

    public static final int RCS_CAPABILITY_RESULT_SUCCESS = 200;

    public static GeoLocation readMapXml(String filepath) {
        GeoLocation geo = null;
        try {
            GeoLocationParser handler = new GeoLocationParser(new FileInputStream(
                    new File(filepath)));
            geo = handler.getGeoLocation();
        } catch (Exception e) {
            RcsLog.w(e);
        }
        return geo;
    }

    public static void burnMessageAtLocal(final Context context, final long id) {
        String smsId = String.valueOf(id);
        ContentValues values = new ContentValues();
        values.put(RcsColumns.SmsRcsColumns.RCS_MSG_STATE, MESSAGE_HAS_BURNED);
        context.getContentResolver().update(Uri.parse("content://sms/"), values, " _id = ? ",
                new String[] {
                    smsId
                });
    }

    public static void updateFileDownloadState(Context context, long messageId) {
        if (messageId <= 0) {
            return;
        }
        Cursor cursor = null;
        int downloadState = -1;
        int type = 0;
        try {
            ContentResolver resolver = context.getContentResolver();
            cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI,
                    new String[] {RcsColumns.SmsRcsColumns.RCS_IS_DOWNLOAD, Sms.TYPE},
                    Sms._ID + " = ?", new String[] {
                    String.valueOf(messageId)
                    }, null);
            if (cursor != null && cursor.moveToNext()) {
                downloadState = cursor.getInt(0);
                type = cursor.getInt(1);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (type == Constants.MessageConstants.CONST_DIRECTION_RECEIVE
            && downloadState != RcsUtils.RCS_IS_DOWNLOAD_FAIL) {
            updateFileDownloadState(context, messageId,
                    RcsUtils.RCS_IS_DOWNLOAD_FAIL);
        }
    }

    public static int queryRcsMsgDownLoadState(Context context, long messageId) {
        if (messageId <= 0) {
            return -1;
        }
        Cursor cursor = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            cursor = SqliteWrapper.query(context, resolver, Sms.CONTENT_URI,
                    new String[] {RcsColumns.SmsRcsColumns.RCS_IS_DOWNLOAD},
                    Sms._ID + " = ?", new String[] {
                    String.valueOf(messageId)
                    }, null);
            if (cursor != null && cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        } catch(Exception e) {
            RcsLog.w(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    public static String getAndroidFormatNumberWith2Space(String number) {
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
        builder.append("+86");
        builder.append(number.substring(0, 3));
        builder.append(" ");
        builder.append(number.substring(3, 7));
        builder.append(" ");
        builder.append(number.substring(7));
        return builder.toString();
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
        values.put(RcsColumns.ThreadColumns.RCS_TOP, 1);
        values.put(RcsColumns.ThreadColumns.RCS_TOP_TIME, System.currentTimeMillis());
        final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/update-top");
        Uri uri = ContentUris.withAppendedId(THREAD_ID_CONTENT_URI, mThreadId);
        context.getContentResolver().update(THREAD_ID_CONTENT_URI, values, "_id=?", new String[] {
            mThreadId + ""
        });
    }

    public static void cancelTopConversion(Context context, long mThreadId) {
        ContentValues values = new ContentValues();
        values.put(RcsColumns.ThreadColumns.RCS_TOP, 0);
        values.put(RcsColumns.ThreadColumns.RCS_TOP_TIME, 0);
        final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/update-top");
        Uri uri = ContentUris.withAppendedId(THREAD_ID_CONTENT_URI, mThreadId);
        context.getContentResolver().update(THREAD_ID_CONTENT_URI, values, "_id=?", new String[] {
            mThreadId + ""
        });
    }

    public static void updateFileDownloadState(Context context, long msgId, int downLoadState) {
        ContentValues values = new ContentValues();
        values.put(RcsColumns.SmsRcsColumns.RCS_IS_DOWNLOAD, downLoadState);
        context.getContentResolver().update(Sms.CONTENT_URI, values,
                Sms._ID + " = ?",
                new String[] {
                    String.valueOf(msgId)
                });
    }

    public static int getDuration(Context context, Uri uri) {
        MediaPlayer player = MediaPlayer.create(context, uri);
        if (player == null) {
            return 0;
        }
        return player.getDuration();
    }

    /**
     * Launch the RCS group chat detail activity.
     */
    public static void startGroupChatDetailActivity(Context context, GroupChat groupChat) {
        if (groupChat != null) {
            long groupId = groupChat.getId();
            startGroupChatDetailActivity(context, groupId);
        }
    }

    /**
     * Launch the RCS group chat detail activity.
     */
    public static void startGroupChatDetailActivity(Context context, long groupId) {
        Intent intent = new Intent(RCS_NATIVE_UI_ACTION_GROUP_CHAT_DETAIL);
        intent.putExtra("groupId", groupId);
        startSafeActivity(context, intent);
    }

    public static void startSafeActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.please_install_application,
                    Toast.LENGTH_LONG).show();
            RcsLog.w(e);
        }
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        boolean installed = false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_PROVIDERS);
            installed = (info != null);
        } catch (NameNotFoundException e) {
        }
        return installed;
    }

    public static void onShowConferenceCallStartScreen(Context context) {
        onShowConferenceCallStartScreen(context, null);
    }

    public static void onShowConferenceCallStartScreen(Context context, String number) {
        Intent intent = new Intent("android.intent.action.ADDPARTICIPANT");
        if (!TextUtils.isEmpty(number)) {
            intent.putExtra("confernece_number_key", number);
        }
        startSafeActivity(context, intent);
    }

    public static void dumpCursorRows(Cursor cursor) {
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getColumnCount();
                RcsLog.d("RcsUtils: dump cursor row");
                for (int i = 0; i < count; i++) {
                    RcsLog.d("RcsUtils: " + cursor.getColumnName(i) + "=" + cursor.getString(i));
                }
            }
        } catch (Exception e) {
            RcsLog.w(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void dumpIntent(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        RcsLog.d("RcsUtils: dumpIntent onReceive broadCast");
        RcsLog.d("RcsUtils: action = " + action);
        if (extras != null) {
            for (String key : extras.keySet()) {
                RcsLog.d("RcsUtils:" + key + "=" + extras.get(key));
            }
        }
    }

    /**
     * Get the chat group name for display. Return 'subject' if the 'remark' is
     * empry.
     */
    public static String getDisplayName(GroupChat groupChat) {
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

    public static String getStringOfNotificationBody(Context context, String body) {
        if (body != null) {
            if (body.startsWith(GroupChatConstants.CONST_NOTIFY_CREATE)) {
                body = context.getString(R.string.group_chat_created);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_SET_CHAIRMAN)) {
                String chairmanNumber = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_update_chairman, chairmanNumber);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_JOIN)) {
                String joinNumber = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_join, joinNumber);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_SET_SUBJECT)) {
                String subject = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_subject, subject);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_REMARK)) {
                String remark = body.substring(body.indexOf(",") + 1);
                if (TextUtils.isEmpty(remark)) {
                    body = context.getString(R.string.group_chat_remark_delete);
                } else {
                    body = context.getString(R.string.group_chat_remark, remark);
                }
            } else if (body.equals(GROUP_CHAT_NOTIFICATION_KEY_WORDS_ACTIVE)) {
                body = context.getString(R.string.group_chat_active);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_ALIAS)) {
                String[] params = body.split(",");
                if (params.length == 3) {
                    body = context.getString(R.string.group_chat_alias, params[1], params[2]);
                }
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_BOOTED)) {
                String number = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_kick, number);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_QUIT)) {
                String number = body.substring(body.indexOf(",") + 1);
                body = context.getString(R.string.group_chat_quit, number);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_DISBAND)) {
                body = context.getString(R.string.group_chat_disbanded);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_SET_POLICY)) {
                body = context.getString(R.string.group_chat_policy);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_GONE)) {
                body = context.getString(R.string.group_chat_gone);
            } else if (body.startsWith(GROUP_CHAT_NOTIFICATION_KEY_WORDS_ACCEPT)) {
                body = context.getString(R.string.group_chat_accept);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_FAILED)) {
                body = context.getString(R.string.group_chat_create_failed);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_GROUP_FULL)){
                body = context.getString(R.string.group_chat_member_full_add_error);
            } else if (body.startsWith(GroupChatConstants.CONST_NOTIFY_INVITE_EXPIRED)) {
                body = context.getString(R.string.group_invite_has_expired);
            }
        }
        return body;
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

    public static String createVcardFile(final Context context, Uri uri) {
        InputStream instream = null;
        Long curtime = System.currentTimeMillis();
        String time = curtime.toString();
        String vcardPath = RCS_MMS_VCARD_PATH + time + ".vcf";
        FileOutputStream fout = null;
        try {

            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            instream = fd.createInputStream();
            File file = new File(vcardPath);

            fout = new FileOutputStream(file);

            byte[] buffer = new byte[8000];
            int size = 0;
            while ((size = instream.read(buffer)) != -1) {
                fout.write(buffer, 0, size);
            }

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                    .fromFile(file)));

        } catch (IOException e) {
            RcsLog.w(e);
        } finally {
            if (null != instream) {
                try {
                    instream.close();
                } catch (IOException e) {
                    RcsLog.w(e);
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    RcsLog.w(e);
                }
            }
        }
        return vcardPath;
    }

    public static Bitmap decodeInSampleSizeBitmap(String imageFilePath) {
        Bitmap bitmap;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imageFilePath, options);
        options.inJustDecodeBounds = false;

        int inSampleSize = (int) (options.outHeight / (float) 200);
        if (inSampleSize <= 0)
            inSampleSize = 1;
        options.inSampleSize = inSampleSize;

        bitmap = BitmapFactory.decodeFile(imageFilePath, options);

        return bitmap;
    }

    public static Bitmap decodeInSampleSizeBitmap(Bitmap bitmap) {
        int inSampleSize = 200/bitmap.getHeight();
        if (inSampleSize >= 0) {
            return bitmap;
        } else {
            Matrix matrix = new Matrix();
            matrix.postScale(inSampleSize,inSampleSize);
            Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return resizeBmp;
        }
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

    public static boolean isLoading(String filePath, long fileSize) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        if (file.exists() && file.length() < fileSize) {
            return true;
        } else {
            return false;
        }
    }

    public static void addNumberToFirewall(Context context, ContactList list, boolean isBlacklist) {
        String number = list.get(0).getNumber();
        if (null == number || number.length() <= 0) {
            // number length is not allowed 0-
            Toast.makeText(context, context.getString(R.string.firewall_number_len_not_valid),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();

        number = number.replaceAll(" ", "");
        number = number.replaceAll("-", "");
        String comparenNumber = number;
        int len = comparenNumber.length();
        if (len > 11) {
            comparenNumber = number.substring(len - 11, len);
        }
        Uri blockUri = isBlacklist ? BLACKLIST_CONTENT_URI
                : WHITELIST_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Uri checkUri = isBlacklist ? WHITELIST_CONTENT_URI
                : BLACKLIST_CONTENT_URI;
        Cursor checkCursor = contentResolver.query(checkUri, new String[] {
                "_id", "number", "person_id", "name"
        }, "number" + " LIKE '%" + comparenNumber + "'", null, null);
        try {
            if (checkCursor != null && checkCursor.getCount() > 0) {
                String toast = isBlacklist ? context.getString(R.string.firewall_number_in_white)
                        : context.getString(R.string.firewall_number_in_black);
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
                return;
            }
        } finally {
            if (checkCursor != null) {
                checkCursor.close();
                checkCursor = null;
            }
        }

        values.put("number", comparenNumber);
        Uri mUri = contentResolver.insert(blockUri, values);

        Toast.makeText(context, context.getString(R.string.firewall_save_success),
                Toast.LENGTH_SHORT).show();
    }

    public static boolean showFirewallMenu(Context context, ContactList list,
            boolean isBlacklist) {
        String number = list.get(0).getNumber();
        if (null == number || number.length() <= 0) {
            return false;
        }
        number = number.replaceAll(" ", "");
        number = number.replaceAll("-", "");
        String comparenNumber = number;
        int len = comparenNumber.length();
        if (len > 11) {
            comparenNumber = number.substring(len - 11, len);
        }
        Uri blockUri = isBlacklist ? BLACKLIST_CONTENT_URI
                : WHITELIST_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cu = contentResolver.query(blockUri, new String[] {
                "_id", "number", "person_id", "name"},
                "number" + " LIKE '%" + comparenNumber + "'",
                null, null);
        try {
            if (cu != null && cu.getCount() > 0) {
                    return false;
            }
        } finally {
            if (cu != null) {
                cu.close();
                cu = null;
            }
        }
        return true;
    }

    public static boolean isFireWallInstalled(Context context) {
        boolean installed = false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    FIREWALL_APK_NAME, PackageManager.GET_PROVIDERS);
            installed = (info != null);
        } catch (NameNotFoundException e) {
        }
        return installed;
    }

    public static String getNumbersExceptMe(ContactList recipients)
            throws ServiceDisconnectedException, RemoteException {
        BasicApi basicApi = BasicApi.getInstance();
        String myPhoneNumber = basicApi.getAccount();
        String numbers = "";

        int size = recipients.size();
        for (int i = 0; i < size; i++) {
            String number = recipients.get(i).getNumber();

            // Skip my phone number.
            if (myPhoneNumber != null && myPhoneNumber.endsWith(number)) {
                continue;
            }

            numbers += number;
            if (i + 1 < size) {
                numbers += ";";
            }
        }

        return numbers;
    }

    public static String getGroupChatDialNumbers(GroupChat groupChat)
            throws ServiceDisconnectedException, RemoteException {
        String numbers = "";
        if (groupChat != null) {
            List<GroupChatMember> users = GroupChatApi.getInstance()
                    .getMembers(groupChat.getId());
            if (users != null) {
                BasicApi basicApi = BasicApi.getInstance();
                String myPhoneNumber = basicApi.getAccount();
                int size = users.size();
                for (int i = 0; i < size; i++) {
                    String number = users.get(i).getNumber();

                    // Skip my phone number.
                    if (myPhoneNumber != null && myPhoneNumber.endsWith(number)) {
                        continue;
                    }

                    numbers += number;
                    if (i + 1 < size) {
                        numbers += ";";
                    }
                }
            }
        }

        return numbers;
    }

    public static ArrayList<String> getGroupChatNumbersExceptMe(GroupChat groupChat)
            throws ServiceDisconnectedException, RemoteException {
        ArrayList<String> numbers= new ArrayList<String>();
        if (groupChat != null) {
            List<GroupChatMember> users = GroupChatApi.getInstance()
                    .getMembers(groupChat.getId());
            if (users != null) {
                BasicApi basicApi = BasicApi.getInstance();
                String myPhoneNumber = basicApi.getAccount();
                int size = users.size();
                for (int i = 0; i < size; i++) {
                    String number = users.get(i).getNumber();
                    String number1 = number.substring(3);
                    // Skip my phone number.
                    if (myPhoneNumber != null && myPhoneNumber.endsWith(number)) {
                        continue;
                    }
                    numbers.add(number1);
                }
            }
        }

        return numbers;
    }

    public static void dialGroupChat(Context context, GroupChat groupChat) {
        try {
            String dialNumbers = getGroupChatDialNumbers(groupChat);
            onShowConferenceCallStartScreen(context, dialNumbers);
        } catch (Exception e) {
            onShowConferenceCallStartScreen(context);
        }
    }

    public static void dialConferenceCall(Context context, ContactList recipients) {
        try {
            String dialNumbers = getNumbersExceptMe(recipients);
            onShowConferenceCallStartScreen(context, dialNumbers);
        } catch (Exception e) {
            onShowConferenceCallStartScreen(context);
        }
    }

    private static void initExceptionHandler() {
        if (null != exceptionHandler) {
            return;
        }
        Looper.prepare();
        exceptionHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case FILE_NAME_NOT_FIT:
                        toast(R.string.file_suffix_vaild_tip);
                        break;
                    case FILE_DURATION_TOO_LONG:
                        toast(R.string.file_size_over);
                        break;
                    case FILE_NOT_EXIST:
                        toast(R.string.file_not_exist);
                        break;
                    case FILE_SIZE_TOO_LARGE:
                        toast(R.string.file_size_over);
                        break;
                    default:
                        break;
                }
            }
        };
        Looper.loop();
    }

    private static void toast(int resId) {
        Toast.makeText(MmsApp.getApplication().getApplicationContext(),
                resId, Toast.LENGTH_LONG).show();
    }
    public static void disposeRcsSendMessageException(Context context, Exception exception,
            int msgType) {
        exception.printStackTrace();
        initExceptionHandler();
        if (exception instanceof FileSuffixException) {
            exceptionHandler.sendEmptyMessage(FILE_NAME_NOT_FIT);
        } else if (exception instanceof FileDurationException){
            exceptionHandler.sendEmptyMessage(FILE_DURATION_TOO_LONG);
        } else if (exception instanceof FileTooLargeException){
            exceptionHandler.sendEmptyMessage(FILE_SIZE_TOO_LARGE);
        } else if (exception instanceof FileNotExistsException){
            exceptionHandler.sendEmptyMessage(FILE_NOT_EXIST);
        }
    }

    public static void setThumbnailForMessageItem(Context context, ImageView imageView,
            MessageItem messageItem) {
        if (messageItem.getRcsMsgType() == RCS_MSG_TYPE_PAID_EMO) {
            String[] body = messageItem.getRcsPath().split(",");
            imageView.setImageResource(R.drawable.rcs_default_emotion);
            RcsEmojiStoreUtil.getInstance().loadImageAsynById(imageView, body[0],
                    RcsEmojiStoreUtil.EMO_STATIC_FILE);
            return;
        }
        Bitmap bitmap = null;
        switch (messageItem.getRcsMsgType()) {
            case RCS_MSG_TYPE_IMAGE: {
                String thumbPath = formatFilePathIfExisted(messageItem.getRcsThumbPath());
                messageItem.setRcsThumbPath(thumbPath);
                bitmap = decodeInSampleSizeBitmap(thumbPath);
                break;
            }
            case RCS_MSG_TYPE_VIDEO: {
                bitmap = BitmapFactory.decodeFile(messageItem.getRcsThumbPath());
                break;
            }
            case RCS_MSG_TYPE_VCARD: {
                String vcardFilePath = messageItem.getRcsPath();
                List<VNode> contactList = RcsMessageOpenUtils.rcsVcardContactList
                        (context, vcardFilePath);
                if (contactList != null) {
                    if (contactList.size() == 1) {
                        ArrayList<PropertyNode> propList = RcsMessageOpenUtils.openRcsVcardDetail(
                                context, vcardFilePath);
                        if (propList != null) {
                            for (PropertyNode propertyNode : propList) {
                                if ("PHOTO".equals(propertyNode.propName)) {
                                    if (propertyNode.propValue_bytes != null) {
                                        byte[] bytes = propertyNode.propValue_bytes;
                                        bitmap = BitmapFactory.decodeByteArray
                                                (bytes, 0, bytes.length);
                                        bitmap = decodeInSampleSizeBitmap(bitmap);
                                    }
                                }
                            }
                        }
                    } else {
                        bitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.rcs_group_vcard_image);
                    }
                }

                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_attach_vcard);
                }
                break;
            }
            case RCS_MSG_TYPE_AUDIO: {
                bitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.rcs_voice);
                break;
            }
            case RCS_MSG_TYPE_MAP: {
                bitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.rcs_map);
                break;
            }
            case RCS_MSG_TYPE_CAIYUNFILE:{
                bitmap = BitmapFactory.decodeResource(
                        context.getResources(),R.drawable.rcs_ic_cloud);
                break;
            }
        }
        if (messageItem.getRcsMsgType() == RCS_MSG_TYPE_VCARD) {
            imageView.setBackground(new BitmapDrawable(bitmap));
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    public static void setThumbnailForMessageItem(Context context,
            ImageView imageView, WorkingMessage workingMessage) {
        int rcsType = workingMessage.getRcsType();
        Bitmap bitmap = null;
        switch (rcsType) {
            case RCS_MSG_TYPE_IMAGE: {
                String imagePath = workingMessage.getRcsPath();
                if (imagePath != null
                        && !new File(imagePath).exists() && imagePath.contains(".")) {
                    imagePath = imagePath.substring(0, imagePath.lastIndexOf("."));
                }
                bitmap = decodeInSampleSizeBitmap(imagePath);
                break;
            }
            case RCS_MSG_TYPE_VIDEO: {
                String videoPath = workingMessage.getRcsPath();
                bitmap = getVideoThumbnail(context, videoPath);
                break;
            }
            case RCS_MSG_TYPE_VCARD: {
                bitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.ic_attach_vcard);
                break;
            }
            case RCS_MSG_TYPE_AUDIO: {
                bitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.rcs_voice);
                break;
            }
            case RCS_MSG_TYPE_MAP: {
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rcs_map);
                break;
            }
            case RCS_MSG_TYPE_CAIYUNFILE:{
                bitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.rcs_ic_cloud);
                break;
            }
            default: {
                break;
            }
        }
        imageView.setImageBitmap(bitmap);
    }

    private static Bitmap getVideoThumbnail(Context context, String path) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(path,
                MediaStore.Images.Thumbnails.MICRO_KIND);
        int px = dip2px(context, DEFAULT_THUMBNAIL_SIZE_IN_DP);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, px, px,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    public static String getContentTypeForMessageItem(MessageItem messageItem) {
        String contentType = "";
        switch (messageItem.getRcsMsgType()) {
            case RCS_MSG_TYPE_IMAGE: {
                contentType = messageItem.getRcsMimeType();
                if (contentType == null) {
                    contentType = RCS_MSG_IMAGE_TYPE_ALL;
                }
                break;
            }
            case RCS_MSG_TYPE_VIDEO: {
                contentType = RCS_MSG_VIDEO_TYPE_ALL;
                break;
            }
            case RCS_MSG_TYPE_VCARD: {
                contentType = RCS_MSG_VCARD_TYPE;
                break;
            }
            case RCS_MSG_TYPE_AUDIO: {
                contentType = RCS_MSG_AUDO_TYPE_ALL;
                break;
            }
            case RCS_MSG_TYPE_MAP: {
                contentType = RCS_MSG_MAP_TYPE_ALL;
                break;
            }
            default:
                break;
        }
        return contentType;
    }

    public static String getRcsMessageStatusText(MessageListItem msgListItem,
            MessageItem msgItem) {
        Context context = msgListItem.getContext();
        String text = "";
        if(TextUtils.isEmpty(msgItem.getTimestamp())){
            if(msgItem.getDate() == 0){
                msgItem.setDate(System.currentTimeMillis());
            }
            String timesTamp = String.format(context.getString(R.string.sent_on),
                    MessageUtils.formatTimeStampString(context, msgItem.getDate()));
            msgItem.setTimestamp(timesTamp);
        }
        switch (msgItem.getRcsMsgState()) {
            case MESSAGE_SENDING:
                if ((msgItem.getRcsMsgType() == RCS_MSG_TYPE_IMAGE ||
                        msgItem.getRcsMsgType() == RCS_MSG_TYPE_VIDEO)) {
                    Long percent = RcsFileTransferCache.getInstance()
                            .getFileTransferPercent(msgItem.getMessageId());
                    if (percent == null) {
                         percent = (long)0;
                    }
                    text = context.getString(R.string.uploading_percent,
                            percent.intValue());
                } else {
                    text = context.getString(R.string.message_adapte_sening);
                }
                break;
            case MESSAGE_HAS_SENDED:
                text = context.getString(R.string.message_adapter_has_send)
                        + "  " + msgItem.getTimestamp();
                break;
            case MESSAGE_FAIL:
                if (msgItem.getRcsMsgType() == RCS_MSG_TYPE_TEXT) {
                    text = context.getString(R.string.message_send_fail);
                } else {
                    text = context.getString(R.string.message_send_fail_resend);
                }
                break;
            case MESSAGE_SEND_RECEIVE:
                text = context.getString(R.string.message_received) + "  "
                        + msgItem.getTimestamp();
                break;
            case MESSAGE_HAS_BURNED:
                text = context.getString(R.string.message_has_been_burned);

                if (msgItem.getRcsMsgState() != MESSAGE_HAS_BURNED)
                    burnMessageAtLocal(context, msgItem.getMessageId());
                break;
            default:
                text = context.getString(R.string.message_adapte_sening);
                break;
        }

        return text;
    }

    public static void startEmojiStore(Activity activity, int requestCode) {
        if (isPackageInstalled(activity, "com.temobi.dm.emoji.store")) {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName("com.temobi.dm.emoji.store",
                    "com.temobi.dm.emoji.store.activity.EmojiActivity");
            intent.setComponent(comp);
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(activity, R.string.install_emoj_store,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isDeletePrefixSpecailNumberAvailable(Context context){
        boolean isDeleSpecailNumber = context.getResources()
            .getBoolean(R.bool.config_mms_delete_prefix_special_number);
            SpecialServiceNumApi specailNumApi = SpecialServiceNumApi
                    .getInstance();
        try{
            if (!isDeleSpecailNumber) {
                specailNumApi.disableSsn();
            } else {
                specailNumApi.enableSsn();
                List<String> specailNum = new ArrayList<String>();
                specailNum = specailNumApi.getSsnList();
                if (specailNum == null || 0 == specailNum.size()) {
                    String[] specialNumberItems = context.getResources()
                        .getStringArray(R.array.special_prefix_number);
                    for (int i = 0; i < specialNumberItems.length; i++)
                        specailNumApi.addSsn(specialNumberItems[i]);
                }
            }
        } catch (ServiceDisconnectedException e){
            RcsLog.w(e);
        } catch (RemoteException e) {
            RcsLog.w(e);
        }
        return isDeleSpecailNumber;
    }

    public static String getCaiYunFileBodyText(Context context, MessageItem msgItem){
        MessageApi messageApi = MessageApi.getInstance();
        CloudFileMessage cloudMessage = messageApi.parseCloudFileMessage(msgItem.getMsgBody());
        return context.getString(R.string.cloud_file_name) + cloudMessage.getFileName() +
               context.getString(R.string.cloud_file_size) + cloudMessage.getFileSize() + "KB";
    }

   public static String saveRcsMassage(Context context, MessageItem msgItem) {
       InputStream input = null;
       FileOutputStream fout = null;
       File file = null;
       try {
           if (msgItem == null) {
                return "";
            }
            int msgType = msgItem.getRcsMsgType();
            if (msgType != RCS_MSG_TYPE_AUDIO
                    && msgType != RCS_MSG_TYPE_IMAGE
                    && msgType != RCS_MSG_TYPE_VIDEO) {
                return "";    // we only save pictures, videos, and sounds.
            }
            String filePath = msgItem.getRcsPath();
            long fileSize = msgItem.getRcsMsgFileSize();
            if(isLoading(filePath, fileSize)){
                return "";
            }
            String fileName = msgItem.getRcsPath();
            int fileNameInex = fileName.lastIndexOf('/');
            fileName = fileName.substring(fileNameInex + 1, fileName.length());
            input = new FileInputStream(filePath);

            String dir = Environment.getExternalStorageDirectory() + "/"
                    + Environment.DIRECTORY_DOWNLOADS  + "/";
            String extension;
            int index;
            index = fileName.lastIndexOf('.');
            extension = fileName.substring(index + 1, fileName.length());
            fileName = fileName.substring(0, index);
            // Remove leading periods. The gallery ignores files starting with a period.
            fileName = fileName.replaceAll("^.", "");

            file = getUniqueDestination(dir + fileName, extension);

            fout = new FileOutputStream(file);

            byte[] buffer = new byte[8000];
            int size = 0;
            while ((size=input.read(buffer)) != -1) {
                fout.write(buffer, 0, size);
            }
            // Notify other applications listening to scanner events
            // that a media file has been added to the sd card
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)));

        } catch (IOException e) {
            // Ignore
            RcsLog.e("IOException caught while opening or reading stream" + e.toString());
            return "";
        } finally {
            closeQuietly(input);
            closeQuietly(fout);
        }
        return file.toString();
    }

    private static File getUniqueDestination(String base, String extension) {
        File file = new File(base + "." + extension);

        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
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

    public static Intent OpenFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;
        String end = file.getName()
                .substring(file.getName().lastIndexOf(".") + 1, file.getName().length())
                .toLowerCase();
        RcsLog.i("END=" + end);
        if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") || end.equals("xmf")
                || end.equals("ogg") || end.equals("wav") || end.equals("amr")) {
            return getAudioFileIntent(filePath);
        } else if(end.equals("GIF")|| end.equals("gif")) {
            return getGifIntent(filePath);
        } else if (end.equals("3gp") || end.equals("mp4")) {
            return getVideoFileIntent(filePath);
        } else if (end.equals("jpg")  || end.equals("png")
                || end.equals("jpeg") || end.equals("bmp") ) {
            return getImageFileIntent(filePath);
        } else if (end.equals("apk")) {
            return getApkFileIntent(filePath);
        } else if (end.equals("ppt")) {
            return getPptFileIntent(filePath);
        } else if (end.equals("xls")) {
            return getExcelFileIntent(filePath);
        } else if (end.equals("doc")) {
            return getWordFileIntent(filePath);
        } else if (end.equals("pdf")) {
            return getPdfFileIntent(filePath);
        } else if (end.equals("chm")) {
            return getChmFileIntent(filePath);
        } else if (end.equals("txt")) {
            return getTextFileIntent(filePath, false);
        } else {
            return getAllIntent(filePath);
        }
    }

    private static Intent getCommonIntent(String type, String param, int flag) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        if (flag > 0) {
            intent.addFlags(flag);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        if (param != null && type != null) {
            Uri uri = Uri.fromFile(new File(param));
            intent.setDataAndType(uri, type);
        }
        return intent;
    }

    private static Intent getVideoFileIntent(String param) {
        Intent intent = getCommonIntent(RCS_MSG_VIDEO_TYPE_ALL, param, 0);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        return intent;
    }

    private static Intent getAudioFileIntent(String param) {
        Intent intent = getCommonIntent("audio/*", param, 0);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        return intent;
    }

    private static Intent getGifIntent(String param) {
        Intent intent = getCommonIntent("image/gif", param, 0);
        intent.setAction("com.android.gallery3d.VIEW_GIF");
        return intent;
    }

    private static Intent getImageFileIntent(String param) {
        Intent intent = getCommonIntent(RCS_MSG_IMAGE_TYPE_ALL, param,
                Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    public static Intent getAllIntent( String param ) {
        return getCommonIntent("*/*", param, Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getApkFileIntent( String param ) {
        return getCommonIntent("application/vnd.android.package-archive", param,
                Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getHtmlFileIntent( String param ) {
        Uri uri = Uri.parse(param ).buildUpon().
                encodedAuthority("com.android.htmlfileprovider").
                scheme("content").encodedPath(param ).build();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        return intent;
    }

    public static Intent getPptFileIntent( String param ) {
        return getCommonIntent("application/vnd.ms-powerpoint", param,
                Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getExcelFileIntent( String param ) {
        return getCommonIntent("application/vnd.ms-excel", param, Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getWordFileIntent( String param ) {
        return getCommonIntent("application/msword", param, Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getChmFileIntent( String param ) {
        return getCommonIntent("application/x-chm", param, Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getTextFileIntent( String param, boolean paramBoolean) {
        if (paramBoolean) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.parse(param);
            intent.setDataAndType(uri, "text/plain");
            return intent;
        } else {
            return getCommonIntent("text/plain", param, Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    public static Intent getPdfFileIntent( String param ) {
        return getCommonIntent("application/pdf", param,
                Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static ArrayList<String> removeDuplicateNumber(ArrayList<String> list) {
        HashSet<String> hashSet = new HashSet<String>(list);
        list.clear();
        list.addAll(hashSet);
        return list;
    }

    public static void addPublicAccountItem(final Context context, ListView listView) {
        if (!SupportApi.getInstance().isRcsSupported()
                || !isPackageInstalled(context, NATIVE_UI_PACKAGE_NAME)) {
            return;
        }
        View view = LayoutInflater.from(context).inflate(
                R.layout.conversation_list_item, null);
        Drawable drawable = context.getResources().getDrawable(
               R.drawable.rcs_ic_public_account_photo);
        Drawable background = context.getResources().getDrawable(
                R.drawable.conversation_item_background_read);
        ConversationListItem publicAccountView = (ConversationListItem) view;
        publicAccountView.bind(context.getResources()
                .getString(R.string.public_account), null);
        publicAccountView.bindAvatar(drawable);
        publicAccountView.setBackground(background);
        listView.addHeaderView(publicAccountView);
        publicAccountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPublicAccountActivity(context);
            }
        });
    }

    public static void addNotificationItem(final Context context, ListView listView) {
        if (!SupportApi.getInstance().isRcsSupported()
                || !isPackageInstalled(context, NATIVE_UI_PACKAGE_NAME)) {
            return;
        }
        View view = LayoutInflater.from(context).inflate(
                R.layout.conversation_list_item, null);
        Drawable drawable = context.getResources().getDrawable(
                R.drawable.rcs_ic_notification_list_photo);
        Drawable background = context.getResources().getDrawable(
                R.drawable.conversation_item_background_read);
        ConversationListItem notificationView = (ConversationListItem) view;
        notificationView.bind(context.getResources().getString(R.string.notifications),
                null);
        notificationView.bindAvatar(drawable);
        notificationView.setBackground(background);
        listView.addHeaderView(notificationView);
        notificationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNotificationListActivity(context);
            }
        });
    }

    /**
     * Launch the RCS notify list activity.
     */
    private static void startNotificationListActivity(Context context) {
        Intent intent = new Intent(RCS_NATIVE_UI_ACTION_NOTIFICATION_LIST);
        startSafeActivity(context, intent);
    }

    private static void startPublicAccountActivity(Context context) {
        Intent intent = new Intent(RCS_NATIVE_UI_ACTION_CONVERSATION_LIST);
        startSafeActivity(context, intent);
    }

    public static boolean isCloudFileDownLoadOk(CloudFileMessage cMessage) throws RemoteException,
            ServiceDisconnectedException {
        if (cMessage == null) {
            return false;
        }
        String filePath = CloudFileApi.getInstance().getLocalRootPath() + cMessage.getFileName();
        RcsLog.i("RcsUtils.isCloudFileDownLoadOk filePath = " + filePath);
        return RcsChatMessageUtils.isFileDownload(filePath, cMessage.getFileSize());
    }

    public static boolean isFileDownLoadoK(MessageItem mMsgItem) {
        if (mMsgItem == null ){
            return false;
        }
        String filePath = mMsgItem.getRcsPath();
        return RcsChatMessageUtils.isFileDownload(filePath, mMsgItem.getRcsMsgFileSize());
    }

    public static boolean isFileDownBeginButNotEnd(String filePath, long fileSize){

        boolean isFileDownload = false;
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        if (fileSize == 0) {
            return false;
        }
        boolean isBeginButNotEnd = false;
        File file = new File(filePath);
        if (file != null) {
            if (file.exists() && file.length() > 0 && file.length() < fileSize) {
                isBeginButNotEnd = true;
            }
        }
        return isBeginButNotEnd;
    }

    public static String getPhoneNumberTypeStr(Context context, PropertyNode propertyNode) {
        String numberTypeStr = "";
        if (null == propertyNode.paramMap_TYPE
                || propertyNode.paramMap_TYPE.size() == 0) {
            return numberTypeStr;
        }
        String number = propertyNode.propValue;
        if (propertyNode.paramMap_TYPE.size() == 2) {
            if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("HOME")) {
                numberTypeStr = context
                        .getString(R.string.vcard_number_fax_home) + number;
            } else if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberTypeStr = context
                        .getString(R.string.vcard_number_fax_work) + number;
            } else if (propertyNode.paramMap_TYPE.contains("PREF")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberTypeStr = context
                        .getString(R.string.vcard_number_pref_work) + number;
            } else if (propertyNode.paramMap_TYPE.contains("CELL")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                numberTypeStr = context
                        .getString(R.string.vcard_number_call_work) + number;
            } else if (propertyNode.paramMap_TYPE.contains("WORK")
                    && propertyNode.paramMap_TYPE.contains("PAGER")) {
                numberTypeStr = context
                        .getString(R.string.vcard_number_work_pager) + number;
            } else {
                numberTypeStr = context.getString(R.string.vcard_number_other)
                        + number;
            }
        } else {
            if (propertyNode.paramMap_TYPE.contains("CELL")) {
                numberTypeStr = context.getString(R.string.vcard_number)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("HOME")) {
                numberTypeStr = context.getString(R.string.vcard_number_home)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("WORK")) {
                numberTypeStr = context.getString(R.string.vcard_number_work)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("PAGER")) {
                numberTypeStr = context.getString(R.string.vcard_number_pager)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("VOICE")) {
                numberTypeStr = context.getString(R.string.vcard_number_other)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("CAR")) {
                numberTypeStr = context.getString(R.string.vcard_number_car)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("ISDN")) {
                numberTypeStr = context.getString(R.string.vcard_number_isdn)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("PREF")) {
                numberTypeStr = context.getString(R.string.vcard_number_pref)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("FAX")) {
                numberTypeStr = context.getString(R.string.vcard_number_fax)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("TLX")) {
                numberTypeStr = context.getString(R.string.vcard_number_tlx)
                        + number;
            } else if (propertyNode.paramMap_TYPE.contains("MSG")) {
                numberTypeStr = context.getString(R.string.vcard_number_msg)
                        + number;
            } else {
                numberTypeStr = context.getString(R.string.vcard_number_other)
                        + number;
            }
        }
        return numberTypeStr;
    }

    public static int getVcardNumberType(PropertyNode propertyNode) {
        if (null == propertyNode.paramMap_TYPE
                || propertyNode.paramMap_TYPE.size() == 0) {
            return 0;
        }
        if (propertyNode.paramMap_TYPE.size() == 2) {
            if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("HOME")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME;
            } else if (propertyNode.paramMap_TYPE.contains("FAX")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;
            } else if (propertyNode.paramMap_TYPE.contains("PREF")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN;
            } else if (propertyNode.paramMap_TYPE.contains("CELL")
                    && propertyNode.paramMap_TYPE.contains("WORK")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE;
            } else if (propertyNode.paramMap_TYPE.contains("WORK")
                    && propertyNode.paramMap_TYPE.contains("PAGER")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER;
            } else {
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            }
        } else {
            if (propertyNode.paramMap_TYPE.contains("CELL")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            } else if (propertyNode.paramMap_TYPE.contains("HOME")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
            } else if (propertyNode.paramMap_TYPE.contains("WORK")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
            } else if (propertyNode.paramMap_TYPE.contains("PAGER")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
            } else if (propertyNode.paramMap_TYPE.contains("VOICE")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            } else if (propertyNode.paramMap_TYPE.contains("CAR")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_CAR;
            } else if (propertyNode.paramMap_TYPE.contains("ISDN")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_ISDN;
            } else if (propertyNode.paramMap_TYPE.contains("PREF")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            } else if (propertyNode.paramMap_TYPE.contains("FAX")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;
            } else if (propertyNode.paramMap_TYPE.contains("TLX")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_TELEX;
            } else if (propertyNode.paramMap_TYPE.contains("MSG")) {
                return ContactsContract.CommonDataKinds.Phone.TYPE_MMS;
            } else {
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            }
        }
    }

    public static String disposeVcardMessage(Context context, String str){
        String vcardFilePath = str;
        List<VNode> contactList = RcsMessageOpenUtils.rcsVcardContactList
                (context, vcardFilePath);
        if (contactList == null) {
            return "";
        }

        if (contactList.size() == 1) {
            ArrayList<PropertyNode> propList = RcsMessageOpenUtils.
                    openRcsVcardDetail(context, vcardFilePath);
            if (propList == null) {
                return "";
            }
            String name = "";
            String number = "";
            String phoneNumber = null;
            String homeNumber = null;
            String workNumber = null;
            for (PropertyNode propertyNode : propList) {
                if ("FN".equals(propertyNode.propName)) {
                    if(!TextUtils.isEmpty(propertyNode.propValue)){
                        name = propertyNode.propValue;
                    }
                } else if ("TEL".equals(propertyNode.propName)) {
                    if(!TextUtils.isEmpty(propertyNode.propValue)){
                        if (propertyNode.paramMap_TYPE.contains("CELL")
                                && !propertyNode.paramMap_TYPE.contains("WORK")) {
                            phoneNumber = context.getString(R.string.vcard_number)
                                    + propertyNode.propValue;
                        } else if (propertyNode.paramMap_TYPE.contains("HOME")
                                && !propertyNode.paramMap_TYPE.contains("FAX")){
                            homeNumber = context.getString(R.string.vcard_number_home)
                                    + propertyNode.propValue;
                        } else if(propertyNode.paramMap_TYPE.contains("WORK")
                                && !propertyNode.paramMap_TYPE.contains("FAX")){
                            workNumber = context.getString(R.string.vcard_number_work)
                                    + propertyNode.propValue;
                        } else {
                            number = RcsUtils.getPhoneNumberTypeStr(context, propertyNode);
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(phoneNumber)) {
                number = phoneNumber;
            } else if (!TextUtils.isEmpty(homeNumber)) {
                number = homeNumber;
            } else if (!TextUtils.isEmpty(workNumber)) {
                number = workNumber;
            }
            return "[Vcard]\n" + context.getString(R.string.vcard_name)
                    + name + "\n" + number;
        } else if (contactList.size() > 1) {
            String contactDetail = String.format(
                    context.getString(R.string.vcard), contactList.size());
            return contactDetail;
        } else {
            return "";
        }
    }

    public static byte[] getBytesFromFile(File f) {
        if (f == null) {
            return null;
        }
        FileInputStream stream = null;
        ByteArrayOutputStream out = null;
        try {
            stream = new FileInputStream(f);
            out = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = stream.read(b)) != -1) {
                out.write(b, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            RcsLog.w(e);
        } finally {
            closeQuietly(out);
            closeQuietly(stream);
        }
        return null;
    }

    private static void closeQuietly (Closeable os) {
        if (null != os) {
            try {
                os.close();
            } catch (IOException e) {
                RcsLog.w(e);
            }
        }
    }
    public static String formatConversationSnippet(Context context,
            String snippet, int rcsMsgType) {
        switch(rcsMsgType) {
            case RCS_MSG_TYPE_IMAGE:
                snippet = context.getString(R.string.msg_type_image);
                break;
            case RCS_MSG_TYPE_VIDEO:
                snippet = context.getString(R.string.msg_type_video);
                break;
            case RCS_MSG_TYPE_AUDIO:
                snippet = context.getString(R.string.msg_type_audio);
                break;
            case RCS_MSG_TYPE_VCARD:
                snippet = context.getString(R.string.msg_type_contact);
                break;
            case RCS_MSG_TYPE_MAP:
                snippet = context.getString(R.string.msg_type_location);
                break;
            case RCS_MSG_TYPE_CAIYUNFILE:
                snippet = context.getString(R.string.msg_type_CaiYun);
                break;
            case RCS_MSG_TYPE_PAID_EMO:
                snippet = context.getString(R.string.msg_type_paid_emo);
                break;
            default:
                break;
        }
        return snippet;
    }

    public static int getActivePhoneId(Context context) {
        try {
            Class<?> telephonyManager = Class.forName("android.telephony.TelephonyManager");
            Method method = telephonyManager.getMethod("getPhoneCount");
            TelephonyManager tm = (TelephonyManager)context
                    .getSystemService(Context.TELEPHONY_SERVICE);

            // get imsi by getPhoneCount()
            int phoneCount = (Integer)method.invoke(tm);
            RcsLog.d("RcsUtils.getActivePhoneId: phoneCount:" + phoneCount);

            method = telephonyManager.getMethod("getSimState", int.class);
            for (int index = 0; index < phoneCount; index++) {
                int state = (int)method.invoke(tm, index);
                if (state != TelephonyManager.SIM_STATE_ABSENT) {
                    return index;
                }
            }
        } catch (Exception e) {
            RcsLog.w(e);
        }

        return 0;
    }

    public static boolean isRcsOnline() {
        try {
            return BasicApi.getInstance().isOnline();
        } catch (Exception e) {
            RcsLog.w(e);
            return false;
        }
    }

    public static boolean isRcsMessage(int chatType) {
        return (chatType > RCS_CHAT_TYPE_DEFAULT && chatType < RCS_CHAT_TYPE_PUBLIC_MESSAGE)
                || chatType == RCS_CHAT_TYPE_TO_PC;
    }

    public static String formatFilePathIfExisted(String filePath) {
        if (filePath != null && !new File(filePath).exists()
                && filePath.contains(".")) {
            filePath = filePath.substring(0,filePath.lastIndexOf("."));
        }
        return filePath;
    }

    public static void deleteGroupchatByThreadIds(Context context, Collection<Long> threadIds,
            boolean deleteLocked, boolean deleteAll) {
        Long[] deleteThreadId = null;
        if (deleteLocked && deleteAll) {
            try {
                GroupChatApi.getInstance().deleteAllGroupChat();
            } catch(Exception e) {
                RcsLog.w(e);
            }
        } else if (deleteLocked && threadIds != null) {
            deleteThreadId = new Long[threadIds.size()];
            threadIds.toArray(deleteThreadId);
        } else {
            Cursor cursor = null;
            String where = Sms.LOCKED + " = 0 and " +
                    RcsColumns.SmsRcsColumns.RCS_CHAT_TYPE
                    + "= 3" + ") group by "+ Sms.THREAD_ID +" -- (";
            try {
                cursor = context.getContentResolver().query(Sms.CONTENT_URI,
                        new String[] {Sms.THREAD_ID}, where, null, null);
                if (cursor != null) {
                    int threadCount = cursor.getCount();
                    if (threadCount > 0) {
                        deleteThreadId = new Long[threadCount];
                        int index = 0;
                        while(cursor.moveToNext()) {
                            long threadId = cursor.getLong(0);
                            if (threadIds == null) {
                                deleteThreadId[index] = threadId;
                                index++;
                            } else if (threadIds.contains(threadId)){
                                deleteThreadId[index] = threadId;
                                index++;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                RcsLog.w(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        try {
            if (deleteThreadId.length > 0) {
                long[] deleteThreadIds = new long[deleteThreadId.length];
                for (int i = 0; i < deleteThreadIds.length; i++) {
                    deleteThreadIds[i] = deleteThreadId[i];
                }
                 GroupChatApi.getInstance().deleteGroupChat(deleteThreadIds);
            }
        } catch(Exception e) {
            RcsLog.w(e);
        }
    }

    public static String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }
    public static String getAccount() {
        try {
            return BasicApi.getInstance().getAccount();
        } catch (Exception e) {
            RcsLog.w(e);
            return "";
        }
    }

    public static void updateFaildMessageType(Context context, long dataId) {
        if (dataId <= 0) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put("type", 5);
            values.put("read", 0);
            ContentResolver resolver = context.getContentResolver();
            resolver.update(Sms.CONTENT_URI, values, Sms._ID + " = ?", new String[] {
                    String.valueOf(dataId)
                    });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isRcsAttachmentEnabled(Context context) {
        return SupportApi.getInstance().isRcsSupported()
                && RcsDualSimMananger.getUserIsUseRcsPolicy(context);
    }

    public static boolean isMyAccount(String str) {
        if (str == null) {
            return false;
        }
        String account = RcsUtils.getAccount().trim().replace(" ", "").replace("+86", "");
        return str.endsWith(account);
    }
}
