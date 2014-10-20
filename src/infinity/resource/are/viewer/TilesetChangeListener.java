// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.EventListener;

public interface TilesetChangeListener extends EventListener
{
  void tilesetChanged(TilesetChangeEvent event);
}
