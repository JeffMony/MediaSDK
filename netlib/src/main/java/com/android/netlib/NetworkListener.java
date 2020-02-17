package com.android.netlib;

public interface NetworkListener {

    void onAvailable();

    void onWifiConnected();

    void onMobileConnected();

    void onNetworkType();

    void onUnConnected();
}
