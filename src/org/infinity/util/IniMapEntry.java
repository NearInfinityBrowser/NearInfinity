// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.infinity.datatype.StringRef;

public class IniMapEntry
{
  /** Regular expression that can be used to split position values in {@link #splitValues(String, String)}. */
  public static final String REGEX_POSITION = "\\[[-0-9]+\\.[-0-9]+(:[0-9]+)?\\]";
  /** Regular expression that can be used to split object values in {@link #splitValues(String, String)}. */
  public static final String REGEX_OBJECT = "\\[(-?\\d+\\.?)+\\]";

  private final String key;
  private final String value;
  /** Line number of ini entry. */
  private final int line;

  public IniMapEntry(String key, String value, int line)
  {
    this.key = (key != null) ? key : "";
    this.value = (value != null) ? value : "";
    this.line = line;
  }

  public boolean hasKey() { return key != null; }
  public String getKey() { return key; }

  public boolean hasValue() { return value != null; }
  public String getValue() { return value; }

  public Integer getIntValue() { return value == null ? null : Integer.valueOf(value); }
  public int getIntValue(int defValue) { return value == null ? defValue : Integer.valueOf(value); }

  public Double getDoubleValue() { return value == null ? null : Double.valueOf(value); }
  public double getDoubleValue(double defValue) { return value == null ? defValue : Double.valueOf(value); }

  public StringRef getStringRefValue() {
    return value == null ? null : new StringRef(key, Integer.valueOf(value));
  }

  public int getLine() { return line; }

  @Override
  public String toString()
  {
    return key + " = " + value;
  }

  /**
   * Helper routine: Splits values and returns them as array of individual values.
   * Using default separator "{@code ,}" (comma).
   */
  public static String[] splitValues(String value)
  {
    return splitValues(value, ',');
  }

  /** Helper routine: Splits values and returns them as array of individual values. */
  public static String[] splitValues(String value, char separator)
  {
    String[] retVal = null;
    if (value != null) {
      retVal = value.split(Character.toString(separator));
    } else {
      retVal = new String[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Splits values matching the specified regular expression pattern
   * and returns them as array of individual values.
   */
  public static String[] splitValues(String value, String pattern)
  {
    String[] retVal = null;
    if (value != null) {
      List<String> results = new ArrayList<>();
      try {
        Matcher matcher = Pattern.compile(pattern).matcher(value);
        while (matcher.find()) {
          results.add(value.substring(matcher.start(), matcher.end()));
        }
      } catch (PatternSyntaxException e) {
        e.printStackTrace();
      }
      retVal = new String[results.size()];
      for (int i = 0; i < results.size(); i++) {
        retVal[i] = results.get(i);
      }
    } else {
      retVal = new String[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Extracts the object identifiers from a string of the format
   * "{@code [ea.general.race.class.specific.gender.align]}" where identifiers after
   * "{@code ea}" are optional.
   */
  public static int[] splitObjectValue(String value)
  {
    int[] retVal = null;
    if (value != null && Pattern.matches("^\\[(-?\\d+\\.?)+\\]$", value)) {
      List<String> results = new ArrayList<>();
      Pattern p = Pattern.compile("-?\\d+");
      Matcher m = p.matcher(value);
      while (m.find()) {
        results.add(value.substring(m.start(), m.end()));
      }
      retVal = new int[results.size()];
      for (int i = 0; i < results.size(); i++) {
        try {
          retVal[i] = Integer.parseInt(results.get(i));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (retVal == null) {
      retVal = new int[0];
    }
    return retVal;
  }

  /**
   * Helper routine: Extracts the coordinates of an Infinity Engine position value in the format
   * "{@code [x.y:dir]}".
   */
  public static int[] splitPositionValue(String value)
  {
    int[] retVal = null;
    if (value != null && Pattern.matches("^\\[[-0-9]+\\.[-0-9]+(:[0-9]+)?\\]$", value)) {
      List<String> results = new ArrayList<>();
      Pattern p = Pattern.compile("-?\\d+");
      Matcher m = p.matcher(value);
      while (m.find()) {
        results.add(value.substring(m.start(), m.end()));
      }
      retVal = new int[results.size()];
      for (int i = 0; i < results.size(); i++) {
        try {
          retVal[i] = Integer.parseInt(results.get(i));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (retVal == null) {
      retVal = new int[0];
    }
    return retVal;
  }
}
