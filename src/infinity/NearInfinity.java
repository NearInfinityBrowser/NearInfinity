// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ButtonPopupWindow;
import infinity.gui.ChildFrame;
import infinity.gui.InfinityTextArea;
import infinity.gui.PopupWindowEvent;
import infinity.gui.PopupWindowListener;
import infinity.gui.QuickSearch;
import infinity.gui.ResourceTree;
import infinity.gui.StatusBar;
import infinity.gui.ViewFrame;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.EffectFactory;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.Viewable;
import infinity.resource.ViewableContainer;
import infinity.resource.bcs.Compiler;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeModel;
import infinity.resource.text.PlainTextResource;
import infinity.search.SearchFrame;
import infinity.updater.UpdateCheck;
import infinity.updater.UpdateInfo;
import infinity.updater.Updater;
import infinity.util.IdsMapCache;
import infinity.util.StringResource;
import infinity.util.io.FileLookup;
import infinity.util.io.FileNI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;

public final class NearInfinity extends JFrame implements ActionListener, ViewableContainer
{
  static {
    // XXX: Works around a known bug in Java's Swing layouts when using FocusTraversalPolicy
    // Note: Required for Area Viewer's JTree control; must be set before executing main()
    System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
  }

  private static final int[] JAVA_VERSION = {1, 6};   // the minimum java version supported

  private static final boolean DEBUG = false;    // indicates whether to enable debugging features

  private static final InfinityTextArea consoletext = new InfinityTextArea(true);
  private static final String KEYFILENAME         = "chitin.key";
  private static final String WINDOW_SIZEX        = "WindowSizeX";
  private static final String WINDOW_SIZEY        = "WindowSizeY";
  private static final String WINDOW_POSX         = "WindowPosX";
  private static final String WINDOW_POSY         = "WindowPosY";
  private static final String WINDOW_STATE        = "WindowState";
  private static final String WINDOW_SPLITTER     = "WindowSplitter";
  private static final String LAST_GAMEDIR        = "LastGameDir";
  private static final String TABLE_WIDTH_ATTR    = "TableColWidthAttr";
  private static final String TABLE_WIDTH_OFS     = "TableColWidthOfs";
  private static final String TABLE_PANEL_HEIGHT  = "TablePanelHeight";

  private static final String STATUSBAR_TEXT_FMT = "Welcome to Near Infinity! - %1$s @ %2$s - %3$d files available";

  private static NearInfinity browser;

  private final JPanel containerpanel;
  private final JSplitPane spSplitter;
  private final ResourceTree tree;
  private final StatusBar statusBar;
  private final WindowBlocker blocker = new WindowBlocker(this);
  // stores table column widths for "Attribute", "Value" and "Offset"
  private final int[] tableColumnWidth = { -1, -1, -1 };

  private Viewable viewable;
  private ButtonPopupWindow bpwQuickSearch;
  private int tablePanelHeight;


  public static boolean isDebug()
  {
    return DEBUG;
  }

