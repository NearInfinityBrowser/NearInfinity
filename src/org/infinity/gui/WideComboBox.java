// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.util.Arrays;
import java.util.function.Function;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * A customized version of {@link JComboBox} that dynamically adapts the width of the popup menu to the
 * width of available list items.
 *
 * @param <E> the type of the elements of this combo box.
 */
class WideComboBox<E> extends JComboBox<E> {
  /** Default string formatter for combo box elements. */
  private final Function<E, String> defaultFormatter = item -> String.format("%s", item);

  private final int maxWidth;
  private final int extraWidth;

  private Function<E, String> formatter;
  private boolean layingOut;
  private boolean wide;
  private int widestLength;

  /**
   * Creates a new {@code WideComboBox} with a default data model. Adaptable width is enabled and
   * maximum width of the popup menu is set to 80 instances of letter 'X'.
   */
  public WideComboBox() {
    this(true);
  }

  /**
   * Creates a new {@code WideComboBox} with a default data model. Maximum width of the popup menu
   * is set to 80 instances of letter 'X'.
   *
   * @param wide whether adaptable width of the popup menu is enabled.
   */
  public WideComboBox(boolean wide) {
    this(wide, createString('X', 80));
  }

  /**
   * Creates a new {@code WideComboBox} with a default data model.
   *
   * @param wide whether adaptable width of the popup menu is enabled.
   * @param prototype String used to calculate the maximum width of the popup menu.
   */
  public WideComboBox(boolean wide, String prototype) {
    super();

    maxWidth = getStringWidth(getFontMetrics(getFont()), getValue(prototype, "XXXXXXXX.XXXX"));

    // extraWidth is added to the width calculation of list items which includes scrollbars, borders, etc.
    int borderWidth = 2;
    final Border b = UIManager.getDefaults().getBorder("PopupMenu.border");
    if (b != null) {
      final Insets insets = b.getBorderInsets(this);
      if (insets != null) {
        borderWidth = insets.left + insets.right;
      }
    }
    extraWidth = UIManager.getDefaults().getInt("ScrollBar.width") + borderWidth + 2; // add some breathing space

    setFormatter(null);
    setWide(wide);
  }

  public boolean isWide() {
    return wide;
  }

  public void setWide(boolean wide) {
    if (wide != this.wide) {
      this.wide = wide;
      this.widestLength = getWidestItemWidth();
    }
  }

  /** Returns the string formatter for combo box elements. */
  public Function<E, String> getFormatter() {
    return formatter != null ? formatter : defaultFormatter;
  }

  /**
   * Sets a custom string formatter for combo box elements.
   *
   * @param formatter Function for converting combo box elements into strings. Specify {@code null} to use the
   * default formatter.
   */
  public void setFormatter(Function<E, String> formatter) {
    this.formatter = formatter;
  }

  @Override
  public void setPopupVisible(boolean v) {
    if (wide && v && !isPopupVisible()) {
      widestLength = getWidestItemWidth();
    }
    super.setPopupVisible(v);
  }

  @Override
  public Dimension getSize() {
    final Dimension dim = super.getSize();
    if (!layingOut && isWide()) {
      dim.width = Math.max(widestLength, dim.width);
    }
    return dim;
  }

  @Override
  public void doLayout() {
    try {
      layingOut = true;
      super.doLayout();
    } finally {
      layingOut = false;
    }
  }

  private int getWidestItemWidth() {
    final FontMetrics fm = getFontMetrics(getFont());
    final ComboBoxModel<E> model = getModel();
    int widest = 0;
    for (int i = 0, numItems = model.getSize(); i < numItems; i++) {
      final E item = model.getElementAt(i);
      final String text = getFormatter().apply(item);
      final int lineWidth = fm.stringWidth(text);
      widest = Math.min(Math.max(widest, lineWidth), maxWidth);
    }

    return widest + extraWidth;
  }

  /** Returns a {@code String} containing {@code count} instances of the character {@code letter}. */
  private static String createString(char letter, int count) {
    final char[] buf = new char[Math.max(0, count)];
    Arrays.fill(buf, letter);
    return new String(buf);
  }

  /** Calculates the width of {@code text}, in pixels, based on the font described by {@code fm}. */
  private static int getStringWidth(FontMetrics fm, String text) {
    int retVal = 0;
    if (fm != null && text != null) {
      retVal = fm.stringWidth(text);
    }
    return retVal;
  }

  /** Returns {@code value} if non-{@code null}. Returns {@code defValue} otherwise. */
  private static <T> T getValue(T value, T defValue) {
    return (value != null) ? value : defValue;
  }
}