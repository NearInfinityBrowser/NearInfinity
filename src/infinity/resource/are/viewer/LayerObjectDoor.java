// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.are.AreResource;
import infinity.resource.are.Door;
import infinity.resource.vertex.ClosedVertex;
import infinity.resource.vertex.OpenVertex;

/**
 * Handles specific layer type: ARE/Door
 * @author argent77
 */
public class LayerObjectDoor extends LayerObject
{
  private static final Color[] Color = new Color[]{new Color(0xFF400040, true), new Color(0xFF400040, true),
                                                   new Color(0xC0800080, true), new Color(0xC0C000C0, true)};

  private final Door door;
  private final Point[] location = new Point[]{new Point(), new Point()};
  private final ShapedLayerItem[] items = new ShapedLayerItem[2];
  private final Point[][] shapeCoords = new Point[2][];

  public LayerObjectDoor(AreResource parent, Door door)
  {
    super("Door", Door.class, parent);
    this.door = door;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return door;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{door};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return items[LayerManager.Open];
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return items;
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (mapOrigin != null) {
      for (int i = 0; i < items.length; i++) {
        if (items[i] != null) {
          items[i].setItemLocation(mapOrigin.x + (int)(location[i].x*zoomFactor + (zoomFactor / 2.0)),
                                   mapOrigin.y + (int)(location[i].y*zoomFactor + (zoomFactor / 2.0)));
          Polygon poly = createPolygon(shapeCoords[i], zoomFactor);
          normalizePolygon(poly);
          items[i].setShape(poly);
        }
      }
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location[0];
  }

  @Override
  public Point[] getMapLocations()
  {
    return location;
  }

  /**
   * Returns the layer item of specified state.
   * @param state The open/closed state of the item.
   * @return The layer item of the specified state.
   */
  public AbstractLayerItem getLayerItem(int state)
  {
    state = (state != LayerManager.Open) ? LayerManager.Closed : LayerManager.Open;
    if (items != null && items.length > state) {
      return items[state];
    } else {
      return null;
    }
  }


  private void init()
  {
    if (door != null) {
      shapeCoords[LayerManager.Open] = null;
      shapeCoords[LayerManager.Closed] = null;
      String[] msg = new String[]{"", ""};
      Polygon[] poly = new Polygon[]{null, null};
      Rectangle[] bounds = new Rectangle[]{null, null};
      try {
        // processing opened state door
        msg[0] = String.format("%1$s (Open)", ((TextString)door.getAttribute("Name")).toString());
        int vNum = ((DecNumber)door.getAttribute("# vertices (open)")).getValue();
        int vOfs = ((HexNumber)getParentStructure().getAttribute("Vertices offset")).getValue();
        shapeCoords[LayerManager.Open] = loadVertices(door, vOfs, 0, vNum, OpenVertex.class);
        poly[LayerManager.Open] = createPolygon(shapeCoords[LayerManager.Open], 1.0);
        bounds[LayerManager.Open] = normalizePolygon(poly[LayerManager.Open]);

        // processing closed state door
        msg[1] = String.format("%1$s (Closed)", ((TextString)door.getAttribute("Name")).toString());
        vNum = ((DecNumber)door.getAttribute("# vertices (closed)")).getValue();
        vOfs = ((HexNumber)getParentStructure().getAttribute("Vertices offset")).getValue();
        shapeCoords[LayerManager.Closed] = loadVertices(door, vOfs, 0, vNum, ClosedVertex.class);
        poly[LayerManager.Closed] = createPolygon(shapeCoords[LayerManager.Closed], 1.0);
        bounds[LayerManager.Closed] = normalizePolygon(poly[LayerManager.Closed]);
      } catch (Exception e) {
        e.printStackTrace();
        for (int i = 0; i < 2; i++) {
          if (shapeCoords[i] == null) {
            shapeCoords[i] = new Point[0];
          }
          if (poly[i] == null) {
            poly[i] = new Polygon();
          }
          if (bounds[i] == null) {
            bounds[i] = new Rectangle();
          }
        }
      }

      for (int i = 0; i < 2; i++) {
        location[i].x = bounds[i].x; location[i].y = bounds[i].y;
        items[i] = new ShapedLayerItem(location[i], door, msg[i], poly[i]);
        items[i].setName(getCategory());
        items[i].setToolTipText(msg[i]);
        items[i].setStrokeColor(AbstractLayerItem.ItemState.NORMAL, Color[0]);
        items[i].setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[1]);
        items[i].setFillColor(AbstractLayerItem.ItemState.NORMAL, Color[2]);
        items[i].setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[3]);
        items[i].setStroked(true);
        items[i].setFilled(true);
        items[i].setVisible(isVisible());
      }
    }
  }
}
