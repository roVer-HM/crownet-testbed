package edu.hm.crownet.testbed.scheduler;

/**
 * Scheduler interface for scheduling and managing tasks.
 * This interface provides methods to schedule one-shot and periodic tasks,
 * as well as to stop existing tasks by their unique identifiers.
 */
public interface Scheduler {

  /**
   * Schedules a one-shot task to be executed after a specified delay.
   * If a task with the same ID already exists, it will be stopped and replaced by the new one.
   *
   * @param taskId              unique identifier for the task
   * @param task                the {@link Runnable} task logic to execute
   * @param delayInMilliseconds the delay before execution, in milliseconds
   */
  void scheduleOneShotTask(String taskId, Runnable task, long delayInMilliseconds);

  /**
   * Schedules a recurring task to be executed at a fixed rate.
   * If a task with the same ID already exists, it will be stopped and replaced by the new one.
   *
   * @param taskId unique identifier for the task
   * @param task   the {@link Runnable} task logic to execute
   * @param delay  the initial delay before the first execution, in milliseconds
   * @param period the period between successive executions, in milliseconds
   */
  void scheduleTask(String taskId, Runnable task, long delay, long period);

  /**
   * Stops an existing task if it is present.
   * If no task with the given ID exists, this method does nothing.
   *
   * @param taskId unique identifier for the task to stop
   */
  void stopExistingTaskIfPresent(String taskId);
}