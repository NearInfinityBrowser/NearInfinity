// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.ArrayList;
import java.util.List;

/**
 * Element, that contains information about mapping dialogue tree entry to GUI items.
 *
 * @author Mingun
 */
final class DlgElement
{
  /**
   * All GUI items, that represent dialogue entry in the tree. Used for update
   * tree when entry changes.
   */
  final List<ItemBase> all = new ArrayList<>();
  /**
   * Reference to the main item of the visual tree item.
   * <p>
   * Storage of the reference to a main item indirectly, in this object, allows
   * to replace quickly the main item object without the need for a traverse of
   * all items which refer to it. Instead each item hold reference to this object.
   * <p>
   * Reference to main item is required when the main visual node is removed from
   * a tree, but other nodes, representing the same {@link TreeItemEntry}, still
   * are available in a tree. At emergence of such situation as the one of such
   * nodes will be chosen as new main item.
   */
  ItemBase main;

  public void add(ItemBase item)
  {
    all.add(item);
    // The first added element becomes the main thing
    if (main == null) {
      main = item;
    }
  }
}
