package edu.hm.crownet.testbed.scheduler;

/**
 * Scheduler interface for managing recurring tasks identified by unique task IDs.
 */
public interface Scheduler {

  /**
   * Starts a one-shot task with the given parameters. If a task with the same ID
   * already exists, it will be stopped and replaced by the new one.
   *
   * @param taskId              unique identifier for the task
   * @param task                the {@link Runnable} task logic to execute
   * @param delayInMilliseconds the delay before executing the task, in milliseconds
   */
  void scheduleOneShotTask(String taskId, Runnable task, long delayInMilliseconds);

  /**
   * Starts a recurring task with the given parameters. If a task with the same ID
   * already exists, it will be stopped and replaced by the new one.
   *
   * @param taskId unique identifier for the task
   * @param task   the {@link Runnable} task logic to execute
   * @param delay  the initial delay before the first execution, in milliseconds
   * @param period the period between subsequent executions, in milliseconds
   */
  void scheduleTask(String taskId, Runnable task, long delay, long period);

  /**
   * Stops and removes the scheduled task with the given ID, if it exists.
   *
   * @param taskId the unique identifier of the task to stop
   */
  void stopExistingTaskIfPresent(String taskId);
}