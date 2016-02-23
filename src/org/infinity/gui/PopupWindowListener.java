// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.util.EventListener;

/**
 * Used in ButtonPopupWindow class to signal showing or hiding a popup window.
 */
public interface PopupWindowListener extends EventListener
{
  /** Called right before the ButtonPopupWindow component becomes invisible. */
  void popupWindowWillBecomeInvisible(PopupWindowEvent event);

  /** Called right before the ButtonPopupWindow component becomes visible. */
  void popupWindowWillBecomeVisible(PopupWindowEvent event);
}
