// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.EnumMap;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.gam.GamResource;
import org.infinity.resource.gam.PartyNPC;
import org.infinity.resource.key.ResourceEntry;

/**
 * Handles specific layer type: global GAM/Actor
 */
public class LayerObjectGlobalActor extends LayerObjectActor
{
  private static final EnumMap<Allegiance, Image[]> ICONS = new EnumMap<Allegiance, Image[]>(Allegiance.class) {{
    put(Allegiance.GOOD, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_G_1),
                                      Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_G_2)});
    put(Allegiance.NEUTRAL, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_B_1),
                                         Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_B_2)});
    put(Allegiance.ENEMY, new Image[] {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_R_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_GAM_ACTOR_R_2)});
  }};
  private static final Point CENTER = new Point(12, 40);

  private final PartyNPC npc;

  private CreResource cre;

  public LayerObjectGlobalActor(GamResource parent, PartyNPC npc)
  {
    super(PartyNPC.class, parent);
    this.npc = Objects.requireNonNull(npc);

    int creOfs = ((IsNumeric)this.npc.getAttribute(PartyNPC.GAM_NPC_OFFSET_CRE)).getValue();
    int creSize = ((IsNumeric)this.npc.getAttribute(PartyNPC.GAM_NPC_CRE_SIZE)).getValue();
    if (creOfs > 0 && creSize > 0) {
      // attached resource?
      StructEntry se = this.npc.getAttribute(PartyNPC.GAM_NPC_CRE_RESOURCE);
      if (se instanceof CreResource) {
        this.cre = (CreResource)se;
      }
    } else {
      // external resource?
      String creRes = ((IsTextual)this.npc.getAttribute(PartyNPC.GAM_NPC_CHARACTER)).getText();
      ResourceEntry entry = ResourceFactory.getResourceEntry(creRes + ".CRE");
      if (entry != null) {
        Resource res = ResourceFactory.getResource(entry);
        if (res instanceof CreResource) {
          this.cre = (CreResource)res;
        }
      }
    }

    if (this.cre == null) {
      throw new NullPointerException("Could not determine CRE resource: " + this.npc.getName());
    }

    location.x = ((IsNumeric)this.npc.getAttribute(PartyNPC.GAM_NPC_LOCATION_X)).getValue();
    location.y = ((IsNumeric)this.npc.getAttribute(PartyNPC.GAM_NPC_LOCATION_Y)).getValue();

    int ea = ((IsNumeric)this.cre.getAttribute(CreResource.CRE_ALLEGIANCE)).getValue();
    Image[] icons = ICONS.get(getAllegiance(ea));
    icons = getIcons(icons);

    String tooltip = getTooltip();
    IconLayerItem item1 = new IconLayerItem(npc, tooltip, icons[0], CENTER);
    item1.setLabelEnabled(Settings.ShowLabelActorsAre);
    item1.setName(getCategory());
    item1.setToolTipText(tooltip);
    item1.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item1.setVisible(isVisible());
    items[0] = item1;

    // payload is initialized on demand
    AnimatedLayerItem item2 = new AnimatedLayerItem(npc, tooltip, AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER);
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
  public void loadAnimation()
  {
    if (items[1] instanceof AnimatedLayerItem) {
      AnimatedLayerItem item = (AnimatedLayerItem)items[1];
      if (item.getAnimation() == AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER) {
        if (cre != null) {
          try {
            int orientation = ((IsNumeric)npc.getAttribute(PartyNPC.GAM_NPC_ORIENTATION)).getValue();
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

  @Override
  public Viewable getViewable()
  {
    return npc;
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    return true;  // always active
  }

  /** Tooltip for actor object. */
  private String getTooltip()
  {
    String retVal = null;

    retVal = ((IsTextual)npc.getAttribute(PartyNPC.GAM_NPC_NAME)).getText().trim();
    if (retVal.isEmpty()) {
      retVal = ((IsTextual)cre.getAttribute(CreResource.CRE_NAME)).getText().trim();
    }
    if (retVal.isEmpty()) {
      retVal = ((IsTextual)cre.getAttribute(CreResource.CRE_TOOLTIP)).getText().trim();
    }
    if (retVal.isEmpty()) {
      retVal = "(no name)";
    }

    return retVal;
  }
}
