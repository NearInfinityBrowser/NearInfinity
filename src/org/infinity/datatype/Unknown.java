// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.gui.StructViewer;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.FileWriterNI;

public class Unknown extends Datatype implements Editable, IsBinary
{
  protected InfinityTextArea textArea;
  protected byte[] data;

  public Unknown(byte[] buffer, int offset, int length)
  {
    this(null, buffer, offset, length, AbstractStruct.COMMON_UNKNOWN);
  }

  public Unknown(StructEntry parent, byte[] buffer, int offset, int length)
  {
    this(parent, buffer, offset, length, AbstractStruct.COMMON_UNKNOWN);
  }

  public Unknown(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public Unknown(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, offset, length, name);
    read(buffer, offset);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    if (data != null && data.length > 0) {
      JButton bUpdate;
      if (textArea == null) {
        textArea = new InfinityTextArea(15, 5, true);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEOLMarkersVisible(false);
        textArea.setMargin(new Insets(3, 3, 3, 3));
      }
      String s = toString();
      textArea.setText(s.substring(0, s.length() - 2));
      textArea.discardAllEdits();
      textArea.setCaretPosition(0);
      InfinityScrollPane scroll = new InfinityScrollPane(textArea, true);
      scroll.setLineNumbersEnabled(false);

      bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
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

      panel.setMinimumSize(DIM_BROAD);
      panel.setPreferredSize(DIM_BROAD);
      return panel;
    } else {
      JPanel panel = new JPanel();
      return panel;
    }
  }

  @Override
  public void select()
  {
  }

  @Override
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

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    FileWriterNI.writeBytes(os, data);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    data = Arrays.copyOfRange(buffer, offset, offset + getSize());

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

//--------------------- Begin Interface IsBinary ---------------------

  @Override
  public byte[] getData()
  {
    return Arrays.copyOf(data, data.length);
  }

//--------------------- End Interface IsBinary ---------------------

  @Override
  public String toString()
  {
    if (data != null && data.length > 0) {
      StringBuffer sb = new StringBuffer(3 * data.length + 1);
      for (final byte d : data) {
        String text = Integer.toHexString((int)d & 0xff);
        if (text.length() < 2) {
          sb.append('0');
        }
        sb.append(text).append(' ');
      }
      sb.append('h');
      return sb.toString();
    } else
      return new String();
  }
}

