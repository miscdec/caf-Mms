/**
Author: yinqi
Date  : 2009-4-30
Usage: view attachments in the pdu, provide operations on the attachment, such as save, view, etc.
*/

package com.android.mms.ui;

import com.android.mms.R;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.TextView;
import android.view.Window;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.content.ContentValues;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import com.android.mms.util.ContactInfoCache;
import android.view.KeyEvent;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import java.util.ArrayList;
import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.util.SparseBooleanArray;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.transaction.MessagingNotification;
import com.google.android.mms.MmsException;
import android.content.ContentUris;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.EncodedStringValue;
import com.android.mms.ui.MessageListAdapter;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import android.provider.Telephony.Threads;
import com.google.android.mms.pdu.PduPersister;
import android.os.HandlerThread;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import android.os.Looper;
import android.widget.AdapterView.OnItemClickListener;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import com.android.mms.model.SlideshowModel;
import android.widget.SimpleAdapter;
import com.google.android.mms.ContentType;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.app.AlertDialog;
import android.provider.Settings;
import android.media.MediaPlayer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import android.provider.MediaStore;
import android.provider.MediaStore.*;
import static android.os.FileUtils.*;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract;
import android.accounts.Account;
import java.io.ByteArrayInputStream;
import android.os.storage.StorageManager; 
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

public class AttachmentList extends ListActivity implements OnClickListener{
    private static final String TAG = "AttachmentList";
    private ListView mListView;
    HandlerThread mThread;  
    ProcessHandler mProcessHandler;
    PduBody body;
    private long mMsgId = -1;
    private static final int MENU_SAVE                    = Menu.FIRST;
    private static final int MENU_PLAY                    = Menu.FIRST + 1;
    private static final int MENU_VIEW                    = Menu.FIRST + 2;
    private static final int MENU_SET_AS_WALLPAPER        = Menu.FIRST + 3;
    private static final int MENU_SET_AS_RINGTONE         = Menu.FIRST + 4;
    private static final int MENU_SET_AS_RINGTONE_SUB1         = Menu.FIRST + 5;
    private static final int MENU_SET_AS_RINGTONE_SUB2         = Menu.FIRST + 6;
    private final static int EVENT_SAVE_VCARD_PART        = 1;
    private final static int EVENT_QUIT                   = 2;
    private final static int EVENT_SAVE_VCARD_PART_OK     = 3;

    private final static int RINGTONE_SUB1     = 1;
    private final static int RINGTONE_SUB2     = 2;

    private static final String PART                      = "part";
    private static final String VCARD                      = "vCard";
    private static final String IMAGE                      = "image";
    private static final String AUDIO                      = "audio";
    private static final String VIDEO                      = "video";
    private static final String VCALENDAR               = "vCalendar";
    private PduPart mPart;
    MediaPlayer mAudioPlayer = null;
    private boolean mPlayerReady = false;
    private boolean playingMenu = true;
    private String direction = new String();
    Uri mUri = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        
        setTitle(R.string.view_attachment);
        //mailboxName = subject;
        setContentView(R.layout.attachment_list);
             
        mListView = getListView();
        mListView.setOnCreateContextMenuListener(mMsgListMenuCreateListener);

        Intent intent = getIntent();
        mMsgId = intent.getLongExtra("msg_id", -1);
        mUri = intent.getData();
        boolean unread = intent.getBooleanExtra("unread", false);

        if ( mMsgId < 0 && null == mUri){
            Log.v(TAG, "----------------------mMsgId < 0 && null == mUri");
            finish();
            return;
        } 

        mThread = new HandlerThread(
                        "get pdu part: Process Thread");
        mThread.start();
        mProcessHandler = new ProcessHandler(mThread.getLooper());
        
