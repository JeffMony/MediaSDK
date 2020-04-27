package com.android.baselib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.baselib.utils.LogUtils;
import com.android.baselib.utils.NetworkUtils;

public class MediaSDKReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.w("Current Network:" + NetworkUtils.getNetworkType(context));
    }
}
