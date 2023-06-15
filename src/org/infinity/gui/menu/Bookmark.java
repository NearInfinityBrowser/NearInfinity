// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.infinity.resource.Profile;
import org.infinity.util.Platform;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

/** Manages bookmarked game entries. */
public class Bookmark implements Cloneable {
  /** "Bookmarks" preferences entries (numbers are 1-based). */
  private static final String BOOKMARK_NUM_ENTRIES    = "BookmarkEntries";
  private static final String FMT_BOOKMARK_NAME       = "BookmarkName%d";
  private static final String FMT_BOOKMARK_ID         = "BookmarkID%d";
  private static final String FMT_BOOKMARK_PATH       = "BookmarkPath%d";
  private static final String FMT_BOOKMARK_HOME_PATH  = "BookmarkHomePath%d";
  private static final String FMT_BOOKMARK_BIN_PATH   = "BookmarkPath%s%d"; // %s: Platform.OS, %d: bookmark index

  private static final String MENUITEM_COMMAND = "OpenBookmark";

  private static final Platform.OS[] SUPPORTED_OS = { Platform.OS.WINDOWS, Platform.OS.MAC_OS, Platform.OS.UNIX };

  private final Profile.Game game;
  private final String path;
  private final EnumMap<Platform.OS, List<String>> binPaths = new EnumMap<>(Platform.OS.class);

  private String name;
  private String homePath;
  private ActionListener listener;
  private JMenuItem item;

  public Bookmark(String name, Profile.Game game, String path, ActionListener listener) {
    this(name, game, path, null, null, listener);
  }

  public Bookmark(String name, Profile.Game game, String path, String homePath, EnumMap<Platform.OS,
      List<String>> binPaths, ActionListener listener) {
    if (game == null || path == null) {
      throw new NullPointerException();
    }
    if (name == null || name.trim().isEmpty()) {
      name = Profile.getProperty(Profile.Key.GET_GLOBAL_GAME_TITLE, game);
    }
    this.name = name;
    this.game = game;
    this.path = path;
    this.homePath = (homePath == null || homePath.isEmpty()) ? null : homePath;
    this.listener = listener;
    if (binPaths != null) {
      this.binPaths.putAll(binPaths);
    }
    updateMenuItem();
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new Bookmark(name, game, path, homePath, binPaths, listener);
  }

  /** Returns user-defined game name. */
  public String getName() {
    return name;
  }

  /** Sets a new name and returns the previous name (if available). */
  public String setName(String newName) {
    String retVal = getName();
    if (newName != null && !newName.trim().isEmpty()) {
      this.name = newName;
      updateMenuItem();
    }
    return retVal;
  }

  /** Returns game type. */
  public Profile.Game getGame() {
    return game;
  }

  /** Returns game path (i.e. full path to the chitin.key). */
  public String getPath() {
    return path;
  }

  /** Returns the custom home path. Otherwise, returns {@code null} if the default home path is used. */
  public String getHomePath() {
    return homePath;
  }

  /** Assigns a custom home path to the bookmark. Specify {@code null} to use the game defaults instead. */
  public void setHomePath(String hp) {
    homePath = (hp == null || hp.isEmpty()) ? null : hp;
  }

  /** Returns a list of available paths to executables for the current platform. */
  public List<String> getBinaryPaths() {
    return getBinaryPaths(Platform.getPlatform());
  }

  /** Returns a list of available paths to executables for the given platform. */
  public List<String> getBinaryPaths(Platform.OS os) {
    if (os == null) {
      os = Platform.getPlatform();
    }
    return Collections.unmodifiableList(binPaths.getOrDefault(os, new ArrayList<String>(1)));
  }

  /**
   * Assigns a new list of executable paths to the specified platform. Returns the previous path list if available.
   */
  public List<String> setBinaryPaths(Platform.OS os, List<String> pathList) {
    if (os == null) {
      os = Platform.getPlatform();
    }
    List<String> retVal = binPaths.get(os);

    List<String> newList = new ArrayList<>();
    if (pathList != null) {
      for (String path : pathList) {
        if (path != null && !(path = path.trim()).isEmpty()) {
          newList.add(path);
        }
      }
    }
    binPaths.put(os, newList);

    return retVal;
  }

  /** Returns associated menu item. */
  public JMenuItem getMenuItem() {
    return item;
  }

  /** Returns whether the bookmark points to an existing game installation. */
  public boolean isEnabled() {
    return (FileEx.create(FileManager.resolve(path)).isFile());
  }

  /** Returns ActionListener used by the associated menu item. */
  public ActionListener getActionListener() {
    return listener;
  }

  /** Assigns a new ActionListener object to the associated menu item. */
  public void setActionListener(ActionListener listener) {
    if (item != null) {
      item.removeActionListener(this.listener);
    }
    this.listener = listener;
    if (listener != null && item != null) {
      item.addActionListener(this.listener);
    }
  }

