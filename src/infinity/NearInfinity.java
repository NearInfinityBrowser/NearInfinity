// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.bcs.Compiler;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeModel;
import infinity.search.SearchFrame;
import infinity.util.IdsMapCache;
import infinity.util.StringResource;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.Preferences;

public final class NearInfinity extends JFrame implements ActionListener, ViewableContainer
{
  private static final JTextArea consoletext = new JTextArea();
  private static NearInfinity browser;
  private static final String KEYFILENAME = "chitin.key";
  private static final String WINDOW_SIZEX = "WindowSizeX";
  private static final String WINDOW_SIZEY = "WindowSizeY";
  private static final String WINDOW_POSX = "WindowPosX";
  private static final String WINDOW_POSY = "WindowPosY";
  private static final String LAST_GAMEDIR = "LastGameDir";
  private final JPanel containerpanel;
  private final ResourceTree tree;
  private final StatusBar statusBar;
  private final WindowBlocker blocker = new WindowBlocker(this);
  private Viewable viewable;

  private static File findKeyfile()
  {
    JFileChooser chooser;
    if (ResourceFactory.getRootDir() == null)
      chooser = new JFileChooser(new File("."));
    else
      chooser = new JFileChooser(ResourceFactory.getRootDir());
    chooser.setDialogTitle("Open game: Locate keyfile");
    chooser.setFileFilter(new FileFilter()
    {
      public boolean accept(File pathname)
      {
        return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".key");
      }

      public String getDescription()
      {
        return "Infinity Keyfile (.KEY)";
      }
    });
    if (chooser.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION)
      return chooser.getSelectedFile();
    return null;
  }

  public static JTextArea getConsoleText()
  {
    return consoletext;
  }

  public static NearInfinity getInstance()
  {
    return browser;
  }

  private static boolean reloadFactory(boolean refreshonly)
  {
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
    String javaVersion = System.getProperty("java.specification.version");
    try {
      if (Integer.parseInt(javaVersion.substring(0, 1)) < 2 &&
          Integer.parseInt(javaVersion.substring(2, 3)) < 5) {
        JOptionPane.showMessageDialog(null, "Version 1.5 or newer of Java is required",
                                      "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(10);
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
    setIconImage(Icons.getIcon("Application16.gif").getImage());
    Preferences prefs = Preferences.userNodeForPackage(getClass());

    BrowserMenuBar menuBar = new BrowserMenuBar(this);
    setJMenuBar(menuBar);

    String lastDir = prefs.get(LAST_GAMEDIR, null);
    if (new File(KEYFILENAME).exists())
      new ResourceFactory(new File(KEYFILENAME));
    else if (lastDir != null && new File(lastDir, KEYFILENAME).exists())
      new ResourceFactory(new File(lastDir, KEYFILENAME));
    else {
      File key = findKeyfile();
      if (key == null)
        System.exit(10);
      new ResourceFactory(key);
    }

    menuBar.gameLoaded(-1, null);

    addWindowListener(new WindowAdapter()
    {
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
      switch (menuBar.getLookAndFeel()) {
        case BrowserMenuBar.LOOKFEEL_JAVA:
          UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
          break;
        case BrowserMenuBar.LOOKFEEL_WINDOWS:
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          break;
        case BrowserMenuBar.LOOKFEEL_MOTIF:
          UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
          break;
        case BrowserMenuBar.LOOKFEEL_PLASTICXP:
          UIManager.setLookAndFeel("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
          break;
      }
      SwingUtilities.updateComponentTreeUI(this);
    } catch (Exception e) {
      e.printStackTrace();
    }

    statusBar = new StatusBar();
    ResourceTreeModel treemodel = ResourceFactory.getInstance().getResources();
    statusBar.setMessage(
            "Welcome to Near Infinity! - " +
            ResourceFactory.getGameName(ResourceFactory.getGameID()) +
            " @ "
            + ResourceFactory.getRootDir().toString() + " - " + treemodel.size() +
            " files available");
    tree = new ResourceTree(treemodel);
    tree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    containerpanel = new JPanel(new BorderLayout());
    containerpanel.setBackground(UIManager.getColor("desktop"));
    JSplitPane splith = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, containerpanel);
    splith.setBorder(BorderFactory.createEmptyBorder());
    splith.setDividerLocation(200);
    Container pane = getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(splith, BorderLayout.CENTER);
    pane.add(statusBar, BorderLayout.SOUTH);

    setSize(prefs.getInt(WINDOW_SIZEX, 930), prefs.getInt(WINDOW_SIZEY, 700));
//    setSize(900, 700);
    int centerX = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth() - getSize().width >> 1;
    int centerY = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight() - getSize().height >> 1;
    setLocation(prefs.getInt(WINDOW_POSX, centerX), prefs.getInt(WINDOW_POSY, centerY));
    setVisible(true);
  }

// --------------------- Begin Interface ActionListener ---------------------

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
        switch (BrowserMenuBar.getInstance().getLookAndFeel()) {
          case BrowserMenuBar.LOOKFEEL_JAVA:
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            break;
          case BrowserMenuBar.LOOKFEEL_WINDOWS:
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            break;
          case BrowserMenuBar.LOOKFEEL_MOTIF:
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
            break;
          case BrowserMenuBar.LOOKFEEL_PLASTICXP:
            UIManager.setLookAndFeel("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
            break;
        }
        SwingUtilities.updateComponentTreeUI(this);
        ChildFrame.updateWindowGUIs();
        tree.reloadRenderer();
        tree.repaint();
        JOptionPane.showMessageDialog(this,
                                      "It might be necessary to restart Near Infinity\nto completely change look and feel.");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ViewableContainer ---------------------

  public StatusBar getStatusBar()
  {
    return statusBar;
  }

  public Viewable getViewable()
  {
    return viewable;
  }

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
    prefs.put(LAST_GAMEDIR, ResourceFactory.getRootDir().toString());
    BrowserMenuBar.getInstance().storePreferences();
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

