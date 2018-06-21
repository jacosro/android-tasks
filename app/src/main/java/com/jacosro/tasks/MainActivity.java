package com.jacosro.tasks;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
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

        final Task<Long, String> task = makeSumTask();

        long t0 = System.nanoTime();
//        task.cancel();

        task.addOnResultListener(result -> {
            log("I got the result: " + result);
            log(String.format("Task spent %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        }).addOnErrorListener(error -> {
            log("Task got an error: " + error);
            log(String.format("Task spent %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        }).setTimeout(100, () -> log("Timeout!!"));

        log("Task launched!");

        Task<Void, Void> motherOfTasks = Tasks.whenAll(
                task,
                Tasks.runOnBackgroundThread(taskFinisher -> taskFinisher.withResult(makeSum())),
                Tasks.runOnBackgroundThread(taskFinisher -> taskFinisher.withError("Error")),
                makeSumTask()
        );

        log(task.toString());

        motherOfTasks
                .addOnResultListener(result -> log("Mother of tasks succeeded"))
                .addOnErrorListener(error -> log("Mother of tasks error: " + error));

        Tasks.runOnBackgroundThread(workFinisher -> {
            Log.d(TAG, "Im on a task that will not call workFinisher");
        }).addOnResultListener(result -> Log.d(TAG, "Task finished"))
                .addOnErrorListener(error -> Log.d(TAG, "Error"));
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private long makeSum() {
        long sum = 0;
        for (long i = 0; i < Math.sqrt(Math.sqrt(Long.MAX_VALUE)); i++) {
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
        return Tasks.runOnBackgroundThread(taskFinisher -> taskFinisher.withResult(makeSum()));
    }

    private void leak() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    SystemClock.sleep(1000);
                }
            }
        }).start();
    }
}
