// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

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

import org.infinity.datatype.Flag;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.are.viewer.AreaViewer;

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

    gbc.insets = new Insets(4, 4, 4, 16);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_NORTH), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_EAST), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_SOUTH), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_WEST), gbl, gbc, true, 80);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_WED_RESOURCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_RAIN), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_SNOW), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_FOG), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_PROBABILITY_LIGHTNING), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, are.getAttribute(AreResource.ARE_AREA_SCRIPT), gbl, gbc, true);

    JButton bView = new JButton("View Area", Icons.ICON_VOLUME_16.getIcon());
    bView.setActionCommand(CMD_VIEWAREA);
    bView.addActionListener(this);
    bView.setEnabled(AreaViewer.isValid(are));
    fieldBasePanel.add(bView, BorderLayout.SOUTH);

    return fieldBasePanel;
  }

  Viewer(AreResource are)
  {
    this.are = are;

    JPanel boxPanel = ViewerUtil.makeCheckPanel((Flag)are.getAttribute(AreResource.ARE_LOCATION), 1);
    JPanel fieldPanel = makeFieldPanel();
    JPanel actorPanel = ViewerUtil.makeListPanel("Actors", are, Actor.class, Actor.ARE_ACTOR_NAME);
    JPanel containerPanel = ViewerUtil.makeListPanel("Containers", are,
                                                     Container.class, Container.ARE_CONTAINER_NAME);
    JPanel doorPanel = ViewerUtil.makeListPanel("Doors", are, Door.class, Door.ARE_DOOR_NAME);
    JPanel itePanel = ViewerUtil.makeListPanel("Points of interest", are,
                                               ITEPoint.class, ITEPoint.ARE_TRIGGER_NAME);

    JPanel pLeft = new JPanel(new GridLayout(2, 1, 4, 4));
    pLeft.add(fieldPanel);
    pLeft.add(boxPanel);

    JPanel pRight = new JPanel(new GridLayout(2, 2, 4, 4));
    pRight.add(actorPanel);
    pRight.add(containerPanel);
    pRight.add(doorPanel);
    pRight.add(itePanel);

    setLayout(new BorderLayout(4, 0));
    add(pLeft, BorderLayout.WEST);
    add(pRight, BorderLayout.CENTER);
    setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getActionCommand().equals(CMD_VIEWAREA)) {
      are.showAreaViewer(this);
    }
  }

//--------------------- End Interface ActionListener ---------------------
}

