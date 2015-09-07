/*
 * Copyright (C) 2012 Adam K
 * Modifications Copyright (C) 2012 The CyanogenMod Project
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

package com.android.mms.quickmessage;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.KeyguardManager.OnKeyguardExitResult;
import android.content.Context;
import com.android.mms.MmsApp;

public class ManageKeyguard {
    private static String LOGTAG = "ManageKeyguard";
    private static KeyguardManager sKeyguardManager = null;
    private static KeyguardLock sKeyguardLock = null;

    public static synchronized void initialize() {
        Context context = MmsApp.getApplication();
        if (sKeyguardManager == null) {
            sKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    public static synchronized void disableKeyguard() {
        initialize();

        if (sKeyguardLock == null) {
            sKeyguardLock = sKeyguardManager.newKeyguardLock(LOGTAG);
        }
        sKeyguardLock.disableKeyguard();
    }

    public static synchronized void reenableKeyguard() {
        if (sKeyguardLock != null) {
            sKeyguardLock.reenableKeyguard();
            sKeyguardLock = null;
        }
    }
}
