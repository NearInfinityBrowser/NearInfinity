// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ScrollPaneConstants;

import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Extends {@link RTextScrollPane} by NearInfinity-specific features.
 */
public class InfinityScrollPane extends RTextScrollPane
{


  /**
   * Constructor. If you use this constructor, you must call {@link #setViewportView(Component)}
   * and pass in an RTextArea for this scroll pane to render line numbers properly.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityScrollPane(boolean applySettings)
  {
    super();
    if (applySettings) {
      applySettings();
    }
  }

  /**
   * Creates a scroll pane. A default value will be used for line number color (gray),
   * and the current line's line number will be highlighted.
   * @param comp The component this scroll pane should display. This should be an instance of
   *             {@link RTextArea}, {@code javax.swing.JLayer} (or the older
   *             {@code org.jdesktop.jxlayer.JXLayer}), or {@code null}.
   *             If this argument is null, you must call {@link #setViewportView(Component)},
   *             passing in an instance of one of the types above.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityScrollPane(Component comp, boolean applySettings)
  {
    super(comp);
    if (comp instanceof InfinityTextArea) {
      ((InfinityTextArea)comp).setScrollPane(this);
    }
    if (applySettings) {
      applySettings();
    }
  }

  /**
   * Creates a scroll pane. A default value will be used for line number color (gray),
   * and the current line's line number will be highlighted.
   * @param comp The component this scroll pane should display. This should be an instance of
   *             {@link RTextArea}, {@code javax.swing.JLayer} (or the older
   *             {@code org.jdesktop.jxlayer.JXLayer}), or {@code null}.
   *             If this argument is null, you must call {@link #setViewportView(Component)},
   *             passing in an instance of one of the types above.
   * @param lineNumbers Whether line numbers should be enabled.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityScrollPane(Component comp, boolean lineNumbers, boolean applySettings)
  {
    super(comp, lineNumbers);
    if (applySettings) {
      applySettings();
    }
    setLineNumbersEnabled(lineNumbers);
  }

  /**
   * Creates a scroll pane.
   * @param comp The component this scroll pane should display. This should be an instance of
   *             {@link RTextArea}, {@code javax.swing.JLayer} (or the older
   *             {@code org.jdesktop.jxlayer.JXLayer}), or {@code null}.
   *             If this argument is null, you must call {@link #setViewportView(Component)},
   *             passing in an instance of one of the types above.
   * @param lineNumbers Whether line numbers should be enabled.
   * @param lineNumberColor The color to use for line numbers.
   * @param applySettings If {@code true}, applies global text editor settings to this component.
   */
  public InfinityScrollPane(Component comp, boolean lineNumbers, Color lineNumberColor,
                            boolean applySettings)
  {
    super(comp, lineNumbers, lineNumberColor);
    if (applySettings) {
      applySettings();
    }
    setLineNumbersEnabled(lineNumbers);
    getGutter().setLineNumberColor(lineNumberColor);
  }

  /** Applies global text editor settings to the specified {@link RTextScrollPane} component. */
  public static void applySettings(RTextScrollPane pane)
  {
    if (pane != null) {
      pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      if (BrowserMenuBar.getInstance() != null) {
        pane.setLineNumbersEnabled(BrowserMenuBar.getInstance().getTextLineNumbers());
      } else {
        pane.setLineNumbersEnabled(false);
      }
    }
  }

  /** Applies language-specific settings to the specified {@link RTextScrollPane} component. */
  public static void applyExtendedSettings(RTextScrollPane pane, InfinityTextArea.Language language)
  {
    if (language != null) {
      switch (language) {
        case BCS:
          if (BrowserMenuBar.getInstance() != null) {
            pane.setFoldIndicatorEnabled(BrowserMenuBar.getInstance().getBcsCodeFoldingEnabled());
          } else {
            pane.setFoldIndicatorEnabled(false);
          }
          break;
        case GLSL:
          if (BrowserMenuBar.getInstance() != null) {
            pane.setFoldIndicatorEnabled(BrowserMenuBar.getInstance().getGlslCodeFoldingEnabled());
          } else {
            pane.setFoldIndicatorEnabled(false);
          }
          break;
        default:
          pane.setFoldIndicatorEnabled(false);
      }
    }
  }

  /** Applies global text editor settings to this component. */
  public void applySettings()
  {
    applySettings(this);
  }

  /** Applies language-specific settings to this component. */
  public void applyExtendedSettings(InfinityTextArea.Language language)
  {
    applyExtendedSettings(this, language);
  }
}
