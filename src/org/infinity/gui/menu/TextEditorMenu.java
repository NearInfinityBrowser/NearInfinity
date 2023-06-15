// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import org.infinity.gui.InfinityTextArea;

/**
 * Handles the "Text Editor" menu items and submenus of the Options menu for the {@link BrowserMenuBar}.
 */
public class TextEditorMenu extends JMenu {
  /** BCS indentations to use when decompiling (indent, title). */
  private static final String[][] BCS_INDENT = {
      { "  ", "2 Spaces" },
      { "    ", "4 Spaces" },
      { "\t", "Tab" }
  };

  /** Available color schemes for highlighted BCS format (scheme, title, description). */
  private static final String[][] BCS_COLOR_SCHEME = {
      { InfinityTextArea.SCHEME_DEFAULT, "Default", "A general-purpose default color scheme" },
      { InfinityTextArea.SCHEME_DARK, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme" },
      { InfinityTextArea.SCHEME_ECLIPSE, "Eclipse", "Mimics Eclipse's default color scheme" },
      { InfinityTextArea.SCHEME_IDEA, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme" },
      { InfinityTextArea.SCHEME_MONOKAI, "Monokai", "A dark color scheme inspired by \"Monokai\"" },
      { InfinityTextArea.SCHEME_VS, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme" },
      { InfinityTextArea.SCHEME_BCS, "BCS Light",
          "A color scheme which is loosely based on the WeiDU Syntax Highlighter for Notepad++" },
  };

  /** Available color schemes for remaining highlighted formats (scheme, title, description). */
  private static final String[][] COLOR_SCHEME = {
      { InfinityTextArea.SCHEME_DEFAULT, "Default", "A general-purpose default color scheme" },
      { InfinityTextArea.SCHEME_DARK, "Dark", "A dark scheme based off of Notepad++'s Obsidian theme" },
      { InfinityTextArea.SCHEME_ECLIPSE, "Eclipse", "Mimics Eclipse's default color scheme" },
      { InfinityTextArea.SCHEME_IDEA, "IntelliJ IDEA", "Mimics IntelliJ IDEA's default color scheme" },
      { InfinityTextArea.SCHEME_MONOKAI, "Monokai", "A dark color scheme inspired by \"Monokai\"" },
      { InfinityTextArea.SCHEME_VS, "Visual Studio", "Mimics Microsoft's Visual Studio color scheme" },
  };

  private static final String OPTION_TEXT_SHOWCURRENTLINE     = "TextShowCurrentLine";
  private static final String OPTION_TEXT_SHOWLINENUMBERS     = "TextShowLineNumbers";
  private static final String OPTION_TEXT_SYMBOLWHITESPACE    = "TextShowWhiteSpace";
  private static final String OPTION_TEXT_SYMBOLEOL           = "TextShowEOL";
  private static final String OPTION_TEXT_TABSEMULATED        = "TextTabsEmulated";
  private static final String OPTION_TEXT_TABSIZE             = "TextTabSize";
  private static final String OPTION_BCS_SYNTAXHIGHLIGHTING   = "BcsSyntaxHighlighting";
  private static final String OPTION_BCS_COLORSCHEME          = "BcsColorScheme";
  private static final String OPTION_BCS_CODEFOLDING          = "BcsCodeFolding";
  private static final String OPTION_BCS_AUTO_INDENT          = "BcsAutoIndent";
  private static final String OPTION_BCS_INDENT               = "BcsIndent";
  private static final String OPTION_GLSL_SYNTAXHIGHLIGHTING  = "GlslSyntaxHighlighting";
  private static final String OPTION_GLSL_COLORSCHEME         = "GlslColorScheme";
  private static final String OPTION_GLSL_CODEFOLDING         = "GlslCodeFolding";
  private static final String OPTION_LUA_SYNTAXHIGHLIGHTING   = "LuaSyntaxHighlighting";
  private static final String OPTION_LUA_COLORSCHEME          = "LuaColorScheme";
  private static final String OPTION_SQL_SYNTAXHIGHLIGHTING   = "SqlSyntaxHighlighting";
  private static final String OPTION_SQL_COLORSCHEME          = "SqlColorScheme";
  private static final String OPTION_TLK_SYNTAXHIGHLIGHTING   = "TlkSyntaxHighlighting";
  private static final String OPTION_TLK_COLORSCHEME          = "TlkColorScheme";
  private static final String OPTION_WEIDU_SYNTAXHIGHLIGHTING = "WeiDUSyntaxHighlighting";
  private static final String OPTION_WEIDU_COLORSCHEME        = "WeiDUColorScheme";

  /** Mask used for one-time resets of options (kept track of in OptionsMenu.OPTION_OPTION_FIXED). */
  private static final int MASK_OPTION_FIXED_AUTO_INDENT = 0x00000001;

  private final JRadioButtonMenuItem[] selectTextTabSize = new JRadioButtonMenuItem[3];
  private final JRadioButtonMenuItem[] selectBcsIndent = new JRadioButtonMenuItem[BCS_INDENT.length];
  private final JRadioButtonMenuItem[] selectBcsColorScheme = new JRadioButtonMenuItem[BCS_COLOR_SCHEME.length];
  private final JRadioButtonMenuItem[] selectGlslColorScheme = new JRadioButtonMenuItem[COLOR_SCHEME.length];
  private final JRadioButtonMenuItem[] selectLuaColorScheme = new JRadioButtonMenuItem[COLOR_SCHEME.length];
  private final JRadioButtonMenuItem[] selectSqlColorScheme = new JRadioButtonMenuItem[COLOR_SCHEME.length];
  private final JRadioButtonMenuItem[] selectTlkColorScheme = new JRadioButtonMenuItem[COLOR_SCHEME.length];
  private final JRadioButtonMenuItem[] selectWeiDUColorScheme = new JRadioButtonMenuItem[COLOR_SCHEME.length];

  private JCheckBoxMenuItem optionTextHightlightCurrent;
  private JCheckBoxMenuItem optionTextLineNumbers;
  private JCheckBoxMenuItem optionTextShowWhiteSpace;
  private JCheckBoxMenuItem optionTextShowEOL;
  private JCheckBoxMenuItem optionTextTabEmulate;
  private JCheckBoxMenuItem optionBCSEnableSyntax;
  private JCheckBoxMenuItem optionBCSEnableCodeFolding;
  private JCheckBoxMenuItem optionBCSEnableAutoIndent;
  private JCheckBoxMenuItem optionGLSLEnableSyntax;
  private JCheckBoxMenuItem optionLUAEnableSyntax;
  private JCheckBoxMenuItem optionSQLEnableSyntax;
  private JCheckBoxMenuItem optionTLKEnableSyntax;
  private JCheckBoxMenuItem optionWeiDUEnableSyntax;
  private JCheckBoxMenuItem optionGLSLEnableCodeFolding;

  public TextEditorMenu(OptionsMenu parent, Preferences prefs) {
    super("Text Editor");

    // Options->Text Viewer/Editor->Tab Settings
    JMenu textTabs = new JMenu("Tab Settings");
    add(textTabs);
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
      selectTextTabSize[i].setToolTipText(String.format("Each (real or emulated) tab will occupy %d spaces.", cnt));
      textTabs.add(selectTextTabSize[i]);
      bg.add(selectTextTabSize[i]);
    }

    // Options > Text Editor > BCS and BAF
    JMenu textBCS = new JMenu("BCS and BAF");
    add(textBCS);
    JMenu textBCSIndent = new JMenu("BCS Indent");
    textBCS.add(textBCSIndent);
    bg = new ButtonGroup();
    int selectedBCSIndent = prefs.getInt(OPTION_BCS_INDENT, 2);
    if (selectedBCSIndent < 0 || selectedBCSIndent >= BCS_INDENT.length) {
      selectedBCSIndent = 2;
    }
    for (int i = 0; i < BCS_INDENT.length; i++) {
      selectBcsIndent[i] = new JRadioButtonMenuItem(BCS_INDENT[i][1], selectedBCSIndent == i);
      textBCSIndent.add(selectBcsIndent[i]);
      bg.add(selectBcsIndent[i]);
    }
    JMenu textBCSColors = new JMenu("Color Scheme");
    textBCS.add(textBCSColors);
    bg = new ButtonGroup();
    int selectedBCSScheme = prefs.getInt(OPTION_BCS_COLORSCHEME, 5);
    if (selectedBCSScheme < 0 || selectedBCSScheme >= BCS_COLOR_SCHEME.length) {
      selectedBCSScheme = 5;
    }
    for (int i = 0; i < BCS_COLOR_SCHEME.length; i++) {
      selectBcsColorScheme[i] = new JRadioButtonMenuItem(BCS_COLOR_SCHEME[i][1], selectedBCSScheme == i);
      selectBcsColorScheme[i].setToolTipText(BCS_COLOR_SCHEME[i][2]);
      textBCSColors.add(selectBcsColorScheme[i]);
      bg.add(selectBcsColorScheme[i]);
    }
    optionBCSEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting",
        prefs.getBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, true));
    textBCS.add(optionBCSEnableSyntax);
    optionBCSEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding",
        prefs.getBoolean(OPTION_BCS_CODEFOLDING, false));
    textBCS.add(optionBCSEnableCodeFolding);
    // XXX: Work-around to fix a previously incorrectly defined option
    optionBCSEnableAutoIndent = new JCheckBoxMenuItem("Enable Automatic Indentation",
        parent.fixOption(MASK_OPTION_FIXED_AUTO_INDENT, true, prefs.getBoolean(OPTION_BCS_AUTO_INDENT, false)));
    // optionBCSEnableAutoIndent = new JCheckBoxMenuItem("Enable Automatic Indentation",
    // getPrefs().getBoolean(OPTION_BCS_AUTO_INDENT, false));
    textBCS.add(optionBCSEnableAutoIndent);

