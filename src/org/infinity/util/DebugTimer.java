// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * Provides methods for measuring time.
 */
public class DebugTimer {
  /** Supported temporal resolutions for timer methods. */
  public enum TimeFormat {
    NANOSECONDS(1L, "ns"),
    MICROSECONDS(1_000L, "µs"),
    MILLISECONDS(1_000_000L, "ms"),
    SECONDS(1_000_000_000L, "s"),
    ;

    private final long factor;
    private final String unit;

    TimeFormat(long factor, String unit) {
      this.factor = factor;
      this.unit = unit;
    }

    /** Returns the scale factor to convert a value in nanoseconds into the desired time format. */
    public long getScaleFactor() {
      return factor;
    }

    /** Returns the time unit as string. */
    public String getUnit() {
      return unit;
    }

    /**
     * Converts the value into the current time unit.
     *
     * @param nanos Time value in nanoseconds.
     * @return Value in the current time unit.
     */
    public long get(long nanos) {
      return nanos / factor;
    }

    /**
     * Returns a string representation of the value in the current time unit.
     *
     * @param nanos Time value in nanoseconds.
     * @return String of the converted time value with time unit symbol.
     */
    public String toString(long nanos) {
      return get(nanos) + " " + getUnit();
    }
  }

  /** Static class instance for global access to the timer. */
  private static final DebugTimer INSTANCE = new DebugTimer();

  /** Provides access to the global instance of the {@code Debugging} class. */
  public static synchronized DebugTimer getInstance() {
    return INSTANCE;
  }

  private TimeFormat defaultFormat;
  private long timeBase;

  /** Creates a new {@code Debugging} object with the default time format {@link TimeFormat#MILLISECONDS}. */
  public DebugTimer() {
    this(null);
  }

  /**
   *  Creates a new {@code Debugging} object with the specified {@link TimeFormat}.
   *
   * @param defaultFormat the default time format to use by the non-parameterized timer methods.
   */
  public DebugTimer(TimeFormat defaultFormat) {
    this.timeBase = System.nanoTime();
    this.defaultFormat = getTimeFormat(defaultFormat);
  }

  /** Resets the timer to the current system time. */
  public synchronized DebugTimer timerReset() {
    timeBase = System.nanoTime();
    return this;
  }

  /**
   * Shows the elapsed time in the default time format and resets timer.
   *
   * @param message Display an optional message
   */
  public DebugTimer timerShow(String message) {
    return timerShow(message, getDefaultTimeFormat());
  }

  /**
   * Shows the elapsed time in the specified time format.
   *
   * @param message Display an optional message
   * @param format  The temporal resolution of the elapsed time
   */
  public DebugTimer timerShow(String message, TimeFormat format) {
    final long timeDiff = timerGetRaw();
    format = getTimeFormat(format);
    if (message != null && !message.isEmpty()) {
      System.out.println("[" + message + "] " + format.toString(timeDiff));
    } else {
      System.out.println(format.toString(timeDiff));
    }
    return this;
  }

  /**
   * Returns the elapsed time since the last timer reset in the default time format.
   *
   * @return The elapsed time in the specified resolution
   */
  public long timerGet() {
    return timerGet(getDefaultTimeFormat());
  }

  /**
   * Returns the elapsed time since the last timer reset in the specified time format.
   *
   * @param format  The temporal resolution of the elapsed time
   * @return The elapsed time in the specified resolution
   */
  public long timerGet(TimeFormat format) {
    final long timeDiff = timerGetRaw();
    return getTimeFormat(format).get(timeDiff) ;
  }

  /** Returns the default {@link TimeFormat}. for use with the non-parameterized methods for getting the timer value. */
  public TimeFormat getDefaultTimeFormat() {
    return defaultFormat;
  }

  /**
   * Defines a new default {@link TimeFormat} for use with the non-parameterized methods for getting the
   * timer value.
   *
   * @param newTimeFormat new default {@link TimeFormat}.
   */
  public void setDefaultTimeFormat(TimeFormat newTimeFormat) {
    this.defaultFormat = (newTimeFormat != null) ? newTimeFormat : TimeFormat.MILLISECONDS;
  }

  /** Returns a non-{@code null} {@link TimeFormat} object. */
  private TimeFormat getTimeFormat(TimeFormat fmt) {
    return (fmt != null) ? fmt : TimeFormat.MILLISECONDS;
  }

  /** Synchronized access to the elapsed time since last timer reset. */
  private synchronized long timerGetRaw() {
    return System.nanoTime() - timeBase;
  }
}
