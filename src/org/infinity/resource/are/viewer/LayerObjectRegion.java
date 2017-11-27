// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.TextString;
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
  private static final String[] TYPE = {"Proximity trigger", "Info point", "Travel region"};
  private static final Color[][] COLOR = new Color[][]{
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800000, true), new Color(0xC0C00000, true)},
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0804040, true), new Color(0xC0C06060, true)},
    {new Color(0xFF400000, true), new Color(0xFF400000, true), new Color(0xC0800040, true), new Color(0xC0C00060, true)},
  };

  private final ITEPoint region;
  private final Point location = new Point();

  private ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectRegion(AreResource parent, ITEPoint region)
  {
    super(ViewerConstants.RESOURCE_ARE, "Region", ITEPoint.class, parent);
    this.region = region;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return region;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{region};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
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
  public void reload()
  {
    init();
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

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }

  private void init()
  {
    if (region != null) {
      shapeCoords = null;
      String msg = "";
      Polygon poly = null;
      Rectangle bounds = null;
      int type = 0;
      try {
        type = ((Bitmap)region.getAttribute(ITEPoint.ARE_TRIGGER_TYPE)).getValue();
        if (type < 0) type = 0; else if (type >= TYPE.length) type = TYPE.length - 1;
        msg = String.format("%s (%s) %s",
                            ((TextString)region.getAttribute(ITEPoint.ARE_TRIGGER_NAME)).toString(),
                            TYPE[type], getAttributes());
        int vNum = ((DecNumber)region.getAttribute(ITEPoint.ARE_TRIGGER_NUM_VERTICES)).getValue();
        int vOfs = ((HexNumber)getParentStructure().getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
        shapeCoords = loadVertices(region, vOfs, 0, vNum, Vertex.class);
        poly = createPolygon(shapeCoords, 1.0);
        bounds = normalizePolygon(poly);
      } catch (Exception e) {
        e.printStackTrace();
        if (shapeCoords == null) {
          shapeCoords = new Point[0];
        }
        if (poly == null) {
          poly = new Polygon();
        }
        if (bounds == null) {
          bounds = new Rectangle();
        }
      }

      int colorType = Settings.UseColorShades ? type : 0;
      location.x = bounds.x; location.y = bounds.y;
      item = new ShapedLayerItem(location, region, msg, msg, poly);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[colorType][2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[colorType][3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
    }
  }

  private String getAttributes()
  {
    StringBuilder sb = new StringBuilder();
    if (region != null) {
      sb.append('[');

      boolean isTrapped = ((IsNumeric)region.getAttribute(ITEPoint.ARE_TRIGGER_TRAPPED)).getValue() != 0;
      if (isTrapped) {
        int v = ((IsNumeric)region.getAttribute(ITEPoint.ARE_TRIGGER_TRAP_REMOVAL_DIFFICULTY)).getValue();
        if (v > 0) sb.append("Trapped (").append(v).append(')');
      }

      String script = ((IsTextual)region.getAttribute(ITEPoint.ARE_TRIGGER_SCRIPT)).getText();
      if (!script.isEmpty() && !script.equalsIgnoreCase("NONE")) {
        if (sb.length() > 1) sb.append(", ");
        sb.append("Script: ").append(script).append(".BCS");
      }

      String area = ((IsTextual)region.getAttribute(ITEPoint.ARE_TRIGGER_DESTINATION_AREA)).getText();
      if (!area.isEmpty() && !area.equalsIgnoreCase("NONE")) {
        if (sb.length() > 1) sb.append(", ");
        sb.append("Destination: ").append(area).append(".ARE");
        String entrance = ((IsTextual)region.getAttribute(ITEPoint.ARE_TRIGGER_ENTRANCE_NAME)).getText();
        if (!entrance.isEmpty() && !entrance.equalsIgnoreCase("NONE")) {
          sb.append('>').append(entrance);
        }
      }

      if (sb.length() == 1) sb.append("No flags");
      sb.append(']');
    }
    return sb.toString();
  }
}
