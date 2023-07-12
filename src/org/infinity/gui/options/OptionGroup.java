// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import javax.swing.JPanel;

/**
 * Defines an options group that groups individual options within a category.
 * <p>
 * An {@code OptionGroup} is represented by a {@code JPanel} with a {@code TitledBorder}.
 * </p>
 * <p>
 * All option elements must has an {@code OptionGroup} as parent.
 * </p>
 */
public class OptionGroup extends OptionContainerBase {
  private JPanel groupPanel;

  public static OptionGroup createDefault(OptionBase... children) {
    return new OptionGroup(DEFAULT_ID, null, children);
  }

  public static OptionGroup create(String label, OptionBase... children) {
    return new OptionGroup(DEFAULT_ID, label, children);
  }

  protected OptionGroup(Object id, String label, OptionBase... children) {
    super(id, label, children);
  }

  /** Returns the {@code JPanel} component of the OptionGroup UI. */
  public JPanel getUiPanel() {
    return groupPanel;
  }

  /** Sets the {@code JPanel} component of the OptionGroup UI. */
  public OptionGroup setUiPanel(JPanel panel) {
    this.groupPanel = panel;
    return this;
  }

  @Override
  public OptionContainerBase getParent() {
    return (OptionContainerBase) super.getParent();
  }

  @Override
  protected OptionBase setParent(OptionBase parent) {
    if (parent == null || parent instanceof OptionContainerBase) {
      super.setParent(parent);
      return this;
    } else {
      throw new IllegalArgumentException("Argument of types derived from OptionContainerBase expected");
    }
  }
}
