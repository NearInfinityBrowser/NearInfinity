// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;

final class ViewerArea extends JPanel implements ActionListener
{
  private final JButton bOpen = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
  private JList<Object> list;

  private static JPanel makeInfoPanel(AreaEntry areaEntry)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute(AreaEntry.WMP_AREA_NAME), gbl, gbc, true, 100);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute(AreaEntry.WMP_AREA_CURRENT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, areaEntry.getAttribute(AreaEntry.WMP_AREA_LOADING_IMAGE), gbl, gbc, true);
    return panel;
  }

  ViewerArea(AreaEntry areaEntry)
  {
    JPanel flagPanel = ViewerUtil.makeCheckPanel((Flag)areaEntry.getAttribute(AreaEntry.WMP_AREA_FLAGS), 1);
    JPanel infoPane = makeInfoPanel(areaEntry);
    JComponent icon = ViewerUtil.makeBamPanel(
            (ResourceRef)areaEntry.getParent().getAttribute(MapEntry.WMP_MAP_ICONS),
            ((IsNumeric)areaEntry.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX)).getValue(),
            0);
    JPanel linkPanelN = ViewerUtil.makeListPanel("North links", areaEntry, AreaLinkNorth.class, AreaLink.WMP_LINK_TARGET_ENTRANCE);
    JPanel linkPanelS = ViewerUtil.makeListPanel("South links", areaEntry, AreaLinkSouth.class, AreaLink.WMP_LINK_TARGET_ENTRANCE);
    JPanel linkPanelW = ViewerUtil.makeListPanel("West links", areaEntry, AreaLinkWest.class, AreaLink.WMP_LINK_TARGET_ENTRANCE);
    JPanel linkPanelE = ViewerUtil.makeListPanel("East links", areaEntry, AreaLinkEast.class, AreaLink.WMP_LINK_TARGET_ENTRANCE);

    JPanel linkPanel = new JPanel(new GridLayout(2,2,6,6));
    linkPanel.add(linkPanelN);
    linkPanel.add(linkPanelE);
    linkPanel.add(linkPanelS);
    linkPanel.add(linkPanelW);

    JPanel leftPanel = new JPanel(new BorderLayout(0, 3));
    leftPanel.add(icon, BorderLayout.NORTH);
    leftPanel.add(infoPane, BorderLayout.CENTER);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets.left = 3;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbl.setConstraints(leftPanel, gbc);
    add(leftPanel);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(flagPanel, gbc);
    add(flagPanel);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbl.setConstraints(linkPanel, gbc);
    add(linkPanel);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen)
      new ViewFrame(getTopLevelAncestor(), (Viewable)list.getSelectedValue());
  }

// --------------------- End Interface ActionListener ---------------------
}
