// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataListener;
import javax.swing.text.JTextComponent;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.key.ResourceTreeFolder;
import org.infinity.resource.key.ResourceTreeModel;
import org.infinity.util.MapTree;
import org.infinity.util.Misc;

/**
 * Implements a search panel for quickly finding specific resources.
 */
public class QuickSearch extends JPanel implements Runnable
{
  // Internally used to control actions in the background task
  private enum Command {
    IDLE, UPDATE, DESTROY
  }

  // Defines available search actions
  private enum Result {
    CANCEL, OPEN, OPEN_NEW,
  }

  private final ButtonPopupWindow parent;
  private final ResourceTree tree;
  private final MapTree<Character, List<ResourceEntry>> resourceTree;
  private final Object monitor = new Object();  // synchronization object
  private final JPanel mainPanel = new JPanel(new GridBagLayout());

  private JLabel lSearch;
  private JComboBox<ResourceEntry> cbSearch;
  private JButton bOk, bOkNew, bCancel;
  private String keyword;
  private Command command;

  public QuickSearch(ButtonPopupWindow parent, ResourceTree tree)
  {
    super();
    if (parent == null || tree == null) {
      throw new NullPointerException("parent and tree must not be null!");
    }
    this.parent = parent;
    this.tree = tree;
    this.resourceTree = new MapTree<>(Character.valueOf('\0'), null);
    this.command = Command.IDLE;
    new Thread(this).start();   // updating list of matching resources is done in the background
    init();
  }

