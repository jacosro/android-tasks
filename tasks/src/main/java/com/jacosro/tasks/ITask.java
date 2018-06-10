package com.jacosro.tasks;

import android.support.annotation.NonNull;

/**
 *
 *
 * Example:
 *
 *      ITask<Integer, String> task = TaskFactory.newTask(new TaskExecution<Integer, String>() {
 *          @Override
 *          public void onExecution(@NonNull TaskFinisher<Integer, String> finish) {
 *              boolean success = // whatever
 *
 *              if (success) {
 *                  finish.withResult(2 + 2);
 *              } else {
 *                  finish.withError("Error executing task");
 *              }
 *          }
 *      }.addOnResultListener(new OnResultListener<Integer>() {
 *          @Override
 *          public void onResult(Integer t) {
 *              Log.d(TAG, "Result of task: " + t);
 *          }
 *      }.addOnErrorListener(new OnErrorListener<String>() {
 *          @Override
 *          public void onError(String t) {
 *              Log.e(TAG, "Error executing task: " + t);
 *          }
 *      };
 *
 *
 * @param <R> The return type
 * @param <E> The error type
 */
public interface ITask<R, E> {

    /**
     * Adds a listener to the task that gets notified when it finishes successfully
     *
     * @param onResultListener The result listener
     * @return An ITask
     */
    @NonNull
    ITask<R, E> addOnResultListener(OnResultListener<R> onResultListener);

    /**
     * Adds a listener to the task that gets notified if the task is cancelled
     *
     * @param onErrorListener The error listener
     * @return An ITask
     */
    @NonNull
    ITask<R, E> addOnErrorListener(OnErrorListener<E> onErrorListener);


    /**
     * Sets up a timeout for the task
     * @param timeoutCallback the code that will be executed when the callback gets triggered
     */
    ITask<R, E> setTimeout(@NonNull TimeoutCallback timeoutCallback);
}
