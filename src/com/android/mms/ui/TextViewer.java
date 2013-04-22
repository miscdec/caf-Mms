package com.android.mms.ui;

import com.android.mms.R;
import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import static android.view.View.*;
import android.widget.ScrollView;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.CharacterSets;
import android.text.method.HideReturnsTransformationMethod;
import android.text.util.Linkify;

public class TextViewer extends Activity{
    private final int WC = LinearLayout.LayoutParams.WRAP_CONTENT; 
    private final static String TAG = "TextViewer";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        //setTitle(R.string.view_attachment);
        
        LinearLayout layout = new LinearLayout(this); 
        layout.setOrientation(LinearLayout.VERTICAL); 
        
        ScrollView sv = new ScrollView(this);
        sv.setScrollBarStyle(SCROLLBARS_OUTSIDE_INSET);
        
        TextView textView = new TextView(this);
        textView.setText(getText());
        textView.setTextSize((float)18.0);
        textView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        textView.setAutoLinkMask(Linkify.ALL);
        textView.setLinksClickable(true);
        sv.addView(textView);
        
        LinearLayout.LayoutParams param =  
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, WC); 
        layout.addView(sv, param); 
        setContentView(layout);
    }

    private String getText(){
        Intent intent = getIntent();
        Uri uri = intent.getData();
        InputStream input = null;

        if(intent.getByteArrayExtra("text") != null){
            String text = new String(intent.getByteArrayExtra("text"));
            return text;
        }
        
         try {
            input = getContentResolver().openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;
                int len = fin.available();
                byte[] buffer = new byte[len];
                fin.read(buffer);
                
                //return new EncodedStringValue(intent.getIntExtra("charset", CharacterSets.UTF_8), buffer).getString();                
                String charset = MessageUtils.getTextcodecFromContent(buffer, buffer.length);                
                String text = new String(buffer, 0, len, charset);
                return text;
            }
         }catch (IOException e) {
         }finally{
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return null;
                }
            }
         }
         
        return null;
    }
}

