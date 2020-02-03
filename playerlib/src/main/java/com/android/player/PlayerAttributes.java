package com.android.player;

public class PlayerAttributes {

    private boolean mVideoCacheSwitch;

    public void setVideoCacheSwitch(boolean videoCacheSwitch) {
        this.mVideoCacheSwitch = videoCacheSwitch;
    }

    public boolean videoCacheSwitch() {
        return mVideoCacheSwitch;
    }
}
