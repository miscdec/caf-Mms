package com.android.mms.ui;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.net.Uri;
import android.content.ContentUris;
import java.io.ByteArrayOutputStream;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.EncodedStringValue;
import com.android.mms.model.SlideModel;
import android.widget.LinearLayout;
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
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionService;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import android.provider.Telephony.Mms;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_ABORT;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_COMPLETE;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_START;
import static com.android.mms.transaction.ProgressCallbackEntity.PROGRESS_STATUS_ACTION;
import com.android.mms.MmsConfig;
import android.app.AlertDialog;
import com.android.mms.util.AddressUtils;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.os.Message;
import com.google.android.mms.util.SqliteWrapper;
import android.database.Cursor;
import com.android.mms.transaction.TransactionState;
import android.text.TextUtils;
import com.android.mms.util.SendingProgressTokenManager;
import android.content.ContentValues;
import com.android.mms.transaction.MessagingNotification;
import java.util.ArrayList;
import android.util.TypedValue;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.BaseColumns;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.app.ActionBar;
import java.util.HashSet;
import android.provider.Telephony.Threads;
import java.util.Arrays;
import com.android.mms.util.ContactInfoCache;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.os.Looper;
import android.content.res.Resources;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
public class MobilePaperShowActivity extends Activity
{
    private static final String TAG = "MobilePaperShowActivity";
    private SlideView mSlideView;
    private static SlideshowModel mSlideModel;
    private SlideshowPresenter mPresenter;
    SlideshowModel model;
    private GenericPdu mPdu;
    private LinearLayout rootView;
    private int mMailboxId                                 = -1;
    private Uri mUri;
    private static ProgressDialog mProgressDlg = null;
    private int mMmsCurrentSize = 0;
    private Uri mSendingMessageUri;
    private Uri mMessageUri;
    private ArrayList<String> m_AllIdList = null;
    private int mTextSize = 27;
    
    private static final int MENU_SLIDESHOW            = 1;
    private static final int MENU_MMS_VIEW_ATTACHMENT      = 2;
    private static final int MENU_REPLY             =3;
    private static final int MENU_REPLY_BY_MMS              = 4;
    private static final int MENU_RESEND              = 5;
    private static final int MENU_MMS_FORWARD                    = 6;
    private static final int MENU_DEL                    = 7;
    private static final int MENU_EXTRACT_NUMBER           = 8;

