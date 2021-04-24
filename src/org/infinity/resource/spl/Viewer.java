// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.spl;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ViewerUtil;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.Effect;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

public final class Viewer extends JPanel
{
  private static final HashMap<Integer, String> SpellPrefix = new HashMap<>();
  private static final HashMap<String, Integer> SpellType = new HashMap<>();
  static {
    SpellPrefix.put(Integer.valueOf(0), "MARW");
    SpellPrefix.put(Integer.valueOf(1), "SPPR");
    SpellPrefix.put(Integer.valueOf(2), "SPWI");
    SpellPrefix.put(Integer.valueOf(3), "SPIN");
    SpellPrefix.put(Integer.valueOf(4), "SPCL");
    SpellType.put("MARW", Integer.valueOf(0));
    SpellType.put("SPPR", Integer.valueOf(1));
    SpellType.put("SPWI", Integer.valueOf(2));
    SpellType.put("SPIN", Integer.valueOf(3));
    SpellType.put("SPCL", Integer.valueOf(4));
  }

  // Returns an (optionally formatted) String containing the symbolic spell name as defined in SPELL.IDS
  public static String getSymbolicName(ResourceEntry entry, boolean formatted)
  {
    if (entry != null) {
      String ext = entry.getExtension().toUpperCase(Locale.ENGLISH);
      String name = entry.getResourceRef().toUpperCase(Locale.ENGLISH);

      if ("SPL".equals(ext) && name.length() >= 7) {
        // fetching spell type
        String s = name.substring(0, 4);
        int type = 0;
        if (SpellType.containsKey(s)) {
          type = SpellType.get(s).intValue();
        }

        // fetching spell code
        s = name.substring(4);
        int code = -1;
        try {
          code = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }

        // returning symbolic spell name
        if (code >= 0) {
          // Note: type >= 10 is technically possible, but is skipped for performance reasons
          int[] types = (type >= 1 && type <= 4) ? new int[]{type} : new int[]{0, 5, 6, 7, 8, 9};
          for (int curType: types) {
            int value = curType*1000 + code;
            IdsMap ids = IdsMapCache.get("SPELL.IDS");
            IdsMapEntry idsEntry = ids.get(value);
            if (idsEntry != null) {
              if (formatted) {
                return String.format("%s (%d)", idsEntry.getSymbol(), (int)idsEntry.getID());
              } else {
                return idsEntry.getSymbol();
              }
            }
          }
        }
      }
    }
    return null;
  }

  // Returns the resource filename associated with the specified symbolic name as defined in SPELL.IDS
  public static String getResourceName(String symbol, boolean extension)
  {
    if (symbol != null) {
      IdsMap ids = IdsMapCache.get("SPELL.IDS");
      IdsMapEntry idsEntry = ids.lookup(symbol);
      if (idsEntry != null) {
        int value = (int)idsEntry.getID();
        return getResourceName(value, extension);
      }
    }
    return null;
  }

  // Returns the resource filename associated with the specified spell code as defined in SPELL.IDS
  public static String getResourceName(int value, boolean extension)
  {
    int type = value / 1000;
    if (type > 4) { type = 0; }
    int code = value % 1000;
    String prefix = SpellPrefix.get(Integer.valueOf(type));
    if (prefix != null) {
      String nameBase = String.format("%s%03d", prefix, code);
      return extension ? nameBase + ".SPL" : nameBase;
    }
    return null;
  }

  private static JPanel makeFieldPanel(SplResource spl)
  {
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel fieldPanel = new JPanel(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_NAME), gbl, gbc, true, 100);
    String s = getSymbolicName(spl.getResourceEntry(), true);
    ViewerUtil.addLabelFieldPair(fieldPanel, "Symbolic name", (s != null) ? s : "n/a", gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_CASTING_ANIMATION), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_PRIMARY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_SECONDARY_TYPE), gbl, gbc, true);
    ViewerUtil.addLabelFieldPair(fieldPanel, spl.getAttribute(SplResource.SPL_LEVEL), gbl, gbc, true);

    return fieldPanel;
  }

  Viewer(SplResource spl)
  {
    JComponent iconPanel = ViewerUtil.makeBamPanel((ResourceRef)spl.getAttribute(SplResource.SPL_ICON), 0, 0);
    JPanel globaleffectsPanel = ViewerUtil.makeListPanel("Global effects", spl, Effect.class, EffectType.EFFECT_TYPE);
    JPanel abilitiesPanel = ViewerUtil.makeListPanel("Abilities", spl, Ability.class, AbstractAbility.ABILITY_TYPE);
    JPanel descPanel = ViewerUtil.makeTextAreaPanel(spl.getAttribute(SplResource.SPL_DESCRIPTION));
    JPanel fieldPanel = makeFieldPanel(spl);

    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.add(iconPanel, BorderLayout.NORTH);
    infoPanel.add(fieldPanel, BorderLayout.CENTER);

    setLayout(new GridLayout(2, 2, 6, 6));
    add(infoPanel);
    add(globaleffectsPanel);
    add(descPanel);
    add(abilitiesPanel);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
  }
}

