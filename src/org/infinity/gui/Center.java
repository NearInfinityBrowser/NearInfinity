// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2023 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * @author Jon Heggland
 */
public final class Center {
  /** Centers the given {@code Component} on the specified {@code area}. */
  public static void center(Component c, Rectangle area) {
    c.setLocation(getCenterLocation(c.getSize(), area));
  }

  /** Returns the location for the specified {@code size} to be centered on the given {@code area}. */
  public static Point getCenterLocation(Dimension size, Rectangle area) {
    if (area == null) {
      area = new Rectangle();
    }

    int x = area.x + (area.width - size.width >> 1);
    int y = area.y + (area.height - size.height >> 1);

    return new Point(Math.max(0, x), Math.max(0, y));
  }

  private Center() {
  }
}
