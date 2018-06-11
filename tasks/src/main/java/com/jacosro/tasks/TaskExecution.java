package com.jacosro.tasks;

import android.support.annotation.NonNull;

@FunctionalInterface
public interface TaskExecution<R, E> {
    void onExecution(TaskFinisher<R, E> taskFinisher);

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
