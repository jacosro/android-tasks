package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public void onStart() {
        super.onStart();

        Task<Integer, String> task = Tasks.createNewTask(finishTask -> {

        });

        makeSumTask()
            .addOnResultListener(result -> log("Task result: " + String.valueOf(result)))
            .addOnErrorListener(ignore -> log("Cancelled task"))
            .setTimeout(new TimeoutCallback(TimeUnit.MILLISECONDS, 500) {
                @Override
                public void onTimeout() {
                    log("Timeout!!!");
                }
            });

        makeSumTask();
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private long makeSum() {
        long sum = 0;
        for (long i = 0; i < Math.sqrt(Long.MAX_VALUE); i++) {
            sum += i;
        }
/*        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            return 0;
        }*/

        return sum;
    }

    private Task<Long, String> makeSumTask() {
        return Tasks.createNewTask(callback -> callback.withError("I don't want to make the sum now"));
    }
}
