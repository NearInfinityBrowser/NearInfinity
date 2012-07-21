// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.ArrayUtil;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ResourceRef extends Datatype implements Editable, ActionListener, ListSelectionListener
{
  private static final String NONE = "None";
  private final String type;
  String resname;
  private JButton bView;
  private TextListPanel list;
  private boolean wasNull;
  private byte buffer[];

  public ResourceRef(byte h_buffer[], int offset, String name, String type)
  {
    this(h_buffer, offset, 8, name, type);
  }

  public ResourceRef(byte h_buffer[], int offset, int length, String name, String type)
  {
    super(offset, length, name);
    this.type = type;
    buffer = ArrayUtil.getSubArray(h_buffer, offset, length);
    if (buffer[0] == 0x00 ||
        buffer[0] == 0x4e && buffer[1] == 0x6f && buffer[2] == 0x6e && buffer[3] == 0x65) {
      resname = NONE;
      wasNull = true;
    }
    else {
      int max = buffer.length;
      for (int i = 0; i < buffer.length; i++) {
        if (buffer[i] == 0x00) {
          max = i;
          break;
        }
      }
      if (max != buffer.length)
        buffer = ArrayUtil.getSubArray(buffer, 0, max);
      resname = new String(buffer).toUpperCase();
    }
    if (resname.equalsIgnoreCase(NONE))
      resname = NONE;
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      Object selected = list.getSelectedValue();
      if (selected == NONE)
        new ViewFrame(list.getTopLevelAncestor(), null);
      else {
        ResourceEntry entry = ((ResourceRefEntry)selected).entry;
        if (entry != null)
          new ViewFrame(list.getTopLevelAncestor(), ResourceFactory.getResource(entry));
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    List<ResourceEntry> resourceList = ResourceFactory.getInstance().getResources(type);
    List values = new ArrayList(1 + resourceList.size());
    values.add(NONE);
    for (int i = 0; i < resourceList.size(); i++) {
      ResourceEntry entry = resourceList.get(i);
      if (ResourceFactory.getGameID() == ResourceFactory.ID_NWN &&
          entry.toString().length() <= 20)
        values.add(new ResourceRefEntry(entry));
      else if (entry.toString().length() <= 12 && isLegalEntry(entry))
        values.add(new ResourceRefEntry(entry));
    }
    addExtraEntries(values);
    list = new TextListPanel(values);
    list.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2)
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
      }
    });
    ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(resname + '.' + type);
    if (entry == null) {
      list.setSelectedValue(NONE, true);
      for (int i = 0; i < values.size(); i++) {
        Object o = values.get(i);
        if (o instanceof ResourceRefEntry && ((ResourceRefEntry)o).name.equals(resname)) {
          list.setSelectedValue(o, true);
          break;
        }
      }
    }
    else {
      for (int i = 0; i < values.size(); i++) {
        Object o = values.get(i);
        if (o instanceof ResourceRefEntry && ((ResourceRefEntry)o).entry == entry) {
          list.setSelectedValue(o, true);
          break;
        }
      }
    }

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bView = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bView.addActionListener(this);
    bView.setEnabled(list.getSelectedValue() != null && list.getSelectedValue() != NONE &&
                     ((ResourceRefEntry)list.getSelectedValue()).entry != null);
    list.addListSelectionListener(this);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridheight = 2;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.gridheight = 1;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(3, 6, 3, 0);
    gbc.anchor = GridBagConstraints.SOUTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.NORTH;
    gbl.setConstraints(bView, gbc);
    panel.add(bView);

    panel.setMinimumSize(DIM_MEDIUM);
    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public boolean updateValue(AbstractStruct struct)
  {
    Object selected = list.getSelectedValue();
    if (selected == NONE) {
      resname = NONE;
      return true;
    }
    ResourceEntry entry = ((ResourceRefEntry)selected).entry;
    if (entry == null)
      resname = ((ResourceRefEntry)selected).name;
    else {
      int i = entry.toString().indexOf('.' + type.toUpperCase());
      if (i == -1)
        return false;
      resname = entry.toString().substring(0, i);
    }
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent e)
  {
    bView.setEnabled(list.getSelectedValue() != null &&
                     list.getSelectedValue() != NONE &&
                     ((ResourceRefEntry)list.getSelectedValue()).entry != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    if (resname.equals(NONE)) {
      if (wasNull)
        Filewriter.writeBytes(os, buffer);
      else
        Filewriter.writeBytes(os, new byte[getSize()]);
    }
    else
      Filewriter.writeString(os, resname, getSize());
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    if (resname.equals(NONE))
      return resname;
    String searchName = getSearchName();
    if (searchName != null)
      return new StringBuffer(getResourceName()).append(" (").append(searchName).append(')').toString();
    return getResourceName();
  }

  public String getResourceName()
  {
    return new StringBuffer(resname).append('.').append(type).toString();
  }

  public String getSearchName()
  {
    ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(getResourceName());
    if (entry != null)
      return entry.getSearchString();
    return null;
  }

  public String getType()
  {
    return type;
  }

  public boolean isLegalEntry(ResourceEntry entry)
  {
    return true;
  }

  void addExtraEntries(List<ResourceRefEntry> entries)
  {
  }

// -------------------------- INNER CLASSES --------------------------

  static final class ResourceRefEntry
  {
    private final ResourceEntry entry;
    private final String name;

    private ResourceRefEntry(ResourceEntry entry)
    {
      this.entry = entry;
      String string = entry.toString();
      String search = entry.getSearchString();
      if (search == null || BrowserMenuBar.getInstance().getResRefMode() == BrowserMenuBar.RESREF_ONLY)
        name = string;
      else if (BrowserMenuBar.getInstance().getResRefMode() == BrowserMenuBar.RESREF_REF_NAME)
        name = string + " (" + search + ')';
      else
        name = search + " (" + string + ')';
    }

    ResourceRefEntry(String name)
    {
      this.name = name;
      entry = null;
    }

    public String toString()
    {
      return name;
    }
  }
}

