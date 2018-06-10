package com.jacosro.tasks;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

import static com.jacosro.tasks.TaskExecutors.MAIN_THREAD_EXECUTOR;

/**
 * Class that contains methods to create Tasks
 */
public class TaskFactory {

    /**
     * Creates a simple task that returns the given result
     * @param result The result
     * @param <R> The result type
     * @param <E> The error type (N/A in this case)
     * @return The task
     */
    public static <R, E> Task<R, E> forResult(final R result) {
        return Task.newTask(new TaskExecution<R, E>() {
            @Override
            public void onExecution(@NonNull TaskFinisher<R, E> finish) {
                finish.withResult(result);
            }
        }, MAIN_THREAD_EXECUTOR);
    }

    /**
     * Creates a simple task that returns the given error
     * @param error The error
     * @param <R> The result type (N/A in this case)
     * @param <E> The error type
     * @return The task
     */
    public static <R, E> Task<R, E> forError(final E error) {
        return Task.newTask(new TaskExecution<R, E>() {
            @Override
            public void onExecution(@NonNull TaskFinisher<R, E> finish) {
                finish.withError(error);
            }
        }, MAIN_THREAD_EXECUTOR);
    }




}
