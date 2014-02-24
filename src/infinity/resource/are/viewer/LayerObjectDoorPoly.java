// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.resource.vertex.Vertex;
import infinity.resource.wed.Door;
import infinity.resource.wed.WedResource;

/**
 * Handles specific layer type: WED/Door Polygon
 * @author argent77
 */
public class LayerObjectDoorPoly extends LayerObject
{
  private static final Color[] Color = new Color[]{new Color(0xFF603080, true), new Color(0xFF603080, true),
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
    if (items.length > 0) {
      return items[0];
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
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (mapOrigin != null) {
      for (int i = 0; i < items.length; i++) {
        items[i].setItemLocation(mapOrigin.x + (int)(location[i].x*zoomFactor + (zoomFactor / 2.0)),
                                 mapOrigin.y + (int)(location[i].y*zoomFactor + (zoomFactor / 2.0)));

        Polygon poly = createPolygon(shapeCoords[i], zoomFactor);
        normalizePolygon(poly);
        items[i].setShape(poly);
//        items[i].setShape(createPolygon(shapeCoords[i], zoomFactor));
      }
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

  /**
   * Returns the number of layer items for a specific open/closed state.
   * @param state The state to return the number of layer items for.
   * @return Number of layer items for this state.
   */
  public int getLayerItemCount(int state)
  {
    if (state == LayerManager.Open) {
      return openCount;
    } else if (state == LayerManager.Closed) {
      return items.length - openCount;
    } else {
      return 0;
    }
  }

  /**
   * Returns an array of layer items of the specified state.
   * @param state The state of the layer items (<code>Open</code> or <code>Closed</code>).
   * @return An array of layer items.
   */
  public AbstractLayerItem[] getLayerItems(int state)
  {
    AbstractLayerItem[] retVal = new AbstractLayerItem[getLayerItemCount(state)];
    if (state == LayerManager.Open) {
      System.arraycopy(items, 0, retVal, 0, getLayerItemCount(LayerManager.Open));
    } else if (state == LayerManager.Closed) {
      System.arraycopy(items, getLayerItemCount(LayerManager.Open), retVal, 0, getLayerItemCount(LayerManager.Closed));
    }
    return retVal;
  }

  private void init()
  {
    if (door != null) {
      location = null;
      shapeCoords = null;
      String[] msg = null;
      Polygon[] poly = null;
      Rectangle[] bounds = null;
      int count = 0;
      try {
        int ofsOpen = ((SectionOffset)door.getAttribute("Polygons open offset")).getValue();
        int ofsClosed = ((SectionOffset)door.getAttribute("Polygons closed offset")).getValue();
        int numOpen = ((SectionCount)door.getAttribute("# polygons open")).getValue();
        int numClosed = ((SectionCount)door.getAttribute("# polygons closed")).getValue();
        count = numOpen + numClosed;
        openCount = numOpen;
        location = new Point[count];
        shapeCoords = new Point[count][];
        msg = new String[count];
        poly = new Polygon[count];
        bounds = new Rectangle[count];
        String[] desc = infinity.resource.wed.Polygon.s_flags;

        // processing open door polygons
        for (int i = 0; i < numOpen; i++) {
          infinity.resource.wed.Polygon p = getPolygonStructure(door, ofsOpen, i);
          if (p != null) {
            String s = ((TextString)door.getAttribute("Name")).toString();
            Flag flags = (Flag)p.getAttribute("Polygon flags");
            if (numOpen > 1) {
              msg[i] = String.format("%1$s %2$d/%3$d %4$s", s, i+1, numOpen, createFlags(flags, desc));
            } else {
              msg[i] = String.format("%1$s %2$s", s, createFlags(flags, desc));
            }
            int vNum = ((SectionCount)p.getAttribute("# vertices")).getValue();
            int vOfs = ((HexNumber)getParentStructure().getAttribute("Vertices offset")).getValue();
            shapeCoords[i] = loadVertices(p, vOfs, 0, vNum, Vertex.class);
            poly[i] = createPolygon(shapeCoords[i], 1.0);
            bounds[i] = normalizePolygon(poly[i]);
          }
        }

        // processing closed door polygons
        for (int i = 0; i < numClosed; i++) {
          infinity.resource.wed.Polygon p = getPolygonStructure(door, ofsClosed, i);
          if (p != null) {
            String s = ((TextString)door.getAttribute("Name")).toString();
            Flag flags = (Flag)p.getAttribute("Polygon flags");
            if (numClosed > 1) {
              msg[numOpen+i] = String.format("%1$s %2$d/%3$d %4$s", s, i+1, numClosed, createFlags(flags, desc));
            } else {
              msg[numOpen+i] = String.format("%1$s %2$s", s, createFlags(flags, desc));
            }
            int vNum = ((SectionCount)p.getAttribute("# vertices")).getValue();
            int vOfs = ((HexNumber)getParentStructure().getAttribute("Vertices offset")).getValue();
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

  // Returns the specified WED polygon structure
  private infinity.resource.wed.Polygon getPolygonStructure(AbstractStruct baseStruct, int baseOfs, int index)
  {
//    WedResource wed = (WedResource)getParentStructure();
    if (baseStruct != null) {
      List<StructEntry> list = baseStruct.getList();
      int idx = 0;
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).getOffset() >= baseOfs && list.get(i) instanceof infinity.resource.wed.Polygon) {
          if (idx == index) {
            return (infinity.resource.wed.Polygon)list.get(i);
          }
          idx++;
        }
      }
    }
    return null;
  }

  // Returns a flags string
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
