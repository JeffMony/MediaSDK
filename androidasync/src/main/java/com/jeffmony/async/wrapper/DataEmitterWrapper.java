package com.jeffmony.async.wrapper;

import com.jeffmony.async.DataEmitter;

public interface DataEmitterWrapper extends DataEmitter {
    DataEmitter getDataEmitter();
}