    // Options > Text Editor > Misc. Resource Types
    JMenu textMisc = new JMenu("Misc. Resource Types");
    add(textMisc);
    JMenu textGLSLColors = new JMenu("Color Scheme for GLSL");
    textMisc.add(textGLSLColors);
    bg = new ButtonGroup();
    int selectedGLSLScheme = prefs.getInt(OPTION_GLSL_COLORSCHEME, 0);
    if (selectedGLSLScheme < 0 || selectedGLSLScheme >= COLOR_SCHEME.length) {
      selectedGLSLScheme = 0;
    }
    for (int i = 0; i < COLOR_SCHEME.length; i++) {
      selectGlslColorScheme[i] = new JRadioButtonMenuItem(COLOR_SCHEME[i][1], selectedGLSLScheme == i);
      selectGlslColorScheme[i].setToolTipText(COLOR_SCHEME[i][2]);
      textGLSLColors.add(selectGlslColorScheme[i]);
      bg.add(selectGlslColorScheme[i]);
    }

    JMenu textLUAColors = new JMenu("Color Scheme for LUA");
    textMisc.add(textLUAColors);
    bg = new ButtonGroup();
    int selectedLUAScheme = prefs.getInt(OPTION_LUA_COLORSCHEME, 0);
    if (selectedLUAScheme < 0 || selectedLUAScheme >= COLOR_SCHEME.length) {
      selectedLUAScheme = 0;
    }
    for (int i = 0; i < COLOR_SCHEME.length; i++) {
      selectLuaColorScheme[i] = new JRadioButtonMenuItem(COLOR_SCHEME[i][1], selectedLUAScheme == i);
      selectLuaColorScheme[i].setToolTipText(COLOR_SCHEME[i][2]);
      textLUAColors.add(selectLuaColorScheme[i]);
      bg.add(selectLuaColorScheme[i]);
    }

