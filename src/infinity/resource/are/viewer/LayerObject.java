// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

import infinity.datatype.DecNumber;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.resource.vertex.Vertex;

/**
 * Common base class for specific layer types.
 * @author argent77
 */
public abstract class LayerObject
{
  private final String category;
  private final Class<? extends AbstractStruct> classType;
  private final AbstractStruct parent;    // base structure (e.g. AreResource or WedResource)

  private boolean visible;

  protected LayerObject(String category, Class<? extends AbstractStruct> classType,
                        AbstractStruct parent)
  {
    this.category = (category != null && !category.isEmpty()) ? category : "Layer object";
    this.classType = (classType != null) ? classType : AbstractStruct.class;
    this.parent = parent;
    visible = false;
  }

  /**
   * Returns the category description of the layer object.
   */
  public String getCategory()
  {
    return category;
  }

  /**
   * Returns the specific class type of the structure associated with the layer object
   * for identification purposes.
   */
  public Class<? extends AbstractStruct> getClassType()
  {
    return classType;
  }

  /**
   * Returns the base structure the current structure belongs to. This can either be an
   * <code>AreResource</code> or a <code>WedResource</code> object.
   */
  public AbstractStruct getParentStructure()
  {
    return parent;
  }

  /**
   * Returns whether the layer item is visible on the map.
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Controls the visibility state of the layer item on the map.
   */
  public void setVisible(boolean state)
  {
    if (state != visible) {
      visible = state;
      AbstractLayerItem[] items = getLayerItems();
      if (items != null) {
        for (int i = 0; i < items.length; i++) {
          items[i].setVisible(visible);
        }
      }
    }
  }


  /**
   * Returns the structure associated with the layer object. If the layer object consists of
   * multiple structures, then the first one available will be returned.
   * @return The structure associated with the layer object.
   */
  public abstract AbstractStruct getStructure();

  /**
   * Returns all structures associated with the layer object. This method is useful for layer objects
   * consisting of multiple structures.
   * @return A list of structures associated with the layer object.
   */
  public abstract AbstractStruct[] getStructures();

  /**
   * Returns the layer item associated with the layer object. If the layer object consists of
   * multiple layer items, then the first one available will be returned.
   * @return The layer item associated with the layer object.
   */
  public abstract AbstractLayerItem getLayerItem();

  /**
   * Returns all layer items associated with the layer object. This method is useful for layer objects
   * consisting of multiple layer items (e.g. door polygons or ambient sounds/sound ranges).
   * @return A list of layer items associated with the layer object.
   */
  public abstract AbstractLayerItem[] getLayerItems();

  /**
   * Reloads structure data and associated layer item(s). Note: {@link #update(Point, double)} has
   * to be called afterwards to account for canvas-specific settings.
   */
  public abstract void reload();

  /**
   * Updates the layer item positions. Takes relative map position on canvas and zoom factor
   * into account. Note: Always call this method after loading/reloading structure data.
   */
  public abstract void update(Point mapOrigin, double zoomFactor);

  /**
   * Returns the original map position of the first available layer item (center or top-left,
   * depending on object type). Note: This is the location specified in the resource structure.
   * The resulting position on the canvas may be different.
   */
  public abstract Point getMapLocation();

  /**
   * Returns the original map positions of all available layer items (center or top-left,
   * depending on object type). Note: This is the location specified in the resource structure.
   * The resulting position on the canvas may be different.
   * @return
   */
  public abstract Point[] getMapLocations();


  /**
   * Loads vertices from the superStruct and stores them in an array of Point objects.
   * @param superStruct The super structure containing the Vertex entries.
   * @param index Index of the first vertex.
   * @param count Number of vertices to load.
   * @parem type The specific vertex type to look for.
   * @return Array of Point objects containing vertex data.
   */
  protected Point[] loadVertices(AbstractStruct superStruct, int baseOfs, int index, int count,
                                 Class<? extends Vertex> type)
  {
    Point[] coords = null;
    if (superStruct != null && index >= 0 && count > 0 && type != null) {
      int idx = 0, cnt = 0;
      coords = new Point[count];
      List<StructEntry> list = superStruct.getList();
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i).getOffset() >= baseOfs && list.get(i).getClass().isAssignableFrom(type)) {
            if (idx >= index) {
              Vertex vertex = (Vertex)list.get(i);
              coords[cnt] = new Point(((DecNumber)vertex.getAttribute("X")).getValue(),
                                      ((DecNumber)vertex.getAttribute("Y")).getValue());
              cnt++;
              if (cnt >= count) {
                break;
              }
            }
            idx++;
          }
        }

      // filling up remaining coordinates with empty values (if any)
      for (int i = cnt; i < coords.length; i++) {
        coords[i] = new Point();
      }
    } else {
      coords = new Point[0];
    }

    return coords;
  }

  /**
   * Creates a polygon object out of the specified coordinates. Uses zoomFactor to scale the result.
   * @param coords Array of coordinates for the polygon.
   * @param zoomFactor Coordinates will be scaled by this value.
   * @return A <code>Polygon</code> object.
   */
  protected Polygon createPolygon(Point[] coords, double zoomFactor)
  {
    Polygon poly = new Polygon();
    if (coords != null) {
      for (int i = 0; i < coords.length; i++) {
        poly.addPoint((int)(coords[i].x*zoomFactor + (zoomFactor / 2.0)),
                      (int)(coords[i].y*zoomFactor + (zoomFactor / 2.0)));
      }
    }
    return poly;
  }

  /**
   * Converts the polygon from a global coordinate system into a local coordinate system where the
   * top/left coordinate is located at [0, 0].
   * @param poly The polygon to normalize (will be processed in place).
   * @return Bounding box of the polygon in global coordinates.
   */
  protected Rectangle normalizePolygon(Polygon poly)
  {
    if (poly != null) {
      Rectangle r = poly.getBounds();
      poly.translate(-r.x, -r.y);
      return r;
    } else {
      return new Rectangle();
    }
  }
}
