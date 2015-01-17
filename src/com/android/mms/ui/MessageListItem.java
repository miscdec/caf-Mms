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

package com.android.mms.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.provider.ContactsContract.Profile;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.LayoutModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.WwwContextMenuActivity;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.ItemLoadedCallback;
import com.android.mms.util.MultiSimUtility;
import com.android.mms.util.ThumbnailManager.ImageLoaded;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonConstant;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.android.mms.rcs.RcsApiManager;
import com.android.mms.rcs.GroupMemberPhotoCache;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import com.android.mms.rcs.GeoLocation;
import com.android.mms.rcs.RcsContactsUtils;
import com.android.mms.rcs.RcsEmojiStoreUtil;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.rcs.RcsChatMessageUtils;
import android.os.Environment;
import android.widget.Toast;
/**
 * This class provides view of a message in the messages list.
 */
public class MessageListItem extends LinearLayout implements
        SlideViewInterface, OnClickListener {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final String TAG = "MessageListItem";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_DONT_LOAD_IMAGES = false;
    // The message is from Browser
    private static final String BROWSER_ADDRESS = "Browser Information";
    private static final String CANCEL_URI = "canceluri";
    // transparent background
    private static final int ALPHA_TRANSPARENT = 0;
    //message status
    private static final int MESSAGE_SENDING = 64;
    private static final int MESSAGE_HAS_SENDED = 32;
    private static final int MESSAGE_SENDED = -1;
    private static final int MESSAGE_FAIL = 128;
    private static final int MESSAGE_HAS_BURNED = 2;
    private static final int MESSAGE_SEND_RECEIVE = 99; //delivered
    private static final int MESSAGE_HAS_READ = 100; //displayed
    private static final int MESSAGE_HAS_SEND_SERVER = 0; //send to server

    static final int MSG_LIST_EDIT    = 1;
    static final int MSG_LIST_PLAY    = 2;
    static final int MSG_LIST_DETAILS = 3;

    private boolean mSimMessagesMode = false;
    private boolean mMultiChoiceMode = false;
    private static boolean mRcsIsStopDown = false;

    private View mMmsView;
    private ImageView mImageView;
    private ImageView mLockedIndicator;
    private ImageView mDeliveredIndicator;
    private ImageView mDetailsIndicator;
    private ImageView mSimIndicatorView;
    private ImageButton mSlideShowButton;
    private TextView mSimMessageAddress;
    private TextView mBodyTextView;
    private TextView mBodyButtomTextView;
    private TextView mBodyTopTextView;
    private Button mDownloadButton;
    private View mDownloading;
    private LinearLayout mMmsLayout;
    private CheckBox mChecked;
    private Handler mHandler;
    private MessageItem mMessageItem;
    private String mDefaultCountryIso;
    private TextView mDateView;
    public View mMessageBlock;
    private QuickContactDivot mAvatar;
    static private Drawable sDefaultContactImage;
    private Presenter mPresenter;
    private int mPosition;      // for debugging
    private ImageLoadedCallback mImageLoadedCallback;
    private boolean mMultiRecipients;
    private Contact mContact;
    private Contact mSelfContact;
    private int mManageMode;
    private TextView downloadTextView;
    private TextView mNameView;
    boolean rcs_showMmsView=false;
    private int rcsGroupId;
    String contentType = "";
    private static HashMap<String, Long> sFileTrasnfer = new HashMap<String, Long>();

    public static void setsFileTrasnfer(HashMap<String, Long> sFileTrasnfer) {
        MessageListItem.sFileTrasnfer = sFileTrasnfer;
    }

    public MessageListItem(Context context) {
        super(context);
        mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();

        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }

    public MessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = mContext.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
        mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();

        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBodyTopTextView = (TextView) findViewById(R.id.text_view_top);
        mBodyTopTextView.setVisibility(View.GONE);
        mBodyButtomTextView = (TextView) findViewById(R.id.text_view_buttom);
        mBodyButtomTextView.setVisibility(View.GONE);
        mDateView = (TextView) findViewById(R.id.date_view);
        mLockedIndicator = (ImageView) findViewById(R.id.locked_indicator);
        mDeliveredIndicator = (ImageView) findViewById(R.id.delivered_indicator);
        mDetailsIndicator = (ImageView) findViewById(R.id.details_indicator);
        mAvatar = (QuickContactDivot) findViewById(R.id.avatar);
        mSimIndicatorView = (ImageView) findViewById(R.id.sim_indicator_icon);
        mMessageBlock = findViewById(R.id.message_block);
        mSimMessageAddress = (TextView) findViewById(R.id.sim_message_address);
        mMmsLayout = (LinearLayout) findViewById(R.id.mms_layout_view_parent);
        mChecked = (CheckBox) findViewById(R.id.selected_check);
        downloadTextView = (TextView) findViewById(R.id.label_downloading);
        mNameView = (TextView) findViewById(R.id.name_view);
    }

    // add for setting the background according to whether the item is selected
    public void markAsSelected(boolean selected) {
        if (selected) {
            if (mChecked != null) {
                mChecked.setChecked(selected);
            }
        } else {
            if (mChecked != null) {
                mChecked.setChecked(selected);
            }
        }
    }

    private void updateBodyTextView() {
        if (mMessageItem.isMms() && mMessageItem.mLayoutType == LayoutModel.LAYOUT_TOP_TEXT) {
            mBodyTextView = mBodyTopTextView;
        } else {
            mBodyTextView = mBodyButtomTextView;
        }
        if (mMessageItem.mRcsId == RcsUtils.SMS_DEFAULT_RCS_ID || mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_MAP) {
            mBodyTextView.setVisibility(View.VISIBLE);
        }
    }

    public void bind(MessageItem msgItem, boolean convHasMultiRecipients, int position,
            int rcsGroupId) {
        if (DEBUG) {
            Log.v(TAG, "bind for item: " + position + " old: " +
                   (mMessageItem != null ? mMessageItem.toString() : "NULL" ) +
                    " new " + msgItem.toString());
        }
        boolean sameItem = mMessageItem != null && mMessageItem.mMsgId == msgItem.mMsgId;
        mMessageItem = msgItem;
        if (mMessageItem.isMms() && mMessageItem.mLayoutType == LayoutModel.LAYOUT_TOP_TEXT) {
            mBodyTextView = mBodyTopTextView;
        } else {
            mBodyTextView = mBodyButtomTextView;
        }
        //mBodyTextView.setVisibility(View.VISIBLE);
        updateBodyTextView();
        mPosition = position;
        this.rcsGroupId = rcsGroupId;
        mMultiRecipients = convHasMultiRecipients;

        setLongClickable(false);
        setClickable(false);    // let the list view handle clicks on the item normally. When
                                // clickable is true, clicks bypass the listview and go straight
                                // to this listitem. We always want the listview to handle the
                                // clicks first.
        mContact = Contact.get(mMessageItem.mAddress, false);
        if (mMessageItem.mRcsId != RcsUtils.SMS_DEFAULT_RCS_ID) {
            if (mMessageItem.mRcsChatType == SuntekMessageData.CHAT_TYPE_GROUP
                    && mMessageItem.mRcsType != SuntekMessageData.MSG_TYPE_NOTIFICATION) {
                if (mNameView != null) {
                    mNameView.setText(RcsContactsUtils.getGroupChatMemberDisplayName(getContext(),
                            rcsGroupId, mMessageItem.mAddress));
                    mNameView.setVisibility(View.VISIBLE);
                }
            }

            Bitmap bitmap = null;
            if (mMessageItem.mRcsBurnFlag == RcsUtils.RCS_IS_BURN_TRUE) {
                setLongClickable(true);
                if (mMessageItem.mRcsIsBurn == 0) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rcs_burn_flag);
                } else {
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.rcs_burnmessage_has_burn);
                }
                contentType = "";
                rcs_showMmsView = true;
                showMmsView(true);
                if (mImageView != null) {
                    mImageView.setImageBitmap(bitmap);
                    mImageView.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View arg0) {
                            if (mMessageItem.mRcsMsgState == MESSAGE_FAIL) {
                                try {
                                    RcsApiManager.getMessageApi().retransmitMessageById(
                                            String.valueOf(mMessageItem.mRcsId));
                                } catch (ServiceDisconnectedException e) {
                                    Toast.makeText(getContext(),
                                            R.string.rcs_service_is_not_available,
                                            Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                                return;
                            }
                            if (mMessageItem.mRcsIsBurn == 0) {
                                RcsChatMessageUtils.startBurnMessageActivity(mContext,
                                        mMessageItem.mRcsIsBurn, mMessageItem.getMessageId());
                            } else {
                                Toast.makeText(getContext(),
                                        R.string.message_has_been_burned,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                mBodyTextView.setVisibility(View.GONE);
            } else {
                if (mMessageItem.mRcsType != RcsUtils.RCS_MSG_TYPE_MAP) {
                    mBodyTextView.setVisibility(View.GONE);
                }
                if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_IMAGE) {
                    if (mMessageItem.mRcsThumbPath != null
                            && new File(mMessageItem.mRcsThumbPath).exists()) {
                    } else if(mMessageItem.mRcsThumbPath != null) {
                        if (mMessageItem.mRcsThumbPath.contains(".")) {
                            mMessageItem.mRcsThumbPath = mMessageItem.mRcsThumbPath.substring(0,
                                    mMessageItem.mRcsThumbPath.lastIndexOf("."));
                        }
                    }
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    bitmap = BitmapFactory.decodeFile(mMessageItem.mRcsThumbPath, options);
                    options.inJustDecodeBounds = false;

                    int be = (int)(options.outHeight / (float)200);
                    if (be <= 0)
                        be = 1;
                    options.inSampleSize = be;

                    bitmap = BitmapFactory.decodeFile(mMessageItem.mRcsThumbPath, options);
                    contentType=mMessageItem.mRcsMimeType;
                    if(contentType==null){
                         contentType = "image/*";
                    }
                    contentType = "image/*";
                } else if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_VIDEO) {
                    bitmap = BitmapFactory.decodeFile(mMessageItem.mRcsThumbPath);
                    contentType = "video/*";

                    mMessageItem.mBody = mMessageItem.mRcsFileSize / 1024 + "KB/ "
                            + mMessageItem.mRcsPlayTime + "''";
                    mBodyTextView.setVisibility(View.VISIBLE);
                } else if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_VCARD) {
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_attach_vcard);
                    contentType = "text/x-vCard";
                } else if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_AUDIO) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rcs_voice);
                    contentType = "audio/*";
                    mBodyTextView.setVisibility(View.VISIBLE);
                    mBodyTextView.setText(mMessageItem.mRcsPlayTime + "''");
                    mMessageItem.mBody = mMessageItem.mRcsPlayTime + "''";
                } else if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_MAP) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rcs_map);
                    contentType = "map/*";
                } else if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_PAID_EMO){
                    String[] body = mMessageItem.mBody.split(",");
                    RcsEmojiStoreUtil.getInstance().loadImageAsynById(mImageView, body[0],
                            RcsEmojiStoreUtil.EMO_STATIC_FILE);
                }

                if (mMessageItem.mRcsType != 0) {

                    showMmsView(true);
                    if (mSlideShowButton == null) {
                        mSlideShowButton = (ImageButton) findViewById(R.id.play_slideshow_button);
                    }
                    if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_VIDEO) {
                        if (mSlideShowButton != null) {
                            mSlideShowButton.setVisibility(View.VISIBLE);
                            mSlideShowButton.setFocusable(false);
                        }
                    } else {
                        if (mSlideShowButton != null) {
                            mSlideShowButton.setVisibility(View.GONE);
                        }
                    }
                    if (mSlideShowButton != null) {
                        mSlideShowButton.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                openRcsSlideShowMessage();
                            }
                        });
                    }
                    if (bitmap != null && mMessageItem.mRcsType != RcsUtils.RCS_MSG_TYPE_PAID_EMO) {
                        Matrix matrix = new Matrix();
                        matrix.postScale(1.5f, 1.5f);
                        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                bitmap.getHeight(), matrix, true);
                        if (mImageView != null) {
                            mImageView.setImageBitmap(bitmap);
                        }
                    }
                    if (mImageView != null) {
                        mImageView.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                resendOrOpenRcsMessage();
                            }
                        });
                    }
                    rcs_showMmsView = true;
                } else {
                    mBodyTextView.setVisibility(View.VISIBLE);
                    rcs_showMmsView = false;
                    showMmsView(false);
                }
            }
        }
        switch (msgItem.mMessageType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                bindNotifInd();
                break;
            default:
                mSelfContact = Contact.getMe(false);
                bindCommonMessage(sameItem);
                break;
        }

        customSIMSmsView();
    }

    private void openRcsSlideShowMessage() {
        String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId,
                mMessageItem.mRcsPath);
        File File = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(File), contentType.toLowerCase());
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(mMessageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg!=null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!mMessageItem.isMe() && !isFileDownload) {
            try {
                mDateView.setText(R.string.rcs_downloading);
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi
                        .getMessageById(String.valueOf(mMessageItem.mRcsId));
                if (isDownloading() && !mRcsIsStopDown) {
                    mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                    mDateView.setText(R.string.stop_down_load);
                } else {
                    mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                }
            } catch (Exception e) {
                Log.w("RCS_UI", e);
            }
            return;
        }
        if (isFileDownload) {
            mContext.startActivity(intent);
        }
    }

    private boolean isDownloading() {
        return sFileTrasnfer.containsKey(mMessageItem.mRcsMessageId);
    }

    private void resendOrOpenRcsMessage() {
        if (mMessageItem.mRcsMsgState == MESSAGE_FAIL
                && mMessageItem.mRcsType != RcsUtils.RCS_MSG_TYPE_TEXT) {
            try {
                RcsApiManager.getMessageApi().retransmitMessageById(
                        String.valueOf(mMessageItem.mRcsId));
            } catch (ServiceDisconnectedException e) {
                toast(R.string.rcs_service_is_not_available);
                e.printStackTrace();
            }
        } else {
            openRcsMessage();
        }
    }

    private void openRcsMessage() {
        switch (mMessageItem.mRcsType) {
            case RcsUtils.RCS_MSG_TYPE_AUDIO:
                openRcsAudioMessage();
                break;
            case RcsUtils.RCS_MSG_TYPE_VIDEO:
                openRcsVideoMessage();
            case RcsUtils.RCS_MSG_TYPE_IMAGE:
                openRcsImageMessage();
                break;
            case RcsUtils.RCS_MSG_TYPE_VCARD:
                openRcsVCardMessage();
                break;
            case RcsUtils.RCS_MSG_TYPE_MAP:
                openRcsLocationMessage();
                break;
            case RcsUtils.RCS_MSG_TYPE_PAID_EMO:
                openRcsEmojiMessage();
                break;
            default:
                break;
        }
    }

    private void openRcsEmojiMessage() {
        try {
            String[] body = mMessageItem.mBody.split(",");
            byte[] data = RcsApiManager.getEmoticonApi().decrypt2Bytes(body[0],
                    EmoticonConstant.EMO_DYNAMIC_FILE);
            RcsUtils.openPopupWindow(getContext(), mImageView, data);
        } catch (ServiceDisconnectedException e) {
            e.printStackTrace();
            return;
        }
    }

    private void openRcsAudioMessage() {
        try {
            String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
            File file = new File(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), contentType.toLowerCase());
            intent.setDataAndType(Uri.parse("file://" + mMessageItem.mRcsPath), "audio/*");
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w("RCS_UI", e);
        }
    }

    private void openRcsVideoMessage() {
        String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), contentType.toLowerCase());
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(mMessageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(msg!=null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!mMessageItem.isMe() && !isFileDownload) {
            try {
                mDateView.setText(mContext.getResources().getString(R.string.rcs_downloading));
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi
                        .getMessageById(String.valueOf(mMessageItem.mRcsId));
                if (isDownloading() && !mRcsIsStopDown ) {
                    mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                    mDateView.setText(mContext.getResources().getString(R.string.stop_down_load));
                } else {
                    mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                  }
            } catch (Exception e) {
                Log.w("RCS_UI", e);
            }
            return;
        }
        mContext.startActivity(intent);
    }

    private void openRcsImageMessage() {
        String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
        File file = new File(filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), contentType.toLowerCase());
        if (mMessageItem.mRcsMimeType != null && mMessageItem.mRcsMimeType.endsWith("image/gif")) {
            intent.setAction("com.android.gallery3d.VIEW_GIF");
        }
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(mMessageItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null)
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        if (!mMessageItem.isMe() && !isFileDownload) {
            try {
                mDateView.setText(mContext.getResources().getString(R.string.rcs_downloading));
                MessageApi messageApi = RcsApiManager.getMessageApi();
                ChatMessage message = messageApi
                        .getMessageById(String.valueOf(mMessageItem.mRcsId));
                if (isDownloading() && !mRcsIsStopDown) {
                    mRcsIsStopDown = true;
                    messageApi.interruptFile(message);
                    mDateView.setText(mContext.getResources().getString(R.string.stop_down_load));
                } else {
                    mRcsIsStopDown = false;
                    messageApi.acceptFile(message);
                }
            } catch (Exception e) {
                Log.w("RCS_UI", e);
            }
            return;
        }
        if (mMessageItem.isMe() || isFileDownload) {
            Log.i("RCS_UI", "filePath=" + filePath);
            mContext.startActivity(intent);
        }
    }

    private void openRcsVCardMessage() {
        try {
            String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
            File file = new File(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), contentType.toLowerCase());
            intent.putExtra("VIEW_VCARD_FROM_MMS", true);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.w("RCS_UI", e);
        }
    }

    private void openRcsLocationMessage() {
        Log.i(TAG, "enter openRcsLocationMessage");
        String filePath = RcsUtils.getFilePath(mMessageItem.mRcsId, mMessageItem.mRcsPath);
        Log.i(TAG, "filePath = " + filePath);
        GeoLocation geo = RcsUtils.readMapXml(filePath);
        Log.i(TAG,"geo = "  +geo);
        String geourl = "geo:" + geo.getLat() + "," + geo.getLng();
        Log.i(TAG, "geourl = " + geourl);
        try {
            Uri uri = Uri.parse(geourl);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geourl));
            mContext.startActivity(intent);
        } catch (Exception e) {
            toast(R.string.toast_install_map);
        }
    }

    private void toast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_LONG).show();
    }

    public void unbind() {
        // Clear all references to the message item, which can contain attachments and other
        // memory-intensive objects
        if (mImageView != null) {
            // Because #setOnClickListener may have set the listener to an object that has the
            // message item in its closure.
            mImageView.setOnClickListener(null);
        }
        if (mSlideShowButton != null) {
            // Because #drawPlaybackButton sets the tag to mMessageItem
            mSlideShowButton.setTag(null);
        }
        // leave the presenter in case it's needed when rebound to a different MessageItem.
        if (mPresenter != null) {
            mPresenter.cancelBackgroundLoading();
        }
    }

    public int getItemPosition() {
        return mPosition;
    }

    public MessageItem getMessageItem() {
        return mMessageItem;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private void bindNotifInd() {
        showMmsView(false);

        String msgSizeText = mContext.getString(R.string.message_size_label)
                                + String.valueOf((mMessageItem.mMessageSize + 1023) / 1024)
                                + mContext.getString(R.string.kilobyte);

        mBodyTextView.setText(formatMessage(mMessageItem, null,
                                            mMessageItem.mSubscription,
                                            mMessageItem.mSubject,
                                            mMessageItem.mHighlight,
                                            mMessageItem.mTextContentType));

        mDateView.setText(buildTimestampLine(msgSizeText + " " + mMessageItem.mTimestamp));

        updateSimIndicatorView(mMessageItem.mSubscription);

        if (mManageMode == MessageUtils.BATCH_DELETE_MODE) {
            return;
        }
        switch (mMessageItem.getMmsDownloadStatus()) {
            case DownloadManager.STATE_PRE_DOWNLOADING:
            case DownloadManager.STATE_DOWNLOADING:
                showDownloadingAttachment();
                break;
            case DownloadManager.STATE_UNKNOWN:
            case DownloadManager.STATE_UNSTARTED:
                DownloadManager downloadManager = DownloadManager.getInstance();
                boolean autoDownload = downloadManager.isAuto();
                boolean dataSuspended = (MmsApp.getApplication().getTelephonyManager()
                        .getDataState() == TelephonyManager.DATA_SUSPENDED);
                // We must check if the target data subscription is user prefer
                // data subscription, if we don't check this, here will be
                // a problem, when user want to download a MMS is not in default
                // data subscription, the other MMS will mark as downloading status.
                // But they can't be download, this will make user confuse.
                boolean isTargetDefaultDataSubscription = mMessageItem.mSubscription ==
                        MultiSimUtility.getCurrentDataSubscription(mContext);

                boolean isMobileDataDisabled = MessageUtils.isMobileDataDisabled(mContext);

                // If we're going to automatically start downloading the mms attachment, then
                // don't bother showing the download button for an instant before the actual
                // download begins. Instead, show downloading as taking place.
                if (autoDownload && !dataSuspended && !isMobileDataDisabled
                        && isTargetDefaultDataSubscription) {
                    showDownloadingAttachment();
                    break;
                }
            case DownloadManager.STATE_TRANSIENT_FAILURE:
            case DownloadManager.STATE_PERMANENT_FAILURE:
            default:
                setLongClickable(true);
                inflateDownloadControls();
                mDownloading.setVisibility(View.GONE);
                mDownloadButton.setVisibility(View.VISIBLE);
                mDownloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(
                                    mContext).load(mMessageItem.mMessageUri);
                            Log.d(TAG, "Download notify Uri = " + mMessageItem.mMessageUri);
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setTitle(R.string.download);
                            builder.setCancelable(true);
                            // Show enable mobile data dialog when click downlod button
                            // with mobile data is disabled and config_setup_mms_data is true.
                            // If click ok, turn on data and download MMS.
                            // If not, don't download MMS.
                            boolean enableMmsData = mContext.getResources().getBoolean(
                                    com.android.internal.R.bool.config_setup_mms_data);
                            // Judge notification weather is expired
                            if (nInd.getExpiry() < System.currentTimeMillis() / 1000L) {
                                // builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
                                builder.setMessage(mContext
                                        .getString(R.string.service_message_not_found));
                                builder.show();
                                SqliteWrapper.delete(mContext, mContext.getContentResolver(),
                                        mMessageItem.mMessageUri, null, null);
                                return;
                            }
                            // Judge whether memory is full
                            else if (MessageUtils.isMmsMemoryFull()) {
                                builder.setMessage(mContext.getString(R.string.sms_full_body));
                                builder.show();
                                return;
                            }
                            // Judge whether message size is too large
                            else if ((int) nInd.getMessageSize() >
                                      MmsConfig.getMaxMessageSize()) {
                                builder.setMessage(mContext.getString(R.string.mms_too_large));
                                builder.show();
                                return;
                            }
                            // Judge whether mobile data is turned off and
                            // enableMmsData is true.
                            else if (MessageUtils.isMobileDataDisabled(mContext) && enableMmsData) {
                                builder.setMessage(mContext.getString(
                                        R.string.mobile_data_disable,
                                        mContext.getString(R.string.mobile_data_download)));
                                builder.setPositiveButton(R.string.yes,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                                startDownloadAttachment();
                                            }
                                        });
                                builder.setNegativeButton(R.string.no, null);
                                builder.show();
                                return;
                            }
                            // If mobile data is turned off, inform user start data and try again.
                            else if (MessageUtils.isMobileDataDisabled(mContext)) {
                                builder.setMessage(mContext.getString(R.string.inform_data_off));
                                builder.show();
                                return;
                            }
                        } catch (MmsException e) {
                            Log.e(TAG, e.getMessage(), e);
                            return;
                        }
                        startDownloadAttachment();
                    }
                });
                break;
        }

        // Hide the indicators.
        if (mMessageItem.mLocked) {
            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
            mLockedIndicator.setVisibility(View.VISIBLE);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
        }
        mDeliveredIndicator.setVisibility(View.GONE);
        mDetailsIndicator.setVisibility(View.GONE);
        if (mSimMessagesMode) {
            updateAvatarView(mMessageItem.mAddress, false);
        } else {
            mAvatar.setVisibility(View.GONE);
        }
    }

    private void startDownloadAttachment() {
        mDownloading.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.GONE);
        Intent intent = new Intent(mContext, TransactionService.class);
        intent.putExtra(TransactionBundle.URI, mMessageItem.mMessageUri.toString());
        intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                Transaction.RETRIEVE_TRANSACTION);
        intent.putExtra(Mms.SUB_ID, mMessageItem.mSubscription); //destination subId
        intent.putExtra(MultiSimUtility.ORIGIN_SUB_ID,
                MultiSimUtility.getDefaultDataSubscription(mContext));

        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            Log.d(TAG, "Download button pressed for sub=" + mMessageItem.mSubscription);
            Intent silentIntent = new Intent(mContext,
                    com.android.mms.ui.SelectMmsSubscription.class);
            silentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            silentIntent.putExtras(intent); //copy all extras
            mContext.startService(silentIntent);
        } else {
            mContext.startService(intent);
        }

        DownloadManager.getInstance().markState(
                 mMessageItem.mMessageUri, DownloadManager.STATE_PRE_DOWNLOADING);
    }

    private void updateSimIndicatorView(int subscription) {
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()
                && subscription >= 0) {
            Drawable mSimIndicatorIcon = MessageUtils.getMultiSimIcon(mContext,
                    subscription);
            mSimIndicatorView.setImageDrawable(mSimIndicatorIcon);
            mSimIndicatorView.setVisibility(View.VISIBLE);
        }
    }

    private String buildTimestampLine(String timestamp) {
        if (!mMultiRecipients || mMessageItem.isMe() || TextUtils.isEmpty(mMessageItem.mContact)) {
            // Never show "Me" for messages I sent.
            return timestamp;
        }
        // This is a group conversation, show the sender's name on the same line as the timestamp.
        return mContext.getString(R.string.message_timestamp_format, mMessageItem.mContact,
                timestamp);
    }

    private void showDownloadingAttachment() {
        inflateDownloadControls();
        mDownloading.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.GONE);
    }

    private void updateAvatarView(String addr, boolean isSelf) {
        Drawable avatarDrawable;
        if (isSelf || !TextUtils.isEmpty(addr)) {
            // As this function will be called two times when bind every MMS item,
            // get contact object only once in bind function in order to save time.
            Contact contact = isSelf ? mSelfContact : mContact;
            avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);

            if (isSelf) {
                mAvatar.assignContactUri(Profile.CONTENT_URI);
            } else {
                if (contact.existsInDatabase()) {
                    mAvatar.assignContactUri(contact.getUri());
                } else if (MessageUtils.isWapPushNumber(contact.getNumber())) {
                    mAvatar.assignContactFromPhone(
                            MessageUtils.getWapPushNumber(contact.getNumber()), true);
                } else if(rcsGroupId != RcsUtils.SMS_DEFAULT_RCS_GROUP_ID){
                    GroupMemberPhotoCache.loadGroupMemberPhoto(String.valueOf(rcsGroupId), addr, mAvatar, sDefaultContactImage);
                } else {
                    mAvatar.assignContactFromPhone(contact.getNumber(), true);
                }
            }
        } else {
            avatarDrawable = sDefaultContactImage;
        }
        mAvatar.setImageDrawable(avatarDrawable);
    }

    public TextView getBodyTextView() {
        return mBodyTextView;
    }

    private void bindCommonMessage(final boolean sameItem) {
        if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.GONE);
            mDownloading.setVisibility(View.GONE);
        }

        //layout type may be changed after reload pdu, so update textView here.
        if (mMessageItem.isMms() && mMessageItem.mLayoutType == LayoutModel.LAYOUT_TOP_TEXT) {
            mBodyTextView = mBodyTopTextView;
            mBodyButtomTextView.setVisibility(View.GONE);
        } else {
            mBodyTextView = mBodyButtomTextView;
            mBodyTopTextView.setVisibility(View.GONE);
        }
        //mBodyTextView.setVisibility(View.VISIBLE);

        // Since the message text should be concatenated with the sender's
        // address(or name), I have to display it here instead of
        // displaying it by the Presenter.
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

        boolean haveLoadedPdu = mMessageItem.isSms() || mMessageItem.mSlideshow != null;
        // Here we're avoiding reseting the avatar to the empty avatar when we're rebinding
        // to the same item. This happens when there's a DB change which causes the message item
        // cache in the MessageListAdapter to get cleared. When an mms MessageItem is newly
        // created, it has no info in it except the message id. The info is eventually loaded
        // and bindCommonMessage is called again (see onPduLoaded below). When we haven't loaded
        // the pdu, we don't want to call updateAvatarView because it
        // will set the avatar to the generic avatar then when this method is called again
        // from onPduLoaded, it will reset to the real avatar. This test is to avoid that flash.
        if (mSimMessagesMode) {
            if (!sameItem || haveLoadedPdu) {
                boolean isSelf = Sms.isOutgoingFolder(mMessageItem.mBoxId);
                String addr = isSelf ? null : mMessageItem.mAddress;
                updateAvatarView(addr, isSelf);
            }
        } else {
            mAvatar.setVisibility(View.GONE);
        }

        // Add SIM sms address above body.
        if (isSimCardMessage()) {
            mSimMessageAddress.setVisibility(VISIBLE);
            SpannableStringBuilder buf = new SpannableStringBuilder();
            if (mMessageItem.mBoxId == Sms.MESSAGE_TYPE_INBOX) {
                buf.append(mContext.getString(R.string.from_label));
            } else {
                buf.append(mContext.getString(R.string.to_address_label));
            }
            buf.append(mContact.getName());
            mSimMessageAddress.setText(buf);
        }

        // Get and/or lazily set the formatted message from/on the
        // MessageItem.  Because the MessageItem instances come from a
        // cache (currently of size ~50), the hit rate on avoiding the
        // expensive formatMessage() call is very high.
        CharSequence formattedMessage = mMessageItem.getCachedFormattedMessage();
        if (formattedMessage == null) {
            formattedMessage = formatMessage(mMessageItem,
                                             mMessageItem.mBody,
                                             mMessageItem.mSubscription,
                                             mMessageItem.mSubject,
                                             mMessageItem.mHighlight,
                                             mMessageItem.mTextContentType);
            mMessageItem.setCachedFormattedMessage(formattedMessage);
        }
        if (!sameItem || haveLoadedPdu) {
            mBodyTextView.setText(formattedMessage);
        }
        updateSimIndicatorView(mMessageItem.mSubscription);
        // Debugging code to put the URI of the image attachment in the body of the list item.
        if (DEBUG) {
            String debugText = null;
            if (mMessageItem.mSlideshow == null) {
                debugText = "NULL slideshow";
            } else {
                SlideModel slide = mMessageItem.mSlideshow.get(0);
                if (slide == null) {
                    debugText = "NULL first slide";
                } else if (!slide.hasImage()) {
                    debugText = "Not an image";
                } else {
                    debugText = slide.getImage().getUri().toString();
                }
            }
            mBodyTextView.setText(mPosition + ": " + debugText);
        }

        // If we're in the process of sending a message (i.e. pending), then we show a "SENDING..."
        // string in place of the timestamp.
        if (!sameItem || haveLoadedPdu) {
            mDateView.setText(buildTimestampLine(mMessageItem.isSending() ?
                    mContext.getResources().getString(R.string.sending_message) :
                        mMessageItem.mTimestamp));
        }
        if (mMessageItem.mRcsId != RcsUtils.SMS_DEFAULT_RCS_ID&&mMessageItem.isSms()) {
            if(TextUtils.isEmpty(mMessageItem.mTimestamp)){
                if(mMessageItem.mDate == 0){
                    mMessageItem.mDate = System.currentTimeMillis();
                }
                mMessageItem.mTimestamp = String.format(mContext.getString(R.string.sent_on),
                        MessageUtils.formatTimeStampString(mContext, mMessageItem.mDate));
            }
            if (mMessageItem.isMe()) {
                switch (mMessageItem.mRcsMsgState) {

                    case MESSAGE_SENDING:
                        if ((mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_IMAGE || mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_VIDEO)) {
                            if (sFileTrasnfer != null) {
                                Long percent = sFileTrasnfer.get(mMessageItem.mRcsMessageId);
                                if (percent != null) {
                                    mDateView.setText(getContext().getString(
                                            R.string.uploading_percent, percent.intValue()));
                                }
                            }
                        } else {
                            mDateView.setText(R.string.message_adapte_sening);
                        }
                        break;
                    case MESSAGE_HAS_SENDED:
                        mDateView.setText(mContext.getResources().getString(
                                R.string.message_adapter_has_send)
                                + mMessageItem.mTimestamp);
                        break;
                    case MESSAGE_SENDED:
                        mDateView.setText(mContext.getResources().getString(
                                R.string.message_received)
                                + mMessageItem.mTimestamp);
                        break;
                    case MESSAGE_FAIL:
                        if (mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_TEXT)
                            mDateView.setText(R.string.message_send_fail);
                        else
                            mDateView.setText(R.string.message_send_fail_resend);
                        break;
                    case MESSAGE_SEND_RECEIVE:
                        mDateView.setText(mContext.getResources().getString(
                                R.string.message_received)
                                + mMessageItem.mTimestamp);
                        break;
                    case MESSAGE_HAS_BURNED:
                        mDateView.setText(mContext.getResources().getString(
                                R.string.message_received)
                                + mMessageItem.mTimestamp);
                        if (mMessageItem.mRcsIsBurn != 1)
                            RcsUtils.burnMessageAtLocal(mContext, mMessageItem.mMsgId);
                        break;
                    case SuntekMessageData.MSG_STATE_DOWNLOAD_FAIL:

                        break;
                    default:
                        mDateView.setText(R.string.message_adapte_sening);
                        break;
                }
            } else {
                if ((mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_IMAGE || mMessageItem.mRcsType == RcsUtils.RCS_MSG_TYPE_VIDEO)) {
                    if (!isFileDownLoadoK(mMessageItem)
                            && mMessageItem.mRcsBurnFlag != RcsUtils.RCS_IS_BURN_TRUE
                            && !isDownloading()) {
                        mDateView.setText(R.string.message_download);
                    } else if (isDownloading() && !mRcsIsStopDown) {
                        if (sFileTrasnfer != null) {
                            Long percent = sFileTrasnfer.get(mMessageItem.mRcsMessageId);
                            if (percent != null) {
                                if (!mMessageItem.isMe()) {
                                    mDateView.setText(getContext().getString(
                                            R.string.downloading_percent, percent.intValue()));
                                } else {
                                    mDateView.setText(getContext().getString(
                                            R.string.uploading_percent, percent.intValue()));
                                }
                            }
                        }
                    } else if (mRcsIsStopDown) {
                        mDateView.setText(R.string.stop_down_load);
                    } else if (mMessageItem.mRcsIsDownload == RcsUtils.RCS_IS_DOWNLOAD_OK) {
                        mDateView.setText(buildTimestampLine(mMessageItem.isSending() ? mContext
                                .getResources().getString(R.string.sending_message)
                                : mMessageItem.mTimestamp));
                    }
                }
            }
            if (mMessageItem.mRcsMsgState == 0
                    && mMessageItem.mRcsType != SuntekMessageData.MSG_TYPE_TEXT
                    && sFileTrasnfer != null) {
                Long percent = sFileTrasnfer.get(mMessageItem.mRcsMessageId);
                if (percent != null) {
                    if (!mMessageItem.isMe()) {
                        mDateView.setText(getContext().getString(
                                R.string.downloading_percent,
                                percent.intValue()));
                    } else {
                        mDateView
                                .setText(getContext().getString(
                                        R.string.uploading_percent,
                                        percent.intValue()));
                    }
                }
            }
        }
        if(!rcs_showMmsView) {
            if (mMessageItem.isSms()) {
                showMmsView(false);
                mMessageItem.setOnPduLoaded(null);
            } else {
                if (DEBUG) {
                    Log.v(TAG, "bindCommonMessage for item: " + mPosition + " " +
                            mMessageItem.toString() +
                            " mMessageItem.mAttachmentType: " + mMessageItem.mAttachmentType +
                            " sameItem: " + sameItem);
                }
                if (mMessageItem.mAttachmentType != WorkingMessage.TEXT) {
                    if (!sameItem) {
                        setImage(null, null);
                    }
                    setOnClickListener(mMessageItem);
                    drawPlaybackButton(mMessageItem);
                } else {
                    showMmsView(false);
                }
                if (mMessageItem.mSlideshow == null) {
                    final int mCurrentAttachmentType = mMessageItem.mAttachmentType;
                    mMessageItem.setOnPduLoaded(new MessageItem.PduLoadedCallback() {
                        public void onPduLoaded(MessageItem messageItem) {
                            if (DEBUG) {
                                Log.v(TAG, "PduLoadedCallback in MessageListItem for item: " + mPosition +
                                        " " + (mMessageItem == null ? "NULL" : mMessageItem.toString()) +
                                        " passed in item: " +
                                        (messageItem == null ? "NULL" : messageItem.toString()));
                            }
                            if (messageItem != null && mMessageItem != null &&
                                    messageItem.getMessageId() == mMessageItem.getMessageId()) {
                                mMessageItem.setCachedFormattedMessage(null);
                                boolean isStillSame =
                                        mCurrentAttachmentType == messageItem.mAttachmentType;
                                bindCommonMessage(isStillSame);
                            }
                        }
                    });
                } else {
                    if (mPresenter == null) {
                        mPresenter = PresenterFactory.getPresenter(
                                "MmsThumbnailPresenter", mContext,
                                this, mMessageItem.mSlideshow);
                    } else {
                        mPresenter.setModel(mMessageItem.mSlideshow);
                        mPresenter.setView(this);
                    }
                    if (mImageLoadedCallback == null) {
                        mImageLoadedCallback = new ImageLoadedCallback(this);
                    } else {
                        mImageLoadedCallback.reset(this);
                    }
                    mPresenter.present(mImageLoadedCallback);
                }
            }
        }
        mMessageBlock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMessageListItemClick();
            }
        });
        // Call context menu of message list.
        mMessageBlock.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return v.showContextMenu();
            }
        });
        drawRightStatusIndicator(mMessageItem);

        requestLayout();
    }

    public static boolean isFileDownLoadoK(MessageItem mMsgItem) {
        if (mMsgItem == null ){
            return false;
        }
        String filePath = RcsUtils.getFilePath(mMsgItem.mRcsId, mMsgItem.mRcsPath);
        ChatMessage msg = null;
        boolean isFileDownload = false;
        try {
            msg = RcsApiManager.getMessageApi().getMessageById(String.valueOf(mMsgItem.mRcsId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg != null) {
            isFileDownload = RcsChatMessageUtils.isFileDownload(filePath, msg.getFilesize());
        }
        return isFileDownload;
    }

    static private class ImageLoadedCallback implements ItemLoadedCallback<ImageLoaded> {
        private long mMessageId;
        private final MessageListItem mListItem;

        public ImageLoadedCallback(MessageListItem listItem) {
            mListItem = listItem;
            mMessageId = listItem.getMessageItem().getMessageId();
        }

        public void reset(MessageListItem listItem) {
            mMessageId = listItem.getMessageItem().getMessageId();
        }

        public void onItemLoaded(ImageLoaded imageLoaded, Throwable exception) {
            if (DEBUG_DONT_LOAD_IMAGES) {
                return;
            }
            // Make sure we're still pointing to the same message. The list item could have
            // been recycled.
            MessageItem msgItem = mListItem.mMessageItem;
            if (msgItem != null && msgItem.getMessageId() == mMessageId) {
                if (imageLoaded.mIsVideo) {
                    mListItem.setVideoThumbnail(null, imageLoaded.mBitmap);
                } else {
                    mListItem.setImage(null, imageLoaded.mBitmap);
                }
            }
        }
    }

    DialogInterface.OnClickListener mCancelLinstener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, final int whichButton) {
            if (mDownloading.getVisibility() == View.VISIBLE) {
                Intent intent = new Intent(mContext, TransactionService.class);
                intent.putExtra(CANCEL_URI, mMessageItem.mMessageUri.toString());
                mContext.startService(intent);
                DownloadManager.getInstance().markState(mMessageItem.mMessageUri,
                        DownloadManager.STATE_UNSTARTED);
            }
        }
    };

    @Override
    public void startAudio() {
        // TODO Auto-generated method stub
    }

    @Override
    public void startVideo() {
        // TODO Auto-generated method stub
    }

    @Override
    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setImage(String name, Bitmap bitmap) {
        // is Multi choice mode
        if (mMultiChoiceMode) {
            showMmsView(false);
            return;
        }
        showMmsView(true);

        try {
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage: out of memory: ", e);
        }
    }

    private void showMmsView(boolean visible) {
        if (mMmsView == null) {
            mMmsView = findViewById(R.id.mms_view);
            // if mMmsView is still null here, that mean the mms section hasn't been inflated

            if (visible && mMmsView == null) {
                //inflate the mms view_stub
                View mmsStub = findViewById(R.id.mms_layout_view_stub);
                if (mmsStub != null)
                    mmsStub.setVisibility(View.VISIBLE);
                mMmsView = findViewById(R.id.mms_view);
            }
        }
        if (mMmsView != null) {
            if (mImageView == null) {
                mImageView = (ImageView) findViewById(R.id.image_view);
            }
            if (mSlideShowButton == null) {
                mSlideShowButton = (ImageButton) findViewById(R.id.play_slideshow_button);
            }
            mMmsView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void inflateDownloadControls() {
        if (mDownloadButton == null) {
            //inflate the download controls
            findViewById(R.id.mms_downloading_view_stub).setVisibility(VISIBLE);
            mDownloadButton = (Button) findViewById(R.id.btn_download_msg);
            if (getResources().getBoolean(R.bool.config_mms_cancelable)) {
                mDownloading = (Button) findViewById(R.id.btn_cancel_download);
                mDownloading.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setTitle(R.string.cancel_downloading)
                                .setIconAttribute(android.R.attr.alertDialogIcon)
                                .setCancelable(true)
                                .setPositiveButton(R.string.yes, mCancelLinstener)
                                .setNegativeButton(R.string.no, null)
                                .setMessage(R.string.confirm_cancel_downloading)
                                .show();
                    }
                });
            } else {
                mDownloading = (TextView) findViewById(R.id.label_downloading);
            }
        }
    }


    private LineHeightSpan mSpan = new LineHeightSpan() {
        @Override
        public void chooseHeight(CharSequence text, int start,
                int end, int spanstartv, int v, FontMetricsInt fm) {
            fm.ascent -= 10;
        }
    };

    TextAppearanceSpan mTextSmallSpan =
        new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);

    ForegroundColorSpan mColorSpan = null;  // set in ctor

    private CharSequence formatMessage(MessageItem msgItem, String body,
                                       int subId, String subject, Pattern highlight,
                                       String contentType) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        boolean hasSubject = !TextUtils.isEmpty(subject);
        if (hasSubject) {
            buf.append(mContext.getResources().getString(R.string.inline_subject, subject));
        }

        if (!TextUtils.isEmpty(body)) {
            // Converts html to spannable if ContentType is "text/html".
            if (contentType != null && ContentType.TEXT_HTML.equals(contentType)) {
                buf.append("\n");
                buf.append(Html.fromHtml(body));
            } else {
                if (hasSubject) {
                    buf.append(" - ");
                }
                buf.append(body);
            }
        }

        if (highlight != null) {
            Matcher m = highlight.matcher(buf.toString());
            while (m.find()) {
                buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
            }
        }
        return buf;
    }

    private boolean isSimCardMessage() {
        return mContext instanceof ManageSimMessages
                || (mContext instanceof ManageMultiSelectAction &&
                mManageMode == MessageUtils.SIM_MESSAGE_MODE);
    }

    public void setManageSelectMode(int manageMode) {
        mManageMode = manageMode;
    }

    private void drawPlaybackButton(MessageItem msgItem) {
        // is Multi choice mode
        if (mMultiChoiceMode) {
            return;
        }
        switch (msgItem.mAttachmentType) {
            case WorkingMessage.SLIDESHOW:
            case WorkingMessage.AUDIO:
            case WorkingMessage.VIDEO:
                // Show the 'Play' button and bind message info on it.
                mSlideShowButton.setTag(msgItem);
                // Set call-back for the 'Play' button.
                mSlideShowButton.setOnClickListener(this);
                mSlideShowButton.setVisibility(View.VISIBLE);
                break;
            default:
                mSlideShowButton.setVisibility(View.GONE);
                break;
        }
    }

    // OnClick Listener for the playback button
    @Override
    public void onClick(View v) {
        sendMessage(mMessageItem, MSG_LIST_PLAY);
    }

    private void sendMessage(MessageItem messageItem, int message) {
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, message);
            msg.obj = messageItem;
            msg.sendToTarget(); // See ComposeMessageActivity.mMessageListItemHandler.handleMessage
        }
    }

    public void onMessageListItemClick() {
        // If the message is a failed one, clicking it should reload it in the compose view,
        // regardless of whether it has links in it
        if (mMessageItem != null &&
                mMessageItem.isOutgoingMessage() &&
                mMessageItem.isFailedMessage() ) {
            //if message is rcsMessage except text,return.
            if( mMessageItem.mRcsId != RcsUtils.SMS_DEFAULT_RCS_ID && mMessageItem.mRcsType != RcsUtils.RCS_MSG_TYPE_TEXT ){
                return;
            }
            // Assuming the current message is a failed one, reload it into the compose view so
            // the user can resend it.
            sendMessage(mMessageItem, MSG_LIST_EDIT);
            return;
        }

        // Check for links. If none, do nothing; if 1, open it; if >1, ask user to pick one
        final URLSpan[] spans = mBodyTextView.getUrls();
        if (spans.length == 0) {
            sendMessage(mMessageItem, MSG_LIST_DETAILS);
        } else {
            boolean wap_push = mContext.getResources().getBoolean(R.bool.config_wap_push);
            if (spans.length == 1 && mMessageItem != null
                    && MessageUtils.isWapPushNumber(mMessageItem.mAddress)
                    && wap_push) {
                DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialog, int which) {
                        spans[0].onClick(mBodyTextView);
                    }
                };
                new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.open_wap_push_title))
                        .setMessage(mContext.getString(R.string.open_wap_push_body))
                        .setPositiveButton(android.R.string.ok, click)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setCancelable(true)
                        .show();
            } else {
                MessageUtils.onMessageContentClick(mContext, mBodyTextView);
            }
        }
    }

    private void setOnClickListener(final MessageItem msgItem) {
        // is Multi choice mode
        if (mMultiChoiceMode) {
            return;
        }
        switch(msgItem.mAttachmentType) {
            case WorkingMessage.VCARD:
            case WorkingMessage.IMAGE:
            case WorkingMessage.VIDEO:
                mImageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage(msgItem, MSG_LIST_PLAY);
                    }
                });
                mImageView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return v.showContextMenu();
                    }
                });
                break;

            default:
                mImageView.setOnClickListener(null);
                break;
            }
    }

    private void drawRightStatusIndicator(MessageItem msgItem) {
        // Locked icon
        if (msgItem.mLocked) {
            mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
            mLockedIndicator.setVisibility(View.VISIBLE);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
        }

        // Delivery icon - we can show a failed icon for both sms and mms, but for an actual
        // delivery, we only show the icon for sms. We don't have the information here in mms to
        // know whether the message has been delivered. For mms, msgItem.mDeliveryStatus set
        // to MessageItem.DeliveryStatus.RECEIVED simply means the setting requesting a
        // delivery report was turned on when the message was sent. Yes, it's confusing!
        if ((msgItem.isOutgoingMessage() && msgItem.isFailedMessage()) ||
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED) {
            mDeliveredIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.isSms() &&
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED) {
            mDeliveredIndicator.setImageResource(R.drawable.ic_sms_mms_delivered);
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else {
            mDeliveredIndicator.setVisibility(View.GONE);
        }

        // Message details icon - this icon is shown both for sms and mms messages. For mms,
        // we show the icon if the read report or delivery report setting was set when the
        // message was sent. Showing the icon tells the user there's more information
        // by selecting the "View report" menu.
        if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.INFO
                || (msgItem.isMms() && !msgItem.isSending() &&
                        msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.PENDING)) {
            mDetailsIndicator.setImageResource(R.drawable.ic_sms_mms_details);
            mDetailsIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.isMms() && !msgItem.isSending() &&
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED) {
            mDetailsIndicator.setImageResource(R.drawable.ic_sms_mms_delivered);
            mDetailsIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.mReadReport) {
            mDetailsIndicator.setImageResource(R.drawable.ic_sms_mms_details);
            mDetailsIndicator.setVisibility(View.VISIBLE);
        } else {
            mDetailsIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setText(String name, String text) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setVideo(String name, Uri uri) {
    }

    @Override
    public void setVideoThumbnail(String name, Bitmap bitmap) {
        if (mMultiChoiceMode) {
            showMmsView(false);
            return;
        }
        showMmsView(true);

        try {
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setVideo: out of memory: ", e);
        }
    }

    @Override
    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void stopAudio() {
        // TODO Auto-generated method stub
    }

    @Override
    public void stopVideo() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
    }

    @Override
    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void pauseAudio() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pauseVideo() {
        // TODO Auto-generated method stub

    }

    @Override
    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVcard(Uri lookupUri, String name) {
        if (mMultiChoiceMode) {
            showMmsView(false);
            return;
        }
        showMmsView(true);

        try {
            mImageView.setImageResource(R.drawable.ic_attach_vcard);
            mImageView.setVisibility(VISIBLE);
        } catch (java.lang.OutOfMemoryError e) {
            // shouldn't be here.
            Log.e(TAG, "setVcard: out of memory: ", e);
        }
    }

    protected void customSIMSmsView() {
        if (isSimCardMessage()) {
            // hide delivered indicator in SIM message
            mDeliveredIndicator.setVisibility(GONE);
            // SIM message have no send date, hide date view
            if (mMessageItem.isOutgoingMessage() || mMessageItem.mBoxId == Sms.MESSAGE_TYPE_SENT) {
                mDateView.setVisibility(View.GONE);
            }
        }
    }

    public void setSimMessagesMode(boolean isSimMessagesMode) {
        mSimMessagesMode = isSimMessagesMode;
    }

    public static void setRcsIsStopDown(boolean rcsIsStopDown) {
        MessageListItem.mRcsIsStopDown = rcsIsStopDown;
    }

    public static HashMap<String, Long> getFileTrasnferHashMap() {
        return sFileTrasnfer;
    }

    public void setMultiChoiceMode(boolean isMultiChoiceMode) {
        mMultiChoiceMode = isMultiChoiceMode;
    }

    @Override
    protected void onDetachedFromWindow() {
        unbind();
        super.onDetachedFromWindow();
    }
}
