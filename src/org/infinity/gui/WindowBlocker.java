// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.MouseAdapter;

import javax.swing.RootPaneContainer;

import org.infinity.NearInfinity;

/**
 * @author Jon Heggland
 */
public final class WindowBlocker
{
  private static final MouseAdapter DUMMY_MOUSE_LISTENER =
          new MouseAdapter()
          {
          };

  private Component glassPane;

  /**
   * Blocks or unblocks the whole GUI and shows the respective mouse cursor.
   * @param block Blocks the GUI if set to {@code true}, unblocks if set to {@code false}.
   */
  public static void blockWindow(boolean block)
  {
    blockWindow(NearInfinity.getInstance(), block);
  }

  /**
   * Blocks or unblocks the specified component and shows the respective mouse cursor.
   * @param block Blocks the component if set to {@code true},
   *              unblocks if set to {@code false}.
   */
  public static void blockWindow(Window window, boolean block)
  {
    if (window != null) {
      if (block) {
        window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      } else {
        window.setCursor(null);
      }
    }
  }

  public WindowBlocker(RootPaneContainer window)
  {
    if (window == null) return;
    glassPane = window.getGlassPane();
    glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    glassPane.addMouseListener(DUMMY_MOUSE_LISTENER);
  }

  public void setBlocked(boolean blocked)
  {
    if (glassPane == null) return;
    if (blocked != glassPane.isVisible()) {
      glassPane.setVisible(blocked);
    }
  }
}

