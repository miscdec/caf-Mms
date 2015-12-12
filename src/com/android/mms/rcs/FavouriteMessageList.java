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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.mms.data.Contact;
import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.rcs.FavoriteMessageListAdapter.OnListContentChangedListener;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.ui.AsyncDialog;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.ConversationList;
import com.android.mms.ui.MessageListAdapter;
import com.android.mms.ui.MessagingPreferenceActivity;
import com.android.mms.ui.MessageUtils;
import com.android.mms.ui.PopupList;
import com.android.mms.ui.SearchActivity;
import com.android.mms.util.DownloadManager;
import com.google.android.mms.pdu.PduHeaders;
import com.suntek.mway.rcs.client.aidl.constant.Constants;
import com.suntek.mway.rcs.client.aidl.constant.Constants.FavoriteMessageProvider;
import com.suntek.mway.rcs.client.aidl.service.entity.SimpleMessage;
import com.suntek.mway.rcs.client.api.message.MessageApi;
import com.suntek.mway.rcs.client.api.exception.ServiceDisconnectedException;

import java.util.ArrayList;

/**
 * This activity provides a list view of MailBox-Mode.
 */
public class FavouriteMessageList extends ListActivity implements
        FavoriteMessageListAdapter.OnListContentChangedListener{

    private static final String TAG = "FavouriteMessageList";

    private static final Uri FAVORITE_URI = FavoriteMessageProvider.CONST_FAVOURITE_MESSAGE_URI;
    private static final int MESSAGE_LIST_QUERY_TOKEN = 9001;

    private final static String THREAD_ID = "thread_id";
    private final static String MESSAGE_ID = "message_id";
    private final static String MESSAGE_TYPE = "message_type";
    private final static String MESSAGE_BODY = "message_body";
    private final static String MESSAGE_SUBJECT = "message_subject";
    private final static String MESSAGE_SUBJECT_CHARSET = "message_subject_charset";
    private final static String NEED_RESEND = "needResend";
    private final static int DELAY_TIME = 500;

    private boolean mIsPause = false;
    private boolean mQueryDone = true;
    private BoxMsgListQueryHandler mQueryHandler;
    private String mSmsWhereDelete = "";
    private String mMmsWhereDelete = "";

    private FavoriteMessageListAdapter mListAdapter = null;
    private Cursor mCursor;
    private final Object mCursorLock = new Object();
    private ListView mListView;
    private TextView mCountTextView;
    private TextView mMessageTitle;
    private ModeCallback mModeCallback = null;
    // mark whether comes into MultiChoiceMode or not.
    private boolean mMultiChoiceMode = false;
    private MenuItem mSearchItem;


    // add for obtain parameters from SearchActivityExtend
    private String mSearchDisplayStr = "";
    private int mMatchWhole = MessageUtils.MATCH_BY_ADDRESS;
    private boolean isChangeToConvasationMode = false;
    private static final String LUNCH_BACKUP_RESTORE_ACTIVITY =
            "com.suntek.mway.rcs.ACTION_LUNCHER_BACKUP_RESOTORE_ALL_ACTIVITY";
    private static final String BACKUP_RESTORE_FAVORITEMESSAGE = "isFavoriteMessage";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQueryHandler = new BoxMsgListQueryHandler(getContentResolver());
        setContentView(R.layout.mailbox_list_screen);
        View spinners = (View) findViewById(R.id.spinners);
      //  View toolsBar = (View) findViewById(R.id.toolbar);
       // toolsBar.setVisibility(View.GONE);
        spinners.setVisibility(View.GONE);

        mListView = getListView();
        getListView().setItemsCanFocus(true);
        mModeCallback = new ModeCallback();
  /*      View actionButton = findViewById(R.id.floating_action_button);
        if (actionButton != null) {
            actionButton.setVisibility(View.GONE);
        }*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public boolean onSearchRequested() {
        if (getResources().getBoolean(R.bool.config_classify_search)) {
            // block search entirely (by simply returning false).
            return false;
        }

        // if comes into multiChoiceMode,do not continue to enter search mode ;
        if (mSearchItem != null && !mMultiChoiceMode) {
            mSearchItem.expandActionView();
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml). Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.
        closeContextMenu();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (!mQueryDone) {
            return;
        }
        Cursor c = (Cursor) l.getAdapter().getItem(position);
        if (c == null) {
            return;
        }
        Intent i = new Intent(FavouriteMessageList.this, FavoriteDetailActivity.class);
        i.setData(FAVORITE_URI);
        i.putExtra(MESSAGE_ID, c.getLong(
                c.getColumnIndex(FavoriteMessageProvider.FavoriteMessage.MSG_ID)));
        FavouriteMessageList.this.startActivity(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        setupActionBar();
        actionBar.setTitle(getString(R.string.my_favorited));
        mIsPause = false;
        startAsyncQuery();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(mModeCallback);

        getListView().invalidateViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsPause = true;
    }

    private void startAsyncQuery() {
        try {
            synchronized (mCursorLock) {
                setProgressBarIndeterminateVisibility(true);
                mQueryDone = false;
                mQueryHandler.startQuery(MESSAGE_LIST_QUERY_TOKEN, 0,
                        FAVORITE_URI,
                        null, null, null, "date DESC");
            }
        } catch (SQLiteException e) {
            mQueryDone = true;
            SqliteWrapper.checkSQLiteException(this, e);
            mListView.setVisibility(View.VISIBLE);
        }
    }


    private final class BoxMsgListQueryHandler extends AsyncQueryHandler {
        public BoxMsgListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            synchronized (mCursorLock) {
                if (cursor != null) {
                    if (mCursor != null) {
                        mCursor.close();
                    }
                    mCursor = cursor;
                    TextView emptyView = (TextView) findViewById(R.id.emptyview);
                    mListView.setEmptyView(emptyView);
                    if (mListAdapter == null) {
                        mListAdapter = new FavoriteMessageListAdapter(FavouriteMessageList.this,
                                FavouriteMessageList.this, cursor);
                        invalidateOptionsMenu();
                        FavouriteMessageList.this.setListAdapter(mListAdapter);
                        int count = cursor.getCount();
                        if (count == 0) {
                            emptyView.setText(getString(R.string.search_empty));
                         } else if (cursor.getCount() == 0) {
                             mListView.setEmptyView(emptyView);
                         }
                     } else {
                        mListAdapter.changeCursor(mCursor);
                        mCountTextView.setVisibility(View.GONE);
                    }
                } else {
                    if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                        Log.e(TAG, "Cannot init the cursor for the thread list.");
                    }
                    finish();
                }
            }
            setProgressBarIndeterminateVisibility(false);
            mQueryDone = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.fav_conversation_multi_select_menu, menu);
        MenuItem unFavItem = menu.findItem(R.id.delete);
        if (unFavItem != null) {
            unFavItem.setVisible(false);
        }
        MenuItem backupRestoreItem = menu.findItem(R.id.backup_restore_favorite_message);
        if (backupRestoreItem != null) {
            backupRestoreItem.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.backup_restore_favorite_message:
                Intent backupRestoreIntent = new Intent();
                backupRestoreIntent.setAction(LUNCH_BACKUP_RESTORE_ACTIVITY);
                backupRestoreIntent.putExtra(BACKUP_RESTORE_FAVORITEMESSAGE, true);
                startActivity(backupRestoreIntent);
                break;

            default:
                break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mIsPause = true;
        if (mCursor != null) {
            mCursor.close();
        }
        if (mListAdapter != null) {
            mListAdapter.changeCursor(null);
        }
        MessageUtils.removeDialogs();
    }

    private void confirmDeleteMessages() {
        UnFavoriteMessagesListener l = new UnFavoriteMessagesListener();
        confirmUnfavoriteDialog(l);
    }

    private class UnFavoriteMessagesListener implements OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int whichButton) {
            cancelFavouriteMessages();
        }
    }

    private void confirmUnfavoriteDialog(final UnFavoriteMessagesListener listener) {
        View contents = View.inflate(this, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(getString(R.string.confirm_cancel_selected_fav_messages));
        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        checkbox.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_cancel_selected_fav_messages);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setCancelable(true);
        builder.setView(contents);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void cancelFavouriteMessages() {
        int count = mListAdapter.getCount();
        SparseBooleanArray booleanArray = mListView.getCheckedItemPositions();
        int size = booleanArray.size();
        ArrayList<SimpleMessage> simpleMsgs = new ArrayList<SimpleMessage>();
        if (count == 0 || size == 0) {
            return;
        }
        for (int j = 0; j < size; j++) {
            int position = booleanArray.keyAt(j);

            if (!mListView.isItemChecked(position)) {
                continue;
            }
            Cursor c = (Cursor) mListAdapter.getItem(position);
            if (c == null) {
                return;
            }
            SimpleMessage sm = new SimpleMessage();
            long msgId = c.getLong(c.getColumnIndex(
                    FavoriteMessageProvider.FavoriteMessage.MSG_ID));
            int chatType = c.getInt(c.getColumnIndex(
                    FavoriteMessageProvider.FavoriteMessage.CHAT_TYPE));
            sm.setMessageRowId(msgId);
            if (!RcsUtils.isRcsMessage(chatType)) {
                sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_SMS);
            } else {
                sm.setStoreType(Constants.MessageConstants.CONST_STORE_TYPE_IM);
            }
            simpleMsgs.add(sm);

        }
        if (mModeCallback != null) {
            mModeCallback.setModeFinish();
        }
        try {
            MessageApi.getInstance().cancelCollect(simpleMsgs);
        } catch (ServiceDisconnectedException e) {
            e.printStackTrace();
        } catch (RemoteException ex){
            ex.printStackTrace();
        }
        mListAdapter.notifyDataSetChanged();
    }

    public void onListContentChanged() {
        if (!mIsPause) {
            startAsyncQuery();
        }
    }

    public void checkAll() {
        int count = getListView().getCount();
        for (int i = 0; i < count; i++) {
            getListView().setItemChecked(i, true);
        }
        mListAdapter.notifyDataSetChanged();
    }

    public void unCheckAll() {
        int count = getListView().getCount();
        for (int i = 0; i < count; i++) {
            getListView().setItemChecked(i, false);
        }
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.mailbox_list_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v, new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        mCountTextView = (TextView) v.findViewById(R.id.message_count);
        mMessageTitle = (TextView) v.findViewById(R.id.message_title);
        mCountTextView.setVisibility(View.GONE);
        mMessageTitle.setVisibility(View.GONE);
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private ImageView mSelectedAll;
        //used in MultiChoiceMode
        private boolean mHasSelectAll = false;
        private RcsSelectionMenu mSelectionMenu;
        private ActionMode mMode = null;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // comes into MultiChoiceMode
            mMultiChoiceMode = true;
            MenuInflater inflater = getMenuInflater();
            mMode = mode;
            inflater.inflate(R.menu.fav_conversation_multi_select_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = (ViewGroup) LayoutInflater
                        .from(FavouriteMessageList.this).inflate(R.layout.action_mode, null);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            mSelectionMenu = new RcsSelectionMenu(getApplicationContext(),
                    (Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu),
                    new PopupList.OnPopupItemClickListener() {
                        @Override
                        public boolean onPopupItemClick(int itemId) {
                            if (itemId == RcsSelectionMenu.SELECT_OR_DESELECT) {
                                if (mHasSelectAll) {
                                    unCheckAll();
                                    mHasSelectAll = false;
                                } else {
                                    checkAll();
                                    mHasSelectAll = true;
                                }
                                mSelectionMenu.updateSelectAllMode(mHasSelectAll);
                            }
                            return true;
                        }
                    });
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mSelectionMenu != null) {
                mSelectionMenu.setTitle(getApplicationContext().getString(R.string.selected_count,
                        getListView().getCheckedItemCount()));
            }
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ListView listView = getListView();
            final int checkedCount = listView.getCheckedItemCount();
            switch (item.getItemId()) {
                case R.id.delete:
                    confirmDeleteMessages();
                    break;
                default:
                    break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            // leave MultiChoiceMode
            mMultiChoiceMode = false;
            getListView().clearChoices();
            mListAdapter.notifyDataSetChanged();
            mSelectionMenu.dismiss();

        }

        public void setModeFinish() {
            if (mMode == null) {
                return;
            }
            mMode.finish();
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            ListView listView = getListView();
            int checkedCount = listView.getCheckedItemCount();
            mSelectionMenu.setTitle(getApplicationContext().getString(R.string.selected_count,
                    checkedCount));
            if (checkedCount == getListAdapter().getCount()) {
                mHasSelectAll = true;
            } else {
                mHasSelectAll = false;
            }
            mSelectionMenu.updateSelectAllMode(mHasSelectAll);
            mListAdapter.updateItemBackgroud(position);
            mListAdapter.notifyDataSetChanged();
        }
    }
}
