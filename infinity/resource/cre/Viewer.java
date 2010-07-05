// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.gui.ViewerUtil;
import infinity.resource.*;

import javax.swing.*;
import java.awt.*;

public final class Viewer extends JPanel
{
  private static JPanel makeMiscPanel(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Allegiance"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("General"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Race"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Class"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Gender"), gbl, gbc, true);
    if (ResourceFactory.getInstance().resourceExists("KIT.IDS"))
      ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Kit"), gbl, gbc, true);
    else
      ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Mage type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Save vs. death"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Save vs. wand"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Save vs. polymorph"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Save vs. breath"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Save vs. spell"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist fire"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist cold"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist electricity"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist acid"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist magic"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist magic fire"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist magic cold"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist slashing"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist crushing"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist piercing"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Resist missile"), gbl, gbc, true);
    return panel;
  }

  private static JPanel makeMiscPanelIWD2(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 0);

    // 22
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Allegiance"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("General"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Race"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Class"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Specific"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Gender"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Alignment"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Kit"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Challenge rating"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Fortitude save"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Reflex save"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Will save"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("# items"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("# attacks/round"), gbl, gbc, true);

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Script name"), gbl, gbc, true);
//    gbc.insets = new Insets(4, 3, 4, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Override script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Special script 1"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Team script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Special script 2"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Combat script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Special script 3"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Movement script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Dialogue"), gbl, gbc, true);
    return panel;
  }

  public Viewer(CreResource cre)
  {
    JTabbedPane tabs = new JTabbedPane();
    JScrollPane scroll = new JScrollPane(makeMainPanel(cre));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    tabs.addTab("Stats", scroll);
    StructEntry version = cre.getAttribute("Version");
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
    p.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute("Feats (3/3)"), 1), BorderLayout.NORTH);
    p.add(makeSkillPanelIWD2(cre), BorderLayout.CENTER);

    JPanel panel = new JPanel(new GridLayout(1, 3, 6, 0));
    panel.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute("Feats (1/3)"), 1));
    panel.add(ViewerUtil.makeCheckPanel((Flag)cre.getAttribute("Feats (2/3)"), 1));
    panel.add(p);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  private JPanel makeItemSpellsPanel(CreResource cre)
  {
    JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    rightPanel.add(
            ViewerUtil.makeListPanel("Known spells", cre, KnownSpells.class, "Spell"));
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
    StructEntry effectFlag = cre.getAttribute("Effect flag");
    if (effectFlag == null)
      return new JPanel();
    else if (effectFlag.toString().equalsIgnoreCase("1"))
      effectPanel = ViewerUtil.makeListPanel("Effects", cre, Effect2.class, "Type");
    else
      effectPanel = ViewerUtil.makeListPanel("Effects", cre, Effect.class, "Type");
    ResourceRef imageRef = (ResourceRef)cre.getAttribute("Large portrait");
    JComponent imagePanel;
//    if (imageRef.getResourceName().endsWith(".BAM"))
//      imagePanel = ViewerUtil.makeBamPanel(imageRef, 0);
    if (imageRef.getResourceName().endsWith(".BMP") &&
        ResourceFactory.getInstance().resourceExists(imageRef.getResourceName()))
      imagePanel = ViewerUtil.makeImagePanel(imageRef);
    else
      imagePanel = ViewerUtil.makeImagePanel((ResourceRef)cre.getAttribute("Small portrait"));

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
    String version = cre.getAttribute("Version").toString();
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

    gbc.insets = new Insets(1, 3, 2, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Alchemy"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Animal empathy"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Bluff"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Concentration"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Diplomacy"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Disable device"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Hide"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Intimidate"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Knowledge (arcana)"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Move silently"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Open lock"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Pick pocket"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Search"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Spellcraft"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Use magic device"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Wilderness lore"), gbl, gbc, true);
    panel.setBorder(BorderFactory.createTitledBorder("Skills"));
    return panel;
  }

  private JPanel makeStatsPanel(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("XP"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("XP value"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Gold"), gbl, gbc, true);

    StructEntry s1 = cre.getAttribute("Level first class");
    StructEntry s2 = cre.getAttribute("Level second class");
    StructEntry s3 = cre.getAttribute("Level third class");
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

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Effective AC"), gbl, gbc, true);

    s1 = cre.getAttribute("Current HP");
    s2 = cre.getAttribute("Maximum HP");
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

    s1 = cre.getAttribute("Strength");
    s2 = cre.getAttribute("Strength bonus");
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

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Dexterity"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Constitution"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Intelligence"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Wisdom"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Charisma"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("THAC0"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("# attacks"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Hide in shadows"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Detect illusions"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Set traps"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Lore"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Open locks"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Move silently"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Find/disarm traps"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Pickpockets"), gbl, gbc, true);
//    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Tracking"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Script name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Override script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Class script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Race script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("General script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Default script"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Dialogue"), gbl, gbc, true);

    return panel;
  }

  private JPanel makeStatsPanelIWD2(CreResource cre)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(2, 3, 3, 0);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("XP"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("XP value"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Total level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Armor class"), gbl, gbc, true);

    StructEntry s1 = cre.getAttribute("Current HP");
    StructEntry s2 = cre.getAttribute("Maximum HP");
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

    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Strength"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Dexterity"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Constitution"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Intelligence"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Wisdom"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Charisma"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Barbarian level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Bard level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Cleric level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Druid level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Fighter level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Monk level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Paladin level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Ranger level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Rogue level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Sorcerer level"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(panel, cre.getAttribute("Wizard level"), gbl, gbc, true);

    return panel;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class SpellListRendererIWD2 extends DefaultListCellRenderer
  {
    private SpellListRendererIWD2()
    {
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct struct = (AbstractStruct)value;
      label.setText(struct.getName() + " (" + (struct.getRowCount() - 2) + ')');
      return label;
    }
  }
}

