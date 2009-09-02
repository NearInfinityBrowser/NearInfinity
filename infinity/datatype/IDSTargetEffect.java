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
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class IDSTargetEffect extends Datatype implements Editable, ListSelectionListener
{
  private final String sIDS[] = new String[]
  {"", "", "EA.IDS", "GENERAL.IDS", "RACE.IDS", "CLASS.IDS",
   "SPECIFIC.IDS", "GENDER.IDS", "ALIGN.IDS"};
  private LongIntegerHashMap<IdsMapEntry> idsMap;
  private TextListPanel fileList, valueList;
  private long idsValue, idsFile;

  public IDSTargetEffect(byte buffer[], int offset)
  {
    this(buffer, offset, "EA.IDS");
  }

  public IDSTargetEffect(byte buffer[], int offset, String secondIDS)
  {
    super(offset, 8, "IDS target");
    idsValue = Byteconvert.convertUnsignedInt(buffer, offset);
    idsFile = Byteconvert.convertUnsignedInt(buffer, offset + 4);
    sIDS[2] = secondIDS;
    if (idsFile < sIDS.length && !sIDS[(int)idsFile].equals(""))
      idsMap = IdsMapCache.get(sIDS[(int)idsFile]).getMap();
    else
      idsMap = new LongIntegerHashMap<IdsMapEntry>();
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    if (fileList == null) {
      if (idsFile >= sIDS.length)
        idsFile = 0;
      List<String> values = new ArrayList<String>(sIDS.length);
      for (int i = 0; i < sIDS.length; i++)
        values.add(getString(i));
      fileList = new TextListPanel(values);
      fileList.addListSelectionListener(this);
      long keys[] = idsMap.keys();
      List items = new ArrayList(keys.length);
      for (long id : keys) {
        Object value = idsMap.get(id);
        if (value instanceof IdsMapEntry)
          items.add(value);
        else if (value != null)
          items.add(value.toString() + " - " + id);
      }
      valueList = new TextListPanel(items);
      valueList.addMouseListener(new MouseAdapter()
      {
        public void mouseClicked(MouseEvent event)
        {
          if (event.getClickCount() == 2)
            container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      });
    }
    if (idsFile >= 0 && idsFile < fileList.getModel().getSize()) {
      int index = 0;
      while (!fileList.getModel().getElementAt(index).equals(getString((int)idsFile)))
        index++;
      fileList.setSelectedIndex(index);
    }
    Object selected = idsMap.get(idsValue);
    if (selected != null) {
      if (selected instanceof IdsMapEntry)
        valueList.setSelectedValue(selected, true);
      else
        valueList.setSelectedValue(selected.toString() + " - " + idsValue, true);
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
    gbl.setConstraints(fileList, gbc);
    panel.add(fileList);

    gbc.insets.left = 6;
    gbl.setConstraints(valueList, gbc);
    panel.add(valueList);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_BROAD);
    panel.setPreferredSize(DIM_BROAD);
    return panel;
  }

  public void select()
  {
    fileList.ensureIndexIsVisible(fileList.getSelectedIndex());
    valueList.ensureIndexIsVisible(valueList.getSelectedIndex());
  }

  public boolean updateValue(AbstractStruct struct)
  {
    String svalue = (String)fileList.getSelectedValue();
    idsFile = 0L;
    while (!svalue.equals(getString((int)idsFile)))
      idsFile++;

    if (valueList.getSelectedValue() instanceof IdsMapEntry) {
      IdsMapEntry selected = (IdsMapEntry)valueList.getSelectedValue();
      idsValue = selected.getID();
    }
    else {
      String selected = valueList.getSelectedValue().toString();
      int i = selected.lastIndexOf(" - ");
      try {
        idsValue = Long.parseLong(selected.substring(i + 3));
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getValueIsAdjusting())
      return;
    long vSelected = 0L;
    if (idsMap.size() > 0 && valueList.getSelectedValue() instanceof IdsMapEntry) {
      IdsMapEntry selected = (IdsMapEntry)valueList.getSelectedValue();
      vSelected = selected.getID();
    }
    else if (idsMap.size() > 0) {
      String selected = valueList.getSelectedValue().toString();
      int i = selected.lastIndexOf(" - ");
      try {
        vSelected = Long.parseLong(selected.substring(i + 3));
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }

    String svalue = (String)fileList.getSelectedValue();
    int fSelected = 0;
    while (!svalue.equals(getString(fSelected)))
      fSelected++;

    if (idsFile < sIDS.length && !sIDS[fSelected].equals(""))
      idsMap = IdsMapCache.get(sIDS[fSelected]).getMap();
    else
      idsMap = new LongIntegerHashMap<IdsMapEntry>();

    long keys[] = idsMap.keys();
    List items = new ArrayList(keys.length);
    for (long id : keys) {
      Object value = idsMap.get(id);
      if (value instanceof IdsMapEntry)
        items.add(value);
      else if (value != null)
        items.add(value.toString() + " - " + id);
    }
    valueList.setValues(items);

    Object selected = idsMap.get(vSelected);
    if (selected != null) {
      if (selected instanceof IdsMapEntry)
        valueList.setSelectedValue(selected, true);
      else
        valueList.setSelectedValue(selected.toString() + " - " + idsValue, true);
    }
    else if (idsMap.size() > 0)
      valueList.setSelectedIndex(0);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeUnsignedInt(os, idsValue);
    Filewriter.writeUnsignedInt(os, idsFile);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    String idsFileStr = getString((int)idsFile) + " / ";
    Object o = idsMap.get(idsValue);
    if (o == null)
      return idsFileStr + "Unknown value - " + idsValue;
    else if (o instanceof IdsMapEntry)
      return idsFileStr + idsMap.get(idsValue).toString();
    else
      return idsFileStr + idsMap.get(idsValue).toString() + " - " + idsValue;
  }

  public long getValue()
  {
    return idsValue;
  }

  private String getString(int nr)
  {
    if (nr >= sIDS.length)
      return "Unknown - " + nr;
    if (nr < 0)
      return "Error - " + nr;
    if (sIDS[nr].equals(""))
      return "Unknown - " + nr;
    return new StringBuffer(sIDS[nr]).append(" - ").append(nr).toString();
  }
}

