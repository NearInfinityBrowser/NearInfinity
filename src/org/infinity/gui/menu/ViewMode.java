// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

/** Determines default tab for viewing structures. */
public enum ViewMode {
  View("View"), Edit("Edit");

  private final String title;

  private ViewMode(String title) {
    this.title = title;
  }

  /** Title of the menu item in Options menu. */
  public String getTitle() {
    return title;
  }
}
