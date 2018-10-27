// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

/**
 * Implementors of this interface, must have a constructor without arguments.
 */
public interface AddRemovable extends StructEntry
{
  /**
   * Determines if this entry can be removed from or added to its owner structure.
   *
   * @return {@code true} if this object can be removed or added now and {@code false} otherwise
   */
  boolean canRemove();
}

