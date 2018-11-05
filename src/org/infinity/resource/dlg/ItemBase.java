// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.Enumeration;
import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.util.StringTable;
import static org.infinity.util.StringTable.Format.NONE;
import static org.infinity.util.StringTable.Format.STRREF_PREFIX;

/** Common base class for node type specific classes. */
abstract class ItemBase implements TreeNode
{
  /** Maximum string length to display. */
  private static final int MAX_LENGTH = 200;

  /** Dialog from which this item. Dialogs can use several dialog resources in one conversation. */
  protected final DlgResource dlg;
  private final boolean showStrrefs;
  private final boolean showIcons;

  public ItemBase(DlgResource dlg)
  {
    this.dlg = dlg;
    this.showStrrefs = BrowserMenuBar.getInstance().showStrrefs();
    this.showIcons = BrowserMenuBar.getInstance().showDlgTreeIcons();
  }

  /** Returns the dialog resource object. */
  public DlgResource getDialog()
  {
    return dlg;
  }

  /** Returns the dialog resource name. */
  public String getDialogName()
  {
    if (dlg != null) {
      return dlg.getResourceEntry().getResourceName();
    } else {
      return "";
    }
  }

  /**
   * Gets path that represents this node in the tree.
   *
   * @return Path from root to this node
   */
  public TreePath getPath()
  {
    final ItemBase parent = getParent();
    if (parent == null) {
      return new TreePath(this);
    }
    return parent.getPath().pathByAddingChild(this);
  }

  /** Returns the icon associated with the item type. */
  public abstract Icon getIcon();

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public abstract ItemBase getChildAt(int childIndex);

  @Override
  public abstract ItemBase getParent();

  @Override
  public abstract Enumeration<? extends ItemBase> children();
  //</editor-fold>

  /** Returns whether to display icons in front of the nodes. */
  protected boolean showIcons() { return showIcons; }

  /**
   * Returns string that can be used to display in the tree.
   *
   * @param value Field with reference to string in the {@link StringTable string table}
   * @return String from string table if necessary cut down up to {@link #MAX_LENGTH} characters
   */
  protected final String getText(StringRef value) {
    final String text = StringTable.getStringRef(value.getValue(), showStrrefs ? STRREF_PREFIX : NONE);
    if (text.length() > MAX_LENGTH) {
      return text.substring(0, MAX_LENGTH) + "...";
    }
    return text;
  }
}

/** Auxiliary class, being the parent for states, for a type safety. */
abstract class StateOwnerItem extends ItemBase
{
  public StateOwnerItem(DlgResource dlg) {
    super(dlg);
  }

  //<editor-fold defaultstate="collapsed" desc="TreeNode">
  @Override
  public abstract StateItem getChildAt(int childIndex);

  @Override
  public abstract Enumeration<? extends StateItem> children();
  //</editor-fold>
}