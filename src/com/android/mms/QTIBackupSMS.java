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

/*
   SMS Backup : currently not dealing with settings

   During backup, we use SMSBackup object to keep a copy all messages.
   We serialize and write it to "smsBackup" file. Reverse operation
   during restore. Currently, only a few fields are used to backup.
 */

package com.android.mms;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

class SMSObject implements Serializable{
    private String id;
    private String addr;
    private String msg;
    private String readState;
    private String time;

    String getId() {
        return id;
    }

    String getAddr() {
        return addr;
    }

    String getMsg() {
        return msg;
    }

    String getReadState() {
        return readState;
    }

    String getTime() {
        return time;
    }

    void setId(String id){
        this.id = id;
    }

    void setAddr(String addr){
        this.addr = addr;
    }

    void setMsg(String msg){
        this.msg = msg;
    }

    void setReadState(String readState) {
        this.readState = readState;
    }

    void setTime(String time) {
        this.time = time;
    }
}

class SMSBackup implements Serializable{

    public ArrayList<SMSObject> SMSList;
    public SMSBackup() {
        SMSList = new ArrayList<SMSObject>();
    }
}

public class QTIBackupSMS{

    Cursor cursor;
    String vfile;
    Context mContext;
    private final boolean DEBUG = false;
    private final String TAG = "QTIBackupSMS";
    SMSBackup smsBackupInbox, smsBackupSent;
    FileOutputStream mFileOutputStream;
    FileInputStream mFileInputStream;
    BufferedOutputStream buf;

    public QTIBackupSMS(Context context, String _vfile) {
        mContext = context;
        vfile = _vfile;
        smsBackupInbox = new SMSBackup();
        smsBackupSent = new SMSBackup();
    }

    private enum MsgBox{
        SENT,
        INBOX;
    }

