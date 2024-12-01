// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import org.infinity.resource.key.Keyfile;

/** Determines, in which virtual folder show resources from Override folder. */
public enum OverrideMode {
  /**
   * All resources shows in folder corresponding to resource extension. Override folder will show only files with
   * unknown extension, that stored in Override folder.
   */
  InTree("In ??? Folders (CRE, SPL, ...)"),
  /** All resources from Override folder shows in Override folder. */
  InOverride("In Override Folder"),
  /**
   * All indexed by {@link Keyfile chitin.key} resources shows in folder corresponding to resource extension, all other
   * - in Override folder.
   */
  Split("Split Between ??? and Override Folders");

  private final String title;

  OverrideMode(String title) {
    this.title = title;
  }

  /** Title of the menu item in Options menu. */
  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
