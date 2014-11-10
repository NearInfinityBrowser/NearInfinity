// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity;

import infinity.gui.BrowserMenuBar;
import infinity.gui.ChildFrame;
import infinity.gui.InfinityTextArea;
import infinity.gui.ResourceTree;
import infinity.gui.StatusBar;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.EffectFactory;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.Viewable;
import infinity.resource.ViewableContainer;
import infinity.resource.bcs.Compiler;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeModel;
import infinity.search.SearchFrame;
import infinity.util.IdsMapCache;
import infinity.util.StringResource;
import infinity.util.io.FileLookup;
import infinity.util.io.FileNI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Image;
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
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
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
  private static final String KEYFILENAME = "chitin.key";
  private static final String WINDOW_SIZEX = "WindowSizeX";
  private static final String WINDOW_SIZEY = "WindowSizeY";
  private static final String WINDOW_POSX = "WindowPosX";
  private static final String WINDOW_POSY = "WindowPosY";
  private static final String WINDOW_STATE = "WindowState";
  private static final String WINDOW_SPLITTER = "WindowSplitter";
  private static final String LAST_GAMEDIR = "LastGameDir";

  private static NearInfinity browser;

  private final JPanel containerpanel;
  private final JSplitPane spSplitter;
  private final ResourceTree tree;
  private final StatusBar statusBar;
  private final WindowBlocker blocker = new WindowBlocker(this);
  private Viewable viewable;


  public static boolean isDebug()
  {
    return DEBUG;
  }

  private static File findKeyfile()
  {
    JFileChooser chooser;
    if (ResourceFactory.getRootDir() == null)
      chooser = new JFileChooser(new FileNI("."));
    else
      chooser = new JFileChooser(ResourceFactory.getRootDir());
    chooser.setDialogTitle("Open game: Locate keyfile");
    chooser.setFileFilter(new FileFilter()
    {
      @Override
      public boolean accept(File pathname)
      {
        return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".key");
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

  private static boolean reloadFactory(boolean refreshonly)
  {
    FileLookup.getInstance().clearCache();
    IdsMapCache.clearCache();
    SearchFrame.clearCache();
    StringResource.close();
    Compiler.restartCompiler();
    if (refreshonly)
      try {
        ResourceFactory.getInstance().loadResources();
      } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "No Infinity Engine game found", "Error",
                                      JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    else {
      File newkeyfile = findKeyfile();
      if (newkeyfile == null)
        return false;
      else {
        EffectFactory.init();
        new ResourceFactory(newkeyfile);
      }
    }
    return true;
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
    browser = new NearInfinity();
  }

  private NearInfinity()
  {
    super("Near Infinity");
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setAppIcon();
    Preferences prefs = Preferences.userNodeForPackage(getClass());

    BrowserMenuBar menuBar = new BrowserMenuBar(this);
    setJMenuBar(menuBar);

    String lastDir = prefs.get(LAST_GAMEDIR, null);
    if (new FileNI(KEYFILENAME).exists()) {
      new ResourceFactory(new FileNI(KEYFILENAME));
    } else if (lastDir != null && new FileNI(lastDir, KEYFILENAME).exists()) {
      new ResourceFactory(new FileNI(lastDir, KEYFILENAME));
    } else {
      File key = findKeyfile();
      if (key == null)
        System.exit(10);
      new ResourceFactory(key);
    }

    menuBar.gameLoaded(-1, null);

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
    ResourceTreeModel treemodel = ResourceFactory.getInstance().getResources();
    setTitle("Near Infinity - " + ResourceFactory.getGameName(ResourceFactory.getGameID()));
    statusBar.setMessage(
            "Welcome to Near Infinity! - " +
            ResourceFactory.getGameName(ResourceFactory.getGameID()) +
            " @ "
            + ResourceFactory.getRootDir().toString() + " - " + treemodel.size() +
            " files available");
    tree = new ResourceTree(treemodel);
    tree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    containerpanel = new JPanel(new BorderLayout());
    spSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, containerpanel);
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
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getActionCommand().equals("Open")) {
      blocker.setBlocked(true);
      int oldGame = ResourceFactory.getGameID();
      String oldFile = ResourceFactory.getKeyfile().toString();
      if (reloadFactory(false)) {
        if (!removeViewable()) {
          blocker.setBlocked(false);
          return;
        }
        ChildFrame.closeWindows();
        ResourceTreeModel treemodel = ResourceFactory.getInstance().getResources();
        setTitle("Near Infinity - " + ResourceFactory.getGameName(ResourceFactory.getGameID()));
        statusBar.setMessage(
                "Welcome to Near Infinity! - " +
                ResourceFactory.getGameName(ResourceFactory.getGameID()) +
                " @ " + ResourceFactory.getRootDir().toString() + " - " +
                treemodel.size() + " files available");
        BrowserMenuBar.getInstance().gameLoaded(oldGame, oldFile);
        tree.setModel(treemodel);
        containerpanel.removeAll();
        containerpanel.revalidate();
        containerpanel.repaint();
      }
      blocker.setBlocked(false);
    }
    else if (event.getActionCommand().equals("Exit")) {
      if (removeViewable()) {
        ChildFrame.closeWindows();
        storePreferences();
        System.exit(0);
      }
    }
    else if (event.getActionCommand().equals("Refresh")) {
      blocker.setBlocked(true);
      reloadFactory(true);
      if (removeViewable()) {
        ChildFrame.closeWindows();
        ResourceTreeModel treemodel = ResourceFactory.getInstance().getResources();
        statusBar.setMessage("Welcome to Near Infinity! - " +
                             ResourceFactory.getGameName(ResourceFactory.getGameID()) +
                             " @ " + ResourceFactory.getRootDir().toString() +
                             " - " + treemodel.size() + " files available");
        tree.setModel(treemodel);
        containerpanel.removeAll();
        containerpanel.revalidate();
        containerpanel.repaint();
      }
      blocker.setBlocked(false);
    }
    else if (event.getActionCommand().equals("ChangeLook")) {
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

  public void openGame(File keyfile)
  {
    blocker.setBlocked(true);
    int oldGame = ResourceFactory.getGameID();
    String oldFile = ResourceFactory.getKeyfile().toString();
    IdsMapCache.clearCache();
    SearchFrame.clearCache();
    StringResource.close();
    Compiler.restartCompiler();
    EffectFactory.init();
    new ResourceFactory(keyfile);
    removeViewable();
    ChildFrame.closeWindows();
    ResourceTreeModel treemodel = ResourceFactory.getInstance().getResources();
    setTitle("Near Infinity - " + ResourceFactory.getGameName(ResourceFactory.getGameID()));
    statusBar.setMessage(
            "Welcome to Near Infinity! - " +
            ResourceFactory.getGameName(ResourceFactory.getGameID()) +
            " @ "
            + ResourceFactory.getRootDir().toString() + " - " + treemodel.size() +
            " files available");
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

  private void storePreferences()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    prefs.putInt(WINDOW_SIZEX, (int)getSize().getWidth());
    prefs.putInt(WINDOW_SIZEY, (int)getSize().getHeight());
    prefs.putInt(WINDOW_POSX, (int)getLocation().getX());
    prefs.putInt(WINDOW_POSY, (int)getLocation().getY());
    prefs.putInt(WINDOW_STATE, getExtendedState());
    prefs.putInt(WINDOW_SPLITTER, spSplitter.getDividerLocation());
    prefs.put(LAST_GAMEDIR, ResourceFactory.getRootDir().toString());
    BrowserMenuBar.getInstance().storePreferences();
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

