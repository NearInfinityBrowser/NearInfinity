// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class IdsBitmap extends Datatype implements Editable
{
  private final LongIntegerHashMap<IdsMapEntry> idsmap;
  private TextListPanel list;
  private long value;

  public IdsBitmap(byte buffer[], int offset, int length, String name, String resource)
  {
    super(offset, length, name);
    idsmap = IdsMapCache.get(resource).getMap();

    if (length == 4)
      value = Byteconvert.convertUnsignedInt(buffer, offset);
    else if (length == 2)
      value = (long)Byteconvert.convertUnsignedShort(buffer, offset);
    else if (length == 1)
      value = (long)Byteconvert.convertUnsignedByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

  public IdsBitmap(byte buffer[], int offset, int length, String name, String resource, int idsStart)
  {
    super(offset, length, name);
    LongIntegerHashMap orgmap = IdsMapCache.get(resource).getMap();
    idsmap = new LongIntegerHashMap<IdsMapEntry>();

    long keys[] = orgmap.keys();
    for (final long id : keys) {
      if (id >= idsStart) {
        IdsMapEntry entry = (IdsMapEntry)orgmap.get(id);
        long newid = id - (long)idsStart;
        idsmap.put(newid, new IdsMapEntry(newid, entry.getString(), entry.getParameters()));
      }
    }

    if (length == 4)
      value = Byteconvert.convertUnsignedInt(buffer, offset);
    else if (length == 2)
      value = (long)Byteconvert.convertUnsignedShort(buffer, offset);
    else if (length == 1)
      value = (long)Byteconvert.convertUnsignedByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    if (list == null) {
      long keys[] = idsmap.keys();
      List<IdsMapEntry> items = new ArrayList<IdsMapEntry>(keys.length);
      for (final long key : keys) 
        items.add(idsmap.get(key));
      list = new TextListPanel(items);
      list.addMouseListener(new MouseAdapter()
      {
        public void mouseClicked(MouseEvent event)
        {
          if (event.getClickCount() == 2)
            container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      });
    }
    Object selected = idsmap.get(value);
    if (selected != null)
      list.setSelectedValue(selected, true);

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
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

  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public boolean updateValue(AbstractStruct struct)
  {
    IdsMapEntry selected = (IdsMapEntry)list.getSelectedValue();
    value = selected.getID();
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    Object o = idsmap.get(value);
    if (o == null)
      return "Unknown - " + value;
    return idsmap.get(value).toString(); // + "(" + value + ")";
  }

  public long getValue()
  {
    return value;
  }
}

