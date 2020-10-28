package com.android.player;

public class PlayerAttributes {

    private boolean mVideoCacheSwitch;
    private String mVideoUrl;
    private int mTaskMode;

    public PlayerAttributes(String url) {
        mVideoUrl = url;
    }

    public void setVideoCacheSwitch(boolean videoCacheSwitch) {
        this.mVideoCacheSwitch = videoCacheSwitch;
    }

    public boolean videoCacheSwitch() {
        return mVideoCacheSwitch;
    }

    public void setTaskMode(int mode) {
        mTaskMode = mode;
    }

    public int getTaskMode() {
        return mTaskMode;
    }

    public String getVideoUrl() { return mVideoUrl; }
}
