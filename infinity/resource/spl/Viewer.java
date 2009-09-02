// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.resource.Effect;

import javax.swing.*;
import java.awt.*;

final class Viewer extends JPanel
{
  private static JPanel makeFieldPanel(SplResource spl)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Spell name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Spell type"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Wizard school"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Priest type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Casting animation"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Primary type (school)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Secondary type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute("Spell level"), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(SplResource spl)
  {
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)spl.getAttribute("Spell icon"), 0);
    JPanel globaleffectsPanel = ViewerUtil.makeListPanel("Global effects", spl,
                                                         Effect.class, "Type");
    JPanel abilitiesPanel = ViewerUtil.makeListPanel("Abilities", spl, Ability.class,
                                                     "Type");
    JPanel descPanel = ViewerUtil.makeTextAreaPanel(spl.getAttribute("Spell description"));
    JPanel fieldPanel = makeFieldPanel(spl);

    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.add(iconPanel, BorderLayout.NORTH);
    infoPanel.add(fieldPanel, BorderLayout.CENTER);

    setLayout(new GridLayout(2, 2, 6, 6));
    add(infoPanel);
    add(globaleffectsPanel);
    add(descPanel);
    add(abilitiesPanel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }
}

