package com.jacosro.tasks;

public interface Task<R, E> extends ObservableTask<R, E> {
    ObservableTask<R, E> execute();
}
