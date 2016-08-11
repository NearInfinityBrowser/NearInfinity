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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.util.LongIntegerHashMap;
import org.infinity.util.ObjectString;

public class HashBitmap extends Datatype implements Editable, IsNumeric
{
  private final LongIntegerHashMap<? extends Object> idsmap;
  private final List<JButton> buttonList;
  private final JButton bUpdate;
  private final boolean sortByName;
  private TextListPanel list;
  private long value;

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap)
  {
    this(null, buffer, offset, length, name, idsmap, true);
  }

  public HashBitmap(ByteBuffer buffer, int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap, boolean sortByName)
  {
    this(null, buffer, offset, length, name, idsmap, sortByName);
  }

  public HashBitmap(StructEntry parent, ByteBuffer buffer, int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap)
  {
    this(parent, buffer, offset, length, name, idsmap, true);
  }

  public HashBitmap(StructEntry parent, ByteBuffer buffer, int offset, int length, String name,
                    LongIntegerHashMap<? extends Object> idsmap, boolean sortByName)
  {
    super(parent, offset, length, name);
    this.idsmap = normalizeHashMap(idsmap);
    this.sortByName = sortByName;
    this.bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    this.buttonList = new ArrayList<>();
    this.buttonList.add(bUpdate);

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
      list.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
          if (!e.getValueIsAdjusting()) {
            listItemChanged();
          }
        }
      });
    }
    Object selected = idsmap.get(value);
    if (selected != null) {
      list.setSelectedValue(selected, true);
    }

    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridheight = buttonList.size() + 2;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.weightx = 0.0;
    gbc.gridheight = 1;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.insets.left = 6;
    gbc.insets.top = 4;
    gbc.insets.bottom = 4;
    // dummy component to center list of buttons vertically
    JPanel p = new JPanel();
    gbl.setConstraints(p, gbc);
    panel.add(p);
    ++gbc.gridy;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    for (final JButton btn: buttonList) {
      gbl.setConstraints(btn, gbc);
      panel.add(btn);
      ++gbc.gridy;
    }
    // dummy component to center list of buttons vertically
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    p = new JPanel();
    gbl.setConstraints(p, gbc);
    panel.add(p);

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
    Long number = getValueOfItem(list.getSelectedValue());
    if (number != null) {
      value = number.longValue();
    } else {
      return false;
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
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    switch (getSize()) {
      case 1:
        value = buffer.get() & 0xff;
        break;
      case 2:
        value = buffer.getShort() & 0xffff;
        break;
      case 4:
        value = buffer.getInt() & 0xffffffff;
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

  /** Called whenever the user selects a new list item. */
  protected void listItemChanged()
  {
  }

  /**
   * Can be used to register one or more custom buttons to the bitmap control.
   * Only effective if called before the UI control is created.
   */
  protected void addButtons(JButton... buttons)
  {
    if (list == null) {
      for (final JButton button: buttons) {
        if (button != null) {
          buttonList.add(button);
        }
      }
    }
  }

  /** Returns the number of registered buttons. */
  public int getButtonCount()
  {
    return buttonList.size();
  }

  /**
   * Returns the button control at the specified index.
   * First entry is always the "Update value" button.
   */
  public JButton getButton(int index)
  {
    return buttonList.get(index);
  }

  /** Returns the TextListPanel control used by this datatype. */
  public TextListPanel getListPanel()
  {
    return list;
  }

  /** Returns the number if IDS entries. */
  public int getListSize()
  {
    return idsmap.size();
  }

  /** Returns an array of numeric IDS values */
  public long[] getKeys()
  {
    return idsmap.keys();
  }

  /** Returns the textual representation of the specified IDS value. */
  public Object getValueOf(long key)
  {
    return idsmap.get(Long.valueOf(key));
  }

  /** Returns the symbol associated with the specified IDS value. */
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

  /** Attempts to extract the IDS value from the specified list item. */
  protected Long getValueOfItem(Object item)
  {
    Long retVal = null;
    if (item != null) {
      if (item instanceof ObjectString && ((ObjectString)item).getObject() instanceof Number) {
        retVal = ((Number)((ObjectString)item).getObject()).longValue();
      } else {
        int i = item.toString().lastIndexOf(" - ");
        try {
          retVal = Long.parseLong(item.toString().substring(i + 3));
        } catch (NumberFormatException e) {
          retVal = null;
        }
      }
    }
    return retVal;
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
    } else if (map == null) {
      return new LongIntegerHashMap<ObjectString>();
    } else {
      return map;
    }
  }
}

