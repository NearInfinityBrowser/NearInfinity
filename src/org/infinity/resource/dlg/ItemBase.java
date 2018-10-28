// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import javax.swing.Icon;

import org.infinity.gui.BrowserMenuBar;

/** Common base class for node type specific classes. */
abstract class ItemBase
{
  private final DlgResource dlg;
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

  /** Returns the icon associated with the item type. */
  public abstract Icon getIcon();

  /** Returns whether to show the Strref value next to the string. */
  protected boolean showStrrefs() { return showStrrefs; }

  /** Returns whether to display icons in front of the nodes. */
  protected boolean showIcons() { return showIcons; }
}
