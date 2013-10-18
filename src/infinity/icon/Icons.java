// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.icon;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public final class Icons
{
  private static final Map<String, ImageIcon> ICONMAP = new HashMap<String, ImageIcon>(100);

  public static ImageIcon getIcon(String name)
  {
    ImageIcon icon = ICONMAP.get(name);
    if (icon == null) {
      icon = new ImageIcon(Icons.class.getResource(name));
      ICONMAP.put(name, icon);
    }
    return icon;
  }

  public static Image getImage(String name)
  {
    ImageIcon icon = getIcon(name);
    if (icon != null) {
      return icon.getImage();
    }
    return null;
  }

  private Icons(){}
}