    public void performRestore(){
        SMSBackup smsbk;

        try {
            mFileInputStream = mContext.openFileInput(vfile);
            ObjectInputStream ois = new ObjectInputStream(mFileInputStream);

            //FOR Inbox
            int numOfObjects = ois.readInt();
            if(numOfObjects < 1){
                if(DEBUG) Log.d(TAG, "No messages to restore in Inbox");
            }
            if(DEBUG) Log.d(TAG, "numOfObjects: " + numOfObjects);
            smsbk = (SMSBackup) ois.readObject();
            writeSMS(smsbk, numOfObjects, MsgBox.INBOX);

            //For Sent
            numOfObjects = ois.readInt();
            if(numOfObjects < 1){
                if(DEBUG) Log.d(TAG, "No messages to restore in Inbox");
            }
            if(DEBUG) Log.d(TAG, "numOfObjects: " + numOfObjects);
            smsbk = (SMSBackup) ois.readObject();
            writeSMS(smsbk, numOfObjects, MsgBox.SENT);

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

    /* Write all messages contained in smsbk back to content provider */
    private void writeSMS(SMSBackup smsbk, int numOfObjects, MsgBox box){

        ContentResolver cr = mContext.getContentResolver();
        ContentValues values = new ContentValues();
        //Here we are taking only some fields

        if( android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
          switch( box ){
            case INBOX:
                for(SMSObject smsObj : smsbk.SMSList){

                    values.put(Telephony.Sms.Inbox._ID, smsObj.getId());
                    values.put(Telephony.Sms.Inbox.ADDRESS, smsObj.getAddr());
                    values.put(Telephony.Sms.Inbox.BODY, smsObj.getMsg());
                    values.put(Telephony.Sms.Inbox.DATE_SENT, smsObj.getTime());
                    values.put(Telephony.Sms.Inbox.READ, smsObj.getReadState());

                    cr.insert(Telephony.Sms.Inbox.CONTENT_URI, values);
                }
                break;
            case SENT:
                for(SMSObject smsObj : smsbk.SMSList){

                    values.put(Telephony.Sms.Sent._ID, smsObj.getId());
                    values.put(Telephony.Sms.Sent.ADDRESS, smsObj.getAddr());
                    values.put(Telephony.Sms.Sent.BODY, smsObj.getMsg());
                    values.put(Telephony.Sms.Sent.DATE_SENT, smsObj.getTime());
                    values.put(Telephony.Sms.Sent.READ, smsObj.getReadState());

                    cr.insert(Telephony.Sms.Sent.CONTENT_URI, values);
                }
          }
        }
        else{
          switch( box ){
            case INBOX:
                for(SMSObject smsObj : smsbk.SMSList){

                    values.put("_id", smsObj.getId());
                    values.put("address", smsObj.getAddr());
                    values.put("body", smsObj.getMsg());
                    values.put("date_sent", smsObj.getTime());
                    values.put("read", smsObj.getReadState());

                    cr.insert(Uri.parse("content://sms/inbox"), values);
                }
                break;
            case SENT:
                for(SMSObject smsObj : smsbk.SMSList){

                    values.put("_id", smsObj.getId());
                    values.put("address", smsObj.getAddr());
                    values.put("body", smsObj.getMsg());
                    values.put("date_sent", smsObj.getTime());
                    values.put("read", smsObj.getReadState());

                    cr.insert(Uri.parse("content://sms/sent"), values);
                }
          }

        }
    }

    public void performBackup(){
        int numOfEntriesInbox = getSMSList(MsgBox.INBOX);
        int numOfEntriesSent = getSMSList(MsgBox.SENT);
        writeSMSList(numOfEntriesInbox, numOfEntriesSent);
    }

    /* Returns number of messages retrieved */
    private int getSMSList(MsgBox box){
        SMSObject smsObj;
        cursor = getSmsCursor(box);
        if(cursor == null){
            return -1;
        }

        int numOfEntries = cursor.getCount();
        if( numOfEntries < 1){
            if(DEBUG) Log.d(TAG, "No messages in Your Phone");
            return 0;
        }

        cursor.moveToFirst();
        numOfEntries = 0;
        switch( box ){
            case INBOX:
                for (int i = 0; ; i++) {
                    smsObj = getSMS(cursor);
                    smsBackupInbox.SMSList.add(smsObj);
                    numOfEntries++;

                    if(DEBUG) Log.d(TAG, "SMS " + (i + 1) + smsBackupInbox.SMSList.get(i));
                    if(cursor.isLast()) {
                        break;
                    } else {
                        cursor.moveToNext();
                    }
                }
                break;

            case SENT:
                for (int i = 0; ; i++) {
                    smsObj = getSMS(cursor);
                    smsBackupSent.SMSList.add(smsObj);
                    numOfEntries++;

                    if(DEBUG) Log.d(TAG, "SMS " + (i + 1) + smsBackupSent.SMSList.get(i));
                    if(cursor.isLast()) {
                        break;
                    } else {
                        cursor.moveToNext();
                    }
                }
                break;
        }

        cursor.close();
        return numOfEntries;
    }

    /* writes SMSBackup object to file. Note that we write number of objects at start.*/
    public void writeSMSList(int numOfEntriesInbox, int numOfEntriesSent){
        try {
            mFileOutputStream =  mContext.openFileOutput(vfile, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(mFileOutputStream);
            oos.writeInt(numOfEntriesInbox);
            oos.writeObject( smsBackupInbox );

            oos.writeInt(numOfEntriesSent);
            oos.writeObject( smsBackupSent );

            oos.flush();
            oos.close();
            mFileOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    /* Retrieves and populates SMSObject */
    public SMSObject getSMS(Cursor c){
        SMSObject sms = new SMSObject();

        sms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
        sms.setAddr(c.getString(c.getColumnIndexOrThrow("address")));
        sms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
        sms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
        sms.setReadState(c.getString(c.getColumnIndexOrThrow("read")));

        return sms;
    }

  //Choose sms content provider URI based on API level
  public Cursor getSmsCursor(MsgBox box) {
    ContentResolver cr = mContext.getContentResolver();
    Cursor c = null;
    Uri uri = null;

    if( android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
      switch ( box ){
        case INBOX:
          uri = Uri.parse("content://sms/inbox");
          break;
        case SENT:
          uri = Uri.parse("content://sms/sent");
          break;
      }
    }
    else{
      switch ( box ){
        case INBOX:
          uri = Telephony.Sms.Inbox.CONTENT_URI;
          break;
        case SENT:
          uri = Telephony.Sms.Sent.CONTENT_URI;
          break;
      }
    }

    c = cr.query(uri, null, null, null, null);
    return c;
  }
}
