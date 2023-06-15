// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager.LookAndFeelInfo;

import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.ViewableContainer;
import org.infinity.util.Misc;

/**
 * Global instance of Near Infinity's menu bar.
 */
public final class BrowserMenuBar extends JMenuBar implements KeyEventDispatcher {
  private static final LookAndFeelInfo DEFAULT_LOOKFEEL =
      new LookAndFeelInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel");

  /** Name of the child node in the GUI preferences path. */
  private static final String PREFS_PROFILES_NODE = "Profiles";

  private static BrowserMenuBar menuBar;

  private final GameMenu gameMenu;
  private final FileMenu fileMenu;
  private final EditMenu editMenu;
  private final SearchMenu searchMenu;
  private final ToolsMenu toolsMenu;
  private final OptionsMenu optionsMenu;
  private final HelpMenu helpMenu;

  private final Preferences prefsGui;
  private final Preferences prefsProfiles;

  /** Returns {@true} if this class has been instantiated. */
  public static boolean isInstantiated() {
    return (menuBar != null);
  }

  /** Returns the singleton {@code BrowserMenuBar} instance. */
  public static BrowserMenuBar getInstance() {
    return menuBar;
  }

  /** Returns the (first) index of the selected AbstractButton array. */
  public static int getSelectedButtonIndex(AbstractButton[] items, int defaultIndex) {
    int retVal = defaultIndex;
    if (items != null && items.length > 0) {
      for (int i = 0; i < items.length; i++) {
        if (items[i] != null && items[i].isSelected()) {
          retVal = i;
          break;
        }
      }
    }
    return retVal;
  }

  /** Returns the platform-specific shortcut key (e.g. Ctrl on Win/Linux, Meta on Mac). */
  public static int getCtrlMask() {
    return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  }

  /** Returns the default Look&Feel theme for Near Infinity. */
  public static LookAndFeelInfo getDefaultLookAndFeel() {
    return DEFAULT_LOOKFEEL;
  }

  public static JMenuItem makeMenuItem(String name, int menuKey, Icon icon, int shortKey, ActionListener listener) {
    JMenuItem item = new JMenuItem(name);
    if (menuKey != -1) {
      item.setMnemonic(menuKey);
    }
    if (icon != null) {
      item.setIcon(icon);
    }
    if (shortKey != -1) {
      item.setAccelerator(KeyStroke.getKeyStroke(shortKey, getCtrlMask()));
    }
    if (listener != null) {
      item.addActionListener(listener);
    }
    return item;
  }

  public BrowserMenuBar() {
    if (menuBar != null) {
      throw new java.lang.UnsupportedOperationException("Instantiating this class multiple times not allowed");
    }
    menuBar = this;

    // For backwards compatibility: Use former BrowserMenuBar package path as Preferences node
    prefsGui = Preferences.userRoot().node(Misc.prefsNodeName("org.infinity.gui.BrowserMenuBar"));
    prefsProfiles = prefsGui.node(PREFS_PROFILES_NODE);
    gameMenu = new GameMenu(this);
    fileMenu = new FileMenu(this);
    editMenu = new EditMenu(this);
    searchMenu = new SearchMenu(this);
    toolsMenu = new ToolsMenu(this);
    optionsMenu = new OptionsMenu(this);
    helpMenu = new HelpMenu(this);
    add(gameMenu);
    add(fileMenu);
    add(editMenu);
    add(searchMenu);
    add(toolsMenu);
    add(optionsMenu);
    add(helpMenu);
  }

  /** Returns the main Preferences instance. */
  public Preferences getPrefs() {
    return prefsGui;
  }

  /** Returns the Preferences instance for profile-specific settings. */
  public Preferences getPrefsProfiles() {
    return prefsProfiles;
  }

  /** Provides access to the "Game" menu. */
  public GameMenu getGameMenu() {
    return gameMenu;
  }

  /** Provides access to the "File" menu. */
  public FileMenu getFileMenu() {
    return fileMenu;
  }

  /** Provides access to the "Edit" menu. */
  public EditMenu getEditMenu() {
    return editMenu;
  }

  /** Provides access to the "Search" menu. */
  public SearchMenu getSearchMenu() {
    return searchMenu;
  }

  /** Provides access to the "Tools" menu. */
  public ToolsMenu getToolsMenu() {
    return toolsMenu;
  }

  /** Provides access to the "Options" menu. */
  public OptionsMenu getOptionsMenu() {
    return optionsMenu;
  }

  /** Provides access to the "Help" menu. */
  public HelpMenu getHelpMenu() {
    return helpMenu;
  }

  public void gameLoaded(Profile.Game oldGame, String oldFile) {
    gameMenu.gameLoaded(oldGame, oldFile);
    fileMenu.gameLoaded();
    searchMenu.gameLoaded();
    optionsMenu.gameLoaded();
  }

  public void storePreferences() {
    toolsMenu.storePreferences();
    optionsMenu.storePreferences();
    gameMenu.storePreferences();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    final KeyStroke acc = toolsMenu.getDumpDebugInfoItem().getAccelerator();
    if (!e.isConsumed() && acc.equals(KeyStroke.getKeyStrokeForEvent(e))) {
      e.consume();
      if (toolsMenu.getDumpDebugInfoItem().isEnabled()) {
        dumpDebugInfo();
      }
      return true;
    }
    return false;
  }

  /**
   * Performs dumping to {@link System#out standard output} some useful debug information about currect active objects
   * in the editor:
   * <ol>
   * <li>Class and title of top-level window</li>
   * <li>Class and name of current {@link Viewable}, if such exist</li>
   * <li>Name of current resource, if viewable is {@link Resource}</li>
   * <li>Class and name of current field if viewable has opened editor</li>
   * </ol>
   */
  public static void dumpDebugInfo() {
    final Frame frame = findActiveFrame();
    if (frame == null) {
      return;
    }
    System.out.println("Current Window  : " + frame.getClass() + ", title: " + frame.getTitle());
    if (!(frame instanceof ViewableContainer)) {
      return;
    }
    final Viewable v = ((ViewableContainer) frame).getViewable();
    final String name = v instanceof StructEntry ? ", name: " + ((StructEntry) v).getName() : "";
    System.out.println("        Viewable: " + (v == null ? null : (v.getClass() + name)));
    if (v instanceof Resource) {
      System.out.println("        Resource: " + ((Resource) v).getResourceEntry());
    }
    if (v instanceof AbstractStruct) {
      final StructViewer viewer = ((AbstractStruct) v).getViewer();
      if (viewer != null) {
        final StructEntry entry = viewer.getSelectedEntry();
        final String info = entry == null ? null : (entry.getClass() + ", name: " + entry.getName());
        System.out.println("        Field   : " + info);
      }
    }
  }

  private static Frame findActiveFrame() {
    for (Frame frame : Frame.getFrames()) {
      if (frame.isActive()) {
        return frame;
      }
    }
    return null;
  }
}
