// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.gui.StatusBar;

public interface ViewableContainer
{
  StatusBar getStatusBar();
  Viewable getViewable();

  void setViewable(Viewable vieweable);
}

