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

import java.io.ByteArrayOutputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

import com.android.mms.R;
import com.android.mms.dom.AttrImpl;
import com.android.mms.dom.smil.SmilDocumentImpl;
import com.android.mms.dom.smil.SmilPlayer;
import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.model.LayoutModel;
import com.android.mms.model.RegionModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.SmilHelper;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.SqliteWrapper;
import android.database.Cursor;
import android.widget.Toast;
import android.view.MenuItem;
import android.os.Message;
import android.content.ContentValues;
import android.provider.Telephony.Mms;
import android.content.DialogInterface;
import android.app.AlertDialog;
import com.android.mms.util.AddressUtils;
import android.content.Context;
import android.view.Menu;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.EncodedStringValue;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import com.android.mms.LogTag;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionService;
import android.content.ContentUris;
import android.app.ProgressDialog;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import android.os.StatFs;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduBody;
import android.os.Environment;
import com.google.android.mms.ContentType;
import java.util.ArrayList;
import android.view.View;

/**
 * Plays the given slideshow in full-screen mode with a common controller.
 */
public class SlideshowActivity extends Activity implements EventListener {
    private static final String TAG = "SlideshowActivity";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = false;

    private MediaController mMediaController;
    private SmilPlayer mSmilPlayer;

    private Handler mHandler;

    private SMILDocument mSmilDoc;

    private SlideView mSlideView;
    private int mSlideCount;
    
    private static final int MENU_NORMALSHOW        = 1;
    private static final int MENU_REPLY             = 2;
    private static final int MENU_REPLY_BY_MMS      = 3;
    private static final int MENU_RESEND            = 4;
    private static final int MENU_MMS_FORWARD       = 5;
    private static final int MENU_DELETE            = 6;

    private static final int MENU_TWO_CALL                 = 9;   
    private static final int MENU_SAVE_TO_CONTACT          = 13; 
    private static final int MENU_LOCK_UNLOCK_MMS          = 14; 
    private static final int MENU_EDIT_CALL            = 17; 
    private static final int MENU_DELIVERY_REPORT  =  20;       
    private static final int MENU_ONE_CALL                 = 21;   
    private static final int MENU_COPY_TO_SDCARD  = 22;  
 
 private static final int SHOW_TOAST = 10;
 private static final int SHOW_MEDIA_CONTROLLER = 3;
    
    private Uri mUri;
    private GenericPdu mPdu;
    private int mMailboxId                                 = -1;
    String msgFromTo = null;
    private int mMmsCurrentSize = 0;
    private static SlideshowModel mSlideModel;
    private static ProgressDialog mProgressDlg = null;
    private static final String VCALENDAR               = "vCalendar";
    private String direction = new String();
    private SlideScrollView mScrollView;

