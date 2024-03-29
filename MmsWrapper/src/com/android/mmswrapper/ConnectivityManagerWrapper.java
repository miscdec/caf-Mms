/*
* Copyright (c) 2017, The Linux Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
* * Redistributions of source code must retain the above copyright
* notice, this list of conditions and the following disclaimer.
* * Redistributions in binary form must reproduce the above
* copyright notice, this list of conditions and the following
* disclaimer in the documentation and/or other materials provided
* with the distribution.
* * Neither the name of The Linux Foundation nor the names of its
* contributors may be used to endorse or promote products derived
* from this software without specific prior written permission.
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

package com.android.mmswrapper;


import android.net.ConnectivityManager;
import android.net.NetworkRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConnectivityManagerWrapper {
    private static final String TAG = "ConnectivityManagerWrapper";

//    public static boolean getMobileDataEnabled(ConnectivityManager cm) {
//        boolean ret = cm.getMobileDataEnabled();
//        LogUtils.logi(TAG, "getMobileDataEnabled=" + ret);
//        return ret;
//    }

    public static boolean getMobileDataEnabled(ConnectivityManager cm){
        String name = "getMobileDataEnabled";

        try {
            Method method = cm.getClass().getMethod(name);
            return (boolean) method.invoke(ConnectivityManagerWrapper.class);
        } catch (Exception e) {
//            throw new RuntimeException(e);
            return false;
        }
    }

    public static void requestNetwork(ConnectivityManager cm, NetworkRequest request,
                                      ConnectivityManager.NetworkCallback networkCallback,
                                      int timeoutMs) {
        LogUtils.logi(TAG, "requestNetwork");
        cm.requestNetwork(request, networkCallback, timeoutMs);
    }
}
