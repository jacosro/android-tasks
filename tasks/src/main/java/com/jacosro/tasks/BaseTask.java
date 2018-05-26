package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents a base task, that is specified in Asynchronous or Synchronous.
 * In order to create a new Task class, it should extend one of these classes, instead of this one
 *
 * A Task is an ObservableTask that can be executed with the execute() method
 *
 * Example:
 *
 *      Task<Integer, String> task = new SynchronousTask<Integer, String>() {
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
 *      }.addOnResultListener(new ObservableTask.Listener<Integer>() {
 *          @Override
 *          public void onTaskFinished(Integer t) {
 *              Log.d(TAG, "Result of task: " + t);
 *          }
 *      }.addOnErrorListener(new ObservableTask.Listener<String>() {
 *          @Override
 *          public void onTaskFinished(String t) {
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

    protected static final int INIT = 0;
    protected static final int RUNNING = 1;
    protected static final int CANCELLED = 2;
    protected static final int FINISHED = 3;

    protected R result;
    protected E error;
    private Queue<OnResultListener<R>> onResultListeners;
    private Queue<OnErrorListener<E>> onErrorListeners;
    private AtomicInteger state;
    private Executor executor;
    private ExecutorService executorService;
    private Handler handler;

    protected class ExecutionCallback {

        public void finishTaskWithResult(R r) {
            BaseTask.this.result = r;
            onFinish(false);
        }

        public void finishTaskWithError(E e) {
            BaseTask.this.error = e;
            cancel();
            onFinish(true);
        }
    }

    protected BaseTask() {
        this(Executors.newSingleThreadExecutor());
        this.executorService = (ExecutorService) this.executor;
    }

    protected BaseTask(Executor executor) {
        this.onResultListeners = new ArrayDeque<>(1);
        this.onErrorListeners = new ArrayDeque<>(1);
        this.state = new AtomicInteger(INIT);
        this.executor = executor;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public BaseTask<R, E> addOnResultListener(OnResultListener<R> onResult) {
        if (onResult != null)
            this.onResultListeners.add(onResult);

        return this;
    }

    public BaseTask<R, E> addOnErrorListener(OnErrorListener<E> onFail) {
        if (onFail != null)
            this.onErrorListeners.add(onFail);

        return this;
    }

    protected void cancel(E error) {
        this.error = error;
        cancel();
    }

    private void cancel() {
        setState(CANCELLED);
    }

    public ObservableTask<R, E> execute() {
        checkState();

        onPreExecute();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                onExecution(new ExecutionCallback());
            }
        });

        return this;
    }

    protected void checkState() {
        if (isRunning())
            throw new IllegalStateException("Task is already running!");

        if (isComplete())
            throw new IllegalStateException("Task has already finished!");

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

    protected abstract void onExecution(ExecutionCallback callback);

    @CallSuper
    /* Call super before overriding */
    protected void onFinish(boolean withError) {
        if (isComplete())
            return; // Task has already finished, ignore future results

        if (!isCancelled()) {
            setState(FINISHED);
        }

        if (withError) {
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

    public boolean isComplete() {
        return state.get() == CANCELLED || state.get() == FINISHED;
    }

    protected void setState(int state) {
        this.state.set(state);
    }
}
