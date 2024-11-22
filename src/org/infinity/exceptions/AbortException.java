// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.exceptions;

/**
 * Thrown to indicate that the current operation was intentionally aborted.
 */
public class AbortException extends Exception {
  /** Constructs an {@code AbortException} with no detail message. */
  public AbortException() {
    super();
  }

  /**
   * Constructs an {@code AbortException} with the specified detail message.
   *
   * @param message the detail message.
   */
  public AbortException(String message) {
    super(message);
  }

  /**
   * Constructs an {@code AbortException} with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause   the cause for this exception to be thrown.
   */
  public AbortException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an {@code AbortException} with a cause but no detail message.
   *
   * @param cause the cause for this exception to be thrown.
   */
  public AbortException(Throwable cause) {
    super(cause);
  }
}
