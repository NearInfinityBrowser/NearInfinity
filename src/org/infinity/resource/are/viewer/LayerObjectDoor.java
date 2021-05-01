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
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Door;
import org.infinity.resource.vertex.ClosedVertex;
import org.infinity.resource.vertex.OpenVertex;

/**
 * Handles specific layer type: ARE/Door
 */
public class LayerObjectDoor extends LayerObject
{
  private static final Color[] COLOR = {new Color(0xFF400040, true), new Color(0xFF400040, true),
                                        new Color(0xC0800080, true), new Color(0xC0C000C0, true)};

  private final Door door;
  private final Point[] location = {new Point(), new Point()};
  private final ShapedLayerItem[] items = new ShapedLayerItem[2];
  private final Point[][] shapeCoords = new Point[2][];

  public LayerObjectDoor(AreResource parent, Door door)
  {
    super("Door", Door.class, parent);
    this.door = door;
    final String[] msg = new String[2];
    try {
      String attr = getAttributes();
      final String name = door.getAttribute(Door.ARE_DOOR_NAME).toString();
      final int vOfs = ((IsNumeric)parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();

      // processing opened state door
      msg[ViewerConstants.DOOR_OPEN] = String.format("%s (Open) %s", name, attr);
      int vNum = ((IsNumeric)door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_OPEN)).getValue();
      shapeCoords[ViewerConstants.DOOR_OPEN] = loadVertices(door, vOfs, 0, vNum, OpenVertex.class);

      // processing closed state door
      msg[ViewerConstants.DOOR_CLOSED] = String.format("%s (Closed) %s", name, attr);
      vNum = ((IsNumeric)door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_CLOSED)).getValue();
      shapeCoords[ViewerConstants.DOOR_CLOSED] = loadVertices(door, vOfs, 0, vNum, ClosedVertex.class);
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (int i = 0; i < 2; i++) {
      final Polygon poly = createPolygon(shapeCoords[i], 1.0);
      final Rectangle bounds = normalizePolygon(poly);

      location[i].x = bounds.x; location[i].y = bounds.y;
      items[i] = new ShapedLayerItem(door, msg[i], poly);
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

  @Override
  public Viewable getViewable()
  {
    return door;
  }

  /**
   * Returns the layer item of specified state.
   * @param type The open/closed state of the item.
   * @return The layer item of the specified state.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (Profile.getEngine() == Profile.Engine.PST) {
      // open/closed states are inverted for PST
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_CLOSED : ViewerConstants.DOOR_OPEN;
    } else {
      type = (type == ViewerConstants.DOOR_OPEN) ? ViewerConstants.DOOR_OPEN : ViewerConstants.DOOR_CLOSED;
    }
    return items[type];
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return items;
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

  private String getAttributes()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    final boolean isLocked = ((Flag)door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(1);
    if (isLocked) {
      int v = ((IsNumeric)door.getAttribute(Door.ARE_DOOR_LOCK_DIFFICULTY)).getValue();
      if (v > 0) {
        sb.append("Locked (").append(v).append(')');
        int bit = (Profile.getEngine() == Profile.Engine.IWD2) ? 14 : 10;
        boolean usesKey = ((Flag)door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(bit);
        if (usesKey) {
          addResResDesc(sb, door, Door.ARE_DOOR_KEY, "Key: ");
        }
      }
    }

    addTrappedDesc(sb, door,
                   Door.ARE_DOOR_TRAPPED,
                   Door.ARE_DOOR_TRAP_REMOVAL_DIFFICULTY,
                   Door.ARE_DOOR_SCRIPT);

    final boolean isSecret = ((Flag)door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(7);
    if (isSecret) {
      if (sb.length() > 1) sb.append(", ");
      sb.append("Secret door");
    }

    if (sb.length() == 1) sb.append("No flags");
    sb.append(']');
    return sb.toString();
  }
}
