// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JComponent;

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
 * <li>Property type: {@code int}</li>
 * <li>Value meaning: value of the enumeration</li>
 * </ul>
 */
public class Bitmap extends AbstractBitmap<String>
{
  private final BiFunction<Long, String, String> formatterBitmap = (value, item) -> {
    String number;
    if (isShowAsHex()) {
      number = getHexValue(value.longValue());
    } else {
      number = value.toString();
    }
    if (item != null) {
      return item + " (" + number + ")";
    } else {
      return "Unknown (" + number + ")";
    }
  };

  public Bitmap(ByteBuffer buffer, int offset, int length, String name, String[] table)
  {
    this(buffer, offset, length, name, table, true, false);
  }

  public Bitmap(ByteBuffer buffer, int offset, int length, String name, String[] table, boolean signed)
  {
    this(buffer, offset, length, name, table, true, false);
  }

  public Bitmap(ByteBuffer buffer, int offset, int length, String name, String[] table, boolean signed, boolean showAsHex)
  {
    super(buffer, offset, length, name, createMap(table), null, signed);
    setShowAsHex(showAsHex);
    setFormatter(formatterBitmap);
  }

  //--------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    if (getDataOf(getLongValue()) == null) {
      putItem(getLongValue(), "Unknown");
    }

    return super.edit(container);
  }

  //--------------------- End Interface Editable ---------------------

  private static TreeMap<Long, String> createMap(String[] symbols)
  {
    TreeMap<Long, String> retVal = new TreeMap<>();
    for (int i = 0; i < symbols.length; i++) {
      String symbol = (symbols[i] != null) ? symbols[i] : "(Unlabeled)";
      retVal.put(Long.valueOf(i), symbol);
    }

    return retVal;
  }
}
