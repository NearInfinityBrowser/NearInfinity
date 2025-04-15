package org.infinity.gui;

import java.awt.Component;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Default {@link ListCellRenderer} implementation for the {@link CheckBoxList} control.
 */
public class CheckBoxCellRenderer implements ListCellRenderer<Object> {
  private final JCheckBox checkBox = new JCheckBox();

  @Override
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index,
      boolean isSelected, boolean cellHasFocus) {
    checkBox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
    checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
    checkBox.setFont(list.getFont());
    checkBox.setText(Objects.toString(value.toString(), "(null)"));

    final CheckBoxListModel<?> model = (CheckBoxListModel<?>)list.getModel();
    checkBox.setSelected(model.isSelected(index));

    return checkBox;
  }
}
