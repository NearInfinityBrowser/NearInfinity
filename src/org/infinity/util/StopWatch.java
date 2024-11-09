// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A "Stopwatch" implementation that provides functionality for starting/stopping, pausing/resuming, and querying
 * elapsed time.
 *
 * <p>The class provides {@link ActionListener} support to fire in regular intervals. Time is measured in
 * milliseconds.</p>
 */
public class StopWatch {
  /** Factor for converting between milliseconds and seconds. */
  private static final long SECONDS_MULTIPLIER = 1000L;

  private final EventListenerList listenerList = new EventListenerList();

  // Runner for triggering delayed action events.
  private final Thread runner = new Thread(new Runner());

  // relative starting time of a timer session
  private long timeBase;

  // time value taken when the timer is paused
  private long timePaused;

  // user-defined delay between events
  private long delay;

  // time when the next event should be fired
  private long timeDelay;

  // indicates whether the timer is paused
  private boolean paused;

  /**
   * Converts a measured time value from milliseconds to seconds.
   *
   * @param time Time in milliseconds.
   * @return Time in seconds.
   */
  public static long toSeconds(long time) {
    return time / SECONDS_MULTIPLIER;
  }

  /**
   * Initializes a new stopwatch timer.
   *
   * @param start Specify {@code true} to automatically start the timer.
   */
  public StopWatch(boolean start) {
    this(0L, start);
  }

  /**
   * Initializes a new stopwatch timer.
   *
   * @param delay Interval delay for firing action events, in milliseconds.
   * @param start Specify {@code true} to automatically start the timer.
   */
  public StopWatch(long delay, boolean start) {
    runner.start();
    setDelay(delay);
    paused = !start;
    reset();
    if (!start) {
      pause();
    }
  }

  /** Starts a new timer session. Old timer session is discarded. */
  public synchronized void reset() {
    timeBase = System.currentTimeMillis();
    if (hasDelay()) {
      calculateTimer(timeBase);
    }
    if (paused) {
      timePaused = timeBase;
    }
  }

  /** Returns whether the timer is paused. */
  public boolean isPaused() {
    return paused;
  }

  /** Pauses the current timer session. */
  public synchronized void pause() {
    if (!paused) {
      paused = true;
      timePaused = System.currentTimeMillis();
    }
  }

  /** Resumes a previously paused timer session. */
  public synchronized void resume() {
    if (paused) {
      final long timeDiff = System.currentTimeMillis() - timePaused;
      timeBase += timeDiff;
      paused = false;
      runner.interrupt();
    }
  }

  /**
   * Returns the total elapsed time since the current timer session started, in milliseconds. Paused durations are
   * ignored.
   */
  public long elapsed() {
    if (isPaused()) {
      return timePaused - timeBase;
    } else {
      final long curTime = System.currentTimeMillis();
      return curTime - timeBase;
    }
  }

  /** Internally used to query whether a delay for firing action events has been defined. */
  private boolean hasDelay() {
    return (delay > 0);
  }

  /**
   * Returns the defined delay for firing action events, in milliseconds.
   * A value of 0 indicates that no delay has been defined.
   */
  public long getDelay() {
    return delay;
  }

  /**
   * Sets the delay for firing action events.
   *
   * @param delay Delay between event intervals, in milliseconds. Specify {@code 0} to disable firing events.
   */
  public synchronized void setDelay(long delay) {
    delay = Math.max(0L, delay);
    if (delay != this.delay) {
      this.delay = delay;
      calculateTimer(System.currentTimeMillis());
    }
  }

  /** Adds an {@link ActionListener} to the stopwatch. */
  public void addActionListener(ActionListener l) {
    if (l != null) {
      listenerList.add(ActionListener.class, l);
    }
  }

  /** Returns all registered {@link ActionListener}s for this stopwatch instance. */
  public ActionListener[] getActionListeners() {
    return listenerList.getListeners(ActionListener.class);
  }

  /** Removes an {@link ActionListener} from the stopwatch instance. */
  public void removeActionListener(ActionListener l) {
    if (l != null) {
      listenerList.remove(ActionListener.class, l);
    }
  }

  @Override
  public String toString() {
    final String state = isPaused() ? "paused" : "running";
    final long curTime = elapsed();
    return "Elapsed time: " + curTime + " ms [state=" + state + "]";
  }

  private void fireActionPerformed(ActionEvent event) {
    if (event != null) {
      Object[] listeners = listenerList.getListenerList();
      ActionEvent e = null;
      for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ActionListener.class) {
          // Event object is lazily created
          if (e == null) {
            final String actionCommand = event.getActionCommand() != null ? event.getActionCommand() : "";
            e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand, event.getWhen(),
                event.getModifiers());
          }
          ((ActionListener)listeners[i + 1]).actionPerformed(e);
        }
      }
    }
  }

  /** Used internally to calculate the time for firing the next action event. */
  private void calculateTimer(long timeBase) {
    if (hasDelay()) {
      timeDelay = timeBase + delay;
    }
  }

  /** Internal helper class for firing timed action events. */
  private class Runner implements Runnable {
    @Override
    public void run() {
//      Logger.debug("Runner started");
      while (true) {
        if (StopWatch.this.isPaused()) {
          try {
//            Logger.debug("Runner going to sleep");
            Thread.sleep(Long.MAX_VALUE);
          } catch (InterruptedException e) {
//            Logger.debug("Runner awakened at: {}", System.currentTimeMillis());
//            if (StopWatch.this.hasDelay()) {
//              Logger.debug("Next event at: {}", StopWatch.this.timeDelay);
//            }
          }
        } else {
          if (StopWatch.this.hasDelay() && !StopWatch.this.isPaused()) {
            final long curTime = System.currentTimeMillis();
            if (curTime >= StopWatch.this.timeDelay) {
//              Logger.debug("Event triggered at: {}", curTime);
              StopWatch.this.calculateTimer(curTime);
              final ActionEvent actionEvent = new ActionEvent(StopWatch.this, 0, "", curTime, 0);
              SwingUtilities.invokeLater(() -> fireActionPerformed(actionEvent));
            }
          }

          try {
            Thread.sleep(1L);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }
}