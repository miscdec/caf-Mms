/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_ABORT;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_COMPLETE;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_START;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_STATUS_ACTION;
import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_FAVOURITE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_ADDRESS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_BODY;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_DATE_SENT;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_READ;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_STATUS;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SUB_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_THREAD_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_RCS_CHAT_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_RCS_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.PROJECTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.Set;

import android.Manifest;
import android.R.integer;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.MediaFile;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.provider.DocumentsContract.Document;
import android.provider.Telephony;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import android.widget.Button;

import com.android.ex.chips.RecipientEditTextView;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.TempFileProvider;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.Conversation.ConversationQueryHandler;
import com.android.mms.data.WorkingMessage;
import com.android.mms.data.WorkingMessage.MessageStatusListener;
import com.android.mms.drm.DrmUtils;
import com.android.mms.model.ContentRestriction;
import com.android.mms.model.ContentRestrictionFactory;
import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.rcs.ComposeMessageCloudFileReceiver;
import com.android.mms.rcs.ComposeMessageFileTransferReceiver;
import com.android.mms.rcs.RcsChatMessageUtils;
import com.android.mms.rcs.RcsCreateGroupChatActivity;
import com.android.mms.rcs.RcsDualSimMananger;
import com.android.mms.rcs.RcsMessageOpenUtils;
import com.android.mms.rcs.RcsMessageOpenUtils.OpenRcsMessageIntent;
import com.android.mms.rcs.RcsSelectionMenu;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.ui.MessageListView.OnSizeChangedListener;
import com.android.mms.ui.MessageUtils.ResizeImageResultCallback;
import com.android.mms.ui.MultiPickContactGroups;
import com.android.mms.ui.RecipientsEditor.RecipientContextMenuInfo;
import com.android.mms.ui.zoom.ZoomGestureOverlayView;
import com.android.mms.ui.zoom.ZoomGestureOverlayView.IZoomListener;
import com.android.mms.ui.zoom.ZoomMessageListItem;
import com.android.mms.util.DraftCache;
import com.android.mms.util.MaterialColorMapUtils;
import com.android.mms.util.MaterialColorMapUtils.MaterialPalette;
import com.android.mms.util.PhoneNumberFormatter;
import com.android.mms.util.SendingProgressTokenManager;
import com.android.mms.widget.MmsWidgetProvider;
import com.google.android.mms.ContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;

import com.suntek.mway.rcs.client.aidl.constant.Actions;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.constant.Parameter;
import com.suntek.mway.rcs.client.aidl.common.RcsColumns;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Avatar;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Profile;
import com.suntek.mway.rcs.client.api.capability.CapabiltyListener;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChat;
import com.suntek.mway.rcs.client.aidl.service.entity.GroupChatMember;
import com.suntek.mway.rcs.client.aidl.service.entity.RCSCapabilities;
import com.suntek.mway.rcs.client.aidl.service.entity.SimpleMessage;
import com.suntek.mway.rcs.client.api.basic.BasicApi;
import com.suntek.mway.rcs.client.api.capability.CapabilityApi;
import com.suntek.mway.rcs.client.api.capability.CapabiltyListener;
import com.suntek.mway.rcs.client.api.emoticon.EmoticonApi;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatApi;
import com.suntek.mway.rcs.client.api.groupchat.GroupChatCallback;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.profile.ProfileApi;
import com.suntek.mway.rcs.client.api.specialnumber.SpecialServiceNumApi;
import com.suntek.mway.rcs.client.api.support.SupportApi;
import com.suntek.rcs.ui.common.mms.GroupChatManagerReceiver;
import com.suntek.rcs.ui.common.mms.GroupChatManagerReceiver.GroupChatNotifyCallback;
import com.suntek.rcs.ui.common.mms.GroupMemberPhotoCache;
import com.suntek.rcs.ui.common.mms.RcsContactsUtils;
import com.suntek.rcs.ui.common.RcsEmojiInitialize;
import com.suntek.rcs.ui.common.RcsEmojiInitialize.EmojiResources;
import com.suntek.rcs.ui.common.RcsEmojiInitialize.ViewOnClickListener;
import com.suntek.rcs.ui.common.RcsFileController;
import com.suntek.rcs.ui.common.RcsLog;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.codeaurora.presenceserv.IPresenceService;
import org.codeaurora.presenceserv.IPresenceServiceCB;

/**
 * This is the main UI for:
 * 1. Composing a new message;
 * 2. Viewing/managing message history of a conversation.
 *
 * This activity can handle following parameters from the intent
 * by which it's launched.
 * thread_id long Identify the conversation to be viewed. When creating a
 *         new message, this parameter shouldn't be present.
 * msg_uri Uri The message which should be opened for editing in the editor.
 * address String The addresses of the recipients in current conversation.
 * exit_on_sent boolean Exit this activity after the message is sent.
 */
public class ComposeMessageActivity extends Activity
        implements View.OnClickListener, TextView.OnEditorActionListener,
        MessageStatusListener, Contact.UpdateListener, IZoomListener {
    public static final int REQUEST_CODE_ATTACH_IMAGE     = 100;
    public static final int REQUEST_CODE_TAKE_PICTURE     = 101;
    public static final int REQUEST_CODE_ATTACH_VIDEO     = 102;
    public static final int REQUEST_CODE_TAKE_VIDEO       = 103;
    public static final int REQUEST_CODE_ATTACH_SOUND     = 104;
    public static final int REQUEST_CODE_RECORD_SOUND     = 105;
    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 106;
    public static final int REQUEST_CODE_ECM_EXIT_DIALOG  = 107;
    public static final int REQUEST_CODE_ADD_CONTACT      = 108;
    public static final int REQUEST_CODE_PICK             = 109;
    public static final int REQUEST_CODE_ATTACH_ADD_CONTACT_INFO     = 110;
    public static final int REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD    = 111;
    public static final int REQUEST_CODE_ATTACH_REPLACE_CONTACT_INFO = 112;
    public static final int REQUEST_CODE_ATTACH_ADD_CONTACT_RCS_VCARD = 113;
    public static final int REQUEST_CODE_ADD_CALENDAR_EVENTS          = 114;
    public static final int REQUEST_CODE_ATTACH_MAP       = 115;
    public static final int REQUEST_CODE_RCS_PICK         = 116;
    public static final int REQUEST_SELECT_CONV           = 117;
    public static final int REQUEST_SELECT_GROUP          = 118;
    public static final int REQUEST_CODE_VCARD_GROUP      = 119;
    public static final int REQUEST_CODE_SAIYUN           = 120;
    public static final int REQUEST_CODE_ADD_RECIPIENTS   = 121;
    public static final int REQUEST_SELECT_LOCAL_AUDIO    = 122;
    public static final int REQUEST_CODE_EMOJI_STORE      = 123;
    public static final int REQUEST_CODE_ADD_CONVERSATION = 124;

    private static final String TAG = LogTag.TAG;

    private static final boolean DEBUG = false;
    private static final boolean TRACE = false;
    private static final boolean LOCAL_LOGV = false;
    private static final boolean DEBUG_MULTI_CHOICE = true;

    // Menu ID
    private static final int MENU_ADD_SUBJECT           = 0;
    private static final int MENU_DELETE_THREAD         = 1;
    private static final int MENU_ADD_ATTACHMENT        = 2;
    private static final int MENU_DISCARD               = 3;
    private static final int MENU_SEND                  = 4;
    private static final int MENU_CALL_RECIPIENT        = 5;
    private static final int MENU_CONVERSATION_LIST     = 6;
    private static final int MENU_DEBUG_DUMP            = 7;
    private static final int MENU_SEND_BY_SLOT1         = 9;
    private static final int MENU_SEND_BY_SLOT2         = 10;

    // Context menu ID
    private static final int MENU_VIEW_CONTACT          = 12;
    private static final int MENU_ADD_TO_CONTACTS       = 13;
    private static final int MENU_COPY_PHONENUMBER      = 14;

    private static final int MENU_EDIT_MESSAGE          = 15;
    private static final int MENU_VIEW_SLIDESHOW        = 16;
    private static final int MENU_VIEW_MESSAGE_DETAILS  = 17;
    private static final int MENU_DELETE_MESSAGE        = 18;
    private static final int MENU_SEARCH                = 19;
    private static final int MENU_DELIVERY_REPORT       = 20;
    private static final int MENU_FORWARD_MESSAGE       = 21;
    private static final int MENU_CALL_BACK             = 22;
    private static final int MENU_SEND_EMAIL            = 23;
    private static final int MENU_COPY_MESSAGE_TEXT     = 24;
    private static final int MENU_COPY_TO_SDCARD        = 25;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 27;
    private static final int MENU_LOCK_MESSAGE          = 28;
    private static final int MENU_UNLOCK_MESSAGE        = 29;
    private static final int MENU_SAVE_RINGTONE         = 30;
    private static final int MENU_PREFERENCES           = 31;
    private static final int MENU_GROUP_PARTICIPANTS    = 32;
    private static final int MENU_IMPORT_TEMPLATE       = 33;
    private static final int MENU_COPY_TO_SIM           = 34;
    private static final int MENU_RESEND                = 35;
    private static final int MENU_COPY_EXTRACT_URL      = 36;
    private static final int MENU_ADD_TO_BLACKLIST      = 37;
    private static final int MENU_FAVOURITE_MESSAGE         = 38;
    private static final int MENU_UNFAVOURITE_MESSAGE       = 39;
    private static final int MENU_TOP_CONVERSATION          = 40;
    private static final int MENU_CANCEL_TOP_CONVERSATION   = 41;
    private static final int MENU_VIDEOCALL_RECIPIENT   = 42;

    private static final int MENU_FIERWALL_ADD_BLACKLIST    = 50;
    private static final int MENU_FIERWALL_ADD_WHITELIST    = 51;
    // RCS menu ID
    private static final int MENU_RCS_GROUP_CHAT_DETAIL  = 100;
    private static final int MENU_RCS_BURN_MESSGEE_FLAG = 101;
    private static final int MENU_RCS_SWITCH_TO_GROUP_CHAT = 102;
    private static final int MENU_RCS_MCLOUD_SHARE = 103;

    private static final int RECIPIENTS_MAX_LENGTH = 312;

    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN = 9528;

    private static final int DELETE_MESSAGE_TOKEN  = 9700;

    private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 10;

    private static final long NO_DATE_FOR_DIALOG = -1L;

    protected static final String KEY_EXIT_ON_SENT = "exit_on_sent";
    protected static final String KEY_FORWARDED_MESSAGE = "forwarded_message";
    protected static final String KEY_REPLY_MESSAGE = "reply_message";


    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    private static final String INTENT_MULTI_PICK_ACTION = "com.android.contacts.action.MULTI_PICK";

    private static final String EXTRA_START_COMPOSE_FROM = "start_compose_from";

    private static String FILE_PATH_COLUMN = "_data";
    private static String BROADCAST_DATA_SCHEME = "file";
    private static String URI_SCHEME_CONTENT = "content";
    private static String URI_HOST_MEDIA = "media";

    // When the conversation has a lot of messages and a new message is sent, the list is scrolled
    // so the user sees the just sent message. If we have to scroll the list more than 20 items,
    // then a scroll shortcut is invoked to move the list near the end before scrolling.
    private static final int MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT = 20;

    // Any change in height in the message list view greater than this threshold will not
    // cause a smooth scroll. Instead, we jump the list directly to the desired position.
    private static final int SMOOTH_SCROLL_THRESHOLD = 200;

    // To reduce janky interaction when message history + draft loads and keyboard opening
    // query the messages + draft after the keyboard opens. This controls that behavior.
    private static final boolean DEFER_LOADING_MESSAGES_AND_DRAFT = true;

    // The max amount of delay before we force load messages and draft.
    // 500ms is determined empirically. We want keyboard to have a chance to be shown before
    // we force loading. However, there is at least one use case where the keyboard never shows
    // even if we tell it to (turning off and on the screen). So we need to force load the
    // messages+draft after the max delay.
    private static final int LOADING_MESSAGES_AND_DRAFT_MAX_DELAY_MS = 500;

    private static final int MSG_ADD_ATTACHMENT_FAILED = 1;

    private static final int DIALOG_IMPORT_TEMPLATE = 1;

    private static final int MSG_COPY_TO_SIM_SUCCESS = 2;

    private static final int KILOBYTE = 1024;
    // The max length of characters for subject.
    private static final int SUBJECT_MAX_LENGTH = MmsConfig.getMaxSubjectLength();
    // The number of buttons in two send button mode
    private static final int NUMBER_OF_BUTTONS = 2;

    // Preferred CDMA subscription mode is NV.
    private static final int CDMA_SUBSCRIPTION_NV = 1;

    // The default displaying page when selecting attachments.
    private static final int DEFAULT_ATTACHMENT_PAGER = 0;

    private static final int SAVE_ATTACHMENT_PERMISSION_REQUEST_CODE = 2016;

    private ContentResolver mContentResolver;

    private BackgroundQueryHandler mBackgroundQueryHandler;

    private Conversation mConversation;     // Conversation we are working in

    // When mSendDiscreetMode is true, this activity only allows a user to type in and send
    // a single sms, send the message, and then exits. The message history and menus are hidden.
    private boolean mSendDiscreetMode;
    private boolean mForwardMessageMode;
    private boolean mReplyMessageMode;

    private Toolbar mToolBar;
    private View mTopPanel;                 // View containing the recipient and subject editors
    private View mBottomPanel;              // View containing the text editor, send button, ec.
    private EditText mTextEditor;           // Text editor to type your message into
    private TextView mTextCounter;          // Shows the number of characters used in text editor
    private View mAttachmentSelector;       // View containing the added attachment types
    private ImageButton mAddAttachmentButton;  // The button for add attachment
    private ViewPager mAttachmentPager;     // Attachment selector pager
    private AttachmentPagerAdapter mAttachmentPagerAdapter;  // Attachment selector pager adapter
    private ImageButton mSendButtonMms;        // Press to send mms
    private TextView mSendButtonMmsText;      // The text on MMS send button
    private ImageButton mSendButtonSms;     // Press to send sms
    private EditText mSubjectTextEditor;    // Text editor for MMS subject
    private TextView mTextCounterSec;   // The second send button text counter
    private View mSendLayoutMmsFir;        // The first mms send layout with sim indicator
    private View mSendLayoutSmsFir;     // The first sms send layout with sim indicator
    private View mSendLayoutMmsSec;    // The second mms send layout with sim indicator
    private View mSendLayoutSmsSec;    // The second sms send layout with sim indicator
    private TextView mSendButtonMmsViewSec;    // The second mms send button without sim indicator
    private ImageButton mSendButtonSmsViewSec; // The second sms send button without sim indicator
    private ImageView mIndicatorForSimMmsFir, mIndicatorForSimSmsFir;
    private ImageView mIndicatorForSimMmsSec, mIndicatorForSimSmsSec;
    private ZoomGestureOverlayView mZoomGestureOverlayView; // overlay for handling zoom
    private ImageButton mBackView;
    private View mDeviderView;

    private AttachmentEditor mAttachmentEditor;
    private View mAttachmentEditorScrollView;

    private MessageListView mMsgListView;        // ListView for messages in this conversation
    public MessageListAdapter mMsgListAdapter;  // and its corresponding ListAdapter
    private ModeCallback mModeCallback;

    private RecipientsEditor mRecipientsEditor;  // UI control for editing recipients
    private ImageButton mRecipientsPicker;       // UI control for recipients picker
    private ImageButton mRecipientsPickerGroups; // UI control for group recipients picker
    private ImageButton mClearButton;


    // For HW keyboard, 'mIsKeyboardOpen' indicates if the HW keyboard is open.
    // For SW keyboard, 'mIsKeyboardOpen' should always be true.
    private boolean mIsKeyboardOpen;
    private boolean mIsLandscape;                // Whether we're in landscape mode

    private boolean mToastForDraftSave;   // Whether to notify the user that a draft is being saved

    private boolean mSentMessage;       // true if the user has sent a message while in this
                                        // activity. On a new compose message case, when the first
                                        // message is sent is a MMS w/ attachment, the list blanks
                                        // for a second before showing the sent message. But we'd
                                        // think the message list is empty, thus show the recipients
                                        // editor thinking it's a draft message. This flag should
                                        // help clarify the situation.

    private WorkingMessage mWorkingMessage;         // The message currently being composed.

    private AlertDialog mInvalidRecipientDialog, mMsgDetailDialog;

    private boolean mWaitingForSubActivity;
    private boolean mInAsyncAddAttathProcess = false;
    private int mLastRecipientCount;            // Used for warning the user on too many recipients.
    private AttachmentTypeSelectorAdapter mAttachmentTypeSelectorAdapter;

    private boolean mSendingMessage;    // Indicates the current message is sending, and shouldn't send again.

    private Intent mAddContactIntent;   // Intent used to add a new contact

    private String mBodyString;         // Only used as a temporary to hold a message body
    private Uri mTempMmsUri;            // Only used as a temporary to hold a slideshow uri
    private long mTempThreadId;         // Only used as a temporary to hold a threadId

    private AsyncDialog mAsyncDialog;   // Used for background tasks.

    private String mDebugRecipients;
    private int mLastSmoothScrollPosition;
    private boolean mScrollOnSend;      // Flag that we need to scroll the list to the end.

    private boolean mIsReplaceAttachment;
    private int mCurrentAttachmentPager;
    private int mSavedScrollPosition = -1;  // we save the ListView's scroll position in onPause(),
                                            // so we can remember it after re-entering the activity.
                                            // If the value >= 0, then we jump to that line. If the
                                            // value is maxint, then we jump to the end.
    private long mLastMessageId;
    private AlertDialog mMsimDialog;     // Used for MSIM subscription choose

    // Record the resend sms recipient when the sms send to more than one recipient
    private String mResendSmsRecipient;

    private static final String INTENT_ACTION_LTE_DATA_ONLY_DIALOG =
            "com.qualcomm.qti.phonefeature.DISABLE_TDD_LTE";
    private static final String LTE_DATA_ONLY_KEY = "network_band";
    private static final int LTE_DATA_ONLY_MODE = 2;
    private static final String SIM_STATE_CHANGE_ACTION =
            "android.intent.action.SIM_STATE_CHANGED";

    /**
     * Whether this activity is currently running (i.e. not paused)
     */
    private boolean mIsRunning;

    // we may call loadMessageAndDraft() from a few different places. This is used to make
    // sure we only load message+draft once.
    private boolean mMessagesAndDraftLoaded;

    /**
     * Whether the attachment error is in the case of sendMms.
     */
    private boolean mIsAttachmentErrorOnSend = false;

    // whether we should load the draft. For example, after attaching a photo and coming back
    // in onActivityResult(), we should not load the draft because that will mess up the draft
    // state of mWorkingMessage. Also, if we are handling a Send or Forward Message Intent,
    // we should not load the draft.
    private boolean mShouldLoadDraft;

    // Whether or not we are currently enabled for SMS. This field is updated in onStart to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;

    private boolean mIsAirplaneModeOn = false;
    private Handler mHandler = new Handler();

    private  boolean mIsRTL = false;

    // keys for extras and icicles
    public final static String THREAD_ID = "thread_id";
    private final static String RECIPIENTS = "recipients";
    public final static String MANAGE_MODE = "manage_mode";
    private final static String MESSAGE_ID = "message_id";
    private final static String MESSAGE_TYPE = "message_type";
    private final static String MESSAGE_BODY = "message_body";
    private final static String MESSAGE_SUBJECT = "message_subject";
    private final static String MESSAGE_SUBJECT_CHARSET = "message_subject_charset";
    private final static String NEED_RESEND = "needResend";

    private final static int MSG_ONLY_ONE_FAIL_LIST_ITEM = 1;

    private static final String LINE_BREAK = "\n";
    private static final String COLON = ":";
    private static final String LEFT_PARENTHESES = "(";
    private static final String RIGHT_PARENTHESES = ")";

    private boolean isLocked = false;
    private boolean mIsPickingContact = false;
    // List for contacts picked from People.
    private ContactList mRecipientsPickList = null;
    /**
    * Whether the recipients is picked from Contacts
    */
    private boolean mIsProcessPickedRecipients = false;
    private int mExistsRecipientsCount = 0;
    private final static String MSG_SUBJECT_SIZE = "subject_size";

    private int mResizeImageCount = 0;
    private Object mObjectLock = new Object();

    private boolean mShowAttachIcon = false;
    private final static int REPLACE_ATTACHMEN_MASK = 1 << 16;

    private boolean mShowTwoButtons = false;

    private boolean mSendMmsSupportViaWiFi = false;

    private boolean isAvoidingSavingDraft = false;

    private IPresenceService mService;
    private static final int PRESENCE_AVAILABILITY_FETCH = 0;
    private Handler mAvailabilityfetchHandler;
    private boolean mVideoCapable = true;
    private boolean mIsBound;
    private boolean mEnablePresence;
    private static final String PRESENCESERV = "com.qualcomm.qti.presenceserv";
    private static final String PRESENCESERV_PRESENCESERVICE =
             "com.qualcomm.qti.presenceserv.PresenceService";

    private static Drawable sDefaultContactImage;
    private static int sPrimaryColorDark;
    private Drawable mAvatarDrawable;
    private static int mActionBarColor;
    private static int mSendContactColor;
    private UpdateAttachmentInfo mUpdateAttachmentTask = null;

    /* Begin add for RCS */

    private View mRcsThumbnailLayout;
    private LinearLayout mSubjectWrapper;   // Wrapp for subject and cancel button
    private ImageButton mButtonEmoj;
    private ImageView mSubjectRemoveButton; // Remove the subject and editor
    private static final String ACTION_BACKUP_MESSAGES =
            Actions.MessageAction.ACTION_MESSAGE_BACKUP;

    private static final String CREATE_GROUP_CHAT = "com.suntek.rcs.action.CREATR_GROUP_CHAT";

    private static final String ACTION_LUNCHER_RCS_SHAREFILE =
            "com.suntek.mway.rcs.nativeui.ACTION_LUNCHER_RCS_SHAREFILE";
    private static final int PHOTO_CROP = 10000;

    private static final String EXTRA_VIEW_ONE_TO_MANY_MSG_STATUS_MSG_ID = "view_id";
    private static final String EXTRA_VIEW_ONE_TO_MANY_MSG_STATUS_MSG_BODY = "view_body";
    private static final String ACTION_VIEW_ONE_TO_MANY_MSG_STATUS =
            "com.suntek.mway.rcs.nativeui.ui.ACTION_VIEW_MESSAGE_STATUS";

    private static final int DIALOG_TEMPLATE_SELECT         = 1;
    private static final int DIALOG_TEMPLATE_NOT_AVAILABLE  = 2;
    private static final int DIALOG_ADD_RECEIVE_CONTACTS    = 0;
    private static final int DIALOG_ADD_RECEIVE_MSG         = 1;
    private static final int LOAD_TEMPLATE_BY_ID        = 0;
    private static final int LOAD_TEMPLATES             = 1;

    private static final String LUNCH_BACKUP_MESSAGE_ACTIVITY=
            "com.suntek.mway.rcs.ACTION_LUNCHER_BACKUP_MANY_MESSAGE_ACTIVITY";
    private static final String BACKUP_MESSAGE_IDS = "ids";
    private static final String BACKUP_MESSAGE_LIST = "msgList";
    private static final String GROUP_PROFILE_CHANGE_NUMBER = "CHANGE_NUMBER";

    private static final int OPERATE_SUCCESS       = 0;
    private static final int OPERATE_FAILURE       = 1;

    // Forward message
    private static final int FORWARD_INPUT_NUMBER = 0;
    private static final int FORWARD_CONTACTS = 1;
    private static final int FORWARD_CONVERSATION = 2;
    private static final int FORWARD_CONTACT_GROUP = 3;

    // RCS photo operation.
    private static final int RCS_PHOTO_CUT = 0;
    private static final int RCS_PHOTO_ZOOM = 1;
    private static final int RCS_PHOTO_ORIGINAL = 2;
    private static final int RCS_PHOTO_CANCEL = 3;

    // rcs image switch to mms resize
    private boolean mSendAfterResize = false;

    private int mAccentColor = 0;
    private int mStatusBarColor = 0;
    private boolean mAccentColorLoaded = false;
    private boolean mLoadingAccentColor = false;
    private String mRcsLargeImagePath;

    // rcs progress
    long lastProgress = 0;

    // top convsersation IDlist
    private List<Long> mTopMsgThreadIdList = new ArrayList<Long>();

    private boolean mHasBurnCapability = false;

    private boolean mConvsertionSelect = false;

    private final static String MULTI_SELECT_CONV = "select_conversation";

    //rcs forward messageitems
    private List<MessageItem> mRcsForwardItems = new ArrayList<MessageItem>();

    // Distinguish new message interface from ConversationList mode or MailBox mode
    private boolean mCreateNewMessageFromConversationList = false;

    private AddNumbersTask mAddNumbersTask;
    // Whether or not the RCS Service is installed and the Sim is supported RCS.
    private boolean mIsRcsEnabled;

    private static boolean mRcsShareVcard = false;

    private static boolean mRcsShareVcardAddNumber = false;

    // RCS Message API
    private MessageApi mMessageApi;

    // RCS Account API
    private BasicApi mBasicApi;

    // Rcs Capability API
    private CapabilityApi mCapabilityApi;

    // RCS Support API
    private SupportApi mSupportApi;

    // RCS Profile API
    private ProfileApi mProfileApi;

    // RCS GroupChatApi
    private GroupChatApi mGroupChatApi;
    // Uri for rcs switch to mms
    private Uri mRcsAttachmentUri;

    private static long BYTE_TO_KB = 1024;

    private static long MMS_LIMIT_SIZE = 300;

    private RcsEmojiInitialize mRcsEmojiInitialize = null;

    private ComposeMessageFileTransferReceiver mFileTranferReceiver;
    private ComposeMessageCloudFileReceiver mCloudFileReceiver;

    private boolean mIsBurnMessage = false;
    private boolean isDisposeImage = false;
    private boolean mIsEnableSelectCopy = false;

    private long mRcsForwardId = 0;

    private List<Long> mTopThread = new ArrayList<Long>();

    ArrayList<SimpleMessage> mSimpleMsgs = new ArrayList<SimpleMessage>();
    private ProgressDialog mProgressDialog;

    private static int FAVOURITE_MSG = 1;
    private int mRcsBurnAfterReadMessageCount = 0;
    private final IntentFilter mAirplaneModeFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    private final IntentFilter mSIMStatusChangeFilter = new IntentFilter(SIM_STATE_CHANGE_ACTION);
    /* End add for RCS */

    @SuppressWarnings("unused")
    public static void log(String logMsg) {
        Thread current = Thread.currentThread();
        long tid = current.getId();
        StackTraceElement[] stack = current.getStackTrace();
        String methodName = stack[3].getMethodName();
        // Prepend current thread ID and name of calling method to the message.
        logMsg = "[" + tid + "] [" + methodName + "] " + logMsg;
        Log.d(TAG, logMsg);
    }

    //==========================================================
    // Inner classes
    //==========================================================

    private void editSlideshow() {
        final int subjectSize = mWorkingMessage.hasSubject()
                    ? mWorkingMessage.getSubject().toString().getBytes().length : 0;
        // The user wants to edit the slideshow. That requires us to persist the slideshow to
        // disk as a PDU in saveAsMms. This code below does that persisting in a background
        // task. If the task takes longer than a half second, a progress dialog is displayed.
        // Once the PDU persisting is done, another runnable on the UI thread get executed to start
        // the SlideshowEditActivity.
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                // This runnable gets run in a background thread.
                mTempMmsUri = mWorkingMessage.saveAsMms(false);
            }
        }, new Runnable() {
            @Override
            public void run() {
                // Once the above background thread is complete, this runnable is run
                // on the UI thread.
                if (mTempMmsUri == null) {
                    return;
                }
                Intent intent = new Intent(ComposeMessageActivity.this,
                        SlideshowEditActivity.class);
                intent.setData(mTempMmsUri);
                intent.putExtra(MSG_SUBJECT_SIZE, subjectSize);
                startActivityForResult(intent, REQUEST_CODE_CREATE_SLIDESHOW);
            }
        }, R.string.building_slideshow_title);
    }

    private void pickContacts(int mode, int requestCode) {
        Intent intent = new Intent(INTENT_MULTI_PICK_ACTION, Contacts.CONTENT_URI);
        intent.putExtra(MultiPickContactsActivity.MODE, mode);
        startActivityForResult(intent, requestCode);
    }

    private final Handler mAttachmentEditorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AttachmentEditor.MSG_EDIT_SLIDESHOW: {
                    editSlideshow();
                    break;
                }
                case AttachmentEditor.MSG_SEND_SLIDESHOW: {
                    if (isPreparedForSending()) {
                        ComposeMessageActivity.this.confirmSendMessageIfNeeded();
                    }
                    break;
                }
                case AttachmentEditor.MSG_VIEW_IMAGE:
                case AttachmentEditor.MSG_PLAY_VIDEO:
                case AttachmentEditor.MSG_PLAY_AUDIO:
                case AttachmentEditor.MSG_PLAY_SLIDESHOW:
                case AttachmentEditor.MSG_VIEW_VCARD:
                    if (mWorkingMessage.getSlideshow() != null) {
                         viewMmsMessageAttachment(msg.what);
                    }
                    break;

                case AttachmentEditor.MSG_REPLACE_IMAGE:
                case AttachmentEditor.MSG_REPLACE_VIDEO:
                case AttachmentEditor.MSG_REPLACE_AUDIO:
                case AttachmentEditor.MSG_REPLACE_VCARD:
                    if (mAttachmentSelector.getVisibility() == View.VISIBLE
                            && mIsReplaceAttachment) {
                        mAttachmentSelector.setVisibility(View.GONE);
                    } else {
                        showAttachmentSelector(true);
                        Toast.makeText(ComposeMessageActivity.this,
                                R.string.replace_current_attachment, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case AttachmentEditor.MSG_REMOVE_ATTACHMENT:
                    // Update the icon state in attachment selector.
                    if (mAttachmentSelector.getVisibility() == View.VISIBLE
                            && !mIsReplaceAttachment) {
                        showAttachmentSelector(true);
                    }
                    mWorkingMessage.removeAttachment(true);
                    break;

                default:
                    break;
            }
        }
    };


    private void viewMmsMessageAttachment(final int requestCode) {
        SlideshowModel slideshow = mWorkingMessage.getSlideshow();
        if (slideshow == null) {
            throw new IllegalStateException("mWorkingMessage.getSlideshow() == null");
        }
        if (slideshow.isSimple()) {
            MessageUtils.viewSimpleSlideshow(this, slideshow);
        } else {
            // The user wants to view the slideshow. That requires us to persist the slideshow to
            // disk as a PDU in saveAsMms. This code below does that persisting in a background
            // task. If the task takes longer than a half second, a progress dialog is displayed.
            // Once the PDU persisting is done, another runnable on the UI thread get executed to
            // start the SlideshowActivity.
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    // This runnable gets run in a background thread.
                    mTempMmsUri = mWorkingMessage.saveAsMms(false);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Once the above background thread is complete, this runnable is run
                    // on the UI thread.
                    if (mTempMmsUri == null) {
                        return;
                    }

                    SlideshowModel slideshowModel = mWorkingMessage.getSlideshow();
                    if (requestCode == AttachmentEditor.MSG_PLAY_AUDIO &&
                            (slideshowModel != null) && slideshowModel.isSimpleAudio()) {
                        MediaModel mm = slideshowModel.get(0).getAudio();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(mm.getUri(), mm.getContentType());
                        startActivityForResult(intent, requestCode);
                        return;
                     }

                    MessageUtils.launchSlideshowActivity(ComposeMessageActivity.this, mTempMmsUri,
                            requestCode);
                }
            }, R.string.building_slideshow_title);
        }
    }


    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MessageItem msgItem = (MessageItem) msg.obj;
            if (msgItem != null) {
                switch (msg.what) {
                    case MessageListItem.MSG_LIST_DETAILS:
                        showMessageDetails(msgItem);
                        break;

                    case MessageListItem.MSG_LIST_EDIT:
                        editMessageItem(msgItem);
                        drawBottomPanel();
                        break;

                    case MessageListItem.MSG_LIST_RESEND:
                        resendMessage(msgItem);
                        break;

                    case MessageListItem.MSG_LIST_PLAY:
                        switch (msgItem.mAttachmentType) {
                            case WorkingMessage.IMAGE:
                            case WorkingMessage.VIDEO:
                                try {
                                    Intent intent = new Intent(getContext(),
                                            PlayVideoOrPicActivity.class);
                                    intent.putExtra(PlayVideoOrPicActivity.VIDEO_PIC_TYPE,
                                            msgItem.mAttachmentType);
                                    intent.putExtra(PlayVideoOrPicActivity.VIDEO_PIC_PATH,
                                            MessageUtils.getPath(getContext(), msgItem));
                                    startActivity(intent);
                                } catch (IOException e) {
                                    Log.i(TAG,Log.getStackTraceString(e));
                                }
                                break;
                            case WorkingMessage.AUDIO:
                            case WorkingMessage.VCARD:
                            case WorkingMessage.SLIDESHOW:
                                MessageUtils.viewMmsMessageAttachment(ComposeMessageActivity.this,
                                        msgItem.mMessageUri, msgItem.mSlideshow,
                                        getAsyncDialog());
                                break;
                        }
                        break;

                    default:
                        Log.w(TAG, "Unknown message: " + msg.what);
                        return;
                }
            }
        }
    };

    private boolean showMessageDetails(MessageItem msgItem) {
        Cursor cursor = mMsgListAdapter.getCursorForItem(msgItem);
        if (cursor == null) {
            return false;
        }
        int subjectSize = (msgItem.mSubject == null) ? 0 : msgItem.mSubject.getBytes().length;
        int messageSize =  msgItem.mMessageSize + subjectSize;
        if (DEBUG) {
            Log.v(TAG,"showMessageDetails subjectSize = " + subjectSize);
            Log.v(TAG,"showMessageDetails messageSize = " + messageSize);
        }
        String messageDetails = MessageUtils.getMessageDetails(
                ComposeMessageActivity.this, cursor, messageSize);
        mMsgDetailDialog = new AlertDialog.Builder(ComposeMessageActivity.this)
                .setTitle(R.string.message_details_title)
                .setMessage(messageDetails)
                .setCancelable(true)
                .show();
        return true;
    }

    private final OnKeyListener mSubjectKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // When the subject editor is empty, press "DEL" to hide the input field.
            if ((keyCode == KeyEvent.KEYCODE_DEL) && (mSubjectTextEditor.length() == 0)) {
                showSubjectEditor(false);
                mWorkingMessage.setSubject(null, true);
                updateSendButtonState();
                return true;
            }
            return false;
        }
    };

    /**
     * Return the messageItem associated with the type ("mms" or "sms") and message id.
     * @param type Type of the message: "mms" or "sms"
     * @param msgId Message id of the message. This is the _id of the sms or pdu row and is
     * stored in the MessageItem
     * @param createFromCursorIfNotInCache true if the item is not found in the MessageListAdapter's
     * cache and the code can create a new MessageItem based on the position of the current cursor.
     * If false, the function returns null if the MessageItem isn't in the cache.
     * @return MessageItem or null if not found and createFromCursorIfNotInCache is false
     */
    private MessageItem getMessageItem(String type, long msgId,
            boolean createFromCursorIfNotInCache) {
        return mMsgListAdapter.getCachedMessageItem(type, msgId,
                createFromCursorIfNotInCache ? mMsgListAdapter.getCursor() : null);
    }

    private boolean isCursorValid() {
        // Check whether the cursor is valid or not.
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            Log.e(TAG, "Bad cursor.", new RuntimeException());
            return false;
        }
        return true;
    }

    private void resetCounter() {
        mTextCounter.setText("");
        mTextCounter.setVisibility(View.GONE);
        if (mShowTwoButtons) {
            mTextCounterSec.setText("");
            mTextCounterSec.setVisibility(View.GONE);
        }
    }

    private void updateCounter(CharSequence text, int start, int before, int count) {
        WorkingMessage workingMessage = mWorkingMessage;
        if (workingMessage.requiresMms()) {
            // If we're not removing text (i.e. no chance of converting back to SMS
            // because of this change) and we're in MMS mode, just bail out since we
            // then won't have to calculate the length unnecessarily.
            final boolean textRemoved = (before > count);
            if (!textRemoved) {
                if (mShowTwoButtons) {
                    showTwoSmsOrMmsSendButton(workingMessage.requiresMms());
                } else {
                    showSmsOrMmsSendButton(workingMessage.requiresMms());
                }

                return;
            }
        }

        int[] params = SmsMessage.calculateLength(text, false);
            /* SmsMessage.calculateLength returns an int[4] with:
             *   int[0] being the number of SMS's required,
             *   int[1] the number of code units used,
             *   int[2] is the number of code units remaining until the next message.
             *   int[3] is the encoding type that should be used for the message.
             */
        int msgCount = params[0];
        int remainingInCurrentMessage = params[2];
        if (!mIsRcsEnabled) {
            if (!MmsConfig.getMultipartSmsEnabled()) {
                // The provider doesn't support multi-part sms's so as soon as the user types
                // an sms longer than one segment, we have to turn the message into an mms.
                mWorkingMessage.setLengthRequiresMms(msgCount > 1, true);
            } else {
                int threshold = MmsConfig.getSmsToMmsTextThreshold(ComposeMessageActivity.this);
                mWorkingMessage.setLengthRequiresMms(threshold > 0 && msgCount > threshold, true);
            }
        }

        // Show the counter only if:
        // - We are not in MMS mode
        // - We are going to send more than one message OR we are getting close
        boolean showCounter = false;
        if (!workingMessage.requiresMms() &&
                (msgCount > 1 ||
                 remainingInCurrentMessage <= CHARS_REMAINING_BEFORE_COUNTER_SHOWN)) {
            showCounter = true;
        }

        if (mShowTwoButtons) {
            showTwoSmsOrMmsSendButton(workingMessage.requiresMms());
        } else {
            showSmsOrMmsSendButton(workingMessage.requiresMms());
        }

        if (showCounter) {
            // Update the remaining characters and number of messages required.
            String counterText = msgCount > 1 ? remainingInCurrentMessage + " / " + msgCount
                    : String.valueOf(remainingInCurrentMessage);
            mTextCounter.setText(counterText);
            mTextCounter.setVisibility(View.VISIBLE);
            if (mShowTwoButtons) {
                mTextCounterSec.setText(counterText);
                mTextCounterSec.setVisibility(View.VISIBLE);
            }
        } else {
            mTextCounter.setVisibility(View.GONE);
            if (mShowTwoButtons) {
                mTextCounterSec.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode)
    {
        // requestCode >= 0 means the activity in question is a sub-activity.
        if (requestCode >= 0) {
            mWaitingForSubActivity = true;
        }
        // The camera and other activities take a long time to hide the keyboard so we pre-hide
        // it here. However, if we're opening up the quick contact window while typing, don't
        // mess with the keyboard.
        if (mIsKeyboardOpen && !QuickContact.ACTION_QUICK_CONTACT.equals(intent.getAction())) {
            hideKeyboard();
        }

        super.startActivityForResult(intent, requestCode);
    }

    private void showConvertToMmsToast() {
        Toast.makeText(this, R.string.converting_to_picture_message, Toast.LENGTH_SHORT).show();
    }

    private void showConvertToSmsToast() {
        Toast.makeText(this, R.string.converting_to_text_message, Toast.LENGTH_SHORT).show();
    }

    private class DeleteMessageListener implements OnClickListener {
        private final MessageItem mMessageItem;

        public DeleteMessageListener(MessageItem messageItem) {
            mMessageItem = messageItem;
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            dialog.dismiss();

            new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... none) {
                    if (mMessageItem.isMms()) {
                        WorkingMessage.removeThumbnailsFromCache(mMessageItem.getSlideshow());

                        MmsApp.getApplication().getPduLoaderManager()
                            .removePdu(mMessageItem.mMessageUri);
                        // Delete the message *after* we've removed the thumbnails because we
                        // need the pdu and slideshow for removeThumbnailsFromCache to work.
                    }
                    Boolean deletingLastItem = false;
                    Cursor cursor = mMsgListAdapter != null ? mMsgListAdapter.getCursor() : null;
                    if (cursor != null) {
                        cursor.moveToLast();
                        long msgId = cursor.getLong(COLUMN_ID);
                        deletingLastItem = msgId == mMessageItem.mMsgId;
                    }
                    mBackgroundQueryHandler.startDelete(DELETE_MESSAGE_TOKEN,
                            deletingLastItem, mMessageItem.mMessageUri,
                            mMessageItem.mLocked ? null : "locked=0", null);
                    return null;
                }
            }.execute();
        }
    }

    private class DiscardDraftListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            mWorkingMessage.discard();
            dialog.dismiss();
            finish();
        }
    }

    private class SendIgnoreInvalidRecipientListener implements OnClickListener {
        private int mSubscription = MessageUtils.SUB_INVALID;

        public SendIgnoreInvalidRecipientListener(int subscription) {
             mSubscription = subscription;
        }

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            if (isMmsWithMobileDataOff(mWorkingMessage.requiresMms(), mSubscription)) {
                showMobileDataDisabledDialog(mSubscription);
            } else if ((TelephonyManager.getDefault().getPhoneCount()) > 1) {
                int subId = (mSubscription == MessageUtils.SUB_INVALID)
                        ? SubscriptionManager.getDefaultSmsSubscriptionId()
                        : mSubscription;
                sendMsimMessage(true, subId);
            } else {
                sendMessage(true);
            }
            dialog.dismiss();
        }
    }

    private class CancelSendingListener implements OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            if (isRecipientsEditorVisible()) {
                mRecipientsEditor.requestFocus();
            }
            dialog.dismiss();
        }
    }

    private void dismissMsimDialog() {
        if (mMsimDialog != null && mMsimDialog.isShowing()) {
            mMsimDialog.dismiss();
        }
    }

    private void launchMsimDialog(final boolean bCheckEcmMode, final boolean isMms) {
        if (mMsimDialog != null && mMsimDialog.isShowing()) {
            mMsimDialog.dismiss();
        }
        LogTag.debugD("LaunchMsimDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(ComposeMessageActivity.this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.multi_sim_sms_sender,
                              (ViewGroup)findViewById(R.id.layout_root));
        builder.setView(layout);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK: {
                            dismissMsimDialog();
                            return true;
                        }
                        case KeyEvent.KEYCODE_SEARCH: {
                            return true;
                        }
                    }
                    return false;
                }
            }
        );

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dismissMsimDialog();
            }
        });

        ContactList recipients = isRecipientsEditorVisible() ?
            mRecipientsEditor.constructContactsFromInput(false) : getRecipients();
        builder.setTitle(getResources().getString(R.string.to_address_label)
                + recipients.formatNamesAndNumbers(","));

        mMsimDialog = builder.create();
        mMsimDialog.setCanceledOnTouchOutside(true);

        int[] smsBtnIds = {R.id.BtnSimOne, R.id.BtnSimTwo, R.id.BtnSimThree};
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        Button[] smsBtns = new Button[phoneCount];
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(
                getApplicationContext()).getActiveSubscriptionInfoList();
        String displayName = null;
        for (int i = 0; i < phoneCount; i++) {
            final int phoneId = i;
            smsBtns[i] = (Button) layout.findViewById(smsBtnIds[i]);
            smsBtns[i].setVisibility(View.VISIBLE);
            if(subInfoList != null) {
                displayName = "SIM " + (phoneId + 1);
                for (SubscriptionInfo info : subInfoList) {
                    if (info.getSimSlotIndex() == phoneId) {
                        displayName = (phoneId + 1) + ": " + info.getDisplayName().toString();
                        break;
                    }
                }
            }
            smsBtns[i].setText(displayName);
            if (mIsRcsEnabled && (mConversation.isGroupChat() || mConversation.isPcChat())) {
                int rcsOnlineSlot = RcsDualSimMananger.getCurrentRcsOnlineSlot();
                if (rcsOnlineSlot != i) {
                    smsBtns[i].setEnabled(false);
                }
            }
            smsBtns[i].setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            dismissMsimDialog();
                            int subId = SubscriptionManager.getSubId(phoneId)[0];
                            LogTag.debugD("LaunchMsimDialog: subscription selected " + subId);
                            if (!SubscriptionManager.from(getApplicationContext())
                                    .isActiveSubId(subId)) {
                                Toast.makeText(ComposeMessageActivity.this,
                                        getString(R.string.send_via_invalid_sub),
                                        Toast.LENGTH_LONG).show();
                            } else if (isMmsWithMobileDataOff(isMms, subId)) {
                                showMobileDataDisabledDialog(subId);
                            } else {
                                sendMsimMessage(bCheckEcmMode, subId);
                            }
                        }
                    });
        }
        mMsimDialog.show();
    }

    private void sendMsimMessage(boolean bCheckEcmMode, int subscription) {
        LogTag.debugD("sendMsimMessage subscription: " + subscription);
        mWorkingMessage.setWorkingMessageSub(subscription);
        sendMessage(bCheckEcmMode);
    }

    private boolean isLTEOnlyMode() {
        try {
            int tddOnly = Settings.Global.getInt(getContentResolver(), LTE_DATA_ONLY_KEY);
            int network = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE);
            return network == RILConstants.NETWORK_MODE_LTE_ONLY && tddOnly == LTE_DATA_ONLY_MODE;
        } catch (SettingNotFoundException snfe) {
            Log.w(TAG, "isLTEOnlyMode: Could not find PREFERRED_NETWORK_MODE!");
        }
        return false;
    }

    private boolean isLTEOnlyMode(int subscription) {
        try {
            int tddOnly = TelephonyManager.getIntAtIndex(getContentResolver(),
                    LTE_DATA_ONLY_KEY, subscription);
            int network = TelephonyManager.getIntAtIndex(getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE, subscription);
            return network == RILConstants.NETWORK_MODE_LTE_ONLY && tddOnly == LTE_DATA_ONLY_MODE;
        } catch (SettingNotFoundException snfe) {
            Log.w(TAG, "isLTEOnlyMode: Could not find PREFERRED_NETWORK_MODE!");
        }
        return false;
    }

    private void showDisableLTEOnlyDialog(int subscription) {
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_LTE_DATA_ONLY_DIALOG);
        intent.putExtra(PhoneConstants.SLOT_KEY, subscription);
        startActivity(intent);
    }

    private boolean canSendMmsMobileDataOff(int subscription) {
        boolean result = false;
        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(subscription);
        if (b != null) {
            result = b.getBoolean("config_enable_mms_with_mobile_data_off");
        }
        LogTag.debug("canSendMmsMobileDataOff result: " + result);
        return result;
    }

    private void confirmSendMessageIfNeeded(int subscription) {
        if (isLTEOnlyMode(subscription)) {
            showDisableLTEOnlyDialog(subscription);
            return;
        }
        boolean isMms = mWorkingMessage.requiresMms();
        if (!isRecipientsEditorVisible()) {
            if (isMmsWithMobileDataOff(isMms, subscription)) {
                showMobileDataDisabledDialog(subscription);
            } else {
                sendMsimMessage(true, subscription);
            }
            return;
        }

        if (mRecipientsEditor.hasInvalidRecipient(isMms)) {
            showInvalidRecipientDialog(subscription);
        } else if (isMmsWithMobileDataOff(isMms, subscription)) {
            showMobileDataDisabledDialog(subscription);
        } else {
            if (!TextUtils.isEmpty(getString(R.string.mms_recipient_Limit))
                    && isMms
                    && checkForMmsRecipients(getString(R.string.mms_recipient_Limit), true)) {
                return;
            }
            // The recipients editor is still open. Make sure we use what's showing there
            // as the destination.
            ContactList contacts = mRecipientsEditor.constructContactsFromInput(false);
            mDebugRecipients = contacts.serialize();
            sendMsimMessage(true, subscription);
        }
    }

    private void confirmSendMessageIfNeeded() {
        LogTag.debugD("confirmSendMessageIfNeeded");
        if (mRcsShareVcard) {
            mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VCARD);
            mRcsShareVcard = false;
        }

        int slot = SubscriptionManager.getSlotId(
                SmsManager.getDefault().getDefaultSmsSubscriptionId());
        if ((TelephonyManager.getDefault().isMultiSimEnabled() &&
                isLTEOnlyMode(slot))
                || (!TelephonyManager.getDefault().isMultiSimEnabled()
                        && isLTEOnlyMode())) {
            showDisableLTEOnlyDialog(slot);
            LogTag.debugD("return for disable LTEOnly");
            return;
        }

        boolean isMms = mWorkingMessage.requiresMms();
        int defaultSubId = SubscriptionManager.getDefaultSmsSubscriptionId();
        if (!isRecipientsEditorVisible()) {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                if ((TelephonyManager.getDefault().getPhoneCount()) > 1 &&
                        MessageUtils.isMsimIccCardActive()) {
                    if(SmsManager.getDefault().isSMSPromptEnabled()) {
                        launchMsimDialog(true, isMms);
                    } else {
                        sendMsimMessageNotPrompt(true, isMms, defaultSubId);
                    }
                } else {
                    sendMsimMessageNotPrompt(true, isMms, defaultSubId);
                }
            } else if (isMmsWithMobileDataOff(isMms, defaultSubId)) {
                showMobileDataDisabledDialog();
            } else {
                sendMessage(true);
            }
            return;
        }

        if (mRecipientsEditor.hasInvalidRecipient(isMms)) {
            showInvalidRecipientDialog();
        } else if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if ((TelephonyManager.getDefault().getPhoneCount()) > 1 &&
                    MessageUtils.isMsimIccCardActive()) {
                if(SmsManager.getDefault().isSMSPromptEnabled()) {
                    launchMsimDialog(true, isMms);
                } else {
                    sendMsimMessageNotPrompt(true, isMms, defaultSubId);
                }
            } else {
                sendMsimMessageNotPrompt(true, isMms, defaultSubId);
            }
        } else if (isMmsWithMobileDataOff(isMms, defaultSubId)) {
            showMobileDataDisabledDialog(defaultSubId);
        } else {
            if (!TextUtils.isEmpty(getString(R.string.mms_recipient_Limit))
                    && isMms
                    && checkForMmsRecipients(getString(R.string.mms_recipient_Limit), true)) {
                return;
            }
            // The recipients editor is still open. Make sure we use what's showing there
            // as the destination.
            ContactList contacts = mRecipientsEditor.constructContactsFromInput(false);
            mDebugRecipients = contacts.serialize();
            sendMessage(true);
        }
    }

    private void showInvalidRecipientDialog() {
        showInvalidRecipientDialog(MessageUtils.SUB_INVALID);
    }

    private void showInvalidRecipientDialog(int subscription) {
        boolean isMms = mWorkingMessage.requiresMms();
        if (mRecipientsEditor.getValidRecipientsCount(isMms)
                > MessageUtils.ALL_RECIPIENTS_INVALID) {
            String title = getResourcesString(R.string.has_invalid_recipient,
                    mRecipientsEditor.formatInvalidNumbers(isMms));
            mInvalidRecipientDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(R.string.invalid_recipient_message)
                    .setPositiveButton(R.string.try_to_send,
                            new SendIgnoreInvalidRecipientListener(subscription))
                    .setNegativeButton(R.string.no, new CancelSendingListener())
                    .show();
        } else {
            mInvalidRecipientDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.cannot_send_message)
                    .setMessage(R.string.cannot_send_message_reason)
                    .setPositiveButton(R.string.yes, new CancelSendingListener())
                    .show();
        }
    }

    private void sendMsimMessageNotPrompt(boolean bCheckEcmMode, boolean isMms, int subId) {
        if (isMmsWithMobileDataOff(isMms, subId)) {
            showMobileDataDisabledDialog(subId);
        } else {
            sendMsimMessage(bCheckEcmMode, subId);
        }
    }

    private boolean isMmsWithMobileDataOff(boolean isMms, int subscription) {
        LogTag.debug("isMmsWithMobileDataOff subscription: " + subscription);
        return isMms && !mSendMmsSupportViaWiFi && canSendMmsMobileDataOff(subscription)
                && !MessageUtils.isMobileDataEnabled(getApplicationContext(), subscription);
    }

    private void showMobileDataDisabledDialog() {
        showMobileDataDisabledDialog(MessageUtils.SUB_INVALID);
    }

    private void showMobileDataDisabledDialog(final int subscription) {
        LogTag.debug("showMobileDataDisabledDialog subscription: " + subscription);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send);
        builder.setMessage(getString(R.string.mobile_data_disable));
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if ((TelephonyManager.getDefault().getPhoneCount()) > 1
                        && (subscription != MessageUtils.SUB_INVALID)) {
                    sendMsimMessage(true, subscription);
                } else {
                    sendMessage(true);
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private final TextWatcher mRecipientsWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a workaround for bug 1609057.  Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Bug 1474782 describes a situation in which we send to
            // the wrong recipient.  We have been unable to reproduce this,
            // but the best theory we have so far is that the contents of
            // mRecipientList somehow become stale when entering
            // ComposeMessageActivity via onNewIntent().  This assertion is
            // meant to catch one possible path to that, of a non-visible
            // mRecipientsEditor having its TextWatcher fire and refreshing
            // mRecipientList with its stale contents.
            if (!isRecipientsEditorVisible()) {
                IllegalStateException e = new IllegalStateException(
                        "afterTextChanged called with invisible mRecipientsEditor");
                // Make sure the crash is uploaded to the service so we
                // can see if this is happening in the field.
                Log.w(TAG,
                     "RecipientsWatcher: afterTextChanged called with invisible mRecipientsEditor");
                return;
            }
            if (recipientCount() <= 1 && !TextUtils.isEmpty(s.toString())
                    && !mRecipientsEditor.containsMultiContacts(new SpannableString(s + "1"))) {
                mClearButton.setVisibility(View.VISIBLE);
                mRecipientsPicker.setVisibility(View.GONE);
            } else {
                mClearButton.setVisibility(View.GONE);
                mRecipientsPicker.setVisibility(View.VISIBLE);
            }

            mWorkingMessage.setWorkingRecipients(mRecipientsEditor.getNumbers());
            mWorkingMessage.setHasEmail(mRecipientsEditor.containsEmail(), true);

            checkForTooManyRecipients();

            // If pick recipients from Contacts,
            // then only update title once when process finished
            if (mIsProcessPickedRecipients) {
                 return;
            }

            if (mRecipientsPickList != null) {
                // Update UI with mRecipientsPickList, which is picked from
                // People.
                updateTitle(mRecipientsPickList);
                mRecipientsPickList = null;
            } else {
                updateTitleForRecipientsChange(s);
            }

            // If we have gone to zero recipients, disable send button.
            updateSendButtonState();
        }
    };

    private void updateTitleForRecipientsChange(Editable s) {
        // If we have gone to zero recipients, we need to update the title.
        if (TextUtils.isEmpty(s.toString().trim())) {
            constructContactAndUpdateTitle();
        }

        // Walk backwards in the text box, skipping spaces. If the last
        // character is a comma, update the title bar.
        for (int pos = s.length() - 1; pos >= 0; pos--) {
            char c = s.charAt(pos);
            if (c == ' ')
                continue;

            if (c == ',') {
                constructContactAndUpdateTitle();
            }
            break;
        }

    }

    private void constructContactAndUpdateTitle() {
        ContactList contacts = mRecipientsEditor.constructContactsFromInput(false);
        updateTitle(contacts);
    }

    private boolean checkForMmsRecipients(String strLimit, boolean isMmsSend) {
        if (mWorkingMessage.requiresMms()) {
            int recipientLimit = Integer.parseInt(strLimit);
            final int currentRecipients = recipientCount();
            if (recipientLimit < currentRecipients) {
                if (currentRecipients != mLastRecipientCount || isMmsSend) {
                    mLastRecipientCount = currentRecipients;
                    String tooManyMsg = getString(R.string.too_many_recipients, currentRecipients,
                            recipientLimit);
                    Toast.makeText(ComposeMessageActivity.this,
                             tooManyMsg, Toast.LENGTH_LONG).show();
                }
                return true;
            } else {
                mLastRecipientCount = currentRecipients;
            }
        }
        return false;
    }

    private void checkForTooManyRecipients() {
        if (!TextUtils.isEmpty(getString(R.string.mms_recipient_Limit))
                && checkForMmsRecipients(getString(R.string.mms_recipient_Limit), false)) {
            return;
        }
        final int recipientLimit = MmsConfig.getRecipientLimit();
        if (recipientLimit != Integer.MAX_VALUE && recipientLimit > 0) {
            final int recipientCount = recipientCount();
            boolean tooMany = recipientCount > recipientLimit;
            int index = mRecipientsEditor.getSelectionStart();
            Editable editable = mRecipientsEditor.getText();

            if (recipientCount != mLastRecipientCount) {
                // Don't warn the user on every character they type when they're over the limit,
                // only when the actual # of recipients changes.
                mLastRecipientCount = recipientCount;
                if (tooMany && (index > 0)) {
                    String tooManyMsg = getString(R.string.too_many_recipients, recipientCount,
                            recipientLimit);
                    Toast.makeText(ComposeMessageActivity.this,
                            tooManyMsg, Toast.LENGTH_LONG).show();
                    editable.delete(index-1 , index);
                }
            }
        }
    }

    private final class RecipientsMenuClickListener implements MenuItem.OnMenuItemClickListener {
        private final Contact mRecipient;

        RecipientsMenuClickListener(Contact recipient) {
            mRecipient = recipient;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (null == mRecipient) {
                return false;
            }

            switch (item.getItemId()) {
                // Context menu handlers for the recipients editor.
                case MENU_VIEW_CONTACT: {
                    Uri contactUri = mRecipient.getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                    return true;
                }
                case MENU_ADD_TO_CONTACTS: {
                    mAddContactIntent = ConversationList.createAddContactIntent(
                            mRecipient.getNumber());
                    ComposeMessageActivity.this.startActivityForResult(mAddContactIntent,
                            REQUEST_CODE_ADD_CONTACT);
                    return true;
                }
                case MENU_COPY_PHONENUMBER: {
                    copyToClipboard(mRecipient.getNumber());
                    return true;
                }
            }
            return false;
        }
    }

    private boolean canAddToContacts(Contact contact) {
        // There are some kind of automated messages, like STK messages, that we don't want
        // to add to contacts. These names begin with special characters, like, "*Info".
        final String name = contact.getName();
        if (!TextUtils.isEmpty(contact.getNumber())) {
            char c = contact.getNumber().charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!TextUtils.isEmpty(name)) {
            char c = name.charAt(0);
            if (isSpecialChar(c)) {
                return false;
            }
        }
        if (!(Mms.isEmailAddress(name) ||
                Telephony.Mms.isPhoneNumber(name) ||
                contact.isMe())) {
            return false;
        }
        return true;
    }

    private boolean isSpecialChar(char c) {
        return c == '*' || c == '%' || c == '$';
    }

    private Uri getSelectedUriFromMessageList(ListView listView, int position) {
        // If the context menu was opened over a uri, get that uri.
        MessageListItem msglistItem = (MessageListItem) listView.getChildAt(position);
        if (msglistItem == null) {
            // FIXME: Should get the correct view. No such interface in ListView currently
            // to get the view by position. The ListView.getChildAt(position) cannot
            // get correct view since the list doesn't create one child for each item.
            // And if setSelection(position) then getSelectedView(),
            // cannot get corrent view when in touch mode.
            return null;
        }

        TextView textView;
        CharSequence text = null;
        int selStart = -1;
        int selEnd = -1;

        //check if message sender is selected
        textView = (TextView) msglistItem.getBodyTextView();
        if (textView != null) {
            text = textView.getText();
            selStart = textView.getSelectionStart();
            selEnd = textView.getSelectionEnd();
        }

        // Check that some text is actually selected, rather than the cursor
        // just being placed within the TextView.
        if (selStart != selEnd) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            URLSpan[] urls = ((Spanned) text).getSpans(min, max,
                                                        URLSpan.class);

            if (urls.length == 1) {
                return Uri.parse(urls[0].getURL());
            }
        }

        //no uri was selected
        return null;
    }

    private Uri getContactUriForEmail(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Email.CONTACT_ID, Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    if (!TextUtils.isEmpty(name)) {
                        return ContentUris.withAppendedId(Contacts.CONTENT_URI, cursor.getLong(0));
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private Uri getContactUriForPhoneNumber(String phoneNumber) {
        Contact contact = Contact.get(phoneNumber, false);
        if (contact.existsInDatabase()) {
            return contact.getUri();
        }
        return null;
    }

    private void editMessageItem(MessageItem msgItem) {
        if ("sms".equals(msgItem.mType)) {
            editSmsMessageItem(msgItem);
        } else {
            editMmsMessageItem(msgItem);
        }
        if (msgItem.isFailedMessage() && mMsgListAdapter.getCount() <= 1) {
            // For messages with bad addresses, let the user re-edit the recipients.
            initRecipientsEditor();
        }
    }

    private void editSmsMessageItem(MessageItem msgItem) {
        editSmsMessageItem(msgItem.mMsgId, msgItem.mBody);
    }

    private void editSmsMessageItem(long msgId, String msgBody) {
        // When the message being edited is the only message in the conversation, the delete
        // below does something subtle. The trigger "delete_obsolete_threads_pdu" sees that a
        // thread contains no messages and silently deletes the thread. Meanwhile, the mConversation
        // object still holds onto the old thread_id and code thinks there's a backing thread in
        // the DB when it really has been deleted. Here we try and notice that situation and
        // clear out the thread_id. Later on, when Conversation.ensureThreadId() is called, we'll
        // create a new thread if necessary.
        synchronized(mConversation) {
            if (mConversation.getMessageCount() <= 1) {
                mConversation.clearThreadId();
                MessagingNotification.setCurrentlyDisplayedThreadId(
                    MessagingNotification.THREAD_NONE);
            }
        }
        // Delete the old undelivered SMS and load its content.
        Uri uri = ContentUris.withAppendedId(Sms.CONTENT_URI, msgId);
        int count = SqliteWrapper.delete(ComposeMessageActivity.this,
                mContentResolver, uri, null, null);

        mWorkingMessage.setText(msgBody);

        // if the ListView only has one message and delete the message success
        // the uri of conversation will be null, so it can't qurey info from DB,
        // so the mMsgListAdapter should change Cursor to null
        if (count > 0) {
            if (mMsgListAdapter.getCount() == MSG_ONLY_ONE_FAIL_LIST_ITEM) {
                mMsgListAdapter.changeCursor(null);
            }
        }
    }

    private void editMmsMessageItem(MessageItem msgItem) {
        editMmsMessageItem(msgItem.mMessageUri, msgItem.mSubject);
    }

    private void editMmsMessageItem(Uri uri, String subject) {
        // Load the selected message in as the working message.
        WorkingMessage newWorkingMessage = WorkingMessage.load(this, uri);
        if (newWorkingMessage == null) {
            return;
        }

        // Discard the current message in progress.
        mWorkingMessage.discard();

        mWorkingMessage = newWorkingMessage;
        mWorkingMessage.setConversation(mConversation);

        drawTopPanel(false);

        // WorkingMessage.load() above only loads the slideshow. Set the
        // subject here because we already know what it is and avoid doing
        // another DB lookup in load() just to get it.
        mWorkingMessage.setSubject(subject, false);

        if (mWorkingMessage.hasSubject()) {
            showSubjectEditor(true);
        }
    }

    private void copyToClipboard(String str) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, str));
    }

    private void resendMessage(MessageItem msgItem) {
        if (SmsManager.getDefault().isSMSPromptEnabled()) {
            mWorkingMessage.setWorkingMessageSub(msgItem.mSubId);
        }
        if (msgItem.isMms()) {
            // If it is mms, we delete current mms and use current mms
            // uri to create new working message object.
            WorkingMessage newWorkingMessage = WorkingMessage.load(this, msgItem.mMessageUri);
            if (newWorkingMessage == null)
                return;

            // Discard the current message in progress.
            mWorkingMessage.discard();

            mWorkingMessage = newWorkingMessage;
            mWorkingMessage.setConversation(mConversation);
            mWorkingMessage.setSubject(msgItem.mSubject, false);
        } else {
            if (getRecipients().size() > 1) {
                // If the number is more than one when send sms, there will show serveral msg items
                // the recipient of msg item is not equal with recipients of conversation
                // so we should record the recipient of this msg item.
                mWorkingMessage.setResendMultiRecipients(true);
                mResendSmsRecipient = msgItem.mAddress;
            }

            editSmsMessageItem(msgItem);
        }

        sendMessage(true);
    }

    private boolean isAllowForwardMessage(MessageItem msgItem) {
        if (msgItem.getSlideshow() == null) {
            return false;
        }
        int messageSize = msgItem.getSlideshow().getTotalMessageSize();
        int smilSize = msgItem.getSlideshow().getSMILSize();
        int forwardStrSize = getString(R.string.forward_prefix).getBytes().length;
        int subjectSize =  (msgItem.mSubject == null) ? 0 : msgItem.mSubject.getBytes().length;
        int totalSize = messageSize + forwardStrSize + subjectSize + smilSize;
        if (DEBUG) {
            Log.e(TAG,"isAllowForwardMessage messageSize = "+ messageSize
                    + ", forwardStrSize = "+forwardStrSize+ ", subjectSize = "+subjectSize
                    + ", totalSize = " + totalSize);
        }
        return totalSize <= (MmsConfig.getMaxMessageSize() - SlideshowModel.SLIDESHOW_SLOP);
    }

    /**
     * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
     * @param msgId
     */
    private boolean haveSomethingToCopyToSDCard(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            Log.e(TAG, "haveSomethingToCopyToSDCard can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        boolean result = false;
        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("[CMA] haveSomethingToCopyToSDCard: part[" + i + "] contentType=" + type);
            }

            if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                    ContentType.isAudioType(type) || DrmUtils.isDrmType(type)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Copies media from an Mms to the DrmProvider
     * @param msgId
     */
    private boolean saveRingtone(long msgId) {
        boolean result = true;
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            Log.e(TAG, "copyToDrmProvider can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type)) {
                // All parts (but there's probably only a single one) have to be successful
                // for a valid result.
                result &= copyPart(part, Long.toHexString(msgId));
            }
        }
        return result;
    }

    /**
     * Returns true if any part is drm'd audio with ringtone rights.
     * @param msgId
     * @return true if one of the parts is drm'd audio with rights to save as a ringtone.
     */
    private boolean isDrmRingtoneWithRights(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            Log.e(TAG, "isDrmRingtoneWithRights can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type)) {
                String mimeType = MmsApp.getApplication().getDrmManagerClient()
                        .getOriginalMimeType(part.getDataUri());
                if (ContentType.isAudioType(mimeType) && DrmUtils.haveRightsForAction(part.getDataUri(),
                        DrmStore.Action.RINGTONE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if all drm'd parts are forwardable.
     * @param msgId
     * @return true if all drm'd parts are forwardable.
     */
    private boolean isForwardable(long msgId) {
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            Log.e(TAG, "getDrmMimeType can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (DrmUtils.isDrmType(type) && !DrmUtils.haveRightsForAction(part.getDataUri(),
                        DrmStore.Action.TRANSFER)) {
                    return false;
            }
        }
        return true;
    }

    private int getDrmMimeMenuStringRsrc(long msgId) {
        if (isDrmRingtoneWithRights(msgId)) {
            return R.string.save_ringtone;
        }
        return 0;
    }

    private int getDrmMimeSavedStringRsrc(long msgId, boolean success) {
        if (isDrmRingtoneWithRights(msgId)) {
            return success ? R.string.saved_ringtone : R.string.saved_ringtone_fail;
        }
        return 0;
    }

    /**
     * Copies media from an Mms to the "download" directory on the SD card. If any of the parts
     * are audio types, drm'd or not, they're copied to the "Ringtones" directory.
     * @param msgId
     */
    private boolean copyMedia(long msgId) {
        boolean result = true;
        PduBody body = null;
        try {
            body = SlideshowModel.getPduBody(this,
                        ContentUris.withAppendedId(Mms.CONTENT_URI, msgId));
        } catch (MmsException e) {
            Log.e(TAG, "copyMedia can't load pdu body: " + msgId);
        }
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);

            // all parts have to be successful for a valid result.
            result &= copyPart(part, Long.toHexString(msgId));
        }
        return result;
    }

    private boolean copyPart(PduPart part, String fallback) {
        Uri uri = part.getDataUri();
        String type = new String(part.getContentType());
        boolean isDrm = DrmUtils.isDrmType(type);
        if (isDrm) {
            type = MmsApp.getApplication().getDrmManagerClient()
                    .getOriginalMimeType(part.getDataUri());
        }
        if (!ContentType.isImageType(type)
                && !ContentType.isVideoType(type)
                && !ContentType.isAudioType(type)
                && !(ContentType.TEXT_VCARD.toLowerCase().equals(type.toLowerCase()))
                && !(ContentType.AUDIO_OGG.toLowerCase().equals(type.toLowerCase()))) {
            return true;    // we only save pictures, videos, and sounds. Skip the text parts,
                            // the app (smil) parts, and other type that we can't handle.
                            // Return true to pretend that we successfully saved the part so
                            // the whole save process will be counted a success.
        }
        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                String fileName;
                if (location == null) {
                    // Use fallback name.
                    fileName = fallback;
                } else {
                    // For locally captured videos, fileName can end up being something like this:
                    //      /mnt/sdcard/Android/data/com.android.mms/cache/.temp1.3gp
                    fileName = new String(location);
                }
                File originalFile = new File(fileName);
                fileName = originalFile.getName();  // Strip the full path of where the "part" is
                                                    // stored down to just the leaf filename.

                // Depending on the location, there may be an
                // extension already on the name or not. If we've got audio, put the attachment
                // in the Ringtones directory.
                String dir = Environment.getExternalStorageDirectory() + "/"
                                + (ContentType.isAudioType(type) ? Environment.DIRECTORY_RINGTONES :
                                    Environment.DIRECTORY_DOWNLOADS)  + "/";
                String extension;
                int index;
                if ((index = fileName.lastIndexOf('.')) == -1) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                } else {
                    extension = fileName.substring(index + 1, fileName.length());
                    fileName = fileName.substring(0, index);
                }
                if (isDrm) {
                    extension += DrmUtils.getConvertExtension(type);
                }
                // Remove leading periods. The gallery ignores files starting with a period.
                fileName = fileName.replaceAll("^.", "");

                File file = getUniqueDestination(dir + fileName, extension);

                // make sure the path is valid and directories created for this file.
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    Log.e(TAG, "[MMS] copyPart: mkdirs for " + parentFile.getPath() + " failed!");
                    return false;
                }

                fout = new FileOutputStream(file);

                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size=fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    private File getUniqueDestination(String base, String extension) {
        File file = new File(base + "." + extension);

        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
    }

    private void showDeliveryReport(long messageId, String type) {
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra(MESSAGE_ID, messageId);
        intent.putExtra(MESSAGE_TYPE, type);
        startActivity(intent);
    }

    private final IntentFilter mHttpProgressFilter = new IntentFilter(PROGRESS_STATUS_ACTION);

    private final BroadcastReceiver mHttpProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PROGRESS_STATUS_ACTION.equals(intent.getAction())) {
                long token = intent.getLongExtra("token",
                                    SendingProgressTokenManager.NO_TOKEN);
                if (token != mConversation.getThreadId()) {
                    return;
                }

                int progress = intent.getIntExtra("progress", 0);
                switch (progress) {
                    case PROGRESS_START:
                        setProgressBarVisibility(true);
                        break;
                    case PROGRESS_ABORT:
                    case PROGRESS_COMPLETE:
                        setProgressBarVisibility(false);
                        break;
                    default:
                        setProgress(100 * progress);
                }
            }
        }
    };

    private final BroadcastReceiver mMediaStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "mMediaStateReceiver action = " + intent.getAction());
            checkAttachFileState(context);
        }
    };

    private static ContactList sEmptyContactList;

    private ContactList getRecipients() {
        // If the recipients editor is visible, the conversation has
        // not really officially 'started' yet.  Recipients will be set
        // on the conversation once it has been saved or sent.  In the
        // meantime, let anyone who needs the recipient list think it
        // is empty rather than giving them a stale one.
        if (isRecipientsEditorVisible()) {
            if (sEmptyContactList == null) {
                sEmptyContactList = new ContactList();
            }
            return sEmptyContactList;
        }
        return mConversation.getRecipients();
    }

    private void updateTitle(ContactList list) {
        String title = null;
        String subTitle = null;
        if (MmsConfig.isRcsVersion() && (mConversation.isPcChat() || mConversation.isGroupChat())) {
            if (mConversation.isPcChat() && list.size() != 0) {
                String number = list.get(0).getNumber().trim().replace(" ", "");
                if(RcsUtils.isMyAccount(number)){
                    title = getString(R.string.rcs_to_pc_conversion)
                            + getString(R.string.rcs_online);
                    mConversation.setMyPcConversation(true);
                } else {
                    title = getString(R.string.rcs_to_pc_conversion)
                            + getString(R.string.rcs_offline);
                    mConversation.setMyPcConversation(false);
                }
            } else if (mConversation.isGroupChat()) {
                GroupChat groupChat = mConversation.getGroupChat();
                if (groupChat != null) {
                    title = RcsUtils.getDisplayName(groupChat);
                } else if (!mSentMessage) {
                    title = getString(R.string.new_group_chat);
                } else {
                    title = getString(R.string.group_chat);
                }
                subTitle = getString(R.string.group_chat) + mConversation.getGroupChatStatusText();
            }
        } else {
            int cnt = list.size();
            switch (cnt) {
                case 0: {
                    String recipient = null;
                    if (mRecipientsEditor != null) {
                        recipient = mRecipientsEditor.getText().toString();
                    }
                    if (MessageUtils.isWapPushNumber(recipient)) {
                        String[] mAddresses = recipient.split(":");
                        title = mAddresses[getResources().getInteger(
                                R.integer.wap_push_address_index)];
                    } else {
                        title = TextUtils.isEmpty(recipient)
                                ? getString(R.string.new_message) : recipient;
                    }
                    break;
                }
                case 1: {
                    title = list.get(0).getName();      // get name returns the number if there's no
                                                        // name available.
                    String number = list.get(0).getNumber();
                    if (MessageUtils.isWapPushNumber(number)) {
                        String[] mTitleNumber = number.split(":");
                        number = mTitleNumber[getResources().getInteger(
                                R.integer.wap_push_address_index)];
                    }
                    if (MessageUtils.isWapPushNumber(title)) {
                        String[] mTitle = title.split(":");
                        title = mTitle[getResources().getInteger(R.integer.wap_push_address_index)];
                    }

                if (mTextCounter.isLayoutRtl() && !TextUtils.isEmpty(number.trim())) {

                        // Change the phonenumber display normally for RTL.
                        if (title.equals(number)) {
                            title = PhoneNumberUtils.formatNumber(number, number,
                                 MmsApp.getApplication().getCurrentCountryIso());
                                 if (title.charAt(0) != '\u202D') {
                                     title = '\u202D' + title + '\u202C';
                                 }
                        } else {
                            subTitle = PhoneNumberUtils.formatNumber(number, number,
                                    MmsApp.getApplication().getCurrentCountryIso());
                                if (subTitle.charAt(0) != '\u202D') {
                                    subTitle = '\u202D' + subTitle + '\u202C';
                                }
                        }
                    } else {
                         if (!title.equals(number)) {
                             subTitle = PhoneNumberUtils.formatNumber(number, number,
                                  MmsApp.getApplication().getCurrentCountryIso());
                         }
                    }
                    break;
                }
                default: {
                    // Handle multiple recipients
                    title = list.formatNames(", ");
                    subTitle = getResources().getQuantityString(R.plurals.recipient_count, cnt, cnt);
                    break;
                }
            }
            mDebugRecipients = list.serialize();

            // the cnt is already be added recipients count
            mExistsRecipientsCount = cnt;

            // Update the statusbar color
            if(cnt >= 1) {
                Contact contact = list.get(0);
                int color = contact.getContactColor();

                if (color == 0) {
                    color = sPrimaryColorDark;
                }

                updateColorPalette(color);
                setActionBarColor(color);
            } else {
                updateColorPalette(sPrimaryColorDark);
                setActionBarColor(sPrimaryColorDark);
            }
        }

        if (mToolBar != null) {
            mToolBar.setTitle(title);
            mToolBar.setSubtitle(subTitle);
        }
    }

    // Get the recipients editor ready to be displayed onscreen.
    private void initRecipientsEditor() {
        if (isRecipientsEditorVisible()) {
            return;
        }
        // Must grab the recipients before the view is made visible because getRecipients()
        // returns empty recipients when the editor is visible.
        final ContactList recipients = getRecipients();

        ViewStub stub = (ViewStub)findViewById(R.id.recipients_editor_stub);
        if (stub != null) {
            View stubView = stub.inflate();
            mRecipientsEditor = (RecipientsEditor) stubView.findViewById(R.id.recipients_editor);
            mRecipientsPicker = (ImageButton) stubView.findViewById(R.id.recipients_picker);
            mClearButton = (ImageButton) stubView.findViewById(R.id.clear);
        } else {
            mRecipientsEditor = (RecipientsEditor)findViewById(R.id.recipients_editor);
            mRecipientsEditor.setVisibility(View.VISIBLE);
            mRecipientsPicker = (ImageButton)findViewById(R.id.recipients_picker);
            mRecipientsPicker.setVisibility(View.VISIBLE);
            mClearButton = (ImageButton) findViewById(R.id.clear);
        }
        mClearButton.setVisibility(View.GONE);
        mClearButton.setOnClickListener(this);
        mRecipientsPicker.setOnClickListener(this);
        mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
        mRecipientsEditor.setAdapter(new ChipsRecipientAdapter(this));

        // Populate recipient editor with runnable thread to ensure display correctly.
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Uri uri = getIntent().getData();
                if (uri != null) {
                    String recipient = Conversation.getRecipients(uri);
                    mRecipientsEditor.setText(recipient + ",");
                } else {
                    mRecipientsEditor.setText(null);
                }
                mRecipientsEditor.populateWithAvatorDrawable(recipients);
            }
        };
        mHandler.post(r);

        // TODO : Remove the max length limitation due to the multiple phone picker is added and the
        // user is able to select a large number of recipients from the Contacts. The coming
        // potential issue is that it is hard for user to edit a recipient from hundred of
        // recipients in the editor box. We may redesign the editor box UI for this use case.
        // mRecipientsEditor.setFilters(new InputFilter[] {
        //         new InputFilter.LengthFilter(RECIPIENTS_MAX_LENGTH) });

        mRecipientsEditor.setOnSelectChipRunnable(new Runnable() {
            @Override
            public void run() {
                // After the user selects an item in the pop-up contacts list, move the
                // focus to the text editor if there is only one recipient.  This helps
                // the common case of selecting one recipient and then typing a message,
                // but avoids annoying a user who is trying to add five recipients and
                // keeps having focus stolen away.
                if (mRecipientsEditor.getRecipientCount() == 1) {
                    // if we're in extract mode then don't request focus
                    final InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputManager == null || !inputManager.isFullscreenMode()) {
                        mTextEditor.requestFocus();
                    }
                }
            }
        });

        mRecipientsEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    RecipientsEditor editor = (RecipientsEditor) v;
                    ContactList contacts = editor.constructContactsFromInput(false);
                    updateTitle(contacts);
                    if (mIsRcsEnabled && contacts != null && contacts.size() == 1) {
                        checkCapability(contacts.get(0).getNumber());
                    }
                } else {
                    if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
                        mAttachmentSelector.setVisibility(View.GONE);
                    }
                }
            }
        });

        PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(this, mRecipientsEditor);

        if (mRecipientsEditor != null) {
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT) {
                mRecipientsEditor.setOrientation(RecipientEditTextView.ScreenOrientation.PORTRAIT);
            } else {
                mRecipientsEditor.setOrientation(RecipientEditTextView.ScreenOrientation.LANDSCAPE);
            }
        }
        mTopPanel.setVisibility(View.VISIBLE);
        mToolBar.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIsSmsEnabled = MmsConfig.isSmsEnabled(this);
        super.onCreate(savedInstanceState);
        if (MessageUtils.checkPermissionsIfNeeded(this)) {
            return;
        }
        if (MessageUtils.checkIsPhoneMemoryFull(this)) {
            ComposeMessageActivity.this.finish();
            return;
        }
        resetConfiguration(getResources().getConfiguration());
        final Window window = ComposeMessageActivity.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setProgressBarVisibility(false);
        mIsRcsEnabled=MmsConfig.isRcsEnabled();
        mShowAttachIcon = getResources().getBoolean(R.bool.config_show_attach_icon_always);

        boolean isBtnStyle = getResources().getBoolean(R.bool.config_btnstyle);
        mShowTwoButtons = isBtnStyle && MessageUtils.isMsimIccCardActive();
        // Initialize members for UI elements.
        initResourceRefs();

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);

        initialize(savedInstanceState, 0);

        mEnablePresence = this.getResources().getBoolean(
                R.bool.config_regional_presence_enable);
        if (mEnablePresence) {
            if (!mIsBound) {
                bindService(this);
            }
            mAvailabilityfetchHandler = new Handler(){

                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PRESENCE_AVAILABILITY_FETCH:
                            getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
                            if (DEBUG) Log.d(TAG, "AvailabilityFetch result updateContact");
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        if (TRACE) {
            android.os.Debug.startMethodTracing("compose");
        }
    }

    private final BroadcastReceiver mSimBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SIM_STATE_CHANGE_ACTION)) {
                boolean mIsMsimIccCardActived = MessageUtils.isMsimIccCardActive();
                log("mSimBroadcastReceiver mIsMsimIccCardActived"
                        + mIsMsimIccCardActived);
                if (mMsgListAdapter != null) {
                    mMsgListAdapter.setIsMsimIccCardActived(mIsMsimIccCardActived);
                }
            }
        }
    };

    @Override
    public void onZoomWithScale(float scale) {
        if (mMsgListView != null) {
            mMsgListView.handleZoomWithScale(scale, false);
        }
        if (mTextEditor != null) {
            ZoomMessageListItem.zoomViewByScale(this, mTextEditor, scale, true);
        }
    }

    private void showSubjectEditor(boolean show) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("" + show);
        }

        if (mSubjectTextEditor == null) {
            // Don't bother to initialize the subject editor if
            // we're just going to hide it.
            if (show == false) {
                return;
            }
            mSubjectTextEditor = (EditText)findViewById(R.id.subject);
            mSubjectTextEditor.setFilters(new InputFilter[] {
                    new LengthFilter(SUBJECT_MAX_LENGTH)});
        }

        mSubjectTextEditor.setOnKeyListener(show ? mSubjectKeyListener : null);

        if (show) {
            mSubjectTextEditor.addTextChangedListener(mSubjectEditorWatcher);
        } else {
            mSubjectTextEditor.removeTextChangedListener(mSubjectEditorWatcher);
        }

        mSubjectTextEditor.setText(mWorkingMessage.getSubject());
        mSubjectTextEditor.setVisibility(show ? View.VISIBLE : View.GONE);
        mDeviderView.setVisibility(show ? View.VISIBLE : View.GONE);
        hideOrShowTopPanel();
    }

    private void hideOrShowTopPanel() {
        boolean anySubViewsVisible = isRecipientsEditorVisible();
        mTopPanel.setVisibility(anySubViewsVisible ? View.VISIBLE : View.GONE);
        mToolBar.setVisibility(anySubViewsVisible ? View.GONE : View.VISIBLE);
    }

    public void initialize(Bundle savedInstanceState, long originalThreadId) {
        // Create a new empty working message.
        mWorkingMessage = WorkingMessage.createEmpty(this);

        mSendMmsSupportViaWiFi = getResources().getBoolean(R.bool.support_send_mms_over_wifi);

        // Read parameters or previously saved state of this activity. This will load a new
        // mConversation
        initActivityState(savedInstanceState);

        // Init the RCS components.
        if (MmsConfig.isRcsVersion()) {
            initRcsComponents();
        }

        if (LogTag.SEVERE_WARNING && originalThreadId != 0 &&
                originalThreadId == mConversation.getThreadId()) {
            LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.initialize: " +
                    " threadId didn't change from: " + originalThreadId, this);
        }

        log("savedInstanceState = " + savedInstanceState +
            " intent = " + getIntent() +
            " mConversation = " + mConversation);

        if (MessageUtils.cancelFailedToDeliverNotification(getIntent(), this)) {
            // Show a pop-up dialog to inform user the message was
            // failed to deliver.
            undeliveredMessageDialog(getMessageDate(null));
        }
        MessageUtils.cancelFailedDownloadNotification(getIntent(), this);

        // Set up the message history ListAdapter
        initMessageList();

        mShouldLoadDraft = true;

        // Load the draft for this thread, if we aren't already handling
        // existing data, such as a shared picture or forwarded message.
        boolean isForwardedMessage = false;
        // We don't attempt to handle the Intent.ACTION_SEND when saveInstanceState is non-null.
        // saveInstanceState is non-null when this activity is killed. In that case, we already
        // handled the attachment or the send, so we don't try and parse the intent again.
        if (savedInstanceState == null && (handleSendIntent() || handleForwardedMessage())) {
            mShouldLoadDraft = false;
        }

        // Let the working message know what conversation it belongs to
        mWorkingMessage.setConversation(mConversation);

        handleResendMessage();

        // Show the recipients editor if we don't have a valid thread. Hide it otherwise.
        if (mConversation.getThreadId() <= 0
                && (!mConversation.isGroupChat() || mConversation.getGroupChat() == null)) {
            // Hide the recipients editor so the call to initRecipientsEditor won't get
            // short-circuited.
            hideRecipientEditor();
            initRecipientsEditor();
            updateColorPalette(sPrimaryColorDark);
            setActionBarColor(sPrimaryColorDark);
        } else {
            hideRecipientEditor();
        }

        updateSendButtonState();

        drawTopPanel(false);
        if (!mShouldLoadDraft) {
            // We're not loading a draft, so we can draw the bottom panel immediately.
            drawBottomPanel();
        }

        onKeyboardStateChanged();

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("update title, mConversation=" + mConversation.toString());
        }

        updateTitle(mConversation.getRecipients());

        if (isForwardedMessage && isRecipientsEditorVisible()) {
            // The user is forwarding the message to someone. Put the focus on the
            // recipient editor rather than in the message editor.
            mRecipientsEditor.requestFocus();
        }

        mMsgListAdapter.setIsGroupConversation(mConversation.getRecipients().size() > 1);
        if (MmsConfig.isRcsVersion()) {
            GroupChat groupChat = mConversation.getGroupChat();
            if (groupChat != null) {
                mMsgListAdapter.setRcsGroupId(groupChat.getId());
            }
        }
    }

    private void handleResendMessage() {
        // In mailbox mode, click sent failed message in outbox folder, re-send message.
        Intent intent = getIntent();
        boolean needResend = intent.getBooleanExtra(NEED_RESEND, false);
        if (!needResend) {
            return;
        }
        long messageId = intent.getLongExtra(MESSAGE_ID, 0);
        String messageType = intent.getStringExtra(MESSAGE_TYPE);
        if (messageId != 0 && !TextUtils.isEmpty(messageType)) {
            if ("sms".equals(messageType)) {
                String messageBody = intent.getStringExtra(MESSAGE_BODY);
                editSmsMessageItem(messageId, messageBody);
                drawBottomPanel();
                invalidateOptionsMenu();
            } else {
                Uri messageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, messageId);
                String messageSubject = "";
                String subject = intent.getStringExtra(MESSAGE_SUBJECT);
                if (!TextUtils.isEmpty(subject)) {
                    int subjectCharset = intent.getIntExtra(MESSAGE_SUBJECT_CHARSET, 0);
                    EncodedStringValue v = new EncodedStringValue(subjectCharset,
                            PduPersister.getBytes(subject));
                    messageSubject = MessageUtils.cleanseMmsSubject(this, v.getString());
                }
                editMmsMessageItem(messageUri, messageSubject);
                drawBottomPanel();
                invalidateOptionsMenu();
            }
        }
    }

    private void resetEditorText() {
        // We have to remove the text change listener while the text editor gets cleared and
        // we subsequently turn the message back into SMS. When the listener is listening while
        // doing the clearing, it's fighting to update its counts and itself try and turn
        // the message one way or the other.
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);
        // Clear the text box.
        TextKeyListener.clear(mTextEditor.getText());
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!(MessageUtils.hasBasicPermissions() && MessageUtils
                .hasPermissions(MessageUtils.sSMSExtendPermissions)))
        {
            return;
        }

        setIntent(intent);

        Conversation conversation = null;
        mSentMessage = false;

        // If we have been passed a thread_id, use that to find our
        // conversation.

        // Note that originalThreadId might be zero but if this is a draft and we save the
        // draft, ensureThreadId gets called async from WorkingMessage.asyncUpdateDraftSmsMessage
        // the thread will get a threadId behind the UI thread's back.
        long threadId = intent.getLongExtra(THREAD_ID, 0);
        Uri intentUri = intent.getData();
        if (null == mConversation) {
            mConversation = Conversation.get(this, intentUri, false);
        }
        long originalThreadId = mConversation.getThreadId();
        boolean sameThread = false;
        if (threadId > 0) {
            conversation = Conversation.get(this, threadId, false);
        } else {
            if (mConversation.getThreadId() == 0) {
                // We've got a draft. Make sure the working recipients are synched
                // to the conversation so when we compare conversations later in this function,
                // the compare will work.
                if (null != mWorkingMessage) {
                    mWorkingMessage.syncWorkingRecipients();
                }
            }
            // Get the "real" conversation based on the intentUri. The intentUri might specify
            // the conversation by a phone number or by a thread id. We'll typically get a threadId
            // based uri when the user pulls down a notification while in ComposeMessageActivity and
            // we end up here in onNewIntent. mConversation can have a threadId of zero when we're
            // working on a draft. When a new message comes in for that same recipient, a
            // conversation will get created behind CMA's back when the message is inserted into
            // the database and the corresponding entry made in the threads table. The code should
            // use the real conversation as soon as it can rather than finding out the threadId
            // when sending with "ensureThreadId".
            conversation = Conversation.get(this, intentUri, false);
        }

        LogTag.debugD("onNewIntent: data=" + intentUri + ", thread_id extra is " + threadId +
                ", new conversation=" + conversation + ", mConversation=" + mConversation);

        // this is probably paranoid to compare both thread_ids and recipient lists,
        // but we want to make double sure because this is a last minute fix for Froyo
        // and the previous code checked thread ids only.
        // (we cannot just compare thread ids because there is a case where mConversation
        // has a stale/obsolete thread id (=1) that could collide against the new thread_id(=1),
        // even though the recipient lists are different)
        sameThread = (conversation.getThreadId() == mConversation.getThreadId()
                && mConversation.getThreadId() != 0 && conversation.equals(mConversation));
        LogTag.debugD("sameThread:" + sameThread);

        if (!sameThread) {
            LogTag.debugD("onNewIntent: different conversation");
            mMessagesAndDraftLoaded = false;
            if (mConversation.getThreadId() == 0 || conversation.getThreadId() == 0) {
                mConversation = conversation;
                mWorkingMessage.setConversation(mConversation);
                updateThreadIdIfRunning();
                invalidateOptionsMenu();
                if (mMsgListAdapter != null) {
                    mMsgListAdapter.changeCursor(null);
                    mMsgListAdapter.cancelBackgroundLoading();
                }
            }
            mIsBurnMessage = false;
            saveDraft(false);    // if we've got a draft, save it first
            resetEditorText();
            initialize(null, originalThreadId);
        }
        loadMessagesAndDraft(0);
    }

    private void sanityCheckConversation() {
        if (mWorkingMessage.getConversation() != mConversation) {
            LogTag.warnPossibleRecipientMismatch(
                    "ComposeMessageActivity: mWorkingMessage.mConversation=" +
                    mWorkingMessage.getConversation() + ", mConversation=" +
                    mConversation + ", MISMATCH!", this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (mWorkingMessage.isDiscarded()) {
            // If the message isn't worth saving, don't resurrect it. Doing so can lead to
            // a situation where a new incoming message gets the old thread id of the discarded
            // draft. This activity can end up displaying the recipients of the old message with
            // the contents of the new message. Recognize that dangerous situation and bail out
            // to the ConversationList where the user can enter this in a clean manner.
            if (mWorkingMessage.isWorthSaving() || mInAsyncAddAttathProcess) {
                if (LogTag.VERBOSE) {
                    log("onRestart: mWorkingMessage.unDiscard()");
                }
                mWorkingMessage.unDiscard();    // it was discarded in onStop().

                sanityCheckConversation();
            } else if (isRecipientsEditorVisible() && recipientCount() > 0) {
                if (LogTag.VERBOSE) {
                    log("onRestart: goToConversationList");
                }
                goToConversationList();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }

        initFocus();
        if (isRecipientsEditorVisible()) {
            mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
        }

        // Register a BroadcastReceiver to listen on HTTP I/O process.
        registerReceiver(mHttpProgressReceiver, mHttpProgressFilter);

        // Register a BroadcastReceiver to listen on SD card state.
        registerReceiver(mMediaStateReceiver, getMediaStateFilter());

        registerReceiver(mAirplaneModeBroadcastReceiver, mAirplaneModeFilter);
        registerReceiver(mSimBroadcastReceiver, mSIMStatusChangeFilter);
        if (mIsRcsEnabled) {
            registerRcsReceiver();
        }

        // figure out whether we need to show the keyboard or not.
        // if there is draft to be loaded for 'mConversation', we'll show the keyboard;
        // otherwise we hide the keyboard. In any event, delay loading
        // message history and draft (controlled by DEFER_LOADING_MESSAGES_AND_DRAFT).
        int mode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        if (DraftCache.getInstance().hasDraft(mConversation.getThreadId())) {
            mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        } else if (mConversation.getThreadId() <= 0) {
            // For composing a new message, bring up the softkeyboard so the user can
            // immediately enter recipients. This call won't do anything on devices with
            // a hard keyboard.
            mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        } else {
            mode |= WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
        }

        getWindow().setSoftInputMode(mode);

        // reset mMessagesAndDraftLoaded
        mMessagesAndDraftLoaded = false;

        CharSequence text = mWorkingMessage.getText();
        if (text != null) {
            mTextEditor.setTextKeepState(text);
        }
        if (!DEFER_LOADING_MESSAGES_AND_DRAFT) {
            loadMessagesAndDraft(1);
        } else {
            // HACK: force load messages+draft after max delay, if it's not already loaded.
            // this is to work around when coming out of sleep mode. WindowManager behaves
            // strangely and hides the keyboard when it should be shown, or sometimes initially
            // shows it when we want to hide it. In that case, we never get the onSizeChanged()
            // callback w/ keyboard shown, so we wouldn't know to load the messages+draft.
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    loadMessagesAndDraft(2);
                }
            }, LOADING_MESSAGES_AND_DRAFT_MAX_DELAY_MS);
        }

        // Update the fasttrack info in case any of the recipients' contact info changed
        // while we were paused. This can happen, for example, if a user changes or adds
        // an avatar associated with a contact.
        mWorkingMessage.syncWorkingRecipients();

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("update title, mConversation=" + mConversation.toString());
        }

        updateTitle(mConversation.getRecipients());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (mRcsShareVcardAddNumber) {
            mRcsShareVcardAddNumber = false;
        }
    }

    public void loadMessageContent() {
        // Don't let any markAsRead DB updates occur before we've loaded the messages for
        // the thread. Unblocking occurs when we're done querying for the conversation
        // items.
        mConversation.blockMarkAsRead(true);
        mConversation.markAsRead();         // dismiss any notifications for this convo
        startMsgListQuery();
        updateSendFailedNotification();
    }

    /**
     * Load message history and draft. This method should be called from main thread.
     * @param debugFlag shows where this is being called from
     */
    private void loadMessagesAndDraft(int debugFlag) {
        if (!mSendDiscreetMode && !mMessagesAndDraftLoaded) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG, "### CMA.loadMessagesAndDraft: flag=" + debugFlag);
            }
            loadMessageContent();
            boolean drawBottomPanel = true;
            long threadId = mWorkingMessage.getConversation().getThreadId();
            // Do not load draft when forwarding to the same recipients.
            if (mShouldLoadDraft && !MessageUtils.sSameRecipientList.contains(threadId)) {
                if (loadDraft()) {
                    drawBottomPanel = false;
                }
            }
            if (drawBottomPanel) {
                drawBottomPanel();
            }
            mMessagesAndDraftLoaded = true;
        }
    }

    private void updateSendFailedNotification() {
        final long threadId = mConversation.getThreadId();
        if (threadId <= 0)
            return;

        // updateSendFailedNotificationForThread makes a database call, so do the work off
        // of the ui thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                MessagingNotification.updateSendFailedNotificationForThread(
                        ComposeMessageActivity.this, threadId);
            }
        }, "ComposeMessageActivity.updateSendFailedNotification").start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(RECIPIENTS, mConversation.getRecipients().serialize());

        mWorkingMessage.writeStateToBundle(outState);

        if (mSendDiscreetMode) {
            outState.putBoolean(KEY_EXIT_ON_SENT, mSendDiscreetMode);
        }
        if (mForwardMessageMode) {
            outState.putBoolean(KEY_FORWARDED_MESSAGE, mForwardMessageMode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // OLD: get notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.startPresenceObserver();

        mIsPickingContact = false;
        addRecipientsListeners();
        setSendButtonImage();
        if (isRecipientsEditorVisible()) {
            mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("update title, mConversation=" + mConversation.toString());
        }

        // There seems to be a bug in the framework such that setting the title
        // here gets overwritten to the original title.  Do this delayed as a
        // workaround.
        mMessageListItemHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ContactList recipients = isRecipientsEditorVisible() ?
                        mRecipientsEditor.constructContactsFromInput(false) : getRecipients();
                if (MmsConfig.isRcsVersion() && mConversation.isGroupChat()) {
                    try {
                        if (mConversation.getGroupChat() != null) {
                            long groupId = mConversation.getGroupChat().getId();
                            GroupChat groupChat = mGroupChatApi.getGroupChatById(groupId);
                            mConversation.setGroupChat(groupChat);
                        }
                    } catch (ServiceDisconnectedException e) {
                        RcsLog.e("Exception onResume()" + e);
                    } catch (RemoteException e) {
                        RcsLog.w("Exception" + e);
                    }
                }
                updateTitle(recipients);
            }
        }, 100);

        mIsRunning = true;
        updateThreadIdIfRunning();
        mIsAirplaneModeOn = MessageUtils.isAirplaneModeOn(this);

        if (getResources().getBoolean(R.bool.def_custom_preferences_settings)) {
            setBackgroundWallpaper();
            setTextFontsize();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (DEBUG) {
            Log.v(TAG, "onPause: setCurrentlyDisplayedThreadId: " +
                        MessagingNotification.THREAD_NONE);
        }
        MessagingNotification.setCurrentlyDisplayedThreadId(MessagingNotification.THREAD_NONE);

        // OLD: stop getting notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but updating presence
        //      there is out of our control.
        //Contact.stopPresenceObserver();

        removeRecipientsListeners();
        if (isRecipientsEditorVisible()) {
            mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
        }

        // remove any callback to display a progress spinner
        if (mAsyncDialog != null) {
            mAsyncDialog.clearPendingProgressDialog();
        }

        // Remember whether the list is scrolled to the end when we're paused so we can rescroll
        // to the end when resumed.
        if (mMsgListAdapter != null &&
                mMsgListView.getLastVisiblePosition() >= mMsgListAdapter.getCount() - 1) {
            mSavedScrollPosition = Integer.MAX_VALUE;
        } else {
            mSavedScrollPosition = mMsgListView.getFirstVisiblePosition();
        }
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.v(TAG, "onPause: mSavedScrollPosition=" + mSavedScrollPosition);
        }

        mConversation.markAsRead();
        mIsRunning = false;

        if (mMMSAudioPlayer != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMMSAudioPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // No need to do the querying when finished this activity
        mBackgroundQueryHandler.cancelOperation(MESSAGE_LIST_QUERY_TOKEN);

        // Allow any blocked calls to update the thread's read status.
        mConversation.blockMarkAsRead(false);

        // Simply setting the choice mode causes the previous choice mode to finish and we exit
        // multi-select mode (if we're in it) and remove all the selections.
        mMsgListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        if (mMsgListAdapter != null) {
            // resets multi select mode
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("save draft");
        }
        if(!isAvoidingSavingDraft)
        {
            saveDraft(true);

            // set 'mShouldLoadDraft' to true, so when coming back to ComposeMessageActivity, we would
            // load the draft, unless we are coming back to the activity after attaching a photo, etc,
            // in which case we should set 'mShouldLoadDraft' to false.
            mShouldLoadDraft = true;
            isAvoidingSavingDraft = false;
        }

        // Cleanup the BroadcastReceiver.
        unregisterReceiver(mHttpProgressReceiver);
        unregisterReceiver(mMediaStateReceiver);
        unregisterReceiver(mAirplaneModeBroadcastReceiver);
        unregisterReceiver(mSimBroadcastReceiver);
        if (mIsRcsEnabled) {
            unregisterRcsReceiver();
        }

        if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
            mAttachmentSelector.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (TRACE) {
            android.os.Debug.stopMethodTracing();
        }
        if (mZoomGestureOverlayView != null) {
            mZoomGestureOverlayView.removeZoomListener(this);
        }

        if (mMsgListAdapter != null) {
            mMsgListAdapter.changeCursor(null);
            mMsgListAdapter.cancelBackgroundLoading();
        }
        if (mInvalidRecipientDialog != null && mInvalidRecipientDialog.isShowing()) {
            mInvalidRecipientDialog.dismiss();
            mInvalidRecipientDialog = null;
        }
        if (mMsgDetailDialog != null && mMsgDetailDialog.isShowing()) {
            mMsgDetailDialog.dismiss();
            mMsgDetailDialog = null;
        }

        if (mEnablePresence) {
            if (mIsBound) {
                unbindService(this);
            }
        }
        super.onDestroy();

        mMMSAudioPlayer.releaseMediaPlayer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mRecipientsEditor != null) {
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT) {
                mRecipientsEditor.setOrientation(RecipientEditTextView.ScreenOrientation.PORTRAIT);
            } else {
                mRecipientsEditor.setOrientation(RecipientEditTextView.ScreenOrientation.LANDSCAPE);
            }
        }

        if (resetConfiguration(newConfig)) {
            // Have to re-layout the attachment editor because we have different layouts
            // depending on whether we're portrait or landscape.
            drawTopPanel(isSubjectEditorVisible());
        }
        if (LOCAL_LOGV) {
            Log.v(TAG, "CMA.onConfigurationChanged: " + newConfig +
                    ", mIsKeyboardOpen=" + mIsKeyboardOpen);
        }
        onKeyboardStateChanged();

        // If locale changed, we need reload the source of mInvalidRecipientDialog's
        // title and message from xml file.
        if (mInvalidRecipientDialog != null && mInvalidRecipientDialog.isShowing()) {
            mInvalidRecipientDialog.dismiss();
            showInvalidRecipientDialog();
        }
        mInvalidRecipientDialog = null;
        if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
            setAttachmentSelectorHeight();
            resetGridColumnsCount();
        }
    }

    // returns true if landscape/portrait configuration has changed
    private boolean resetConfiguration(Configuration config) {
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (mIsLandscape != isLandscape) {
            mIsLandscape = isLandscape;
            return true;
        }
        return false;
    }

    private void onKeyboardStateChanged() {
        // If the keyboard is hidden, don't show focus highlights for
        // things that cannot receive input.
        if (mIsRcsEnabled && mWorkingMessage.getCacheRcsMessage()) {
            mTextEditor.setEnabled(false);
        } else {
            mTextEditor.setEnabled(mIsSmsEnabled);
        }
        if (!mIsSmsEnabled) {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusableInTouchMode(false);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusableInTouchMode(false);
            }
            mTextEditor.setFocusableInTouchMode(false);
            mTextEditor.setHint(R.string.sending_disabled_not_default_app);
        } else if (mIsKeyboardOpen) {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusableInTouchMode(true);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusableInTouchMode(true);
            }
            mTextEditor.setFocusableInTouchMode(true);
            mTextEditor.setHint(R.string.type_to_compose_text_enter_to_send);
        } else {
            if (mRecipientsEditor != null) {
                mRecipientsEditor.setFocusable(false);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusable(false);
            }
            mTextEditor.setFocusable(false);
            mTextEditor.setHint(R.string.open_keyboard_to_compose_message);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if ((mMsgListAdapter != null) && mMsgListView.isFocused()) {
                    Cursor cursor;
                    try {
                        cursor = (Cursor) mMsgListView.getSelectedItem();
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Unexpected ClassCastException.", e);
                        return super.onKeyDown(keyCode, event);
                    }

                    if (cursor != null) {
                        String type = cursor.getString(COLUMN_MSG_TYPE);
                        long msgId = cursor.getLong(COLUMN_ID);
                        MessageItem msgItem = mMsgListAdapter.getCachedMessageItem(type, msgId,
                                cursor);
                        if (msgItem != null) {
                            DeleteMessageListener l = new DeleteMessageListener(msgItem);
                            confirmDeleteDialog(l, msgItem.mLocked);
                        }
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
                    mAttachmentSelector.setVisibility(View.GONE);
                } else {
                    exitComposeMessageActivity(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void exitComposeMessageActivity(final Runnable exit) {
        // If the message is empty, just quit -- finishing the
        // activity will cause an empty draft to be deleted.
        if (!mWorkingMessage.isWorthSaving()) {
            exit.run();
            mWorkingMessage.discard();
            return;
        }

        // If the recipient is empty, the meesgae shouldn't be saved, and should pop up the
        // confirm delete dialog.
        if (isRecipientEmpty()) {
            MessageUtils.showDiscardDraftConfirmDialog(this,
                    new DiscardDraftListener(), getValidNumCount());
            return;
        }

        mToastForDraftSave = true;
        exit.run();
    }

    private int getValidNumCount() {
        // If mRecipientsEditor is empty we need show empty info.
        int count = MessageUtils.ALL_RECIPIENTS_EMPTY;
        if (!TextUtils.isEmpty(mRecipientsEditor.getText())) {
            count = mRecipientsEditor.getValidRecipientsCount(mWorkingMessage.requiresMms());
        }
        return count;
    }

    private boolean isRecipientEmpty() {
        return isRecipientsEditorVisible()
                && (mRecipientsEditor.getValidRecipientsCount(mWorkingMessage.requiresMms())
                != MessageUtils.ALL_RECIPIENTS_VALID
                || (0 == mRecipientsEditor.getRecipientCount()));
    }

    private void goToConversationList() {
        finish();
        String currentClass = "";
        Intent intent = getIntent();
        if (intent != null) {
            currentClass = intent.getStringExtra(EXTRA_START_COMPOSE_FROM);
        }
        if(SearchConversationActivity.class.getSimpleName().equals(currentClass)){
            super.onBackPressed();
        } else if (NotificationConversationList.class.getSimpleName().equals(currentClass)) {
            startActivity(new Intent(this, NotificationConversationList.class));
        } else {
            startActivity(new Intent(this, ConversationList.class));
        }
    }

    private void hideRecipientEditor() {
        if (mRecipientsEditor != null) {
            mRecipientsEditor.removeTextChangedListener(mRecipientsWatcher);
            mRecipientsEditor.setVisibility(View.GONE);
            if (mRecipientsPicker != null) {
                mRecipientsPicker.setVisibility(View.GONE);
            }
            hideOrShowTopPanel();
        }
    }

    private boolean isRecipientsEditorVisible() {
        return (null != mRecipientsEditor)
                    && (View.VISIBLE == mRecipientsEditor.getVisibility());
    }

    private boolean isSubjectEditorVisible() {
        return (null != mSubjectTextEditor)
                    && (View.VISIBLE == mSubjectTextEditor.getVisibility());
    }

    @Override
    public void onAttachmentChanged() {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                drawBottomPanel();
                updateSendButtonState();
                drawTopPanel(isSubjectEditorVisible());
                if (mIsRcsEnabled && mWorkingMessage.requiresMms() && mSendAfterResize) {
                    sendMessage(true);
                    mSendAfterResize = false;
                }

            }
        });
    }

    @Override
    public void onProtocolChanged(final boolean convertToMms) {
        // Have to make sure we're on the UI thread. This function can be called off of the UI
        // thread when we're adding multi-attachments
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mShowTwoButtons) {
                    showTwoSmsOrMmsSendButton(convertToMms);
                } else {
                    showSmsOrMmsSendButton(convertToMms);
                }

                if (convertToMms) {
                    // In the case we went from a long sms with a counter to an mms because
                    // the user added an attachment or a subject, hide the counter --
                    // it doesn't apply to mms.
                    mTextCounter.setVisibility(View.GONE);

                    if (mShowTwoButtons) {
                        mTextCounterSec.setVisibility(View.GONE);
                    }
                    showConvertToMmsToast();
                } else {
                    mTextCounter.setVisibility(View.VISIBLE);
                    if (mShowTwoButtons) {
                        mTextCounterSec.setVisibility(View.VISIBLE);
                    }
                    showConvertToSmsToast();
                }
                updateSendButtonState();
            }
        });
    }

    // Show or hide the Sms or Mms button as appropriate. Return the view so that the caller
    // can adjust the enableness and focusability.
    private View showSmsOrMmsSendButton(boolean isMms) {
        View showButton;
        View hideButton;
        if (isMms) {
            showButton = mSendButtonMms;
            hideButton = mSendButtonSms;
            mSendButtonMmsText.setVisibility(View.VISIBLE);
        } else {
            showButton = mSendButtonSms;
            hideButton = mSendButtonMms;
            mSendButtonMmsText.setVisibility(View.GONE);
        }
        showButton.setVisibility(View.VISIBLE);
        hideButton.setVisibility(View.GONE);
        hideButton.setEnabled(false);

        return showButton;
    }

    private View[] showTwoSmsOrMmsSendButton(boolean isMms) {
        View[] showButton = new View[NUMBER_OF_BUTTONS];
        View[] hideButton = new View[NUMBER_OF_BUTTONS];
        if (isMms) {
            showButton[PhoneConstants.SUB1] = mSendLayoutMmsFir;
            showButton[PhoneConstants.SUB2] = mSendLayoutMmsSec;
            hideButton[PhoneConstants.SUB1] = mSendLayoutSmsFir;
            hideButton[PhoneConstants.SUB2] = mSendLayoutSmsSec;
        } else {
            showButton[PhoneConstants.SUB1] = mSendLayoutSmsFir;
            showButton[PhoneConstants.SUB2] = mSendLayoutSmsSec;
            hideButton[PhoneConstants.SUB1] = mSendLayoutMmsFir;
            hideButton[PhoneConstants.SUB2] = mSendLayoutMmsSec;
        }
        showButton[PhoneConstants.SUB1].setVisibility(View.VISIBLE);
        showButton[PhoneConstants.SUB2].setVisibility(View.VISIBLE);
        hideButton[PhoneConstants.SUB1].setVisibility(View.GONE);
        hideButton[PhoneConstants.SUB2].setVisibility(View.GONE);

        return showButton;
    }

    Runnable mResetMessageRunnable = new Runnable() {
        @Override
        public void run() {
            resetMessage();
        }
    };

    @Override
    public void onPreMessageSent() {
        runOnUiThread(mResetMessageRunnable);
    }

    @Override
    public void onMessageSent() {
        // This callback can come in on any thread; put it on the main thread to avoid
        // concurrency problems
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // If we already have messages in the list adapter, it
                // will be auto-requerying; don't thrash another query in.
                // TODO: relying on auto-requerying seems unreliable when priming an MMS into the
                // outbox. Need to investigate.
//                if (mMsgListAdapter.getCount() == 0) {
                    if (LogTag.VERBOSE) {
                        log("onMessageSent");
                    }
                    startMsgListQuery();
//                }

                // The thread ID could have changed if this is a new message that we just inserted
                // into the database (and looked up or created a thread for it)
                updateThreadIdIfRunning();
            }
        });
    }

    @Override
    public void onMaxPendingMessagesReached() {
        saveDraft(false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ComposeMessageActivity.this, R.string.too_many_unsent_mms,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onAttachmentError(final int error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIsAttachmentErrorOnSend = true;
                handleAddAttachmentError(error, R.string.type_picture);
                onMessageSent();        // now requery the list of messages
            }
        });
    }

    // We don't want to show the "call" option unless there is only one
    // recipient and it's a phone number.
    private boolean isRecipientCallable() {
        ContactList recipients = getRecipients();
        return (recipients.size() == 1 && !recipients.containsEmail()
                && !(MessageUtils.isWapPushNumber(recipients.get(0).getNumber())));
    }

    private void dialRecipient() {
        if (isRecipientCallable()) {
            if (mIsRcsEnabled) {
                if (mConversation != null && mConversation.isGroupChat()) {
                    RcsUtils.dialGroupChat(this, mConversation.getGroupChat());
                } else {
                    ContactList recipients = getRecipients();
                    int size = recipients.size();
                    if (size > 1) {
                        RcsUtils.dialConferenceCall(this, getRecipients());
                    } else if (size == 1) {
                        String number = getRecipients().get(0).getNumber();
                        if (mIsRcsEnabled && RcsUtils.isDeletePrefixSpecailNumberAvailable(this)) {
                            try {
                                number = SpecialServiceNumApi.getInstance()
                                        .deleteSsnPrefix(number);
                            } catch (ServiceDisconnectedException e) {
                                RcsLog.w("delSpecialPreNum error");
                            } catch (RemoteException e) {
                                RcsLog.w("Exception delSpecialPreNum()" + e);
                            }
                        }
                        Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + number));
                        startActivity(dialIntent);
                    }
                }
            } else {
                String number = getRecipients().get(0).getNumber();
                Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                startActivity(dialIntent);
            }
        }
    }

    private void videoCallRecipient() {
        String number = getRecipients().get(0).getNumber();
        final Intent intent = new Intent(Intent.ACTION_CALL, getCallUri(number));
        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_BIDIRECTIONAL);
        startActivity(intent);
    }

    public  Uri getCallUri(String number) {
        if (isUriNumber(number)) {
            return Uri.fromParts("sip", number, null);
        }
        return Uri.fromParts("tel", number, null);
    }

    public  boolean isUriNumber(String number) {
        return number != null && (number.contains("@") || number.contains("%40"));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu) ;

        menu.clear();

        if (mSendDiscreetMode && !mForwardMessageMode && !mReplyMessageMode) {
            // When we're in send-a-single-message mode from the lock screen, don't show
            // any menus.
            return true;
        }

        if (mEnablePresence) {
            if (isRecipientCallable()) {
                final String number = getRecipients().get(0).getNumber();
                MenuItem item = menu.add(0, MENU_VIDEOCALL_RECIPIENT, 0, R.string.menu_call);
                mVideoCapable = getVTCapability(number);
                new Thread(new Runnable(){
                     public void run(){
                         if (null != number) {
                             boolean newVTCapable = startAvailabilityFetch(number);
                             if (mVideoCapable != newVTCapable) {
                                 mAvailabilityfetchHandler
                                         .sendEmptyMessage(PRESENCE_AVAILABILITY_FETCH);
                             }
                         }
                     }
                 }).start();
                if (mVideoCapable) {
                    item.setIcon(R.drawable.ic_attach_capture_video_holo_light);
                    item.setEnabled(true);
                } else {
                    item.setIcon(R.drawable.ic_attach_capture_video_disable);
                    item.setEnabled(false);
                }
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
        // Don't show the call icon if the device don't support voice calling.
        boolean voiceCapable =
                getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        if (isRecipientCallable() && voiceCapable) {
            MenuItem item = menu.add(0, MENU_CALL_RECIPIENT, 0, R.string.menu_call)
                .setIcon(R.drawable.call)
                .setTitle(R.string.menu_call);
            if (!isRecipientsEditorVisible()) {
                // If we're not composing a new message, show the call icon in the actionbar
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }

        if (MmsConfig.getMmsEnabled() && mIsSmsEnabled) {
            if (showAddAttachementButton()) {
                mAddAttachmentButton.setVisibility(View.VISIBLE);
            }
        }
        if (mIsRcsEnabled && mConversation.getThreadId() != 0) {
            if ((!mConversation.isTop()) &&
                    !mTopThread.contains(mConversation.getThreadId())) {
                menu.add(0, MENU_TOP_CONVERSATION, 0, R.string.top_conversation).setIcon(
                        R.drawable.ic_menu_edit);
            } else {
                menu.add(0, MENU_CANCEL_TOP_CONVERSATION, 0, R.string.cancel_top_conversation)
                        .setIcon(R.drawable.ic_menu_edit);
            }
        }
        if (isPreparedForSending() && mIsSmsEnabled) {
           if (mShowTwoButtons) {
                menu.add(0, MENU_SEND_BY_SLOT1, 0, R.string.send_by_slot1)
                        .setIcon(android.R.drawable.ic_menu_send);
                menu.add(0, MENU_SEND_BY_SLOT2, 0, R.string.send_by_slot2)
                        .setIcon(android.R.drawable.ic_menu_send);
            } else {
                menu.add(0, MENU_SEND, 0, R.string.send).setIcon(android.R.drawable.ic_menu_send);
            }
        }

        if (getRecipients().size() > 1) {
            menu.add(0, MENU_GROUP_PARTICIPANTS, 0, R.string.menu_group_participants);
        }

        if (mMsgListAdapter.getCount() > 0 && mIsSmsEnabled) {
            // Removed search as part of b/1205708
            //menu.add(0, MENU_SEARCH, 0, R.string.menu_search).setIcon(
            //        R.drawable.ic_menu_search);
            Cursor cursor = mMsgListAdapter.getCursor();
            if ((null != cursor) && (cursor.getCount() > 0)) {
                menu.add(0, MENU_DELETE_THREAD, 0, R.string.delete_thread).setIcon(
                    android.R.drawable.ic_menu_delete);
            }
        } else if (mIsSmsEnabled) {
            menu.add(0, MENU_DISCARD, 0, R.string.discard).setIcon(android.R.drawable.ic_menu_delete);
        }
        if (MmsConfig.isRcsVersion()) {
            if (!mConversation.isGroupChat()) {
                buildAddAddressToContactMenuItem(menu);
            }
        } else {
            buildAddAddressToContactMenuItem(menu);
        }
        // ADD firewall menu
        if (mIsRcsEnabled && !mConversation.isGroupChat() && 1 == getRecipients().size()
                && RcsUtils.isFireWallInstalled(ComposeMessageActivity.this)) {
            if (RcsUtils.showFirewallMenu(getContext(),
                    mConversation.getRecipients(), true)) {
                menu.add(0, MENU_FIERWALL_ADD_BLACKLIST, 0,
                        getString(R.string.menuid_add_to_black_list));
            }
            if (RcsUtils.showFirewallMenu(getContext(),
                    mConversation.getRecipients(), false)) {
                menu.add(0, MENU_FIERWALL_ADD_WHITELIST, 0,
                        getString(R.string.menuid_add_to_white_list));
            }
        }


        if (LogTag.DEBUG_DUMP) {
            menu.add(0, MENU_DEBUG_DUMP, 0, R.string.menu_debug_dump);
        }
        if (mIsRcsEnabled) {
            if (mConversation.isGroupChat()) {
                GroupChat groupChat = mConversation.getGroupChat();
                if (groupChat != null) {
                    menu.add(0, MENU_RCS_GROUP_CHAT_DETAIL, 0, R.string.rcs_group_chat_detail);
                }
            } else {
                if (recipientCount() == 1) {
                    if (isRecipientsEditorVisible()) {
                        List<String> numbers = mRecipientsEditor.getNumbers();
                        checkCapability(numbers.get(0));
                    }
                    addBurnMessageMenu(menu);
                }

                if (!isRecipientsEditorVisible()) {
                    addSwitchToGroupChatMenuItem(menu);
                }
            }
        }
        if (mIsRcsEnabled && mSupportApi.isPluginInstalled(this)) {
            menu.add(0, MENU_RCS_MCLOUD_SHARE, 0, R.string.rcs_mcloud_share_file);
        }

        return true;
    }

    private void buildAddAddressToContactMenuItem(Menu menu) {
        // bug #7087793: for group of recipients, remove "Add to People" action. Rely on
        // individually creating contacts for unknown phone numbers by touching the individual
        // sender's avatars, one at a time
        ContactList contacts = getRecipients();
        if (contacts.size() != 1) {
            return;
        }

        // if we don't have a contact for the recipient, create a menu item to add the number
        // to contacts.
        Contact c = contacts.get(0);
        if (!c.existsInDatabase() && canAddToContacts(c)) {
            Intent intent = ConversationList.createAddContactIntent(c.getNumber());
            menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, R.string.menu_add_to_contacts)
                .setIcon(android.R.drawable.ic_menu_add)
                .setIntent(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_SUBJECT:
                showSubjectEditor(true);
                mWorkingMessage.setSubject("", true);
                updateSendButtonState();
                mSubjectTextEditor.requestFocus();
                break;
            case MENU_DISCARD:
                mWorkingMessage.discard();
                finish();
                break;
            case MENU_SEND:
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded();
                }
                break;
            case MENU_SEND_BY_SLOT1:
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded(
                            SubscriptionManager.getSubId(PhoneConstants.SUB1)[0]);
                }
                break;
            case MENU_SEND_BY_SLOT2:
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded(
                            SubscriptionManager.getSubId(PhoneConstants.SUB2)[0]);
                }
                break;
            case MENU_SEARCH:
                onSearchRequested();
                break;
            case MENU_DELETE_THREAD:
                confirmDeleteThread(mConversation.getThreadId());
                break;
            case MENU_TOP_CONVERSATION:
                mTopThread.add(mConversation.getThreadId());
                RcsUtils.topConversion(this,mConversation.getThreadId());
                startMsgListQuery();
                break;
            case MENU_CANCEL_TOP_CONVERSATION:
                mTopThread.remove(mConversation.getThreadId());
                RcsUtils.cancelTopConversion(this, mConversation.getThreadId());
                startMsgListQuery();
                break;
            case android.R.id.home:
            case MENU_CONVERSATION_LIST:
                exitComposeMessageActivity(new Runnable() {
                    @Override
                    public void run() {
                        goToConversationList();
                    }
                });
                break;
            case MENU_CALL_RECIPIENT:
                dialRecipient();
                break;
            case MENU_VIDEOCALL_RECIPIENT:
                videoCallRecipient();
                break;
            case MENU_IMPORT_TEMPLATE:
                showDialog(DIALOG_IMPORT_TEMPLATE);
                break;
            case MENU_GROUP_PARTICIPANTS:
            {
                Intent intent = new Intent(this, RecipientListActivity.class);
                intent.putExtra(THREAD_ID, mConversation.getThreadId());
                startActivity(intent);
                break;
            }
            case MENU_VIEW_CONTACT: {
                // View the contact for the first (and only) recipient.
                ContactList list = getRecipients();
                if (list.size() == 1 && list.get(0).existsInDatabase()) {
                    Uri contactUri = list.get(0).getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                }
                break;
            }
            case MENU_ADD_ADDRESS_TO_CONTACTS:
                mAddContactIntent = item.getIntent();
                startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                break;
            case MENU_PREFERENCES: {
                Intent intent = new Intent(this, MessagingPreferenceActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            }
            case MENU_DEBUG_DUMP:
                mWorkingMessage.dump();
                Conversation.dump();
                LogTag.dumpInternalTables(this);
                break;
            case MENU_RCS_GROUP_CHAT_DETAIL: {
                // launch the RCS group chat detail activity.
                GroupChat myGroupChat = null;
                try {
                    myGroupChat = mGroupChatApi.getGroupChatByThreadId(mConversation.getThreadId());
                } catch (RemoteException e) {
                    RcsLog.w(e);
                } catch (ServiceDisconnectedException e) {
                    RcsLog.w(e);
                }
                if (myGroupChat != null) {
                    RcsLog.i("ComposeMessageActivity: start group chat detail status="
                            + myGroupChat.getStatus());
                }
                if (myGroupChat != null && myGroupChat.getStatus() == GroupChat.STATUS_STARTED) {
                    RcsUtils.startGroupChatDetailActivity(ComposeMessageActivity.this, myGroupChat);
                } else {
                    Toast.makeText(ComposeMessageActivity.this, R.string.group_chat_deleted,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case MENU_RCS_MCLOUD_SHARE: {
                mWorkingMessage.setRequiringRcsAttachment(true);
                Intent intent = new Intent();
                intent.setAction(ACTION_LUNCHER_RCS_SHAREFILE);
                startActivityForResult(intent, REQUEST_CODE_SAIYUN);
                break;
            }
            case MENU_FIERWALL_ADD_WHITELIST:
                RcsUtils.addNumberToFirewall(this, mConversation.getRecipients(), false);
                break;
            case MENU_FIERWALL_ADD_BLACKLIST:
                showAddBlacklistDialog();
                break;
            case MENU_ADD_TO_BLACKLIST:
                confirmAddBlacklist();
                break;
        }

        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_IMPORT_TEMPLATE:
            return showImportTemplateDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_IMPORT_TEMPLATE:
                removeDialog(id);
                break;
        }
        super.onPrepareDialog(id, dialog);
    }

    private Dialog showImportTemplateDialog(){
        String [] smsTempArray = null;
        Uri uri = Uri.parse("content://com.android.mms.MessageTemplateProvider/messages");
        Cursor cur = null;
        try {
            cur = getContentResolver().query(uri, null, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                int index = 0;
                smsTempArray = new String[cur.getCount()];
                String title = null;
                do {
                    title = cur.getString(cur.getColumnIndex("message"));
                    smsTempArray[index++] = title;
                } while (cur.moveToNext());
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }

        TemplateSelectListener listener = new TemplateSelectListener(smsTempArray);
        return new AlertDialog.Builder(ComposeMessageActivity.this)
                .setTitle(R.string.message_template)
                .setItems(smsTempArray, listener)
                .create();
    }

    private class TemplateSelectListener implements DialogInterface.OnClickListener {

        private String[] mTempArray;
        TemplateSelectListener(String[] tempArray) {
            mTempArray = tempArray;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            if (mTempArray != null && mTempArray.length > which) {
                // If the subject EditText is visible and has the focus,
                // add the string from the template to the subject EditText
                // or else add the string to the message EditText.
                EditText etSubject = ComposeMessageActivity.this.mSubjectTextEditor;
                if (isSubjectEditorVisible() && etSubject.hasFocus()) {
                    int subjectIndex = etSubject.getSelectionStart();
                    etSubject.getText().insert(subjectIndex, mTempArray[which]);
                } else {
                    EditText et = ComposeMessageActivity.this.mTextEditor;
                    int index = et.getSelectionStart();
                    et.getText().insert(index, mTempArray[which]);
                    // Need require foucus,if do not do so,foucus still on mRecipientEditor,
                    // so mRecipientsWatcher will  call afterTextChanged to do
                    // setWorkingRecipients(...), and then mWorkingRecipients != null and will
                    // call setRecipients() set mThreadId = 0. Because of mThreadId = 0,
                    // asyncDeleteDraftSmsMessage will can not delete draft successful.
                    et.requestFocus();
                }
            }
        }

    }
    private void confirmDeleteThread(long threadId) {
        Conversation.startQueryHaveLockedMessages(mBackgroundQueryHandler,
                threadId, ConversationList.HAVE_LOCKED_MESSAGES_TOKEN);
    }

//    static class SystemProperties { // TODO, temp class to get unbundling working
//        static int getInt(String s, int value) {
//            return value;       // just return the default value or now
//        }
//    }

    private int getSlideNumber() {
        int slideNum = 0;
        SlideshowModel slideshow = mWorkingMessage.getSlideshow();
        if (slideshow != null) {
            slideNum = slideshow.size();
        }
        return slideNum;
    }

    private boolean showAddAttachementButton() {
        if (!mShowAttachIcon) {
            return !mWorkingMessage.hasAttachment();
        } else {
            return !mWorkingMessage.hasVcard()
                    && getSlideNumber() < MmsConfig.getMaxSlideNumber();
        }
    }

    private boolean isAppendRequest(int requestCode) {
        return (requestCode & REPLACE_ATTACHMEN_MASK) == 0;
    }

    private int getRequestCode(int requestCode) {
        return requestCode & ~REPLACE_ATTACHMEN_MASK;
    }

    private int getMakRequestCode(boolean replace, int requestCode) {
        if (replace) {
            return requestCode | REPLACE_ATTACHMEN_MASK;
        }
        return requestCode;
    }

    private void addAttachment(int type, boolean replace) {
        // Calculate the size of the current slide if we're doing a replace so the
        // slide size can optionally be used in computing how much room is left for an attachment.
        int currentSlideSize = 0;
        SlideshowModel slideShow = mWorkingMessage.getSlideshow();
        if (replace && slideShow != null) {
            WorkingMessage.removeThumbnailsFromCache(slideShow);
            SlideModel slide = slideShow.get(0);
            currentSlideSize = slide.getSlideSize();
        }
        switch (type) {
            case AttachmentPagerAdapter.ADD_SUBJECT:
                showSubjectEditor(true);
                mWorkingMessage.setSubject("", true);
                updateSendButtonState();
                mSubjectTextEditor.requestFocus();
                break;

            case AttachmentPagerAdapter.ADD_IMAGE:
                MessageUtils.selectImage(this,
                        getMakRequestCode(replace, REQUEST_CODE_ATTACH_IMAGE));
                break;

            case AttachmentPagerAdapter.TAKE_PICTURE: {
                MessageUtils.capturePicture(this,
                        getMakRequestCode(replace, REQUEST_CODE_TAKE_PICTURE));
                break;
            }

            case AttachmentPagerAdapter.ADD_VIDEO:
                MessageUtils.selectVideo(this,
                        getMakRequestCode(replace, REQUEST_CODE_ATTACH_VIDEO));
                break;

            case AttachmentPagerAdapter.RECORD_VIDEO: {
                long sizeLimit = 0;
                if (mIsRcsEnabled && mWorkingMessage.requiringRcsAttachment()) {
                    sizeLimit = RcsFileController.getRcsTransferFileMaxSize(
                            RcsUtils.RCS_MSG_TYPE_VIDEO);
                } else {
                    sizeLimit = computeAttachmentSizeLimit(slideShow, currentSlideSize);
                }
                if (sizeLimit > 0) {
                    MessageUtils.recordVideo(this,
                        getMakRequestCode(replace, REQUEST_CODE_TAKE_VIDEO), sizeLimit,
                        mWorkingMessage.requiresMms());
                } else {
                    Toast.makeText(this,
                            getString(R.string.message_too_big_for_video),
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;

            case AttachmentPagerAdapter.ADD_SOUND:
                MessageUtils.selectAudio(this,
                        getMakRequestCode(replace, REQUEST_CODE_ATTACH_SOUND));
                break;

            case AttachmentPagerAdapter.RECORD_SOUND:
                long sizeLimit = computeAttachmentSizeLimit(slideShow, currentSlideSize);
                MessageUtils.recordSound(this,
                        getMakRequestCode(replace, REQUEST_CODE_RECORD_SOUND), sizeLimit,
                        mWorkingMessage.requiringRcsAttachment());
                break;

            case AttachmentPagerAdapter.ADD_SLIDESHOW:
                if (mIsRcsEnabled) {
                    mWorkingMessage.setRequiringRcsAttachment(false);
                }
                editSlideshow();
                break;
            case AttachmentPagerAdapter.ADD_CONTACT_AS_TEXT:
                pickContacts(MultiPickContactsActivity.MODE_INFO,
                        REQUEST_CODE_ATTACH_ADD_CONTACT_INFO);
                break;
            case AttachmentPagerAdapter.ADD_CONTACT_AS_VCARD:
                if (mIsRcsEnabled && mWorkingMessage.requiringRcsAttachment()) {
                    vcardContactOrGroup();
                } else {
                    pickContacts(MultiPickContactsActivity.MODE_VCARD,
                            REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD);
                }
                break;
            case AttachmentPagerAdapter.ADD_TEMPLATE:
                showDialog(DIALOG_IMPORT_TEMPLATE);
                break;
            case AttachmentPagerAdapter.ADD_MAP:
                if (mIsRcsEnabled && mWorkingMessage.requiresMms()) {
                    toast(R.string.mms_does_not_support_location_sharing);
                    break;
                }
                try {
                    if (!RcsUtils.isRcsOnline()) {
                        Toast.makeText(this, getString(R.string.rcs_service_is_not_available),
                                Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        final Intent intent = new Intent();
                        intent.setAction("com.suntek.mway.rcs.MAP_POSITION_SELECT");
                        startActivityForResult(intent, REQUEST_CODE_ATTACH_MAP);
                        mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_MAP);
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, getString(R.string.please_install_rcs_map),
                            Toast.LENGTH_LONG).show();
                }

                break;

            default:
                break;
        }
    }

    public static long computeAttachmentSizeLimit(SlideshowModel slideShow, int currentSlideSize) {
        // Computer attachment size limit. Subtract 1K for some text.
        long sizeLimit = MmsConfig.getMaxMessageSize() - SlideshowModel.SLIDESHOW_SLOP;
        if (slideShow != null) {
            sizeLimit = sizeLimit -slideShow.getCurrentMessageSize()-
                    slideShow.getTotalTextMessageSize();

            // We're about to ask the camera to capture some video (or the sound recorder
            // to record some audio) which will eventually replace the content on the current
            // slide. Since the current slide already has some content (which was subtracted
            // out just above) and that content is going to get replaced, we can add the size of the
            // current slide into the available space used to capture a video (or audio).
            sizeLimit += currentSlideSize;
        }
        return sizeLimit;
    }

    private void showAttachmentSelector(final boolean replace) {
        //close KB and emoji view.
        if (mIsRcsEnabled) {
            if (mRcsEmojiInitialize != null) {
                mRcsEmojiInitialize.closeViewAndKB();
            }
            RcsUtils.closeKB(ComposeMessageActivity.this);
        }
        mAttachmentPager = (ViewPager) findViewById(R.id.attachments_selector_pager);
        mIsReplaceAttachment = replace;
        mCurrentAttachmentPager = DEFAULT_ATTACHMENT_PAGER;
        hideKeyboard();
        if (mAttachmentPagerAdapter == null) {
            mAttachmentPagerAdapter = new AttachmentPagerAdapter(this);
        }
        boolean showSubject = false;
        if (MmsConfig.getMmsEnabled() && mIsSmsEnabled) {
            if (!isSubjectEditorVisible() && !mConversation.isGroupChat()) {
                showSubject = true;
            }
        }
        mAttachmentPagerAdapter.setExistAttachmentType(mWorkingMessage.hasAttachment(),
                mWorkingMessage.hasVcard(), mWorkingMessage.hasSlideshow(), replace, showSubject);
        if (mIsRcsEnabled) {
            if (RcsUtils.isRcsOnline() && RcsDualSimMananger.getUserIsUseRcsPolicy(this)) {
                setRcsAttachment();
            } else if (mConversation.isGroupChat()) {
                mWorkingMessage.setRequiringRcsAttachment(true);
            } else {
                mWorkingMessage.setRequiringRcsAttachment(false);
            }
        }
        mAttachmentPagerAdapter.setGridItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    addAttachment((mCurrentAttachmentPager > DEFAULT_ATTACHMENT_PAGER ? position
                            + mAttachmentPagerAdapter.PAGE_GRID_COUNT : position), replace);
                    if (mIsRTL) {
                        addAttachment((mCurrentAttachmentPager > DEFAULT_ATTACHMENT_PAGER ? position
                            : mAttachmentPagerAdapter.PAGE_GRID_COUNT + position), replace);
                    }
                    mAttachmentSelector.setVisibility(View.GONE);
                }
            }
        });
        setAttachmentSelectorHeight();
        mAttachmentPager.setAdapter(mAttachmentPagerAdapter);
        mAttachmentPager.setCurrentItem(((mIsRTL) ? 1 : 0));
        mCurrentAttachmentPager = ((mIsRTL) ? 1 : 0);
        mAttachmentPager.setOnPageChangeListener(mAttachmentPagerChangeListener);
        mAttachmentSelector.setVisibility(View.VISIBLE);
        // Delay 200ms for drawing view completed.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAttachmentSelector.requestFocus();
            }
        }, 200);
    }

    private final OnPageChangeListener mAttachmentPagerChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            updateAttachmentSelectorIndicator(position);
            mCurrentAttachmentPager = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private void updateAttachmentSelectorIndicator(int pagerPosition) {
        ImageView pagerIndicatorFirst = (ImageView) mAttachmentSelector.findViewById(
                R.id.pager_indicator_first);
        ImageView pagerIndicatorSecond = (ImageView) mAttachmentSelector.findViewById(
                R.id.pager_indicator_second);

        if (mIsRTL) {
            pagerIndicatorSecond.setImageResource(pagerPosition == 0 ? R.drawable.dot_chosen
                    : R.drawable.dot_unchosen);
            pagerIndicatorFirst.setImageResource(pagerPosition == 0 ? R.drawable.dot_unchosen
                    : R.drawable.dot_chosen);
            return;
        }

        pagerIndicatorFirst.setImageResource(pagerPosition == 0 ? R.drawable.dot_chosen
                : R.drawable.dot_unchosen);
        pagerIndicatorSecond.setImageResource(pagerPosition == 0 ? R.drawable.dot_unchosen
                : R.drawable.dot_chosen);
    }

    private void setAttachmentSelectorHeight() {
        // Show different lines of grid for horizontal and vertical screen.
        Configuration configuration = getResources().getConfiguration();
        LayoutParams params = (LayoutParams) mAttachmentPager.getLayoutParams();
        int pagerHeight = (int) (mAttachmentPagerAdapter.GRID_ITEM_HEIGHT
                * getResources().getDisplayMetrics().density + 0.5f);
        params.height = (configuration.orientation == configuration.ORIENTATION_PORTRAIT)
                ? pagerHeight * 2 : pagerHeight;
        mAttachmentPager.setLayoutParams(params);
    }

    private void resetGridColumnsCount() {
        Configuration configuration = getResources().getConfiguration();
        ArrayList<GridView> pagerGridViews = mAttachmentPagerAdapter.getPagerGridViews();
        for (GridView grid : pagerGridViews) {
            grid.setNumColumns((configuration.orientation == configuration.ORIENTATION_PORTRAIT)
                    ? mAttachmentPagerAdapter.GRID_COLUMN_COUNT
                    : mAttachmentPagerAdapter.GRID_COLUMN_COUNT * 2);
        }
    }

    @Override
    protected void onActivityResult(int maskResultCode, int resultCode, Intent data) {
        if (LogTag.VERBOSE) {
            log("onActivityResult: requestCode=" + getRequestCode(maskResultCode) +
                    ", resultCode=" + resultCode + ", data=" + data);
        }
        mWaitingForSubActivity = false;          // We're back!
        mShouldLoadDraft = false;
        int requestCode = getRequestCode(maskResultCode);
        boolean append = isAppendRequest(maskResultCode);
        if (mWorkingMessage.isFakeMmsForDraft()) {
            // We no longer have to fake the fact we're an Mms. At this point we are or we aren't,
            // based on attachments and other Mms attrs.
            mWorkingMessage.removeFakeMmsForDraft();
        }

        if (requestCode == REQUEST_CODE_PICK) {
            mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
        }

        if (requestCode == REQUEST_CODE_ADD_CONTACT) {
            // The user might have added a new contact. When we tell contacts to add a contact
            // and tap "Done", we're not returned to Messaging. If we back out to return to
            // messaging after adding a contact, the resultCode is RESULT_CANCELED. Therefore,
            // assume a contact was added and get the contact and force our cached contact to
            // get reloaded with the new info (such as contact name). After the
            // contact is reloaded, the function onUpdate() in this file will get called
            // and it will update the title bar, etc.
            if (mAddContactIntent != null) {
                String address =
                    mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.EMAIL);
                if (address == null) {
                    address =
                        mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE);
                }
                if (address != null) {
                    Contact contact = Contact.get(address, false);
                    if (contact != null) {
                        contact.reload();
                    }
                }
            }
        }

        if (requestCode == REQUEST_CODE_EMOJI_STORE) {
            if (mRcsEmojiInitialize != null) {
                mRcsEmojiInitialize.refreshData();
            }
        }

        if (resultCode != RESULT_OK) {
            if (LogTag.VERBOSE) log("bail due to resultCode=" + resultCode);
            return;
        }

        if (MmsConfig.isCreationModeEnabled()) {
            ContentRestrictionFactory.reset();
        }

        if (mIsRcsEnabled && processedRcsActivityResult(requestCode, data)) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_CREATE_SLIDESHOW:
                if (data != null) {
                    WorkingMessage newMessage = WorkingMessage.load(this, data.getData());
                    if (newMessage != null) {
                        // Here we should keep the subject from the old mWorkingMessage.
                        setNewMessageSubject(newMessage);
                        mWorkingMessage = newMessage;
                        mWorkingMessage.setConversation(mConversation);
                        updateThreadIdIfRunning();
                        updateMmsSizeIndicator();
                        drawTopPanel(false);
                        drawBottomPanel();
                        updateSendButtonState();
                    }
                }
                break;

            case REQUEST_CODE_TAKE_PICTURE: {
                // create a file based uri and pass to addImage(). We want to read the JPEG
                // data directly from file (using UriImage) instead of decoding it into a Bitmap,
                // which takes up too much memory and could easily lead to OOM.
                File file = new File(TempFileProvider.getScrapPath(this));
                Uri uri = Uri.fromFile(file);

                // Remove the old captured picture's thumbnail from the cache
                MmsApp.getApplication().getThumbnailManager().removeThumbnail(uri);

                addImageAsync(uri, append);
                break;
            }

            case REQUEST_CODE_ATTACH_IMAGE: {
                if (data != null) {
                    addImageAsync(data.getData(), append);
                }
                break;
            }

            case REQUEST_CODE_TAKE_VIDEO:
                Uri videoUri = TempFileProvider.renameScrapFile(".3gp",
                        Integer.toString(getSlideNumber()), this);
                // Remove the old captured video's thumbnail from the cache
                MmsApp.getApplication().getThumbnailManager().removeThumbnail(videoUri);

                addVideoAsync(videoUri, append);      // can handle null videoUri
                break;

            case REQUEST_CODE_ATTACH_VIDEO:
                if (data != null) {
                    addVideoAsync(data.getData(), append);
                }
                break;

            case REQUEST_CODE_ATTACH_SOUND: {
                // Attempt to add the audio to the  attachment.
                Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri == null) {
                    uri = data.getData();
                } else if (Settings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                    break;
                }
                addAudio(uri, append);
                drawBottomPanel();
                break;
            }

            case REQUEST_CODE_RECORD_SOUND:
                if (data != null) {
                    addAudio(data.getData(), append);
                }
                break;

            case REQUEST_CODE_ECM_EXIT_DIALOG:
                boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
                LogTag.debugD("outOfEmergencyMode:" + outOfEmergencyMode);
                if (outOfEmergencyMode) {
                    sendMessage(false);
                }
                break;

            case REQUEST_CODE_PICK:
                if (data != null && mRecipientsEditor != null) {
                    processPickResult(data);
                }
                break;

            case REQUEST_SELECT_GROUP:
            case REQUEST_CODE_RCS_PICK:
                if (data != null) {
                    Bundle bundle = data.getExtras().getBundle("result");
                    final Set<String> keySet = bundle.keySet();
                    final int recipientCount = (keySet != null) ? keySet.size() : 0;
                    final ContactList list;
                    list = ContactList.blockingGetByUris(buildUris(keySet, recipientCount));
                    boolean success = false;
                    String[] numbers = list.getNumbers(false);
                    forwardRcsMessage(Arrays.asList(list.getNumbers()));
                }
                break;

            case REQUEST_SELECT_CONV:
                if (data != null) {
                    MessageItem smsMsgItem = mRcsForwardItems.get(0);
                    boolean success = RcsChatMessageUtils.sendRcsForwardMessage(
                            ComposeMessageActivity.this, null, data, smsMsgItem.mMsgId);
                    if (success) {
                        toast(R.string.forward_message_success);
                    } else {
                        toast(R.string.forward_message_fail);
                    }
                }
                break;

            case REQUEST_CODE_ATTACH_REPLACE_CONTACT_INFO:
                // Caused by user choose to replace the attachment, so we need remove
                // the attachment and then add the contact info to text.
                if (data != null) {
                    mWorkingMessage.removeAttachment(true);
                }
            case REQUEST_CODE_ATTACH_ADD_CONTACT_INFO:
                if (data != null) {
                    String newText = mWorkingMessage.getText() +
                        data.getStringExtra(MultiPickContactsActivity.EXTRA_INFO);
                    mWorkingMessage.setText(newText);
                }
                if (mIsRcsEnabled) {
                    mWorkingMessage.setRequiringRcsAttachment(false);
                }
                break;

            case REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD:
                if (data != null) {
                    String extraVCard = data.getStringExtra(MultiPickContactsActivity.EXTRA_VCARD);
                    if (extraVCard != null) {
                        Uri vcard = Uri.parse(extraVCard);
                        mRcsAttachmentUri = vcard;
                        addVcard(vcard);
                    }
                }
                break;

            case REQUEST_CODE_ADD_CONVERSATION:
                long threadId = data.getLongExtra("selectThreadId", -1);
                Conversation conv = Conversation.get(ComposeMessageActivity.this, threadId, false);
                ContactList recipients = conv.getRecipients();
                ContactList existing = mRecipientsEditor.constructContactsFromInput(false);
                for (Contact contact : existing) {
                    recipients.add(contact);
                }
                mRecipientsEditor.populate(recipients);
                break;

            default:
                if (LogTag.VERBOSE) log("bail due to unknown requestCode=" + requestCode);
                break;
        }
    }

    /**
     * Set newWorkingMessage's subject from mWorkingMessage. If we create a new
     * slideshow. We will drop the old workingMessage and create a new one. And
     * we should keep the subject of the old workingMessage.
     */
    private void setNewMessageSubject(WorkingMessage newWorkingMessage) {
        if (null != newWorkingMessage && mWorkingMessage.hasSubject()) {
            newWorkingMessage.setSubject(mWorkingMessage.getSubject(), true);
        }
    }

    private void updateMmsSizeIndicator() {
        mAttachmentEditorHandler.post(mUpdateMmsSizeIndRunnable);
    }

    private Runnable mUpdateMmsSizeIndRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWorkingMessage.getSlideshow() != null) {
                mWorkingMessage.getSlideshow().updateTotalMessageSize();
            }
            mAttachmentEditor.update(mWorkingMessage);
        }
    };

    private void processPickResult(final Intent data) {
        // The EXTRA_PHONE_URIS stores the phone's urls that were selected by user in the
        // multiple phone picker.
        Bundle bundle = data.getExtras().getBundle("result");
        if (null == bundle) {
            bundle = new Bundle();
        }
        Bundle onlyNumberBundle = data.getExtras().getBundle("result_only_number");
        final Bundle numberBundle = (null != onlyNumberBundle) ? onlyNumberBundle : (new Bundle());
        final Set<String> keySet = bundle.keySet();
        final int recipientCount = (keySet != null) ? keySet.size() : 0;
        final Set<String> numberKeySet = numberBundle.keySet();
        final int numberRecipientCount = (numberKeySet != null) ? numberKeySet.size() : 0;

        // If total recipients count > recipientLimit,
        // then forbid add reipients to RecipientsEditor
        final int recipientLimit = MmsConfig.getRecipientLimit();
        int totalRecipientsCount = mExistsRecipientsCount + recipientCount + numberRecipientCount;
        if (recipientLimit != Integer.MAX_VALUE && totalRecipientsCount > recipientLimit) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.too_many_recipients, totalRecipientsCount,
                            recipientLimit))
                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // if already exists some recipients,
                            // then new pick recipients with exists recipients count
                            // can't more than recipient limit count.
                            int newPickRecipientsCount = recipientLimit - mExistsRecipientsCount;
                            if (newPickRecipientsCount <= 0) {
                                return;
                            }
                            if (newPickRecipientsCount >= numberRecipientCount) {
                                inputNumbers(numberBundle, numberKeySet, numberRecipientCount);
                                newPickRecipientsCount -= numberRecipientCount;
                                processAddRecipients(keySet, newPickRecipientsCount);
                            } else {
                                inputNumbers(numberBundle, numberKeySet, newPickRecipientsCount);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return;
        }

        inputNumbers(numberBundle, numberKeySet, numberRecipientCount);
        processAddRecipients(keySet, recipientCount);
    }

    private void inputNumbers(final Bundle bundle,
                              final Set<String> keySet,
                              final int numberRecipientCount) {
        Iterator<String> it = keySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            String number = bundle.getStringArray(it.next())[0];
            mRecipientsEditor.append(number+",");
            if (i == numberRecipientCount) {
                break;
            }
        }
    }

    private Uri[] buildUris(final Set<String> keySet, final int newPickRecipientsCount) {
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

    private void processAddRecipients(final Set<String> keySet, final int newPickRecipientsCount) {
        // if process pick result that is pick recipients from Contacts
        mIsProcessPickedRecipients = true;
        final Handler handler = new Handler();
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getText(R.string.pick_too_many_recipients));
        progressDialog.setMessage(getText(R.string.adding_recipients));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        final Runnable showProgress = new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        };
        // Only show the progress dialog if we can not finish off parsing the return data in 1s,
        // otherwise the dialog could flicker.
        handler.postDelayed(showProgress, 1000);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final ContactList list;
                try {
                    list = ContactList.blockingGetByUris(buildUris(keySet, newPickRecipientsCount));
                } finally {
                    handler.removeCallbacks(showProgress);
                }
                if (mRecipientsEditor != null) {
                    ContactList exsitList = mRecipientsEditor.constructContactsFromInput(true);
                    // Remove the repeat recipients.
                    if (exsitList.equals(list)) {
                        exsitList.clear();
                        list.addAll(0, exsitList);
                    } else {
                        list.removeAll(exsitList);
                        list.addAll(0, exsitList);
                    }
                }
                // TODO: there is already code to update the contact header
                // widget and recipients
                // editor if the contacts change. we can re-use that code.
                final Runnable populateWorker = new Runnable() {
                    @Override
                    public void run() {
                        //Boolean param in order to allow the RecipientEntry to sort,
                        //no need for sort RecipientEntry who added by button,
                        //so whatever true or false.
                        mRecipientsEditor.populateWithAvatorDrawable(list);

                        // Set value for mRecipientsPickList and
                        // mRecipientsWatcher will update the UI.
                        mRecipientsPickList = list;
                        updateTitle(list);
                        // if process finished, then dismiss the progress dialog
                        progressDialog.dismiss();

                        // if populate finished, then recipients pick process
                        // end
                        mIsProcessPickedRecipients = false;

                        if (mIsRcsEnabled && mRcsShareVcard) {
                            rcsSend();
                        }
                        updateSendButtonState();
                    }
                };
                handler.post(populateWorker);
            }
        }, "ComoseMessageActivity.processPickResult").start();
    }

    private final ResizeImageResultCallback mResizeImageCallback = new ResizeImageResultCallback() {
        // TODO: make this produce a Uri, that's what we want anyway
        @Override
        public void onResizeResult(PduPart part, boolean append) {
            synchronized (mObjectLock) {
                mResizeImageCount = mResizeImageCount - 1;
                if (mResizeImageCount <= 0) {
                    log("finish resize all images.");
                    mObjectLock.notifyAll();
                }
            }

            if (part == null) {
                handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
                return;
            }

            Context context = ComposeMessageActivity.this;
            PduPersister persister = PduPersister.getPduPersister(context);
            int result;

            Uri messageUri = mWorkingMessage.saveAsMms(true);
            if (messageUri == null) {
                result = WorkingMessage.UNKNOWN_ERROR;
            } else {
                try {
                    Uri dataUri = persister.persistPart(part,
                            ContentUris.parseId(messageUri), null);
                    result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, dataUri, append);
                    if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("ResizeImageResultCallback: dataUri=" + dataUri);
                    }
                } catch (MmsException e) {
                    result = WorkingMessage.UNKNOWN_ERROR;
                }
            }

            updateMmsSizeIndicator();
            drawBottomPanel();
            handleAddAttachmentError(result, R.string.type_picture);
        }
    };

    private void handleAddAttachmentError(final int error, final int mediaTypeStringId) {
        if (error == WorkingMessage.OK) {
            return;
        }
        Log.d(TAG, "handleAddAttachmentError: " + error);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Resources res = getResources();
                String mediaType = res.getString(mediaTypeStringId);
                String title, message;

                switch(error) {
                case WorkingMessage.UNKNOWN_ERROR:
                    message = res.getString(R.string.failed_to_add_media, mediaType);
                    Toast.makeText(ComposeMessageActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                case WorkingMessage.UNSUPPORTED_TYPE:
                    title = res.getString(R.string.unsupported_media_format, mediaType);
                    message = res.getString(R.string.select_different_media, mediaType);
                    break;
                case WorkingMessage.MESSAGE_SIZE_EXCEEDED:
                    title = res.getString(R.string.exceed_message_size_limitation,
                        mediaType);
                    // We should better prompt the "message size limit reached,
                    // cannot send out message" while we send out the Mms.
                    if (mIsAttachmentErrorOnSend) {
                        message = res.getString(R.string.media_size_limit);
                        mIsAttachmentErrorOnSend = false;
                    } else {
                        message = res.getString(R.string.failed_to_add_media, mediaType);
                    }
                    break;
                case WorkingMessage.IMAGE_TOO_LARGE:
                    title = res.getString(R.string.failed_to_resize_image);
                    message = res.getString(R.string.resize_image_error_information);
                    break;
                case WorkingMessage.NEGATIVE_MESSAGE_OR_INCREASE_SIZE:
                    title = res.getString(R.string.illegal_message_or_increase_size);
                    message = res.getString(R.string.failed_to_add_media, mediaType);
                    break;
                default:
                    throw new IllegalArgumentException("unknown error " + error);
                }

                MessageUtils.showErrorDialog(ComposeMessageActivity.this, title, message);
            }
        });
    }

    private void addImageAsync(final Uri uri, final boolean append) {
        mInAsyncAddAttathProcess = true;
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                addImage(uri, append);
                mInAsyncAddAttathProcess = false;
            }
        }, null, R.string.adding_attachments_title);
    }

    private void addImage(final Uri uri, final boolean append) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("addImage: append=" + append + ", uri=" + uri);
        }

        int result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, uri, append);

        if (result == WorkingMessage.IMAGE_TOO_LARGE ||
            result == WorkingMessage.MESSAGE_SIZE_EXCEEDED) {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("resize image " + uri);
            }
            mResizeImageCount ++;
            MessageUtils.resizeImageAsync(ComposeMessageActivity.this,
                    uri, mWorkingMessage.hasSlideshow() ? mWorkingMessage.getSlideshow()
                            .getCurrentMessageSize() : 0, mAttachmentEditorHandler,
                    mResizeImageCallback, append);
            return;
        }

        if ((MmsConfig.isCreationModeEnabled())
                && (result == WorkingMessage.UNSUPPORTED_TYPE_WARNING)) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    addImage(uri, append);
                }
            };
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleUnsupportedTypeWarning(runnable);
                }
            });
            return;
        }
        updateMmsSizeIndicator();
        handleAddAttachmentError(result, R.string.type_picture);
    }

    private void addVideoAsync(final Uri uri, final boolean append) {
        mInAsyncAddAttathProcess = true;
        getAsyncDialog().runAsync(new Runnable() {
            @Override
            public void run() {
                addVideo(uri, append);
                mInAsyncAddAttathProcess = false;
            }
        }, null, R.string.adding_attachments_title);
    }

    private void addVideo(final Uri uri, final boolean append) {
        mInAsyncAddAttathProcess = true;
        if (uri != null) {
            int result = mWorkingMessage.setAttachment(WorkingMessage.VIDEO, uri, append);
            if (MmsConfig.isCreationModeEnabled()) {
                if (result == WorkingMessage.UNSUPPORTED_TYPE_WARNING) {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            addVideo(uri, append);
                            mInAsyncAddAttathProcess = false;
                        }
                    };
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleUnsupportedTypeWarning(runnable);
                        }
                    });
                    return;
                }

                if (result == WorkingMessage.MESSAGE_SIZE_EXCEEDED_WARNING) {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            addVideo(uri, append);
                        }
                    };
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                               handleMessageSizeExceededWarning(runnable);
                        }
                    });
                    return;
                }
            }
            updateMmsSizeIndicator();
            handleAddAttachmentError(result, R.string.type_video);
        }
    }

    private void addAudio(final Uri uri, final boolean append) {
        if (uri != null) {
            int result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, append);
            if (MmsConfig.isCreationModeEnabled()) {
                if (result == WorkingMessage.UNSUPPORTED_TYPE_WARNING) {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            addAudio(uri, append);
                        }
                    };
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleUnsupportedTypeWarning(runnable);
                        }
                    });
                    return;
                }

                if (result == WorkingMessage.MESSAGE_SIZE_EXCEEDED_WARNING) {
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            addAudio(uri, append);
                        }
                    };
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleMessageSizeExceededWarning(runnable);
                        }
                    });
                    return;
                }
            }

            updateMmsSizeIndicator();
            handleAddAttachmentError(result, R.string.type_audio);
        }
    }

    private void addVcard(Uri uri) {
        int result = mWorkingMessage.setAttachment(WorkingMessage.VCARD, uri, false);
        handleAddAttachmentError(result, R.string.type_vcard);
    }

    AsyncDialog getAsyncDialog() {
        if (mAsyncDialog == null) {
            mAsyncDialog = new AsyncDialog(this);
        }
        return mAsyncDialog;
    }

    private boolean handleForwardedMessage() {
        Intent intent = getIntent();

        // If this is a forwarded message, it will have an Intent extra
        // indicating so.  If not, bail out.
        if (!mForwardMessageMode) {
            if (mConversation != null) {
                mConversation.setHasMmsForward(false);
            }
            return false;
        }

        if (mConversation != null) {
            mConversation.setHasMmsForward(true);
            String[] recipientNumber = intent.getStringArrayExtra("msg_recipient");
            mConversation.setForwardRecipientNumber(recipientNumber);
        }
        Uri uri = intent.getParcelableExtra("msg_uri");

        if (Log.isLoggable(LogTag.APP, Log.DEBUG)) {
            log("" + uri);
        }

        if (uri != null) {
            mWorkingMessage = WorkingMessage.load(this, uri);
            mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
        } else {
            mWorkingMessage.setText(intent.getStringExtra("sms_body"));
        }

        // let's clear the message thread for forwarded messages
        mMsgListAdapter.changeCursor(null);

        return true;
    }

    // Handle send actions, where we're told to send a picture(s) or text.
    private boolean handleSendIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        }

        final String mimeType = intent.getType();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                final Uri uri = (Uri)extras.getParcelable(Intent.EXTRA_STREAM);
                boolean isRcsAvailable = mIsRcsEnabled && RcsUtils.isRcsOnline();
                if (isRcsAvailable && uri.toString().contains("as_vcard")) {
                    String vcardPath = RcsUtils.createVcardFile(this, uri);
                    mWorkingMessage.setVcardPath(vcardPath);
                    mRcsShareVcard = true;
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VCARD);
                    cacheWorkingMessage();
                    return false;
                }
                getAsyncDialog().runAsync(new Runnable() {
                    @Override
                    public void run() {
                        MmsApp.getApplication().getThumbnailManager().removeThumbnail(uri);
                        MmsApp.getApplication().getThumbnailManager().removeBackingStoreThumbnail(uri);
                        addAttachment(mimeType, uri, false);
                    }
                }, null, R.string.adding_attachments_title);
                return true;
            } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                mWorkingMessage.setText(extras.getString(Intent.EXTRA_TEXT));
                return true;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) &&
                extras.containsKey(Intent.EXTRA_STREAM)) {
            SlideshowModel slideShow = mWorkingMessage.getSlideshow();
            final ArrayList<Parcelable> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
            int currentSlideCount = slideShow != null ? slideShow.size() : 0;
            int importCount = uris.size();
            if (importCount + currentSlideCount > MmsConfig.getMaxSlideNumber()) {
                importCount = Math.min(MmsConfig.getMaxSlideNumber() - currentSlideCount,
                        importCount);
                Toast.makeText(ComposeMessageActivity.this,
                        getString(R.string.too_many_attachments,
                                MmsConfig.getMaxSlideNumber(), importCount),
                                Toast.LENGTH_LONG).show();
            }

            // Attach all the pictures/videos asynchronously off of the UI thread.
            // Show a progress dialog if adding all the slides hasn't finished
            // within half a second.
            final int numberToImport = importCount;
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    setRequestedOrientation(mIsLandscape ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    String type = mimeType;
                    for (int i = 0; i < numberToImport; i++) {
                        Parcelable uri = uris.get(i);
                        if (uri != null && "*/*".equals(mimeType)) {
                            type = getAttachmentMimeType((Uri) uri);
                        }
                        addAttachment(type, (Uri) uri, true);
                    }
                    updateMmsSizeIndicator();
                    synchronized (mObjectLock) {
                        if (mResizeImageCount != 0) {
                            waitForResizeImages();
                        }
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                }
            }, null, R.string.adding_attachments_title);
            return true;
        }
        return false;
    }

    private void waitForResizeImages() {
        try {
            log("wait for " + mResizeImageCount + " resize image finish. ");
            mObjectLock.wait();
        } catch (InterruptedException ex) {
            // try again by virtue of the loop unless mQueryPending is false
        }
    }

    private String getAttachmentMimeType(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String scheme = uri.getScheme();
        String attachmentType = "*/*";
        // Support uri with "content" scheme
        if ("content".equals(scheme)) {
            Cursor metadataCursor = null;
            try {
                metadataCursor = contentResolver.query(uri, new String[] {
                        Document.COLUMN_MIME_TYPE}, null, null, null);
            } catch (SQLiteException e) {
                // some content providers don't support the COLUMN_MIME_TYPE columns
                if (metadataCursor != null) {
                    metadataCursor.close();
                }
                metadataCursor = null;
            } catch (Exception e) {
                metadataCursor = null;
            }

            if (metadataCursor != null) {
                try {
                    if (metadataCursor.moveToFirst()) {
                        attachmentType = metadataCursor.getString(0);
                        Log.d(TAG, "attachmentType = " + attachmentType);
                    }
                 } finally {
                     metadataCursor.close();
                 }
            }
        } else if ("file".equals(scheme)) {
            String ext = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
            if (MessageUtils.is3GPP(ext) || MessageUtils.is3GPP2(ext)) {
                attachmentType = MessageUtils.getMimeType(uri.getPath(), ext);
                Log.d(TAG, "remap file " + ext + " to " + attachmentType);
            }
        }

        return attachmentType;
    }

    private boolean isAudioFile(Uri uri) {
        String path = uri.getPath();
        String mimeType = MediaFile.getMimeTypeForFile(path);
        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        return MediaFile.isAudioFileType(fileType);
    }

    private boolean isImageFile(Uri uri) {
        String path = uri.getPath();
        String mimeType = MediaFile.getMimeTypeForFile(path);
        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        return MediaFile.isImageFileType(fileType);
    }

    private boolean isVideoFile(Uri uri) {
        String path = uri.getPath();
        String mimeType = MediaFile.getMimeTypeForFile(path);
        int fileType = MediaFile.getFileTypeForMimeType(mimeType);
        return MediaFile.isVideoFileType(fileType);
    }

    // mVideoUri will look like this: content://media/external/video/media
    private static final String mVideoUri = Video.Media.getContentUri("external").toString();
    // mImageUri will look like this: content://media/external/images/media
    private static final String mImageUri = Images.Media.getContentUri("external").toString();
    // mAudioUri will look like this: content://media/external/audio/media
    private static final String mAudioUri = Audio.Media.getContentUri("external").toString();

    private void addAttachment(String type, Uri uri, boolean append) {
        if (uri != null) {
            // When we're handling Intent.ACTION_SEND_MULTIPLE, the passed in items can be
            // videos, and/or images, and/or some other unknown types we don't handle. When
            // a single attachment is "shared" the type will specify an image or video. When
            // there are multiple types, the type passed in is "*/*". In that case, we've got
            // to look at the uri to figure out if it is an image or video.

            if (MmsConfig.isCreationModeEnabled()) {
                ContentRestrictionFactory.reset();
            }

            boolean wildcard = "*/*".equals(type);
            if (type.startsWith("image/") || (wildcard && uri.toString().startsWith(mImageUri))
                    || (wildcard && isImageFile(uri))) {
                addImage(uri, append);
            } else if (type.startsWith("video/") ||
                    (wildcard && uri.toString().startsWith(mVideoUri))
                    || (wildcard && isVideoFile(uri))) {
                addVideo(uri, append);
            } else if (type.startsWith("audio/")
                    || (wildcard && uri.toString().startsWith(mAudioUri))
                    || (wildcard && isAudioFile(uri))) {
                addAudio(uri, append);
            } else if (this.getResources().getBoolean(R.bool.config_vcard)
                    && (type.equals("text/x-vcard")
                    || (wildcard && isVcardFile(uri)))) {
                addVcard(uri);
            } else {
                // Add prompt when file type is not image/video/audio.
                Message msg = Message.obtain(mAddAttachmentHandler,
                        MSG_ADD_ATTACHMENT_FAILED, uri);
                mAddAttachmentHandler.sendMessage(msg);
            }
        }
    }

    // handler for handle add attachment failt.
    private Handler mAddAttachmentHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_ATTACHMENT_FAILED:
                    Toast.makeText(ComposeMessageActivity.this,
                            getAttachmentPostfix((Uri) msg.obj), Toast.LENGTH_SHORT)
                            .show();
                    break;
                default:
                    break;
            }
        }

        private String getAttachmentPostfix(Uri uri) {
            // if uri is valid,parse it as normal.
            if (isValidUri(uri)) {
                int lastDot = uri.toString().lastIndexOf(".");
                String postfix = uri.toString().substring(lastDot + 1);
                return getResourcesString(R.string.unsupported_media_format,
                        postfix);
            } else {
                // if uri is invalid,show just show unsupported "Unsupported format".
                return getResources().getString(R.string.unsupported_format);
            }
        }

        //Used to check the uri is valid or not.
        private boolean isValidUri(Uri uri) {
            String path = uri == null ? null : uri.toString();
            if (null != path && path.contains("/")) {
                String fileName = path.substring(path.lastIndexOf("/"));
                if (null != fileName && !fileName.isEmpty()
                        && fileName.contains(".")) {
                    String fileType = fileName.substring(fileName
                            .lastIndexOf(".") + 1);
                    return !fileType.isEmpty() && fileType.trim().length() > 0
                            && fileType != "";
                }
            }
            return false;
        }
    };

    private String getResourcesString(int id, String mediaName) {
        Resources r = getResources();
        return r.getString(id, mediaName);
    }

    /**
     * draw the compose view at the bottom of the screen.
     */
    private void drawBottomPanel() {
        // Reset the counter for text editor.
        resetCounter();

        if (mWorkingMessage.hasSlideshow()) {
            if (mShowTwoButtons) {
                mTextEditor.setVisibility(View.GONE);
                mAttachmentEditor.requestFocus();
                return;
            } else {
                mTextEditor.setVisibility(View.INVISIBLE);
                mTextEditor.setText("");
                mAttachmentEditor.hideSlideshowSendButton();
                mAttachmentEditor.requestFocus();
                return;
            }
        }

        if (LOCAL_LOGV) {
            Log.v(TAG, "CMA.drawBottomPanel");
        }
        if (mTextEditor.getVisibility() != View.VISIBLE) {
            mTextEditor.setVisibility(View.VISIBLE);
        }

        CharSequence text = mWorkingMessage.getText();

        // TextView.setTextKeepState() doesn't like null input.
        if (text != null && mIsSmsEnabled) {
            mTextEditor.setTextKeepState(text);

            // Set the edit caret to the end of the text.
            mTextEditor.setSelection(mTextEditor.length());
        } else {
            mTextEditor.setText("");
        }
        onKeyboardStateChanged();
    }

    private void hideBottomPanel() {
        if (LOCAL_LOGV) {
            Log.v(TAG, "CMA.hideBottomPanel");
        }
        mBottomPanel.setVisibility(View.INVISIBLE);
    }

    private void drawTopPanel(boolean showSubjectEditor) {
        boolean showingAttachment = mAttachmentEditor.update(mWorkingMessage);
        mAttachmentEditorScrollView.setVisibility(showingAttachment ? View.VISIBLE : View.GONE);
        showSubjectEditor(showSubjectEditor || mWorkingMessage.hasSubject());
        int subjectSize = mWorkingMessage.hasSubject()
                ? mWorkingMessage.getSubject().toString().getBytes().length : 0;
        if (mWorkingMessage.getSlideshow()!= null) {
            mWorkingMessage.getSlideshow().setSubjectSize(subjectSize);
        }
        if (mShowTwoButtons) {
            mAttachmentEditor.hideSlideshowSendButton();
        }

        invalidateOptionsMenu();
        onKeyboardStateChanged();
    }

    //==========================================================
    // Interface methods
    //==========================================================


    @Override
    public void onClick(View v) {
        mIsRTL = (v.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        if ((v == mSendButtonSms || v == mSendButtonMms) && isPreparedForSending()) {
            if (v == mSendButtonSms) {
                LogTag.debugD("send SMS button clicked");
            } else {
                LogTag.debugD("send MMS button clicked");
            }
            try {
                if (MessageUtils.isWfcUnavailable(getContext())) {
                    MessageUtils.pupConnectWifiAlertDialog(getContext());
                    MessageUtils.pupConnectWifiNotification(getContext());
                    LogTag.debugD("not send for pupConnectWifiAlertDialog");
                    return;
                }
            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }
            if (mShowTwoButtons) {
                confirmSendMessageIfNeeded(SubscriptionManager.getSubId(PhoneConstants.SUB1)[0]);
            } else {
                if (mIsRcsEnabled) {
                    if (RcsUtils.isRcsOnline()
                            && ((mWorkingMessage.getCacheRcsMessage() || (!mWorkingMessage
                                    .requiresMms() && mWorkingMessage.hasText())))) {
                        rcsSend();
                    } else {
                        if (mConversation.isGroupChat()) {
                            toast(R.string.rcs_offline_unable_to_send);
                            return;
                        }
                        if (mIsBurnMessage) {
                            toast(R.string.rcs_not_online_can_not_send_burn_message);
                            mIsBurnMessage = false;
                            return;
                        } else {
                            confirmSendMessageIfNeeded();
                        }
                    }
                } else {
                    confirmSendMessageIfNeeded();
                }
            }
        } else if ((v == mSendButtonSmsViewSec || v == mSendButtonMmsViewSec) &&
                mShowTwoButtons && isPreparedForSending()) {
            confirmSendMessageIfNeeded(SubscriptionManager.getSubId(PhoneConstants.SUB2)[0]);
        } else if (v == mRecipientsPicker) {
            if (mIsRcsEnabled) {
                addGrouChatWayOrConversation(new addGrouChatWayClickListener());
            } else {
                launchMultiplePhonePicker();
            }
        } else if (v == mRecipientsPickerGroups) {
            launchRcsContactGroupPicker(REQUEST_CODE_PICK);
        } else if ((v == mAddAttachmentButton)) {
            if (mAttachmentSelector.getVisibility() == View.VISIBLE && !mIsReplaceAttachment) {
                mAttachmentSelector.setVisibility(View.GONE);
            } else {
                showAttachmentSelector(false);
                if (mWorkingMessage.hasAttachment()) {
                    Toast.makeText(this, R.string.add_another_attachment, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } else if (v == mButtonEmoj) {
            if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
                mAttachmentSelector.setVisibility(View.GONE);
            }
            ViewStub viewStub = (ViewStub) findViewById(R.id.view_stub);
            showEmojiView(viewStub);
        } else if (v == mBackView) {
            exitComposeMessageActivity(new Runnable() {
                @Override
                public void run() {
                    goToConversationList();
                }
            });
        } else if (v == mClearButton) {
            mRecipientsEditor.setText("");
        }
    }

    private void launchMultiplePhonePicker() {
        Intent intent = new Intent(INTENT_MULTI_PICK_ACTION, Contacts.CONTENT_URI);
        String exsitNumbers = mRecipientsEditor.getExsitNumbers();
        if (!TextUtils.isEmpty(exsitNumbers)) {
            intent.putExtra(Intents.EXTRA_PHONE_URIS, exsitNumbers);
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.contact_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null) {
            // if shift key is down, then we want to insert the '\n' char in the TextView;
            // otherwise, the default action is to send the message.
            if (!event.isShiftPressed() && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded();
                }
                return true;
            }
            return false;
        }

        if (isPreparedForSending()) {
            confirmSendMessageIfNeeded();
        }
        return true;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        private boolean mIsChanged = false;
        private String mTextBefore = "";

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (!mIsChanged) {
                mTextBefore = s.length() > 0 ? s.toString() : "";
             }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mIsChanged) {
                return;
            }
            if (mWorkingMessage.hasAttachment()) {
                if ((before != count) && !mAttachmentEditor.canAddTextForMms(s)) {
                    if (mTextEditor != null) {
                        mIsChanged = true;
                        mTextEditor.setText(mTextBefore);
                        mIsChanged = false;
                        Toast.makeText(ComposeMessageActivity.this,
                                R.string.cannot_add_text_anymore, Toast.LENGTH_SHORT).show();
                    }
                    mAttachmentEditor.canAddTextForMms(mTextBefore);
                    return;
                }
            }
            // This is a workaround for bug 1609057.  Since onUserInteraction() is
            // not called when the user touches the soft keyboard, we pretend it was
            // called when textfields changes.  This should be removed when the bug
            // is fixed.
            onUserInteraction();

            mWorkingMessage.setText(s);

            updateSendButtonState();

            updateCounter(s, start, before, count);

            ensureCorrectButtonHeight();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Ensures that if the text edit box extends past two lines then the
     * button will be shifted up to allow enough space for the character
     * counter string to be placed beneath it.
     */
    private void ensureCorrectButtonHeight() {
        int currentTextLines = mTextEditor.getLineCount();
        if (currentTextLines <= 2) {
            mTextCounter.setVisibility(View.GONE);
            if (mShowTwoButtons) {
                mTextCounterSec.setVisibility(View.GONE);
            }

        }
        else if (currentTextLines > 2 && mTextCounter.getVisibility() == View.GONE) {
            // Making the counter invisible ensures that it is used to correctly
            // calculate the position of the send button even if we choose not to
            // display the text.
            mTextCounter.setVisibility(View.INVISIBLE);
        }
    }

    private final TextWatcher mSubjectEditorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().getBytes().length <= SUBJECT_MAX_LENGTH) {
                mWorkingMessage.setSubject(s, true);
                updateSendButtonState();
                if (s.toString().getBytes().length == SUBJECT_MAX_LENGTH
                        && before < SUBJECT_MAX_LENGTH) {
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.subject_full, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().getBytes().length > SUBJECT_MAX_LENGTH) {
                String subject = s.toString();
                Toast.makeText(ComposeMessageActivity.this,
                        R.string.subject_full, Toast.LENGTH_SHORT).show();
                while (subject.getBytes().length > SUBJECT_MAX_LENGTH) {
                    subject = subject.substring(0, subject.length() - 1);
                }
                s.clear();
                s.append(subject);
            }
        }
    };

    //==========================================================
    // Private methods
    //==========================================================

    /**
     * Initialize all UI elements from resources.
     */
    private void initResourceRefs() {

        View inflate = getLayoutInflater().inflate(R.layout.compose_message_activity, null);
        mZoomGestureOverlayView = new ZoomGestureOverlayView(this);
        mZoomGestureOverlayView.addZoomListener(this);
        mZoomGestureOverlayView.addView(inflate);
        mZoomGestureOverlayView.setEventsInterceptionEnabled(true);
        mZoomGestureOverlayView.setGestureVisible(false);
        setContentView(mZoomGestureOverlayView);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolBar);
        if (sPrimaryColorDark == 0) {
            sPrimaryColorDark = getResources().getColor(R.color.primary_color_dark);
        }
        mBackView = (ImageButton)findViewById(R.id.back_view);
        mBackView.setOnClickListener(this);
        mDeviderView = findViewById(R.id.subject_text_editor_divider);

        mMsgListView = (MessageListView) findViewById(R.id.history);
        mMsgListView.setDivider(null);      // no divider so we look like IM conversation.
        if (mIsRcsEnabled) {
            mMsgListView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            if (mRcsEmojiInitialize != null)
                                mRcsEmojiInitialize.closeViewAndKB();
                            RcsUtils.closeKB(ComposeMessageActivity.this);
                            break;
                    }
                    return false;
                }
            });
        }

        // called to enable us to show some padding between the message list and the
        // input field but when the message list is scrolled that padding area is filled
        // in with message content
        mMsgListView.setClipToPadding(false);

        mMsgListView.setOnSizeChangedListener(new OnSizeChangedListener() {
            public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.v(TAG, "onSizeChanged: w=" + width + " h=" + height +
                            " oldw=" + oldWidth + " oldh=" + oldHeight);
                }

                if (!mMessagesAndDraftLoaded && (oldHeight-height > SMOOTH_SCROLL_THRESHOLD)) {
                    // perform the delayed loading now, after keyboard opens
                    loadMessagesAndDraft(3);
                }


                // The message list view changed size, most likely because the keyboard
                // appeared or disappeared or the user typed/deleted chars in the message
                // box causing it to change its height when expanding/collapsing to hold more
                // lines of text.
                smoothScrollToEnd(false, height - oldHeight);
            }
        });
        //Let listView scroll to bottom
        if (mIsRcsEnabled) {
            mMsgListView.setSelection(mMsgListView.getBottom());
        }
        if (mShowTwoButtons) {
            initTwoSendButton();
        } else {
            mBottomPanel = findViewById(R.id.bottom_panel);
            mBottomPanel.setVisibility(View.VISIBLE);
            mTextEditor = (EditText) findViewById(R.id.embedded_text_editor);
            mButtonEmoj = (ImageButton)findViewById(R.id.send_emoj);
            mTextCounter = (TextView) findViewById(R.id.text_counter);
            mAddAttachmentButton = (ImageButton) findViewById(R.id.add_attachment_first);
            mSendButtonMms = (ImageButton) findViewById(R.id.send_button_mms);
            mSendButtonMmsText = (TextView) findViewById(R.id.send_button_mms_text);
            mSendButtonSms = (ImageButton) findViewById(R.id.send_button_sms);
            mAddAttachmentButton.setOnClickListener(this);
            mButtonEmoj.setOnClickListener(this);
            mSendButtonMms.setOnClickListener(this);
            mSendButtonSms.setOnClickListener(this);
        }
        mTextEditor.setOnEditorActionListener(this);
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        mIsEnableSelectCopy = sp.getBoolean(MessagingPreferenceActivity.ENABLE_SELECTABLE_COPY,
                MessagingPreferenceActivity.ENABLE_SELECTABLE_COPY_DEFAULT_VALUE);
        float mTextSize = sp.getFloat(MessagingPreferenceActivity.ZOOM_MESSAGE,
                MmsConfig.DEFAULT_FONT_SIZE);
        mTextEditor.setTextSize((int) mTextSize + ZoomMessageListItem.DIFF_FONT_SIZE);
        if (getResources().getInteger(R.integer.limit_count) == 0) {
            mTextEditor.setFilters(new InputFilter[] {
                    new LengthFilter(MmsConfig.getMaxTextLimit())});
        } else if (getResources().getInteger(R.integer.slide_text_limit_size) != 0) {
            mTextEditor.setFilters(new InputFilter[] {
                    new LengthFilter(getResources().getInteger(R.integer.slide_text_limit_size))});
        }
        if (getResources().getInteger(R.integer.limit_count) == 0) {
            mTextEditor.setFilters(new InputFilter[] {
                    new LengthFilter(MmsConfig.getMaxTextLimit())});
        } else if (getResources().getInteger(R.integer.slide_text_limit_size) != 0) {
            mTextEditor.setFilters(new InputFilter[] {
                    new LengthFilter(getResources().getInteger(R.integer.slide_text_limit_size))});
        }
        mTextEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && mAttachmentSelector.getVisibility() == View.VISIBLE) {
                    mAttachmentSelector.setVisibility(View.GONE);
                }
                if (hasFocus && mIsRcsEnabled && mRcsEmojiInitialize != null
                        && mRcsEmojiInitialize.getEmojiView().getVisibility() == View.VISIBLE) {
                    mRcsEmojiInitialize.closeViewAndKB();
                }
            }
        });

        mTopPanel = findViewById(R.id.recipients_subject_linear);
        mTopPanel.setFocusable(false);
        mAttachmentEditor = (AttachmentEditor) findViewById(R.id.attachment_editor);
        mAttachmentEditor.setHandler(mAttachmentEditorHandler);
        mAttachmentEditorScrollView = findViewById(R.id.attachment_editor_scroll_view);
        mAttachmentSelector = findViewById(R.id.attachments_selector);
        mRcsThumbnailLayout = findViewById(R.id.rcs_cache_view);
    }

    private void initTwoSendButton() {
        mBottomPanel = findViewById(R.id.bottom_panel_btnstyle);
        mBottomPanel.setVisibility(View.VISIBLE);
        mTextEditor = (EditText) findViewById(R.id.embedded_text_editor_btnstyle);
        mButtonEmoj = (ImageButton)findViewById(R.id.send_emoj_btnstyle);
        mTextCounter = (TextView) findViewById(R.id.first_text_counter);
        mAddAttachmentButton = (ImageButton) findViewById(R.id.add_attachment_second);
        mSendButtonMms = (ImageButton) findViewById(R.id.first_send_button_mms_view);
        mSendButtonSms = (ImageButton) findViewById(R.id.first_send_button_sms_view);
        mSendLayoutMmsFir = findViewById(R.id.first_send_button_mms);
        mSendLayoutSmsFir = findViewById(R.id.first_send_button_sms);
        mIndicatorForSimMmsFir = (ImageView) findViewById(R.id.first_sim_card_indicator_mms);
        mIndicatorForSimSmsFir = (ImageView) findViewById(R.id.first_sim_card_indicator_sms);
        mIndicatorForSimMmsFir.setImageDrawable(MessageUtils
               .getMultiSimIcon(this, PhoneConstants.SUB1));
        mIndicatorForSimSmsFir.setImageDrawable(MessageUtils
                .getMultiSimIcon(this, PhoneConstants.SUB1));
        mAddAttachmentButton.setOnClickListener(this);
        mButtonEmoj.setOnClickListener(this);
        mSendButtonMms.setOnClickListener(this);
        mSendButtonSms.setOnClickListener(this);

        mTextCounterSec = (TextView) findViewById(R.id.second_text_counter);
        mSendButtonMmsViewSec = (TextView) findViewById(R.id.second_send_button_mms_view);
        mSendButtonSmsViewSec = (ImageButton) findViewById(R.id.second_send_button_sms_view);
        mSendLayoutMmsSec = findViewById(R.id.second_send_button_mms);
        mSendLayoutSmsSec = findViewById(R.id.second_send_button_sms);
        mIndicatorForSimMmsSec = (ImageView) findViewById(R.id.second_sim_card_indicator_mms);
        mIndicatorForSimSmsSec = (ImageView) findViewById(R.id.second_sim_card_indicator_sms);
        mIndicatorForSimMmsSec.setImageDrawable(MessageUtils
               .getMultiSimIcon(this, PhoneConstants.SUB2));
        mIndicatorForSimSmsSec.setImageDrawable(MessageUtils
                .getMultiSimIcon(this, PhoneConstants.SUB2));
        mSendButtonMmsViewSec.setOnClickListener(this);
        mSendButtonSmsViewSec.setOnClickListener(this);
    }

    private void confirmDeleteDialog(OnClickListener listener, boolean locked) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(locked ? R.string.confirm_delete_locked_message :
                    R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.delete, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    void undeliveredMessageDialog(long date) {
        String body;

        if (date >= 0) {
            body = getString(R.string.undelivered_msg_dialog_body,
                    MessageUtils.formatTimeStampString(this, date));
        } else {
            // FIXME: we can not get sms retry time.
            body = getString(R.string.undelivered_sms_dialog_body);
        }

        Toast.makeText(this, body, Toast.LENGTH_LONG).show();
    }

    private Runnable messageListQueryRunnable = new Runnable() {
        @Override
        public void run() {
            startMsgListQuery(MESSAGE_LIST_QUERY_TOKEN);
        }
    };

    private void startMsgListQuery() {
        mHandler.removeCallbacks(messageListQueryRunnable);
        mHandler.postDelayed(messageListQueryRunnable, 200);
    }

    private void startMsgListQuery(int token) {
        if (mSendDiscreetMode || MessageUtils.isMailboxMode()) {
            return;
        }
        Uri conversationUri = mConversation.getUri();

        if (conversationUri == null) {
            log("##### startMsgListQuery: conversationUri is null, bail!");
            return;
        }

        long threadId = mConversation.getThreadId();
        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("startMsgListQuery for " + conversationUri + ", threadId=" + threadId +
                    " token: " + token + " mConversation: " + mConversation);
        }

        // Cancel any pending queries
        mBackgroundQueryHandler.cancelOperation(token);
        try {
            // Kick off the new query
            mBackgroundQueryHandler.startQuery(
                    token,
                    threadId /* cookie */,
                    conversationUri,
                    PROJECTION,
                    null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void initMessageList() {
        if (mMsgListAdapter != null) {
            LogTag.debugD("setOnDataSetChangedListener");
            mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
            return;
        }

        String highlightString = getIntent().getStringExtra("highlight");
        Pattern highlight = highlightString == null
            ? null
            : Pattern.compile("\\b" + Pattern.quote(highlightString), Pattern.CASE_INSENSITIVE);

        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new MessageListAdapter(this, null, mMsgListView, true, highlight);
        mMsgListAdapter.setIsMsimIccCardActived(MessageUtils.isMsimIccCardActive());
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListAdapter.setMMSAudioPlayer(mMMSAudioPlayer);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility((mSendDiscreetMode || MessageUtils.isMailboxMode())
                ? View.INVISIBLE : View.VISIBLE);
        mMsgListView.setOnItemClickListener(new OnItemDoubleClickListener() {
            @Override
            public void onItemSingleClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null && view instanceof MessageListItem) {
                    ((MessageListItem) view).onMessageListItemClick();
                }
            }

            @Override
            public void onItemDoubleClick(AdapterView<?> parent, View view, int position, long id) {
                if (mIsEnableSelectCopy && view != null && view instanceof MessageListItem) {
                    ((MessageListItem) view).startSelectableCopyActivity();
                }
            }
        });
        mModeCallback = new ModeCallback();
        mMsgListView.setMultiChoiceModeListener(mModeCallback);
        mMsgListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mFileTranferReceiver = new ComposeMessageFileTransferReceiver(mMsgListAdapter);
        mCloudFileReceiver = new ComposeMessageCloudFileReceiver(mMsgListAdapter, getListView());
    }

    /**
     * Load the draft
     *
     * If mWorkingMessage has content in memory that's worth saving, return false.
     * Otherwise, call the async operation to load draft and return true.
     */
    private boolean loadDraft() {
        if (mWorkingMessage.isWorthSaving()) {
            Log.w(TAG, "CMA.loadDraft: called with non-empty working message, bail");
            if (mConversation.hasDraft() &&
                    mConversation.getMessageCount() == 0) {
                mWorkingMessage.asyncDeleteDraftSmsMessage(mConversation);
                mConversation.clearThreadId();
                mWorkingMessage.setConversation(mConversation);
            }
            return false;
        }

        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("CMA.loadDraft");
        }

        mWorkingMessage = WorkingMessage.loadDraft(this, mConversation,
                new Runnable() {
                    @Override
                    public void run() {
                        updateMmsSizeIndicator();
                        // It decides whether or not to display the subject editText view,
                        // according to the situation whether there's subject
                        // or the editText view is visible before leaving it.
                        drawTopPanel(isSubjectEditorVisible());
                        drawBottomPanel();
                        updateSendButtonState();
                    }
                });

        // WorkingMessage.loadDraft() can return a new WorkingMessage object that doesn't
        // have its conversation set. Make sure it is set.
        mWorkingMessage.setConversation(mConversation);

        return true;
    }

    private void saveDraft(boolean isStopping) {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            LogTag.debug("saveDraft");
        }
        // TODO: Do something better here.  Maybe make discard() legal
        // to call twice and make isEmpty() return true if discarded
        // so it is caught in the clause above this one?
        if (mWorkingMessage.isDiscarded()) {
            return;
        }

        if (mIsRcsEnabled && mWorkingMessage.isBurn()) {
            return;
        }
        if ((!mWaitingForSubActivity &&
                !mWorkingMessage.isWorthSaving() &&
                (!isRecipientsEditorVisible() || recipientCount() == 0)) ||
                MessageUtils.checkIsPhoneMemoryFull(this)) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                log("not worth saving, discard WorkingMessage and bail");
            }
            mWorkingMessage.discard();
            return;
        }

        mWorkingMessage.saveDraft(isStopping);

        if (mToastForDraftSave) {
            Toast.makeText(this, R.string.message_saved_as_draft,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPreparedForSending() {
        if (mIsAirplaneModeOn && !TelephonyManager.getDefault().isImsRegistered()) {
            LogTag.debugD("airplane mode on and ims not registered");
            return false;
        }

        int recipientCount = recipientCount();
        if (mIsRcsEnabled) {
            if (mConversation.isGroupChat()) {
                return (recipientCount > 0 && (mWorkingMessage.hasRcsAttach() || mWorkingMessage
                        .hasText())) && mConversation.isGroupChatActive();
            }
            if (mConversation.isPcChat()) {
                return (recipientCount > 0 && (mWorkingMessage.hasRcsAttach() || mWorkingMessage
                        .hasText())) && mConversation.isMyPcConversation();
            }
            if (recipientCount > 0 && mWorkingMessage.getCacheRcsMessage()) {
                return true;
            }
        }

        if (getContext().getResources().getBoolean(R.bool.enable_send_blank_message)) {
            Log.d(TAG, "Blank SMS");
            return (MessageUtils.getActivatedIccCardCount() > 0 || isCdmaNVMode()) &&
                    recipientCount > 0 && recipientCount <= MmsConfig.getRecipientLimit() &&
                    mIsSmsEnabled;
        } else {
            return (MessageUtils.getActivatedIccCardCount() > 0 || isCdmaNVMode() ||
                    TelephonyManager.getDefault().isImsRegistered()) &&
                    recipientCount > 0 && recipientCount <= MmsConfig.getRecipientLimit() &&
                    mIsSmsEnabled &&
                    (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText() ||
                        mWorkingMessage.hasSubject());

        }
    }

    private BroadcastReceiver mAirplaneModeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                updateSendButtonState();
            }
        }
    };

    private boolean isCdmaNVMode() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            Log.d(TAG, "isCdmaNVMode: CDMA NV mode just for single SIM");
            return false;
        }
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
        int cdmaSubscriptionMode = Settings.Global.getInt(getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, CDMA_SUBSCRIPTION_NV);
        Log.d(TAG, "isCdmaNVMode: activePhoneType=" + activePhoneType + " cdmaSubscriptionMode="
                + cdmaSubscriptionMode);
        if ((activePhoneType == TelephonyManager.PHONE_TYPE_CDMA) &&
                cdmaSubscriptionMode == CDMA_SUBSCRIPTION_NV) {
            return true;
        }
        return false;
    }

    private int recipientCount() {
        int recipientCount;

        // To avoid creating a bunch of invalid Contacts when the recipients
        // editor is in flux, we keep the recipients list empty.  So if the
        // recipients editor is showing, see if there is anything in it rather
        // than consulting the empty recipient list.
        if (isRecipientsEditorVisible()) {
            recipientCount = mRecipientsEditor.getRecipientCount();
        } else {
            recipientCount = getRecipients().size();
        }
        return recipientCount;
    }

    private boolean checkMessageSizeExceeded(){
        int messageSizeLimit = MmsConfig.getMaxMessageSize();
        int mmsCurrentSize = 0;
        boolean indicatorSizeOvered = false;
        SlideshowModel slideShow = mWorkingMessage.getSlideshow();
        if (slideShow != null) {
            mmsCurrentSize = slideShow.getTotalMessageSize();
            // The AttachmentEditor only can edit text if there only one silde.
            // And the slide already includes text size, need to recalculate the total size.
            if (mWorkingMessage.hasText() && slideShow.size() == 1) {
                int totalTextSize = slideShow.getTotalTextMessageSize();
                int currentTextSize = mWorkingMessage.getText().toString().getBytes().length;
                int subjectSize = slideShow.getSubjectSize();
                mmsCurrentSize = mmsCurrentSize - totalTextSize + currentTextSize;
                indicatorSizeOvered = getSizeWithOverHead(mmsCurrentSize + subjectSize)
                        > (MmsConfig.getMaxMessageSize() / KILOBYTE);
            }
        } else if (mWorkingMessage.hasText()) {
            mmsCurrentSize = mWorkingMessage.getText().toString().getBytes().length;
        }
        Log.v(TAG, "compose mmsCurrentSize = " + mmsCurrentSize
                + ", indicatorSizeOvered = " + indicatorSizeOvered);
        // Mms max size is 300k, but we reserved 1k just in case there are other over size problem.
        // In this way, here the first condition will always false.
        // Therefore add indicatorSizeOvered in it.
        // If indicator displays larger than 300k, it can not send this Mms.
        if (mmsCurrentSize > messageSizeLimit || indicatorSizeOvered) {
            mIsAttachmentErrorOnSend = true;
            handleAddAttachmentError(WorkingMessage.MESSAGE_SIZE_EXCEEDED,
                    R.string.type_picture);
            return true;
        }
        return false;
    }

    private int getSizeWithOverHead(int size) {
        return (size + KILOBYTE -1) / KILOBYTE + 1;
    }

    private void sendMessage(boolean bCheckEcmMode) {
        LogTag.debugD("sendMessage true");
        if (mIsRcsEnabled && hasConvertRcsAttachmentToMmsAndSent()) {
            return;
        }
        // Check message size, if >= max message size, do not send message.
        if(checkMessageSizeExceeded()){
            LogTag.debugD("MessageSizeExceeded");
            return;
        }

        if (bCheckEcmMode) {
            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    startActivityForResult(
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    Log.e(TAG, "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }

        // Make the recipients editor lost focus, recipients editor will shrink
        // and filter useless char in recipients to avoid send sms failed.
        if (isRecipientsEditorVisible()
                && mRecipientsEditor.isFocused()
                && !mWorkingMessage.requiresMms()) {
            mTextEditor.requestFocus();
        }

        if (!mSendingMessage) {
            if (LogTag.SEVERE_WARNING) {
                String sendingRecipients = mConversation.getRecipients().serialize();
                if (!sendingRecipients.equals(mDebugRecipients)) {
                    String workingRecipients = mWorkingMessage.getWorkingRecipients();
                    if (workingRecipients != null && !mDebugRecipients.equals(workingRecipients)) {
                        LogTag.warnPossibleRecipientMismatch("ComposeMessageActivity.sendMessage" +
                                " recipients in window: \"" +
                                mDebugRecipients + "\" differ from recipients from conv: \"" +
                                sendingRecipients + "\" and working recipients: " +
                                workingRecipients, this);
                    }
                }
                sanityCheckConversation();
            }

            // send can change the recipients. Make sure we remove the listeners first and then add
            // them back once the recipient list has settled.
            removeRecipientsListeners();


            if (mWorkingMessage.getResendMultiRecipients()) {
                // If resend sms recipient is more than one, use mResendSmsRecipient
                LogTag.debugD("mWorkingMessage send mResendSmsRecipient=" + mResendSmsRecipient);
                mWorkingMessage.send(mResendSmsRecipient);
            } else {
                LogTag.debugD("mWorkingMessage send mDebugRecipients=" + mDebugRecipients);
                mWorkingMessage.send(mDebugRecipients);
            }

            mSentMessage = true;
            mSendingMessage = true;
            addRecipientsListeners();

            mScrollOnSend = true;   // in the next onQueryComplete, scroll the list to the end.
        }
        // But bail out if we are supposed to exit after the message is sent.
        if (mSendDiscreetMode || MessageUtils.isMailboxMode()) {
            finish();
        }
    }

    private void resetMessage() {
        if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            log("resetMessage");
        }

        // Make the attachment editor hide its view.
        mAttachmentEditor.hideView();
        mAttachmentEditorScrollView.setVisibility(View.GONE);

        // Hide the subject editor.
        showSubjectEditor(false);

        // Focus to the text editor.
        mTextEditor.requestFocus();

        // We have to remove the text change listener while the text editor gets cleared and
        // we subsequently turn the message back into SMS. When the listener is listening while
        // doing the clearing, it's fighting to update its counts and itself try and turn
        // the message one way or the other.
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);

        // Clear the text box.
        TextKeyListener.clear(mTextEditor.getText());

        mWorkingMessage.clearConversation(mConversation, false);
        mWorkingMessage = WorkingMessage.createEmpty(this);
        mWorkingMessage.setConversation(mConversation);

        hideRecipientEditor();
        drawBottomPanel();

        // "Or not", in this case.
        updateSendButtonState();

        // Our changes are done. Let the listener respond to text changes once again.
        mTextEditor.addTextChangedListener(mTextEditorWatcher);

        // Close the soft on-screen keyboard if we're in landscape mode so the user can see the
        // conversation.
        if (mIsLandscape || isInMultiWindowMode()) {
            hideKeyboard();
        }

        mLastRecipientCount = 0;
        mSendingMessage = false;
        invalidateOptionsMenu();
        if (mAttachmentSelector.getVisibility() == View.VISIBLE) {
            mAttachmentSelector.setVisibility(View.GONE);
        }
        if (mIsRcsEnabled) {
            cancelRcsMessageCache();
        }
   }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
            (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mTextEditor.getWindowToken(), 0);
    }

    private void updateSendButtonState() {
        boolean enable = false;
        if (isPreparedForSending()) {
            enable = true;
        }

        boolean requiresMms = mWorkingMessage.requiresMms();
        if (mShowTwoButtons) {
            View[] sendButtons = showTwoSmsOrMmsSendButton(requiresMms);
            if (sendButtons[PhoneConstants.SUB1] == mSendLayoutMmsFir
                    && sendButtons[PhoneConstants.SUB2] == mSendLayoutMmsSec) {
                mSendButtonMms.setEnabled(enable);
                mSendButtonMmsViewSec.setEnabled(enable);
                mSendButtonMms.setFocusable(enable);
                mSendButtonMmsViewSec.setFocusable(enable);
            } else if (sendButtons[PhoneConstants.SUB1] == mSendLayoutSmsFir
                    && sendButtons[PhoneConstants.SUB2] == mSendLayoutSmsSec) {
                mSendButtonSms.setEnabled(enable);
                mSendButtonSmsViewSec.setEnabled(enable);
                mSendButtonSms.setFocusable(enable);
                mSendButtonSmsViewSec.setFocusable(enable);
            }
        } else {
            View sendButton = showSmsOrMmsSendButton(requiresMms);
            sendButton.setEnabled(enable);
            sendButton.setFocusable(enable);
        }
        setSendButtonImage();
    }

    private void setSendButtonImage() {
        Contact contact = Contact.getMe(false);
        if (sDefaultContactImage == null) {
            sDefaultContactImage = this.getResources().getDrawable(R.drawable.default_avatar);
        }
        mAvatarDrawable = contact.getAvatar(this, sDefaultContactImage);
        if (mAvatarDrawable.equals(sDefaultContactImage)) {
            if (mWorkingMessage.requiresMms()) {
                mSendButtonMms.setBackground(this.getResources().getDrawable(
                        R.drawable.send_arrow_background));
                mSendButtonMms.setImageDrawable(this.getResources().getDrawable(
                        R.drawable.send_button_selector));
                mSendButtonMms.setScaleType(ImageButton.ScaleType.CENTER);
            } else {
                mSendButtonSms.setImageDrawable(this.getResources().getDrawable(
                        R.drawable.send_button_selector));
                mSendButtonSms.setBackground(this.getResources().getDrawable(
                        R.drawable.send_arrow_background));
                mSendButtonSms.setScaleType(ImageButton.ScaleType.CENTER);
            }
        } else {
            if (mWorkingMessage.requiresMms()) {
                if (mSendButtonMms.isEnabled()) {
                    mSendButtonMms.setScaleType(ImageButton.ScaleType.CENTER);
                    mSendButtonMms.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
                    mSendButtonMms.setBackground(this.getResources().getDrawable(
                            R.drawable.send_arrow_background));
                } else {
                    mSendButtonMms.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                    mSendButtonMms.setImageDrawable(mAvatarDrawable);
                    mSendButtonMms.setBackground(null);
                }
            } else {
                if (mSendButtonSms.isEnabled()) {
                    mSendButtonSms.setScaleType(ImageButton.ScaleType.CENTER);
                    mSendButtonSms.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
                    mSendButtonSms.setBackground(this.getResources().getDrawable(
                            R.drawable.send_arrow_background));
                } else {
                    mSendButtonSms.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                    mSendButtonSms.setImageDrawable(mAvatarDrawable);
                    mSendButtonSms.setBackground(null);
                }
            }
        }
    }

    private long getMessageDate(Uri uri) {
        if (uri != null) {
            Cursor cursor = SqliteWrapper.query(this, mContentResolver,
                    uri, new String[] { Mms.DATE }, null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        return cursor.getLong(0) * 1000L;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return NO_DATE_FOR_DIALOG;
    }

    private void initActivityState(Bundle bundle) {
        Intent intent = getIntent();
        if (bundle != null) {
            setIntent(getIntent().setAction(Intent.ACTION_VIEW));
            String recipients = bundle.getString(RECIPIENTS);
            if (LogTag.VERBOSE) log("get mConversation by recipients " + recipients);
            mConversation = Conversation.get(this,
                    ContactList.getByNumbers(recipients,
                            false /* don't block */, true /* replace number */), false);
            addRecipientsListeners();
            mSendDiscreetMode = bundle.getBoolean(KEY_EXIT_ON_SENT, false);
            mForwardMessageMode = bundle.getBoolean(KEY_FORWARDED_MESSAGE, false);

            if (mSendDiscreetMode) {
                mMsgListView.setVisibility(View.INVISIBLE);
            }
            mWorkingMessage.readStateFromBundle(bundle);

            return;
        }

        // If we have been passed a thread_id, use that to find our conversation.
        long threadId = intent.getLongExtra(THREAD_ID, 0);
        if (threadId > 0) {
            if (LogTag.VERBOSE) log("get mConversation by threadId " + threadId);
            mConversation = Conversation.get(this, threadId, false);
        } else {
            Uri intentData = intent.getData();
            if (intentData != null) {
                // try to get a conversation based on the data URI passed to our intent.
                if (LogTag.VERBOSE) log("get mConversation by intentData " + intentData);
                mConversation = Conversation.get(this, intentData, false);
                mWorkingMessage.setText(getBody(intentData));
            } else {
                // special intent extra parameter to specify the address
                String address = intent.getStringExtra("address");
                if (!TextUtils.isEmpty(address)) {
                    if (intent.getBooleanExtra("isGroupChat", false)) {
                        if (LogTag.VERBOSE) log("create new conversation");
                        mConversation = Conversation.createNew(this);
                        mConversation.setRecipients(ContactList.getByNumbers(address,
                                false /* don't block */, true /* replace number */));
                        mConversation.setIsGroupChat(true);
                    } else {
                        if (LogTag.VERBOSE) log("get mConversation by address " + address);
                        mConversation = Conversation.get(this, ContactList.getByNumbers(address,
                                false /* don't block */, true /* replace number */), false);
                    }
                } else {
                    if (LogTag.VERBOSE) log("create new conversation");
                    mConversation = Conversation.createNew(this);
                }
            }
        }
        addRecipientsListeners();
        updateThreadIdIfRunning();

        mSendDiscreetMode = intent.getBooleanExtra(KEY_EXIT_ON_SENT, false);
        mForwardMessageMode = intent.getBooleanExtra(KEY_FORWARDED_MESSAGE, false);
        mReplyMessageMode = intent.getBooleanExtra(KEY_REPLY_MESSAGE, false);
        if (mSendDiscreetMode) {
            mMsgListView.setVisibility(View.INVISIBLE);
        }
        if (intent.hasExtra("sms_body")) {
            mWorkingMessage.setText(intent.getStringExtra("sms_body"));
        }
        mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
    }

    private void initFocus() {
        if (!mIsKeyboardOpen) {
            return;
        }

        // If the recipients editor is visible, there is nothing in it,
        // and the text editor is not already focused, focus the
        // recipients editor.
        if (isRecipientsEditorVisible()
                && !mTextEditor.isFocused()) {
            mRecipientsEditor.requestFocus();
            return;
        }

        // If we decided not to focus the recipients editor, focus the text editor.
        mTextEditor.requestFocus();
    }

    private final MessageListAdapter.OnDataSetChangedListener
                    mDataSetChangedListener = new MessageListAdapter.OnDataSetChangedListener() {
        @Override
        public void onDataSetChanged(MessageListAdapter adapter) {
        }

        @Override
        public void onContentChanged(MessageListAdapter adapter) {
            startMsgListQuery();
        }
    };

    /**
     * smoothScrollToEnd will scroll the message list to the bottom if the list is already near
     * the bottom. Typically this is called to smooth scroll a newly received message into view.
     * It's also called when sending to scroll the list to the bottom, regardless of where it is,
     * so the user can see the just sent message. This function is also called when the message
     * list view changes size because the keyboard state changed or the compose message field grew.
     *
     * @param force always scroll to the bottom regardless of current list position
     * @param listSizeChange the amount the message list view size has vertically changed
     */
    private void smoothScrollToEnd(boolean force, int listSizeChange) {
        int lastItemVisible = mMsgListView.getLastVisiblePosition();
        int lastItemInList = mMsgListAdapter.getCount() - 1;
        if (lastItemVisible < 0 || lastItemInList < 0) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG, "smoothScrollToEnd: lastItemVisible=" + lastItemVisible +
                        ", lastItemInList=" + lastItemInList +
                        ", mMsgListView not ready");
            }
            return;
        }

        View lastChildVisible =
                mMsgListView.getChildAt(lastItemVisible - mMsgListView.getFirstVisiblePosition());
        int lastVisibleItemBottom = 0;
        int lastVisibleItemHeight = 0;
        if (lastChildVisible != null) {
            lastVisibleItemBottom = lastChildVisible.getBottom();
            lastVisibleItemHeight = lastChildVisible.getHeight();
        }

        if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
            Log.v(TAG, "smoothScrollToEnd newPosition: " + lastItemInList +
                    " mLastSmoothScrollPosition: " + mLastSmoothScrollPosition +
                    " first: " + mMsgListView.getFirstVisiblePosition() +
                    " lastItemVisible: " + lastItemVisible +
                    " lastVisibleItemBottom: " + lastVisibleItemBottom +
                    " lastVisibleItemBottom + listSizeChange: " +
                    (lastVisibleItemBottom + listSizeChange) +
                    " mMsgListView.getHeight() - mMsgListView.getPaddingBottom(): " +
                    (mMsgListView.getHeight() - mMsgListView.getPaddingBottom()) +
                    " listSizeChange: " + listSizeChange);
        }
        // Only scroll if the list if we're responding to a newly sent message (force == true) or
        // the list is already scrolled to the end. This code also has to handle the case where
        // the listview has changed size (from the keyboard coming up or down or the message entry
        // field growing/shrinking) and it uses that grow/shrink factor in listSizeChange to
        // compute whether the list was at the end before the resize took place.
        // For example, when the keyboard comes up, listSizeChange will be negative, something
        // like -524. The lastChild listitem's bottom value will be the old value before the
        // keyboard became visible but the size of the list will have changed. The test below
        // add listSizeChange to bottom to figure out if the old position was already scrolled
        // to the bottom. We also scroll the list if the last item is taller than the size of the
        // list. This happens when the keyboard is up and the last item is an mms with an
        // attachment thumbnail, such as picture. In this situation, we want to scroll the list so
        // the bottom of the thumbnail is visible and the top of the item is scroll off the screen.
        int listHeight = mMsgListView.getHeight();
        boolean lastItemTooTall = lastVisibleItemHeight > listHeight;
        boolean willScroll = force ||
                ((listSizeChange != 0 || lastItemInList != mLastSmoothScrollPosition) &&
                lastVisibleItemBottom + listSizeChange <=
                    listHeight - mMsgListView.getPaddingBottom());
        if (willScroll || (lastItemTooTall && lastItemInList == lastItemVisible)) {
            if (Math.abs(listSizeChange) > SMOOTH_SCROLL_THRESHOLD) {
                // When the keyboard comes up, the window manager initiates a cross fade
                // animation that conflicts with smooth scroll. Handle that case by jumping the
                // list directly to the end.
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.v(TAG, "keyboard state changed. setSelection=" + lastItemInList);
                }
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole height of the list,
                    // we need to scroll that item so that its top is negative or above the top of
                    // the list. That way, the bottom of the last item will be exposed above the
                    // keyboard.
                    mMsgListView.setSelectionFromTop(lastItemInList,
                            listHeight - lastVisibleItemHeight);
                } else {
                    mMsgListView.setSelection(lastItemInList);
                }
            } else if (lastItemInList - lastItemVisible > MAX_ITEMS_TO_INVOKE_SCROLL_SHORTCUT) {
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.v(TAG, "too many to scroll, setSelection=" + lastItemInList);
                }
                mMsgListView.setSelection(lastItemInList);
            } else {
                if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    Log.v(TAG, "smooth scroll to " + lastItemInList);
                }
                if (lastItemTooTall) {
                    // If the height of the last item is taller than the whole height of the list,
                    // we need to scroll that item so that its top is negative or above the top of
                    // the list. That way, the bottom of the last item will be exposed above the
                    // keyboard. We should use smoothScrollToPositionFromTop here, but it doesn't
                    // seem to work -- the list ends up scrolling to a random position.
                    mMsgListView.setSelectionFromTop(lastItemInList,
                            listHeight - lastVisibleItemHeight);
                } else {
                    mMsgListView.smoothScrollToPosition(lastItemInList);
                }
                mLastSmoothScrollPosition = lastItemInList;
            }
        }
    }

    private final class BackgroundQueryHandler extends ConversationQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch(token) {
                case MESSAGE_LIST_QUERY_TOKEN:
                    mConversation.blockMarkAsRead(false);

                    // check consistency between the query result and 'mConversation'
                    long tid = (Long) cookie;

                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("##### onQueryComplete: msg history result for threadId " + tid);
                    }
                    if (tid != mConversation.getThreadId()) {
                        if (mConversation.getThreadId() == 0) {
                            // Do nothing
                            // mConversation.setThreadId(tid);
                        } else {
                            log("onQueryComplete: msg history query result is for threadId " +
                                    tid + ", but mConversation has threadId " +
                                    mConversation.getThreadId() + " starting a new query");
                            if (cursor != null) {
                                cursor.close();
                            }
                            startMsgListQuery();
                            return;
                        }
                    }

                    // check consistency b/t mConversation & mWorkingMessage.mConversation
                    ComposeMessageActivity.this.sanityCheckConversation();

                    int newSelectionPos = -1;
                    long targetMsgId = getIntent().getLongExtra("select_id", -1);
                    if (targetMsgId != -1) {
                        if (cursor != null) {
                            cursor.moveToPosition(-1);
                            while (cursor.moveToNext()) {
                                long msgId = cursor.getLong(COLUMN_ID);
                                if (msgId == targetMsgId) {
                                    newSelectionPos = cursor.getPosition();
                                    break;
                                }
                            }
                        }
                    } else if (mSavedScrollPosition != -1) {
                        // mSavedScrollPosition is set when this activity pauses. If equals maxint,
                        // it means the message list was scrolled to the end. Meanwhile, messages
                        // could have been received. When the activity resumes and we were
                        // previously scrolled to the end, jump the list so any new messages are
                        // visible.
                        if (mSavedScrollPosition == Integer.MAX_VALUE) {
                            int cnt = mMsgListAdapter.getCount();
                            if (cnt > 0) {
                                // Have to wait until the adapter is loaded before jumping to
                                // the end.
                                newSelectionPos = cnt - 1;
                                mSavedScrollPosition = -1;
                            }
                        } else {
                            // remember the saved scroll position before the activity is paused.
                            // reset it after the message list query is done
                            newSelectionPos = mSavedScrollPosition;
                            mSavedScrollPosition = -1;
                        }
                    }

                    mMsgListAdapter.changeCursor(cursor);

                    if (newSelectionPos != -1) {
                        mMsgListView.setSelection(newSelectionPos);     // jump the list to the pos
                    } else {
                        int count = mMsgListAdapter.getCount();
                        long lastMsgId = 0;
                        if (cursor != null && count > 0) {
                            cursor.moveToLast();
                            lastMsgId = cursor.getLong(COLUMN_ID);
                        }
                        // mScrollOnSend is set when we send a message. We always want to scroll
                        // the message list to the end when we send a message, but have to wait
                        // until the DB has changed. We also want to scroll the list when a
                        // new message has arrived.
                        smoothScrollToEnd(mScrollOnSend || lastMsgId != mLastMessageId, 0);
                        mLastMessageId = lastMsgId;
                        mScrollOnSend = false;
                    }
                    // Adjust the conversation's message count to match reality. The
                    // conversation's message count is eventually used in
                    // WorkingMessage.clearConversation to determine whether to delete
                    // the conversation or not.
                    mConversation.setMessageCount(mMsgListAdapter.getCount());

                    // Once we have completed the query for the message history, if
                    // there is nothing in the cursor and we are not composing a new
                    // message, we must be editing a draft in a new conversation (unless
                    // mSentMessage is true).
                    // Show the recipients editor to give the user a chance to add
                    // more people before the conversation begins.
                    if (cursor != null && cursor.getCount() == 0
                            && !isRecipientsEditorVisible() && !mSentMessage) {
                        initRecipientsEditor();
                        mRecipientsEditor.addTextChangedListener(mRecipientsWatcher);
                    }

                    // FIXME: freshing layout changes the focused view to an unexpected
                    // one. In this situation, mRecipientsEditor has higher priority to
                    // get the focus.
                    if (isRecipientsEditorVisible()) {
                        mRecipientsEditor.requestFocus();
                    } else {
                        mTextEditor.requestFocus();
                    }

                    invalidateOptionsMenu();    // some menu items depend on the adapter's count
                    return;

                case ConversationList.HAVE_LOCKED_MESSAGES_TOKEN:
                    if (ComposeMessageActivity.this.isFinishing()) {
                        Log.w(TAG, "ComposeMessageActivity is finished, do nothing ");
                        if (cursor != null) {
                            cursor.close();
                        }
                        return ;
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<Long> threadIds = (ArrayList<Long>)cookie;
                    ConversationList.confirmDeleteThreadDialog(
                            new ConversationList.DeleteThreadListener(threadIds,
                                mBackgroundQueryHandler, null, ComposeMessageActivity.this),
                            threadIds,
                            cursor != null && cursor.getCount() > 0,
                            ComposeMessageActivity.this);
                    if (cursor != null) {
                        cursor.close();
                    }
                    break;

                case MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN:
                    // check consistency between the query result and 'mConversation'
                    tid = (Long) cookie;

                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        log("##### onQueryComplete (after delete): msg history result for threadId "
                                + tid);
                    }
                    if (cursor == null) {
                        return;
                    }
                    if (tid > 0 && cursor.getCount() == 0) {
                        // We just deleted the last message and the thread will get deleted
                        // by a trigger in the database. Clear the threadId so next time we
                        // need the threadId a new thread will get created.
                        log("##### MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN clearing thread id: "
                                + tid);
                        Conversation conv = Conversation.get(ComposeMessageActivity.this, tid,
                                false);
                        if (conv != null) {
                            conv.clearThreadId();
                            conv.setDraftState(false);
                        }
                        // The last message in this converation was just deleted. Send the user
                        // to the conversation list.
                        exitComposeMessageActivity(new Runnable() {
                            @Override
                            public void run() {
                                goToConversationList();
                            }
                        });
                    }
                    cursor.close();
            }
        }

        private Runnable updateAttachTypeRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updateAttachTypeRunnable after onDeleteComplete");
                updateThreadAttachType();
            }
        };

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            super.onDeleteComplete(token, cookie, result);
            mHandler.removeCallbacks(updateAttachTypeRunnable);
            mHandler.postDelayed(updateAttachTypeRunnable, 200);

            switch(token) {
                case ConversationList.DELETE_CONVERSATION_TOKEN:
                    mConversation.setMessageCount(0);
                    // fall through
                case DELETE_MESSAGE_TOKEN:
                    if (cookie instanceof Boolean && ((Boolean)cookie).booleanValue()) {
                        // If we just deleted the last message, reset the saved id.
                        mLastMessageId = 0;
                    }
                    // Update the notification for new messages since they
                    // may be deleted.
                    MessagingNotification.nonBlockingUpdateNewMessageIndicator(
                            ComposeMessageActivity.this, MessagingNotification.THREAD_NONE, false);
                    // Update the notification for failed messages since they
                    // may be deleted.
                    updateSendFailedNotification();
                    break;
            }
            // If we're deleting the whole conversation, throw away
            // our current working message and bail.
            if (token == ConversationList.DELETE_CONVERSATION_TOKEN) {
                ContactList recipients = mConversation.getRecipients();
                mWorkingMessage.discard();

                // Remove any recipients referenced by this single thread from the
                // contacts cache. It's possible for two or more threads to reference
                // the same contact. That's ok if we remove it. We'll recreate that contact
                // when we init all Conversations below.
                if (recipients != null) {
                    for (Contact contact : recipients) {
                        contact.removeFromCache();
                    }
                }

                // Make sure the conversation cache reflects the threads in the DB.
                Conversation.init(getApplicationContext());
                finish();
            } else if (token == DELETE_MESSAGE_TOKEN) {
                // Check to see if we just deleted the last message
                startMsgListQuery(MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN);
            }

            MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
        }
    }
    private void updateThreadAttachType() {
        final Context context = getApplicationContext();
        if (null == mUpdateAttachmentTask) {
            mUpdateAttachmentTask = new UpdateAttachmentInfo(context, mConversation.getThreadId());
        }
        mUpdateAttachmentTask.requestUpdate();
    }

    @Override
    public void onUpdate(final Contact updated) {
        // Using an existing handler for the post, rather than conjuring up a new one.
        mMessageListItemHandler.post(new Runnable() {
            @Override
            public void run() {
                ContactList recipients = isRecipientsEditorVisible() ?
                        mRecipientsEditor.constructContactsFromInput(false) : getRecipients();
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("[CMA] onUpdate contact updated: " + updated);
                    log("[CMA] onUpdate recipients: " + recipients);
                }
                updateTitle(recipients);

                // The contact information for one (or more) of the recipients has changed.
                // Rebuild the message list so each MessageItem will get the last contact info.
                ComposeMessageActivity.this.mMsgListAdapter.notifyDataSetChanged();

                // Don't do this anymore. When we're showing chips, we don't want to switch from
                // chips to text.
//                if (mRecipientsEditor != null) {
//                    mRecipientsEditor.populate(recipients);
//                }
            }
        });
    }

    private void addRecipientsListeners() {
        Contact.addListener(this);
    }

    private void removeRecipientsListeners() {
        Contact.removeListener(this);
    }

    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
        }

        return intent;
    }

    public static Intent createIntent(Context context, long threadId, String from) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

        intent.putExtra(EXTRA_START_COMPOSE_FROM, from);
        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
        }

        return intent;
    }

    private String getBody(Uri uri) {
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (!urlStr.contains("?")) {
            return null;
        }
        urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
        String[] params = urlStr.split("&");
        for (String p : params) {
            if (p.startsWith("body=")) {
                try {
                    return URLDecoder.decode(p.substring(5), "UTF-8");
                } catch (UnsupportedEncodingException e) { }
            }
        }
        return null;
    }

    private void updateThreadIdIfRunning() {
        if (mIsRunning && mConversation != null) {
            if (DEBUG) {
                Log.v(TAG, "updateThreadIdIfRunning: threadId: " +
                        mConversation.getThreadId());
            }
            MessagingNotification.setCurrentlyDisplayedThreadId(mConversation.getThreadId());
        } else {
            if (DEBUG) {
                Log.v(TAG, "updateThreadIdIfRunning: mIsRunning: " + mIsRunning +
                        " mConversation: " + mConversation);
            }
        }
        // If we're not running, but resume later, the current thread ID will be set in onResume()
    }

    // Handler for handle copy mms to SIM with toast.
    private Handler mCopyToSimWithToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int sum = 0;
            int success = 0;
            switch (msg.what) {
            case MSG_COPY_TO_SIM_SUCCESS:
                sum = msg.arg1;
                success = msg.arg2;
                break;
            default:
                break;
            }
            String toast = getString(R.string.copy_to_sim_success, sum, success);
            Toast.makeText(ComposeMessageActivity.this, toast,
                    Toast.LENGTH_SHORT).show();
        }
    };

    private class CopyToSimSelectListener implements DialogInterface.OnClickListener {
        private CopyOnWriteArrayList<MessageItem> msgItems;
        private int slot;

        public CopyToSimSelectListener(CopyOnWriteArrayList<MessageItem> msgItems) {
            super();
            this.msgItems = msgItems;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which >= 0) {
                slot = which;
            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                int [] subId = SubscriptionManager.getSubId(slot);
                if (MessageUtils.hasInvalidSmsRecipient(getContext(), msgItems)) {
                    showInvalidCopyDialog();
                } else {
                    new Thread(new CopyToSimThread(msgItems, subId[0])).start();
                }
            }
        }
    }

    private void showInvalidCopyDialog() {
        AlertDialog invalidCopyDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.copy_to_sim_fail)
                .setMessage(R.string.cannot_copy_to_sim_reason)
                .setPositiveButton(R.string.yes, null)
                .show();
    }

    private class CopyToSimThread extends Thread {
        private CopyOnWriteArrayList<MessageItem> msgItems;
        private int subscription;

        public CopyToSimThread(CopyOnWriteArrayList<MessageItem> msgItems) {
            this.msgItems = msgItems;
            this.subscription = SmsManager.getDefault().getDefaultSmsSubscriptionId();
        }

        public CopyToSimThread(CopyOnWriteArrayList<MessageItem> msgItems, int subscription) {
            this.msgItems = msgItems;
            this.subscription = subscription;
        }

        @Override
        public void run() {
            Message msg = mCopyToSimWithToastHandler.obtainMessage();
            int sum = msgItems.size();
            int success = 0;
            for (MessageItem msgItem : msgItems) {
                if (copyToSim(msgItem, subscription)) {
                    success++;
                }
            }
            msg.what = MSG_COPY_TO_SIM_SUCCESS;
            msg.arg1 = sum;
            msg.arg2 = success;
            Log.d(TAG, "copy sms to sim: sum=" + sum + ", success=" + success);
            msg.sendToTarget();
        }
    }

    private boolean copyToSim(MessageItem msgItem) {
        return copyToSim(msgItem, SmsManager.getDefault().getDefaultSmsSubscriptionId());
    }

    private boolean copyToSim(MessageItem msgItem, int subId) {
        int boxId = msgItem.mBoxId;
        String address = msgItem.mAddress;
        if (MessageUtils.isWapPushNumber(address)) {
            String[] number = address.split(":");
            address = number[0];
        }
        String text = msgItem.mBody;
        long timestamp = msgItem.mDate != 0 ? msgItem.mDate : System.currentTimeMillis();

        SmsManager sm = SmsManager.getDefault();
        ArrayList<String> messages = SmsManager.getDefault().divideMessage(text);

        boolean ret = true;
        for (String message : messages) {
            ContentValues values = new ContentValues();
            values.put(PhoneConstants.SUBSCRIPTION_KEY, subId);
            values.put(Sms.ADDRESS, address);
            values.put(Sms.BODY, message);
            values.put(MessageUtils.SMS_BOX_ID, boxId);
            values.put(Sms.DATE, timestamp);
            Uri uri = getContentResolver().insert(MessageUtils.ICC_URI, values);
            if (uri != null) {
                ret = MessageUtils.COPY_SMS_INTO_SIM_SUCCESS.equals(uri.getLastPathSegment());
            }
            if (!ret) {
                break;
            }
        }
        return ret;
    }

    private IntentFilter getMediaStateFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme(BROADCAST_DATA_SCHEME);
        return filter;
    }

    private String getAttachFilePath(Context context, Uri uri){
        if (URI_SCHEME_CONTENT.equals(uri.getScheme())
                && URI_HOST_MEDIA.equals(uri.getHost())) {
            Cursor c = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        return c.getString(c.getColumnIndex(FILE_PATH_COLUMN));
                    }
                } finally {
                    c.close();
                }
            }
            return null;
        } else {
            return uri.getPath().toString();
        }
    }

    private void checkAttachFileState(Context context) {
        if (mWorkingMessage.hasAttachment() && !mWorkingMessage.hasSlideshow()) {
            ArrayList<Uri> attachFileUris = mWorkingMessage.getSlideshow().getAttachFileUri();
            for (Uri uri : attachFileUris) {
                Log.i(TAG, "Attach file uri is " + uri);
                if (uri == null) {
                    continue;
                }
                String path = getAttachFilePath(context, uri);
                Log.i(TAG, "File path is " + path);
                File f = null;
                if (!TextUtils.isEmpty(path)) {
                    f = new File(path);
                }
                if (f == null || !f.exists()) {
                    Log.i(TAG, "set attachment null");
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.cannot_send_attach_reason,
                            Toast.LENGTH_SHORT).show();
                    mWorkingMessage.setAttachment(WorkingMessage.TEXT, null, false);
                    break;
                }
            }
        }
    }

    // Get the path of uri and compare it to ".vcf" to judge whether it is a
    // vcard file.
    private boolean isVcardFile(Uri uri) {
        String path = uri.getPath();
        return null != path && path.toLowerCase().endsWith(".vcf");
    }

    private ListView getListView() {
        return mMsgListView;
    }

    private void logMultiChoice(String msg) {
        if (DEBUG_MULTI_CHOICE) {
            Log.d(TAG, msg);
        }
    }

    private Context getContext() {
        return ComposeMessageActivity.this;
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private ImageView mSelectedAll;
        private boolean mHasSelectAll = false;
        // build action bar with a spinner
        private SelectionMenu mSelectionMenu;
        // need define variable to keep info of mms count, lock count, unlock
        // count.
        private int mMmsSelected = 0;
        private int mRcsSelected = 0;
        private int mRcsMediaSelected = 0;
        private int mUnlockedCount = 0;
        private int mCheckedCount = 0;
        private boolean mDeleteLockedMessages = false;
        private int mUnFavouriteCount = 0;
        private Menu mMenu;

        private WorkThread mWorkThread;
        public final static int WORK_TOKEN_DELETE = 0;
        public final static int WORK_TOKEN_LOCK = 1;
        public final static int WORK_TOKEN_UNLOCK = 2;
        public final static int WORK_TOKEN_FAVOURITE=3;
        public final static int WORK_TOKEN_UNFAVOURITE=4;
        public final static int WORK_TOKEN_COMPLAIN = 5;
        ArrayList<Integer> mSelectedPos = new ArrayList<Integer>();
        ArrayList<Uri> mSelectedMsg = new ArrayList<Uri>();
        ArrayList<Long> mSelectedRcsMsgId = new ArrayList<Long>();
        ArrayList<MessageItem> mMessageItems = new ArrayList<MessageItem>();
        ArrayList<Uri> mSelectedLockedMsg = new ArrayList<Uri>();

        public void checkAll(boolean isCheck) {
            for (int i = 0; i < getListView().getCount(); i++) {
                MessageItem item = getMessageItemByPos(i);
                boolean isBurnMsg = false;
                if (mIsRcsEnabled) {
                    if (mConversation != null && !mConversation.isGroupChat()) {
                        isBurnMsg = item.isRcsBurnMessage();
                    }
                    if (!isBurnMsg) {
                        getListView().setItemChecked(i, isCheck);
                    }
                } else {
                    getListView().setItemChecked(i, isCheck);
                }
            }
        }


        private class DeleteMessagesListener implements OnClickListener {
            public void setDeleteLockedMessage(boolean deleteLockedMessage) {
                mDeleteLockedMessages = deleteLockedMessage;
            }

            public void onClick(DialogInterface dialog, int whichButton) {
                deleteMessages();
            }
        }

        public class WorkThread extends Thread {
            private int mToken = -1;

            public WorkThread() {
                super("WorkThread");
            }

            public void startWork(int token) {
                mToken = token;
                this.start();
            }

            public void run() {
                switch (mToken) {
                case WORK_TOKEN_DELETE:
                    deleteCheckedMessage();
                    break;
                case WORK_TOKEN_LOCK:
                    lockMessage(true);
                    break;
                case WORK_TOKEN_UNLOCK:
                    lockMessage(false);
                    break;
                case WORK_TOKEN_FAVOURITE:
                    favouriteMessage(true);
                    break;
                case WORK_TOKEN_UNFAVOURITE:
                    favouriteMessage(false);
                    break;
                case WORK_TOKEN_COMPLAIN:
                    accuseMessage();
                    break;

                default:
                    break;
                }
            }
        }

        private void lockMessage(boolean lock) {
            final ContentValues values = new ContentValues(1);
            values.put("locked", lock ? 1 : 0);
            for (Uri uri : mSelectedMsg) {
                getContentResolver().update(uri, values, null, null);
            }
        }

        private WorkThread getWorkThread() {
            mWorkThread = new WorkThread();
            mWorkThread.setPriority(Thread.MAX_PRIORITY);
            return mWorkThread;
        }

        private void deleteMessages() {
            getWorkThread().startWork(WORK_TOKEN_DELETE);
        }

        private void deleteCheckedMessage() {
            for (Uri uri : mSelectedMsg) {
                if (!mDeleteLockedMessages && mSelectedLockedMsg.contains(uri)) {
                    continue;
                }
                mBackgroundQueryHandler.startDelete(
                        WORK_TOKEN_DELETE,
                        mConversation.getThreadId(),
                        uri,
                        null,
                        null);
            }
            mDeleteLockedMessages = false;
            startMsgListQuery(MESSAGE_LIST_QUERY_AFTER_DELETE_TOKEN);
        }

        private void calculateSelectedMsgUri() {
            mSelectedMsg.clear();
            mSelectedRcsMsgId.clear();
            mSelectedLockedMsg.clear();
            for (Integer pos : mSelectedPos) {
                Cursor c = (Cursor) getListView().getAdapter().getItem(pos);
                String type = c.getString(COLUMN_MSG_TYPE);
                logMultiChoice("message type is:" + type);
                if ("sms".equals(type)) {
                    mSelectedMsg.add(ContentUris.withAppendedId(
                            Sms.CONTENT_URI, c.getLong(COLUMN_ID)));
                    if (c.getInt(COLUMN_SMS_LOCKED) != 0) {
                        mSelectedLockedMsg.add(ContentUris.withAppendedId(
                                Sms.CONTENT_URI, c.getLong(COLUMN_ID)));
                    }
                } else if ("mms".equals(type)) {
                    mSelectedMsg.add(ContentUris.withAppendedId(
                            Mms.CONTENT_URI, c.getLong(COLUMN_ID)));
                    if (c.getInt(COLUMN_MMS_LOCKED) != 0) {
                        mSelectedLockedMsg.add(ContentUris.withAppendedId(
                                Mms.CONTENT_URI, c.getLong(COLUMN_ID)));
                    }
                }
                if (mIsRcsEnabled) {
                    long msgId = c.getLong(COLUMN_ID);
                    try {
                        MessageItem msgItem = mMsgListAdapter.getCachedMessageItem(type, msgId, c);
                        if (msgItem == null) {
                            continue;
                        }
                        mSelectedRcsMsgId.add(msgId);
                        SimpleMessage sm = new SimpleMessage();
                        sm.setMessageRowId(msgId);
                        if (!msgItem.isRcsMessage() && msgItem.isSms()) {
                            sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_SMS);
                        } else if (msgItem.isMms()) {
                            sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_MMS);
                        } else if (msgItem.isRcsMessage() && msgItem.isSms()) {
                            sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_IM);
                        }
                        mSimpleMsgs.add(sm);
                    } catch (Exception e) {
                        RcsLog.d(e.toString());
                    }
                }

            }
        }

        private void recordAllSelectedItems() {
            // must calculate all checked msg before multi selection done.
            SparseBooleanArray booleanArray = getListView()
                    .getCheckedItemPositions();
            mSelectedPos.clear();
            logMultiChoice("booleanArray = " + booleanArray);
            for (int i = 0; i < booleanArray.size(); i++) {
                int pos = booleanArray.keyAt(i);
                boolean checked = booleanArray.get(pos);
                logMultiChoice("pos=" + pos + ",checked=" + checked);
                if (checked) {
                    mSelectedPos.add(pos);
                }
            }
            calculateSelectedMsgUri();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            logMultiChoice("onCreateActionMode");
            // reset statics
            if (MmsConfig.isRcsVersion()) {
                mRcsBurnAfterReadMessageCount = getBurnAfterReadMessageCount();
            }
            mMmsSelected = 0;
            mRcsSelected = 0;
            mRcsMediaSelected = 0;
            mUnlockedCount = 0;
            mCheckedCount = 0;
            mUnFavouriteCount = 0;
            mMenu = menu;
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.compose_multi_select_menu, menu);
            getWindow().setStatusBarColor(
                    getResources().getColor(R.color.statubar_select_background));
            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(getContext())
                        .inflate(R.layout.action_mode, null);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            mSelectionMenu = new SelectionMenu(getContext(),
                    (Button) mMultiSelectActionBarView
                            .findViewById(R.id.selection_menu),
                    new PopupList.OnPopupItemClickListener() {
                        @Override
                        public boolean onPopupItemClick(int itemId) {
                            if (itemId == SelectionMenu.SELECT_OR_DESELECT) {
                                boolean selectAll = getListView().getCheckedItemCount() <
                                        getMsgCount() ? true : false;
                                checkAll(selectAll);
                                mSelectionMenu.updateSelectAllMode(selectAll);
                            }
                            return true;
                        }
                    },
                    (ImageView) mMultiSelectActionBarView
                            .findViewById(R.id.expand));
            return true;
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            logMultiChoice("onPrepareActionMode");
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater
                        .from(getContext())
                        .inflate(
                                R.layout.conversation_list_multi_select_actionbar,
                                null);
                mode.setCustomView(v);
                mSelectedConvCount = (TextView) v
                        .findViewById(R.id.selected_conv_count);
            }
            if (MessageUtils.getActivatedIccCardCount() < 1) {
                MenuItem item = menu.findItem(R.id.copy_to_sim);
                if (item != null) {
                    item.setVisible(false);
                }
            }
            MenuItem complainItem = menu.findItem(R.id.complain);
            if (complainItem != null) {
                complainItem.setVisible(mIsRcsEnabled);
            }
            MenuItem saveBackItem = menu.findItem(R.id.save_back);
            if (saveBackItem != null) {
                saveBackItem.setVisible(mIsRcsEnabled);
            }
            MenuItem favouriteItem = menu.findItem(R.id.favourite);
            if (favouriteItem != null) {
                favouriteItem.setVisible(mIsRcsEnabled);
            }
            MenuItem viewOneToManyStatusItem = menu.findItem(R.id.view_one_to_many_msg_status);
            if (viewOneToManyStatusItem != null) {
                boolean isOneToManyMsg = !mConversation.isGroupChat()
                        && (getRecipients().size() > 1);
                boolean isNativeUiInstall = RcsUtils.
                        isPackageInstalled(getContext(), RcsUtils.NATIVE_UI_PACKAGE_NAME);
                viewOneToManyStatusItem.setVisible(mIsRcsEnabled && isOneToManyMsg
                        && isNativeUiInstall);
            }
            return true;
        }

        private void showMessageDetail() {
            Cursor c = (Cursor) getListView().getAdapter().getItem(
                    mSelectedPos.get(0));
            String type = c.getString(COLUMN_MSG_TYPE);
            if (type.equals("sms")) {
                // this only for sms
                MessageUtils.showSmsMessageContent(getContext(), c.getLong(COLUMN_ID));
            } else if (type.equals("mms")) {
                MessageUtils.viewMmsMessageAttachment(
                        ComposeMessageActivity.this, mSelectedMsg.get(0), null,
                        new AsyncDialog(ComposeMessageActivity.this));
            }
        }

        private void saveAttachment(long msgId) {
            int resId = copyMedia(msgId) ? R.string.copy_to_sdcard_success
                    : R.string.copy_to_sdcard_fail;
            Toast.makeText(ComposeMessageActivity.this, resId,
                    Toast.LENGTH_SHORT).show();
        }

        private void showReport() {
            Cursor c = (Cursor) getListView().getAdapter().getItem(
                    mSelectedPos.get(0));
            showDeliveryReport(c.getLong(COLUMN_ID), c.getString(COLUMN_MSG_TYPE));
        }

        private void resendCheckedMessage() {
            Cursor c = (Cursor) getListView().getAdapter().getItem(mSelectedPos.get(0));
            if (isRcsMessage(c)) {
                RcsMessageOpenUtils.retransmisMessage(mMsgListAdapter.getCachedMessageItem(
                        c.getString(COLUMN_MSG_TYPE), c.getLong(COLUMN_ID), c));
            } else {
                MessageItem item = mMsgListAdapter.getCachedMessageItem(
                        c.getString(COLUMN_MSG_TYPE), c.getLong(COLUMN_ID), c);
                if (getResources().getBoolean(R.bool.config_resend_to_edit)) {
                    editMessageItem(item);
                    drawBottomPanel();
                } else {
                    resendMessage(item);
                }
            }
        }

        private void copySmsToSim() {
            mMessageItems.clear();
            for (Integer pos : mSelectedPos) {
                Cursor c = (Cursor) mMsgListAdapter.getItem(pos);
                mMessageItems.add(mMsgListAdapter.getCachedMessageItem(
                        c.getString(COLUMN_MSG_TYPE), c.getLong(COLUMN_ID), c));

            }
            CopyOnWriteArrayList<MessageItem> messageItems =
                    new CopyOnWriteArrayList<MessageItem>(mMessageItems);
            if (MessageUtils.getActivatedIccCardCount() > 1) {
                String[] items = new String[TelephonyManager.getDefault()
                        .getPhoneCount()];
                for (int i = 0; i < items.length; i++) {
                    items[i] = MessageUtils.getMultiSimName(
                            ComposeMessageActivity.this, i);
                }
                CopyToSimSelectListener listener = new CopyToSimSelectListener(
                        messageItems);
                new AlertDialog.Builder(ComposeMessageActivity.this)
                        .setTitle(R.string.copy_to_sim)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setSingleChoiceItems(items, 0, listener)
                        .setCancelable(true).show();
            } else {
                if (MessageUtils.hasInvalidSmsRecipient(getContext(), messageItems)) {
                    showInvalidCopyDialog();
                } else {
                    new Thread(new CopyToSimThread(messageItems)).start();
                }
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            recordAllSelectedItems();
            switch (item.getItemId()) {
            case R.id.forward:
                int position = mSelectedPos.get(0).intValue();
                MessageItem msgItem = getMessageItemByPos(position);
                if (msgItem != null &&
                        msgItem.isMms() &&
                        !isAllowForwardMessage(msgItem)) {
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.forward_size_over,
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                forwardMessageCheck();
                break;
            case R.id.delete:
                confirmDeleteDialog(new DeleteMessagesListener(), mCheckedCount != mUnlockedCount);
                break;
            case R.id.lock:
                if (item.getTitle().equals(
                        getContext().getString(R.string.menu_lock))) {
                    getWorkThread().startWork(WORK_TOKEN_LOCK);
                } else {
                    getWorkThread().startWork(WORK_TOKEN_UNLOCK);
                }
                break;
            case R.id.resend:
                mode.finish();
                resendCheckedMessage();
                return true;
            case R.id.favourite:
                if (item.getTitle().equals(
                        getContext().getString(R.string.favorited))) {
                    getWorkThread().startWork(WORK_TOKEN_FAVOURITE);
                } else {
                    getWorkThread().startWork(WORK_TOKEN_UNFAVOURITE);
                }
                break;
            case R.id.complain:
                confirmComplainDialog(new ComplainMessageListener());
                break;
            case R.id.save_back:
                Intent backupIntent = new Intent();
                backupIntent.setAction(LUNCH_BACKUP_MESSAGE_ACTIVITY);
                Bundle bundle = new Bundle();
                bundle.putSerializable(BACKUP_MESSAGE_IDS, mSimpleMsgs);
                backupIntent.putExtra(BACKUP_MESSAGE_LIST,bundle);
                startActivity(backupIntent);
                mSimpleMsgs.clear();
                break;
            case R.id.view_one_to_many_msg_status:
                viewOneToManyStatus();
                break;
            case R.id.copy_to_sim:
                copySmsToSim();
                break;
            case R.id.detail:
                showMessageDetail();
                break;
            case R.id.save_attachment:
                    if (MessageUtils.hasStoragePermission()) {
                        saveAttachment();
                    } else {
                        requestPermissions(new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, SAVE_ATTACHMENT_PERMISSION_REQUEST_CODE);
                    }
                break;
            case R.id.report:
                showReport();
                break;
            case R.id.copy_text:
                int pos = mSelectedPos.get(0).intValue();
                MessageItem copyItem = getMessageItemByPos(pos);
                if (copyItem != null && copyItem.isSms()) {
                    copyToClipboard(copyItem.mBody);
                }
                break;
            case R.id.more:
                prepareActionMode(mode);
                return true;
            default:
                break;
            }
            mode.finish();
            return true;
        }

        private void prepareActionMode(ActionMode mode) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater.from(getContext())
                    .inflate(R.layout.conversation_list_multi_select_actionbar, null);
                mode.setCustomView(v);
                mSelectedConvCount = (TextView) v
                        .findViewById(R.id.selected_conv_count);
            }
            if (MessageUtils.getActivatedIccCardCount() < 1) {
                MenuItem copyTextItem = mMenu.findItem(R.id.copy_to_sim);
                if (copyTextItem != null) {
                    copyTextItem.setVisible(false);
                }
            }
            MenuItem complainItem = mMenu.findItem(R.id.complain);
            if (complainItem != null) {
                complainItem.setVisible(mIsRcsEnabled);
            }
            MenuItem saveBackItem = mMenu.findItem(R.id.save_back);
            if (saveBackItem != null) {
                saveBackItem.setVisible(mIsRcsEnabled);
            }
            MenuItem favouriteItem = mMenu.findItem(R.id.favourite);
            if (favouriteItem != null) {
                favouriteItem.setVisible(mIsRcsEnabled);
            }
            MenuItem viewOneToManyStatusItem = mMenu.findItem(R.id.view_one_to_many_msg_status);
            if (viewOneToManyStatusItem != null) {
                boolean isOneToManyMsg = !mConversation.isGroupChat()
                        && (getRecipients().size() > 1);
                boolean isNativeUiInstall = RcsUtils.
                        isPackageInstalled(getContext(), RcsUtils.NATIVE_UI_PACKAGE_NAME);
                viewOneToManyStatusItem.setVisible(mIsRcsEnabled && isOneToManyMsg
                        && isNativeUiInstall);
            }
        }

        private String getAllSMSBody() {
            if (!getResources().getBoolean(R.bool.config_forwardconv)) {
                //There should be only one messageItem if forwardconv config is disable.
                return mMessageItems.get(0).mBody;
            }

            StringBuilder body = new StringBuilder();
            for (MessageItem msgItem : mMessageItems) {
                // if not the first append a new line
                if (mMessageItems.indexOf(msgItem) != 0) {
                    body.append(LINE_BREAK);
                }
                if (Sms.isOutgoingFolder(msgItem.mBoxId)) {
                    body.append(msgItem.mContact + COLON + LINE_BREAK);
                } else {
                    if (Contact.get(msgItem.mAddress, false).existsInDatabase()) {
                        body.append(msgItem.mContact + LEFT_PARENTHESES +
                                msgItem.mAddress + RIGHT_PARENTHESES + COLON + LINE_BREAK);
                    } else {
                        body.append(msgItem.mAddress + COLON + LINE_BREAK);
                    }
                }
                body.append(msgItem.mBody);
                if (!TextUtils.isEmpty(msgItem.mTimestamp)) {
                    body.append(LINE_BREAK);
                    body.append(msgItem.mTimestamp);
                }
            }
            return body.toString();
        }

        private int getMsgCount() {
            int msgcount = getListView().getCount();
            if (MmsConfig.isRcsVersion()) {
                msgcount -= mRcsBurnAfterReadMessageCount;
            }
            return msgcount;
        }

        private class ComplainMessageListener implements OnClickListener{
            public void onClick(DialogInterface dialog, int whichButton) {
                complainMessages();
            }
        }

       private void favouriteMessage(boolean favourite) {
           ArrayList<SimpleMessage> simpleMsgs = new ArrayList<SimpleMessage>();
           for (Integer pos : mSelectedPos) {
               Cursor c = (Cursor) mMsgListAdapter.getItem(pos);
               MessageItem messageItem = mMsgListAdapter.getCachedMessageItem(
                       c.getString(COLUMN_MSG_TYPE), c.getLong(COLUMN_ID), c);
               if (messageItem == null) {
                   continue;
               }
               SimpleMessage sm = new SimpleMessage();
               sm.setMessageRowId(messageItem.getMessageId());
               if (!messageItem.isRcsMessage() && messageItem.isSms()) {
                   sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_SMS);
               } else if (messageItem.isMms()) {
                   sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_MMS);
               } else if (messageItem.isRcsMessage() && messageItem.isSms()) {
                   sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_IM);
               }
               simpleMsgs.add(sm);
           }
           try {
               if (favourite) {
                   mMessageApi.collect(simpleMsgs);
                   mToastMessageHandler.sendEmptyMessage(OPERATE_SUCCESS);
               } else {
                   mMessageApi.cancelCollect(simpleMsgs);
                   mToastMessageHandler.sendEmptyMessage(OPERATE_SUCCESS);
               }
           } catch (ServiceDisconnectedException e) {
               e.printStackTrace();
               mToastMessageHandler.sendEmptyMessage(OPERATE_FAILURE);
           } catch (RemoteException ex){
               ex.printStackTrace();
               mToastMessageHandler.sendEmptyMessage(OPERATE_FAILURE);
           }
        }

        private void complainMessages() {
            getWorkThread().startWork(WORK_TOKEN_COMPLAIN);
        }

        private void accuseMessage() {
            for (Integer pos : mSelectedPos) {
                Cursor c = (Cursor) getListView().getAdapter().getItem(pos);
                long msgId = c.getLong(COLUMN_ID);
                try {
                    mMessageApi.complain(msgId);
                } catch (ServiceDisconnectedException e) {
                    e.printStackTrace();
                } catch (RemoteException ex){
                    ex.printStackTrace();
                }
            }
        }

        private void confirmComplainDialog(OnClickListener listener){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.confirm_complain_dialog_title);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, null);
            builder.setMessage(R.string.confirm_complain_selected_messages);
            builder.show();
        }

        private boolean isRcsMessageAttachment(Cursor cursor) {
            MessageItem messageItem = mMsgListAdapter.getCachedMessageItem(
                    cursor.getString(COLUMN_MSG_TYPE),
                    cursor.getLong(COLUMN_ID), cursor);
            return (messageItem != null) && (messageItem.isRcsMessage());
        }

        private void saveRcsAttachment(MessageItem msgItem) {
            if (mIsRcsEnabled) {
                if (msgItem != null) {
                    saveRcsMassages(msgItem);
                }
            }
        }

        private boolean isRcsMessage(Cursor cursor) {
            if (!MmsConfig.isRcsVersion() || cursor == null) {
                return false;
            }
            int rcsChatType = cursor.getInt(COLUMN_RCS_CHAT_TYPE);
            return rcsChatType > RcsUtils.RCS_CHAT_TYPE_DEFAULT
                    && rcsChatType < RcsUtils.RCS_CHAT_TYPE_PUBLIC_MESSAGE;
        }

        private void saveRcsMassages(final MessageItem msgItem) {

            new Thread() {
                @Override
                public void run() {
                    final String filePath = RcsUtils.saveRcsMassage(
                            ComposeMessageActivity.this, msgItem);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (TextUtils.isEmpty(filePath)) {
                                Toast.makeText(ComposeMessageActivity.this,
                                        R.string.copy_to_sdcard_fail, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ComposeMessageActivity.this,
                                        ComposeMessageActivity.this.getString(
                                        R.string.copy_to_sdcard_success_path, filePath),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }.start();
        }

        private boolean forwardMessageCheck() {
            if (!mIsRcsEnabled) {
                int position = mSelectedPos.get(0).intValue();
                MessageItem msgItem = getMessageItemByPos(position);
                if (msgItem != null &&
                        msgItem.isMms() &&
                        !isAllowForwardMessage(msgItem)) {
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.forward_size_over,
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                forwardMessage();
            } else if (isAirPlaneModeOn()) {
                toast(R.string.on_airplain_mode);
            } else {
                int position = mSelectedPos.get(0).intValue();
                MessageItem msgItem = getMessageItemByPos(position);
                if (msgItem != null && msgItem.isMms()) {
                    if (!isAllowForwardMessage(msgItem)) {
                        Toast.makeText(ComposeMessageActivity.this,
                                R.string.forward_size_over,
                                Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        forwardMessage();
                        return true;
                    }
                }
                if (msgItem.isRcsMediaMsg() &&
                        msgItem.getMsgDownlaodState() != RcsUtils.RCS_IS_DOWNLOAD_OK
                        && !msgItem.isRcsAudioMessage()) {
                    toast(R.string.forward_message_not_download);
                    return false;
                }
                if (msgItem.isRcsMessage() && msgItem.getRcsMsgType() !=
                        Constants.MessageConstants.CONST_MESSAGE_TEXT && !RcsUtils.isRcsOnline()) {
                    toast(R.string.rcs_not_online_can_not_forward_media_message);
                    return false;
                }
                mRcsForwardItems.clear();
                mRcsForwardItems.add(msgItem);
                boolean isRcsOnline = RcsUtils.isRcsOnline();
                boolean isRcsPolicyEnable = RcsDualSimMananger
                        .getUserIsUseRcsPolicy(ComposeMessageActivity.this);

                if (isRcsPolicyEnable) {
                    if (msgItem != null && msgItem.getRcsMsgType()
                            == Constants.MessageConstants.CONST_MESSAGE_PAID_EMOTICON) {
                        if (emotItemCheck(msgItem)) {
                            RcsChatMessageUtils.forwardContactOrConversation(
                                    ComposeMessageActivity.this, new ForwardClickListener());
                            return true;
                        } else {
                            toast(R.string.forward_message_not_support);
                            return true;
                        }
                    }
                    if (!isRcsOnline && msgItem != null) {
                        if (msgItem.getRcsMsgType()
                                == Constants.MessageConstants.CONST_MESSAGE_TEXT) {
                            RcsChatMessageUtils.forwardContactOrConversation(
                                  ComposeMessageActivity.this, new ForwardClickListener());
                        } else if (msgItem.isSms()) {
                            forwardMessage();
                        } else {
                            toast(R.string.rcs_offline_forwards_not_support);
                        }
                    } else if (isRcsOnline && msgItem != null) {
                        RcsChatMessageUtils.forwardContactOrConversation(
                                ComposeMessageActivity.this, new ForwardClickListener());
                    } else {
                        toast(R.string.forward_message_not_support);
                    }
                } else {
                    if (msgItem != null && msgItem.isRcsMessage()) {
                        toast(R.string.rcs_sending_policy_not_support_forwarding);
                    } else if (msgItem != null && msgItem.isSms()) {
                        forwardMessage();
                    } else {
                        toast(R.string.forward_message_not_support);
                    }
                }
            }
            return true;
        }

        public void saveAttachment() {
            MessageItem messageItem = null;
            try {
                Cursor cursor = (Cursor) mMsgListAdapter.getItem(mSelectedPos.get(0));
                messageItem = mMsgListAdapter.getCachedMessageItem(
                        cursor.getString(COLUMN_MSG_TYPE), cursor.getLong(COLUMN_ID), cursor);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (messageItem != null) {
                saveAttachment(messageItem);
            }
        }

        private void saveAttachment(MessageItem msgItem) {
            Cursor cursor = (Cursor)mMsgListAdapter.getItem(mSelectedPos.get(0));
            if (cursor != null && isAttachmentSaveable(cursor)) {
                if (!isRcsMessage(cursor)) {
                    saveAttachment(cursor.getLong(COLUMN_ID));
                }
            } else if (cursor != null && isRcsMessage(cursor)) {
                saveRcsAttachment(msgItem);
            } else {
                Toast.makeText(ComposeMessageActivity.this, R.string.copy_to_sdcard_fail,
                        Toast.LENGTH_SHORT).show();
            }
        }


        private void viewOneToManyStatus() {
              Cursor cursor = (Cursor)mMsgListAdapter.getItem(mSelectedPos.get(0));
              if (cursor != null) {
                  Intent viewInent = new Intent(ACTION_VIEW_ONE_TO_MANY_MSG_STATUS);
                  viewInent.putExtra(EXTRA_VIEW_ONE_TO_MANY_MSG_STATUS_MSG_ID,
                          cursor.getLong(COLUMN_ID));
                  viewInent.putExtra(EXTRA_VIEW_ONE_TO_MANY_MSG_STATUS_MSG_BODY,
                          cursor.getString(COLUMN_SMS_BODY));
                  startActivity(viewInent);
              }
        }

        private class ForwardClickListener implements OnClickListener{

            public void onClick(DialogInterface dialog, int whichButton) {
                switch (whichButton) {
                    case FORWARD_INPUT_NUMBER:
                        inputNumberForwarMessage();
                        break;
                    case FORWARD_CONTACTS:
                       launchRcsPhonePicker();
                        break;
                    case FORWARD_CONVERSATION:
                        Intent intent = new Intent(ComposeMessageActivity.this,
                                ConversationList.class);
                        intent.putExtra(MULTI_SELECT_CONV, true);
                        startActivityForResult(intent, REQUEST_SELECT_CONV);
                        break;
                    case FORWARD_CONTACT_GROUP:
                        launchRcsContactGroupPicker(REQUEST_SELECT_GROUP);
                    default:
                        break;
                }
            }
        }

        private void inputNumberForwarMessage() {
            final EditText editText = new EditText(ComposeMessageActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            editText.setLayoutParams(lp);
            editText.setInputType(InputType.TYPE_CLASS_PHONE);
            editText.setHint(R.string.forward_input_number_hint);
            new AlertDialog.Builder(ComposeMessageActivity.this)
            .setTitle(R.string.forward_input_number_title)
            .setView(editText)
            .setPositiveButton(android.R.string.ok,  new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String input = editText.getText().toString();
                    if (TextUtils.isEmpty(input)) {
                        toast(R.string.forward_input_number_title);
                    } else {
                        forwardForInputNumber(input);
                    }
                }
            }).setNegativeButton(android.R.string.cancel, null)
            .show();
        }

        private void forwardForInputNumber(String input){
            String[] numbers = input.split(";");
            if (numbers != null && numbers.length > 0) {
                ArrayList<String> numberList = new ArrayList<String>();
                for (int i = 0; i < numbers.length; i++) {
                    numberList.add(numbers[i]);
                }
                forwardRcsMessage(numberList);
            }
        }

        private void updateUnFavouriteCount(int favourite, boolean checked) {
            if (favourite != FAVOURITE_MSG) {
                if (checked) {
                    mUnFavouriteCount++;
                } else {
                    mUnFavouriteCount--;
                }
            }
        }

        private void updateRcsSelected(boolean checked) {
            if (checked) {
                mRcsSelected++;
            } else {
                mRcsSelected--;
            }
        }

        private void updateRcsMediaSelected(boolean checked) {
            if (checked) {
                mRcsMediaSelected++;
            } else {
                mRcsMediaSelected--;
            }
        }

        private void forwardMessage() {
            mMessageItems.clear();
            mRcsForwardItems.clear();
            for (Integer pos : mSelectedPos) {
                Cursor c = (Cursor) mMsgListAdapter.getItem(pos);
                mMessageItems.add(mMsgListAdapter.getCachedMessageItem(
                        c.getString(COLUMN_MSG_TYPE), c.getLong(COLUMN_ID), c));
                if (mIsRcsEnabled && mMessageItems.size() > 0) {
                    mRcsForwardItems.addAll(mMessageItems);
                }
            }
            LogTag.debugD("clear DataSetChangedListener");
            mMsgListAdapter.setOnDataSetChangedListener(null);

            final MessageItem msgItem = mMessageItems.get(0);
            getAsyncDialog().runAsync(new Runnable() {
                @Override
                public void run() {
                    // This runnable gets run in a background thread.
                    if (msgItem.mType.equals("mms")) {
                        SendReq sendReq = new SendReq();
                        String subject = getString(R.string.forward_prefix);
                        if (msgItem.mSubject != null) {
                            subject += msgItem.mSubject;
                        }
                        sendReq.setSubject(new EncodedStringValue(subject));
                        sendReq.setBody(msgItem.mSlideshow.makeCopy());

                        mTempMmsUri = null;
                        try {
                            PduPersister persister = PduPersister.getPduPersister(
                                    ComposeMessageActivity.this);
                            // Copy the parts of the message here.
                            mTempMmsUri = persister
                                    .persist(
                                            sendReq,
                                            Mms.Draft.CONTENT_URI,
                                            true,
                                            MessagingPreferenceActivity
                                                .getIsGroupMmsEnabled(
                                                     ComposeMessageActivity.this), null);
                            mTempThreadId = MessagingNotification.getThreadId(
                                    ComposeMessageActivity.this, mTempMmsUri);
                        } catch (MmsException e) {
                            Log.e(TAG, "Failed to copy message: "
                                    + msgItem.mMessageUri);
                            Toast.makeText(ComposeMessageActivity.this,
                                    R.string.cannot_save_message,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        mBodyString = getAllSMSBody();
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Once the above background thread is complete, this
                    // runnable is run
                    // on the UI thread.
                    Intent intent = createIntent(ComposeMessageActivity.this, 0);

                    intent.putExtra(KEY_FORWARDED_MESSAGE, true);
                    if (mTempThreadId > 0) {
                        intent.putExtra(THREAD_ID, mTempThreadId);
                    }

                    if (msgItem.mType.equals("sms")) {
                        intent.putExtra("sms_body", mBodyString);
                    } else {
                        intent.putExtra("msg_uri", mTempMmsUri);
                        String subject = getString(R.string.forward_prefix);
                        if (msgItem.mSubject != null) {
                            subject += msgItem.mSubject;
                        }
                        intent.putExtra("subject", subject);
                        String[] numbers = mConversation.getRecipients()
                                .getNumbers();
                        if (numbers != null) {
                            intent.putExtra("msg_recipient", numbers);
                        }
                    }

                    intent.setClassName(ComposeMessageActivity.this,
                            "com.android.mms.ui.ForwardMessageActivity");
                    startActivity(intent);
                }
            }, R.string.building_slideshow_title);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            updateColorPalette(mActionBarColor);
            mSelectionMenu.dismiss();
        }

        private void updateUnlockedCount(int lock, boolean checked) {
            if (lock != 1) {
                if (checked) {
                    mUnlockedCount++;
                } else {
                    mUnlockedCount--;
                }
            }
        }

        private void  updateMmsSelected(boolean checked) {
            if (checked) {
                mMmsSelected++;
            } else {
                mMmsSelected--;
            }
        }

        private void updateStatics(int pos, boolean checked) {
            Cursor c = (Cursor) getListView().getAdapter().getItem(pos);
            String type = c.getString(COLUMN_MSG_TYPE);
            if (type.equals("mms")) {
                int lock = c.getInt(COLUMN_MMS_LOCKED);
                updateUnlockedCount(lock, checked);
                updateMmsSelected(checked);
            } else if (type.equals("sms")) {
                int lock = c.getInt(COLUMN_SMS_LOCKED);
                updateUnlockedCount(lock, checked);
                if (mIsRcsEnabled && isRcsMessage(c)) {
                    updateRcsSelected(checked);
                    if (c.getInt(COLUMN_RCS_MSG_TYPE) != RcsUtils.RCS_MSG_TYPE_TEXT) {
                        updateRcsMediaSelected(checked);
                    }
                }
            }
            if (mIsRcsEnabled) {
                int favourite = c.getInt(COLUMN_FAVOURITE);
                updateUnFavouriteCount(favourite, checked);
            }
        }

        private boolean isAttachmentSaveable(Cursor cursor) {
            MessageItem messageItem = mMsgListAdapter.getCachedMessageItem(
                    cursor.getString(COLUMN_MSG_TYPE),
                    cursor.getLong(COLUMN_ID), cursor);
            return messageItem != null && messageItem.hasAttachemntToSave();
        }

        private void customMenuVisibility(ActionMode mode, int checkedCount,
                int position, boolean checked) {

            boolean noRcsSelected = mRcsSelected == 0;
            boolean noRcsMediaSelected = mRcsMediaSelected == 0;
            mode.getMenu().findItem(R.id.lock).setVisible(false);
            if (checkedCount > 1) {
                // no detail
                mode.getMenu().findItem(R.id.detail).setVisible(false);
                // no delivery report
                mode.getMenu().findItem(R.id.report).setVisible(false);
                // no save attachment
                mode.getMenu().findItem(R.id.save_attachment).setVisible(false);
                // no resend
                mode.getMenu().findItem(R.id.resend).setVisible(false);
                // no copy text
                mode.getMenu().findItem(R.id.copy_text).setVisible(false);

                // all locked show unlock, other wise show lock.
                /*
                if (mUnlockedCount == 0) {
                    mode.getMenu()
                            .findItem(R.id.lock)
                            .setTitle(
                                    getContext()
                                            .getString(R.string.menu_unlock));
                } else {
                    mode.getMenu()
                            .findItem(R.id.lock)
                            .setTitle(
                                    getContext().getString(R.string.menu_lock));
                }*/

                if (mMmsSelected > 0 || (mIsRcsEnabled && !noRcsMediaSelected)) {
                    mode.getMenu().findItem(R.id.forward).setVisible(false);
                    mode.getMenu().findItem(R.id.copy_to_sim).setVisible(false);
                } else {
                    if (getResources().getBoolean(R.bool.config_forwardconv)) {
                        mode.getMenu().findItem(R.id.forward).setVisible(true);
                    } else {
                        mode.getMenu().findItem(R.id.forward).setVisible(false);
                    }
                    mode.getMenu().findItem(R.id.copy_to_sim).setVisible(true);
                }

            } else {
                mode.getMenu().findItem(R.id.detail).setVisible(true);
                if (mIsRcsEnabled) {
                    mode.getMenu().findItem(R.id.save_attachment)
                            .setVisible(mMmsSelected > 0 || mRcsMediaSelected > 0);
                } else {
                    mode.getMenu().findItem(R.id.save_attachment).setVisible(mMmsSelected > 0);
                }
                /*
                if (mUnlockedCount == 0) {
                    mode.getMenu()
                            .findItem(R.id.lock)
                            .setTitle(
                                    getContext()
                                            .getString(R.string.menu_unlock));
                } else {
                    mode.getMenu()
                            .findItem(R.id.lock)
                            .setTitle(
                                    getContext().getString(R.string.menu_lock));
                }*/

                mode.getMenu().findItem(R.id.resend).setVisible(isFailedMessage(position));
                if (mIsRcsEnabled) {
                    if (noRcsSelected) {
                        mode.getMenu().findItem(R.id.forward)
                                .setVisible(isMessageForwardable(position));
                    } else {
                        mode.getMenu().findItem(R.id.forward).setVisible(true);
                    }
                } else {
                    mode.getMenu().findItem(R.id.forward).setVisible(isMessageForwardable(position));
                }

                if (mMmsSelected > 0 || (mIsRcsEnabled && !noRcsMediaSelected)) {
                    mode.getMenu().findItem(R.id.copy_to_sim).setVisible(false);
                    mode.getMenu().findItem(R.id.copy_text).setVisible(false);
                } else {
                    mode.getMenu().findItem(R.id.copy_to_sim).setVisible(true);
                    mode.getMenu().findItem(R.id.copy_text).setVisible(true);
                }

                mode.getMenu().findItem(R.id.report).setVisible(isDeliveryReportMsg(position));
            }
            MenuItem moremenu = mode.getMenu().findItem(R.id.more);
            boolean hasVisible = moremenu.getSubMenu().hasVisibleItems();
            moremenu.setVisible(hasVisible);
        }

        private MessageItem getMessageItemByPos(int position) {
            if (mMsgListAdapter.getItemViewType(position)
                    != MessageListAdapter.GROUP_CHAT_ITEM_TYPE) {
                Cursor cursor = (Cursor) mMsgListAdapter.getItem(position);
                if (cursor != null) {
                    return mMsgListAdapter.getCachedMessageItem(
                            cursor.getString(COLUMN_MSG_TYPE),
                            cursor.getLong(COLUMN_ID), cursor);
                }
            }
            return null;
        }

        private boolean isDeliveryReportMsg(int position) {
            MessageItem msgItem = getMessageItemByPos(position);
            if (msgItem == null) {
                return false;
            }

            if (msgItem.mDeliveryStatus != MessageItem.DeliveryStatus.NONE ||
                    msgItem.mReadReport) {
                return true;
            } else {
                return false;
            }
        }

        private boolean isMessageForwardable(int position) {
            MessageItem msgItem = getMessageItemByPos(position);
            if (msgItem == null) {
                return false;
            }

            return msgItem.isSms() || (msgItem.isDownloaded() && msgItem.mIsForwardable);
        }

        private boolean isFailedMessage(int position) {
            MessageItem msgItem = getMessageItemByPos(position);
            if (msgItem == null) {
                return false;
            }
            return msgItem.isFailedMessage();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                long id, boolean checked) {
            logMultiChoice("onItemCheckedStateChanged... position=" + position
                    + ", checked=" + checked);
            if (mMsgListAdapter == null || mMsgListAdapter.getCount() <= 0) {
                logMultiChoice("onItemCheckedStateChanged list not ready!");
                return;
            }

            mCheckedCount = getListView().getCheckedItemCount();
            updateStatics(position, checked);
            customMenuVisibility(mode, mCheckedCount, position, checked);
            mSelectionMenu.setTitle(getApplicationContext().getString(
                    R.string.selected_count, mCheckedCount));
            mSelectionMenu.updateSelectAllMode(getMsgCount() == mCheckedCount);
            mSelectionMenu.updateCheckedCount();
        }

        private void confirmDeleteDialog(final DeleteMessagesListener listener,
                boolean locked) {
            View contents = View.inflate(ComposeMessageActivity.this,
                    R.layout.delete_thread_dialog_view, null);
            TextView msg = (TextView) contents.findViewById(R.id.message);
            msg.setText(getString(R.string.confirm_delete_selected_messages));
            final CheckBox checkbox = (CheckBox) contents
                    .findViewById(R.id.delete_locked);
            checkbox.setChecked(false);
            if (mSelectedLockedMsg.size() == 0) {
                checkbox.setVisibility(View.GONE);
            } else {
                listener.setDeleteLockedMessage(checkbox.isChecked());
                checkbox.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        listener.setDeleteLockedMessage(checkbox.isChecked());
                    }
                });
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    ComposeMessageActivity.this);
            builder.setTitle(R.string.confirm_dialog_title);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setCancelable(true);
            builder.setView(contents);
            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, null);
            builder.show();
        }
    }

    private void handleUnsupportedTypeWarning(final Runnable runnable) {
        if (runnable != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.restricted_format));
            builder.setMessage(R.string.attach_anyway);
            builder.setIcon(R.drawable.ic_sms_mms_not_delivered);
            builder.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            ContentRestrictionFactory
                                    .initContentRestriction(
                                    MessagingPreferenceActivity.CREATION_MODE_FREE);
                            runnable.run();
                        }
                    });
            builder.setNegativeButton(R.string.no, null);
            builder.create().show();
        }
    }

    private void handleMessageSizeExceededWarning(final Runnable runnable) {
        if (runnable != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.exceed_message_size_limitation));
            builder.setMessage(R.string.attach_anyway);
            builder.setIcon(R.drawable.ic_sms_mms_not_delivered);
            builder.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            ContentRestrictionFactory
                                    .initContentRestriction(
                                    MessagingPreferenceActivity.CREATION_MODE_FREE);
                            runnable.run();
                        }
                    });
            builder.setNegativeButton(R.string.no, null);
            builder.create().show();
        }
    }

    private void setBackgroundWallpaper() {
        SharedPreferences mPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String imageUri = mPreferences.getString(
                MessagingPreferenceActivity.CHAT_WALLPAPER, null);
        if (!TextUtils.isEmpty(imageUri)) {
            Bitmap bitmap = BitmapFactory.decodeFile(mPreferences
                    .getString(
                            MessagingPreferenceActivity.CHAT_WALLPAPER, null));
            if(bitmap != null) {
                mMsgListView.setBackground(new BitmapDrawable(bitmap));
            }
        }
    }
    private void setTextFontsize() {
        int size =  MessageUtils.getFontSize();
        if (mTextEditor != null) {
            mTextEditor.setTextSize(size);
        }
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setTextSize(size);
        }

        if (mMsgListView != null
                && mMsgListView.getVisibility() == View.VISIBLE) {
            int count = mMsgListView.getChildCount();
            for (int i = 0; i < count; i++) {
                MessageListItem item = (MessageListItem) mMsgListView
                        .getChildAt(i);
                if (item != null) {
                    item.setBodyTextSize(size);
                }
            }
        }
    }

    /* Begin add for RCS */

    private boolean processedRcsActivityResult(int requestCode, Intent data) {
        boolean isRcsMessage = (requestCode == REQUEST_CODE_ATTACH_IMAGE)
                || (requestCode == REQUEST_CODE_TAKE_PICTURE)
                || (requestCode == REQUEST_CODE_ATTACH_VIDEO)
                || (requestCode == PHOTO_CROP)
                || (requestCode == REQUEST_CODE_TAKE_VIDEO)
                || (requestCode == REQUEST_CODE_ATTACH_SOUND)
                || (requestCode == REQUEST_CODE_RECORD_SOUND)
                || (requestCode == REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD)
                || (requestCode == REQUEST_CODE_ATTACH_ADD_CONTACT_RCS_VCARD)
                || (requestCode == REQUEST_CODE_ATTACH_MAP)
                || (requestCode == REQUEST_CODE_VCARD_GROUP)
                || (requestCode == REQUEST_CODE_SAIYUN)
                || (requestCode == REQUEST_SELECT_LOCAL_AUDIO);
        boolean isMms = mWorkingMessage.requiresMms();
        boolean rcsAttachment = mWorkingMessage.requiringRcsAttachment();
        if (!isMms && rcsAttachment && mIsRcsEnabled && RcsUtils.isRcsOnline() && isRcsMessage
                && (mSendButtonMms != null && mSendButtonMms.getVisibility() == View.GONE)) {
            if (data == null) {
                // taking picture and taking video do not need to check data.
                if (requestCode != REQUEST_CODE_TAKE_PICTURE
                        && requestCode != REQUEST_CODE_TAKE_VIDEO) {
                    return true;
                }
            }
            switch (requestCode) {
                case PHOTO_CROP:
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_IMAGE);
                    Uri cropData = data.getData();
                    if (cropData == null) {
                        break;
                    }
                    String cropPath = getRealPathFromURI(cropData);
                    if (!isAttachmentFileLegality(cropPath, RcsUtils.RCS_MSG_TYPE_IMAGE)) {
                        return true;
                    }
                    mWorkingMessage.setIsBurn(mIsBurnMessage);
                    mWorkingMessage.setRcsPath(cropPath);
                    break;
                case REQUEST_CODE_ATTACH_IMAGE:
                    Uri uriData = data.getData();
                    mRcsAttachmentUri = uriData;
                    String imagePath = RcsFileController.getFilePath(this, uriData);
                    if (imagePath.toLowerCase().endsWith("gif")) {
                        if (!isAttachmentFileLegality(imagePath, RcsUtils.RCS_MSG_TYPE_IMAGE)) {
                            return true;
                        }
                        mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_IMAGE);
                        mWorkingMessage.setIsBurn(mIsBurnMessage);
                        mWorkingMessage.setRcsPath(imagePath);
                    } else {
                        imageDispose(imagePath);
                        mRcsLargeImagePath = imagePath;
                    }
                    break;
                case REQUEST_CODE_TAKE_PICTURE:
                    Uri pictureUri = TempFileProvider.renameScrapFile(".jpg",
                            String.valueOf(System.currentTimeMillis()), this);
                    if (pictureUri == null) {
                        return true;
                    }
                    String takePictureImagePath = RcsFileController.getFilePath(this, pictureUri);
                    if (!TextUtils.isEmpty(takePictureImagePath)) {
                        mRcsAttachmentUri = pictureUri;
                        imageDispose(takePictureImagePath);
                        mRcsLargeImagePath = takePictureImagePath;
                    }
                    break;
                case REQUEST_CODE_ATTACH_VIDEO:
                    mRcsAttachmentUri = data.getData();
                    String videoPtch = RcsFileController.getFilePath(this, data.getData());
                    if (!isAttachmentFileLegality(videoPtch, RcsUtils.RCS_MSG_TYPE_VIDEO)) {
                        return true;
                    }
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VIDEO);
                    mWorkingMessage.setRcsPath(videoPtch);
                    mWorkingMessage.setDuration(RcsUtils.getDuration(this, data.getData()));
                    mWorkingMessage.setIsRecord(false);
                    break;
                case REQUEST_CODE_TAKE_VIDEO:
                    Uri videoUri = TempFileProvider.renameScrapFile(".3gp",
                            String.valueOf(System.currentTimeMillis()),
                            ComposeMessageActivity.this);
                    if (videoUri == null) {
                        return true;
                    }
                    mRcsAttachmentUri = videoUri;
                    int videoDuration = RcsUtils.getDuration(
                            ComposeMessageActivity.this, videoUri);
                    if (videoDuration < 1000) {
                        toast(R.string.rcs_take_video_duration_too_small);
                        return true;
                    }
                    String takeVideoPtch = RcsFileController.getFilePath(this, videoUri);
                    if (!isAttachmentFileLegality(takeVideoPtch, RcsUtils.RCS_MSG_TYPE_VIDEO)) {
                        return true;
                    }
                    if (videoDuration >= 1) {
                        mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VIDEO);
                        mWorkingMessage.setRcsPath(takeVideoPtch);
                        mWorkingMessage.setDuration(videoDuration);
                        mWorkingMessage.setIsRecord(true);
                    } else {
                        toast(R.string.cannot_send_video);
                    }
                    break;
                case REQUEST_CODE_ATTACH_SOUND:
                    // Attempt to add the audio to the attachment.
                    Uri uri = (Uri)data.getParcelableExtra(
                            RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri == null) {
                        uri = data.getData();
                    } else if (Settings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                        break;
                    }
                    mRcsAttachmentUri = uri;
                    String audioPath = RcsFileController.getFilePath(this, uri);
                    if (!isAttachmentFileLegality(audioPath, RcsUtils.RCS_MSG_TYPE_AUDIO)) {
                        return true;
                    }
                    mWorkingMessage.setRcsPath(audioPath);
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_AUDIO);
                    mWorkingMessage.setDuration(RcsUtils.getDuration(this, uri));
                    mWorkingMessage.setIsRecord(false);
                    break;
                case REQUEST_SELECT_LOCAL_AUDIO:
                    if (data != null) {
                        Uri selectUri = data.getData();
                        mRcsAttachmentUri = selectUri;
                        String path = RcsFileController.getFilePath(this, selectUri);
                        if (!isAttachmentFileLegality(path, RcsUtils.RCS_MSG_TYPE_AUDIO)) {
                            return true;
                        }
                        if(!TextUtils.isEmpty(path) && path.contains(".")){
                            String endsWith = path.substring(path.lastIndexOf("."),
                                    path.length()).toLowerCase();
                            if (endsWith.equals(".3gp") || endsWith.equals(".mp3")
                                || endsWith.equals(".amr")|| endsWith.equals(".aac")
                                || endsWith.equals(".m4a")) {
                                mWorkingMessage.setRcsPath(path);
                                mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_AUDIO);
                                mWorkingMessage.setDuration(
                                        RcsUtils.getDuration(this, selectUri));
                                mWorkingMessage.setIsRecord(false);
                            } else {
                                toast(R.string.audio_file_error);
                            }
                        }
                    }
                    break;
                case REQUEST_CODE_RECORD_SOUND:
                    Uri audioUri = data.getData();
                    mRcsAttachmentUri = audioUri;
                    audioPath = RcsFileController.getFilePath(this, audioUri);
                    if (!isAttachmentFileLegality(audioPath, RcsUtils.RCS_MSG_TYPE_AUDIO)) {
                        return true;
                    }
                    int audioDuration = RcsUtils.getDuration(this, audioUri);
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_AUDIO);
                    mWorkingMessage.setRcsPath(audioPath);
                    mWorkingMessage.setDuration(audioDuration);
                    mWorkingMessage.setIsRecord(true);
                    break;
                case REQUEST_CODE_VCARD_GROUP:
                    if (data == null) {
                        return true;
                    }
                    Bundle bundle = data.getExtras().getBundle("result");
                    final Set<String> keySet = bundle.keySet();
                    final int recipientCount = (keySet != null) ? keySet.size() : 0;
                    final ContactList list = ContactList.blockingGetByUris(buildUris(keySet,
                            recipientCount));
                    StringBuffer buffer = new StringBuffer();
                    for (Contact contact : list) {
                        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                                contact.getPersonId());
                        String lookup = Uri.encode(Contacts
                                .getLookupUri(this.getContentResolver(), contactUri)
                                .getPathSegments().get(2));
                        buffer.append(lookup + ":");
                    }
                    String subStringbuffer = buffer.substring(0, buffer.lastIndexOf(":"));
                        Uri multiVcardUri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI,
                                Uri.encode(subStringbuffer));
                        String vcardPath = RcsUtils.createVcardFile(ComposeMessageActivity.this,
                                multiVcardUri);
                        mWorkingMessage.setVcardPath(vcardPath);
                        mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VCARD);
                    break;
                case REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD:
                        // In a case that a draft message has an attachment whose type is slideshow,
                        // then reopen it and replace the attachment through attach icon, we have to
                        // remove the old attachement silently first.
                        if (mWorkingMessage != null) {
                            mWorkingMessage.removeAttachment(false);
                        }
                        String extraVCard = data.getStringExtra(MultiPickContactsActivity.EXTRA_VCARD);
                        if (extraVCard != null) {
                            Uri vcard = Uri.parse(extraVCard);
                            mRcsAttachmentUri = vcard;
                            String contactVcardPath = RcsUtils.createVcardFile(
                                    ComposeMessageActivity.this, vcard);
                            mWorkingMessage.setVcardPath(contactVcardPath);
                            mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VCARD);
                        }
                    break;
                case REQUEST_CODE_ATTACH_MAP:
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_MAP);
                    double latitude = data.getDoubleExtra("latitude", 39.90865);
                    mWorkingMessage.setLatitude(latitude);
                    double longitude = data.getDoubleExtra("longitude", 116.39751);
                    mWorkingMessage.setLongitude(longitude);
                    String address = data.getStringExtra("address");
                    mWorkingMessage.setLocation(address);
                    break;
                case REQUEST_CODE_SAIYUN:
                    String id = data.getStringExtra("id");
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_CAIYUNFILE);
                    mWorkingMessage.setCloudFileId(id);
                    break;
                default:
                    break;
            }
            if (!isDisposeImage) {
                cacheWorkingMessage();
            }
            return true;
        }
        if (!RcsUtils.isRcsOnline() && mConversation.isGroupChat()) {
            toast(R.string.rcs_offline_on_groupchat);
            return true;
        }
        return false;
    }

    private void setRcsAttachment() {
        int attachmentState = RcsDualSimMananger.getAttachmentState(
                ComposeMessageActivity.this);
        if (attachmentState == RcsDualSimMananger.RCS_ATTACHMENT &&
                !mWorkingMessage.requiresMms()) {
            mWorkingMessage.setRequiringRcsAttachment(true);
        } else if (attachmentState == RcsDualSimMananger.DEFAULT_ATTACHMENT) {
            mWorkingMessage.setRequiringRcsAttachment(false);
        } else if (attachmentState ==
                RcsDualSimMananger.RCS_OFFLINE_AND_RCS_MESSAGE_ONLY) {
            toast(R.string.rcs_offline_not_support_attachment);
        }
    }

    private boolean hasConvertRcsAttachmentToMmsAndSent() {
        boolean shouldSendMsgWithRcsPolicy = mWorkingMessage.shouldSendMessageWithRcsPolicy();
        if (shouldSendMsgWithRcsPolicy) {
            return false;
        } else if (!shouldSendMsgWithRcsPolicy && mWorkingMessage.requiringRcsAttachment()) {
            mWorkingMessage.setRequiringRcsAttachment(false);
            try {
                int mRcsType = mWorkingMessage.getRcsType();
                if (isRcsMediaMsg(mRcsType)) {
                    long attachFilesize = RcsFileController.getFileSizes(
                            MmsApp.getApplication().getApplicationContext(), mRcsAttachmentUri);
                    if (attachFilesize / BYTE_TO_KB > MMS_LIMIT_SIZE) {
                        if (mWorkingMessage.getRcsType() == RcsUtils.RCS_MSG_TYPE_IMAGE) {
                            mSendAfterResize = true;
                        } else {
                            mWorkingMessage.setRequiringRcsAttachment(true);
                            toast(R.string.exceed_message_size_limitation);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                RcsLog.w(e);
            }
            switch (mWorkingMessage.getRcsType()) {
                case RcsUtils.RCS_MSG_TYPE_IMAGE:
                    addImage(mRcsAttachmentUri, false);
                    if (!mSendAfterResize) {
                        sendMessage(true);
                    }
                    break;
                case RcsUtils.RCS_MSG_TYPE_VIDEO:
                    addVideo(mRcsAttachmentUri, false);
                    sendMessage(true);
                    break;
                case RcsUtils.RCS_MSG_TYPE_AUDIO:
                    addAudio(mRcsAttachmentUri, false);
                    sendMessage(true);
                    break;
                case RcsUtils.RCS_MSG_TYPE_VCARD:
                    addVcard(mRcsAttachmentUri);
                    sendMessage(true);
                    break;
                default:
                    toast(R.string.rcs_msgtype_only);
                    if (mWorkingMessage.getRcsType() == RcsUtils.RCS_MSG_TYPE_PAID_EMO) {
                        resetRcsMessage();
                    } else {
                        mWorkingMessage.setRequiringRcsAttachment(true);
                    }
                    break;
            }
            return true;
        }
        return false;
    }

    private boolean isAttachmentFileLegality(String filePath, int fileType) {
        int checkedResult = RcsFileController.checkFileLegality(filePath, fileType);
        if (checkedResult == RcsFileController.FILE_NOT_EXCEEDED_RCS_LIMIT) {
            return true;
        } else if (checkedResult == RcsFileController.FILE_EXCEEDED_RCS_LIMIT) {
            int resId = R.string.file_size_error_reselect;
            switch(fileType) {
                case RcsUtils.RCS_MSG_TYPE_AUDIO:
                    resId = R.string.audio_file_size_error_reselect;
                    break;
                case RcsUtils.RCS_MSG_TYPE_IMAGE:
                    resId = R.string.image_file_size_error_reselect;
                    break;
                case RcsUtils.RCS_MSG_TYPE_VIDEO:
                    resId = R.string.vido_file_size_error_reselect;
                    break;
                default:
                    break;
            }
            toast(resId);
            return false;
        } else {
            toast(R.string.file_error_reselect);
            return false;
        }
    }

    private void showSetRcsPolicyDialog() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setMessage(R.string.rcs_message_sent_first_time)
                .setPositiveButton(R.string.send_confirm_ok,
                 new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        rcsSend();
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.set_poliy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        Intent intent = new Intent(ComposeMessageActivity.this,
                                MessagingPreferenceActivity.class);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                }).create();
        alert.setCanceledOnTouchOutside(false);
        alert.setCancelable(false);
        alert.show();
    }

    private boolean isAirPlaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void launchRcsContactGroupPicker(int requestCode) {
        Intent intent = new Intent(this, MultiPickContactGroups.class);
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            toast(R.string.contact_app_not_found);
        }
    }

    public boolean isRcsMediaMsg(int rcsType) {
        if (rcsType == RcsUtils.RCS_MSG_TYPE_IMAGE
                || rcsType == RcsUtils.RCS_MSG_TYPE_AUDIO
                || rcsType == RcsUtils.RCS_MSG_TYPE_VIDEO) {
            return true;
        } else {
            return false;
        }
    }

    private final Handler mToastMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case OPERATE_SUCCESS:
                    toast(R.string.operate_success);
                    break;
                case OPERATE_FAILURE:
                    toast(R.string.operate_failure);
                    break;
                default:
                    break;
            }
        }
    };

    private void sendRcsOption() {
        ContactList recipients = getRecipients();
        Contact data = recipients.get(0);
        if (data != null && !TextUtils.isEmpty(data.getNumber())) {
            checkCapability(data.getNumber());
        }
    }

    private void initRcsComponents() {
        Intent intent = getIntent();
        // Whether is creating a RCS group chat.
        if (intent.hasExtra("isGroupChat") && mConversation != null) {
            boolean isGroupChat = intent.getBooleanExtra("isGroupChat", false);
            mConversation.setIsGroupChat(isGroupChat);
        }
        if (intent.hasExtra("fromNormol")) {
            mCreateNewMessageFromConversationList = intent.getBooleanExtra("fromNormol", false);
        }
        mMessageApi = MessageApi.getInstance();
        mBasicApi = BasicApi.getInstance();
        mCapabilityApi = CapabilityApi.getInstance();
        mSupportApi = SupportApi.getInstance();
        mProfileApi = ProfileApi.getInstance();
        mGroupChatApi = GroupChatApi.getInstance();
        if (mButtonEmoj != null) {
            mButtonEmoj.setVisibility(mIsRcsEnabled ? View.VISIBLE : View.GONE);
        }
        if (mIsRcsEnabled) {
            if (mConversation.isGroupChat()) {
                initGroupChat(intent);
            } else if (1 == getRecipients().size()) {
                sendRcsOption();
            }
        }
    }

    private void initGroupChat(Intent intent) {
        long groupId = intent.getLongExtra("groupId", -1);
        if (groupId != -1) {
            try {
                GroupChat groupChat = mGroupChatApi.getGroupChatById(groupId);
                mConversation.setGroupChat(groupChat);
                mSentMessage = true;
            } catch (ServiceDisconnectedException e) {
                RcsLog.w("Exception initRcsComponents()" + e);
            } catch (RemoteException e) {
                RcsLog.w("Exception initRcsComponents()" + e);
            }
        } else {
            long threadId = mConversation.getThreadId();
            if (threadId > 0) {
                try {
                    GroupChat groupChat = mGroupChatApi
                            .getGroupChatByThreadId(threadId);
                    mConversation.setGroupChat(groupChat);
                    mSentMessage = true;
                } catch (ServiceDisconnectedException e) {
                    RcsLog.w("Exception initRcsComponents()" + e);
                } catch (RemoteException e) {
                    RcsLog.w("Exception initRcsComponents()" + e);
                }
            }
        }
    }

    private void registerRcsReceiver(){
        IntentFilter fileFilter = new IntentFilter();
        fileFilter.addAction(Actions.MessageAction.ACTION_MESSAGE_FILE_TRANSFER_PROGRESS);
        fileFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        fileFilter.addAction(Actions.ACTION_ERROR);
        registerReceiver(mFileTranferReceiver, fileFilter);
        registerReceiver(mGroupReceiver, new IntentFilter(
                Actions.GroupChatAction.ACTION_GROUP_CHAT_MANAGE_NOTIFY));
        IntentFilter cloudFileFilter = new IntentFilter();
        cloudFileFilter.addAction(Actions.PluginAction.ACTION_MCLOUD_DOWNLOAD_FILE_FROM_URL);
        cloudFileFilter.addAction(Actions.PluginAction.ACTION_MCLOUD_SHARE_AND_SEND_FILE);
        cloudFileFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mCloudFileReceiver, cloudFileFilter);
        IntentFilter emotionFilter = new IntentFilter();
        emotionFilter.addAction(Actions.MessageAction.ACTION_MESSAGE_DOWNLOAD_EMOTICON_RESULT);
        registerReceiver(mEmotionDownloadReceiver, emotionFilter);
        IntentFilter rcsStatusUpdateFilter = new IntentFilter(
                RcsContactsUtils.NOTIFY_CONTACT_PHOTO_CHANGE);
        rcsStatusUpdateFilter.addAction(Actions.RegisterAction.ACTION_REGISTER_STATUS_CHANGED);
        registerReceiver(mRcsStatusUpdateReceiver, rcsStatusUpdateFilter);
    }

    private void unregisterRcsReceiver() {
        try {
            unregisterReceiver(mFileTranferReceiver);
            unregisterReceiver(mGroupReceiver);
            unregisterReceiver(mCloudFileReceiver);
            unregisterReceiver(mEmotionDownloadReceiver);
            unregisterReceiver(mRcsStatusUpdateReceiver);
        } catch (Exception e) {
            RcsLog.w(e);
        }
    }

    private void addBurnMessageMenu(Menu menu) {
        if (mHasBurnCapability && RcsUtils.isRcsOnline()) {
            MenuItem burnMenu = menu.add(0, MENU_RCS_BURN_MESSGEE_FLAG, 0,
                    getString(R.string.burn_message_flag));
            burnMenu.setCheckable(true);
            burnMenu.setChecked(mIsBurnMessage);
            burnMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem arg0) {
                    mIsBurnMessage = !mIsBurnMessage;
                    arg0.setChecked(mIsBurnMessage);
                    mWorkingMessage.setIsBurn(mIsBurnMessage);
                    return false;
                }
            });
        }
    }

    private void addSwitchToGroupChatMenuItem(Menu menu) {
        MenuItem item = menu.add(0, MENU_RCS_SWITCH_TO_GROUP_CHAT, 0,
                getString(R.string.switch_to_group_chat));
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switchToGroupChat();
                return false;
            }
        });
    }

    private void switchToGroupChat() {
        try {
            String numbers = RcsUtils.getNumbersExceptMe(getRecipients());
            Intent intent = new Intent(CREATE_GROUP_CHAT);
            intent.putExtra(RcsCreateGroupChatActivity.EXTRA_RECIPIENTS, numbers);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            RcsLog.e(e.toString());
        }
    }

    private void checkCapability(final String number) {
        try {
            mCapabilityApi.getCapability(number, false, new CapabiltyListener() {
                @Override
                public void onCallback(RCSCapabilities capabilities, int resultCode,
                        String resultDesc, String number) throws RemoteException {
                    RcsLog.i("resultCode = " + resultCode + " RCSCapabilities = "
                            + (capabilities == null ? "null" : capabilities.toString()));
                    if (resultCode == RcsUtils.RCS_CAPABILITY_RESULT_SUCCESS && capabilities != null
                            && capabilities.isBurnAfterReading()) {
                        mHasBurnCapability = true;
                    } else {
                        mHasBurnCapability = false;
                    }
                }
            });
        } catch (RemoteException e) {
            RcsLog.i(e.toString());
        } catch (ServiceDisconnectedException e) {
            RcsLog.i(e.toString());
        }
    }

    Runnable mResetRcsMessageRunnable = new Runnable() {
        @Override
        public void run() {
            resetRcsMessage();
        }
    };

    private void resetRcsMessage() {
        mAttachmentEditor.hideView();
        mAttachmentEditorScrollView.setVisibility(View.GONE);
        showSubjectEditor(false);
        CharSequence text = mWorkingMessage.getText();
        mWorkingMessage.clearConversation(mConversation, false);
        mWorkingMessage = WorkingMessage.createEmpty(this);
        if (!TextUtils.isEmpty(text)) {
            mWorkingMessage.setText(text);
        }
        mWorkingMessage.setConversation(mConversation);
        hideRecipientEditor();
        updateSendButtonState();
        if (mIsLandscape) {
            hideKeyboard();
        }
        mLastRecipientCount = 0;
        mSendingMessage = false;
        invalidateOptionsMenu();
        if (mIsRcsEnabled) {
            cancelRcsMessageCache();
        }
    }

    private void showAddBlacklistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.firewall_add_blacklist_wring);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RcsUtils.addNumberToFirewall(ComposeMessageActivity.this,
                        mConversation.getRecipients(), true);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onPreRcsMessageSent() {
        runOnUiThread(mResetRcsMessageRunnable);
    }

    private void cacheWorkingMessage() {
        mWorkingMessage.setCacheRcsMessage(true);
        updateSendButtonState();
        mRcsThumbnailLayout.setVisibility(View.VISIBLE);
        mWorkingMessage.setIsBurn(mIsBurnMessage);
        ImageView imageView = (ImageView) findViewById(R.id.image_view_thumbnail);
        RcsUtils.setThumbnailForMessageItem(this, imageView, mWorkingMessage);
        mButtonEmoj.setEnabled(false);
        mTextEditor.setEnabled(false);
        findViewById(R.id.remove_attachment_button).setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDisposeImage) {
                    cancelRcsMessageCache();
                    mWorkingMessage.clearCacheRcsMessage();
                }
            }
        });
    }

    private void cancelRcsMessageCache(){
        mWorkingMessage.setCacheRcsMessage(false);
        mRcsThumbnailLayout.setVisibility(View.GONE);
        mButtonEmoj.setEnabled(true);
        mTextEditor.setEnabled(true);
        mTextEditor.requestFocus();
        mWorkingMessage.setRequiringRcsAttachment(false);
    }

    private void imageDispose(final String photoPath){
        isDisposeImage = true;
        String[] imageItems = getResources().getStringArray(R.array.del_image_mode);
        new AlertDialog.Builder(ComposeMessageActivity.this)
                .setTitle(R.string.del_image_action)
                .setItems(imageItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isDisposeImage = false;
                switch (which) {
                    case RCS_PHOTO_CUT:
                        File mCurrentPhotoFile = new File(photoPath);
                        doCropPhoto(mCurrentPhotoFile);
                        dialog.dismiss();
                        break;
                    case RCS_PHOTO_ZOOM:
                        showQualityDialog(photoPath);
                        break;
                    case RCS_PHOTO_ORIGINAL:
                        if (!isAttachmentFileLegality(photoPath, RcsUtils.RCS_MSG_TYPE_IMAGE)) {
                            dialog.dismiss();
                            return;
                        }
                        mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_IMAGE);
                        mWorkingMessage.setRcsPath(photoPath);
                        cacheWorkingMessage();
                        dialog.dismiss();
                        break;
                    case RCS_PHOTO_CANCEL:
                        dialog.dismiss();
                        break;
                    default:
                        break;
                }
            }
        }).create().show();
    }

    private void doCropPhoto(File file) {
        try {
            OpenRcsMessageIntent intent = new OpenRcsMessageIntent("com.android.camera.action.CROP");
            intent.setDataAndType(Uri.fromFile(file), RcsUtils.RCS_MSG_IMAGE_TYPE_ALL);
            intent.putExtra("crop", true);
            startActivityForResult(intent, PHOTO_CROP);
        } catch (Exception e) {
            Toast.makeText(ComposeMessageActivity.this, R.string.not_intent,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showQualityDialog(final String photoPath) {
        final EditText editText = new EditText(ComposeMessageActivity.this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(R.string.please_input_1_100_int);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Pattern pattern = Pattern.compile("^(?:[0-9]?\\d|100|00[1-9])$");
                Matcher matcher = pattern.matcher(s);
                if (!matcher.find()) {
                    s.clear();
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.input_no_fit, Toast.LENGTH_SHORT).show();
                }
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(ComposeMessageActivity.this);
        builder.setTitle(R.string.input_quality);
        builder.setView(editText);
        builder.setPositiveButton(R.string.send_confirm_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String quality = editText.getText().toString().trim();
                if (TextUtils.isEmpty(quality) || Long.parseLong(quality) > Integer.MAX_VALUE
                        || Integer.parseInt(quality) == 0 || Integer.parseInt(quality) > 100) {
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.input_no_fit, Toast.LENGTH_SHORT).show();
                } else {
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_IMAGE);
                    mWorkingMessage.setRcsPath(photoPath);
                    mWorkingMessage.setScaling(quality);
                    cacheWorkingMessage();
                }
            }
        });
        builder.setNegativeButton(R.string.send_comfirm_cancel, null);
        builder.create().show();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {
                MediaStore.Images.Media.DATA
        };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        String path = null;
        if (cursor != null && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(column_index);
        }
        if (cursor != null) {
            cursor.close();
        }
        return path;
    }

    private boolean emotItemCheck(MessageItem messageItem) {
        try {
            String fileName = messageItem.getRcsPath();
            String[] id = fileName.split(",");
            return EmoticonApi.getInstance().isCanSend(id[0]);
        } catch (ServiceDisconnectedException e) {
            e.printStackTrace();
            return false;
        } catch (RemoteException e) {
            RcsLog.w(e.toString());
            return false;
        }
    }

    private void notifyChangeGroupChat(long groupId) {
        if (groupId > 0) {
            mMsgListAdapter.setRcsGroupId(groupId);
            mMsgListAdapter.notifyDataSetChanged();
        }
    }

    private void showProgressDialog(Context context, int progress, String title, int total) {
        RcsLog.i("ComposeMessageActivty enter showProgressDialog");
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setButton(
                    context.getResources().getString(R.string.cacel_back_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mProgressDialog.cancel();
                                mSimpleMsgs.clear();
                            } catch (Exception e) {
                                RcsLog.e(e.toString());
                            }
                        }
                    });
        }
        if (total > 0) {
            mProgressDialog.setMax(total);
        }
        mProgressDialog.setMessage(title);
        mProgressDialog.setProgress(progress);
        mProgressDialog.show();
    }

    private BroadcastReceiver mEmotionDownloadReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Actions.MessageAction.ACTION_MESSAGE_DOWNLOAD_EMOTICON_RESULT.equals(action)) {
                boolean downloadResult =
                        intent.getBooleanExtra(Parameter.EXTRA_RESULT, false);
                long msgId = intent.getLongExtra(Parameter.EXTRA_ID, -1);
                if (!downloadResult) {
                    toast(R.string.emotion_download_fail);
                    RcsUtils.updateFileDownloadState(context, msgId,
                            RcsUtils.RCS_IS_DOWNLOAD_FAIL);
                } else {
                     RcsUtils.updateFileDownloadState(context, msgId,
                            RcsUtils.RCS_IS_DOWNLOAD_OK);
                }
                mMsgListAdapter.notifyDataSetChanged();
            }
        }
    };

    /**
     *  Pop up a dialog confirming adding the current number to the blacklist
     */
    private void confirmAddBlacklist() {
        //TODO: get the sender number
        final String number = getSenderNumber();
        if (TextUtils.isEmpty(number)) {
            return;
        }
        // Show dialog
        final String message = getString(R.string.add_to_blacklist_message, number);
        new AlertDialog.Builder(this)
                .setTitle(R.string.menuid_add_to_black_list)
                .setMessage(message)
                .setPositiveButton(R.string.alert_dialog_yes,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                  //      BlacklistUtils.addOrUpdate(getApplicationContext(), number,
                   //             BlacklistUtils.BLOCK_MESSAGES, BlacklistUtils.BLOCK_MESSAGES);
                    }
                })
                .setNegativeButton(R.string.alert_dialog_no, null)
                .show();
    }

    private String getSenderNumber() {
        if (isRecipientCallable()) {
            return getRecipients().get(0).getNumber().toString();
        }
        // Not a callable sender
        return null;
    }

    private void vcardContactOrGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ComposeMessageActivity.this);
        builder.setCancelable(true);
        builder.setTitle(R.string.select_contact_conversation);
        builder.setItems(
                new String[] {
                        getContext().getString(R.string.forward_contact),
                        getContext().getString(R.string.forward_contact_group),
                        getContext().getString(R.string.my_vcard)
                }, new SendVcardClickListener());
        builder.show();
    }

    private class SendVcardClickListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            switch (whichButton) {
                case 0:
                    pickContacts(MultiPickContactsActivity.MODE_VCARD,
                            REQUEST_CODE_ATTACH_ADD_CONTACT_VCARD);
                    break;
                case 1:
                    launchRcsContactGroupPicker(REQUEST_CODE_VCARD_GROUP);
                    break;
                case 2:
                    String rawContactId = RcsContactsUtils
                            .getMyRcsRawContactId(ComposeMessageActivity.this);
                    if (TextUtils.isEmpty(rawContactId)) {
                        toast(R.string.please_set_my_profile);
                        return;
                    }
                    Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                            Long.parseLong(rawContactId));
                    String lookup = Uri.encode(Contacts
                            .getLookupUri(getContentResolver(), contactUri).getPathSegments()
                            .get(2));
                    Uri uri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookup);
                    mRcsAttachmentUri = uri;
                    String vcardPath = RcsUtils.createVcardFile(ComposeMessageActivity.this, uri);
                    mWorkingMessage.setVcardPath(vcardPath);
                    mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_VCARD);
                    mWorkingMessage.setIsBurn(mIsBurnMessage);
                    rcsSend();
                    break;
                default:
                    break;
            }
        }
    }

    public void rcsSend() {
        try {
            if (isPreparedForSending() || mIsRcsEnabled) {
                if (RcsDualSimMananger.getUserIsUseRcsPolicy(ComposeMessageActivity.this)) {
                    if (RcsUtils.isRcsOnline()) {
                        if (!RcsDualSimMananger
                                .getDefaultSendingConfirmValue(ComposeMessageActivity.this)) {
                            RcsDualSimMananger
                                    .setDefaultSendingConfirmValue(ComposeMessageActivity.this);
                            showSetRcsPolicyDialog();
                            return;
                        }
                        mWorkingMessage.setIsBurn(mIsBurnMessage);
                        confirmSendMessageIfNeeded();
                    } else {
                        if (!mConversation.isGroupChat()
                                && mWorkingMessage.hasText()
                                && !mWorkingMessage.getCacheRcsMessage()
                                && ((!RcsDualSimMananger.isContentExceedLimit(mWorkingMessage
                                        .getText().toString(), RcsUtils.RCS_MAX_SMS_LENGHTH)))) {
                            confirmSendMessageIfNeeded();
                            return;
                        }
                        toast(R.string.rcs_offline_unable_to_send);
                    }
                } else {
                    if (!mConversation.isGroupChat() && mWorkingMessage.hasText()
                            && !mWorkingMessage.getCacheRcsMessage()) {
                        confirmSendMessageIfNeeded();
                        return;
                    }
                    toast(R.string.rcs_sending_policy_wrong);
                }
            }
        } catch (Exception e) {
            RcsLog.w(e.toString());
        }
    }

    private void forwardRcsMessage(ArrayList<String> numbers) {
        ContactList list = ContactList.getByNumbers(numbers, true);
        long a = -1;
        boolean success = false;
        try {
            MessageItem msgItem = mRcsForwardItems.get(0);
            success = RcsChatMessageUtils.sendRcsForwardMessage(ComposeMessageActivity.this,
                    numbers, null, msgItem.mMsgId);
            if (success) {
                toast(R.string.forward_message_success);
            } else {
                toast(R.string.forward_message_fail);
            }
        } catch (Exception e) {
            toast(R.string.forward_message_fail);
        }
    }

    private void forwardRcsMessage(List<String> numbers) {
        ContactList list = ContactList.getByNumbers(numbers, true);
        long a = -1;
        boolean success = false;
        try {
            MessageItem msgItem = mRcsForwardItems.get(0);
            success = RcsChatMessageUtils.sendRcsForwardMessage(ComposeMessageActivity.this,
                    numbers, null, msgItem.mMsgId);
            if (success) {
                toast(R.string.forward_message_success);
            } else {
                toast(R.string.forward_message_fail);
            }
        } catch (Exception e) {
            toast(R.string.forward_message_fail);
        }
    }

    class AddNumbersTask extends AsyncTask<ArrayList<String>, Void, Void> {
        ProgressDialog mPD;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mPD = new ProgressDialog(ComposeMessageActivity.this);
            mPD.setMessage(getString(R.string.adding_selected_recipients_dialog_text));
            mPD.show();
        }

        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            if (params == null || params.length < 1) {
                return null;
            }
            ArrayList<String> numbers = params[0];
            ContactList list = ContactList.getByNumbers(numbers, true);
            ContactList existing = mRecipientsEditor.constructContactsFromInput(true);
            for (Contact contact : existing) {
                if (!contact.existsInDatabase()) {
                    list.add(contact);
                }
            }
            mRecipientsEditor.populate(list);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mPD != null && mPD.isShowing()) {
                mPD.dismiss();
            }
        }
    }

    public void addGrouChatWayOrConversation(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ComposeMessageActivity.this);
        builder.setCancelable(true);
        builder.setTitle(R.string.select_contact_conversation);
        builder.setItems(new String[] {
                getContext().getString(R.string.forward_contact),
                getContext().getString(R.string.forward_conversation),
        }, listener);
        builder.show();
    }

    private class addGrouChatWayClickListener implements OnClickListener{
        public void onClick(DialogInterface dialog, int whichButton) {
            switch (whichButton) {
                case DIALOG_ADD_RECEIVE_CONTACTS:
                    launchMultiplePhonePicker();
                    break;
                case DIALOG_ADD_RECEIVE_MSG:
                    Intent intent = new Intent(ComposeMessageActivity.this, ConversationList.class);
                    intent.putExtra(MULTI_SELECT_CONV, true);
                    startActivityForResult(intent, REQUEST_CODE_ADD_CONVERSATION);
                    break;
                default:
                    break;
            }
        }
    }

    private void send() {
        if (mShowTwoButtons) {
            confirmSendMessageIfNeeded(PhoneConstants.SUB1);
        } else {
            confirmSendMessageIfNeeded();
        }
    }

    private void showEmojiView(ViewStub emojiViewStub) {
        if (mRcsEmojiInitialize == null) {
            EmojiResources resources = EmojiResources.create(
                    R.id.title,
                    R.id.icon,
                    R.id.text_face,
                    R.id.item,
                    R.drawable.rcs_emoji_button_bg,
                    R.layout.rcs_emoji_grid_view_item,
                    R.id.delete_emoji_btn,
                    R.id.add_emoji_btn,
                    R.id.emoji_grid_view,
                    R.id.content_linear_layout,
                    R.drawable.rcs_emoji_popup_bg);
            mRcsEmojiInitialize = new RcsEmojiInitialize(this, emojiViewStub,
                    mViewOnClickListener, resources);
        }
        mRcsEmojiInitialize.closeOrOpenView();
    }

    private ViewOnClickListener mViewOnClickListener = new ViewOnClickListener() {
        @Override
        public void emojiSelectListener(EmoticonBO emoticonBO) {
            mWorkingMessage.setRcsType(RcsUtils.RCS_MSG_TYPE_PAID_EMO);
            mWorkingMessage.setRcsEmoId(emoticonBO.getEmoticonId());
            mWorkingMessage.setRcsEmoName(emoticonBO.getEmoticonName());
            mWorkingMessage.setIsBurn(mIsBurnMessage);
            mWorkingMessage.setRequiringRcsAttachment(true);
            rcsSend();
        }

        @Override
        public void faceTextSelectListener(String faceText) {
            CharSequence text = mTextEditor.getText() + faceText;
            mTextEditor.setText(text);
            mTextEditor.setSelection(text.length());
        }

        @Override
        public void onEmojiDeleteListener() {
            new Thread() {
                public void run() {
                    try {
                        Instrumentation inst = new Instrumentation();
                        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL);
                    } catch (Exception e) {
                        Log.e("Exception when sendKeyDownUpSync", e.toString());
                    }
                };
            }.start();
        }

        @Override
        public void addEmojiPackageListener() {
            RcsUtils.startEmojiStore(ComposeMessageActivity.this,
                    REQUEST_CODE_EMOJI_STORE);
        }

        @Override
        public void viewOpenOrCloseListener(boolean isOpen) {
            if (isOpen) {
                mButtonEmoj.setImageResource(R.drawable.rcs_emotion_true);
            } else {
                mButtonEmoj.setImageResource(R.drawable.rcs_emotion_false);
            }
        }
    };

    private void launchRcsPhonePicker() {
        Intent intent = new Intent(INTENT_MULTI_PICK_ACTION, Contacts.CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_CODE_RCS_PICK);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.contact_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver mRcsStatusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((Actions.RegisterAction.ACTION_REGISTER_STATUS_CHANGED).equals(action)) {
                int registerCode = intent.getIntExtra(Parameter.EXTRA_CODE, -1);
                RcsLog.i("mRegisterStatusReceiver action=" + action + ",registerCode="
                        + registerCode);
                if (registerCode == Constants.RegisterConstants.CONST_ONLINE) {
                    if (mConversation.getGroupChat() != null) {
                        final long groupId = mConversation.getGroupChat().getId();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mGroupChatApi.rejoin(groupId);
                                } catch (Exception e) {
                                    RcsLog.w(e);
                                }
                            }
                        }).start();
                        RcsLog.i("mRegisterStatusReceiver rejoin groupChatId =" + groupId);
                    }
                }
            } else {
                if (mConversation.isGroupChat()) {
                    String number = intent.getStringExtra(GROUP_PROFILE_CHANGE_NUMBER);
                    GroupMemberPhotoCache.getInstance().removeCache(number);
                }
                mMsgListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

    private GroupChatManagerReceiver mGroupReceiver = new GroupChatManagerReceiver(
            new GroupChatNotifyCallback() {

                @Override
                public void onGroupChatCreate(Bundle extras) {
                    handleRcsGroupChatCreate(extras);
                }

                @Override
                public void onMemberAliasChange(Bundle extras) {
                    if (mMsgListAdapter != null) {
                        mMsgListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onDisband(Bundle extras) {
                    handleRcsGroupChatDeleted(extras);
                }

                @Override
                public void onDeparted(Bundle extras) {
                    handleRcsGroupChatDeparted(extras);
                }

                @Override
                public void onUpdateSubject(Bundle extras) {
                    handleRcsGroupChatUpdateSubject(extras);
                }

                @Override
                public void onUpdateRemark(Bundle extras) {
                    handleRcsGroupChatUpdateRemark(extras);
                }

                @Override
                public void onCreateNotActive(Bundle extras) {
                }

                @Override
                public void onBootMe(Bundle extras) {
                    handleRcsGroupChatBooted(extras);
                }

                @Override
                public void onGroupGone(Bundle extras) {
                    handleRcsGroupChatDeleted(extras);
                }

                @Override
                public void onGroupInviteExpired(Bundle extras) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(R.string.group_invite_has_expired);
                        }
                    });
                }
            });

    private void handleRcsGroupChatDeparted(Bundle extras) {
        try {
            long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
            GroupChat groupChat = mGroupChatApi.getGroupChatById(groupId);
            mConversation.setGroupChat(groupChat);
            updateTitle(new ContactList());
        } catch (ServiceDisconnectedException e) {
            RcsLog.w(e.toString());
        } catch (RemoteException ex) {
            RcsLog.w(ex.toString());
        }
    }

    private void handleRcsGroupChatDeleted(Bundle extras) {
        long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
        if (mConversation != null && mConversation.getGroupChat() != null
                && groupId == mConversation.getGroupChat().getId()) {
            try {
                GroupChat groupChat = mGroupChatApi.getGroupChatById(groupId);
                mConversation.setGroupChat(groupChat);
                runOnUiThread(mResetMessageRunnable);
                updateTitle(new ContactList());
            } catch (ServiceDisconnectedException e) {
                RcsLog.e("Exception onDisband()" + e);
            } catch (RemoteException e) {
                RcsLog.w("Exception" + e);
            }
        }
    }

    private void handleRcsGroupChatCreate(Bundle extras) {
        long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
        String newSubject = extras.getString(Parameter.EXTRA_SUBJECT);
        GroupChat groupChat = mConversation.getGroupChat();
        RcsLog.d("handleRcsGroupChatCreate(): groupId=" + groupId + ", groupChat="
                + groupChat);
        if (groupChat != null) {
            if (groupId > 0 && groupId == groupChat.getId()) {
                RcsLog.d(groupChat.toString());
                groupChat.setStatus(GroupChat.STATUS_STARTED);
                groupChat.setSubject(newSubject);
                mConversation.setGroupChat(groupChat);
                mWorkingMessage.setConversation(mConversation);
                // Reset text editor, receipents editor and update title.
                runOnUiThread(mResetMessageRunnable);
                updateTitle(new ContactList());
                notifyChangeGroupChat(groupId);
                toast(R.string.group_chat_status_ok);
            }
        }
    }

    private void handleRcsGroupChatUpdateSubject(Bundle extras) {
        GroupChat groupChat = mConversation.getGroupChat();
        if (groupChat != null) {
            long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
            if (groupId > 0 && groupId == groupChat.getId()) {
                String newSubject = extras.getString(Parameter.EXTRA_SUBJECT);
                RcsLog.d("update group subject: " + groupChat.getSubject() + " -> "
                        + newSubject);
                groupChat.setSubject(newSubject);
                updateTitle(new ContactList());
            }
        }
    }

    private void handleRcsGroupChatUpdateRemark(Bundle extras) {
        GroupChat groupChat = mConversation.getGroupChat();
        if (groupChat != null) {
            long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
            if (groupId > 0 && groupId == groupChat.getId()) {
                String newRemark = extras.getString(Parameter.EXTRA_REMARK);
                RcsLog.d("update group subject: " + groupChat.getRemark() + " -> "
                        + newRemark);
                groupChat.setRemark(newRemark);
                updateTitle(new ContactList());
            }
        }
    }

    private void handleRcsGroupChatBooted(Bundle extras) {
        GroupChat groupChat = mConversation.getGroupChat();
        if (groupChat != null) {
            long groupId = extras.getLong(Parameter.EXTRA_GROUP_CHAT_ID);
            if (groupId > 0 && groupId == groupChat.getId()) {
                String phoneNumber = extras.getString(Parameter.EXTRA_NUMBER);
                RcsLog.d("phoneNumber: " + groupChat.getStatus() + " -> "
                        + phoneNumber);
                String myPhoneNumber=null;
                try {
                    myPhoneNumber = mBasicApi.getAccount();
                    RcsLog.d("myPhoneNumber: " + groupChat.getStatus() + " -> "
                            + myPhoneNumber);
                } catch (ServiceDisconnectedException e) {
                    RcsLog.d(e.toString());
                } catch (RemoteException e){
                    RcsLog.d(e.toString());
                }
                if (phoneNumber != null && myPhoneNumber.endsWith(phoneNumber)) {
                    RcsLog.d("update group state: " + groupChat.getStatus() + " -> "
                            + GroupChat.STATUS_TERMINATED);
                    groupChat.setStatus(GroupChat.STATUS_TERMINATED);
                    updateTitle(new ContactList());
                }
            }
        }
    }

    private int getBurnAfterReadMessageCount() {
        Cursor cursor = null;
        String where = Conversations.THREAD_ID + " = " + mConversation.getThreadId() + " and "
                + RcsColumns.SmsRcsColumns.RCS_BURN + "!= -1";
        cursor = getContentResolver().query(Sms.CONTENT_URI, new String[] {
                RcsColumns.SmsRcsColumns.RCS_BURN}, where, null, null);
        int burnCount = 0;
        if (cursor != null) {
            try {
                burnCount = cursor.getCount();
            } finally {
                cursor.close();
            }
        }
        return burnCount;
    }
/* End add for RCS */

    private void updateColorPalette(int color) {
        MaterialPalette palette = determinePalette(color);
        updateThemeColors(palette.mPrimaryColor, palette.mSecondaryColor);

        mAccentColor = palette.mPrimaryColor;
        mStatusBarColor = palette.mSecondaryColor;
        mSendContactColor = mAccentColor;
    }

    private void updateThemeColors(int accentColor, int statusBarColor) {
        final int ANIMATION_DURATION = 200;
        final ColorDrawable background = new ColorDrawable();
        final ObjectAnimator backgroundAnimation = ObjectAnimator.ofInt(background,
                "color", mAccentColor, accentColor);
        final ObjectAnimator statusBarAnimation = ObjectAnimator.ofInt(getWindow(),
                "statusBarColor", mStatusBarColor, statusBarColor);

        backgroundAnimation.setEvaluator(new ArgbEvaluator());
        statusBarAnimation.setEvaluator(new ArgbEvaluator());
        findViewById(R.id.header).setBackground(background);

        final AnimatorSet animation = new AnimatorSet();
        animation.playTogether(backgroundAnimation, statusBarAnimation);
        animation.setDuration(isResumed() ? ANIMATION_DURATION : 0);
        animation.start();
     }

    private MaterialPalette determinePalette(int color) {
        final Resources res = ComposeMessageActivity.this.getResources();
        if (color != 0) {
            MaterialColorMapUtils mcmu = new MaterialColorMapUtils(res);
            return mcmu.calculatePrimaryAndSecondaryColor(color);
        }

        return MaterialColorMapUtils.getDefaultPrimaryAndSecondaryColors(res);
    }

    private void setActionBarColor(int color) {
        mActionBarColor = color;
    }

    public static int getSendContactColor() {
        return mSendContactColor;
    }

    /* Following code used to support inline play Audio, while not
    play audio by choosing another application */

    public interface IMMSUpdateProgressBar {
        void update(String time, int pos);
        void reset();
    }

    public interface IMMSAudioPlayer {
        void prepare(String path, ImageView view, int color);
        void setUpdateCallback(IMMSUpdateProgressBar callback);
        void pause();
        void releaseMediaPlayer();
    }

    private IMMSAudioPlayer mMMSAudioPlayer = new MMSAudioPlayer();
    private MediaPlayer mMediaPlayer;
    private boolean mMediaPlayerPrepared = false;
    private IMMSUpdateProgressBar mUpdateCallBack;
    private String mCurPlayUrl;
    private final int POST_DELAY = 100;
    private final int MAX_DURATION = 100;

    private class MMSAudioPlayer implements IMMSAudioPlayer {
        private ImageView mPlayPause;
        private Drawable mPlayPauseDrawable;
        private int mColor;

        /**
         * Check current MediaPlayer status, according which to determine
         * whether need init player or play/pause audio.
         */
        @Override
        public void prepare(String path, ImageView view, int color) {
            ImageView oldPlayerView = mPlayPause;
            mPlayPause = view;
            mColor = color;
            if (path == null) return;
            if (mCurPlayUrl == null) {
                mCurPlayUrl = path;
            }
            if (mMediaPlayerPrepared) {
                if (path.equals(mCurPlayUrl)) {
                    if (mMediaPlayer.isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                } else {
                    mPlayPauseDrawable = getResources().getDrawable(R.drawable.audio_play).mutate();
                    mPlayPauseDrawable.setTint(mColor);
                    if (null != oldPlayerView) {
                        oldPlayerView.setBackground(mPlayPauseDrawable);
                    }
                    releaseMediaPlayer();
                    initMediaPlayer(path);
                }
            } else {
                initMediaPlayer(path);
            }
            mCurPlayUrl = path;
        }

        private void play() {
            mMediaPlayer.start();
            mHandler.post(mUpdateThread);
            mPlayPauseDrawable = getResources().getDrawable(R.drawable.audio_pause).mutate();
            mPlayPauseDrawable.setTint(mColor);
            mPlayPause.setBackground(mPlayPauseDrawable);
        }

        @Override
        public void pause() {
            mMediaPlayer.pause();
            mPlayPauseDrawable = getResources().getDrawable(R.drawable.audio_play).mutate();
            mPlayPauseDrawable.setTint(mColor);
            mPlayPause.setBackground(mPlayPauseDrawable);
        }

        @Override
        public void setUpdateCallback(IMMSUpdateProgressBar callback) {
            mUpdateCallBack = callback;
        }

        private void initMediaPlayer(final String path) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(path));
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mMediaPlayerPrepared = true;
                        play();
                    }
                });
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mUpdateCallBack.reset();
                        mPlayPauseDrawable = getResources().getDrawable(R.drawable.audio_play).mutate();
                        mPlayPauseDrawable.setTint(mColor);
                        mPlayPause.setBackground(mPlayPauseDrawable);
                        mHandler.removeCallbacks(mUpdateThread);
                    }
                });
                mMediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.w(TAG, "init MediaPlayer error:" + e);
            }
        }

        @Override
        public void releaseMediaPlayer() {
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
                mMediaPlayerPrepared = false;
            }
        }
    }

    private Runnable mUpdateThread = new Runnable() {
        int pos;
        int time;

        @Override
        public void run() {
            if (mMediaPlayer == null) {
                mHandler.removeCallbacks(mUpdateThread);
                return;
            }
            pos = mMediaPlayer.getCurrentPosition();
            time = mMediaPlayer.getDuration();
            if (time != 0) {
                mUpdateCallBack.update(MessageUtils.getDisplayTime(pos), pos * MAX_DURATION / time);
            }
            mHandler.postDelayed(mUpdateThread, POST_DELAY);
        }
    };
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String permissions[],
            final int[] grantResults) {
        if (requestCode == SAVE_ATTACHMENT_PERMISSION_REQUEST_CODE) {
            if (MessageUtils.hasStoragePermission()) {
                mModeCallback.saveAttachment();
            } else {
                Toast.makeText(this, R.string.no_permission_save_attachment_to_storage, Toast.LENGTH_SHORT).show();
            }
        }
    }
   private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DEBUG) Log.d(TAG, "PresenceService connected");
            mService = IPresenceService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "PresenceService registerCallback error " + e);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            if (DEBUG) Log.d(TAG, "PresenceService disconnected");
            mService = null;
        }
    };

    private IPresenceServiceCB mCallback = new IPresenceServiceCB.Stub() {

        public void setIMSEnabledCB() {
            if (DEBUG) Log.d(TAG, "PresenceService setIMSEnabled callback");
        }
    };

    private void bindService(Context context) {
        if (DEBUG) Log.d(TAG, "PresenceService BindService");
        Intent intent = new Intent(IPresenceService.class.getName());
        intent.setClassName(PRESENCESERV, PRESENCESERV_PRESENCESERVICE);
        mIsBound = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService(Context context) {
        if (DEBUG) Log.d(TAG, "PresenceService unbindService");
        if (mService != null) {
            try {
                mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "PresenceService unregister error " + e);
            }
        }
        if (mIsBound) {
            if (DEBUG) Log.d(TAG, "PresenceService unbind");
            context.unbindService(mConnection);
            mIsBound = false;
        }
    }

    private boolean startAvailabilityFetch(String number){
        if (DEBUG) Log.d(TAG, "startAvailabilityFetch   number " + number);
        if (null != mService) {
            try {
                boolean vt = false;
                vt = mService.invokeAvailabilityFetch(number);
                return vt;
            } catch (Exception e) {
                Log.d(TAG, "getVTCapOfContact ERROR " + e);
            }
        }
        return false;
    }

    private boolean getVTCapability(String number) {
        if (DEBUG) Log.d(TAG, "getVTCapability   number " + number);
        if (null != mService) {
            try {
                boolean vt = false;
                vt = mService.hasVTCapability(number);
                if (DEBUG) Log.d(TAG,
                    "getVTCapability success number " + number + " " + vt);
                return vt;
            } catch (Exception e) {
                Log.d(TAG, "getVTCapability ERROR " + e);
            }
        }
        return false;
    }

    private class UpdateAttachmentInfo {
        private static final String SUB_TAG = "UpdateAttachmentInfo";
        private Context mContext;
        private boolean mNewQuery;
        private boolean mIsRunning;
        private final long mThreadId;

        public UpdateAttachmentInfo(Context context, long thread) {
            mContext = context;
            mThreadId = thread;
            mNewQuery = false;
            mIsRunning = false;
        }

        public void requestUpdate() {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.d(TAG, SUB_TAG + " requestUpdate mIsRunning=" + mIsRunning);
            }
            mNewQuery = true;
            if (!mIsRunning) {
                handleUpdate();
            }
        }

        private void handleUpdate() {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        Log.d(TAG, SUB_TAG + " handleUpdate run");
                    }
                    mIsRunning = true;
                    mNewQuery = false;
                    MessageUtils.updateThreadAttachTypeByThreadId(mContext, mThreadId);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        Log.d(TAG, SUB_TAG + " onPostExecute mNewQuery=" + mNewQuery);
                    }
                    mIsRunning = false;
                    if (mNewQuery) {
                        handleUpdate();
                    }
                }
            }.execute((Void) null);
        }
    }
}
