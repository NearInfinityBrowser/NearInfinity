// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.infinity.datatype.Flag;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;

public final class ViewerActor extends JPanel
{
  ViewerActor(Actor actor)
  {
    JTabbedPane tabs = new JTabbedPane();

    JPanel presentPanel = ViewerUtil.makeCheckPanel((Flag)actor.getAttribute(Actor.ARE_ACTOR_PRESENT_AT), 2);
    JPanel flagsPanel = ViewerUtil.makeCheckPanel((Flag)actor.getAttribute(Actor.ARE_ACTOR_FLAGS), 1);
    JPanel difficultyPanel = null;
    if (actor.getParent() != null &&
        actor.getParent().getAttribute(AbstractStruct.COMMON_VERSION).toString().equalsIgnoreCase("V9.1")) {
      difficultyPanel = ViewerUtil.makeCheckPanel((Flag)actor.getAttribute(Actor.ARE_ACTOR_DIFFICULTY), 1);
    }
    JPanel fieldPanel = makeFieldPanel(actor);

    JPanel mainPanel = new JPanel(new GridBagLayout());
    JPanel subPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.insets = new Insets(0, 0, 0, 10);
    gbc.gridheight = 2;
    subPanel.add(fieldPanel, gbc);

    gbc.gridx = 1;
    if (difficultyPanel != null)
      gbc.gridheight = 1;
    gbc.insets.left = 10;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    subPanel.add(flagsPanel, gbc);
    if  (difficultyPanel != null) {
      gbc.gridy = 1;
      gbc.insets.top = 10;
      subPanel.add(difficultyPanel, gbc);
      gbc.gridy = 0;
    }

    gbc.gridx = 2;
    gbc.gridheight = 2;
    gbc.insets.right = gbc.insets.top = 0;
    subPanel.add(presentPanel, gbc);

    mainPanel.add(subPanel);

    tabs.addTab("Actor", mainPanel);
    CreResource cre = (CreResource)actor.getAttribute(Actor.ARE_ACTOR_CRE_FILE);
    if (cre != null) {
      tabs.add("CRE", new org.infinity.resource.cre.Viewer(cre));
    }
    setLayout(new BorderLayout());
    add(tabs, BorderLayout.CENTER);
  }

  private JPanel makeFieldPanel(Actor actor)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_NAME), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_CHARACTER), gbl, gbc, true);

    StructEntry s1 = actor.getAttribute(Actor.ARE_ACTOR_POS_X);
    StructEntry s2 = actor.getAttribute(Actor.ARE_ACTOR_POS_Y);
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

    StructEntry s3 = actor.getAttribute(Actor.ARE_ACTOR_DEST_X);
    StructEntry s4 = actor.getAttribute(Actor.ARE_ACTOR_DEST_Y);
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

    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_ORIENTATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_ANIMATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_OVERRIDE), gbl, gbc, true);
    if (actor.getParent() != null &&
        actor.getParent().getAttribute(AbstractStruct.COMMON_VERSION).toString().equalsIgnoreCase("V9.1")) {
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_SPECIAL_1), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_TEAM), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_SPECIAL_2), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_COMBAT), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_SPECIAL_3), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_MOVEMENT), gbl, gbc, true);
    }
    else {
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_SPECIFICS), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_CLASS), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_RACE), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_GENERAL), gbl, gbc, true);
      ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_SCRIPT_DEFAULT), gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(fieldPanel, actor.getAttribute(Actor.ARE_ACTOR_DIALOG), gbl, gbc, true);

    return fieldPanel;
  }
}
