/*
 * Copyright (c) 2014 pci-suntektech Technologies, Inc.  All Rights Reserved.
 * pci-suntektech Technologies Proprietary and Confidential.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.android.mms.rcs;

import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmojiPackageBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonConstant;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.provider.model.MessageSessionModel;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.android.mms.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RcsEmojiInitialize {

    private Context mContext;

    private ViewStub mViewStub;

    private View mEmojiView = null;

    private GridView mEmojiGridView;

    private GirdViewAdapter mGirdViewAdapter;

    private LinearLayout mLinearLayout;

    private ImageButton mDeleteBtn;

    private String mSelectPackageId = "";

    private ArrayList<EmojiPackageBO> mEmojiPackages = new ArrayList<EmojiPackageBO>();

    private ViewOnClickListener mViewOnClickListener;

    private LinearLayout.LayoutParams mLayoutParams;

    private ArrayList<ImageButton> packageListButton = new ArrayList<ImageButton>();

    public interface ViewOnClickListener {

        public void viewOpenOrCloseListener(boolean isOpen);

        public void emojiSelectListener(EmoticonBO emojiObject);

        public void onEmojiDeleteListener();

        public void addEmojiPackageListener();
    }

    public RcsEmojiInitialize(Context context, ViewStub viewStub,
            ViewOnClickListener viewOnClickListener) {
        this.mContext = context;
        this.mViewStub = viewStub;
        this.mViewOnClickListener = viewOnClickListener;
        mLayoutParams = new LinearLayout.LayoutParams(
                RcsUtils.dip2px(mContext, 45),
                LinearLayout.LayoutParams.MATCH_PARENT);
        mLayoutParams.leftMargin = RcsUtils.dip2px(mContext, 1);
        new LoadSessionTask().execute();
    }

    public void closeOrOpenView() {
        if (mEmojiView == null) {
            RcsUtils.closeKB((Activity) mContext);
            initEmojiView();
            mViewOnClickListener.viewOpenOrCloseListener(true);
            return;
        }
        if (mEmojiView != null && mEmojiView.getVisibility() == View.GONE) {
            RcsUtils.closeKB((Activity) mContext);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEmojiView.setVisibility(View.VISIBLE);
                    mViewOnClickListener.viewOpenOrCloseListener(true);
                }
            }, 200);
        } else {
            mEmojiView.setVisibility(View.GONE);
            RcsUtils.openKB(mContext);
            mViewOnClickListener.viewOpenOrCloseListener(false);
        }
    }

    public void closeViewAndKB() {
        mEmojiView.setVisibility(View.GONE);
        mViewOnClickListener.viewOpenOrCloseListener(false);
    }

    private void initEmojiView() {
        mEmojiView = mViewStub.inflate();
        mEmojiGridView = (GridView) mEmojiView.findViewById(R.id.emoji_grid_view);
        mLinearLayout = (LinearLayout) mEmojiView
                .findViewById(R.id.content_linear_layout);
        mDeleteBtn = (ImageButton) mEmojiView.findViewById(R.id.delete_emoji_btn);
        mDeleteBtn.setVisibility(View.GONE);
        mEmojiView.findViewById(R.id.add_emoji_btn).setOnClickListener(
                mClickListener);
        mDeleteBtn.setOnClickListener(mClickListener);
        mGirdViewAdapter = new GirdViewAdapter(mContext, mViewOnClickListener);
        mEmojiGridView.setNumColumns(4);
        mEmojiGridView.setAdapter(mGirdViewAdapter);
        mDeleteBtn.setVisibility(View.GONE);
    }

    class LoadSessionTask extends AsyncTask<Void, Void, List<EmojiPackageBO>> {
        @Override
        protected List<EmojiPackageBO> doInBackground(Void... params) {
            List<EmojiPackageBO> packageList = new ArrayList<EmojiPackageBO>();
            List<EmojiPackageBO> list = getStorePackageList();
            if (list != null) {
                packageList.addAll(list);
            }
            return packageList;
        }

        @Override
        protected void onPostExecute(List<EmojiPackageBO> result) {
            super.onPostExecute(result);
            mEmojiPackages.clear();
            mEmojiPackages.addAll(result);
            if (mEmojiPackages.size() == 0)
                return;
            EmojiPackageBO emojiPackageBO = mEmojiPackages.get(0);
            mSelectPackageId = emojiPackageBO.getPackageId();
            initPackageView(result);
            setImageButtonCheck(mSelectPackageId);
            mGirdViewAdapter.setEmojiData(mSelectPackageId);
        }

        private ArrayList<EmojiPackageBO> getStorePackageList() {
            ArrayList<EmojiPackageBO> storelist = new ArrayList<EmojiPackageBO>();
            try {
                List<EmojiPackageBO> list = RcsApiManager.getEmoticonApi().queryEmojiPackages();
                if(list != null && list.size() > 0){
                    storelist.addAll(list);
                }
            } catch (ServiceDisconnectedException e) {
                e.printStackTrace();
            }
            return storelist;
        }
    }

    private void initPackageView(List<EmojiPackageBO> packageList) {
        for (int i = 0; i < packageList.size(); i++) {
            EmojiPackageBO emojiPackageBO = packageList.get(i);
            ImageButton imageButton = createImageView(emojiPackageBO);
            mLinearLayout.addView(imageButton);
            packageListButton.add(imageButton);
        }
    }

    private void setImageButtonCheck(String checkId) {
        for (ImageButton imageButton : packageListButton) {
            EmojiPackageBO emojiPackageBO = (EmojiPackageBO) imageButton
                    .getTag();
            if (!emojiPackageBO.getPackageId().equals(checkId))
                imageButton.setBackgroundResource(R.color.gray5);
            else
                imageButton.setBackgroundResource(R.color.white);
        }
    }

    private ImageButton createImageView(EmojiPackageBO emojiPackageBO) {
        ImageButton imageButton = new ImageButton(mContext);
        imageButton.setLayoutParams(mLayoutParams);
        imageButton.setScaleType(ScaleType.CENTER_INSIDE);
        imageButton.setPadding(2, 2, 2, 2);
        RcsEmojiStoreUtil.getInstance().loadImageAsynById(imageButton,
                emojiPackageBO.getPackageId(), RcsEmojiStoreUtil.EMO_PACKAGE_FILE);
        imageButton.setTag(emojiPackageBO);
        imageButton.setOnClickListener(mImageOnClickListener);
        return imageButton;
    }

    private OnClickListener mImageOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            EmojiPackageBO emojiPackageBO = (EmojiPackageBO) view
                    .getTag();
            if (emojiPackageBO == null)
                return;
            if (mSelectPackageId == emojiPackageBO.getPackageId())
                return;
            mSelectPackageId = emojiPackageBO.getPackageId();
            setImageButtonCheck(mSelectPackageId);
            mGirdViewAdapter.setEmojiData(mSelectPackageId);
        }
    };

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.delete_emoji_btn:
                mViewOnClickListener.onEmojiDeleteListener();
                break;
            case R.id.add_emoji_btn:
                mViewOnClickListener.addEmojiPackageListener();
                break;
            default:
                break;
            }
        }
    };

    public class GirdViewAdapter extends BaseAdapter {

        private ArrayList<EmoticonBO> mEmojiObjects = new ArrayList<EmoticonBO>();

        private Context mContext;

        private ViewOnClickListener mViewOnClickListener;

        public GirdViewAdapter(Context context,
                ViewOnClickListener viewOnClickListener) {
            this.mContext = context;
            this.mViewOnClickListener = viewOnClickListener;
        }

        public void setEmojiData(String packageId) {
            try {
                List<EmoticonBO> list = RcsApiManager.getEmoticonApi()
                        .queryEmoticons(packageId);
                if(list != null && list.size() > 0){
                    this.mEmojiObjects.clear();
                    this.mEmojiObjects.addAll(list);
                    this.notifyDataSetChanged();
                }
            } catch (ServiceDisconnectedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCount() {
            return mEmojiObjects.size();
        }

        @Override
        public Object getItem(int position) {
            return mEmojiObjects.size();
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.rcs_emoji_grid_view_item, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) holder.mItemView
                    .getLayoutParams();
            param.height = RcsUtils.dip2px(mContext, 80);
            EmoticonBO bean = mEmojiObjects.get(position);
            holder.title.setText(bean.getEmoticonName());
            RcsEmojiStoreUtil.getInstance().loadImageAsynById(holder.icon,
                        bean.getEmoticonId(), RcsEmojiStoreUtil.EMO_STATIC_FILE);
            holder.mItemView.setTag(bean);
            holder.mItemView.setOnClickListener(mClickListener);
            holder.mItemView.setOnLongClickListener(onLongClickListener);
            return convertView;
        }

        private OnLongClickListener onLongClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                try {
                    EmoticonBO bean = (EmoticonBO)arg0.getTag();
                    byte[] data = RcsApiManager.getEmoticonApi().decrypt2Bytes(bean.getEmoticonId(),
                            EmoticonConstant.EMO_DYNAMIC_FILE);
                    RcsUtils.openPopupWindow(mContext, arg0, data);
                } catch (ServiceDisconnectedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        };

        private OnClickListener mClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                EmoticonBO bean = (EmoticonBO) v.getTag();
                mViewOnClickListener.emojiSelectListener(bean);
            }
        };

        private class ViewHolder {
            RelativeLayout mItemView;

            TextView title;

            ImageView icon;

            public ViewHolder(View convertView) {
                this.title = (TextView) convertView.findViewById(R.id.title);
                this.icon = (ImageView) convertView.findViewById(R.id.icon);
                this.mItemView = (RelativeLayout) convertView
                        .findViewById(R.id.item);
                this.mItemView.setBackgroundResource(R.drawable.rcs_emoji_button_bg);
            }
        }
    }

}
