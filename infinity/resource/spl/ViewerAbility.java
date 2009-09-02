// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.resource.Effect;

import javax.swing.*;
import java.awt.*;

final class ViewerAbility extends JPanel
{
  private static JPanel makeFieldPanel(Ability ability)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Ability location"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Target"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Range (feet)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Minimum level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("Casting speed"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute("# charges"), gbl, gbc, true);

    return fieldPanel;
  }

  ViewerAbility(Ability ability)
  {
    JPanel fieldPanel = makeFieldPanel(ability);
    JPanel effectsPanel = ViewerUtil.makeListPanel("Effects", ability, Effect.class,
                                                   "Type");
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)ability.getAttribute("Icon"), 0);

    JPanel mainPanel = new JPanel(new GridLayout(1, 3, 6, 6));
    mainPanel.add(iconPanel);
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
}