    JMenu textSQLColors = new JMenu("Color Scheme for SQL");
    textMisc.add(textSQLColors);
    bg = new ButtonGroup();
    int selectedSQLScheme = prefs.getInt(OPTION_SQL_COLORSCHEME, 0);
    if (selectedSQLScheme < 0 || selectedSQLScheme >= COLOR_SCHEME.length) {
      selectedSQLScheme = 0;
    }
    for (int i = 0; i < COLOR_SCHEME.length; i++) {
      selectSqlColorScheme[i] = new JRadioButtonMenuItem(COLOR_SCHEME[i][1], selectedSQLScheme == i);
      selectSqlColorScheme[i].setToolTipText(COLOR_SCHEME[i][2]);
      textSQLColors.add(selectSqlColorScheme[i]);
      bg.add(selectSqlColorScheme[i]);
    }

    JMenu textTLKColors = new JMenu("Color Scheme for text strings");
    textMisc.add(textTLKColors);
    bg = new ButtonGroup();
    int selectedTLKScheme = prefs.getInt(OPTION_TLK_COLORSCHEME, 0);
    if (selectedTLKScheme < 0 || selectedTLKScheme >= COLOR_SCHEME.length) {
      selectedTLKScheme = 0;
    }
    for (int i = 0; i < COLOR_SCHEME.length; i++) {
      selectTlkColorScheme[i] = new JRadioButtonMenuItem(COLOR_SCHEME[i][1], selectedTLKScheme == i);
      selectTlkColorScheme[i].setToolTipText(COLOR_SCHEME[i][2]);
      textTLKColors.add(selectTlkColorScheme[i]);
      bg.add(selectTlkColorScheme[i]);
    }

