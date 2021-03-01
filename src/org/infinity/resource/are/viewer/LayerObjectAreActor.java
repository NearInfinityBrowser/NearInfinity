// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.EnumMap;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.cre.CreResource;

/**
 * Handles specific layer type: ARE/Actor
 */
public class LayerObjectAreActor extends LayerObjectActor
{
  private static EnumMap<Allegiance, Image[]> ICONS = new EnumMap<Allegiance, Image[]>(Allegiance.class) {{
    put(Allegiance.GOOD, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_1),
                                      Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_2)});
    put(Allegiance.NEUTRAL, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_1),
                                         Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_2)});
    put(Allegiance.ENEMY, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_2)});
  }};
  private static final Point CENTER = new Point(12, 40);

  private final Actor actor;
  private Flag scheduleFlags;

  public LayerObjectAreActor(AreResource parent, Actor actor)
  {
    super(Actor.class, parent);
    this.actor = actor;

    String actorName = null;
    String actorCreName = null;
    Image[] icons = ICONS.get(Allegiance.NEUTRAL);
    int ea = 128;   // default: neutral
    ActorAnimationProvider sprite = null;
    try {
      // initializations
      actorName  = ((IsTextual)actor.getAttribute(Actor.ARE_ACTOR_NAME)).getText();
      location.x = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_X)).getValue();
      location.y = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_Y)).getValue();
      int orientation = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_ORIENTATION)).getValue();

      scheduleFlags = ((Flag)actor.getAttribute(Actor.ARE_ACTOR_PRESENT_AT));

      StructEntry obj = actor.getAttribute(Actor.ARE_ACTOR_CHARACTER);
      CreResource cre = null;
      if (obj instanceof TextString) {
        // ARE in saved game
        cre = (CreResource)actor.getAttribute(Actor.ARE_ACTOR_CRE_FILE);
      } else
      if (obj instanceof ResourceRef) {
        final ResourceRef creRef = (ResourceRef)obj;
        if (!creRef.isEmpty()) {
          cre = new CreResource(ResourceFactory.getResourceEntry(creRef.getResourceName()));
        }
      }
      if (cre != null) {
        actorCreName = cre.getAttribute(CreResource.CRE_NAME).toString();
        ea = ((IsNumeric)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
      }

      sprite = createAnimationProvider(cre);
      sprite.setOrientation(orientation);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    icons = ICONS.get(getAllegiance(ea));
    icons = getIcons(icons);

    final String msg = (actorCreName == null) ? actorName : actorCreName + " (" + actorName + ')';

    IconLayerItem item1 = new IconLayerItem(actor, msg, icons[0], CENTER);
    item1.setLabelEnabled(Settings.ShowLabelActorsAre);
    item1.setName(getCategory());
    item1.setToolTipText(msg);
    item1.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item1.setVisible(isVisible());
    items[0] = item1;

    AnimatedLayerItem item2 = new AnimatedLayerItem(actor, msg, sprite);
    item2.setName(getCategory());
    item2.setToolTipText(msg);
    item2.setVisible(false);
    item2.setFrameRate(10.0);
    item2.setAutoPlay(false);
    item2.setComposite(Settings.UseActorAccurateBlending ? sprite.getDecoder().getComposite() : null);
    item2.setFrameColor(AbstractLayerItem.ItemState.NORMAL, COLOR_FRAME_NORMAL);
    item2.setFrameWidth(AbstractLayerItem.ItemState.NORMAL, 2);
    item2.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, false);
    item2.setFrameColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR_FRAME_HIGHLIGHTED);
    item2.setFrameWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
    item2.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, true);
    items[1] = item2;
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
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
  //</editor-fold>
}
