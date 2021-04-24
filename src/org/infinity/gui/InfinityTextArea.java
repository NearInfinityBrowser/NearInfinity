// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.infinity.resource.text.modes.BCSFoldParser;
import org.infinity.resource.text.modes.BCSTokenMaker;
import org.infinity.resource.text.modes.GLSLTokenMaker;
import org.infinity.resource.text.modes.TLKTokenMaker;
import org.infinity.resource.text.modes.WeiDULogTokenMaker;
import org.infinity.util.Misc;

/**
 * Extends {@link RSyntaxTextArea} by NearInfinity-specific features.
 */
public class InfinityTextArea extends RSyntaxTextArea implements ChangeListener
{
  /** Available languages for syntax highlighting. */
  public enum Language {
    /** Disables syntax highlighting */
    NONE,
    /** Select BCS highlighting. */
    BCS,
    /** Select TLK highlighting. */
    TLK,
    /** Select GLSL highlighting. */
    GLSL,
    /** Select LUA highlighting. */
    LUA,
    /** Select SQL highlighting. */
    SQL,
    /** Select WeiDU.log highlighting. */
    WEIDU,
  }

  /** Available color schemes for use when enabling syntax highlighting. */
  public enum Scheme {
    /** Disables any color scheme. */
    NONE,
    /** The default color scheme. */
    DEFAULT,
    /** Color scheme based on Notepad++'s Obsidian scheme. */
    DARK,
    /** Color scheme based on Eclipse's defaults. */
    ECLIPSE,
    /** Color scheme based on IntelliJ IDEA's defaults. */
    IDEA,
    /** A dark color scheme inspired by "Monokai". */
    MONOKAI,
    /** Color scheme based on Microsoft Visual Studio's defaults. */
    VS,
    /** Color scheme loosely based on the WeiDU Syntax Highlighter for Notepad++. */
    BCS,
  }

  /** The default color of the currently highlighted text line. */
  public static final Color DefaultLineHighlightColor = new Color(0xe8e8ff);

  /** Color scheme for unicolored text */
  public static final String SchemeNone = "org/infinity/resource/text/modes/ThemeNone.xml";
  /** Default color scheme */
  public static final String SchemeDefault = "org/infinity/resource/text/modes/ThemeDefault.xml";
  /** Dark color scheme */
  public static final String SchemeDark = "org/infinity/resource/text/modes/ThemeDark.xml";
  /** Eclipse color scheme */
  public static final String SchemeEclipse = "org/infinity/resource/text/modes/ThemeEclipse.xml";
  /** IntelliJ IDEA color scheme */
  public static final String SchemeIdea = "org/infinity/resource/text/modes/ThemeIdea.xml";
  /** "Monokai" color scheme */
  public static final String SchemeMonokai = "org/infinity/resource/text/modes/ThemeMonokai.xml";
  /** Visual Studio color scheme */
  public static final String SchemeVs = "org/infinity/resource/text/modes/ThemeVs.xml";
  /** BCS color scheme based on WeiDU Highlighter for Notepad++ */
  public static final String SchemeBCS = "org/infinity/resource/text/modes/ThemeBCSLight.xml";

  private static EnumMap<Scheme, String> SchemeMap = new EnumMap<>(Scheme.class);

