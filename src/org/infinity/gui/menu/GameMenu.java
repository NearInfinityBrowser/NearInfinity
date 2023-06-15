// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOError;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.infinity.NearInfinity;
import org.infinity.gui.BookmarkEditor;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.GameProperties;
import org.infinity.gui.OpenFileFrame;
import org.infinity.gui.StandardDialogs;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.Platform;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.tuples.Couple;

/**
 * Handles Game menu items for the {@link BrowserMenuBar}.
 */
public class GameMenu extends JMenu implements BrowserSubMenu, ActionListener {
  private final JMenu gameRecent = new JMenu("Recently opened games");
  private final List<RecentGame> recentList = new ArrayList<>();
  private final JPopupMenu.Separator gameRecentSeparator = new JPopupMenu.Separator();

  private final JMenu gameBookmarks = new JMenu("Bookmarked games");
  private final List<Bookmark> bookmarkList = new ArrayList<>();
  private final JPopupMenu.Separator gameBookmarkSeparator = new JPopupMenu.Separator();

  private final BrowserMenuBar menuBar;

  private final JMenuItem gameOpenFile;
  private final JMenuItem gameOpenGame;
  private final JMenuItem gameRefresh;
  private final JMenuItem gameExit;
  private final JMenuItem gameProperties;
  private final JMenuItem gameBookmarkAdd;
  private final JMenuItem gameBookmarkEdit;
  private final JMenuItem gameRecentClear;

