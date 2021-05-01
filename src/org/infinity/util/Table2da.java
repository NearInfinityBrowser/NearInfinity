// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

/**
 * Stores content of a 2DA resource as table.
 */
public class Table2da
{
  /** Column index pointing to column labels. */
  public static final int COLUMN_HEADER = 0;
  /** Row index pointing to row labels. */
  public static final int ROW_HEADER    = 0;

  private final List<String> header = new ArrayList<>();
  private final List<List<String>> table = new ArrayList<>();
  private final ResourceEntry entry;
  private String defaultValue;

  public Table2da(String resource)
  {
    this(ResourceFactory.getResourceEntry(resource));
  }

  public Table2da(ResourceEntry entry)
  {
    this.entry= entry;
    init(entry);
  }

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

  /** Removes old content and reloads data from 2DA file. */
  public void reload()
  {
    init(entry);
  }

  /** Returns number of columns, including header column. */
  public int getColCount()
  {
    return table.isEmpty() ? header.size() : table.get(0).size();
  }

  /** Returns number of rows, including header row. */
  public int getRowCount()
  {
    return table.size();
  }

  /**
   * Returns element at specified location.
   * Returns {@link #getDefaultValue()} if arguments are out of range.
   */
  public String get(int row, int col)
  {
    if (row >= 0 && row < getRowCount()) {
      if (col >= 0 && col < getColCount()) {
        return table.get(row).get(col);
      }
    }
    return defaultValue;
  }

  /**
   * Returns header label of specified column.
   * <b>Note:</b> Column 0 always contains empty label.
   * Returns {@code null} on error.
   */
  public String getHeader(int col)
  {
    return (col >= 0 && col < header.size()) ? header.get(col) : null;
  }

  /** Returns whether table contains any data. */
  public boolean isEmpty()
  {
    return table.isEmpty() && header.isEmpty();
  }

  /** Returns the default value of the table. */
  public String getDefaultValue()
  {
    return (defaultValue != null && !defaultValue.isEmpty()) ? defaultValue : "0";
  }

  private void init(ResourceEntry entry)
  {
    table.clear();

    if (entry == null) {
      return;
    }

    try {
      PlainTextResource text = new PlainTextResource(entry);
      String[] lines = text.getText().split("\r?\n");
      if (lines.length >= 2) {
        int minSize = 0;

        // checking signature
        String[] sig = lines[0].trim().split("\\s+");
        if (sig.length > 1) {
          if (!sig[0].equalsIgnoreCase("2DA")) {
            throw new Exception("Invalid signature: " + sig[0]);
          }
          if (!sig[1].equalsIgnoreCase("V1.0")) {
            throw new Exception("Invalid version: " + sig[1]);
          }
        } else {
          return;
        }

        // storing default value
        defaultValue = lines[1].trim();

        // setting table header
        if (lines.length > 2) {
          String[] elements = lines[2].split("\\s+");
          header.add(""); // first column does not contain label
          for (final String s: elements) {
            if (!s.isEmpty()) {
              header.add(s);
            }
          }
        }

        // adding actual table entries
        for (int idx = 3; idx < lines.length; idx++) {
          String curLine = lines[idx].trim();
          String[] elements = curLine.split("\\s+");
          if (elements.length > 0 && !elements[0].isEmpty()) {
            List<String> listLine = new ArrayList<>();
            for (final String s: elements) {
              if (!s.isEmpty()) {
                listLine.add(s);
              }
            }
            table.add(listLine);
            minSize = Math.max(minSize, listLine.size());
          }
        }

        // normalizing row lengths
        for (int idx = 0, size = table.size(); idx < size; idx++) {
          List<String> curList = table.get(idx);
          while (curList.size() < minSize) {
            curList.add(defaultValue);
          }
        }
        while (header.size() < minSize) {
          header.add("");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
