// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.infinity.datatype.Flag;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.StructEntry;

public final class ViewerContainer extends JPanel
{
  ViewerContainer(Container container)
  {
    JPanel fieldPanel = makeFieldPanel(container);
    JPanel itemPanel = ViewerUtil.makeListPanel("Items", container, Item.class, Item.ARE_ITEM_RESREF);

    JPanel mainPanel = new JPanel(new GridLayout(1, 2, 16, 3));
    mainPanel.add(fieldPanel);
    mainPanel.add(itemPanel);
    setLayout(new GridBagLayout());
    add(mainPanel);
  }

  private JPanel makeFieldPanel(Container container)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_NAME),
                                 gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_TYPE),
                                 gbl, gbc, true);

    StructEntry s1 = container.getAttribute(Container.ARE_CONTAINER_LOCATION_X);
    StructEntry s2 = container.getAttribute(Container.ARE_CONTAINER_LOCATION_Y);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Location");
    gbl.setConstraints(dlabel, gbc);
    fieldPanel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel('(' + s1.toString() + ',' + s2.toString() + ')');
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    fieldPanel.add(tf1);

    StructEntry s3 = container.getAttribute(Container.ARE_CONTAINER_LAUNCH_POINT_X);
    StructEntry s4 = container.getAttribute(Container.ARE_CONTAINER_LAUNCH_POINT_Y);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel2 = new JLabel("Launch point");
    gbl.setConstraints(dlabel2, gbc);
    fieldPanel.add(dlabel2);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf2 = new JLabel('(' + s3.toString() + ',' + s4.toString() + ')');
    tf2.setFont(tf1.getFont());
    gbl.setConstraints(tf2, gbc);
    fieldPanel.add(tf2);

    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_LOCK_DIFFICULTY),
                                 gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_TRAP_DETECTION_DIFFICULTY),
                                 gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_TRAP_REMOVAL_DIFFICULTY),
                                 gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_SCRIPT_TRAP), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute(Container.ARE_CONTAINER_KEY), gbl, gbc, true);

    JComponent check1 = ViewerUtil.makeCheckPanel((Flag)container.getAttribute(Container.ARE_CONTAINER_FLAGS), 1);
    JComponent check2 = ViewerUtil.makeCheckLabel(container.getAttribute(Container.ARE_CONTAINER_TRAPPED),
                                                  "Yes (1)");
    JComponent check3 = ViewerUtil.makeCheckLabel(container.getAttribute(Container.ARE_CONTAINER_TRAP_DETECTED),
                                                  "Yes (1)");
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(check1, gbc);
    fieldPanel.add(check1);
    gbl.setConstraints(check2, gbc);
    fieldPanel.add(check2);
    gbl.setConstraints(check3, gbc);
    fieldPanel.add(check3);

    return fieldPanel;
  }
}

