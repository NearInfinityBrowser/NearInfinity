// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
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
  private static final Image[] ICONS_GOOD = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_1),
                                             Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_2)};
  private static final Image[] ICONS_NEUTRAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_1),
                                                Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_2)};
  private static final Image[] ICONS_EVIL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_1),
                                             Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_2)};
  private static final Point CENTER = new Point(12, 40);

  private final Actor actor;
  private Flag scheduleFlags;

  public LayerObjectAreActor(AreResource parent, Actor actor)
  {
    super(Actor.class, parent);
    this.actor = actor;
    String actorName = null;
    String actorCreName = null;
    Image[] icons = ICONS_NEUTRAL;
    int ea = 128;   // default: neutral
    try {
      actorName  = ((IsTextual)actor.getAttribute(Actor.ARE_ACTOR_NAME)).getText();
      location.x = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_X)).getValue();
      location.y = ((IsNumeric)actor.getAttribute(Actor.ARE_ACTOR_POS_Y)).getValue();

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
      if (ea >= 2 && ea <= 30) {
        icons = ICONS_GOOD;
      } else if (ea >= 200) {
        icons = ICONS_EVIL;
      } else {
        icons = ICONS_NEUTRAL;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    icons = getIcons(icons);

    final String msg = actorCreName == null
        ? actorName
        : actorCreName + " (" + actorName + ')';
    item = new IconLayerItem(actor, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelActorsAre);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
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
