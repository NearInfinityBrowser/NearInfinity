// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.Flag;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
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
  private ShapedLayerItem[] items;
  private Point[][] shapeCoords;
  private int openCount;

  public LayerObjectDoorPoly(WedResource parent, Door doorPoly)
  {
    super("Door Poly", Door.class, parent);
    this.door = doorPoly;
    init();
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
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
    if (items.length > 0) {
      return items[0];
    } else {
      return null;
    }
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
  public void reload()
  {
    init();
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

  @Override
  public Point getMapLocation()
  {
    if (location.length > 0) {
      return location[0];
    } else {
      return null;
    }
  }

  @Override
  public Point[] getMapLocations()
  {
    return location;
  }
  //</editor-fold>

  /**
   * Returns the number of layer items for a specific open/closed state.
   * @param state The state to return the number of layer items for.
   * @return Number of layer items for this state.
   */
  public int getLayerItemCount(int state)
  {
    if (state == ViewerConstants.DOOR_OPEN) {
      return openCount;
    } else if (state == ViewerConstants.DOOR_CLOSED) {
      return items.length - openCount;
    } else {
      return 0;
    }
  }

  /**
   * Returns an array of layer items of the specified state.
   * @param state The state of the layer items ({@code Open} or {@code Closed}).
   * @return An array of layer items.
   */
  public AbstractLayerItem[] getLayerItems(int state)
  {
    AbstractLayerItem[] retVal = new AbstractLayerItem[getLayerItemCount(state)];
    if (state == ViewerConstants.DOOR_OPEN) {
      System.arraycopy(items, 0, retVal, 0, getLayerItemCount(ViewerConstants.DOOR_OPEN));
    } else if (state == ViewerConstants.DOOR_CLOSED) {
      System.arraycopy(items, getLayerItemCount(ViewerConstants.DOOR_OPEN), retVal, 0, getLayerItemCount(ViewerConstants.DOOR_CLOSED));
    }
    return retVal;
  }

  private void init()
  {
    if (door != null) {
      location = null;
      shapeCoords = null;
      String[] info = null;
      String[] msg = null;
      Polygon[] poly = null;
      Rectangle[] bounds = null;
      int count = 0;
      try {
        int ofsOpen = ((SectionOffset)door.getAttribute(Door.WED_DOOR_OFFSET_POLYGONS_OPEN)).getValue();
        int ofsClosed = ((SectionOffset)door.getAttribute(Door.WED_DOOR_OFFSET_POLYGONS_CLOSED)).getValue();
        int numOpen = ((SectionCount)door.getAttribute(Door.WED_DOOR_NUM_POLYGONS_OPEN)).getValue();
        int numClosed = ((SectionCount)door.getAttribute(Door.WED_DOOR_NUM_POLYGONS_CLOSED)).getValue();
        count = numOpen + numClosed;
        openCount = numOpen;
        location = new Point[count];
        shapeCoords = new Point[count][];
        info = new String[count];
        msg = new String[count];
        poly = new Polygon[count];
        bounds = new Rectangle[count];
        String[] desc = org.infinity.resource.wed.Polygon.s_flags;

        // processing open door polygons
        for (int i = 0; i < numOpen; i++) {
          org.infinity.resource.wed.Polygon p = getPolygonStructure(door, ofsOpen, i);
          if (p != null) {
            String s = ((TextString)door.getAttribute(Door.WED_DOOR_NAME)).toString();
            Flag flags = (Flag)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_FLAGS);
            info[i] = s;
            if (numOpen > 1) {
              msg[i] = String.format("%s %d/%d %s", s, i+1, numOpen, createFlags(flags, desc));
            } else {
              msg[i] = String.format("%s %s", s, createFlags(flags, desc));
            }
            int vNum = ((SectionCount)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_NUM_VERTICES)).getValue();
            int vOfs = ((HexNumber)getParentStructure().getAttribute(WedResource.WED_OFFSET_VERTICES)).getValue();
            shapeCoords[i] = loadVertices(p, vOfs, 0, vNum, Vertex.class);
            poly[i] = createPolygon(shapeCoords[i], 1.0);
            bounds[i] = normalizePolygon(poly[i]);
          }
        }

        // processing closed door polygons
        for (int i = 0; i < numClosed; i++) {
          org.infinity.resource.wed.Polygon p = getPolygonStructure(door, ofsClosed, i);
          if (p != null) {
            String s = ((TextString)door.getAttribute(Door.WED_DOOR_NAME)).toString();
            Flag flags = (Flag)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_FLAGS);
            info[numOpen+i] = s;
            if (numClosed > 1) {
              msg[numOpen+i] = String.format("%s %d/%d %s", s, i+1, numClosed, createFlags(flags, desc));
            } else {
              msg[numOpen+i] = String.format("%s %s", s, createFlags(flags, desc));
            }
            int vNum = ((SectionCount)p.getAttribute(org.infinity.resource.wed.Polygon.WED_POLY_NUM_VERTICES)).getValue();
            int vOfs = ((HexNumber)getParentStructure().getAttribute(WedResource.WED_OFFSET_VERTICES)).getValue();
            shapeCoords[numOpen+i] = loadVertices(p, vOfs, 0, vNum, Vertex.class);
            poly[numOpen+i] = createPolygon(shapeCoords[numOpen+i], 1.0);
            bounds[numOpen+i] = normalizePolygon(poly[numOpen+i]);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        if (shapeCoords == null) {
          shapeCoords = new Point[count][];
        }
        if (info == null) {
          info = new String[count];
        }
        if (msg == null) {
          msg = new String[count];
        }
        if (poly == null) {
          poly = new Polygon[count];
        }
        if (bounds == null) {
          bounds = new Rectangle[count];
        }
      }

      // sanity checks
      for (int i = 0; i < count; i++) {
        if (shapeCoords[i] == null) {
          shapeCoords[i] = new Point[0];
        }
        if (info[i] == null) {
          info[i] = "";
        }
        if (msg[i] == null) {
          msg[i] = "";
        }
        if (poly[i] == null) {
          poly[i] = new Polygon();
        }
        if (bounds[i] == null) {
          bounds[i] = new Rectangle();
        }
      }

      // creating layer items
      items = new ShapedLayerItem[count];
      for (int i = 0; i < count; i++) {
        location[i] = new Point(bounds[i].x, bounds[i].y);
        items[i] = new ShapedLayerItem(door, msg[i], info[i], poly[i]);
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
  }

  /** Returns the specified WED polygon structure. */
  private org.infinity.resource.wed.Polygon getPolygonStructure(AbstractStruct baseStruct, int baseOfs, int index)
  {
//    WedResource wed = (WedResource)getParentStructure();
    if (baseStruct != null) {
      int idx = 0;
      for (final StructEntry e : baseStruct.getFields()) {
        if (e.getOffset() >= baseOfs && e instanceof org.infinity.resource.wed.Polygon) {
          if (idx == index) {
            return (org.infinity.resource.wed.Polygon)e;
          }
          idx++;
        }
      }
    }
    return null;
  }

  /** Returns a flags string. */
  private String createFlags(Flag flags, String[] desc)
  {
    if (flags != null) {
      int numFlags = 0;
      for (int i = 0; i < (flags.getSize() << 3); i++) {
        if (flags.isFlagSet(i)) {
          numFlags++;
        }
      }

      if (numFlags > 0) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < (flags.getSize() << 3); i++) {
          if (flags.isFlagSet(i)) {
            numFlags--;
            if (desc != null && i+1 < desc.length) {
              sb.append(desc[i+1]);
            } else {
              sb.append("Bit " + i);
            }
            if (numFlags > 0) {
              sb.append(", ");
            }
          }
        }
        sb.append("]");
        return sb.toString();
      }
    }
    return "[No flags]";
  }
}
