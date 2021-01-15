// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

/**
 * Implementor of this interface can store fields, that can be added or removed.
 */
public interface HasChildStructs
{
  /**
   * Returns an array of available {@link AddRemovable} prototype objects.
   * Returned object will be {@link Object#clone cloned} when an editor needs to
   * create the new element
   *
   * @return An array of available {@link AddRemovable} objects.
   *         Can't be {@code null} or contain {@code null}'s
   */
  AddRemovable[] getPrototypes() throws Exception;

  /**
   * This method is called whenever an {@link AddRemovable} entry is about to be added
   * to the parent structure. It allows subclasses to make final modifications to the given
   * {@link AddRemovable} argument before it is added to the structure or to cancel the operation.
   * @param entry The {@link AddRemovable} entry to add.
   * @return The {@link AddRemovable} entry to add.
   *         May return {@code null} to cancel the operation.
   */
  AddRemovable confirmAddEntry(AddRemovable entry) throws Exception;
}
