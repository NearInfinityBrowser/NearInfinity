// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.Viewable;
import infinity.resource.bcs.BcsResource;
import infinity.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class IdsBrowser extends ChildFrame implements ActionListener
{
  private final JButton binsert;
  private final JComboBox idsfiles;
  private final TextListPanel list;

  public IdsBrowser()
  {
    super("IDS Browser");
    setIconImage(Icons.getIcon("History16.gif").getImage());

    idsfiles = new JComboBox(ResourceFactory.getInstance().getResources("IDS").toArray());
    idsfiles.setEditable(false);
    idsfiles.setSelectedIndex(0);
    idsfiles.addActionListener(this);

    binsert = new JButton("Insert reference", Icons.getIcon("Paste16.gif"));
    binsert.setMnemonic('i');
    binsert.addActionListener(this);
    binsert.setToolTipText("Inserts selected text into script displayed in main window");
    getRootPane().setDefaultButton(binsert);

    IdsMap idsmap = IdsMapCache.get(idsfiles.getSelectedItem().toString());
    list = new TextListPanel(idsmap.getAllValues());
    list.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2)
          insertString(((IdsMapEntry)list.getSelectedValue()).getString());
      }
    });

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.insets = new Insets(6, 6, 6, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(idsfiles, gbc);
    pane.add(idsfiles);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbl.setConstraints(list, gbc);
    pane.add(list);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbl.setConstraints(binsert, gbc);
    pane.add(binsert);

    setSize(350, 500);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == idsfiles)
      refreshList();
    else if (event.getSource() == binsert)
      insertString(((IdsMapEntry)list.getSelectedValue()).getString());
  }

// --------------------- End Interface ActionListener ---------------------

  public void refreshList()
  {
    IdsMap idsmap = IdsMapCache.get(idsfiles.getSelectedItem().toString());
    list.setValues(idsmap.getAllValues());
  }

  private void insertString(String s)
  {
    Viewable viewable = NearInfinity.getInstance().getViewable();
    if (viewable == null || !(viewable instanceof BcsResource))
      JOptionPane.showMessageDialog(this, "No script displayed in the main window", "Error",
                                    JOptionPane.ERROR_MESSAGE);
    else
      ((BcsResource)viewable).insertString(s);
  }
}

