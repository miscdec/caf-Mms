/*
Copyright (c) 2014, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.android.mms;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.pdu.PduParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;


class QMMSBackup implements Serializable{

    public ArrayList<QTIMMSObject> MMSList;
    public QMMSBackup() {
        MMSList = new ArrayList<QTIMMSObject>();
    }
}

public class QTIBackupMMS {
    Cursor cursor;
    String vfile;
    Context mContext;
    private final boolean DEBUG = false;
    private final String TAG = "QTIBackupMMS";
    QMMSBackup mmsBackupSent, mmsBackupInbox;
    FileOutputStream mFileOutputStream;
    FileInputStream mFileInputStream;
    Uri InboxUri = Telephony.Mms.Inbox.CONTENT_URI;
    Uri SentUri = Telephony.Mms.Sent.CONTENT_URI;

    private enum MsgBox{
        SENT,
        INBOX;
    }

    static final String[] MAILBOX_PROJECTION_KK = new String[] {
        BaseColumns._ID,
            Mms.MESSAGE_BOX,
            Mms.MESSAGE_TYPE,
            Mms.READ,
            Mms.DATE,
            Mms.LOCKED,
            Mms.MESSAGE_SIZE,
            "sub_id",
    };

    static final String[] MAILBOX_PROJECTION_JB = new String[] {
        "_id",
        "msg_box",
        "m_type",
        "read",
        "date",
        "locked",
        "m_size"
    };

    static String[] MAILBOX_PROJECTION = MAILBOX_PROJECTION_KK;

    public QTIBackupMMS(Context context, String _vfile) {
        mContext = context;
        vfile = _vfile;
        mmsBackupSent = new QMMSBackup();
        mmsBackupInbox = new QMMSBackup();

        if( android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
          InboxUri = Uri.parse("content://mms/inbox");
          SentUri = Uri.parse("content://mms/sent");

          MAILBOX_PROJECTION = MAILBOX_PROJECTION_JB;
        }
   }

    public void performBackup(){
        int numOfMmsSent = getMmsList(MsgBox.SENT);
        int numOfMmsInbox = getMmsList(MsgBox.INBOX);
        writeMmsList(numOfMmsSent, numOfMmsInbox);
    }

    public void performRestore(){
        QMMSBackup mmsbk;
        try {
            mFileInputStream = mContext.openFileInput(vfile);
            ObjectInputStream ois = new ObjectInputStream(mFileInputStream);

            //For sent messages
            int numOfObjects = ois.readInt();
            mmsbk = (QMMSBackup) ois.readObject();
            if(numOfObjects > 0){
              writeMMS(mmsbk, numOfObjects, SentUri);
            }

            //For inbox messages
            numOfObjects = ois.readInt();
            mmsbk = (QMMSBackup) ois.readObject();
            if(numOfObjects > 0){
                writeMMS(mmsbk, numOfObjects, InboxUri);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (StreamCorruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    /* Read the deserialized object contents and write them to database.
     * MMS data is also restored using PduPersister.
     */
    private void writeMMS(QMMSBackup mmsbk, int numOfObjects, Uri uri) {
        ContentResolver cr = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        PduPersister pdup = PduPersister.getPduPersister(mContext);

        for(QTIMMSObject mmsObj : mmsbk.MMSList){

            byte[] Data = mmsObj.getMmData();
            Uri msgUri = null;
            try {
                if (uri.equals(InboxUri)) {
                    RetrieveConf retrieveConf = (RetrieveConf) new PduParser(
                            Data).parse();
                    msgUri = pdup.persist(retrieveConf, uri,true ,false ,null);
                } else {
                    SendReq retrieveConf = (SendReq) new PduParser(Data).parse();
                    msgUri = pdup.persist(retrieveConf, uri,true ,false ,null);
                }
            } catch (MmsException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            if(DEBUG) Log.d(TAG, msgUri.toString());

            values.put("date", mmsObj.getDate());
            values.put("read", mmsObj.getRead());
            values.put("locked", mmsObj.getLocked());
            values.put("m_size", mmsObj.getMsg_size());

            try{
                int updatecount =
                    mContext.getContentResolver().update(msgUri, values, null, null);
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                return;
            }
        }
    }

    /* Serializes mmsBackup object. Writes it to mmsBackup file */
    private void writeMmsList(int numSent, int numInbox) {
        try {
            mFileOutputStream =  mContext.openFileOutput(vfile, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(mFileOutputStream);
            oos.writeInt(numSent);
            oos.writeObject( mmsBackupSent );
            oos.writeInt(numInbox);
            oos.writeObject( mmsBackupInbox );

            oos.flush();
            oos.close();
            mFileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private int getMmsList(MsgBox Box){
        // The URI will change depending on what we want to backup.
        // Currently only backing up Sent messages
        int numOfEntries = 0;
        Uri uri;
        Cursor cr;

        switch (Box){
            case SENT:
                cr = mContext.getContentResolver().query(SentUri, MAILBOX_PROJECTION, null, null, null);
                numOfEntries = BackupMsg(cr, mmsBackupSent);
                cr.close();
                break;

            case INBOX:
                cr = mContext.getContentResolver().query(InboxUri, MAILBOX_PROJECTION, null, null, null);
                numOfEntries = BackupMsg(cr, mmsBackupInbox);
                cr.close();
                break;
        }

        return numOfEntries;
    }

    /* Iterate over all rows in cursor. Create QTIMMSObject for each row
     * and add them to array list in mmsBacup object.
     * */
    private int BackupMsg(Cursor cr, QMMSBackup mmsBackup) {
        if(cr == null){
            if(DEBUG) Log.d(TAG, "cursor is null");
            return -1;
        }

        if(cr.getCount() == 0){
            if(DEBUG) Log.d(TAG, "No messages");
            return 0;
        }

        QTIMMSObject mmsObj;
        int numOfEntries = 0;

        cr.moveToFirst();
        for (int i = 0; ; i++) {
            mmsObj = exportMms(cr);
            mmsBackup.MMSList.add(mmsObj);
            numOfEntries++;

            if(DEBUG) Log.d(TAG, "MMS " + (i + 1) + mmsBackup.MMSList.get(i));
            if(cr.isLast()) {
                break;
            } else {
                cr.moveToNext();
            }
        }
        return numOfEntries;
    }

    /* Read MMS.Sent table contents to object. */
    private QTIMMSObject exportMms(Cursor c) {
        QTIMMSObject mms = new QTIMMSObject();

        mms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
        mms.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        mms.setLocked(c.getString(c.getColumnIndexOrThrow("locked")));
        mms.setMsg_box(c.getString(c.getColumnIndexOrThrow("msg_box")));
        mms.setMsg_size(c.getString(c.getColumnIndexOrThrow("m_size")));
        mms.setMsg_type(c.getString(c.getColumnIndexOrThrow("m_type")));
        mms.setRead(c.getString(c.getColumnIndexOrThrow("read")));

        //Getting pdu content
        //Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, Long.parseLong( mms.getId() ));

        Uri uri = ContentUris.withAppendedId(Uri.parse("content://mms/"), Long.parseLong( mms.getId() ));
        PduPersister persister = PduPersister.getPduPersister(mContext);
        SendReq sendReq = null;
        RetrieveConf retrieveConf = null;
        byte[] mmData = null;
        PduComposer composer = null;

        //1 represents MMS inbox
        if( Long.parseLong ( mms.getMsg_box()) == 1) {
            try{
                retrieveConf = (RetrieveConf) persister.load(uri);
                composer = new PduComposer(mContext, retrieveConf);
                mmData = composer.make();
            } catch(MmsException e){
                Log.e(TAG, e.getLocalizedMessage());
            } catch(ClassCastException e){
                Log.e(TAG, e.getLocalizedMessage());
            } catch(Exception e){
                Log.e(TAG, e.getLocalizedMessage());
            }
        } else{
            try {
                sendReq = (SendReq) persister.load(uri);
                mmData = new PduComposer(mContext, sendReq).make();
            } catch(MmsException e){
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        mms.setMmData(mmData);
        return mms;
    }
}
