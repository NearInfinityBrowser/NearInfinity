// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.key.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Stack;

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
      TreePath tp = ResourceFactory.getInstance().getResources().getPathToNode(entry);
      tree.scrollPathToVisible(tp);
      tree.addSelectionPath(tp);
    }
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

    public void keyTyped(KeyEvent event)
    {
      currentkey += new Character(event.getKeyChar()).toString().toUpperCase();
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
            path.getLastPathComponent().toString().startsWith(currentkey)) {
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
              path.getLastPathComponent().toString().startsWith(currentkey)) {
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

    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

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

    TreePopupMenu()
    {
      add(mi_open);
      add(mi_opennew);
      add(mi_export);
      add(mi_addcopy);
      add(mi_rename);
      add(mi_delete);
      mi_open.addActionListener(this);
      mi_opennew.addActionListener(this);
      mi_export.addActionListener(this);
      mi_addcopy.addActionListener(this);
      mi_rename.addActionListener(this);
      mi_delete.addActionListener(this);
      mi_opennew.setFont(mi_opennew.getFont().deriveFont(Font.PLAIN));
      mi_export.setFont(mi_opennew.getFont());
      mi_addcopy.setFont(mi_opennew.getFont());
      mi_rename.setFont(mi_opennew.getFont());
      mi_delete.setFont(mi_opennew.getFont());
    }

    public void show(Component invoker, int x, int y)
    {
      super.show(invoker, x, y);
      mi_rename.setEnabled(tree.getLastSelectedPathComponent() instanceof FileResourceEntry);
      if (tree.getLastSelectedPathComponent() instanceof ResourceEntry) {
        ResourceEntry entry = (ResourceEntry)tree.getLastSelectedPathComponent();
        mi_delete.setEnabled(entry.hasOverride() || entry instanceof FileResourceEntry);
      }
      else
        mi_delete.setEnabled(false);
    }

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
      else if (event.getSource() == mi_export)
        ResourceFactory.getInstance().exportResource(node, NearInfinity.getInstance());
      else if (event.getSource() == mi_addcopy)
        ResourceFactory.getInstance().saveCopyOfResource(node);
      else if (event.getSource() == mi_rename) {
        FileResourceEntry entry = (FileResourceEntry)tree.getLastSelectedPathComponent();
        String filename = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter new filename",
                                                      "Rename " + entry.toString(),
                                                      JOptionPane.QUESTION_MESSAGE);
        if (filename == null)
          return;
        if (!filename.toUpperCase().endsWith(entry.getExtension()))
          filename = filename + '.' + entry.getExtension();
        if (new File(entry.getActualFile().getParentFile(), filename).exists()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "File already exists!", "Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        entry.renameFile(filename);
        ResourceFactory.getInstance().getResources().resourceEntryChanged(entry);
      }
      else if (event.getSource() == mi_delete) {
        if (tree.getLastSelectedPathComponent() instanceof FileResourceEntry) {
          FileResourceEntry entry = (FileResourceEntry)tree.getLastSelectedPathComponent();
          String options[] = {"Delete", "Cancel"};
          if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), "Are you sure you want to delete " +
                                                                       entry +
                                                                       '?',
                                           "Delete file", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
            return;
          NearInfinity.getInstance().removeViewable();
          ResourceFactory.getInstance().getResources().removeResourceEntry(entry);
          entry.deleteFile();
        }
        else if (tree.getLastSelectedPathComponent() instanceof BIFFResourceEntry) {
          BIFFResourceEntry entry = (BIFFResourceEntry)tree.getLastSelectedPathComponent();
          String options[] = {"Delete", "Cancel"};
          if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), "Are you sure you want to delete the " +
                                                                       "override file to " + entry + '?',
                                           "Delete file", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
            return;
          NearInfinity.getInstance().removeViewable();
          entry.deleteOverride();
        }
      }
    }
  }

  private static final class ResourceTreeRenderer extends DefaultTreeCellRenderer
  {
    private ResourceTreeRenderer()
    {
    }

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

