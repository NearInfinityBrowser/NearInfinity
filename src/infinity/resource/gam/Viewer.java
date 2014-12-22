// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.gui.ViewerUtil;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

final class Viewer extends JPanel
{
  private static JPanel makeMiscPanel(GamResource gam)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute("Current area"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute("Game time (game seconds)"), gbl, gbc, true);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.isEnhancedEdition()) // V2.0 - better check?
      ViewerUtil.addLabelFieldPair(panel, gam.getAttribute("Game time (real seconds)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute("Party gold"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute("Master area"), gbl, gbc, true);
    return panel;
  }

  Viewer(GamResource gam)
  {
    JPanel stats1Panel, stats2Panel;
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG1 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG1TOTSC) {
      stats1Panel =
      ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class, null);
      stats2Panel =
      ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, null);
    }
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      stats1Panel =
      ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class, "Name");
      stats2Panel =
      ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, "Name");
    }
    else {
      stats1Panel =
      ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class,
                               "Character");
      stats2Panel =
      ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, "Character");
    }

    JPanel var1Panel = ViewerUtil.makeListPanel("Variables", gam, Variable.class, "Name",
                                                new VariableListRenderer());

    setLayout(new GridLayout(2, 3, 3, 3));
    add(makeMiscPanel(gam));
    add(stats2Panel);
    add(var1Panel);
    add(stats1Panel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class VariableListRenderer extends DefaultListCellRenderer
  {
    private VariableListRenderer()
    {
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct effect = (AbstractStruct)value;
      StructEntry entry1 = effect.getAttribute("Name");
      StructEntry entry2 = effect.getAttribute("Value");
      label.setText(entry1 + " = " + entry2);
      return label;
    }
  }
}

