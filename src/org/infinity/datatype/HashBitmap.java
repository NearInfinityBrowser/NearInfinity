// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.LongIntegerHashMap;
import org.infinity.util.ObjectString;

public class HashBitmap extends Datatype implements Editable, IsNumeric
{
  private final LongIntegerHashMap<? extends Object> idsmap;
  private final boolean sortByName;
  private TextListPanel list;
  private long value;

  public HashBitmap(byte buffer[], int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap)
  {
    this(null, buffer, offset, length, name, idsmap, true);
  }

  public HashBitmap(byte buffer[], int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap, boolean sortByName)
  {
    this(null, buffer, offset, length, name, idsmap, sortByName);
  }

  public HashBitmap(StructEntry parent, byte buffer[], int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap)
  {
    this(parent, buffer, offset, length, name, idsmap, true);
  }

  public HashBitmap(StructEntry parent, byte buffer[], int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap, boolean sortByName)
  {
    super(parent, offset, length, name);
    this.idsmap = normalizeHashMap(idsmap);
    this.sortByName = sortByName;

    read(buffer, offset);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    if (list == null) {
      long[] keys = idsmap.keys();
      List<Object> items = new ArrayList<Object>(keys.length);
      for (final long id : keys) {
        if (idsmap.containsKey(id)) {
          Object o = idsmap.get(id);
          if (o != null) {
            items.add(o);
          }
        }
      }
      list = new TextListPanel(items, sortByName);
      list.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mouseClicked(MouseEvent event)
        {
          if (event.getClickCount() == 2)
            container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      });
    }
    Object selected = idsmap.get(value);
    if (selected != null) {
      list.setSelectedValue(selected, true);
    }

    JButton bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets.left = 6;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_MEDIUM);
    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    // updating value
    Object selected = list.getSelectedValue();
    if (selected instanceof ObjectString && ((ObjectString)selected).getObject() instanceof Number) {
      value = ((Number)((ObjectString)selected).getObject()).longValue();
    } else {
      int i = selected.toString().lastIndexOf(" - ");
      try {
        value = Long.parseLong(selected.toString().substring(i + 3));
      } catch (NumberFormatException e) {
        return false;
      }
    }

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        value = (long)DynamicArray.getUnsignedByte(buffer, offset);
        break;
      case 2:
        value = (long)DynamicArray.getUnsignedShort(buffer, offset);
        break;
      case 4:
        value = DynamicArray.getUnsignedInt(buffer, offset);
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    Object o = idsmap.get(value);
    if (o == null) {
      return "Unknown - " + value;
    } else {
      return o.toString();
    }
  }

//--------------------- Begin Interface IsNumeric ---------------------

  @Override
  public long getLongValue()
  {
    return value;
  }

  @Override
  public int getValue()
  {
    return (int)value;
  }

//--------------------- End Interface IsNumeric ---------------------

  protected void setValue(long newValue)
  {
    this.value = newValue;
  }

  public int getListSize()
  {
    return idsmap.size();
  }

  public long[] getKeys()
  {
    return idsmap.keys();
  }

  public Object getValueOf(long key)
  {
    return idsmap.get(Long.valueOf(key));
  }

  public String getSymbol(long index)
  {
    Object o = idsmap.get(index);
    if (o != null) {
      if (o instanceof ObjectString) {
        return ((ObjectString)o).getString();
      } else {
        int i = o.toString().lastIndexOf(" - ");
        if (i >= 0) {
          return o.toString().substring(0, i);
        }
      }
    }
    return null;
  }

  protected LongIntegerHashMap<? extends Object> getHashBitmap()
  {
    return idsmap;
  }

  private static LongIntegerHashMap<? extends Object> normalizeHashMap(LongIntegerHashMap<? extends Object> map)
  {
    if (map != null && !map.isEmpty() && map.get(map.keys()[0]) instanceof String) {
      LongIntegerHashMap<ObjectString> retVal = new LongIntegerHashMap<ObjectString>();
      long[] keys = map.keys();
      for (final long key: keys) {
        retVal.put(Long.valueOf(key), new ObjectString(map.get(key).toString(), Long.valueOf(key),
                                                       ObjectString.FMT_OBJECT_HYPHEN));
      }
      return retVal;
    } else {
      return map;
    }
  }
}

