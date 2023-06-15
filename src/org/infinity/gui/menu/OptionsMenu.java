// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.infinity.NearInfinity;
import org.infinity.gui.DataRadioButtonMenuItem;
import org.infinity.gui.FontChooser;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.CharsetDetector;
import org.infinity.util.Misc;
import org.infinity.util.Platform;
import org.infinity.util.StringTable;
import org.infinity.util.tuples.Couple;

/**
 * Handles Option menu items for the {@link BrowserMenuBar}.
 */
public class OptionsMenu extends JMenu implements BrowserSubMenu, ActionListener, ItemListener {
  // List of predefined font sizes (-1 indicates a custom value)
  private static final int[] FONT_SIZES = { 50, 75, 100, 125, 150, 175, 200, 250, 300, 400, -1 };

  // List of predefined ui scaling factors (-1 indicates a custom value)
  private static final int[] UI_SCALING = { 100, 125, 133, 150, 175, 200, 250, 300, 400, -1 };

  // List of predefined script text fonts
  private static final Font[] FONTS = {
      new Font(Font.MONOSPACED, Font.PLAIN, 12),
      new Font(Font.SERIF, Font.PLAIN, 12),
      new Font(Font.SANS_SERIF, Font.PLAIN, 12),
      new Font(Font.DIALOG, Font.PLAIN, 12),
      null, // custom font selection
  };

  private static final String DEFAULT_CHARSET = "Auto";

  private static final List<String[]> CHARSETS_USED = new ArrayList<>();

  static {
    // Order: Display name, Canonical charset name, Tooltip
    CHARSETS_USED.add(new String[] { "UTF-8", "UTF-8",
        "The character set of choice for the Enhanced Editions of the Baldur's Gate games." });
    CHARSETS_USED.add(new String[] { "Windows-1252", "windows-1252",
        "Character set used in English and other Latin-based languages, such as French, German, Italian or Spanish." });
    CHARSETS_USED.add(new String[] { "Windows-1251", "windows-1251",
        "Character set used in Russian and other Cyrillic-based languages." });
    CHARSETS_USED.add(new String[] { "Windows-1250", "windows-1250",
        "Character set used in Central European and Eastern European languages, such as Polish or Czech." });
    CHARSETS_USED.add(new String[] { "Windows-31J", "windows-31j", "Character set used in Japanese localizations." });
    CHARSETS_USED.add(new String[] { "GBK", "GBK", "Character set for Simplified Chinese text." });
    CHARSETS_USED.add(new String[] { "Big5-HKSCS", "Big5-HKSCS",
        "Character set for Traditional Chinese text (may not be fully compatible)." });
    CHARSETS_USED.add(new String[] { "IBM-949", "x-IBM949", "Character set used in Korean localizations." });
  }

  private static final String OPTION_SHOWOFFSETS              = "ShowOffsets";
  private static final String OPTION_SHOWOFFSETSRELATIVE      = "ShowOffsetsRelative";
  private static final String OPTION_SHOWSIZE                 = "ShowSize";
  private static final String OPTION_SHOWSIZEHEX              = "ShowSizeHex";
  private static final String OPTION_BACKUPONSAVE             = "BackupOnSave";
  private static final String OPTION_IGNOREOVERRIDE           = "IgnoreOverride";
  private static final String OPTION_IGNOREREADERRORS         = "IgnoreReadErrors";
  private static final String OPTION_LAUNCHGAMEALLOWED        = "LaunchGameAllowed";
  private static final String OPTION_SHOWUNKNOWNRESOURCES     = "ShowUnknownResources";
  private static final String OPTION_SHOWTREESEARCHNAMES      = "ShowTreeSearchNames";
  private static final String OPTION_HIGHLIGHT_OVERRIDDEN     = "HighlightOverridden";
  private static final String OPTION_CACHEOVERRIDE            = "CacheOverride";
  private static final String OPTION_KEEPVIEWONCOPY           = "UpdateTreeOnCopy";
  private static final String OPTION_SHOWSTRREFS              = "ShowStrrefs";
  private static final String OPTION_SHOWCOLOREDSTRUCTURES    = "ShowColoredStructures";
  private static final String OPTION_SHOWHEXCOLORED           = "ShowHexColored";
  private static final String OPTION_SHOWSYSINFO              = "ShowSysInfo";
  private static final String OPTION_OPENBOOKMARKSPROMPT      = "OpenBookmarksPrompt";
  private static final String OPTION_REMEMBER_CHILDFRAME_RECT = "RememberChildFrameRect";
  private static final String OPTION_AUTOCHECK_BCS            = "AutocheckBCS";
  private static final String OPTION_AUTOGEN_BCS_COMMENTS     = "AutogenBCSComments";
  private static final String OPTION_MORECOMPILERWARNINGS     = "MoreCompilerWarnings";
//  private static final String OPTION_MONITORFILECHANGES        = "MonitorFileChanges";
  private static final String OPTION_SHOWOVERRIDES            = "ShowOverridesIn";
  private static final String OPTION_SHOWRESREF               = "ShowResRef";
  private static final String OPTION_LOOKANDFEELCLASS         = "LookAndFeelClass";
  private static final String OPTION_VIEWOREDITSHOWN          = "ViewOrEditShown";
  private static final String OPTION_FONT                     = "Font";
  private static final String OPTION_FONT_NAME                = "FontName";
  private static final String OPTION_FONT_STYLE               = "FontStyle";
  private static final String OPTION_FONT_SIZE                = "FontSize";
  private static final String OPTION_TLKCHARSET               = "TLKCharsetType";
  private static final String OPTION_LANGUAGE_GAMES           = "GameLanguages";

  /** This preferences key can be used internally to reset incorrectly set default values after a public release. */
  private static final String OPTION_OPTION_FIXED             = "OptionFixedInternal";

  /** Mask used for one-time resets of options (kept track of in OPTION_OPTION_FIXED). */
//    private static final int MASK_OPTION_FIXED_AUTO_INDENT = 0x00000001;

  /** Identifier for autodetected game language. */
  private static final String LANGUAGE_AUTODETECT = "Auto";

  private final List<DataRadioButtonMenuItem> lookAndFeel = new ArrayList<>();

  private final BrowserMenuBar menuBar;

  private final JRadioButtonMenuItem[] showOverrides = new JRadioButtonMenuItem[OverrideMode.values().length];
  private final JRadioButtonMenuItem[] showResRef = new JRadioButtonMenuItem[ResRefMode.values().length];
  private final JRadioButtonMenuItem[] viewOrEditShown = new JRadioButtonMenuItem[ViewMode.values().length];
  private final JRadioButtonMenuItem[] selectFont = new JRadioButtonMenuItem[FONTS.length];
  private final DataRadioButtonMenuItem[] globalFontSize = new DataRadioButtonMenuItem[FONT_SIZES.length];
  private final DataRadioButtonMenuItem[] globalUiScaling = new DataRadioButtonMenuItem[UI_SCALING.length];

  /** Stores available languages in BG(2)EE. */
  private final HashMap<JRadioButtonMenuItem, String> gameLanguage = new HashMap<>();

