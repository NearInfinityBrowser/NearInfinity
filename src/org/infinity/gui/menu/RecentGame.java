// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.infinity.resource.Profile;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

/** Manages individual "Recently used games" entries. */
public class RecentGame implements Cloneable {
  /** "Recently opened games" preferences entries (numbers are 1-based). */
  private static final int MAX_LASTGAME_ENTRIES = 10;

  private static final String FMT_LASTGAME_IDS = "LastGameID%d";
  private static final String FMT_LASTGAME_PATH = "LastGamePath%d";

  private static final String MENUITEM_COMMAND = "OpenOldGame";

  private final Profile.Game game;
  private final String path;

  private JMenuItem item;
  private ActionListener listener;
  private int index;

  public RecentGame(Profile.Game game, String path, int index, ActionListener listener) {
    if (game == null || game == Profile.Game.Unknown || path == null
        || !FileEx.create(FileManager.resolve(path)).isFile()) {
      throw new NullPointerException();
    }
    this.game = game;
    this.path = path;
    this.index = -1;
    this.listener = listener;
    setIndex(index);
  }

  @Override
  public String toString() {
    if (index >= 0) {
      return String.format("%d  %s", index + 1, game.getTitle());
    } else {
      return game.getTitle();
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new RecentGame(getGame(), getPath(), getIndex(), getActionListener());
  }

  /** Returns game type. */
  public Profile.Game getGame() {
    return game;
  }

  /** Returns game path (i.e. full path to the chitin.key). */
  public String getPath() {
    return path;
  }

  /** Returns associated menu item. */
  public JMenuItem getMenuItem() {
    return item;
  }

  /** Returns current entry index. */
  public int getIndex() {
    return index;
  }

  /** Updates existing menu item or creates a new one, based on the given index. */
  public void setIndex(int index) {
    if (index >= 0 && index < getEntryCount() && index != this.index) {
      this.index = index;
      if (item == null) {
        item = new JMenuItem(toString());
        item.setToolTipText(path);
        item.setActionCommand(MENUITEM_COMMAND);
        if (listener != null) {
          item.addActionListener(listener);
        }
      } else {
        item.setText(toString());
      }
    }
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

  /** Removes the currently associated menu item. */
  public void clear() {
    if (item != null) {
      if (listener != null) {
        item.removeActionListener(listener);
        item.setEnabled(false);
        if (item.getParent() != null) {
          item.getParent().remove(item);
        }
        item = null;
      }
    }
  }

  /** Returns the command string used for all menu items. */
  public static String getCommand() {
    return MENUITEM_COMMAND;
  }

  /** Returns the max. number of supported last game entries. */
  public static int getEntryCount() {
    return MAX_LASTGAME_ENTRIES;
  }

  /** Returns the Preferences key for a specific LastGameID. */
  public static String getGameKey(int index) {
    if (index >= 0 && index < getEntryCount()) {
      return String.format(FMT_LASTGAME_IDS, index + 1);
    } else {
      return null;
    }
  }

  /** Returns the Preferences key for a specific LastGamePath. */
  public static String getPathKey(int index) {
    if (index >= 0 && index < getEntryCount()) {
      return String.format(FMT_LASTGAME_PATH, index + 1);
    } else {
      return null;
    }
  }
}