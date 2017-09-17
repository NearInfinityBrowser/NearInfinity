// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Door;
import org.infinity.resource.vertex.ClosedVertex;
import org.infinity.resource.vertex.OpenVertex;

/**
 * Handles specific layer type: ARE/Door
 */
public class LayerObjectDoor extends LayerObject
{
  private static final Color[] Color = {new Color(0xFF400040, true), new Color(0xFF400040, true),
                                        new Color(0xC0800080, true), new Color(0xC0C000C0, true)};

  private final Door door;
  private final Point[] location = {new Point(), new Point()};
  private final ShapedLayerItem[] items = new ShapedLayerItem[2];
  private final Point[][] shapeCoords = new Point[2][];

  public LayerObjectDoor(AreResource parent, Door door)
  {
    super(ViewerConstants.RESOURCE_ARE, "Door", Door.class, parent);
    this.door = door;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return door;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{door};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return items[ViewerConstants.DOOR_OPEN];
  }

  /**
   * Returns the layer item of specified state.
   * @param type The open/closed state of the item.
   * @return The layer item of the specified state.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (Profile.getEngine() == Profile.Engine.PST) {
      // open/closed states are inverted for PST
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
    } else {
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_OPEN : ViewerConstants.DOOR_CLOSED;
    }
    if (items != null && items.length > type) {
      return items[type];
    } else {
      return null;
    }
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
  public void update(double zoomFactor)
  {
    for (int i = 0; i < items.length; i++) {
      if (items[i] != null) {
        items[i].setItemLocation((int)(location[i].x*zoomFactor + (zoomFactor / 2.0)),
                                 (int)(location[i].y*zoomFactor + (zoomFactor / 2.0)));
        Polygon poly = createPolygon(shapeCoords[i], zoomFactor);
        normalizePolygon(poly);
        items[i].setShape(poly);
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


  private void init()
  {
    if (door != null) {
      shapeCoords[ViewerConstants.DOOR_OPEN] = null;
      shapeCoords[ViewerConstants.DOOR_CLOSED] = null;
      String[] msg = new String[]{"", ""};
      Polygon[] poly = new Polygon[]{null, null};
      Rectangle[] bounds = new Rectangle[]{null, null};
      try {
        // processing opened state door
        msg[0] = String.format("%1$s (Open)", ((TextString)door.getAttribute(Door.ARE_DOOR_NAME)).toString());
        int vNum = ((DecNumber)door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_OPEN)).getValue();
        int vOfs = ((HexNumber)getParentStructure().getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
        shapeCoords[ViewerConstants.DOOR_OPEN] = loadVertices(door, vOfs, 0, vNum, OpenVertex.class);
        poly[ViewerConstants.DOOR_OPEN] = createPolygon(shapeCoords[ViewerConstants.DOOR_OPEN], 1.0);
        bounds[ViewerConstants.DOOR_OPEN] = normalizePolygon(poly[ViewerConstants.DOOR_OPEN]);

        // processing closed state door
        msg[1] = String.format("%1$s (Closed)", ((TextString)door.getAttribute(Door.ARE_DOOR_NAME)).toString());
        vNum = ((DecNumber)door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_CLOSED)).getValue();
        vOfs = ((HexNumber)getParentStructure().getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
        shapeCoords[ViewerConstants.DOOR_CLOSED] = loadVertices(door, vOfs, 0, vNum, ClosedVertex.class);
        poly[ViewerConstants.DOOR_CLOSED] = createPolygon(shapeCoords[ViewerConstants.DOOR_CLOSED], 1.0);
        bounds[ViewerConstants.DOOR_CLOSED] = normalizePolygon(poly[ViewerConstants.DOOR_CLOSED]);
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
        items[i] = new ShapedLayerItem(location[i], door, msg[i], msg[i], poly[i]);
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
