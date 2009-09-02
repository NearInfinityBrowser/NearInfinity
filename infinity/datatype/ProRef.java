// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class ProRef extends Datatype implements Editable, ActionListener, ListSelectionListener
{
  private final LongIntegerHashMap idsmap;
  private JButton bView;
  private TextListPanel list;
  private long value;

  public ProRef(byte buffer[], int offset, String name)
  {
    super(offset, 2, name);
    idsmap = IdsMapCache.get("PROJECTL.IDS").getMap();
    value = (long)Byteconvert.convertUnsignedShort(buffer, offset);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      ProRefEntry selected = (ProRefEntry)list.getSelectedValue();
      if (selected == null || !(selected.proref instanceof ResourceEntry))
        new ViewFrame(list.getTopLevelAncestor(), null);
      else
        new ViewFrame(list.getTopLevelAncestor(),
                      ResourceFactory.getResource((ResourceEntry)selected.proref));
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    long keys[] = idsmap.keys();
    List<ProRefEntry> items = new ArrayList<ProRefEntry>(keys.length);
    for (long id : keys) {
      String resourcename = ((IdsMapEntry)idsmap.get(id)).getString() + ".PRO";
      ResourceEntry resourceEntry = ResourceFactory.getInstance().getResourceEntry(resourcename);
      if (resourceEntry == null)
        System.err.println("Could not find " + resourcename + " (key = " + id + ")");
      else
        items.add(new ProRefEntry(id + 1L, resourceEntry));
    }
    items.add(new ProRefEntry(1L, "None"));
    list = new TextListPanel(items);
    list.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2)
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
      }
    });

    String selected = toString();
    for (int i = 0; i < list.getModel().getSize(); i++)
      if (selected.equalsIgnoreCase(list.getModel().getElementAt(i).toString())) {
        list.setSelectedIndex(i);
        break;
      }

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);
    bView = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bView.addActionListener(this);
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
    ProRefEntry selected = (ProRefEntry)list.getSelectedValue();
    value = selected.val;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent e)
  {
    bView.setEnabled(list.getSelectedValue() != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    if (idsmap.containsKey(value - (long)1))
      return ResourceFactory.getInstance().getResourceEntry(((IdsMapEntry)idsmap.get(value - 1)).getString()
                                                            + ".PRO") + " (" + value + ')';
    return "None (" + value + ')';
  }

  public ResourceEntry getSelectedEntry()
  {
    if (!idsmap.containsKey(value - (long)1))
      return null;
    return ResourceFactory.getInstance().getResourceEntry(
            ((IdsMapEntry)idsmap.get(value - 1)).getString() + ".PRO");
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class ProRefEntry implements Comparable
  {
    private final long val;
    private final Object proref;

    private ProRefEntry(long val, Object proref)
    {
      this.val = val;
      this.proref = proref;
    }

    public String toString()
    {
      return proref.toString() + " (" + val + ')';
    }

    public int compareTo(Object o)
    {
      return proref.toString().compareTo(o.toString());
    }
  }
}

