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
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructureFactory;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.DialogSearcher;
import infinity.search.SearchFrame;
import infinity.search.TextResourceSearcher;
import infinity.util.MassExporter;
import infinity.util.NIFile;
import infinity.util.StringResource;
import infinity.util.FileCI;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

public final class BrowserMenuBar extends JMenuBar
{
  public static final String VERSION = "v1.35.0-alpha.131123 CI";
  public static final int OVERRIDE_IN_THREE = 0, OVERRIDE_IN_OVERRIDE = 1, OVERRIDE_SPLIT = 2;
  public static final int LOOKFEEL_JAVA = 0, LOOKFEEL_WINDOWS = 1, LOOKFEEL_MOTIF = 2, LOOKFEEL_PLASTICXP = 3;
  public static final int RESREF_ONLY = 0, RESREF_REF_NAME = 1, RESREF_NAME_REF = 2;
  public static final int DEFAULT_VIEW = 0, DEFAULT_EDIT = 1;
  private static BrowserMenuBar menuBar;
  private static final Font FONTS[] = {new Font("Monospaced", Font.PLAIN, 12),
                                       new Font("Serif", Font.PLAIN, 12),
                                       new Font("SansSerif", Font.PLAIN, 12),
                                       new Font("Lucida", Font.PLAIN, 12)};
  private static final String BCSINDENT[] = {"  ", "    ", "\t"};
  private static final String OPTION_SHOWOFFSETS = "ShowOffsets";
  private static final String OPTION_IGNOREOVERRIDE = "IgnoreOverride";
  private static final String OPTION_IGNOREREADERRORS = "IgnoreReadErrors";
  private static final String OPTION_AUTOCHECK_BCS = "AutocheckBCS";
  private static final String OPTION_CACHEOVERRIDE = "CacheOverride";
  private static final String OPTION_CHECKSCRIPTNAMES = "CheckScriptNames";
  private static final String OPTION_SHOWOVERRIDES = "ShowOverridesIn";
  private static final String OPTION_SHOWRESREF = "ShowResRef";
  private static final String OPTION_LOOKANDFEEL = "LookAndFeel";
  private static final String OPTION_VIEWOREDITSHOWN = "ViewOrEditShown";
  private static final String OPTION_FONT = "Font";
  private static final String OPTION_BCSINDENT = "BcsIndent";
  private static final String OPTION_TLKCHARSET = "TLKCharset";
  private final EditMenu editMenu;
  private final FileMenu fileMenu;
  private final GameMenu gameMenu;
  private final JRadioButtonMenuItem showOverrides[] = new JRadioButtonMenuItem[3];
  private final JRadioButtonMenuItem lookAndFeel[] = new JRadioButtonMenuItem[4];
  private final JRadioButtonMenuItem showResRef[] = new JRadioButtonMenuItem[3];
  private final JRadioButtonMenuItem viewOrEditShown[] = new JRadioButtonMenuItem[3];
  private final JRadioButtonMenuItem selectFont[] = new JRadioButtonMenuItem[FONTS.length];
  private final JRadioButtonMenuItem selectBcsIndent[] = new JRadioButtonMenuItem[BCSINDENT.length];
  private JCheckBoxMenuItem optionShowOffset, optionIgnoreOverride, optionIgnoreReadErrors;
  private JCheckBoxMenuItem optionAutocheckBCS, optionCacheOverride, optionCheckScriptNames;

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
      item.setAccelerator(KeyStroke.getKeyStroke(shortKey, ActionEvent.CTRL_MASK));
    if (listener != null)
      item.addActionListener(listener);
    return item;
  }

  public BrowserMenuBar(NearInfinity browser)
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    gameMenu = new GameMenu(prefs, browser);
    fileMenu = new FileMenu();
    editMenu = new EditMenu();
    add(gameMenu);
    add(fileMenu);
    add(editMenu);
    add(new SearchMenu());
    add(new ToolsMenu());
    add(makeOptionsMenu(prefs, browser));
    add(new HelpMenu());
    menuBar = this;
  }

  public boolean autocheckBCS()
  {
    return optionAutocheckBCS.isSelected();
  }

  public boolean checkScriptNames()
  {
    return optionCheckScriptNames.isSelected();
  }

  public boolean cacheOverride()
  {
    return optionCacheOverride.isSelected();
  }

  public void gameLoaded(int oldGame, String oldFile)
  {
    gameMenu.gameLoaded(oldGame, oldFile);
    fileMenu.gameLoaded();
    editMenu.gameLoaded();
  }

  public String getBcsIndent()
  {
    for (int i = 0; i < BCSINDENT.length; i++)
      if (selectBcsIndent[i].isSelected())
        return BCSINDENT[i];
    return BCSINDENT[2];
  }

  public int getDefaultStructView()
  {
    if (viewOrEditShown[DEFAULT_VIEW].isSelected())
      return DEFAULT_VIEW;
    return DEFAULT_EDIT;
  }

  public int getLookAndFeel()
  {
    for (int i = 0; i < lookAndFeel.length; i++) {
      if (lookAndFeel[i] != null && lookAndFeel[i].isSelected())
        return i;
    }
    return LOOKFEEL_JAVA;
  }

  public int getOverrideMode()
  {
    if (showOverrides[OVERRIDE_IN_THREE].isSelected())
      return OVERRIDE_IN_THREE;
    else if (showOverrides[OVERRIDE_IN_OVERRIDE].isSelected())
      return OVERRIDE_IN_OVERRIDE;
    return OVERRIDE_SPLIT;
  }

  public int getResRefMode()
  {
    if (showResRef[RESREF_ONLY].isSelected())
      return RESREF_ONLY;
    else if (showResRef[RESREF_NAME_REF].isSelected())
      return RESREF_NAME_REF;
    return RESREF_REF_NAME;
  }

  public Font getScriptFont()
  {
    for (int i = 0; i < FONTS.length; i++)
      if (selectFont[i].isSelected())
        return FONTS[i];
    return FONTS[0];
  }

  public boolean ignoreOverrides()
  {
    return optionIgnoreOverride.isSelected();
  }

  public boolean ignoreReadErrors()
  {
    return optionIgnoreReadErrors.isSelected();
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
    return optionShowOffset.isSelected();
  }

  public void storePreferences()
  {
    Preferences prefs = Preferences.userNodeForPackage(getClass());
    prefs.putBoolean(OPTION_SHOWOFFSETS, optionShowOffset.isSelected());
    prefs.putBoolean(OPTION_IGNOREOVERRIDE, optionIgnoreOverride.isSelected());
    prefs.putBoolean(OPTION_IGNOREREADERRORS, optionIgnoreReadErrors.isSelected());
    prefs.putBoolean(OPTION_AUTOCHECK_BCS, optionAutocheckBCS.isSelected());
    prefs.putBoolean(OPTION_CACHEOVERRIDE, optionCacheOverride.isSelected());
    prefs.putBoolean(OPTION_CHECKSCRIPTNAMES, optionCheckScriptNames.isSelected());
    prefs.putInt(OPTION_SHOWRESREF, getResRefMode());
    prefs.putInt(OPTION_SHOWOVERRIDES, getOverrideMode());
    prefs.putInt(OPTION_LOOKANDFEEL, getLookAndFeel());
    prefs.putInt(OPTION_VIEWOREDITSHOWN, getDefaultStructView());
    int selectedFont = 0;
    for (int i = 0; i < selectFont.length; i++)
      if (selectFont[i].isSelected())
        selectedFont = i;
    prefs.putInt(OPTION_FONT, selectedFont);
    int selectedIndent = 0;
    for (int i = 0; i < selectBcsIndent.length; i++)
      if (selectBcsIndent[i].isSelected())
        selectedIndent = i;
    prefs.putInt(OPTION_BCSINDENT, selectedIndent);
    prefs.put(OPTION_TLKCHARSET, StringResource.getCharset().name());
    gameMenu.storePreferences(prefs);
  }

  ///////////////////////////////
  // Options Menu
  ///////////////////////////////

  private JMenu makeOptionsMenu(Preferences prefs, NearInfinity browser)
  {
    final JMenu menu = new JMenu("Options");
    menu.setMnemonic(KeyEvent.VK_O);

    optionIgnoreOverride =
    new JCheckBoxMenuItem("Ignore Overrides", prefs.getBoolean(OPTION_IGNOREOVERRIDE, false));
    menu.add(optionIgnoreOverride);
    optionIgnoreReadErrors =
    new JCheckBoxMenuItem("Ignore Read Errors", prefs.getBoolean(OPTION_IGNOREREADERRORS, false));
    menu.add(optionIgnoreReadErrors);
    optionShowOffset =
    new JCheckBoxMenuItem("Show Hex Offsets", prefs.getBoolean(OPTION_SHOWOFFSETS, false));
    menu.add(optionShowOffset);
    optionAutocheckBCS =
    new JCheckBoxMenuItem("Autocheck BCS", prefs.getBoolean(OPTION_AUTOCHECK_BCS, true));
    menu.add(optionAutocheckBCS);
    optionCacheOverride =
    new JCheckBoxMenuItem("Autocheck for Overrides", prefs.getBoolean(OPTION_CACHEOVERRIDE, false));
    optionCacheOverride.setToolTipText("Without this option selected, Refresh Tree is required " +
                                       "to discover new override files added while NI is open");
    menu.add(optionCacheOverride);
    optionCheckScriptNames =
    new JCheckBoxMenuItem("Interactive script names", prefs.getBoolean(OPTION_CHECKSCRIPTNAMES, true));
    optionCheckScriptNames.setToolTipText("With this option disabled, performance may be boosted " +
                                          "but many features involving script names will be disabled.");
    menu.add(optionCheckScriptNames);


    menu.addSeparator();

    JMenu bcsindentmenu = new JMenu("BCS Indent");
    menu.add(bcsindentmenu);
    ButtonGroup bg = new ButtonGroup();
    int selectedbcsindent = prefs.getInt(OPTION_BCSINDENT, 2);
    selectBcsIndent[0] = new JRadioButtonMenuItem("2 Spaces", selectedbcsindent == 0);
    selectBcsIndent[1] = new JRadioButtonMenuItem("4 Spaces", selectedbcsindent == 1);
    selectBcsIndent[2] = new JRadioButtonMenuItem("Tab", selectedbcsindent == 2);
    for (int i = 0; i < BCSINDENT.length; i++) {
      bcsindentmenu.add(selectBcsIndent[i]);
      bg.add(selectBcsIndent[i]);
    }

    JMenu showresrefmenu = new JMenu("Show ResourceRefs As");
    menu.add(showresrefmenu);
    int selectedresref = prefs.getInt(OPTION_SHOWRESREF, RESREF_REF_NAME);
    showResRef[RESREF_ONLY] = new JRadioButtonMenuItem("Filename", selectedresref == RESREF_ONLY);
    showResRef[RESREF_ONLY].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.CTRL_MASK));
    showResRef[RESREF_REF_NAME] =
    new JRadioButtonMenuItem("Filename (Name)", selectedresref == RESREF_REF_NAME);
    showResRef[RESREF_REF_NAME].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.CTRL_MASK));
    showResRef[RESREF_NAME_REF] =
    new JRadioButtonMenuItem("Name (Filename)", selectedresref == RESREF_NAME_REF);
    showResRef[RESREF_NAME_REF].setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.CTRL_MASK));
    bg = new ButtonGroup();
    for (int i = RESREF_ONLY; i <= RESREF_NAME_REF; i++) {
      showresrefmenu.add(showResRef[i]);
      bg.add(showResRef[i]);
    }

    JMenu overridesubmenu = new JMenu("Show Override Files");
    menu.add(overridesubmenu);
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
      showOverrides[i].addActionListener(browser);
    }

    JMenu vieworeditmenu = new JMenu("Default Structure Display");
    menu.add(vieworeditmenu);
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

    JMenu lookandfeelmenu = new JMenu("Look and Feel");
    menu.add(lookandfeelmenu);
    int selectedfeel = prefs.getInt(OPTION_LOOKANDFEEL, LOOKFEEL_JAVA);
    lookAndFeel[LOOKFEEL_JAVA] = new JRadioButtonMenuItem("Java", selectedfeel == LOOKFEEL_JAVA);
    lookAndFeel[LOOKFEEL_WINDOWS] = new JRadioButtonMenuItem("Native", selectedfeel == LOOKFEEL_WINDOWS);
    lookAndFeel[LOOKFEEL_MOTIF] = new JRadioButtonMenuItem("Motif", selectedfeel == LOOKFEEL_MOTIF);
    try {
      Class.forName("com.jgoodies.plaf.plastic.PlasticLookAndFeel");
      lookAndFeel[LOOKFEEL_PLASTICXP] =
      new JRadioButtonMenuItem("Plastic XP", selectedfeel == LOOKFEEL_PLASTICXP);
    } catch (ClassNotFoundException e) {
      if (selectedfeel == LOOKFEEL_PLASTICXP)
        lookAndFeel[LOOKFEEL_JAVA].setSelected(true);
    }
    bg = new ButtonGroup();
    for (final JRadioButtonMenuItem lf : lookAndFeel) {
      if (lf != null) {
        lookandfeelmenu.add(lf);
        bg.add(lf);
        lf.setActionCommand("ChangeLook");
        lf.addActionListener(browser);
      }
    }

    JMenu scriptmenu = new JMenu("Text Font");
    menu.add(scriptmenu);
    bg = new ButtonGroup();
    int selectedfont = prefs.getInt(OPTION_FONT, 0);
    for (int i = 0; i < FONTS.length; i++) {
      selectFont[i] =
      new JRadioButtonMenuItem(FONTS[i].getName() + ' ' + FONTS[i].getSize(), i == selectedfont);
      selectFont[i].setFont(FONTS[i]);
      scriptmenu.add(selectFont[i]);
      bg.add(selectFont[i]);
    }

    final JMenu charsetmenu = new JMenu("TLK Charset");
    menu.add(charsetmenu);
    String defaultCharset = StringResource.getCharset().name();
    String charset = prefs.get(OPTION_TLKCHARSET, defaultCharset);
    if (! charset.equals(defaultCharset)) {
      StringResource.setCharset(charset);
    }
    final JTextField tf = new JTextField(charset, 15);
    tf.setMargin(new Insets(2, 2, 2, 2));
    tf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        try {
          StringResource.setCharset(event.getActionCommand());

          // XXX: don't know how to deselect the menu properly
          charsetmenu.setPopupMenuVisible(false);
          menu.setPopupMenuVisible(false);
          menu.setSelected(false);

          // re-read strings
          ActionEvent refresh = new ActionEvent(tf, 0, "Refresh");
          NearInfinity.getInstance().actionPerformed(refresh);
        }
        catch (IllegalArgumentException e) {
          tf.setText(StringResource.getCharset().name());
          JOptionPane.showMessageDialog(null, "Illegal charset or charset not supported",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    tf.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent event) {
        // only trigger when states differ
        if (! tf.getText().equals(StringResource.getCharset().name())) {
          tf.postActionEvent();
        }
      }
    });
    charsetmenu.add(tf);

    return menu;
  }

