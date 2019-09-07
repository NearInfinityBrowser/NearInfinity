// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.resource.key.ResourceEntry;

/**
 * Renderer for lists of {@link ResourceEntry} objects.
 *
 * @author Mingun
 */
public class ResourceCellRenderer extends DefaultListCellRenderer
{
  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
  {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    final ResourceEntry entry = (ResourceEntry)value;
    if (entry != null) {
      setText(BrowserMenuBar.getInstance().getResRefMode().format(entry));
    } else {
      setText("<none>");
    }
    return this;
  }
}
