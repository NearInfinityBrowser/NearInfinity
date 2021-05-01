// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;

import org.infinity.datatype.ResourceRef;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;

/**
 * Handles specific layer type: ARE/Map transition
 */
public class LayerObjectTransition extends LayerObject
{
  public static final String[] FIELD_NAME = {
    AreResource.ARE_AREA_NORTH,
    AreResource.ARE_AREA_EAST,
    AreResource.ARE_AREA_SOUTH,
    AreResource.ARE_AREA_WEST,
  };

  private static final Color[] COLOR = {new Color(0xFF404000, true), new Color(0xFF404000, true),
                                        new Color(0xC0808000, true), new Color(0xC0C0C000, true)};
  private static final int WIDTH = 16;    // "width" of the transition polygon

  /** Destination area. */
  private final AreResource destination;
  private final Point[] shapeCoords = {new Point(), new Point(), new Point(), new Point()};
  private final int edge;
  private final TilesetRenderer renderer;

  private final ShapedLayerItem item;

  public LayerObjectTransition(AreResource parent, AreResource destination, int edge, TilesetRenderer renderer)
  {
    super("Transition", AreResource.class, parent);
    this.destination = destination;
    this.edge = Math.min(ViewerConstants.EDGE_WEST, Math.max(ViewerConstants.EDGE_NORTH, edge));
    this.renderer = renderer;
    String msg = null;
    try {
      final ResourceRef ref = (ResourceRef)parent.getAttribute(FIELD_NAME[this.edge]);
      if (ref != null && !ref.isEmpty()) {
        msg = String.format("Transition to %s", ref);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    item = new ShapedLayerItem(destination, msg, null);
    item.setName(getCategory());
    update(1.0);
    item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
    item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
    item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
    item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
    item.setStroked(true);
    item.setFilled(true);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return destination;
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
    if (renderer != null) {
      int mapW = renderer.getMapWidth(true);
      int mapH = renderer.getMapHeight(true);
      switch (edge) {
        case ViewerConstants.EDGE_NORTH:
          shapeCoords[0].x = 0;    shapeCoords[0].y = 0;
          shapeCoords[1].x = mapW; shapeCoords[1].y = 0;
          shapeCoords[2].x = mapW; shapeCoords[2].y = WIDTH;
          shapeCoords[3].x = 0;    shapeCoords[3].y = WIDTH;
          break;
        case ViewerConstants.EDGE_EAST:
          shapeCoords[0].x = mapW - WIDTH; shapeCoords[0].y = 0;
          shapeCoords[1].x = mapW;         shapeCoords[1].y = 0;
          shapeCoords[2].x = mapW;         shapeCoords[2].y = mapH;
          shapeCoords[3].x = mapW - WIDTH; shapeCoords[3].y = mapH;
          break;
        case ViewerConstants.EDGE_SOUTH:
          shapeCoords[0].x = 0;    shapeCoords[0].y = mapH - WIDTH;
          shapeCoords[1].x = mapW; shapeCoords[1].y = mapH - WIDTH;
          shapeCoords[2].x = mapW; shapeCoords[2].y = mapH;
          shapeCoords[3].x = 0;    shapeCoords[3].y = mapH;
          break;
        case ViewerConstants.EDGE_WEST:
          shapeCoords[0].x = 0;     shapeCoords[0].y = 0;
          shapeCoords[1].x = WIDTH; shapeCoords[1].y = 0;
          shapeCoords[2].x = WIDTH; shapeCoords[2].y = mapH;
          shapeCoords[3].x = 0;     shapeCoords[3].y = mapH;
          break;
        default:
          return;
      }
      item.setItemLocation(shapeCoords[0].x, shapeCoords[0].y);
      Polygon poly = createPolygon(shapeCoords, 1.0);
      normalizePolygon(poly);
      item.setShape(poly);
    }
  }
}
