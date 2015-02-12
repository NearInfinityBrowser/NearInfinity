// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.check.BCSIDSChecker;
import infinity.check.CreInvChecker;
import infinity.check.DialogChecker;
import infinity.check.EffectsIndexChecker;
import infinity.check.IDSRefChecker;
import infinity.check.ResRefChecker;
import infinity.check.ResourceUseChecker;
import infinity.check.ScriptChecker;
import infinity.check.StringUseChecker;
import infinity.check.StructChecker;
import infinity.gui.converter.ConvertToBam;
import infinity.gui.converter.ConvertToBmp;
import infinity.gui.converter.ConvertToMos;
import infinity.gui.converter.ConvertToPvrz;
import infinity.gui.converter.ConvertToTis;
import infinity.icon.Icons;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructureFactory;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.DialogSearcher;
import infinity.search.SearchFrame;
import infinity.search.SearchResource;
import infinity.search.TextResourceSearcher;
import infinity.util.MassExporter;
import infinity.util.StringResource;
import infinity.util.io.FileNI;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class BrowserMenuBar extends JMenuBar
{
  public static final String VERSION = "v1.35.0-snapshot-20150125";
  public static final int OVERRIDE_IN_THREE = 0, OVERRIDE_IN_OVERRIDE = 1, OVERRIDE_SPLIT = 2;
  public static final LookAndFeelInfo DEFAULT_LOOKFEEL =
      new LookAndFeelInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel");
  public static final int RESREF_ONLY = 0, RESREF_REF_NAME = 1, RESREF_NAME_REF = 2;
  public static final int DEFAULT_VIEW = 0, DEFAULT_EDIT = 1;

  // Defines platform-specific shortcut key (e.g. Ctrl on Win/Linux, Meta on Mac)
  private static final int CTRL_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  private static BrowserMenuBar menuBar;

  private final EditMenu editMenu;
  private final FileMenu fileMenu;
  private final GameMenu gameMenu;
  private final OptionsMenu optionsMenu;
  private final SearchMenu searchMenu;
  private final ToolsMenu toolsMenu;
  private final HelpMenu helpMenu;

  public static BrowserMenuBar getInstance()
  {
    return menuBar;
  }

  private static JMenuItem makeMenuItem(String name, int menuKey, Icon icon, int shortKey,
                                        ActionListener listener)
  {
    JMenuItem item = new JMenuItem(name);
    if (menuKey != -1)
      item.setMnemonic(menuKey);
    if (icon != null)
      item.setIcon(icon);
    if (shortKey != -1)
      item.setAccelerator(KeyStroke.getKeyStroke(shortKey, CTRL_MASK));
    if (listener != null)
      item.addActionListener(listener);
    return item;
  }

  public BrowserMenuBar()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    gameMenu = new GameMenu(prefs);
    fileMenu = new FileMenu();
    editMenu = new EditMenu();
    searchMenu = new SearchMenu();
    toolsMenu = new ToolsMenu();
    optionsMenu = new OptionsMenu(prefs);
    helpMenu = new HelpMenu();
    add(gameMenu);
    add(fileMenu);
    add(editMenu);
    add(searchMenu);
    add(toolsMenu);
    add(optionsMenu);
    add(helpMenu);
    menuBar = this;
  }

  public boolean autocheckBCS()
  {
    return optionsMenu.optionAutocheckBCS.isSelected();
  }

  public boolean checkScriptNames()
  {
    return optionsMenu.optionCheckScriptNames.isSelected();
  }

  public boolean showStrrefs()
  {
    return optionsMenu.optionShowStrrefs.isSelected();
  }

  public boolean getHexColorMapEnabled()
  {
    return optionsMenu.optionShowHexColored.isSelected();
  }

  public boolean cacheOverride()
  {
    return optionsMenu.optionCacheOverride.isSelected();
  }

  public void gameLoaded(Profile.Game oldGame, String oldFile)
  {
    gameMenu.gameLoaded(oldGame, oldFile);
    fileMenu.gameLoaded();
    editMenu.gameLoaded();
    searchMenu.gameLoaded();
    optionsMenu.gameLoaded();
  }

  /** Returns state of "Text: Show Highlight Current Line" */
  public boolean getTextHighlightCurrentLine()
  {
    return optionsMenu.optionTextHightlightCurrent.isSelected();
  }

  /** Returns state of "Text: Show Line Numbers" */
  public boolean getTextLineNumbers()
  {
    return optionsMenu.optionTextLineNumbers.isSelected();
  }

  /** Returns state of "Text: Show Whitespace and Tab" */
  public boolean getTextWhitespaceVisible()
  {
    return optionsMenu.optionTextShowWhiteSpace.isSelected();
  }

  /** Returns state of "Text: Show End of Line" */
  public boolean getTextEOLVisible()
  {
    return optionsMenu.optionTextShowEOL.isSelected();
  }

  /** Returns the selected BCS color scheme. */
  public String getBcsColorScheme()
  {
    if (NearInfinity.isDebug() && optionsMenu.getDebugColorScheme() != null) {
      return optionsMenu.getDebugColorScheme();
    } else {
      return optionsMenu.getBcsColorScheme();
    }
  }

  /** Returns state of "BCS: Enable Syntax Highlighting" */
  public boolean getBcsSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionBCSEnableSyntax.isSelected();
  }

  /** Returns state of "BCS: Enable Code Folding" */
  public boolean getBcsCodeFoldingEnabled()
  {
    return optionsMenu.optionBCSEnableCodeFolding.isSelected();
  }

//  /** Returns state of "BCS: Enable Auto-Completion" */
//  public boolean getBcsAutoCompleteEnabled()
//  {
//    return optionsMenu.optionBCSEnableAutoComplete.isSelected();
//  }

  /** Returns the selected GLSL color scheme. */
  public String getGlslColorScheme()
  {
    if (NearInfinity.isDebug() && optionsMenu.getDebugColorScheme() != null) {
      return optionsMenu.getDebugColorScheme();
    } else {
      return optionsMenu.getGlslColorScheme();
    }
  }

  /** Returns the selected SQL color scheme. */
  public String getSqlColorScheme()
  {
    if (NearInfinity.isDebug() && optionsMenu.getDebugColorScheme() != null) {
      return optionsMenu.getDebugColorScheme();
    } else {
      return optionsMenu.getSqlColorScheme();
    }
  }

  /** Returns state of "Enable Syntax Highlighting for GLSL" */
  public boolean getGlslSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionGLSLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for SQL" */
  public boolean getSqlSyntaxHighlightingEnabled()
  {
    return optionsMenu.optionSQLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Code Folding for GLSL" */
  public boolean getGlslCodeFoldingEnabled()
  {
    return optionsMenu.optionGLSLEnableCodeFolding.isSelected();
  }

  /** Returns whether to emulate tabs by inserting spaces instead. */
  public boolean isTextTabEmulated()
  {
    return optionsMenu.optionTextTabEmulate.isSelected();
  }

  /** Returns the number of spaces used for (real or emulated) tabs. */
  public int getTextTabSize()
  {
    return 1 << (optionsMenu.getTextIndentIndex()+1);
  }

  /** Returns the selected BCS indent. */
  public String getBcsIndent()
  {
    return optionsMenu.getBcsIndent();
  }

  public int getDefaultStructView()
  {
    return optionsMenu.getDefaultStructView();
  }

  public LookAndFeelInfo getLookAndFeel()
  {
    return optionsMenu.getLookAndFeel();
  }

  public int getOverrideMode()
  {
    return optionsMenu.getOverrideMode();
  }

  public int getResRefMode()
  {
    return optionsMenu.getResRefMode();
  }

  public Font getScriptFont()
  {
    for (int i = 0; i < OptionsMenu.FONTS.length; i++)
      if (optionsMenu.selectFont[i].isSelected())
        return OptionsMenu.FONTS[i];
    return OptionsMenu.FONTS[0];
  }

  public String getSelectedCharset()
  {
    return optionsMenu.charsetName(optionsMenu.getSelectedButtonData());
  }

  public boolean backupOnSave()
  {
    return optionsMenu.optionBackupOnSave.isSelected();
  }

  public boolean ignoreOverrides()
  {
    return optionsMenu.optionIgnoreOverride.isSelected();
  }

  public boolean ignoreReadErrors()
  {
    return optionsMenu.optionIgnoreReadErrors.isSelected();
  }

  public void resourceEntrySelected(ResourceEntry entry)
  {
    fileMenu.resourceEntrySelected(entry);
  }

  public void resourceShown(Resource res)
  {
    fileMenu.resourceShown(res);
  }

  public boolean showOffsets()
  {
    return optionsMenu.optionShowOffset.isSelected();
  }

  /** Returns the language code of the selected game language (BG(2)EE only). */
  public String getSelectedGameLanguage()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    return prefs.get(OptionsMenu.OPTION_LANGUAGE_GAME, "");
  }

  public void storePreferences()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    optionsMenu.storePreferences(prefs);
    gameMenu.storePreferences(prefs);
  }


