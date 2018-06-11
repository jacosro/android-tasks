package com.jacosro.tasks;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

import static com.jacosro.tasks.TaskExecutors.MAIN_THREAD_EXECUTOR;

public class TaskFactory {

    /**
     * Creates a simple task that returns the given result
     * @param result The result
     * @param <R> The result type
     * @param <E> The error type (N/A in this case)
     * @return The task
     */
    public static <R, E> Task<R, E> forResult(R result) {
        return newMainThreadTask(taskFinisher -> taskFinisher.withResult(result));
    }

    /**
     * Creates a simple task that returns the given error
     * @param error The error
     * @param <R> The result type (N/A in this case)
     * @param <E> The error type
     * @return The task
     */
    public static <R, E> Task<R, E> forError(E error) {
        return newMainThreadTask(taskFinisher -> taskFinisher.withError(error));
    }

    /**
     * Creates a task that executes the code from TaskExecution in a background thread
     * Task will use TaskExecutors.defaultBackgroundThread()
     *
     * @param taskExecution The code that will be executed
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskExecution in background
     */
    public static <R, E> Task<R, E> newTask(@NonNull TaskExecution<R, E> taskExecution) {
        return new Task<R, E>() {
            @Override
            protected void onExecution(TaskExecution.TaskFinisher<R, E> taskFinisher) {
                taskExecution.onExecution(taskFinisher);
            }
        };
    }

    /**
     * Creates a task that executes the code from TaskExecution in main thread
     * @param taskExecution The code that will be executed
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskExecution in main thread
     */
    public static <R, E> Task<R, E> newMainThreadTask(@NonNull TaskExecution<R, E> taskExecution) {
        return new Task<R, E>(MAIN_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(TaskExecution.TaskFinisher<R, E> taskFinisher) {
                taskExecution.onExecution(taskFinisher);
            }
        };
    }

    /**
     * Creates a task that executes the code from TaskExecution
     * @param taskExecution The code that will be executed
     * @param executor The executor that will execute the code
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskExecution
     */
    public static <R, E> Task<R, E> newTask(@NonNull TaskExecution<R, E> taskExecution, @NonNull Executor executor) {
        return new Task<R, E>(executor) {
            @Override
            protected void onExecution(TaskExecution.TaskFinisher<R, E> taskFinisher) {
                taskExecution.onExecution(taskFinisher);
            }
        };
    }
}
