// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
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
  private static final Color[] COLOR = {new Color(0xFF004040, true), new Color(0xFF004040, true),
                                        new Color(0xC0008080, true), new Color(0xC000C0C0, true)};

  private final Container container;
  private final Point location = new Point();

  private final ShapedLayerItem item;
  private Point[] shapeCoords;

  public LayerObjectContainer(AreResource parent, Container container)
  {
    super("Container", Container.class, parent);
    this.container = container;
    String msg = null;
    try {
      int type = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_TYPE)).getValue();
      if (type < 0) type = 0; else if (type >= Container.s_type.length) type = Container.s_type.length - 1;
      msg = String.format("%s (%s) %s",
                          container.getAttribute(Container.ARE_CONTAINER_NAME).toString(),
                          Container.s_type[type], getAttributes());
      int vNum = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_NUM_VERTICES)).getValue();
      int vOfs = ((IsNumeric)parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
      shapeCoords = loadVertices(container, vOfs, 0, vNum, Vertex.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    final Polygon poly = createPolygon(shapeCoords, 1.0);
    final Rectangle bounds = normalizePolygon(poly);

    location.x = bounds.x; location.y = bounds.y;
    item = new ShapedLayerItem(container, msg, poly);
    item.setName(getCategory());
    item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
    item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
    item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
    item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
    item.setStroked(true);
    item.setFilled(true);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable()
  {
    return container;
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

  private String getAttributes()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    final boolean isLocked = ((Flag)container.getAttribute(Container.ARE_CONTAINER_FLAGS)).isFlagSet(0);
    if (isLocked) {
      int v = ((IsNumeric)container.getAttribute(Container.ARE_CONTAINER_LOCK_DIFFICULTY)).getValue();
      if (v > 0) {
        sb.append("Locked (").append(v).append(')');
        addResResDesc(sb, container, Container.ARE_CONTAINER_KEY, "Key: ");
      }
    }

    addTrappedDesc(sb, container,
                   Container.ARE_CONTAINER_TRAPPED,
                   Container.ARE_CONTAINER_TRAP_REMOVAL_DIFFICULTY,
                   Container.ARE_CONTAINER_SCRIPT_TRAP);

    if (sb.length() == 1) sb.append("No Flags");
    sb.append(']');
    return sb.toString();
  }
}
