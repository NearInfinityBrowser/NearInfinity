// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;

/**
 * Handles specific layer type: INI/Actor
 */
public class LayerObjectIniActor extends LayerObjectActor
{
  private static final Image[] ICONS_GOOD = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_1),
                                             Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_2)};
  private static final Image[] ICONS_NEUTRAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_1),
                                                Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_2)};
  private static final Image[] ICONS_EVIL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_1),
                                             Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_2)};
  private static final Point CENTER = new Point(12, 40);

  private final PlainTextResource ini;

  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData, int creIndex) throws IllegalArgumentException
  {
    super(CreResource.class, null);
    this.ini = ini;
    // preparations
    IniMapEntry entrySpec = creData.getEntry("spec");
    int[] object = (entrySpec != null) ? IniMapEntry.splitObjectValue(entrySpec.getValue()) : null;

    IniMapEntry entryPoint = creData.getEntry("spawn_point");
    if (entryPoint == null) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point - entry \"spawn_point\" not found in .INI");
    }
    String[] position = IniMapEntry.splitValues(entryPoint.getValue(), IniMapEntry.REGEX_POSITION);
    if (position == null || creIndex >= position.length) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point index (" + creIndex + ")");
    }
    int[] pos = IniMapEntry.splitPositionValue(position[creIndex]);
    if (pos == null || pos.length < 2) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point value #" + creIndex);
    }

    String sectionName = creData.getName();
    String[] creNames = IniMapEntry.splitValues(creData.getEntry("cre_file").getValue());
    String creName = (creNames.length > 0) ? (creNames[0] + ".cre") : null;
    ResourceEntry creEntry = ResourceFactory.getResourceEntry(creName);
    if (creEntry == null) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid CRE resref (" + creName + ")");
    }
    CreResource cre = null;
    try {
      cre = new CreResource(creEntry);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException(creData.getName() + ": Invalid CRE resource", e);
    }

    // initializations
    final String msg = cre.getAttribute(CreResource.CRE_NAME).toString() + " [" + sectionName + "]";
    int ea = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
    location.x = pos[0];
    location.y = pos[1];

    // checking for overridden allegiance
    if (object != null && object.length > 0 && object[0] != 0) {
      ea = object[0];
    }

    Image[] icons;
    if (ea >= 2 && ea <= 30) {
      icons = ICONS_GOOD;
    } else if (ea >= 200) {
      icons = ICONS_EVIL;
    } else {
      icons = ICONS_NEUTRAL;
    }

    // Using cached icons
    icons = getIcons(icons);

    ini.setHighlightedLine(creData.getLine() + 1);
    item = new IconLayerItem(ini, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelActorsIni);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
  @Override
  public Viewable getViewable()
  {
    return ini;
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    return true;  // always active
  }
  //</editor-fold>
}
