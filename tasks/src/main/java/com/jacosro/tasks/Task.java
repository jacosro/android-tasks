package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;

public interface Task<R, E> {

    /**
     * Adds an OnResultListener to the task that will be executed in main thread when the task completes successfully
     * @param onResultListener the result listener
     * @return the current Task
     */
    @NonNull
    Task<R, E> addOnResultListener(@NonNull OnResultListener<R> onResultListener);

    /**
     * Adds an OnResultListener to the task that will be executed in the given executor when the task completes successfully
     * @param executor the executor in which the listener will be executed
     * @param onResultListener the result listener
     * @return the current Task
     */
    @NonNull
    Task<R, E> addOnResultListener(@NonNull Executor executor, @NonNull OnResultListener<R> onResultListener);

    /**
     * Adds an OnErrorListener to the task that will be executed in main thread when the task completes with an error
     * @param onErrorListener the result listener
     * @return the current Task
     */
    @NonNull
    Task<R, E> addOnErrorListener(@NonNull OnErrorListener<E> onErrorListener);

    /**
     * Adds an OnErrorListener to the task that will be executed in the given executor when the task completes with an error
     * @param executor the executor in which the listener will be executed
     * @param onErrorListener the result listener
     * @return the current Task
     */
    @NonNull
    Task<R, E> addOnErrorListener(@NonNull Executor executor, @NonNull OnErrorListener<E> onErrorListener);

    /**
     * Sets a timeout for the task.
     * If the task does not finish whithin the time specified it is cancelled and the
     * TimeoutCallback is called and executed in main thread
     *
     * @param milliseconds The timeout time in milliseconds
     * @param callback The code to execute after timeout
     */
    void setTimeout(long milliseconds, @Nullable TimeoutCallback callback);

    /**
     * Sets a timeout for the task.
     * If the task does not finish whithin the time specified it is cancelled and the
     * TimeoutCallback is called and executed in the given executor
     *
     * @param milliseconds The timeout time in milliseconds
     * @param executor The executor in which the callback will be executed
     * @param callback The code to execute after timeout
     */
    void setTimeout(long milliseconds, @NonNull Executor executor, @Nullable TimeoutCallback callback);

    /**
     * @return true if the task has finished successfully, false otherwise
     */
    boolean isSuccessful();

    /**
     * @return true if the task has finished with an error, false otherwise
     */
    boolean isFailed();

    /**
     * @return true if the task has finished due to a timeout, false otherwise
     */
    boolean isTimeout();

    /**
     * @return true if the task has been cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * @return true if the task has finished, false otherwise
     */
    boolean isFinished();

    /**
     * Cancels the task
     */
    void cancel();

    /**
     * Returns the result of the task if it finished successfully, null otherwise
     * @return R the result
     */
    @Nullable
    R getResult();

    /**
     * Returns the error of the task in case it was cancelled, null otherwise
     * @return E the error
     */
    @Nullable
    E getError();

}
