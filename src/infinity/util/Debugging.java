package infinity.util;

/**
 * Collection of static methods for debugging and profiling needs.
 * @author argent77
 */
public class Debugging
{
  /**
   * Supported temporal resolutions for timer methods.
   */
  public enum TimeFormat { NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS }

  private static long timeBase = System.nanoTime();

  /**
   * Resets timer to current time.
   */
  public static synchronized void timerReset()
  {
    timeBase = System.nanoTime();
  }

  /**
   * Shows elapsed time in the desired resolution and resets timer.
   * @param msg Display an optional message
   * @param fmt The temporaral resolution of the elapsed time
   */
  public static synchronized void timerShow(String msg, TimeFormat fmt)
  {
    if (msg != null && !msg.isEmpty())
      System.out.println("[" + msg + "] " + toTimeFormatString(fmt, System.nanoTime() - timeBase));
    else
      System.out.println(toTimeFormatString(fmt, System.nanoTime() - timeBase));
    timerReset();
  }

  /**
   * Returns elapsed time in the desired resolution and resets timer.
   * @param fmt The temporal resolution of the elapsed time
   * @return The elapsed time in the specified resolution
   */
  public static synchronized long timerGet(TimeFormat fmt)
  {
    long time = toTimeFormat(fmt, System.nanoTime() - timeBase);
    timerReset();
    return time;
  }


// ------------------------------ PRIVATE METHODS ------------------------------

  private static long toTimeFormat(TimeFormat fmt, long time)
  {
    switch (fmt) {
      case MICROSECONDS:
        return time / 1000L;
      case MILLISECONDS:
        return time / 1000000L;
      case SECONDS:
        return time / 1000000000L;
      default:
        return time;
    }
  }

  private static String toTimeFormatString(TimeFormat fmt, long time)
  {
    switch (fmt) {
      case NANOSECONDS:
        return toTimeFormat(fmt, time) + " ns";
      case MICROSECONDS:
        return toTimeFormat(fmt, time) + " µs";
      case MILLISECONDS:
        return toTimeFormat(fmt, time) + " ms";
      case SECONDS:
        return toTimeFormat(fmt, time) + " s";
      default:
        return Long.toString(time);
    }
  }
}
