package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class represents a base task.
 * In order to create a new Task class, it should extend this one
 *
 *
 * Example:
 *
 *      Task<Integer, String> task = TaskFactory.newTask(new TaskExecution<Integer, String>() {
 *          @Override
 *          public void onExecution(@NonNull TaskFinisher<Integer, String> taskFinisher) {
 *              boolean success = // whatever
 *
 *              if (success) {
 *                  taskFinisher.withResult(2 + 2);
 *              } else {
 *                  taskFinisher.withError("Error executing task");
 *              }
 *          }
 *      }.addOnResultListener(new OnResultListener<Integer>() {
 *          @Override
 *          public void onResult(Integer t) {
 *              Log.d(TAG, "Result of task: " + t);
 *          }
 *      }.addOnErrorListener(new OnErrorListener<String>() {
 *          @Override
 *          public void onError(String t) {
 *              Log.e(TAG, "Error executing task: " + t);
 *          }
 *      };
 * @param <R> The mResult of the task
 * @param <E> The mError in case the task is cancelled
 */
public abstract class Task<R, E> {
    protected static final int INIT = 0;
    protected static final int RUNNING = 1;
    protected static final int TIMEOUT = 2;
    protected static final int SUCCESS = 3;
    protected static final int ERROR = 4;
    protected static final int CANCELLED = 5;

    // Task data
    private WeakReference<R> mResult;
    private WeakReference<E> mError;
    private Queue<OnResultListener<R>> mOnResultListeners;
    private Queue<OnErrorListener<E>> mOnErrorListeners;

    // Task state
    private int mState;
    private final Object stateLock = new Object();
    private AtomicBoolean done;

    // Handlers
    private Executor mExecutor;
    private ExecutorService mExecutorService;
    private Handler mHandler;

    // Timeout
    private Timer mTimeoutTimer;
    private TimeoutCallback mTimeoutCallback;

    private final TaskExecution.TaskFinisher<R, E> THE_CALLBACK = new TaskExecution.TaskFinisher<R, E>() {
        @Override
        public void withResult(R result) {
            Task.this.mResult = new WeakReference<>(result);
            setState(SUCCESS);
            onFinish();
        }

        @Override
        public void withError(E error) {
            Task.this.mError = new WeakReference<>(error);
            setState(ERROR);
            onFinish();
        }
    };

    protected Task(@NonNull Executor executor) {
        initialize(executor);

        from();
    }

    protected Task() {
        initialize(TaskExecutors.defaultBackgroundExecutor());
        this.mExecutorService = (ExecutorService) this.mExecutor;

        from();
    }

    private void initialize(Executor executor) {
        this.mResult = new WeakReference<>(null);
        this.mError = new WeakReference<>(null);
        this.mOnResultListeners = new ArrayDeque<>(1);
        this.mOnErrorListeners = new ArrayDeque<>(1);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mExecutor = executor;
        this.mState = INIT;
        this.done = new AtomicBoolean(false);
    }

    @NonNull
    public Task<R, E> addOnResultListener(OnResultListener<R> onResultListener) {
        if (onResultListener != null) {
            if (isDone()) {
                R result = mResult.get();
                if (result != null) {
                    onResultListener.onResult(result);
                }

                return this;
            }

            this.mOnResultListeners.add(onResultListener);
        }

        return this;
    }

    @NonNull
    public Task<R, E> addOnErrorListener(OnErrorListener<E> onErrorListener) {
        if (onErrorListener != null) {
            if (isDone()) {
                E error = mError.get();
                if (error != null) {
                    onErrorListener.onError(error);
                }

                return this;
            }

            this.mOnErrorListeners.add(onErrorListener);
        }

        return this;
    }

    public void setTimeout(@NonNull TimeoutCallback timeoutCallback) {
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
        }

        mTimeoutTimer = new Timer();
        mTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                setState(TIMEOUT);
                onFinish();
            }
        }, timeoutCallback.getTimeoutInMillis());

        mTimeoutCallback = timeoutCallback;
    }

    public void cancel() {
        setState(CANCELLED);
    }

    private void from() {
        onPreExecute();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(THE_CALLBACK);
            }
        });
    }

    protected abstract void onExecution(TaskExecution.TaskFinisher<R, E> taskFinisher);

    @CallSuper
    /* Call super after overriding */
    protected void onPreExecute() {
        if (isFailed()) {
            onFinish();
            return;
        }

        setState(RUNNING);
    }

    @CallSuper
    /* Call super before overriding */
    protected synchronized void onFinish() {
        if (isDone()) {
            return; // Task has already finished, ignore future results or errors
        }

        Runnable afterExecution;

        if (isCancelled()) {
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    // Nothing to do. Null objects and that's it
                }
            };
        } else if (isFailed()) { // Task is cancelled
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    E error = mError.get();

                    if (error == null)
                        return;

                    while (!mOnErrorListeners.isEmpty())
                        mOnErrorListeners.poll().onError(error);
                }
            };
        } else if (isTimeout()) { // Task is timeout
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    if (mTimeoutCallback != null) // TimeoutCallback should not be null in any case
                        mTimeoutCallback.onTimeout();
                }
            };
        } else { // Task is successful
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    R result = mResult.get();

                    if (result == null)
                        return;

                    while (!mOnResultListeners.isEmpty())
                        mOnResultListeners.poll().onResult(result);
                }
            };
        }

        done.set(true);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                afterExecution.run();

                nullObjects();
            }
        });
    }

    private void nullObjects() {
        mOnResultListeners = null;
        mOnErrorListeners = null;
        mExecutor = null;
        mHandler = null;
        mTimeoutCallback = null;

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
    }

    protected boolean isSuccessful() {
        synchronized (stateLock) {
            return mState == SUCCESS;
        }
    }

    protected boolean isFailed() {
        synchronized (stateLock) {
            return mState == ERROR;
        }
    }

    protected boolean isTimeout() {
        synchronized (stateLock) {
            return mState == TIMEOUT;
        }
    }

    public boolean isCancelled() {
        synchronized (stateLock) {
            return mState == CANCELLED;
        }
    }

    protected boolean isDone() {
        return done.get();
    }

    protected void setState(int state) {
        synchronized (stateLock) {
            if (mState < state) {
                mState = state;
            }
        }
    }
}
