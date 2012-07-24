// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.Flag;
import infinity.gui.ViewerUtil;

import javax.swing.*;
import java.awt.*;

final class Viewer extends JPanel
{
  private static JPanel makeFieldPanel(StoResource sto)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Name"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Type"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Sell markup"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Buy markup"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Lore"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Cost to identify"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Stealing difficulty"), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, sto.getAttribute("Depreciation rate"), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(StoResource sto)
  {
    JPanel salePanel;
    if (sto.getAttribute("Version").toString().equalsIgnoreCase("V1.1"))
      salePanel =
      ViewerUtil.makeListPanel("Items for sale", sto, ItemSale11.class, "Item");
    else
      salePanel =
      ViewerUtil.makeListPanel("Items for sale", sto, ItemSale.class, "Item");
    JPanel curePanel = ViewerUtil.makeListPanel("Cures for sale", sto, Cure.class,
                                                "Spell");
    JPanel drinkPanel = ViewerUtil.makeListPanel("Drinks for sale", sto, Drink.class,
                                                 "Drink name");
    JPanel buyPanel = ViewerUtil.makeListPanel("Items purchased", sto, Purchases.class,
                                               null);
    JPanel flagsPanel = ViewerUtil.makeCheckPanel((Flag)sto.getAttribute("Flags"), 1);
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