// -------------------------- INNER CLASSES --------------------------

  ///////////////////////////////
  // Game Menu
  ///////////////////////////////
  private static final class GameMenu extends JMenu implements ActionListener
  {
    private final String LASTGAME_IDS[] =
      {"LastGameID1", "LastGameID2", "LastGameID3", "LastGameID4", "LastGameID5",
       "LastGameID6", "LastGameID7", "LastGameID8", "LastGameID9", "LastGameID10"};
    private final String LASTGAME_PATH[] =
      {"LastGamePath1", "LastGamePath2", "LastGamePath3", "LastGamePath4", "LastGamePath5",
       "LastGamePath6", "LastGamePath7", "LastGamePath8", "LastGamePath9", "LastGamePath10"};
    private final JMenuItem gameOpenFile, gameOpenGame, gameRefresh, gameExit, gameCloseTLK, gameRecentClear;
    private final JMenuItem gameLastGame[] = new JMenuItem[LASTGAME_IDS.length];
    private final List<Profile.Game> lastGameID = new ArrayList<Profile.Game>();
    private final List<String> lastGamePath = new ArrayList<String>();

    private GameMenu(Preferences prefs)
    {
      super("Game");
      setMnemonic(KeyEvent.VK_G);

      gameOpenFile = makeMenuItem("Open File...", KeyEvent.VK_F, Icons.getIcon("Open16.gif"),
                                  KeyEvent.VK_I, this);
      add(gameOpenFile);
      gameOpenGame = makeMenuItem("Open Game...", KeyEvent.VK_O, Icons.getIcon("Open16.gif"),
                                  KeyEvent.VK_O, NearInfinity.getInstance());
      gameOpenGame.setActionCommand("Open");
      add(gameOpenGame);
      gameRefresh = makeMenuItem("Refresh Tree", KeyEvent.VK_R, Icons.getIcon("Refresh16.gif"),
                                 -1, NearInfinity.getInstance());
      gameRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
      gameRefresh.setActionCommand("Refresh");
      add(gameRefresh);
      gameCloseTLK = makeMenuItem("Release Dialog.tlk Lock", KeyEvent.VK_D, Icons.getIcon("Release16.gif"),
                                  -1, this);
      add(gameCloseTLK);

      addSeparator();

      // adding recently opened games list
      JMenu recentGames = new JMenu("Recently opened games");
      recentGames.setMnemonic('r');
      add(recentGames);

      for (int i = 0; i < LASTGAME_IDS.length; i++) {
        Profile.Game game = Profile.Game.valueOf(prefs.get(LASTGAME_IDS[i], Profile.Game.Unknown.toString()));
        String gamepath = prefs.get(LASTGAME_PATH[i], null);
        if (game != Profile.Game.Unknown && gamepath != null && new FileNI(gamepath).exists()) {
          lastGameID.add(game);
          lastGamePath.add(gamepath);
        }
      }
      for (int i = 0; i < LASTGAME_IDS.length; i++) {
        if (i < lastGameID.size()) {
          String label = String.format("%1$d  %2$s", i+1,
                                       (String)Profile.getProperty(Profile.GET_GLOBAL_GAME_TITLE,
                                                                   lastGameID.get(i)));
          String toolTip = lastGamePath.get(i);
          gameLastGame[i] = new JMenuItem(label);
          gameLastGame[i].setToolTipText(toolTip);
        } else {
          gameLastGame[i] = new JMenuItem(String.valueOf(i+1));
          gameLastGame[i].setEnabled(false);
        }
        gameLastGame[i].addActionListener(this);
        gameLastGame[i].setActionCommand("OpenOldGame");
        recentGames.add(gameLastGame[i]);
      }

      recentGames.addSeparator();

      gameRecentClear = new JMenuItem("Clear list of recent games");
      gameRecentClear.addActionListener(this);
      recentGames.add(gameRecentClear);

      addSeparator();

      gameExit = makeMenuItem("Quit", KeyEvent.VK_Q, Icons.getIcon("Exit16.gif"), KeyEvent.VK_Q,
                              NearInfinity.getInstance());
      gameExit.setActionCommand("Exit");
      add(gameExit);
    }

    private void gameLoaded(Profile.Game oldGame, String oldFile)
    {
      int newIndex = -1;
      for (int i = 0; i < lastGamePath.size(); i++)
        if (ResourceFactory.getKeyfile().toString().equalsIgnoreCase(lastGamePath.get(i)))
          newIndex = i;
      if (newIndex != -1) {
        lastGameID.remove(newIndex);
        lastGamePath.remove(newIndex);
      }
      if (oldGame != Profile.Game.Unknown) {
        int oldIndex = -1;
        for (int i = 0; i < lastGamePath.size(); i++)
          if (oldFile.equalsIgnoreCase(lastGamePath.get(i))) {
            oldIndex = i;
          }
        if (oldIndex != -1) {
          lastGameID.remove(oldIndex);
          lastGamePath.remove(oldIndex);
        }
        lastGameID.add(0, oldGame);
        lastGamePath.add(0, oldFile);
      }
      while (lastGameID.size() > LASTGAME_IDS.length) {
        lastGamePath.remove(lastGameID.size() - 1);
        lastGameID.remove(lastGameID.size() - 1);
      }
      if (newIndex != 1 || oldGame != Profile.Game.Unknown) {
        for (int i = 0; i < lastGameID.size(); i++) {
          gameLastGame[i].setText(String.format("%1$d %2$s", i+1, (String)Profile.getProperty(Profile.GET_GLOBAL_GAME_TITLE, lastGameID.get(i))));
          gameLastGame[i].setToolTipText(lastGamePath.get(i));
          gameLastGame[i].setEnabled(true);
        }
        for (int i = lastGameID.size(); i < LASTGAME_IDS.length; i++) {
          gameLastGame[i].setText(String.valueOf(i + 1));
          gameLastGame[i].setEnabled(false);
        }
      }
    }

    private void storePreferences(Preferences prefs)
    {
      for (int i = 0; i < LASTGAME_IDS.length; i++) {
        if (i < lastGameID.size()) {
          prefs.put(LASTGAME_IDS[i], lastGameID.get(i).toString());
          prefs.put(LASTGAME_PATH[i], lastGamePath.get(i));
        } else {
          prefs.remove(LASTGAME_IDS[i]);
          prefs.remove(LASTGAME_PATH[i]);
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == gameOpenFile) {
        OpenFileFrame openframe = (OpenFileFrame)ChildFrame.getFirstFrame(OpenFileFrame.class);
        if (openframe == null)
          openframe = new OpenFileFrame();
        openframe.setVisible(true);
      }
      else if (event.getActionCommand().equals("OpenOldGame")) {
        int selected = -1;
        for (int i = 0; i < gameLastGame.length; i++)
          if (event.getSource() == gameLastGame[i])
            selected = i;
        File keyfile = new FileNI(lastGamePath.get(selected));
        if (!keyfile.exists())
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), lastGamePath.get(selected) +
                                                                    " could not be found",
                                        "Open game failed", JOptionPane.ERROR_MESSAGE);
        else
          NearInfinity.getInstance().openGame(keyfile);
      }
      else if (event.getSource() == gameCloseTLK) {
        StringResource.close();
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Read lock released",
                                      "Release Dialog.tlk", JOptionPane.INFORMATION_MESSAGE);
      }
      else if (event.getSource() == gameRecentClear) {
        for (int i = 0; i < LASTGAME_IDS.length; i++) {
          gameLastGame[i].setText(String.valueOf(i+1));
          gameLastGame[i].setEnabled(false);
        }
        lastGameID.clear();
        lastGamePath.clear();
      }
    }
  }

  ///////////////////////////////
  // File Menu
  ///////////////////////////////

  private static final class FileMenu extends JMenu implements ActionListener
  {
    private static final class ResInfo {
      public final String label;
      public final StructureFactory.ResType resId;
      private final EnumSet<Profile.Game> supportedGames = EnumSet.noneOf(Profile.Game.class);

      public ResInfo(StructureFactory.ResType id, String text) {
        this(id, text, new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC, Profile.Game.PST,
                                          Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowToTLM,
                                          Profile.Game.IWD2, Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                          Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE});
      }

      public ResInfo(StructureFactory.ResType id, String text, Profile.Game[] games) {
        resId = id;
        label = text;
        if (games != null)
          for (final Profile.Game g : games) {
            supportedGames.add(g);
          }
      }

      public boolean gameSupported(Profile.Game game)
      {
        return supportedGames.contains(game);
      }
    }

    private static final ResInfo RESOURCE[] = {
      new ResInfo(StructureFactory.ResType.RES_2DA, "2DA"),
      new ResInfo(StructureFactory.ResType.RES_ARE, "ARE"),
      new ResInfo(StructureFactory.ResType.RES_BAF, "BAF"),
      new ResInfo(StructureFactory.ResType.RES_BCS, "BCS"),
      new ResInfo(StructureFactory.ResType.RES_BIO, "BIO",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_CHR, "CHR",
                  new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC,
                                     Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowToTLM,
                                     Profile.Game.IWD2, Profile.Game.BG1EE, Profile.Game.BG2EE,
                                     Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_CRE, "CRE"),
      new ResInfo(StructureFactory.ResType.RES_EFF, "EFF",
                  new Profile.Game[]{Profile.Game.BG1, Profile.Game.BG1TotSC,
                                     Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_IDS, "IDS"),
      new ResInfo(StructureFactory.ResType.RES_ITM, "ITM"),
      new ResInfo(StructureFactory.ResType.RES_INI, "INI",
                  new Profile.Game[]{Profile.Game.PST, Profile.Game.IWD, Profile.Game.IWDHoW,
                                     Profile.Game.IWDHowToTLM, Profile.Game.IWD2}),
      new ResInfo(StructureFactory.ResType.RES_PRO, "PRO",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_RES, "RES",
                  new Profile.Game[]{Profile.Game.IWD, Profile.Game.IWDHoW, Profile.Game.IWDHowToTLM,
                                     Profile.Game.IWD2}),
      new ResInfo(StructureFactory.ResType.RES_SPL, "SPL"),
      new ResInfo(StructureFactory.ResType.RES_SRC, "SRC",
                  new Profile.Game[]{Profile.Game.PST, Profile.Game.IWD2}),
      new ResInfo(StructureFactory.ResType.RES_STO, "STO"),
      new ResInfo(StructureFactory.ResType.RES_VEF, "VEF",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_VVC, "VVC",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_WED, "WED"),
      new ResInfo(StructureFactory.ResType.RES_WFX, "WFX",
                  new Profile.Game[]{Profile.Game.BG2SoA, Profile.Game.BG2ToB,
                                     Profile.Game.BG1EE, Profile.Game.BG2EE, Profile.Game.IWDEE}),
      new ResInfo(StructureFactory.ResType.RES_WMAP, "WMAP"),
    };

    private final JMenu newFileMenu;
    private final JMenuItem fileOpenNew, fileExport, fileAddCopy, fileRename, fileDelete, fileRestore;

    private FileMenu()
    {
      super("File");
      setMnemonic(KeyEvent.VK_F);

      newFileMenu = new JMenu("New Resource");
      newFileMenu.setIcon(Icons.getIcon("New16.gif"));
      newFileMenu.setMnemonic(KeyEvent.VK_N);
      add(newFileMenu);
      fileOpenNew = makeMenuItem("Open in New Window", KeyEvent.VK_W, Icons.getIcon("Open16.gif"), -1, this);
      fileOpenNew.setEnabled(false);
      add(fileOpenNew);
      fileExport = makeMenuItem("Export...", KeyEvent.VK_E, Icons.getIcon("Export16.gif"), -1, this);
      fileExport.setEnabled(false);
      add(fileExport);
      fileAddCopy = makeMenuItem("Add Copy Of...", KeyEvent.VK_A, Icons.getIcon("Add16.gif"), -1, this);
      fileAddCopy.setEnabled(false);
      add(fileAddCopy);
      fileRename = makeMenuItem("Rename...", KeyEvent.VK_R, Icons.getIcon("Edit16.gif"), -1, this);
      fileRename.setEnabled(false);
      add(fileRename);
      fileDelete = makeMenuItem("Delete", KeyEvent.VK_D, Icons.getIcon("Delete16.gif"), -1, this);
      fileDelete.setEnabled(false);
      add(fileDelete);
      fileRestore = makeMenuItem("Restore backup", KeyEvent.VK_B, Icons.getIcon("Undo16.gif"), -1, this);
      fileRestore.setEnabled(false);
      add(fileRestore);
    }

    private void gameLoaded()
    {
      if (newFileMenu != null) {
        newFileMenu.removeAll();

        for (final ResInfo res : RESOURCE) {
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

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == fileOpenNew) {
        Resource res = ResourceFactory.getResource(
                NearInfinity.getInstance().getResourceTree().getSelected());
        if (res != null)
          new ViewFrame(NearInfinity.getInstance(), res);
      } else if (event.getSource() == fileExport) {
        ResourceFactory.exportResource(NearInfinity.getInstance().getResourceTree().getSelected(),
                                       NearInfinity.getInstance());
      } else if (event.getSource() == fileAddCopy) {
        ResourceFactory.saveCopyOfResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else if (event.getSource() == fileRename) {
        if (NearInfinity.getInstance().getResourceTree().getSelected() instanceof FileResourceEntry) {
          ResourceTree.renameResource((FileResourceEntry)NearInfinity.getInstance().getResourceTree().getSelected());
        }
      } else if (event.getSource() == fileDelete) {
        ResourceTree.deleteResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else if (event.getSource() == fileRestore) {
        ResourceTree.restoreResource(NearInfinity.getInstance().getResourceTree().getSelected());
      } else {
        for (final ResInfo res : RESOURCE) {
          if (event.getActionCommand().equals(res.label)) {
            StructureFactory.getInstance().newResource(res.resId, NearInfinity.getInstance());
          }
        }
      }
    }

    private void resourceEntrySelected(ResourceEntry entry)
    {
      fileOpenNew.setEnabled(entry != null);
      fileExport.setEnabled(entry != null);
      fileAddCopy.setEnabled(entry != null);
      fileRename.setEnabled(entry instanceof FileResourceEntry);
      fileDelete.setEnabled((entry != null && entry.hasOverride()) || entry instanceof FileResourceEntry);
      fileRestore.setEnabled(ResourceTree.isBackupAvailable(entry));
    }

    private void resourceShown(Resource res)
    {
      // not used anymore
    }
  }

  ///////////////////////////////
  // Edit Menu
  ///////////////////////////////

  private static final class EditMenu extends JMenu implements ActionListener
  {
    private final JMenuItem editString, editString2, editBIFF, editVarVar;

    private EditMenu()
    {
      super("Edit");
      setMnemonic(KeyEvent.VK_E);

      editString =
      makeMenuItem("Dialog.tlk", KeyEvent.VK_D, Icons.getIcon("Edit16.gif"), KeyEvent.VK_S, this);
      add(editString);
      editString2 = makeMenuItem("DialogF.tlk", KeyEvent.VK_F, Icons.getIcon("Edit16.gif"), -1, this);
      add(editString2);
      editVarVar = makeMenuItem("Var.var", KeyEvent.VK_V, Icons.getIcon("RowInsertAfter16.gif"), -1, this);
      add(editVarVar);
      editBIFF = makeMenuItem("BIFF", KeyEvent.VK_B, Icons.getIcon("Edit16.gif"), KeyEvent.VK_E, this);
      add(editBIFF);
    }

    private void gameLoaded()
    {
      editString2.setEnabled(Profile.getProperty(Profile.GET_GAME_DIALOGF_FILE) != null);
      editVarVar.setEnabled(FileNI.getFile(Profile.getRootFolders(), "VAR.VAR").isFile());
      if (editString2.isEnabled())
        editString2.setToolTipText("");
      else
        editString2.setToolTipText("DialogF.tlk not found");
      if (editVarVar.isEnabled())
        editVarVar.setToolTipText("");
      else
        editVarVar.setToolTipText("Only available for Planescape: Torment");
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == editString) {
        StringEditor editor = null;
        List<ChildFrame> frames = ChildFrame.getFrames(StringEditor.class);
        for (int i = 0; i < frames.size(); i++) {
          StringEditor e = (StringEditor)frames.get(i);
          if (e.getFile().equals(StringResource.getFile()))
            editor = e;
        }
        if (editor == null)
          new StringEditor(StringResource.getFile(), 0);
        else
          editor.setVisible(true);
      }
      else if (event.getSource() == editString2) {
        StringEditor editor = null;
        File file = (File)Profile.getProperty(Profile.GET_GAME_DIALOGF_FILE);
        List<ChildFrame> frames = ChildFrame.getFrames(StringEditor.class);
        for (int i = 0; i < frames.size(); i++) {
          StringEditor e = (StringEditor)frames.get(i);
          if (e.getFile().equals(file))
            editor = e;
        }
        if (editor == null)
          new StringEditor(file, 0);
        else
          editor.setVisible(true);
      }
      else if (event.getSource() == editVarVar) {
        new ViewFrame(NearInfinity.getInstance(),
                      ResourceFactory.getResource(
                              new FileResourceEntry(
                                  FileNI.getFile(Profile.getRootFolders(), "VAR.VAR"))));
      }
      else if (event.getSource() == editBIFF)
        new BIFFEditor();
    }
  }

  ///////////////////////////////
  // Search Menu
  ///////////////////////////////

  private static final class SearchMenu extends JMenu implements ActionListener
  {
    private final String TEXTSEARCH[] = {"2DA", "BCS", "DLG", "IDS", "INI"};
    private final JMenuItem searchString, searchFile, searchResource;
    private final JMenu textSearchMenu;

    private SearchMenu()
    {
      super("Search");
      setMnemonic(KeyEvent.VK_S);

      searchString =
          makeMenuItem("StringRef...", KeyEvent.VK_S, Icons.getIcon("Find16.gif"), KeyEvent.VK_L, this);
      add(searchString);
      searchFile =
          makeMenuItem("CRE/ITM/SPL/STO...", KeyEvent.VK_C, Icons.getIcon("Find16.gif"), KeyEvent.VK_F, this);
      add(searchFile);
      searchResource =
          makeMenuItem("Extended search...", KeyEvent.VK_X, Icons.getIcon("Find16.gif"), -1, this);
      searchResource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
          Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.ALT_MASK));
      add(searchResource);

      textSearchMenu = new JMenu("Text Search");
      textSearchMenu.setIcon(Icons.getIcon("Edit16.gif"));
      for (final String type : TEXTSEARCH) {
        JMenuItem textSearch = new JMenuItem(type);
        textSearch.addActionListener(this);
        textSearch.setActionCommand(type);
        textSearchMenu.add(textSearch);
      }
      add(textSearchMenu);
    }

    private void gameLoaded()
    {
      // Enable INI search only if the game is supporting it
      for (int i = 0, count = textSearchMenu.getMenuComponentCount(); i < count; i++) {
        if (textSearchMenu.getMenuComponent(i) instanceof JMenuItem) {
          JMenuItem mi = (JMenuItem)textSearchMenu.getMenuComponent(i);
          if ("INI".equals(mi.getText())) {
            if ((Boolean)Profile.getProperty(Profile.IS_SUPPORTED_INI)) {
              mi.setEnabled(true);
            } else {
              mi.setEnabled(false);
            }
          }
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == searchString) {
        StringLookup lookup = (StringLookup)ChildFrame.getFirstFrame(StringLookup.class);
        if (lookup == null)
          lookup = new StringLookup();
        lookup.setVisible(true);
      }
      else if (event.getSource() == searchFile) {
        SearchFrame search = (SearchFrame)ChildFrame.getFirstFrame(SearchFrame.class);
        if (search == null)
          search = new SearchFrame();
        search.setVisible(true);
      }
      else if (event.getSource() == searchResource) {
        SearchResource resource = (SearchResource)ChildFrame.getFirstFrame(SearchResource.class);
        if (resource == null) {
          resource = new SearchResource();
        }
        resource.setVisible(true);
      }
      else {
        for (final String type : TEXTSEARCH) {
          if (event.getActionCommand().equals(type)) {
            if (event.getActionCommand().equals("DLG")) {
              new DialogSearcher(ResourceFactory.getResources(type),
                                 getTopLevelAncestor());
            } else {
              new TextResourceSearcher(ResourceFactory.getResources(type),
                                       getTopLevelAncestor());
            }
            return;
          }
        }
      }
    }
  }

  ///////////////////////////////
  // Tools Menu
  ///////////////////////////////

  private static final class ToolsMenu extends JMenu implements ActionListener
  {
    private final JMenuItem toolInfinityAmp, toolCleanKeyfile, toolCheckAllDialog, toolCheckOverrideDialog;
    private final JMenuItem toolCheckResRef, toolIDSBrowser, toolDropZone, toolCheckCREInv;
    private final JMenuItem toolCheckIDSRef, toolCheckIDSBCSRef, toolCheckScripts, toolCheckStructs;
    private final JMenuItem toolCheckStringUse, toolCheckFileUse, toolMassExport;
    private final JMenuItem toolCheckEffectsIndex;
    private final JMenuItem toolConvImageToBam, toolConvImageToBmp, toolConvImageToMos, toolConvImageToTis,
                            toolConvImageToPvrz;
//    private JMenuItem toolBatchJob;
    private final JCheckBoxMenuItem toolConsole, toolClipBoard;

    private ToolsMenu()
    {
      super("Tools");
      setMnemonic(KeyEvent.VK_T);

      toolInfinityAmp = makeMenuItem("InfinityAmp", KeyEvent.VK_I, Icons.getIcon("Volume16.gif"), -1, this);
      add(toolInfinityAmp);

      addSeparator();

      toolCleanKeyfile =
          makeMenuItem("Keyfile Cleanup", KeyEvent.VK_K, Icons.getIcon("Refresh16.gif"), -1, this);
      add(toolCleanKeyfile);

      addSeparator();

      // *** Begin Check submenu ***
      JMenu checkMenu = new JMenu("Check");
//      checkMenu.setIcon(Icons.getIcon("Find16.gif"));
      checkMenu.setMnemonic('c');
      add(checkMenu);

      JMenu checkSubMenu = new JMenu("Triggers & Actions For");
      checkSubMenu.setIcon(Icons.getIcon("Refresh16.gif"));
      toolCheckAllDialog = new JMenuItem("All Dialogues");
      toolCheckAllDialog.addActionListener(this);
      checkSubMenu.add(toolCheckAllDialog);
      toolCheckOverrideDialog = new JMenuItem("Override Dialogues Only");
      toolCheckOverrideDialog.addActionListener(this);
      checkSubMenu.add(toolCheckOverrideDialog);
      checkMenu.add(checkSubMenu);

      toolCheckScripts =
          makeMenuItem("Scripts", KeyEvent.VK_S, Icons.getIcon("Refresh16.gif"), -1, this);
      checkMenu.add(toolCheckScripts);

      toolCheckCREInv =
          makeMenuItem("For CRE Items Not in Inventory", KeyEvent.VK_C, Icons.getIcon("Refresh16.gif"),
                       -1, this);
      toolCheckCREInv.setToolTipText("Reports items present in the file but not in the inventory");
      checkMenu.add(toolCheckCREInv);

      toolCheckResRef =
          makeMenuItem("For Illegal ResourceRefs...", KeyEvent.VK_R, Icons.getIcon("Find16.gif"), -1, this);
      toolCheckResRef.setToolTipText("Reports resource references pointing to nonexistent files");
      checkMenu.add(toolCheckResRef);

      JMenu findMenu = new JMenu("For Unknown IDS References In");
      findMenu.setIcon(Icons.getIcon("Find16.gif"));
      toolCheckIDSBCSRef = new JMenuItem("BCS & BS Files");
      toolCheckIDSBCSRef.addActionListener(this);
      findMenu.add(toolCheckIDSBCSRef);
      toolCheckIDSRef = new JMenuItem("Other Files...");
      toolCheckIDSRef.addActionListener(this);
      findMenu.add(toolCheckIDSRef);
      checkMenu.add(findMenu);
      findMenu.setToolTipText("Reports IDS references to unknown IDS values");
      toolCheckIDSBCSRef.setToolTipText("Note: GTimes, Time, Scroll, ShoutIDs, and Specific are ignored");
      toolCheckIDSRef.setToolTipText("Note: \"0\" references are ignored");

      toolCheckStructs =
          makeMenuItem("For Corrupted Files...", KeyEvent.VK_F, Icons.getIcon("Find16.gif"), -1, this);
      toolCheckStructs.setToolTipText("Reports structured files with partially overlapping subsections");
      checkMenu.add(toolCheckStructs);

      toolCheckStringUse =
          makeMenuItem("For Unused Strings", KeyEvent.VK_U, Icons.getIcon("Find16.gif"), -1, this);
      checkMenu.add(toolCheckStringUse);

      toolCheckFileUse = makeMenuItem("For Unused Files...", -1, Icons.getIcon("Find16.gif"), -1, this);
      checkMenu.add(toolCheckFileUse);

      toolCheckEffectsIndex =
          makeMenuItem("For Mis-indexed Effects", -1, Icons.getIcon("Find16.gif"), -1, this);
      checkMenu.add(toolCheckEffectsIndex);
      // *** End Check submenu ***

      // *** Begin Convert submenu ***
      JMenu convertMenu = new JMenu("Convert");
      convertMenu.setMnemonic('v');
      add(convertMenu);

      toolConvImageToBam =
          makeMenuItem("BAM Converter...", KeyEvent.VK_B, Icons.getIcon("Export16.gif"), -1, this);
      convertMenu.add(toolConvImageToBam);

      toolConvImageToBmp =
          makeMenuItem("Image to BMP...", KeyEvent.VK_I, Icons.getIcon("Export16.gif"), -1, this);
      convertMenu.add(toolConvImageToBmp);

      toolConvImageToMos =
          makeMenuItem("Image to MOS...", KeyEvent.VK_M, Icons.getIcon("Export16.gif"), -1, this);
      convertMenu.add(toolConvImageToMos);

      toolConvImageToPvrz =
          makeMenuItem("Image to PVRZ...", KeyEvent.VK_P, Icons.getIcon("Export16.gif"), -1, this);
      convertMenu.add(toolConvImageToPvrz);

      toolConvImageToTis =
          makeMenuItem("Image to TIS...", KeyEvent.VK_T, Icons.getIcon("Export16.gif"), -1, this);
      convertMenu.add(toolConvImageToTis);
      // *** End Convert submenu ***

      addSeparator();

      toolIDSBrowser =
          makeMenuItem("IDS Browser", KeyEvent.VK_B, Icons.getIcon("History16.gif"), KeyEvent.VK_B, this);
      add(toolIDSBrowser);
      toolDropZone =
          makeMenuItem("Script Drop Zone", KeyEvent.VK_Z, Icons.getIcon("History16.gif"), KeyEvent.VK_Z, this);
      add(toolDropZone);

      addSeparator();

      toolMassExport =
          makeMenuItem("Mass Export...", KeyEvent.VK_M, Icons.getIcon("Export16.gif"), -1, this);
      add(toolMassExport);

      addSeparator();

      toolClipBoard = new JCheckBoxMenuItem("Show Clipboard", Icons.getIcon("Paste16.gif"));
      toolClipBoard.addActionListener(this);
      add(toolClipBoard);
      toolConsole = new JCheckBoxMenuItem("Show Debug Console", Icons.getIcon("Properties16.gif"));
      toolConsole.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, CTRL_MASK));
      toolConsole.addActionListener(this);
      add(toolConsole);

//      toolBatchJob = makeMenuItem("Batch jobs", KeyEvent.VK_J, ResourceFactory.getIcon("History16.gif"), KeyEvent.VK_J, this);
//      add(toolBatchJob);
    }

    private static void cleanKeyfile()
    {
      JLabel infolabel = new JLabel("<html><center>This will delete empty BIFFs and remove<br>" +
                                    "references to nonexistent BIFFs.<br><br>" +
                                    "Warning: Your existing " + ResourceFactory.getKeyfile() +
                                    " will be overwritten!<br><br>Continue?</center></html>");
      String options[] = {"Continue", "Cancel"};
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), infolabel,
                                       "Keyfile cleanup", JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return;
      boolean updated = ResourceFactory.getKeyfile().cleanUp();
      if (!updated)
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No cleanup necessary", "Cleanup completed",
                                      JOptionPane.INFORMATION_MESSAGE);
      else {
        try {
          ResourceFactory.getKeyfile().write();
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation completed successfully", "Cleanup completed",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error writing keyfile", "Error",
                                        JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == toolInfinityAmp) {
        InfinityAmp infAmp = (InfinityAmp)ChildFrame.getFirstFrame(InfinityAmp.class);
        if (infAmp == null)
          infAmp = new InfinityAmp();
        infAmp.setVisible(true);
      }
      else if (event.getSource() == toolIDSBrowser) {
        IdsBrowser browser = (IdsBrowser)ChildFrame.getFirstFrame(IdsBrowser.class);
        if (browser == null)
          browser = new IdsBrowser();
        browser.setVisible(true);
      }
      else if (event.getSource() == toolClipBoard) {
        ClipboardViewer viewer = (ClipboardViewer)ChildFrame.getFirstFrame(
                ClipboardViewer.class);
        if (viewer == null) {
          viewer = new ClipboardViewer();
          viewer.addWindowListener(new WindowAdapter()
          {
            @Override
            public void windowClosing(WindowEvent e)
            {
              toolClipBoard.setSelected(false);
            }
          });
        }
        viewer.setVisible(toolClipBoard.isSelected());
      }
      else if (event.getSource() == toolConsole) {
        DebugConsole console = (DebugConsole)ChildFrame.getFirstFrame(DebugConsole.class);
        if (console == null) {
          console = new DebugConsole();
          console.addWindowListener(new WindowAdapter()
          {
            @Override
            public void windowClosing(WindowEvent e)
            {
              toolConsole.setSelected(false);
            }
          });
        }
        console.setVisible(toolConsole.isSelected());
      }
      else if (event.getSource() == toolCleanKeyfile)
        cleanKeyfile();
      else if (event.getSource() == toolDropZone) {
        BcsDropFrame bcsframe = (BcsDropFrame)ChildFrame.getFirstFrame(BcsDropFrame.class);
        if (bcsframe == null)
          bcsframe = new BcsDropFrame();
        bcsframe.setVisible(true);
      }
      else if (event.getSource() == toolCheckAllDialog)
        new DialogChecker(false);
      else if (event.getSource() == toolCheckOverrideDialog)
        new DialogChecker(true);
      else if (event.getSource() == toolCheckResRef)
        new ResRefChecker();
      else if (event.getSource() == toolCheckCREInv)
        new CreInvChecker();
      else if (event.getSource() == toolCheckIDSRef)
        new IDSRefChecker();
      else if (event.getSource() == toolCheckIDSBCSRef)
        new BCSIDSChecker();
      else if (event.getSource() == toolCheckScripts)
        new ScriptChecker();
      else if (event.getSource() == toolCheckStructs)
        new StructChecker();
      else if (event.getSource() == toolCheckStringUse)
        new StringUseChecker();
      else if (event.getSource() == toolCheckFileUse)
        new ResourceUseChecker(NearInfinity.getInstance());
      else if (event.getSource() == toolMassExport)
        new MassExporter();
      else if (event.getSource() == toolCheckEffectsIndex)
        new EffectsIndexChecker();
      else if (event.getSource() == toolConvImageToPvrz) {
        ConvertToPvrz dlg = (ConvertToPvrz)ChildFrame.getFirstFrame(ConvertToPvrz.class);
        if (dlg == null) {
          dlg = new ConvertToPvrz();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToTis) {
        ConvertToTis dlg = (ConvertToTis)ChildFrame.getFirstFrame(ConvertToTis.class);
        if (dlg == null) {
          dlg = new ConvertToTis();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToMos) {
        ConvertToMos dlg = (ConvertToMos)ChildFrame.getFirstFrame(ConvertToMos.class);
        if (dlg == null) {
          dlg = new ConvertToMos();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToBmp) {
        ConvertToBmp dlg = (ConvertToBmp)ChildFrame.getFirstFrame(ConvertToBmp.class);
        if (dlg == null) {
          dlg = new ConvertToBmp();
        }
        dlg.setVisible(true);
      } else if (event.getSource() == toolConvImageToBam) {
        ConvertToBam dlg = (ConvertToBam)ChildFrame.getFirstFrame(ConvertToBam.class);
        if (dlg == null) {
          dlg = new ConvertToBam();
        }
        dlg.setVisible(true);
      }
    }
  }

  ///////////////////////////////
  // Options Menu
  ///////////////////////////////

  private static final class OptionsMenu extends JMenu implements ActionListener, ItemListener
  {
    private static final Font[] FONTS = {
      new Font(Font.MONOSPACED, Font.PLAIN, 12), new Font(Font.SERIF, Font.PLAIN, 12),
      new Font(Font.SANS_SERIF, Font.PLAIN, 12), new Font(Font.DIALOG, Font.PLAIN, 12)};
    private static final String DefaultCharset = "Auto";
    private static final List<String[]> CharsetsUsed = new ArrayList<String[]>();
    // BCS indentations to use when decompiling (indent, title)
    private static final String[][] BCSINDENT = {
      new String[]{"  ", "2 Spaces"}, new String[]{"    ", "4 Spaces"}, new String[]{"\t", "Tab"},
    };
    // Available color schemes for highlighted BCS format (scheme, title, description)
    private static final String[][] BCSCOLORSCHEME = {
      new String[]{InfinityTextArea.SchemeDefault, "Default", "A general-purpose default color scheme"},
      new String[]{InfinityTextArea.SchemeDark, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme"},
      new String[]{InfinityTextArea.SchemeEclipse, "Eclipse", "Mimics Eclipse's default color scheme"},
      new String[]{InfinityTextArea.SchemeIdea, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme"},
      new String[]{InfinityTextArea.SchemeVs, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme"},
      new String[]{InfinityTextArea.SchemeBCS, "BCS Light", "A color scheme which is loosely based on the WeiDU Syntax Highlighter for Notepad++"},
//      new String[]{null, "External color scheme...", "Use an external color scheme definition file"},
    };
    // Available color schemes for remaining highlighted formats (scheme, title, description)
    private static final String[][] COLORSCHEME = {
      new String[]{InfinityTextArea.SchemeDefault, "Default", "A general-purpose default color scheme"},
      new String[]{InfinityTextArea.SchemeDark, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme"},
      new String[]{InfinityTextArea.SchemeEclipse, "Eclipse", "Mimics Eclipse's default color scheme"},
      new String[]{InfinityTextArea.SchemeIdea, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme"},
      new String[]{InfinityTextArea.SchemeVs, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme"},
//      new String[]{null, "External color scheme...", "Use an external color scheme definition file"},
    };

    static {
      // Order: Display name, Canonical charset name, Tooltip
      CharsetsUsed.add(new String[]{"UTF-8", "UTF-8", "The character set of choice for the Enhanced Editions of the Baldur's Gate games."});
      CharsetsUsed.add(new String[]{"Windows-1252", "windows-1252", "Character set used in english and other latin-based languages, such as french, german, italian or spanish."});
      CharsetsUsed.add(new String[]{"Windows-1251", "windows-1251", "Character set used in russian and other cyrillic-based languages."});
      CharsetsUsed.add(new String[]{"Windows-1250", "windows-1250", "Character set used in central european and eastern european languages, such as polish or czech."});
      CharsetsUsed.add(new String[]{"Windows-31J", "windows-31j", "Character set used in japanese localizations."});
      CharsetsUsed.add(new String[]{"GBK", "GBK", "Character set for Simplified Chinese text."});
      CharsetsUsed.add(new String[]{"Big5-HKSCS", "Big5-HKSCS", "Character set for Traditional Chinese text (may not be fully compatible)."});
      CharsetsUsed.add(new String[]{"IBM-949", "x-IBM949", "Character set used in korean localizations."});
    }

    private static final String OPTION_SHOWOFFSETS              = "ShowOffsets";
    private static final String OPTION_BACKUPONSAVE             = "BackupOnSave";
    private static final String OPTION_IGNOREOVERRIDE           = "IgnoreOverride";
    private static final String OPTION_IGNOREREADERRORS         = "IgnoreReadErrors";
    private static final String OPTION_AUTOCHECK_BCS            = "AutocheckBCS";
    private static final String OPTION_CACHEOVERRIDE            = "CacheOverride";
    private static final String OPTION_CHECKSCRIPTNAMES         = "CheckScriptNames";
    private static final String OPTION_SHOWSTRREFS              = "ShowStrrefs";
    private static final String OPTION_SHOWHEXCOLORED           = "ShowHexColored";
    private static final String OPTION_SHOWOVERRIDES            = "ShowOverridesIn";
    private static final String OPTION_SHOWRESREF               = "ShowResRef";
    private static final String OPTION_LOOKANDFEELCLASS         = "LookAndFeelClass";
    private static final String OPTION_VIEWOREDITSHOWN          = "ViewOrEditShown";
    private static final String OPTION_FONT                     = "Font";
    private static final String OPTION_TLKCHARSET               = "TLKCharsetType";
    private static final String OPTION_LANGUAGE_GAME            = "GameLanguage";
    private static final String OPTION_TEXT_SHOWCURRENTLINE     = "TextShowCurrentLine";
    private static final String OPTION_TEXT_SHOWLINENUMBERS     = "TextShowLineNumbers";
    private static final String OPTION_TEXT_SYMBOLWHITESPACE    = "TextShowWhiteSpace";
    private static final String OPTION_TEXT_SYMBOLEOL           = "TextShowEOL";
    private static final String OPTION_TEXT_TABSEMULATED        = "TextTabsEmulated";
    private static final String OPTION_TEXT_TABSIZE             = "TextTabSize";
    private static final String OPTION_BCS_SYNTAXHIGHLIGHTING   = "BcsSyntaxHighlighting";
    private static final String OPTION_BCS_COLORSCHEME          = "BcsColorScheme";
    private static final String OPTION_BCS_CODEFOLDING          = "BcsCodeFolding";
//    private static final String OPTION_BCS_AUTOCOMPLETE         = "BcsAutoComplete";
    private static final String OPTION_BCS_INDENT               = "BcsIndent";
    private static final String OPTION_GLSL_SYNTAXHIGHLIGHTING  = "GlslSyntaxHighlighting";
    private static final String OPTION_GLSL_COLORSCHEME         = "GlslColorScheme";
    private static final String OPTION_GLSL_CODEFOLDING         = "GlslCodeFolding";
    private static final String OPTION_SQL_SYNTAXHIGHLIGHTING   = "SqlSyntaxHighlighting";
    private static final String OPTION_SQL_COLORSCHEME          = "SqlColorScheme";
    private static final String OPTION_TEXT_DEBUG_ENABLECOLORSCHEME = "DebugColorSchemeEnabled";
    private static final String OPTION_TEXT_DEBUG_COLORSCHEME       = "DebugColorSchemeFile";

    // For debugging purposes only
    private static String DEBUGCOLORSCHEME = "";

    private final List<DataRadioButtonMenuItem> lookAndFeel = new ArrayList<DataRadioButtonMenuItem>();

    private final JRadioButtonMenuItem showOverrides[] = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem showResRef[] = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem viewOrEditShown[] = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem selectFont[] = new JRadioButtonMenuItem[FONTS.length];
    private final JRadioButtonMenuItem selectTextTabSize[] = new JRadioButtonMenuItem[3];
    private final JRadioButtonMenuItem selectBcsIndent[] = new JRadioButtonMenuItem[BCSINDENT.length];
    private final JRadioButtonMenuItem selectBcsColorScheme[] = new JRadioButtonMenuItem[BCSCOLORSCHEME.length];
    private final JRadioButtonMenuItem selectGlslColorScheme[] = new JRadioButtonMenuItem[COLORSCHEME.length];
    private final JRadioButtonMenuItem selectSqlColorScheme[] = new JRadioButtonMenuItem[COLORSCHEME.length];
    private JCheckBoxMenuItem optionTextHightlightCurrent, optionTextLineNumbers,
                              optionTextShowWhiteSpace, optionTextShowEOL, optionTextTabEmulate,
                              optionBCSEnableSyntax, optionBCSEnableCodeFolding,
                              optionGLSLEnableSyntax, optionSQLEnableSyntax,
//                              optionBCSEnableAutoComplete,
                              optionGLSLEnableCodeFolding,
                              optionTextDebugColorSchemeEnabled;
    private JMenuItem optionTextDebugColorSchemeSelect;

    private JCheckBoxMenuItem optionBackupOnSave, optionShowOffset, optionIgnoreOverride;
    private JCheckBoxMenuItem optionIgnoreReadErrors, optionAutocheckBCS, optionCacheOverride;
    private JCheckBoxMenuItem optionCheckScriptNames, optionShowStrrefs, optionShowHexColored;
    private final JMenu mCharsetMenu, mLanguageMenu;
    private ButtonGroup bgCharsetButtons;

    // Stores available languages in BG(2)EE
    private final HashMap<JRadioButtonMenuItem, String> gameLanguage = new HashMap<JRadioButtonMenuItem, String>();

    private OptionsMenu(Preferences prefs)
    {
      super("Options");
      setMnemonic(KeyEvent.VK_O);

      // Options
      optionBackupOnSave =
          new JCheckBoxMenuItem("Backup on save", prefs.getBoolean(OPTION_BACKUPONSAVE, false));
      optionBackupOnSave.setToolTipText("Enable this option to automatically create a backup " +
                                        "of the resource you want to save.");
      add(optionBackupOnSave);
      optionIgnoreOverride =
          new JCheckBoxMenuItem("Ignore Overrides", prefs.getBoolean(OPTION_IGNOREOVERRIDE, false));
      add(optionIgnoreOverride);
      optionIgnoreReadErrors =
          new JCheckBoxMenuItem("Ignore Read Errors", prefs.getBoolean(OPTION_IGNOREREADERRORS, false));
      add(optionIgnoreReadErrors);
      optionShowOffset =
          new JCheckBoxMenuItem("Show Hex Offsets", prefs.getBoolean(OPTION_SHOWOFFSETS, false));
      add(optionShowOffset);
      optionAutocheckBCS =
          new JCheckBoxMenuItem("Autocheck BCS", prefs.getBoolean(OPTION_AUTOCHECK_BCS, true));
      add(optionAutocheckBCS);
      optionCacheOverride =
          new JCheckBoxMenuItem("Autocheck for Overrides", prefs.getBoolean(OPTION_CACHEOVERRIDE, false));
      optionCacheOverride.setToolTipText("Without this option selected, Refresh Tree is required " +
                                         "to discover new override files added while NI is open");
      add(optionCacheOverride);
      optionCheckScriptNames =
          new JCheckBoxMenuItem("Interactive script names", prefs.getBoolean(OPTION_CHECKSCRIPTNAMES, true));
      optionCheckScriptNames.setToolTipText("With this option disabled, performance may be boosted " +
                                            "but many features involving script names will be disabled.");
      add(optionCheckScriptNames);
      optionShowStrrefs =
          new JCheckBoxMenuItem("Show Strrefs in View tabs", prefs.getBoolean(OPTION_SHOWSTRREFS, false));
      add(optionShowStrrefs);
      optionShowHexColored =
          new JCheckBoxMenuItem("Show colored blocks in Raw tabs", prefs.getBoolean(OPTION_SHOWHEXCOLORED, true));
      add(optionShowHexColored);

      addSeparator();

      // Options->Text Editor
      JMenu textMenu = new JMenu("Text Editor");
      add(textMenu);
      // Options->Text Editor->Show Symbols
      JMenu textSymbols = new JMenu("Show Symbols");
      textMenu.add(textSymbols);
      optionTextShowWhiteSpace = new JCheckBoxMenuItem("Show Spaces and Tabs",
                                                       prefs.getBoolean(OPTION_TEXT_SYMBOLWHITESPACE, false));
      textSymbols.add(optionTextShowWhiteSpace);
      optionTextShowEOL = new JCheckBoxMenuItem("Show End of Line",
                                                prefs.getBoolean(OPTION_TEXT_SYMBOLEOL, false));
      textSymbols.add(optionTextShowEOL);

      // Options->Text Viewer/Editor->Tab Settings
      JMenu textTabs = new JMenu("Tab Settings");
      textMenu.add(textTabs);
      optionTextTabEmulate = new JCheckBoxMenuItem("Emulate Tabs with Spaces",
                                                   prefs.getBoolean(OPTION_TEXT_TABSEMULATED, false));
      textTabs.add(optionTextTabEmulate);
      textTabs.addSeparator();
      ButtonGroup bg = new ButtonGroup();
      int selectedTextTabSize = prefs.getInt(OPTION_TEXT_TABSIZE, 1);
      selectTextTabSize[0] = new JRadioButtonMenuItem("Expand by 2 Spaces", selectedTextTabSize == 0);
      selectTextTabSize[1] = new JRadioButtonMenuItem("Expand by 4 Spaces", selectedTextTabSize == 1);
      selectTextTabSize[2] = new JRadioButtonMenuItem("Expand by 8 Spaces", selectedTextTabSize == 2);
      for (int i = 0; i < selectTextTabSize.length; i++) {
        int cnt = 1 << (i + 1);
        selectTextTabSize[i].setToolTipText(String.format("Each (real or emulated) tab will occupy %1$d spaces.", cnt));
        textTabs.add(selectTextTabSize[i]);
        bg.add(selectTextTabSize[i]);
      }

      // Options->Text Editor->BCS and BAF
      JMenu textBCS = new JMenu("BCS and BAF");
      textMenu.add(textBCS);
      JMenu textBCSIndent = new JMenu("BCS Indent");
      textBCS.add(textBCSIndent);
      bg = new ButtonGroup();
      int selectedBCSIndent = prefs.getInt(OPTION_BCS_INDENT, 2);
      if (selectedBCSIndent < 0 || selectedBCSIndent >= BCSINDENT.length) {
        selectedBCSIndent = 2;
      }
      for (int i = 0; i < BCSINDENT.length; i++) {
        selectBcsIndent[i] = new JRadioButtonMenuItem(BCSINDENT[i][1], selectedBCSIndent == i);
        textBCSIndent.add(selectBcsIndent[i]);
        bg.add(selectBcsIndent[i]);
      }
      JMenu textBCSColors = new JMenu("Color Scheme");
      textBCS.add(textBCSColors);
      bg = new ButtonGroup();
      int selectedBCSScheme = prefs.getInt(OPTION_BCS_COLORSCHEME, 5);
      if (selectedBCSScheme < 0 || selectedBCSScheme >= BCSCOLORSCHEME.length) {
        selectedBCSScheme = 5;
      }
      for (int i = 0; i < BCSCOLORSCHEME.length; i++) {
        selectBcsColorScheme[i] = new JRadioButtonMenuItem(BCSCOLORSCHEME[i][1], selectedBCSScheme == i);
        selectBcsColorScheme[i].setToolTipText(BCSCOLORSCHEME[i][2]);
        textBCSColors.add(selectBcsColorScheme[i]);
        bg.add(selectBcsColorScheme[i]);
      }
      optionBCSEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting",
                                                    prefs.getBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, true));
      textBCS.add(optionBCSEnableSyntax);
      optionBCSEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding",
                                                         prefs.getBoolean(OPTION_BCS_CODEFOLDING, false));
      textBCS.add(optionBCSEnableCodeFolding);
      // TODO: add auto-complete support
//      optionBCSEnableAutoComplete = new JCheckBoxMenuItem("Enable Auto-Completion",
//                                                          prefs.getBoolean(OPTION_BCS_AUTOCOMPLETE, false));
//      optionBCSEnableAutoComplete.setVisible(false);
//      textBCS.add(optionBCSEnableAutoComplete);

      // Options->Text Viewer/Editor->Misc. Resource Types
      JMenu textMisc = new JMenu("Misc. Resource Types");
      textMenu.add(textMisc);
      JMenu textGLSLColors = new JMenu("Color Scheme for GLSL");
      textMisc.add(textGLSLColors);
      bg = new ButtonGroup();
      int selectedGLSLScheme = prefs.getInt(OPTION_GLSL_COLORSCHEME, 0);
      if (selectedGLSLScheme < 0 || selectedGLSLScheme >= COLORSCHEME.length) {
        selectedGLSLScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectGlslColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedGLSLScheme == i);
        selectGlslColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textGLSLColors.add(selectGlslColorScheme[i]);
        bg.add(selectGlslColorScheme[i]);
      }
      JMenu textSQLColors = new JMenu("Color Scheme for SQL");
      textMisc.add(textSQLColors);
      bg = new ButtonGroup();
      int selectedSQLScheme = prefs.getInt(OPTION_SQL_COLORSCHEME, 0);
      if (selectedSQLScheme < 0 || selectedSQLScheme >= COLORSCHEME.length) {
        selectedSQLScheme = 0;
      }
      for (int i = 0; i < COLORSCHEME.length; i++) {
        selectSqlColorScheme[i] = new JRadioButtonMenuItem(COLORSCHEME[i][1], selectedSQLScheme == i);
        selectSqlColorScheme[i].setToolTipText(COLORSCHEME[i][2]);
        textSQLColors.add(selectSqlColorScheme[i]);
        bg.add(selectSqlColorScheme[i]);
      }
      optionGLSLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for GLSL",
                                                     prefs.getBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionGLSLEnableSyntax);
      optionSQLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for SQL",
                                                     prefs.getBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, true));
      textMisc.add(optionSQLEnableSyntax);
      optionGLSLEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding for GLSL",
                                                          prefs.getBoolean(OPTION_GLSL_CODEFOLDING, false));
      textMisc.add(optionGLSLEnableCodeFolding);

      // Options->Text Editor (continued)
      optionTextHightlightCurrent = new JCheckBoxMenuItem("Show Highlighted Current Line",
                                                          prefs.getBoolean(OPTION_TEXT_SHOWCURRENTLINE, true));
      textMenu.add(optionTextHightlightCurrent);
      optionTextLineNumbers = new JCheckBoxMenuItem("Show Line Numbers",
                                                    prefs.getBoolean(OPTION_TEXT_SHOWLINENUMBERS, true));
      textMenu.add(optionTextLineNumbers);

      // Options->Text Editor->Debug: External color scheme
      if (NearInfinity.isDebug()) {
        JMenu textDebug = new JMenu("Debug: External color scheme");
        textMenu.add(textDebug);
        optionTextDebugColorSchemeEnabled = new JCheckBoxMenuItem("Color scheme enabled",
                                                                   prefs.getBoolean(OPTION_TEXT_DEBUG_ENABLECOLORSCHEME, true));
        textDebug.add(optionTextDebugColorSchemeEnabled);
        textDebug.addSeparator();
        optionTextDebugColorSchemeSelect = new JMenuItem("Select color scheme...");
        optionTextDebugColorSchemeSelect.addActionListener(this);
        textDebug.add(optionTextDebugColorSchemeSelect);
        loadDebugColorScheme(prefs.get(OPTION_TEXT_DEBUG_COLORSCHEME, ""));
      }

      // Options->Show ResourceRefs As
      JMenu showresrefmenu = new JMenu("Show ResourceRefs As");
      add(showresrefmenu);
      int selectedresref = prefs.getInt(OPTION_SHOWRESREF, RESREF_REF_NAME);
      showResRef[RESREF_ONLY] = new JRadioButtonMenuItem("Filename", selectedresref == RESREF_ONLY);
      showResRef[RESREF_ONLY].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, CTRL_MASK));
      showResRef[RESREF_REF_NAME] =
      new JRadioButtonMenuItem("Filename (Name)", selectedresref == RESREF_REF_NAME);
      showResRef[RESREF_REF_NAME].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, CTRL_MASK));
      showResRef[RESREF_NAME_REF] =
      new JRadioButtonMenuItem("Name (Filename)", selectedresref == RESREF_NAME_REF);
      showResRef[RESREF_NAME_REF].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, CTRL_MASK));
      bg = new ButtonGroup();
      for (int i = RESREF_ONLY; i <= RESREF_NAME_REF; i++) {
        showresrefmenu.add(showResRef[i]);
        bg.add(showResRef[i]);
      }

      // Options->Show Override Files
      JMenu overridesubmenu = new JMenu("Show Override Files");
      add(overridesubmenu);
      int selectedmode = prefs.getInt(OPTION_SHOWOVERRIDES, OVERRIDE_SPLIT);
      showOverrides[OVERRIDE_IN_THREE] =
      new JRadioButtonMenuItem("In ??? Folders (CRE, SPL, ...)", selectedmode == OVERRIDE_IN_THREE);
      showOverrides[OVERRIDE_IN_OVERRIDE] =
      new JRadioButtonMenuItem("In Override Folder", selectedmode == OVERRIDE_IN_OVERRIDE);
      showOverrides[OVERRIDE_SPLIT] =
      new JRadioButtonMenuItem("Split Between ??? and Override Folders", selectedmode == OVERRIDE_SPLIT);
      showOverrides[OVERRIDE_SPLIT].setToolTipText(
              "Indexed by Chitin.key => ??? folders; Not indexed => Override folder");
      bg = new ButtonGroup();
      for (int i = OVERRIDE_IN_THREE; i <= OVERRIDE_SPLIT; i++) {
        overridesubmenu.add(showOverrides[i]);
        bg.add(showOverrides[i]);
        showOverrides[i].setActionCommand("Refresh");
        showOverrides[i].addActionListener(NearInfinity.getInstance());
      }

      // Options->Default Structure Display
      JMenu vieworeditmenu = new JMenu("Default Structure Display");
      add(vieworeditmenu);
      int selectedview = prefs.getInt(OPTION_VIEWOREDITSHOWN, DEFAULT_VIEW);
      viewOrEditShown[DEFAULT_VIEW] =
      new JRadioButtonMenuItem("View", selectedview == DEFAULT_VIEW);
      viewOrEditShown[DEFAULT_EDIT] =
      new JRadioButtonMenuItem("Edit", selectedview == DEFAULT_EDIT);
      bg = new ButtonGroup();
      bg.add(viewOrEditShown[DEFAULT_VIEW]);
      bg.add(viewOrEditShown[DEFAULT_EDIT]);
      vieworeditmenu.add(viewOrEditShown[DEFAULT_VIEW]);
      vieworeditmenu.add(viewOrEditShown[DEFAULT_EDIT]);

      // Options->Look and Feel
      JMenu lookandfeelmenu = new JMenu("Look and Feel");
      add(lookandfeelmenu);
      final String selectedLF = prefs.get(OPTION_LOOKANDFEELCLASS, DEFAULT_LOOKFEEL.getClassName());
      LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
      bg = new ButtonGroup();
      if (info != null && info.length > 0) {
        // dynamically create a list of supported look&feel themes
        DataRadioButtonMenuItem dbmi;
        for (int i = 0; i < info.length; i++) {
          dbmi = new DataRadioButtonMenuItem(info[i].getName(),
                                             selectedLF.equalsIgnoreCase(info[i].getClassName()),
                                             info[i]);
          lookAndFeel.add(dbmi);
          bg.add(dbmi);
        }
      } else {
        // fallback solution: adding default look&feel theme
        DataRadioButtonMenuItem dbmi;
        dbmi = new DataRadioButtonMenuItem(DEFAULT_LOOKFEEL.getName(), true, DEFAULT_LOOKFEEL);
        lookAndFeel.add(dbmi);
        bg.add(dbmi);
      }
      if (bg.getSelection() == null) {
        lookAndFeel.get(0).setSelected(true);
      }
      for (final JRadioButtonMenuItem lf : lookAndFeel) {
        if (lf != null) {
          lookandfeelmenu.add(lf);
          lf.setActionCommand("ChangeLook");
          lf.addActionListener(NearInfinity.getInstance());
        }
      }

      // Options->Text Font
      JMenu scriptmenu = new JMenu("Text Font");
      add(scriptmenu);
      bg = new ButtonGroup();
      int selectedfont = prefs.getInt(OPTION_FONT, 0);
      for (int i = 0; i < FONTS.length; i++) {
        selectFont[i] =
        new JRadioButtonMenuItem(FONTS[i].getName() + ' ' + FONTS[i].getSize(), i == selectedfont);
        selectFont[i].setFont(FONTS[i]);
        scriptmenu.add(selectFont[i]);
        bg.add(selectFont[i]);
      }

      // Options->TLK Charset
      String charset = prefs.get(OPTION_TLKCHARSET, DefaultCharset);
      if (!charsetAvailable(charset)) {
        System.err.println(String.format("Charset \"%1$s\" not available.", charset));
        charset = DefaultCharset;
      }
      if (!charsetName(charset).equals(StringResource.getCharset().name())) {
        StringResource.setCharset(charsetName(charset));
      }
      mCharsetMenu = initCharsetMenu(charset);
      add(mCharsetMenu);

      // Options->TLK Language
      mLanguageMenu = new JMenu("TLK Language (EE only)");
      add(mLanguageMenu);
    }

    // (Re-)creates a list of available TLK languages
    private void resetGameLanguage()
    {
      final String autodetect = "Autodetect";
      final String tlkFileName = "dialog.tlk";

      for (JRadioButtonMenuItem r: gameLanguage.keySet()) {
        r.removeActionListener(this);
      }
      mLanguageMenu.removeAll();
      gameLanguage.clear();

      Preferences prefs = Preferences.userNodeForPackage(getClass());
      String selectedCode = prefs.get(OPTION_LANGUAGE_GAME, autodetect);

      ButtonGroup bg = new ButtonGroup();
      JRadioButtonMenuItem rbmi;

      // adding "Autodetect" for all available game ids
      rbmi = createLanguageMenuItem("", autodetect,
                                    "Autodetect language from baldur.ini. Defaults to english if not available.", bg, true);
      mLanguageMenu.add(rbmi);

      if (Profile.isEnhancedEdition()) {
        File langFile = new FileNI(Profile.getGameRoot(), "lang");
        if (langFile.isDirectory()) {
          File[] langFileList = langFile.listFiles();
          for (int i = 0; i < langFileList.length; i++) {
            if (langFileList[i].isDirectory()) {
              if ((new FileNI(langFileList[i], tlkFileName)).isFile()) {
                String[] langCode = langFileList[i].getName().split("_");
                if (langCode.length >= 2) {
                  Locale locale = new Locale(langCode[0], langCode[1]);
                  rbmi = createLanguageMenuItem(langFileList[i].getName(),
                                                String.format("%1$s (%2$s)",
                                                              locale.getDisplayLanguage(),
                                                              langFileList[i].getName()),
                                                null, bg,
                                                selectedCode.equalsIgnoreCase(langFileList[i].getName()));
                  mLanguageMenu.add(rbmi);
                } else {
                  rbmi = createLanguageMenuItem(langFileList[i].getName(), langFileList[i].getName(),
                                                null, bg,
                                                selectedCode.equalsIgnoreCase(langFileList[i].getName()));
                  mLanguageMenu.add(rbmi);
                }
              }
            }
          }
        }
      } else {
        rbmi.setEnabled(false);
        rbmi.setToolTipText(null);
      }
    }

    // Initializes and returns a radio button menuitem
    private JRadioButtonMenuItem createLanguageMenuItem(String code, String name, String tooltip,
                                                        ButtonGroup bg, boolean selected)
    {
      JRadioButtonMenuItem rbmi = null;
      if (code == null) {
        code = "";
      }
      if (name != null && !name.isEmpty()) {
        rbmi = new JRadioButtonMenuItem(name);
        if (tooltip != null && !tooltip.isEmpty()) {
          rbmi.setToolTipText(tooltip);
        }
        if (bg != null) {
          bg.add(rbmi);
        }
        rbmi.setSelected(selected);
        rbmi.addItemListener(this);
        gameLanguage.put(rbmi, code);
      }
      return rbmi;
    }

    private JMenu initCharsetMenu(String charset)
    {
      bgCharsetButtons = new ButtonGroup();
      JMenu menu = new JMenu("TLK Charset");
      DataRadioButtonMenuItem dmi =
          new DataRadioButtonMenuItem("Autodetect Charset", false, DefaultCharset);
      dmi.setToolTipText("Attempts to determine the correct character encoding automatically. " +
                         "May not work reliably for non-english games.");
      dmi.addActionListener(this);
      bgCharsetButtons.add(dmi);
      menu.add(dmi);

      // creating primary list of charsets
      for (int i = 0; i < CharsetsUsed.size(); i++) {
        String[] info = CharsetsUsed.get(i);
        if (info != null && info.length > 2) {
          dmi = new DataRadioButtonMenuItem(info[0], false, info[1]);
          StringBuilder sb = new StringBuilder();
          sb.append(info[2]);
          Charset cs = Charset.forName(info[1]);
          if (cs != null && !cs.aliases().isEmpty()) {
            sb.append(" Charset aliases: ");
            Iterator<String> iter = cs.aliases().iterator();
            while (iter.hasNext()) {
              sb.append(iter.next());
              if (iter.hasNext())
                sb.append(", ");
            }
          }
          dmi.setToolTipText(sb.toString());
          dmi.addActionListener(this);
          bgCharsetButtons.add(dmi);
          menu.add(dmi);
        }
      }

      int count = 0;
      JMenu menu2 = new JMenu("More character sets");
      menu.add(menu2);

      // creating secondary list(s) of charsets
      Iterator<String> iter = Charset.availableCharsets().keySet().iterator();
      if (iter != null) {
        while (iter.hasNext()) {
          String name= iter.next();

          // check whether charset has already been added
          boolean match = false;
          for (int i = 0; i < CharsetsUsed.size(); i++) {
            String[] info = CharsetsUsed.get(i);
            if (info != null && info.length > 2) {
              if (name.equalsIgnoreCase(info[1])) {
                match = true;
                break;
              }
            }
          }
          if (match) {
            continue;
          }

          boolean official = !(name.startsWith("x-") || name.startsWith("X-"));
          String desc = official ? name : String.format("%1$s (unofficial)", name.substring(2));
          dmi = new DataRadioButtonMenuItem(desc, false, name);
          Charset cs = Charset.forName(name);
          if (cs != null && !cs.aliases().isEmpty()) {
            StringBuilder sb = new StringBuilder("Charset aliases: ");
            Iterator<String> csIter = cs.aliases().iterator();
            while (csIter.hasNext()) {
              sb.append(csIter.next());
              if (csIter.hasNext())
                sb.append(", ");
            }
            dmi.setToolTipText(sb.toString());
          }
          dmi.addActionListener(this);
          bgCharsetButtons.add(dmi);
          menu2.add(dmi);

          count++;

          // splitting list of charsets into manageable segments
          if (count % 30 == 0) {
            JMenu tmpMenu = new JMenu("More character sets");
            menu2.add(tmpMenu);
            menu2 = tmpMenu;
          }
        }
      }

      // Selecting specified menu item
      dmi = findCharsetButton(charset);
      if (dmi == null) {
        dmi = findCharsetButton(DefaultCharset);
      }
      if (dmi != null) {
        dmi.setSelected(true);
      }

      return menu;
    }

    // Returns the menuitem that is associated with the specified string
    private DataRadioButtonMenuItem findCharsetButton(String charset)
    {
      if (bgCharsetButtons != null && charset != null && !charset.isEmpty()) {
        Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
        while (buttonSet.hasMoreElements()) {
          AbstractButton b = buttonSet.nextElement();
          if (b instanceof DataRadioButtonMenuItem) {
            Object data = ((DataRadioButtonMenuItem)b).getData();
            if (data instanceof String) {
              if (charset.equalsIgnoreCase((String)data)) {
                return (DataRadioButtonMenuItem)b;
              }
            }
          }
        }
      }
      return null;
    }

    // Returns the charset string associated with the currently selected charset menuitem
    private String getSelectedButtonData()
    {
      Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
      if (buttonSet != null) {
        while (buttonSet.hasMoreElements()) {
          AbstractButton b = buttonSet.nextElement();
          if (b instanceof DataRadioButtonMenuItem) {
            DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem)b;
            if (dmi.isSelected()) {
              return (dmi.getData() != null) ? (String)dmi.getData() : DefaultCharset;
            }
          }
        }
      }
      return DefaultCharset;
    }

    // Attempts to determine the correct charset for the current game
    private String charsetName(String charset)
    {
      // TODO: detect specific localizations
      if (DefaultCharset.equalsIgnoreCase(charset)) {
        if (Profile.isEnhancedEdition()) {
          return "UTF-8";
        } else {
          return "windows-1252";
        }
      } else {
        return charset;
      }
    }

    private boolean charsetAvailable(String charset)
    {
      if (charset != null && !charset.isEmpty()) {
        if (DefaultCharset.equalsIgnoreCase(charset)) {
          return true;
        }
        try {
          return (Charset.forName(charset) != null);
        } catch (Throwable t) {
          return false;
        }
      }
      return false;
    }

    private void gameLoaded()
    {
      // update charset selection
      StringResource.setCharset(charsetName(getSelectedButtonData()));
      // update language selection
      resetGameLanguage();
    }

    private void storePreferences(Preferences prefs)
    {
      prefs.putBoolean(OPTION_SHOWOFFSETS, optionShowOffset.isSelected());
      prefs.putBoolean(OPTION_BACKUPONSAVE, optionBackupOnSave.isSelected());
      prefs.putBoolean(OPTION_IGNOREOVERRIDE, optionIgnoreOverride.isSelected());
      prefs.putBoolean(OPTION_IGNOREREADERRORS, optionIgnoreReadErrors.isSelected());
      prefs.putBoolean(OPTION_AUTOCHECK_BCS, optionAutocheckBCS.isSelected());
      prefs.putBoolean(OPTION_CACHEOVERRIDE, optionCacheOverride.isSelected());
      prefs.putBoolean(OPTION_CHECKSCRIPTNAMES, optionCheckScriptNames.isSelected());
      prefs.putBoolean(OPTION_SHOWSTRREFS, optionShowStrrefs.isSelected());
      prefs.putBoolean(OPTION_SHOWHEXCOLORED, optionShowHexColored.isSelected());
      prefs.putInt(OPTION_SHOWRESREF, getResRefMode());
      prefs.putInt(OPTION_SHOWOVERRIDES, getOverrideMode());
      prefs.put(OPTION_LOOKANDFEELCLASS, getLookAndFeel().getClassName());
      prefs.putInt(OPTION_VIEWOREDITSHOWN, getDefaultStructView());
      int selectedFont = getSelectedButtonIndex(selectFont, 0);
      prefs.putInt(OPTION_FONT, selectedFont);
      int selectedIndent = getSelectedButtonIndex(selectBcsIndent, 0);
      prefs.putInt(OPTION_BCS_INDENT, selectedIndent);
      prefs.putBoolean(OPTION_TEXT_SHOWCURRENTLINE, optionTextHightlightCurrent.isSelected());
      prefs.putBoolean(OPTION_TEXT_SHOWLINENUMBERS, optionTextLineNumbers.isSelected());
      prefs.putBoolean(OPTION_TEXT_SYMBOLWHITESPACE, optionTextShowWhiteSpace.isSelected());
      prefs.putBoolean(OPTION_TEXT_SYMBOLEOL, optionTextShowEOL.isSelected());
      prefs.putBoolean(OPTION_TEXT_TABSEMULATED, optionTextTabEmulate.isSelected());
      int selectTabSize = getSelectedButtonIndex(selectTextTabSize, 1);
      prefs.putInt(OPTION_TEXT_TABSIZE, selectTabSize);
      int selectColorScheme = getSelectedButtonIndex(selectBcsColorScheme, 5);
      prefs.putInt(OPTION_BCS_COLORSCHEME, selectColorScheme);
      prefs.putBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, optionBCSEnableSyntax.isSelected());
      prefs.putBoolean(OPTION_BCS_CODEFOLDING, optionBCSEnableCodeFolding.isSelected());
