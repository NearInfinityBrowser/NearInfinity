// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;

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

public class Bitmap extends Datatype implements Editable
{
  private final List<UpdateListener> listeners = new ArrayList<UpdateListener>();
  private final String[] table;

  private TextListPanel list;
  private int value;

  public Bitmap(byte[] buffer, int offset, int length, String name, String[] table)
  {
    this(null, buffer, offset, length, name, table);
  }

  public Bitmap(StructEntry parent, byte[] buffer, int offset, int length, String name, String[] table)
  {
    super(parent, offset, length, name);
    this.table = table;
    read(buffer, offset);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    if (list == null) {
      List<String> values = new ArrayList<String>(Math.max(table.length, value + 1));
      for (int i = 0; i < Math.max(table.length, value + 1); i++)
        values.add(getString(i));
      list = new TextListPanel(values, false);
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
    if (value >= 0 && value < list.getModel().getSize()) {
      int index = 0;
      while (!list.getModel().getElementAt(index).equals(getString(value)))
        index++;
      list.setSelectedIndex(index);
    }

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
    // updating value
    String svalue = (String)list.getSelectedValue();
    value = 0;
    while (!svalue.equals(getString(value))) {
      value++;
    }

    // notifying listeners
    if (!listeners.isEmpty()) {
      boolean ret = false;
      UpdateEvent event = new UpdateEvent(this, struct);
      for (final UpdateListener l: listeners) {
        ret |= l.valueUpdated(event);
      }
      if (ret) {
        struct.fireTableDataChanged();
      }
    }

    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeInt(os, value);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        value = (int)DynamicArray.getByte(buffer, offset);
        break;
      case 2:
        value = (int)DynamicArray.getShort(buffer, offset);
        break;
      case 4:
        value = DynamicArray.getInt(buffer, offset);
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
    return getString(value);
  }

  public int getValue()
  {
    return value;
  }

  /**
   * Adds the specified update listener to receive update events from this object.
   * If listener l is null, no exception is thrown and no action is performed.
   * @param l The update listener
   */
  public void addUpdateListener(UpdateListener l)
  {
    if (l != null)
      listeners.add(l);
  }

  /**
   * Returns an array of all update listeners registered on this object.
   * @return All of this object's update listener or an empty array if no listener is registered.
   */
  public UpdateListener[] getUpdateListeners()
  {
    UpdateListener[] ar = new UpdateListener[listeners.size()];
    for (int i = 0; i < listeners.size(); i++)
      ar[i] = listeners.get(i);
    return ar;
  }

  /**
   * Removes the specified update listener, so that it no longer receives update events
   * from this object.
   * @param l The update listener
   */
  public void removeUpdateListener(UpdateListener l)
  {
    if (l != null)
      listeners.remove(l);
  }

  private String getString(int nr)
  {
    if (nr >= table.length) {
      return "Unknown (" + nr + ')';
    } else if (nr < 0) {
      return "Error (" + nr + ')';
    } else if (table[nr] == null || table[nr].equals("")) {
      return "Unknown (" + nr + ')';
    } else {
      return new StringBuffer(table[nr]).append(" (").append(nr).append(')').toString();
    }
  }
}

