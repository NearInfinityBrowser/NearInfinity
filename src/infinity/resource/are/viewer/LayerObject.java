// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
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
  private final int resourceType;
  private final String category;
  private final Class<? extends AbstractStruct> classType;
  private final AbstractStruct parent;    // base structure (e.g. AreResource or WedResource)

  private boolean visible;

  protected LayerObject(int resourceType, String category, Class<? extends AbstractStruct> classType,
                        AbstractStruct parent)
  {
    this.resourceType = resourceType;
    this.category = (category != null && !category.isEmpty()) ? category : "Layer object";
    this.classType = (classType != null) ? classType : AbstractStruct.class;
    this.parent = parent;
    visible = false;
  }

  /**
   * Returns the type of the parent resource (either RESOURCE_ARE or RESOURCE_WED).
   */
  public int getResourceType()
  {
    return resourceType;
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
   * Returns the specified layer item. <code>type</code> is layer type specific, usually defined
   * as an identifier in <code>ViewerConstants</code>.
   * @param type A layer-specific type to identify the item to return.
   * @return The desired layer item, or <code>null</code> if not available.
   */
  public abstract AbstractLayerItem getLayerItem(int type);

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
   * Updates the layer item positions. Takes zoom factor into account.
   * Note: Always call this method after loading/reloading structure data.
   */
  public abstract void update(double zoomFactor);

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
   * Returns whether the layer object is active at a specific scheduled time.
   * @param time The desired scheduled time index.
   * @return <code>true</code> if the animation is active at the specified scheduled time,
   *         <code>false</code> otherwise.
   */
  public boolean isScheduled(int schedule)
  {
    // Default implementation: always active
    return true;
  }

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

  /**
   * A helper method for easily finding out whether the object is active during the specified day time.
   * @param flags The appearance schedule.
   * @param dayTime The desired day time.
   */
  protected boolean isActiveAt(Flag flags, int dayTime)
  {
    if (flags != null && flags.getSize() > 2) {
      if (dayTime == ViewerConstants.LIGHTING_NIGHT) {
        // Night: 21:30..06:29
        return (flags.isFlagSet(0) || flags.isFlagSet(1) || flags.isFlagSet(2) ||
                flags.isFlagSet(3) || flags.isFlagSet(4) || flags.isFlagSet(5) ||
                flags.isFlagSet(21) || flags.isFlagSet(22) || flags.isFlagSet(23));
      } else {
        // Day: 06:30..21:29
        return (flags.isFlagSet(6) || flags.isFlagSet(7) || flags.isFlagSet(8) ||
            flags.isFlagSet(9) || flags.isFlagSet(10) || flags.isFlagSet(11) ||
            flags.isFlagSet(12) || flags.isFlagSet(13) || flags.isFlagSet(14) ||
            flags.isFlagSet(15) || flags.isFlagSet(16) || flags.isFlagSet(17) ||
            flags.isFlagSet(18) || flags.isFlagSet(19) || flags.isFlagSet(20));
      }
    } else {
      return false;
    }
  }
}
