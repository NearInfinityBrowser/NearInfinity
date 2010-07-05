// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Flag;
import infinity.gui.ViewerUtil;
import infinity.resource.StructEntry;

import javax.swing.*;
import java.awt.*;

final class ViewerActor extends JPanel
{
  ViewerActor(Actor actor)
  {
    JPanel boxPanel = ViewerUtil.makeCheckPanel((Flag)actor.getAttribute("Present at"), 2);
    JPanel fieldPanel = makeFieldPanel(actor);

    JPanel mainPanel = new JPanel(new GridLayout(1, 2, 3, 3));
    mainPanel.add(fieldPanel);
    mainPanel.add(boxPanel);

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

  private JPanel makeFieldPanel(Actor actor)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Character"), gbl, gbc, true);

    StructEntry s1 = actor.getAttribute("Position: X");
    StructEntry s2 = actor.getAttribute("Position: Y");
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Position");
    gbl.setConstraints(dlabel, gbc);
    fieldPanel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel('(' + s1.toString() + ',' + s2.toString() + ')');
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    fieldPanel.add(tf1);

    StructEntry s3 = actor.getAttribute("Destination: X");
    StructEntry s4 = actor.getAttribute("Destination: Y");
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel2 = new JLabel("Destination");
    gbl.setConstraints(dlabel2, gbc);
    fieldPanel.add(dlabel2);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf2 = new JLabel('(' + s3.toString() + ',' + s4.toString() + ')');
    tf2.setFont(tf1.getFont());
    gbl.setConstraints(tf2, gbc);
    fieldPanel.add(tf2);

    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Orientation"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Override script"), gbl, gbc, true);
    if (actor.getSuperStruct() != null &&
        actor.getSuperStruct().getAttribute("Version").toString().equalsIgnoreCase("V9.1")) {
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Special 1 script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Team script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Special 2 script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Combat script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Special 3 script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Movement script"), gbl, gbc, true);
    }
    else {
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Specifics script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Class script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Race script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("General script"), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Default script"), gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute("Dialogue"), gbl, gbc, true);

    return fieldPanel;
  }
}

