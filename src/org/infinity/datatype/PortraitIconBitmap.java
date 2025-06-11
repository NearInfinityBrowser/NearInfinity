// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.infinity.datatype.PortraitIconBitmap.IndexString;
import org.infinity.gui.TextListPanel;
import org.infinity.resource.effects.BaseOpcode;

/**
 * Specialized field that represents an integer enumeration of portrait icon definitions.
 */
public class PortraitIconBitmap extends AbstractBitmap<IndexString> {
  /** Creates a field with a populated list of available portrait icon entries. */
  public PortraitIconBitmap(ByteBuffer buffer, int offset, int length, String name) {
    this(buffer, offset, length, name, null);
  }

  /**
   * Creates a field with a populated list of available portrait icon entries. The item at index 0 is labeled as
   * defined by the {@code defaultName} parameter.
   */
  public PortraitIconBitmap(ByteBuffer buffer, int offset, int length, String name, String defaultName) {
    super(buffer, offset, length, name, createMap(BaseOpcode.getPortraitIconNames(defaultName)), null, true);
    setIconType(TextListPanel.IconType.PORTRAIT);
    setShowAsHex(false);
    setFormatter(formatterBitmap);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    if (getDataOf(getLongValue()) == null) {
      putItem(getLongValue(), new IndexString(TEXT_UNKNOWN, -1));
    }

    return super.edit(container);
  }

  private static TreeMap<Long, IndexString> createMap(String[] symbols) {
    final TreeMap<Long, IndexString> retVal = new TreeMap<>();

    if (symbols != null) {
      for (int i = 0; i < symbols.length; i++) {
        final String symbol = (symbols[i] != null) ? symbols[i] : TEXT_UNLABELED;
        retVal.put((long)i, new IndexString(symbol, i));
      }
    }

    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  /** This class associates a numeric index with a string value. */
  public static class IndexString implements Comparable<IndexString> {
    private final int index;
    private final String text;

    private boolean sortByIndex;

    public IndexString(String text, int index) {
      this(text, index, false);
    }

    public IndexString(String text, int index, boolean sortByIndex) {
      this.text = text;
      this.index = index;
      this.sortByIndex = sortByIndex;
    }

    public int getIndex() {
      return index;
    }

    public String getText() {
      return text;
    }

    public boolean isSortByIndex() {
      return sortByIndex;
    }

    @Override
    public int compareTo(IndexString o) {
      if (sortByIndex) {
        return (o != null) ? index - o.index : index;
      } else {
        final String text2 = (o != null) ? o.text : null;
        if (text != null) {
          return (text2 != null) ? text.compareTo(o.text) : 1;
        } else {
          return (text2 != null) ? -1 : 0;
        }
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, text);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      IndexString other = (IndexString)obj;
      return index == other.index && Objects.equals(text, other.text);
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
