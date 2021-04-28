// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.beans.PropertyChangeEvent;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.infinity.resource.AbstractStruct;

/**
 * Field that represents an integer enumeration of some values.
 *
 * <h2>Bean property</h2>
 * When this field is child of {@link AbstractStruct}, then changes of its internal
 * value reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent}
 * struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@code long}</li>
 * <li>Value meaning: numerical value of this field</li>
 * </ul>
 */
public class HashBitmap extends AbstractBitmap<String>
{
  private final BiFunction<Long, String, String> formatterHashBitmap = (value, item) -> {
    String number;
    if (isShowAsHex()) {
      number = getHexValue(value.longValue());
    } else {
      number = value.toString();
    }
    if (item != null) {
      return item.toString() + " - " + number;
    } else {
      return "Unknown - " + number;
    }
  };

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, String> idsmap)
  {
    this(buffer, offset, length, name, idsmap, true, false, false);
  }

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, String> idsmap,
                    boolean sortByName)
  {
    this(buffer, offset, length, name, idsmap, sortByName, false, false);
  }

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, String> idsmap,
                    boolean sortByName, boolean signed)
  {
    this(buffer, offset, length, name, idsmap, sortByName, signed, false);
  }

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, String> idsmap,
                    boolean sortByName, boolean signed, boolean showAsHex)
  {
    super(buffer, offset, length, name, idsmap, null, signed);
    setSortByName(sortByName);
    setShowAsHex(showAsHex);
    setFormatter(formatterHashBitmap);
  }
}
