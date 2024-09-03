// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.tinylog.Logger;

/**
 * A convenience class for performing multiple tasks in parallel.
 */
public class Threading implements AutoCloseable {
  /** Controls the amount of threads to allocate by a new thread pool. */
  public enum Priority {
    /** Always allocates a single thread even if more are available. */
    LOWEST(0.0),
    /** Allocates about 25 percent of available free threads, but always at least one thread. */
    LOW(0.25),
    /** Allocates about 50 percent of available free threads, but always at least one thread. */
    NORMAL(0.5),
    /** Allocates about 75 percent of available free threads, but always at least one thread. */
    HIGH(0.75),
    /** Allocates all available free threads, but always at least one thread. */
    HIGHEST(1.0),

    /** Always allocates about 25 percent of {@link Threading#MAX_THREADS_AVAILABLE} threads. */
    ABS_LOW(-0.25),
    /** Always allocates about 50 percent of {@link Threading#MAX_THREADS_AVAILABLE} threads. */
    ABS_NORMAL(-0.5),
    /** Always allocates about 75 percent of {@link Threading#MAX_THREADS_AVAILABLE} threads. */
    ABS_HIGH(-0.75),
    /** Always allocates all of {@link Threading#MAX_THREADS_AVAILABLE} threads. */
    ABS_HIGHEST(-1.0),
    ;

    private final double factor;

    private Priority(double factor) {
      this.factor = Math.max(-1.0, Math.min(1.0, factor));
    }

    /**
     * Returns the thread allocation factor.<br>
     * A positive value refers to the currently available number of free threads.<br>
     * A negative value refers to the max. number of available threads {@link Threading#MAX_THREADS_AVAILABLE}.
     */
    public double getFactor() {
      return factor;
    }

    @Override
    public String toString() {
      return this.name() + " [factor=" + this.factor + "]";
    }
  }

  /** Defines the total number of threads that can be executed in parallel on the current system. */
  public static final int MAX_THREADS_AVAILABLE = Runtime.getRuntime().availableProcessors();

  /** Contains the number of remaining threads for use by new thread pools. */
  private static final AtomicInteger THREADS_AVAILABLE = new AtomicInteger(MAX_THREADS_AVAILABLE);


  /** Stores submitted tasks for internal evaluation purposes. */
  private final List<Future<?>> taskList = new LinkedList<>();

  private final ThreadPoolExecutor executor;
  private final int numThreads;

  private boolean closed;
  private Object initialized;
  private Object released;

  /**
   * Initializes a new {@link Threading} object with {@link Priority#NORMAL}.
   */
  public Threading() {
    this(Priority.NORMAL);
  }

  /**
   * Initializes a new {@link Threading} object.
   *
   * @param priority {@link Priority} value that is used to calculate the optimal number of threads for this thread
   *                   pool.
   */
  public Threading(Priority priority) {
    this(calculateThreadCount(priority));
  }

  /**
   * Initializes a new {@link Threading} object.
   *
   * @param numThreads Max. number of active threads.
   * @throws IllegalArgumentException if <code>numThreads <= 0</code>.
   */
  private Threading(int numThreads) {
    this.numThreads = getValidatedThreadCount(numThreads);
    this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(getThreadCount());
    allocateThreads();
  }

  /**
   * Returns the number of threads reserved by this thread pool.
   *
   * @return number of threads reserved by this thread pool.
   */
  public int getThreadCount() {
    return numThreads;
  }

  /**
   * Returns {@code true} if the task list contains one or more entries.
   *
   * @param forceUpdate Specify {@code true} to remove all completed or cancelled tasks before calculating the result.
   * @return {@code true} if the task list contains one or more tasks that have not yet been completed or cancelled,
   *         {@code false} otherwise.
   */
  public boolean hasTasks(boolean forceUpdate) {
    if (forceUpdate) {
      dispose();
    }
    return !taskList.isEmpty();
  }

  /**
   * Returns the approximate number of threads that are actively executing tasks.
   *
   * @return the number of threads.
   */
  public int getActiveThreadCount() {
    return executor.getActiveCount();
  }

  /**
   * Returns {@code true} if there are any tasks submitted to this thread pool that have not yet begun executing.
   *
   * @return {@code true} if there are any queued submissions.
   */
  public boolean hasQueuedSubmissions() {
    return !executor.getQueue().isEmpty();
  }

  /**
   * Returns an estimate of the number of tasks submitted to this pool that have not yet begun executing.
   *
   * @return the number of queued submissions.
   */
  public int getQueuedSubmissionCount() {
    return executor.getQueue().size();
  }

