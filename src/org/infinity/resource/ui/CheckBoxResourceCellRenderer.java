package org.infinity.resource.ui;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JList;

import org.infinity.gui.CheckBoxCellRenderer;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.key.ResourceEntry;

/**
 * Specialized {@link CheckBoxCellRenderer} for {@code ResourceEntry} objects.
 */
public class CheckBoxResourceCellRenderer extends CheckBoxCellRenderer {
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index,
      boolean isSelected, boolean cellHasFocus) {
    final JCheckBox cb = (JCheckBox)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    final ResourceEntry entry = (ResourceEntry) value;
    if (entry != null) {
      cb.setText(BrowserMenuBar.getInstance().getOptions().getResRefMode().format(entry));
    } else {
      cb.setText("<none>");
    }

    return cb;
  }
}
