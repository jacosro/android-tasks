package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a base task.
 * In order to create a new Task class, it should extend this one<br>
 * Usage example:
 *
 * <pre>{@code
 *      Tasks.runOnBackgroundThread(taskFinisher -> {
 *          boolean success = // whatever
 *
 *          if (success) {
 *              taskFinisher.withResult(2 + 2);
 *          } else {
 *              taskFinisher.withError("Error executing task");
 *          }
 *      }).addOnResultListener(result -> Log.d("Task", "Result: " + result))
 *      .addOnErrorListener(error -> Log.d("Task", "Error: " + error)
 *      .setTimeout(1000, () -> Log.d("Task", "Task is timeout"));
 * }</pre>
 *
 * @param <R> The result type of the task
 * @param <E> The error type in case the task is cancelled
 */
public abstract class Task<R, E> {
    protected static final int RUNNING = 1;
    protected static final int TIMEOUT = 2;
    protected static final int SUCCESS = 3;
    protected static final int ERROR = 4;
    protected static final int CANCELLED = 5;

    // Task data
    private R mResult;
    private E mError;
    private Queue<OnResultListener<R>> mOnResultListeners;
    private Queue<OnErrorListener<E>> mOnErrorListeners;

    // Task state
    private int mState;
    private final Object stateLock = new Object();
    private AtomicBoolean mFinished;

    // Handlers
    private Executor mExecutor;
    private ExecutorService mExecutorService;
    private Handler mHandler;

    // Timeout
//    private Timer mTimeoutTimer;
//    private OldTimeoutCallback mTimeoutCallback;
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
        initialize(executor);

        execute();
    }

    protected Task() {
        initialize(TaskExecutors.defaultBackgroundExecutor());
        this.mExecutorService = (ExecutorService) this.mExecutor;

        execute();
    }

    private void initialize(Executor executor) {
        this.mOnResultListeners = new ArrayDeque<>(1);
        this.mOnErrorListeners = new ArrayDeque<>(1);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mExecutor = executor;
        this.mState = RUNNING;
        this.mFinished = new AtomicBoolean(false);
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
     * it is cancelled and the OldTimeoutCallback is called
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

        mTimeoutTask = Tasks.runOnBackgroundThread(new TaskWork<Void, Void>() {
            @CallTaskFinisher
            @Override
            public void doWork(@NonNull WorkFinisher<Void, Void> workFinisher) {
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

    private void execute() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(FINISHER);
            }
        });
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
                    if (mTimeoutCallback != null)
                        mTimeoutCallback.onTimeout();
                    /*if (mTimeoutCallback != null) // OldTimeoutCallback should not be null in any case
                        mTimeoutCallback.onTimeout();*/
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

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                afterExecution.run();

                nullObjects();
            }
        });

        mFinished.set(true);

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
        mExecutor = null;
        mHandler = null;

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }

        // Timeout
        mTimeoutCallback = null;

        if (mTimeoutTask != null) {
            mTimeoutTask = null;
        }
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
}
