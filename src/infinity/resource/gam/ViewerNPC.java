// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.gui.ViewerUtil;
import infinity.resource.cre.CreResource;
import infinity.resource.cre.Viewer;
import infinity.resource.StructEntry;

import javax.swing.*;
import java.awt.*;

final class ViewerNPC extends JPanel
{
  ViewerNPC(PartyNPC npc)
  {
    JTabbedPane tabs = new JTabbedPane();
    JScrollPane scroll = new JScrollPane(makeStatsPanel(npc));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    tabs.addTab("Game stats", scroll);
    CreResource cre = (CreResource)npc.getAttribute("CRE file");
    if (cre != null)
      tabs.add("CRE", new Viewer(cre));
    setLayout(new BorderLayout());
    add(tabs, BorderLayout.CENTER);
  }

  private JPanel makeStatsPanel(PartyNPC npc)
  {
    JPanel panel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    panel.setLayout(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    if (npc.getAttribute("Name") != null) {
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Name"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Current area"), gbl, gbc, true);

      StructEntry s1 = npc.getAttribute("Location: X");
      StructEntry s2 = npc.getAttribute("Location: Y");
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
//    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Happyness"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Selection state"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Party position"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Most powerful foe vanquished"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("XP for most powerful foe"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Kill XP (chapter)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Kill XP (game)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("# kills (chapter)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("# kills (game)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Quick spell 1"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Quick spell 2"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Quick spell 3"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite spell 1"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite spell 2"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite spell 3"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite spell 4"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite weapon 1"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite weapon 2"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite weapon 3"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, npc.getAttribute("Favorite weapon 4"), gbl, gbc, true);
    return panel;
  }
}