//      prefs.putBoolean(OPTION_BCS_AUTOCOMPLETE, optionBCSEnableAutoComplete.isSelected());
      selectColorScheme = getSelectedButtonIndex(selectGlslColorScheme, 0);
      prefs.putInt(OPTION_GLSL_COLORSCHEME, selectColorScheme);
      selectColorScheme = getSelectedButtonIndex(selectSqlColorScheme, 0);
      prefs.putInt(OPTION_SQL_COLORSCHEME, selectColorScheme);
      prefs.putBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, optionGLSLEnableSyntax.isSelected());
      prefs.putBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, optionSQLEnableSyntax.isSelected());
      prefs.putBoolean(OPTION_GLSL_CODEFOLDING, optionGLSLEnableCodeFolding.isSelected());
      if (NearInfinity.isDebug()) {
        prefs.putBoolean(OPTION_TEXT_DEBUG_ENABLECOLORSCHEME,
                         optionTextDebugColorSchemeEnabled.isSelected());
        prefs.put(OPTION_TEXT_DEBUG_COLORSCHEME, DEBUGCOLORSCHEME);
      }

      String charset = getSelectedButtonData();
      prefs.put(OPTION_TLKCHARSET, charset);

      for (JRadioButtonMenuItem r: gameLanguage.keySet()) {
        if (r.isSelected() && r.isEnabled()) {
          String lang = gameLanguage.get(r);
          prefs.put(OPTION_LANGUAGE_GAME, lang);
          break;
        }
      }
    }

    // Returns the (first) index of the selected AbstractButton array
    private int getSelectedButtonIndex(AbstractButton[] items, int defaultIndex)
    {
      int retVal = defaultIndex;
      if (items != null && items.length > 0) {
        for (int i = 0; i < items.length; i++) {
          if (items[i] != null && items[i].isSelected()) {
            retVal = i;
            break;
          }
        }
      }
      return retVal;
    }

