package com.jacosro.tasks;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents a base task.
 * In order to create a new Task class, it should extend this one<br>
 * Usage example:
 *
 * <pre>{@code
 *      Tasks.runOnBackgroundThread(workFinisher -> {
 *          boolean success = // whatever
 *
 *          if (success) {
 *              workFinisher.withResult(2 + 2);
 *          } else {
 *              workFinisher.withError("Error executing task");
 *          }
 *      })
 *      .addOnResultListener(result -> Log.d("Task", "Result: " + result))
 *      .addOnErrorListener(error -> Log.d("Task", "Error: " + error)
 *      .setTimeout(1000, () -> Log.d("Task", "Timeout!"));
 * }</pre>
 *
 * @param <R> The result type of the task
 * @param <E> The error type in case the task is cancelled
 */
public abstract class ExecutableTask<R, E> implements Task<R, E> {

    protected static final int INIT = 0;
    protected static final int RUNNING = 1;
    protected static final int TIMEOUT = 2;
    protected static final int SUCCESS = 3;
    protected static final int ERROR = 4;
    protected static final int CANCELLED = 5;

    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    // Task data
    private int id;
    private R mResult;
    private E mError;
    private ListenersQueue<OnResultListener<R>> mOnResultListeners;
    private ListenersQueue<OnErrorListener<E>> mOnErrorListeners;

    // Task state
    private int mState;
    private final Object stateLock = new Object();
    private AtomicBoolean mFinished;

    // Executors
    private Executor mTaskWorkExecutor;
    private Executor mTimeoutExecutor;

    // Timeout
    private Task<Void, Void> mTimeoutTask;
    private TimeoutCallback mTimeoutCallback;

    private TaskWork.WorkFinisher<R, E> mTaskWorkFinisher = new TaskWork.WorkFinisher<R, E>() {
        @Override
        public void withResult(R result) {
            setResult(result);
        }

        @Override
        public void withError(E error) {
            setError(error);
        }
    };

    protected ExecutableTask(@NonNull Executor executor) {
        this.mOnResultListeners = new ListenersQueue<>(1);
        this.mOnErrorListeners = new ListenersQueue<>(1);
        this.mTaskWorkExecutor = executor;
        this.mFinished = new AtomicBoolean(false);
        this.mState = INIT;
        assignId();
    }

    private void assignId() {
        LAST_ID.compareAndSet(Integer.MAX_VALUE, 0);
        this.id = LAST_ID.getAndIncrement();
    }

    @Override
    @NonNull
    public Task<R, E> addOnResultListener(@NonNull OnResultListener<R> onResultListener) {
        return addOnResultListener(TaskExecutors.MAIN_THREAD_EXECUTOR, onResultListener);
    }

    @Override
    @NonNull
    public Task<R, E> addOnResultListener(@NonNull Executor executor, @NonNull OnResultListener<R> onResultListener) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(onResultListener);

        if (isFinished()) {
            if (isSuccessful()) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onResultListener.onResult(mResult);
                    }
                });
            }

            return this;
        }

        this.mOnResultListeners.add(onResultListener, executor);

        return this;
    }

    @Override
    @NonNull
    public Task<R, E> addOnErrorListener(@NonNull OnErrorListener<E> onErrorListener) {
        return addOnErrorListener(TaskExecutors.MAIN_THREAD_EXECUTOR, onErrorListener);
    }

    @Override
    @NonNull
    public Task<R, E> addOnErrorListener(@NonNull Executor executor, @NonNull OnErrorListener<E> onErrorListener) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(onErrorListener);

        if (isFinished()) {
            if (isFailed()) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onErrorListener.onError(mError);
                    }
                });
            }

            return this;
        }

        this.mOnErrorListeners.add(onErrorListener, executor);

        return this;
    }

    @Override
    public void setTimeout(long milliseconds, @Nullable TimeoutCallback callback) {
        setTimeout(milliseconds, TaskExecutors.MAIN_THREAD_EXECUTOR, callback);
    }

    @Override
    public void setTimeout(long milliseconds, @NonNull Executor executor, @Nullable TimeoutCallback callback) {
        Objects.requireNonNull(executor);

        if (milliseconds < 0)
            throw new IllegalArgumentException("Milliseconds cannot be < 0");


        if (mTimeoutTask != null) {
            mTimeoutTask.cancel();
        }

        mTimeoutCallback = callback;
        mTimeoutExecutor = executor;

        mTimeoutTask = Tasks.runAsync(new TaskWork<Void, Void>() {
            @CallWorkFinisher
            @Override
            public void onWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                SystemClock.sleep(milliseconds);

                ExecutableTask.this.setState(TIMEOUT);
                ExecutableTask.this.doOnFinish();

                workFinisher.withResult(null);
            }
        });
    }

    @Override
    public void cancel() {
        setState(CANCELLED);
        mOnResultListeners.clear();
        mOnErrorListeners.clear();
        doOnFinish();
    }

    /**
     * Executes the task
     * @return The task
     * @throws IllegalStateException if the task has already finished
     */
    public Task<R, E> execute() {
        if (isFinished())
            throw new IllegalStateException("Task is already finished. Cannot execute it again");

        setState(RUNNING);

        mTaskWorkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(mTaskWorkFinisher);

                /* checkWorkFinisherCalled();

                 Cannot perform this check because the execution of this task may not finish
                 until the executions of one or more async task, which may be causing an
                 unexpected thrown of the IllegalStateException
                */
            }
        });

        return this;
    }

    private void checkWorkFinisherCalled() {
        if (!isFinished())
            throw new IllegalStateException("WorkFinisher must be called on task work");
    }

    protected abstract void onExecution(TaskWork.WorkFinisher<R, E> workFinisher);

    protected void doOnFinish() {
        if (isFinished()) {
            return; // Task has already finished, ignore future results or errors
        }

        mFinished.set(true);

        if (isSuccessful()) { // Task is successful
            while (mOnResultListeners.isNotEmpty()) {
                Pair<OnResultListener<R>, Executor> pair = mOnResultListeners.poll();

                pair.second.execute(new Runnable() {
                    @Override
                    public void run() {
                        pair.first.onResult(mResult);
                    }
                });
            }
        } else if (isFailed()) { // Task gave an error
            while (mOnErrorListeners.isNotEmpty()) {
                Pair<OnErrorListener<E>, Executor> pair = mOnErrorListeners.poll();

                pair.second.execute(new Runnable() {
                    @Override
                    public void run() {
                        pair.first.onError(mError);
                    }
                });
            }

        } else if (isTimeout()) { // Task is timeout
            mTimeoutExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (mTimeoutCallback != null) // Should not be null in any case at this point
                        mTimeoutCallback.onTimeout();

                    mTimeoutCallback = null;
                }
            });
        }

        nullObjects();
        onFinish();
    }

    /**
     * Does nothing by default. It may be overriden and it will be executed when task is finished.
     * It is useful for nulling objects and freeing resources used in task work
     */
    protected void onFinish() {

    }

    private void nullObjects() {
        // Data
        mOnResultListeners = null;
        mOnErrorListeners = null;

        //Handlers
        mTaskWorkExecutor = null;

        // Timeout
        mTimeoutExecutor = null;
        mTimeoutTask = null;

        mTaskWorkFinisher = null;
    }


    @Override
    @Nullable
    public R getResult() {
        return mResult;
    }

    void setResult(R result) {
        mResult = result;
        setState(SUCCESS);
        doOnFinish();
    }

    @Override
    @Nullable
    public E getError() {
        return mError;
    }

    void setError(E error) {
        mError = error;
        setState(ERROR);
        doOnFinish();
    }


    @Override
    public boolean isSuccessful() {
        synchronized (stateLock) {
            return mState == SUCCESS;
        }
    }


    @Override
    public boolean isFailed() {
        synchronized (stateLock) {
            return mState == ERROR;
        }
    }


    @Override
    public boolean isTimeout() {
        synchronized (stateLock) {
            return mState == TIMEOUT;
        }
    }


    @Override
    public boolean isCancelled() {
        synchronized (stateLock) {
            return mState == CANCELLED;
        }
    }


    @Override
    public boolean isFinished() {
        return mFinished.get();
    }

    protected void setState(int state) {
        synchronized (stateLock) {
            if (mState < state) {
                mState = state;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Task-%s [%s]", id, (
                mState == INIT ? "INIT":
                    (mState == RUNNING ? "RUNNING" :
                            (mState == TIMEOUT ? "TIMEOUT" :
                                    (mState == SUCCESS ? "SUCCESS" :
                                            (mState == ERROR ? "ERROR" : "CANCELLED")))))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof ExecutableTask))
            return false;

        ExecutableTask task = (ExecutableTask) o;
        return task.id == this.id;
    }

    private class ListenersQueue<T> {

        private Queue<Pair<T, Executor>> mQueue;

        ListenersQueue(int initialSize) {
            mQueue = new ArrayDeque<>(initialSize);
        }

        void add(T listener, Executor executor) {
            mQueue.add(Pair.create(listener, executor));
        }

        boolean isNotEmpty() {
            return !mQueue.isEmpty();
        }

        Pair<T, Executor> poll() {
            return mQueue.poll();
        }

        void clear() {
            mQueue.clear();
        }
    }
}
