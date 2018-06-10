package com.jacosro.tasks;

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
        return newTask(callback -> callback.withResult(result), MAIN_THREAD_EXECUTOR);
    }

    /**
     * Creates a simple task that returns the given error
     * @param error The error
     * @param <R> The result type (N/A in this case)
     * @param <E> The error type
     * @return The task
     */
    public static <R, E> Task<R, E> forError(final E error) {
        return newTask(callback -> callback.withError(error), MAIN_THREAD_EXECUTOR);
    }

    /**
     * Creates a new task that will execute the code inside TaskExecution
     * @param taskExecution The code that will be executed
     * @param <R> The result type
     * @param <E> The error type
     * @return A Task that will execute the code
     */
    public static <R, E> Task<R, E> newTask(TaskExecution<R, E> taskExecution) {
        return new BaseTask<>(taskExecution);
    }

    /**
     * Creates a new task that will execute the code inside TaskExecution
     * @param taskExecution The code that will be executed
     * @param executor The executor who will execute the code
     * @param <R> The result type
     * @param <E> The error type
     * @return A Task that will execute the code
     */
    public static <R, E> Task<R, E> newTask(TaskExecution<R, E> taskExecution, Executor executor) {
        return new BaseTask<>(taskExecution, executor);
    }
}
