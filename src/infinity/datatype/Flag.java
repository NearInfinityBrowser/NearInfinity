// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.util.Byteconvert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;

public class Flag extends Datatype implements Editable, ActionListener
{
  String nodesc;
  String[] table;
  private ActionListener container;
  private JButton bAll, bNone;
  private JCheckBox checkBoxes[];
  private long value;

  Flag(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    if (length == 4)
      value = (long)Byteconvert.convertInt(buffer, offset);
    else if (length == 2)
      value = (long)Byteconvert.convertShort(buffer, offset);
    else if (length == 1)
      value = (long)Byteconvert.convertByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

  public Flag(byte buffer[], int offset, int length, String name, String[] stable)
  {
    this(buffer, offset, length, name);
    nodesc = stable[0];
    table = new String[8 * length];
    for (int i = 1; i < stable.length; i++)
      table[i - 1] = stable[i];
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bAll) {
      for (final JCheckBox checkBox : checkBoxes)
        checkBox.setSelected(true);
    }
    else if (event.getSource() == bNone) {
      for (final JCheckBox checkBox : checkBoxes)
        checkBox.setSelected(false);
    }
    container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    this.container = container;
    checkBoxes = new JCheckBox[table.length];
    for (int i = 0; i < table.length; i++) {
      if (table[i] == null || table[i].equals(""))
        checkBoxes[i] = new JCheckBox("Unknown (" + i + ')');
      else
        checkBoxes[i] = new JCheckBox(table[i] + " (" + i + ')');
      checkBoxes[i].addActionListener(container);
      checkBoxes[i].setActionCommand(StructViewer.UPDATE_VALUE);
    }
    bAll = new JButton("Select all");
    bNone = new JButton("Select none");
    bAll.setMargin(new Insets(0, bAll.getMargin().left, 0, bAll.getMargin().right));
    bNone.setMargin(bAll.getMargin());
    bAll.addActionListener(this);
    bNone.addActionListener(this);

    JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    bPanel.add(bAll);
    bPanel.add(bNone);
    bPanel.add(new JLabel("None = " + nodesc));

    JPanel boxPanel = new JPanel(new GridLayout(0, 4));
    int rows = checkBoxes.length >> 2;
    if (rows << 2 != checkBoxes.length) {
      for (int i = 0; i < checkBoxes.length; i++) {
        boxPanel.add(checkBoxes[i]);
        checkBoxes[i].setSelected(isFlagSet(i));
      }
    }
    else {
      for (int i = 0; i < rows; i++)
        for (int j = 0; j < 4; j++) {
          int index = i + j * rows;
          boxPanel.add(checkBoxes[index]);
          checkBoxes[index].setSelected(isFlagSet(index));
        }
    }

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(boxPanel, BorderLayout.CENTER);
    panel.add(bPanel, BorderLayout.SOUTH);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);

    return panel;
  }

  public void select()
  {
  }

  public boolean updateValue(AbstractStruct struct)
  {
    value = (long)0;
    for (int i = 0; i < checkBoxes.length; i++)
      if (checkBoxes[i].isSelected())
        setFlag(i);
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
    StringBuffer sb = new StringBuffer("( ");
    if (value == 0)
      sb.append(nodesc).append(' ');
    else {
      for (int i = 0; i < 8 * getSize(); i++)
        if (isFlagSet(i)) {
          if (i < table.length && table[i] != null && !table[i].equals(""))
            sb.append(table[i]).append('(').append(i).append(") ");
          else
            sb.append("Unknown(").append(i).append(") ");
        }
    }
    sb.append(')');
    return sb.toString();
  }

  public String getString(int i)
  {
    return table[i];
  }

  public boolean isFlagSet(int i)
  {
    long bitnr = (long)Math.pow((double)2, (double)i);
    return (value & bitnr) == bitnr;
  }

  private void setFlag(int i)
  {
    long bitnr = (long)Math.pow((double)2, (double)i);
    value |= bitnr;
  }
}

