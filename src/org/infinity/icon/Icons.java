// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.icon;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

public class Icons
{
  public static final String ICON_ABOUT_16            = "About16.gif";
  public static final String ICON_ADD_16              = "Add16.gif";
  public static final String ICON_APP_16              = "App16.png";
  public static final String ICON_APP_32              = "App32.png";
  public static final String ICON_APP_64              = "App64.png";
  public static final String ICON_APP_128             = "App128.png";
  public static final String ICON_APPLICATION_16      = "Application16.gif";
  public static final String ICON_ARROW_DOWN_15       = "ArrowDown15.gif";
  public static final String ICON_ARROW_UP_15         = "ArrowUp15.gif";
  public static final String ICON_BACK_16             = "Back16.gif";
  public static final String ICON_BLUE_CIRCLE_16      = "BlueCircle16.gif";
  public static final String ICON_BLUE_CIRCLE_20      = "BlueCircle20.gif";
  public static final String ICON_BUNDLE_16           = "Bundle16.gif";
  public static final String ICON_CHECK_NOT_16        = "Check_Not16.gif";
  public static final String ICON_CHECK_16            = "Check16.gif";
  public static final String ICON_CIRCLE_20           = "Circle20.gif";
  public static final String ICON_COLLAPSE_16         = "Collapse16.png";
  public static final String ICON_COLLAPSE_ALL_24     = "CollapseAll24.png";
  public static final String ICON_COLOR_16            = "Color16.gif";
  public static final String ICON_COPY_16             = "Copy16.gif";
  public static final String ICON_CRE_VIEWER_24       = "CreViewer24.png";
  public static final String ICON_CUT_16              = "Cut16.gif";
  public static final String ICON_DELETE_16           = "Delete16.gif";
  public static final String ICON_DOWN_16             = "Down16.gif";
  public static final String ICON_EDIT_16             = "Edit16.gif";
  public static final String ICON_END_16              = "End16.gif";
  public static final String ICON_ERROR_16            = "Error16.png";
  public static final String ICON_EXIT_16             = "Exit16.gif";
  public static final String ICON_EXPAND_16           = "Expand16.png";
  public static final String ICON_EXPAND_ALL_24       = "ExpandAll24.png";
  public static final String ICON_EXPORT_16           = "Export16.gif";
  public static final String ICON_FIND_16             = "Find16.gif";
  public static final String ICON_FIND_AGAIN_16       = "FindAgain16.gif";
  public static final String ICON_FORWARD_16          = "Forward16.gif";
  public static final String ICON_GREEN_CIRCLE_16     = "GreenCircle16.gif";
  public static final String ICON_HELP_16             = "Help16.gif";
  public static final String ICON_HISTORY_16          = "History16.gif";
  public static final String ICON_IMPORT_16           = "Import16.gif";
  public static final String ICON_INFORMATION_16      = "Information16.png";
  public static final String ICON_LAUNCH_24           = "LaunchRed24.png";
  public static final String ICON_LAUNCH_PLUS_24      = "LaunchRedPlus24.png";
  public static final String ICON_MAGNIFY_16          = "Magnify16.png";
  public static final String ICON_MOVIE_16            = "Movie16.gif";
  public static final String ICON_NEW_16              = "New16.gif";
  public static final String ICON_OPEN_16             = "Open16.gif";
  public static final String ICON_PASTE_16            = "Paste16.gif";
  public static final String ICON_PAUSE_16            = "Pause16.gif";
  public static final String ICON_PLAY_16             = "Play16.gif";
  public static final String ICON_PRINT_16            = "Print16.gif";
  public static final String ICON_PROPERTIES_16       = "Properties16.gif";
  public static final String ICON_REDO_16             = "Redo16.gif";
  public static final String ICON_REFRESH_16          = "Refresh16.gif";
  public static final String ICON_RELEASE_16          = "Release16.gif";
  public static final String ICON_REMOVE_16           = "Remove16.gif";
  public static final String ICON_ROW_INSERT_AFTER_16 = "RowInsertAfter16.gif";
  public static final String ICON_SAVE_16             = "Save16.gif";
  public static final String ICON_SELECT_IN_TREE_16   = "SelectInTree16.png";
  public static final String ICON_STEP_BACK_16        = "StepBack16.gif";
  public static final String ICON_STEP_FORWARD_16     = "StepForward16.gif";
  public static final String ICON_STOP_16             = "Stop16.gif";
  public static final String ICON_STOP_24             = "Stop24.gif";
  public static final String ICON_UNDO_16             = "Undo16.gif";
  public static final String ICON_UP_16               = "Up16.gif";
  public static final String ICON_VOLUME_16           = "Volume16.gif";
  public static final String ICON_WARNING_16          = "Warning16.png";
  public static final String ICON_YELLOW_CIRCLE_16    = "YellowCircle16.gif";
  public static final String ICON_YELLOW_CIRCLE_20    = "YellowCircle20.gif";
  public static final String ICON_ZOOM_16             = "Zoom16.gif";

  private static final Map<URL, ImageIcon> ICONMAP = new HashMap<>(100);

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
