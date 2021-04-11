// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.ViewerUtil.ListValueRenderer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Effect;
import org.infinity.resource.Effect2;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;

public final class Viewer extends JPanel
{
  private static JPanel makeMiscPanel(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 6, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ALLEGIANCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ALIGNMENT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_GENERAL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RACE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CLASS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_GENDER), gbl, gbc, true);
    if (Profile.getGame() != Profile.Game.PSTEE &&
        ResourceFactory.resourceExists("KIT.IDS")) {
      ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_KIT), gbl, gbc, true);
    } else {
      ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MAGE_TYPE), gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RACIAL_ENEMY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ANIMATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MORALE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MORALE_BREAK), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MORALE_RECOVERY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_DEATH), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_WAND), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_POLYMORPH), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_BREATH), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_SPELL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_FIRE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_COLD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_ELECTRICITY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_ACID), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_MAGIC), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_MAGIC_FIRE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_MAGIC_COLD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_SLASHING), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_CRUSHING), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_PIERCING), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RESISTANCE_MISSILE), gbl, gbc, true);
    return panel;
  }

  private static JPanel makeMiscPanelIWD2(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 6, 3, 0);

    // 22
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ALLEGIANCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_GENERAL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_RACE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CLASS), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Specific"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_GENDER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ALIGNMENT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_KIT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ANIMATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CHALLENGE_RATING), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_FORTITUDE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_REFLEX), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SAVE_WILL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_NUM_ITEMS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ATTACKS_PER_ROUND), gbl, gbc, true);

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_NAME), gbl, gbc, true);
//    gbc.insets = new Insets(4, 3, 4, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_OVERRIDE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_SPECIAL_1), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_TEAM), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_SPECIAL_2), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_COMBAT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_SPECIAL_3), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_MOVEMENT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DIALOG), gbl, gbc, true);
    return panel;
  }

  public Viewer(CreResource cre)
  {
    JTabbedPane tabs = new JTabbedPane();
    JScrollPane scroll = new JScrollPane(makeMainPanel(cre));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(32);
    scroll.getHorizontalScrollBar().setUnitIncrement(32);
    tabs.addTab("Stats", scroll);
    StructEntry version = cre.getAttribute(AbstractStruct.COMMON_VERSION);
    if (version.toString().equalsIgnoreCase("V2.2")) {
      tabs.addTab("Feats/Skills", makeFeatsPanel(cre));
      tabs.addTab("Items/Spells", makeItemSpellsPanelIWD2(cre));
    }
    else if (version != null)
      tabs.addTab("Items/Spells", makeItemSpellsPanel(cre));
    setLayout(new BorderLayout());
    add(tabs, BorderLayout.CENTER);
  }

  private JPanel makeFeatsPanel(CreResource cre)
  {
    JPanel p = new JPanel(new BorderLayout());
    p.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute(CreResource.CRE_FEATS_3), 1), BorderLayout.NORTH);
    p.add(makeSkillPanelIWD2(cre), BorderLayout.CENTER);

    JPanel panel = new JPanel(new GridLayout(1, 6, 6, 0));
    panel.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute(CreResource.CRE_FEATS_1), 1));
    panel.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute(CreResource.CRE_FEATS_2), 1));
    panel.add(p);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  private JPanel makeItemSpellsPanel(CreResource cre)
  {
    JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    rightPanel.add(
            ViewerUtil.makeListPanel("Known spells", cre, KnownSpells.class, KnownSpells.CRE_KNOWN_RESREF));
    rightPanel.add(new ViewerSpells(cre));
    JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
    panel.add(new ViewerItems(cre));
    panel.add(rightPanel);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  private JPanel makeItemSpellsPanelIWD2(CreResource cre)
  {
    JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
    panel.add(new ViewerItems(cre));
    panel.add(ViewerUtil.makeListPanel("Spells/abilities (# known)", cre, Iwd2Struct.class, null,
                                       new SpellListRendererIWD2()));
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  private JPanel makeMainPanel(CreResource cre)
  {
    JPanel effectPanel;
    IsNumeric effectVersion = (IsNumeric)cre.getAttribute(CreResource.CRE_EFFECT_VERSION);
    if (effectVersion == null) {
      return new JPanel();
    } else {
      effectPanel = ViewerUtil.makeListPanel("Effects", cre,
                                            (effectVersion.getValue() == 1) ? Effect2.class : Effect.class,
                                            EffectType.EFFECT_TYPE);
    }
    ResourceRef imageRef = (ResourceRef)cre.getAttribute(CreResource.CRE_PORTRAIT_LARGE);
    JComponent imagePanel;
    if (ResourceFactory.resourceExists(imageRef.getResourceName(), true)) {
      imagePanel = ViewerUtil.makeImagePanel(imageRef, true);
    } else {
      imagePanel = ViewerUtil.makeImagePanel((ResourceRef)cre.getAttribute(CreResource.CRE_PORTRAIT_SMALL), true);
    }

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel leftPanel = new JPanel(gbl);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(3, 0, 3, 0);
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(imagePanel, gbc);
    leftPanel.add(imagePanel);
    gbc.weighty = 1.0;
    gbl.setConstraints(effectPanel, gbc);
    leftPanel.add(effectPanel);

    JPanel panel = new JPanel(new GridLayout(1, 3));
    panel.add(leftPanel);
    String version = cre.getAttribute(AbstractStruct.COMMON_VERSION).toString();
    if (version.equalsIgnoreCase("V2.2")) {
      panel.add(makeStatsPanelIWD2(cre));
      panel.add(makeMiscPanelIWD2(cre));
    }
    else {
      panel.add(makeStatsPanel(cre));
      panel.add(makeMiscPanel(cre));
    }
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  private JPanel makeSkillPanelIWD2(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(1, 6, 2, 0);
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ALCHEMY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ANIMAL_EMPATHY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_BLUFF), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CONCENTRATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DIPLOMACY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DISABLE_DEVICE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_HIDE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_INTIMIDATE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_KNOWLEDGE_ARCANA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MOVE_SILENTLY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_OPEN_LOCKS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_PICK_POCKETS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SEARCH), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SPELLCRAFT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_USE_MAGIC_DEVICE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_WILDERNESS_LORE), gbl, gbc, true);
    panel.setBorder(BorderFactory.createTitledBorder("Skills"));
    return panel;
  }

  private JPanel makeStatsPanel(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 6, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_NAME), gbl, gbc, true, 100);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_XP), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_XP_VALUE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_GOLD), gbl, gbc, true);

    StructEntry s1 = cre.getAttribute(CreResource.CRE_LEVEL_FIRST_CLASS);
    StructEntry s2 = cre.getAttribute(CreResource.CRE_LEVEL_SECOND_CLASS);
    StructEntry s3 = cre.getAttribute(CreResource.CRE_LEVEL_THIRD_CLASS);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Level");
    gbl.setConstraints(dlabel, gbc);
    panel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel(s1.toString() + '/' + s2.toString() + '/' + s3.toString());
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    panel.add(tf1);

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_AC_EFFECTIVE), gbl, gbc, true);

    s1 = cre.getAttribute(CreResource.CRE_HP_CURRENT);
    s2 = cre.getAttribute(CreResource.CRE_HP_MAX);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel1 = new JLabel("Hit points");
    gbl.setConstraints(dlabel1, gbc);
    panel.add(dlabel1);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf2 = new JLabel(s1.toString() + '/' + s2.toString());
    tf2.setFont(tf1.getFont());
    gbl.setConstraints(tf2, gbc);
    panel.add(tf2);

    s1 = cre.getAttribute(CreResource.CRE_STRENGTH);
    s2 = cre.getAttribute(CreResource.CRE_STRENGTH_BONUS);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel2 = new JLabel("Strength");
    gbl.setConstraints(dlabel2, gbc);
    panel.add(dlabel2);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf3 = new JLabel(s1.toString() + '/' + s2.toString());
    tf3.setFont(tf2.getFont());
    gbl.setConstraints(tf3, gbc);
    panel.add(tf3);

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DEXTERITY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CONSTITUTION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_INTELLIGENCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_WISDOM), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CHARISMA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_THAC0), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ATTACKS_PER_ROUND), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_HIDE_IN_SHADOWS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DETECT_ILLUSION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SET_TRAPS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LORE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_OPEN_LOCKS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_MOVE_SILENTLY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_FIND_TRAPS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_PICK_POCKETS), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Tracking"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_NAME), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_OVERRIDE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_CLASS), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_RACE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_GENERAL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_SCRIPT_DEFAULT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DIALOG), gbl, gbc, true);

    return panel;
  }

  private JPanel makeStatsPanelIWD2(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 6, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_NAME), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_XP), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_XP_VALUE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVELS_TOTAL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_ARMOR_CLASS), gbl, gbc, true);

    StructEntry s1 = cre.getAttribute(CreResource.CRE_HP_CURRENT);
    StructEntry s2 = cre.getAttribute(CreResource.CRE_HP_MAX);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Hit points");
    gbl.setConstraints(dlabel, gbc);
    panel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel(s1.toString() + '/' + s2.toString());
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    panel.add(tf1);

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_STRENGTH), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_DEXTERITY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CONSTITUTION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_INTELLIGENCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_WISDOM), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_CHARISMA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_BARBARIAN), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_BARD), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_CLERIC), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_DRUID), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_FIGHTER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_MONK), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_PALADIN), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_RANGER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_ROGUE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_SORCERER), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute(CreResource.CRE_LEVEL_WIZARD), gbl, gbc, true);

    return panel;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class SpellListRendererIWD2 extends DefaultListCellRenderer
      implements ListValueRenderer
  {
    private SpellListRendererIWD2()
    {
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      label.setText(getListValue(value));
      return label;
    }

    @Override
    public String getListValue(Object value)
    {
      if (value instanceof AbstractStruct) {
        AbstractStruct struct = (AbstractStruct)value;
        return struct.getName() + " (" + (struct.getFields().size() - 2) + ')';
      }
      if (value != null) {
        return value.toString();
      }
      return "";
    }
  }
}
