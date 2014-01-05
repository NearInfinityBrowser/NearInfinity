// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.BrowserMenuBar;
import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.Filewriter;

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

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ResourceRef extends Datatype implements Editable, ActionListener, ListSelectionListener
{
  private static final String NONE = "None";
  private final String type[];
  private String curtype;
  String resname;
  private JButton bView;
  private TextListPanel list;
  private boolean wasNull;
  private byte buffer[];
  private final Comparator<Object> ignorecaseextcomparator = new IgnoreCaseExtComparator<Object>();

  public ResourceRef(byte h_buffer[], int offset, String name, String type)
  {
    this(h_buffer, offset, 8, name, type);
  }

  public ResourceRef(byte h_buffer[], int offset, int length, String name, String type)
  {
    this(h_buffer, offset, length, name, new String[]{type});
  }

  public ResourceRef(byte h_buffer[], int offset, String name, String[] type)
  {
    this(h_buffer, offset, 8, name, type);
  }

  public ResourceRef(byte h_buffer[], int offset, int length, String name, String[] type)
  {
    super(offset, length, name);
    if (type == null || type.length == 0)
      this.type = new String[]{""};
    else
      this.type = type;
    curtype = type[0];
    buffer = Arrays.copyOfRange(h_buffer, offset, offset + length);
    if (buffer[0] == 0x00 ||
        buffer[0] == 0x4e && buffer[1] == 0x6f && buffer[2] == 0x6e && buffer[3] == 0x65 && buffer[4] == 0x00) {
      resname = NONE;
      wasNull = true;
    } else {
      int max = buffer.length;
      for (int i = 0; i < buffer.length; i++) {
        if (buffer[i] == 0x00) {
          max = i;
          break;
        }
      }
      if (max != buffer.length)
        buffer = Arrays.copyOfRange(buffer, 0, max);
      resname = new String(buffer).toUpperCase();
    }
    if (resname.equalsIgnoreCase(NONE))
      resname = NONE;

    // determine the correct file extension
    if (!resname.equals(NONE)) {
      for (int i = 0; i < type.length; i++) {
        if (null != ResourceFactory.getInstance().getResourceEntry(resname + "." + type[i])) {
          curtype = type[i];
          break;
        }
      }
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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

  @Override
  public JComponent edit(final ActionListener container)
  {
    List<List<ResourceEntry>> resourceList = new ArrayList<List<ResourceEntry>>(type.length);
    int entrynum = 0;
    for (int i = 0; i < type.length; i++) {
      resourceList.add(ResourceFactory.getInstance().getResources(type[i]));
      entrynum += resourceList.get(i).size();
    }

    List<Object> values = new ArrayList<Object>(1 + entrynum);
    values.add(NONE);
    for (int i = 0; i < type.length; i++) {
      for (int j = 0; j < resourceList.get(i).size(); j++) {
        ResourceEntry entry = resourceList.get(i).get(j);
        if (ResourceFactory.getGameID() == ResourceFactory.ID_NWN &&
            entry.toString().length() <= 20)
          values.add(new ResourceRefEntry(entry));
        else if (entry.toString().lastIndexOf('.') <= 8 && isLegalEntry(entry))
          values.add(new ResourceRefEntry(entry));
      }
      addExtraEntries(values);
    }
    Collections.sort(values, ignorecaseextcomparator);
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
      entry = ResourceFactory.getInstance().getResourceEntry(resname + '.' + type[i]);
      if (entry != null) {
        for (int j = 0; j < values.size(); j++) {
          Object o = values.get(j);
          if (o instanceof ResourceRefEntry && ((ResourceRefEntry)o).entry == entry) {
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
      return true;
    }
    ResourceEntry entry = ((ResourceRefEntry)selected).entry;
    if (entry == null) {
      resname = ((ResourceRefEntry)selected).name;
    } else {
      int i = -1;
      for (int j = 0; j < type.length && i == -1; j++) {
        i = entry.toString().indexOf('.' + type[j].toUpperCase());
        if (i != -1) {
          resname = entry.toString().substring(0, i);
          curtype = type[j];
        }
      }
      if (i == -1)
        return false;
    }
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

  public String getResourceName()
  {
    return new StringBuffer(resname).append('.').append(curtype).toString();
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

  final class IgnoreCaseExtComparator<T> implements Comparator<T>
  {
    @Override
    public int compare(T o1, T o2)
    {
      if (o1 != null && o2 != null) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        int i1 = s1.lastIndexOf('.') > 0 ? s1.lastIndexOf('.') : s1.length();
        int i2 = s2.lastIndexOf('.') > 0 ? s2.lastIndexOf('.') : s2.length();
        return s1.substring(0, i1).compareToIgnoreCase(s2.substring(0, i2));
      } else
        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
      return obj.equals(this);
    }
  }
}

