// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.infinity.gui.BIFFEditor;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.StringEditor;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.util.StringTable;

/**
 * Handles Edit menu items for the {@link BrowserMenuBar}.
 */
public class EditMenu extends JMenu implements BrowserSubMenu, ActionListener {
  private final BrowserMenuBar menuBar;

  private final JMenuItem editString;
  private final JMenuItem editBIFF;

  public EditMenu(BrowserMenuBar parent) {
    super("Edit");
    setMnemonic(KeyEvent.VK_E);

    menuBar = parent;

    editString = BrowserMenuBar.makeMenuItem("String table", KeyEvent.VK_S, Icons.ICON_EDIT_16.getIcon(), KeyEvent.VK_S, this);
    add(editString);
    // TODO: reactivate when fixed
    editBIFF = BrowserMenuBar.makeMenuItem("BIFF", KeyEvent.VK_B, Icons.ICON_EDIT_16.getIcon(), KeyEvent.VK_E, this);
    final List<Path> dlcPaths = Profile.getProperty(Profile.Key.GET_GAME_DLC_FOLDERS_AVAILABLE);
    if (Profile.isEnhancedEdition() && dlcPaths != null && !dlcPaths.isEmpty()) {
      editBIFF.setToolTipText("Temporarily disabled");
      editBIFF.setEnabled(false);
    }
    add(editBIFF);
  }

  public void gameLoaded() {
    // String table may not be available
    editString.setEnabled(StringTable.isAvailable());
  }

  @Override
  public BrowserMenuBar getMenuBar() {
    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == editString) {
      ChildFrame.show(StringEditor.class, StringEditor::new);
    } else if (event.getSource() == editBIFF) {
      new BIFFEditor();
    }
  }
}
