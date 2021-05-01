// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser.icon;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public class Icons
{
  public static final String ICON_END            = "btn_end.png";
  public static final String ICON_HOME           = "btn_home.png";
  public static final String ICON_PAUSE          = "btn_pause.png";
  public static final String ICON_PLAY           = "btn_play.png";
  public static final String ICON_RESUME         = "btn_resume.png";
  public static final String ICON_STEP_BACK      = "btn_step_back.png";
  public static final String ICON_STEP_FORWARD   = "btn_step_forward.png";
  public static final String ICON_STOP           = "btn_stop.png";
  public static final String ICON_CENTER         = "btn_center.png";

  public static final String ICON_CIRCLE_GREEN   = "circle_green.png";
  public static final String ICON_CIRCLE_BLUE    = "circle_blue.png";
  public static final String ICON_CIRCLE_RED     = "circle_red.png";
  public static final String ICON_CIRCLE_YELLOW  = "circle_yellow.png";

  private static final Map<URL, ImageIcon> ICONMAP = new HashMap<>(20);

  /**
   * Returns an ImageIcon object of the specified graphics filename.
   * @param name The graphics filename, can include a path relative to the current class path.
   * @return The ImageIcon object, or {@code null} on error.
   */
  public static ImageIcon getIcon(String name)
  {
    return getIcon(null, name);
  }

  /**
   * Returns an ImageIcon object of the specified graphics filename.
   * @param c A class located in the same package as the specified graphics file. The full package name
   *          of the class will be used to determine the correct path of the graphics file.
   * @param fileName The graphics filename.
   * @return The ImageIcon object, or {@code null} on error.
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
   * @param fileName The graphics filename, can include a path relative to the current class path.
   * @return The Image object, or {@code null} on error.
   */
  public static Image getImage(String fileName)
  {
    return getImage(null, fileName);
  }

  /**
   * Returns an Image object of the specified graphics filename.
   * @param c A class located in the same package as the specified graphics file. The full package name
   *          of the class will be used to determine the correct path of the graphics file.
   * @param fileName The graphics filename.
   * @return The Image object, or {@code null} on error.
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
