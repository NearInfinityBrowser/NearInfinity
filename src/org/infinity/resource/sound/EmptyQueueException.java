// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

/**
 * An I/O exception that is thrown if the audio queue does not contain any more elements.
 */
public class EmptyQueueException extends Exception {
  /** Constructs an {@code EmptyQueueException} with no detail message. */
  public EmptyQueueException() {
    super();
  }

  /**
   * Constructs an {@code EmptyQueueException} with the specified detail message.
   *
   * @param message the detail message.
   */
  public EmptyQueueException(String message) {
    super(message);
  }

  /**
   * Constructs an {@code EmptyQueueException} with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause   the cause for this exception to be thrown.
   */
  public EmptyQueueException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an {@code EmptyQueueException} with a cause but no detail message.
   *
   * @param cause the cause for this exception to be thrown.
   */
  public EmptyQueueException(Throwable cause) {
    super(cause);
  }
}
