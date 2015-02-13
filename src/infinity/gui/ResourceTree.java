// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeFolder;
import infinity.resource.key.ResourceTreeModel;
import infinity.util.io.FileNI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public final class ResourceTree extends JPanel implements TreeSelectionListener, ActionListener
{
  private final JButton bnext = new JButton("Forward", Icons.getIcon("Forward16.gif"));
  private final JButton bprev = new JButton("Back", Icons.getIcon("Back16.gif"));
  private final JTree tree = new JTree();
  private final Stack<ResourceEntry> nextstack = new Stack<ResourceEntry>();
  private final Stack<ResourceEntry> prevstack = new Stack<ResourceEntry>();
  private ResourceEntry prevnextnode, shownresource;
  private boolean showresource = true;

  public ResourceTree(ResourceTreeModel treemodel)
  {
    tree.setCellRenderer(new ResourceTreeRenderer());
    tree.addKeyListener(new TreeKeyListener());
    tree.addMouseListener(new TreeMouseListener());
    tree.setModel(treemodel);
    tree.putClientProperty("JTree.lineStyle", "Angled");
    tree.clearSelection();
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setRootVisible(false);
    tree.addTreeSelectionListener(this);
    tree.setShowsRootHandles(true);

    bnext.addActionListener(this);
    bprev.addActionListener(this);
    bnext.setEnabled(false);
    bprev.setEnabled(false);
    bnext.setHorizontalTextPosition(SwingConstants.LEADING);
    bnext.setMargin(new Insets(3, 1, 3, 1));
    bprev.setMargin(bnext.getMargin());

    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(1, 2));
    panel.add(bprev);
    panel.add(bnext);

    setLayout(new BorderLayout());
    add(new JScrollPane(tree), BorderLayout.CENTER);
    add(panel, BorderLayout.SOUTH);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bprev) {
      nextstack.push(prevnextnode);
      prevnextnode = prevstack.pop();
      bnext.setEnabled(true);
      bprev.setEnabled(!prevstack.empty());
      select(prevnextnode);
    }
    else if (event.getSource() == bnext) {
      prevstack.push(prevnextnode);
      prevnextnode = nextstack.pop();
      bprev.setEnabled(true);
      bnext.setEnabled(!nextstack.empty());
      select(prevnextnode);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface TreeSelectionListener ---------------------

  @Override
  public void valueChanged(TreeSelectionEvent event)
  {
    Object node = tree.getLastSelectedPathComponent();
    if (node == null) {
      tree.clearSelection();
      BrowserMenuBar.getInstance().resourceEntrySelected(null);
    }
    else if (node instanceof ResourceEntry) {
      ResourceEntry entry = (ResourceEntry)node;
      BrowserMenuBar.getInstance().resourceEntrySelected((ResourceEntry)node);
      if (entry != prevnextnode) { // Not result of pressing 'Back' or 'Forward'
        if (prevnextnode != null) {
          prevstack.push(prevnextnode);
          bprev.setEnabled(true);
        }
        nextstack.removeAllElements();
        bnext.setEnabled(false);
        prevnextnode = entry;
      }
      if (showresource) {
        shownresource = entry;
        NearInfinity.getInstance().setViewable(ResourceFactory.getResource(entry));
      }
    }
    else
      BrowserMenuBar.getInstance().resourceEntrySelected(null);
  }

// --------------------- End Interface TreeSelectionListener ---------------------

  @Override
  public boolean requestFocusInWindow()
  {
    return tree.requestFocusInWindow();
  }

  public ResourceEntry getSelected()
  {
    Object node = tree.getLastSelectedPathComponent();
    if (node instanceof ResourceEntry)
      return (ResourceEntry)node;
    return null;
  }

  public void reloadRenderer()
  {
    tree.setCellRenderer(new ResourceTreeRenderer());
  }

  public void select(ResourceEntry entry)
  {
    if (entry == null)
      tree.clearSelection();
    else if (entry != shownresource) {
      TreePath tp = ResourceFactory.getResources().getPathToNode(entry);
      tree.scrollPathToVisible(tp);
      tree.addSelectionPath(tp);
    }
  }

  public ResourceTreeModel getModel()
  {
    return (ResourceTreeModel)tree.getModel();
  }

  public void setModel(ResourceTreeModel treemodel)
  {
    nextstack.removeAllElements();
    prevstack.removeAllElements();
    bnext.setEnabled(false);
    bprev.setEnabled(false);
    tree.setModel(treemodel);
    tree.repaint();
  }

  public void expandAll()
  {
    ResourceTreeModel model = (ResourceTreeModel)tree.getModel();
    if (model != null) {
      ResourceTreeFolder root = (ResourceTreeFolder)model.getRoot();
      processAllNodes(tree, new TreePath(root), true);
    }
  }

  public void collapseAll()
  {
    ResourceTreeModel model = (ResourceTreeModel)tree.getModel();
    if (model != null) {
      ResourceTreeFolder root = (ResourceTreeFolder)model.getRoot();
      processAllNodes(tree, new TreePath(root), false);
      tree.expandPath(new TreePath(root));  // virtual root node is always expanded
    }
  }

  public void expandSelected()
  {
    TreePath path = tree.getSelectionPath();
    if (path != null && path.getPathCount() > 1) {
      Object node = path.getPathComponent(1);
      if (node instanceof ResourceTreeFolder) {
        Object root = path.getPathComponent(0);
        processAllNodes(tree, new TreePath(new Object[]{root, node}), true);
      }
    }
  }

  public void collapseSelected()
  {
    TreePath path = tree.getSelectionPath();
    if (path != null && path.getPathCount() > 1) {
      Object node = path.getPathComponent(1);
      if (node instanceof ResourceTreeFolder) {
        Object root = path.getPathComponent(0);
        processAllNodes(tree, new TreePath(new Object[]{root, node}), false);
      }
    }
  }

  /** Attempts to rename the specified file resource entry. */
  static void renameResource(FileResourceEntry entry)
  {
    String filename = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter new filename",
                                                  "Rename " + entry.toString(),
                                                  JOptionPane.QUESTION_MESSAGE);
    if (filename == null)
      return;
    if (!filename.toUpperCase(Locale.ENGLISH).endsWith(entry.getExtension()))
      filename = filename + '.' + entry.getExtension();
    if (new FileNI(entry.getActualFile().getParentFile(), filename).exists()) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "File already exists!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    entry.renameFile(filename);
    ResourceFactory.getResources().resourceEntryChanged(entry);
  }

  /** Attempts to delete the specified resource if it exists as a file in the game path. */
  static void deleteResource(ResourceEntry entry)
  {
    if (entry instanceof FileResourceEntry) {
      String options[] = {"Delete", "Cancel"};
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), "Are you sure you want to delete " +
                                                                   entry +
                                                                   '?',
                                       "Delete file", JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return;
      NearInfinity.getInstance().removeViewable();
      ResourceFactory.getResources().removeResourceEntry(entry);
      File bakFile = getBackupFile(entry);
      if (bakFile != null) {
        bakFile.delete();
      }
      ((FileResourceEntry)entry).deleteFile();
    }
    else if (entry instanceof BIFFResourceEntry) {
      String options[] = {"Delete", "Cancel"};
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), "Are you sure you want to delete the " +
                                                                   "override file to " + entry + '?',
                                       "Delete file", JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return;
      NearInfinity.getInstance().removeViewable();
      File bakFile = getBackupFile(entry);
      if (bakFile != null) {
        bakFile.delete();
      }
      ((BIFFResourceEntry)entry).deleteOverride();
    }
  }

  /** Attempts to restore the specified resource entry if it's backed up by an associated "*.bak" file. */
  static void restoreResource(ResourceEntry entry)
  {
    if (entry != null) {
      final String[] options = { "Restore", "Cancel" };
      final String msgBackup = "Are you sure you want to restore " + entry + " with a previous version?";
      final String msgBiffed = "Are you sure you want to restore the biffed version of " + entry + "?";
      File bakFile = getBackupFile(entry);
      boolean isBackedUp = (bakFile != null && (bakFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".bak")));
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), isBackedUp ?
                                       msgBackup : msgBiffed, "Restore backup",
                                       JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                       null, options, options[0]) == JOptionPane.YES_OPTION) {
        NearInfinity.getInstance().removeViewable();
        if (bakFile != null && (bakFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".bak"))) {
          // .bak available -> restore .bak version
          File curFile = getCurrentFile(entry);
          File tmpFile = getTempFile(curFile);
          if (curFile != null && curFile.isFile() && bakFile != null && bakFile.isFile()) {
            if (curFile.renameTo(tmpFile)) {
              if (bakFile.renameTo(curFile)) {
                tmpFile.delete();
                JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                              "Backup has been restored successfully.",
                                              "Restore backup", JOptionPane.INFORMATION_MESSAGE);
                return;
              } else if (!tmpFile.renameTo(curFile)) {
                // Worst possible scenario: failed restore operation can't restore original resource
                String path = tmpFile.getParent();
                String tmp = tmpFile.getName();
                String cur = curFile.getName();
                JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                              "Error while restoring resource.\n" +
                                              "Near Infinity is unable to recover from the restore operation.\n" +
                                              String.format("Please manually rename the file \"%1$s\" into \"%2$s\", located in \n + \"%3$s\"",
                                                            tmp, cur, path),
                                              "Critical Error", JOptionPane.ERROR_MESSAGE);
                return;
              }
            }
          }
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                        "Error while restoring resource.\nRestore operation has been cancelled.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        } else if (entry instanceof BIFFResourceEntry && entry.hasOverride()) {
          // Biffed and no .bak available -> delete overridden copy
          ((BIFFResourceEntry)entry).deleteOverride();
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                        "Backup has been restored successfully.",
                                        "Restore backup", JOptionPane.INFORMATION_MESSAGE);
          return;
        }
      }
    }
  }

  private static void processAllNodes(JTree tree, TreePath parent, boolean expand)
  {
    if (tree != null && parent != null) {
      Object node = parent.getLastPathComponent();
      if (node instanceof ResourceTreeFolder) {
        ResourceTreeFolder folder = (ResourceTreeFolder)node;
        if (folder.getChildCount() >= 0) {
          List<ResourceTreeFolder> list = folder.getFolders();
          for (int i = 0, size = list.size(); i < size; i++) {
            ResourceTreeFolder f = list.get(i);
            TreePath path = parent.pathByAddingChild(f);
            processAllNodes(tree, path, expand);
          }
        }
      }
      if (expand) {
        tree.expandPath(parent);
      } else {
        tree.collapsePath(parent);
      }
    }
  }

  /**
   * Returns whether a backup exists in the same folder as the specified resource entry
   * or a biffed file has been overriden. */
  static boolean isBackupAvailable(ResourceEntry entry)
  {
    if (entry != null) {
      return (getBackupFile(entry) != null ||
              (entry instanceof BIFFResourceEntry && entry.hasOverride()));
    }
    return false;
  }

  // Returns the backup file of the specified resource entry if available or null.
  // A backup file is either a *.bak file or a biffed file which has been overridden.
  private static File getBackupFile(ResourceEntry entry)
  {
    File file = getCurrentFile(entry);
    if (entry instanceof FileResourceEntry || (entry instanceof BIFFResourceEntry && entry.hasOverride())) {
      if (file != null) {
        File bakFile = new FileNI(file.getPath() + ".bak");
        if (bakFile.isFile()) {
          return bakFile;
        }
      }
    } else if (entry instanceof BIFFResourceEntry) {
      return file;
    }
    return null;
  }

  // Returns the actual physical file of the given resource entry or null.
  private static File getCurrentFile(ResourceEntry entry)
  {
    if (entry instanceof FileResourceEntry ||
        (entry instanceof BIFFResourceEntry && entry.hasOverride())) {
      return entry.getActualFile();
    } else {
      return null;
    }
  }

  // Returns an unoccupied filename based on 'file'.
  private static File getTempFile(File file)
  {
    File retVal = null;
    if (file != null && file.isFile()) {
      final String fmt = ".%1$03d";
      final String baseFile = file.getPath();
      for (int i = 0; i < 1000; i++) {
        retVal = new FileNI(baseFile + String.format(fmt, i));
        if (!retVal.exists()) {
          return retVal;
        }
      }
    }
    return null;
  }

