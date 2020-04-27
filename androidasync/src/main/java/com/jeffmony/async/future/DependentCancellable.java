package com.jeffmony.async.future;

public interface DependentCancellable extends Cancellable {
    boolean setParent(Cancellable parent);
}