  private final JMenu mCharsetMenu;
  private final JMenu mLanguageMenu;

  private final JCheckBoxMenuItem optionAutocheckBCS;
  private final JCheckBoxMenuItem optionMoreCompileWarnings;
  private final JCheckBoxMenuItem optionAutogenBCSComments;

  private final TextEditorMenu textEditorMenu;
  private final DialogViewerMenu dialogViewerMenu;

  private final JCheckBoxMenuItem optionBackupOnSave;
  private final JCheckBoxMenuItem optionShowOffset;
  private final JCheckBoxMenuItem optionShowSize;
  private final JCheckBoxMenuItem optionSizeInHex;
  private final JCheckBoxMenuItem optionOffsetRelative;
  private final JCheckBoxMenuItem optionIgnoreOverride;
  private final JCheckBoxMenuItem optionIgnoreReadErrors;
  private final JCheckBoxMenuItem optionCacheOverride;
  private final JCheckBoxMenuItem optionShowStrrefs;
  private final JCheckBoxMenuItem optionShowColoredStructures;
  private final JCheckBoxMenuItem optionShowHexColored;
  private final JCheckBoxMenuItem optionShowSystemInfo;
  private final JCheckBoxMenuItem optionOpenBookmarksPrompt;
  private final JCheckBoxMenuItem optionRememberChildFrameRect;
  private final JCheckBoxMenuItem optionShowUnknownResources;
  private final JCheckBoxMenuItem optionKeepViewOnCopy;
  private final JCheckBoxMenuItem optionTreeSearchNames;
  private final JCheckBoxMenuItem optionHighlightOverridden;
  private final JCheckBoxMenuItem optionLaunchGameAllowed;
  private final JCheckBoxMenuItem optionUiScalingEnabled;
//    private final JCheckBoxMenuItem optionMonitorFileChanges;

  private ButtonGroup bgCharsetButtons;
  private String languageDefinition;

  private int optionFixedInternal;

  public OptionsMenu(BrowserMenuBar parent) {
    super("Options");
    setMnemonic(KeyEvent.VK_O);

    menuBar = parent;

    optionFixedInternal = getMenuBar().getPrefs().getInt(OPTION_OPTION_FIXED, 0);

    // Options
    optionBackupOnSave = new JCheckBoxMenuItem("Backup on save", getMenuBar().getPrefs().getBoolean(OPTION_BACKUPONSAVE, false));
    optionBackupOnSave
        .setToolTipText("Enable this option to automatically create a backup " + "of the resource you want to save.");
    add(optionBackupOnSave);
    optionLaunchGameAllowed = new JCheckBoxMenuItem("Allow launching games",
        getMenuBar().getPrefs().getBoolean(OPTION_LAUNCHGAMEALLOWED, true));
    optionLaunchGameAllowed.setToolTipText("Enabling this option allows you to launch the game executable "
        + "associated with the current game from within Near Infinity.");
    optionLaunchGameAllowed.addActionListener(this);
    add(optionLaunchGameAllowed);
    optionIgnoreOverride = new JCheckBoxMenuItem("Ignore Overrides",
        getMenuBar().getPrefs().getBoolean(OPTION_IGNOREOVERRIDE, false));
    add(optionIgnoreOverride);
    optionIgnoreReadErrors = new JCheckBoxMenuItem("Ignore read errors",
        getMenuBar().getPrefs().getBoolean(OPTION_IGNOREREADERRORS, false));
    add(optionIgnoreReadErrors);
    optionShowUnknownResources = new JCheckBoxMenuItem("Show unknown resource types",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWUNKNOWNRESOURCES, true));
    optionShowUnknownResources.setActionCommand("Refresh");
    optionShowUnknownResources.addActionListener(NearInfinity.getInstance());
    optionShowUnknownResources
        .setToolTipText("Uncheck this option to hide unknown or unsupported resource types and invalid filenames.");
    add(optionShowUnknownResources);

