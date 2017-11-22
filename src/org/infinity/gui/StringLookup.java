// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.datatype.StringRef;
import org.infinity.icon.Icons;
import org.infinity.search.SearchClient;
import org.infinity.search.SearchMaster;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

final class StringLookup extends ChildFrame implements SearchClient
{
  private final StringRef strref;

  StringLookup()
  {
    super("StringRef Lookup");
    setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());
    strref = new StringRef("StringRef:", 0);
    Component com = strref.edit(null);

    JPanel findpanel = SearchMaster.createAsPanel(this, this);
    findpanel.setBorder(BorderFactory.createTitledBorder("Find: StringRef"));

    JPanel pane = (JPanel)getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 2.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(6, 6, 3, 6);
    gbl.setConstraints(com, gbc);
    pane.add(com);

    gbc.weighty = 0.0;
    gbc.insets = new Insets(3, 6, 3, 8);
    gbl.setConstraints(findpanel, gbc);
    pane.add(findpanel);

    setSize(Misc.getScaledValue(540), Misc.getScaledValue(350));
    Center.center(this, NearInfinity.getInstance().getBounds());

    // pre-caching string table to significantly reduce search time
    new Thread(new Runnable() {
      @Override
      public void run()
      {
        StringTable.ensureFullyLoaded();
      }
    }).start();
  }

// --------------------- Begin Interface SearchClient ---------------------

  @Override
  public String getText(int index)
  {
    if (index < 0 || index >= StringTable.getNumEntries()) {
      return null;
    }
    return StringTable.getStringRef(index);
  }

  @Override
  public void hitFound(int index)
  {
    strref.setValue(index);
  }

// --------------------- End Interface SearchClient ---------------------
}

