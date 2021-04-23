// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.ITEPoint;
import org.infinity.resource.vertex.Vertex;

/**
 * Handles specific layer type: ARE/Region
 */
public class LayerObjectRegion extends LayerObject
{
  private static final Color[][] COLOR = {
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800000, true), new Color(0xC0C00000, true)},
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0804040, true), new Color(0xC0C06060, true)},
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800040, true), new Color(0xC0C00060, true)},
  };

  private final ITEPoint region;
  private final Point location = new Point();

  private final ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectRegion(AreResource parent, ITEPoint region)
  {
    super("Region", ITEPoint.class, parent);
    this.region = region;
    String msg = null;
    int type = 0;
    try {
      type = ((IsNumeric)region.getAttribute(ITEPoint.ARE_TRIGGER_TYPE)).getValue();
      if (type < 0) type = 0; else if (type >= ITEPoint.s_type.length) type = ITEPoint.s_type.length - 1;

      final IsTextual info = (IsTextual)region.getAttribute(ITEPoint.ARE_TRIGGER_INFO_POINT_TEXT);
      msg = String.format("%s (%s) %s\n%s",
                          region.getAttribute(ITEPoint.ARE_TRIGGER_NAME).toString(),
                          ITEPoint.s_type[type], getAttributes(),
                          // For "1 - Info point" show description
                          type == 1 && info != null ? info.getText() : "");
      final int vNum = ((IsNumeric)region.getAttribute(ITEPoint.ARE_TRIGGER_NUM_VERTICES)).getValue();
      final int vOfs = ((IsNumeric)parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
      shapeCoords = loadVertices(region, vOfs, 0, vNum, Vertex.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    final Polygon poly = createPolygon(shapeCoords, 1.0);
    final Rectangle bounds = normalizePolygon(poly);

    int colorType = Settings.UseColorShades ? type : 0;
    location.x = bounds.x; location.y = bounds.y;
    item = new ShapedLayerItem(region, msg, poly);
    item.setName(getCategory());
    item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][0]);
    item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][1]);
    item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][2]);
    item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][3]);
    item.setStroked(true);
    item.setFilled(true);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return region;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
      Polygon poly = createPolygon(shapeCoords, zoomFactor);
      normalizePolygon(poly);
      item.setShape(poly);
    }
  }

  private String getAttributes()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    addTrappedDesc(sb, region,
                   ITEPoint.ARE_TRIGGER_TRAPPED,
                   ITEPoint.ARE_TRIGGER_TRAP_REMOVAL_DIFFICULTY,
                   ITEPoint.ARE_TRIGGER_SCRIPT);

    final ResourceRef dest = (ResourceRef)region.getAttribute(ITEPoint.ARE_TRIGGER_DESTINATION_AREA);
    if (dest != null && !dest.isEmpty()) {
      if (sb.length() > 1) sb.append(", ");

      final AreResource self = (AreResource)getParentStructure();
      final boolean isSelf = dest.getResourceName().equalsIgnoreCase(self.getName());
      sb.append("Destination: ").append(isSelf ? "(this area)" : dest);
      String entrance = ((IsTextual)region.getAttribute(ITEPoint.ARE_TRIGGER_ENTRANCE_NAME)).getText();
      if (!entrance.isEmpty() && !entrance.equalsIgnoreCase("NONE")) {
        sb.append('>').append(entrance);
      }
    }

    if (sb.length() == 1) sb.append("No flags");
    sb.append(']');
    return sb.toString();
  }
}
