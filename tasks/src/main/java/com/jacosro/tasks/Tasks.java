package com.jacosro.tasks;

import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
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
        ExecutableTask<R, E> task = new ExecutableTask<R, E>(CURRENT_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {}
        };

        task.setResult(result);
        return task;
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
        ExecutableTask<R, E> task = new ExecutableTask<R, E>(CURRENT_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {

            }
        };

        task.setError(error);
        return task;
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
        return new ExecutableTask<R, E>(executor) {
            @Override
            protected void onExecution(TaskWork.WorkFinisher<R, E> workFinisher) {
                taskWork.onWork(workFinisher);
            }
        }.execute();
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
        final Executor executor = defaultBackgroundExecutor();

        return runAsync(new TaskWork<Void, Void>() {
            @CallWorkFinisher
            @Override
            public void onWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                final AtomicInteger count = new AtomicInteger(tasks.size());

                for (Task task : tasks) {
                    task.addOnResultListener(executor, new OnResultListener() {
                        @Override
                        public void onResult(Object result) {
                            if (count.decrementAndGet() == 0) {
                                workFinisher.withResult(null);
                            }
                        }
                    }).addOnErrorListener(executor, new OnErrorListener() {
                        @Override
                        public void onError(Object error) {
                            workFinisher.withError(null);
                        }
                    });
                }
            }
        });
    }

    /**
     * Schedules a Task that will be executed in the time specified in milliseconds.
     *
     * This method returns a Task that may be cancelled, so the TaskWork will not be executed.
     * The returned Task is not related to the task that will execute the given TaskWork. They are
     * different Tasks.
     *
     * The Task that will execute the TaskWork is not suscriptable to events, that is why it
     * must return a Void result and a Void error.
     *
     * @param millis The time in millis until the Task is going to be run
     * @param taskWork The code to execute in a new Task
     * @return A task that waits and then calls a new task that executes the taskWork
     */
    public static Task<Void, Void> schedule(long millis, @NonNull TaskWork<Void, Void> taskWork) {
        return runAsync(new TaskWork<Void, Void>() {
            @Override
            public void onWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                SystemClock.sleep(millis);

                workFinisher.withResult(null);
            }
        }).addOnResultListener(defaultBackgroundExecutor(), new OnResultListener<Void>() {
            @Override
            public void onResult(Void result) {
                runAsync(taskWork);
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
        final Executor executor = TaskExecutors.defaultBackgroundExecutor();

        return runOnExecutor(executor, new TaskWork<List<R>, Void>() {
            @Override
            public void onWork(@NonNull WorkFinisher<List<R>, Void> workFinisher) {
                Object[] resultsArray = new Object[tasks.size()];
                final int[] count = { tasks.size() };

                int i = 0;
                for (Task<R, ?> task : tasks) {
                    final int pos = i++;

                    task.addOnResultListener(executor, new OnResultListener<R>() {
                        @Override
                        public void onResult(R result) {
                            resultsArray[pos] = result;

                            if (--count[0] == 0) {
                                workFinisher.withResult(Arrays.asList((R[]) resultsArray));
                            }
                        }
                    }).addOnErrorListener(executor, new OnErrorListener() {
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