        List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();

        
        try {
           if(!(mMsgId < 0)){
               Log.v(TAG, "----------------------!(mMsgId < 0)");
               body = SlideshowModel.getPduBody(this, ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId));
           } else if(null != mUri){
               Log.v(TAG, "----------------------(null != mUri");
               body = SlideshowModel.getPduBody(this, mUri);
           }
        } catch (MmsException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        
        boolean showMms = intent.getBooleanExtra("show",false);
        
        int partNum = body.getPartsNum();
        Log.v(TAG, "getPartsNum="+partNum);
        for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());
            HashMap<String, Object> entry = new HashMap<String, Object>();
            Log.v(TAG, "type = "+type);
            //:TODO: add octet-stream support later
            if ( type.equals( ContentType.APP_SMIL ) == false ) {  

                entry.put(PART, part );  
                
                byte[] location = part.getContentLocation();
                if (location == null) {
                    Log.v(TAG,"part.getContentLocation()");
                    location = part.getName();
                }
                
                if (location == null) {
                    Log.v(TAG,"part.getFilename()");
                    location = part.getFilename();
                }                
 
                if (null == location){
                    Log.w(TAG,"can't get file name");
                    location = new String("Unknown").getBytes();
                }
                
                // Depending on the location, there may be an
                // extension already on the name or not
                String fileName = new String(location);
                Log.v(TAG,"------location    fileName  =  "+fileName);
                /*for ( int k = 0; k < fileName.length();k++){
                    Log.v(TAG,fileName.format("char = %x",(int)fileName.charAt(k)));
                }*/
                /*try{
                    //entry.put("name", new EncodedStringValue(CharacterSets.getMibEnumValue(CharacterSets.MIMENAME_UTF_8),fileName.getBytes()).getString());
                    entry.put("name", fileName);
                    entries.add( entry );
                }catch( java.io.UnsupportedEncodingException e ){
                }*/
                entry.put("name", fileName);
                entries.add( entry );
            }else if (showMms){
                Log.v(TAG,"------showMms    viewMmsMessageAttachment");
                finish();
                Uri uri = null;
                if(!(mMsgId < 0)){
                    uri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
                } else if(null != mUri){
                    uri = mUri;
                }
                MessageUtils.viewMmsMessageAttachment(this,
                                                              uri,
                                                              null /* slideshow */, 0 /* persister */, null);
            }
        }

        //get pdu data and set listview adapter
        final SimpleAdapter a = new SimpleAdapter( AttachmentList.this,
                                                   entries,
                                                   R.layout.attachment_view,
                                                   new String[] {PART, "name"},
                                                   new int[] {R.id.attachment_icon, R.id.attachment_name});

        SimpleAdapter.ViewBinder viewBinder = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {

                Log.v(TAG,"------view instanceof ImageView");
                if (view instanceof ImageView) {
                    PduPart part = (PduPart)data;//body.getPart((Integer)data);   
                    String type = new String(part.getContentType());
                    Log.v(TAG,"------setViewValue");

                    if ( type.indexOf("image") != -1 || isImage(part)){                                    
                        Uri uri = part.getDataUri();
                        try
                        {
                            ((ImageView)view).setImageURI(uri);
                        }
                        catch (OutOfMemoryError ex)
                        {
                            Drawable img = getResources().getDrawable(com.android.internal.R.drawable.unknown_image);
                            ((ImageView)view).setImageDrawable(img);
                        }                        
                    }else if ( type.indexOf("video") != -1 || MessageUtils.isVideo(part)){
                        Drawable img = getResources().getDrawable(R.drawable.ic_menu_movie);
                        ((ImageView)view).setImageDrawable(img);
                    }else if ( type.indexOf("audio") != -1 || -1 != type.indexOf("ogg") || MessageUtils.isMusic(part)){
                        Drawable img = getResources().getDrawable(R.drawable.ic_mms_music);
                        ((ImageView)view).setImageDrawable(img);
                    }/*else if ( type.indexOf(VCARD) != -1 || isVcard(part)){
                        Drawable img = getResources().getDrawable(R.drawable.mms_icon_vcard);
                        ((ImageView)view).setImageDrawable(img);
                    }*/else if ( type.indexOf("vCalendar") != -1 || isVcalender(part)){
                        Drawable img = getResources().getDrawable(R.drawable.mms_icon_attach_calendar1);
                        ((ImageView)view).setImageDrawable(img);
                    }else if ( type.indexOf("text") != -1 ){
                        Drawable img = getResources().getDrawable(R.drawable.txt_file);
                        ((ImageView)view).setImageDrawable(img);
                    }else if(type.indexOf("stream") != -1){
                        Log.v(TAG,"------type if file");
                        Drawable img = getResources().getDrawable(com.android.internal.R.drawable.unknown_image);
                        ((ImageView)view).setImageDrawable(img); 
                    }else{
                        //unknown type
                        Log.v(TAG,"------unknown type");
                        Drawable img = getResources().getDrawable(com.android.internal.R.drawable.unknown_image);
                        ((ImageView)view).setImageDrawable(img);
                    }
                    return true;
                }
                return false;
            }
        };

        Log.v(TAG,"------setViewBinder");
        a.setViewBinder(viewBinder);
        mListView.setAdapter(a);
        if (unread && null != mUri){
            ContentValues values = new ContentValues(1);
            values.put(Mms.READ, MessageUtils.MESSAGE_READ);
            SqliteWrapper.update(this, getContentResolver(), 
                mUri, values, null, null);
           // MessagingNotification.nonBlockingUpdateNewMessageIndicator(this);
        }
        setProgressBarIndeterminateVisibility(false);
    }  

    @Override
    protected void onPause() {
        super.onStop();
        
        if ( null != mAudioPlayer && mAudioPlayer.isPlaying()){
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            mPlayerReady = false;
        }
    }    

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mProcessHandler != null)
        {
            mProcessHandler.sendEmptyMessage(EVENT_QUIT);
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
        menu.clear();        

        HashMap<String, Object> entry = (HashMap<String, Object>)mListView.getSelectedItem();

        if ( entry != null ){
            PduPart part = (PduPart)entry.get(PART);

            if ( null != part ){
                
                menu.add(0, MENU_SAVE, 0, R.string.save_btn).setIcon(
                    android.R.drawable.ic_menu_save);
                String type = new String(part.getContentType());

                if ( type.indexOf("audio") != -1 || -1 != type.indexOf("ogg") || MessageUtils.isMusic(part)){
                    if ( null != mAudioPlayer && mAudioPlayer.isPlaying()){
                        menu.add(0, MENU_PLAY, 0, R.string.stop).setIcon(R.drawable.ic_launcher_musicplayer_2);
                    }else{
                        menu.add(0, MENU_PLAY, 0, R.string.play).setIcon(R.drawable.ic_launcher_musicplayer_2);
                    }
                    
                if(isSupportDualPhoneRingTone())
                    {
                        menu.add(0, MENU_SET_AS_RINGTONE_SUB1, 0, R.string.set_as_ringtone1);
                        menu.add(0, MENU_SET_AS_RINGTONE_SUB2, 0, R.string.set_as_ringtone2);
                    }
                else
                        menu.add(0, MENU_SET_AS_RINGTONE, 0, R.string.set_as_ringtone).setIcon(R.drawable.ic_launcher_musicplayer_2);
                }else if ( type.indexOf("image") != -1 || isImage(part)){                                    
                    menu.add(0, MENU_VIEW, 0, R.string.view).setIcon(R.drawable.ic_launcher_video_player);
                }else if ( type.indexOf("video") != -1 || MessageUtils.isVideo(part)){
                    menu.add(0, MENU_PLAY, 0, R.string.play).setIcon(R.drawable.ic_launcher_video_player);
                }
            }
        }
        return true;
    }   
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {        
        HashMap<String, Object> entry = (HashMap<String, Object>)mListView.getSelectedItem();

        if ( entry != null ){
            PduPart part = (PduPart)entry.get(PART);
            
            if ( part != null ){            
                switch (item.getItemId()) {
                    case MENU_SAVE:
                        save(part);
                        break;
                    case MENU_PLAY:
                        playMusic(part);
                        break;
                    case MENU_SET_AS_RINGTONE:
                    case MENU_SET_AS_RINGTONE_SUB1:
                                setRingTone(part,RINGTONE_SUB1);
                                break;
                    case MENU_SET_AS_RINGTONE_SUB2:
                                setRingTone(part,RINGTONE_SUB2);
                                break;
                    case MENU_VIEW:
                        viewImage(part);
                    default:
                        break;
                }
            }
        }
        //return false;
        return true;
    }    

    @Override
    protected void onListItemClick(ListView parent, View v, int position, long id) {
        //Log.v(TAG,"onItemClick " + position + " id= " + id);
        HashMap<String, Object> entry = (HashMap<String, Object>)(parent.getAdapter().getItem(position));
        PduPart part = (PduPart)entry.get(PART);

        playingMenu = true;//reset
        
        if ( null != part ){
            final String type = new String(part.getContentType());

            if ( -1 != type.indexOf( VCARD ) || -1 != type.indexOf("stream")
                                                ||( -1 != type.indexOf( VCALENDAR ) || isVcalender(part))){
                save(part);
            } else if ( -1 != type.indexOf( IMAGE ) || isImage(part)){
                viewImage(part);
            } else if ( -1 != type.indexOf( AUDIO ) || MessageUtils.isMusic(part)){
                playMusic(part);
            } else if ( -1 != type.indexOf( VIDEO ) || MessageUtils.isVideo(part)){
                playVideo(part);
            } else if ( -1 != type.indexOf( "text/plain" ) ){
                viewText(part);
            }
        }
    }

    
    private void confirmImportDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, listener);
        builder.setNegativeButton(R.string.no, null);
        builder.setMessage(R.string.import_contact);
        builder.show();
    }
    
    private final OnCreateContextMenuListener mMsgListMenuCreateListener =
        new OnCreateContextMenuListener() 
    {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) 
        {
            menu.clear();
            menu.setHeaderTitle(R.string.attachment_options);
            menu.add(0, MENU_SAVE, 0, R.string.save_btn);
            AdapterView.AdapterContextMenuInfo info =
                        (AdapterView.AdapterContextMenuInfo) menuInfo;
            
            //HashMap<String, Object> entry = (HashMap<String, Object>)mListView.getSelectedItem();
            HashMap<String, Object> entry = (HashMap<String, Object>)(((ListView)v).getAdapter().getItem(info.position));
            Log.v(TAG,"onCreateContextMenu " + info.position + " entry= " + entry);
            PduPart part = (PduPart)entry.get(PART);
            mPart = part;
            
            if ( null != part ){
                String type = new String(part.getContentType());

                if ( type.indexOf("audio") != -1 || -1 != type.indexOf("ogg")){
                    if ( null != mAudioPlayer && mAudioPlayer.isPlaying()){
                        menu.add(0, MENU_PLAY, 0, R.string.stop);
                        playingMenu = false;
                    }else{
                        menu.add(0, MENU_PLAY, 0, R.string.play);
                        playingMenu = true;
                    }
                if(isSupportDualPhoneRingTone())
                {
                        menu.add(0, MENU_SET_AS_RINGTONE_SUB1, 0, R.string.set_as_ringtone1);
                        menu.add(0, MENU_SET_AS_RINGTONE_SUB2, 0, R.string.set_as_ringtone2);
                }
                else
                        menu.add(0, MENU_SET_AS_RINGTONE, 0, R.string.set_as_ringtone);
                }else if ( type.indexOf("image") != -1 ){                                    
                    menu.add(0, MENU_VIEW, 0, R.string.view);
                }else if ( type.indexOf("video") != -1 ){
                    menu.add(0, MENU_PLAY, 0, R.string.play);
                }
            }        
        }
    };

    /**
    check whether the part contains image media
    */
    private boolean isImage(PduPart part){
        String ct = new String(part.getContentType());

        //we only supervise the type of application/oct-stream
        if (!ct.equals("application/oct-stream")
            && !ct.equals("application/octet-stream")){
            return false;
        }
        
        //jpg|png|gif|vnd.wap.wbmp|jpeg|bmp|wbmp
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

        //jpg|png|gif|vnd.wap.wbmp|jpeg|bmp|wbmp
        if (name.contains(".jpg")
            || name.contains(".png")
            || name.contains(".wbmp")
            || name.contains(".jpeg")
            || name.contains(".bmp")
            || name.contains(".vnd.wap.wbmp")){
            return true;
        }

        return false;
    }

    /**
    check whether the part contains vcard media
    */
    private boolean isVcard(PduPart part){
        String ct = new String(part.getContentType());

        //we only supervise the type of application/oct-stream
        if (!ct.equals("application/oct-stream")
            && !ct.equals("application/octet-stream")){
            return false;
        }
        
        //jpg|png|gif|vnd.wap.wbmp|jpeg|bmp|wbmp
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

        //vcf
        if (name.contains(".vcf")){
            return true;
        }

        return false;
    }

    /**
    check whether the part contains vcalender media
    */
    private boolean isVcalender(PduPart part){
        String ct = new String(part.getContentType());

        //we only supervise the type of application/oct-stream
        if (!ct.equals("application/oct-stream")
            && !ct.equals("application/octet-stream")){
            return false;
        }
        
        //jpg|png|gif|vnd.wap.wbmp|jpeg|bmp|wbmp
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

        //vcs
        if (name.contains(".vcs")){
            return true;
        }

        return false;
    }
            
    /**
     *save attachment according to its type
     *@part  pdu part
     */
    private void save(PduPart part){
        String type = new String(part.getContentType());
        boolean success = false;
        int tip = R.string.copy_to_sdcard_direction_success;
        

        success = copyPart( part );
        String playDirection = direction;
        if(playDirection.startsWith("/sdcard/"))
        {
            String sdString = getString(R.string.Directory_NAME_ANDROID);
            playDirection = playDirection.replace("/sdcard/",sdString + "/");
        }

        
        if ( true == success ){
            direction = getResources().getString(tip) + playDirection;
            Toast.makeText( this, direction, Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText( this, R.string.copy_to_sdcard_fail, Toast.LENGTH_LONG).show();
        }
    }

    /**
     *view image
     */
    private void viewImage(PduPart part){        
        Intent openMmsIntent = new Intent();
        openMmsIntent.setAction(Intent.ACTION_VIEW);
        openMmsIntent.setType(new String(part.getContentType()));
        Log.v(TAG, "viewImage uri="+part.getDataUri());
        openMmsIntent.setData(part.getDataUri());
        openMmsIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(openMmsIntent);
    }

    /**
    *set attachment as ringtone
    */
    private void setRingTone(PduPart part,int sub){
        try {
            ContentValues cv = new ContentValues();
            //cv.put(MediaStore.Audio.Media.IS_MUSIC, "1");
            
            byte[] location = part.getName();
            if (location == null) {
                        location = part.getContentLocation();
            }
            if (location == null) {
                        location = part.getFilename();
            }
            
            if (null == location){
                        Log.w(TAG,"can't get file name");
                        location = new String("Unknown").getBytes();
            }
                            
            cv.put(MediaStore.Audio.Media.TITLE, new String(location));            
            cv.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
            //cv.put(MediaStore.Audio.Media.IS_ALARM, "1");
            cv.put(MediaStore.Audio.Media.MIME_TYPE, part.getContentType());
            Uri base = part.getDataUri();
            Log.v(TAG,"base uri = "+base);
            ContentResolver resolver = getContentResolver();
            Cursor c = resolver.query(base, new String[]{"_data"}, null, null, null);            
            
            String path;
            // Depending on the location, there may be an
            // extension already on the name or not
            String fileName = new String(location);
            String dir = "/sdcard/download/";
            if(MessageUtils.sdcardCanuse())
                        dir = "/sdcard/download/";
            else
            {
                dir = Environment.getInternalStorageDirectory() + "/"+"download/";
                File dirFile1=new File(dir);
                if(!dirFile1.exists()){
                    if(!dirFile1.mkdirs()){
                        return ;
                }
                }

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

            if(fileName.contains("/") || fileName.contains("*") || fileName.contains("?")
                        || fileName.contains("\\") || fileName.contains("<") || fileName.contains(">")
                        || fileName.contains("|") || fileName.contains(":")){
                        fileName ="rename";
            }

            File file = getUniqueDestination(dir + fileName, extension);

            // make sure the path is valid and directories created for this file.
            File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                Log.e(TAG, "[MMS] copyPart: mkdirs for " + parentFile.getPath() + " failed!");
                Toast.makeText(this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if ( c != null && 1 == c.getCount()){
                c.moveToFirst();
                path = c.getString(0);
                c.close();
                
                FileInputStream input = null;
                FileOutputStream output = null;
                boolean ok = false;
                String outPath = file.getAbsolutePath();                
                try{
                    //FileUtils.setPermissions("/data/message/databases/mmssms.db", FileUtils.S_IRWXO, -1, -1);
                    input = new FileInputStream(path);
                    output= new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    int len = 0;
                    while((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                    output.flush();  
                    ok = true;                    
                }catch(java.io.FileNotFoundException e){
                    Log.v(TAG,"write fail" + e);
                }catch(java.io.IOException e){
                    Log.v(TAG,"write fail" + e);
                }finally{
                    try {
                        if (output != null) output.close();
                    } catch (java.io.IOException e) {
                        Toast.makeText( this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error closing ", e);
                        return;
                    }
                    try {
                        if (input != null) input.close();
                    } catch (java.io.IOException e) {
                        Toast.makeText( this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error closing", e);
                        return;
                    }

                    if ( false == ok ){
                        Toast.makeText( this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                //android.os.FileUtils.setPermissions(outPath, S_IROTH | S_IRGRP | S_IRUSR, -1, -1);
                cv.put(MediaStore.Audio.Media.DATA, outPath);
                Log.v(TAG,"copy ok");
            }else{
                Toast.makeText( AttachmentList.this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Uri result = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv);
            if (result == null) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.error_mediadb_new_record)
                    .setPositiveButton(R.string.alert_dialog_ok, null)
                    .setCancelable(false)
                    .show();
                return;
            }
            Log.v(TAG,"result uri="+result);
            if(sub==RINGTONE_SUB1)
                        {
            if ( Settings.System.putString(getContentResolver(), Settings.System.RINGTONE, result.toString()) ){
                Toast.makeText( AttachmentList.this, R.string.setRingtone_Ok, Toast.LENGTH_SHORT).show();
            }else{
                //failed to set the ring tone
                Toast.makeText( AttachmentList.this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
            } 
                        }
            else
                        {
            if ( Settings.System.putString(getContentResolver(), "ringtone_2", result.toString()) ){
                Toast.makeText( AttachmentList.this, R.string.setRingtone_Ok, Toast.LENGTH_SHORT).show();
            }else{
                //failed to set the ring tone
                Toast.makeText( AttachmentList.this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
            } 
                        }
                                    
        } catch (UnsupportedOperationException ex) {
            // most likely the card just got unmounted
            Log.e(TAG, "couldn't set ringtone flag");
            Toast.makeText( AttachmentList.this, R.string.setRingtone_failed, Toast.LENGTH_SHORT).show();
        }          
    }

    /**
    * play music
    */
    private void playMusic(PduPart part){       

        if ( false == mPlayerReady ){
            mAudioPlayer = new MediaPlayer();
            mPlayerReady = true;
            mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                    public void onCompletion(MediaPlayer mp){
                        //wxj release the mediaplayer when completed
                        Log.v(TAG,"play completed");
                        mp.stop();
                        mp.release();
                        mPlayerReady = false;
                        mAudioPlayer = null;
                    }
                });
        }
        
        if ( mPlayerReady && mAudioPlayer.isPlaying() ){
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            mPlayerReady = false;
            return;
        }

        if (false == playingMenu){
            return;
        }
        
        try{                                
            Log.v(TAG,"part.DataUri()="+part.getDataUri());
            mAudioPlayer.setDataSource(this, part.getDataUri());
            mAudioPlayer.setAudioStreamType(android.media.AudioSystem.STREAM_MUSIC );
            mAudioPlayer.prepare();
            mAudioPlayer.start();
            
            Thread.sleep(50);
            //mAudioPlayer.prepareAsync();
        }catch(java.io.IOException e){
            Log.e(TAG,"exception " + e);
            Toast.makeText(this, R.string.cannot_play_audio, Toast.LENGTH_SHORT).show();
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            mPlayerReady = false;
        }catch (IllegalArgumentException ex) {
            // TODO: notify the user why the file couldn't be opened
            Log.e(TAG,"exception " + ex);
            Toast.makeText(this, R.string.cannot_play_audio, Toast.LENGTH_SHORT).show();
            mAudioPlayer.stop();
            mAudioPlayer.release();
            mAudioPlayer = null;
            mPlayerReady = false;
        }
        catch (Exception e)
        {
                    e.printStackTrace();
        }                                    
    }

    /**
    *play video
    */
    private void playVideo(PduPart part){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String type = new String(part.getContentType());
        intent.setDataAndType(part.getDataUri(), type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void viewText(PduPart part){
        Intent intent = new Intent();
        intent.putExtra("text",part.getData());
        intent.setData(part.getDataUri());
        intent.setClass(this, TextViewer.class);
        startActivity(intent);
    }
    
    /**
    *dialog handler
    */
    public void onClick(DialogInterface dialog, int whichButton){
        mProcessHandler.sendEmptyMessage(EVENT_SAVE_VCARD_PART);         
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) 
    {                
        switch (item.getItemId()) {
            case MENU_SAVE:
                save(mPart);
                break;
            case MENU_PLAY:
                final String type = new String(mPart.getContentType());
                
                if (-1 != type.indexOf( VIDEO ) || MessageUtils.isVideo(mPart)){
                    playVideo(mPart);
                }else{
                    playMusic(mPart);
                }
                break;
            case MENU_SET_AS_RINGTONE:
            case MENU_SET_AS_RINGTONE_SUB1:
                        setRingTone(mPart,RINGTONE_SUB1);
                        break;
            case MENU_SET_AS_RINGTONE_SUB2:
                        setRingTone(mPart,RINGTONE_SUB2);
                        break;
            case MENU_VIEW:
                viewImage(mPart);
            default:
                break;
        }
        
        return super.onContextItemSelected(item);
    }    

    
    private File getUniqueDestination(String base, String extension) {
        File file;
        
        if (null != extension){
            file = new File(base + "." + extension);
            for (int i = 2; file.exists(); i++) {
                file = new File(base + "_" + i + "." + extension);
            }
        }else{
            file = new File(base);
            for (int i = 2; file.exists(); i++) {
                file = new File(base + "_" + i);
            }
        }        
        
        return file;
    }

    private boolean copyPart(PduPart part) {
        Uri uri = part.getDataUri();
        String dir ;
        InputStream input = null;
        FileOutputStream fout = null;
        String mimeType = new String(part.getContentType());
        try { 
                byte[] location = part.getContentLocation();
                if (location == null) {
                    location = part.getName();
                }
                if (location == null) {
                    location = part.getFilename();
                }

                if (null == location){
                    Log.w(TAG,"can't get file name");
                    location = new String("Unknown").getBytes();
                }
                                                
                // Depending on the location, there may be an
                // extension already on the name or not
                //wxj modify 
                String fileName = new String(location);
                String subPath;

                if(mimeType.startsWith("image")){
                    subPath = "/Picture/";
                }else if(mimeType.startsWith("audio") || MessageUtils.isMusic(part)){
                    subPath = "/Audio/";
                }else if(mimeType.startsWith("video") || MessageUtils.isVideo(part)){
                    subPath = "/Video/";
                }else if(-1 != mimeType.indexOf(VCALENDAR)){
                    subPath = "/Other/vCalendar/";
                    final String dir_vcal_path="/sdcard/Other/vCalendar/";
                    File dirFile=new File(dir_vcal_path);
                    if(!dirFile.exists()){
                        if(!dirFile.mkdirs()){
                            //
                            return false;
                        }
                    }
                }else{
                    subPath = "/Other/";
                }
                if(MessageUtils.sdcardCanuse())
               //  dir = "/sdcard" + subPath;
                                                
                                                dir = Environment.getExternalStorageDirectory() + "/"
                                                                           + Environment.DIRECTORY_DOWNLOADS  + "/";
                else
                {
                     dir = Environment.getInternalStorageDirectory() + "/"+subPath;
                    File dirFile1=new File(dir);
                        if(!dirFile1.exists()){
                        if(!dirFile1.mkdirs()){
                            return false;
                        }
                    }

                }
                    
                /*
                String fileName = new String(location);
                String dir = "/sdcard/download/";
                */
                String extension;
                int index;
                if ((index = fileName.indexOf(".")) == -1) {
                    String type = new String(part.getContentType());
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                } else {
                    extension = fileName.substring(index + 1, fileName.length());
                    fileName = fileName.substring(0, index);
                }
                if(fileName.contains("/") || fileName.contains("*") || fileName.contains("?")
                            || fileName.contains("\\") || fileName.contains("<") || fileName.contains(">")
                            || fileName.contains("|") || fileName.contains(":")){
                            fileName ="rename";
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

                if(mimeType.startsWith("text/plain")){
                    fout.write(part.getData());
                    // Notify other applications listening to scanner events
                    // that a media file has been added to the sd card
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)));
                } else{
                    input = getContentResolver().openInputStream(uri);
                    if (input instanceof FileInputStream) {
                        FileInputStream fin = (FileInputStream) input;
                        byte[] buffer = new byte[8000];
                        int len = 0;
                        while((len = fin.read(buffer)) != -1) {
                            fout.write(buffer, 0, len);
                        }

                        // Notify other applications listening to scanner events
                        // that a media file has been added to the sd card
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(file)));
                    }
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

    public Handler mToastHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            int event = msg.arg1;
            String tips = "";
            switch (event)
            {
                case EVENT_SAVE_VCARD_PART_OK:
                    Toast.makeText(AttachmentList.this, 
                        R.string.save_contact_ok, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
       

    private class ProcessHandler extends Handler {

        public ProcessHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_QUIT:
                    getLooper().quit();
                    return;
            }
        }

                        
                        
        private ArrayList<String> stringToArrays(String strr){
            String strtemp = strr;
            ArrayList<String> strArrays = new ArrayList<String>();
                                    strArrays.clear();
                                    
            while(strtemp.contains("END:VCARD")){
                int b = strtemp.length();
                int a = strtemp.indexOf("END:VCARD");
                String strtemp2 = strtemp.substring(0,a+9);
                strArrays.add(strtemp2);
                strtemp = strtemp.substring(a+9,b);
            }
            return strArrays;
        }
    }    
            static boolean isSupportDualPhoneRingTone() {
                                    boolean isSupprotDualRingtongeFlag = false;//TelephonyManager.isMultiSimEnabled();
                                    Object localObjectTelephone = ReflectHelper.callStaticMethod(
                                                            "android.telephony.TelephonyManager", "isMultiSimEnabled",
                                                            null, null);
                                    if(localObjectTelephone != null && localObjectTelephone.toString().equals("true")){
                                                isSupprotDualRingtongeFlag = true;
                                    }
                                    return isSupprotDualRingtongeFlag;
                        }
}

