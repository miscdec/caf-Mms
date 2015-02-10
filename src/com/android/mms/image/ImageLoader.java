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
package com.android.mms.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ImageLoader {
    // private HashMap<String, SoftReference<Bitmap>> bitmapCacheMap;
    private static final int MIN_MEMORY = 10 * 1024 * 1024;// at least 10m
    private LruCache<String, Bitmap> bitmapCache;
    private ExecutorService executor;
    private HashMap<String, Future> futureMap;
    private Context context;
    private Handler handler;

    public ImageLoader(Context context) {
        // bitmapCacheMap = new HashMap<String, SoftReference<Bitmap>>();
        // futureMap = new HashMap<String, Future>();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 8;
        bitmapCache = new LruCache<String, Bitmap>(Math.max(mCacheSize, MIN_MEMORY)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        executor = Executors.newSingleThreadExecutor();
        futureMap = new HashMap<String, Future>();
        this.context = context;
        this.handler = new Handler();
    }

    public void load(ImageView imageView, String path) {
        if (TextUtils.isEmpty(path) || imageView == null) {
            return;
        }

        // check if the image already download if it is from net
        ImageGetter imageGetter;
        boolean isNetImage = false;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            String realPath = getRealPath(path);
            if (realPath.equals(path)) {
                // image not download yet
                isNetImage = true;
            }else{
                path = realPath;
            }
        }
        final String uri = path;

        Bitmap bitmap = bitmapCache.get(path);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        final ImageTask imageTask = new ImageTask(uri, imageView);
        ImageLoaderListener listener = new ImageLoaderListener() {

            @Override
            public void onLoaded(String url, final Bitmap bitmap, final ImageView imageView) {
                if (bitmap == null) {
                    return;
                }
                if (imageView.getTag() != null && !imageTask.isCanceled()) {
                    if (String.valueOf(imageView.getTag()).equals(url)) {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }

                bitmapCache.put(uri, bitmap);
                // save bitmap to sdcard
                NetImageUtil.saveBitmap(context, uri, bitmap);
            }

            @Override
            public boolean onStartLoad() {
                return !imageTask.isCanceled();
            }

            @Override
            public void onEndLoad() {
                futureMap.remove(uri);
            }
        };

        if (isNetImage) {
            imageGetter = new NetImageGetter(imageTask, listener);
        } else {
            imageGetter = new FileImageGetter(imageTask, listener);
        }

        imageView.setTag(uri);
        // executor.execute(imageGetter);
        Future future = executor.submit(imageGetter);
        futureMap.put(uri, future);
    }

    public void cancel(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        path = getRealPath(path);
        Future future = futureMap.get(path);
        if (future != null) {
            future.cancel(true);
            futureMap.remove(path);
        }
    }

    private String getRealPath(String path) {
        String filePath = NetImageUtil.getImgDownloadPath(context)
                + NetImageUtil.getImgNameByUrl(path);
        if (new File(filePath).exists()) {
            path = filePath;
        }
        return path;
    }

    public void destroy() {
        executor.shutdown();
        bitmapCache.evictAll();
        futureMap.clear();
        context = null;
    }
}
