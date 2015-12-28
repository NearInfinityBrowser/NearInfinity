// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.SectionOffset;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.resource.Viewable;
import infinity.resource.vertex.Vertex;
import infinity.resource.wed.WallPolygon;
import infinity.resource.wed.WedResource;

/**
 * Handles specific layer type: ARE/Wall Polygon
 * @author argent77
 */
public class LayerObjectWallPoly extends LayerObject
{
  private static final Color[] Color = new Color[]{new Color(0xFF005046, true), new Color(0xFF005046, true),
                                                   new Color(0x8020A060, true), new Color(0xA030B070, true)};

  private final WallPolygon wall;
  private final Point location = new Point();

  private ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectWallPoly(WedResource parent, WallPolygon wallPoly)
  {
    super(ViewerConstants.RESOURCE_WED, "Wall Poly", WallPolygon.class, parent);
    this.wall = wallPoly;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return wall;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{wall};
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
    if (wall != null) {
      shapeCoords = null;
      String msg = "";
      Polygon poly = null;
      Rectangle bounds = null;
      int count = 0;
      try {
        int baseOfs = ((SectionOffset)getParentStructure().getAttribute("Wall polygons offset")).getValue();
        int ofs = wall.getOffset();
        count = (ofs - baseOfs) / wall.getSize();
        msg = String.format("Wall polygon #%1$d %2$s", count,
                            createFlags((Flag)wall.getAttribute("Polygon flags"),
                                        infinity.resource.wed.Polygon.s_flags));
        int vNum = ((DecNumber)wall.getAttribute("# vertices")).getValue();
        int vOfs = ((HexNumber)getParentStructure().getAttribute("Vertices offset")).getValue();
        shapeCoords = loadVertices(wall, vOfs, 0, vNum, Vertex.class);
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

      location.x = bounds.x; location.y = bounds.y;
      item = new ShapedLayerItem(location, wall, msg, poly);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, Color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, Color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
    }
  }

  // Returns a flags string
  private String createFlags(Flag flags, String[] desc)
  {
    if (flags != null) {
      int numFlags = 0;
      for (int i = 0, size = flags.getSize() << 3; i < size; i++) {
        if (flags.isFlagSet(i)) {
          numFlags++;
        }
      }

      if (numFlags > 0) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0, size = flags.getSize() << 3; i < size; i++) {
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
