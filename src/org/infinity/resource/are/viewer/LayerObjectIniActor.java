// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.EnumMap;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
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
  private static final EnumMap<Allegiance, Image[]> ICONS = new EnumMap<Allegiance, Image[]>(Allegiance.class) {{
    put(Allegiance.GOOD, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_1),
                                      Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_G_2)});
    put(Allegiance.NEUTRAL, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_1),
                                         Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_B_2)});
    put(Allegiance.ENEMY, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_INI_ACTOR_R_2)});
  }};
  private static final Point CENTER = new Point(12, 40);

  private final PlainTextResource ini;
  private final IniMapSection creData;
  private final int creIndex;
  private final CreResource cre;

  /**
   * Creates a new {@code LayerObjectIniActor} instance.
   * @param ini INI resource containing actor definitiuons
   * @param creData the INI section relevant for this actor definition
   * @param creIndex the spawn point location index for this creature
   * @throws IllegalArgumentException
   */
  public LayerObjectIniActor(PlainTextResource ini, IniMapSection creData, int creIndex) throws IllegalArgumentException
  {
    super(CreResource.class, null);
    this.ini = Objects.requireNonNull(ini);
    this.creData = Objects.requireNonNull(creData);
    this.creIndex = creIndex;

    // preparations
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

    this.cre = cre;

    // initializations
    int[] pos = getCreatureLocation();
    location.x = pos[0];
    location.y = pos[1];

    // setting creature allegiance
    int ea = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();

    IniMapEntry entrySpec = creData.getEntry("spec");
    int[] object = (entrySpec != null) ? IniMapEntry.splitObjectValue(entrySpec.getValue()) : null;
    if (object != null && object.length > 0 && object[0] != 0) {
      ea = object[0];
    }

    // Using cached icons
    Image[] icons = ICONS.get(getAllegiance(ea));
    icons = getIcons(icons);

    String tooltip = getTooltip();
    ini.setHighlightedLine(creData.getLine() + 1);

    IconLayerItem item1 = new IconLayerItem(ini, tooltip, icons[0], CENTER);
    item1.setLabelEnabled(Settings.ShowLabelActorsIni);
    item1.setName(getCategory());
    item1.setToolTipText(tooltip);
    item1.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item1.setVisible(isVisible());
    items[0] = item1;

    // payload is initialized on demand
    AnimatedLayerItem item2 = new AnimatedLayerItem(ini, tooltip, AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER);
    item2.setName(getCategory());
    item2.setToolTipText(tooltip);
    item2.setVisible(false);
    item2.setFrameRate(Settings.getDefaultFrameRateAnimations());
    item2.setAutoPlay(false);
    item2.setFrameColor(AbstractLayerItem.ItemState.NORMAL, COLOR_FRAME_NORMAL);
    item2.setFrameWidth(AbstractLayerItem.ItemState.NORMAL, 2);
    item2.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, false);
    item2.setFrameColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR_FRAME_HIGHLIGHTED);
    item2.setFrameWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
    item2.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, true);
    items[1] = item2;
  }

  public IniMapSection getCreatureData()
  {
    return creData;
  }

  public int getCreatureIndex()
  {
    return creIndex;
  }

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

  // Returns position and orientation of the current creature
  private int[] getCreatureLocation() throws IllegalArgumentException
  {
    int[] retVal = {0, 0, 0};

    IniMapEntry entryPoint = creData.getEntry("spawn_point");
    if (entryPoint == null) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point - entry \"spawn_point\" not found in .INI");
    }

    String[] items = IniMapEntry.splitValues(entryPoint.getValue(), IniMapEntry.REGEX_POSITION);
    if (items == null || creIndex >= items.length) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point index (" + creIndex + ")");
    }

    int[] pos = IniMapEntry.splitPositionValue(items[creIndex]);
    if (pos == null || pos.length < 2) {
      throw new IllegalArgumentException(creData.getName() + ": Invalid spawn point value #" + creIndex);
    }

    System.arraycopy(pos, 0, retVal, 0, Math.min(retVal.length, pos.length));

    return retVal;
  }

  @Override
  public synchronized void loadAnimation()
  {
    if (items[1] instanceof AnimatedLayerItem) {
      AnimatedLayerItem item = (AnimatedLayerItem)items[1];
      if (item.getAnimation() == AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER) {
        try {
          int[] pos = getCreatureLocation();
          int orientation = (pos.length > 2) ? pos[2] : 0;
          ActorAnimationProvider sprite = createAnimationProvider(cre);
          sprite.setOrientation(orientation);

          item.setAnimation(sprite);
          item.setComposite(Settings.UseActorAccurateBlending ? sprite.getDecoder().getComposite() : null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Tooltip for actor object. */
  private String getTooltip()
  {
    String sectionName = creData.getName();
    return cre.getAttribute(CreResource.CRE_NAME).toString() + " [" + sectionName + "]";
  }
}
