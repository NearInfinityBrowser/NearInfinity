// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;

import org.infinity.datatype.ProRef;
import org.infinity.datatype.Song2daBitmap;
import org.infinity.datatype.SpellProtType;
import org.infinity.datatype.Summon2daBitmap;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.DataMenuItem;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.OpenFileFrame;
import org.infinity.gui.PopupWindowEvent;
import org.infinity.gui.PopupWindowListener;
import org.infinity.gui.QuickSearch;
import org.infinity.gui.ResourceTree;
import org.infinity.gui.StatusBar;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.bcs.Signatures;
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.effects.BaseOpcode;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.key.ResourceTreeModel;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.updater.UpdateCheck;
import org.infinity.updater.UpdateInfo;
import org.infinity.updater.Updater;
import org.infinity.updater.Utils;
import org.infinity.util.CharsetDetector;
import org.infinity.util.CreMapCache;
import org.infinity.util.FileDeletionHook;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IniMapCache;
import org.infinity.util.LauncherUtils;
import org.infinity.util.Misc;
import org.infinity.util.Platform;
import org.infinity.util.StringTable;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.DlcManager;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.tuples.Couple;

public final class NearInfinity extends JFrame implements ActionListener, ViewableContainer {
  private static final int[] JAVA_VERSION = { 1, 8 }; // the minimum java version supported

  private static final String KEYFILENAME             = "chitin.key";
  private static final String WINDOW_SIZEX            = "WindowSizeX";
  private static final String WINDOW_SIZEY            = "WindowSizeY";
  private static final String WINDOW_POSX             = "WindowPosX";
  private static final String WINDOW_POSY             = "WindowPosY";
  private static final String WINDOW_STATE            = "WindowState";
  private static final String WINDOW_SPLITTER         = "WindowSplitter";
  private static final String LAST_GAMEDIR            = "LastGameDir";
  private static final String TABLE_WIDTH_ATTR        = "TableColWidthAttr";
  private static final String TABLE_WIDTH_OFS         = "TableColWidthOfs";
  private static final String TABLE_WIDTH_SIZE        = "TableColWidthSize";
  private static final String TABLE_PANEL_HEIGHT      = "TablePanelHeight";
  private static final String OPTION_GLOBAL_FONT_SIZE = "GlobalFontSize";
  private static final String APP_UI_SCALE_ENABLED    = "AppUiScaleEnabled";
  private static final String APP_UI_SCALE_FACTOR     = "AppUiScaleFactor";

  private static final String STATUSBAR_TEXT_FMT = "Welcome to Near Infinity! - %s @ %s - %d files available";

  static {
    // Setting Swing UI scale factor (only available for Java 9 or higher)
    if (Platform.JAVA_VERSION > 8) {
      final int uiScale = getUiScalingOption();
      if (uiScale > 0) {
        // Must be set BEFORE any Swing Library calls to be effective
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.uiScale", Double.toString(uiScale / 100.0));
      }
    }

    if (Platform.IS_MACOS) {
      // Enforce proper macOS menu bar integration
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }
  }

  private static final InfinityTextArea CONSOLE_TEXT = new InfinityTextArea(true);

  private static NearInfinity browser;

  private final JPanel containerpanel;
  private final JSplitPane spSplitter;
  private final ResourceTree tree;
  private final StatusBar statusBar;
  private final WindowBlocker blocker = new WindowBlocker(this);

  /** Stores table column widths for "Attribute", "Value" and "Offset" */
  private final int[] tableColumnWidth = { -1, -1, -1, -1 };

  private Viewable viewable;
  private ButtonPopupWindow bpwQuickSearch;
  private JButton btnLaunchGame;
  private JPopupMenu launchMenu;
  private int tablePanelHeight;
  private ProgressMonitor pmProgress;
  private int progressIndex, globalFontSize, uiScalingFactor;
  private boolean uiScalingEnabled;

