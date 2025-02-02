// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
public class Table2da {
  /** Column index pointing to column labels. */
  public static final int COLUMN_HEADER = 0;

  /** Row index pointing to row labels. */
  public static final int ROW_HEADER = 0;

  private final List<Entry> header = new ArrayList<>();
  private final List<List<Entry>> table = new ArrayList<>();
  private final ResourceEntry entry;

  private final boolean strict;

  private int columnCount;
  private Entry defaultValue;

  public Table2da(String resource) {
    this(ResourceFactory.getResourceEntry(resource));
  }

  public Table2da(ResourceEntry entry) {
    this(entry, true);
  }

  public Table2da(ResourceEntry entry, boolean strict) {
    this.entry = entry;
    this.strict = strict;
    init(entry);
  }

  public ResourceEntry getResourceEntry() {
    return entry;
  }

  /** Removes old content and reloads data from 2DA file. */
  public void reload() {
    init(entry);
  }

  /** Returns total number of data columns. */
  public int getColCount() {
    return table.isEmpty() ? header.size() : columnCount;
  }

  /** Returns the number of columns for the specified table row. */
  public int getColCount(int row) {
    if (row >= 0 && row < table.size()) {
      return table.get(row).size();
    }
    return 0;
  }

  /** Returns number of data rows. */
  public int getRowCount() {
    return table.size();
  }

  /**
   * Returns element at specified location. Returns {@link #getDefaultValue()} if arguments are out of range.
   *
   * @param row Row of table content. First row starts at 0.
   * @param col Column of table content. First column starts at 0.
   * @return Table value as {@code String}.
   */
  public String get(int row, int col) {
    return getEntry(row, col).getValue();
  }

  /**
   * Returns the table content entry at the specified location.
   * Returns {@link #getDefaultValue()} if arguments are out of range.
   *
   * @param row Row of table content. First row starts at 0.
   * @param col Column of table content. First column starts at 0.
   * @return Table value as {@link Entry} object.
   */
  public Entry getEntry(int row, int col) {
    if (row >= 0 && row < getRowCount()) {
      if (col >= 0 && col < table.get(row).size()) {
        return table.get(row).get(col);
      }
    }
    return defaultValue;
  }

  /**
   * Returns header label of specified column. <b>Note:</b> Column 0 always contains empty label. Returns {@code null}
   * on error.
   */
  public String getHeader(int col) {
    final Entry entry = getHeaderEntry(col);
    return (entry != null) ? entry.getValue() : null;
  }

  /**
   * Returns header entry of specified column.
   *
   * Column 0 always contains an empty label. Returns {@code null} if the specified column contains no data.
   */
  public Entry getHeaderEntry(int col) {
    return (col >= 0 && col < header.size()) ? header.get(col) : null;
  }

  /** Returns whether table contains any data. */
  public boolean isEmpty() {
    return table.isEmpty() && header.isEmpty();
  }

  /** Returns the default value of the table. */
  public String getDefaultValue() {
    return (defaultValue != null && !defaultValue.getValue().isEmpty()) ? defaultValue.getValue() : "0";
  }

  /** Returns the default entry of the table. */
  public Entry getDefaultEntry() {
    return defaultValue;
  }

  /**
   * Returns the whole 2DA table definition as a single text string.
   *
   * @return 2DA table as {@code String}.
   * @see PlainTextResource#alignTableColumnsCompact(String)
   * @see PlainTextResource#alignTableColumnsUniform(String)
   */
  public String assemble() {
    final String nl = "\r\n";
    final String space = Misc.generate(4, ' ');
    final StringBuilder sb = new StringBuilder(256);

    // signature
    sb.append("2DA V1.0").append(nl);

    // default value
    sb.append(defaultValue.getValue()).append(nl);

    // table header
    for (final Entry entry : header) {
      sb.append(space).append(entry.getValue());
    }
    sb.append(nl);

    // table data
    for (final List<Entry> rowList : table) {
      for (int col = 0, colCount = rowList.size(); col < colCount; col++) {
        if (col > 0) {
          sb.append(space);
        }
        sb.append(rowList.get(col).getValue());
      }
      sb.append(nl);
    }
    sb.append(nl);

    return sb.toString();
  }

  @Override
  public String toString() {
    return "2DA V1.0 [columnCount=" + columnCount + ", rowCount=" + getRowCount() + ", defaultValue="
        + defaultValue.getValue() + "]";
  }

  private void init(ResourceEntry entry) {
    table.clear();

    if (entry == null) {
      return;
    }

    try {
      PlainTextResource text = new PlainTextResource(entry);
      String[] lines = text.getText().split("\r?\n");
      if (lines.length >= 2) {
        int minSize = 0;

        if (strict) {
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
        }

        // storing default value
        List<Entry> entries = getSplitEntries(lines[1], 1);
        defaultValue = entries.isEmpty() ? new Entry(this, lines[1].trim(), 1, 0) : entries.get(0);

        // setting table header
        if (lines.length > 2) {
          entries = getSplitEntries(lines[2], 2);
          header.add(new Entry(this, "", 2, 0));  // first column does not contain label
          header.addAll(entries);
        }

        // adding actual table entries
        for (int idx = 3; idx < lines.length; idx++) {
          entries = getSplitEntries(lines[idx], idx);
          if (!entries.isEmpty()) {
            table.add(entries);
            minSize = Math.max(minSize, entries.size());
          }
        }

        columnCount = minSize;
      }

      if (defaultValue == null) {
        defaultValue = new Entry(this, "0", 1, 0);
      }
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  /**
   * Extracts all table entries from the specified string.
   * @param line String of a single table row.
   * @param lineIndex Row number of the line.
   * @return List of all extracted table entries.
   */
  private List<Entry> getSplitEntries(String line, int lineIndex) {
    List<Entry> retVal = new ArrayList<>();

    if (line != null) {
      String[] tokens = line.trim().split("\\s+");
      int fromIndex = 0;
      for (final String token : tokens) {
        fromIndex = line.indexOf(token, fromIndex);
        if (fromIndex >= 0) {
          retVal.add(new Entry(this, token, lineIndex, fromIndex));
          fromIndex += token.length();
        } else {
          break;
        }
      }
    }

    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Encapsulates a single 2DA table value. */
  public static class Entry {
    private final Table2da parent;
    private final String value;
    private final int line;
    private final int pos;

    private Entry(Table2da parent, String value, int line, int pos) {
      this.parent = parent;
      this.value = value != null ? value : "";
      this.line = line;
      this.pos = pos;
    }

    /** Returns the associated {@link Table2da} instance. */
    public Table2da getParent() {
      return parent;
    }

    /** Returns the value of the table entry. */
    public String getValue() {
      return value;
    }

    /** Returns the 0-based line number where the table value can be found.*/
    public int getLine() {
      return line;
    }

    /** Returns the 0-based position of the table value in the line. */
    public int getPosition() {
      return pos;
    }
  }
}
