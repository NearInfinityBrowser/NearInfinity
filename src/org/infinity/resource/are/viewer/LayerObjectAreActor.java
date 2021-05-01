// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.EnumMap;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;

/**
 * Handles specific layer type: ARE/Actor
 */
public class LayerObjectAreActor extends LayerObjectActor
{
  private static final EnumMap<Allegiance, Image[]> ICONS = new EnumMap<Allegiance, Image[]>(Allegiance.class) {{
    put(Allegiance.GOOD, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_1),
                                      Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_2)});
    put(Allegiance.NEUTRAL, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_1),
                                         Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_2)});
    put(Allegiance.ENEMY, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_2)});
  }};
  private static final Point CENTER = new Point(12, 40);

  private final Actor actor;
  private final CreResource cre;
  private Flag scheduleFlags;

  public LayerObjectAreActor(AreResource parent, Actor actor)
  {
    super(Actor.class, parent);
    this.actor = actor;

    int ea = 128;   // default: neutral
    CreResource cre = null;
    try {
      // initializations
      location.x = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_X)).getValue();
      location.y = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_Y)).getValue();
      scheduleFlags = ((Flag)actor.getAttribute(Actor.ARE_ACTOR_PRESENT_AT));

      boolean isReference = ((Flag)actor.getAttribute(Actor.ARE_ACTOR_FLAGS)).isFlagSet(0);
      if (isReference) {
        // external CRE resource?
        ResourceEntry creEntry = ResourceFactory.getResourceEntry(((IsReference)actor.getAttribute(Actor.ARE_ACTOR_CHARACTER)).getResourceName());
        if (creEntry != null) {
          Resource res = ResourceFactory.getResource(creEntry);
          if (res instanceof CreResource) {
            cre = (CreResource)res;
          }
        }
      } else {
        // attached CRE resource?
        cre = (CreResource)actor.getAttribute(Actor.ARE_ACTOR_CRE_FILE);
      }

      if (cre != null) {
        ea = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    this.cre = cre;

    // Using cached icons
    Image[] icons = ICONS.get(getAllegiance(ea));
    icons = getIcons(icons);

    String tooltip = getTooltip();
    IconLayerItem item1 = new IconLayerItem(actor, tooltip, icons[0], CENTER);
    item1.setLabelEnabled(Settings.ShowLabelActorsAre);
    item1.setName(getCategory());
    item1.setToolTipText(tooltip);
    item1.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item1.setVisible(isVisible());
    items[0] = item1;

    // payload is initialized on demand
    AnimatedLayerItem item2 = new AnimatedLayerItem(actor, tooltip, AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER);
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

  @Override
  public Viewable getViewable()
  {
    return actor;
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
    } else {
      return false;
    }
  }

  @Override
  public synchronized void loadAnimation()
  {
    if (items[1] instanceof AnimatedLayerItem) {
      AnimatedLayerItem item = (AnimatedLayerItem)items[1];
      if (item.getAnimation() == AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER) {
        if (cre != null) {
          try {
            int orientation = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_ORIENTATION)).getValue();
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
  }

  /** Tooltip for actor object. */
  private String getTooltip()
  {
    String retVal = null;
    if (cre != null) {
      retVal = ((IsTextual)cre.getAttribute(CreResource.CRE_NAME)).getText();
    } else {
      retVal = ((IsTextual)actor.getAttribute(Actor.ARE_ACTOR_NAME)).getText();
    }
    return retVal;
  }
}