  private static Path findKeyfile() {
    JFileChooser chooser;
    if (Profile.getGameRoot() == null) {
      chooser = new JFileChooser(new File("."));
    } else {
      chooser = new JFileChooser(Profile.getGameRoot().toFile());
    }
    chooser.setDialogTitle("Open game: Locate keyfile");
    chooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".key");
      }

      @Override
      public String getDescription() {
        return "Infinity Keyfile (.KEY)";
      }
    });
    if (chooser.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().toPath();
    }
    return null;
  }

  public static InfinityTextArea getConsoleText() {
    return CONSOLE_TEXT;
  }

  public static NearInfinity getInstance() {
    return browser;
  }

  /** Returns the NearInfinity version. */
  public static String getVersion() {
    return BrowserMenuBar.VERSION;
  }

  public static void printHelp(String jarFile) {
    if (jarFile == null || jarFile.isEmpty() || !jarFile.toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
      jarFile = "NearInfinity.jar";
    }
    System.out.format("Usage: java -jar %s [options] [game_path]", jarFile).println();
    System.out.println("\nOptions:");
    System.out.println("  -v, -version      Display version information.");
    System.out.println("  -h, -help         Display this help.");
    System.out.println("  -no-update        Disables the update check option in the menu bar.");
    System.out.println("  -no-launch-game   Hides the \"Launch game\" button.");
    System.out.println("  -t type           Force the current or specified game to be of");
    System.out.println("                    specific type. (Use with care!)");
    System.out.println("                    Supported game types:");
    for (final Profile.Game game : Profile.Game.values()) {
      System.out.println("                      " + game.toString());
    }
    System.out.println("\nExamples:");
    System.out.format("Specify game path: java -jar %s \"C:\\Games\\Baldurs Gate II\"", jarFile).println();
    System.out.format("Force game type:   java -jar %s -t bg2tob", jarFile).println();
    System.out.format("Display version:   java -jar %s -v", jarFile).println();
    System.out.format("Display help:      java -jar %s -help", jarFile).println();
  }

  /** Advances the progress monitor by one step with optional note. */
  public static void advanceProgress(String note) {
    if (getInstance() != null && getInstance().pmProgress != null) {
      getInstance().pmProgress.setNote((note != null) ? note : "");
      getInstance().pmProgress.setProgress(++getInstance().progressIndex);
    }
  }

  public static void main(String args[]) {
    Profile.Game forcedGame = null;
    Path gameOverride = null;
    boolean enableUpdate = true;
    boolean showLaunchGame = true;

    for (int idx = 0; idx < args.length; idx++) {
      switch (args[idx].toLowerCase(Locale.ENGLISH)) {
        case "-v":
        case "-version":
          System.out.println("Near Infinity " + getVersion());
          System.exit(0);
        case "-h":
        case "-help":
          String jarFile = Utils.getJarFileName(NearInfinity.class);
          if (!jarFile.isEmpty()) {
            jarFile = FileManager.resolve(jarFile).getFileName().toString();
          }
          printHelp(jarFile);
          System.exit(0);
        case "-no-update":
          enableUpdate = false;
          break;
        case "-no-launch-game":
          showLaunchGame = false;
          break;
        case "-t":
          if (idx + 1 < args.length) {
            idx++;
            String type = args[idx];
            Profile.Game[] games = Profile.Game.values();
            for (final Profile.Game game : games) {
              if (game.toString().equalsIgnoreCase(type)) {
                forcedGame = game;
                break;
              }
            }
            break;
          } else {
            System.err.println("Missing argument for option '-t'.");
            System.exit(1);
          }
        default:
          // Override game folder via application parameter
          Path f = FileManager.resolve(args[idx]);
          if (FileEx.create(f).isFile()) {
            f = f.getParent();
          }
          if (FileEx.create(f).isDirectory()) {
            gameOverride = f;
          }
      }
    }

    String[] javaVersion = System.getProperty("java.specification.version").split("\\.");
    try {
      for (int i = 0; i < Math.min(JAVA_VERSION.length, javaVersion.length); i++) {
        if (Integer.parseInt(javaVersion[i]) < JAVA_VERSION[i]) {
          JOptionPane.showMessageDialog(null,
                                        String.format("Version %d.%d or newer of Java is required!",
                                                      JAVA_VERSION[0], JAVA_VERSION[1]),
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
          System.exit(10);
        }
      }
    } catch (Exception e) { // Try starting anyway if the test goes sour
      e.printStackTrace();
    }
    System.setOut(new ConsoleStream(System.out, CONSOLE_TEXT));
    System.setErr(new ConsoleStream(System.err, CONSOLE_TEXT));

    final Options options = new Options(gameOverride, forcedGame, enableUpdate, showLaunchGame);
    new NearInfinity(options);
  }

  private NearInfinity(Options options) {
    super("Near Infinity");
    browser = this;
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setAppIcon();

    // setting more reasonable tooltip timings
    ToolTipManager.sharedInstance().setDismissDelay(8000);

    // FileDeletionHook provides a way to delete files when the Java Virtual Machine shuts down
    Runtime.getRuntime().addShutdownHook(FileDeletionHook.getInstance());

    // Migrate preferences from "infinity" to "org.infinity" if needed
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    migratePreferences("infinity", prefs, true);

    // updating UI scale override
    uiScalingEnabled = prefs.getBoolean(APP_UI_SCALE_ENABLED, false);
    uiScalingFactor = prefs.getInt(APP_UI_SCALE_FACTOR, 100);

    // updating relative default font size globally
    globalFontSize = Math.max(50, Math.min(400, prefs.getInt(OPTION_GLOBAL_FONT_SIZE, 100)));
    resizeUIFont(globalFontSize);

    final BrowserMenuBar menu = new BrowserMenuBar();
    menu.setUpdateMenuEnabled(options.isUpdateEnabled());
    menu.setLaunchGameMenuEnabled(options.isLaunchGameVisible());
    // Registers menu as key event dispatcher to intercept Ctrl+Shift+D from any window
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(menu);
    setJMenuBar(menu);

    final String lastDir;
    if (options.isGameOverride() && FileEx.create(options.getGameOverride()).isDirectory()) {
      lastDir = options.getGameOverride().toString();
    } else {
      lastDir = prefs.get(LAST_GAMEDIR, null);
    }

    final Path keyFile;
    Path path;
    if (FileEx.create(path = FileManager.resolve(KEYFILENAME)).isFile()) {
      keyFile = path;
    } else if (lastDir != null && FileEx.create(path = FileManager.resolve(lastDir, KEYFILENAME)).isFile()) {
      keyFile = path;
    } else {
      keyFile = findKeyfile();
    }
    if (keyFile == null) {
      System.exit(10);
    }

    showProgress("Starting Near Infinity" + Misc.MSG_EXPAND_LARGE, 6);
    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile), options.getForcedGame());

        // making sure vital game resources are accessible
        Path tlkFile = Profile.getProperty(Profile.Key.GET_GAME_DIALOG_FILE);
        try {
          checkFileAccess(tlkFile);
        } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(NearInfinity.this,
                                        String.format("Unable to open the game \"%s\".\n" +
                                                      "The file \"%s\" is locked by another process.",
                                                      Profile.getProperty(Profile.Key.GET_GAME_TITLE), tlkFile.getFileName()),
                                        "Near Infinity Error", JOptionPane.ERROR_MESSAGE);
          System.exit(10);
        }

        advanceProgress("Initializing GUI...");
        BrowserMenuBar.getInstance().gameLoaded(Profile.Game.Unknown, null);
        CreMapCache.reset();
        BaseOpcode.initOpcodes();
