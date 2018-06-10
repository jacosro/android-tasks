package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * This interface provides basic methods for checking the state of a Task and its result
 *
 * @param <R> The task result
 * @param <E> The task error, in case it is cancelled
 */
@Deprecated
public interface ObservableTask<R, E> {

    /**
     * @return Whether the task is running or not
     */
    boolean isRunning();

    /**
     * @return Whether the task has been cancelled or not
     */
    boolean isCancelled();

    /**
     * @return Whether the task is finished successfully or not
     */
    boolean isSuccessful();

    /**
     * @return Whether the task has finished or not
     */
    boolean isComplete();
}
