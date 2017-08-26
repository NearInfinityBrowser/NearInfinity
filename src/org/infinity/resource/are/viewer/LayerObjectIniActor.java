// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.StringRef;
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
  private static final Image[] IconGood = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_1),
                                           Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_2)};
  private static final Image[] IconNeutral = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_1),
                                              Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_2)};
  private static final Image[] IconEvil = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_1),
                                           Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_2)};
  private static final Point Center = new Point(12, 40);

  private final PlainTextResource ini;
  private final IniMapSection creData;
  private final int creIndex;

  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData) throws Exception
  {
    this(ini, creData, 0);
  }

  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData, int creIndex) throws Exception
  {
    super(CreResource.class, null);
    this.ini = ini;
    this.creData = creData;
    this.creIndex = creIndex;
    init();
  }

  @Override
  public void reload()
  {
    try {
      init();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public Viewable getViewable()
  {
    return ini;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{ini};
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    return true;  // always active
  }


  private void init() throws Exception
  {
    if (ini != null && creData != null && creIndex >= 0) {
      // preparations
      IniMapEntry entrySpec = creData.getEntry("spec");
      int[] object = (entrySpec != null) ? IniMapEntry.splitObjectValue(entrySpec.getValue()) : null;

      IniMapEntry entryPoint = creData.getEntry("spawn_point");
      if (entryPoint == null) {
        throw new Exception(creData.getName() + ": Invalid spawn point");
      }
      String[] position = IniMapEntry.splitValues(entryPoint.getValue(), IniMapEntry.REGEX_POSITION);
      if (position == null || creIndex >= position.length) {
        throw new Exception(creData.getName() + ": Invalid spawn point index (" + creIndex + ")");
      }
      int[] pos = IniMapEntry.splitPositionValue(position[creIndex]);
      if (pos == null || pos.length < 2) {
        throw new Exception(creData.getName() + ": Invalid spawn point value");
      }

      String sectionName = creData.getName();
      String[] creNames = IniMapEntry.splitValues(creData.getEntry("cre_file").getValue());
      String creName = (creNames.length > 0) ? (creNames[0] + ".cre") : null;
      ResourceEntry creEntry = ResourceFactory.getResourceEntry(creName);
      if (creEntry == null) {
        throw new Exception(creData.getName() + ": Invalid CRE resref (" + creName + ")");
      }
      CreResource cre = null;
      try {
        cre = new CreResource(creEntry);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (cre == null) {
        throw new Exception(creData.getName() + ": Invalid CRE resource");
      }

      // initializations
      Image[] icon;
      String info = sectionName;
      String msg = ((StringRef)cre.getAttribute(CreResource.CRE_NAME)).toString() + " [" + sectionName + "]";
      int ea = (int)((IdsBitmap)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
      location.x = pos[0];
      location.y = pos[1];

      // checking for overridden allegiance
      if (object != null && object.length > 0 && object[0] != 0) {
        ea = object[0];
      }

      if (ea >= 2 && ea <= 30) {
        icon = IconGood;
      } else if (ea >= 200) {
        icon = IconEvil;
      } else {
        icon = IconNeutral;
      }

      // Using cached icons
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(icon[0]),
                                                 SharedResourceCache.createKey(icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.ICON, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.ICON, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon);
      } else {
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      ini.setHighlightedLine(creData.getLine() + 1);
      item = new IconLayerItem(location, ini, msg, info, icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(info);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
