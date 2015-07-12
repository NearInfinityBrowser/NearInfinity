// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.resource.text.PlainTextResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores content of a 2DA resource as table.
 */
public class Table2da
{
  /** Column index pointing to column labels. */
  public static final int COLUMN_HEADER = 0;
  /** Row index pointing to row labels. */
  public static final int ROW_HEADER    = 0;

  private final List<List<String>> table = new ArrayList<List<String>>();
  private final ResourceEntry entry;

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
    return table.isEmpty() ? 0 : table.get(0).size();
  }

  /** Returns number of rows, including header row. */
  public int getRowCount()
  {
    return table.size();
  }

  /** Returns element at specified location. Returns <code>null</code> on error. */
  public String get(int row, int col)
  {
    if (row >= 0 && row < getRowCount()) {
      if (col >= 0 && col < getColCount()) {
        return table.get(row).get(col);
      }
    }
    return null;
  }

  /** Returns whether table contains any data. */
  public boolean isEmpty()
  {
    return table.isEmpty();
  }

  private void init(ResourceEntry entry)
  {
    table.clear();

    if (entry == null) {
      return;
    }

    try {
      PlainTextResource text = new PlainTextResource(entry);
      String[] lines = text.getText().split("\\r?\\n");
      if (lines.length >= 2) {
        int minSize = 0;

        // adding 2da entries (skipping first two lines)
        for (int idx = 2; idx < lines.length; idx++) {
          String curLine = lines[idx].trim();
          String[] elements = curLine.split("\\s+");
          if (elements.length > 0) {
            List<String> listLine = new ArrayList<String>();
            if (idx == 2) {
              // special: contains column labels
              listLine.add("");
            }
            for (final String s: elements) {
              listLine.add(s);
            }
            table.add(listLine);
            minSize = Math.max(minSize, listLine.size());
          }
        }

        // normalizing row lengths
        for (int idx = 0, size = table.size(); idx < size; idx++) {
          List<String> curList = table.get(idx);
          while (curList.size() < minSize) {
            curList.add("");
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
