// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.resource.Viewable;
import infinity.resource.are.AreResource;
import infinity.resource.are.Container;
import infinity.resource.vertex.Vertex;

/**
 * Handles specific layer type: ARE/Container
 * @author argent77
 */
public class LayerObjectContainer extends LayerObject
{
  private static final String[] Type = new String[]{"Unknown", "Bag", "Chest", "Drawer", "Pile",
                                                    "Table", "Shelf", "Altar", "Invisible",
                                                    "Spellbook", "Body", "Barrel", "Crate"};
  private static final Color[] Color = new Color[]{new Color(0xFF004040, true), new Color(0xFF004040, true),
                                                   new Color(0xC0008080, true), new Color(0xC000C0C0, true)};

  private final Container container;
  private final Point location = new Point();

  private ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectContainer(AreResource parent, Container container)
  {
    super(ViewerConstants.RESOURCE_ARE, "Container", Container.class, parent);
    this.container = container;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return container;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{container};
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

  /**
   * Returns vertices of the polygon used to define the container shape.
   */
  public Point[] getShapeCoords()
  {
    return shapeCoords;
  }

  private void init()
  {
    if (container != null) {
      shapeCoords = null;
      String msg = "";
      Polygon poly = null;
      Rectangle bounds = null;
      try {
        int type = ((Bitmap)container.getAttribute(Container.ARE_CONTAINER_TYPE)).getValue();
        if (type < 0) type = 0; else if (type >= Type.length) type = Type.length - 1;
        msg = String.format("%1$s (%2$s)", ((TextString)container.getAttribute(Container.ARE_CONTAINER_NAME)).toString(), Type[type]);
        int vNum = ((DecNumber)container.getAttribute(Container.ARE_CONTAINER_NUM_VERTICES)).getValue();
        int vOfs = ((HexNumber)getParentStructure().getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
        shapeCoords = loadVertices(container, vOfs, 0, vNum, Vertex.class);
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
      item = new ShapedLayerItem(location, container, msg, poly);
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
}
