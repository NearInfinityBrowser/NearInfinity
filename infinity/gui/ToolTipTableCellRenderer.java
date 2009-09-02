// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public final class ToolTipTableCellRenderer extends DefaultTableCellRenderer
{
// --------------------- Begin Interface TableCellRenderer ---------------------

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                 int row, int column)
  {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (table.getColumnModel().getColumn(column).getWidth() <
        getFontMetrics(getFont()).stringWidth(getText())) {
      StringBuffer sb = new StringBuffer("<html>");
      String string = getText();
      int index = 0;
      while (index < string.length()) {
        if (index > 0)
          sb.append("<br>");
        int delta = string.indexOf((int)' ', index + 100);
        if (delta == -1)
          delta = string.length();
        sb.append(string.substring(index, Math.min(delta, string.length())));
        index = delta;
      }
      sb.append("</html>");
      setToolTipText(sb.toString());
    }
    else
      setToolTipText(null);
    return this;
  }

// --------------------- End Interface TableCellRenderer ---------------------
}

