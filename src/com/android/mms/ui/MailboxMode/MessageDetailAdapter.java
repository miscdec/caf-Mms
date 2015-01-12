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

package com.android.mms.ui;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
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

import com.android.mms.R;
import com.android.mms.RcsApiManager;
import com.android.mms.rcs.GeoLocation;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.ui.MessageUtils;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;

public class MessageDetailAdapter extends PagerAdapter {

    private Context mContext;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private float mBodyFontSize;
    private ArrayList<TextView> mScaleTextList;
    private String mContentType = "";
    private int mMsgType = -1;

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

        TextView bodyText = (TextView) content.findViewById(R.id.textViewBody);
        LinearLayout mLinearLayout = (LinearLayout)content.findViewById(R.id.other_type_layout);

        mMsgType = mCursor.getInt(mCursor.getColumnIndex("rcs_msg_type"));
        if (mMsgType == RcsUtils.RCS_MSG_TYPE_TEXT) {
            initTextMsgView(bodyText);
        } else {
            bodyText.setVisibility(View.GONE);
            mLinearLayout.setVisibility(View.VISIBLE);
            ImageView imageView = (ImageView)mLinearLayout.findViewById(R.id.image_view);
            TextView textView = (TextView)mLinearLayout.findViewById(R.id.type_text_view);
            imageView.setOnClickListener(mOnClickListener);
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
                        .getColumnIndexOrThrow("rcs_thumb_path"));
                Bitmap bitmap = BitmapFactory.decodeFile(thumbPath);
                imageView.setImageBitmap(bitmap);
                showContentFileSize(textView);
                mContentType = "video/*";
            } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_MAP) {
                imageView.setImageResource(R.drawable.rcs_map);
                String body = mCursor.getString(mCursor.getColumnIndexOrThrow(Sms.BODY));
                textView.setText(body.substring(body.lastIndexOf("/") + 1, body.length()));
                mContentType = "map/*";
            } else if (mMsgType == RcsUtils.RCS_MSG_TYPE_VCARD) {
                textView.setVisibility(View.GONE);
                imageView.setImageResource(R.drawable.ic_attach_vcard);
                mContentType = "text/x-vCard";
            } else {
                bodyText.setVisibility(View.VISIBLE);
                mLinearLayout.setVisibility(View.GONE);
                initTextMsgView(bodyText);
            }
        }

        TextView detailsText = (TextView) content.findViewById(R.id.textViewDetails);
        detailsText.setText(MessageUtils.getTextMessageDetails(mContext, mCursor, true));
        view.addView(content);

        return content;
    }
    private void showContentFileSize(TextView textView){
        long fileSize = mCursor.getLong(mCursor.getColumnIndex("rcs_file_size"));
        if(fileSize > 1024){
            textView.setText(fileSize / 1024 + " KB");
        }else{
            textView.setText(fileSize + " B");
        }
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
        String thumbPath = mCursor.getString(mCursor.getColumnIndexOrThrow("rcs_thumb_path"));
        String filePath = mCursor.getString(mCursor.getColumnIndexOrThrow("rcs_path"));
        ImageView imageView = (ImageView)linearLayout.findViewById(R.id.image_view);
        Bitmap bitmap = null;
        if(!TextUtils.isEmpty(thumbPath))
            bitmap = RcsUtils.createBitmap_Compress(thumbPath);
        else if (!TextUtils.isEmpty(filePath))
            bitmap = RcsUtils.createBitmap_Compress(filePath);
        if(bitmap != null){
            imageView.setBackground(RcsUtils.createDrawable(mContext, bitmap));
        }else{
            imageView.setBackgroundResource(R.drawable.ic_attach_picture_holo_light);
        }
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String rcsPath = mCursor.getString(mCursor.getColumnIndexOrThrow("rcs_path"));
            if(TextUtils.isEmpty(rcsPath)){
                if(mMsgType == RcsUtils.RCS_MSG_TYPE_IMAGE){
                    Toast.makeText(mContext, R.string.not_download_image, Toast.LENGTH_SHORT)
                    .show();
                }else if(mMsgType == RcsUtils.RCS_MSG_TYPE_VIDEO){
                    Toast.makeText(mContext, R.string.not_download_video, Toast.LENGTH_SHORT)
                    .show();
                }else{
                    Toast.makeText(mContext, R.string.file_path_null, Toast.LENGTH_SHORT)
                    .show();
                }
                return;
            }
            int rcsId = mCursor.getInt(mCursor.getColumnIndexOrThrow("rcs_id"));
            String filepath = RcsUtils.getFilePath(rcsId, rcsPath);
            String rcsMimeType = mCursor.getString(mCursor.getColumnIndexOrThrow("rcs_mime_type"));

            File file = new File(filepath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mContentType.toLowerCase());
            if (rcsMimeType != null && rcsMimeType.endsWith("image/gif")) {
                intent.setAction("com.android.gallery3d.VIEW_GIF");
            }
            switch (mMsgType) {
                case RcsUtils.RCS_MSG_TYPE_AUDIO:
                    try {
                        intent.setDataAndType(Uri.parse("file://" + rcsPath), "audio/*");
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
                    intent.putExtra("VIEW_VCARD_FROM_MMS", true);
                    mContext.startActivity(intent);
                    break;
                case RcsUtils.RCS_MSG_TYPE_MAP:
                    Intent intent_map = new Intent();
                    GeoLocation geo = RcsUtils.readMapXml(rcsPath);
                    String geourl = "geo:" + geo.getLat() + "," + geo.getLng();
                    try {
                        Uri uri = Uri.parse(geourl);
                        Intent it = new Intent(Intent.ACTION_VIEW, uri);
                        mContext.startActivity(it);
                    } catch (Exception e) {
                        Toast.makeText(mContext, R.string.toast_install_map, Toast.LENGTH_SHORT)
                                .show();
                    }

                    break;
                default:
                    break;
            }
        }
    };
}
