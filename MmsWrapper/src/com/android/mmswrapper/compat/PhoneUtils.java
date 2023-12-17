package com.android.mmswrapper.compat;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.android.mmswrapper.ConnectivityManagerWrapper;
import com.android.mmswrapper.SubscriptionManagerWrapper;

public class PhoneUtils {
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static int getPhoneIdAboveP(Context context, int subId) {
        int slotIndex = SubscriptionManager.getSlotIndex(subId);
        int phoneCount = getPhoneCount(context);
        if (phoneCount == 1) return 0;
        return slotIndex;
    }

    public static int getPhoneId(int subId, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getPhoneIdAboveP(context, subId);
        } else {
            return PhoneUtils.getPhoneId(subId, context.getApplicationContext());
        }
    }

    private static int getPhoneCount(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return telephonyManager.getActiveModemCount();
        } else {
            return telephonyManager.getPhoneCount();
        }
        
    }
}
