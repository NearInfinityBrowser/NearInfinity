// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;

import javax.swing.JButton;

import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

/**
 * Provides a button that opens the associated INI or 2DA resource of the selected animation slot.
 */
public class AnimateBitmap extends IdsBitmap implements ActionListener
{
  private final JButton showIni;
  private IdsMap idsMap;

  public AnimateBitmap(ByteBuffer buffer, int offset, int length, String name, String resource)
  {
    this(null, buffer, offset, length, name, resource, 0);
  }

  public AnimateBitmap(StructEntry parent, ByteBuffer buffer, int offset, int length, String name,
                       String resource)
  {
    this(parent, buffer, offset, length, name, resource, 0);
  }

  public AnimateBitmap(ByteBuffer buffer, int offset, int length, String name, String resource, int idsStart)
  {
    this(null, buffer, offset, length, name, resource, idsStart);
  }

  public AnimateBitmap(StructEntry parent, ByteBuffer buffer, int offset, int length, String name,
                       String resource, int idsStart)
  {
    super(parent, buffer, offset, length, name, resource, idsStart);

    // Don't show button for games that don't support INI/2DA files
    if (Profile.isEnhancedEdition()) {
      showIni = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
      showIni.setToolTipText("Open associated INI resource");
    } else if (ResourceFactory.resourceExists("ANISND.IDS")) {
      showIni = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
      showIni.setToolTipText("Open associated 2DA resource");
      idsMap = IdsMapCache.get("ANISND.IDS");
    } else {
      showIni = null;
    }

    if (showIni != null) {
      showIni.setEnabled(false);
      showIni.addActionListener(this);
      addButtons(showIni);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == showIni) {
      String animRes = getAnimResource(getValueOfItem(getListPanel().getSelectedValue()));
      if (animRes != null) {
        ResourceEntry entry = ResourceFactory.getResourceEntry(animRes);
        if (entry != null) {
          new ViewFrame(getListPanel().getTopLevelAncestor(), ResourceFactory.getResource(entry));
        }
      }
    }
  }

  @Override
  protected void listItemChanged()
  {
    if (showIni != null) {
      boolean b = false;
      Long value = getValueOfItem(getListPanel().getSelectedValue());
      if (value != null) {
        b = (getAnimResource(value) != null);
      }
      showIni.setEnabled(b);
    }
  }

  private String getAnimResource(long value)
  {
    String animRes = null;
    if (idsMap != null) {
      // checking for 2DA
      IdsMapEntry entry = idsMap.getValue(value);
      if (entry != null) {
        String symbol = entry.getString();
        int idx = symbol.indexOf(' ');
        if (idx < 0) { idx = symbol.indexOf('\t'); }
        if (idx > 0) {
          animRes = symbol.substring(0, idx).trim() + ".2DA";
        }
      }
    } else {
      // checking for INI
      animRes = String.format("%04X.INI", value);
    }

    if (animRes != null && !ResourceFactory.resourceExists(animRes)) {
      animRes = null;
    }
    return animRes;
  }
}
