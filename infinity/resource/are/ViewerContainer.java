// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.gui.ViewerUtil;
import infinity.resource.StructEntry;
import infinity.datatype.Flag;

import javax.swing.*;
import java.awt.*;

final class ViewerContainer extends JPanel
{
  ViewerContainer(Container container)
  {
    JPanel fieldPanel = makeFieldPanel(container);
    JPanel itemPanel = ViewerUtil.makeListPanel("Items", container, Item.class,
                                                "Item");

    JPanel mainPanel = new JPanel(new GridLayout(1, 2, 3, 3));
    mainPanel.add(fieldPanel);
    mainPanel.add(itemPanel);

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

  private JPanel makeFieldPanel(Container container)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Type"), gbl, gbc, true);

    StructEntry s1 = container.getAttribute("Location: X");
    StructEntry s2 = container.getAttribute("Location: Y");
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

    StructEntry s3 = container.getAttribute("Launch point: X");
    StructEntry s4 = container.getAttribute("Launch point: Y");
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

    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Lock difficulty"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Trap detection difficulty"), gbl, gbc,
                                 true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Trap removal difficulty"), gbl, gbc,
                                 true);
    ViewerUtil.addLabelFieldPair(fieldPanel, container.getAttribute("Key"), gbl, gbc, true);

    JComponent check1 = ViewerUtil.makeCheckPanel((Flag)container.getAttribute("Flags"), 1);
    JComponent check2 = ViewerUtil.makeCheckLabel(container.getAttribute("Is trapped?"), "Yes (1)");
    JComponent check3 = ViewerUtil.makeCheckLabel(container.getAttribute("Is trap detected?"), "Yes (1)");
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

