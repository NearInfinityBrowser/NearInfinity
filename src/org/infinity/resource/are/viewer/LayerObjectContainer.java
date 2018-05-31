// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.TextString;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Container;
import org.infinity.resource.vertex.Vertex;

/**
 * Handles specific layer type: ARE/Container
 */
public class LayerObjectContainer extends LayerObject
{
  private static final String[] TYPE = {"Unknown", "Bag", "Chest", "Drawer", "Pile",
                                        "Table", "Shelf", "Altar", "Invisible",
                                        "Spellbook", "Body", "Barrel", "Crate"};
  private static final Color[] COLOR = {new Color(0xFF004040, true), new Color(0xFF004040, true),
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
        if (type < 0) type = 0; else if (type >= TYPE.length) type = TYPE.length - 1;
        msg = String.format("%s (%s) %s",
                            ((TextString)container.getAttribute(Container.ARE_CONTAINER_NAME)).toString(),
                            TYPE[type], getAttributes());
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
      item = new ShapedLayerItem(location, container, msg, msg, poly);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
    }
  }

  private String getAttributes()
  {
    StringBuilder sb = new StringBuilder();
    if (container != null) {
      sb.append('[');

      boolean isLocked = ((Flag)container.getAttribute(Container.ARE_CONTAINER_FLAGS)).isFlagSet(0);
      if (isLocked) {
        int v = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_LOCK_DIFFICULTY)).getValue();
        if (v > 0) {
          sb.append("Locked (").append(v).append(')');
          String key = ((IsTextual)container.getAttribute(Container.ARE_CONTAINER_KEY)).getText();
          if (!key.isEmpty() && !key.equalsIgnoreCase("NONE")) {
            sb.append(", Key: ").append(key).append(".ITM");
          }
        }
      }

      boolean isTrapped = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_TRAPPED)).getValue() != 0;
      if (isTrapped) {
        int v = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_TRAP_REMOVAL_DIFFICULTY)).getValue();
        if (v > 0) {
          if (sb.length() > 1) sb.append(", ");
          sb.append("Trapped (").append(v).append(')');
        }
      }

      String script = ((IsTextual)container.getAttribute(Container.ARE_CONTAINER_SCRIPT_TRAP)).getText();
      if (!script.isEmpty() && !script.equalsIgnoreCase("NONE")) {
        if (sb.length() > 1) sb.append(", ");
        sb.append("Script: ").append(script).append(".BCS");
      }

      if (sb.length() == 1) sb.append("No Flags");
      sb.append(']');
    }
    return sb.toString();
  }
}