  private static File findKeyfile()
  {
    JFileChooser chooser;
    if (Profile.getGameRoot() == null) {
      chooser = new JFileChooser(new FileNI("."));
    } else {
      chooser = new JFileChooser(Profile.getGameRoot());
    }
    chooser.setDialogTitle("Open game: Locate keyfile");
    chooser.setFileFilter(new FileFilter()
    {
      @Override
      public boolean accept(File pathname)
      {
        return pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".key");
      }

      @Override
      public String getDescription()
      {
        return "Infinity Keyfile (.KEY)";
      }
    });
    if (chooser.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION)
      return chooser.getSelectedFile();
    return null;
  }

  public static InfinityTextArea getConsoleText()
  {
    return consoletext;
  }

  public static NearInfinity getInstance()
  {
    return browser;
  }

  /** Returns the NearInfinity version. */
  public static String getVersion()
  {
    return BrowserMenuBar.VERSION;
  }

  private static boolean reloadFactory(boolean refreshonly)
  {
    FileLookup.getInstance().clearCache();
    IdsMapCache.clearCache();
    SearchFrame.clearCache();
    StringResource.close();
    Compiler.restartCompiler();
    File keyFile = refreshonly ? Profile.getChitinKey() : findKeyfile();
    if (keyFile != null) {
      EffectFactory.init();
      Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
      return true;
    }
    return false;
  }

  public static void main(String args[])
  {
    String[] javaVersion = System.getProperty("java.specification.version").split("\\.");
    try {
      for (int i = 0; i < Math.min(JAVA_VERSION.length, javaVersion.length); i++) {
        if (Integer.parseInt(javaVersion[i]) < JAVA_VERSION[i]) {
          JOptionPane.showMessageDialog(null,
                                        String.format("Version %1$d.%2$d or newer of Java is required!",
                                                      JAVA_VERSION[0], JAVA_VERSION[1]),
                                        "Error", JOptionPane.ERROR_MESSAGE);
          System.exit(10);
        }
      }
    } catch (Exception e) { // Try starting anyway if the test goes sour
      e.printStackTrace();
    }
    System.setOut(new ConsoleStream(System.out, consoletext));
    System.setErr(new ConsoleStream(System.err, consoletext));

    // Override game folder via application parameter
    File gameOverride = null;
    if (args.length > 0) {
      File f = new FileNI(args[0]);
      if (f.isFile()) {
        f = f.getParentFile();
      }
      if (f.isDirectory()) {
        gameOverride = f;
      }
    }

    new NearInfinity(gameOverride);
  }

  private NearInfinity(File gameOverride)
  {
    super("Near Infinity");
    browser = this;
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setAppIcon();
    Preferences prefs = Preferences.userNodeForPackage(getClass());

    BrowserMenuBar menuBar = new BrowserMenuBar();
    setJMenuBar(menuBar);

    String lastDir = null;
    if (gameOverride != null && gameOverride.isDirectory()) {
      lastDir = gameOverride.toString();
    } else {
      lastDir = prefs.get(LAST_GAMEDIR, null);
    }
    if (new FileNI(KEYFILENAME).isFile()) {
      File keyFile = new FileNI(KEYFILENAME);
      Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
    } else if (lastDir != null && new FileNI(lastDir, KEYFILENAME).isFile()) {
      File keyFile = new FileNI(lastDir, KEYFILENAME);
      Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
    } else {
      File keyFile = findKeyfile();
      if (keyFile == null) {
        System.exit(10);
      }
      Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
    }

    menuBar.gameLoaded(Profile.Game.Unknown, null);

    addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent event)
      {
        if (removeViewable()) {
          storePreferences();
          ChildFrame.closeWindows();
          System.exit(0);
        }
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
    ResourceTreeModel treemodel = ResourceFactory.getResources();
    updateWindowTitle();
    final String msg = String.format(STATUSBAR_TEXT_FMT,
                                     Profile.getProperty(Profile.GET_GAME_TITLE),
                                     Profile.getGameRoot(), treemodel.size());
    statusBar.setMessage(msg);
    tree = new ResourceTree(treemodel);
    tree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    JToolBar toolBar = new JToolBar("Navigation", JToolBar.HORIZONTAL);
    JButton b;
    toolBar.setRollover(true);
    toolBar.setFloatable(false);
    b = new JButton(Icons.getIcon("Expand16.png"));
    b.addActionListener(this);
    b.setActionCommand("Expand");
    b.setToolTipText("Expand selected node");
    b.setMargin(new Insets(4, 4, 4, 4));
    toolBar.add(b);
    b = new JButton(Icons.getIcon("Collapse16.png"));
    b.addActionListener(this);
    b.setActionCommand("Collapse");
    b.setToolTipText("Collapse selected node");
    b.setMargin(new Insets(4, 4, 4, 4));
    toolBar.add(b);
    toolBar.addSeparator(new Dimension(8, 24));
    b = new JButton(Icons.getIcon("ExpandAll24.png"));
    b.addActionListener(this);
    b.setActionCommand("ExpandAll");
    b.setToolTipText("Expand all");
    b.setMargin(new Insets(0, 0, 0, 0));
    toolBar.add(b);
    b = new JButton(Icons.getIcon("CollapseAll24.png"));
    b.addActionListener(this);
    b.setActionCommand("CollapseAll");
    b.setToolTipText("Collapse all");
    b.setMargin(new Insets(0, 0, 0, 0));
    toolBar.add(b);
    toolBar.addSeparator(new Dimension(8, 24));
    bpwQuickSearch = new ButtonPopupWindow(Icons.getIcon("Magnify16.png"));
    bpwQuickSearch.setToolTipText("Find resource");
    bpwQuickSearch.setMargin(new Insets(4, 4, 4, 4));
    toolBar.add(bpwQuickSearch);
    bpwQuickSearch.addPopupWindowListener(new PopupWindowListener() {

      @Override
      public void popupWindowWillBecomeVisible(PopupWindowEvent event)
      {
        // XXX: Working around a visual glitch in QuickSearch's JComboBox popup list
        //      by creating new QuickSearch instances on activation
        bpwQuickSearch.setContent(new QuickSearch(bpwQuickSearch, tree));

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run()
          {
            Component c = bpwQuickSearch.getContent();
            if (c != null) {
              c.requestFocusInWindow();
            }
          }
        });
      }

      @Override
      public void popupWindowWillBecomeInvisible(PopupWindowEvent event)
      {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run()
          {
            bpwQuickSearch.setContent(null);
            tree.requestFocusInWindow();
          }
        });
      }
    });

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(tree, BorderLayout.CENTER);
    leftPanel.add(toolBar, BorderLayout.NORTH);

    containerpanel = new JPanel(new BorderLayout());
    spSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, containerpanel);
    spSplitter.setBorder(BorderFactory.createEmptyBorder());
    spSplitter.setDividerLocation(prefs.getInt(WINDOW_SPLITTER, 200));
    Container pane = getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(spSplitter, BorderLayout.CENTER);
    pane.add(statusBar, BorderLayout.SOUTH);

    setSize(prefs.getInt(WINDOW_SIZEX, 930), prefs.getInt(WINDOW_SIZEY, 700));
    int centerX = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth() - getSize().width >> 1;
    int centerY = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight() - getSize().height >> 1;
    setLocation(prefs.getInt(WINDOW_POSX, centerX), prefs.getInt(WINDOW_POSY, centerY));
    setVisible(true);
    setExtendedState(prefs.getInt(WINDOW_STATE, NORMAL));

    tableColumnWidth[0] = Math.max(15, prefs.getInt(TABLE_WIDTH_ATTR, 300));
    tableColumnWidth[1] = 0;
    tableColumnWidth[2] = Math.max(15, prefs.getInt(TABLE_WIDTH_OFS, 100));
    tablePanelHeight = Math.max(50, prefs.getInt(TABLE_PANEL_HEIGHT, 250));

    // Checking for updates
    if (Updater.getInstance().isAutoUpdateCheckEnabled() &&
        Updater.getInstance().hasAutoUpdateCheckDateExpired()) {
      // storing last check date for future reference
      Updater.getInstance().setAutoUpdateCheckDate(null);
      // running check in background with as little as possible interference with user interactions
      new Thread(new Runnable() {
        @Override
        public void run()
        {
          UpdateInfo info = Updater.getInstance().loadUpdateInfo();
          if (info != null) {
            if (Updater.isNewRelease(info.getRelease(), true)) {
              UpdateCheck.showDialog(NearInfinity.getInstance(), info);
            }
          }
        }
      }).start();
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        tree.requestFocusInWindow();
      }
    });
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
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
        ResourceTreeModel treemodel = ResourceFactory.getResources();
        updateWindowTitle();
        final String msg = String.format(STATUSBAR_TEXT_FMT,
                                         Profile.getProperty(Profile.GET_GAME_TITLE),
                                         Profile.getGameRoot(), treemodel.size());
        statusBar.setMessage(msg);
        BrowserMenuBar.getInstance().gameLoaded(oldGame, oldFile);
        tree.setModel(treemodel);
        containerpanel.removeAll();
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
    } else if (event.getActionCommand().equals("ChangeLook")) {
      try {
        LookAndFeelInfo info = BrowserMenuBar.getInstance().getLookAndFeel();
        UIManager.setLookAndFeel(info.getClassName());
        SwingUtilities.updateComponentTreeUI(this);
        ChildFrame.updateWindowGUIs();
        tree.reloadRenderer();
        tree.repaint();
        JOptionPane.showMessageDialog(this, "It might be necessary to restart Near Infinity\n" +
                                            "to completely change look and feel.");
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
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ViewableContainer ---------------------

  @Override
  public StatusBar getStatusBar()
  {
    return statusBar;
  }

  @Override
  public Viewable getViewable()
  {
    return viewable;
  }

  @Override
  public void setViewable(Viewable newViewable)
  {
    if (newViewable == null || !(newViewable instanceof Resource))
      removeViewable();
    else {
      Resource resource = (Resource)newViewable;
      if (viewable != null) {
        if (resource.getResourceEntry() == ((Resource)viewable).getResourceEntry())
          return;
        else if (viewable instanceof Closeable) {
          try {
            ((Closeable)viewable).close();
          } catch (Exception e) {
            if (viewable instanceof Resource)
              tree.select(((Resource)viewable).getResourceEntry());
            return;
          }
        }
      }
      viewable = newViewable;
      tree.select(resource.getResourceEntry());
      BrowserMenuBar.getInstance().resourceShown(resource);
      statusBar.setMessage(resource.getResourceEntry().getActualFile().toString());
      containerpanel.removeAll();
      containerpanel.add(viewable.makeViewer(this), BorderLayout.CENTER);
      containerpanel.revalidate();
      containerpanel.repaint();
      toFront();
    }
  }

// --------------------- End Interface ViewableContainer ---------------------

  public ResourceTree getResourceTree()
  {
    return tree;
  }

  public void openGame(File keyFile)
  {
    blocker.setBlocked(true);
    Profile.Game oldGame = Profile.getGame();
    String oldFile = Profile.getChitinKey().toString();
    IdsMapCache.clearCache();
    SearchFrame.clearCache();
    StringResource.close();
    Compiler.restartCompiler();
    EffectFactory.init();
    Profile.openGame(keyFile, BrowserMenuBar.getInstance().getBookmarkName(keyFile));
    removeViewable();
    ChildFrame.closeWindows();
    ResourceTreeModel treemodel = ResourceFactory.getResources();
    updateWindowTitle();
    final String msg = String.format(STATUSBAR_TEXT_FMT,
                                     Profile.getProperty(Profile.GET_GAME_TITLE),
                                     Profile.getGameRoot(), treemodel.size());
    statusBar.setMessage(msg);
    BrowserMenuBar.getInstance().gameLoaded(oldGame, oldFile);
    tree.setModel(treemodel);
    containerpanel.removeAll();
    containerpanel.revalidate();
    containerpanel.repaint();
    blocker.setBlocked(false);
  }

  public boolean removeViewable()
  {
    if (viewable != null && viewable instanceof Closeable) {
      try {
        ((Closeable)viewable).close();
      } catch (Exception e) {
        return false;
      }
    }
    viewable = null;
    BrowserMenuBar.getInstance().resourceShown(null);
    tree.select(null);
    containerpanel.removeAll();
    containerpanel.revalidate();
    containerpanel.repaint();
    return true;
  }

  public void showResourceEntry(ResourceEntry resourceEntry)
  {
    tree.select(resourceEntry);
  }

  public void quit()
  {
    if (removeViewable()) {
      ChildFrame.closeWindows();
      storePreferences();
      System.exit(0);
    }
  }

  // Re-initializes currently selected game
  public void refreshGame()
  {
    try {
      blocker.setBlocked(true);
      reloadFactory(true);
      if (removeViewable()) {
        ChildFrame.closeWindows();
        ResourceTreeModel treemodel = ResourceFactory.getResources();
        final String msg = String.format(STATUSBAR_TEXT_FMT,
                                         Profile.getProperty(Profile.GET_GAME_TITLE),
                                         Profile.getGameRoot(), treemodel.size());
        statusBar.setMessage(msg);
        BrowserMenuBar.getInstance().gameLoaded(null, null);
        tree.setModel(treemodel);
        containerpanel.removeAll();
        containerpanel.revalidate();
        containerpanel.repaint();
      }
    } finally {
      blocker.setBlocked(false);
    }
  }

  // Set/Reset main window title
  public void updateWindowTitle()
  {
    String title = (String)Profile.getProperty(Profile.GET_GAME_TITLE);
    String desc = (String)Profile.getProperty(Profile.GET_GAME_DESC);
    if (desc != null && !desc.isEmpty()) {
      setTitle(String.format("Near Infinity - %1$s (%2$s)", title, desc));
    } else {
      setTitle(String.format("Near Infinity - %1$s", title));
    }
  }

  // Opens game's ini file in text editor
  public boolean editGameIni(Component parent)
  {
    boolean retVal = false;
    File iniFile = (File)Profile.getProperty(Profile.GET_GAME_INI_FILE);
    try {
      if (iniFile != null && iniFile.isFile()) {
        new ViewFrame(parent, new PlainTextResource(new FileResourceEntry(iniFile)));
      } else {
        throw new Exception();
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(parent, "Cannot open INI file.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return retVal;
  }

  /**
   * Returns the current column width for tables of structured resources.
   * @param index The column index (0 = Attribute, 1 = Value and, optionally, 2 = Offset)
   */
  public int getTableColumnWidth(int index)
  {
    index = Math.min(Math.max(0, index), 2);
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
      }
    }
    return tableColumnWidth[index];
  }

  /**
   * Updates the current default column width for tables of structured resources.
   * @param index     The column index (0 = Attribute, 1 = Value and, optionally, 2 = Offset)
   * @param newValue  New width in pixels for the specified column
   * @return          Old column width
   */
  public int updateTableColumnWidth(int index, int newValue)
  {
    index = Math.min(Math.max(0, index), 2);
    int retVal = tableColumnWidth[index];
    tableColumnWidth[index] = Math.max(15, newValue);
    return retVal;
  }

  /** Returns the current height of the panel below the table of structured resources.*/
  public int getTablePanelHeight()
  {
    return tablePanelHeight;
  }

  /**
   * Updates the height of the panel below the table of structured resources.
   */
  public int updateTablePanelHeight(int newValue)
  {
    newValue = Math.max(50, newValue);
    int retVal = tablePanelHeight;
    tablePanelHeight = newValue;
    return retVal;
  }

  private void storePreferences()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    // preserve non-maximized size and position of the window if possible
    if ((getExtendedState() & Frame.MAXIMIZED_HORIZ) == 0) {
      prefs.putInt(WINDOW_SIZEX, (int)getSize().getWidth());
      prefs.putInt(WINDOW_POSX, (int)getLocation().getX());
    }
    if ((getExtendedState() & Frame.MAXIMIZED_VERT) == 0) {
      prefs.putInt(WINDOW_SIZEY, (int)getSize().getHeight());
      prefs.putInt(WINDOW_POSY, (int)getLocation().getY());
    }
    prefs.putInt(WINDOW_STATE, getExtendedState());
    prefs.putInt(WINDOW_SPLITTER, spSplitter.getDividerLocation());
    prefs.put(LAST_GAMEDIR, Profile.getGameRoot().toString());
    prefs.putInt(TABLE_WIDTH_ATTR, getTableColumnWidth(0));
    prefs.putInt(TABLE_WIDTH_OFS, getTableColumnWidth(2));
    prefs.putInt(TABLE_PANEL_HEIGHT, getTablePanelHeight());
    BrowserMenuBar.getInstance().storePreferences();
    Updater.getInstance().saveUpdateSettings();
  }

  private void setAppIcon()
  {
    List<Image> list = new ArrayList<Image>();
    for (int i = 4; i < 8; i++) {
      list.add(Icons.getImage(String.format("App%1$d.png", 1 << i)));
    }
    setIconImages(list);
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class ConsoleStream extends PrintStream
  {
    private final JTextArea text;

    private ConsoleStream(OutputStream out, JTextArea text)
    {
      super(out);
      this.text = text;
    }

    @Override
    public void write(byte buf[], int off, int len)
    {
      super.write(buf, off, len);
      try {
        text.append(new String(buf, off, len));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

