package com.jacosro.tasks;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
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

        task.setTimeout(new TimeoutCallback(TimeUnit.MILLISECONDS, 500) {
            @Override
            public void onTimeout() {
                log("Timeout!");
            }
        });

        task.addOnResultListener(result -> {
            log("I got the result: " + result);
            log(String.format("Task spent %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        }).addOnErrorListener(error -> {
            log("Task got an error: " + error);
            log(String.format("Task spent %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0)));
        });

        log("Task launched!");

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
        return TaskFactory.newTask(taskFinisher -> taskFinisher.withResult(makeSum()));
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