    private static final int MENU_TWO_CALL                 = 9;   
    private static final int MENU_CALL                     = 10;   
    private static final int MENU_EDIT_CALL                =  11; 
    private static final int MENU_ADD_TO_BLACK             = 12; 
    private static final int MENU_SAVE_TO_CONTACT          =  13; 
    private static final int MENU_LOCK_UNLOCK_MMS          =  14; 
    private static final int MENU_VT_CALL          =  15; 
    private static final int MENU_IP_CALL          = 16; 
    private static final int MENU_ADD_TO_WHITE            =  17; 
    private static final int MENU_DETAIL            =  18; 
    private static final int MENU_DELETE_BY_NUMBER  =  19;   
    private static final int MENU_DELIVERY_REPORT  = 20;   
    private static final int MENU_ONE_CALL                 =  21;   
    private static final int SHOWPREMSG = 1;
    private static final int SHOWNEXTMSG = 2;
    private static final int ZOOMIN = 4;
    private static final int ZOOMOUT = 5;
    private static final int OPERATE_DEL_SINGLE_OVER = 6;
    private static final int SHOW_TOAST = 10;
    private static final String TYPE_RECIPIENT_ID_COLUMN = "recipient_ids";    
    String[] PROJECTION = new String[] {
        "'mms' AS " + MmsSms.TYPE_DISCRIMINATOR_COLUMN,
        BaseColumns._ID,
        Conversations.THREAD_ID,
        // For SMS
       // "_address",
        "NULL AS" + Sms.BODY,
        "pdu.date * 1000 AS " + Sms.DATE,
        Sms.READ,
        "NULL AS" + Sms.TYPE,
        "NULL AS " + Sms.STATUS,
        Sms.LOCKED,        
        "NULL AS " +Sms.ERROR_CODE,        
        // For MMS
        Mms.SUBJECT,
        Mms.SUBJECT_CHARSET,
        Mms.DATE,
        Mms.READ,
        Mms.MESSAGE_TYPE,
        Mms.MESSAGE_BOX,
        Mms.DELIVERY_REPORT,
        Mms.READ_REPORT,
        "NULL AS " + PendingMessages.ERROR_TYPE,
        Mms.LOCKED,        
        "NULL AS " + TYPE_RECIPIENT_ID_COLUMN,
        Mms.CONTENT_TYPE, 
        Mms.MESSAGE_ID,
    };
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
                    Toast.makeText(MobilePaperShowActivity.this, toastStr, 
                                    Toast.LENGTH_LONG).show();
                    break; 
                }
               case ZOOMIN:
               {
                    zoomIn();
                    break;
               }
               case ZOOMOUT:
               {
                    zoomOut();
                    break;
               }        
                case OPERATE_DEL_SINGLE_OVER:
                {
                    int result = msg.arg1;
                    if (result > 0)
                    {
                        Toast.makeText(MobilePaperShowActivity.this, R.string.operate_success,
                                       Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(MobilePaperShowActivity.this, R.string.operate_failure,
                                       Toast.LENGTH_SHORT).show();
                    }
                    break;
                }                            
            }
        }
    };
    public static void setCurrentTextSet(Context context, int value)
    {
        SharedPreferences prefsms = PreferenceManager.getDefaultSharedPreferences(context);        
        prefsms.edit().putString("smsfontsize", String.valueOf(value)).commit();
    }

    //more big
    private void zoomIn()
    {
          mTextSize = mTextSize + 9 <= 36 ? mTextSize + 9 : 36;
          if (mTextSize >= 36)
          {
              mTextSize = 36;
          }          
        Log.d(TAG,"+++++++++++zoomin:" + mTextSize);
        setCurrentTextSet(this, (int)mTextSize);
        redrawPaper();
    }

    private void zoomOut()
    {
           mTextSize = mTextSize - 9 < 18 ? 18 : mTextSize - 9;
           if (mTextSize <= 18)
           {
               mTextSize = 18;
           }           
        Log.d(TAG,"+++++++++++zoomin:" + mTextSize);
        setCurrentTextSet(this, (int)mTextSize);
        redrawPaper();
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
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);    
        
        Intent intent = getIntent();
        Uri msg = intent.getData();
        setContentView(R.layout.mobile_paper_view);
        mMailboxId = getMmsMessageBoxID(this, msg);
        mUri = msg;
        mMessageUri = msg;
        m_AllIdList = intent.getStringArrayListExtra("sms_id_list");
        rootView = (LinearLayout)findViewById(R.id.view_root);   
        SlideScrollView scrollView = (SlideScrollView)findViewById(R.id.view_scroll);   
        scrollView.setHandler(this, uihandler);
        try {
            // mod for differ play from other such as forward      
            model = SlideshowModel.createFromMessageUri(this, msg);
            mSlideModel = model;
            PduPersister p = PduPersister.getPduPersister(this);
            mPdu = p.load(msg);
            //add 11.4.27 for add a slide when preview && slide is 0          
            if (0==mSlideModel.size())
            {
                SlideModel slModel=new SlideModel(mSlideModel);
                mSlideModel.add(slModel);
            }

            PduHeaders ph = mPdu.getPduHeaders();                                

            if (null != ph){
                EncodedStringValue ev = ph.getEncodedStringValue(PduHeaders.SUBJECT);
                if(ev != null){
                    String sub = ev.getString();
                    if(null != sub){
                        setTitle(sub);
                    } else{
                        setTitle("");
                    }
                } else{
                    setTitle("");
                }
            }else{
                setTitle("");
            }
        } catch (MmsException e) {
            Log.e(TAG, "Cannot present the slide show.", e);
            Toast.makeText( this, R.string.cannot_play, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
            LayoutInflater mInflater = LayoutInflater.from(this);
            for(int index = 0; index < mSlideModel.size();index++)
            {
                Log.d(TAG,"+++++++++++++++++" + index);
                SlideListItemView view = (SlideListItemView) mInflater.inflate(R.layout.mobile_paper_item,null);                
                mPresenter = (SlideshowPresenter)PresenterFactory.getPresenter("SlideshowPresenter", this, (SlideViewInterface)view, model);
                SharedPreferences prefsms = PreferenceManager.getDefaultSharedPreferences(this);
                mTextSize = Integer.parseInt(prefsms.getString("smsfontsize", "27"));//14,21,28
                TextView contentText;
                contentText = (TextView) view.findViewById(R.id.text_preview);
                contentText.setTelUrl("tels:");
                contentText.setWebUrl("www_custom:");           
                contentText.setTextIsSelectable(true);
                contentText.setClickable(false);
                mPresenter.presentSlide( (SlideViewInterface)view, mSlideModel.get(index));
                contentText.setTextSize(TypedValue.COMPLEX_UNIT_PX,mTextSize);     
                TextView text;
                text = (TextView) view.findViewById(R.id.duration_text);
                text.setFocusable(false);
                text.setFocusableInTouchMode(false);
                if(text.getText() != null)
                {
                    text.setVisibility(View.GONE);
                }
                text = (TextView) view.findViewById(R.id.slide_number_text);
                text.setFocusable(false);
                text.setFocusableInTouchMode(false);
                text.setTextSize(18);              
                text.setTextExt(getString(R.string.slide_number, index + 1));                
                rootView.addView(view);
            }
        boolean unread = intent.getBooleanExtra("unread", false);
        if (unread)
        {
            ContentValues values = new ContentValues(1);
            values.put(Mms.READ, MessageUtils.MESSAGE_READ);
            SqliteWrapper.update(this, getContentResolver(), 
                mUri, values, null, null);
         
         MessagingNotification.blockingUpdateNewMessageIndicator(
                 this, MessagingNotification.THREAD_NONE, false);
         
        }        
        // Register a BroadcastReceiver to listen on HTTP I/O process.
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);              
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
                }
                
    private void redrawPaper()
    {
            rootView.removeAllViews();
            LayoutInflater mInflater = LayoutInflater.from(this);
            for(int index = 0; index < mSlideModel.size();index++)
            {
                Log.d(TAG,"+++++++++++++++++" + index);
                SlideListItemView view = (SlideListItemView) mInflater.inflate(R.layout.mobile_paper_item,null);                
                mPresenter = (SlideshowPresenter)PresenterFactory.getPresenter("SlideshowPresenter", this, (SlideViewInterface)view, model);
                SharedPreferences prefsms = PreferenceManager.getDefaultSharedPreferences(this);
                int mTextSize = Integer.parseInt(prefsms.getString("smsfontsize", "27"));//14,21,28
                TextView contentText;
                contentText = (TextView) view.findViewById(R.id.text_preview);
                contentText.setTelUrl("tels:");
                contentText.setWebUrl("www_custom:");             
                contentText.setTextIsSelectable(true);
                contentText.setTextSize(TypedValue.COMPLEX_UNIT_PX,mTextSize);                
                mPresenter.presentSlide( (SlideViewInterface)view, mSlideModel.get(index));
                TextView text = (TextView) view.findViewById(R.id.slide_number_text);
                text.setText(getString(R.string.slide_number, index + 1));                
                rootView.addView(view);
            }        
    }

    private boolean inPortraitMode() {
        final Configuration configuration = getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private SlideViewInterface createView(int index) {
        boolean inPortrait = inPortraitMode();
        SlideModel slide = mSlideModel.get(index);
        LinearLayout view = new LinearLayout(this);
        return (SlideViewInterface)view;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        Cursor cursor = SqliteWrapper.query(this, this.getContentResolver(),
                                            mUri, new String[] {Mms.LOCKED}, null, null, null);
        cursor.moveToFirst();
        if(mPdu == null)
        {
            return false;
        }
        int msgType = mPdu.getMessageType();
        if(Mms.MESSAGE_BOX_INBOX == mMailboxId){
            menu.add(0, MENU_REPLY, 0, R.string.menu_reply);
        } else if(Mms.MESSAGE_BOX_OUTBOX == mMailboxId || Mms.MESSAGE_BOX_SENT == mMailboxId){
            menu.add(0, MENU_RESEND, 0, R.string.menu_resend);
        } 

        if(!(Mms.MESSAGE_BOX_DRAFTS == mMailboxId)){
            menu.add(0, MENU_MMS_FORWARD, 0, R.string.menu_forward);
            menu.add(0, MENU_DEL, 0, R.string.menu_delete_msg);
            menu.add(0, MENU_ONE_CALL, 0, R.string.menu_call);                                                 
        }
        if(mMailboxId != Mms.MESSAGE_BOX_DRAFTS){

            if(cursor.getInt(0) == 0 ){
                menu.add(0, MENU_LOCK_UNLOCK_MMS, 0, R.string.menu_lock);
            } else{
                menu.add(0, MENU_LOCK_UNLOCK_MMS, 0, R.string.menu_unlock);
            }

        }        
        
        menu.add(0, MENU_DETAIL, 0, R.string.view_message_details);

        if((Mms.MESSAGE_BOX_SENT == mMailboxId)){
            if (getIntent().getBooleanExtra("mms_report", false)) {
                menu.add(0, MENU_DELIVERY_REPORT, 0, R.string.view_delivery_report);
            }     
        }
        
        if ((PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF == msgType || PduHeaders.MESSAGE_TYPE_SEND_REQ == msgType)
                                                && !(Mms.MESSAGE_BOX_DRAFTS == mMailboxId)){

            menu.add(0, MENU_SAVE_TO_CONTACT, 0, R.string.menu_save_to_contact);
        }
                                
        menu.add(0, MENU_SLIDESHOW, 0, R.string.view_slideshow);

        cursor.close();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case MENU_SLIDESHOW:
            Intent intent = getIntent();
            Uri msg = intent.getData();   
            viewMmsMessageAttachmentSliderShow(this,msg,null,null,intent.getStringArrayListExtra("sms_id_list"),intent.getBooleanExtra("mms_report", false));
            finish();
            break;    
        case MENU_DEL:{
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
        case MENU_CALL:
            call();
            break;     
        case MENU_ONE_CALL:
            call();
            break;     
        case MENU_SAVE_TO_CONTACT:
            addToContact();
            break;
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
        case MENU_DETAIL:
        {
            Cursor cursor = SqliteWrapper.query(this, this.getContentResolver(),
                                            mUri, PROJECTION, null, null, null);
            cursor.moveToFirst();
            String messageDetails = getMessageDetails(
                    MobilePaperShowActivity.this, cursor, mSlideModel.getCurrentMessageSize());
            cursor.close();
            new AlertDialog.Builder(MobilePaperShowActivity.this)
                    .setTitle(R.string.message_details_title)
                    .setMessage(messageDetails)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(true)
                    .show();            
            break;
        }        
        case android.R.id.home:        
            finish();
            break;
        case MENU_DELIVERY_REPORT:
            showDeliveryReport(ContentUris.parseId(mUri),"mms");
            break;             
        default:
            break;
        }
        return true;
    }
    
    
        public static void viewMmsMessageAttachmentSliderShow(Context context, Uri msgUri,
                SlideshowModel slideshow, PduPersister persister, ArrayList<String> allIdList,boolean report)
        {
    
            boolean isSimple = (slideshow == null) ? false : slideshow.isSimple();
            if (isSimple || msgUri == null)
            {
                MessageUtils.viewSimpleSlideshow(context, slideshow);
            }
            else
            {
                Intent intent = new Intent(context, SlideshowActivity.class);
                intent.setData(msgUri);
                intent.putExtra("mms_report", report);
                intent.putStringArrayListExtra("sms_id_list", allIdList);
                context.startActivity(intent);
            }
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

    private void showDeliveryReport(long messageId, String type) {
        Intent intent = new Intent(this, DeliveryReportActivity.class);
        intent.putExtra("message_id", messageId);
        intent.putExtra("message_type", type);

        startActivity(intent);
    }

    private void showCallSelectDialog(final String msgFromTo){
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
                         MessageUtils.dialRecipient(MobilePaperShowActivity.this, msgFromTo, MessageUtils.SUB1);
                         Looper.loop();
                        }
                    }).start();
                }
                else
                {
                    new Thread(new Runnable() {
                        public void run() {
                         Looper.prepare();
                         MessageUtils.dialRecipient(MobilePaperShowActivity.this, msgFromTo, MessageUtils.SUB2);
                         Looper.loop();
                        }
                    }).start();
                }
                dialog.dismiss();
            }
        });
        builder.show();    
    }
    private void call() {
        String msgFromTo = null;
        if (mMailboxId == Mms.MESSAGE_BOX_INBOX) {
            msgFromTo = AddressUtils.getFrom(this, mUri);
        }else if (Mms.MESSAGE_BOX_OUTBOX == mMailboxId || Mms.MESSAGE_BOX_SENT == mMailboxId) {
            msgFromTo = AddressUtils.getTo(this, mUri);
        }else {
            Log.v(TAG,"   mmsEditCall  error draft box ");
            msgFromTo = AddressUtils.getTo(this, mUri);
        }
        if(msgFromTo == null){
            return;
        }
       
       if(MessageUtils.isMultiSimEnabledMms())
       {
           if(MessageUtils.getActivatedIccCardCount() > 1)
           {
               showCallSelectDialog(msgFromTo);
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
       
    }

    private void addToContact() {
        String msgFromTo = null;
        if (mMailboxId == Mms.MESSAGE_BOX_INBOX) {
            msgFromTo = AddressUtils.getFrom(this, mUri);
        }else if (Mms.MESSAGE_BOX_OUTBOX == mMailboxId || Mms.MESSAGE_BOX_SENT == mMailboxId) {
            msgFromTo = AddressUtils.getTo(this, mUri);
        }else {
            Log.v(TAG,"   mmsEditCall  error draft box ");
            msgFromTo = AddressUtils.getTo(this, mUri);
        }
        if(msgFromTo == null){
            return;
        }
        String[] addresses = msgFromTo.split(";");
        String sb = addresses[0];
        Log.v(TAG,"   addToContact    number = "+sb);
        if (sb == null || sb.length() == 0) {
            return;
        }
       
       // address must be a single recipient
       Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
       intent.setType(Contacts.CONTENT_ITEM_TYPE);
       if (Mms.isEmailAddress(msgFromTo)) {
           intent.putExtra(ContactsContract.Intents.Insert.EMAIL, msgFromTo);
       } else {
           intent.putExtra(ContactsContract.Intents.Insert.PHONE, msgFromTo);
           intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
           ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
       }
       intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
       this.startActivity(intent); 
       
    }
    //wxj add end
    
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

    //sunzuohua ADD
    private void reSendMms(){
        Log.d("MobilePaperShowActivity","reSendMms");
        mMmsCurrentSize = mSlideModel.getCurrentMessageSize();
        mProgressDlg = null;
        initSendDlg();
        if (0 == mMmsCurrentSize){
            mMmsCurrentSize = 1;
        }           
        mProgressDlg.setMax(mMmsCurrentSize +2*1024); //kB   
        mProgressDlg.setMessage(getString(R.string.message_size_label)
                                 + String.valueOf((mMmsCurrentSize + 2*1024) / 1024)
                                 + getString(R.string.kilobyte));
        mProgressDlg.show();
        mProgressDlg.setTitle(getString(R.string.dialing));
        mProgressDlg.setProgress(0);

        mSendingMessageUri = mUri;
        Intent intent = new Intent(this, TransactionService.class);
        intent.putExtra(TransactionBundle.URI, mUri.toString());
        intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                            Transaction.SEND_TRANSACTION);        
        startService(intent);

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
                            }
                        });   
    }

