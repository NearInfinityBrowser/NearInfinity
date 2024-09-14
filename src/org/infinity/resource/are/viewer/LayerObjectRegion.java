// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.ITEPoint;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.vertex.Vertex;
import org.infinity.util.Logger;

/**
 * Handles specific layer type: ARE/Region
 */
public class LayerObjectRegion extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_REGION_TARGET_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_REGION_TARGET_2.getIcon().getImage() };

  private static final Image[] ICONS_ALT = { ViewerIcons.ICON_ITM_REGION_TARGET_A_1.getIcon().getImage(),
                                             ViewerIcons.ICON_ITM_REGION_TARGET_A_2.getIcon().getImage() };

  private static final Image[] ICONS_SPEAKER = { ViewerIcons.ICON_ITM_REGION_TARGET_S_1.getIcon().getImage(),
                                                 ViewerIcons.ICON_ITM_REGION_TARGET_S_2.getIcon().getImage() };

  private static final Point CENTER = new Point(13, 29);

  private static final Color[][] COLOR = {
      { new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800000, true), new Color(0xC0C00000, true) },
      { new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0804040, true), new Color(0xC0C06060, true) },
      { new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800040, true), new Color(0xC0C00060, true) },
  };

  private final ITEPoint region;
  private final Point location = new Point();
  private final Point launchPoint = new Point();
  private final Point alternatePoint = new Point();
  private final Point speakerPoint = new Point();

  /** Region area */
  private final ShapedLayerItem item;

  /** Launch point of the region. */
  private final IconLayerItem itemIcon;

  /** Optional alternate/activation point of the region. */
  private final IconLayerItem itemIconAlternate;

  /** Optional speaker location of the region (PST/PSTEE). */
  private final IconLayerItem itemIconSpeaker;

  private Point[] shapeCoords;
  private boolean alternateEnabled;

  public LayerObjectRegion(AreResource parent, ITEPoint region) {
    super("Region", ITEPoint.class, parent);
    this.region = region;
    String label = null;
    String msg = null;
    int type = 0;
    try {
      type = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_TYPE)).getValue();
      if (type < 0) {
        type = 0;
      } else if (type >= ITEPoint.TYPE_ARRAY.length) {
        type = ITEPoint.TYPE_ARRAY.length - 1;
      }

      final IsTextual info = (IsTextual) region.getAttribute(ITEPoint.ARE_TRIGGER_INFO_POINT_TEXT);
      msg = String.format("%s (%s) %s\n%s", region.getAttribute(ITEPoint.ARE_TRIGGER_NAME).toString(),
          ITEPoint.TYPE_ARRAY[type], getAttributes(),
          // For "1 - Info point" show description
          type == 1 && info != null ? info.getText() : "");
      final int vNum = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_NUM_VERTICES)).getValue();
      final int vOfs = ((IsNumeric) parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
      shapeCoords = loadVertices(region, vOfs, 0, vNum, Vertex.class);

      label = region.getAttribute(ITEPoint.ARE_TRIGGER_NAME).toString();
      int flags = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_FLAGS)).getValue();
      alternateEnabled = (flags & (1 << 10)) != 0;
      if (alternateEnabled) {
        if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
          alternatePoint.x = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_ALTERNATE_POINT_X)).getValue();
          alternatePoint.y = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_ALTERNATE_POINT_Y)).getValue();
        } else if (Profile.getEngine() != Profile.Engine.PST) {
          alternatePoint.x = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_ACTIVATION_POINT_X)).getValue();
          alternatePoint.y = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_ACTIVATION_POINT_Y)).getValue();
        }
      } else {
        launchPoint.x = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_LAUNCH_POINT_X)).getValue();
        launchPoint.y = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_LAUNCH_POINT_Y)).getValue();
      }
      if (Profile.getEngine() == Profile.Engine.PST || Profile.getGame() == Profile.Game.PSTEE) {
        if (!((ResourceRef) region.getAttribute(ITEPoint.ARE_TRIGGER_DIALOG)).isEmpty()) {
          speakerPoint.x = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_SPEAKER_POINT_X)).getValue();
          speakerPoint.y = ((IsNumeric) region.getAttribute(ITEPoint.ARE_TRIGGER_SPEAKER_POINT_Y)).getValue();
        }
      }
    } catch (Exception e) {
      Logger.error(e);
    }
    final Polygon poly = createPolygon(shapeCoords, 1.0);
    final Rectangle bounds = normalizePolygon(poly);

    int colorType = Settings.UseColorShades ? type : 0;
    location.x = bounds.x;
    location.y = bounds.y;
    item = new ShapedLayerItem(region, msg, poly);
    item.setName(getCategory());
    item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][0]);
    item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][1]);
    item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][2]);
    item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][3]);
    item.setStroked(true);
    item.setFilled(true);
    item.setVisible(isVisible());

    itemIconSpeaker = createValidatedLayerItem(speakerPoint, label, getIcons(ICONS_SPEAKER));
    if (alternateEnabled) {
      itemIcon = null;
      itemIconAlternate = createValidatedLayerItem(alternatePoint, label, getIcons(ICONS_ALT));
    } else {
      itemIcon = createValidatedLayerItem(launchPoint, label, getIcons(ICONS));
      itemIconAlternate = null;
    }
  }

  @Override
  public Viewable getViewable() {
    return region;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    switch (type) {
      case ViewerConstants.LAYER_ITEM_POLY:
        if (item != null) {
          return new AbstractLayerItem[] { item };
        }
        break;
      case ViewerConstants.LAYER_ITEM_ICON:
        final List<AbstractLayerItem> list = new ArrayList<>();
        if (itemIcon != null) {
          list.add(itemIcon);
        }
        if (itemIconAlternate != null) {
          list.add(itemIconAlternate);
        }
        if (itemIconSpeaker != null) {
          list.add(itemIconSpeaker);
        }
        return list.toArray(new AbstractLayerItem[0]);
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    final List<AbstractLayerItem> retVal = new ArrayList<>();
    if (item != null) {
      retVal.add(item);
    }
    if (itemIcon != null) {
      retVal.add(itemIcon);
    }
    if (itemIconAlternate != null) {
      retVal.add(itemIconAlternate);
    }
    if (itemIconSpeaker != null) {
      retVal.add(itemIconSpeaker);
    }
    return retVal.toArray(new AbstractLayerItem[0]);
  }

  @Override
  public void update(double zoomFactor) {
    if (item != null) {
      item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (location.y * zoomFactor + (zoomFactor / 2.0)));
      Polygon poly = createPolygon(shapeCoords, zoomFactor);
      normalizePolygon(poly);
      item.setShape(poly);
    }

    if (itemIcon != null) {
      itemIcon.setItemLocation((int) (launchPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (launchPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }

    if (itemIconAlternate != null) {
      itemIconAlternate.setItemLocation((int) (alternatePoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (alternatePoint.y * zoomFactor + (zoomFactor / 2.0)));
    }

    if (itemIconSpeaker != null) {
      itemIconSpeaker.setItemLocation((int) (speakerPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (speakerPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }

  private String getAttributes() {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    addTrappedDesc(sb, region, ITEPoint.ARE_TRIGGER_TRAPPED, ITEPoint.ARE_TRIGGER_TRAP_REMOVAL_DIFFICULTY,
        ITEPoint.ARE_TRIGGER_SCRIPT);

    final ResourceRef dest = (ResourceRef) region.getAttribute(ITEPoint.ARE_TRIGGER_DESTINATION_AREA);
    if (dest != null && !dest.isEmpty()) {
      if (sb.length() > 1) {
        sb.append(", ");
      }

      final AreResource self = (AreResource) getParentStructure();
      final boolean isSelf = dest.getResourceName().equalsIgnoreCase(self.getName());
      sb.append("Destination: ").append(isSelf ? "(this area)" : dest);
      String entrance = ((IsTextual) region.getAttribute(ITEPoint.ARE_TRIGGER_ENTRANCE_NAME)).getText();
      if (!entrance.isEmpty() && !entrance.equalsIgnoreCase("NONE")) {
        sb.append('>').append(entrance);
      }
    }

    if (sb.length() == 1) {
      sb.append("No flags");
    }
    sb.append(']');
    return sb.toString();
  }

  private IconLayerItem createValidatedLayerItem(Point pt, String label, Image[] icons) {
    IconLayerItem retVal = null;

    if (pt.x > 0 && pt.y > 0) {
      retVal = new IconLayerItem(region, label, LayerRegion.LAYER_ICONS_TARGET, icons[0], CENTER);
      retVal.setLabelEnabled(Settings.ShowLabelRegionTargets);
      retVal.setName(getCategory());
      retVal.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
      retVal.setVisible(isVisible());
    }

    return retVal;
  }
}
