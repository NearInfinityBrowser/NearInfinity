// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.icon;

import javax.swing.ImageIcon;

import org.infinity.resource.graphics.ColorConvert;

/**
 * Provides {@code ImageIcon} instances of selected graphics files.
 */
public enum Icons {
  ICON_ABOUT_16("About16.gif"),
  ICON_ADD_16("Add16.gif"),
  ICON_APP_16("App16.png"),
  ICON_APP_32("App32.png"),
  ICON_APP_64("App64.png"),
  ICON_APP_128("App128.png"),
  ICON_APP_256("App256.png"),
  ICON_APP_512("App512.png"),
  ICON_APPLICATION_16("Application16.gif"),
  ICON_ARROW_DOWN_15("ArrowDown15.gif"),
  ICON_ARROW_UP_15("ArrowUp15.gif"),
  ICON_BACK_16("Back16.gif"),
  ICON_BLUE_CIRCLE_16("BlueCircle16.gif"),
  ICON_BLUE_CIRCLE_20("BlueCircle20.gif"),
  ICON_BUNDLE_16("Bundle16.gif"),
  ICON_CHECK_NOT_16("Check_Not16.gif"),
  ICON_CHECK_16("Check16.gif"),
  ICON_CIRCLE_20("Circle20.gif"),
  ICON_COLLAPSE_16("Collapse16.png"),
  ICON_COLLAPSE_ALL_24("CollapseAll24.png"),
  ICON_COLOR_16("Color16.gif"),
  ICON_COPY_16("Copy16.gif"),
  ICON_CRE_VIEWER_24("CreViewer24.png"),
  ICON_CUT_16("Cut16.gif"),
  ICON_DELETE_16("Delete16.gif"),
  ICON_DOWN_16("Down16.gif"),
  ICON_EDIT_16("Edit16.gif"),
  ICON_END_16("End16.gif"),
  ICON_ERROR_16("Error16.png"),
  ICON_EXIT_16("Exit16.gif"),
  ICON_EXPAND_16("Expand16.png"),
  ICON_EXPAND_ALL_24("ExpandAll24.png"),
  ICON_EXPORT_16("Export16.gif"),
  ICON_FILTER_16("Filter16.png"),
  ICON_FIND_16("Find16.gif"),
  ICON_FIND_AGAIN_16("FindAgain16.gif"),
  ICON_FIRST_16("First16.gif"),
  ICON_FORWARD_16("Forward16.gif"),
  ICON_GOTO_24("Goto24.gif"),
  ICON_GREEN_CIRCLE_16("GreenCircle16.gif"),
  ICON_HELP_16("Help16.gif"),
  ICON_HISTORY_16("History16.gif"),
  ICON_IMPORT_16("Import16.gif"),
  ICON_INFORMATION_16("Information16.png"),
  ICON_LAST_16("Last16.gif"),
  ICON_LAUNCH_24("LaunchRed24.png"),
  ICON_LAUNCH_PLUS_24("LaunchRedPlus24.png"),
  ICON_MAGNIFY_16("Magnify16.png"),
  ICON_MOVIE_16("Movie16.gif"),
  ICON_MUSIC_16("Music16.png"),
  ICON_NEW_16("New16.gif"),
  ICON_OPEN_16("Open16.gif"),
  ICON_PASTE_16("Paste16.gif"),
  ICON_PAUSE_16("Pause16.gif"),
  ICON_PLAY_16("Play16.gif"),
  ICON_PRINT_16("Print16.gif"),
  ICON_PROPERTIES_16("Properties16.gif"),
  ICON_REDO_16("Redo16.gif"),
  ICON_REFRESH_16("Refresh16.gif"),
  ICON_RELEASE_16("Release16.gif"),
  ICON_REMOVE_16("Remove16.gif"),
  ICON_ROW_INSERT_AFTER_16("RowInsertAfter16.gif"),
  ICON_SAVE_16("Save16.gif"),
  ICON_SELECT_IN_TREE_16("SelectInTree16.png"),
  ICON_STEP_BACK_16("StepBack16.gif"),
  ICON_STEP_FORWARD_16("StepForward16.gif"),
  ICON_STOP_16("Stop16.gif"),
  ICON_STOP_24("Stop24.gif"),
  ICON_UNDO_16("Undo16.gif"),
  ICON_UP_16("Up16.gif"),
  ICON_VOLUME_16("Volume16.gif"),
  ICON_WARNING_16("Warning16.png"),
  ICON_YELLOW_CIRCLE_16("YellowCircle16.gif"),
  ICON_YELLOW_CIRCLE_20("YellowCircle20.gif"),
  ICON_ZOOM_16("Zoom16.gif");

  private final String fileName;
  private ImageIcon icon;

  Icons(String fileName) {
    this.fileName = fileName;
  }

  /** Returns the {@code ImageIcon} instance of the enum object. */
  public ImageIcon getIcon() {
    if (icon == null) {
      icon = ColorConvert.loadAppIcon(Icons.class, fileName);
      if (icon == null) {
        throw new NullPointerException("Icon is null");
      }
    }
    return icon;
  }

  /** Returns the name of the graphics file. */
  public String getFileName() {
    return fileName;
  }
}
