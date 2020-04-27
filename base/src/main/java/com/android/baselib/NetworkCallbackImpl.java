package com.android.baselib;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import com.android.baselib.utils.LogUtils;

@SuppressLint("NewApi")
public class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

    private NetworkListener mListener;

    public NetworkCallbackImpl(NetworkListener listener) {
        mListener = listener;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        mListener.onAvailable();
    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        LogUtils.e("onCapabilitiesChanged: "+ networkCapabilities);
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                mListener.onWifiConnected();
            } else {
                mListener.onMobileConnected();
            }
        }
    }

    @Override
    public void onLost(@NonNull Network network) {
    }

    @Override
    public void onUnavailable() {
        mListener.onUnConnected();
    }
}