  private void init()
  {
    // Action for pressing "Enter"
    final Action acceptAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        close(Result.OPEN);
      }
    };

    // Action for pressing "Enter"
    final Action acceptNewAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        close(Result.OPEN_NEW);
      }
    };

    // Action for pressing "Escape"
    final Action rejectAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        close(Result.CANCEL);
      }
    };

    // Action for changing text in search field
    final KeyListener keyListener = new KeyListener() {
      @Override
      public void keyReleased(KeyEvent event)
      {
        switch (event.getKeyCode()) {
          case KeyEvent.VK_ESCAPE:
            event.consume();
            close(Result.CANCEL);
            break;
          case KeyEvent.VK_ENTER:
            event.consume();
            if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK) {
              close(Result.OPEN_NEW);
            } else {
              close(Result.OPEN);
            }
            break;
          default:
            if (event.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
              updateSuggestions(getSearchString());
            }
        }
      }

      @Override public void keyTyped(KeyEvent e) {}
      @Override public void keyPressed(KeyEvent e) {}
    };

    final PopupWindowListener popupListener = new PopupWindowListener() {
      @Override public void popupWindowWillBecomeVisible(PopupWindowEvent event) {}

      @Override
      public void popupWindowWillBecomeInvisible(PopupWindowEvent event)
      {
        cbSearch.hidePopup();

        synchronized (monitor) {
          command = Command.DESTROY;
          monitor.notify();
        }
      }
    };

    parent.addGlobalKeyStroke("ENTER", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), acceptAction);
    parent.addGlobalKeyStroke("ESCAPE", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), rejectAction);
    parent.addPopupWindowListener(popupListener);

    setLayout(new GridBagLayout());

    lSearch = new JLabel("Search:", SwingConstants.LEFT);

    cbSearch = new JComboBox<>();
    cbSearch.setPreferredSize(Misc.getPrototypeSize(cbSearch, "WWWWWWWW.WWWW"));    // space for at least 8.4 characters
    cbSearch.setEditable(true);
    cbSearch.getEditor().getEditorComponent().addKeyListener(keyListener);

    bOk = new JButton(Icons.getIcon(Icons.ICON_CHECK_16));
    bOk.addActionListener(acceptAction);
    bOk.setMargin(new Insets(1, 4, 1, 4));
    bOk.setToolTipText("Open (Shortcut: Enter)");
    bOkNew = new JButton(Icons.getIcon(Icons.ICON_OPEN_16));
    bOkNew.addActionListener(acceptNewAction);
    bOkNew.setMargin(new Insets(1, 5, 1, 4));
    bOkNew.setToolTipText("Open in new window (Shortcut: Shift+Enter)");
    bCancel = new JButton(Icons.getIcon(Icons.ICON_CHECK_NOT_16));
    bCancel.setMargin(new Insets(1, 2, 1, 2));
    bCancel.setToolTipText("Cancel search (Shortcut: Esc)");
    bCancel.addActionListener(rejectAction);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(lSearch, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    mainPanel.add(cbSearch, gbc);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.VERTICAL, new Insets(0, 4, 0, 0), 0, 0);
    mainPanel.add(bOk, gbc);
    gbc = ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.VERTICAL, new Insets(0, 4, 0, 0), 0, 0);
    mainPanel.add(bOkNew, gbc);
    gbc = ViewerUtil.setGBC(gbc, 4, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.VERTICAL, new Insets(0, 4, 0, 0), 0, 0);
    mainPanel.add(bCancel, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    add(mainPanel, gbc);
  }

  @Override
  public boolean requestFocusInWindow()
  {
    return cbSearch.requestFocusInWindow();
  }

  // Updates the list of resources matching the specified text
  private void updateSuggestions(String text)
  {
    synchronized (monitor) {
      keyword = (text != null) ? text : "";
      command = Command.UPDATE;
      monitor.notify();
    }
  }

  // Returns the text field content of the combobox
  private String getSearchString()
  {
    return ((JTextComponent)cbSearch.getEditor().getEditorComponent()).getText();
  }

  // Executed when accepting current input
  private void close(Result result)
  {
    if (result != Result.CANCEL) {
      Object item = cbSearch.getSelectedItem();
      if (!(item instanceof ResourceEntry)) {
        item = cbSearch.getItemAt(0);
      }

      if (item instanceof ResourceEntry) {
        if (result == Result.OPEN) {
          tree.select((ResourceEntry)item);
        } else if (result == Result.OPEN_NEW) {
          Resource res = ResourceFactory.getResource((ResourceEntry)item);
          if (res != null) {
            new ViewFrame(NearInfinity.getInstance(), res);
          }
        }
      }
    }

    parent.hidePopupWindow();
  }

  // Generates root list of resource entries
  private void generateRootNode()
  {
    // removing old list (if any)
    List<ResourceEntry> list = resourceTree.getValue();
    if (list != null) {
      list.clear();
    } else {
      list = new Vector<>();
      resourceTree.setValue(list);
    }

    // populating list with new entries
    ResourceTreeModel model = tree.getModel();
    if (model != null) {
      SortedSet<ResourceEntry> entries = generateResourceList(model.getRoot(), null);
      if (entries != null) {
        list.addAll(entries);
      }
    }
  }

  // Returns a set of resource entries from the resource tree
  private SortedSet<ResourceEntry> generateResourceList(ResourceTreeFolder folder, SortedSet<ResourceEntry> set)
  {
    if (set == null) {
      set = new TreeSet<>();
    }

    if (folder != null) {
      set.addAll(folder.getResourceEntries());
      for (final ResourceTreeFolder subFolder: folder.getFolders()) {
        generateResourceList(subFolder, set);
      }
    }
    return set;
  }

  // Creates a new node with a list of matching resources based on the specified node and the new character
  private MapTree<Character, List<ResourceEntry>> generateNode(MapTree<Character, List<ResourceEntry>> node, char ch)
  {
    // determining node level (0 = first level)
    int index = 0;
    for (MapTree<Character, List<ResourceEntry>> curNode = node;
         curNode.getParent() != null;
         curNode = curNode.getParent()) {
      index++;
    }

    // generating filtered list of resource entries based on list of previous node
    ch = Character.toUpperCase(ch);

    // preparing child node
    MapTree<Character, List<ResourceEntry>> retVal = node.getChild(Character.valueOf(ch));
    if (retVal != null) {
      if (retVal.getValue() != null) {
        retVal.getValue().clear();
      } else {
        retVal.setValue(new Vector<ResourceEntry>());
      }
    } else {
      retVal = new MapTree<>(Character.valueOf(ch), new Vector<ResourceEntry>());
    }
    node.addChild(retVal);

    // generating filtered list of resource entries
    List<ResourceEntry> parentList = node.getValue();
    List<ResourceEntry> curList = retVal.getValue();
    for (Iterator<ResourceEntry> iter = parentList.iterator(); iter.hasNext();) {
      final ResourceEntry entry = iter.next();
      final String resName = entry.getResourceName();
      if (resName.length() > index && Character.toUpperCase(resName.charAt(index)) == ch) {
        curList.add(entry);
      }
    }

    return retVal;
  }

  // Removes all child nodes and their values recursively
