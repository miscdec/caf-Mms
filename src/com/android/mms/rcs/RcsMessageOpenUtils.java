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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.ui.MessageItem;
import com.android.mms.ui.MessageListItem;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.exception.VCardException;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import com.suntek.rcs.ui.common.mms.GeoLocation;
import com.suntek.rcs.ui.common.PropertyNode;
import com.suntek.rcs.ui.common.RcsLog;
import com.suntek.rcs.ui.common.VNode;
import com.suntek.rcs.ui.common.VNodeBuilder;
import com.suntek.mway.rcs.client.aidl.plugin.callback.ICloudOperationCtrl;
import com.suntek.mway.rcs.client.aidl.plugin.entity.cloudfile.TransNode;
import com.suntek.mway.rcs.client.aidl.plugin.entity.cloudfile.CloudFileMessage;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonConstant;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.cloudfile.CloudFileApi;
import com.suntek.mway.rcs.client.api.basic.BasicApi;
import com.suntek.mway.rcs.client.api.emoticon.EmoticonApi;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;

public class RcsMessageOpenUtils {
    private static final int VIEW_VCARD_DETAIL = 0;
    private static final int IMPORT_VCARD = 1;
    private static final int MERGE_VCARD_CONTACTS = 2;
    private static final String ACTION_GROUP_VCARD =
            "com.suntek.mway.rcs.nativeui.ACTION_GROUP_VCARD_DETAIL";

