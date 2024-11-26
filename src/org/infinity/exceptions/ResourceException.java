// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.exceptions;

/**
 * A generic exception that is thrown if a resource-related problem occurs.
 */
public class ResourceException extends Exception {
  /** Constructs an {@code ResourceException} with no detail message. */
  public ResourceException() {
    super();
  }

  /**
   * Constructs an {@code ResourceException} with the specified detail message.
   *
   * @param message the detail message.
   */
  public ResourceException(String message) {
    super(message);
  }

  /**
   * Constructs an {@code ResourceException} with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause   the cause for this exception to be thrown.
   */
  public ResourceException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an {@code ResourceException} with a cause but no detail message.
   *
   * @param cause the cause for this exception to be thrown.
   */
  public ResourceException(Throwable cause) {
    super(cause);
  }
}
