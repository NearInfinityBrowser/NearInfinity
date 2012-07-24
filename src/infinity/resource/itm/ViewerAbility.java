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

final class ViewerAbility extends JPanel
{
  ViewerAbility(Ability ability)
  {
    JPanel fieldPanel = makeFieldPanel(ability);
    JPanel effectsPanel = ViewerUtil.makeListPanel("Effects", ability, Effect.class,
                                                   "Type");
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)ability.getAttribute("Icon"), 0);
    JComponent boxPanel1 = ViewerUtil.makeCheckLabel(ability.getAttribute("Identify to use?"), "Yes (1)");
    JPanel flagPanel = ViewerUtil.makeCheckPanel((Flag)ability.getAttribute("Flags"), 1);

    JPanel boxPanel2 = new JPanel(new GridLayout(0, 1, 0, 3));
//    boxPanel.add(ViewerUtil.makeCheckLabel(ability.getAttribute("Identify to use?"), "Yes (1)"));
//    boxPanel.add(ViewerUtil.makeCheckLabel(ability.getAttribute("Allow strength bonus?"), "Yes (1)"));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute("Is arrow?"), "Yes (1)"));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute("Is bolt?"), "Yes (1)"));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute("Is bullet?"), "Yes (1)"));

    JPanel boxPanel = new JPanel(new BorderLayout(0, 6));
    boxPanel.add(boxPanel1, BorderLayout.NORTH);
    boxPanel.add(flagPanel, BorderLayout.CENTER);
    boxPanel.add(boxPanel2, BorderLayout.SOUTH);

    JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
    leftPanel.add(iconPanel, BorderLayout.NORTH);
    leftPanel.add(boxPanel, BorderLayout.CENTER);

    JPanel mainPanel = new JPanel(new GridLayout(1, 3, 6, 6));
    mainPanel.add(leftPanel);
    mainPanel.add(fieldPanel);
    mainPanel.add(effectsPanel);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbl.setConstraints(mainPanel, gbc);
    add(mainPanel);
  }

  private JPanel makeFieldPanel(Ability ability)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Target"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Range (feet)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Speed"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Bonus to hit"), gbl, gbc, true);
    StructEntry s1 = ability.getAttribute("Dice size");
    StructEntry s2 = ability.getAttribute("# dice thrown");
    StructEntry s3 = ability.getAttribute("Damage bonus");
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Damage");
    gbl.setConstraints(dlabel, gbc);
    fieldPanel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel(s2.toString() + 'd' + s1.toString() + '+' + s3.toString());
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    fieldPanel.add(tf1);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Damage type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("# charges"), gbl, gbc, true);

    return fieldPanel;
  }
}

