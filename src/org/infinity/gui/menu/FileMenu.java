// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.EnumSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.infinity.NearInfinity;
import org.infinity.gui.ResourceTree;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructureFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;

/**
 * Handles File menu items for the {@link BrowserMenuBar}.
 */
public class FileMenu extends JMenu implements BrowserSubMenu, ActionListener {
  private static final FileMenu.ResInfo[] RESOURCE = {
      new ResInfo(StructureFactory.ResType.RES_2DA, "2DA"),
      new ResInfo(StructureFactory.ResType.RES_ARE, "ARE"),
      new ResInfo(StructureFactory.ResType.RES_BAF, "BAF"),
      new ResInfo(StructureFactory.ResType.RES_BCS, "BCS"),
      new ResInfo(StructureFactory.ResType.RES_BIO, "BIO",
          new Profile.Game[] { Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD,
              Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_CHR, "CHR",
          new Profile.Game[] { Profile.Game.BG1, Profile.Game.BG1TotSC, Profile.Game.BG2SoA, Profile.Game.BG2ToB,
              Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM, Profile.Game.IWD2, Profile.Game.BG1EE,
              Profile.Game.BG1SoD, Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_CRE, "CRE"),
      new ResInfo(StructureFactory.ResType.RES_EFF, "EFF",
          new Profile.Game[] { Profile.Game.BG1, Profile.Game.BG1TotSC, Profile.Game.BG2SoA, Profile.Game.BG2ToB,
              Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.PSTEE,
              Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_IDS, "IDS"),
      new ResInfo(StructureFactory.ResType.RES_ITM, "ITM"),
      new ResInfo(StructureFactory.ResType.RES_INI, "INI",
          new Profile.Game[] { Profile.Game.PST, Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM,
              Profile.Game.IWD2, Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE, Profile.Game.IWDEE,
              Profile.Game.PSTEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_PRO, "PRO",
          new Profile.Game[] { Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD,
              Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_RES, "RES",
          new Profile.Game[] { Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM, Profile.Game.IWD2 }),
      new ResInfo(StructureFactory.ResType.RES_SPL, "SPL"),
      new ResInfo(StructureFactory.ResType.RES_SRC, "SRC",
          new Profile.Game[] { Profile.Game.PST, Profile.Game.IWD2, Profile.Game.PSTEE }),
      new ResInfo(StructureFactory.ResType.RES_STO, "STO"),
      new ResInfo(StructureFactory.ResType.RES_VEF, "VEF",
          new Profile.Game[] { Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD,
              Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_VVC, "VVC",
          new Profile.Game[] { Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD,
              Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_WED, "WED"),
      new ResInfo(StructureFactory.ResType.RES_WFX, "WFX",
          new Profile.Game[] { Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD,
              Profile.Game.BG2EE, Profile.Game.IWDEE, Profile.Game.PSTEE, Profile.Game.EET }),
      new ResInfo(StructureFactory.ResType.RES_WMAP, "WMAP"), };

  private final BrowserMenuBar menuBar;

  private final JMenu newFileMenu;
  private final JMenuItem fileOpenNew;
  private final JMenuItem fileReference;
  private final JMenuItem fileExport;
  private final JMenuItem fileAddCopy;
  private final JMenuItem fileRename;
  private final JMenuItem fileDelete;
  private final JMenuItem fileRestore;

  public FileMenu(BrowserMenuBar parent) {
    super("File");
    setMnemonic(KeyEvent.VK_F);

    menuBar = parent;

    newFileMenu = new JMenu("New Resource");
    newFileMenu.setIcon(Icons.ICON_NEW_16.getIcon());
    newFileMenu.setMnemonic(KeyEvent.VK_N);
    add(newFileMenu);
    fileOpenNew = BrowserMenuBar.makeMenuItem("Open in New Window", KeyEvent.VK_W, Icons.ICON_OPEN_16.getIcon(), -1, this);
    fileOpenNew.setEnabled(false);
    add(fileOpenNew);
    fileReference = BrowserMenuBar.makeMenuItem("Find references...", KeyEvent.VK_F, Icons.ICON_FIND_16.getIcon(), -1, this);
    fileReference.setEnabled(false);
    add(fileReference);
    fileExport = BrowserMenuBar.makeMenuItem("Export...", KeyEvent.VK_E, Icons.ICON_EXPORT_16.getIcon(), -1, this);
    fileExport.setEnabled(false);
    add(fileExport);
    fileAddCopy = BrowserMenuBar.makeMenuItem("Add Copy Of...", KeyEvent.VK_A, Icons.ICON_ADD_16.getIcon(), -1, this);
    fileAddCopy.setEnabled(false);
    add(fileAddCopy);
    fileRename = BrowserMenuBar.makeMenuItem("Rename...", KeyEvent.VK_R, Icons.ICON_EDIT_16.getIcon(), -1, this);
    fileRename.setEnabled(false);
    add(fileRename);
    fileDelete = BrowserMenuBar.makeMenuItem("Delete", KeyEvent.VK_D, Icons.ICON_DELETE_16.getIcon(), -1, this);
    fileDelete.setEnabled(false);
    add(fileDelete);
    fileRestore = BrowserMenuBar.makeMenuItem("Restore backup", KeyEvent.VK_B, Icons.ICON_UNDO_16.getIcon(), -1, this);
    fileRestore.setEnabled(false);
    add(fileRestore);
  }

  public void gameLoaded() {
    if (newFileMenu != null) {
      newFileMenu.removeAll();

      for (final FileMenu.ResInfo res : RESOURCE) {
        if (res.gameSupported(Profile.getGame())) {
          JMenuItem newFile = new JMenuItem(res.label);
          newFile.addActionListener(this);
          newFile.setActionCommand(res.label);
          newFile.setEnabled(true);
          newFileMenu.add(newFile);
        }
      }
      newFileMenu.setEnabled(newFileMenu.getItemCount() > 0);
    }
  }

  public void resourceEntrySelected(ResourceEntry entry) {
    fileOpenNew.setEnabled(entry != null);
    Class<? extends Resource> cls = ResourceFactory.getResourceType(entry);
    fileReference.setEnabled(cls != null && Referenceable.class.isAssignableFrom(cls));
    fileExport.setEnabled(entry != null);
    fileAddCopy.setEnabled(entry != null);
    fileRename.setEnabled(entry instanceof FileResourceEntry);
    fileDelete.setEnabled((entry != null && entry.hasOverride()) || entry instanceof FileResourceEntry);
    fileRestore.setEnabled(ResourceTree.isBackupAvailable(entry));
  }

  @Override
  public BrowserMenuBar getMenuBar() {
    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == fileOpenNew) {
      Resource res = ResourceFactory.getResource(NearInfinity.getInstance().getResourceTree().getSelected());
      if (res != null) {
        new ViewFrame(NearInfinity.getInstance(), res);
      }
    } else if (event.getSource() == fileReference) {
      Resource res = ResourceFactory.getResource(NearInfinity.getInstance().getResourceTree().getSelected());
      if (res instanceof Referenceable) {
        if (((Referenceable) res).isReferenceable()) {
          ((Referenceable) res).searchReferences(NearInfinity.getInstance());
        } else {
          JOptionPane.showMessageDialog(
              NearInfinity.getInstance(), "Finding references is not supported for "
                  + NearInfinity.getInstance().getResourceTree().getSelected() + ".",
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } else if (event.getSource() == fileExport) {
      ResourceFactory.exportResource(NearInfinity.getInstance().getResourceTree().getSelected(),
          NearInfinity.getInstance());
    } else if (event.getSource() == fileAddCopy) {
      ResourceFactory.saveCopyOfResource(NearInfinity.getInstance().getResourceTree().getSelected());
    } else if (event.getSource() == fileRename) {
      if (NearInfinity.getInstance().getResourceTree().getSelected() instanceof FileResourceEntry) {
        ResourceTree.renameResource((FileResourceEntry) NearInfinity.getInstance().getResourceTree().getSelected());
      }
    } else if (event.getSource() == fileDelete) {
      ResourceTree.deleteResource(NearInfinity.getInstance().getResourceTree().getSelected());
    } else if (event.getSource() == fileRestore) {
      ResourceTree.restoreResource(NearInfinity.getInstance().getResourceTree().getSelected());
    } else {
      for (final FileMenu.ResInfo res : RESOURCE) {
        if (event.getActionCommand().equals(res.label)) {
          StructureFactory.getInstance().newResource(res.resId, NearInfinity.getInstance());
        }
      }
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class ResInfo {
    public final String label;
    public final StructureFactory.ResType resId;
    private final EnumSet<Profile.Game> supportedGames = EnumSet.noneOf(Profile.Game.class);

    public ResInfo(StructureFactory.ResType id, String text) {
      this(id, text,
          new Profile.Game[] { Profile.Game.BG1, Profile.Game.BG1TotSC, Profile.Game.PST, Profile.Game.IWD,
              Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM, Profile.Game.IWD2, Profile.Game.BG2SoA,
              Profile.Game.BG2ToB, Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE, Profile.Game.IWDEE,
              Profile.Game.PSTEE, Profile.Game.EET });
    }

    public ResInfo(StructureFactory.ResType id, String text, Profile.Game[] games) {
      resId = id;
      label = text;
      if (games != null) {
        Collections.addAll(supportedGames, games);
      }
    }

    public boolean gameSupported(Profile.Game game) {
      return supportedGames.contains(game);
    }
  }
}
