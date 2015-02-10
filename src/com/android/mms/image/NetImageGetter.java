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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetImageGetter extends ImageGetter {

    public NetImageGetter(ImageTask imageTask, ImageLoaderListener listener) {
        super(imageTask, listener);
    }

    @Override
    public void loadImage(String path) {
        Log.i("imageloader", "load net image:" + path);
        imageTask.setLoading(true);
        // load image from net
        // Bitmap bitmap = null;
        // try {
        // bitmap = BitmapFactory.decodeStream(new URL(path).openStream());
        // } catch (MalformedURLException e) {
        // e.printStackTrace();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        Bitmap bitmap = getHttpBitmap(path);
        listener.onLoaded(path, bitmap, imageTask.getImageView());

        imageTask.setLoading(false);
    }

    // 第一种方法
    public Bitmap getHttpBitmap(String path) {
        Bitmap bitmap = null;
        try {
            // 初始化一个URL对象
            URL url = new URL(path);
            // 获得HTTPConnection网络连接对象
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5 * 1000);
            connection.setDoInput(true);
            connection.connect();
            // 得到输入流
            InputStream is = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            // 关闭输入流
            is.close();
            // 关闭连接
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}
