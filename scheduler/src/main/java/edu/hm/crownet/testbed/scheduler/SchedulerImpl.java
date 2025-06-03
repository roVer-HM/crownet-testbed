package edu.hm.crownet.testbed.scheduler;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Default implementation of the {@link Scheduler} interface using a shared {@link ScheduledExecutorService}
 * to manage recurring tasks.
 *
 * <p>Each task is tracked using a unique task ID and can be started or stopped individually.
 */
@Service
public class SchedulerImpl implements Scheduler {

  /**
   * Scheduled executor with a thread pool size equal to the number of available processors.
   */
  private final ScheduledExecutorService scheduler = newScheduledThreadPool(getRuntime().availableProcessors());

  /**
   * A thread-safe map that holds the currently scheduled tasks by their unique identifiers.
   */
  private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void scheduleOneShotTask(String taskId, Runnable task, long delayInMilliseconds) {
    stopScheduledTask(taskId); // stop existing if present
    var future = scheduler.schedule(task, delayInMilliseconds, MILLISECONDS);
    tasks.put(taskId, future);
  }

  @Override
  public void scheduleTask(String taskId, Runnable task, long delay, long period) {
    stopScheduledTask(taskId); // stop existing if present
    var future = scheduler.scheduleAtFixedRate(task, delay, period, MILLISECONDS);
    tasks.put(taskId, future);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stopScheduledTask(String taskId) {
    var future = tasks.remove(taskId);
    if (future != null) {
      future.cancel(false);
    }
  }
}