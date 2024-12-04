// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import org.infinity.AppOption;
import org.tinylog.Level;
import org.tinylog.Supplier;
import org.tinylog.TaggedLogger;

/**
 * Wrapper class for the {@link org.tinylog.Logger} class from the tinylog package to provide more control over log
 * level visibility.
 */
public class Logger {
  /**
   * Gets a tagged logger instance. Tags are case-sensitive.
   *
   * @param tag Tag for logger or {@code null} for receiving an untagged logger
   * @return Logger instance
   */
  public static TaggedLogger tag(final String tag) {
    return org.tinylog.Logger.tag(tag);
  }

  /**
   * Gets a tagged logger instance that logs to multiple tags. Tags are case-sensitive.
   *
   * @param tags Tags for the logger or nothing for an untagged logger. If specified, each tag should be unique
   * @return Logger instance
   */
  public static TaggedLogger tags(final String... tags) {
    return org.tinylog.Logger.tags(tags);
  }


  /**
   * Logs a message at the specified log level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void log(final Level logLevel, final Object message) {
    switch (logLevel) {
      case TRACE:
        trace(message);
        break;
      case DEBUG:
        debug(message);
        break;
      case INFO:
        info(message);
        break;
      case WARN:
        warn(message);
        break;
      case ERROR:
        error(message);
        break;
      default:
    }
  }

  /**
   * Logs a lazy message at the specified log level. The message will be only evaluated if the log entry is
   * really output.
   *
   * @param message Function that produces the message
   */
  public static void log(final Level logLevel, final Supplier<?> message) {
    switch (logLevel) {
      case TRACE:
        trace(message);
        break;
      case DEBUG:
        debug(message);
        break;
      case INFO:
        info(message);
        break;
      case WARN:
        warn(message);
        break;
      case ERROR:
        error(message);
        break;
      default:
    }
  }

  /**
   * Logs a formatted message at the specified log level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void log(final Level logLevel, final String message, final Object... arguments) {
    switch (logLevel) {
      case TRACE:
        trace(message, arguments);
        break;
      case DEBUG:
        debug(message, arguments);
        break;
      case INFO:
        info(message, arguments);
        break;
      case WARN:
        warn(message, arguments);
        break;
      case ERROR:
        error(message, arguments);
        break;
      default:
    }
  }

  /**
   * Logs a formatted message at the specified log level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void log(final Level logLevel, final String message, final Supplier<?>... arguments) {
    switch (logLevel) {
      case TRACE:
        trace(message, arguments);
        break;
      case DEBUG:
        debug(message, arguments);
        break;
      case INFO:
        info(message, arguments);
        break;
      case WARN:
        warn(message, arguments);
        break;
      case ERROR:
        error(message, arguments);
        break;
      default:
    }
  }

  /**
   * Logs an exception at the specified log level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void log(final Level logLevel, final Throwable exception) {
    switch (logLevel) {
      case TRACE:
        trace(exception);
        break;
      case DEBUG:
        debug(exception);
        break;
      case INFO:
        info(exception);
        break;
      case WARN:
        warn(exception);
        break;
      case ERROR:
        error(exception);
        break;
      default:
    }
  }

  /**
   * Logs an exception with a custom message at the specified log level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void log(final Level logLevel, final Throwable exception, final String message) {
    switch (logLevel) {
      case TRACE:
        trace(exception, message);
        break;
      case DEBUG:
        debug(exception, message);
        break;
      case INFO:
        info(exception, message);
        break;
      case WARN:
        warn(exception, message);
        break;
      case ERROR:
        error(exception, message);
        break;
      default:
    }
  }

  /**
   * Logs an exception with a custom lazy message at the specified log level. The message will be only evaluated if the
   * log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void log(final Level logLevel, final Throwable exception, final Supplier<String> message) {
    switch (logLevel) {
      case TRACE:
        trace(exception, message);
        break;
      case DEBUG:
        debug(exception, message);
        break;
      case INFO:
        info(exception, message);
        break;
      case WARN:
        warn(exception, message);
        break;
      case ERROR:
        error(exception, message);
        break;
      default:
    }
  }

  /**
   * Logs an exception with a formatted custom message at the specified log level. "{}" placeholders will be replaced by
   * given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void log(final Level logLevel, final Throwable exception, final String message,
      final Object... arguments) {
    switch (logLevel) {
      case TRACE:
        trace(exception, message, arguments);
        break;
      case DEBUG:
        debug(exception, message, arguments);
        break;
      case INFO:
        info(exception, message, arguments);
        break;
      case WARN:
        warn(exception, message, arguments);
        break;
      case ERROR:
        error(exception, message, arguments);
        break;
      default:
    }
  }

  /**
   * Logs an exception with a formatted message at the specified log level. "{}" placeholders will be replaced by given
   * lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void log(final Level logLevel, final Throwable exception, final String message,
      final Supplier<?>... arguments) {
    switch (logLevel) {
      case TRACE:
        trace(exception, message, arguments);
        break;
      case DEBUG:
        debug(exception, message, arguments);
        break;
      case INFO:
        info(exception, message, arguments);
        break;
      case WARN:
        warn(exception, message, arguments);
        break;
      case ERROR:
        error(exception, message, arguments);
        break;
      default:
    }
  }

  /**
   * Checks whether log entries at {@link Level#TRACE TRACE} level will be output.
   *
   * @return {@code true} if {@link Level#TRACE TRACE} level is enabled, {@code false} if disabled
   */
  public static boolean isTraceEnabled() {
    return isLogLevelEnabled(Level.TRACE);
  }

