// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Flag extends Datatype implements Editable, ActionListener
{
  public static final String DESC_NONE = "No flags set";
  private final List<UpdateListener> listeners = new ArrayList<UpdateListener>();

  protected String nodesc;
  protected String[] table, toolTable;
  private ActionListener container;
  private JButton bAll, bNone;
  private JCheckBox[] checkBoxes;
  private long value;

  Flag(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  Flag(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, offset, length, name);
    read(buffer, offset);
  }

  /**
   * @param stable Contains default value when no flag is selected and a list of flag descriptions.
   *               Optionally you can combine flag descriptions with tool tips, using the defaul
   *               separator char ';'.
   */
  public Flag(byte buffer[], int offset, int length, String name, String[] stable)
  {
    this(null, buffer, offset, length, name, stable);
  }

  /**
   * @param stable Contains default value when no flag is selected and a list of flag descriptions.
   *               Optionally you can combine flag descriptions with tool tips, using the specified
   *               separator char.
   * @param separator Character that can be used to split flag description and tool tip.
   */
  public Flag(byte buffer[], int offset, int length, String name, String[] stable, char separator)
  {
    this(null, buffer, offset, length, name, stable, separator);
  }

  /**
   * @param stable Contains default value when no flag is selected and a list of flag descriptions.
   *               Optionally you can combine flag descriptions with tool tips, using the defaul
   *               separator char ';'.
   */
  public Flag(StructEntry parent, byte buffer[], int offset, int length, String name, String[] stable)
  {
    this(parent, buffer, offset, length, name, stable, ';');
  }

  /**
   * @param stable Contains default value when no flag is selected and a list of flag descriptions.
   *               Optionally you can combine flag descriptions with tool tips, using the specified
   *               separator char.
   * @param separator Character that can be used to split flag description and tool tip.
   */
  public Flag(StructEntry parent, byte buffer[], int offset, int length, String name, String[] stable,
              char separator)
  {
    this(parent, buffer, offset, length, name);
    setEmptyDesc((stable == null || stable.length == 0) ? null : stable[0]);
    setFlagDescriptions(length, stable, 1, separator);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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

  @Override
  public JComponent edit(ActionListener container)
  {
    this.container = container;
    checkBoxes = new JCheckBox[table.length];
    for (int i = 0; i < table.length; i++) {
      if (table[i] == null || table[i].isEmpty()) {
        checkBoxes[i] = new JCheckBox("Unknown (" + i + ')');
      } else {
        checkBoxes[i] = new JCheckBox(table[i] + " (" + i + ')');
      }
      if (toolTable[i] != null && !toolTable[i].isEmpty()) {
        checkBoxes[i].setToolTipText(toolTable[i]);
      }
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
    panel.setPreferredSize(DIM_WIDE);

    return panel;
  }

  @Override
  public void select()
  {
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    // updating value
    value = (long)0;
    for (int i = 0; i < checkBoxes.length; i++)
      if (checkBoxes[i].isSelected()) {
        setFlag(i);
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
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 1:
        value = DynamicArray.getUnsignedByte(buffer, offset);
        break;
      case 2:
        value = DynamicArray.getUnsignedShort(buffer, offset);
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

  public long getValue()
  {
    return value;
  }

  public void setValue(long newValue)
  {
    value = newValue;
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

  private void setFlag(int i)
  {
    long bitnr = (long)Math.pow((double)2, (double)i);
    value |= bitnr;
  }

  // Sets description for empty flags
  protected void setEmptyDesc(String desc)
  {
    nodesc = (desc != null) ? desc : DESC_NONE;
  }

  // Sets labels and optional tooltips for each flag
  protected void setFlagDescriptions(int size, String[] stable, int startOfs, char separator)
  {
    table = new String[8*size];
    toolTable = new String[8*size];
    if (stable != null) {
      for (int i = startOfs; i < stable.length; i++) {
        if (stable[i] == null) {
          stable[i] = "";
        }
        String[] s = null;
        try {
          s = stable[i].split(String.valueOf(separator));
        } catch (PatternSyntaxException pse) {
          pse.printStackTrace();
        }
        if (s == null || s.length == 0) {
          table[i - startOfs] = stable[i];
          toolTable[i - startOfs] = null;
        } else {
          table[i - startOfs] = s[0];
          toolTable[i - startOfs] = (s.length > 1) ? s[1] : null;
        }
      }
    }
  }
}