//    // Returns the path to a color scheme definition file
//    private String getColorSchemeFile(String rootPath)
//    {
//      if (rootPath == null) {
//        if (ResourceFactory.getInstance() != null) {
//          rootPath = ResourceFactory.getRootDir().toString();
//        } else {
//          rootPath = ".";
//        }
//      }
//      JFileChooser fc = new JFileChooser(rootPath);
//      File file = new File(rootPath);
//      if (!file.isDirectory()) {
//        fc.setSelectedFile(file);
//      }
//      fc.setDialogTitle("Select color scheme definition file");
//      fc.setDialogType(JFileChooser.OPEN_DIALOG);
//      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
//      fc.setMultiSelectionEnabled(false);
//      fc.addChoosableFileFilter(new FileNameExtensionFilter("Color Scheme Definitions (*.xml)", "xml"));
//      fc.setFileFilter(fc.getChoosableFileFilters()[0]);
//      if (fc.showOpenDialog(NearInfinity.getInstance()) == JFileChooser.APPROVE_OPTION) {
//        return fc.getSelectedFile().toString();
//      }
//      return null;
//    }

    public int getTextIndentIndex()
    {
      for (int i = 0; i < selectTextTabSize.length; i++) {
        if (selectTextTabSize[i].isSelected()) {
          return i;
        }
      }
      return 1;   // default
    }

    public String getBcsIndent()
    {
      int idx = getSelectedButtonIndex(selectBcsIndent, 2);
      return BCSINDENT[idx][0];
    }

    public String getBcsColorScheme()
    {
      int idx = getSelectedButtonIndex(selectBcsColorScheme, 5);
      return BCSCOLORSCHEME[idx][0];
    }

    public String getGlslColorScheme()
    {
      int idx = getSelectedButtonIndex(selectGlslColorScheme, 0);
      return COLORSCHEME[idx][0];
    }

    public String getSqlColorScheme()
    {
      int idx = getSelectedButtonIndex(selectSqlColorScheme, 0);
      return COLORSCHEME[idx][0];
    }


    // Registers the specified filename as an external color scheme
    public void loadDebugColorScheme(String fileName)
    {
      if (NearInfinity.isDebug()) {
        if (fileName != null && !fileName.isEmpty() && (new FileNI(fileName)).isFile()) {
          // adding specified file reference to list
          DEBUGCOLORSCHEME = fileName;
          optionTextDebugColorSchemeSelect
            .setToolTipText(String.format("Color scheme \"%1$s\" selected", fileName));
        } else {
          // removing file reference from list
          DEBUGCOLORSCHEME = "";
          optionTextDebugColorSchemeSelect.setToolTipText("No color scheme selected");
        }
      }
    }

    // Returns the selected external color scheme file (only if enabled and file exists)
    public String getDebugColorScheme()
    {
      if (NearInfinity.isDebug() && optionTextDebugColorSchemeEnabled.isSelected()) {
        if (DEBUGCOLORSCHEME != null && (new FileNI(DEBUGCOLORSCHEME)).isFile()) {
          return DEBUGCOLORSCHEME;
        }
      }
      return null;
    }


    public int getResRefMode()
    {
      if (showResRef[RESREF_ONLY].isSelected())
        return RESREF_ONLY;
      else if (showResRef[RESREF_NAME_REF].isSelected())
        return RESREF_NAME_REF;
      return RESREF_REF_NAME;
    }

    public int getOverrideMode()
    {
      if (showOverrides[OVERRIDE_IN_THREE].isSelected())
        return OVERRIDE_IN_THREE;
      else if (showOverrides[OVERRIDE_IN_OVERRIDE].isSelected())
        return OVERRIDE_IN_OVERRIDE;
      return OVERRIDE_SPLIT;
    }

    public LookAndFeelInfo getLookAndFeel()
    {
      for (int i = 0; i < lookAndFeel.size(); i++) {
        if (lookAndFeel.get(i) != null && lookAndFeel.get(i).isSelected()) {
          return (LookAndFeelInfo)lookAndFeel.get(i).getData();
        }
      }
      return DEFAULT_LOOKFEEL;
    }

    public int getDefaultStructView()
    {
      if (viewOrEditShown[DEFAULT_VIEW].isSelected())
        return DEFAULT_VIEW;
      return DEFAULT_EDIT;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() instanceof DataRadioButtonMenuItem) {
        DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem)event.getSource();
        String csName = (String)dmi.getData();
        if (csName != null) {
          StringResource.setCharset(charsetName(csName));
          // re-read strings
          ActionEvent refresh = new ActionEvent(dmi, 0, "Refresh");
          NearInfinity.getInstance().actionPerformed(refresh);
        }
      } else if (event.getSource() == optionTextDebugColorSchemeSelect) {
        // Debug: loading external color scheme file
        File file = null;
        if (DEBUGCOLORSCHEME != null) {
          file = new FileNI(DEBUGCOLORSCHEME);
          if (!file.isFile()) {
            file = null;
          }
        }
        String rootPath = (file != null) ? file.getParent() : Profile.getGameRoot().toString();
        JFileChooser fc = new JFileChooser(rootPath);
        if (file != null) {
          fc.setSelectedFile(file);
        }
        fc.setDialogTitle("Select color scheme file");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Color Scheme files (*.xml)", "xml");
        fc.setFileFilter(filter);
        if (fc.showOpenDialog(NearInfinity.getInstance()) == JFileChooser.APPROVE_OPTION) {
          loadDebugColorScheme(fc.getSelectedFile().toString());
        }
        fc = null;
      }
    }

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      if (event.getSource() instanceof JRadioButtonMenuItem &&
          gameLanguage.containsKey(event.getSource())) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
              "Please restart NearInfinity to make the changes visible.",
              "TLK language changed", JOptionPane.INFORMATION_MESSAGE);
        }
      }
    }
  }

  ///////////////////////////////
  // Help Menu
  ///////////////////////////////

  private static final class HelpMenu extends JMenu implements ActionListener
  {
    private final JMenuItem helpAbout, helpLicense, helpBsdLicense,
                            helpJOrbisLicense, helpFifeLicense, helpJHexViewLicense;

    private HelpMenu()
    {
      super("Help");
      setMnemonic(KeyEvent.VK_H);

      helpAbout = makeMenuItem("About Near Infinity", KeyEvent.VK_A, Icons.getIcon("About16.gif"), -1, this);
      add(helpAbout);

      helpLicense =
          makeMenuItem("Near Infinity License", KeyEvent.VK_N, Icons.getIcon("Edit16.gif"), -1, this);
      add(helpLicense);

      JMenu miscLicenses = new JMenu("Third-party licenses");
      miscLicenses.setMnemonic(KeyEvent.VK_T);
      add(miscLicenses);

      helpBsdLicense =
          makeMenuItem("Plastic XP License", KeyEvent.VK_P, Icons.getIcon("Edit16.gif"), -1, this);
      miscLicenses.add(helpBsdLicense);

      helpJOrbisLicense =
          makeMenuItem("JOrbis License", KeyEvent.VK_J, Icons.getIcon("Edit16.gif"), -1, this);
      miscLicenses.add(helpJOrbisLicense);

      helpFifeLicense =
          makeMenuItem("Fifesoft License", KeyEvent.VK_R, Icons.getIcon("Edit16.gif"), -1, this);
      miscLicenses.add(helpFifeLicense);

      helpJHexViewLicense =
          makeMenuItem("JHexView License", KeyEvent.VK_R, Icons.getIcon("Edit16.gif"), -1, this);
      miscLicenses.add(helpJHexViewLicense);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == helpAbout) {
        displayAbout();
      } else if (event.getSource() == helpLicense) {
        displayLicense("infinity/License.txt", "LGPL License");
      } else if (event.getSource() == helpBsdLicense) {
        displayLicense("infinity/bsd-license.txt", "BSD License");
      } else if (event.getSource() == helpJOrbisLicense) {
          displayLicense("infinity/JOrbis.License.txt", "LGPL License");
      } else if (event.getSource() == helpFifeLicense) {
        displayLicense("infinity/RSyntaxTextArea.License.txt", "BSD License");
      } else if (event.getSource() == helpJHexViewLicense) {
        displayLicense("infinity/JHexView.License.txt", "GPL License");
      }
    }

    private void displayAbout()
    {
      final String hauglidPage = "http://www.idi.ntnu.no/~joh/ni/";
      final String githubPage = "https://github.com/NearInfinityBrowser/NearInfinity/";
      final String versionText = "Near Infinity " + VERSION;
      final String githubHTML = "<html><a href=" + githubPage + "/>" + githubPage + "</a></html>";
      final String hauglidVersionText = "From Near Infinity 1.32.1 beta 24";
      final String hauglidCopyrightText = "Copyright (\u00A9) 2001-2005 - Jon Olav Hauglid";
      final String hauglidHTML = "<html><a href=" + hauglidPage + "/>" + hauglidPage + "</a></html>";

      // NearInfinity copyright message
      final String[] copyNearInfinityText = new String[]{
          "This program is free and may be distributed according to the terms of ",
          "the GNU Lesser General Public License."
      };

      // List of contributors (ordered chronologically)
      final String[] contributorsText = new String[]{
          "devSin",
          "FredSRichardson",
          "Taimon",
          "Valerio Bigiani (aka The Bigg)",
          "Fredrik Lindgren (aka Wisp)",
          "Argent77",
      };
      final String[] contributorsMisc = new String[]{
          "Near Infinity logo/icon by Cuv and Troodon80",
      };

      // Third-party copyright messages
      final String[] copyThirdPartyText = new String[]{
          "Most icons (\u00A9) eclipse.org - Common Public License.",
          "Plastic XP L&F (\u00A9) jgoodies.com - Berkeley Software Distribution License.",
          "RSyntaxTextArea (\u00A9) Fifesoft - Berkeley Software Distribution License.",
          "JOrbis (\u00A9) JCraft Inc. - GNU Lesser General Public License.",
          "JHexView by Sebastian Porst - GNU General Public License.",
      };

      // Fixed elements
      JLabel version = new JLabel(versionText);
      JLabel githubLink = new JLabel(githubHTML, JLabel.LEADING);
      githubLink.addMouseListener(new UrlBrowser(githubPage));
      githubLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      JLabel hauglidVersion = new JLabel(hauglidVersionText, JLabel.LEADING);
      JLabel hauglidCopyright = new JLabel(hauglidCopyrightText, JLabel.LEADING);
      JLabel hauglidLink = new JLabel(hauglidHTML, JLabel.LEADING);
      hauglidLink.addMouseListener(new UrlBrowser(hauglidPage));
      hauglidLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      final Font defaultfont = version.getFont();
      final Font font = defaultfont.deriveFont(13.0f);
      final Font bigFont = defaultfont.deriveFont(Font.BOLD, 20.0f);
      final Font smallFont = defaultfont.deriveFont(11.0f);

      version.setFont(bigFont);
      githubLink.setFont(font);
      hauglidVersion.setFont(font);
      hauglidCopyright.setFont(font);
      hauglidLink.setFont(font);

      JPanel panel = new JPanel();
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      panel.setLayout(gbl);

      gbc.insets = new Insets(6, 6, 3, 6);
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbl.setConstraints(version, gbc);
      panel.add(version);
      gbc.insets = new Insets(3, 6, 3, 6);
      gbl.setConstraints(githubLink, gbc);
      panel.add(githubLink);
      gbc.insets = new Insets(6, 6, 0, 6);
      gbl.setConstraints(hauglidVersion, gbc);
      panel.add(hauglidVersion);
      gbc.insets = new Insets(0, 6, 0, 6);
      gbl.setConstraints(hauglidCopyright, gbc);
      panel.add(hauglidCopyright);
      gbc.insets = new Insets(3, 6, 3, 6);
      gbl.setConstraints(hauglidLink, gbc);
      panel.add(hauglidLink);

      // Adding code contributors
      gbc.insets = new Insets(9, 6, 0, 6);
      // trying to limit line width to a certain maximum
      FontMetrics fm = getFontMetrics(font);
      double maxWidth = fm.getStringBounds(hauglidCopyrightText, getGraphics()).getWidth() * 1.75;

      // adding title
      JLabel label = new JLabel("Additional Contributors:", SwingConstants.LEADING);
      label.setFont(smallFont.deriveFont(12.0f));
      gbl.setConstraints(label, gbc);
      panel.add(label);
      gbc.insets.top = 1;

      // adding names
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < contributorsText.length; i++) {
        if (i > 0) {
          if (i+1 == contributorsText.length) {
            sb.append(" and ");
          } else {
            sb.append(", ");
          }
        }
        String s = sb.toString() + contributorsText[i];
        if (fm.getStringBounds(s, getGraphics()).getWidth() > maxWidth) {
          label = new JLabel(sb.toString(), SwingConstants.LEADING);
          label.setFont(smallFont);
          gbl.setConstraints(label, gbc);
          panel.add(label);
          gbc.insets.top = 0;
          sb = new StringBuilder();
        }
        sb.append(contributorsText[i]);
      }
      label = new JLabel(sb.toString(), SwingConstants.LEADING);
      label.setFont(smallFont);
      gbl.setConstraints(label, gbc);
      panel.add(label);

      // Adding misc. contributors
      gbc.insets.top = 4;
      for (int i = 0; i < contributorsMisc.length; i++) {
        label = new JLabel(contributorsMisc[i], SwingConstants.LEADING);
        label.setFont(smallFont);
        gbl.setConstraints(label, gbc);
        panel.add(label);
        gbc.insets.top = 0;
      }

      // NearInfinity copyright message
      gbc.insets.top = 12;
      label = new JLabel("Near Infinity license:");
      label.setFont(smallFont.deriveFont(12.0f));
      gbl.setConstraints(label, gbc);
      panel.add(label);
      gbc.insets.top = 1;

      for (int i = 0; i < copyNearInfinityText.length; i++) {
        label = new JLabel(copyNearInfinityText[i], SwingConstants.LEADING);
        label.setFont(smallFont);
        gbl.setConstraints(label, gbc);
        panel.add(label);
        gbc.insets.top = 0;
      }

      // Third-party copyright messages
      gbc.insets.top = 12;
      label = new JLabel("Additional licenses:");
      label.setFont(smallFont.deriveFont(12.0f));
      gbl.setConstraints(label, gbc);
      panel.add(label);
      gbc.insets.top = 1;

      for (int i = 0; i < copyThirdPartyText.length; i++) {
        label = new JLabel(copyThirdPartyText[i], SwingConstants.LEADING);
        label.setFont(smallFont);
        gbl.setConstraints(label, gbc);
        panel.add(label);
        gbc.insets.top = 0;
      }

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, "About Near Infinity",
                                    JOptionPane.INFORMATION_MESSAGE, Icons.getIcon("App128.png"));
    }

    private void displayLicense(String classPath, String title)
    {
      JPanel panel = new JPanel(new BorderLayout());
      JTextPane tphelp = new JTextPane();
      tphelp.setFont(new Font("Monospaced", Font.PLAIN, 12));
      tphelp.setEditable(false);
      tphelp.setMargin(new Insets(3, 3, 3, 3));
      panel.add(new JScrollPane(tphelp), BorderLayout.CENTER);
      panel.setPreferredSize(new Dimension(640, 480));

      try {
        tphelp.setPage(ClassLoader.getSystemResource(classPath));
      } catch (IOException e) {
        e.printStackTrace();
      }

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, title,
                                    JOptionPane.PLAIN_MESSAGE);
    }
  }
}