  /**
   * Logs a message at {@link Level#TRACE TRACE} level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void trace(final Object message) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(message);
  }

  /**
   * Logs a lazy message at {@link Level#TRACE TRACE} level. The message will be only evaluated if the log entry is
   * really output.
   *
   * @param message Function that produces the message
   */
  public static void trace(final Supplier<?> message) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(message);
  }

  /**
   * Logs a formatted message at {@link Level#TRACE TRACE} level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void trace(final String message, final Object... arguments) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(message, arguments);
  }

  /**
   * Logs a formatted message at {@link Level#TRACE TRACE} level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void trace(final String message, final Supplier<?>... arguments) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(message, arguments);
  }

  /**
   * Logs an exception at {@link Level#TRACE TRACE} level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void trace(final Throwable exception) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(exception);
  }

  /**
   * Logs an exception with a custom message at {@link Level#TRACE TRACE} level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void trace(final Throwable exception, final String message) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(exception, message);
  }

  /**
   * Logs an exception with a custom lazy message at {@link Level#TRACE TRACE} level. The message will be only evaluated
   * if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void trace(final Throwable exception, final Supplier<String> message) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(exception, message);
  }

  /**
   * Logs an exception with a formatted custom message at {@link Level#TRACE TRACE} level. "{}" placeholders will be
   * replaced by given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void trace(final Throwable exception, final String message, final Object... arguments) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(exception, message, arguments);
  }

  /**
   * Logs an exception with a formatted message at {@link Level#TRACE TRACE} level. "{}" placeholders will be replaced
   * by given lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void trace(final Throwable exception, final String message, final Supplier<?>... arguments) {
    if (isTraceEnabled())
      org.tinylog.Logger.trace(exception, message, arguments);
  }

  /**
   * Checks whether log entries at {@link Level#DEBUG DEBUG} level will be output.
   *
   * @return {@code true} if {@link Level#DEBUG DEBUG} level is enabled, {@code false} if disabled
   */
  public static boolean isDebugEnabled() {
    return isLogLevelEnabled(Level.DEBUG);
  }

  /**
   * Logs a message at {@link Level#DEBUG DEBUG} level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void debug(final Object message) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(message);
  }

  /**
   * Logs a lazy message at {@link Level#DEBUG DEBUG} level. The message will be only evaluated if the log entry is
   * really output.
   *
   * @param message Function that produces the message
   */
  public static void debug(final Supplier<?> message) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(message);
  }

  /**
   * Logs a formatted message at {@link Level#DEBUG DEBUG} level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void debug(final String message, final Object... arguments) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(message, arguments);
  }

  /**
   * Logs a formatted message at {@link Level#DEBUG DEBUG} level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void debug(final String message, final Supplier<?>... arguments) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(message, arguments);
  }

  /**
   * Logs an exception at {@link Level#DEBUG DEBUG} level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void debug(final Throwable exception) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(exception);
  }

  /**
   * Logs an exception with a custom message at {@link Level#DEBUG DEBUG} level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void debug(final Throwable exception, final String message) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(exception, message);
  }

  /**
   * Logs an exception with a custom lazy message at {@link Level#DEBUG DEBUG} level. The message will be only evaluated
   * if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void debug(final Throwable exception, final Supplier<String> message) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(exception, message);
  }

  /**
   * Logs an exception with a formatted custom message at {@link Level#DEBUG DEBUG} level. "{}" placeholders will be
   * replaced by given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void debug(final Throwable exception, final String message, final Object... arguments) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(exception, message, arguments);
  }

  /**
   * Logs an exception with a formatted message at {@link Level#DEBUG DEBUG} level. "{}" placeholders will be replaced
   * by given lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void debug(final Throwable exception, final String message, final Supplier<?>... arguments) {
    if (isDebugEnabled())
      org.tinylog.Logger.debug(exception, message, arguments);
  }

  /**
   * Checks whether log entries at {@link Level#INFO INFO} level will be output.
   *
   * @return {@code true} if {@link Level#INFO INFO} level is enabled, {@code false} if disabled
   */
  public static boolean isInfoEnabled() {
    return isLogLevelEnabled(Level.INFO);
  }

  /**
   * Logs a message at {@link Level#INFO INFO} level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void info(final Object message) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(message);
  }

  /**
   * Logs a lazy message at {@link Level#INFO INFO} level. The message will be only evaluated if the log entry is really
   * output.
   *
   * @param message Function that produces the message
   */
  public static void info(final Supplier<?> message) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(message);
  }

  /**
   * Logs a formatted message at {@link Level#INFO INFO} level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void info(final String message, final Object... arguments) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(message, arguments);
  }

  /**
   * Logs a formatted message at {@link Level#INFO INFO} level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void info(final String message, final Supplier<?>... arguments) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(message, arguments);
  }

  /**
   * Logs an exception at {@link Level#INFO INFO} level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void info(final Throwable exception) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(exception);
  }

  /**
   * Logs an exception with a custom message at {@link Level#INFO INFO} level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void info(final Throwable exception, final String message) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(exception, message);
  }

  /**
   * Logs an exception with a custom lazy message at {@link Level#INFO INFO} level. The message will be only evaluated
   * if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void info(final Throwable exception, final Supplier<String> message) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(exception, message);
  }

  /**
   * Logs an exception with a formatted custom message at {@link Level#INFO INFO} level. "{}" placeholders will be
   * replaced by given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void info(final Throwable exception, final String message, final Object... arguments) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(exception, message, arguments);
  }

  /**
   * Logs an exception with a formatted message at {@link Level#INFO INFO} level. "{}" placeholders will be replaced by
   * given lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void info(final Throwable exception, final String message, final Supplier<?>... arguments) {
    if (isInfoEnabled())
      org.tinylog.Logger.info(exception, message, arguments);
  }

  /**
   * Checks whether log entries at {@link Level#WARN WARN} level will be output.
   *
   * @return {@code true} if {@link Level#WARN WARN} level is enabled, {@code false} if disabled
   */
  public static boolean isWarnEnabled() {
    return isLogLevelEnabled(Level.WARN);
  }

  /**
   * Logs a message at {@link Level#WARN WARN} level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void warn(final Object message) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(message);
  }

  /**
   * Logs a lazy message at {@link Level#WARN WARN} level. The message will be only evaluated if the log entry is really
   * output.
   *
   * @param message Function that produces the message
   */
  public static void warn(final Supplier<?> message) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(message);
  }

  /**
   * Logs a formatted message at {@link Level#WARN WARN} level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void warn(final String message, final Object... arguments) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(message, arguments);
  }

  /**
   * Logs a formatted message at {@link Level#WARN WARN} level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void warn(final String message, final Supplier<?>... arguments) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(message, arguments);
  }

  /**
   * Logs an exception at {@link Level#WARN WARN} level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void warn(final Throwable exception) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(exception);
  }

  /**
   * Logs an exception with a custom message at {@link Level#WARN WARN} level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void warn(final Throwable exception, final String message) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(exception, message);
  }

  /**
   * Logs an exception with a custom lazy message at {@link Level#WARN WARN} level. The message will be only evaluated
   * if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void warn(final Throwable exception, final Supplier<String> message) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(exception, message);
  }

  /**
   * Logs an exception with a formatted custom message at {@link Level#WARN WARN} level. "{}" placeholders will be
   * replaced by given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void warn(final Throwable exception, final String message, final Object... arguments) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(exception, message, arguments);
  }

  /**
   * Logs an exception with a formatted message at {@link Level#WARN WARN} level. "{}" placeholders will be replaced by
   * given lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void warn(final Throwable exception, final String message, final Supplier<?>... arguments) {
    if (isWarnEnabled())
      org.tinylog.Logger.warn(exception, message, arguments);
  }

  /**
   * Checks whether log entries at {@link Level#ERROR ERROR} level will be output.
   *
   * @return {@code true} if {@link Level#ERROR ERROR} level is enabled, {@code false} if disabled
   */
  public static boolean isErrorEnabled() {
    return isLogLevelEnabled(Level.ERROR);
  }

  /**
   * Logs a message at {@link Level#ERROR ERROR} level.
   *
   * @param message String or any other object with a meaningful {@link #toString()} method
   */
  public static void error(final Object message) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(message);
  }

  /**
   * Logs a lazy message at {@link Level#ERROR ERROR} level. The message will be only evaluated if the log entry is
   * really output.
   *
   * @param message Function that produces the message
   */
  public static void error(final Supplier<?> message) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(message);
  }

  /**
   * Logs a formatted message at {@link Level#ERROR ERROR} level. "{}" placeholders will be replaced by given arguments.
   *
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void error(final String message, final Object... arguments) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(message, arguments);
  }

  /**
   * Logs a formatted message at {@link Level#ERROR ERROR} level. "{}" placeholders will be replaced by given lazy
   * arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void error(final String message, final Supplier<?>... arguments) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(message, arguments);
  }

  /**
   * Logs an exception at {@link Level#ERROR ERROR} level.
   *
   * @param exception Caught exception or any other throwable to log
   */
  public static void error(final Throwable exception) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(exception);
  }

  /**
   * Logs an exception with a custom message at {@link Level#ERROR ERROR} level.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Text message to log
   */
  public static void error(final Throwable exception, final String message) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(exception, message);
  }

  /**
   * Logs an exception with a custom lazy message at {@link Level#ERROR ERROR} level. The message will be only evaluated
   * if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Function that produces the message
   */
  public static void error(final Throwable exception, final Supplier<String> message) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(exception, message);
  }

  /**
   * Logs an exception with a formatted custom message at {@link Level#ERROR ERROR} level. "{}" placeholders will be
   * replaced by given arguments.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Arguments for formatted text message
   */
  public static void error(final Throwable exception, final String message, final Object... arguments) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(exception, message, arguments);
  }

  /**
   * Logs an exception with a formatted message at {@link Level#ERROR ERROR} level. "{}" placeholders will be replaced
   * by given lazy arguments. The arguments will be only evaluated if the log entry is really output.
   *
   * @param exception Caught exception or any other throwable to log
   * @param message   Formatted text message to log
   * @param arguments Functions that produce the arguments for formatted text message
   */
  public static void error(final Throwable exception, final String message, final Supplier<?>... arguments) {
    if (isErrorEnabled())
      org.tinylog.Logger.error(exception, message, arguments);
  }

  /**
   * Checks if the given severity level is covered by the minimum level specified in the app preferences.
   *
   * @param level Severity level to check.
   * @return {@code true} if the given severity level is covered, otherwise {@code false}.
   */
  private static boolean isLogLevelEnabled(Level level) {
    return (level != null) && AppOption.APP_LOG_LEVEL.getIntValue() <= level.ordinal();
  }
}
