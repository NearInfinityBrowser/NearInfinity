// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Viewer;

final class ViewerNPC extends JPanel {
  ViewerNPC(PartyNPC npc) {
    JTabbedPane tabs = new JTabbedPane();
    JScrollPane scroll = new JScrollPane(makeStatsPanel(npc));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    tabs.addTab("Game stats", scroll);
    CreResource cre = (CreResource) npc.getAttribute(PartyNPC.GAM_NPC_CRE_RESOURCE);
    if (cre != null) {
      tabs.add("CRE", new Viewer(cre));
    }
    setLayout(new BorderLayout());
    add(tabs, BorderLayout.CENTER);
  }

  private JPanel makeStatsPanel(PartyNPC npc) {
    JPanel panel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    panel.setLayout(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    if (npc.getAttribute(PartyNPC.GAM_NPC_NAME) != null) {
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_NAME), gbl, gbc, true, 100);
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_CURRENT_AREA), gbl, gbc, true);

      StructEntry s1 = npc.getAttribute(PartyNPC.GAM_NPC_LOCATION_X);
      StructEntry s2 = npc.getAttribute(PartyNPC.GAM_NPC_LOCATION_Y);
      gbc.weightx = 0.0;
      gbc.fill = GridBagConstraints.NONE;
      gbc.gridwidth = 1;
      JLabel dlabel = new JLabel("Location");
      gbl.setConstraints(dlabel, gbc);
      panel.add(dlabel);
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      JLabel tf1 = new JLabel('(' + s1.toString() + ',' + s2.toString() + ')');
      tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
      gbl.setConstraints(tf1, gbc);
      panel.add(tf1);
    }
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_SELECTION_STATE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_PARTY_POSITION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_FOE_VANQUISHED), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_XP_FOE_VANQUISHED), gbl, gbc, true, 100);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_KILLS_XP_CHAPTER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_KILLS_XP_GAME), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_NUM_KILLS_CHAPTER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(PartyNPC.GAM_NPC_STAT_NUM_KILLS_GAME), gbl, gbc, true);
    for (int i = 1; i < 4; i++) {
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(String.format(PartyNPC.GAM_NPC_QUICK_SPELL_FMT, i)), gbl,
          gbc, true);
    }
    for (int i = 1; i < 5; i++) {
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(String.format(PartyNPC.GAM_NPC_STAT_FAV_SPELL_FMT, i)), gbl,
          gbc, true);
    }
    for (int i = 1; i < 5; i++) {
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute(String.format(PartyNPC.GAM_NPC_STAT_FAV_WEAPON_FMT, i)), gbl,
          gbc, true);
    }
    return panel;
  }
}