    // Options > Show table columns
    JMenu tableMenu = new JMenu("Show table columns");
    add(tableMenu);
    optionShowOffset = new JCheckBoxMenuItem("Show field offsets", getMenuBar().getPrefs().getBoolean(OPTION_SHOWOFFSETS, true));
    optionShowOffset.setToolTipText(
        "If checked, absolute field offsets are shown in a separate \"Offset\" column in structured resources.");
    optionShowOffset.addActionListener(this);
    tableMenu.add(optionShowOffset);
    optionOffsetRelative = new JCheckBoxMenuItem("Show relative field offsets",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWOFFSETSRELATIVE, false));
    optionOffsetRelative.setToolTipText("If checked, offsets relative to the parent structure are additionally shown "
        + "in parentheses in the \"Offset\" column for fields in substructures.");
    optionOffsetRelative.setEnabled(optionShowOffset.isSelected());
    tableMenu.add(optionOffsetRelative);
    optionShowSize = new JCheckBoxMenuItem("Show field sizes", getMenuBar().getPrefs().getBoolean(OPTION_SHOWSIZE, true));
    optionShowSize.setToolTipText(
        "If checked, field sizes in bytes are shown in a separate \"Size\" column in structured resources.");
    optionShowSize.addActionListener(this);
    tableMenu.add(optionShowSize);
    optionSizeInHex = new JCheckBoxMenuItem("Field size as hex number",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWSIZEHEX, true));
    optionSizeInHex.setToolTipText("If checked, field sizes are shown in hexadecimal notation.");
    optionSizeInHex.setEnabled(optionShowSize.isSelected());
    tableMenu.add(optionSizeInHex);

    optionTreeSearchNames = new JCheckBoxMenuItem("Show search names in resource tree",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWTREESEARCHNAMES, true));
    optionTreeSearchNames.setActionCommand("RefreshTree");
    optionTreeSearchNames.addActionListener(NearInfinity.getInstance());
    add(optionTreeSearchNames);
    optionHighlightOverridden = new JCheckBoxMenuItem("Show overridden files in bold in resource tree",
        getMenuBar().getPrefs().getBoolean(OPTION_HIGHLIGHT_OVERRIDDEN, true));
    optionHighlightOverridden.setActionCommand("RefreshTree");
    optionHighlightOverridden.addActionListener(NearInfinity.getInstance());
    optionHighlightOverridden.setToolTipText(
        "<html>If checked, files that are listed in the chitin.key and are located in the Override folder, will be shown<br>"
            + "in <b>bold</b> in the Resource Tree. This setting has no effect if override files are shown only in the Override folder.</html>");
    add(optionHighlightOverridden);
    // optionMonitorFileChanges =
    // new JCheckBoxMenuItem("Autoupdate resource tree", getPrefs().getBoolean(OPTION_MONITORFILECHANGES, true));
    // optionMonitorFileChanges.addActionListener(this);
    // optionMonitorFileChanges.setToolTipText("Automatically updates the resource tree whenever a file change occurs
    // in any supported override folders.");
    // add(optionMonitorFileChanges);
    optionCacheOverride = new JCheckBoxMenuItem("Autocheck for Overrides",
        getMenuBar().getPrefs().getBoolean(OPTION_CACHEOVERRIDE, false));
    optionCacheOverride.setToolTipText("Without this option selected, Refresh Tree is required "
        + "to discover new override files added while NI is open");
    add(optionCacheOverride);
    optionKeepViewOnCopy = new JCheckBoxMenuItem("Keep view after copy operations",
        getMenuBar().getPrefs().getBoolean(OPTION_KEEPVIEWONCOPY, false));
    optionKeepViewOnCopy.setToolTipText(
        "With this option enabled the resource tree will not switch to the new resource created by an \"Add Copy Of\" operation.");
    add(optionKeepViewOnCopy);
    optionShowStrrefs = new JCheckBoxMenuItem("Show Strrefs in View tabs",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWSTRREFS, false));
    add(optionShowStrrefs);
    optionShowColoredStructures = new JCheckBoxMenuItem("Show colored structures in Edit tabs",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWCOLOREDSTRUCTURES, true));
    optionShowColoredStructures.setActionCommand("RefreshView");
    optionShowColoredStructures.addActionListener(NearInfinity.getInstance());
    add(optionShowColoredStructures);
    optionShowHexColored = new JCheckBoxMenuItem("Show colored blocks in Raw tabs",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWHEXCOLORED, true));
    add(optionShowHexColored);
    optionShowSystemInfo = new JCheckBoxMenuItem("Display system information at startup",
        getMenuBar().getPrefs().getBoolean(OPTION_SHOWSYSINFO, true));
    add(optionShowSystemInfo);
    optionOpenBookmarksPrompt = new JCheckBoxMenuItem("Confirm opening bookmarked games",
        getMenuBar().getPrefs().getBoolean(OPTION_OPENBOOKMARKSPROMPT, true));
    add(optionOpenBookmarksPrompt);
    optionRememberChildFrameRect = new JCheckBoxMenuItem("Remember last child frame size and position",
        getMenuBar().getPrefs().getBoolean(OPTION_REMEMBER_CHILDFRAME_RECT, false));
    optionRememberChildFrameRect.setToolTipText(
        "<html>With this option enabled, placement of the last opened child window will be reused for new child windows.<br/>"
        + "This information will only be valid for the current Near Infinity session.</html>");
    add(optionRememberChildFrameRect);

    addSeparator();

    // Options > Script Compiler
    JMenu compilerMenu = new JMenu("Script Compiler");
    add(compilerMenu);
    optionAutocheckBCS = new JCheckBoxMenuItem("Autocheck BCS", getMenuBar().getPrefs().getBoolean(OPTION_AUTOCHECK_BCS, true));
    optionAutocheckBCS.setToolTipText("Automatically scans scripts for compile error with this option enabled.");
    compilerMenu.add(optionAutocheckBCS);
    optionMoreCompileWarnings = new JCheckBoxMenuItem("Show more compiler warnings",
        getMenuBar().getPrefs().getBoolean(OPTION_MORECOMPILERWARNINGS, false));
    optionMoreCompileWarnings.setToolTipText("Script compiler will generate an additional set of less severe "
        + "warning messages with this option enabled.");
    compilerMenu.add(optionMoreCompileWarnings);
    optionAutogenBCSComments = new JCheckBoxMenuItem("Autogenerate BCS comments",
        getMenuBar().getPrefs().getBoolean(OPTION_AUTOGEN_BCS_COMMENTS, true));
    compilerMenu.add(optionAutogenBCSComments);

    // Options > Text Editor
    textEditorMenu = new TextEditorMenu(this, getMenuBar().getPrefs());
    add(textEditorMenu);

    // Options > Dialog Viewer
    dialogViewerMenu = new DialogViewerMenu(getMenuBar().getPrefs());
    add(dialogViewerMenu);

    addSeparator();

    // Options > Show ResourceRefs As
    JMenu showresrefmenu = new JMenu("Show ResourceRefs As");
    add(showresrefmenu);
    int selectedresref = getMenuBar().getPrefs().getInt(OPTION_SHOWRESREF, ResRefMode.RefName.ordinal());

    ButtonGroup bg = new ButtonGroup();
    for (final ResRefMode mode : ResRefMode.values()) {
      final int i = mode.ordinal();
      final JRadioButtonMenuItem menu = new JRadioButtonMenuItem(mode.getTitle(), i == selectedresref);
      menu.setAccelerator(KeyStroke.getKeyStroke(mode.getKeyCode(), BrowserMenuBar.getCtrlMask()));

      bg.add(menu);
      showresrefmenu.add(menu);
      showResRef[i] = menu;
    }

    // Options->Show Override Files
    JMenu overridesubmenu = new JMenu("Show Override Files");
    add(overridesubmenu);
    int selectedmode = getMenuBar().getPrefs().getInt(OPTION_SHOWOVERRIDES, OverrideMode.Split.ordinal());

    bg = new ButtonGroup();
    for (final OverrideMode mode : OverrideMode.values()) {
      final int i = mode.ordinal();
      final JRadioButtonMenuItem menu = new JRadioButtonMenuItem(mode.getTitle(), i == selectedmode);
      menu.setActionCommand("Refresh");
      menu.addActionListener(NearInfinity.getInstance());

      bg.add(menu);
      overridesubmenu.add(menu);
      showOverrides[i] = menu;
    }
    showOverrides[OverrideMode.Split.ordinal()]
        .setToolTipText("Indexed by Chitin.key => ??? folders; Not indexed => Override folder");

    // Options->Default Structure Display
    JMenu vieworeditmenu = new JMenu("Default Structure Display");
    add(vieworeditmenu);
    final int selectedview = getMenuBar().getPrefs().getInt(OPTION_VIEWOREDITSHOWN, ViewMode.Edit.ordinal());
    bg = new ButtonGroup();
    for (final ViewMode mode : ViewMode.values()) {
      final int i = mode.ordinal();
      final JRadioButtonMenuItem menu = new JRadioButtonMenuItem(mode.getTitle(), i == selectedview);

      bg.add(menu);
      vieworeditmenu.add(menu);
      viewOrEditShown[i] = menu;
    }

    // Options->Override UI Scaling
    JMenu uiScalingMenu = new JMenu("Override UI Scaling");
    add(uiScalingMenu);
    // adding "Enabled" checkbox
    optionUiScalingEnabled = new JCheckBoxMenuItem("Enabled", NearInfinity.getInstance().isUiScalingEnabled());
    optionUiScalingEnabled.setToolTipText("Enabling this option overrides the global scaling factor for UI elements.");
    optionUiScalingEnabled.addActionListener(this);
    uiScalingMenu.add(optionUiScalingEnabled);
    uiScalingMenu.addSeparator();
    // adding scaling factors
    bg = new ButtonGroup();
    uiScalingMenu.addItemListener(this);
    int selectedScale = NearInfinity.getInstance().getUiScalingFactor();
    selectedScale = Math.min(Math.max(selectedScale, 50), 400);
    boolean isCustomScale = true;
    for (int i = 0; i < UI_SCALING.length; i++) {
      int scale = UI_SCALING[i];
      if (scale > 0) {
        String msg = UI_SCALING[i] + " %" + (scale == 100 ? " (Default)" : "");
        globalUiScaling[i] = new DataRadioButtonMenuItem(msg, UI_SCALING[i] == selectedScale,
            Integer.valueOf(UI_SCALING[i]));
        if (UI_SCALING[i] == selectedScale) {
          isCustomScale = false;
        }
      } else {
        String msg = isCustomScale ? "Custom (" + selectedScale + " %)..." : "Custom...";
        globalUiScaling[i] = new DataRadioButtonMenuItem(msg, isCustomScale, isCustomScale ? selectedScale : scale);
      }
      globalUiScaling[i].setEnabled(NearInfinity.getInstance().isUiScalingEnabled());
      globalUiScaling[i].setActionCommand("ChangeUiScaling");
      globalUiScaling[i].addActionListener(this);
      uiScalingMenu.add(globalUiScaling[i]);
      bg.add(globalUiScaling[i]);
    }
    // Option is only available on Java Runtime 9 or higher
    if (Platform.JAVA_VERSION <= 8) {
      uiScalingMenu.setEnabled(false);
      uiScalingMenu.setToolTipText("Only available on Java 9 or higher.");
    }

    // Options->Global Font Size
    JMenu fontSizeMenu = new JMenu("Change Global Font Size");
    add(fontSizeMenu);
    bg = new ButtonGroup();
    fontSizeMenu.addItemListener(this);
    int selectedSize = NearInfinity.getInstance().getGlobalFontSize();
    selectedSize = Math.min(Math.max(selectedSize, 50), 400);
    boolean isCustom = true;
    for (int i = 0; i < FONT_SIZES.length; i++) {
      int size = FONT_SIZES[i];
      if (size > 0) {
        String msg = FONT_SIZES[i] + " %" + (size == 100 ? " (Default)" : "");
        globalFontSize[i] = new DataRadioButtonMenuItem(msg, FONT_SIZES[i] == selectedSize,
            Integer.valueOf(FONT_SIZES[i]));
        if (FONT_SIZES[i] == selectedSize) {
          isCustom = false;
        }
      } else {
        String msg = isCustom ? "Custom (" + selectedSize + " %)..." : "Custom...";
        globalFontSize[i] = new DataRadioButtonMenuItem(msg, isCustom, isCustom ? selectedSize : size);
      }
      globalFontSize[i].setActionCommand("ChangeFontSize");
      globalFontSize[i].addActionListener(this);
      fontSizeMenu.add(globalFontSize[i]);
      bg.add(globalFontSize[i]);
    }

    // Options->Look and Feel
    JMenu lookandfeelmenu = new JMenu("Look and Feel");
    add(lookandfeelmenu);
    final String selectedLF = getMenuBar().getPrefs().get(OPTION_LOOKANDFEELCLASS, BrowserMenuBar.getDefaultLookAndFeel().getClassName());
    LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
    bg = new ButtonGroup();
    if (info != null && info.length > 0) {
      // dynamically create a list of supported look&feel themes
      DataRadioButtonMenuItem dbmi;
      for (LookAndFeelInfo element : info) {
        dbmi = new DataRadioButtonMenuItem(element.getName(), selectedLF.equalsIgnoreCase(element.getClassName()),
            element);
        try {
          // L&F description is only available from class instance
          Class<?> cls = Class.forName(element.getClassName());
          Object o = cls.getDeclaredConstructor().newInstance();
          if (o instanceof LookAndFeel) {
            dbmi.setToolTipText(((LookAndFeel) o).getDescription());
          }
        } catch (Exception ex) {
        }
        lookAndFeel.add(dbmi);
        bg.add(dbmi);
      }
    } else {
      // fallback solution: adding default look&feel theme
      DataRadioButtonMenuItem dbmi;
      dbmi = new DataRadioButtonMenuItem(BrowserMenuBar.getDefaultLookAndFeel().getName(), true, BrowserMenuBar.getDefaultLookAndFeel());
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
    int selectedFont = getMenuBar().getPrefs().getInt(OPTION_FONT, 0);
    selectedFont = Math.min(Math.max(selectedFont, 0), FONTS.length - 1);
    for (int i = 0; i < FONTS.length; i++) {
      if (FONTS[i] != null) {
        selectFont[i] = new JRadioButtonMenuItem(FONTS[i].getName() + ' ' + FONTS[i].getSize(), i == selectedFont);
        // selectFont[i].setFont(Misc.getScaledFont(FONTS[i]));
      } else {
        Font font = null;
        String fontName = getMenuBar().getPrefs().get(OPTION_FONT_NAME, "");
        if (!fontName.isEmpty()) {
          font = new Font(fontName, getMenuBar().getPrefs().getInt(OPTION_FONT_STYLE, Font.PLAIN),
              getMenuBar().getPrefs().getInt(OPTION_FONT_SIZE, 12));
        }
        selectFont[i] = new JRadioButtonMenuItem("Select font...", i == selectedFont);
        selectFont[i].setActionCommand("TextFont");
        selectFont[i].addActionListener(this);
        applyCustomFont(font);
      }
      scriptmenu.add(selectFont[i]);
      bg.add(selectFont[i]);
    }

    // Options->TLK Charset
    String charset = getMenuBar().getPrefs().get(OPTION_TLKCHARSET, DEFAULT_CHARSET);
    if (!charsetAvailable(charset)) {
      System.err.println(String.format("Charset \"%s\" not available.", charset));
      charset = DEFAULT_CHARSET;
    }
    if (!charsetName(charset, false).equals(StringTable.getCharset().name())) {
      StringTable.setCharset(charsetName(charset, false));
    }
    mCharsetMenu = initCharsetMenu(charset);
    add(mCharsetMenu);

    // Options->TLK Language
    mLanguageMenu = new JMenu("TLK Language (EE only)");
    add(mLanguageMenu);
    languageDefinition = getMenuBar().getPrefs().get(OPTION_LANGUAGE_GAMES, "");
  }

  /** Provides access to the "Text Editor" submenu. */
  public TextEditorMenu getTextEditorMenu() {
    return textEditorMenu;
  }

  /** Provides access to the "Dialog Tree Viewer" submenu. */
  public DialogViewerMenu getDialogViewerMenu() {
    return dialogViewerMenu;
  }

  /** Returns the charset string associated with the currently selected charset menuitem. */
  public String getSelectedButtonData() {
    Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
    if (buttonSet != null) {
      while (buttonSet.hasMoreElements()) {
        AbstractButton b = buttonSet.nextElement();
        if (b instanceof DataRadioButtonMenuItem) {
          DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem) b;
          if (dmi.isSelected()) {
            return (dmi.getData() != null) ? (String) dmi.getData() : DEFAULT_CHARSET;
          }
        }
      }
    }
    return DEFAULT_CHARSET;
  }

  /** Attempts to determine the correct charset for the current game. */
  public String charsetName(String charset, boolean detect) {
    if (DEFAULT_CHARSET.equalsIgnoreCase(charset)) {
      charset = CharsetDetector.guessCharset(detect);
    } else {
      charset = CharsetDetector.setCharset(charset);
    }
    return charset;
  }

  public void gameLoaded() {
    // update charset selection
    StringTable.setCharset(charsetName(getSelectedButtonData(), true));
    // update language selection
    resetGameLanguage();
  }

  public void storePreferences() {
    final Preferences prefs = getMenuBar().getPrefs();
    prefs.putInt(OPTION_OPTION_FIXED, optionFixedInternal);
    prefs.putBoolean(OPTION_BACKUPONSAVE, optionBackupOnSave.isSelected());
    prefs.putBoolean(OPTION_LAUNCHGAMEALLOWED, optionLaunchGameAllowed.isSelected());
    prefs.putBoolean(OPTION_IGNOREOVERRIDE, optionIgnoreOverride.isSelected());
    prefs.putBoolean(OPTION_IGNOREREADERRORS, optionIgnoreReadErrors.isSelected());
    prefs.putBoolean(OPTION_SHOWUNKNOWNRESOURCES, optionShowUnknownResources.isSelected());
    prefs.putBoolean(OPTION_SHOWTREESEARCHNAMES, optionTreeSearchNames.isSelected());
    prefs.putBoolean(OPTION_HIGHLIGHT_OVERRIDDEN, optionHighlightOverridden.isSelected());
    prefs.putBoolean(OPTION_CACHEOVERRIDE, optionCacheOverride.isSelected());
    prefs.putBoolean(OPTION_KEEPVIEWONCOPY, optionKeepViewOnCopy.isSelected());
    prefs.putBoolean(OPTION_SHOWSTRREFS, optionShowStrrefs.isSelected());
    prefs.putBoolean(OPTION_SHOWCOLOREDSTRUCTURES, optionShowColoredStructures.isSelected());
    prefs.putBoolean(OPTION_SHOWHEXCOLORED, optionShowHexColored.isSelected());
    prefs.putBoolean(OPTION_SHOWSYSINFO, optionShowSystemInfo.isSelected());
    prefs.putBoolean(OPTION_OPENBOOKMARKSPROMPT, optionOpenBookmarksPrompt.isSelected());
    prefs.putBoolean(OPTION_REMEMBER_CHILDFRAME_RECT, optionRememberChildFrameRect.isSelected());

    prefs.putBoolean(OPTION_SHOWOFFSETS, optionShowOffset.isSelected());
    prefs.putBoolean(OPTION_SHOWOFFSETSRELATIVE, optionOffsetRelative.isSelected());
    prefs.putBoolean(OPTION_SHOWSIZE, optionShowSize.isSelected());
    prefs.putBoolean(OPTION_SHOWSIZEHEX, optionSizeInHex.isSelected());

    prefs.putBoolean(OPTION_AUTOCHECK_BCS, optionAutocheckBCS.isSelected());
    prefs.putBoolean(OPTION_AUTOGEN_BCS_COMMENTS, optionAutogenBCSComments.isSelected());
    prefs.putBoolean(OPTION_MORECOMPILERWARNINGS, optionMoreCompileWarnings.isSelected());

    getTextEditorMenu().storePreferences(prefs);

    getDialogViewerMenu().storePreferences(prefs);

    // getPrefs().putBoolean(OPTION_MONITORFILECHANGES, optionMonitorFileChanges.isSelected());
    prefs.putInt(OPTION_SHOWRESREF, getResRefMode().ordinal());
    prefs.putInt(OPTION_SHOWOVERRIDES, getOverrideMode().ordinal());
    prefs.putInt(OPTION_VIEWOREDITSHOWN, getDefaultStructView().ordinal());
    prefs.put(OPTION_LOOKANDFEELCLASS, getLookAndFeel().getClassName());

    int selectedFont = BrowserMenuBar.getSelectedButtonIndex(selectFont, 0);
    prefs.putInt(OPTION_FONT, selectedFont);
    Font font = FONTS[FONTS.length - 1];
    if (font != null) {
      prefs.put(OPTION_FONT_NAME, font.getName());
      prefs.putInt(OPTION_FONT_STYLE, font.getStyle());
      prefs.putInt(OPTION_FONT_SIZE, font.getSize());
    }

    String charset = getSelectedButtonData();
    prefs.put(OPTION_TLKCHARSET, charset);

    prefs.put(OPTION_LANGUAGE_GAMES, languageDefinition);
  }

  /** Returns the currently selected game language. Returns empty string on autodetect. */
  public String getSelectedGameLanguage() {
    String lang = getGameLanguage(languageDefinition, Profile.getGame());
    return lang.equalsIgnoreCase(LANGUAGE_AUTODETECT) ? "" : lang;
  }

  /** Specifies the enabled state of the "Allow launching games" entry in the Options menu. */
  public void setLaunchGameMenuEnabled(boolean enable) {
    optionLaunchGameAllowed.setEnabled(enable);
  }

  /** Returns whether system information are shown when NI starts up. */
  public boolean showSysInfo() {
    return optionShowSystemInfo.isSelected();
  }

  /** Returns whether to show a dialog prompt whenever a bookmarked game is opened. */
  public boolean showOpenBookmarksPrompt() {
    return optionOpenBookmarksPrompt.isSelected();
  }

  /**
   * Controls whether to show a dialog prompt whenever a bookmarked game is opened.
   *
   * @param show {@code true} to show a dialog prompt, {@code false} to open bookmarked games without confirmation.
   */
  public void setShowOpenBookmarksPrompt(boolean show) {
    optionOpenBookmarksPrompt.setSelected(show);
  }

  /** Returns whether the size and position of the last opened child frame should be reused for new child frames. */
  public boolean rememberChildFrameRect() {
    return optionRememberChildFrameRect.isSelected();
  }

  /** Returns whether scripts are automatically scanned for compile errors. */
  public boolean autocheckBCS() {
    return optionAutocheckBCS.isSelected();
  }

  /** Returns whether helpful comments are generated for decompiled scripts. */
  public boolean autogenBCSComments() {
    return optionAutogenBCSComments.isSelected();
  }

  /** Returns whether search names are displayed alongside resource names in the resource tree. */
  public boolean showTreeSearchNames() {
    return optionTreeSearchNames.isSelected();
  }

  /** Returns whether overridden files are displayed in bold in the resource tree. */
  public boolean highlightOverridden() {
    return optionHighlightOverridden.isSelected();
  }

  /** Returns whether extended compiler warnings are shown for scripts. */
  public boolean showMoreCompileWarnings() {
    return optionMoreCompileWarnings.isSelected();
  }

  /** Returns whether string references are shown alongside strings. */
  public boolean showStrrefs() {
    return optionShowStrrefs.isSelected();
  }

  /** Returns whether the dialog tree viewer shows icons in front of state and response entries. */
  public boolean showDlgTreeIcons() {
    return dialogViewerMenu.showDlgTreeIcons();
  }

  /** Returns whether root states are sorted by processing order (based on state trigger index). */
  public boolean sortStatesByWeight() {
    return dialogViewerMenu.sortStatesByWeight();
  }

  /** Returns whether state 0 is always shown as a root node in the dialog tree viewer. */
  public boolean alwaysShowState0() {
    return dialogViewerMenu.alwaysShowState0();
  }

  /** Returns whether external dialog references are shown with a colored background in the dialog tree viewer. */
  public boolean colorizeOtherDialogs() {
    return dialogViewerMenu.colorizeOtherDialogs();
  }

  /**
   * Returns whether duplicate states are combined and only shown once to break infinite loops in the dialog tree
   * viewer.
   */
  public boolean breakCyclesInDialogs() {
    return dialogViewerMenu.breakCyclesInDialogs();
  }

  /** Returns whether response entries in the dialog tree viewer are shown with a colored background. */
  public boolean useDifferentColorForResponses() {
    return dialogViewerMenu.useDifferentColorForResponses();
  }

  /** Returns whether additional information about the dialog is shown in the dialog tree viewer. */
  public boolean showDlgTechInfo() {
    return dialogViewerMenu.showDlgTechInfo();
  }

  /** Returns whether substructures and their related offset and count fields are colorized for structured resources. */
  public boolean getColoredOffsetsEnabled() {
    return optionShowColoredStructures.isSelected();
  }

  /** Returns whether substructures are colorized in the raw tab of structured resources. */
  public boolean getHexColorMapEnabled() {
    return optionShowHexColored.isSelected();
  }

  /** Returns whether the "Add copy of" operation keeps the original resource selected. */
  public boolean getKeepViewOnCopy() {
    return optionKeepViewOnCopy.isSelected();
  }

  /** Returns whether the "Autocheck for Overrides" option is enabled. */
  public boolean cacheOverride() {
    return optionCacheOverride.isSelected();
  }

  /** Returns whether launching game executables in NI is enabled. */
  public boolean getLauncherEnabled() {
    return optionLaunchGameAllowed.isSelected();
  }

  /** Returns the character encoding of the string table. */
  public String getSelectedCharset() {
    return charsetName(getSelectedButtonData(), true);
  }

  /** Returns whether a backup is created when resources are modified. */
  public boolean backupOnSave() {
    return optionBackupOnSave.isSelected();
  }

  /** Returns whether override files are ignored in the resource tree. */
  public boolean ignoreOverrides() {
    return optionIgnoreOverride.isSelected();
  }

  /** Returns if read errors are shown in the status bar instead of a dialog prompt. */
  public boolean ignoreReadErrors() {
    return optionIgnoreReadErrors.isSelected();
  }

  /** Returns whether unknown or unrecognized resources are displayed in the resource tree. */
  public boolean showUnknownResourceTypes() {
    return optionShowUnknownResources.isSelected();
  }

  /** Returns whether offset column is shown for structured resources. */
  public boolean showTableOffsets() {
    return optionShowOffset.isSelected();
  }

  /** Returns whether relative offsets are shown for fields in substructures. */
  public boolean showTableOffsetsRelative() {
    return optionOffsetRelative.isSelected();
  }

  /** Returns whether size column is shown for structured resources. */
  public boolean showTableSize() {
    return optionShowSize.isSelected();
  }

  /** Returns whether size column is shown in hex (or decimal) for structured resources. */
  public boolean showTableSizeInHex() {
    return optionSizeInHex.isSelected();
  }

  public ResRefMode getResRefMode() {
    for (ResRefMode mode : ResRefMode.values()) {
      if (showResRef[mode.ordinal()].isSelected()) {
        return mode;
      }
    }
    return ResRefMode.RefName;
  }

  public OverrideMode getOverrideMode() {
    for (OverrideMode mode : OverrideMode.values()) {
      if (showOverrides[mode.ordinal()].isSelected()) {
        return mode;
      }
    }
    return OverrideMode.Split;
  }

  /** Returns the selected font of scripts. */
  public Font getScriptFont() {
    for (int i = 0; i < FONTS.length; i++) {
      if (selectFont[i].isSelected()) {
        return FONTS[i];
      }
    }
    return FONTS[0];
  }

  public LookAndFeelInfo getLookAndFeel() {
    for (DataRadioButtonMenuItem element : lookAndFeel) {
      if (element != null && element.isSelected()) {
        return (LookAndFeelInfo) element.getData();
      }
    }
    return BrowserMenuBar.getDefaultLookAndFeel();
  }

  public boolean isUiScalingEnabled() {
    return optionUiScalingEnabled.isSelected();
  }

  public int getUiScalingFactor() {
    return ((Integer) globalUiScaling[BrowserMenuBar.getSelectedButtonIndex(globalUiScaling, 0)].getData());
  }

  public int getGlobalFontSize() {
    return ((Integer) globalFontSize[BrowserMenuBar.getSelectedButtonIndex(globalFontSize, 2)].getData());
  }

  public ViewMode getDefaultStructView() {
    for (ViewMode mode : ViewMode.values()) {
      if (viewOrEditShown[mode.ordinal()].isSelected()) {
        return mode;
      }
    }
    return ViewMode.Edit;
  }

  /**
   * Returns whether file changes in override folders are tracked at real time and reflected in the resource tree.
   *
   * (not yet implemented)
   */
  public boolean getMonitorFileChanges() {
//      return optionMonitorFileChanges.isSelected();
    return false;
  }

  /** Returns defValue if masked bit is clear or value if masked bit is already set. */
  public boolean fixOption(int mask, boolean defValue, boolean value) {
    boolean retVal = value;
    if ((optionFixedInternal & mask) == 0) {
      retVal = defValue;
      optionFixedInternal |= mask;
    }
    return retVal;
  }

  @Override
  public BrowserMenuBar getMenuBar() {
    return menuBar;
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    // if (event.getSource() == optionMonitorFileChanges) {
    //   if (optionMonitorFileChanges.isSelected()) {
    //     FileWatcher.getInstance().start();
    //   } else {
    //     FileWatcher.getInstance().stop();
    //   }
    // } else if (event.getSource() == selectFont[selectFont.length - 1]) {
    if (event.getSource() == optionShowOffset) {
      optionOffsetRelative.setEnabled(optionShowOffset.isSelected());
    } else if (event.getSource() == optionLaunchGameAllowed) {
      NearInfinity.getInstance().updateLauncher();
    } else if (event.getSource() == optionShowSize) {
      optionSizeInHex.setEnabled(optionShowSize.isSelected());
    } else if (event.getSource() == optionUiScalingEnabled) {
      final boolean isEnabled = optionUiScalingEnabled.isSelected();
      for (int i = 0; i < globalUiScaling.length; i++) {
        globalUiScaling[i].setEnabled(isEnabled);
      }
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
          "You have to restart Near Infinity\n" + "for the UI scale override setting to take effect.");
    } else if (event.getActionCommand().equals("TextFont")) {
      int index = FONTS.length - 1;
      FontChooser fc = new FontChooser();
      if (FONTS[index] != null) {
        fc.setSelectedFont(FONTS[index]);
      }
      if (fc.showDialog(NearInfinity.getInstance()) == FontChooser.OK_OPTION) {
        applyCustomFont(fc.getSelectedFont());
      }
    } else if (event.getActionCommand().equals("ChangeUiScaling")) {
      applyUiScaling((DataRadioButtonMenuItem) event.getSource());
    } else if (event.getActionCommand().equals("ChangeFontSize")) {
      applyGlobalFontSize((DataRadioButtonMenuItem) event.getSource());
    } else if (event.getActionCommand().equals("Charset")) {
      DataRadioButtonMenuItem dmi = (DataRadioButtonMenuItem) event.getSource();
      String csName = (String) dmi.getData();
      if (csName != null) {
        CharsetDetector.clearCache();
        StringTable.setCharset(charsetName(csName, true));
        // re-read strings
        ActionEvent refresh = new ActionEvent(dmi, 0, "Refresh");
        NearInfinity.getInstance().actionPerformed(refresh);
      }
    }
  }

  @Override
  public void itemStateChanged(ItemEvent event) {
    if (event.getStateChange() == ItemEvent.SELECTED && event.getSource() instanceof JRadioButtonMenuItem
        && gameLanguage.containsKey(event.getSource())) {
      switchGameLanguage(gameLanguage.get(event.getSource()));
    }
  }

  /** (Re-)creates a list of available TLK languages. */
  private void resetGameLanguage() {
    // removing old list of available game languages
    for (JRadioButtonMenuItem r : gameLanguage.keySet()) {
      r.removeActionListener(this);
    }
    mLanguageMenu.removeAll();
    gameLanguage.clear();

    // initializing new list of available game languages
    String selectedCode = getGameLanguage(languageDefinition, Profile.getGame());

    ButtonGroup bg = new ButtonGroup();
    JRadioButtonMenuItem rbmi;

    // adding "Autodetect" for all available game types
    rbmi = createLanguageMenuItem(LANGUAGE_AUTODETECT, "Autodetect",
        "Autodetect language from baldur.ini. " + "Defaults to english if not available.", bg, true);
    mLanguageMenu.add(rbmi);

    if (Profile.isEnhancedEdition()) {
      List<String> languages = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAMES_AVAILABLE);
      for (final String lang : languages) {
        String langName = getDisplayLanguage(lang);
        if (!langName.equalsIgnoreCase(lang)) {
          rbmi = createLanguageMenuItem(lang, String.format("%s (%s)", langName, lang), null, bg,
              selectedCode.equalsIgnoreCase(lang));
          mLanguageMenu.add(rbmi);
        } else {
          rbmi = createLanguageMenuItem(lang, lang, null, bg, selectedCode.equalsIgnoreCase(lang));
          mLanguageMenu.add(rbmi);
        }
      }
    } else {
      rbmi.setEnabled(false);
      rbmi.setToolTipText(null);
    }
  }

  /** Returns the name of the language specified by the given language code. */
  private String getDisplayLanguage(String langCode) {
    String retVal = langCode;
    String[] lang = langCode.split("_");
    if (lang.length >= 2) {
      retVal = (new Locale(lang[0], lang[1])).getDisplayLanguage();
      if (retVal == null || retVal.isEmpty()) {
        retVal = langCode;
      }
    }
    return retVal;
  }

  /** Initializes and returns a radio button menuitem. */
  private JRadioButtonMenuItem createLanguageMenuItem(String code, String name, String tooltip, ButtonGroup bg,
      boolean selected) {
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

  private JMenu initCharsetMenu(String charset) {
    bgCharsetButtons = new ButtonGroup();
    JMenu menu = new JMenu("TLK Charset");
    DataRadioButtonMenuItem dmi = new DataRadioButtonMenuItem("Autodetect Charset", false, DEFAULT_CHARSET);
    dmi.setToolTipText("Attempts to determine the correct character encoding automatically. "
        + "May not work reliably for all game languages.");
    dmi.addActionListener(this);
    bgCharsetButtons.add(dmi);
    menu.add(dmi);

    // creating primary list of charsets
    for (String[] info : CHARSETS_USED) {
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
            if (iter.hasNext()) {
              sb.append(", ");
            }
          }
        }
        dmi.setToolTipText(sb.toString());
        dmi.setActionCommand("Charset");
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
        String name = iter.next();

        // check whether charset has already been added
        boolean match = false;
        for (String[] info : CHARSETS_USED) {
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
        String desc = official ? name : String.format("%s (unofficial)", name.substring(2));
        dmi = new DataRadioButtonMenuItem(desc, false, name);
        Charset cs = Charset.forName(name);
        if (cs != null && !cs.aliases().isEmpty()) {
          StringBuilder sb = new StringBuilder("Charset aliases: ");
          Iterator<String> csIter = cs.aliases().iterator();
          while (csIter.hasNext()) {
            sb.append(csIter.next());
            if (csIter.hasNext()) {
              sb.append(", ");
            }
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
      dmi = findCharsetButton(DEFAULT_CHARSET);
    }
    if (dmi != null) {
      dmi.setSelected(true);
    }

    return menu;
  }

  /** Returns the menuitem that is associated with the specified string. */
  private DataRadioButtonMenuItem findCharsetButton(String charset) {
    if (bgCharsetButtons != null && charset != null && !charset.isEmpty()) {
      Enumeration<AbstractButton> buttonSet = bgCharsetButtons.getElements();
      while (buttonSet.hasMoreElements()) {
        AbstractButton b = buttonSet.nextElement();
        if (b instanceof DataRadioButtonMenuItem) {
          Object data = ((DataRadioButtonMenuItem) b).getData();
          if (data instanceof String) {
            if (charset.equalsIgnoreCase((String) data)) {
              return (DataRadioButtonMenuItem) b;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean charsetAvailable(String charset) {
    if (charset != null && !charset.isEmpty()) {
      if (DEFAULT_CHARSET.equalsIgnoreCase(charset)) {
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

  /** Extracts entries of Game/Language pairs from the given argument. */
  private List<Couple<String, String>> extractGameLanguages(String definition) {
    List<Couple<String, String>> list = new ArrayList<>();
    if (definition != null && !definition.isEmpty()) {
      String[] entries = definition.split(";");
      if (entries != null) {
        for (final String entry : entries) {
          String[] elements = entry.split("=");
          if (elements != null && elements.length == 2) {
            Profile.Game game = Profile.gameFromString(elements[0]);
            if (game != Profile.Game.Unknown) {
              String lang = elements[1].trim();
              Couple<String, String> pair = null;
              if (lang.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
                pair = Couple.with(game.toString(), LANGUAGE_AUTODETECT);
              } else if (lang.matches("[a-z]{2}_[A-Z]{2}")) {
                pair = Couple.with(game.toString(), lang);
              }

              // check if game/language pair is already in the list
              if (pair != null) {
                for (final Couple<String, String> curPair : list) {
                  if (curPair.getValue0().equalsIgnoreCase(pair.getValue0())) {
                    curPair.setValue1(pair.getValue1());
                    pair = null;
                    break;
                  }
                }
              }

              if (pair != null) {
                list.add(pair);
              }
            }
          }
        }
      }
    }
    return list;
  }

  /** Creates a formatted string out of the Game/Language pairs included in the given list. */
  private String createGameLanguages(List<Couple<String, String>> list) {
    StringBuilder sb = new StringBuilder();
    if (list != null) {
      for (final Iterator<Couple<String, String>> iter = list.iterator(); iter.hasNext();) {
        Couple<String, String> pair = iter.next();
        sb.append(String.format("%s=%s", pair.getValue0(), pair.getValue1()));
        if (iter.hasNext()) {
          sb.append(';');
        }
      }
    }
    return sb.toString();
  }

  /** Adds or updates the Game/Language pair in the formatted "definition" string. */
  private String updateGameLanguages(String definition, Couple<String, String> pair) {
    List<Couple<String, String>> list = extractGameLanguages(definition);
    if (pair != null && pair.getValue0() != null && pair.getValue1() != null) {
      // attempt to update existing entry first
      for (final Couple<String, String> curPair : list) {
        if (curPair.getValue0().equalsIgnoreCase(pair.getValue0())) {
          curPair.setValue1(pair.getValue1());
          pair = null;
          break;
        }
      }

      // add new entry if necessary
      if (pair != null) {
        list.add(pair);
      }

      return createGameLanguages(list);
    }
    return "";
  }

  /** Returns the language definition stored in "definition" for the specified game. */
  private String getGameLanguage(String definition, Profile.Game game) {
    if (game != null && game != Profile.Game.Unknown) {
      List<Couple<String, String>> list = extractGameLanguages(definition);
      for (Couple<String, String> pair : list) {
        Profile.Game curGame = Profile.gameFromString(pair.getValue0());
        if (curGame == game) {
          return pair.getValue1();
        }
      }
    }
    return LANGUAGE_AUTODETECT;
  }

  /** Attempts to switch the game language in Enhanced Edition games. */
  private void switchGameLanguage(String newLanguage) {
    if (newLanguage != null) {
      // switch language and refresh resources
      String oldLanguage = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAME);
      String oldLangName = getDisplayLanguage(oldLanguage);
      String newLanguageCode;
      if (newLanguage.equalsIgnoreCase(LANGUAGE_AUTODETECT)) {
        // "Autodetect" must be converted into an actual language code before proceeding
        newLanguageCode = ResourceFactory.autodetectGameLanguage(Profile.getProperty(Profile.Key.GET_GAME_INI_FILE));
      } else {
        newLanguageCode = newLanguage;
      }
      String newLangName = getDisplayLanguage(newLanguageCode);
      boolean success = false, showErrorMsg = false;
      if (JOptionPane.showConfirmDialog(NearInfinity.getInstance(),
          String.format("Do you want to switch from \"%s\" to \"%s\"?", oldLangName, newLangName),
          "Switch game language", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        if (Profile.updateGameLanguage(newLanguageCode)) {
          languageDefinition = updateGameLanguages(languageDefinition,
              Couple.with(Profile.getGame().toString(), newLanguage));
          NearInfinity.getInstance().refreshGame();
          success = true;
        } else {
          showErrorMsg = true;
        }
      }
      if (!success) {
        if (showErrorMsg) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Unable to set new language.", "Error",
              JOptionPane.ERROR_MESSAGE);
        }
        for (Entry<JRadioButtonMenuItem, String> entry : gameLanguage.entrySet()) {
          if (oldLanguage.equalsIgnoreCase(entry.getValue())) {
            JRadioButtonMenuItem rbmi = entry.getKey();
            // don't trigger item event
            rbmi.removeItemListener(this);
            entry.getKey().setSelected(true);
            rbmi.addItemListener(this);
            Profile.updateGameLanguage(oldLanguage);
            break;
          }
        }
      }
    }
  }

  private void applyCustomFont(Font font) {
    int index = FONTS.length - 1;
    FONTS[index] = (font != null) ? font : UIManager.getFont("MenuItem.font").deriveFont(Font.PLAIN);
    selectFont[index]
        .setText(String.format("Select font... (%s %d)", FONTS[index].getName(), FONTS[index].getSize()));
    selectFont[index].setFont(FONTS[index].deriveFont(Misc.getScaledValue(12.0f)));
  }

  /** Enables the UI scaling factor provided by the specified menu item. */
  private void applyUiScaling(DataRadioButtonMenuItem dmi) {
    int percent = ((Integer) dmi.getData());
    if (dmi == globalUiScaling[globalUiScaling.length - 1]) {
      if (percent < 0) {
        percent = NearInfinity.getInstance().getUiScalingFactor();
      }
      String ret = JOptionPane.showInputDialog(NearInfinity.getInstance(),
          "Enter UI scaling factor in percent (50 - 400):", Integer.valueOf(percent));
      if (ret == null) {
        dmi.setData(Integer.valueOf(percent));
        dmi.setText("Custom (" + percent + " %)...");
        return;
      }

      int value = NearInfinity.getInstance().getUiScalingFactor();
      try {
        int radix = 10;
        if (ret.toLowerCase().startsWith("0x")) {
          ret = ret.substring(2);
          radix = 16;
        }
        value = Integer.parseInt(ret, radix);
        if (value < 50 || value > 400) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
              "Number out of range. Using current value " + percent + ".");
          value = NearInfinity.getInstance().getUiScalingFactor();
        }

      } catch (NumberFormatException nfe) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
            "Invalid number entered. Using current value " + percent + ".");
      }
      dmi.setData(Integer.valueOf(value));
      dmi.setText("Custom (" + value + " %)...");
      if (value == NearInfinity.getInstance().getUiScalingFactor()) {
        return;
      }
    }
    if (percent != NearInfinity.getInstance().getUiScalingFactor()) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
          "You have to restart Near Infinity\n" + "for the UI scale factor change to take effect.");
    }
  }

  /** Enables the global font size provided by the specified menu item. */
  private void applyGlobalFontSize(DataRadioButtonMenuItem dmi) {
    int percent = ((Integer) dmi.getData());
    if (dmi == globalFontSize[globalFontSize.length - 1]) {
      if (percent < 0) {
        percent = NearInfinity.getInstance().getGlobalFontSize();
      }
      String ret = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter font size in percent (50 - 400):",
          Integer.valueOf(percent));
      if (ret == null) {
        dmi.setData(Integer.valueOf(percent));
        dmi.setText("Custom (" + percent + " %)...");
        return;
      }

      int value = NearInfinity.getInstance().getGlobalFontSize();
      try {
        int radix = 10;
        if (ret.toLowerCase().startsWith("0x")) {
          ret = ret.substring(2);
          radix = 16;
        }
        value = Integer.parseInt(ret, radix);
        if (value < 50 || value > 400) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
              "Number out of range. Using current value " + percent + ".");
          value = NearInfinity.getInstance().getGlobalFontSize();
        }
      } catch (NumberFormatException nfe) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
            "Invalid number entered. Using current value " + percent + ".");
      }
      dmi.setData(Integer.valueOf(value));
      dmi.setText("Custom (" + value + " %)...");
      if (value == NearInfinity.getInstance().getGlobalFontSize()) {
        return;
      }
    }
    if (percent != NearInfinity.getInstance().getGlobalFontSize()) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
          "You have to restart Near Infinity\n" + "for the font size change to take effect.");
    }
  }
}
