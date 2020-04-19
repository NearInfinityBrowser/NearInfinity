// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.Misc;

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
public class Bitmap extends Datatype implements Editable, IsNumeric
{
  private final String[] table;

  private TextListPanel<String> list;
  private int base; // numeric value of first item index
  private int value;

  public Bitmap(ByteBuffer buffer, int offset, int length, String name, String[] table)
  {
    super(offset, length, name);
    this.base = 0;
    this.table = table;
    read(buffer, offset);
  }

  //<editor-fold defaultstate="collapsed" desc="Editable">
  @Override
  public JComponent edit(final ActionListener container)
  {
    base = 0;
    int capacity = table.length;
    if (value < 0) {
      capacity += Math.abs(value);
      base = value;
    } else if (value >= table.length) {
      capacity = value + 1;
    }
    final List<String> values = new ArrayList<>(capacity);
    for (int i = 0; i < capacity; i++) {
      values.add(toString(base + i));
    }
    list = new TextListPanel<>(values, false);
    list.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      }
    });
    if (value >= base && value < list.getModel().getSize() + base) {
      int index = 0;
      while (!list.getModel().getElementAt(index).equals(toString(value))) {
        index++;
      }
      list.setSelectedIndex(index);
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

    panel.setMinimumSize(Misc.getScaledDimension(DIM_MEDIUM));
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
    setValue(calcValue());

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Writeable">
  @Override
  public void write(OutputStream os) throws IOException
  {
    writeInt(os, value);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Readable">
  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    switch (getSize()) {
      case 1:
        value = buffer.get();
        break;
      case 2:
        value = buffer.getShort();
        break;
      case 4:
        value = buffer.getInt();
        break;
      default:
        throw new IllegalArgumentException();
    }

    return offset + getSize();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="IsNumeric">
  @Override
  public long getLongValue()
  {
    return (long)value & 0xffffffffL;
  }

  @Override
  public int getValue()
  {
    return value;
  }
  //</editor-fold>

  @Override
  public String toString()
  {
    return toString(value);
  }

  /** Returns the unformatted description of the specified value. */
  protected String getString(int nr)
  {
    if (nr >= 0 && nr < table.length) {
      return table[nr];
    }
    return null;
  }

  /** Returns a formatted description of the specified value. */
  private String toString(int nr)
  {
    if (nr < 0 || nr >= table.length) {
      return "Unknown (" + nr + ')';
    } else if (table[nr] == null || table[nr].isEmpty()) {
      return "Unknown (" + nr + ')';
    } else {
      return table[nr] + " (" + nr + ')';
    }
  }

  private void setValue(int newValue)
  {
    final int oldValue = value;
    value = newValue;
    if (oldValue != newValue) {
      firePropertyChange(oldValue, newValue);
    }
  }

  private int calcValue()
  {
    final String svalue = list.getSelectedValue();
    int val = base;
    //FIXME: Ineffective code
    while (!svalue.equals(toString(val))) {
      val++;
    }
    return val;
  }
}
