// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import javax.swing.JScrollPane;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.infinity.gui.PreferencesDialog.Category;

/**
 * Defines an options category. Categories are displayed in the category tree view of the options dialog.
 */
public class OptionCategory extends OptionContainerBase {
  private MutableTreeNode treeNode;
  private JScrollPane categoryPane;

  public static OptionCategory createDefault(OptionContainerBase... children) {
    return new OptionCategory(Category.DEFAULT, children);
  }

  public static OptionCategory create(Category id, OptionContainerBase... children) {
    return new OptionCategory(id, children);
  }

  protected OptionCategory(Category id, OptionContainerBase... children) {
    super(id, id.getLabel(), children);
  }

  /** Returns the {@link TreeNode} instance associated with the OptionCategory UI. */
  public MutableTreeNode getUiTreeNode() {
    return treeNode;
  }

  /** Sets the {@link TreeNode} instance associated with the OptionCategory UI. */
  public OptionCategory setUiTreeNode(MutableTreeNode node) {
    this.treeNode = node;
    return this;
  }

  /** Returns the {@code JScrollPane} component associated with the OptionCategory UI. */
  public JScrollPane getUiPanel() {
    return categoryPane;
  }

  /** Sets the {@code JScrollPane} component associated with the OptionCategory UI. */
  public OptionCategory setUiScrollPane(JScrollPane pane) {
    this.categoryPane = pane;
    return this;
  }

  @Override
  public OptionCategory getParent() {
    return (OptionCategory) super.getParent();
  }

  @Override
  protected OptionBase setParent(OptionBase parent) {
    if (parent == null || parent instanceof OptionCategory) {
      super.setParent(parent);
      return this;
    } else {
      throw new IllegalArgumentException("Argument of type OptionCategory expected");
    }
  }
}