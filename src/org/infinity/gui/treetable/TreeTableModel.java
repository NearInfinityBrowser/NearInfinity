// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.treetable;

import javax.swing.tree.TreeModel;

public interface TreeTableModel extends TreeModel
{
  /**
   * Returns the type for column number {@code column}.
   */
  Class<? extends Object> getColumnClass(int column);
  /**
   * Returns the number ofs availible column.
   */
  int getColumnCount();

  /**
   * Returns the name for column number {@code column}.
   */
  String getColumnName(int column);

  /**
   * Returns the value to be displayed for node {@code node},
   * at column number {@code column}.
   */
  Object getValueAt(Object node, int column);

  /**
   * Indicates whether the the value for node {@code node},
   * at column number {@code column} is editable.
   */
  boolean isCellEditable(Object node, int column);

  /**
   * Sets the value for node {@code node},
   * at column number {@code column}.
   */
  void setValueAt(Object aValue, Object node, int column);
}

