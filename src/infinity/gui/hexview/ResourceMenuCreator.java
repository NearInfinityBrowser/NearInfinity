// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.hexview;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import infinity.gui.DataMenuItem;
import infinity.gui.StructViewer;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.resource.dlg.AbstractCode;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import tv.porst.jhexview.IMenuCreator;
import tv.porst.jhexview.JHexView;

/**
 * Provides a dynamic popupmenu for the HexViewer component.
 *
 * @author argent77
 */
public class ResourceMenuCreator implements IMenuCreator, ActionListener
{
  private final JPopupMenu popup;
  private final JMenuItem miUndo;
  private final JMenuItem miRedo;
  private final JMenuItem miCopy;
  private final JMenuItem miPaste;
  private final JMenuItem miSelectAll;
  private final JHexView hexView;
  private final AbstractStruct struct;


  public ResourceMenuCreator(JHexView hexView, AbstractStruct struct)
  {
    if (hexView == null) {
      throw new NullPointerException("hexView is null");
    }
    this.hexView = hexView;

    if (struct == null) {
      throw new NullPointerException("struct is null");
    }
    this.struct = struct;

    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    popup = new JPopupMenu();
    miUndo = new JMenuItem("Undo", Icons.getIcon("Undo16.gif"));
    miUndo.setMnemonic('u');
    miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl));

    miRedo = new JMenuItem("Redo", Icons.getIcon("Redo16.gif"));
    miRedo.setMnemonic('r');
    miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl));

    miCopy = new JMenuItem("Copy", Icons.getIcon("Copy16.gif"));
    miCopy.setMnemonic('c');
    miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl));

    miPaste = new JMenuItem("Paste", Icons.getIcon("Paste16.gif"));
    miPaste.setMnemonic('p');
    miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrl));

    miSelectAll = new JMenuItem("Select All");
    miSelectAll.setMnemonic('a');
    miSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ctrl));

    miUndo.addActionListener(this);
    miRedo.addActionListener(this);
    miCopy.addActionListener(this);
    miPaste.addActionListener(this);
    miSelectAll.addActionListener(this);
  }

//--------------------- Begin Interface IMenuCreator ---------------------

  @Override
  public JPopupMenu createMenu(long offset)
  {
    // generating popup menu
    popup.removeAll();

    List<JMenuItem> list = createStructEntries((int)offset);
    for (final JMenuItem mi: list) {
      mi.addActionListener(this);
      popup.add(mi);
    }

    popup.addSeparator();

    miUndo.setText(String.format("Undo %1$s", getHexView().getUndoPresentationName()));
    miUndo.setEnabled(isUndoAvailable());
    popup.add(miUndo);
    miRedo.setEnabled(isRedoAvailable());
    miRedo.setText(String.format("Redo %1$s", getHexView().getRedoPresentationName()));
    popup.add(miRedo);

    popup.addSeparator();

    miCopy.setEnabled(isCopyAvailable());
    popup.add(miCopy);
    miPaste.setEnabled(isPasteAvailable());
    popup.add(miPaste);
    popup.add(miSelectAll);

    return popup;
  }

//--------------------- End Interface IMenuCreator ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof DataMenuItem<?>) {
      DataMenuItem<StructEntry[]> mi = (DataMenuItem<StructEntry[]>)e.getSource();
      StructEntry[] entries = mi.getData();
      StructViewer curViewer = null;
      if (entries != null && entries.length > 0) {
        // opens every StructEntry in the list and selects the respective data field
        for (int i = 0; i < entries.length; i++) {
          StructEntry entry = entries[i];
          if (entry != null) {
            if (entry instanceof AbstractStruct) {
              if (entry == getStruct()) {
                curViewer = ((AbstractStruct)entry).getViewer();
                if (curViewer != null) {
                  curViewer.selectEditTab();
                  curViewer.selectEntry(entry.getOffset(), false);
                }
              } else {
                if (curViewer != null) {
                  curViewer.selectEntry(entry.getOffset(), false);
                }
                ViewFrame curFrame = curViewer.getViewFrame((AbstractStruct)entry);
                if (curFrame.getViewable() instanceof AbstractStruct) {
                  curViewer = ((AbstractStruct)curFrame.getViewable()).getViewer();
                } else {
                  curViewer = null;
                }
              }
            } else if (curViewer != null) {
              curViewer.selectEntry(entry.getOffset(), false);
              curViewer = null;
            }
          }
        }
      }
    } else if (e.getSource() == miUndo) {
      getHexView().undo();
    } else if (e.getSource() == miRedo) {
      getHexView().redo();
    } else if (e.getSource() == miCopy) {
      getHexView().copy();
    } else if (e.getSource() == miPaste) {
      getHexView().paste();
    } else if (e.getSource() == miSelectAll) {
      getHexView().selectAll();
    }
  }

//--------------------- End Interface ActionListener ---------------------

  /** Returns the parent JHexView component. */
  public JHexView getHexView()
  {
    return hexView;
  }

  /** Returns the associated resource structure instance. */
  public AbstractStruct getStruct()
  {
    return struct;
  }

  // Creates a list of all structures containing the specified offset, starting from topmost level.
  private List<JMenuItem> createStructEntries(int offset)
  {
    List<JMenuItem> list = new ArrayList<JMenuItem>();
    List<StructEntry> listEntries = new ArrayList<StructEntry>();
    StructEntry curEntry = getStruct();
    while (curEntry != null) {
      listEntries.add(curEntry);

      if (curEntry instanceof AbstractStruct) {
        StructEntry newEntry = null;
        for (final StructEntry e: ((AbstractStruct)curEntry).getList()) {
          if (e instanceof AbstractCode) {
            // AbstractCode instances consist of two separate data blocks
            AbstractCode ac = (AbstractCode)e;
            if ((offset >= ac.getOffset() && offset < ac.getOffset()+ac.getSize()) ||
                (offset >= ac.getTextOffset() && offset < ac.getTextOffset()+ac.getTextLength())) {
              newEntry = e;
              break;
            }
          } else {
            if (offset >= e.getOffset() && offset < e.getOffset()+e.getSize()) {
              newEntry = e;
              break;
            }
          }
        }
        curEntry = newEntry;
      } else {
        curEntry = null;
      }
    }

    // skipping root structure
    for (int i = 1; i < listEntries.size(); i++) {
      StructEntry[] entries = new StructEntry[i+1];
      for (int j = 0; j <= i; j++) {
        entries[j] = listEntries.get(j);
      }
      JMenuItem mi =
          new DataMenuItem<StructEntry[]>(String.format("Go to \"%1$s\"",
                                                        entries[i].getName()), null, entries);
      list.add(mi);
    }

    return list;
  }

  private boolean isUndoAvailable()
  {
    return getHexView().canUndo();
  }

  private boolean isRedoAvailable()
  {
    return getHexView().canRedo();
  }

  private boolean isCopyAvailable()
  {
    return (getHexView().getSelectionLength() > 0);
  }

  private boolean isPasteAvailable()
  {
    return Toolkit.getDefaultToolkit().getSystemClipboard()
        .isDataFlavorAvailable(DataFlavor.stringFlavor);
  }
}
