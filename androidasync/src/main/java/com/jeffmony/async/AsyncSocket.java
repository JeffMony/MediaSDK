package com.jeffmony.async;

public interface AsyncSocket extends DataEmitter, DataSink {
    AsyncServer getServer();
}