//  private void clearResourceTree(MapTree<Character, List<ResourceEntry>> node)
//  {
//    if (node != null) {
//      for (Iterator<MapTree<Character, List<ResourceEntry>>> iter = node.getChildren().iterator();
//           iter.hasNext();) {
//        MapTree<Character, List<ResourceEntry>> curNode = iter.next();
//        clearResourceTree(curNode);
//      }
//      node.removeAllChildren();
//      List<ResourceEntry> list = node.setValue(null);
//      if (list != null) {
//        list.clear();
//        list = null;
//      }
//    }
//  }

// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    // main loop
    while (true) {
      if (command == Command.DESTROY) {
        synchronized (monitor) {
          command = Command.IDLE;
        }
        break;
      } else if (command == Command.UPDATE) {
        synchronized (monitor) {
          command = Command.IDLE;

          // populating root node
          if (resourceTree.getValue() == null || resourceTree.getValue().isEmpty()) {
            generateRootNode();
          }

          // processing new keyword
          if (keyword != null) {
            keyword = keyword.toUpperCase(Locale.ENGLISH);
            MapTree<Character, List<ResourceEntry>> node = resourceTree;
            for (int i = 0, size = keyword.length(); i < size; i++) {
              MapTree<Character, List<ResourceEntry>> newNode = node.getChild(Character.valueOf(keyword.charAt(i)));
              if (newNode == null) {
                node = generateNode(node, keyword.charAt(i));
              } else {
                node = newNode;
              }
            }

            // setting matching resource entries
            DefaultComboBoxModel<ResourceEntry> cbModel = (DefaultComboBoxModel<ResourceEntry>)cbSearch.getModel();

            // Deactivating listeners to prevent autoselecting items
            ListDataListener[] listeners = cbModel.getListDataListeners();
            for (int i = listeners.length - 1; i >= 0; i--) {
              cbModel.removeListDataListener(listeners[i]);
            }

            cbSearch.hidePopup();   // XXX: work-around to force visual update of file list
            cbModel.removeAllElements();
            if (!keyword.isEmpty() && node != null && node.getValue() != null) {
              List<ResourceEntry> list = node.getValue();
              for (Iterator<ResourceEntry> iter = list.iterator(); iter.hasNext();) {
                cbModel.addElement(iter.next());
              }
            }

            // Reactivating listeners
            for (int i = 0; i < listeners.length; i++) {
              cbModel.addListDataListener(listeners[i]);
            }

            cbSearch.setMaximumRowCount(Math.min(8, cbModel.getSize()));
            if (cbModel.getSize() > 0 && !cbSearch.isPopupVisible()) {
              cbSearch.showPopup();
            } else if (cbModel.getSize() == 0 && cbSearch.isPopupVisible()) {
              cbSearch.hidePopup();
            }
          }
//      } else if (command == Command.Clear) {
//        // reset data
//        synchronized(monitor) {
//          command = Command.Idle;
//          clearResourceTree(resourceTree);
//          ((DefaultComboBoxModel)cbSearch.getModel()).removeAllElements();
//          ((JTextComponent)cbSearch.getEditor().getEditorComponent()).setText("");
        }
      } else {
        // nothing else to do?
        synchronized(monitor) {
          try {
            monitor.wait();
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }

// --------------------- End Interface Runnable ---------------------
}