public static String getMessageDetails(Context context, Cursor cursor, int size) {
    if (cursor == null) {
        return null;
    }

    int type = cursor.getInt(14);
    switch (type) {
        case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
        case PduHeaders.MESSAGE_TYPE_SEND_REQ:
            return getMultimediaMessageDetails(context, cursor, size);
        default:
            Log.w(TAG, "No details could be retrieved.");
            return "";
        }
    }
    

    private static String getMultimediaMessageDetails(
            Context context, Cursor cursor, int size) {

        StringBuilder details = new StringBuilder();
        Resources res = context.getResources();

        long id = cursor.getLong(MessageListAdapter.COLUMN_ID);
        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, id);
        MultimediaMessagePdu msg;

        try {
            msg = (MultimediaMessagePdu) PduPersister.getPduPersister(
                    context).load(uri);
        } catch (MmsException e) {
            Log.e(TAG, "Failed to load the message: " + uri, e);
            return context.getResources().getString(R.string.cannot_get_details);
        }

        // Message Type: Text message.
        details.append(res.getString(R.string.message_type_label));
        details.append(res.getString(R.string.multimedia_message));

        if (msg instanceof RetrieveConf) {
            // From: ***
            String from = MessageUtils.extractEncStr(context, ((RetrieveConf) msg).getFrom());
            details.append('\n');
            details.append(res.getString(R.string.from_label));
            details.append(!TextUtils.isEmpty(from)? from:
                                  res.getString(R.string.hidden_sender_address));
        }

        // To: ***
        details.append('\n');
        details.append(res.getString(R.string.to_address_label));
        EncodedStringValue[] to = msg.getTo();
        if (to != null) {
            details.append(EncodedStringValue.concat(to));
        }
        else {
            Log.w(TAG, "recipient list is empty!");
        }


        // Bcc: ***
        if (msg instanceof SendReq) {
            EncodedStringValue[] values = ((SendReq) msg).getBcc();
            if ((values != null) && (values.length > 0)) {
                details.append('\n');
                details.append(res.getString(R.string.bcc_label));
                details.append(EncodedStringValue.concat(values));
            }
        }

        // Date: ***
        details.append('\n');
        int msgBox = cursor.getInt(MessageListAdapter.COLUMN_MMS_MESSAGE_BOX);
        if (msgBox == Mms.MESSAGE_BOX_DRAFTS) {
            details.append(res.getString(R.string.saved_label));
        } else if (msgBox == Mms.MESSAGE_BOX_INBOX) {
            details.append(res.getString(R.string.received_label));
        } else {
            details.append(res.getString(R.string.sent_label));
        }

        details.append(MessageUtils.formatTimeStampString(
                context, msg.getDate() * 1000L, true));

        // Subject: ***
        details.append('\n');
        details.append(res.getString(R.string.subject_label));

        EncodedStringValue subject = msg.getSubject();
        if (subject != null) {
            String subStr = subject.getString();
            // Message size should include size of subject.
            size += subStr.length();
            details.append(subStr);
        }

        // Priority: High/Normal/Low
        details.append('\n');
        details.append(res.getString(R.string.priority_label));
        details.append(MessageUtils.getPriorityDescription(context, msg.getPriority()));

        // Message size: *** KB
        details.append('\n');
        details.append(res.getString(R.string.message_size_label));
        if(size>MmsConfig.getMaxMessageSize())
           size=MmsConfig.getMaxMessageSize();
        details.append((size+1023)/1024);
        details.append(" KB");

        return details.toString();
    }
    
}
