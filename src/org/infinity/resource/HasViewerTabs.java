// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import javax.swing.JComponent;

/** Adds support for multiple tabs in resource viewers. */
public interface HasViewerTabs {
  /**
   * Returns the number of additional view tabs provided by the resource viewer.
   *
   * @return Number of provided view tabs.
   */
  int getViewerTabCount();

  /**
   * Returns the name of the given view tab.
   *
   * @param index Identifies the view tab.
   * @return The name of the given view tab. Should be short and unique.
   */
  String getViewerTabName(int index);

  /**
   * Returns the panel containing required controls for the given view tab.
   *
   * @param index Identifies the view tab.
   * @return Panel of controls for the given view tab.
   */
  JComponent getViewerTab(int index);

  /**
   * Returns whether the given view tab is added before the "Edit" tab.
   *
   * @param index Identifies the view tab.
   * @return true if added before the "Edit" tab, false otherwise.
   */
  boolean viewerTabAddedBefore(int index);
}
