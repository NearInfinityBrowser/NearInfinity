// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFormattedTextField.AbstractFormatter;

import org.infinity.util.DataString;
import org.infinity.util.Logger;

/**
 * A simplified NumberFormatter that handles conversion from Object to String and back. Allowed text input is limited to
 * decimal and hexadecimal numbers within specified bounds.
 */
public class NumberFormatterEx extends AbstractFormatter {
  public enum NumberFormat {
    DECIMAL, HEXADECIMAL
  }

  // Regular Expression patterns for validating decimal numbers
  private static final Pattern[] PATTERN_DEC = { Pattern.compile("^-?[0-9]+$", Pattern.CASE_INSENSITIVE) };

  // Regular Expression patterns for validating hexadecimal numbers
  private static final Pattern[] PATTERN_HEX = { Pattern.compile("^-?(0x)[0-9a-f]+$", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^-?[0-9a-f]+(h)$", Pattern.CASE_INSENSITIVE) };

  // Default representation of dec/hex strings
  private static final String FMT_DEC = "%d";
  private static final String FMT_HEX = "%x h";

  private final long valueMin;
  private final long valueMax;
  private final long defaultValue;

  private NumberFormat numberFormat;
  private long currentValue;

  /**
   * Creates a NumberFormatterEx instance with the following default values: decimal representation, bound to lowest and
   * highest long value, default value 0.
   */
  public NumberFormatterEx() {
    this(NumberFormat.DECIMAL, Long.MIN_VALUE, Long.MAX_VALUE, 0L);
  }

  /**
   * Creates a NumberFormatterEx instance with the specified number format and the following default values: bound to
   * lowest and highest long value, default value 0.
   *
   * @param format Number format for visual representation.
   */
  public NumberFormatterEx(NumberFormat format) {
    this(format, Long.MIN_VALUE, Long.MAX_VALUE, 0L);
  }

  /**
   * Creates a NumberFormatterEx instance with the specified parameters.
   *
   * @param format       Number format for visual representation.
   * @param minValue     Lowest valid numeric value.
   * @param maxValue     Highest valid numeric value.
   * @param defaultValue Default value to display if no valid input is available.
   */
  public NumberFormatterEx(NumberFormat format, long minValue, long maxValue, long defaultValue) {
    this.numberFormat = (format != null) ? format : NumberFormat.DECIMAL;
    this.valueMin = Math.min(minValue, maxValue);
    this.valueMax = Math.max(maxValue, minValue);
    this.defaultValue = getBoundedValue(defaultValue);
    this.currentValue = this.defaultValue;
  }

  @Override
  public Object stringToValue(String text) throws ParseException {
    currentValue = convertToNumber(text);
    switch (numberFormat) {
      case HEXADECIMAL:
        return DataString.with(String.format(FMT_HEX, currentValue), currentValue, "");
      default:
        return DataString.with(String.format(FMT_DEC, currentValue), currentValue, "");
    }
  }

  @Override
  public String valueToString(Object value) throws ParseException {
    long retVal = currentValue;
    if (value != null) {
      if (value instanceof DataString<?> && ((DataString<?>) value).getData() instanceof Number) {
        DataString<?> os = (DataString<?>) value;
        Number n = (Number) os.getData();
        retVal = getBoundedValue(n.longValue());
      } else {
        retVal = convertToNumber(value.toString());
      }
    }
    currentValue = retVal;

    switch (numberFormat) {
      case HEXADECIMAL:
        return String.format(FMT_HEX, currentValue);
      default:
        return String.format(FMT_DEC, currentValue);
    }
  }

  /** Returns the currently displayed numeric notation */
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  /** Forces the display to the specified numeric notation. */
  public void setNumberFormat(NumberFormat format) {
    if (format != null && format != numberFormat) {
      long v = getNumericValue();
      switch (format) {
        case DECIMAL:
          getFormattedTextField().setValue(Long.toString(v));
          break;
        case HEXADECIMAL:
          getFormattedTextField().setValue(Long.toString(v, 16) + "h");
          break;
      }
    }
  }

  /** Returns the lower bound of the allowed numeric range */
  public long getMinimumValue() {
    return valueMin;
  }

  /** Returns the upper bound of the allowed numeric range */
  public long getMaximumValue() {
    return valueMax;
  }

  /** Returns the default value defined in the formatter. */
  public long getDefaultValue() {
    return defaultValue;
  }

  /** A helper function that returns the last number entered into the text field associated with this formatter. */
  public long getNumericValue() {
    try {
      return getNumericValue(getFormattedTextField().getText());
    } catch (Exception e) {
      Logger.trace(e);
    }
    return currentValue;
  }

  /**
   * A helper function that attempts to extract or convert the given Object into a numeric value. Throws
   * {@code ParseException} if the parameter can not be resolved into a number.
   */
  public long getNumericValue(Object value) throws ParseException {
    Number n = null;
    if (value instanceof DataString<?> && ((DataString<?>) value).getData() instanceof Number) {
      n = (Number) ((DataString<?>) value).getData();
    } else if (value instanceof Number) {
      n = (Number) value;
    } else {
      DataString<?> os = (DataString<?>) stringToValue(value.toString());
      n = (Number) os.getData();
    }

    return getBoundedValue(n.longValue());
  }

  /** Attempts to convert given string into a numeric value. */
  private long convertToNumber(String text) throws ParseException {
    text = text.replace(" ", "");
    int base = 0;

    // testing decimal number
    for (Pattern p : PATTERN_DEC) {
      if (base == 0) {
        Matcher m = p.matcher(text);
        if (m.find()) {
          base = 10;
          numberFormat = NumberFormat.DECIMAL;
        }
      }
    }

    // testing hexadecimal
    for (Pattern p : PATTERN_HEX) {
      if (base == 0) {
        Matcher m = p.matcher(text);
        if (m.find()) {
          base = 16;
          numberFormat = NumberFormat.HEXADECIMAL;
          // remove hexadecimal marker
          String s = "";
          if (m.start(1) > 0)
            s = text.substring(0, m.start(1));
          if (m.end(1) < text.length())
            s = s + text.substring(m.end(1));
          text = s;
        }
      }
    }

    if (base == 0)
      throw new ParseException("Invalid number format.", 0);

    try {
      return getBoundedValue(Long.parseLong(text, base));
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid number format.", 0);
    }
  }

  /** Ensures that given value is within current bounds. */
  private long getBoundedValue(long value) {
    return Math.min(Math.max(value, valueMin), valueMax);
  }
}
