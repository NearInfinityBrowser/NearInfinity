// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.FontMetrics;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.IconCache;

public class ToolTipTableCellRenderer extends DefaultTableCellRenderer {
  private final boolean showIcons;
  private final int iconSize;

  public ToolTipTableCellRenderer() {
    this(false);
  }

  public ToolTipTableCellRenderer(boolean showIcons) {
    super();
    this.showIcons = showIcons;

    int fontHeight = 0;
    if (this.showIcons) {
      final FontMetrics fm = getFontMetrics(getFont());
      fontHeight = fm.getHeight();
    }
    // scale icon size up to the next multiple of 4
    this.iconSize = Math.max(IconCache.getDefaultTreeIconSize(), (fontHeight + 3) & ~3);
  }

  // --------------------- Begin Interface TableCellRenderer ---------------------

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (table.getColumnModel().getColumn(column).getWidth() < getFontMetrics(getFont()).stringWidth(getText())) {
      final StringBuilder sb = new StringBuilder("<html>");
      String string = getText();
      int index = 0;
      while (index < string.length()) {
        if (index > 0) {
          sb.append("<br>");
        }
        int delta = string.indexOf(' ', index + 100);
        if (delta == -1) {
          delta = string.length();
        }
        sb.append(string.substring(index, Math.min(delta, string.length())));
        index = delta;
      }
      sb.append("</html>");
      setToolTipText(sb.toString());
    } else {
      setToolTipText(null);
    }

    if (showIcons) {
      Icon icon = null;
      if (value instanceof ResourceRef) {
        final ResourceRef ref = (ResourceRef) value;
        icon = IconCache.get(ResourceFactory.getResourceEntry(ref.getResourceName()), iconSize);
      }
      setIcon(icon);
    }

    return this;
  }

  // --------------------- End Interface TableCellRenderer ---------------------
}
