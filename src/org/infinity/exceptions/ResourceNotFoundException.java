// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.exceptions;

/**
 * Thrown if a game resource could not be found.
 */
public class ResourceNotFoundException extends ResourceException {
  /** Constructs an {@code ResourceNotFoundException} with no detail message. */
  public ResourceNotFoundException() {
    super();
  }

  /**
   * Constructs an {@code ResourceNotFoundException} with the specified detail message.
   *
   * @param message the detail message.
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs an {@code ResourceNotFoundException} with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause   the cause for this exception to be thrown.
   */
  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an {@code ResourceNotFoundException} with a cause but no detail message.
   *
   * @param cause the cause for this exception to be thrown.
   */
  public ResourceNotFoundException(Throwable cause) {
    super(cause);
  }
}
