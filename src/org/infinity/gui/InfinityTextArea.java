// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.infinity.NearInfinity;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.Keyfile;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.modes.BCSFoldParser;
import org.infinity.resource.text.modes.BCSTokenMaker;
import org.infinity.resource.text.modes.GLSLTokenMaker;
import org.infinity.resource.text.modes.INITokenMaker;
import org.infinity.resource.text.modes.MenuTokenMaker;
import org.infinity.resource.text.modes.TLKTokenMaker;
import org.infinity.resource.text.modes.WeiDULogTokenMaker;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

/**
 * Extends {@link RSyntaxTextArea} by NearInfinity-specific features.
 */
public class InfinityTextArea extends RSyntaxTextArea
    implements ActionListener, ChangeListener, KeyListener, HierarchyListener, PopupMenuListener {
  /** Available languages for syntax highlighting. */
  public enum Language {
    /** Disables syntax highlighting */
    NONE(SyntaxConstants.SYNTAX_STYLE_NONE),
    /** Select BCS highlighting. */
    BCS(BCSTokenMaker.SYNTAX_STYLE_BCS),
    /** Select DLG tree viewer trigger/action highlighting. */
    DLG(BCSTokenMaker.SYNTAX_STYLE_BCS),
    /** Select TLK highlighting. */
    TLK(TLKTokenMaker.SYNTAX_STYLE_TLK),
    /** Select GLSL highlighting. */
    GLSL(GLSLTokenMaker.SYNTAX_STYLE_GLSL),
    /** Select INI highlighting. */
    INI(INITokenMaker.SYNTAX_STYLE_INI),
    /** Select LUA highlighting. */
    LUA(SyntaxConstants.SYNTAX_STYLE_LUA),
    /** Select MENU highlighting. */
    MENU(MenuTokenMaker.SYNTAX_STYLE_MENU),
    /** Select SQL highlighting. */
    SQL(SyntaxConstants.SYNTAX_STYLE_SQL),
    /** Select WeiDU.log highlighting. */
    WEIDU(WeiDULogTokenMaker.SYNTAX_STYLE_WEIDU),
    ;

    private final String style;

    Language(String style) {
      this.style = style;
    }

    /** Returns the syntax highlighting style definition. */
    public String getStyle() {
      return style;
    }
  }

  /** Available color schemes for use when enabling syntax highlighting. */
  public enum Scheme {
    /** Disables any color scheme. */
    NONE("None", InfinityTextArea::getNoneScheme),
    /** The default color scheme. */
    DEFAULT("Default", () -> SCHEME_DEFAULT),
    /** Color scheme based on Notepad++'s Obsidian scheme. */
    DARK("Dark", () -> SCHEME_DARK),
    /** A dark green Color scheme. */
    DRUID("Druid", () -> SCHEME_DRUID),
    /** Color scheme based on Eclipse's defaults. */
    ECLIPSE("Eclipse", () -> SCHEME_ECLIPSE),
    /** Color scheme based on IntelliJ IDEA's defaults. */
    IDEA("IntelliJ IDEA", () -> SCHEME_IDEA),
    /** A dark color scheme inspired by "Monokai". */
    MONOKAI("Monokai", () -> SCHEME_MONOKAI),
    /** Color scheme based on Microsoft Visual Studio's defaults. */
    VS("Visual Studio", () -> SCHEME_VS),
    /** Color scheme loosely based on the WeiDU Syntax Highlighter for Notepad++. */
    BCS("BCS Light", () -> SCHEME_BCS),
    ;

    private final String label;

    // Stored as functional interface to react to dark/light UI theme changes
    private final Supplier<String> scheme;

    Scheme(String label, Supplier<String> scheme) {
      this.label = label;
      this.scheme = scheme;
    }

    /** Returns a descriptive label for the color scheme. */
    public String getLabel() {
      return label;
    }

    /** Returns the path to the color scheme. */
    public String getScheme() {
      return scheme.get();
    }
  }

  /** The default color of the currently highlighted text line. */
  public static final Color DEFAULT_LINE_HIGHLIGHT_COLOR = new Color(0xe8e8ff);

  /** Identifies a menu item from the "open resource" menu. */
  private static final String CMD_OPEN_RESOURCE = "OpenResource";
  /** Identifies a menu item from the "open strref" menu. */
  private static final String CMD_OPEN_STRREF   = "OpenStrref";

  /** Format string for an "open resource" menu item label. */
  private static final String OPEN_RESOURCE_LABEL_FMT     = "Open \"%s\" as resource...";
  /** Default string for "open resource" menu item label. */
  private static final String OPEN_RESOURCE_LABEL_DEFAULT = "Open as resource...";

  /** Format string for an "open strref" menu item label. */
  private static final String OPEN_STRREF_LABEL_FMT     = "Open string reference %d ...";
  /** Default string for "open strref" menu item label. */
  private static final String OPEN_STRREF_LABEL_DEFAULT = "Open string reference...";

  /** Color scheme for unicolored text */
  private static final String SCHEME_NONE       = "org/infinity/resource/text/modes/ThemeNone.xml";
  /** Dark color scheme for unicolored text */
  private static final String SCHEME_NONE_DARK  = "org/infinity/resource/text/modes/ThemeNoneDark.xml";
  /** Default color scheme */
  private static final String SCHEME_DEFAULT    = "org/infinity/resource/text/modes/ThemeDefault.xml";
  /** Dark color scheme */
  private static final String SCHEME_DARK       = "org/infinity/resource/text/modes/ThemeDark.xml";
  /** Druid color scheme */
  private static final String SCHEME_DRUID      = "org/infinity/resource/text/modes/ThemeDruid.xml";
  /** Eclipse color scheme */
  private static final String SCHEME_ECLIPSE    = "org/infinity/resource/text/modes/ThemeEclipse.xml";
  /** IntelliJ IDEA color scheme */
  private static final String SCHEME_IDEA       = "org/infinity/resource/text/modes/ThemeIdea.xml";
  /** "Monokai" color scheme */
  private static final String SCHEME_MONOKAI    = "org/infinity/resource/text/modes/ThemeMonokai.xml";
  /** Visual Studio color scheme */
  private static final String SCHEME_VS         = "org/infinity/resource/text/modes/ThemeVs.xml";
  /** BCS color scheme based on WeiDU Highlighter for Notepad++ */
  private static final String SCHEME_BCS        = "org/infinity/resource/text/modes/ThemeBCSLight.xml";

  static {
    // adding custom code folding definitions
    FoldParserManager.get().addFoldParserMapping(BCSTokenMaker.SYNTAX_STYLE_BCS, new BCSFoldParser());
    FoldParserManager.get().addFoldParserMapping(GLSLTokenMaker.SYNTAX_STYLE_GLSL, new CurlyFoldParser());

    // adding custom syntax highlighting definitions
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(BCSTokenMaker.SYNTAX_STYLE_BCS,
        BCSTokenMaker.class.getCanonicalName());
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(TLKTokenMaker.SYNTAX_STYLE_TLK,
        TLKTokenMaker.class.getCanonicalName());
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(WeiDULogTokenMaker.SYNTAX_STYLE_WEIDU,
        WeiDULogTokenMaker.class.getCanonicalName());
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(GLSLTokenMaker.SYNTAX_STYLE_GLSL,
        GLSLTokenMaker.class.getCanonicalName());
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(INITokenMaker.SYNTAX_STYLE_INI,
        INITokenMaker.class.getCanonicalName());
    ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(MenuTokenMaker.SYNTAX_STYLE_MENU,
        MenuTokenMaker.class.getCanonicalName());
  }

  private final SortedMap<Integer, GutterIcon> gutterIcons = new TreeMap<>();
  private final Map<Integer, GutterIconInfo> gutterIconsActive = new HashMap<>();
  private final HashMap<KeyStroke, Action> inputActionMap = new HashMap<>();
  private final JPopupMenu.Separator menuOpenEntrySeparator = new JPopupMenu.Separator();
  private final JMenu menuOpenResource = new JMenu(OPEN_RESOURCE_LABEL_DEFAULT);
  private final JMenuItem menuOpenStrref = new JMenuItem(OPEN_STRREF_LABEL_DEFAULT);

  private RTextScrollPane scrollPane;
  private List<JMenuItem> resourceTypeItems;
  private boolean openResrefEnabled;
  private boolean openStrrefEnabled;

  /**
   * Constructor.
   *
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(boolean applySettings) {
    super();
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param doc           The document for the editor.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(RSyntaxDocument doc, boolean applySettings) {
    super(doc);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param text          The initial text to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(String text, boolean applySettings) {
    super(text);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param textMode      Either {@code INSERT_MODE} or {@code OVERWRITE_MODE}.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(int textMode, boolean applySettings) {
    super(textMode);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param rows          The number of rows to display.
   * @param cols          The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(int rows, int cols, boolean applySettings) {
    super(rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param text          The initial text to display.
   * @param rows          The number of rows to display.
   * @param cols          The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(String text, int rows, int cols, boolean applySettings) {
    super(text, rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Constructor.
   *
   * @param doc           The document for the editor.
   * @param text          The initial text to display.
   * @param rows          The number of rows to display.
   * @param cols          The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(RSyntaxDocument doc, String text, int rows, int cols, boolean applySettings) {
    super(doc, text, rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    initTextArea();
  }

  /**
   * Applies global text editor settings to the specified {@link RSyntaxTextArea} component.
   *
   * @param resetUndo Specifies whether the undo history will be discarded.
   */
  public static void applySettings(InfinityTextArea edit, boolean resetUndo) {
    if (edit != null) {
      // Allows key strokes defined by a parent components to be processed when this component has focus
      edit.addKeyListener(edit);
      edit.addHierarchyListener(edit);

      edit.setCurrentLineHighlightColor(DEFAULT_LINE_HIGHLIGHT_COLOR);
      if (BrowserMenuBar.isInstantiated()) {
        edit.setTabsEmulated(BrowserMenuBar.getInstance().getOptions().isTextTabEmulated());
        edit.setTabSize(BrowserMenuBar.getInstance().getOptions().getTextTabSize());
        edit.setWhitespaceVisible(BrowserMenuBar.getInstance().getOptions().getTextWhitespaceVisible());
        edit.setEOLMarkersVisible(BrowserMenuBar.getInstance().getOptions().getTextEOLVisible());
        edit.setHighlightCurrentLine(BrowserMenuBar.getInstance().getOptions().getTextHighlightCurrentLine());
      } else {
        // default settings
        edit.setTabsEmulated(false);
        edit.setTabSize(4);
        edit.setWhitespaceVisible(false);
        edit.setEOLMarkersVisible(false);
        edit.setHighlightCurrentLine(false);
      }
      if (resetUndo) {
        edit.discardAllEdits(); // clearing undo history
      }
    }
  }

  /**
   * Applies syntax highlighting and color schemes to the specified {@link RSyntaxTextArea} component.
   *
   * @param language The language to highlight. Specifying {@code null} uses {@code Language.NONE} for syntax
   *                 hightlighting.
   * @param scheme   A color scheme to apply to the specified {@link RSyntaxTextArea} component. Specifying {@code null}
   *                 uses the color scheme as defined for the specified language.
   */
  public static void applyExtendedSettings(RSyntaxTextArea edit, Language language, Scheme scheme) {
    if (edit != null) {
      if (language == null) {
        language = Language.NONE;
      }

      // applying syntax highlighting
      edit.setSyntaxEditingStyle(language.getStyle());

      // applying color scheme
      String schemePath;
      if (scheme != null) {
        // applying explicit color scheme
        schemePath = scheme.getScheme();
      } else {
        // applying implicit color scheme
        schemePath = getNoneScheme();
        switch (language) {
          case BCS:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getBcsColorScheme();
            }
            break;
          case DLG:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getDlgColorScheme();
            }
            break;
          case TLK:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getTlkColorScheme();
            }
            break;
          case GLSL:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getGlslColorScheme();
            }
            break;
          case INI:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getIniColorScheme();
            }
            break;
          case LUA:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getLuaColorScheme();
            }
            break;
          case MENU:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getMenuColorScheme();
            }
            break;
          case SQL:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getSqlColorScheme();
            }
            break;
          case WEIDU:
            if (BrowserMenuBar.isInstantiated()) {
              schemePath = BrowserMenuBar.getInstance().getOptions().getWeiDUColorScheme();
            }
            break;
          default:
        }
      }

      if (schemePath != null) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(schemePath)) {
          Theme theme = Theme.load(is);
          theme.apply(edit);
        } catch (NullPointerException e) {
          // ignore
        } catch (IOException e) {
          Logger.error(e);
        }
      }

      // apply code folding
      switch (language) {
        case BCS:
          if (BrowserMenuBar.isInstantiated()) {
            edit.setCodeFoldingEnabled(BrowserMenuBar.getInstance().getOptions().getBcsCodeFoldingEnabled());
          } else {
            edit.setCodeFoldingEnabled(false);
          }
          break;
        case GLSL:
          if (BrowserMenuBar.isInstantiated()) {
            edit.setCodeFoldingEnabled(BrowserMenuBar.getInstance().getOptions().getGlslCodeFoldingEnabled());
          } else {
            edit.setCodeFoldingEnabled(false);
          }
          break;
        default:
          edit.setCodeFoldingEnabled(false);
      }
    }
  }

  /** Returns the color scheme when syntax highlighting is disabled. */
  private static String getNoneScheme() {
    if (NearInfinity.getInstance() != null) {
      return NearInfinity.getInstance().isDarkMode() ? SCHEME_NONE_DARK : SCHEME_NONE;
    }
    return SCHEME_NONE;
  }

  /**
   * Applies global text editor settings to this component.
   *
   * @param resetUndo Specifies whether the undo history will be discarded.
   */
  public void applySettings(boolean resetUndo) {
    applySettings(this, resetUndo);
  }

  /**
   * Applies syntax highlighting and color schemes to this component.
   *
   * @param language The language to highlight. Specifying {@code null} uses {@code Language.NONE} for syntax
   *                 hightlighting.
   * @param scheme   A color scheme to apply to the specified {@link RSyntaxTextArea} component. Specifying {@code null}
   *                 uses the color scheme as defined for the specified language.
   */
  public void applyExtendedSettings(Language language, Scheme scheme) {
    applyExtendedSettings(this, language, scheme);
  }

  /** Returns whether the context menu should display an option to open selected text as a game resource. */
  public boolean isOpenResrefEnabled() {
    return openResrefEnabled;
  }

  /** Specifies whether the context menu should display an option to open selected text as a game resource. */
  public void setOpenResrefEnabled(boolean b) {
    if (b != openResrefEnabled) {
      openResrefEnabled = b;
      if (openResrefEnabled) {
        initOpenResource();
      }
      menuOpenEntrySeparator.setVisible(isOpenEntryEnabled());
      menuOpenResource.setVisible(openResrefEnabled);
    }
  }

  /** Returns whether the context menu should display an option to open selected text as a string reference. */
  public boolean isOpenStrrefEnabled() {
    return openStrrefEnabled;
  }

  /** Specifies whether the context menu should display an option to open selected text as a string reference. */
  public void setOpenStrrefEnabled(boolean b) {
    if (b != openStrrefEnabled) {
      openStrrefEnabled = b;
      menuOpenEntrySeparator.setVisible(isOpenEntryEnabled());
      menuOpenStrref.setVisible(openStrrefEnabled);
    }
  }

  @Override
  public void setText(String text) {
    // skips carriage return characters
    if (text != null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c != '\r') {
          sb.append(c);
        }
      }
      super.setText(sb.toString());
    } else {
      super.setText(null);
    }
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (CMD_OPEN_RESOURCE.equals(e.getActionCommand())) {
      final JMenuItem item = (JMenuItem)e.getSource();
      final String resref = getResrefFromText();
      if (!resref.isEmpty()) {
        final String resName = resref + '.' + item.getText();
        final ResourceEntry entry = ResourceFactory.getResourceEntry(resName);
        if (entry != null) {
          new ViewFrame(getTopLevelAncestor(), ResourceFactory.getResource(entry));
        }
      }
    } else if (CMD_OPEN_STRREF.equals(e.getActionCommand())) {
      final int strref = getStrrefFromText();
      if (strref >= 0) {
        final StringLookup lookup = ChildFrame.show(StringLookup.class, StringLookup::new);
        if (lookup != null) {
          lookup.setStrref(strref);
        }
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof JViewport) {
      refreshGutterIcons();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface KeyListener ---------------------

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // Processing key strokes defined in parent components.
    // Registration of key strokes is handled by the HierarchyListener.
    final KeyStroke keyStroke = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
    final Action action = inputActionMap.get(keyStroke);
    if (action != null) {
      action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
      e.consume();
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  // --------------------- End Interface KeyListener ---------------------

  // --------------------- Begin Interface HierarchyListener ---------------------

  @Override
  public void hierarchyChanged(HierarchyEvent e) {
    // InfinityTextArea appears to discard or override certain global key stroke definitions.
    // This method registers key strokes from parent components, so that they can still be processed.
    // Registration process is placed into a HierarchyListener to register key strokes even if
    // parent components are assigned later.
    if (!(e.getChanged() instanceof JComponent && getTopLevelAncestor() instanceof JFrame)) {
      return;
    }

    final JRootPane rootPane = ((JFrame) getTopLevelAncestor()).getRootPane();
    if (rootPane == null) {
      return;
    }

    final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final KeyStroke[] keyStrokes = inputMap.allKeys();
    final ActionMap actionMap = rootPane.getActionMap();
    if (inputMap == null || keyStrokes == null || keyStrokes.length == 0 || actionMap == null) {
      return;
    }

    for (final KeyStroke keyStroke : keyStrokes) {
      final Object binding = inputMap.get(keyStroke);
      if (binding != null) {
        final Action action = actionMap.get(binding);
        if (action != null) {
          inputActionMap.put(keyStroke, action);
        }
      }
    }
  }

  // --------------------- End Interface HierarchyListener ---------------------

  // --------------------- Begin Interface PopupMenuListener ---------------------

  @Override
  public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    if (isOpenResrefEnabled()) {
      initResrefPopupMenu();
    }

    if (isOpenStrrefEnabled()) {
      initStrrefPopupMenu();
    }
  }

  @Override
  public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
  }

  @Override
  public void popupMenuCanceled(PopupMenuEvent e) {
  }

  // --------------------- End Interface PopupMenuListener ---------------------

  /**
   * Returns the underlying ScrollPane if available. Returns {@code null} otherwise.
   */
  public RTextScrollPane getScrollPane() {
    return scrollPane;
  }

  /**
   * Can be used to set the underlying ScrollPane instance.
   */
  public void setScrollPane(RTextScrollPane scrollPane) {
    if (scrollPane != this.scrollPane) {
      if (this.scrollPane != null) {
        this.scrollPane.getViewport().removeChangeListener(this);
      }

      this.scrollPane = scrollPane;
      if (this.scrollPane != null) {
        this.scrollPane.getViewport().addChangeListener(this);
        this.scrollPane.setIconRowHeaderEnabled(true);
        refreshGutterIcons();
      }
    }
  }

  /** Adds a new icon to the left vertical margin, optionally with a tooltip. */
  public void addGutterIcon(int line, Icon icon, String msg) {
    GutterIcon item = gutterIcons.get(line);
    if (item == null) {
      item = new GutterIcon(line, icon, msg);
      gutterIcons.put(line, item);
    } else {
      item.icon = icon;
      item.message = msg;
    }
    refreshGutterIcon(line);
  }

  /** Get information about the gutter icon at the specified line. */
  public GutterIcon getGutterIconInfo(int line) {
    return gutterIcons.get(line);
  }

  /** Returns whether the gutter icon at the specified line is currently applied. */
  public boolean isGutterIconActive(int line) {
    return gutterIconsActive.containsKey(line);
  }

  /** Removes the gutter icon from the specified line. */
  public void removeGutterIcon(int line) {
    if (gutterIcons.remove(line) != null) {
      refreshGutterIcon(line);
    }
  }

  /** Removes all gutter icons at once. */
  public void clearGutterIcons() {
    if (getScrollPane() != null) {
      Gutter gutter = getScrollPane().getGutter();
      Iterator<Integer> iter = gutterIconsActive.keySet().iterator();
      while (iter.hasNext()) {
        Integer key = iter.next();
        GutterIconInfo info = gutterIconsActive.get(key);
        if (info != null) {
          gutter.removeTrackingIcon(info);
        }
        iter.remove();
      }
    }
    gutterIcons.clear();
  }

  /** Returns the currently min/max visible line numbers. */
  protected Point getVisibleLineRange(Point range) {
    if (range == null) {
      range = new Point(-1, -1);
    } else {
      range.x = range.y = -1;
    }

    RTextScrollPane pane = getScrollPane();
    if (pane != null) {
      Rectangle view = pane.getViewport().getViewRect();
      Point pt = view.getLocation();
      try {
        int startLine = getLineOfOffset(viewToModel(pt));
        pt.y += view.height;
        int endLine = getLineOfOffset(viewToModel(pt));
        range.x = startLine;
        range.y = endLine;
      } catch (BadLocationException e) {
        Logger.trace(e);
      }
    }

    return range;
  }

  private void refreshGutterIcon(int line) {
    if (getScrollPane() != null) {
      Integer key = line;
      gutterIconsActive.remove(key);
      GutterIcon item = gutterIcons.get(key);
      if (item != null) {
        Point range = getVisibleLineRange(null);
        if (item.line >= range.x && item.line <= range.y) {
          try {
            GutterIconInfo info = getScrollPane().getGutter().addLineTrackingIcon(item.line, item.icon, item.message);
            gutterIconsActive.put(key, info);
          } catch (BadLocationException e) {
            Logger.trace(e);
          }
        }
      }
    }
  }

  private void refreshGutterIcons() {
    if (getScrollPane() != null && !gutterIcons.isEmpty()) {
      Gutter gutter = getScrollPane().getGutter();
      Point range = getVisibleLineRange(null);
      if (range.x >= 0) {
        // 1. remove items outside of range
        Iterator<Integer> iter = gutterIconsActive.keySet().iterator();
        while (iter.hasNext()) {
          Integer key = iter.next();
          int line = key;
          if (line < range.x || line > range.y) {
            GutterIconInfo info = gutterIconsActive.get(key);
            gutter.removeTrackingIcon(info);
            iter.remove();
          }
        }

        // 2. add items that are inside of range
        SortedMap<Integer, GutterIcon> submap = gutterIcons.subMap(range.x, range.y + 1);
        if (!submap.isEmpty()) {
          for (final Integer key : submap.keySet()) {
            if (!gutterIconsActive.containsKey(key)) {
              GutterIcon item = submap.get(key);
              try {
                GutterIconInfo info = gutter.addLineTrackingIcon(item.line, item.icon, item.message);
                gutterIconsActive.put(item.line, info);
              } catch (BadLocationException e) {
                Logger.trace(e);
              }
            }
          }
        }
      }
    }
  }

  // Returns scaled global font
  private Font getGlobalFont() {
    Font f = BrowserMenuBar.isInstantiated() ? BrowserMenuBar.getInstance().getOptions().getScriptFont() : getFont();
    if (f != null) {
      f = Misc.getScaledFont(f);
    }
    return f;
  }

  // First-time initializations
  private void initTextArea() {
    setFont(getGlobalFont());

    menuOpenEntrySeparator.setVisible(isOpenEntryEnabled());

    menuOpenStrref.addActionListener(this);
    menuOpenStrref.setActionCommand(CMD_OPEN_STRREF);
    menuOpenStrref.setVisible(openStrrefEnabled);

    menuOpenResource.setVisible(openResrefEnabled);

    final JPopupMenu popup = getPopupMenu();
    if (popup != null) {
      popup.add(menuOpenEntrySeparator);
      popup.add(menuOpenStrref);
      popup.add(menuOpenResource);
      popup.addPopupMenuListener(this);
    }
  }

  // Called lazily to initialize list of potential game resource types
  private void initOpenResource() {
    if (resourceTypeItems == null) {
      resourceTypeItems = new ArrayList<>();
    }

    if (resourceTypeItems.isEmpty() && Profile.isGameOpen()) {
      final String[] types = Profile.getAvailableResourceTypes();
      final Keyfile keyFile = ResourceFactory.getKeyfile();
      for (final String type : types) {
        final ImageIcon icon = (keyFile != null) ? keyFile.getIcon(type) : null;
        final JMenuItem item = new JMenuItem(type, icon);
        item.addActionListener(this);
        item.setActionCommand(CMD_OPEN_RESOURCE);
        resourceTypeItems.add(item);
      }
    }
  }

  /** Returns whether any of the "open xxx" menu entries have been enabled. */
  private boolean isOpenEntryEnabled() {
    return openResrefEnabled || openStrrefEnabled;
  }

  /**
   * Returns the current text selection. If no text is selected then the word under the text cursor is returned.
   *
   * @return Returns the current selection or word under the text cursor. Returns empty string if word could not be
   *         determined.
   */
  private String getTextWord() {
    // invalid characters for potential resref or strref strings
    final String invalid = "*=:.,;\"'[]()/|\\?<>`";

    String text = getSelectedText();
    if (text == null || text.isEmpty()) {
      try {
        final int startOfs = getLineStartOffsetOfCurrentLine();
        final int endOfs = getLineEndOffsetOfCurrentLine();
        final String line = getText(startOfs, endOfs - startOfs);

        int curStartOfs = getCaretPosition() - startOfs;
        while (curStartOfs >= 0) {
          final char ch = line.charAt(curStartOfs);
          if (Character.isWhitespace(ch) || invalid.indexOf(ch) >= 0) {
            curStartOfs++;
            break;
          }
          curStartOfs--;
        }
        curStartOfs = Math.max(0, curStartOfs);

        int curEndOfs = getCaretPosition() - startOfs;
        while (curEndOfs < endOfs) {
          final char ch = line.charAt(curEndOfs);
          if (Character.isWhitespace(ch) || invalid.indexOf(ch) >= 0) {
            break;
          }
          curEndOfs++;
        }

        if (curEndOfs > curStartOfs) {
          text = line.substring(curStartOfs, curEndOfs).trim();
        }
      } catch (BadLocationException e) {
      }
    }

    if (text == null) {
      text = "";
    }

    return text;
  }

  /**
   * Captures the currently selected text or determines the word under the caret and transforms it into a valid strref
   * value if possible.
   *
   * @return String reference if available, -1 otherwise.
   */
  private int getStrrefFromText() {
    return getStrrefFromText(getTextWord());
  }

  /**
   * Attempts to convert the given string into a valid string reference.
   *
   * @param text Text to convert.
   * @return String reference if available, -1 otherwise.
   */
  private int getStrrefFromText(String text) {
    int retVal = -1;
    if (text != null) {
      final Matcher m = Pattern.compile("-?\\b((0x[0-9a-fA-F]+)|(\\d+))\\b").matcher(text);
      if (m.find()) {
        final String group = m.group();
        try {
          if (group.contains("0x")) {
            retVal = Integer.parseInt(group.replace("0x", ""), 16);
          } else {
            retVal = Integer.parseInt(group);
          }
          final Table2da vtable = Table2daCache.get("ENGINEST.2DA");
          final int numVirtual = (vtable != null) ? vtable.getRowCount() : 0;
          boolean valid = (retVal >= StringTable.STRREF_VIRTUAL && retVal < StringTable.STRREF_VIRTUAL + numVirtual)
              || (retVal >= 0 && retVal < StringTable.getNumEntries());
          if (!valid) {
            retVal = -1;
          }
        } catch (NumberFormatException e) {
        }
      }
    }

    return retVal;
  }

  /**
   * Captures the currently selected text or determines the word under the caret and transforms it into a valid resource
   * resref string if possible.
   *
   * @return Resource reference string if available, empty string otherwise.
   */
  private String getResrefFromText() {
    return getResrefFromText(getTextWord());
  }

  /**
   * Returns the processed string if it contains a valid resource resref string.
   *
   * @param text Text to process.
   * @return Resource reference string if available, empty string otherwise.
   */
  private String getResrefFromText(String text) {
    String retVal = "";
    if (text != null) {
      retVal = text.trim();
      if (retVal.isEmpty() || retVal.length() > 8) {
        retVal = "";
      } else if (Pattern.compile("\\s+").matcher(retVal).find()) {
        retVal = "";
      }
    }
    return retVal;
  }

  /** Updates the "open resref" popup menu to the current state. */
  private void initResrefPopupMenu() {
    // removing old resource type entries
    for (int i = menuOpenResource.getMenuComponentCount() - 1; i >= 0; i--) {
      final Component c = menuOpenResource.getMenuComponent(i);
      if (c instanceof JMenuItem) {
        menuOpenResource.remove(i);
      }
    }
    menuOpenResource.setEnabled(false);
    menuOpenResource.setText(OPEN_RESOURCE_LABEL_DEFAULT);

    // populating menu with available resource types
    final String resref = getResrefFromText();
    if (!resref.isEmpty()) {
      int count = 0;
      for (final JMenuItem item : resourceTypeItems) {
        final boolean exists = ResourceFactory.resourceExists(resref + '.' + item.getText());
        if (exists) {
          menuOpenResource.add(item);
          count++;
        }
      }

      if (count > 0) {
        menuOpenResource.setEnabled(true);
        menuOpenResource.setText(String.format(OPEN_RESOURCE_LABEL_FMT, resref));
      }
    }
  }

  /** Updates the "open strref" popup menu to the current state. */
  private void initStrrefPopupMenu() {
    final int strref = getStrrefFromText();
    if (strref >= 0) {
      menuOpenStrref.setEnabled(true);
      menuOpenStrref.setText(String.format(OPEN_STRREF_LABEL_FMT, strref));
    } else {
      menuOpenStrref.setEnabled(false);
      menuOpenStrref.setText(OPEN_STRREF_LABEL_DEFAULT);
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  public static class GutterIcon {
    int line;
    Icon icon;
    String message;

    public GutterIcon(int line, Icon icon, String message) {
      this.line = line;
      this.icon = icon;
      this.message = message;
    }
  }
}
