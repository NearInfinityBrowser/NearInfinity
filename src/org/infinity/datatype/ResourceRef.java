// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileWriterNI;

public class ResourceRef extends Datatype
    implements Editable, IsTextual, IsReference, ActionListener, ListSelectionListener
{
  private static final Comparator<Object> ignoreCaseExtComparator = new IgnoreCaseExtComparator();

  private static final String NONE = "None";
  private final String[] type;
  private String curtype;
  private String resname;
  private JButton bView;
  private TextListPanel list;
  private byte[] buffer;

  public ResourceRef(byte[] h_buffer, int offset, String name, String type)
  {
    this(null, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(StructEntry parent, byte[] h_buffer, int offset, String name, String type)
  {
    this(parent, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(byte[] h_buffer, int offset, int length, String name, String type)
  {
    this(null, h_buffer, offset, length, name, new String[]{type});
  }

  public ResourceRef(StructEntry parent, byte[] h_buffer, int offset, int length, String name,
                     String type)
  {
    this(parent, h_buffer, offset, length, name, new String[]{type});
  }

  public ResourceRef(byte[] h_buffer, int offset, String name, String[] type)
  {
    this(null, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(StructEntry parent, byte[] h_buffer, int offset, String name, String[] type)
  {
    this(parent, h_buffer, offset, 8, name, type);
  }

  public ResourceRef(byte[] h_buffer, int offset, int length, String name, String[] type)
  {
    this(null, h_buffer, offset, length, name, type);
  }

  public ResourceRef(StructEntry parent, byte[] h_buffer, int offset, int length, String name,
                     String[] type)
  {
    super(parent, offset, length, name);
    if (type == null || type.length == 0)
      this.type = new String[]{""};
    else
      this.type = type;
    curtype = this.type[0];
    read(h_buffer, offset);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      Object selected = list.getSelectedValue();
      if (selected == NONE) {
        new ViewFrame(list.getTopLevelAncestor(), null);
      } else {
        ResourceEntry entry = ((ResourceRefEntry)selected).entry;
        if (entry != null) {
          new ViewFrame(list.getTopLevelAncestor(), ResourceFactory.getResource(entry));
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    List<List<ResourceEntry>> resourceList = new ArrayList<List<ResourceEntry>>(type.length);
    int entrynum = 0;
    for (int i = 0; i < type.length; i++) {
      resourceList.add(ResourceFactory.getResources(type[i]));
      entrynum += resourceList.get(i).size();
    }

    List<Object> values = new ArrayList<Object>(1 + entrynum);
    values.add(NONE);
    for (int i = 0; i < type.length; i++) {
      for (int j = 0; j < resourceList.get(i).size(); j++) {
        ResourceEntry entry = resourceList.get(i).get(j);
        if (entry.toString().lastIndexOf('.') <= 8 && isLegalEntry(entry))
          values.add(new ResourceRefEntry(entry));
      }
      addExtraEntries(values);
    }
    Collections.sort(values, ignoreCaseExtComparator);
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

    ResourceEntry entry = null;
    for (int i = 0; i < type.length && entry == null; i++) {
      entry = ResourceFactory.getResourceEntry(resname + '.' + type[i], true);
      if (entry != null) {
        for (int j = 0; j < values.size(); j++) {
          Object o = values.get(j);
          if (o instanceof ResourceRefEntry && ((ResourceRefEntry)o).entry.equals(entry)) {
            list.setSelectedValue(o, true);
            break;
          }
        }
      }
    }
    if (entry == null) {
      list.setSelectedValue(NONE, true);
      for (int j = 0; j < values.size(); j++) {
        Object o = values.get(j);
        if (o instanceof ResourceRefEntry && ((ResourceRefEntry)o).name.equals(resname)) {
          list.setSelectedValue(o, true);
          break;
        }
      }
    }

    JButton bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bView = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
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

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    Object selected = list.getSelectedValue();
    if (selected == NONE) {
      resname = NONE;

      // notifying listeners
      fireValueUpdated(new UpdateEvent(this, struct));

      return true;
    }

    ResourceEntry entry = ((ResourceRefEntry)selected).entry;
    if (entry == null) {
      resname = ((ResourceRefEntry)selected).name;
    } else {
      int i = -1;
      for (int j = 0; j < type.length && i == -1; j++) {
        i = entry.toString().indexOf('.' + type[j].toUpperCase(Locale.ENGLISH));
        if (i != -1) {
          resname = entry.toString().substring(0, i);
          curtype = type[j];
        }
      }
      if (i == -1) {
        return false;
      }
    }

    // notifying listeners
    fireValueUpdated(new UpdateEvent(this, struct));

    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    bView.setEnabled(list.getSelectedValue() != null &&
                     list.getSelectedValue() != NONE &&
                     ((ResourceRefEntry)list.getSelectedValue()).entry != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (resname.equalsIgnoreCase(NONE)) {
      String s = DynamicArray.getString(buffer, 0, buffer.length);
      if (s.equalsIgnoreCase(NONE)) {
        FileWriterNI.writeBytes(os, buffer);
      } else {
        FileWriterNI.writeBytes(os, (byte)0, buffer.length);
      }
    } else {
      FileWriterNI.writeString(os, resname, getSize());
    }
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(byte[] buffer, int offset)
  {
    this.buffer = Arrays.copyOfRange(buffer, offset, offset + getSize());
    String s = new String(this.buffer);
    if (this.buffer[0] == 0x00 || s.equalsIgnoreCase(NONE)) {
      resname = NONE;
    } else {
      int max = this.buffer.length;
      for (int i = 0; i < this.buffer.length; i++) {
        if (this.buffer[i] == 0x00) {
          max = i;
          break;
        }
      }
      if (max < this.buffer.length) {
        resname = new String(this.buffer, 0, max).toUpperCase(Locale.ENGLISH);
      } else {
        resname = new String(this.buffer).toUpperCase(Locale.ENGLISH);
      }

      if (resname.equalsIgnoreCase(NONE))
        resname = NONE;
    }

    // determine the correct file extension
    if (!resname.equals(NONE)) {
      for (int i = 0; i < this.type.length; i++) {
        if (null != ResourceFactory.getResourceEntry(resname + "." + this.type[i], true)) {
          curtype = this.type[i];
          break;
        }
      }
    }

    return offset + getSize();
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    if (resname.equals(NONE))
      return resname;
    String searchName = getSearchName();
    if (searchName != null)
      return new StringBuffer(getResourceName()).append(" (").append(searchName).append(')').toString();
    return getResourceName();
  }

//--------------------- Begin Interface IsTextual ---------------------

  @Override
  public String getText()
  {
    return resname;
  }

//--------------------- End Interface IsTextual ---------------------

//--------------------- Begin Interface IsReference ---------------------

  @Override
  public String getResourceName()
  {
    if (resname.equals(NONE)) {
      return resname;
    } else {
      return new StringBuffer(resname).append('.').append(curtype).toString();
    }
  }

//--------------------- End Interface IsReference ---------------------

  public boolean isEmpty()
  {
    return (resname.equals(NONE));
  }

  public String getSearchName()
  {
    ResourceEntry entry = ResourceFactory.getResourceEntry(getResourceName(), true);
    if (entry != null)
      return entry.getSearchString();
    return null;
  }

  public String getType()
  {
    return curtype;
  }

  public boolean isLegalEntry(ResourceEntry entry)
  {
    return entry.toString().lastIndexOf('.') != 0;
  }

  void addExtraEntries(List<Object> entries)
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

    @Override
    public String toString()
    {
      return name;
    }
  }

  private static class IgnoreCaseExtComparator implements Comparator<Object>
  {
    @Override
    public int compare(Object o1, Object o2)
    {
      if (o1 != null && o2 != null) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        int i1 = s1.lastIndexOf('.') > 0 ? s1.lastIndexOf('.') : s1.length();
        int i2 = s2.lastIndexOf('.') > 0 ? s2.lastIndexOf('.') : s2.length();
        return s1.substring(0, i1).compareToIgnoreCase(s2.substring(0, i2));
      } else {
        return 0;
      }
    }

    @Override
    public boolean equals(Object obj)
    {
      return obj.equals(this);
    }
  }
}

