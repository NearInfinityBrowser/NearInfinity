// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;

/**
 * Specialization of the {@link JList} class that displays interactive {@code JCheckBox} controls instead of
 * {@code JLabel} controls.
 * <p>
 * Data models are required to be compatible with the {@link CheckBoxListModel} class.
 * </p>
 * <p>
 * The cell renderer must be compatible with the {@link CheckBoxCellRenderer} class.
 * </p>
 */
public class CheckBoxList<E> extends JList<E> {
  private boolean initialized;

  public CheckBoxList() {
    super();
    setModel(new CheckBoxListModel<>());
    init();
  }

  public CheckBoxList(E[] listData, boolean isSelected) {
    super(new CheckBoxListModel<>(Arrays.asList(listData), isSelected));
    init();
  }

  public CheckBoxList(Collection<? extends E> listData, boolean isSelected) {
    super(new CheckBoxListModel<>(listData, isSelected));
    init();
  }

  public CheckBoxList(CheckBoxListModel<E> model) {
    super(model);
    init();
  }

  @Override
  public void setModel(ListModel<E> model) {
    if (model == null) {
      model = new CheckBoxListModel<>();
    }

    if (model instanceof CheckBoxListModel<?>) {
      super.setModel(model);
    } else {
      throw new IllegalArgumentException("Argument is not compatible with the CheckBoxListModel class");
    }
  }

  public void setModel(CheckBoxListModel<E> model) {
    if (model == null) {
      model = new CheckBoxListModel<>();
    }
    super.setModel(model);
  }

  @Override
  public CheckBoxListModel<E> getModel() {
    return (CheckBoxListModel<E>)super.getModel();
  }

  @Override
  public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
    if (cellRenderer == null) {
      cellRenderer = new CheckBoxCellRenderer();
    }

    if (!initialized || cellRenderer instanceof CheckBoxCellRenderer) {
      super.setCellRenderer(cellRenderer);
    } else {
      throw new UnsupportedOperationException("CheckBoxCellRenderer or derived type required");
    }
  }

  /** Sets the delegate that is used to paint each cell in the list. */
  public void setCellRenderer(CheckBoxCellRenderer cellRenderer) {
    if (cellRenderer == null) {
      cellRenderer = new CheckBoxCellRenderer();
    }
    super.setCellRenderer(cellRenderer);
  }

  private void init() {
    super.setCellRenderer(new CheckBoxCellRenderer());

    // try to determine the horizontal check icon region
    final Icon icon = UIManager.getIcon("CheckBox.icon");
    final Insets margin = UIManager.getInsets("CheckBox.margin");
    final int gap = UIManager.getInt("CheckBox.textIconGap");
    int w;
    if (icon != null) {
      w = icon.getIconWidth();
      if (margin != null) {
        w += margin.left + margin.right;
      }
      w += ((Number)gap).intValue();
    } else {
      w = getFontMetrics(getFont()).getHeight() + 4;  // approximation
    }
    final int checkBoxIconWidth = w;

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        final int index = locationToIndex(e.getPoint());
        if (index != -1) {
          final CheckBoxListModel<E> model = getModel();

          if (e.getPoint().x < checkBoxIconWidth) {
            model.setSelected(index, !model.isSelected(index));
          }
        }
      }
    });

    initialized = true;
  }
}
