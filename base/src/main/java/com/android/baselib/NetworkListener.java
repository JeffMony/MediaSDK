package com.android.baselib;

public interface NetworkListener {

    void onAvailable();

    void onWifiConnected();

    void onMobileConnected();

    void onNetworkType();

    void onUnConnected();
}