// -------------------------- INNER CLASSES --------------------------

  private final class TreeKeyListener extends KeyAdapter implements ActionListener
  {
    private static final int TIMER_DELAY = 900;
    private String currentkey = "";
    private final Timer timer;

    private TreeKeyListener()
    {
      timer = new Timer(TIMER_DELAY, this);
      timer.setRepeats(false);
    }

    @Override
    public void keyTyped(KeyEvent event)
    {
      currentkey += new Character(event.getKeyChar()).toString().toUpperCase(Locale.ENGLISH);
      if (timer.isRunning())
        timer.restart();
      else
        timer.start();
      int startrow = 0;
      if (tree.getSelectionPath() != null)
        startrow = tree.getRowForPath(tree.getSelectionPath());
      for (int i = startrow; i < tree.getRowCount(); i++) {
        TreePath path = tree.getPathForRow(i);
        if (path != null && path.getLastPathComponent() instanceof ResourceEntry &&
            path.getLastPathComponent().toString().toUpperCase(Locale.ENGLISH).startsWith(currentkey)) {
          showresource = false;
          tree.scrollPathToVisible(path);
          tree.addSelectionPath(path);
          return;
        }
      }
      if (startrow > 0) {
        for (int i = 0; i < startrow; i++) {
          TreePath path = tree.getPathForRow(i);
          if (path != null && path.getLastPathComponent() instanceof ResourceEntry &&
              path.getLastPathComponent().toString().toUpperCase(Locale.ENGLISH).startsWith(currentkey)) {
            showresource = false;
            tree.scrollPathToVisible(path);
            tree.addSelectionPath(path);
            return;
          }
        }
      }
      currentkey = "";
      shownresource = null;
      tree.clearSelection();
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      currentkey = "";
      if (tree.getLastSelectedPathComponent() != null &&
          tree.getLastSelectedPathComponent() instanceof ResourceEntry) {
        shownresource = (ResourceEntry)tree.getLastSelectedPathComponent();
        NearInfinity.getInstance().setViewable(ResourceFactory.getResource(shownresource));
      }
      showresource = true;
    }
  }

  private final class TreeMouseListener extends MouseAdapter
  {
    private final TreePopupMenu pmenu = new TreePopupMenu();

    @Override
    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger()) {
        TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
        if (path != null && path.getPathCount() > 2) {
          showresource = false;
          tree.addSelectionPath(path);
          pmenu.show(e.getComponent(), e.getX(), e.getY());
        }
      }
      else
        showresource = true;
    }
  }

  private final class TreePopupMenu extends JPopupMenu implements ActionListener
  {
    private final JMenuItem mi_open = new JMenuItem("Open");
    private final JMenuItem mi_opennew = new JMenuItem("Open in new window");
    private final JMenuItem mi_export = new JMenuItem("Export");
    private final JMenuItem mi_addcopy = new JMenuItem("Add copy of");
    private final JMenuItem mi_rename = new JMenuItem("Rename");
    private final JMenuItem mi_delete = new JMenuItem("Delete");
    private final JMenuItem mi_restore = new JMenuItem("Restore backup");

    TreePopupMenu()
    {
      add(mi_open);
      add(mi_opennew);
      add(mi_export);
      add(mi_addcopy);
      add(mi_rename);
      add(mi_delete);
      add(mi_restore);
      mi_open.addActionListener(this);
      mi_opennew.addActionListener(this);
      mi_export.addActionListener(this);
      mi_addcopy.addActionListener(this);
      mi_rename.addActionListener(this);
      mi_delete.addActionListener(this);
      mi_restore.addActionListener(this);
      mi_opennew.setFont(mi_opennew.getFont().deriveFont(Font.PLAIN));
      mi_export.setFont(mi_opennew.getFont());
      mi_addcopy.setFont(mi_opennew.getFont());
      mi_rename.setFont(mi_opennew.getFont());
      mi_delete.setFont(mi_opennew.getFont());
      mi_restore.setFont(mi_opennew.getFont());
    }

    @Override
    public void show(Component invoker, int x, int y)
    {
      super.show(invoker, x, y);
      mi_rename.setEnabled(tree.getLastSelectedPathComponent() instanceof FileResourceEntry);
      if (tree.getLastSelectedPathComponent() instanceof ResourceEntry) {
        ResourceEntry entry = (ResourceEntry)tree.getLastSelectedPathComponent();
        mi_delete.setEnabled(entry != null && entry.hasOverride() || entry instanceof FileResourceEntry);
        mi_restore.setEnabled(isBackupAvailable(entry));
      }
      else {
        mi_delete.setEnabled(false);
        mi_restore.setEnabled(false);
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      showresource = true;
      ResourceEntry node = (ResourceEntry)tree.getLastSelectedPathComponent();
      if (event.getSource() == mi_open) {
        if (prevnextnode != null)
          prevstack.push(prevnextnode);
        nextstack.removeAllElements();
        bnext.setEnabled(false);
        bprev.setEnabled(prevnextnode != null);
        prevnextnode = node;
        shownresource = node;
        NearInfinity.getInstance().setViewable(ResourceFactory.getResource(node));
      }
      else if (event.getSource() == mi_opennew) {
        Resource res = ResourceFactory.getResource(node);
        if (res != null)
          new ViewFrame(NearInfinity.getInstance(), res);
      }
      else if (event.getSource() == mi_export) {
        ResourceFactory.exportResource(node, NearInfinity.getInstance());
      }
      else if (event.getSource() == mi_addcopy) {
        ResourceFactory.saveCopyOfResource(node);
      }
      else if (event.getSource() == mi_rename) {
        if (tree.getLastSelectedPathComponent() instanceof FileResourceEntry) {
          renameResource((FileResourceEntry)tree.getLastSelectedPathComponent());
        }
      }
      else if (event.getSource() == mi_delete) {
        if (tree.getLastSelectedPathComponent() instanceof ResourceEntry) {
          deleteResource((ResourceEntry)tree.getLastSelectedPathComponent());
        }
      }
      else if (event.getSource() == mi_restore) {
        if (tree.getLastSelectedPathComponent() instanceof ResourceEntry) {
          restoreResource((ResourceEntry)tree.getLastSelectedPathComponent());
        }
      }
    }
  }

  private static final class ResourceTreeRenderer extends DefaultTreeCellRenderer
  {
    private ResourceTreeRenderer()
    {
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object o, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus)
    {
      if (leaf && o instanceof ResourceEntry) {
        super.getTreeCellRendererComponent(tree, o, sel, expanded, leaf, row, hasFocus);
        setIcon(((ResourceEntry)o).getIcon());
        return this;
      }
      else
        return super.getTreeCellRendererComponent(tree, o, sel, expanded, leaf, row, hasFocus);
    }
  }
}

