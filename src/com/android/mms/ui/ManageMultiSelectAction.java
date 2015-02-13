/*<!-- Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 *Redistribution and use in source and binary forms, with or without
 *modification, are permitted provided that the following conditions are
 *met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 *THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * -->*/

package com.android.mms.ui;

import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.android.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.android.mms.ui.MessageListAdapter.COLUMN_SMS_LOCKED;
import static com.android.mms.ui.MessageListAdapter.FORWARD_PROJECTION;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Conversations;
import android.util.SparseBooleanArray;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.data.Contact;
import com.android.mms.R;
import com.android.mms.rcs.RcsApiManager;
import com.android.mms.rcs.RcsUtils;
import com.android.mms.ui.PopupList;
import com.android.mms.ui.SelectionMenu;

import com.suntek.mway.rcs.client.api.im.impl.MessageApi;
import com.suntek.mway.rcs.client.aidl.provider.model.SimpleMsg;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;

import java.util.ArrayList;

/**
 * Displays a list of the SMS messages with checkbox and support multi select action.
 */

public class ManageMultiSelectAction extends Activity {
    private static final String TAG = "ManageMultiSelectAction";

    private Cursor mCursor;
    private ListView mMsgListView;
    private TextView mMessage;
    private Button mActionButton;
    private SelectionMenu mSelectionMenu;
    private MessageListAdapter mMsgListAdapter;
    private ContentResolver mContentResolver;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private boolean mHasSelectAll = false;
    private int mSubscription; // add for DSDS
    private Uri mIccUri;
    private ProgressDialog mProgressDialog = null;
    private OperateThread mOperateThread = null;
    ArrayList<Cursor> mSelectedCursors = new ArrayList<Cursor>();
    ArrayList<String> mSelectedUris = new ArrayList<String>();
    ArrayList<String> mSelectedLockedUris = new ArrayList<String>();
    ArrayList<MessageItem> mMessageItems = new ArrayList<MessageItem>();
    ArrayList<SimpleMsg> mSimpleMsgs = new ArrayList<SimpleMsg>();
    private MessageApi mMessageApi;

    private int mManageMode;
    private long mThreadId;
    private static final int SUB_INVALID = -1;
    private static final int INVALID_THREAD = -1;
    private static final int SHOW_TOAST = 1;
    private static final int FOWARD_DONE = 2;

    private static boolean mIsDeleteLockChecked = false;
    private static final int MESSAGE_LOCKED = 1;

    private static final int DELAY_TIME = 500;
    private static final String SORT_ORDER = "date ASC";
    private static final String MESSAGE_CONTENT = "sms_body";
    // add for merge messages
    private static final String COLON = ":";
    private static final String LEFT_PARENTHESES = "(";
    private static final String RIGHT_PARENTHESES = ")";
    private static final String LINE_BREAK = "\n";

