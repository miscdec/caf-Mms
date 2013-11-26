/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Not a Contribution
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.android.mms.R;

/**
 * Demonstrates how to write an efficient list adapter. The adapter used in this example binds
 * to an ImageView and to a TextView for each row in the list.
 *
 * To work efficiently the adapter implemented here uses two techniques:
 * - It reuses the convertView passed to getView() to avoid inflating View when it is not necessary
 * - It uses the ViewHolder pattern to avoid calling findViewById() when it is not necessary
 *
 * The ViewHolder pattern consists in storing a data structure in the tag of the view returned by
 * getView(). This data structures contains references to the views we want to bind data to, thus
 * avoiding calls to findViewById() every time getView() is invoked.
 */

public class WwwContextMenuActivity extends Activity {
    private static final Uri BOOKMARKS_URI = Uri.parse("content://browser/bookmarks");

    private String mUrlString = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        initUi(getIntent());
    }

    private void initUi(Intent intent) {
        Uri uri = intent.getData();
        mUrlString = uri.toString();
        showMenu();
    }

    private void loadURL() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_connect_url);
        builder.setMessage(getString(R.string.loadurlinfo_str));
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, new OnClickListener() {
            @Override
            final public void onClick(DialogInterface dialog, int which) {
                loadUrl(mUrlString);
                WwwContextMenuActivity.this.finish();
            }
        });
        builder.setNegativeButton(R.string.no, new OnClickListener() {
            @Override
            final public void onClick(DialogInterface dialog, int which) {
                WwwContextMenuActivity.this.finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                WwwContextMenuActivity.this.finish();
            }
        });
        builder.show();

    }

    private void loadUrl(String url) {
        if (!url.regionMatches(true, 0, "http://", 0, 7)
                && !url.regionMatches(true, 0, "https://", 0, 8)
                && !url.regionMatches(true, 0, "rtsp://", 0, 7)) {
            url = "http://" + url;
        }
        url = url.replace("Http://","http://");
        url = url.replace("Https://","https://");
        url = url.replace("HTTP://","http://");
        url = url.replace("HTTPS://","https://");
        url = url.replace("Rtsp://","rtsp://");
        url = url.replace("RTSP://","rtsp://");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        if ((url.substring(url.length() - 4).compareToIgnoreCase(".mp4") == 0)
                || (url.substring(url.length() - 4).compareToIgnoreCase(".3gp") == 0)) {
            intent.setDataAndType(Uri.parse(url), "video/*");
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            return;
        }
    }

    private void addToLabel() {
        Intent i = new Intent(Intent.ACTION_INSERT, BOOKMARKS_URI);
        i.putExtra("title", "");
        i.putExtra("url", mUrlString);
        i.putExtra("extend", "outside");
        startActivity(i);
    }

    private void showMenu() {
        final String[] texts = new String[] {
                            getString(R.string.menu_connect_url),
                            getString(R.string.menu_add_to_label),
                            };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.message_options));
        builder.setItems(texts, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    loadURL();
                } else if (which == 1) {
                    addToLabel();
                    WwwContextMenuActivity.this.finish();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                WwwContextMenuActivity.this.finish();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }
}
