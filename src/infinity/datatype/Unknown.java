// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.util.ArrayUtil;
import infinity.util.Filewriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;

public class Unknown extends Datatype implements Editable
{
  private static final String UNKNOWN = "Unknown";
  JTextArea textArea;
  byte[] data;

  public Unknown(byte[] buffer, int offset, int length)
  {
    super(offset, length, UNKNOWN);
    data = ArrayUtil.getSubArray(buffer, offset, length);
  }

  public Unknown(byte[] buffer, int offset, int length, String name)
  {
    super(offset, length, name);
    data = ArrayUtil.getSubArray(buffer, offset, length);
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    JButton bUpdate;
    if (textArea == null) {
      textArea = new JTextArea(15, 5);
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    }
    String s = toString();
    textArea.setText(s.substring(0, s.length() - 2));

    bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    JScrollPane scroll = new JScrollPane(textArea);

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

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  public void select()
  {
  }

  public boolean updateValue(AbstractStruct struct)
  {
    String value = textArea.getText().trim();
    value = value.replace('\n', ' ');
    value = value.replace('\r', ' ');
    int index = value.indexOf((int)' ');
    while (index != -1) {
      value = value.substring(0, index) + value.substring(index + 1);
      index = value.indexOf((int)' ');
    }
    if (value.length() != 2 * data.length)
      return false;
    byte newdata[] = new byte[data.length];
    for (int i = 0; i < newdata.length; i++) {
      String bytechars = value.substring(2 * i, 2 * i + 2);
      try {
        newdata[i] = (byte)Integer.parseInt(bytechars, 16);
      } catch (NumberFormatException e) {
        return false;
      }
    }
    data = newdata;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeBytes(os, data);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    StringBuffer sb = new StringBuffer(3 * data.length + 1);
    for (final byte d : data) {
      String text = Integer.toHexString((int)d);
      if (text.length() == 1)
        sb.append('0');
      else if (text.length() > 2)
        text = text.substring(text.length() - 2);
      sb.append(text).append(' ');
    }
    sb.append('h');
    return sb.toString();
  }
}

