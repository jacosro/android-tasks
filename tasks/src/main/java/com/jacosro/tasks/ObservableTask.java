package com.jacosro.tasks;

/**
 * This interface provides basic methods for checking the state of a Task and its result
 *
 * @param <R> The task result
 * @param <E> The task error, in case it is cancelled
 */
public interface ObservableTask<R, E> {

    /**
     * Adds a listener to the task that gets notified when it finishes successfully
     *
     * @param OnResultListener The result listener
     * @return An ObservableTask
     */
    ObservableTask<R, E> addOnResultListener(OnResultListener<R> OnResultListener);

    /**
     * Adds a listener to the task that gets notified when it finishes with an error
     *
     * @param OnErrorListener The error listener
     * @return An ObservableTask
     */
    ObservableTask<R, E> addOnErrorListener(OnErrorListener<E> OnErrorListener);

    /**
     * @return If the task is running
     */
    boolean isRunning();

    /**
     * @return If the task has been cancelled
     */
    boolean isCancelled();

    /**
     * @return If the task has finished
     */
    boolean isComplete();
}
