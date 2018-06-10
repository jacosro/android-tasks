package com.jacosro.tasks;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.TaskExecutors;


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

        Task<String, Void> task = Tasks.forResult("My result");

        task.addOnResultListener(this::log);
        task.addOnErrorListener(ignore -> log("Cancelled task"));
    }

    private void log(String message) {
        if (message == null) {
            message = "No message";
        }

        Log.d(TAG, message);
    }

    private long makeSum() {
        long sum = 0;
        for (long i = 0; i < Math.sqrt(Math.sqrt(Long.MAX_VALUE)); i++) {
            sum += i;
        }
        return sum;
    }

    private Task<Integer, String> makeSumTask() {
        return new BaseTask<Integer, String>() {
            @Override
            protected void onExecution(@NonNull ExecutionCallback callback) {

            }
        };
    }
}