  public GameMenu(BrowserMenuBar parent) {
    super("Game");
    setMnemonic(KeyEvent.VK_G);

    menuBar = parent;

    gameOpenFile = BrowserMenuBar.makeMenuItem("Open File...", KeyEvent.VK_F, Icons.ICON_OPEN_16.getIcon(), KeyEvent.VK_I, this);
    add(gameOpenFile);
    gameOpenGame = BrowserMenuBar.makeMenuItem("Open Game...", KeyEvent.VK_O, Icons.ICON_OPEN_16.getIcon(), KeyEvent.VK_O,
        NearInfinity.getInstance());
    gameOpenGame.setActionCommand("Open");
    add(gameOpenGame);
    gameRefresh = BrowserMenuBar.makeMenuItem("Refresh Tree", KeyEvent.VK_R, Icons.ICON_REFRESH_16.getIcon(), -1,
        NearInfinity.getInstance());
    gameRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    gameRefresh.setActionCommand("Refresh");
    add(gameRefresh);

    gameProperties = BrowserMenuBar.makeMenuItem("Game Properties...", KeyEvent.VK_P, Icons.ICON_EDIT_16.getIcon(), -1, this);
    add(gameProperties);

    addSeparator();

    // adding bookmarked games list
    gameBookmarks.setMnemonic('b');
    add(gameBookmarks);

    bookmarkList.clear();
    int gameCount = getMenuBar().getPrefsProfiles().getInt(Bookmark.getEntryCountKey(), 0);
    for (int i = 0; i < gameCount; i++) {
      Profile.Game game = Profile
          .gameFromString(getMenuBar().getPrefsProfiles().get(Bookmark.getGameKey(i), Profile.Game.Unknown.toString()));
      final String gamePath = getMenuBar().getPrefsProfiles().get(Bookmark.getPathKey(i), null);
      final String homePath = getMenuBar().getPrefsProfiles().get(Bookmark.getHomePathKey(i), null);
      final String gameName = getMenuBar().getPrefsProfiles().get(Bookmark.getNameKey(i), null);
      EnumMap<Platform.OS, List<String>> binPaths = null;
      for (final Platform.OS os : Bookmark.getSupportedOS()) {
        final String path = getMenuBar().getPrefsProfiles().get(Bookmark.getBinaryPathKey(os, i), null);
        if (path != null) {
          if (binPaths == null) {
            binPaths = new EnumMap<>(Platform.OS.class);
          }
          List<String> list = Bookmark.unpackBinPaths(os, path);
          binPaths.put(os, list);
        }
      }
      try {
        final Bookmark b = new Bookmark(gameName, game, gamePath, homePath, binPaths, this);
        addBookmarkedGame(bookmarkList.size(), b);
      } catch (NullPointerException e) {
        // skipping entry
      }
    }

    gameBookmarks.add(gameBookmarkSeparator);

    gameBookmarkAdd = new JMenuItem("Add current game...");
    gameBookmarkAdd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, BrowserMenuBar.getCtrlMask() | InputEvent.ALT_DOWN_MASK));
    gameBookmarkAdd.addActionListener(this);
    gameBookmarks.add(gameBookmarkAdd);

    gameBookmarkEdit = new JMenuItem("Edit bookmarks...");
    gameBookmarkEdit.addActionListener(this);
    gameBookmarks.add(gameBookmarkEdit);

    gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
    gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());

    // adding recently opened games list
    gameRecent.setMnemonic('r');
    add(gameRecent);

    recentList.clear();
    for (int i = 0; i < RecentGame.getEntryCount(); i++) {
      final Profile.Game game = Profile
          .gameFromString(parent.getPrefsProfiles().get(RecentGame.getGameKey(i), Profile.Game.Unknown.toString()));
      final String gamePath = parent.getPrefsProfiles().get(RecentGame.getPathKey(i), null);
      try {
        RecentGame rg = new RecentGame(game, gamePath, recentList.size(), this);
        addLastGame(recentList.size(), rg);
      } catch (NullPointerException e) {
        // skipping entry
      }
    }

    gameRecent.add(gameRecentSeparator);

    gameRecentClear = new JMenuItem("Clear list of recent games");
    gameRecentClear.addActionListener(this);
    gameRecent.add(gameRecentClear);

    gameRecent.setEnabled(!recentList.isEmpty());
    gameRecentSeparator.setVisible(!recentList.isEmpty());

    addSeparator();

    gameExit = BrowserMenuBar.makeMenuItem("Quit", KeyEvent.VK_Q, Icons.ICON_EXIT_16.getIcon(), KeyEvent.VK_Q,
        NearInfinity.getInstance());
    gameExit.setActionCommand("Exit");
    add(gameExit);
  }

  public void gameLoaded(Profile.Game oldGame, String oldFile) {
    // updating "Recently opened games" list
    for (int i = 0; i < recentList.size(); i++) {
      if (ResourceFactory.getKeyfile().toString().equalsIgnoreCase(recentList.get(i).getPath())) {
        removeLastGame(i);
        i--;
      }
    }

    if (oldGame != null && oldGame != Profile.Game.Unknown) {
      for (int i = 0; i < recentList.size(); i++) {
        if (oldFile.equalsIgnoreCase(recentList.get(i).getPath())) {
          removeLastGame(i);
          i--;
        }
      }
      addLastGame(0, new RecentGame(oldGame, oldFile, 0, this));
    }

    while (recentList.size() > RecentGame.getEntryCount()) {
      removeLastGame(recentList.size() - 1);
    }
  }

  public void storePreferences() {
    // storing bookmarks
    // 1. removing excess bookmark entries from preferences
    int oldSize = getMenuBar().getPrefsProfiles().getInt(Bookmark.getEntryCountKey(), 0);
    if (oldSize > bookmarkList.size()) {
      for (int i = bookmarkList.size(); i < oldSize; i++) {
        getMenuBar().getPrefsProfiles().remove(Bookmark.getNameKey(i));
        getMenuBar().getPrefsProfiles().remove(Bookmark.getPathKey(i));
        getMenuBar().getPrefsProfiles().remove(Bookmark.getHomePathKey(i));
        getMenuBar().getPrefsProfiles().remove(Bookmark.getGameKey(i));
        for (final Platform.OS os : Bookmark.getSupportedOS()) {
          getMenuBar().getPrefsProfiles().remove(Bookmark.getBinaryPathKey(os, i));
        }
      }
    }
    // 2. storing bookmarks in preferences
    getMenuBar().getPrefsProfiles().putInt(Bookmark.getEntryCountKey(), bookmarkList.size());
    for (int i = 0; i < bookmarkList.size(); i++) {
      final Bookmark bookmark = bookmarkList.get(i);
      getMenuBar().getPrefsProfiles().put(Bookmark.getNameKey(i), bookmark.getName());
      getMenuBar().getPrefsProfiles().put(Bookmark.getPathKey(i), bookmark.getPath());
      final String homePath = (bookmark.getHomePath() != null) ? bookmark.getHomePath() : "";
      getMenuBar().getPrefsProfiles().put(Bookmark.getHomePathKey(i), homePath);
      getMenuBar().getPrefsProfiles().put(Bookmark.getGameKey(i), bookmark.getGame().toString());
      for (final Platform.OS os : Bookmark.getSupportedOS()) {
        final String value = Bookmark.packBinPaths(os, bookmark.getBinaryPaths(os));
        if (value.isEmpty()) {
          getMenuBar().getPrefsProfiles().remove(Bookmark.getBinaryPathKey(os, i));
        } else {
          getMenuBar().getPrefsProfiles().put(Bookmark.getBinaryPathKey(os, i), value);
        }
      }
    }

    // storing recently used games
    for (int i = 0; i < RecentGame.getEntryCount(); i++) {
      if (i < recentList.size()) {
        final RecentGame rg = recentList.get(i);
        getMenuBar().getPrefsProfiles().put(RecentGame.getGameKey(i), rg.getGame().toString());
        getMenuBar().getPrefsProfiles().put(RecentGame.getPathKey(i), rg.getPath());
      } else {
        getMenuBar().getPrefsProfiles().remove(RecentGame.getGameKey(i));
        getMenuBar().getPrefsProfiles().remove(RecentGame.getPathKey(i));
      }
    }
  }

  /**
   * Attempts to find and return a matching bookmark object.
   *
   * @param keyFile The path to the game's chitin.key used to determine the correct bookmark instance.
   * @return The matching bookmark instance if available, {@code null} otherwise.
   */
  public Bookmark getBookmarkOf(Path keyFile) {
    if (keyFile != null) {
      String path = keyFile.toAbsolutePath().toString();
      for (Bookmark bookmark : bookmarkList) {
        if (bookmark.getPath().equalsIgnoreCase(path)) {
          return bookmark;
        }
      }
    }
    return null;
  }

  /**
   * Attempts to find a matching bookmark and returns its name.
   *
   * @param keyFile The path to the game's chitin.key.
   * @return The bookmark name of a matching game or {@code null} otherwise.
   */
  public String getBookmarkName(Path keyFile) {
    Bookmark bookmark = getBookmarkOf(keyFile);
    return (bookmark != null) ? bookmark.getName() : null;
  }

  @Override
  public BrowserMenuBar getMenuBar() {
    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == gameOpenFile) {
      ChildFrame.show(OpenFileFrame.class, OpenFileFrame::new);
    } else if (event.getActionCommand().equals(Bookmark.getCommand())) {
      // Bookmark item selected
      int selected = -1;
      for (int i = 0; i < bookmarkList.size(); i++) {
        if (event.getSource() == bookmarkList.get(i).getMenuItem()) {
          selected = i;
          break;
        }
      }
      if (selected != -1) {
        final Bookmark bookmark = bookmarkList.get(selected);
        Path keyFile = FileManager.resolve(bookmark.getPath());
        if (!FileEx.create(keyFile).isFile()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
              bookmark.getPath() + " could not be found", "Open game failed",
              JOptionPane.ERROR_MESSAGE);
        } else {
          boolean isEqual = false;
          try {
            isEqual = keyFile.equals(Profile.getChitinKey().toAbsolutePath());
          } catch (IOError e) {
            e.printStackTrace();
          }
          if (!isEqual) {
            int confirm = JOptionPane.YES_OPTION;
            if (getMenuBar().getOptionsMenu().showOpenBookmarksPrompt()) {
              String message = String.format("Open bookmarked game \"%s\"?", bookmark.getName());
              Couple<Integer, Boolean> result = StandardDialogs.showConfirmDialogExtra(NearInfinity.getInstance(),
                  message, "Open game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                  StandardDialogs.Extra.with(StandardDialogs.Extra.MESSAGE_DO_NOT_SHOW_PROMPT,
                      "Confirmation prompt can be enabled or disabled in the options menu."));
              if (result.getValue1()) {
                getMenuBar().getOptionsMenu().setShowOpenBookmarksPrompt(false);
              }
              confirm = result.getValue0();
            }
            if (confirm == JOptionPane.YES_OPTION) {
              NearInfinity.getInstance().openGame(keyFile);
            }
          }
        }
      }
    } else if (event.getActionCommand().equals(RecentGame.getCommand())) {
      // Recently opened game item selected
      int selected = -1;
      for (int i = 0; i < recentList.size(); i++) {
        if (event.getSource() == recentList.get(i).getMenuItem()) {
          selected = i;
          break;
        }
      }
      if (selected != -1) {
        Path keyFile = FileManager.resolve(recentList.get(selected).getPath());
        if (!FileEx.create(keyFile).isFile()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
              recentList.get(selected).getPath() + " could not be found", "Open game failed",
              JOptionPane.ERROR_MESSAGE);
        } else {
          NearInfinity.getInstance().openGame(keyFile);
        }
      }
    } else if (event.getSource() == gameProperties) {
      new GameProperties(NearInfinity.getInstance());
    } else if (event.getSource() == gameBookmarkAdd) {
      Object name = Profile.getProperty(Profile.Key.GET_GAME_TITLE);
      Bookmark bookmark = getBookmarkOf(Profile.getChitinKey());
      if (bookmark != null) {
        int retVal = JOptionPane.showConfirmDialog(NearInfinity.getInstance(),
            "The game has already been bookmarked.\nDo you want to update it?", "Update bookmark",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (retVal == JOptionPane.YES_OPTION) {
          name = bookmark.getName();
        } else {
          return;
        }
      }
      name = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter bookmark name:", "Add game to bookmarks",
          JOptionPane.QUESTION_MESSAGE, null, null, name);
      if (name != null) {
        addNewBookmark(name.toString());
      }
    } else if (event.getSource() == gameBookmarkEdit) {
      List<Bookmark> list = BookmarkEditor.editBookmarks(bookmarkList);
      if (list != null) {
        bookmarkList.clear();
        bookmarkList.addAll(list);
        updateBookmarkedGames();
      }
    } else if (event.getSource() == gameRecentClear) {
      while (!recentList.isEmpty()) {
        removeLastGame(0);
      }
    }
  }

  /** Updates list of bookmark menu items. */
  private void updateBookmarkedGames() {
    // 1. remove old bookmark items from menu
    while (gameBookmarks.getPopupMenu().getComponentCount() > 0) {
      if (gameBookmarks.getPopupMenu().getComponent(0) != gameBookmarkSeparator) {
        gameBookmarks.getPopupMenu().remove(0);
      } else {
        break;
      }
    }

    // 2. add new bookmark items to menu
    for (int i = 0, size = bookmarkList.size(); i < size; i++) {
      gameBookmarks.insert(bookmarkList.get(i).getMenuItem(), i);
    }
    updateBookmarkShortcuts();
    gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
    gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());

    // Updating current game if needed
    Bookmark bookmark = getBookmarkOf(Profile.getChitinKey());
    if (bookmark != null) {
      Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, bookmark.getName());
      NearInfinity.getInstance().updateWindowTitle();
    }
  }

  /** Removes the bookmark specified by item index from the list and associated menu. */
  private void removeBookmarkedGame(int idx) {
    if (idx >= 0 && idx < bookmarkList.size()) {
      Bookmark b = bookmarkList.remove(idx);
      if (b != null) {
        b.setActionListener(null);
        updateBookmarkShortcuts();
      }
      if (gameBookmarks.getPopupMenu().getComponent(idx) == b.getMenuItem()) {
        gameBookmarks.getPopupMenu().remove(idx);
      } else {
        for (int i = 0, count = gameBookmarks.getPopupMenu().getComponentCount(); i < count; i++) {
          if (gameBookmarks.getPopupMenu().getComponent(i) == b.getMenuItem()) {
            gameBookmarks.getPopupMenu().remove(i);
            break;
          }
        }
      }
      Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, null);
      NearInfinity.getInstance().updateWindowTitle();
    }
  }

  /** Adds the specified bookmark to the list and associated menu. */
  private void addBookmarkedGame(int idx, Bookmark bookmark) {
    if (idx < 0) {
      idx = 0;
    } else if (idx > bookmarkList.size()) {
      idx = bookmarkList.size();
    }

    // use either separator item or menu item count as upper bounds for inserting new bookmark items
    int separatorIdx = gameBookmarks.getPopupMenu().getComponentIndex(gameBookmarkSeparator);
    if (separatorIdx < 0) {
      separatorIdx = gameBookmarks.getPopupMenu().getComponentCount();
    }

    if (bookmark != null && idx <= separatorIdx) {
      bookmarkList.add(idx, bookmark);
      updateBookmarkShortcuts();
      gameBookmarks.insert(bookmark.getMenuItem(), idx);
      gameBookmarkSeparator.setVisible(!bookmarkList.isEmpty());
      gameBookmarkEdit.setEnabled(!bookmarkList.isEmpty());
      Profile.addProperty(Profile.Key.GET_GAME_DESC, Profile.Type.STRING, bookmark.getName());
      NearInfinity.getInstance().updateWindowTitle();
    }
  }

  /** Adds or replaces the current game to the bookmark section. */
  private void addNewBookmark(String name) {
    if (name != null) {
      name = name.trim();
      if (name.isEmpty()) {
        name = Profile.getProperty(Profile.Key.GET_GAME_TITLE);
      }
      Profile.Game game = Profile.getGame();
      String path = Profile.getChitinKey().toAbsolutePath().toString();
      Bookmark b = new Bookmark(name, game, path, this);

      // check whether to replace existing bookmark
      Bookmark curBookmark = getBookmarkOf(Profile.getChitinKey());
      int idx = (curBookmark != null) ? bookmarkList.indexOf(curBookmark) : -1;
      if (idx >= 0) {
        // replace existing bookmark
        removeBookmarkedGame(idx);
        addBookmarkedGame(idx, b);
      } else {
        // add new bookmark
        addBookmarkedGame(bookmarkList.size(), b);
      }
    } else {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No name specified.", "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  /** Updates shortcuts for bookmark menu items */
  private void updateBookmarkShortcuts() {
    for (int i = 0, count = bookmarkList.size(); i < count; i++) {
      final Bookmark bookmark = bookmarkList.get(i);
      if (i < 10) {
        // Ctrl+Alt+[digit]
        int key = (i < 9) ? KeyEvent.VK_1 + i : KeyEvent.VK_0;
        bookmark.updateAccelerator(KeyStroke.getKeyStroke(key, BrowserMenuBar.getCtrlMask() | InputEvent.ALT_DOWN_MASK));
      } else if (i < 20) {
        // Ctrl+Alt+Shift+[digit]
        int key = (i < 19) ? KeyEvent.VK_1 + (i % 10) : KeyEvent.VK_0;
        bookmark.updateAccelerator(
            KeyStroke.getKeyStroke(key, BrowserMenuBar.getCtrlMask() | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
      } else {
        // No shortcut
        bookmark.updateAccelerator(null);
      }
    }
  }

  /** Adds the specified last game entry to the list. */
  private void addLastGame(int idx, RecentGame rg) {
    if (rg != null) {
      if (idx < 0 || idx > recentList.size()) {
        idx = recentList.size();
      }
      rg.setIndex(idx);
      recentList.add(idx, rg);
      gameRecent.insert(rg.getMenuItem(), idx);
      gameRecent.setEnabled(!recentList.isEmpty());
      gameRecentSeparator.setVisible(!recentList.isEmpty());

      for (int i = 0; i < recentList.size(); i++) {
        recentList.get(i).setIndex(i);
      }
    }
  }

  /** Removes the specified last game entry from the list. */
  private void removeLastGame(int idx) {
    if (idx >= 0 && idx < recentList.size()) {
      recentList.get(idx).clear();
      recentList.remove(idx);
      gameRecent.setEnabled(!recentList.isEmpty());
      gameRecentSeparator.setVisible(!recentList.isEmpty());

      for (int i = 0; i < recentList.size(); i++) {
        recentList.get(i).setIndex(i);
      }
    }
  }
}
