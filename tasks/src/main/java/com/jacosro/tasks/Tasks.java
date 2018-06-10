package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.jacosro.tasks.TaskExecutors.MAIN_THREAD_EXECUTOR;

public class Tasks {

    public static <R, E> Task<R, E> forResult(final R result) {
        return new BaseTask<R, E>(MAIN_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(@NonNull ExecutionCallback callback) {
                callback.finishTaskWithResult(result);
            }
        };
    }

    public static <R, E> Task<R, E> forError(final E error) {
        return new BaseTask<R, E>(MAIN_THREAD_EXECUTOR) {
            @Override
            protected void onExecution(@NonNull ExecutionCallback callback) {
                callback.finishTaskWithError(error);
            }
        };
    }
}
