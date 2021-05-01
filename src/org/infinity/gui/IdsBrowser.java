// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;

public final class IdsBrowser extends ChildFrame implements ActionListener
{
  private final JButton binsert;
  private final JComboBox<ResourceEntry> idsfiles;
  private final TextListPanel<IdsMapEntry> list;

  public IdsBrowser()
  {
    super("IDS Browser");
    setIconImage(Icons.getIcon(Icons.ICON_HISTORY_16).getImage());

    List<ResourceEntry> resList = ResourceFactory.getResources("IDS");
    idsfiles = new JComboBox<>(resList.toArray(new ResourceEntry[resList.size()]));
    idsfiles.setEditable(false);
    idsfiles.setSelectedIndex(0);
    idsfiles.addActionListener(this);

    binsert = new JButton("Insert reference", Icons.getIcon(Icons.ICON_PASTE_16));
    binsert.setMnemonic('i');
    binsert.addActionListener(this);
    binsert.setToolTipText("Inserts selected text into script displayed in main window");
    getRootPane().setDefaultButton(binsert);

    final IdsMap idsMap = IdsMapCache.get(idsfiles.getSelectedItem().toString());
    list = new TextListPanel<>(idsMap.getAllValues());
    list.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2)
          insertString(list.getSelectedValue().getSymbol());
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

    setSize(Misc.getScaledValue(350), Misc.getScaledValue(500));
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == idsfiles)
      refreshList();
    else if (event.getSource() == binsert)
      insertString(list.getSelectedValue().getSymbol());
  }

// --------------------- End Interface ActionListener ---------------------

  public void refreshList()
  {
    final IdsMap idsMap = IdsMapCache.get(idsfiles.getSelectedItem().toString());
    list.setValues(idsMap.getAllValues());
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

