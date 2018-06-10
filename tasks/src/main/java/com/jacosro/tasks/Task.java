package com.jacosro.tasks;

import android.support.annotation.NonNull;

public interface Task<R, E> {

    /**
     * Adds a listener to the task that gets notified when it finishes successfully
     *
     * @param onResultListener The result listener
     * @return An Task
     */
    @NonNull
    Task<R, E> addOnResultListener(OnResultListener<R> onResultListener);

    /**
     * Adds a listener to the task that gets notified if the task is cancelled
     *
     * @param onErrorListener The error listener
     * @return An Task
     */
    @NonNull
    Task<R, E> addOnErrorListener(OnErrorListener<E> onErrorListener);


    /**
     * Sets up a timeout for the task
     * @param timeoutCallback the code that will be executed when the callback gets triggered
     */
    Task<R, E> setTimeout(@NonNull TimeoutCallback timeoutCallback);
}
