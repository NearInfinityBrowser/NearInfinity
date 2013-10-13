// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.DynamicArray;
import infinity.util.LongIntegerHashMap;

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

public class HashBitmap extends Datatype implements Editable
{
  private final LongIntegerHashMap<String> idsmap;
  private TextListPanel list;
  private long value;

  public HashBitmap(byte buffer[], int offset, int length, String name, LongIntegerHashMap<String> idsmap)
  {
    super(offset, length, name);
    this.idsmap = new LongIntegerHashMap<String>(idsmap);

    if (length == 4)
      value = DynamicArray.getUnsignedInt(buffer, offset);
    else if (length == 2)
      value = (long)DynamicArray.getUnsignedShort(buffer, offset);
    else if (length == 1)
      value = (long)DynamicArray.getUnsignedByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    if (list == null) {
      long[] keys = idsmap.keys();
      List<String> items = new ArrayList<String>(keys.length);
      for (long id : keys) {
        if (idsmap.containsKey(id))
          items.add(idsmap.get(id).toString() + " - " + id);
      }
      list = new TextListPanel(items);
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
    String selected = idsmap.get(value);
    if (selected != null)
      list.setSelectedValue(selected.toString() + " - " + value, true);

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

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    String selected = list.getSelectedValue().toString();
    int i = selected.lastIndexOf(" - ");
    try {
      value = Long.parseLong(selected.substring(i + 3));
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public String toString()
  {
    Object o = idsmap.get(value);
    if (o == null)
      return "Unknown - " + value;
    return idsmap.get(value).toString() + " - " + value;
  }

  public long getValue()
  {
    return value;
  }
}

