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
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.mms.ui;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.Telephony.Sms;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.rcs.RcsChatMessageUtils;
import com.android.mms.rcs.RcsMessageOpenUtils;
import com.android.mms.rcs.RcsUtils;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.mms.ui.MessageItem;
import com.android.mms.ui.MessageListItem;
import com.android.mms.ui.MessageUtils;

import com.suntek.rcs.ui.common.mms.GeoLocation;
import com.suntek.rcs.ui.common.PropertyNode;
import com.suntek.rcs.ui.common.RcsEmojiStoreUtil;
import com.suntek.rcs.ui.common.VNode;
import com.suntek.rcs.ui.common.VNodeBuilder;

import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.rcs.ui.common.RcsLog;

public class MessageDetailAdapter extends PagerAdapter {


    private Context mContext;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private float mBodyFontSize;
    private ArrayList<TextView> mScaleTextList;

    /* Begin add for RCS */
    private String RCS_TAG = "RCS_UI";
    private String mContentType = "";
    private int mMsgType = -1;
    public static final int SHOW_DETAIL_VCARD          = 0;
    public static final int VIEW_VCARD_FROM_MMS        = 1;
    public static final int MERGE_VCARD_DETAIL         = 2;
    /* End add for RCS */

    public MessageDetailAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
        mInflater = LayoutInflater.from(context);
        mBodyFontSize = MessageUtils.getTextFontSize(context);
    }

    @Override
    public Object instantiateItem(ViewGroup view, int position) {
        mCursor.moveToPosition(position);
        View content = mInflater.inflate(R.layout.message_detail_content, view, false);

        final TextView bodyText = (TextView) content.findViewById(R.id.textViewBody);
        if (MmsConfig.isRcsVersion()) {
            LinearLayout mLinearLayout = (LinearLayout)content.findViewById(R.id.other_type_layout);

            mMsgType = mCursor.getInt(mCursor.getColumnIndex(RcsColumns.SmsRcsColumns.RCS_MSG_TYPE));
            if (mMsgType == RcsUtils.RCS_MSG_TYPE_TEXT) {
                initTextMsgView(bodyText);
            } else {
                bodyText.setVisibility(View.GONE);
                mLinearLayout.setVisibility(View.VISIBLE);
                ImageView imageView = (ImageView)mLinearLayout.findViewById(R.id.image_view);
                TextView textView = (TextView)mLinearLayout.findViewById(R.id.type_text_view);
                if (mMsgType != RcsUtils.RCS_MSG_TYPE_CAIYUNFILE) {
                    imageView.setOnClickListener(mOnClickListener);
                }
                if (mMsgType == RcsUtils.RCS_MSG_TYPE_IMAGE) {
                    initImageMsgView(mLinearLayout);
                    showContentFileSize(textView);
                    mContentType = "image/*";
                } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_AUDIO) {
                    imageView.setImageResource(R.drawable.rcs_voice);
                    showContentFileSize(textView);
                    mContentType = "audio/*";
                } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_VIDEO) {
                    String thumbPath = mCursor.getString(mCursor
                            .getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_THUMB_PATH));
                    Bitmap bitmap = BitmapFactory.decodeFile(thumbPath);
                    imageView.setImageBitmap(bitmap);
                    showContentFileSize(textView);
                    mContentType = "video/*";
                } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_MAP) {
                    imageView.setImageResource(R.drawable.rcs_map);
                    String body = mCursor.getString(mCursor.getColumnIndexOrThrow(Sms.BODY));
                    textView.setText(body);
                    mContentType = "map/*";
                } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_VCARD) {
                    textView.setVisibility(View.GONE);
                    initVcardMagView(mLinearLayout);
                    mContentType = "text/x-vCard";
                } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_PAID_EMO) {
                    String messageBody = mCursor.getString(mCursor.getColumnIndex(
                            RcsColumns.SmsRcsColumns.RCS_FILENAME));
                    String[] body = messageBody.split(",");
                    RcsEmojiStoreUtil.getInstance().loadImageAsynById(imageView, body[0],
                            RcsEmojiStoreUtil.EMO_STATIC_FILE);
                }  else {
                    bodyText.setVisibility(View.VISIBLE);
                    mLinearLayout.setVisibility(View.GONE);
                    initTextMsgView(bodyText);
                }
            }
        } else {
            bodyText.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(Sms.BODY)));
            bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBodyFontSize);
            bodyText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            bodyText.setTextIsSelectable(true);
            bodyText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MessageUtils.onMessageContentClick(mContext, bodyText);
                }
            });
        }

        TextView detailsText = (TextView) content.findViewById(R.id.textViewDetails);
        if (MmsConfig.isRcsVersion()) {
            detailsText.setText(MessageUtils.getTextMessageDetails(mContext, mCursor, true));
        } else {
            detailsText.setText(MessageUtils.getTextMessageDetails(mContext, mCursor, false));
        }
        view.addView(content);

        return content;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void setPrimaryItem(View container, int position, Object object) {
        TextView currentBody = (TextView) container.findViewById(R.id.textViewBody);
        if (mScaleTextList.size() > 0) {
            mScaleTextList.clear();
        }
        mScaleTextList.add(currentBody);
    }

    public void setBodyFontSize(float currentFontSize) {
        mBodyFontSize = currentFontSize;
    }

    public void setScaleTextList(ArrayList<TextView> scaleTextList) {
        mScaleTextList = scaleTextList;
    }

    /* Begin add for RCS */
    private void initTextMsgView(final TextView bodyText){
        bodyText.setText(mCursor.getString(mCursor.getColumnIndexOrThrow(Sms.BODY)));
        bodyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBodyFontSize);
        bodyText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        bodyText.setTextIsSelectable(true);
        bodyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageUtils.onMessageContentClick(mContext, bodyText);
            }
        });
    }

    private void initImageMsgView(LinearLayout linearLayout) {
        String thumbPath = mCursor.getString(
                mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_THUMB_PATH));
        String fileName = mCursor.getString(
                mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_FILENAME));
        thumbPath = RcsUtils.formatFilePathIfExisted(thumbPath);
        fileName = RcsUtils.formatFilePathIfExisted(fileName);
        ImageView imageView = (ImageView)linearLayout.findViewById(R.id.image_view);
        Bitmap thumbPathBitmap = null;
        Bitmap filePathBitmap = null;
        if(!TextUtils.isEmpty(thumbPath)) {
            thumbPathBitmap = RcsUtils.decodeInSampleSizeBitmap(thumbPath);
        } else if (!TextUtils.isEmpty(fileName)) {
            filePathBitmap = RcsUtils.decodeInSampleSizeBitmap(fileName);
        }
        if (thumbPathBitmap != null) {
            imageView.setBackgroundDrawable(new BitmapDrawable(thumbPathBitmap));
        } else if (filePathBitmap != null) {
            imageView.setBackgroundDrawable(new BitmapDrawable(filePathBitmap));
        } else {
            imageView.setBackgroundResource(R.drawable.ic_attach_picture_holo_light);
        }
    }

    private void initVcardMagView(LinearLayout linearLayout){
        ImageView imageView = (ImageView)linearLayout.findViewById(R.id.image_view);
        String fileName = mCursor.getString(
                mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_FILENAME));
        String vcardFileName = fileName;
        ArrayList<PropertyNode> propList = RcsMessageOpenUtils.openRcsVcardDetail(
                mContext, vcardFileName);
        Bitmap bitmap = null;
        for (PropertyNode propertyNode : propList) {
            if ("PHOTO".equals(propertyNode.propName)) {
                if(propertyNode.propValue_bytes != null){
                    byte[] bytes = propertyNode.propValue_bytes;
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    bitmap = RcsUtils.decodeInSampleSizeBitmap(bitmap);
                    break;
                }
            }
        }
        if (bitmap != null) {
            imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
        } else {
            imageView.setBackgroundResource(R.drawable.ic_attach_vcard);
        }
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String fileName = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_FILENAME));
            long filesize = mCursor.getInt(
                    mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_FILE_SIZE));
            fileName = RcsUtils.formatFilePathIfExisted(fileName);
            boolean isFileDownload = RcsChatMessageUtils.isFileDownload(fileName, filesize);
            if (mMsgType == RcsUtils.RCS_MSG_TYPE_VIDEO ||
                    mMsgType == RcsUtils.RCS_MSG_TYPE_IMAGE) {
                if (!isFileDownload) {
                    if (mMsgType == RcsUtils.RCS_MSG_TYPE_IMAGE) {
                        Toast.makeText(mContext, R.string.not_download_image, Toast.LENGTH_SHORT)
                        .show();
                    } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_VIDEO) {
                        Toast.makeText(mContext, R.string.not_download_video, Toast.LENGTH_SHORT)
                        .show();
                    } else {
                        Toast.makeText(mContext, R.string.file_path_null, Toast.LENGTH_SHORT)
                        .show();
                    }
                    return;
                }
            }
            String rcsMimeType = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_MIME_TYPE));
            File file = new File(fileName);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mContentType.toLowerCase());
            if (rcsMimeType != null && rcsMimeType.endsWith("image/gif")) {
                intent.setAction("com.android.gallery3d.VIEW_GIF");
            }
            switch (mMsgType) {
                case RcsUtils.RCS_MSG_TYPE_AUDIO:
                    try {
                        mContext.startActivity(intent);
                    } catch (Exception e) {
                    }
                    break;
                case RcsUtils.RCS_MSG_TYPE_VIDEO:
                case RcsUtils.RCS_MSG_TYPE_IMAGE:
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra("SingleItemOnly", true);
                    mContext.startActivity(intent);
                    break;
                case RcsUtils.RCS_MSG_TYPE_VCARD:
                    showOpenRcsVcardDialog();
                    break;
                case RcsUtils.RCS_MSG_TYPE_MAP:
                    openMapMessage(fileName);
                    break;
                default:
                    break;
            }
        }
    };

    private void openMapMessage(String path){
        try {
            Intent intent_map = new Intent();
            GeoLocation geo = RcsUtils.readMapXml(path);
            String geourl = "geo:" + geo.getLat() + "," + geo.getLng() +
                    "?q=" + geo.getLabel();
            Uri uri = Uri.parse(geourl);
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            mContext.startActivity(it);
        } catch (NullPointerException e) {
            Log.w(RCS_TAG, e);
        } catch (ActivityNotFoundException ae) {
            Toast.makeText(mContext, R.string.toast_install_map,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(RCS_TAG, e);
        }
    }

    private void showOpenRcsVcardDialog(){
        final String vcardFileName = mCursor.getString(
                mCursor.getColumnIndexOrThrow(RcsColumns.SmsRcsColumns.RCS_FILENAME));
        final String[] openVcardItems = new String[] {
                mContext.getString(R.string.vcard_detail_info),
                mContext.getString(R.string.vcard_import),
                mContext.getString(R.string.merge_contacts)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setItems(openVcardItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case SHOW_DETAIL_VCARD:
                        ArrayList<PropertyNode> propList = RcsMessageOpenUtils.
                                openRcsVcardDetail(mContext, vcardFileName);
                        RcsMessageOpenUtils.showDetailVcard(mContext, propList);
                        break;
                    case VIEW_VCARD_FROM_MMS:
                        try {
                          File file = new File(vcardFileName);
                          Intent intent = new Intent(Intent.ACTION_VIEW);
                          intent.setDataAndType(Uri.fromFile(file), mContentType
                                  .toLowerCase());
                          intent.putExtra("VIEW_VCARD_FROM_MMS", true);
                          mContext.startActivity(intent);
                      } catch (Exception e) {
                          Log.w(RCS_TAG, e);
                      }
                        break;
                    case MERGE_VCARD_DETAIL:
                        ArrayList<PropertyNode> mergePropList
                                = openRcsVcardDetail(mContext, vcardFileName);
                        mergeVcardDetail(mContext, mergePropList);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.create().show();
    }

    public static ArrayList<PropertyNode> openRcsVcardDetail(Context context, String fileName){
        if (TextUtils.isEmpty(fileName)){
            return null;
        }
        try {
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            VNodeBuilder builder = new VNodeBuilder();
            VCardParser parser = new VCardParser_V21();
            parser.addInterpreter(builder);
            parser.parse(fis);
            List<VNode> vNodeList = builder.getVNodeList();
            ArrayList<PropertyNode> propList = vNodeList.get(0).propList;
            return propList;
        } catch (Exception e) {
            RcsLog.w(e);
            return null;
        }
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
        context.startActivity(intent);
    }

    private void showContentFileSize(TextView textView) {
        long fileSize = mCursor.getLong(mCursor
                .getColumnIndex(RcsColumns.SmsRcsColumns.RCS_FILE_SIZE));
        if (fileSize > MmsConfig.KB_IN_BYTES) {
            textView.setText(fileSize / MmsConfig.KB_IN_BYTES + " KB");
        } else {
            textView.setText(fileSize + " B");
        }
    }
    /* End add for RCS */
}
