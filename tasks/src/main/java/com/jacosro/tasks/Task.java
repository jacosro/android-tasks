package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * This class represents a base task.
 * In order to create a new ITask class, it should extend this one
 *
 * @param <R> The mResult of the task
 * @param <E> The mError in case the task is cancelled
 */
public class Task<R, E> { //} implements ITask<R, E> {

    protected static final int INIT = 0;
    protected static final int RUNNING = 1;
    protected static final int SUCCESS = 2;
    protected static final int TIMEOUT = 3;
    protected static final int CANCELLED = 4;
    protected static final int DONE = 10;

    // Task data
    private R mResult;
    private E mError;
    private Queue<OnResultListener<R>> mOnResultListeners;
    private Queue<OnErrorListener<E>> mOnErrorListeners;

    // Task state
    private int mState;
    private final Object stateLock = new Object();

    // Handlers
    private TaskExecution<R, E> mTaskExecution;
    private Executor mExecutor;
    private ExecutorService mExecutorService;
    private Handler mHandler;

    // Timeout
    private Timer mTimeoutTimer;
    private TimeoutCallback mTimeoutCallback;

    private final TaskExecution.TaskFinisher<R, E> THE_CALLBACK = new TaskExecution.TaskFinisher<R, E>() {
        @Override
        public void withResult(R result) {
            Task.this.mResult = result;
            setState(SUCCESS);
            onFinish();
        }

        @Override
        public void withError(E error) {
            Task.this.mError = error;
            setState(CANCELLED);
            onFinish();
        }
    };

    private Task() {

    }

    protected Task(@NonNull TaskExecution<R, E> task, @NonNull Executor executor) {
        initialize(task, executor);

        execute();
    }

    protected Task(@NonNull TaskExecution<R, E> task) {
        initialize(task, TaskExecutors.defaultBackgroundExecutor());
        this.mExecutorService = (ExecutorService) this.mExecutor;

        execute();
    }

    private void initialize(TaskExecution<R, E> task, Executor executor) {
        this.mTaskExecution = task;
        this.mOnResultListeners = new ArrayDeque<>(1);
        this.mOnErrorListeners = new ArrayDeque<>(1);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mExecutor = executor;
        this.mState = INIT;
    }

    @NonNull
    public Task<R, E> addOnResultListener(OnResultListener<R> onResultListener) {
        if (onResultListener != null)
            this.mOnResultListeners.add(onResultListener);

        return this;
    }

    @NonNull
    public Task<R, E> addOnErrorListener(OnErrorListener<E> onErrorListener) {
        if (onErrorListener != null)
            this.mOnErrorListeners.add(onErrorListener);

        return this;
    }

    // @Override
    public Task<R, E> setTimeout(@NonNull TimeoutCallback timeoutCallback) {
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

        return this;
    }

    protected void cancel(E error) {
        this.mError = error;
        setState(CANCELLED);
    }

    private void execute() {
        onPreExecute();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mTaskExecution.onExecution(THE_CALLBACK);
            }
        });
    }

    @CallSuper
    /* Call super after overriding */
    protected void onPreExecute() {
        if (isCancelled()) {
            onFinish();
            return;
        }

        setState(RUNNING);
    }

    @CallSuper
    /* Call super before overriding */
    protected synchronized void onFinish() {
        if (isDone()) {
            return; // ITask has already finished, ignore future results or errors
        }

        Runnable afterExecution;

        if (isCancelled()) { // ITask is cancelled
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    while (!mOnErrorListeners.isEmpty()) {
                        mOnErrorListeners.poll().onError(mError);
                    }
                }
            };
        } else if (isTimeout()) { // ITask is timeout
            afterExecution = new Runnable() {
                @Override
                public void run() {
                    if (mTimeoutCallback != null) // TimeoutCallback should not be null in any case
                        mTimeoutCallback.onTimeout();
                }
            };
        } else { // ITask is successful

            afterExecution = new Runnable() {
                @Override
                public void run() {
                    while (!mOnResultListeners.isEmpty()) {
                        mOnResultListeners.poll().onResult(mResult);
                    }
                }
            };
        }

        setState(DONE);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                afterExecution.run();

                nullObjects();
            }
        });
    }

    private void nullObjects() {
        mResult = null;
        mError = null;
        mOnResultListeners = null;
        mOnErrorListeners = null;
        mExecutor = null;
        mHandler = null;
        mTaskExecution = null;
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

    protected boolean isCancelled() {
        synchronized (stateLock) {
            return mState == CANCELLED;
        }
    }

    protected boolean isTimeout() {
        synchronized (stateLock) {
            return mState == TIMEOUT;
        }
    }

    protected boolean isComplete() {
        synchronized (stateLock) {
            return mState > RUNNING;
        }
    }

    protected boolean isDone() {
        synchronized (stateLock) {
            return mState == DONE;
        }
    }

    protected void setState(int state) {
        synchronized (stateLock) {
            if (mState < state) {
                mState = state;
            }
        }
    }

    /**
     * Creates a new task that will execute the code inside TaskExecution
     * @param taskExecution The code that will be executed
     * @param <R> The result type
     * @param <E> The error type
     * @return A ITask that will execute the code
     */
    public static <R, E> Task<R, E> newTask(TaskExecution<R, E> taskExecution) {
        return new Task<>(taskExecution);
    }

    /**
     * Creates a new task that will execute the code inside TaskExecution
     * @param taskExecution The code that will be executed
     * @param executor The executor who will execute the code
     * @param <R> The result type
     * @param <E> The error type
     * @return A ITask that will execute the code
     */
    public static <R, E> Task<R, E> newTask(TaskExecution<R, E> taskExecution, Executor executor) {
        return new Task<>(taskExecution, executor);
    }
}
