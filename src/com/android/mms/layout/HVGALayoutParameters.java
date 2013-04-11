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

package com.android.mms.layout;

import android.content.Context;
import android.util.Log;
import android.util.DisplayMetrics;

public class HVGALayoutParameters implements LayoutParameters {
    private static final String TAG = "HVGALayoutParameters";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = true;

    private int mType = -1;

    private static int mImageHeightLandscape;
    private static int mTextHeightLandscape;
    private static int mImageHeightPortrait;
    private static int mTextHeightPortrait;
    private static int mMaxHeight;
    private static int mMaxWidth;
    private static int IMAGE_HEIGHT_LANDSCAPE = 180;
    private static int TEXT_HEIGHT_LANDSCAPE  =  60;
    private static int IMAGE_HEIGHT_PORTRAIT  = 240;
    private static int TEXT_HEIGHT_PORTRAIT   =  80;
	private Context mcontext = null;

    public HVGALayoutParameters(Context context, int type) {
        if ((type != HVGA_LANDSCAPE) && (type != HVGA_PORTRAIT)) {
            throw new IllegalArgumentException(
                    "Bad layout type detected: " + type);
        }

        if (LOCAL_LOGV) {
            Log.v(TAG, "HVGALayoutParameters.<init>(" + type + ").");
        }
        mType = type;
		mcontext=context;

        DisplayMetrics scale = getDefaultDisplay();

		IMAGE_HEIGHT_LANDSCAPE = scale.widthPixels/2;
		TEXT_HEIGHT_LANDSCAPE  = scale.widthPixels/2;
		IMAGE_HEIGHT_PORTRAIT  = scale.heightPixels/2;
		TEXT_HEIGHT_PORTRAIT   = scale.heightPixels/2;

        if (LOCAL_LOGV) {
            Log.v(TAG, "HVGALayoutParameters IMAGE_HEIGHT_LANDSCAPE: " + IMAGE_HEIGHT_LANDSCAPE +
                    " TEXT_HEIGHT_LANDSCAPE: " + TEXT_HEIGHT_LANDSCAPE +
                    " IMAGE_HEIGHT_PORTRAIT: " + IMAGE_HEIGHT_PORTRAIT +
                    " TEXT_HEIGHT_PORTRAIT: " + TEXT_HEIGHT_PORTRAIT);
        }

    }

    public int getWidth() {
        return mType == HVGA_LANDSCAPE ? getDefaultDisplay().heightPixels
                                       : getDefaultDisplay().widthPixels;
    }

    public int getHeight() {
        return mType == HVGA_LANDSCAPE ? getDefaultDisplay().widthPixels
                                       : getDefaultDisplay().heightPixels;
    }

    public int getImageHeight() {
        return mType == HVGA_LANDSCAPE ? IMAGE_HEIGHT_LANDSCAPE
                                       : IMAGE_HEIGHT_PORTRAIT;
    }

    public int getTextHeight() {
        return mType == HVGA_LANDSCAPE ? TEXT_HEIGHT_LANDSCAPE
                                       : TEXT_HEIGHT_PORTRAIT;
    }

    public int getType() {
        return mType;
    }

    public String getTypeDescription() {
        return mType == HVGA_LANDSCAPE ? "HVGA-L" : "HVGA-P";
    }
	private DisplayMetrics getDefaultDisplay(){
		DisplayMetrics dm =mcontext.getResources().getDisplayMetrics();
		
		return dm;
		}
}
