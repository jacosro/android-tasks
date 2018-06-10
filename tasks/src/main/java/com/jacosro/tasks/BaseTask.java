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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents a base task, that is specified in Asynchronous or Synchronous.
 * In order to create a new Task class, it should extend one of these classes, instead of this one
 *
 * A Task is an Task that can be executed with the execute() method
 *
 * Example:
 *
 *      Task<Integer, String> task = new BaseTask<Integer, String>() {
 *          @Override
 *          protected void onExecution(ExecutionCallback callback) {
 *              boolean success = // whatever
 *
 *              if (success) {
 *                  callback.finishTaskWithResult(2 + 2);
 *              } else {
 *                  callback.finishTaskWithError("Error executing task");
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
 *
 *      task.execute();
 *
 *
 * @param <R> The result of the task
 * @param <E> The error in case the task is cancelled
 */
public abstract class BaseTask<R, E> implements Task<R, E> {

    protected static final int RUNNING = 1;
    protected static final int CANCELLED = 2;
    protected static final int FINISHED = 3;

    private final ExecutionCallback THE_CALLBACK = new ExecutionCallback();

    private R result;
    private E error;
    private Queue<OnResultListener<R>> onResultListeners;
    private Queue<OnErrorListener<E>> onErrorListeners;
    private AtomicInteger state;
    private Executor executor;
    private ExecutorService executorService;
    private Handler handler;

    protected BaseTask(Executor executor) {
        initialize(executor);
    }

    protected BaseTask() {
        initialize(TaskExecutors.BACKGROUND_THREAD_EXECUTOR);
        this.executorService = (ExecutorService) this.executor;
    }

    private void initialize(Executor executor) {
        this.onResultListeners = new ArrayDeque<>(1);
        this.onErrorListeners = new ArrayDeque<>(1);
        this.handler = new Handler(Looper.getMainLooper());
        this.executor = executor;
        this.state = new AtomicInteger(RUNNING);
    }

    @NonNull
    public Task<R, E> addOnResultListener(OnResultListener<R> onResultListener) {
        if (onResultListener != null)
            this.onResultListeners.add(onResultListener);

        return this;
    }

    @NonNull
    public Task<R, E> addOnErrorListener(OnErrorListener<E> onErrorListener) {
        if (onErrorListener != null)
            this.onErrorListeners.add(onErrorListener);

        return this;
    }

    @Override
    public Task<R, E> setTimeout(@NonNull TimeoutCallback timeoutCallback) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                BaseTask.this.cancel();
                timeoutCallback.onTimeout();
            }
        }, timeoutCallback.getTimeoutInMillis());

        return this;
    }

    protected void cancel(E error) {
        this.error = error;
        cancel();
    }

    private void cancel() {
        setState(CANCELLED);
    }

    @Override
    public void execute() {
        onPreExecute();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(THE_CALLBACK);
            }
        });
    }

    @CallSuper
    /* Call super after overriding */
    protected void onPreExecute() {
        if (isCancelled()) {
            onFinish(true);
            return;
        }

        setState(RUNNING);
    }

    protected abstract void onExecution(@NonNull ExecutionCallback callback);

    @CallSuper
    /* Call super before overriding */
    protected void onFinish(boolean withError) {
        if (isComplete())
            return; // Task has already finished, ignore future results or errors

        if (!isCancelled()) {
            setState(FINISHED);
        }

        if (withError || isCancelled()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    while (!onErrorListeners.isEmpty()) {
                        onErrorListeners.poll().onError(error);
                    }

                    onResultListeners = null;
                    onErrorListeners = null;
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    while (!onResultListeners.isEmpty()) {
                        onResultListeners.poll().onResult(result);
                    }

                    onResultListeners = null;
                    onErrorListeners = null;
                }
            });
        }

        result = null;
        error = null;
        executor = null;
        handler = null;

        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    public boolean isRunning() {
        return state.get() == RUNNING;
    }

    public boolean isCancelled() {
        return state.get() == CANCELLED;
    }

    public boolean isSuccessful() {
        return state.get() == FINISHED;
    }

    public boolean isComplete() {
        return isCancelled() || isSuccessful();
    }

    protected void setState(int state) {
        this.state.set(state);
    }

    protected class ExecutionCallback {

        public void finishTaskWithResult(R result) {
            BaseTask.this.result = result;
            onFinish(false);
        }
        public void finishTaskWithError(E error) {
            BaseTask.this.error = error;
            cancel();
            onFinish(true);
        }
    }
}