  /**
   * Returns {@code true} if this pool has been shut down.
   *
   * @return {@code true} if this pool has been shut down.
   */
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  /**
   * Returns {@code true} if all tasks have completed following shut down.
   *
   * @return {@code true} if all tasks have completed following shut down.
   */
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  /**
   * This method removes all completed or cancelled tasks from the task list.
   *
   * @return Number of remaining {@link ForkJoinTask} instances that haven been completed or cancelled yet.
   */
  public synchronized int dispose() {
    taskList.removeIf(Future::isDone);
    return taskList.size();
  }

  /**
   * Possibly initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be
   * accepted. Tasks that are in the process of being submitted concurrently during the course of this method may or may
   * not be rejected.
   */
  public void shutdown() {
    executor.shutdown();
    releaseThreads();
  }

  /**
   * Possibly attempts to cancel and/or stop all tasks, and reject all subsequently submitted tasks. Invocation has no
   * additional effect if already shut down. Otherwise, tasks that are in the process of being submitted or executed
   * concurrently during the course of this method may or may not be rejected. This method cancels both existing and
   * not executed tasks, in order to permit termination in the presence of task dependencies.
   */
  public void shutdownNow() {
    executor.shutdownNow();
    releaseThreads();
    dispose();
  }

  /**
   * Submits a new task to be executed by the thread pool and returns a {@code Future} representing that task. The
   * Future's {@code get} method will return the given result upon successful completion.
   *
   * @param <T>  Type of the task's result.
   * @param task The task to submit.
   * @return a {@link Future} representing pending completion of the task.
   * @throws RejectedExecutionException if the task cannot bescheduled for execution.
   * @throws NullPointerException       if the task is {@code null}.
   */
  public <T> Future<T> submit(Callable<T> task) {
    Objects.requireNonNull(task, "Task is null");
    return registerFuture(executor.submit(task));
  }

  /**
   * Submits a {@code Runnable} task for execution in the thread pool and returns a {@code Future} representing that
   * task. The Future's {@code get} method will return the given result upon successful completion.
   *
   * @param <T>    Type of the result.
   * @param task   The task to submit, as {@link Runnable} object.
   * @param result The result to return.
   * @return a {@link Future} representing pending completion of the task.
   * @throws RejectedExecutionException if the task cannot bescheduled for execution.
   * @throws NullPointerException       if the task is {@code null}.
   */
  public <T> Future<T> submit(Runnable task, T result) {
    Objects.requireNonNull(task, "Task is null");
    return registerFuture(executor.submit(task, result));
  }

  /**
   * Submits a {@code Runnable} task for execution in the thread pool and returns a {@code Future} representing that
   * task. The Future's {@code get} method will return {@code null} upon successful completion.
   *
   * @param task The task to submit, as {@link Runnable} object.
   * @return a {@link Future} representing pending completion of the task.
   * @throws RejectedExecutionException if the task cannot bescheduled for execution.
   * @throws NullPointerException       if the task is {@code null}.
   */
  public Future<?> submit(Runnable task) {
    Objects.requireNonNull(task, "Task is null");
    return registerFuture(executor.submit(task));
  }

  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    Objects.requireNonNull(tasks, "Tasks collection is null");

