// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.resource.text.modes.BCSTokenMaker;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

import javax.swing.ScrollPaneConstants;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Encapsulates <code>RTextScrollPane</code> along with a pre-configured <code>RSyntaxTextArea</code>
 * and applies NI-specific settings.
 * Additionally provides static methods for initializing editor-specific settings,
 * syntax highlighting and color schemes.
 * @author argent77
 */
public class ScrolledTextArea extends RTextScrollPane
{
  public static final Color DefaultLineHighlightColor = new Color(0xe8e8ff);

  /** Default color scheme */
  public static final String SchemeDefault = "infinity/resource/text/modes/ThemeDefault.xml";
  /** Dark color scheme */
  public static final String SchemeDark = "infinity/resource/text/modes/ThemeDark.xml";
  /** Eclipse color scheme */
  public static final String SchemeEclipse = "infinity/resource/text/modes/ThemeEclipse.xml";
  /** IntelliJ IDEA color scheme */
  public static final String SchemeIdea = "infinity/resource/text/modes/ThemeIdea.xml";
  /** Visual Studio color scheme */
  public static final String SchemeVs = "infinity/resource/text/modes/ThemeVs.xml";
  /** BCS color scheme based on WeiDU Highlighter for Notepad++ */
  public static final String SchemeBCS = "infinity/resource/text/modes/ThemeBCSLight.xml";


  // TODO: create GLSL highlighting definitions
  /** Available languages for syntax highlighting. */
  public enum Language {
    /** Disables syntax highlighting */
    NONE,
    /** Select BCS highlighting. */
    BCS,
    /** Select GLSL highlighting. */
    GLSL,
    /** Select SQL highlighting. */
    SQL,
  }

  /** Available color schemes for use when enabling syntax highlighting. */
  public enum Scheme {
    /** Synonym for <code>DEFAULT</code>. */
    NONE,
    /** The default color scheme. */
    DEFAULT,
    /** Color scheme based on Notepad++'s Obsidian scheme. */
    DARK,
    /** Color scheme based on Eclipse's defaults. */
    ECLIPSE,
    /** Color scheme based on IntelliJ IDEA's defaults. */
    IDEA,
    /** Color scheme based on Microsoft Visual Studio's defaults. */
    VS,
    /** Color scheme loosely based on the WeiDU Syntax Highlighter for Notepad++. */
    BCS,
  }

  private static EnumMap<Scheme, String> SchemeMap = new EnumMap<Scheme, String>(Scheme.class);

  static {
//    FoldParserManager.get().addFoldParserMapping(BCSTokenMaker.SYNTAX_STYLE_BCS, new BCSFoldParser());

    SchemeMap.put(Scheme.DEFAULT, SchemeDefault);
    SchemeMap.put(Scheme.DARK, SchemeDark);
    SchemeMap.put(Scheme.ECLIPSE, SchemeEclipse);
    SchemeMap.put(Scheme.IDEA, SchemeIdea);
    SchemeMap.put(Scheme.VS, SchemeVs);
    SchemeMap.put(Scheme.BCS, SchemeBCS);
  }

  private RSyntaxTextArea edit;

  /**
   * Constructor.
   */
  public ScrolledTextArea()
  {
    super();
    edit = new InfinityTextArea();
    init(true);
  }

  /**
   * Constructor.
   * @param doc The document for the editor.
   */
  public ScrolledTextArea(RSyntaxDocument doc)
  {
    super();
    edit = new InfinityTextArea(doc);
    init(true);
  }

  /**
   * Constructor.
   * @param text The initial text to display.
   */
  public ScrolledTextArea(String text)
  {
    super();
    edit = new InfinityTextArea(text);
    init(true);
  }

  /**
   * Creates a new InfinityTextArea.
   * @param textMode Either INSERT_MODE or OVERWRITE_MODE.
   */
  public ScrolledTextArea(int textMode)
  {
    super();
    edit = new InfinityTextArea(textMode);
    init(true);
  }

  /**
   * Constructor.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   */
  public ScrolledTextArea(int rows, int cols)
  {
    super();
    edit = new InfinityTextArea(rows, cols);
    init(true);
  }

  /**
   * Constructor.
   * @param text The initial text to display.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   */
  public ScrolledTextArea(String text, int rows, int cols)
  {
    super();
    edit = new InfinityTextArea(text, rows, cols);
    init(true);
  }

  /**
   * Constructor.
   * @param doc The document for the editor.
   * @param text The initial text to display.
   * @param rows The number of rows to display.
   * @param cols The number of columns to display.
   */
  public ScrolledTextArea(RSyntaxDocument doc, String text, int rows, int cols)
  {
    super();
    edit = new InfinityTextArea(doc, text, rows, cols);
    init(true);
  }

