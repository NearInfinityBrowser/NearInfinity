// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Flag;
import infinity.gui.ViewerUtil;

import javax.swing.*;
import java.awt.*;

final class Viewer extends JPanel
{
  private static JPanel makeFieldPanel(AreResource are)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Area north"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Area east"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Area south"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Area west"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("WED resource"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Rain probability"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Snow probability"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Fog probability"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Lightning probability"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute("Area script"), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(AreResource are)
  {
    JPanel boxPanel = ViewerUtil.makeCheckPanel((Flag)are.getAttribute("Location"), 1);
    JPanel fieldPanel = makeFieldPanel(are);
    JPanel actorPanel = ViewerUtil.makeListPanel("Actors", are, Actor.class, "Name");
    JPanel containerPanel = ViewerUtil.makeListPanel("Containers", are,
                                                     Container.class, "Name");
    JPanel doorPanel = ViewerUtil.makeListPanel("Doors", are, Door.class, "Name");
    JPanel itePanel = ViewerUtil.makeListPanel("Points of interest", are,
                                               ITEPoint.class, "Name");

    setLayout(new GridLayout(2, 3, 3, 3));
    add(fieldPanel);
    add(actorPanel);
    add(containerPanel);
    add(boxPanel);
    add(doorPanel);
    add(itePanel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }
}