    final List<Future<T>> retVal = executor.invokeAll(tasks);
    for (final Future<T> future : retVal) {
      registerFuture(future);
    }
    return retVal;
  }

  /**
   * Blocks until all submitted tasks have completed execution, or the current thread is interrupted, whichever happens
   * first. Unlike {@link #awaitTermination(long, TimeUnit)} this method does not depend on a shutdown request, which
   * allows to submit more tasks after completion.
   *
   * @return {@code true} if all submitted tasks terminated and {@code false} if the timeout elapsed before termination.
   * @throws InterruptedException if interrupted while waiting.
   */
  public void waitForCompletion() throws InterruptedException {
    waitForCompletion(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  /**
   * Blocks until all submitted tasks have completed execution, or the timeout occurs, or the current thread is
   * interrupted, whichever happens first. Unlike {@link #awaitTermination(long, TimeUnit)} this method does not
   * depend on a shutdown request, which allows to submit more tasks after completion.
   *
   * @param timeout the maximum time to wait.
   * @param unit    the time unit of the timeout argument.
   * @return {@code true} if all submitted tasks terminated and {@code false} if the timeout elapsed before termination.
   * @throws InterruptedException if interrupted while waiting.
   */
  public boolean waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
    boolean retVal = !hasTasks(true);

    long nsTimeOut = unit.toNanos(timeout);
    long timeBase, timeElapsed;
    timeBase = System.nanoTime();
    while (!retVal && nsTimeOut > 0L) {
      retVal = !hasTasks(true);
      timeElapsed = System.nanoTime() - timeBase;
      nsTimeOut -= timeElapsed;

      if (nsTimeOut > 0L) {
        timeBase = System.nanoTime();
        Thread.sleep((nsTimeOut > 500_000L) ? 1L : 0L);
        timeElapsed = System.nanoTime() - timeBase;
        nsTimeOut -= timeElapsed;
      }

      timeBase = System.nanoTime();
    }

    return retVal;
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request, or the current thread is interrupted,
   * whichever happens first.
   *
   * @throws InterruptedException if interrupted while waiting.
   */
  public void awaitTermination() throws InterruptedException {
    awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  /**
   * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current
   * thread is interrupted, whichever happens first.
   *
   * @param timeout the maximum time to wait.
   * @param unit    the time unit of the timeout argument.
   * @return {@code true} if this thread pool terminated and {@code false} if the timeout elapsed before termination.
   * @throws InterruptedException if interrupted while waiting.
   */
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  /**
   * Performs an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
   * Tasks that are in the process of being submitted concurrently during the course of this method may or may not be
   * rejected.
   */
  @Override
  public void close() throws Exception {
    // initiating orderly shutdown
    shutdown();

    if (!closed) {
      closed = true;

      // waiting for all tasks to be done, or a timeout occurred
      final Thread cleanupThread = new Thread(() -> {
        long timeOutNs = TimeUnit.SECONDS.toNanos(60L);
        long timeBase = System.nanoTime();
        while (hasTasks(true) && timeOutNs > 0L) {
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
            Logger.trace(e);
          }
          final long timeElapsed = System.nanoTime() - timeBase;
          timeBase = System.nanoTime();
          timeOutNs -= timeElapsed;
        }
      });
      cleanupThread.setPriority(Thread.MIN_PRIORITY);
      cleanupThread.start();
    }
  }

  @Override
  public int hashCode() {
    return executor.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return executor.equals(obj);
  }

  @Override
  public String toString() {
    return "Threading [numThreads=" + numThreads + ", executor=" + executor + "]";
  }

  /**
   * Used internally to register new {@code Future} objects.
   *
   * @param <T> Type of the task's result.
   * @param future {@link ForkJoinTask} object to register.
   * @return the specified {@code future} argument.
   */
  private <T, U extends Future<T>> U registerFuture(U future) {
    Objects.requireNonNull(future, "Future is null");
    taskList.add(future);
    return future;
  }

  /**
   * Reserves the amount of threads defined by the given {@code Threading} object and updates
   * {@link #THREADS_AVAILABLE}.
   *
   * @return {@code true} if reservation completed successfully, {@code false} otherwise.
   */
  private synchronized boolean allocateThreads() {
    if (initialized == null && released == null) {
      THREADS_AVAILABLE.set(Math.max(1, THREADS_AVAILABLE.get() - numThreads));
      initialized = new Object();
      return true;
    }
    return false;
  }

  /**
   * Releases the amount of threads defined by the given {@code Threading} object and updates
   * {@link #THREADS_AVAILABLE}.
   *
   * @return {@code true} if release complete successfully, {@code false} otherwise.
   */
  private synchronized boolean releaseThreads() {
    if (initialized != null && released == null) {
      THREADS_AVAILABLE.set(Math.min(MAX_THREADS_AVAILABLE, THREADS_AVAILABLE.get() + numThreads));
      released = new Object();
      return true;
    }
    return false;
  }

  /**
   * Calculates the number of threads to reserve based on the given {@code priority}.
   *
   * @param priority the thread {@link Priority}. Default: {@link Priority#NORMAL}
   * @return Number of threads to reserve according to the given {@code priority}.
   */
  public static int calculateThreadCount(Priority priority) {
    if (priority == null) {
      priority = Priority.NORMAL;
    }

    final int numThreadsTotal = (priority.getFactor() < 0) ? MAX_THREADS_AVAILABLE : THREADS_AVAILABLE.get();
    final int numThreadsCalculated = (int) Math.floor(Math.abs(priority.getFactor()) * numThreadsTotal);
    return Math.max(1, Math.min(MAX_THREADS_AVAILABLE, numThreadsCalculated));
  }

  /**
   * Returns the number of currently available threads that can be allocated by new {@link Threading} objects.
   *
   * @return number of currently available threads.
   */
  public static int getAvailableThreads() {
    return THREADS_AVAILABLE.get();
  }

  /**
   * Ensures that the returned thread count is always within the range of <code>[1,  # available processors]</code>.
   *
   * @param numThreads Thread count to validate.
   * @return The validated thread count.
   */
  private static int getValidatedThreadCount(int numThreads) {
    return Math.max(1, Math.min(MAX_THREADS_AVAILABLE, numThreads));
  }
}
