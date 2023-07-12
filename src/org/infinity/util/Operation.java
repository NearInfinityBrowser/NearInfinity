// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * Represents an operation without explicit input and output.
 *
 * <p>
 * This is a functional interface whose functional method is {@link #perform()}.
 * </p>
 */
@FunctionalInterface
public interface Operation {
  /** Performs the operation. */
  void perform();
}
