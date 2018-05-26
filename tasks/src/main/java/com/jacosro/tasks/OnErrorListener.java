package com.jacosro.tasks;

@FunctionalInterface
public interface OnErrorListener<E> {
    void onError(E e);
}
