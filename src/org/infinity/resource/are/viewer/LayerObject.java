// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventListener;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.LayerItemListener;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.vertex.Vertex;

/**
 * Common base class for specific layer types.
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
   * Closes and cleans up the current layer object.
   */
  public void close()
  {
    for (final AbstractLayerItem item : getLayerItems()) {
      // removing listeners from layer item
      EventListener[][] listeners = new EventListener[4][];
      listeners[0] = item.getActionListeners();
      listeners[1] = item.getLayerItemListeners();
      listeners[2] = item.getMouseListeners();
      listeners[3] = item.getMouseMotionListeners();
      for (int j = 0; j < listeners.length; j++) {
        if (listeners[j] != null) {
          for (final EventListener l : listeners[j]) {
            switch (j) {
              case 0:
                item.removeActionListener((ActionListener) l);
                break;
              case 1:
                item.removeLayerItemListener((LayerItemListener) l);
                break;
              case 2:
                item.removeMouseListener((MouseListener) l);
                break;
              case 3:
                item.removeMouseMotionListener((MouseMotionListener) l);
                break;
            }
          }
        }
      }
      // removing items from container
      if (item.getParent() != null) {
        item.getParent().remove(item);
      }
    }
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
   * {@code AreResource} or a {@code WedResource} object.
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
      for (final AbstractLayerItem item : getLayerItems()) {
        item.setVisible(visible);
      }
    }
  }


  /**
   * Returns the structure associated with the layer object. If the layer object consists of
   * multiple structures, then the first one available will be returned.
   * @return The structure associated with the layer object.
   */
  public abstract Viewable getViewable();

  /**
   * Returns the specified layer item. {@code type} is layer type specific, usually defined
   * as an identifier in {@code ViewerConstants}.
   * @param type A layer-specific type to identify the item to return.
   * @return The desired layer item, or {@code null} if not available.
   */
  public abstract AbstractLayerItem getLayerItem(int type);

  /**
   * Returns all layer items associated with the layer object. This method is useful for layer objects
   * consisting of multiple layer items (e.g. door polygons or ambient sounds/sound ranges).
   * @return A list of layer items associated with the layer object. Never {@code null}
   *         and array do not contain {@code null}'s
   */
  public abstract AbstractLayerItem[] getLayerItems();

  /**
   * Updates the layer item positions. Takes zoom factor into account.
   * Note: Always call this method after loading/reloading structure data.
   */
  public abstract void update(double zoomFactor);

  /**
   * Returns whether the layer object is active at a specific scheduled time.
   * @param schedule The desired scheduled time index.
   * @return {@code true} if the animation is active at the specified scheduled time,
   *         {@code false} otherwise.
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
   * @param type The specific vertex type to look for.
   * @return Array of Point objects containing vertex data.
   */
  protected static Point[] loadVertices(AbstractStruct superStruct, int baseOfs, int index, int count,
                                        Class<? extends Vertex> type)
  {
    if (superStruct != null && index >= 0 && count > 0 && type != null) {
      int idx = 0, cnt = 0;
      final Point[] coords = new Point[count];
      for (final StructEntry e : superStruct.getFields()) {
        if (e.getOffset() >= baseOfs && type.isAssignableFrom(e.getClass())) {
          if (idx >= index) {
            final Vertex vertex = (Vertex)e;
            coords[cnt] = new Point(((IsNumeric)vertex.getAttribute(Vertex.VERTEX_X)).getValue(),
                                    ((IsNumeric)vertex.getAttribute(Vertex.VERTEX_Y)).getValue());
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
      return coords;
    }
    return new Point[0];
  }

  /**
   * Creates a polygon object out of the specified coordinates. Uses zoomFactor to scale the result.
   * @param coords Array of coordinates for the polygon.
   * @param zoomFactor Coordinates will be scaled by this value.
   * @return A {@code Polygon} object.
   */
  protected static Polygon createPolygon(Point[] coords, double zoomFactor)
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
  protected static Rectangle normalizePolygon(Polygon poly)
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
  protected static boolean isActiveAt(Flag flags, int dayTime)
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

  protected static Image[] getIcons(Image[] defIcons)
  {
    final Image[] icons;
    final String keyIcon = SharedResourceCache.createKey(defIcons[0])
                         + SharedResourceCache.createKey(defIcons[1]);
    if (SharedResourceCache.contains(SharedResourceCache.Type.ICON, keyIcon)) {
      icons = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.ICON, keyIcon)).getData();
      SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon);
    } else {
      icons = defIcons;
      SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon, new ResourceIcon(keyIcon, icons));
    }
    return icons;
  }

  protected static void addResResDesc(StringBuilder sb, AbstractStruct struct, String resRefAttr, String desc)
  {
    final ResourceRef res = (ResourceRef)struct.getAttribute(resRefAttr, false);
    if (res != null && !res.isEmpty()) {
      if (sb.length() > 1) sb.append(", ");
      sb.append(desc).append(res);
    }
  }

  protected static void addTrappedDesc(StringBuilder sb, AbstractStruct struct, String trappedAttr, String difficultyAttr, String scriptAttr)
  {
    final boolean isTrapped = ((IsNumeric)struct.getAttribute(trappedAttr, false)).getValue() != 0;
    if (isTrapped) {
      int v = ((IsNumeric)struct.getAttribute(difficultyAttr, false)).getValue();
      if (v > 0) {
        if (sb.length() > 1) sb.append(", ");
        sb.append("Trapped (").append(v).append(')');
      }
    }
    addResResDesc(sb, struct, scriptAttr, "Script: ");
  }
}
