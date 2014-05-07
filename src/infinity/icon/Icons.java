// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.icon;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public class Icons
{
  private static final Map<URL, ImageIcon> ICONMAP = new HashMap<URL, ImageIcon>(100);

  /**
   * Returns an ImageIcon object of the specified graphics filename.
   * @param name The graphics filename, can include a path relative to the current class path.
   * @return The ImageIcon object, or <code>null</code> on error.
   */
  public static ImageIcon getIcon(String name)
  {
    return getIcon(null, name);
  }

  /**
   * Returns an ImageIcon object of the specified graphics filename.
   * @param c A class located in the same package as the specified graphics file. The full package name
   *          of the class will be used to determine the correct path of the graphics file.
   * @param name The graphics filename.
   * @return The ImageIcon object, or <code>null</code> on error.
   */
  public static ImageIcon getIcon(Class<?> c, String fileName)
  {
    URL url = getValidURL(c, fileName);
    if (url != null) {
      ImageIcon icon = ICONMAP.get(url);
      if (icon == null) {
        icon = new ImageIcon(url);
        ICONMAP.put(url, icon);
      }
      return icon;
    }
    return null;
  }

  /**
   * Returns an Image object of the specified graphics filename.
   * @param name The graphics filename, can include a path relative to the current class path.
   * @return The Image object, or <code>null</code> on error.
   */
  public static Image getImage(String fileName)
  {
    return getImage(null, fileName);
  }

  /**
   * Returns an Image object of the specified graphics filename.
   * @param c A class located in the same package as the specified graphics file. The full package name
   *          of the class will be used to determine the correct path of the graphics file.
   * @param name The graphics filename.
   * @return The Image object, or <code>null</code> on error.
   */
  public static Image getImage(Class<?> c, String fileName)
  {
    ImageIcon icon = getIcon(c, fileName);
    if (icon != null) {
      return icon.getImage();
    }
    return null;
  }

  // Returns a URL instance that points to the specified filename
  private static URL getValidURL(Class<?> c, String fileName)
  {
    URL retVal = null;
    if (fileName != null && !fileName.isEmpty()) {
      if (c == null) {
        retVal = ClassLoader.getSystemResource(fileName);
      }
      if (retVal == null) {
        if (c == null) {
          c = Icons.class;
        }
        String basePath = c.getPackage().getName().replace('.', '/');
        String separator = (fileName.charAt(0) == '/') ? "" : "/";
        retVal = ClassLoader.getSystemResource(basePath + separator + fileName);
      }
    }
    return retVal;
  }

  protected Icons(){}
}

