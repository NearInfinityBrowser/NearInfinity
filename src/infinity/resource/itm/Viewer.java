// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.itm;

import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.resource.Effect;
import infinity.resource.StructEntry;

import javax.swing.*;
import java.awt.*;

final class Viewer extends JPanel
{
  Viewer(ItmResource itm)
  {
    JComponent iconPanel1 = ViewerUtil.makeBamPanel((ResourceRef)itm.getAttribute("Icon"), 0);
    JComponent iconPanel2 = ViewerUtil.makeBamPanel((ResourceRef)itm.getAttribute("Ground icon"), 0);
    JPanel globaleffectsPanel = ViewerUtil.makeListPanel("Global effects", itm,
                                                         Effect.class, "Type");
    JPanel abilitiesPanel = ViewerUtil.makeListPanel("Abilities", itm, Ability.class,
                                                     "Type");
    JPanel fieldPanel = makeFieldPanel(itm);
    JPanel boxPanel = ViewerUtil.makeCheckPanel((Flag)itm.getAttribute("Flags"), 1);
    StructEntry desc = itm.getAttribute("Identified description");
    if (desc.toString().equalsIgnoreCase("No such index"))
      desc = itm.getAttribute("General description");
    JPanel descPanel = ViewerUtil.makeTextAreaPanel(desc);

    JPanel iconPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    iconPanel.add(iconPanel1);
    iconPanel.add(iconPanel2);

    JPanel panel1 = new JPanel(new BorderLayout(3, 0));
    panel1.add(iconPanel, BorderLayout.CENTER);
    panel1.add(boxPanel, BorderLayout.WEST);

    JPanel panel2 = new JPanel(new GridLayout(1, 2, 6, 3));
    panel2.add(abilitiesPanel);
    panel2.add(globaleffectsPanel);

    JPanel panel3 = new JPanel(new GridLayout(2, 1, 6, 6));
    panel3.add(descPanel);
    panel3.add(panel2);

    JPanel panel4 = new JPanel(new BorderLayout());
    panel4.add(fieldPanel, BorderLayout.NORTH);
    panel4.add(panel1, BorderLayout.CENTER);

    setLayout(new GridLayout(1, 2, 3, 3));
    add(panel4);
    add(panel3);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }

  private JPanel makeFieldPanel(ItmResource itm)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("General name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Identified name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Category"), gbl, gbc, true);
//    if (!itm.getAttribute("Version").toString().equalsIgnoreCase("V1.1")) {
      StructEntry s1 = itm.getAttribute("Minimum strength");
      StructEntry s2 = itm.getAttribute("Minimum strength bonus");
      gbc.weightx = 0.0;
      gbc.fill = GridBagConstraints.NONE;
      gbc.gridwidth = 1;
      JLabel dlabel = new JLabel("Minimum strength");
      gbl.setConstraints(dlabel, gbc);
      fieldPanel.add(dlabel);
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      JLabel tf1 = new JLabel(s1.toString() + '/' + s2.toString());
      tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
      gbl.setConstraints(tf1, gbc);
      fieldPanel.add(tf1);
      ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Minimum dexterity"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Minimum constitution"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Minimum intelligence"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Minimum wisdom"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Minimum charisma"), gbl, gbc, true);
//    }
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Price"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Lore to identify"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Enchantment"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute("Weight"), gbl, gbc, true);
    return fieldPanel;
  }
}

