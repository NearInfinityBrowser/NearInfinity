// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.gui.ViewerUtil;
import infinity.resource.AbstractStruct;
import infinity.resource.Profile;
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
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_CURRENT_AREA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_GAME_TIME), gbl, gbc, true);
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) { // V2.0 - better check?
      ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_REAL_TIME), gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_PARTY_GOLD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_MASTER_AREA), gbl, gbc, true);
    return panel;
  }

  Viewer(GamResource gam)
  {
    JPanel stats1Panel, stats2Panel;
    if (Profile.getEngine() == Profile.Engine.PST || Profile.getEngine() == Profile.Engine.BG1) {
      stats1Panel =
      ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class, null);
      stats2Panel =
      ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, null);
    } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
      stats1Panel =
      ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class, NonPartyNPC.GAM_NPC_NAME);
      stats2Panel =
      ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, PartyNPC.GAM_NPC_NAME);
    } else {
      stats1Panel = ViewerUtil.makeListPanel("Non-player characters", gam, NonPartyNPC.class,
                                             NonPartyNPC.GAM_NPC_CHARACTER);
      stats2Panel = ViewerUtil.makeListPanel("Player characters", gam, PartyNPC.class, PartyNPC.GAM_NPC_CHARACTER);
    }

    JPanel var1Panel = ViewerUtil.makeListPanel("Variables", gam, Variable.class, Variable.GAM_VAR_NAME,
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
      StructEntry entry1 = effect.getAttribute(effect.getOffset(), false);
      StructEntry entry2 = effect.getAttribute(effect.getOffset() + 40, false);
      label.setText(entry1 + " = " + entry2);
      return label;
    }
  }
}

