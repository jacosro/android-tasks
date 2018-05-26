package com.jacosro.tasks;

@FunctionalInterface
public interface OnResultListener<R> {
    void onResult(R r);
}
