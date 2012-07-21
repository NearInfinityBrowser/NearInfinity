// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
    glassPane.setVisible(blocked);
  }
}