    public static void openRcsSlideShowMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        if (messageItem.getRcsMsgState() == RcsUtils.MESSAGE_FAIL
                && messageItem.getRcsMsgType() != RcsUtils.RCS_MSG_TYPE_TEXT) {
            retransmisMessage(messageItem);
            return;
        }
        MessageApi messageApi = MessageApi.getInstance();
        String filepath = messageItem.getRcsPath();
        File File = new File(filepath);
        OpenRcsMessageIntent intent = new OpenRcsMessageIntent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(File),
                messageListItem.getRcsContentType().toLowerCase());
        if (!messageItem.isMe() && !RcsUtils.isFileDownLoadoK(messageItem)) {
            try {
                if (RcsUtils.isRcsOnline()) {
                    if (messageItem.getMsgDownlaodState() == RcsUtils.RCS_IS_DOWNLOADING) {
                        RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                                messageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOAD_PAUSE);
                        messageApi.pauseDownload(messageItem.getMessageId());
                    } else {
                        RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                                messageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOADING);
                        messageApi.download(messageItem.getMessageId());
                    }
                } else {
                    Toast.makeText(messageListItem.getContext(), R.string.rcs_network_unavailable,
                        Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                RcsLog.e(e);
            }
        } else if (messageItem.isMe() || RcsUtils.isFileDownLoadoK(messageItem)){
            RcsUtils.startSafeActivity(messageListItem.getContext(), intent);
        }
    }

    public static void retransmisMessage(MessageItem messageItem) {
        try {
            MessageApi.getInstance().resend(messageItem.getMessageId());
        } catch (ServiceDisconnectedException e) {
            RcsLog.e(e);
        } catch (RemoteException e) {
            RcsLog.e(e);
        }
    }

    public static void setRcsImageViewClickListener(
            ImageView imageView, final MessageListItem messageListItem){
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendOrOpenRcsMessage(messageListItem);
            }
        });
    }

    private static void resendOrOpenRcsMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        if (messageItem.getRcsMsgState() == RcsUtils.MESSAGE_FAIL
                && messageItem.getRcsMsgType() != RcsUtils.RCS_MSG_TYPE_TEXT) {
            retransmisMessage(messageItem);
        } else {
            openRcsMessage(messageListItem);
        }
    }

    private static void openRcsMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        switch (messageItem.getRcsMsgType()) {
            case RcsUtils.RCS_MSG_TYPE_AUDIO:
                openRcsAudioMessage(messageListItem);
                break;
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
                openRcsCaiYunFile(messageListItem);
                break;
            default:
                break;
        }
    }

    private static void openRcsCaiYunFile(MessageListItem messageListItem) {
        MessageItem mMessageItem = messageListItem.getMessageItem();
        boolean isFileDownloadOk = false;
        CloudFileMessage cMessage = null;
        CloudFileApi api = null;
        ICloudOperationCtrl operation = null;
        TransNode.TransOper transOper = TransNode.TransOper.NEW;
        String filePath = "";
        try {
            cMessage = MessageApi.parseCloudFileMessage(mMessageItem.getMsgBody());
            api = CloudFileApi.getInstance();
            filePath = api.getLocalRootPath() + cMessage.getFileName();
            if (cMessage != null) {
                isFileDownloadOk = RcsUtils.isCloudFileDownLoadOk(cMessage);
            }
            if (RcsUtils.isFileDownBeginButNotEnd(filePath, cMessage.getFileSize())) {
                transOper = TransNode.TransOper.RESUME;
            }
            if (RcsUtils.isRcsOnline()) {
                if (!isFileDownloadOk && mMessageItem.getMsgDownlaodState() ==
                        RcsUtils.RCS_IS_DOWNLOADING) {
                    RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                            mMessageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOAD_PAUSE);
                    operation.pause();
                    messageListItem.setDateViewText(R.string.stop_down_load);
                } else if (!isFileDownloadOk) {
                    RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                            mMessageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOADING);
                    operation = api.downloadFileFromUrl(cMessage.getShareUrl(),
                            cMessage.getFileName(), transOper, mMessageItem.getMessageId());
                    messageListItem.setDateViewText(R.string.rcs_downloading);
                }
            } else {
                Toast.makeText(messageListItem.getContext(), R.string.rcs_network_unavailable,
                        Toast.LENGTH_SHORT).show();
            }
            if (isFileDownloadOk) {
                String path = api.getLocalRootPath() + cMessage.getFileName();
                Intent intent2 = RcsUtils.OpenFile(path);
                messageListItem.getContext().startActivity(intent2);
            }
        } catch (Exception e) {
            if(e instanceof ActivityNotFoundException){
                Toast.makeText(messageListItem.getContext(), R.string.please_install_application,
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    private static void openRcsEmojiMessage(MessageListItem messageListItem){
        MessageItem messageItem = messageListItem.getMessageItem();
        String[] body = messageItem.getRcsPath().split(",");
        EmoticonApi emotionApi = EmoticonApi.getInstance();
        byte[] data = null;
        try {
            if (messageItem.getMsgDownlaodState() == RcsUtils.RCS_IS_DOWNLOAD_FAIL) {
                emotionApi.downloadEmoticon(body[0], messageItem.getMessageId());
                return;
            }
            data = emotionApi.decrypt2Bytes(body[0], EmoticonConstant.EMO_DYNAMIC_FILE);
        } catch (ServiceDisconnectedException e) {
            RcsLog.e(e);
            return;
        } catch (RemoteException e) {
            RcsLog.e(e);
            return;
        }
        if(data == null || data.length <= 0){
            return;
        }
        Context context = messageListItem.getContext();
        View view = messageListItem.getImageView();
        com.suntek.rcs.ui.common.utils.RcsUtils.openPopupWindow(context, view, data,
                R.drawable.rcs_emoji_popup_bg);
    }

    private static void openRcsAudioMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        String rcsContentType = messageListItem.getRcsContentType();
        String filePath = messageItem.getRcsPath();
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(messageListItem.getContext(), R.string.file_not_exist,
                    Toast.LENGTH_LONG).show();
            return;
        }
        OpenRcsMessageIntent intent = new OpenRcsMessageIntent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), rcsContentType.toLowerCase());
        RcsUtils.startSafeActivity(messageListItem.getContext(), intent);
    }

    private static void openRcsImageMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();

        String filePath = messageItem.getRcsPath();
        File file = new File(filePath);
        OpenRcsMessageIntent intent = new OpenRcsMessageIntent(Intent.ACTION_VIEW);
        String rcsMimeType = messageItem.getRcsMimeType();
        byte[] gifData = null;
        if (rcsMimeType != null && rcsMimeType.endsWith(RcsUtils.RCS_MSG_IMAGE_TYPE_BMP)) {
            intent.setDataAndType(Uri.fromFile(file), RcsUtils.RCS_MSG_IMAGE_TYPE_BMP);
        } else if (rcsMimeType != null && rcsMimeType.endsWith(RcsUtils.RCS_MSG_IMAGE_TYPE_GIF)) {
            gifData = RcsUtils.getBytesFromFile(file);
        } else {
            intent.setDataAndType(Uri.fromFile(file), RcsUtils.RCS_MSG_IMAGE_TYPE_ALL);
        }
        boolean isFileDownload = false;
        if (messageItem != null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath,
                    messageItem.getRcsMsgFileSize());
        if (!messageItem.isMe() && !isFileDownload) {
            try {
                if (RcsUtils.isRcsOnline()) {
                    MessageApi messageApi = MessageApi.getInstance();
                    if (messageItem.getMsgDownlaodState() == RcsUtils.RCS_IS_DOWNLOADING) {
                        RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                                messageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOAD_PAUSE);
                        messageApi.pauseDownload(messageItem.getMessageId());
                    } else {
                        RcsUtils.updateFileDownloadState(messageListItem.getContext(),
                                messageItem.getMessageId(), RcsUtils.RCS_IS_DOWNLOADING);
                        messageApi.download(messageItem.getMessageId());
                    }
                } else {
                    Toast.makeText(messageListItem.getContext(), R.string.rcs_network_unavailable,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                RcsLog.e(e);
            }
            return;
        }
        if (messageItem.isMe() || isFileDownload) {
            if (rcsMimeType != null && rcsMimeType.endsWith(RcsUtils.RCS_MSG_IMAGE_TYPE_GIF)) {
                Context context = messageListItem.getContext();
                View view = messageListItem.getImageView();
                com.suntek.rcs.ui.common.utils.RcsUtils.openPopupWindow(context, view, gifData,
                        R.drawable.rcs_emoji_popup_bg);
                return;
            } else {
                RcsUtils.startSafeActivity(messageListItem.getContext(), intent);
            }
        }
    }

    private static void openRcsVCardMessage(MessageListItem messageListItem) {
            Context context = messageListItem.getContext();
            showOpenRcsVcardDialog(context,messageListItem);
    }

    private static void showOpenRcsVcardDialog(final Context context,final MessageListItem messageListItem){
        final String[] openVcardItems = new String[] {
                context.getString(R.string.vcard_detail_info),
                context.getString(R.string.vcard_import),
                context.getString(R.string.merge_contacts)
        };
        final MessageItem messageItem = messageListItem.getMessageItem();
        AlertDialog.Builder builder = new AlertDialog.Builder(messageListItem.getContext());
        builder.setItems(openVcardItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case VIEW_VCARD_DETAIL:
                        String vcardFilePath = messageItem.getRcsPath();
                        List<VNode> vnodeList = rcsVcardContactList(context, vcardFilePath);
                        if (vnodeList != null) {
                            if (vnodeList.size() == 1) {
                                ArrayList<PropertyNode> propList =
                                        openRcsVcardDetail(context,vcardFilePath);
                                showDetailVcard(context, propList);
                            } else {
                                Intent intent = new Intent(ACTION_GROUP_VCARD);
                                intent.putExtra("vcardFilePath", vcardFilePath);
                                context.startActivity(intent);
                            }
                        }
                        break;
                    case IMPORT_VCARD:
                        try {
                          String filePath = messageItem.getRcsPath();
                          File file = new File(filePath);
                          Intent intent = new Intent(Intent.ACTION_VIEW);
                          intent.setDataAndType(Uri.fromFile(file),
                                  messageListItem.getRcsContentType().toLowerCase());
                          intent.putExtra("VIEW_VCARD_FROM_MMS", true);
                          messageListItem.getContext().startActivity(intent);
                      } catch (Exception e) {
                          RcsLog.e(e);
                      }
                        break;
                    case MERGE_VCARD_CONTACTS:
                        String mergeVcardFilePath = messageItem.getRcsPath();
                        ArrayList<PropertyNode> mergePropList
                                = openRcsVcardDetail(context,mergeVcardFilePath);
                        mergeVcardDetail(context, mergePropList);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.create().show();
    }

    private static void mergeVcardDetail(Context context,
            ArrayList<PropertyNode> propList) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        ArrayList<ContentValues> phoneValue = new ArrayList<ContentValues>();
        for (PropertyNode propertyNode : propList) {
            if ("FN".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    intent.putExtra(ContactsContract.Intents.Insert.NAME,
                            propertyNode.propValue);
                }
            } else if ("TEL".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    ContentValues value = new ContentValues();
                    value.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                    value.put(Phone.TYPE, RcsUtils.getVcardNumberType(propertyNode));
                    value.put(Phone.NUMBER, propertyNode.propValue);
                    phoneValue.add(value);
                }
            } else if ("ADR".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL,
                            propertyNode.propValue);
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE,
                            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
                }
            } else if ("ORG".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    intent.putExtra(ContactsContract.Intents.Insert.COMPANY,
                            propertyNode.propValue);
                }
            } else if ("TITLE".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                        intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE,
                                propertyNode.propValue);
                }
            }
        }
        if (phoneValue.size() > 0) {
            intent.putParcelableArrayListExtra(Insert.DATA, phoneValue);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        RcsUtils.startSafeActivity(context, intent);
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
            RcsLog.e(e);
            return null;
        }
    }

    public static List<VNode> rcsVcardContactList(Context context,String filePath) {
        if (TextUtils.isEmpty(filePath)) {
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
            return vNodeList;
        } catch (Exception e) {
            RcsLog.e(e);
            return null;
        }
    }

    public static void showDetailVcard(Context context,
            ArrayList<PropertyNode> propList) {
        AlertDialog.Builder builder = new Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View vcardView = inflater.inflate(R.layout.rcs_vcard_detail, null);

        ImageView photoView = (ImageView) vcardView
                .findViewById(R.id.vcard_photo);
        TextView nameView, priNumber, addrText, comName, positionText;
        nameView = (TextView) vcardView.findViewById(R.id.vcard_name);
        priNumber = (TextView) vcardView.findViewById(R.id.vcard_number);
        addrText = (TextView) vcardView.findViewById(R.id.vcard_addre);
        positionText = (TextView) vcardView.findViewById(R.id.vcard_position);
        comName = (TextView) vcardView.findViewById(R.id.vcard_com_name);

        ArrayList<String> numberList = new ArrayList<String>();
        for (PropertyNode propertyNode : propList) {
            if ("FN".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    nameView.setText(context.getString(R.string.vcard_name)
                            + propertyNode.propValue);
                }
            } else if ("TEL".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    String numberTypeStr =
                            RcsUtils.getPhoneNumberTypeStr(context, propertyNode);
                    if(!TextUtils.isEmpty(numberTypeStr)){
                        numberList.add(numberTypeStr);
                    }
                }
            } else if ("ADR".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    String address = propertyNode.propValue;
                    address = address.replaceAll(";", "");
                    addrText.setText(context
                            .getString(R.string.vcard_compony_addre)
                            + ":"
                            + address);
                }
            } else if ("ORG".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    comName.setText(context
                            .getString(R.string.vcard_compony_name)
                            + ":"
                            + propertyNode.propValue);
                }
            } else if ("TITLE".equals(propertyNode.propName)) {
                if (!TextUtils.isEmpty(propertyNode.propValue)) {
                    positionText.setText(context
                            .getString(R.string.vcard_compony_position)
                            + ":"
                            + propertyNode.propValue);
                }
            } else if ("PHOTO".equals(propertyNode.propName)) {
                if (propertyNode.propValue_bytes != null) {
                    byte[] bytes = propertyNode.propValue_bytes;
                    final Bitmap vcardBitmap = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.length);
                    photoView.setImageBitmap(vcardBitmap);
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

    private static void openRcsLocationMessage(MessageListItem messageListItem) {
        MessageItem messageItem = messageListItem.getMessageItem();
        String filePath = messageItem.getRcsPath();
        String messageItemBody = messageItem.getMsgBody();
        try {
            GeoLocation geo = RcsUtils.readMapXml(filePath);
            String geourl = "geo:" + geo.getLat() + "," + geo.getLng()+ "?q=" + geo.getLabel();
            OpenRcsMessageIntent intent = new OpenRcsMessageIntent(Intent.ACTION_VIEW,
                    Uri.parse(geourl));
            messageListItem.getContext().startActivity(intent);
        } catch (NullPointerException e) {
            RcsLog.e(e);
        } catch (ActivityNotFoundException ae) {
            Toast.makeText(messageListItem.getContext(),
                    R.string.toast_install_map, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            RcsLog.e(e);
        }
    }
    public static boolean isCaiYunFileDown(MessageItem messageItem) {
        boolean isFileDownload = false;
        CloudFileMessage cMessage = null;
        CloudFileApi api = null;

        try {
            cMessage = MessageApi.getInstance().parseCloudFileMessage(messageItem.getMsgBody());
            api = CloudFileApi.getInstance();
            if (cMessage != null)
                isFileDownload = RcsChatMessageUtils.isFileDownload(api.getLocalRootPath()
                                    + cMessage.getFileName(), cMessage.getFileSize());
        } catch (Exception e) {
            RcsLog.e(e);
        }
        return isFileDownload;
    }

    public static class OpenRcsMessageIntent extends Intent {
        public OpenRcsMessageIntent(String actionView, Uri uri) {
            super(actionView, uri);
        }

        public OpenRcsMessageIntent(String actionView) {
            super(actionView);
        }

        @Override
        public String toString() {
           if (super.toString().length() != super.toString().getBytes().length) {
               RcsLog.i("OpenRcsMessageIntent removed special characters from path");
               return  super.toString().replaceAll("[^\\x00-\\x7F]", "");
           } else {
               return super.toString();
           }
       }
    }

}