  @Override
  public void setViewportView(Component view)
  {
    if (view instanceof RSyntaxTextArea) {
      edit = (RSyntaxTextArea)view;
      super.setViewportView(edit);
      initEditor(true);
    }
  }

  /** (Re-)Applies global preferences to the component. */
  public void setup(boolean resetUndo)
  {
    init(resetUndo);
  }

  /**
   * Applies syntax highlighting to the specified text component.
   * @param language The language to highlight (Specifying <code>Language.NONE<code> or
   *        <code>null</code> disables syntax highlighting).
   * @param scheme A color scheme to apply (only relevant when enabling a language).
   *               Specifying <code>null</code> applies the default color scheme.
   */
  public void setSyntaxHighlighter(Language language, Scheme scheme)
  {
    setSyntaxHighlighter(edit, language, scheme);
  }

  /** Applies global preferences to the specified scrollpane and associated view component. */
  public static void setupScrolledEditor(ScrolledTextArea scroll, boolean resetUndo)
  {
    if (scroll != null) {
      scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//      scroll.setFoldIndicatorEnabled(true);
      if (BrowserMenuBar.getInstance() != null) {
        scroll.setLineNumbersEnabled(BrowserMenuBar.getInstance().getTextLineNumbers());
      } else {
        // default settings
        scroll.setLineNumbersEnabled(false);
      }
      if (scroll.getTextArea() instanceof RSyntaxTextArea) {
        setupEditor((RSyntaxTextArea)scroll.getTextArea(), resetUndo);
      }
    }
  }

  /** Applies global preferences to the specified text component. */
  public static void setupEditor(RSyntaxTextArea edit, boolean resetUndo)
  {
    if (edit != null) {
      edit.setCurrentLineHighlightColor(DefaultLineHighlightColor);
      edit.setBracketMatchingEnabled(true);
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
   * Applies syntax highlighting to the specified text component.
   * @param edit The text component.
   * @param language The language to highlight (Specifying <code>Language.NONE<code> or
   *        <code>null</code> disables syntax highlighting).
   * @param scheme A color scheme to apply (only relevant when enabling a language).
   *               If <code>null</code> has been specified, the currently selected color scheme for
   *               the language will be applied.
   */
  public static void setSyntaxHighlighter(RSyntaxTextArea edit, Language language, Scheme scheme)
  {
    if (edit != null) {
      if (language == null) {
        language = Language.NONE;
      }

      // applying syntax highlighting
      String style;
      switch (language) {
        case BCS:
        {
          AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance();
          atmf.putMapping(BCSTokenMaker.SYNTAX_STYLE_BCS, "infinity.resource.text.modes.BCSTokenMaker");
          style = BCSTokenMaker.SYNTAX_STYLE_BCS;
          edit.setPaintMatchedBracketPair(true);
        }
        break;
        case GLSL:
          style = SyntaxConstants.SYNTAX_STYLE_C;
          edit.setPaintMatchedBracketPair(true);
          break;
        case SQL:
          style = SyntaxConstants.SYNTAX_STYLE_SQL;
          break;
        default:
          style = SyntaxConstants.SYNTAX_STYLE_NONE;
      }
      edit.setSyntaxEditingStyle(style);

      // applying color scheme
      String schemePath;
      if (scheme != null) {
        // applying explicit scheme
        schemePath = SchemeMap.get(scheme);
        if (schemePath == null || schemePath.isEmpty()) {
          schemePath = SchemeDefault;
        }
      } else {
        // applying implicit scheme
        schemePath = SchemeDefault;
        switch (language) {
          case BCS:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getBcsColorScheme();
            }
            break;
          case GLSL:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getGlslColorScheme();
            }
            break;
          case SQL:
            if (BrowserMenuBar.getInstance() != null) {
              schemePath = BrowserMenuBar.getInstance().getSqlColorScheme();
            }
            break;
          default:
        }
      }
      InputStream is = ClassLoader.getSystemResourceAsStream(schemePath);
      if (is != null) {
        try {
          Theme theme = Theme.load(is);
          if (theme != null) {
            theme.apply(edit);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      // handling code folding
//      if (language == Language.BCS && BrowserMenuBar.getInstance() != null) {
//        edit.setCodeFoldingEnabled(BrowserMenuBar.getInstance().getBcsCodeFoldingEnabled());
//      } else {
//        edit.setCodeFoldingEnabled(false);
//      }
    }
  }

  /** First-time initializations. */
  protected void init(boolean resetUndo)
  {
    setViewportView(edit);
    setupScrolledEditor(this, resetUndo);
    initEditor(resetUndo);
  }

  /** First-time initializations of the attached RTextEditor instance. */
  protected void initEditor(boolean resetUndo)
  {
    setupEditor(edit, resetUndo);
  }
}
