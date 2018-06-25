package com.jacosro.tasks;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
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

        log("Main thread: " + Thread.currentThread().getName());

        final Task<Long, String> task = makeSumTask();

        long t0 = System.nanoTime();

        task.addOnResultListener(TaskExecutors.MAIN_THREAD_EXECUTOR, result -> {
            log(String.format("%s: Task success spent %s ms", Thread.currentThread().getName(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        }).addOnErrorListener(error -> {
            log("Task got an error: " + error);
            log(String.format("%s: Task error spent %s ms", Thread.currentThread().getName(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        }).setTimeout(60, () -> log(String.format("%s: Task timeout", Thread.currentThread().getName())));

        log("Task launched! " + task.toString());

        Task<List<Long>, Void> motherOfTasks = Tasks.whenAllSuccess(
                task,
                Tasks.runAsync(taskFinisher -> taskFinisher.withResult(makeSum())),
//                Tasks.runAsync(taskFinisher -> taskFinisher.withError("Error")),
                makeSumTask()
        );

        motherOfTasks
                .addOnResultListener(result -> log(String.format("%s: %s", motherOfTasks.toString(), result.toString())))
                .addOnErrorListener(error -> log(String.format("%s: %s", motherOfTasks.toString(), error)));

        /*Tasks.runAsync(workFinisher -> {
            Log.d(TAG, "Im on a task that will not call workFinisher");
        }).addOnResultListener(result -> Log.d(TAG, "Task finished"))
                .addOnErrorListener(error -> Log.d(TAG, "Error"));*/


        // Test run sync
        Task<Long, String> syncTask = Tasks.run(new TaskWork<Long, String>() {
            @Override
            public void onWork(@NonNull WorkFinisher<Long, String> workFinisher) {
                workFinisher.withResult(makeSum());
            }
        });
        log(syncTask.toString() + ": " + syncTask.getResult());

        Tasks.run(workFinisher -> log("Im on thread " + Thread.currentThread().getName()));

        Tasks.schedule(3000, workFinisher -> {
            log("Im scheduled 3 seconds later");

            workFinisher.withResult(null);
        });
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
        return Tasks.runAsync(taskFinisher -> taskFinisher.withResult(makeSum()));
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
