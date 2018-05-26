package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log("Starting tasks");

        final long t0 = System.nanoTime();

        Task<Long, Void> task1 = new BaseTask<Long, Void>() {
            @Override
            protected void onExecution(@NonNull ExecutionCallback callback) {
                log("I'm working on thread: " + Thread.currentThread().getName());
                long sum = makeSum();
                callback.finishTaskWithResult(sum);
            }
        }.addOnResultListener(new OnResultListener<Long>() {
            @Override
            public void onResult(Long aLong) {
                log("I'm on thread: " + Thread.currentThread().getName());
                log("Task 1 took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0) + " ms");
            }
        }).addOnErrorListener(new OnErrorListener<Void>() {
            @Override
            public void onError(Void aVoid) {
                log("Error task 1");
            }
        });

        task1.execute();

        final long tt0 = System.nanoTime();

        Task<Long, Void> task2 = new BaseTask<Long, Void>() {
            @Override
            protected void onExecution(@NonNull ExecutionCallback callback) {
                log("I'm working on thread: " + Thread.currentThread().getName());
                callback.finishTaskWithResult(makeSum());
            }
        }.addOnResultListener(new OnResultListener<Long>() {
            @Override
            public void onResult(Long aLong) {
                log("I'm on thread: " + Thread.currentThread().getName());
                log("Task 2 took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - tt0) + " ms");
            }
        }).addOnErrorListener(new OnErrorListener<Void>() {
            @Override
            public void onError(Void aVoid) {
                log("Error task 2");
            }
        });


        task2.execute();

    }

    private void log(String message) {
        String tag = "Tasks"; // Thread.currentThread().getName();
        Log.d(tag, message);
    }

    private long makeSum() {
        long sum = 0;
        for (long i = 0; i < Math.sqrt(Math.sqrt(Long.MAX_VALUE)); i++) {
            sum += i;
        }
        return sum;
    }
}
