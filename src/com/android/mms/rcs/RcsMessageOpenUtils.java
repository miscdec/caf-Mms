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

import com.android.mms.ui.MessageItem;
import com.android.mms.ui.MessageListItem;
import com.android.mms.R;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonConstant;
import com.suntek.mway.rcs.client.aidl.plugin.entity.mcloudfile.TransNode;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;
import com.suntek.mway.rcs.client.aidl.provider.model.CloudFileMessage;
import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.api.mcloud.McloudFileApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class RcsMessageOpenUtils {
    private static final String LOG_TAG = "RCS_UI";

    public static void retransmisMessage(MessageItem messageItem) {
        try {
            RcsApiManager.getMessageApi().retransmitMessageById(
                    String.valueOf(messageItem.mRcsId));
        } catch (ServiceDisconnectedException e) {
            Log.w(LOG_TAG, e);
        }
        return;
    }

    public static void resendOrOpenRcsMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        if (messageItem.mRcsMsgState == RcsUtils.MESSAGE_FAIL
                && messageItem.mRcsType != RcsUtils.RCS_MSG_TYPE_TEXT) {
            try {
                RcsApiManager.getMessageApi().retransmitMessageById(
                        String.valueOf(messageItem.mRcsId));
            } catch (ServiceDisconnectedException e) {
                Toast.makeText(messageListItem.getContext(), R.string.rcs_service_is_not_available,
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            openRcsMessage(messageListItem);
        }
    }

    public static void openRcsMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        switch (messageItem.mRcsType) {
            case RcsUtils.RCS_MSG_TYPE_AUDIO:
                openRcsAudioMessage(messageListItem);
                break;
            case RcsUtils.RCS_MSG_TYPE_VIDEO:
                openRcsVideoMessage(messageListItem);
            case RcsUtils.RCS_MSG_TYPE_IMAGE:
                openRcsImageMessage(messageListItem);
                break;
            case RcsUtils.RCS_MSG_TYPE_VCARD:
                openRcsVCardMessage(messageListItem);
                break;
            case RcsUtils.RCS_MSG_TYPE_MAP:
                openRcsLocationMessage(messageListItem);
                break;
            case RcsUtils.RCS_MSG_TYPE_PAID_EMO:
                openRcsEmojiMessage(messageListItem);
                break;
            case RcsUtils.RCS_MSG_TYPE_CAIYUNFILE:
                openRcsCaiyunMessage(messageListItem);
                break;
            default:
                break;
        }
    }

    public static void openRcsSlideShowMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        String filePath = RcsUtils.getFilePath(messageItem.mRcsId, messageItem.mRcsPath);
        File File = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(File), messageListItem.mRcsContentType.toLowerCase());
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(messageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!messageItem.isMe() && !isFileDownload) {
            try {
                messageListItem.mDateView.setText(R.string.rcs_downloading);
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi.getMessageById(String.valueOf(messageItem.mRcsId));
                if (messageListItem.isDownloading() && !messageListItem.mRcsIsStopDown) {
                    messageListItem.mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                } else {
                    messageListItem.mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
            return;
        }
        if (isFileDownload) {
            messageListItem.getContext().startActivity(intent);
        }
    }

    public static void openRcsEmojiMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        try {
            String[] body = messageItem.mBody.split(",");
            byte[] data = RcsApiManager.getEmoticonApi().decrypt2Bytes(body[0],
                    EmoticonConstant.EMO_DYNAMIC_FILE);
            if(data == null || data.length <= 0){
                return;
            }
            RcsUtils.openPopupWindow(messageListItem.getContext(), messageListItem.mImageView, data);
        } catch (ServiceDisconnectedException e) {
            e.printStackTrace();
            return;
        }
    }

    public static void openRcsAudioMessage(MessageListItem messageListItem) {
        MessageItem mMessageItem = messageListItem.getMessageItem();
        try {
            String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
            File file = new File(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), messageListItem.mRcsContentType.toLowerCase());
            intent.setDataAndType(Uri.parse("file://" + mMessageItem.mRcsPath), "audio/*");
            messageListItem.getContext().startActivity(intent);
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    public static void openRcsVideoMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        String filePath = RcsUtils.getFilePath(messageItem.mRcsId, messageItem.mRcsPath);
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), messageListItem.mRcsContentType.toLowerCase());
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(messageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!messageItem.isMe() && !isFileDownload) {
            try {
                messageListItem.mDateView.setText(R.string.rcs_downloading);
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi.getMessageById(String.valueOf(messageItem.mRcsId));
                if (messageListItem.isDownloading() && !messageListItem.mRcsIsStopDown) {
                    messageListItem.mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                    messageListItem.mDateView.setText(R.string.stop_down_load);
                } else {
                    messageListItem.mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
            return;
        }
        messageListItem.getContext().startActivity(intent);
    }

    public static void openRcsImageMessage(MessageListItem messageListItem) {
        MessageItem mMessageItem = messageListItem.getMessageItem();
        String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), messageListItem.mRcsContentType.toLowerCase());
        if (mMessageItem.mRcsMimeType != null
                && mMessageItem.mRcsMimeType.endsWith("image/gif")) {
            intent.setAction("com.android.gallery3d.VIEW_GIF");
        }
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(
                    String.valueOf(mMessageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!mMessageItem.isMe() && !isFileDownload) {
            try {
                messageListItem.mDateView.setText(R.string.rcs_downloading);
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi.getMessageById(String
                        .valueOf(mMessageItem.mRcsId));
                if (messageListItem.isDownloading() && !messageListItem.mRcsIsStopDown) {
                    messageListItem.mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                    messageListItem.mDateView.setText(R.string.stop_down_load);
                } else {
                    messageListItem.mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
            return;
        }
        if (mMessageItem.isMe() || isFileDownload) {
            Log.i(LOG_TAG, "filePath=" + filePath);
            messageListItem.getContext().startActivity(intent);
        }
    }

    public static void openRcsVCardMessage(MessageListItem messageListItem) {
        RcsUtils.showOpenRcsVcardDialog(messageListItem.getContext(), messageListItem);
    }

    public static void openRcsLocationMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        String filePath = RcsUtils.getFilePath(messageItem.mRcsId, messageItem.mRcsPath);
        GeoLocation geo = RcsUtils.readMapXml(filePath);
        String geourl = "geo:" + geo.getLat() + "," + geo.getLng();
        try {
            Uri uri = Uri.parse(geourl);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geourl));
            messageListItem.getContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(messageListItem.getContext(), R.string.toast_install_map,
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void openRcsCaiyunMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        ChatMessage msg = null;
        boolean isFileDownload = false;
        CloudFileMessage cMessage = null;
        McloudFileApi api = null;
        TransNode.TransOper transOper = TransNode.TransOper.NEW;

        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(messageItem.mRcsId));
            cMessage = msg.getCloudFileMessage();
            api = RcsApiManager.getMcloudFileApi();
            if (msg != null)
                isFileDownload = RcsChatMessageUtils.isFileDownload(api.getLocalRootPath()
                                    + cMessage.getFileName(), cMessage.getFileSize());
            if(messageListItem.isDownloading()){
                transOper = TransNode.TransOper.RESUME;
            }

            if (!isFileDownload) {
                try {
                    messageListItem.mDateView.setText(R.string.rcs_downloading);
                    if (messageListItem.isDownloading() && messageListItem.getRcsIsStopDown()) {
                        messageListItem.mRcsIsStopDown = true;
                        if (messageListItem.operation != null) {
                            messageListItem.operation.pause();
                        }
                        messageListItem.mDateView.setText(R.string.stop_down_load);
                    } else {
                        messageListItem.mRcsIsStopDown = false;
                        messageListItem.operation = api.downloadFileFromUrl(cMessage.getShareUrl(),
                                cMessage.getFileName(), transOper, messageItem.mRcsId);
                    }
                } catch (Exception e) {
                    Log.w(LOG_TAG, e);
                }
                return;
            } else {
                String path = api.getLocalRootPath() + cMessage.getFileName();
                Intent intent2 = RcsUtils.OpenFile(path);
                messageListItem.getContext().startActivity(intent2);
            }
        } catch (Exception e) {
            if(e instanceof ActivityNotFoundException){
                Toast.makeText(messageListItem.getContext(), R.string.please_install_application,
                        Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG, e);
            }
        }
    }

    public static boolean isCaiYunFileDown(MessageItem messageItem){
        ChatMessage msg = null;
        boolean isFileDownload = false;
        CloudFileMessage cMessage = null;
        McloudFileApi api = null;
        
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(messageItem.mRcsId));
            cMessage = msg.getCloudFileMessage();
            api = RcsApiManager.getMcloudFileApi();
            if (msg != null)
                isFileDownload = RcsChatMessageUtils.isFileDownload(api.getLocalRootPath()
                                    + cMessage.getFileName(), cMessage.getFileSize());
        } catch (Exception e) {
            Log.w("RCS_UI",e);
        }
        return isFileDownload;
    }
}
