// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.spl;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.Effect;

final class ViewerAbility extends JPanel
{
  private static JPanel makeFieldPanel(Ability ability)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_LOCATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_TARGET), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_RANGE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(Ability.SPL_ABIL_MIN_LEVEL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(Ability.SPL_ABIL_CASTING_SPEED), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, ability.getAttribute(AbstractAbility.ABILITY_PROJECTILE), gbl, gbc, true);

    // workaround to increase size of effects list control
    JPanel spacer = new JPanel();
    spacer.setPreferredSize(new Dimension(256, 100));
    fieldPanel.add(spacer, gbc);

    return fieldPanel;
  }

  ViewerAbility(Ability ability)
  {
    JPanel fieldPanel = makeFieldPanel(ability);
    JPanel effectsPanel = ViewerUtil.makeListPanel("Effects", ability, Effect.class, EffectType.EFFECT_TYPE);
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)ability.getAttribute(AbstractAbility.ABILITY_ICON), 0);

    JPanel mainPanel = new JPanel(new GridLayout(1, 3, 6, 6));
    mainPanel.add(iconPanel);
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
}