  static {
    // adding custom code folding definitions
    FoldParserManager.get().addFoldParserMapping(BCSTokenMaker.SYNTAX_STYLE_BCS, new BCSFoldParser());
    FoldParserManager.get().addFoldParserMapping(GLSLTokenMaker.SYNTAX_STYLE_GLSL, new CurlyFoldParser());

    // adding custom syntax highlighting definitions
    ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance())
      .putMapping(BCSTokenMaker.SYNTAX_STYLE_BCS, "org.infinity.resource.text.modes.BCSTokenMaker");
    ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance())
      .putMapping(TLKTokenMaker.SYNTAX_STYLE_TLK, "org.infinity.resource.text.modes.TLKTokenMaker");
    ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance())
      .putMapping(WeiDULogTokenMaker.SYNTAX_STYLE_WEIDU, "org.infinity.resource.text.modes.WeiDULogTokenMaker");
    ((AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance())
      .putMapping(GLSLTokenMaker.SYNTAX_STYLE_GLSL, "org.infinity.resource.text.modes.GLSLTokenMaker");

    // initializing color schemes
    SchemeMap.put(Scheme.NONE, SchemeNone);
    SchemeMap.put(Scheme.DEFAULT, SchemeDefault);
    SchemeMap.put(Scheme.DARK, SchemeDark);
    SchemeMap.put(Scheme.ECLIPSE, SchemeEclipse);
    SchemeMap.put(Scheme.IDEA, SchemeIdea);
    SchemeMap.put(Scheme.MONOKAI, SchemeMonokai);
    SchemeMap.put(Scheme.VS, SchemeVs);
    SchemeMap.put(Scheme.BCS, SchemeBCS);
  }

  private final SortedMap<Integer, GutterIcon> gutterIcons = new TreeMap<>();
  private final Map<Integer, GutterIconInfo> gutterIconsActive = new HashMap<>();

  private RTextScrollPane scrollPane;

  /**
   * Constructor.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(boolean applySettings)
  {
    super();
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param doc The document for the editor.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(RSyntaxDocument doc, boolean applySettings)
  {
    super(doc);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param text The initial text to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(String text, boolean applySettings)
  {
    super(text);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param textMode Either {@code INSERT_MODE} or {@code OVERWRITE_MODE}.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityTextArea(int textMode, boolean applySettings)
  {
    super(textMode);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(int rows, int cols, boolean applySettings)
  {
    super(rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param text The initial text to display.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(String text, int rows, int cols, boolean applySettings)
  {
    super(text, rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Constructor.
   * @param doc The document for the editor.
   * @param text The initial text to display.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   * @throws IllegalArgumentException If either {@code rows} or {@code cols} is negative.
   */
  public InfinityTextArea(RSyntaxDocument doc, String text, int rows, int cols, boolean applySettings)
  {
    super(doc, text, rows, cols);
    if (applySettings) {
      applySettings(true);
      applyExtendedSettings(null, null);
    }
    setFont(getGlobalFont());
  }

  /**
   * Applies global text editor settings to the specified {@link RSyntaxTextArea} component.
   * @param resetUndo Specifies whether the undo history will be discarded.
   */
  public static void applySettings(RSyntaxTextArea edit, boolean resetUndo)
  {
    if (edit != null) {
      edit.setCurrentLineHighlightColor(DefaultLineHighlightColor);
      if (BrowserMenuBar.getInstance() != null) {
        edit.setTabsEmulated(BrowserMenuBar.getInstance().isTextTabEmulated());
        edit.setTabSize(BrowserMenuBar.getInstance().getTextTabSize());
        edit.setWhitespaceVisible(BrowserMenuBar.getInstance().getTextWhitespaceVisible());
        edit.setEOLMarkersVisible(BrowserMenuBar.getInstance().getTextEOLVisible());
        edit.setHighlightCurrentLine(BrowserMenuBar.getInstance().getTextHighlightCurrentLine());
      } else {
        // default settings
        edit.setTabsEmulated(false);
        edit.setTabSize(4);
        edit.setWhitespaceVisible(false);
        edit.setEOLMarkersVisible(false);
        edit.setHighlightCurrentLine(false);
      }
      if (resetUndo) {
        edit.discardAllEdits();   // clearing undo history
      }
    }
  }

  /**
   * Applies syntax highlighting and color schemes to the specified {@link RSyntaxTextArea} component.
   * @param language The language to highlight. Specifying {@code null} uses
   *                 {@code Language.NONE} for syntax hightlighting.
   * @param scheme A color scheme to apply to the specified {@link RSyntaxTextArea} component.
   *               Specifying {@code null} uses the color scheme as defined for the specified
   *               language.
   */
  public static void applyExtendedSettings(RSyntaxTextArea edit, Language language, Scheme scheme)
  {
    if (edit != null) {
      if (language == null) {
        language = Language.NONE;
      }

      // applying syntax highlighting
      String style;
      switch (language) {
        case BCS:
          style = BCSTokenMaker.SYNTAX_STYLE_BCS;
          break;
        case TLK:
          style = TLKTokenMaker.SYNTAX_STYLE_TLK;
          break;
        case GLSL:
          style = GLSLTokenMaker.SYNTAX_STYLE_GLSL;
          break;
        case LUA:
          style = SyntaxConstants.SYNTAX_STYLE_LUA;
          break;
        case SQL:
          style = SyntaxConstants.SYNTAX_STYLE_SQL;
          break;
        case WEIDU:
          style = WeiDULogTokenMaker.SYNTAX_STYLE_WEIDU;
          break;
        default:
          style = SyntaxConstants.SYNTAX_STYLE_NONE;
      }
      edit.setSyntaxEditingStyle(style);


      // applying color scheme
      String schemePath;
      if (scheme != null) {
        // applying explicit color scheme
        schemePath = SchemeMap.get(scheme);
        if (schemePath == null || schemePath.isEmpty()) {
          schemePath = SchemeNone;
        }
      } else {
        // applying implicit color scheme
        schemePath = SchemeNone;
        switch (language) {
          case BCS:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getBcsColorScheme();
            }
            break;
          case TLK:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getTlkColorScheme();
            }
            break;
          case GLSL:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getGlslColorScheme();
            }
            break;
          case LUA:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getLuaColorScheme();
            }
            break;
          case SQL:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getSqlColorScheme();
            }
            break;
          case WEIDU:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getWeiDUColorScheme();
            }
            break;
          default:
        }
      }

      try (InputStream is = ClassLoader.getSystemResourceAsStream(schemePath)) {
        Theme theme = Theme.load(is);
        if (theme != null) {
          theme.apply(edit);
        }
      } catch (NullPointerException e) {
        // ignore
      } catch (IOException e) {
        e.printStackTrace();
      }


      // apply code folding
      switch (language) {
        case BCS:
          if (BrowserMenuBar.getInstance() != null) {
            edit.setCodeFoldingEnabled(BrowserMenuBar.getInstance().getBcsCodeFoldingEnabled());
          } else {
            edit.setCodeFoldingEnabled(false);
          }
          break;
        case GLSL:
          if (BrowserMenuBar.getInstance() != null) {
            edit.setCodeFoldingEnabled(BrowserMenuBar.getInstance().getGlslCodeFoldingEnabled());
          } else {
            edit.setCodeFoldingEnabled(false);
          }
          break;
        default:
          edit.setCodeFoldingEnabled(false);
      }
    }
  }

  /**
   * Applies global text editor settings to this component.
   * @param resetUndo Specifies whether the undo history will be discarded.
   */
  public void applySettings(boolean resetUndo)
  {
    applySettings(this, resetUndo);
  }

  /**
   * Applies syntax highlighting and color schemes to this component.
   * @param language The language to highlight. Specifying {@code null} uses
   *                 {@code Language.NONE} for syntax hightlighting.
   * @param scheme A color scheme to apply to the specified {@link RSyntaxTextArea} component.
   *               Specifying {@code null} uses the color scheme as defined for the specified
   *               language.
   */
  public void applyExtendedSettings(Language language, Scheme scheme)
  {
    applyExtendedSettings(this, language, scheme);
  }

  @Override
  public void setText(String text)
  {
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

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e)
  {
    if (e.getSource() instanceof JViewport) {
      refreshGutterIcons();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

  /**
   * Returns the underlying ScrollPane if available. Returns {@code null} otherwise.
   */
  public RTextScrollPane getScrollPane()
  {
    return scrollPane;
  }

  /**
   * Can be used to set the underlying ScrollPane instance.
   */
  public void setScrollPane(RTextScrollPane scrollPane)
  {
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
  public void addGutterIcon(int line, Icon icon, String msg)
  {
    GutterIcon item = gutterIcons.get(line);
    if (item == null) {
      item = new GutterIcon(line, icon, msg);
      gutterIcons.put(Integer.valueOf(line), item);
    } else {
      item.icon = icon;
      item.message = msg;
    }
    refreshGutterIcon(line);
  }

  /** Get information about the gutter icon at the specified line. */
  public GutterIcon getGutterIconInfo(int line)
  {
    return gutterIcons.get(Integer.valueOf(line));
  }

  /** Returns whether the gutter icon at the specified line is currently applied. */
  public boolean isGutterIconActive(int line)
  {
    return gutterIconsActive.containsKey(Integer.valueOf(line));
  }

  /** Removes the gutter icon from the specified line. */
  public void removeGutterIcon(int line)
  {
    if (gutterIcons.remove(Integer.valueOf(line)) != null) {
      refreshGutterIcon(line);
    }
  }

  /** Removes all gutter icons at once. */
  public void clearGutterIcons()
  {
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
  protected Point getVisibleLineRange(Point range)
  {
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
      }
    }

    return range;
  }

  private void refreshGutterIcon(int line)
  {
    if (getScrollPane() != null) {
      Integer key = Integer.valueOf(line);
      gutterIconsActive.remove(key);
      GutterIcon item = gutterIcons.get(key);
      if (item != null) {
        Point range = getVisibleLineRange(null);
        if (item.line >= range.x && item.line <= range.y) {
          try {
            GutterIconInfo info = getScrollPane().getGutter().addLineTrackingIcon(item.line, item.icon, item.message);
            gutterIconsActive.put(key, info);
          } catch (BadLocationException e) {
          }
        }
      }
    }
  }

  private void refreshGutterIcons()
  {
    if (getScrollPane() != null && !gutterIcons.isEmpty()) {
      Gutter gutter = getScrollPane().getGutter();
      Point range = getVisibleLineRange(null);
      if (range.x >= 0) {
        // 1. remove items outside of range
        Iterator<Integer> iter = gutterIconsActive.keySet().iterator();
        while (iter.hasNext()) {
          Integer key = iter.next();
          int line = key.intValue();
          if (line < range.x || line > range.y) {
            GutterIconInfo info = gutterIconsActive.get(key);
            gutter.removeTrackingIcon(info);
            iter.remove();
          }
        }

        // 2. add items that are inside of range
        SortedMap<Integer, GutterIcon> submap = gutterIcons.subMap(Integer.valueOf(range.x), Integer.valueOf(range.y + 1));
        if (!submap.isEmpty()) {
          for (final Integer key: submap.keySet()) {
            if (!gutterIconsActive.containsKey(key)) {
              GutterIcon item = submap.get(key);
              try {
                GutterIconInfo info = gutter.addLineTrackingIcon(item.line, item.icon, item.message);
                gutterIconsActive.put(Integer.valueOf(item.line), info);
              } catch (BadLocationException e) {
              }
            }
          }
        }
      }
    }
  }

  // Returns scaled global font
  private Font getGlobalFont() {
    Font f = (BrowserMenuBar.getInstance() != null) ? BrowserMenuBar.getInstance().getScriptFont() : getFont();
    if (f != null)
      f = Misc.getScaledFont(f);
    return f;
  }


//-------------------------- INNER CLASSES --------------------------

  static class GutterIcon
  {
    int line;
    Icon icon;
    String message;

    public GutterIcon(int line, Icon icon, String message)
    {
      this.line = line;
      this.icon = icon;
      this.message = message;
    }
  }
}
