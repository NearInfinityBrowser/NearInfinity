// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

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

import org.infinity.datatype.Flag;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.ViewerUtil.ListValueRenderer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;

final class Viewer extends JPanel
{
  private static JPanel makeMiscPanel(GamResource gam)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_WORLD_AREA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_GAME_TIME), gbl, gbc, true);
    StructEntry se = gam.getAttribute(GamResource.GAM_REAL_TIME);
    if (se != null)
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_PARTY_GOLD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, gam.getAttribute(GamResource.GAM_MASTER_AREA), gbl, gbc, true);
    se = gam.getAttribute(GamResource.GAM_WORLDMAP);
    if (se != null)
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);
    se = gam.getAttribute(GamResource.GAM_ZOOM_LEVEL);
    if (se != null)
      ViewerUtil.addLabelFieldPair(panel, se, gbl, gbc, true);

    gbc.insets.top = 10;
    gbc.gridwidth = 2;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    se = gam.getAttribute(GamResource.GAM_WEATHER);
    if (se != null) {
      JPanel weatherPanel = ViewerUtil.makeCheckPanel((Flag)se, 2);
      panel.add(weatherPanel, gbc);
    }

    se = gam.getAttribute(GamResource.GAM_CONFIGURATION);
    if (se != null) {
      JPanel configPanel = ViewerUtil.makeCheckPanel((Flag)se, 2);
      panel.add(configPanel, gbc);
    }

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

    JPanel var1Panel = ViewerUtil.makeListPanel("Variables", gam, Variable.class, Variable.VAR_NAME,
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
      implements ListValueRenderer
  {
    private VariableListRenderer()
    {
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      label.setText(getListValue(value));
      return label;
    }

    @Override
    public String getListValue(Object value)
    {
      if (value instanceof AbstractStruct) {
        AbstractStruct effect = (AbstractStruct)value;
        StructEntry entry1 = effect.getAttribute(effect.getOffset(), false);
        StructEntry entry2 = effect.getAttribute(effect.getOffset() + 40, false);
        return entry1 + " = " + entry2;
      } else if (value != null) {
        return value.toString();
      }
      return "";
    }
  }
}

