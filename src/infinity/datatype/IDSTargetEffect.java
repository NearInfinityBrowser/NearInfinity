// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Profile;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.io.FileWriterNI;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class IDSTargetEffect extends Datatype implements Editable, ListSelectionListener
{
  /** The default field name of this datatype. */
  public static final String DEFAULT_NAME = "IDS target";

  private static final String[] sIDS_default = {"", "", "EA.IDS", "GENERAL.IDS", "RACE.IDS",
                                                "CLASS.IDS", "SPECIFIC.IDS", "GENDER.IDS",
                                                "ALIGNMEN.IDS", ""};
  private final String[] sIDS;

  private LongIntegerHashMap<IdsMapEntry> idsMap;
  private TextListPanel fileList, valueList;
  private long idsValue, idsFile;

  public IDSTargetEffect(byte buffer[], int offset)
  {
    this(null, buffer, offset, 8, DEFAULT_NAME, "EA.IDS");
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset)
  {
    this(parent, buffer, offset, 8, DEFAULT_NAME, "EA.IDS");
  }

  public IDSTargetEffect(byte buffer[], int offset, int size)
  {
    this(null, buffer, offset, size, DEFAULT_NAME, "EA.IDS");
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset, int size)
  {
    this(parent, buffer, offset, size, DEFAULT_NAME, "EA.IDS");
  }

  public IDSTargetEffect(byte buffer[], int offset, String name)
  {
    this(null, buffer, offset, 8, name, "EA.IDS");
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset, String name)
  {
    this(parent, buffer, offset, 8, name, "EA.IDS");
  }

  public IDSTargetEffect(byte buffer[], int offset, int size, String name)
  {
    this(null, buffer, offset, size, name, "EA.IDS");
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset, int size, String name)
  {
    this(parent, buffer, offset, size, name, "EA.IDS");
  }

  public IDSTargetEffect(byte buffer[], int offset, String name, String secondIDS)
  {
    this(null, buffer, offset, 8, name, secondIDS);
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset, String name, String secondIDS)
  {
    this(parent, buffer, offset, 8, name, secondIDS);
  }

  public IDSTargetEffect(byte buffer[], int offset, int size, String name, String secondIDS)
  {
    this(null, buffer, offset, size, name, secondIDS);
  }

  public IDSTargetEffect(StructEntry parent, byte buffer[], int offset, int size, String name,
                         String secondIDS)
  {
    super(parent, offset, size, (name != null) ? name : DEFAULT_NAME);
    sIDS = sIDS_default;
    sIDS[2] = secondIDS;
    if (Profile.isEnhancedEdition() || (Boolean)Profile.getProperty(Profile.IS_GAME_TOBEX)) {
      sIDS[9] = "KIT.IDS";
    }
    sIDS[8] = (String)Profile.getProperty(Profile.GET_IDS_ALIGNMENT);
    read(buffer, offset);
  }

  public IDSTargetEffect(byte buffer[], int offset, String name, String[] ids)
  {
    this(buffer, offset, 8, name, ids);
  }

  public IDSTargetEffect(byte buffer[], int offset, int size, String name, String[] ids)
  {
    super(offset, size, (name != null) ? name : DEFAULT_NAME);
    if (ids != null) {
      sIDS = ids;
    } else {
      sIDS = sIDS_default;
    }
    read(buffer, offset);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
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
      long[] keys = idsMap.keys();
      List<Object> items = new ArrayList<Object>(keys.length);
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
        @Override
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

  @Override
  public void select()
  {
    fileList.ensureIndexIsVisible(fileList.getSelectedIndex());
    valueList.ensureIndexIsVisible(valueList.getSelectedIndex());
  }

  @Override
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

  @Override
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

    if (idsFile < sIDS.length && !sIDS[fSelected].equals("")) {
      idsMap = IdsMapCache.get(sIDS[fSelected]).getMap();
      if (!idsMap.containsKey(Long.valueOf(0L)) && sIDS[fSelected].equalsIgnoreCase("EA.IDS")) {
        idsMap.put(Long.valueOf(0L), new IdsMapEntry(0L, "ANYONE", null));
      }
    } else {
      idsMap = new LongIntegerHashMap<IdsMapEntry>();
    }

    long[] keys = idsMap.keys();
    List<Object> items = new ArrayList<Object>(keys.length);
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

  @Override
  public void write(OutputStream os) throws IOException
  {
    switch (getSize()) {
      case 2:
        FileWriterNI.writeByte(os, (byte)idsValue);
        FileWriterNI.writeByte(os, (byte)idsFile);
        break;
      case 4:
        FileWriterNI.writeShort(os, (short)idsValue);
        FileWriterNI.writeShort(os, (short)idsFile);
        break;
      case 8:
        FileWriterNI.writeInt(os, (int)idsValue);
        FileWriterNI.writeInt(os, (int)idsFile);
        break;
    }
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    switch (getSize()) {
      case 2:
        idsValue = DynamicArray.getUnsignedByte(buffer, offset);
        idsFile = DynamicArray.getUnsignedByte(buffer, offset + 1);
        if (idsFile < sIDS.length && !sIDS[(int)idsFile].equals("")) {
          idsMap = IdsMapCache.get(sIDS[(int)idsFile]).getMap();
          if (!idsMap.containsKey(Long.valueOf(0L)) && sIDS[(int)idsFile].equalsIgnoreCase("EA.IDS")) {
            idsMap.put(Long.valueOf(0L), new IdsMapEntry(0L, "ANYONE", null));
          }
        } else {
          idsMap = new LongIntegerHashMap<IdsMapEntry>();
        }
        break;
      case 4:
        idsValue = DynamicArray.getUnsignedShort(buffer, offset);
        idsFile = DynamicArray.getUnsignedShort(buffer, offset + 2);
        if (idsFile < sIDS.length && !sIDS[(int)idsFile].equals("")) {
          idsMap = IdsMapCache.get(sIDS[(int)idsFile]).getMap();
          if (!idsMap.containsKey(Long.valueOf(0L)) && sIDS[(int)idsFile].equalsIgnoreCase("EA.IDS")) {
            idsMap.put(Long.valueOf(0L), new IdsMapEntry(0L, "ANYONE", null));
          }
        } else {
          idsMap = new LongIntegerHashMap<IdsMapEntry>();
        }
        break;
      case 8:
        idsValue = DynamicArray.getUnsignedInt(buffer, offset);
        idsFile = DynamicArray.getUnsignedInt(buffer, offset + 4);
        if (idsFile < sIDS.length && !sIDS[(int)idsFile].equals("")) {
          idsMap = IdsMapCache.get(sIDS[(int)idsFile]).getMap();
          if (!idsMap.containsKey(Long.valueOf(0L)) && sIDS[(int)idsFile].equalsIgnoreCase("EA.IDS")) {
            idsMap.put(Long.valueOf(0L), new IdsMapEntry(0L, "ANYONE", null));
          }
        } else {
          idsMap = new LongIntegerHashMap<IdsMapEntry>();
        }
        break;
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
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

