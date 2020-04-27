package com.jeffmony.async.future;

public abstract class TransformFuture<T, F> extends SimpleFuture<T> implements FutureCallback<F> {
    public TransformFuture(F from) {
        onCompleted(null, from);
    }

    public TransformFuture() {
    }

    @Override
    public void onCompleted(Exception e, F result) {
        if (isCancelled())
            return;
        if (e != null) {
            error(e);
            return;
        }

        try {
            transform(result);
        }
        catch (Exception ex) {
            error(ex);
        }
    }

    protected void error(Exception e) {
        setComplete(e);
    }

    protected abstract void transform(F result) throws Exception;
}