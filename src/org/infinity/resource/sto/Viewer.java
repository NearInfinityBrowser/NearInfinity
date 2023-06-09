// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.infinity.datatype.Flag;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractStruct;

public final class Viewer extends JPanel {
  private static JPanel makeFieldPanel(StoResource sto) {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_NAME), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_MARKUP_SELL), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_MARKUP_BUY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_LORE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_COST_TO_IDENTIFY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_STEALING_DIFFICULTY), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute(StoResource.STO_DEPRECIATION_RATE), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(StoResource sto) {
    JPanel salePanel;
    if (sto.getAttribute(AbstractStruct.COMMON_VERSION).toString().equalsIgnoreCase("V1.1")) {
      salePanel = ViewerUtil.makeListPanel("Items for sale", sto, ItemSale11.class, ItemSale11.STO_SALE_ITEM);
    } else {
      salePanel = ViewerUtil.makeListPanel("Items for sale", sto, ItemSale.class, ItemSale.STO_SALE_ITEM);
    }
    JPanel curePanel = ViewerUtil.makeListPanel("Cures for sale", sto, Cure.class, Cure.STO_CURE_SPELL);
    JPanel drinkPanel = ViewerUtil.makeListPanel("Drinks for sale", sto, Drink.class, Drink.STO_DRINK_NAME);
    JPanel buyPanel = ViewerUtil.makeListPanel("Items purchased", sto, Purchases.class, null);
    JPanel flagsPanel = ViewerUtil.makeCheckPanel((Flag) sto.getAttribute(StoResource.STO_FLAGS), 1);
    JPanel fieldPanel = makeFieldPanel(sto);

    setLayout(new GridLayout(2, 3, 6, 6));
    add(fieldPanel);
    add(salePanel);
    add(buyPanel);
    add(flagsPanel);
    add(curePanel);
    add(drinkPanel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }
}
