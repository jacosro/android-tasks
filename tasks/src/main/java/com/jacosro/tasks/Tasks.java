package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.Executor;

import static com.jacosro.tasks.TaskExecutors.MAIN_THREAD_EXECUTOR;

public class Tasks {

    /**
     * Creates a simple task that returns the given result
     * @param result The result
     * @param <R> The result type
     * @param <E> The error type (N/A in this case)
     * @return The task
     */
    @NonNull
    public static <R, E> Task<R, E> forResult(R result) {
        return runOnMainThread(workFinisher -> workFinisher.withResult(result));
    }

    /**
     * Creates a simple task that returns the given error
     * @param error The error
     * @param <R> The result type (N/A in this case)
     * @param <E> The error type
     * @return The task
     */
    @NonNull
    public static <R, E> Task<R, E> forError(E error) {
        return runOnMainThread(workFinisher -> workFinisher.withError(error));
    }

    /**
     * Creates a task that executes the code from TaskWork in a background thread
     * Task will use TaskExecutors.defaultBackgroundThread()
     *
     * @param taskWork The code that will be executed
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskWork in background
     */
    @NonNull
    public static <R, E> Task<R, E> runOnBackgroundThread(@NonNull TaskWork<R, E> taskWork) {
        return new Task<R, E>() {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {
                taskWork.doWork(workFinisher);
            }
        };
    }

    /**
     * Creates a task that executes the code from TaskWork in main thread
     * @param taskWork The code that will be executed
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskWork in main thread
     */
    @NonNull
    public static <R, E> Task<R, E> runOnMainThread(@NonNull TaskWork<R, E> taskWork) {
        return new Task<R, E>(MAIN_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {
                taskWork.doWork(workFinisher);
            }
        };
    }

    /**
     * Creates a task that executes the code from TaskWork
     * @param taskWork The code that will be executed
     * @param executor The executor that will execute the code
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskWork
     */
    @NonNull
    public static <R, E> Task<R, E> runOnExecutor(@NonNull Executor executor, @NonNull TaskWork<R, E> taskWork) {
        return new Task<R, E>(executor) {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {
                taskWork.doWork(workFinisher);
            }
        };
    }

    /**
     * Returns a task that completes successfully if all tasks supplied are completed
     * successfully too. If any of the tasks fail, the returned task will fail too.
     *
     * @param tasks The tasks
     * @return A task which completes successfully if every supplied task completes successfully
     */
    @NonNull
    public static Task<Void, Void> whenAll(Task<?, ?>... tasks) {
        return whenAll(Arrays.asList(tasks));
    }

    /**
     * Returns a task that completes successfully if all tasks supplied are completed
     * successfully too. If any of the tasks fail, the returned task will fail too.
     *
     * @param tasks The tasks
     * @return A task which completes successfully if every supplied task completes successfully
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static Task<Void, Void> whenAll(Collection<? extends Task> tasks) {
        return runOnBackgroundThread(new TaskWork<Void, Void>() {
            @CallTaskFinisher
            @Override
            public void doWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                Stack resultsStack = new Stack();

                for (Task task : tasks) {
                    task.addOnResultListener(new OnResultListener() {
                        @Override
                        public void onResult(Object result) {
                            resultsStack.push(result);

                            if (resultsStack.size() == tasks.size()) {
                                workFinisher.withResult(null);
                            }
                        }
                    }).addOnErrorListener(new OnErrorListener() {
                        @Override
                        public void onError(Object error) {
                            workFinisher.withError(null);
                        }
                    });
                }
            }
        });
    }
}
