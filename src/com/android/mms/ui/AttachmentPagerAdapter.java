/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.mms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;

import com.android.mms.R;
import com.android.mms.ui.IconListAdapter.IconListItem;

public class AttachmentPagerAdapter extends PagerAdapter {
    public static final int GRID_COLUMN_COUNT = 3;
    public static final int PAGE_GRID_COUNT   = 6;
    public static final int GRID_ITEM_HEIGHT  = 91;

    private static final String GRID_ITEM_IMAGE = "grid_item_image";
    private static final String GRID_ITEM_TEXT  = "grid_item_text";
    private static final int PAGE_COUNT = 2;

    private Context mContext;
    private ArrayList<GridView> mPagerGridViewViews;
    private OnItemClickListener mGridItemClickListener;

    public AttachmentPagerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup view, int position) {
        View pagerContent = LayoutInflater.from(mContext).inflate(
                R.layout.attachment_selector_pager, view, false);
        bindPagerView(pagerContent, position);
        view.addView(pagerContent);
        return pagerContent;
    }

    private void bindPagerView(View pagerContent, int position) {
        GridView gridView = (GridView) pagerContent.findViewById(R.id.attachment_pager_grid);
        List<IconListItem> attachmentList = AttachmentTypeSelectorAdapter.getData(
                AttachmentTypeSelectorAdapter.MODE_WITH_SLIDESHOW, mContext);
        ArrayList<HashMap<String, Object>> gridDate = new ArrayList<HashMap<String, Object>>();

        if (position == 0) {
            for (int i = 0; i < PAGE_GRID_COUNT; i++) {
                IconListItem item = (IconListItem) attachmentList.get(i);
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(GRID_ITEM_IMAGE, item.getResource());
                map.put(GRID_ITEM_TEXT, item.getTitle());
                gridDate.add(map);
            }
        } else {
            for (int i = PAGE_GRID_COUNT; i < attachmentList.size(); i++) {
                IconListItem item = (IconListItem) attachmentList.get(i);
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(GRID_ITEM_IMAGE, item.getResource());
                map.put(GRID_ITEM_TEXT, item.getTitle());
                gridDate.add(map);
            }
        }
        gridView.setAdapter(new SimpleAdapter(mContext, gridDate, R.layout.attachment_selector_grid,
                new String[] {GRID_ITEM_IMAGE, GRID_ITEM_TEXT},
                new int[] {R.id.attachment_selector_image, R.id.attachment_selector_text}));
        Configuration configuration = mContext.getResources().getConfiguration();
        gridView.setNumColumns((configuration.orientation == configuration.ORIENTATION_PORTRAIT)
                ? GRID_COLUMN_COUNT : GRID_COLUMN_COUNT * 2);
        gridView.setOnItemClickListener(mGridItemClickListener);
        setPagerGridViews(gridView);
    }

    public void setGridItemClickListener(OnItemClickListener l) {
        mGridItemClickListener = l;
    }

    public void setPagerGridViews(GridView gridView) {
        if (mPagerGridViewViews == null) {
            mPagerGridViewViews = new ArrayList<GridView>();
        }
        if (!mPagerGridViewViews.contains(gridView)) {
            mPagerGridViewViews.add(gridView);
        }
    }

    public ArrayList<GridView> getPagerGridViews() {
        return mPagerGridViewViews;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }
}