//        if (BrowserMenuBar.getInstance().getMonitorFileChanges()) {
//          FileWatcher.getInstance().start();
//        }

        return null;
      }
    };

    try {
      try {
        worker.execute();
        worker.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        System.exit(10);
      }

      addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent event) {
          quit();
        }
      });
      try {
        LookAndFeelInfo info = BrowserMenuBar.getInstance().getLookAndFeel();
        UIManager.setLookAndFeel(info.getClassName());
        SwingUtilities.updateComponentTreeUI(this);
      } catch (Exception e) {
        e.printStackTrace();
      }

      statusBar = new StatusBar();
      ResourceTreeModel treemodel = ResourceFactory.getResourceTreeModel();
      updateWindowTitle();
      final String msg = String.format(STATUSBAR_TEXT_FMT,
                                       Profile.getProperty(Profile.Key.GET_GAME_TITLE),
                                       Profile.getGameRoot(), treemodel.size());
      statusBar.setMessage(msg);
      tree = new ResourceTree(treemodel);
      tree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      JToolBar toolBar = new JToolBar("Navigation", SwingConstants.HORIZONTAL);
      toolBar.setMargin(new Insets(4, 4, 4, 4));
      JButton b;
      toolBar.setRollover(true);
      toolBar.setFloatable(false);
      b = new JButton(Icons.ICON_EXPAND_16.getIcon());
      b.addActionListener(this);
      b.setActionCommand("Expand");
      b.setToolTipText("Expand selected node");
      b.setMargin(new Insets(4, 4, 4, 4));
      toolBar.add(b);
      b = new JButton(Icons.ICON_COLLAPSE_16.getIcon());
      b.addActionListener(this);
      b.setActionCommand("Collapse");
      b.setToolTipText("Collapse selected node");
      b.setMargin(new Insets(4, 4, 4, 4));
      toolBar.add(b);
      toolBar.addSeparator(new Dimension(8, 24));
      b = new JButton(Icons.ICON_EXPAND_ALL_24.getIcon());
      b.addActionListener(this);
      b.setActionCommand("ExpandAll");
      b.setToolTipText("Expand all");
      b.setMargin(new Insets(0, 0, 0, 0));
      toolBar.add(b);
      b = new JButton(Icons.ICON_COLLAPSE_ALL_24.getIcon());
      b.addActionListener(this);
      b.setActionCommand("CollapseAll");
      b.setToolTipText("Collapse all");
      b.setMargin(new Insets(0, 0, 0, 0));
      toolBar.add(b);
      toolBar.addSeparator(new Dimension(8, 24));
      bpwQuickSearch = new ButtonPopupWindow(Icons.ICON_MAGNIFY_16.getIcon());
      bpwQuickSearch.setToolTipText("Find resource");
      bpwQuickSearch.setMargin(new Insets(4, 4, 4, 4));
      toolBar.add(bpwQuickSearch);
      bpwQuickSearch.addPopupWindowListener(new PopupWindowListener() {

        @Override
        public void popupWindowWillBecomeVisible(PopupWindowEvent event) {
          // XXX: Working around a visual glitch in QuickSearch's JComboBox popup list
          // by creating new QuickSearch instances on activation
          bpwQuickSearch.setContent(new QuickSearch(bpwQuickSearch, tree));

          SwingUtilities.invokeLater(() -> {
            Component c = bpwQuickSearch.getContent();
            if (c != null) {
              c.requestFocusInWindow();
            }
          });
        }

        @Override
        public void popupWindowWillBecomeInvisible(PopupWindowEvent event) {
          SwingUtilities.invokeLater(() -> {
            bpwQuickSearch.setContent(null);
            tree.requestFocusInWindow();
          });
        }
      });

      toolBar.add(Box.createHorizontalGlue());
      btnLaunchGame = new JButton(Icons.ICON_LAUNCH_24.getIcon());
      btnLaunchGame.setFocusable(false);
      btnLaunchGame.setEnabled(false);
      btnLaunchGame.setMargin(new Insets(0, 0, 0, 0));
      btnLaunchGame.setToolTipText("Launch game");
      btnLaunchGame.addActionListener(this);
      btnLaunchGame.setVisible(options.isLaunchGameVisible());
      toolBar.add(btnLaunchGame);
      launchMenu = new JPopupMenu();

      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(tree, BorderLayout.CENTER);
      leftPanel.add(toolBar, BorderLayout.NORTH);

      containerpanel = new JPanel(new BorderLayout());
      containerpanel.add(createJavaInfoPanel(), BorderLayout.CENTER);
      spSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, containerpanel);
      spSplitter.setBorder(BorderFactory.createEmptyBorder());
      spSplitter.setDividerLocation(prefs.getInt(WINDOW_SPLITTER, 200));
      Container pane = getContentPane();
      pane.setLayout(new BorderLayout());
      pane.add(spSplitter, BorderLayout.CENTER);
      pane.add(statusBar, BorderLayout.SOUTH);
    } finally {
      hideProgress();
    }

    updateLauncher();
    setSize(prefs.getInt(WINDOW_SIZEX, 930), prefs.getInt(WINDOW_SIZEY, 700));
    int centerX = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() - getSize().width >> 1;
    int centerY = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - getSize().height >> 1;
    setLocation(prefs.getInt(WINDOW_POSX, centerX), prefs.getInt(WINDOW_POSY, centerY));
    setVisible(true);
    setExtendedState(prefs.getInt(WINDOW_STATE, NORMAL));

    // XXX: Workaround to trigger standard window closing callback on OSX when using command-Q
    if (Platform.IS_MACOS) {
      enableOSXQuitStrategy();
    }

    tableColumnWidth[0] = Math.max(15, prefs.getInt(TABLE_WIDTH_ATTR, 300));
    tableColumnWidth[1] = 0;
    tableColumnWidth[2] = Math.max(15, prefs.getInt(TABLE_WIDTH_OFS, 100));
    tableColumnWidth[3] = Math.max(15, prefs.getInt(TABLE_WIDTH_SIZE, 75));
    tablePanelHeight = Math.max(50, prefs.getInt(TABLE_PANEL_HEIGHT, 250));

    // enabling file drag and drop for whole window
    new DropTarget(getRootPane(), new FileDropTargetListener());

    // Checking for updates
    if (options.isUpdateEnabled() &&
        Updater.getInstance().isAutoUpdateCheckEnabled() &&
        Updater.getInstance().hasAutoUpdateCheckDateExpired()) {
      // storing last check date for future reference
      Updater.getInstance().setAutoUpdateCheckDate(null);
      // running check in background with as little as possible interference with user interactions
      new Thread(() -> {
        try {
          UpdateInfo info = Updater.getInstance().loadUpdateInfo();
          if (info != null) {
            if (Updater.isNewRelease(info.getRelease(), true)) {
              UpdateCheck.showDialog(NearInfinity.getInstance(), info);
            }
          }
        } catch (Exception e) {
          System.out.println("Failed to check for updates: " + e.getMessage());
        }
      }).start();
    }

    SwingUtilities.invokeLater(() -> tree.requestFocusInWindow());
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getActionCommand().equals("Open")) {
      blocker.setBlocked(true);
      Profile.Game oldGame = Profile.getGame();
      String oldFile = Profile.getChitinKey().toString();
      if (reloadFactory(false)) {
        if (!removeViewable()) {
          blocker.setBlocked(false);
          return;
        }
        ChildFrame.closeWindows();
        ResourceTreeModel treemodel = ResourceFactory.getResourceTreeModel();
        updateWindowTitle();
        final String msg = String.format(STATUSBAR_TEXT_FMT, Profile.getProperty(Profile.Key.GET_GAME_TITLE),
                                         Profile.getGameRoot(), treemodel.size());
        statusBar.setMessage(msg);
        BrowserMenuBar.getInstance().gameLoaded(oldGame, oldFile);
        tree.setModel(treemodel);
        containerpanel.removeAll();
        containerpanel.add(createJavaInfoPanel(), BorderLayout.CENTER);
        containerpanel.revalidate();
        containerpanel.repaint();
      }
      blocker.setBlocked(false);
    } else if (event.getActionCommand().equals("Exit")) {
      quit();
    } else if (event.getActionCommand().equals("GameIni")) {
      editGameIni(this);
    } else if (event.getActionCommand().equals("Refresh")) {
      refreshGame();
    } else if (event.getActionCommand().equals("RefreshTree")) {
      try {
        WindowBlocker.blockWindow(this, true);
        tree.reloadRenderer();
        tree.repaint();
        tree.requestFocusInWindow();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getActionCommand().equals("RefreshView")) {
      // repaint UI controls of current view
      if (getViewable() instanceof AbstractStruct) {
        StructViewer sv = ((AbstractStruct) getViewable()).getViewer();
        if (sv != null) {
          SwingUtilities.updateComponentTreeUI(sv);
        }
      }
      // repaint UI controls of child windows
      ChildFrame.updateWindowGUIs();
    } else if (event.getActionCommand().equals("ChangeLook")) {
      try {
        LookAndFeelInfo info = BrowserMenuBar.getInstance().getLookAndFeel();
        UIManager.setLookAndFeel(info.getClassName());
        SwingUtilities.updateComponentTreeUI(this);
        ChildFrame.updateWindowGUIs();
        tree.reloadRenderer();
        tree.repaint();
        JOptionPane.showMessageDialog(this, "It might be necessary to restart Near Infinity\n" + "to completely change look and feel.");
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else if (event.getActionCommand().equals("Collapse")) {
      try {
        WindowBlocker.blockWindow(this, true);
        tree.collapseSelected();
        tree.requestFocusInWindow();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getActionCommand().equals("Expand")) {
      try {
        WindowBlocker.blockWindow(this, true);
        tree.expandSelected();
        tree.requestFocusInWindow();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getActionCommand().equals("CollapseAll")) {
      try {
        WindowBlocker.blockWindow(this, true);
        tree.collapseAll();
        tree.requestFocusInWindow();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getActionCommand().equals("ExpandAll")) {
      try {
        WindowBlocker.blockWindow(this, true);
        tree.expandAll();
        tree.requestFocusInWindow();
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getSource() == btnLaunchGame) {
      // Path launchPath = null;
      DataMenuItem dmi = null;
      boolean ctrl = (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
      if (ctrl || launchMenu.getComponentCount() == 1) {
        // getting first available binary path
        for (int i = 0, cnt = launchMenu.getComponentCount(); i < cnt; i++) {
          if (launchMenu.getComponent(i) instanceof DataMenuItem) {
            dmi = (DataMenuItem) launchMenu.getComponent(i);
            break;
          }
        }
      }

      if ((launchMenu.getComponentCount() > 1 && ctrl) || launchMenu.getComponentCount() == 1) {
        if (dmi != null) {
          dmi.doClick();
        } else {
          JOptionPane.showMessageDialog(this, "Could not determine game executable.", "Launch game",
                                        JOptionPane.ERROR_MESSAGE);
        }
      } else if (launchMenu.getComponentCount() > 1) {
        launchMenu.show(btnLaunchGame, 0, btnLaunchGame.getHeight());
      }
    } else if (event.getSource() instanceof DataMenuItem
        && ((DataMenuItem) event.getSource()).getParent() == launchMenu) {
      DataMenuItem dmi = (DataMenuItem) event.getSource();
      if (dmi.getData() instanceof Path) {
        Path path = (Path) dmi.getData();
        if (!launchGameBinary(path)) {
          JOptionPane.showMessageDialog(this, "Game executable could not be launched.", "Launch game",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ViewableContainer ---------------------

  @Override
  public StatusBar getStatusBar() {
    return statusBar;
  }

  @Override
  public Viewable getViewable() {
    return viewable;
  }

  @Override
  public void setViewable(Viewable newViewable) {
    if (newViewable == null || !(newViewable instanceof Resource)) {
      removeViewable();
    } else {
      Resource resource = (Resource) newViewable;
      if (viewable != null) {
        if (resource.getResourceEntry() == ((Resource) viewable).getResourceEntry()) {
          return;
        } else if (viewable instanceof Closeable) {
          try {
            ((Closeable) viewable).close();
          } catch (Exception e) {
            if (viewable instanceof Resource) {
              tree.select(((Resource) viewable).getResourceEntry());
            }
            return;
          }
        }
      }
      viewable = newViewable;
      tree.select(resource.getResourceEntry());
      statusBar.setMessage(resource.getResourceEntry().getActualPath().toString());
      containerpanel.removeAll();
      containerpanel.add(viewable.makeViewer(this), BorderLayout.CENTER);
      containerpanel.revalidate();
      containerpanel.repaint();
      toFront();
    }
  }

  // --------------------- End Interface ViewableContainer ---------------------

  public ResourceTree getResourceTree() {
    return tree;
  }

  public void openGame(Path keyFile) {
    blocker.setBlocked(true);
    try {
      Profile.Game oldGame = Profile.getGame();
      Path oldKeyFile = Profile.getChitinKey();
      ChildFrame.closeWindows();
      clearCache(false);
      BaseOpcode.reset();
      Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));

      // making sure vital game resources are accessible
      Path tlkPath = Profile.getProperty(Profile.Key.GET_GAME_DIALOG_FILE);
      try {
        checkFileAccess(tlkPath);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(NearInfinity.this,
                                      String.format("The file \"%s\" of the game \"%s\"\nis locked by another process. "
                                          + "Reverting to the previous game.", tlkPath.getFileName(),
                                                    Profile.getProperty(Profile.Key.GET_GAME_TITLE)),
                                      "Near Infinity Error", JOptionPane.ERROR_MESSAGE);
        openGame(oldKeyFile);
        return;
      }

      CreMapCache.reset();
      removeViewable();
      ResourceTreeModel treemodel = ResourceFactory.getResourceTreeModel();
      updateWindowTitle();
      updateLauncher();
      final String msg = String.format(STATUSBAR_TEXT_FMT, Profile.getProperty(Profile.Key.GET_GAME_TITLE),
                                       Profile.getGameRoot(), treemodel.size());
      statusBar.setMessage(msg);
      BrowserMenuBar.getInstance().gameLoaded(oldGame, oldKeyFile.toString());
      tree.setModel(treemodel);
      containerpanel.removeAll();
      containerpanel.add(createJavaInfoPanel(), BorderLayout.CENTER);
      containerpanel.revalidate();
      containerpanel.repaint();
    } finally {
      blocker.setBlocked(false);
    }
  }

  public boolean removeViewable() {
    if (viewable != null && viewable instanceof Closeable) {
      try {
        ((Closeable) viewable).close();
      } catch (Exception e) {
        return false;
      }
    }
    viewable = null;
    tree.select(null);
    containerpanel.removeAll();
    containerpanel.add(createJavaInfoPanel(), BorderLayout.CENTER);
    containerpanel.revalidate();
    containerpanel.repaint();
    return true;
  }

  public void showResourceEntry(ResourceEntry resourceEntry) {
    tree.select(resourceEntry);
  }

  public void quit() {
    if (removeViewable()) {
      // FileWatcher.getInstance().stop();
      ChildFrame.closeWindows();
      storePreferences();
      clearCache(false);
      System.exit(0);
    }
  }

  // Re-initializes currently selected game
  public void refreshGame() {
    try {
      blocker.setBlocked(true);
      reloadFactory(true);
      if (removeViewable()) {
        ChildFrame.closeWindows();
        ResourceTreeModel treemodel = ResourceFactory.getResourceTreeModel();
        updateWindowTitle();
        updateLauncher();
        final String msg = String.format(STATUSBAR_TEXT_FMT, Profile.getProperty(Profile.Key.GET_GAME_TITLE),
            Profile.getGameRoot(), treemodel.size());
        statusBar.setMessage(msg);
        statusBar.invalidate();
        BrowserMenuBar.getInstance().gameLoaded(null, null);
        tree.setModel(treemodel);
        containerpanel.removeAll();
        containerpanel.add(createJavaInfoPanel(), BorderLayout.CENTER);
        containerpanel.revalidate();
        containerpanel.repaint();
      }
    } finally {
      blocker.setBlocked(false);
    }
  }

  // Set/Reset main window title
  public void updateWindowTitle() {
    String title = Profile.getProperty(Profile.Key.GET_GAME_TITLE);
    String desc = Profile.getProperty(Profile.Key.GET_GAME_DESC);
    if (desc != null && !desc.isEmpty()) {
      setTitle(String.format("Near Infinity - %s (%s)", title, desc));
    } else {
      setTitle(String.format("Near Infinity - %s", title));
    }
  }

  // Opens game's ini file in text editor
  public boolean editGameIni(Component parent) {
    boolean retVal = false;
    Path iniFile = Profile.getProperty(Profile.Key.GET_GAME_INI_FILE);
    try {
      if (iniFile != null && FileEx.create(iniFile).isFile()) {
        new ViewFrame(parent, new PlainTextResource(new FileResourceEntry(iniFile)));
      } else {
        throw new Exception();
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(parent, "Cannot open INI file.", "Error", JOptionPane.ERROR_MESSAGE);
    }
    return retVal;
  }

  /**
   * Returns the current column width for tables of structured resources.
   *
   * @param index The column index (0 = Attribute, 1 = Value and, optionally, 2 = Offset, 3 = Size)
   */
  public int getTableColumnWidth(int index) {
    index = Math.min(Math.max(0, index), tableColumnWidth.length - 1);
    if (tableColumnWidth[index] < 0) {
      switch (index) {
        case 0: // "Attribute" column
          tableColumnWidth[index] = 300;
          break;
        case 1: // "Value" column (calculated dynamically)
          tableColumnWidth[index] = 0;
          break;
        case 2: // optional "Offset" column
          tableColumnWidth[index] = 100;
          break;
        case 3: // optional "Size" column
          tableColumnWidth[index] = 75;
          break;
      }
    }
    return tableColumnWidth[index];
  }

  /**
   * Updates the current default column width for tables of structured resources.
   *
   * @param index    The column index (0 = Attribute, 1 = Value and, optionally, 2 = Offset, 3 = Size)
   * @param newValue New width in pixels for the specified column
   * @return Old column width
   */
  public int updateTableColumnWidth(int index, int newValue) {
    index = Math.min(Math.max(0, index), tableColumnWidth.length - 1);
    int retVal = tableColumnWidth[index];
    tableColumnWidth[index] = Math.max(15, newValue);
    return retVal;
  }

  /** Returns the current height of the panel below the table of structured resources. */
  public int getTablePanelHeight() {
    return tablePanelHeight;
  }

  /**
   * Updates the height of the panel below the table of structured resources.
   */
  public int updateTablePanelHeight(int newValue) {
    newValue = Math.max(50, newValue);
    int retVal = tablePanelHeight;
    tablePanelHeight = newValue;
    return retVal;
  }

  /**
   * Returns whether global UI Scaling is overridden by the application.
   */
  public boolean isUiScalingEnabled() {
    return uiScalingEnabled;
  }

  /**
   * Returns the currently selected UI Scaling factor, in percent.
   */
  public int getUiScalingFactor() {
    return uiScalingFactor;
  }

  /**
   * Returns the currently selected global font size relative to UI defaults.
   */
  public int getGlobalFontSize() {
    return globalFontSize;
  }

  /** Updates the launcher button configuration. */
  public void updateLauncher() {
    SwingUtilities.invokeLater(() -> {
      // cleaning up old configuration
      for (int idx = launchMenu.getComponentCount() - 1; idx >= 0; idx--) {
        Component c = launchMenu.getComponent(idx);
        if (c instanceof JMenuItem) {
          ((JMenuItem) c).removeActionListener(NearInfinity.this);
        }
      }
      launchMenu.removeAll();

      // setting up new configuration
      BrowserMenuBar.Bookmark bookmark = BrowserMenuBar.getInstance().getBookmarkOf(Profile.getChitinKey());
      List<Path> binPaths = null;
      if (bookmark != null) {
        List<String> list = bookmark.getBinaryPaths(Platform.getPlatform());
        if (list != null && !list.isEmpty()) {
          binPaths = new ArrayList<>();
          for (final String name : list) {
            Path path1 = null;
            if (name.startsWith("/")) {
              path1 = FileManager.resolveExisting(name);
              if (path1 == null) {
                path1 = FileManager.resolveExisting(Profile.getGameRoot().toString(), name);
              }
            } else {
              path1 = FileManager.resolveExisting(Profile.getGameRoot().toString(), name);
            }
            if (path1 != null) {
              binPaths.add(path1);
            }
          }
        }
      }
      if (binPaths == null || binPaths.isEmpty()) {
        binPaths = Profile.getGameBinaryPaths();
      }
      if (binPaths != null && binPaths.isEmpty()) {
        binPaths = null;
      }

      // updating launch controls
      if (binPaths != null) {
        for (final Path path2 : binPaths) {
          DataMenuItem dmi = new DataMenuItem(path2.toString());
          dmi.setData(path2);
          dmi.addActionListener(NearInfinity.this);
          launchMenu.add(dmi);
        }
      }
      boolean isEnabled = (binPaths != null) && BrowserMenuBar.getInstance().getLauncherEnabled();
      btnLaunchGame.setEnabled(isEnabled);
      if (binPaths == null) {
        btnLaunchGame.setIcon(Icons.ICON_LAUNCH_24.getIcon());
        btnLaunchGame.setToolTipText("Launch game");
      } else if (binPaths.size() == 1) {
        btnLaunchGame.setIcon(Icons.ICON_LAUNCH_24.getIcon());
        btnLaunchGame.setToolTipText("Launch " + binPaths.get(0).toString());
      } else {
        String ctrlName = (Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() == Event.CTRL_MASK) ? "Ctrl"
            : "Command";
        btnLaunchGame.setIcon(Icons.ICON_LAUNCH_PLUS_24.getIcon());
        btnLaunchGame.setToolTipText("Launch game (launch directly with " + ctrlName + "+Click)");
      }
    });
  }

  /**
   * Returns the UI scaling factor (in percent) if overridden by the app. Returns 0 if system-wide UI scale factor
   * is used.
   */
  private static int getUiScalingOption() {
    int retVal = 0;
    final Preferences prefs = Preferences.userNodeForPackage(NearInfinity.class);
    if (prefs.getBoolean(APP_UI_SCALE_ENABLED, false)) {
      retVal = Math.max(50, Math.min(400, prefs.getInt(APP_UI_SCALE_FACTOR, 100)));
    }
    return retVal;
  }

  private static boolean reloadFactory(boolean refreshOnly) {
    boolean retVal = false;
    clearCache(refreshOnly);
    Path keyFile = refreshOnly ? Profile.getChitinKey() : findKeyfile();
    if (keyFile != null) {
      retVal = Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
      if (retVal) {
        CreMapCache.reset();
      }
    }
    return retVal;
  }

  // Central method for clearing cached data
  private static void clearCache(boolean refreshOnly) {
    if (ResourceFactory.getKeyfile() != null) {
      ResourceFactory.getKeyfile().closeBIFFFiles();
    }
    if (!refreshOnly) {
      CharsetDetector.clearCache();
    }
    DlcManager.close();
    FileManager.reset();
    IdsMapCache.clearCache();
    IniMapCache.clearCache();
    Table2daCache.clearCache();
    CreMapCache.clearCache();
    BaseOpcode.reset();
//    SearchFrame.clearCache();
    StringTable.resetAll();
    ProRef.clearCache();
    Signatures.clearCache();
    ColorConvert.clearCache();
    SpriteUtils.clearCache();
    ItemInfo.clearCache();
    AreResource.clearCache();
    Song2daBitmap.resetSonglist();
    SpellProtType.resetTypeTable();
    Summon2daBitmap.resetSummonTable();
  }

  private static void showProgress(String msg, int max) {
    if (getInstance() != null && getInstance().pmProgress == null) {
      getInstance().pmProgress = new ProgressMonitor(null, msg, "   ", 0, max);
      getInstance().pmProgress.setMillisToDecideToPopup(0);
      getInstance().pmProgress.setMillisToPopup(0);
      getInstance().progressIndex = 0;
      getInstance().pmProgress.setProgress(++getInstance().progressIndex);
    }
  }

  private static void hideProgress() {
    if (getInstance() != null && getInstance().pmProgress != null) {
      getInstance().pmProgress.close();
      getInstance().pmProgress = null;
    }
  }

  private static void resizeUIFont(int percent) {
    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof FontUIResource) {
        FontUIResource fr = (FontUIResource) value;
        Font f = Misc.getScaledFont(fr, percent);
        UIManager.put(key, new FontUIResource(f));
      }
    }
    CONSOLE_TEXT.setFont(Misc.getScaledFont(CONSOLE_TEXT.getFont()));
  }

  private void storePreferences() {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    // preserve non-maximized size and position of the window if possible
    if ((getExtendedState() & Frame.MAXIMIZED_HORIZ) == 0) {
      prefs.putInt(WINDOW_SIZEX, (int) getSize().getWidth());
      prefs.putInt(WINDOW_POSX, (int) getLocation().getX());
    }
    if ((getExtendedState() & Frame.MAXIMIZED_VERT) == 0) {
      prefs.putInt(WINDOW_SIZEY, (int) getSize().getHeight());
      prefs.putInt(WINDOW_POSY, (int) getLocation().getY());
    }
    prefs.putInt(WINDOW_STATE, getExtendedState());
    prefs.putInt(WINDOW_SPLITTER, spSplitter.getDividerLocation());
    prefs.put(LAST_GAMEDIR, Profile.getGameRoot().toString());
    prefs.putInt(TABLE_WIDTH_ATTR, getTableColumnWidth(0));
    prefs.putInt(TABLE_WIDTH_OFS, getTableColumnWidth(2));
    prefs.putInt(TABLE_WIDTH_SIZE, getTableColumnWidth(3));
    prefs.putInt(TABLE_PANEL_HEIGHT, getTablePanelHeight());
    prefs.putBoolean(APP_UI_SCALE_ENABLED, BrowserMenuBar.getInstance().isUiScalingEnabled());
    prefs.putInt(APP_UI_SCALE_FACTOR, BrowserMenuBar.getInstance().getUiScalingFactor());
    prefs.putInt(OPTION_GLOBAL_FONT_SIZE, BrowserMenuBar.getInstance().getGlobalFontSize());
    BrowserMenuBar.getInstance().storePreferences();
    Updater.getInstance().saveUpdateSettings();
  }

  private void setAppIcon() {
    List<Image> list = new ArrayList<>();
    for (int i = 4; true; i++) {
      final Image icon = Icons.getImage(null, String.format("App%d.png", 1 << i));
      if (icon != null) {
        list.add(icon);
      } else {
        break;
      }
    }
    setIconImages(list);
  }

  // Migrate preferences from sourceNode to the currently used prefs node if needed.
  // Returns true if content of sourceNode has been cloned into the current node.
  private boolean migratePreferences(String sourceNode, Preferences curPrefs, boolean showError) {
    boolean retVal = false;
    if (sourceNode != null && !sourceNode.isEmpty() && curPrefs != null) {
      Preferences prefsOld = null;
      boolean isPrefsEmpty = false;
      try {
        isPrefsEmpty = (curPrefs.keys().length == 0);
        sourceNode = sourceNode.trim();
        if (Preferences.userRoot().nodeExists(sourceNode)) {
          prefsOld = Preferences.userRoot().node(sourceNode);
        }
      } catch (Exception e) {
        prefsOld = null;
        e.printStackTrace();
      }
      if (isPrefsEmpty && prefsOld != null && !prefsOld.equals(curPrefs)) {
        try {
          clonePrefsNode(prefsOld, curPrefs);
          retVal = true;
        } catch (Exception e) {
          retVal = false;
          try {
            curPrefs.clear();
          } catch (BackingStoreException bse) {
          }
          e.printStackTrace();
          if (showError) {
            JOptionPane.showMessageDialog(this, "Error migrating old Near Infinity settings. Using defaults.", "Error",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    }
    return retVal;
  }

  // Duplicates content from prefsOld to prefsNew recursively
  private void clonePrefsNode(Preferences prefsOld, Preferences prefsNew) throws Exception {
    if (prefsOld != null && prefsNew != null && !prefsOld.equals(prefsNew)) {
      // cloning keys
      String[] keyNames = prefsOld.keys();
      if (keyNames != null) {
        for (String keyName : keyNames) {
          String value = prefsOld.get(keyName, null);
          if (value != null) {
            prefsNew.put(keyName, value);
          }
        }
      }

      // cloning child nodes
      String[] childNames = prefsOld.childrenNames();
      if (childNames != null) {
        for (String childName : childNames) {
          clonePrefsNode(prefsOld.node(childName), prefsNew.node(childName));
        }
      }
    }
  }

  // Enables command-Q on OSX to trigger the window closing callback
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void enableOSXQuitStrategy() {
    // Doesn't work anymore on JRE 9 and higher:
    // Java 9 introduced streamlined access to macOS-specific features,
    //  e.g. menu bar integration, quit strategy, about dialog, ...
    // TODO: Reimplement macOS-specific functionality if/when NI switches to Java 9+ compatibility
    try {
      Class application = Class.forName("com.apple.eawt.Application");
      Method getApplication = application.getMethod("getApplication");
      Object instance = getApplication.invoke(application);
      Class strategy = Class.forName("com.apple.eawt.QuitStrategy");
      Enum closeAllWindows = Enum.valueOf(strategy, "CLOSE_ALL_WINDOWS");
      Method method = application.getMethod("setQuitStrategy", strategy);
      method.invoke(instance, closeAllWindows);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  // Executes the specified path
  private boolean launchGameBinary(Path binPath) {
    boolean retVal = false;
    if (binPath != null && Files.exists(binPath)) {
      try {
        if (Platform.IS_MACOS && binPath.toString().toLowerCase(Locale.ENGLISH).endsWith(".app")) {
          // This method may be required for launching Mac App Bundles
          LauncherUtils.open(binPath);
        } else {
          ProcessBuilder pb = new ProcessBuilder(binPath.toString());
          pb.directory(binPath.getParent().toFile());
          pb.inheritIO();
          pb.start();
        }
        retVal = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Checks read access of the specified file.
   *
   * @param path The file path to check.
   * @throws IOException if specified path could not be opened for reading.
   */
  private void checkFileAccess(Path path) throws IOException {
    if (path != null) {
      try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
      }
    }
  }

  /**
   * Shows Java Runtime information when there are no components attached to the main view.
   */
  private JPanel createJavaInfoPanel() {
    if (!BrowserMenuBar.getInstance().showSysInfo()) {
      return new JPanel();
    }

    final List<Couple<String, String>> entries = new ArrayList<>();
    entries.add(new Couple<String, String>("Near Infinity", BrowserMenuBar.VERSION));
    entries.add(new Couple<String, String>("Java Runtime", System.getProperty("java.runtime.name")));

    String s1 = System.getProperty("java.version", "n/a");
    String s2 = System.getProperty("java.version.date", "");
    String value = s2.isEmpty() ? s1 : String.format("%s (%s)", s1, s2);
    entries.add(new Couple<String, String>("Java Version", value));

    value = System.getProperty("java.vm.name", "n/a") + " (" + System.getProperty("java.vm.version", "n/a");
    s1 = System.getProperty("java.vm.info", "");
    if (!s1.isEmpty()) {
      value += ", " + s1;
    }
    value += ")";
    entries.add(new Couple<String, String>("Java VM", value));

    entries.add(new Couple<String, String>("Java VM Architecture", System.getProperty("os.arch", "n/a")));

    long memoryMax = Runtime.getRuntime().maxMemory();
    if (memoryMax != Long.MAX_VALUE) {
      memoryMax /= 1024L * 1024L;
      entries.add(new Couple<String, String>("Available Memory", String.format("%d MB", memoryMax)));
    } else {
      entries.add(new Couple<String, String>("Available Memory", "n/a"));
    }

    JPanel infoPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    final JLabel titleLabel = new JLabel("System Information");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() * 1.25f));

    int row = 0;
    c = ViewerUtil.setGBC(c, 0, row, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 4, 0), 0, 0);
    infoPanel.add(titleLabel, c);
    row++;

    for (final Couple<String, String> entry : entries) {
      final JLabel keyLabel = new JLabel(entry.getValue0());
      c = ViewerUtil.setGBC(c, 0, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(8, 0, 0, 0), 0, 0);
      infoPanel.add(keyLabel, c);

      final JLabel valueLabel = new JLabel(entry.getValue1());
      c = ViewerUtil.setGBC(c, 1, row, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
          new Insets(8, 16, 0, 0), 0, 0);
      infoPanel.add(valueLabel, c);

      row++;
    }

    if (memoryMax < 480) {
      String message = "<html><center>Your Java memory settings may not be sufficient to use all features of Near Infinity.<br>";
      if (System.getProperty("os.arch").contains("64")) {
        message += "Please consider running Near Infinity with improved memory settings.";
      } else {
        message += "Please consider upgrading your Java Runtime Environment to 64-bit<br>or run Near Infinity with improved memory settings.";
      }
      message += "</center></html>";
      final JLabel infoLabel = new JLabel(message);
      infoLabel.setForeground(new Color(0x800000));

      c = ViewerUtil.setGBC(c, 0, row, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(16, 0, 0, 0), 0, 0);
      infoPanel.add(infoLabel, c);
      row++;
    }

    return infoPanel;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class Options {
    private final Path gameOverride;
    private final Profile.Game forcedGame;
    private final boolean enableUpdate;
    private final boolean showLaunchGame;

    public Options(Path gameOverride, Profile.Game forcedGame, boolean enableUpdate, boolean showLaunchGame) {
      this.gameOverride = gameOverride;
      this.forcedGame = forcedGame;
      this.enableUpdate = enableUpdate;
      this.showLaunchGame = showLaunchGame;
    }

    public boolean isGameOverride() {
      return gameOverride != null;
    }

    public Path getGameOverride() {
      return gameOverride;
    }

    @SuppressWarnings("unused")
    public boolean isForcedGame() {
      return forcedGame != null;
    }

    public Profile.Game getForcedGame() {
      return forcedGame;
    }

    public boolean isUpdateEnabled() {
      return enableUpdate;
    }

    public boolean isLaunchGameVisible() {
      return showLaunchGame;
    }
  }

  private static final class ConsoleStream extends PrintStream {
    private final JTextArea text;

    private ConsoleStream(OutputStream out, JTextArea text) {
      super(out);
      this.text = text;
    }

    @Override
    public void write(byte buf[], int off, int len) {
      super.write(buf, off, len);
      try {
        text.append(new String(buf, off, len));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private class FileDropTargetListener implements DropTargetListener, Runnable {
    private List<File> files;

    private FileDropTargetListener() {
    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent event) {
      if (event.isLocalTransfer() || !event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        event.rejectDrop();
        return;
      }
      try {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        files = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
      } catch (Exception e) {
        e.printStackTrace();
        event.dropComplete(false);
      }
      event.dropComplete(true);
      if (files != null && files.size() == 1) {
        Path path = files.get(0).toPath();
        if (path != null && FileEx.create(path).isFile()
            && path.getFileName().toString().toUpperCase(Locale.ENGLISH).endsWith(".KEY")) {
          Path curFile = Profile.getChitinKey();
          if (!path.equals(curFile)) {
            int ret = JOptionPane.showConfirmDialog(NearInfinity.getInstance(), "Open game \"" + path + "\"?",
                "Open game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ret == JOptionPane.YES_OPTION) {
              NearInfinity.getInstance().openGame(path);
            }
          }
          return;
        }
      }
      new Thread(this).start();
    }

    @Override
    public void run() {
      if (files != null) {
        files.forEach(file -> {
          Path path = file.toPath();
          if (FileEx.create(path).isFile()) {
            OpenFileFrame.openExternalFile(NearInfinity.getInstance(), path);
          }
        });
      }
    }
  }
}
