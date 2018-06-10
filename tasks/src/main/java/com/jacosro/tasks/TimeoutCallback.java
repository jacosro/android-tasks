package com.jacosro.tasks;

import java.util.concurrent.TimeUnit;

public abstract class TimeoutCallback {

    public static TimeoutCallback emptyCallback(TimeUnit timeUnit, long time) {
        return new TimeoutCallback(timeUnit, time) {
            @Override
            public void onTimeout() {

            }
        };
    }

    private TimeUnit timeUnit;
    private long time;

    public TimeoutCallback(TimeUnit timeUnit, long time) {
        this.timeUnit = timeUnit;
        this.time = time;
    }

    public abstract void onTimeout();

    public long getTimeoutInMillis() {
        return getTimeoutTimeIn(TimeUnit.MILLISECONDS);
    }

    public long getTimeoutTimeIn(TimeUnit destTimeUnit) {
        return destTimeUnit.convert(time, this.timeUnit);
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
