// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import org.infinity.datatype.StringRef;
import org.infinity.resource.StructEntry;

/**
 * Interface for structures that represent dialog tree items.
 *
 * @author Mingun
 */
public interface TreeItemEntry extends StructEntry {
  /**
   * Returns attribute that represents text for show in the tree.
   *
   * @return Text for show or {@code null} if case of broken structure
   */
  StringRef getAssociatedText();
}
