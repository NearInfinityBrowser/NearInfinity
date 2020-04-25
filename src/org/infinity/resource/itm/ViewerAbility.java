// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.itm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.Effect;
import org.infinity.resource.StructEntry;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

final class ViewerAbility extends JPanel
{
  ViewerAbility(Ability ability)
  {
    JPanel fieldPanel = makeFieldPanel(ability);
    JPanel effectsPanel = ViewerUtil.makeListPanel("Effects", ability, Effect.class, EffectType.EFFECT_TYPE);
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)ability.getAttribute(AbstractAbility.ABILITY_ICON), 0);
    JComponent boxPanel1 = ViewerUtil.makeCheckPanel((Flag)ability.getAttribute(AbstractAbility.ABILITY_TYPE_FLAGS), 1);
    JPanel flagPanel = ViewerUtil.makeCheckPanel((Flag)ability.getAttribute(Ability.ITM_ABIL_FLAGS), 1);

    JPanel boxPanel2 = new JPanel(new GridLayout(0, 1, 0, 3));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute(Ability.ITM_ABIL_IS_ARROW), "Yes (1)"));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute(Ability.ITM_ABIL_IS_BOLT), "Yes (1)"));
    boxPanel2.add(ViewerUtil.makeCheckLabel(ability.getAttribute(Ability.ITM_ABIL_IS_BULLET), "Yes (1)"));

    JPanel boxPanel = new JPanel(new BorderLayout(0, 6));
    boxPanel.add(boxPanel1, BorderLayout.NORTH);
    boxPanel.add(flagPanel, BorderLayout.CENTER);
    boxPanel.add(boxPanel2, BorderLayout.SOUTH);

    JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
    leftPanel.add(iconPanel, BorderLayout.NORTH);
    leftPanel.add(boxPanel, BorderLayout.CENTER);

    JPanel mainPanel = new JPanel(new GridLayout(1, 3, 6, 6));
    mainPanel.add(leftPanel);
    mainPanel.add(fieldPanel);
    mainPanel.add(effectsPanel);

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

  private JPanel makeFieldPanel(Ability ability)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    String abilityName = getAbilityName(ability);
    if (abilityName != null) {
      ViewerUtil.addLabelFieldPair(fieldPanel, "Tooltip", abilityName, gbl, gbc, true);
    }
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_TARGET), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_RANGE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(Ability.ITM_ABIL_SPEED), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_HIT_BONUS), gbl, gbc, true);
    StructEntry s1 = ability.getAttribute(AbstractAbility.ABILITY_DICE_SIZE);
    StructEntry s2 = ability.getAttribute(AbstractAbility.ABILITY_DICE_COUNT);
    StructEntry s3 = ability.getAttribute(AbstractAbility.ABILITY_DAMAGE_BONUS);
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    JLabel dlabel = new JLabel("Damage");
    gbl.setConstraints(dlabel, gbc);
    fieldPanel.add(dlabel);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    JLabel tf1 = new JLabel(s2.toString() + 'd' + s1.toString() + '+' + s3.toString());
    tf1.setFont(tf1.getFont().deriveFont(Font.PLAIN));
    gbl.setConstraints(tf1, gbc);
    fieldPanel.add(tf1);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_DAMAGE_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_NUM_CHARGES), gbl, gbc, true);

    return fieldPanel;
  }

  // Returns the ability name (from tooltip.2da), or null if not available.
  private static String getAbilityName(Ability ability)
  {
    String retVal = null;
    Table2da table = Table2daCache.get("tooltip.2da");
    if (table != null) {
      // getting parent item resref
      String resref = ability.getParent().getResourceEntry().getResourceRef();

      // fetching tooltip label for ability, if available
      if (resref != null) {
        int[] strrefs = null;
        for (int row = 0, numRows = table.getRowCount(); row < numRows; row++) {
          String entry = table.get(row, 0);
          if (resref.equalsIgnoreCase(entry)) {
            int numCols = table.getColCount();
            strrefs = new int[numCols - 1];
            for (int col = 1; col < numCols; col++) {
              String value = table.get(row, col);
              int number = -1;
              try { number = Integer.parseInt(value); } catch (NumberFormatException nfe) {}
              strrefs[col - 1] = number;
            }
            break;
          }
        }

        if (strrefs != null) {
          int idx = ability.getName().lastIndexOf(' ');
          if (idx > 0) {
            String value = ability.getName().substring(idx).trim();
            int number = -1;
            try { number = Integer.parseInt(value); } catch (NumberFormatException nfe) {}
            if (number >= 0 && number < strrefs.length) {
              int strref = strrefs[number];
              retVal = StringTable.getStringRef(strref);
            }
          }
        }
      }
    }
    return retVal;
  }
}
