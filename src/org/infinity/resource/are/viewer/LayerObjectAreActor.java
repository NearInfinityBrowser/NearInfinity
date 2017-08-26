// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IdsBitmap;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
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
  private static final Image[] IconGood = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_1),
                                           Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_G_2)};
  private static final Image[] IconNeutral = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_1),
                                              Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_B_2)};
  private static final Image[] IconEvil = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_1),
                                           Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_ARE_ACTOR_R_2)};
  private static final Point Center = new Point(12, 40);

  private final Actor actor;
  private Flag scheduleFlags;

  public LayerObjectAreActor(AreResource parent, Actor actor)
  {
    super(Actor.class, parent);
    this.actor = actor;
    init();
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return actor;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{actor};
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

  private void init()
  {
    if (actor != null) {
      String actorName = "";
      String actorCreName = "";
      Image[] icon = IconNeutral;
      int ea = 128;   // default: neutral
      try {
        actorName = ((IsTextual)actor.getAttribute(Actor.ARE_ACTOR_NAME)).getText();
        location.x = ((DecNumber)actor.getAttribute(Actor.ARE_ACTOR_POS_X)).getValue();
        location.y = ((DecNumber)actor.getAttribute(Actor.ARE_ACTOR_POS_Y)).getValue();

        scheduleFlags = ((Flag)actor.getAttribute(Actor.ARE_ACTOR_PRESENT_AT));

        StructEntry obj = actor.getAttribute(Actor.ARE_ACTOR_CHARACTER);
        CreResource cre = null;
        if (obj instanceof TextString) {
          // ARE in saved game
          cre = (CreResource)actor.getAttribute(Actor.ARE_ACTOR_CRE_FILE);
        } else if (obj instanceof ResourceRef) {
          String creName = ((ResourceRef)obj).getResourceName();
          if (creName.lastIndexOf('.') > 0) {
            cre = new CreResource(ResourceFactory.getResourceEntry(creName));
          }
        }
        if (cre != null) {
          actorCreName = ((StringRef)cre.getAttribute(Actor.ARE_ACTOR_NAME)).toString();
          ea = (int)((IdsBitmap)cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
        }
        if (ea >= 2 && ea <= 30) {
          icon = IconGood;
        } else if (ea >= 200) {
          icon = IconEvil;
        } else {
          icon = IconNeutral;
        }
      } catch (Exception e) {
        e.printStackTrace();
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

      String info = actorName.isEmpty() ? actorCreName : actorName;
      String msg = actorName;
      if (!actorCreName.equals(actorName)) {
        msg += " (" + actorCreName + ")";
      }
      item = new IconLayerItem(location, actor, msg, info, icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(info);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