  /** Creates or updates associated menu item. */
  private void updateMenuItem() {
    if (item == null) {
      item = new JMenuItem(getName());
      item.setToolTipText(path);
      item.setActionCommand(MENUITEM_COMMAND);
      if (listener != null) {
        item.addActionListener(listener);
      }
    } else {
      item.setText(getName());
    }
    item.setEnabled(isEnabled());
  }

  /**
   * Assigns the specified {@link KeyStroke} to the menu item that is associated with this bookmark.
   * Previous accelerators are removed.
   *
   * @param accelerator The new {@code KeyStroke}. Specify {@code null} to remove any existing keystroke.
   */
  public void updateAccelerator(KeyStroke accelerator) {
    removeAccelerator();
    if (accelerator != null) {
      item.setAccelerator(accelerator);
    }
  }

  /** Removes the current accelerator keystroke from the menu item that is associated with the bookmark. */
  public void removeAccelerator() {
    removeAcceleratorFromMap(item.getInputMap(JComponent.WHEN_FOCUSED), item.getAccelerator());
    removeAcceleratorFromMap(item.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), item.getAccelerator());
    removeAcceleratorFromMap(item.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), item.getAccelerator());
    item.setAccelerator(null);
  }

  /** Helper method: Removes an accelerator from the given input map. */
  private void removeAcceleratorFromMap(InputMap map, KeyStroke accelerator) {
    if (map != null && accelerator != null) {
      map.remove(accelerator);
      final InputMap parentMap = map.getParent();
      if (parentMap != null) {
        parentMap.remove(accelerator);
      }
    }
  }

  /** Returns the command string used for all menu items. */
  public static String getCommand() {
    return MENUITEM_COMMAND;
  }

  /** Returns the Preferences key for the number of available Bookmark entries. */
  public static String getEntryCountKey() {
    return BOOKMARK_NUM_ENTRIES;
  }

  /** Returns the Preferences key for a specific BookmarkID. */
  public static String getGameKey(int idx) {
    if (idx >= 0) {
      return String.format(FMT_BOOKMARK_ID, idx + 1);
    } else {
      return null;
    }
  }

  /** Returns the Preferences key for a specific BookmarkPath. */
  public static String getPathKey(int idx) {
    if (idx >= 0) {
      return String.format(FMT_BOOKMARK_PATH, idx + 1);
    } else {
      return null;
    }
  }

  /** Returns the Preferences key for a specific BookmarkHomePath. */
  public static String getHomePathKey(int idx) {
    if (idx >= 0) {
      return String.format(FMT_BOOKMARK_HOME_PATH, idx + 1);
    } else {
      return null;
    }
  }

  /** Returns the Preferences key for a specific BookmarkName. */
  public static String getNameKey(int idx) {
    if (idx >= 0) {
      return String.format(FMT_BOOKMARK_NAME, idx + 1);
    } else {
      return null;
    }
  }

  /** Returns the Preferences key for a specific BookmarkBinPath for the current platform. */
  public static String getBinaryPathKey(int idx) {
    return getBinaryPathKey(Platform.getPlatform(), idx);
  }

  /** Returns the Preferences key for a specific BookmarkBinPath for the given platform. */
  public static String getBinaryPathKey(Platform.OS os, int idx) {
    if (idx >= 0) {
      return String.format(FMT_BOOKMARK_BIN_PATH, os.name().toUpperCase(Locale.ENGLISH), idx + 1);
    } else {
      return null;
    }
  }

  /**
   * Constructs a Preferences string value out of the specified list of path strings.
   *
   * @param os       Platform associated with the path strings. Needed to determine the correct path separator.
   * @param binPaths List of path strings.
   * @return A string consisting of concatenated path strings.
   */
  public static String packBinPaths(Platform.OS os, List<String> binPaths) {
    StringBuilder sb = new StringBuilder();
    if (os != null && binPaths != null && !binPaths.isEmpty()) {
      String sep = (os == Platform.OS.WINDOWS) ? ";" : ":";
      for (String binPath : binPaths) {
        String path = binPath;
        if (path != null && !(path = path.trim()).isEmpty()) {
          path = path.replace(sep, "?"); // hack: avoid ambiguity with path separator char
          if (sb.length() > 0) {
            sb.append(sep);
          }
          sb.append(path);
        }
      }
    }
    return sb.toString();
  }

  /**
   * Splits all paths defined in the specified argument and returns them as a list.
   */
  public static List<String> unpackBinPaths(Platform.OS os, String paths) {
    List<String> list = new ArrayList<>();
    if (os != null && paths != null) {
      String sep = (os == Platform.OS.WINDOWS) ? ";" : ":";
      String[] items = paths.split(sep);
      for (String item : items) {
        item = item.replace("?", sep); // hack: fix ambiguity with path separator char
        item = item.trim();
        if (!item.isEmpty()) {
          list.add(item);
        }
      }
    }
    return list;
  }

  /** Returns an array containing all supported {@code Platform.OS} types. */
  public static Platform.OS[] getSupportedOS() {
    return SUPPORTED_OS;
  }
}
