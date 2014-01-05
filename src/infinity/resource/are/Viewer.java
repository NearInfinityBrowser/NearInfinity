// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Flag;
import infinity.gui.ViewerUtil;
import infinity.icon.Icons;
import infinity.resource.are.viewer.AreaViewer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

final class Viewer extends JPanel implements ActionListener
{
  private static final String CMD_VIEWAREA = "ViewArea";

  private final AreResource are;

  private JPanel makeFieldPanel()
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldBasePanel = new JPanel(new BorderLayout());
    JPanel fieldPanel = new JPanel(gbl);
    fieldBasePanel.add(fieldPanel, BorderLayout.CENTER);

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

    JButton bView = new JButton("View Area (Experimental)", Icons.getIcon("Volume16.gif"));
    bView.setActionCommand(CMD_VIEWAREA);
    bView.addActionListener(this);
    bView.setEnabled(AreaViewer.IsValid(are));
    fieldBasePanel.add(bView, BorderLayout.SOUTH);

    return fieldBasePanel;
  }

  Viewer(AreResource are)
  {
    this.are = are;

    JPanel boxPanel = ViewerUtil.makeCheckPanel((Flag)are.getAttribute("Location"), 1);
    JPanel fieldPanel = makeFieldPanel();
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

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getActionCommand().equals(CMD_VIEWAREA)) {
      new AreaViewer(this, are);
    }
  }

//--------------------- End Interface ActionListener ---------------------
}

