// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import org.infinity.datatype.ResourceRef;

/**
 * Interface for entries that can have icons.
 *
 * @author Mingun
 */
public interface HasIcon {
  /**
   * Returns a resource reference to icon which a game uses when display object
   * in stores and the inventory.
   *
   * @return {@code null} if such icon not exist for object
   */
  ResourceRef getIcon();
}
