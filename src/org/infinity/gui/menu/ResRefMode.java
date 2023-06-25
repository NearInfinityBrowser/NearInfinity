// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.KeyEvent;

import org.infinity.resource.key.ResourceEntry;

/** Determines how show resource reference value and title. */
public enum ResRefMode {
  OnlyRef(KeyEvent.VK_1, "Resource Name") {
    @Override
    public String format(ResourceEntry entry) {
      return entry.getResourceName();
    }
  },
  RefName(KeyEvent.VK_2, "Resource Name (Search Name)") {
    @Override
    public String format(ResourceEntry entry) {
      final String search = entry.getSearchString();
      final String resname = entry.getResourceName();
      return search == null ? resname : resname + " (" + search + ')';
    }
  },
  NameRef(KeyEvent.VK_3, "Search Name (Resource Name)") {
    @Override
    public String format(ResourceEntry entry) {
      final String search = entry.getSearchString();
      final String resname = entry.getResourceName();
      return search == null ? resname : search + " (" + resname + ')';
    }
  };

  private final int keyCode;
  private final String title;

  private ResRefMode(int keyCode, String title) {
    this.keyCode = keyCode;
    this.title = title;
  }

  /** Virtual key code for the {@code ResRefMode} instance. */
  public int getKeyCode() {
    return keyCode;
  }

  /** Title of the menu item in Options menu. */
  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  public abstract String format(ResourceEntry entry);
}
