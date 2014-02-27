// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;

import infinity.datatype.ResourceRef;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.are.AreResource;

/**
 * Handles specific layer type: ARE/Map transition
 * @author argent77
 */
public class LayerObjectTransition extends LayerObject
{
  public static final String[] FieldName = new String[]{"Area north", "Area east", "Area south", "Area west"};

  private static final Color[] Color = new Color[]{new Color(0xFF404000, true), new Color(0xFF404000, true),
                                                   new Color(0xC0808000, true), new Color(0xC0C0C000, true)};
  private static final int Width = 16;    // "width" of the transition polygon

  private final AreResource are;
  private final Point location = new Point();
  private final Point[] shapeCoords = new Point[]{new Point(), new Point(), new Point(), new Point()};
  private final int edge;
  private final TilesetRenderer renderer;

  private ShapedLayerItem item;

  public LayerObjectTransition(AreResource parent, AreResource are, int edge, TilesetRenderer renderer)
  {
    super("Transition", AreResource.class, parent);
    this.are = are;
    this.edge = Math.min(ViewerConstants.EDGE_WEST, Math.max(ViewerConstants.EDGE_NORTH, edge));
    this.renderer = renderer;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return are;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{are};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
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
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (item != null && renderer != null) {
      int mapW = renderer.getMapWidth(true);
      int mapH = renderer.getMapHeight(true);
      switch (edge) {
        case ViewerConstants.EDGE_NORTH:
          shapeCoords[0].x = 0;    shapeCoords[0].y = 0;
          shapeCoords[1].x = mapW; shapeCoords[1].y = 0;
          shapeCoords[2].x = mapW; shapeCoords[2].y = Width;
          shapeCoords[3].x = 0;    shapeCoords[3].y = Width;
          break;
        case ViewerConstants.EDGE_EAST:
          shapeCoords[0].x = mapW - Width; shapeCoords[0].y = 0;
          shapeCoords[1].x = mapW;         shapeCoords[1].y = 0;
          shapeCoords[2].x = mapW;         shapeCoords[2].y = mapH;
          shapeCoords[3].x = mapW - Width; shapeCoords[3].y = mapH;
          break;
        case ViewerConstants.EDGE_SOUTH:
          shapeCoords[0].x = 0;    shapeCoords[0].y = mapH - Width;
          shapeCoords[1].x = mapW; shapeCoords[1].y = mapH - Width;
          shapeCoords[2].x = mapW; shapeCoords[2].y = mapH;
          shapeCoords[3].x = 0;    shapeCoords[3].y = mapH;
          break;
        case ViewerConstants.EDGE_WEST:
          shapeCoords[0].x = 0;     shapeCoords[0].y = 0;
          shapeCoords[1].x = Width; shapeCoords[1].y = 0;
          shapeCoords[2].x = Width; shapeCoords[2].y = mapH;
          shapeCoords[3].x = 0;     shapeCoords[3].y = mapH;
          break;
        default:
          return;
      }
      item.setItemLocation(shapeCoords[0].x, shapeCoords[0].y);
      Polygon poly = createPolygon(shapeCoords, 1.0);
      normalizePolygon(poly);
      item.setShape(poly);
//      item.setShape(createPolygon(shapeCoords, 1.0));
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

  /**
   * Returns the edge of the map this transition is location.
   */
  public int getEdge()
  {
    return edge;
  }

  private void init()
  {
    if (getParentStructure() instanceof AreResource && are != null && renderer != null) {
      AreResource parent = (AreResource)getParentStructure();
      String msg = "";
      try {
        ResourceRef ref = (ResourceRef)parent.getAttribute(FieldName[edge]);
        if (ref != null && !ref.getResourceName().isEmpty() &&
            !"None".equalsIgnoreCase(ref.getResourceName())) {
          msg = String.format("Transition to %1$s", ref.getResourceName());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new ShapedLayerItem(location, are, msg);
      item.setName(getCategory());
      item.setToolTipText(msg);
      update(new Point(), 1.0);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, Color[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, Color[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, Color[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
    }
  }
}
