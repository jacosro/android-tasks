package com.jacosro.tasks;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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

          makeSumTask()
            .addOnResultListener(result -> log("ITask result: " + String.valueOf(result)))
            .addOnErrorListener(ignore -> log("Cancelled task"))
            .setTimeout(new TimeoutCallback(TimeUnit.MILLISECONDS, 2000) {
                @Override
                public void onTimeout() {
                    log("Timeout!!!");
                }
            });

        makeSumTask().addOnResultListener(result -> toast(String.valueOf(result)));

    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        return Task.newTask(new TaskExecution<Long, String>() {
            @Override
            public void onExecution(@NonNull TaskFinisher<Long, String> finish) {
                finish.withResult(makeSum());
            }
        });
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
