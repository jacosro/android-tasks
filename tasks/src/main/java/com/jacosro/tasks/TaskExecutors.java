package com.jacosro.tasks;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskExecutors {

    public static final Executor MAIN_THREAD_EXECUTOR = new Executor() {

        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    };

    public static final Executor BACKGROUND_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();
}
