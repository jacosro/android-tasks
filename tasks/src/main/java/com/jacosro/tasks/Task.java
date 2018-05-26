package com.jacosro.tasks;

/**
 * A Task is an ObservableTask that can be executed with the execute() method
 *
 * @param <R> The result of the task
 * @param <E> The error of the task
 */
public interface Task<R, E> extends ObservableTask<R, E> {

    /**
     * @return This task as an ObservableTask
     */
    ObservableTask<R, E> execute();
}
