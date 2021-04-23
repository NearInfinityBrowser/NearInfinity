// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.vertex.Vertex;
import org.infinity.resource.wed.Door;
import org.infinity.resource.wed.WedResource;

/**
 * Handles specific layer type: WED/Door Polygon.
 * <p>
 * Polygon represents clickable area for interaction with door. As a rule, closed
 * state geometry of door polygon matches opened state geometry of the
 * {@link LayerObjectDoor door itself} and vice versa.
 */
public class LayerObjectDoorPoly extends LayerObject
{
  private static final Color[] COLOR = {new Color(0xFF603080, true), new Color(0xFF603080, true),
                                        new Color(0x80A050C0, true), new Color(0xC0C060D0, true)};

  private final Door door;

  private Point[] location;
  private final ShapedLayerItem[] items;
  private Point[][] shapeCoords;
  private int openCount;

  public LayerObjectDoorPoly(WedResource parent, Door doorPoly)
  {
    super("Door Poly", Door.class, parent);
    this.door = doorPoly;
    location = null;
    String[] info = null;
    int count = 0;
    try {
      final int ofsOpen   = ((IsNumeric)door.getAttribute(Door.WED_DOOR_OFFSET_POLYGONS_OPEN)).getValue();
      final int ofsClosed = ((IsNumeric)door.getAttribute(Door.WED_DOOR_OFFSET_POLYGONS_CLOSED)).getValue();
      final int numOpen   = ((IsNumeric)door.getAttribute(Door.WED_DOOR_NUM_POLYGONS_OPEN)).getValue();
      final int numClosed = ((IsNumeric)door.getAttribute(Door.WED_DOOR_NUM_POLYGONS_CLOSED)).getValue();
      count = numOpen + numClosed;
      openCount = numOpen;
      location = new Point[count];
      shapeCoords = new Point[count][];
      info = new String[count];

      // processing open door polygons
      fillData(ofsOpen  , numOpen  ,       0, info);
      // processing closed door polygons
      fillData(ofsClosed, numClosed, numOpen, info);
    } catch (Exception e) {
      e.printStackTrace();
      if (info == null) {
        info = new String[count];
      }
    }

    // creating layer items
    items = new ShapedLayerItem[count];
    for (int i = 0; i < count; i++) {
      final Polygon poly = createPolygon(shapeCoords[i], 1.0);
      final Rectangle bounds = normalizePolygon(poly);

      location[i] = bounds.getLocation();
      items[i] = new ShapedLayerItem(door, info[i], poly);
      items[i].setName(getCategory());
      items[i].setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
      items[i].setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
      items[i].setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
      items[i].setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
      items[i].setStroked(true);
      items[i].setFilled(true);
      items[i].setVisible(isVisible());
    }
  }

  @Override
  public Viewable getViewable()
  {
    return door;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (Profile.getEngine() == Profile.Engine.PST) {
      // open/closed states are inverted for PST
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
    } else {
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_OPEN : ViewerConstants.DOOR_CLOSED;
    }
    if (type == ViewerConstants.DOOR_OPEN) {
      return (openCount > 0) ? items[0] : null;
    } else {
      return (items.length - openCount > 0) ? items[openCount] : null;
    }
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return items;
  }

  @Override
  public void update(double zoomFactor)
  {
    for (int i = 0; i < items.length; i++) {
      items[i].setItemLocation((int)(location[i].x*zoomFactor + (zoomFactor / 2.0)),
                               (int)(location[i].y*zoomFactor + (zoomFactor / 2.0)));

      Polygon poly = createPolygon(shapeCoords[i], zoomFactor);
      normalizePolygon(poly);
      items[i].setShape(poly);
    }
  }

  /**
   * Returns an array of layer items of the specified state.
   * @param state The state of the layer items ({@code Open} or {@code Closed}).
   * @return An array of layer items.
   */
  public AbstractLayerItem[] getLayerItems(int state)
  {
    if (state == ViewerConstants.DOOR_OPEN) {
      return Arrays.copyOf(items, openCount);
    }
    if (state == ViewerConstants.DOOR_CLOSED) {
      return Arrays.copyOfRange(items, openCount, items.length);
    }
    return new AbstractLayerItem[0];
  }

  /**
   * Fills arrays with information about doors.
   *
   * @param start First polygon index in the {@link #door}
   * @param count Count of polygons for the door
   * @param offset Offset to first filled items into arrays
   * @param info Information for infopanel and tooltip. Output parameter
   */
  private void fillData(int start, int count, int offset, String[] info)
  {
    final String name = door.getAttribute(Door.WED_DOOR_NAME).toString();
    final int vOfs = ((IsNumeric)getParentStructure().getAttribute(WedResource.WED_OFFSET_VERTICES, false)).getValue();
    // processing closed door polygons
    for (int i = 0; i < count; i++) {
      final org.infinity.resource.wed.Polygon p = getPolygonStructure(door, start, i);
      if (p == null) { continue; }

      final Flag flags = (Flag)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_FLAGS, false);
      final int index = offset + i;
      if (count > 1) {
        info[index] = String.format("%s: %s %d/%d %s", door.getName(), name, i+1, count, flags);
      } else {
        info[index] = String.format("%s: %s %s", door.getName(), name, flags);
      }
      final int vNum = ((IsNumeric)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_NUM_VERTICES, false)).getValue();
      shapeCoords[index] = loadVertices(p, vOfs, 0, vNum, Vertex.class);
    }
  }

  /** Returns the specified WED polygon structure. */
  private static org.infinity.resource.wed.Polygon getPolygonStructure(AbstractStruct baseStruct, int baseOfs, int index)
  {
    int idx = 0;
    for (final StructEntry e : baseStruct.getFields()) {
      if (e.getOffset() >= baseOfs && e instanceof org.infinity.resource.wed.Polygon) {
        if (idx == index) {
          return (org.infinity.resource.wed.Polygon)e;
        }
        idx++;
      }
    }
    return null;
  }
}
