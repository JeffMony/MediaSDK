package com.android.player;

public class PlayerAttributes {

    private boolean mUseLocalProxy;

    public void setUseLocalProxy(boolean useLocalProxy) {
        this.mUseLocalProxy = useLocalProxy;
    }

    public boolean userLocalProxy() {
        return mUseLocalProxy;
    }
}
