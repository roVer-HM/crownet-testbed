package edu.hm.crownet.testbed.scheduler.impl;

import edu.hm.crownet.testbed.scheduler.Scheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Implementation of the Scheduler interface using a ScheduledExecutorService.
 * This class allows scheduling one-shot and periodic tasks, as well as stopping existing tasks by their unique identifiers.
 */
@Service
public class SchedulerImpl implements Scheduler {

  /**
   * A ScheduledExecutorService that manages the scheduling and execution of tasks.
   * The thread pool size is set to the number of available processors to optimize performance.
   */
  private final ScheduledExecutorService scheduler = newScheduledThreadPool(getRuntime().availableProcessors());

  /**
   * A thread-safe map that holds the scheduled tasks, allowing for quick access and management of tasks by their unique identifiers.
   */
  private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

  @Override
  public void scheduleOneShotTask(String taskId, Runnable task, long delayInMilliseconds) {
    stopExistingTaskIfPresent(taskId);
    var future = scheduler.schedule(task, delayInMilliseconds, MILLISECONDS);
    tasks.put(taskId, future);
  }

  @Override
  public void scheduleTask(String taskId, Runnable task, long delay, long period) {
    stopExistingTaskIfPresent(taskId);
    var future = scheduler.scheduleAtFixedRate(task, delay, period, MILLISECONDS);
    tasks.put(taskId, future);
  }

  @Override
  public void stopExistingTaskIfPresent(String taskId) {
    var future = tasks.remove(taskId);
    if (future != null) {
      future.cancel(false);
    }
  }
}