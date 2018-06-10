package com.jacosro.tasks;

import android.support.annotation.NonNull;

/**
 * Represents the execution of a task.
 * This class is a handler for the code that will be executed
 *
 * To finish the execution of the task, the TaskFinisher must be called. It is just a callback
 * who executes the onFinish method of the ITask
 *
 * @param <R> The result of the execution
 * @param <E> The possible error of the execution
 */
@FunctionalInterface
public interface TaskExecution<R, E> {
    void onExecution(@NonNull TaskFinisher<R, E> finish);

    /**
     * The callback for the execution. To finish a task, this callback must be called with an error
     * or with a result
     *
     * @param <R> The result of the execution
     * @param <E> The possible error of the execution
     */
    interface TaskFinisher<R, E> {
        void withResult(R result);
        void withError(E error);
    }
}
