// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.util.EventObject;

/**
 * Used in ButtonPopupWindow class to signal showing or hiding a popup window.
 */
public class PopupWindowEvent extends EventObject
{
  public PopupWindowEvent(Object source)
  {
    super(source);
  }
}