    JMenu textWeiDUColors = new JMenu("Color Scheme for WeiDU.log");
    textMisc.add(textWeiDUColors);
    bg = new ButtonGroup();
    int selectedWeiDUScheme = prefs.getInt(OPTION_WEIDU_COLORSCHEME, 0);
    if (selectedWeiDUScheme < 0 || selectedWeiDUScheme >= COLOR_SCHEME.length) {
      selectedWeiDUScheme = 0;
    }
    for (int i = 0; i < COLOR_SCHEME.length; i++) {
      selectWeiDUColorScheme[i] = new JRadioButtonMenuItem(COLOR_SCHEME[i][1], selectedWeiDUScheme == i);
      selectWeiDUColorScheme[i].setToolTipText(COLOR_SCHEME[i][2]);
      textWeiDUColors.add(selectWeiDUColorScheme[i]);
      bg.add(selectWeiDUColorScheme[i]);
    }

    optionGLSLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for GLSL",
        prefs.getBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, true));
    textMisc.add(optionGLSLEnableSyntax);
    optionLUAEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for LUA",
        prefs.getBoolean(OPTION_LUA_SYNTAXHIGHLIGHTING, true));
    textMisc.add(optionLUAEnableSyntax);
    optionSQLEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for SQL",
        prefs.getBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, true));
    textMisc.add(optionSQLEnableSyntax);
    optionTLKEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for text strings",
        prefs.getBoolean(OPTION_TLK_SYNTAXHIGHLIGHTING, true));
    textMisc.add(optionTLKEnableSyntax);
    optionWeiDUEnableSyntax = new JCheckBoxMenuItem("Enable Syntax Highlighting for WeiDU.log",
        prefs.getBoolean(OPTION_WEIDU_SYNTAXHIGHLIGHTING, true));
    textMisc.add(optionWeiDUEnableSyntax);
    optionGLSLEnableCodeFolding = new JCheckBoxMenuItem("Enable Code Folding for GLSL",
        prefs.getBoolean(OPTION_GLSL_CODEFOLDING, false));
    textMisc.add(optionGLSLEnableCodeFolding);

    // Options > Text Editor (continued)
    optionTextShowWhiteSpace = new JCheckBoxMenuItem("Show Spaces and Tabs",
        prefs.getBoolean(OPTION_TEXT_SYMBOLWHITESPACE, false));
    add(optionTextShowWhiteSpace);
    optionTextShowEOL = new JCheckBoxMenuItem("Show End of Line", prefs.getBoolean(OPTION_TEXT_SYMBOLEOL, false));
    add(optionTextShowEOL);
    optionTextHightlightCurrent = new JCheckBoxMenuItem("Show Highlighted Current Line",
        prefs.getBoolean(OPTION_TEXT_SHOWCURRENTLINE, true));
    add(optionTextHightlightCurrent);
    optionTextLineNumbers = new JCheckBoxMenuItem("Show Line Numbers",
        prefs.getBoolean(OPTION_TEXT_SHOWLINENUMBERS, true));
    add(optionTextLineNumbers);
  }

  /** Returns the number of spaces used for (real or emulated) tabs. */
  public int getTextTabSize() {
    return 1 << (getTextIndentIndex() + 1);
  }

  /** Returns the array index of the selected tab size. */
  public int getTextIndentIndex() {
    for (int i = 0; i < selectTextTabSize.length; i++) {
      if (selectTextTabSize[i].isSelected()) {
        return i;
      }
    }
    return 1; // default
  }

  /** Returns the selected indentation for BCS resources as literal string. */
  public String getBcsIndent() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectBcsIndent, 2);
    return BCS_INDENT[idx][0];
  }

  /** Returns state of "Text: Show Highlight Current Line" */
  public boolean getTextHighlightCurrentLine() {
    return optionTextHightlightCurrent.isSelected();
  }

  /** Returns state of "Text: Show Line Numbers" */
  public boolean getTextLineNumbers() {
    return optionTextLineNumbers.isSelected();
  }

  /** Returns state of "Text: Show Whitespace and Tab" */
  public boolean getTextWhitespaceVisible() {
    return optionTextShowWhiteSpace.isSelected();
  }

  /** Returns state of "Text: Show End of Line" */
  public boolean getTextEOLVisible() {
    return optionTextShowEOL.isSelected();
  }

  /** Returns state of "BCS: Enable Syntax Highlighting" */
  public boolean getBcsSyntaxHighlightingEnabled() {
    return optionBCSEnableSyntax.isSelected();
  }

  /** Returns state of "BCS: Enable Code Folding" */
  public boolean getBcsCodeFoldingEnabled() {
    return optionBCSEnableCodeFolding.isSelected();
  }

  /** Returns state of "BCS: Enable Automatic Indentation" */
  public boolean getBcsAutoIndentEnabled() {
    return optionBCSEnableAutoIndent.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for GLSL" */
  public boolean getGlslSyntaxHighlightingEnabled() {
    return optionGLSLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for LUA" */
  public boolean getLuaSyntaxHighlightingEnabled() {
    return optionLUAEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for SQL" */
  public boolean getSqlSyntaxHighlightingEnabled() {
    return optionSQLEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for TLK" */
  public boolean getTlkSyntaxHighlightingEnabled() {
    return optionTLKEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Syntax Highlighting for WeiDU.log" */
  public boolean getWeiDUSyntaxHighlightingEnabled() {
    return optionWeiDUEnableSyntax.isSelected();
  }

  /** Returns state of "Enable Code Folding for GLSL" */
  public boolean getGlslCodeFoldingEnabled() {
    return optionGLSLEnableCodeFolding.isSelected();
  }

  /** Returns whether to emulate tabs by inserting spaces instead. */
  public boolean isTextTabEmulated() {
    return optionTextTabEmulate.isSelected();
  }

  public String getBcsColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectBcsColorScheme, 5);
    return BCS_COLOR_SCHEME[idx][0];
  }

  public String getGlslColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectGlslColorScheme, 0);
    return COLOR_SCHEME[idx][0];
  }

  public String getLuaColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectLuaColorScheme, 0);
    return COLOR_SCHEME[idx][0];
  }

  public String getSqlColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectSqlColorScheme, 0);
    return COLOR_SCHEME[idx][0];
  }

  public String getTlkColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectTlkColorScheme, 0);
    return COLOR_SCHEME[idx][0];
  }

  public String getWeiDUColorScheme() {
    int idx = BrowserMenuBar.getSelectedButtonIndex(selectWeiDUColorScheme, 0);
    return COLOR_SCHEME[idx][0];
  }

  public void storePreferences(Preferences prefs) {
    int selectedIndent = BrowserMenuBar.getSelectedButtonIndex(selectBcsIndent, 0);
    prefs.putInt(OPTION_BCS_INDENT, selectedIndent);
    prefs.putBoolean(OPTION_TEXT_SHOWCURRENTLINE, optionTextHightlightCurrent.isSelected());
    prefs.putBoolean(OPTION_TEXT_SHOWLINENUMBERS, optionTextLineNumbers.isSelected());
    prefs.putBoolean(OPTION_TEXT_SYMBOLWHITESPACE, optionTextShowWhiteSpace.isSelected());
    prefs.putBoolean(OPTION_TEXT_SYMBOLEOL, optionTextShowEOL.isSelected());
    prefs.putBoolean(OPTION_TEXT_TABSEMULATED, optionTextTabEmulate.isSelected());
    int selectTabSize = BrowserMenuBar.getSelectedButtonIndex(selectTextTabSize, 1);
    prefs.putInt(OPTION_TEXT_TABSIZE, selectTabSize);
    int selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectBcsColorScheme, 5);
    prefs.putInt(OPTION_BCS_COLORSCHEME, selectColorScheme);
    prefs.putBoolean(OPTION_BCS_SYNTAXHIGHLIGHTING, optionBCSEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_BCS_CODEFOLDING, optionBCSEnableCodeFolding.isSelected());
    prefs.putBoolean(OPTION_BCS_AUTO_INDENT, optionBCSEnableAutoIndent.isSelected());
    selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectGlslColorScheme, 0);
    prefs.putInt(OPTION_GLSL_COLORSCHEME, selectColorScheme);
    selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectLuaColorScheme, 0);
    prefs.putInt(OPTION_LUA_COLORSCHEME, selectColorScheme);
    selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectSqlColorScheme, 0);
    prefs.putInt(OPTION_SQL_COLORSCHEME, selectColorScheme);
    selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectTlkColorScheme, 0);
    prefs.putInt(OPTION_TLK_COLORSCHEME, selectColorScheme);
    selectColorScheme = BrowserMenuBar.getSelectedButtonIndex(selectWeiDUColorScheme, 0);
    prefs.putInt(OPTION_WEIDU_COLORSCHEME, selectColorScheme);
    prefs.putBoolean(OPTION_GLSL_SYNTAXHIGHLIGHTING, optionGLSLEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_LUA_SYNTAXHIGHLIGHTING, optionLUAEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_SQL_SYNTAXHIGHLIGHTING, optionSQLEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_TLK_SYNTAXHIGHLIGHTING, optionTLKEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_WEIDU_SYNTAXHIGHLIGHTING, optionWeiDUEnableSyntax.isSelected());
    prefs.putBoolean(OPTION_GLSL_CODEFOLDING, optionGLSLEnableCodeFolding.isSelected());
  }
}
