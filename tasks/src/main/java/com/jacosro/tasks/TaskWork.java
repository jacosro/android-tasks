package com.jacosro.tasks;

import android.support.annotation.NonNull;

@FunctionalInterface
public interface TaskWork<R, E> {

    @CallWorkFinisher
    void onWork(@NonNull WorkFinisher<R, E> workFinisher);

    /**
     * The callback for the execution. To finish a task, this callback must be called with an error
     * or with a result
     *
     * @param <R> The result of the execution
     * @param <E> The possible error of the execution
     */
    interface WorkFinisher<R, E> {
        void withResult(R result);
        void withError(E error);
    }

}