    private ProgressDialog mSaveOrBackProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sim_list);

        Intent intent = getIntent();
        mManageMode = intent.getIntExtra(ComposeMessageActivity.MANAGE_MODE,
                MessageUtils.INVALID_MODE);
        if (mManageMode == MessageUtils.FORWARD_MODE
                || mManageMode == MessageUtils.BATCH_DELETE_MODE) {
            mThreadId = intent.getLongExtra(ComposeMessageActivity.THREAD_ID, INVALID_THREAD);
        } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE){
            mSubscription = intent.getIntExtra(MessageUtils.SUB_KEY, SUB_INVALID);
            mIccUri = MessageUtils.getIccUriBySubscription(mSubscription);
        } else if (mManageMode == MessageUtils.BATCH_FAVOURITE_MODE
                || mManageMode == MessageUtils.BATCH_BACKUP_MODE) {
            mThreadId = intent.getLongExtra(ComposeMessageActivity.THREAD_ID, INVALID_THREAD);
        }

        mMsgListView = (ListView) findViewById(R.id.messages);
        mMessage = (TextView) findViewById(R.id.empty_message);

        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mMessageApi = RcsApiManager.getMessageApi();

        startMsgListQuery();

    }

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_TOAST: {
                    String toastStr = (String) msg.obj;
                    Toast.makeText(ManageMultiSelectAction.this, toastStr,
                            Toast.LENGTH_SHORT).show();
                    clearSelect();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    setResult(RESULT_OK);
                    finish();
                    break;
                }
                case FOWARD_DONE: {
                    clearSelect();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    finish();
                    break;
                }
                default:
                    break;
            }
        }
    };

    final Runnable mShowProgress = new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.show();
                }
            }
        };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            mCursor.close();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return true;
        }
        return true;
    }

    private void confirmMultiAction() {
        SparseBooleanArray booleanArray = mMsgListView.getCheckedItemPositions();
        int size = booleanArray.size();

        mSelectedLockedUris.clear();
        mSelectedUris.clear();

        if (size > 0) {
            for (int j = 0; j < size; j++) {
                int position = booleanArray.keyAt(j);
                if (!mMsgListView.isItemChecked(position)) {
                    continue;
                }
                Cursor c = (Cursor) mMsgListAdapter.getItem(position);
                if (c == null) {
                    return;
                }
                mSelectedCursors.add(c);
                if (mManageMode == MessageUtils.FORWARD_MODE
                        || mManageMode == MessageUtils.BATCH_BACKUP_MODE) {
                    String type = c.getString(COLUMN_MSG_TYPE);
                    long msgId = c.getLong(COLUMN_ID);
                    mMessageItems.add(mMsgListAdapter.getCachedMessageItem(type, msgId, c));
                    try {
                        MessageItem mi = mMsgListAdapter.getCachedMessageItem(type, msgId, c);
                        String rowId = String.valueOf(mi.mRcsId);
                        SimpleMsg sm = new SimpleMsg();
                        if (mi.mRcsId == RcsUtils.SMS_DEFAULT_RCS_ID && mi.isSms()) {
                            sm.setStoreType(SuntekMessageData.STORE_TYPE_SMS);
                            sm.setRowId(String.valueOf(msgId));
                            sm.setMessageId(rowId);
                        }else if (mi.isMms()) {
                            sm.setStoreType(SuntekMessageData.STORE_TYPE_MMS);
                            sm.setRowId(String.valueOf(msgId));
                            sm.setMessageId(rowId);
                        }else if (mi.mRcsId != RcsUtils.SMS_DEFAULT_RCS_ID && mi.isSms()) {
                            sm.setRowId(rowId);
                            sm.setMessageId(String.valueOf(msgId));
                            sm.setStoreType(SuntekMessageData.STORE_TYPE_NEW_MSG);
                        }
                        mSimpleMsgs.add(sm);
                        Log.i("RCS_UI","rowId ->" + rowId + " messageId ->" + msgId + " type ->"+ sm.getStoreType());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE) {
                    mSelectedUris.add(getUriStrByCursor(c));
                } else if (mManageMode == MessageUtils.BATCH_DELETE_MODE
                        || mManageMode == MessageUtils.BATCH_FAVOURITE_MODE) {
                    long msgId = c.getLong(COLUMN_ID);
                    Uri uri = c.getString(COLUMN_MSG_TYPE).equals("sms")
                            ? ContentUris.withAppendedId(Sms.CONTENT_URI, msgId)
                            : ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);
                    if (c.getInt(COLUMN_SMS_LOCKED) == MESSAGE_LOCKED) {
                        mSelectedLockedUris.add(uri.toString());
                    }
                    mSelectedUris.add(uri.toString());
                }
            }
        }

        if (mManageMode == MessageUtils.FORWARD_MODE) {
            forwardSmsConversation();
        } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE
                || mManageMode == MessageUtils.BATCH_DELETE_MODE) {
            MultiMessagesListener l = new MultiMessagesListener();
            confirmDeleteDialog(l);
        } else if (mManageMode == MessageUtils.BATCH_FAVOURITE_MODE) {
            favouriteMessage();
        } else if (mManageMode == MessageUtils.BATCH_BACKUP_MODE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.suntek.mway.rcs.BACKUP_ALL_MESSAGE");
            registerReceiver(mBackupStateReceiver, filter);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActionButton.setEnabled(false);
                }
            });
            backupMessage();
        }
    }

    private void forwardSmsConversation() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        //mProgressDialog.setTitle(R.string.merging_sms_title);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mUiHandler.postDelayed(mShowProgress, DELAY_TIME);
        if (mOperateThread == null) {
            mOperateThread = new OperateThread();
        }
        Thread thread = new Thread(mOperateThread);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void mergeMessagesAndForward() {
        StringBuilder forwardContent = new StringBuilder();
        if (mMessageItems.size() > 1) {
            for (MessageItem msgItem : mMessageItems) {
                if (Sms.isOutgoingFolder(msgItem.mBoxId)) {
                    forwardContent.append(msgItem.mContact + COLON + LINE_BREAK);
                } else {
                    if (Contact.get(msgItem.mAddress, false).existsInDatabase()) {
                        forwardContent.append(msgItem.mContact + LEFT_PARENTHESES +
                                msgItem.mAddress + RIGHT_PARENTHESES + COLON + LINE_BREAK);
                    } else {
                        forwardContent.append(msgItem.mAddress + COLON + LINE_BREAK);
                    }
                }
                forwardContent.append(msgItem.mBody + LINE_BREAK + msgItem.mTimestamp);
                forwardContent.append(LINE_BREAK);
            }
        } else if (mMessageItems.size() == 1) {
            // we don't add the recipient's information if only forward one sms.
            forwardContent.append(mMessageItems.get(0).mBody + LINE_BREAK
                    + mMessageItems.get(0).mTimestamp);
        }

        forwardMessage(forwardContent.toString());
    }

    private void forwardMessage(String forwardContent) {
        Intent intent = new Intent(this, ComposeMessageActivity.class);
        intent.putExtra(ComposeMessageActivity.KEY_EXIT_ON_SENT, true);
        intent.putExtra(ComposeMessageActivity.KEY_FORWARDED_MESSAGE, true);
        intent.putExtra(MESSAGE_CONTENT, forwardContent);
        startActivity(intent);

        Message msg = Message.obtain();
        msg.what = FOWARD_DONE;
        mUiHandler.sendMessage(msg);
    }

    private class MultiMessagesListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton) {
            deleteSelectedMessages();
        }
    }

    private void deleteSelectedMessages() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(R.string.deleting_title);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.show();
        if (mOperateThread == null) {
            mOperateThread = new OperateThread();
        }
        Thread thread = new Thread(mOperateThread);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void deleteMessages() {
        for (String uri : mSelectedUris) {
            if (mIsDeleteLockChecked || !mSelectedLockedUris.contains(uri)) {
                SqliteWrapper.delete(ManageMultiSelectAction.this, mContentResolver,
                        Uri.parse(uri), null, null);
            }
        }

        Message msg = Message.obtain();
        msg.what = SHOW_TOAST;
        msg.obj = getString(R.string.operate_success);
        mUiHandler.sendMessage(msg);
    }

    private String getUriStrByCursor(Cursor cursor) {
        String messageIndexString =
                cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        Uri simUri = mIccUri.buildUpon().appendPath(messageIndexString).build();

        return simUri.toString();
    }

    private void confirmDeleteDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View contents = View.inflate(this, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);
        msg.setText(R.string.confirm_delete_selected_messages);

        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_locked);

        if (mSelectedLockedUris.size() == 0) {
            checkbox.setVisibility(View.GONE);
        } else {
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mIsDeleteLockChecked = checkbox.isChecked();
                }
            });
        }
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.setView(contents);
        builder.show();
    }

    private void startMsgListQuery() {
        try {
            mMsgListView.setVisibility(View.GONE);
            mMessage.setVisibility(View.GONE);
            setTitle(getString(R.string.refreshing));
            setProgressBarIndeterminateVisibility(true);
            if (mManageMode == MessageUtils.FORWARD_MODE) {
                mBackgroundQueryHandler.startQuery(0, null, Sms.CONTENT_URI,
                        FORWARD_PROJECTION, Conversations.THREAD_ID + "=?",
                        new String[]{String.valueOf(mThreadId)}, SORT_ORDER);
            } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE) {
                mBackgroundQueryHandler.startQuery(0, null, mIccUri, null, null, null, null);
            } else if (mManageMode == MessageUtils.BATCH_FAVOURITE_MODE
                    || mManageMode == MessageUtils.BATCH_BACKUP_MODE) {
                Uri uri = ContentUris.withAppendedId(Threads.CONTENT_URI, mThreadId);
                mBackgroundQueryHandler.startQuery(0, null, uri,
                        MessageListAdapter.PROJECTION, "rcs_burn_flag != 1", null, null);
            } else if (mManageMode == MessageUtils.BATCH_DELETE_MODE){
                Uri uri = ContentUris.withAppendedId(Threads.CONTENT_URI, mThreadId);
                mBackgroundQueryHandler.startQuery(0, null, uri,
                        MessageListAdapter.PROJECTION, null, null, null);
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                    ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE |
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setTitle("");
            actionBar.setCustomView(R.layout.conversation_list_multi_actionbar);
            mActionButton = (Button) findViewById(R.id.multi_action_done);
            if (mManageMode == MessageUtils.FORWARD_MODE) {
                mActionButton.setText(R.string.menu_forward);
            } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE) {
                mActionButton.setText(R.string.done_delete);
            } else if (mManageMode == MessageUtils.BATCH_DELETE_MODE) {
                mActionButton.setText(R.string.menu_batch_delete);
            } else if (mManageMode == MessageUtils.BATCH_FAVOURITE_MODE) {
                mActionButton.setText(R.string.batch_favourite);
            } else if (mManageMode == MessageUtils.BATCH_BACKUP_MODE) {
                mActionButton.setText(R.string.batch_backup);
            }
            mActionButton.setEnabled(false);
            mActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmMultiAction();
                }
            });
            Button mSelectionMenuButton = (Button) findViewById(R.id.select_menu);
            mSelectionMenu = new SelectionMenu(this, mSelectionMenuButton,
                    new PopupList.OnPopupItemClickListener() {
                        @Override
                        public boolean onPopupItemClick(int itemId) {
                            if (itemId == SelectionMenu.SELECT_OR_DESELECT) {
                                if (mHasSelectAll) {
                                    clearSelect();
                                    mHasSelectAll = false;
                                } else {
                                    allSelect();
                                    mHasSelectAll = true;
                                }
                                mSelectionMenu.updateSelectAllMode(mHasSelectAll);
                            }
                            return true;
                        }
                    });
            mSelectionMenu.setTitle(getString(R.string.selected_count, 0));
        }
    }

    private void clearSelect() {
        mMsgListView.clearChoices();
        final int checkedCount = mMsgListView.getCheckedItemCount();
        mSelectionMenu.setTitle(getString(R.string.selected_count, checkedCount));
        if (checkedCount == 0) {
            mActionButton.setEnabled(false);
        }
        mMsgListView.invalidateViews();
    }

    private void allSelect() {
        int count = mMsgListAdapter.getCount();
        for (int i = 0; i < count; i++) {
            mMsgListView.setItemChecked(i, true);
        }
        final int checkedCount = mMsgListView.getCheckedItemCount();
        mSelectionMenu.setTitle(getString(R.string.selected_count, checkedCount));
        if (checkedCount > 0) {
            mActionButton.setEnabled(true);
        }
        mMsgListView.invalidateViews();
    }

    private class OperateThread extends Thread {
        public OperateThread() {
            super("OperateThread");
        }

        public void run() {
            if (mManageMode == MessageUtils.FORWARD_MODE) {
                mergeMessagesAndForward();
            } else if (mManageMode == MessageUtils.SIM_MESSAGE_MODE
                    || mManageMode == MessageUtils.BATCH_DELETE_MODE) {
                deleteMessages();
            }
        }
    }

    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            setProgressBarVisibility(false);
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = cursor;

            if (mCursor != null) {
                if (!mCursor.moveToFirst()) {
                    finish();
                } else if (mMsgListAdapter == null) {
                    setupActionBar();
                    mMsgListAdapter = new MessageListAdapter(
                            ManageMultiSelectAction.this, mCursor, mMsgListView, false, null);
                    mMsgListAdapter.setMultiChoiceMode(true);
                    mMsgListAdapter.setMultiManageMode(mManageMode);
                    mMsgListAdapter.setMultiManageClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            updateSelectState(v);
                            updateSelectMenu();
                        }
                    });
                    mMsgListView.setAdapter(mMsgListAdapter);
                    mMsgListView.setDivider(null);
                    mMsgListView.setVisibility(View.VISIBLE);
                    mMessage.setVisibility(View.GONE);
                    mMsgListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    setProgressBarIndeterminateVisibility(false);
                } else {
                    mMsgListAdapter.changeCursor(mCursor);
                }
            } else {
                finish();
            }
            return;
        }
    }

    private void favouriteMessage() {

        for (int i = 0; i < mSelectedUris.size(); i++) {
            final Uri lockUri = Uri.parse(mSelectedUris.get(i));
            final ContentValues values = new ContentValues(1);
            values.put("favourite", true ? 1 : 0);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getContentResolver().update(lockUri, values, null, null);
                }
            }, "ManageMultiSelectActivity.favourite").start();

        }
        Message msg = Message.obtain();
        msg.what = SHOW_TOAST;
        msg.obj = getString(R.string.operate_success);
        mUiHandler.sendMessage(msg);

    }
    
    private void backupMessage() {

        try {
            mMessageApi.backupMessageList(mSimpleMsgs);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void updateSelectState(View listItemChild) {
        int clickPosition = getMessageListItem(listItemChild).getItemPosition();
        mMsgListView.setItemChecked(
                clickPosition, !(mMsgListView.isItemChecked(clickPosition)));
        mMsgListView.invalidateViews();
    }

    private MessageListItem getMessageListItem(View listItemChild) {
        View parent = (View) listItemChild.getParent();
        while (!(parent instanceof MessageListItem)) {
            parent = (View) parent.getParent();
        }
        return (MessageListItem) parent;
    }

    private void updateSelectMenu() {
        final int checkedCount = mMsgListView.getCheckedItemCount();
        mSelectionMenu.setTitle(getString(R.string.selected_count, checkedCount));
        int count = mMsgListAdapter.getCount();
        if (checkedCount == count) {
            mHasSelectAll = true;
        } else {
            mHasSelectAll = false;
        }
        mSelectionMenu.updateSelectAllMode(mHasSelectAll);
        if (checkedCount == 0) {
            mActionButton.setEnabled(false);
        } else {
            mActionButton.setEnabled(true);
        }
    }

    private final BroadcastReceiver mBackupStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.suntek.mway.rcs.BACKUP_ALL_MESSAGE".equals(action)) {
                int progress = intent.getIntExtra("progress", 0);
                int total = intent.getIntExtra("total", 0);
                int status = intent.getIntExtra("status", 0);
                Log.i("RCS_UI", "progress = " + progress + " total = " + total + " status = "
                        + status);
                switch (status) {
                    case 0:
                        showProgressDialog(ManageMultiSelectAction.this, 0,
                                context.getString(R.string.message_is_begin), total);
                        if (mSaveOrBackProgressDialog != null && !mSaveOrBackProgressDialog.isShowing()) {
                            mSaveOrBackProgressDialog.show();
                        }
                        break;
                    case 1:
                        if (total == 0) {
                            return;
                        }
                        showProgressDialog(ManageMultiSelectAction.this, progress,
                                context.getString(R.string.message_is_saving), total);
                        if (mSaveOrBackProgressDialog != null && !mSaveOrBackProgressDialog.isShowing()) {
                            mSaveOrBackProgressDialog.show();
                        }
                        break;
                    case 2:
                        if (mSaveOrBackProgressDialog != null) {
                            mSaveOrBackProgressDialog.dismiss();
                        }
                        // Toast.makeText(ManageMultiSelectAction.this,
                        // R.string.message_save_ok, 0).show();
                        clearSelect();
                        mSimpleMsgs.clear();
                        mSaveOrBackProgressDialog = null;
                        mActionButton.setEnabled(true);
                        unregisterReceiver(mBackupStateReceiver);
                        Toast.makeText(context, R.string.message_save_ok, Toast.LENGTH_SHORT).show();
                        break;
                    case -1:
                        if (mSaveOrBackProgressDialog != null) {
                            mSaveOrBackProgressDialog.dismiss();
                        }
                        clearSelect();
                        mSimpleMsgs.clear();
                        mSaveOrBackProgressDialog = null;
                        mActionButton.setEnabled(true);
                        unregisterReceiver(mBackupStateReceiver);
                        Toast.makeText(context, R.string.message_save_fail, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void showProgressDialog(Context context,int progress,String title,int total) {
        if (mSaveOrBackProgressDialog == null) {
            mSaveOrBackProgressDialog = new ProgressDialog(context);
            mSaveOrBackProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mSaveOrBackProgressDialog.setMessage(title);
            mSaveOrBackProgressDialog.setCancelable(false);
            mSaveOrBackProgressDialog.setCanceledOnTouchOutside(false);
            mSaveOrBackProgressDialog.setButton(
                    context.getResources().getString(R.string.cacel_back_message),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                RcsApiManager.getMessageApi().cancelBackup();
                                mSaveOrBackProgressDialog.cancel();
                                clearSelect();
                                mSimpleMsgs.clear();
                                mActionButton.setEnabled(true);
                            } catch (Exception e) {
                                Log.w("RCS_UI", e);
                            } finally {
                                unregisterReceiver(mBackupStateReceiver);
                            }
                        }
                    });
            mSaveOrBackProgressDialog.show();
            mSaveOrBackProgressDialog.setMax(total);
            mSaveOrBackProgressDialog.setProgress(progress);
        } else {
            mSaveOrBackProgressDialog.setMessage(title);
            mSaveOrBackProgressDialog.setMax(total);
            mSaveOrBackProgressDialog.setProgress(progress);
        }
    }
}