    /**
     * @return whether the Smil has MMS conformance layout.
     * Refer to MMS Conformance Document OMA-MMS-CONF-v1_2-20050301-A
     */
    private static final boolean isMMSConformance(SMILDocument smilDoc) {
        SMILElement head = smilDoc.getHead();
        if (head == null) {
            // No 'head' element
            return false;
        }
        NodeList children = head.getChildNodes();
        if (children == null || children.getLength() != 1) {
            // The 'head' element should have only one child.
            return false;
        }
        Node layout = children.item(0);
        if (layout == null || !"layout".equals(layout.getNodeName())) {
            // The child is not layout element
            return false;
        }
        NodeList layoutChildren = layout.getChildNodes();
        if (layoutChildren == null) {
            // The 'layout' element has no child.
            return false;
        }
        int num = layoutChildren.getLength();
        if (num <= 0) {
            // The 'layout' element has no child.
            return false;
        }
        for (int i = 0; i < num; i++) {
            Node layoutChild = layoutChildren.item(i);
            if (layoutChild == null) {
                // The 'layout' child is null.
                return false;
            }
            String name = layoutChild.getNodeName();
            if ("root-layout".equals(name)) {
                continue;
            } else if ("region".equals(name)) {
                NamedNodeMap map = layoutChild.getAttributes();
                for (int j = 0; j < map.getLength(); j++) {
                    Node node = map.item(j);
                    if (node == null) {
                        return false;
                    }
                    String attrName = node.getNodeName();
                    // The attr should be one of left, top, height, width, fit and id
                    if ("left".equals(attrName) || "top".equals(attrName) ||
                            "height".equals(attrName) || "width".equals(attrName) ||
                            "fit".equals(attrName)) {
                        continue;
                    } else if ("id".equals(attrName)) {
                        String value;
                        if (node instanceof AttrImpl) {
                            value = ((AttrImpl)node).getValue();
                        } else {
                            return false;
                        }
                        if ("Text".equals(value) || "Image".equals(value)) {
                            continue;
                        } else {
                            // The id attr is not 'Text' or 'Image'
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                // The 'layout' element has the child other than 'root-layout' or 'region'
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHandler = new Handler();

        // Play slide-show in full-screen mode.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.slideshow);
        mScrollView = (SlideScrollView)findViewById(R.id.scroll_slide_view);
        mScrollView.setScrollBarStyle(0x03000000);
        mScrollView.setHandler(this, uihandler);

        Intent intent = getIntent();
        Uri msg = intent.getData();
        final SlideshowModel model;
        mUri = msg;

        try {
            model = SlideshowModel.createFromMessageUri(this, msg);
            mSlideModel = model;
            mSlideCount = model.size();
            PduPersister p = PduPersister.getPduPersister(this);
            mPdu = p.load(msg);
        } catch (MmsException e) {
            Log.e(TAG, "Cannot present the slide show.", e);
            finish();
            return;
        }

        mMailboxId = getMmsMessageBoxID(this, mUri);
        mSlideView = (SlideView) findViewById(R.id.slide_view);
        PresenterFactory.getPresenter("SlideshowPresenter", this, mSlideView, model);

        if (mMailboxId == Mms.MESSAGE_BOX_INBOX) {
            msgFromTo = AddressUtils.getFrom(this, mUri);
        }else if (Mms.MESSAGE_BOX_OUTBOX == mMailboxId || Mms.MESSAGE_BOX_SENT == mMailboxId) {
            msgFromTo = AddressUtils.getTo(this, mUri);
        }else {
            Log.v(TAG,"   mmsEditCall  error draft box ");
            msgFromTo = AddressUtils.getTo(this, mUri);
        }
        mHandler.post(new Runnable() {
            private boolean isRotating() {
                return mSmilPlayer.isPausedState()
                        || mSmilPlayer.isPlayingState()
                        || mSmilPlayer.isPlayedState();
            }

            public void run() {
                mSmilPlayer = SmilPlayer.getPlayer();
                if (mSlideCount > 1) {
                    // Only show the slideshow controller if we have more than a single slide.
                    // Otherwise, when we play a sound on a single slide, it appears like
                    // the slide controller should control the sound (seeking, ff'ing, etc).
                    initMediaController();
                    mSlideView.setMediaController(mMediaController);
                }
                // Use SmilHelper.getDocument() to ensure rebuilding the
                // entire SMIL document.
                mSmilDoc = SmilHelper.getDocument(model);
                if (isMMSConformance(mSmilDoc)) {
                    int imageLeft = 0;
                    int imageTop = 0;
                    int textLeft = 0;
                    int textTop = 0;
                    LayoutModel layout = model.getLayout();
                    if (layout != null) {
                        RegionModel imageRegion = layout.getImageRegion();
                        if (imageRegion != null) {
                            imageLeft = imageRegion.getLeft();
                            imageTop = imageRegion.getTop();
                        }
                        RegionModel textRegion = layout.getTextRegion();
                        if (textRegion != null) {
                            textLeft = textRegion.getLeft();
                            textTop = textRegion.getTop();
                        }
                    }
                    mSlideView.enableMMSConformanceMode(textLeft, textTop, imageLeft, imageTop);
                }
                if (DEBUG) {
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    SmilXmlSerializer.serialize(mSmilDoc, ostream);
                    if (LOCAL_LOGV) {
                        Log.v(TAG, ostream.toString());
                    }
                }

                // Add event listener.
                ((EventTarget) mSmilDoc).addEventListener(
                        SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT,
                        SlideshowActivity.this, false);

                mSmilPlayer.init(mSmilDoc);
                if (isRotating()) {
                    mSmilPlayer.reload();
                } else {
                    mSmilPlayer.play();
                }
            }
        });
    }

    private void initMediaController() {
        mMediaController = new MediaController(SlideshowActivity.this, false);
        mMediaController.setMediaPlayer(new SmilPlayerController(mSmilPlayer));
        mMediaController.setAnchorView(findViewById(R.id.slide_view));
        mMediaController.setPrevNextListeners(
            new OnClickListener() {
              public void onClick(View v) {
                  mSmilPlayer.next();
              }
            },
            new OnClickListener() {
              public void onClick(View v) {
                  mSmilPlayer.prev();
              }
            });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if ((mSmilPlayer != null) && (mMediaController != null)) {
            mMediaController.show();
        }
        return false;
    }
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG,"onPause");
        if (mSmilDoc != null) {
            ((EventTarget) mSmilDoc).removeEventListener(
                    SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT, this, false);
        }
        if (mSmilPlayer != null) {
            mSmilPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG,"onstop");
        if ((null != mSmilPlayer)) {
            if (isFinishing()) {
                mSmilPlayer.stop();
            } else {
                mSmilPlayer.stopWhenReload();
            }
            if (mMediaController != null) {
                // Must do this so we don't leak a window.
                mMediaController.hide();
            }
        }
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v(TAG,"onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG,"onDestroy");
        if(mProgressDlg!=null)
            mProgressDlg = null;
        if (mSmilDoc != null) {
            Log.v(TAG, "------removeEventListener------");
            ((EventTarget) mSmilDoc).removeEventListener(
                    SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT, this, false);
        }
        if ((null != mSmilPlayer)) {
            if (isFinishing()) {
                mSmilPlayer.stop();
            } else {
                mSmilPlayer.stopWhenReload();
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_MENU:
                /*if ((mSmilPlayer != null) &&
                        (mSmilPlayer.isPausedState()
                        || mSmilPlayer.isPlayingState()
                        || mSmilPlayer.isPlayedState())) {
                    mSmilPlayer.stop();
                }*/
                break;
            default:
                if ((mSmilPlayer != null) && (mMediaController != null)) {
                    mMediaController.show();
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class SmilPlayerController implements MediaPlayerControl {
        private final SmilPlayer mPlayer;
        /**
         * We need to cache the playback state because when the MediaController issues a play or
         * pause command, it expects subsequent calls to {@link #isPlaying()} to return the right
         * value immediately. However, the SmilPlayer executes play and pause asynchronously, so
         * {@link #isPlaying()} will return the wrong value for some time. That's why we keep our
         * own version of the state of whether the player is playing.
         *
         * Initialized to true because we always programatically start the SmilPlayer upon creation
         */
        private boolean mCachedIsPlaying = true;

        public SmilPlayerController(SmilPlayer player) {
            mPlayer = player;
        }

        public int getBufferPercentage() {
            // We don't need to buffer data, always return 100%.
            return 100;
        }

        public int getCurrentPosition() {
            return mPlayer.getCurrentPosition();
        }

        public int getDuration() {
            return mPlayer.getDuration();
        }

        public boolean isPlaying() {
            return mCachedIsPlaying;
        }

        public void pause() {
            mPlayer.pause();
            mCachedIsPlaying = false;
        }

        public void seekTo(int pos) {
            // Don't need to support.
        }

        public void start() {
            mPlayer.start();
            mCachedIsPlaying = true;
        }

        public boolean canPause() {
            return true;
        }

        public boolean canSeekBackward() {
            return true;
        }

        public boolean canSeekForward() {
            return true;
        }
    }

    public void handleEvent(Event evt) {
        final Event event = evt;
        mHandler.post(new Runnable() {
            public void run() {
                String type = event.getType();
                Log.w(TAG,"handleEvent type="+type);
                if(type.equals(SmilDocumentImpl.SMIL_DOCUMENT_END_EVENT)) {
                    finish();
                }
            }
        });
    }




    public static int getMmsMessageBoxID(Context context, Uri uri)
    {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                                            uri, new String[] {Mms.MESSAGE_BOX}, null, null, null);

        if (cursor != null)
        {
            try
            {
                if (cursor.moveToFirst())
                {
                    return cursor.getInt(0);
                }
            }
            finally
            {
                cursor.close();
            }
        }
        return -1;
    }


    

    private void initSendDlg(){
        if (mProgressDlg != null){
            return;
        }
       
        //mProgressDlg = new ProgressDialog(this, "KB");
        mProgressDlg = new ProgressDialog(this);
        mProgressDlg.setTitle(getString(R.string.dialing));
        //mProgressDlg.setMessage(body);
        mProgressDlg.setIndeterminate(false);
        mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener(){
                            final public void onClick(DialogInterface dialog, int which){
                                if ( mProgressDlg.isShowing() ){
                                    mProgressDlg.dismiss();
                                }
                               // ServiceControl.getInstance().cancelMmsSending();                                    
                            }
                        });   
    }

    //sunzuohua ADD
    private void reSendMms(){
        Log.d("slideshowActivity","reSendMms");
//        ServiceControl.getInstance().bindService();
        mMmsCurrentSize = mSlideModel.getCurrentMessageSize();
        if (0 == mMmsCurrentSize){
            mMmsCurrentSize = 1;
        }        
        if(mProgressDlg!=null)
        {
            if (mProgressDlg.isShowing() ){
                return ;
            }
        }
        mProgressDlg = null;
        initSendDlg();
        mProgressDlg.setMax((mMmsCurrentSize + 2*1024)/1024); //kB   
        mProgressDlg.setMessage(getString(R.string.message_size_label)
                                 + String.valueOf((mMmsCurrentSize +2*1024) / 1024)
                                 + getString(R.string.kilobyte));
        mProgressDlg.show();
        mProgressDlg.setTitle(getString(R.string.dialing));
        mProgressDlg.setProgress(0);

        Intent intent = new Intent(this, TransactionService.class);
        intent.putExtra(TransactionBundle.URI, mUri.toString());
        intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                            Transaction.SEND_TRANSACTION);        
        startService(intent);

    }
    private void forwardMms(){
        Intent intent = new Intent(this,ComposeMessageActivity.class);

        String tmp = null;
        PduHeaders ph = mPdu.getPduHeaders();
        EncodedStringValue ev = ph.getEncodedStringValue(PduHeaders.SUBJECT);

        if ( null != ev ){
            tmp = ev.getString();            
        }
        
        String subject = getString(R.string.forward_prefix);

        if ( null != tmp ){
            subject = subject + tmp;
        }

        while(subject.getBytes().length > 40){
            subject = subject.substring(0, subject.length() - 1);
        }

        
        long msgId = ContentUris.parseId(mUri);   
                            
        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, msgId);

        intent.putExtra("copythenforward", true);
        intent.putExtra("forwarded_message", true);
        intent.putExtra("msg_uri", uri);
        intent.putExtra("subject", subject);
        startActivity(intent);
    }
    private void addToContact()
    {
        if(msgFromTo == null){
            return;
        }
        String address = msgFromTo;
        if (TextUtils.isEmpty(address)) {
            if (LogTag.VERBOSE || Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG,"  saveToContact fail for null address! ");                     
            }
            return;
        }

        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        this.startActivity(intent); 
    }
    public static void replyMessage(Context context, String number)
    {

        Intent intent = new Intent(context, ComposeMessageActivity.class);
        Log.v(TAG, "__________number   ==  "+number);
        intent.putExtra("address", number);
        intent.putExtra("msg_reply", true);
        intent.putExtra("exit_on_sent", true);        
        context.startActivity(intent);
    }
    private class DeleteMessageListener implements DialogInterface.OnClickListener
    {
        public DeleteMessageListener()
        {
        }
        public void onClick(DialogInterface dialog, int whichButton)
        {
            delete();
        }
    }

    private void delete(){
        SqliteWrapper.delete(this, getContentResolver(),
                                    mUri, null, null);
        Toast.makeText(this, R.string.operate_success, Toast.LENGTH_LONG).show();
        finish();
    }

    private void confirmDeleteDialog(DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_dialog_title);
        builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setCancelable(true);
        builder.setMessage(R.string.confirm_delete_message);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }
    private Handler uihandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case SHOW_TOAST:
                {
                    String toastStr = (String) msg.obj;
                    Toast.makeText(SlideshowActivity.this, toastStr, 
                                    Toast.LENGTH_LONG).show();
                    break; 
                }
                case SHOW_MEDIA_CONTROLLER:
                 {
                 if(mMediaController!=null)
                     mMediaController.show();
                     break; 
                 }   
                               
            }
        }
    };

    private void showCallSelectDialog(){
        String[] items = new String[MessageUtils.getActivatedIccCardCount()];
        for (int i = 0; i < items.length; i++) {
            items[i] = MessageUtils.getMultiSimName(this, i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_call));
        builder.setCancelable(true);
        builder.setItems(items, new DialogInterface.OnClickListener()
        {
            public final void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(SlideshowActivity.this, msgFromTo, MessageUtils.SUB1);
                         Looper.loop();
                        }
                    }).start();
                }
                else
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(SlideshowActivity.this, msgFromTo, MessageUtils.SUB2);
                         Looper.loop();
                        }
                    }).start();
                }
                dialog.dismiss();
            }
        });
        builder.show();    
    }
    /**
     * Simple cache to prevent having to load the same PduBody again and again for the same uri.
     */
    private static class PduBodyCache {
        private static PduBody mLastPduBody;
        private static Uri mLastUri;
    
        static public PduBody getPduBody(Context context, Uri contentUri) {
            if (contentUri.equals(mLastUri)) {
                return mLastPduBody;
            }
            try {
                mLastPduBody = SlideshowModel.getPduBody(context, contentUri);
                mLastUri = contentUri;
             } catch (MmsException e) {
                 Log.e(TAG, e.getMessage(), e);
                 return null;
             }
             return mLastPduBody;
        }
    };

      
      
      
      private boolean sdcardCanuse(){
      
       if(isSDCardExist()){
           File mVcardDirectory = new File("/sdcard/"); 
           StatFs fs = new StatFs(mVcardDirectory.getAbsolutePath());
           long blocks = fs.getAvailableBlocks();
           long blockSize = fs.getBlockSize();
           return (blocks*blockSize)>(50*1024);
       }
       return false;
      }
      
      private final boolean isSDCardExist() {
              boolean ret = true;
              String status = Environment.getExternalStorageState();
              if (status.equals(Environment.MEDIA_REMOVED) 
                  ||status.equals(Environment.MEDIA_BAD_REMOVAL)
                  ||status.equals(Environment.MEDIA_CHECKING)
                  ||status.equals(Environment.MEDIA_SHARED)
                  ||status.equals(Environment.MEDIA_UNMOUNTED)
                  ||status.equals(Environment.MEDIA_NOFS)
                  ||status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
                  ||status.equals(Environment.MEDIA_UNMOUNTABLE)) {
                  ret = false; 
              }
              return ret;
          }  
      
      /**
      check whether the part contains video media
      */
      private boolean isVideo(PduPart part){
          String ct = new String(part.getContentType());
      
          //we only supervise the type of application/oct-stream
          if (!ct.equals("application/oct-stream")
              && !ct.equals("application/octet-stream")){
              return false;
          }
          
          //mp3|wav|aac|amr|mid|ogg
          byte[] location = part.getContentLocation();
                  
          if (location == null) {
              location = part.getName();
          }
          if (location == null) {
              location = part.getFilename();
          }
      
          if (location == null){
              return false;
          }
      
          String name = new String(location);
      
          //mp4|3gp|3gpp2|3gpp
          if (name.contains(".mp4")
              || name.contains(".3gp")
              || name.contains(".3gpp2")
              || name.contains(".3gpp")){
              return true;
          }
      
          return false;
      }
      
      /**
      check whether the part contains music media
      */
      private boolean isMusic(PduPart part){
          String ct = new String(part.getContentType());
      
          //we only supervise the type of application/oct-stream
          if (!ct.equals("application/oct-stream")
              && !ct.equals("application/octet-stream")){
              if(ct.contains("ogg"))
                  return true;
              return false;
          }
          
          //mp3|wav|aac|amr|mid|ogg
          byte[] location = part.getContentLocation();
                  
          if (location == null) {
              location = part.getName();
          }
          if (location == null) {
              location = part.getFilename();
          }
      
          if (location == null){
              return false;
          }
      
          String name = new String(location);
      
          if (name.contains(".mp3")
              || name.contains(".wav")
              || name.contains(".aac")
              || name.contains(".amr")
              || name.contains(".mid")
              || name.contains(".wma")
              || name.contains(".ogg")){
              Log.v(TAG,"is music");
              return true;
          }
      
          return false;
      }
      
      
      private File getUniqueDestination(String base, String extension) {
          File file = new File(base + "." + extension);
      
          for (int i = 2; file.exists(); i++) {
              file = new File(base + "_" + i + "." + extension);
          }
          return file;
      }
       private boolean copyPart(PduPart part, String fallback) {
          Uri uri = part.getDataUri();
          String dir;
          String subPath;
      
          InputStream input = null;
          FileOutputStream fout = null;
          String mimeType = new String(part.getContentType());
          try {
              input = getContentResolver().openInputStream(uri);
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
                      fileName = new String(location);
                  }
      
                    if(mimeType.startsWith("image")){
                        subPath = "/Picture/";
                    }else if(mimeType.startsWith("audio") || isMusic(part)){
                        subPath = "/Audio/";
                    }else if(mimeType.startsWith("video") || isVideo(part)){
                        subPath = "/Video/";
                    }else if(-1 != mimeType.indexOf(VCALENDAR)){
                        subPath = "/Other/vCalendar/";
                        final String dir_vcal_path="/sdcard/Other/vCalendar/";
                        File dirFile=new File(dir_vcal_path);
                        if(!dirFile.exists()){
                            if(!dirFile.mkdirs()){
                                return false;
                            }
                        }
                    }else{
                        subPath = "/Other/";
                    }
                    
                   if(sdcardCanuse())
                   
                   dir = Environment.getExternalStorageDirectory() + "/"
                              + Environment.DIRECTORY_DOWNLOADS  + "/";
                   else
                   {
                       return false;
                  
                   }
                  String extension;
                  int index;
                  if ((index = fileName.indexOf(".")) == -1) {
                      String type = new String(part.getContentType());
                      extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                  } else {
                      extension = fileName.substring(index + 1, fileName.length());
                      fileName = fileName.substring(0, index);
                  }
      
                  File file = getUniqueDestination(dir + fileName, extension);
                  direction = file.getAbsolutePath();//dir + fileName;
      
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
        private boolean copyMedia(Uri uri) {
          boolean result = true;
          PduBody body = PduBodyCache.getPduBody(this, uri);
          if (body == null) {
              return false;
          }
      
          long msgId = getMmsMessageBoxID(this, uri);
      
          int partNum = body.getPartsNum();
          for(int i = 0; i < partNum; i++) {
              PduPart part = body.getPart(i);
              String type = new String(part.getContentType());
              
      
              if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                      ContentType.isAudioType(type)) {
                  result &= copyPart(part, Long.toHexString(msgId));   // all parts have to be successful for a valid result.
              }
          }
          return result;
      }
      /**
       * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
       * @param msgId
       */
      private boolean haveSomethingToCopyToSDCard(Uri uri) {
          PduBody body = PduBodyCache.getPduBody(this, uri);
          if (body == null) {
              return false;
          }
      
          boolean result = false;
          int partNum = body.getPartsNum();
          for(int i = 0; i < partNum; i++) {
              PduPart part = body.getPart(i);
              String type = new String(part.getContentType());
      
              Log.v(TAG,"[CMA] haveSomethingToCopyToSDCard: part[" + i + "] contentType=" + type);
      
              if (ContentType.isImageType(type) || ContentType.isVideoType(type) ||
                      ContentType.isAudioType(type)) {
                  result = true;
                  break;
              }
          }
          return result;
      }

      private void showDeliveryReport(long messageId, String type) {
          Intent intent = new Intent(this, DeliveryReportActivity.class);
          intent.putExtra("message_id", messageId);
          intent.putExtra("message_type", type);
  
          startActivity(intent);
      }
           
      public static void viewMmsMessageAttachmentMobilepaper(Context context, Uri msgUri,
                  SlideshowModel slideshow, PduPersister persister, ArrayList<String> allIdList,boolean report)
          {
      
              boolean isSimple = (slideshow == null) ? false : slideshow.isSimple();
              if (isSimple || msgUri == null)
              {
                  // In attachment-editor mode, we only ever have one slide.
                  MessageUtils.viewSimpleSlideshow(context, slideshow);
              }
              else
              {
                  Intent intent = new Intent(context, MobilePaperShowActivity.class);            
                  intent.setData(msgUri);
                  intent.putExtra("mms_report", report);
                  intent.putStringArrayListExtra("sms_id_list", allIdList);
                  context.startActivity(intent);
              }
          }
      @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
            menu.clear();
            Cursor cursor = SqliteWrapper.query(this, this.getContentResolver(),
                                                mUri, new String[] {Mms.LOCKED}, null, null, null);
            cursor.moveToFirst();
            
            int msgType = mPdu.getMessageType();
            if(!(Mms.MESSAGE_BOX_DRAFTS == mMailboxId)){
               {
                   menu.add(0, MENU_DELETE, 0, R.string.menu_delete_msg);
                    menu.add(0, MENU_ONE_CALL, 0, R.string.menu_call);  
               }
            }
            if(Mms.MESSAGE_BOX_INBOX == mMailboxId){
                menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
                }
            if(Mms.MESSAGE_BOX_INBOX == mMailboxId||mMailboxId==Mms.MESSAGE_BOX_SENT){
                menu.add(0, MENU_MMS_FORWARD, 0, R.string.menu_forward);     
            }        
            if(Mms.MESSAGE_BOX_OUTBOX == mMailboxId || Mms.MESSAGE_BOX_SENT == mMailboxId){
                menu.add(0, MENU_RESEND, 0, R.string.menu_resend);
                   } 
            if(mMailboxId != Mms.MESSAGE_BOX_DRAFTS){
    
                if(cursor.getInt(0) == 0 ){
                    menu.add(0, MENU_LOCK_UNLOCK_MMS, 0, R.string.menu_lock);
                } else{
                    menu.add(0, MENU_LOCK_UNLOCK_MMS, 0, R.string.menu_unlock);
                }
    
            }         
                 
           if (haveSomethingToCopyToSDCard(mUri)){
                 menu.add(0, MENU_COPY_TO_SDCARD, 0, R.string.copy_to_sdcard);
           }

           if((Mms.MESSAGE_BOX_SENT == mMailboxId)){
               if (getIntent().getBooleanExtra("mms_report", false)) {
                   menu.add(0, MENU_DELIVERY_REPORT, 0, R.string.view_delivery_report);
               }  
           }  
           
           if ((PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF == msgType || PduHeaders.MESSAGE_TYPE_SEND_REQ == msgType)
                && !(Mms.MESSAGE_BOX_DRAFTS == mMailboxId)){
    
                menu.add(0, MENU_SAVE_TO_CONTACT, 0, R.string.menu_save_to_contact).setIcon(
                    R.drawable.ic_menu_move_up);
            }
           
    
             menu.add(0, MENU_NORMALSHOW, 0, R.string.normal_show);
            cursor.close();
            return true;
        }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){

        case MENU_NORMALSHOW:
            Intent intent = getIntent();
            Uri msg = intent.getData();   
            viewMmsMessageAttachmentMobilepaper(this,msg,null,null,intent.getStringArrayListExtra("sms_id_list"),intent.getBooleanExtra("mms_report", false));
            finish();
            break;    
        case MENU_ONE_CALL:
            if(MessageUtils.isMultiSimEnabledMms())
            {
                if(MessageUtils.getActivatedIccCardCount() > 1)
                {
                    showCallSelectDialog();
                }
                else
                {
                    if(MessageUtils.isIccCardActivated(MessageUtils.SUB1))
                    {
                        MessageUtils.dialRecipient(this, msgFromTo, MessageUtils.SUB1);
                    }
                    else if(MessageUtils.isIccCardActivated(MessageUtils.SUB2))
                    {
                        MessageUtils.dialRecipient(this, msgFromTo, MessageUtils.SUB2);
                    }                         
                }
            }
            else
            {
                MessageUtils.dialRecipient(this, msgFromTo, MessageUtils.SUB_INVALID);
            }
            break;
        case MENU_RESEND:{
            reSendMms();
            break;
        }
        
        case MENU_MMS_FORWARD:{
            forwardMms();
            finish();
            break;
        }
        case MENU_REPLY:{
            replyMessage(this, AddressUtils.getFrom(this,mUri));   
            finish();
            break;
        }
        //wxj add end
        case MENU_LOCK_UNLOCK_MMS :
            {
            Cursor cursor = SqliteWrapper.query(this, this.getContentResolver(),
                                            mUri, new String[] {Mms.LOCKED}, null, null, null);
            cursor.moveToFirst();
            boolean locked = false;
            locked = cursor.getInt(0) != 0;                     
            cursor.close();
            final ContentValues values = new ContentValues(1);
            values.put("locked", locked ? 0 : 1);

            new Thread(new Runnable() {
                public void run() {
                    getContentResolver().update(mUri,
                            values, null, null);
                    Message msg = Message.obtain();
                    msg.what = SHOW_TOAST;
                    msg.obj = getString(R.string.operate_success);
                    uihandler.sendMessage(msg);  
                }
            }).start();
            break;
        }
        case MENU_DELIVERY_REPORT:
            showDeliveryReport(ContentUris.parseId(mUri),"mms");
            break;  
        case MENU_SAVE_TO_CONTACT:
            addToContact();
            break;
        case MENU_COPY_TO_SDCARD:{
            if(copyMedia(mUri)){
            
                int resId = R.string.copy_to_sdcard_direction_success ;
                String direction1;
                if(sdcardCanuse())
                     direction1 = getResources().getString(resId) 
                                    + Environment.getExternalStorageDirectory() + "/"
                                    + Environment.DIRECTORY_DOWNLOADS + "/";
                Toast.makeText(SlideshowActivity.this, direction, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(SlideshowActivity.this, R.string.copy_to_sdcard_fail, Toast.LENGTH_SHORT).show();
            }
            break;
            }        
        case MENU_DELETE:{
            Cursor cursor = SqliteWrapper.query(this, this.getContentResolver(),
                                            mUri, new String[] {Mms.LOCKED}, null, null, null);
            cursor.moveToFirst();
            boolean locked = false;
            locked = cursor.getInt(0) != 0;   
            cursor.close();
            if (locked)
            {
                Toast.makeText(this, R.string.delete_lock_err, Toast.LENGTH_LONG).show();
                return false;
            }

            DeleteMessageListener l = new DeleteMessageListener();
            confirmDeleteDialog(l);
            break;
        }
        default:
            break;
        }
        return true;
    }
}
