/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2013 The Android Open Source Project
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

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.mms.R;

/**
 * A drawable that encapsulates all the functionality needed to display a letter tile to
 * represent a contact image.
 */
public class LetterTileDrawable extends Drawable {

    private final String TAG = LetterTileDrawable.class.getSimpleName();

    private final Paint mPaint;

    /** Letter tile */
    private static TypedArray sColors;
    private static int sDefaultColor;
    private static int sTileFontColor;
    private static float sLetterToTileRatio;

    /** Reusable components to avoid new allocations */
    private static final Paint sPaint = new Paint();
    private static final Rect sRect = new Rect();
    private static final char[] sFirstChar = new char[1];

    /** Contact type constants */
    public static final int TYPE_PERSON = 1;
    public static final int TYPE_BUSINESS = 2;
    public static final int TYPE_VOICEMAIL = 3;
    public static final int TYPE_DEFAULT = TYPE_PERSON;

    private String mDisplayName;
    private String mIdentifier;
    private int mContactType = TYPE_DEFAULT;
    private float mScale = 1.0f;
    private float mOffset = 0.0f;
    private Context mContext;
    private boolean mIsCircle = false;


    public LetterTileDrawable(final Context context, final Resources res) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);
        mContext = context;

        if (sColors == null) {
            sColors = res.obtainTypedArray(R.array.letter_tile_colors);
            sDefaultColor = res.getColor(R.color.avatar_default_color);
            sTileFontColor = res.getColor(R.color.letter_tile_font_color);
            sLetterToTileRatio = res.getFraction(R.dimen.letter_to_tile_ratio, 1, 1);
            sPaint.setTypeface(Typeface.create(
                    res.getString(R.string.letter_tile_letter_font_family), Typeface.NORMAL));
            sPaint.setTextAlign(Align.CENTER);
            sPaint.setAntiAlias(true);
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect bounds = getBounds();
        if (!isVisible() || bounds.isEmpty()) {
            return;
        }
        // Draw letter tile.
        drawLetterTile(canvas);
    }

    /**
     * Draw the bitmap onto the canvas at the current bounds taking into account the current scale.
     */
    private void drawBitmap(final Bitmap bitmap, final int width, final int height,
            final Canvas canvas) {
        // The bitmap should be drawn in the middle of the canvas without changing its width to
        // height ratio.
        final Rect destRect = copyBounds();

        // Crop the destination bounds into a square, scaled and offset as appropriate
        final int halfLength = (int) (mScale * Math.min(destRect.width(), destRect.height()) / 2);

        destRect.set(destRect.centerX() - halfLength,
                (int) (destRect.centerY() - halfLength + mOffset * destRect.height()),
                destRect.centerX() + halfLength,
                (int) (destRect.centerY() + halfLength + mOffset * destRect.height()));

        // Source rectangle remains the entire bounds of the source bitmap.
        sRect.set(0, 0, width, height);

        canvas.drawBitmap(bitmap, sRect, destRect, mPaint);
    }

    private void drawLetterTile(final Canvas canvas) {
        // Draw background color.
        sPaint.setColor(pickColor(mIdentifier));

        sPaint.setAlpha(mPaint.getAlpha());
        final Rect bounds = getBounds();
        final int minDimension = Math.min(bounds.width(), bounds.height());

        if (mIsCircle) {
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), minDimension / 2, sPaint);
        } else {
            canvas.drawRect(bounds, sPaint);
        }

        // Draw letter/digit only if the first character is an english letter
        if (mDisplayName != null
                && isEnglishLetter(mDisplayName.charAt(0))) {
            // Draw letter or digit.
            sFirstChar[0] = Character.toUpperCase(mDisplayName.charAt(0));

            // Scale text by canvas bounds and user selected scaling factor
            sPaint.setTextSize(mScale * sLetterToTileRatio * minDimension);
            //sPaint.setTextSize(sTileLetterFontSize);
            sPaint.getTextBounds(sFirstChar, 0, 1, sRect);
            sPaint.setColor(sTileFontColor);

            // Draw the letter in the canvas, vertically shifted up or down by the user-defined
            // offset
            canvas.drawText(sFirstChar, 0, 1, bounds.centerX(),
                    bounds.centerY() + mOffset * bounds.height() + sRect.height() / 2,
                    sPaint);
        }
    }

    public int getColor() {
        return pickColor(mIdentifier);
    }

    /**
     * Returns a deterministic color based on the provided contact identifier string.
     */
    private int pickColor(final String identifier) {
        if (TextUtils.isEmpty(identifier) || mContactType == TYPE_VOICEMAIL) {
            return sDefaultColor;
        }
        // String.hashCode() implementation is not supposed to change across java versions, so
        // this should guarantee the same email address always maps to the same color.
        // The email should already have been normalized by the ContactRequest.
        final int color = Math.abs(identifier.hashCode()) % sColors.length();
        return sColors.getColor(color, sDefaultColor);
    }

    public static boolean isEnglishLetterString(final String name) {
        if(name != null) {
            return isEnglishLetter(name.charAt(0));
        }
        return false;
    }

    public static boolean isEnglishLetter(final char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    @Override
    public void setAlpha(final int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.OPAQUE;
    }

    /**
     * Scale the drawn letter tile to a ratio of its default size
     *
     * @param scale The ratio the letter tile should be scaled to as a percentage of its default
     * size, from a scale of 0 to 2.0f. The default is 1.0f.
     */
    public void setScale(float scale) {
        mScale = scale;
    }

    /**
     * Assigns the vertical offset of the position of the letter tile to the ContactDrawable
     *
     * @param offset The provided offset must be within the range of -0.5f to 0.5f.
     * If set to -0.5f, the letter will be shifted upwards by 0.5 times the height of the canvas
     * it is being drawn on, which means it will be drawn with the center of the letter starting
     * at the top edge of the canvas.
     * If set to 0.5f, the letter will be shifted downwards by 0.5 times the height of the canvas
     * it is being drawn on, which means it will be drawn with the center of the letter starting
     * at the bottom edge of the canvas.
     * The default is 0.0f.
     */
    public void setOffset(float offset) {
        mOffset = offset;
    }

    public void setContactDetails(final String displayName, final String identifier) {
        mDisplayName = displayName;
        mIdentifier = identifier;
    }

    public void setContactType(int contactType) {
        mContactType = contactType;
    }

    public void setIsCircular(boolean isCircle) {
        mIsCircle = isCircle;
    }
}
