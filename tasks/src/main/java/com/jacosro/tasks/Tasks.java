package com.jacosro.tasks;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jacosro.tasks.TaskExecutors.CURRENT_THREAD_EXECUTOR;
import static com.jacosro.tasks.TaskExecutors.MAIN_THREAD_EXECUTOR;
import static com.jacosro.tasks.TaskExecutors.defaultBackgroundExecutor;

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
        return run(workFinisher -> workFinisher.withResult(result));
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
        return run(workFinisher -> workFinisher.withError(error));
    }

    /**
     * Creates a task that executes the code from TaskWork in the current thread
     *
     * @param taskWork The code that will be executed
     * @param <R> The Result type
     * @param <E> The Error type
     * @return A task that will execute the code from TaskWork in the current thread
     */
    @NonNull
    public static <R, E> Task<R, E> run(@NonNull TaskWork<R, E> taskWork) {
        return runOnExecutor(CURRENT_THREAD_EXECUTOR, taskWork);
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
    public static <R, E> Task<R, E> runAsync(@NonNull TaskWork<R, E> taskWork) {
        return runOnExecutor(defaultBackgroundExecutor(), taskWork);
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
        return runOnExecutor(MAIN_THREAD_EXECUTOR, taskWork);
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
                taskWork.onWork(workFinisher);
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
    public static Task<Void, Void> whenAll(Task... tasks) {
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
        return runAsync(new TaskWork<Void, Void>() {
            @CallWorkFinisher
            @Override
            public void onWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                final AtomicInteger count = new AtomicInteger(tasks.size());

                for (Task task : tasks) {
                    task.addOnResultListener(new OnResultListener() {
                        @Override
                        public void onResult(Object result) {
                            if (count.decrementAndGet() == 0) {
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


    @SafeVarargs
    @NonNull
    public static <R> Task<List<R>, Void> whenAllSuccess(Task<R, ?>... tasks) {
        return whenAllSuccess(Arrays.asList(tasks));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <R> Task<List<R>, Void> whenAllSuccess(Collection<? extends Task<R, ?>> tasks) {
        return runAsync(new TaskWork<List<R>, Void>() {
            @Override
            public void onWork(@NonNull WorkFinisher<List<R>, Void> workFinisher) {
                List<R> resultsList = new ArrayList<>(tasks.size());

                for (Task<R, ?> task : tasks) {
                    task.addOnResultListener(new OnResultListener<R>() {
                        @Override
                        public void onResult(R result) {
                            resultsList.add(result);

                            if (resultsList.size() == tasks.size()) {
                                workFinisher.withResult(resultsList);
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
