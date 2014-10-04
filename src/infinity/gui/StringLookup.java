// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.datatype.StringRef;
import infinity.icon.Icons;
import infinity.search.SearchClient;
import infinity.search.SearchMaster;
import infinity.util.StringResource;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

final class StringLookup extends ChildFrame implements SearchClient
{
  private final StringRef strref;

  StringLookup()
  {
    super("StringRef Lookup");
    setIconImage(Icons.getIcon("Find16.gif").getImage());
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

    setSize(540, 350);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface SearchClient ---------------------

  @Override
  public String getText(int index)
  {
    if (index < 0 || index >= StringResource.getMaxIndex())
      return null;
    return StringResource.getStringRef(index);
  }

  @Override
  public void hitFound(int index)
  {
    strref.setValue(index);
  }

// --------------------- End Interface SearchClient ---------------------
}