// -------------------------- INNER CLASSES --------------------------

  ///////////////////////////////
  // Game Menu
  ///////////////////////////////
  private static final class GameMenu extends JMenu implements ActionListener
  {
    private final String LASTGAME_IDS[] = {"LastGameID1", "LastGameID2", "LastGameID3", "LastGameID4",
                                           "LastGameID5"};
    private final String LASTGAME_PATH[] = {"LastGamePath1", "LastGamePath2", "LastGamePath3", "LastGamePath4",
                                            "LastGamePath5"};
    private final JMenuItem gameOpenFile, gameOpenGame, gameRefresh, gameExit, gameCloseTLK;
    private final JMenuItem gameLastGame[] = new JMenuItem[LASTGAME_IDS.length];
    private final List<Integer> lastGameID = new ArrayList<Integer>();
    private final List<String> lastGamePath = new ArrayList<String>();

    private GameMenu(Preferences prefs, NearInfinity browser)
    {
      super("Game");
      setMnemonic(KeyEvent.VK_G);

      gameOpenFile = makeMenuItem("Open File...", KeyEvent.VK_F, Icons.getIcon("Open16.gif"), KeyEvent.VK_I, this);
      add(gameOpenFile);
      gameOpenGame = makeMenuItem("Open Game...", KeyEvent.VK_O, Icons.getIcon("Open16.gif"), KeyEvent.VK_O, browser);
      gameOpenGame.setActionCommand("Open");
      add(gameOpenGame);
      gameRefresh = makeMenuItem("Refresh Tree", KeyEvent.VK_R, Icons.getIcon("Refresh16.gif"), -1, browser);
      gameRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
      gameRefresh.setActionCommand("Refresh");
      add(gameRefresh);
      gameCloseTLK = makeMenuItem("Release Dialog.tlk Lock", KeyEvent.VK_D, Icons.getIcon("Release16.gif"), -1, this);
      add(gameCloseTLK);

      addSeparator();

      for (int i = 0; i < LASTGAME_IDS.length; i++) {
        int gameid = prefs.getInt(LASTGAME_IDS[i], -1);
        String gamepath = prefs.get(LASTGAME_PATH[i], null);
        if (gameid != -1 && gamepath != null && new File(gamepath).exists()) { // File is used intentionally
          lastGameID.add(new Integer(gameid));
          lastGamePath.add(gamepath);
        }
      }
      for (int i = 0; i < lastGameID.size(); i++) {
        gameLastGame[i] = new JMenuItem(i + 1 + " " + ResourceFactory.getGameName(lastGameID.get(i).intValue())
                                        + " @ " + lastGamePath.get(i));
        gameLastGame[i].setToolTipText(lastGamePath.get(i));
        gameLastGame[i].addActionListener(this);
        gameLastGame[i].setActionCommand("OpenOldGame");
        add(gameLastGame[i]);
      }
      for (int i = lastGameID.size(); i < LASTGAME_IDS.length; i++) {
        gameLastGame[i] = new JMenuItem(String.valueOf(i + 1));
        gameLastGame[i].setEnabled(false);
        gameLastGame[i].addActionListener(this);
        gameLastGame[i].setActionCommand("OpenOldGame");
        add(gameLastGame[i]);
      }

      addSeparator();

      gameExit = makeMenuItem("Quit", KeyEvent.VK_Q, Icons.getIcon("Exit16.gif"), KeyEvent.VK_Q, browser);
      gameExit.setActionCommand("Exit");
      add(gameExit);
    }

    private void gameLoaded(int oldGame, String oldFile)
    {
      int newIndex = -1;
      for (int i = 0; i < lastGamePath.size(); i++)
        if (ResourceFactory.getKeyfile().toString().equalsIgnoreCase(lastGamePath.get(i)))
          newIndex = i;
      if (newIndex != -1) {
        lastGameID.remove(newIndex);
        lastGamePath.remove(newIndex);
      }
      if (oldGame != -1) {
        int oldIndex = -1;
        for (int i = 0; i < lastGamePath.size(); i++)
          if (oldFile.equalsIgnoreCase(lastGamePath.get(i)))
            oldIndex = i;
        if (oldIndex != -1) {
          lastGameID.remove(oldIndex);
          lastGamePath.remove(oldIndex);
        }
        lastGameID.add(0, new Integer(oldGame));
        lastGamePath.add(0, oldFile);
      }
      while (lastGameID.size() > 5) {
        lastGamePath.remove(lastGameID.size() - 1);
        lastGameID.remove(lastGameID.size() - 1);
      }
      if (newIndex != 1 || oldGame != -1) {
        for (int i = 0; i < lastGameID.size(); i++) {
          gameLastGame[i].setText(i + 1 + " " + ResourceFactory.getGameName(lastGameID.get(i).intValue())
                                  + " @ " + lastGamePath.get(i));
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
          prefs.putInt(LASTGAME_IDS[i], lastGameID.get(i).intValue());
          prefs.put(LASTGAME_PATH[i], lastGamePath.get(i));
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
        File keyfile = new File(lastGamePath.get(selected)); // File is used intentionally
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
      private int supportedGames;

      public ResInfo(StructureFactory.ResType id, String text) {
        this(id, text, new int[]{ResourceFactory.ID_BG1, ResourceFactory.ID_BG1TOTSC, ResourceFactory.ID_TORMENT,
                                 ResourceFactory.ID_ICEWIND, ResourceFactory.ID_ICEWINDHOW,
                                 ResourceFactory.ID_ICEWINDHOWTOT, ResourceFactory.ID_ICEWIND2,
                                 ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                                 ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE});
      }

      public ResInfo(StructureFactory.ResType id, String text, int[] games) {
        resId = id;
        label = text;
        supportedGames = 0;
        if (games != null)
          for (final int g : games)
            supportedGames |= 1 << g;
      }

      public boolean gameSupported(int game) {
        return (game >= 0 && game < 32 && (supportedGames & (1 << game)) != 0);
      }
    }

    private static final ResInfo RESOURCE[] = {
      new ResInfo(StructureFactory.ResType.RES_2DA, "2DA"),
      new ResInfo(StructureFactory.ResType.RES_ARE, "ARE"),
      new ResInfo(StructureFactory.ResType.RES_BCS, "BCS"),
      new ResInfo(StructureFactory.ResType.RES_BIO, "BIO",
                  new int[]{ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_CHR, "CHR",
                  new int[]{ResourceFactory.ID_BG1, ResourceFactory.ID_BG1TOTSC,
                            ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_ICEWIND,
                            ResourceFactory.ID_ICEWINDHOW, ResourceFactory.ID_ICEWINDHOWTOT,
                            ResourceFactory.ID_ICEWIND2, ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_CRE, "CRE"),
      new ResInfo(StructureFactory.ResType.RES_EFF, "EFF",
                  new int[]{ResourceFactory.ID_BG1, ResourceFactory.ID_BG1TOTSC,
                            ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_IDS, "IDS"),
      new ResInfo(StructureFactory.ResType.RES_ITM, "ITM"),
      new ResInfo(StructureFactory.ResType.RES_INI, "INI",
                  new int[]{ResourceFactory.ID_TORMENT, ResourceFactory.ID_ICEWIND,
                            ResourceFactory.ID_ICEWINDHOW, ResourceFactory.ID_ICEWINDHOWTOT,
                            ResourceFactory.ID_ICEWIND2}),
      new ResInfo(StructureFactory.ResType.RES_PRO, "PRO",
                  new int[]{ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_RES, "RES",
                  new int[]{ResourceFactory.ID_ICEWIND, ResourceFactory.ID_ICEWINDHOW,
                            ResourceFactory.ID_ICEWINDHOWTOT, ResourceFactory.ID_ICEWIND2}),
      new ResInfo(StructureFactory.ResType.RES_SPL, "SPL"),
      new ResInfo(StructureFactory.ResType.RES_SRC, "SRC",
                  new int[]{ResourceFactory.ID_TORMENT, ResourceFactory.ID_ICEWIND2}),
      new ResInfo(StructureFactory.ResType.RES_STO, "STO"),
      new ResInfo(StructureFactory.ResType.RES_VEF, "VEF",
                  new int[]{ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_VVC, "VVC",
                  new int[]{ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_WED, "WED"),
      new ResInfo(StructureFactory.ResType.RES_WFX, "WFX",
                  new int[]{ResourceFactory.ID_BG2, ResourceFactory.ID_BG2TOB,
                            ResourceFactory.ID_BGEE, ResourceFactory.ID_BG2EE}),
      new ResInfo(StructureFactory.ResType.RES_WMAP, "WMAP"),
    };

    private final JMenu newFileMenu;
    private final JMenuItem fileOpenNew, fileExport, fileAddCopy, fileRename, fileDelete;

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
    }

    private void gameLoaded()
    {
      if (newFileMenu != null) {
        newFileMenu.removeAll();

        for (final ResInfo res : RESOURCE) {
          if (res.gameSupported(ResourceFactory.getGameID())) {
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
      }
      else if (event.getSource() == fileExport)
        ResourceFactory.getInstance().exportResource(
                NearInfinity.getInstance().getResourceTree().getSelected(), NearInfinity.getInstance());
      else if (event.getSource() == fileAddCopy)
        ResourceFactory.getInstance().saveCopyOfResource(
                NearInfinity.getInstance().getResourceTree().getSelected());
      else if (event.getSource() == fileRename) {
        FileResourceEntry entry = (FileResourceEntry)NearInfinity.getInstance().getResourceTree().getSelected();
        String filename = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter new filename",
                                                      "Rename " + entry.toString(),
                                                      JOptionPane.QUESTION_MESSAGE);
        if (filename == null)
          return;
        if (!filename.toUpperCase().endsWith(entry.getExtension()))
          filename = filename + '.' + entry.getExtension();
        if (new FileCI(entry.getActualFile().getParentFile(), filename).exists()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "File already exists!", "Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        entry.renameFile(filename);
        ResourceFactory.getInstance().getResources().resourceEntryChanged(entry);
      }
      else if (event.getSource() == fileDelete) {
        FileResourceEntry entry = (FileResourceEntry)NearInfinity.getInstance().getResourceTree().getSelected();
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
      fileDelete.setEnabled(entry instanceof FileResourceEntry);
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
      editString2.setEnabled(new FileCI(ResourceFactory.getTLKRoot(), "dialogF.tlk").exists());
      editVarVar.setEnabled(NIFile.getFile(ResourceFactory.getRootDirs(), "VAR.VAR").exists());
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
        File file = new FileCI(ResourceFactory.getTLKRoot(), "dialogF.tlk");
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
                                      NIFile.getFile(ResourceFactory.getRootDirs(), "VAR.VAR"))));
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
    private final String TEXTSEARCH[] = {"2DA", "BCS", "DLG", "IDS"};
    private final JMenuItem searchString, searchFile;

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

      JMenu textSearchMenu = new JMenu("Text Search");
      textSearchMenu.setIcon(Icons.getIcon("Edit16.gif"));
      for (final String type : TEXTSEARCH) {
        JMenuItem textSearch = new JMenuItem(type);
        textSearch.addActionListener(this);
        textSearch.setActionCommand(type);
        textSearchMenu.add(textSearch);
      }
      add(textSearchMenu);
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
      else {
        for (final String type : TEXTSEARCH) {
          if (event.getActionCommand().equals(type)) {
            if (event.getActionCommand().equals("DLG"))
              new DialogSearcher(ResourceFactory.getInstance().getResources(type),
                                 getTopLevelAncestor());
            else
              new TextResourceSearcher(ResourceFactory.getInstance().getResources(type),
                                       getTopLevelAncestor());
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

      JMenu checkMenu = new JMenu("Check Triggers & Actions For");
      checkMenu.setIcon(Icons.getIcon("Refresh16.gif"));
      toolCheckAllDialog = new JMenuItem("All Dialogues");
      toolCheckAllDialog.addActionListener(this);
      checkMenu.add(toolCheckAllDialog);
      toolCheckOverrideDialog = new JMenuItem("Override Dialogues Only");
      toolCheckOverrideDialog.addActionListener(this);
      checkMenu.add(toolCheckOverrideDialog);
      add(checkMenu);

      toolCheckScripts =
      makeMenuItem("Check Scripts", KeyEvent.VK_S, Icons.getIcon("Refresh16.gif"), -1, this);
      add(toolCheckScripts);

      toolCheckCREInv =
      makeMenuItem("Find CRE Items Not in Inventory", KeyEvent.VK_C, Icons.getIcon("Refresh16.gif"), -1,
                   this);
      toolCheckCREInv.setToolTipText("Reports items present in the file but not in the inventory");
      add(toolCheckCREInv);

      toolCheckResRef =
      makeMenuItem("Find Illegal ResourceRefs...", KeyEvent.VK_R, Icons.getIcon("Find16.gif"), -1, this);
      toolCheckResRef.setToolTipText("Reports resource references pointing to nonexistent files");
      add(toolCheckResRef);

      JMenu findMenu = new JMenu("Find Unknown IDS References In");
      findMenu.setIcon(Icons.getIcon("Find16.gif"));
      toolCheckIDSBCSRef = new JMenuItem("BCS & BS Files");
      toolCheckIDSBCSRef.addActionListener(this);
      findMenu.add(toolCheckIDSBCSRef);
      toolCheckIDSRef = new JMenuItem("Other Files...");
      toolCheckIDSRef.addActionListener(this);
      findMenu.add(toolCheckIDSRef);
      add(findMenu);
      findMenu.setToolTipText("Reports IDS references to unknown IDS values");
      toolCheckIDSBCSRef.setToolTipText("Note: GTimes, Time, Scroll, ShoutIDs, and Specific are ignored");
      toolCheckIDSRef.setToolTipText("Note: \"0\" references are ignored");

      toolCheckStructs =
      makeMenuItem("Find Corrupted Files...", KeyEvent.VK_F, Icons.getIcon("Find16.gif"), -1, this);
      toolCheckStructs.setToolTipText("Reports structured files with partially overlapping subsections");
      add(toolCheckStructs);

      toolCheckStringUse =
      makeMenuItem("Find Unused Strings", KeyEvent.VK_U, Icons.getIcon("Find16.gif"), -1, this);
      add(toolCheckStringUse);

      toolCheckFileUse = makeMenuItem("Find Unused Files...", -1, Icons.getIcon("Find16.gif"), -1, this);
      add(toolCheckFileUse);

      toolCheckEffectsIndex = makeMenuItem("Find Mis-indexed Effects", -1,
                                           Icons.getIcon("Find16.gif"), -1, this);
      add(toolCheckEffectsIndex);

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
      toolConsole.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
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
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
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
    }
  }

  ///////////////////////////////
  // Help Menu
  ///////////////////////////////

  private static final class HelpMenu extends JMenu implements ActionListener
  {
    private final JMenuItem helpAbout, helpLicense, helpBsdLicense,
      helpJOrbisLicense;

    private HelpMenu()
    {
      super("Help");
      setMnemonic(KeyEvent.VK_H);

      helpAbout = makeMenuItem("About Near Infinity", KeyEvent.VK_A, Icons.getIcon("About16.gif"), -1, this);
      add(helpAbout);

      helpLicense =
      makeMenuItem("Near Infinity License", KeyEvent.VK_N, Icons.getIcon("Edit16.gif"), -1, this);
      add(helpLicense);

      helpBsdLicense =
      makeMenuItem("Plastic XP License", KeyEvent.VK_P, Icons.getIcon("Edit16.gif"), -1, this);
      add(helpBsdLicense);

      helpJOrbisLicense =
        makeMenuItem("JOrbis License", KeyEvent.VK_J, Icons.getIcon("Edit16.gif"), -1, this);
      add(helpJOrbisLicense);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == helpAbout) {
        final String hauglidPage = "http://www.idi.ntnu.no/~joh/ni/";
        final String githubPage = "https://github.com/NearInfinityBrowser/NearInfinity/";
        final String versionText = "Near Infinity " + VERSION;
        final String githubHTML = "<html><a href=" + githubPage + "/>" +
          githubPage + "</a></html>";
        final String hauglidVersionText = "From Near Infinity 1.32.1 beta 24";
        final String hauglidCopyrightText = "Copyright (\u00A9) 2001-2005 - Jon Olav Hauglid";
        final String hauglidHTML = "<html><a href=" + hauglidPage + "/>" +
          hauglidPage + "</a></html>";

        //TODO: add list of contributors

        JLabel version = new JLabel(versionText);
        JLabel githubLink = new JLabel(githubHTML, JLabel.CENTER);
        githubLink.addMouseListener(new UrlBrowser(githubPage));
        githubLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel hauglidVersion = new JLabel(hauglidVersionText, JLabel.CENTER);
        JLabel hauglidCopyright = new JLabel(hauglidCopyrightText, JLabel.CENTER);
        JLabel hauglidLink = new JLabel(hauglidHTML, JLabel.CENTER);
        hauglidLink.addMouseListener(new UrlBrowser(hauglidPage));
        hauglidLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font defaultfont = version.getFont();
        Font boldFont = defaultfont.deriveFont(Font.BOLD, 20.0f);
        Font font = defaultfont.deriveFont(13.0f);

        version.setFont(boldFont);
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

        JLabel label4 = new JLabel("This program is free and may be distributed according", JLabel.CENTER);
        JLabel label5 = new JLabel("to the terms of the GNU Lesser General Public License.", JLabel.CENTER);
        JLabel label6 = new JLabel("Most icons (\u00A9) eclipse.org - Common Public License.", JLabel.CENTER);
        JLabel label7 = new JLabel(
                "Plastic XP L&F (\u00A9) jgoodies.com - Berkeley Software Distribution License.", JLabel.CENTER);
        JLabel label8 = new JLabel("JOrbis (\u00A9) JCraft Inc. - GNU Lesser General Public License.", JLabel.CENTER);
        Font smallFont = defaultfont.deriveFont(11.0f);
        label4.setFont(smallFont);
        label5.setFont(smallFont);
        label6.setFont(smallFont);
        label7.setFont(smallFont);
        label8.setFont(smallFont);
        gbc.insets = new Insets(3, 6, 0, 6);
        gbl.setConstraints(label4, gbc);
        panel.add(label4);
        gbc.insets.top = 0;
        gbl.setConstraints(label5, gbc);
        panel.add(label5);
        gbc.insets.top = 6;
        gbl.setConstraints(label6, gbc);
        panel.add(label6);
        gbc.insets.top = 0;
        gbl.setConstraints(label7, gbc);
        panel.add(label7);
        gbl.setConstraints(label8, gbc);
        panel.add(label8);

        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                      panel, "About Near Infinity",
                                      JOptionPane.PLAIN_MESSAGE);
      }
      else if (event.getSource() == helpLicense) {
        displayLicense(NearInfinity.class, "License.txt", "LGPL License");
      }
      else if (event.getSource() == helpBsdLicense) {
        displayLicense(NearInfinity.class, "bsd-license.txt", "BSD License");
      }
      else if (event.getSource() == helpJOrbisLicense) {
        displayLicense(NearInfinity.class, "License.txt", "LGPL License");
      }
    }

    private void displayLicense(Class<? extends Object> c, String resource, String title)
    {
      JPanel panel = new JPanel(new BorderLayout());
      JTextPane tphelp = new JTextPane();
      tphelp.setEditable(false);
      tphelp.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      panel.add(new JScrollPane(tphelp), BorderLayout.CENTER);
      panel.setPreferredSize(new Dimension(500, 400));

      try {
        tphelp.setPage(c.getResource(resource));
      } catch (IOException e) {
        e.printStackTrace();
      }

      JOptionPane.showMessageDialog(NearInfinity.getInstance(), panel, title,
                                    JOptionPane.PLAIN_MESSAGE);
    }
  }
}
