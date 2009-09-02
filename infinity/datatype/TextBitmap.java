// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.Byteconvert;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;

public final class TextBitmap extends Datatype implements Editable
{
  private final String[] ids;
  private final String[] names;
  private JTable table;
  private String text;

  public TextBitmap(byte buffer[], int offset, int length, String name, String ids[], String names[])
  {
    super(offset, length, name);
    text = Byteconvert.convertString(buffer, offset, length);
    this.ids = ids;
    this.names = names;
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    if (table == null) {
      table = new JTable(new BitmapTableModel());
      table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setDragEnabled(false);
      table.setTableHeader(null);
    }
    for (int i = 0; i < ids.length; i++)
      if (ids[i].equalsIgnoreCase(text))
        table.getSelectionModel().setSelectionInterval(i, i);

    JScrollPane scroll = new JScrollPane(table);
    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    panel.add(scroll);

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
    table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), 0, false));
  }

  public boolean updateValue(AbstractStruct struct)
  {
    int index = table.getSelectedRow();
    if (index == -1)
      return false;
    text = ids[index];
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeString(os, text, getSize());
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    for (int i = 0; i < ids.length; i++)
      if (ids[i].equalsIgnoreCase(text))
        return text + " - " + names[i];
    return text;
  }

// -------------------------- INNER CLASSES --------------------------

  private final class BitmapTableModel extends AbstractTableModel
  {
    private BitmapTableModel()
    {
    }

    public int getRowCount()
    {
      return ids.length;
    }

    public int getColumnCount()
    {
      return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      if (columnIndex == 0)
        return ids[rowIndex];
      return names[rowIndex];
    }
  }
}

