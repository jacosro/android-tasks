package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
public abstract class Task<R, E> {

    private static final AtomicInteger LAST_ID = new AtomicInteger(0);

    protected static final int RUNNING = 1;
    protected static final int TIMEOUT = 2;
    protected static final int SUCCESS = 3;
    protected static final int ERROR = 4;
    protected static final int CANCELLED = 5;

    // Task data
    private int id;
    private R mResult;
    private E mError;
    private Queue<OnResultListener<R>> mOnResultListeners;
    private Queue<OnErrorListener<E>> mOnErrorListeners;

    // Task state
    private int mState;
    private final Object stateLock = new Object();
    private AtomicBoolean mFinished;

    // Executors
    private Executor mExecutor;
    private Executor mOnCompleteExecutor;

    // Timeout
    private Task<Void, Void> mTimeoutTask;
    private TimeoutCallback mTimeoutCallback;

    private final TaskWork.WorkFinisher<R, E> FINISHER = new TaskWork.WorkFinisher<R, E>() {
        @Override
        public void withResult(R result) {
            setResult(result);
        }

        @Override
        public void withError(E error) {
            setError(error);
        }
    };

    protected Task(@NonNull Executor executor) {
        this.mOnResultListeners = new ArrayDeque<>(1);
        this.mOnErrorListeners = new ArrayDeque<>(1);
        this.mExecutor = executor;
        this.mOnCompleteExecutor = TaskExecutors.MAIN_THREAD_EXECUTOR;
        this.mState = RUNNING;
        this.mFinished = new AtomicBoolean(false);
        assignId();

        execute();
    }

    private void assignId() {
        LAST_ID.compareAndSet(Integer.MAX_VALUE, 0);
        this.id = LAST_ID.getAndIncrement();
    }

    @NonNull
    public Task<R, E> addOnResultListener(@NonNull OnResultListener<R> onResultListener) {
        Objects.requireNonNull(onResultListener);

        if (isSuccessful()) {
            onResultListener.onResult(mResult);
            return this;
        }

        this.mOnResultListeners.add(onResultListener);

        return this;
    }

    @NonNull
    public Task<R, E> addOnErrorListener(@NonNull OnErrorListener<E> onErrorListener) {
        Objects.requireNonNull(onErrorListener);

        if (isFailed()) {
            onErrorListener.onError(mError);
            return this;
        }

        this.mOnErrorListeners.add(onErrorListener);

        return this;
    }

    /**
     * Sets a timeout for the task in which if the task does not finish whithin the time specified
     * it is cancelled and the TimeoutCallback is called
     *
     * @param milliseconds The timeout time in milliseconds
     * @param callback The code to execute after timeout
     */
    public void setTimeout(long milliseconds, @Nullable TimeoutCallback callback) {
        if (milliseconds < 0)
            throw new IllegalArgumentException("Milliseconds cannot be < 0");

        if (mTimeoutTask != null) {
            mTimeoutTask.cancel();
        }

        mTimeoutCallback = callback;

        mTimeoutTask = Tasks.runAsync(new TaskWork<Void, Void>() {
            @CallWorkFinisher
            @Override
            public void onWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException ignored) {
                } finally {
                    Task.this.setState(TIMEOUT);
                    Task.this.doOnFinish();

                    workFinisher.withResult(null);
                }
            }
        });
    }

    public void cancel() {
        setState(CANCELLED);
    }

    protected void execute() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(FINISHER);

                /* Cannot perform this check because
                 if there is another async task inside execution,
                 the execution of this task will finish before others,
                 causing an early thrown of the exception
                 */

                // checkWorkFinisherCalled();
            }
        });
    }

    private void checkWorkFinisherCalled() {
        if (!isFinished())
            throw new IllegalStateException("WorkFinisher must be called on task work");
    }

    protected abstract void onExecution(TaskWork.WorkFinisher<R, E> workFinisher);

    protected synchronized void doOnFinish() {
        if (isFinished()) {
            return; // Task has already finished, ignore future results or errors
        }

        Runnable afterExecution;

        if (isCancelled()) { // Task is cancelled
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    // Nothing to do. Null objects and that's it
                }
            };
        } else if (isFailed()) { // Task gave an error
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    while (!mOnErrorListeners.isEmpty())
                        mOnErrorListeners.poll().onError(mError);
                }
            };
        } else if (isTimeout()) { // Task is timeout
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    if (mTimeoutCallback != null) // Should not be null in any case at this point
                        mTimeoutCallback.onTimeout();
                }
            };
        } else { // Task is successful
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    while (!mOnResultListeners.isEmpty())
                        mOnResultListeners.poll().onResult(mResult);
                }
            };
        }

        mOnCompleteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                afterExecution.run();

                nullObjects();
                onFinish();
            }
        });

        mFinished.set(true);
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
        mExecutor = null;
        mOnCompleteExecutor = null;

        // Timeout
        mTimeoutCallback = null;
        mTimeoutTask = null;
    }

    /**
     * Returns the result of the task if it finished successfully, null otherwise
     * @return R the result
     */
    @Nullable
    public R getResult() {
        return mResult;
    }

    /**
     * Returns the error of the task in case it was cancelled, null otherwise
     * @return E the error
     */
    @Nullable
    public E getError() {
        return mError;
    }

    private void setResult(R result) {
        mResult = result;
        setState(SUCCESS);
        doOnFinish();
    }

    private void setError(E error) {
        mError = error;
        setState(ERROR);
        doOnFinish();
    }

    /**
     * @return true if the task has finished successfully, false otherwise
     */
    public boolean isSuccessful() {
        synchronized (stateLock) {
            return mState == SUCCESS;
        }
    }

    /**
     * @return true if the task has finished with an error, false otherwise
     */

    public boolean isFailed() {
        synchronized (stateLock) {
            return mState == ERROR;
        }
    }

    /**
     * @return true if the task has finished due to a timeout, false otherwise
     */
    public boolean isTimeout() {
        synchronized (stateLock) {
            return mState == TIMEOUT;
        }
    }

    /**
     * @return true if the task has been cancelled, false otherwise
     */
    public boolean isCancelled() {
        synchronized (stateLock) {
            return mState == CANCELLED;
        }
    }

    /**
     * @return true if the task has finished, false otherwise
     */
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
        return String.format("Task number %s: %s", id, (
                mState == RUNNING ? "RUNNING" :
                        (mState == TIMEOUT ? "TIMEOUT" :
                                (mState == SUCCESS ? "SUCCESS" :
                                        (mState == ERROR ? "ERROR" : "CANCELLED"))))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Task))
            return false;

        Task task = (Task) o;
        return task.id == this.id;
    }
}
