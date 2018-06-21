package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskExecutors {

    public static final Executor CURRENT_THREAD_EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };

    public static final Executor MAIN_THREAD_EXECUTOR = new Executor() {

        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    };

    public static Executor defaultBackgroundExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
