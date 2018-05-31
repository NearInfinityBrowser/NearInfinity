// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.infinity.icon.Icons;

import tv.porst.jhexview.IMenuCreator;
import tv.porst.jhexview.JHexView;

/**
 * Provides a basic popupmenu for the HexViewer components.
 */
public class MenuCreator implements IMenuCreator, ActionListener
{
  private final JPopupMenu popup;
  private final JMenuItem miUndo;
  private final JMenuItem miRedo;
  private final JMenuItem miCopy;
  private final JMenuItem miPaste;
  private final JMenuItem miSelectAll;
  private final JHexView hexView;

  public MenuCreator(JHexView hexView)
  {
    if (hexView == null) {
      throw new NullPointerException("hexView is null");
    }
    this.hexView = hexView;

    int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    popup = new JPopupMenu();
    miUndo = new JMenuItem("Undo", Icons.getIcon(Icons.ICON_UNDO_16));
    miUndo.setMnemonic('u');
    miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl));

    miRedo = new JMenuItem("Redo", Icons.getIcon(Icons.ICON_REDO_16));
    miRedo.setMnemonic('r');
    miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl));

    miCopy = new JMenuItem("Copy", Icons.getIcon(Icons.ICON_COPY_16));
    miCopy.setMnemonic('c');
    miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl));

    miPaste = new JMenuItem("Paste", Icons.getIcon(Icons.ICON_PASTE_16));
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

    miUndo.setText(String.format("Undo %s", getHexView().getUndoPresentationName()));
    miUndo.setEnabled(isUndoAvailable());
    popup.add(miUndo);
    miRedo.setEnabled(isRedoAvailable());
    miRedo.setText(String.format("Redo %s", getHexView().getRedoPresentationName()));
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
    if (e.getSource() == miUndo) {
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

  protected JPopupMenu getPopupMenu()
  {
    return popup;
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
