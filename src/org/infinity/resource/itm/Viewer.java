// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.itm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.nio.ByteBuffer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.Effect;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

final class Viewer extends JPanel {
  Viewer(ItmResource itm) {
    // row 0, column 0
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef) itm.getAttribute(ItmResource.ITM_ICON), 1, 1);
    JComponent groundIconPanel = ViewerUtil.makeBamPanel((ResourceRef) itm.getAttribute(ItmResource.ITM_ICON_GROUND), 1);
    JPanel iconsPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    iconsPanel.add(iconPanel);
    iconsPanel.add(groundIconPanel);

    JPanel flagsPanel = ViewerUtil.makeCheckPanel((Flag) itm.getAttribute(ItmResource.ITM_FLAGS), 1);

    JPanel propertiesPanel = makeFieldPanel(itm);

    JPanel iconsFlagsPanel = new JPanel(new BorderLayout(3, 0));
    iconsFlagsPanel.add(iconsPanel, BorderLayout.CENTER);
    iconsFlagsPanel.add(flagsPanel, BorderLayout.WEST);

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(propertiesPanel, BorderLayout.NORTH);
    leftPanel.add(iconsFlagsPanel, BorderLayout.CENTER);

    JScrollPane scrollPane = new JScrollPane(leftPanel);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setPreferredSize(scrollPane.getMinimumSize());

    // row 0, column 1
    StructEntry descGeneral = itm.getAttribute(ItmResource.ITM_DESCRIPTION_GENERAL);
    StructEntry descIdentified = itm.getAttribute(ItmResource.ITM_DESCRIPTION_IDENTIFIED);
    JTabbedPane tabbedDescPanel = new JTabbedPane(SwingConstants.TOP);
    tabbedDescPanel.addTab(descGeneral.getName(), ViewerUtil.makeTextAreaPanel(descGeneral, false));
    tabbedDescPanel.setEnabledAt(0, StringTable.isValidStringRef(((IsNumeric) descGeneral).getValue()));
    tabbedDescPanel.addTab(descIdentified.getName(), ViewerUtil.makeTextAreaPanel(descIdentified, false));
    tabbedDescPanel.setEnabledAt(1, StringTable.isValidStringRef(((IsNumeric) descIdentified).getValue()));
    if (tabbedDescPanel.isEnabledAt(1)) {
      tabbedDescPanel.setSelectedIndex(1);
    }

    JPanel abilitiesPanel = ViewerUtil.makeListPanel("Abilities", itm, Ability.class, AbstractAbility.ABILITY_TYPE);
    JPanel globaleffectsPanel = ViewerUtil.makeListPanel("Global effects", itm, Effect.class, EffectType.EFFECT_TYPE);

    JPanel abilitiesEffectsPanel = new JPanel(new GridLayout(1, 2, 6, 3));
    abilitiesEffectsPanel.add(abilitiesPanel);
    abilitiesEffectsPanel.add(globaleffectsPanel);

    JPanel rightPanel = new JPanel(new GridLayout(2, 1, 6, 6));
    rightPanel.add(tabbedDescPanel);
    rightPanel.add(abilitiesEffectsPanel);

    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    setLayout(new GridLayout(1, 2, 4, 4));
    add(scrollPane);
    add(rightPanel);
  }

  private JPanel makeFieldPanel(ItmResource itm) {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_NAME_GENERAL), gbl, gbc, true, 100);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_NAME_IDENTIFIED), gbl, gbc, true, 100);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_CATEGORY), gbl, gbc, true);
    StructEntry s1 = itm.getAttribute(ItmResource.ITM_MIN_STRENGTH);
    StructEntry s2 = itm.getAttribute(ItmResource.ITM_MIN_STRENGTH_BONUS);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Minimum strength");
    gbl.setConstraints(dlabel, gbc);
    fieldPanel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel(s1.toString() + '/' + s2.toString());
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    fieldPanel.add(tf1);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_MIN_DEXTERITY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_MIN_CONSTITUTION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_MIN_INTELLIGENCE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_MIN_WISDOM), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_MIN_CHARISMA), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_PRICE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_LORE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_ENCHANTMENT), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_WEIGHT), gbl, gbc, true);
    if (Profile.getEngine() == Profile.Engine.PST) {
      if (((Flag) itm.getAttribute(ItmResource.ITM_FLAGS)).isFlagSet(11)) {
        ViewerUtil.addLabelFieldPair(fieldPanel, itm.getAttribute(ItmResource.ITM_DIALOG), gbl, gbc, true);
      }
    } else if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      String resref = getItemDialog(itm);
      if (resref != null) {
        ViewerUtil.addLabelFieldPair(fieldPanel,
            new ResourceRef(ByteBuffer.wrap(resref.getBytes()), 0, ItmResource.ITM_DIALOG, "DLG"), gbl, gbc, true);
      }
    }
    return fieldPanel;
  }

  // Returns the associated dialog resref (from itemdial.2da), or null if not available.
  private static String getItemDialog(ItmResource itm) {
    String retVal = null;
    Table2da table = Table2daCache.get("itemdial.2da");
    if (table != null && table.getColCount() > 2) {
      // getting item resref
      String resref = itm.getResourceEntry().getResourceRef();

      // fetching item dialog file, if available
      if (resref != null) {
        for (int row = 0, numRows = table.getRowCount(); row < numRows; row++) {
          String entry = table.get(row, 0);
          if (resref.equalsIgnoreCase(entry)) {
            retVal = table.get(row, 2);
            break;
          }
        }
      }
    }
    return retVal;
  }
}
