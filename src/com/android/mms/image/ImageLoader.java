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
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ImageLoader {
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

    public void load(ImageView imageView, String path, int default_id, final int fail_id) {
        if (imageView == null) {
            return;
        }

        if (TextUtils.isEmpty(path) || path.equals("null")) {
            if (default_id > 0) {
                imageView.setImageResource(default_id);
            } else {
                imageView.setImageBitmap(null);
            }
            return;
        }

        // check if the image already download if it is from net
        ImageGetter imageGetter;
        final boolean isNetImage;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            String realPath = getLocalFilePath(path);
            if (realPath.equals(path)) {
                // image not download yet
                isNetImage = true;
            } else {
                path = realPath;
                isNetImage = false;
            }
        } else {
            isNetImage = false;
        }
        final String uri = path;

        Bitmap bitmap = bitmapCache.get(path);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        if (isUriLoading(uri)) {
            // the uri loading
            return;
        }

        final ImageTask imageTask = new ImageTask(uri, imageView, default_id, fail_id);
        ImageLoaderListener listener = new ImageLoaderListener() {

            @Override
            public void onLoaded(final String url, Bitmap bitmap, final ImageView imageView) {
                final Bitmap resultBitmap;
                if (bitmap != null) {
                    if (isNetImage) {
                        // save bitmap to sdcard
                        String savePath = NetImageUtil.saveBitmap(context, uri, bitmap);
                        bitmap = ScaleBitmapDecoder.decodeFile(savePath, 200, 200);
                    }
                    bitmapCache.put(uri, bitmap);
                    resultBitmap = bitmap;
                } else {
                    resultBitmap = null;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (resultBitmap == null) {
                            if (fail_id > 0) {
                                imageView.setImageResource(fail_id);
                            } else {
                                imageView.setImageBitmap(null);
                            }
                            return;
                        }
                        if (imageView.getTag() != null && !imageTask.isCanceled()) {
                            if (String.valueOf(imageView.getTag()).equals(url)) {
                                imageView.setImageBitmap(resultBitmap);
                            }
                        }
                    }
                });
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
        if (default_id > 0) {
            imageView.setImageResource(default_id);
        } else {
            imageView.setImageBitmap(null);
        }
        // executor.execute(imageGetter);
        Future future = executor.submit(imageGetter);
        futureMap.put(uri, future);
    }

    private boolean isUriLoading(String uri) {
        Future future = futureMap.get(uri);
        if (future != null && !future.isDone()) {
            return true;
        } else {
            return false;
        }
    }

    public void cancel(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        path = getLocalFilePath(path);
        Future future = futureMap.get(path);
        if (future != null) {
            future.cancel(true);
            futureMap.remove(path);
        }
    }

    private String getLocalFilePath(String path) {
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
