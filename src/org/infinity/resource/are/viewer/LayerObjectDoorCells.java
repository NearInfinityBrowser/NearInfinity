// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.*;
import java.util.stream.Collectors;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Door;
import org.infinity.resource.vertex.ClosedVertexImpeded;
import org.infinity.resource.vertex.OpenVertexImpeded;
import org.tinylog.Logger;

/**
 * Handles specific layer subtype: ARE/Door blocked cells
 */
public class LayerObjectDoorCells extends LayerObject {
  private static final Color[] COLOR = { new Color(0xFF004000, true), new Color(0xFF004000, true),
                                         new Color(0xC000C000, true), new Color(0xC000FF00, true) };

  private final HashMap<Integer, DoorInfo> doorMap = new HashMap<>(4);
  private final Door door;

  /**
   * @param parent
   * @param door
   */
  public LayerObjectDoorCells(AreResource parent, Door door) {
    super("Door", Door.class, parent);
    this.door = door;
    final DoorInfo doorOpen = new DoorInfo();
    doorMap.put(ViewerConstants.DOOR_OPEN, doorOpen);
    final DoorInfo doorClosed = new DoorInfo();
    doorMap.put(ViewerConstants.DOOR_CLOSED, doorClosed);
    try {
      String attr = LayerObjectDoor.getAttributes(this.door);
      final String name = door.getAttribute(Door.ARE_DOOR_NAME).toString();
      final int cOfs = ((IsNumeric) parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();

      // processing opened state door cells
      doorOpen.setMessage(String.format("%s (Open) %s", name, attr));
      int cNum = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_IMPEDED_OPEN)).getValue();
      Point[][] itemCoords = createCellPolygons(loadVertices(door, cOfs, 0, cNum, OpenVertexImpeded.class));
      doorOpen.setCoords(itemCoords);

      // processing closed state door cells
      doorClosed.setMessage(String.format("%s (Closed) %s", name, attr));
      cNum = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_IMPEDED_CLOSED)).getValue();
      itemCoords = createCellPolygons(loadVertices(door, cOfs, 0, cNum, ClosedVertexImpeded.class));
      doorClosed.setCoords(itemCoords);
    } catch (Exception e) {
      Logger.error(e);
    }

    for (final DoorInfo info: getDoors()) {
      final Point[][] coords = info.getCoords();
      final ShapedLayerItem[] items = new ShapedLayerItem[coords.length];
      final Point[] locations = new Point[coords.length];
      for (int i = 0; i < coords.length; i++) {
        final Polygon poly = createPolygon(coords[i], 1.0);
        final Rectangle bounds = normalizePolygon(poly);

        locations[i] = new Point(bounds.x, bounds.y);
        final ShapedLayerItem cell = new ShapedLayerItem(this.door, info.getMessage(), poly);
        cell.setName(getCategory());
        cell.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
        cell.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
        cell.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
        cell.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
        cell.setStroked(false);
        cell.setFilled(true);
        cell.setVisible(isVisible());
        items[i] = cell;
      }
      info.setCellItems(items);
      info.setLocations(locations);
    }
  }

  @Override
  public Viewable getViewable() {
    return door;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    boolean isClosed = (type & ViewerConstants.DOOR_CLOSED) == ViewerConstants.DOOR_CLOSED;
    boolean isOpen = (type & ViewerConstants.DOOR_OPEN) == ViewerConstants.DOOR_OPEN;

    if (Profile.getEngine() == Profile.Engine.PST) {
      // open/closed states are inverted for PST
      boolean tmp = isClosed;
      isClosed = isOpen;
      isOpen = tmp;
    }

    List<AbstractLayerItem> list = new ArrayList<>();
    if (isOpen) {
      final DoorInfo info = getDoor(ViewerConstants.DOOR_OPEN);
      if (info != null && info.getCellItems() != null) {
        final ShapedLayerItem[] items = info.getCellItems();
        list.addAll(Arrays.asList(items));
      }
    }
    if (isClosed) {
      final DoorInfo info = getDoor(ViewerConstants.DOOR_CLOSED);
      if (info != null && info.getCellItems() != null) {
        final ShapedLayerItem[] items = info.getCellItems();
        list.addAll(Arrays.asList(items));
      }
    }

    return list.toArray(new AbstractLayerItem[0]);
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    List<AbstractLayerItem> list = new ArrayList<>();

    for (final AbstractLayerItem[] items : getDoorItems()) {
      if (items != null) {
        list.addAll(Arrays.asList(items));
      }
    }

    return list.toArray(new AbstractLayerItem[0]);
  }

  @Override
  public void update(double zoomFactor) {
    for (final DoorInfo info : getDoors()) {
      final ShapedLayerItem[] items = info.getCellItems();
      final Point[] locations = info.getLocations();
      final Point[][] coords = info.getCoords();

      for (int i = 0; i < items.length; i++) {
        items[i].setItemLocation((int) (locations[i].x * zoomFactor + (zoomFactor / 2.0)),
            (int) (locations[i].y * zoomFactor + (zoomFactor / 2.0)));
        final Polygon poly = createPolygon(coords[i], zoomFactor);
        normalizePolygon(poly);
        items[i].setShape(poly);
      }
    }
  }

  private Collection<DoorInfo> getDoors() {
    return doorMap.values();
  }

  private Collection<ShapedLayerItem[]> getDoorItems() {
    return doorMap.values().stream().map(DoorInfo::getCellItems).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private DoorInfo getDoor(int id) {
    return doorMap.get(id);
  }

  /**
   * Creates polygons out of the given cell block coordinates and returns them as arrays of Point objects (one array per
   * polygon).
   *
   * @param cells Array of search map coordinates for blocked cells.
   * @return Two-dimensional array of polygon points. First array dimension indicates the polygon, second dimension
   *         indicates coordinates for the polygon.
   */
  private Point[][] createCellPolygons(Point[] cells) {
    List<Point[]> polys = new ArrayList<>();

    // add one polygon per cell
    // TODO: combine as many cells as possible into one polygon to improve performance
    if (cells != null && cells.length > 0) {
      final int cw = 16;  // search map cell width, in pixels
      final int ch = 12;  // search map cell height, in pixels
      for (Point cell : cells) {
        final int x = cell.x * cw;
        final int y = cell.y * ch;
        polys.add(new Point[]{new Point(x, y), new Point(x + cw, y), new Point(x + cw, y + ch), new Point(x, y + ch)});
      }
    }

    Point[][] retVal = new Point[polys.size()][];
    for (int i = 0, size = polys.size(); i < size; i++) {
      retVal[i] = polys.get(i);
    }
    return retVal;
  }

  // ----------------------------- INNER CLASSES -----------------------------

  /** Storage for open/close-based door cell information. */
  private static class DoorInfo {
    private String message;
    private ShapedLayerItem[] cells;
    private Point[] locations;
    private Point[][] coords;

    public DoorInfo() {}

    /** Returns a message or label associated with the door. */
    public String getMessage() {
      return message;
    }

    /** Defines a message or label associated with the door. */
    public DoorInfo setMessage(String msg) {
      this.message = msg;
      return this;
    }

    /** Returns the layer for blocked door cells. */
    public ShapedLayerItem[] getCellItems() {
      return this.cells;
    }

    /** Defines the layer items for blocked door cells. */
    public DoorInfo setCellItems(ShapedLayerItem[] cells) {
      this.cells = cells;
      return this;
    }

    /** Returns the origin of all blocked door cells. */
    public Point[] getLocations() {
      return this.locations;
    }

    /** Defines the origin of all blocked door cells. */
    public DoorInfo setLocations(Point[] locations) {
      this.locations = locations;
      return this;
    }

    /** Returns the vertices for all blocked door cells. */
    public Point[][] getCoords() {
      return this.coords;
    }

    /** Defines the vertices for all blocked door cells. */
    public DoorInfo setCoords(Point[][] coords) {
      this.coords = coords;
      return this;
    }
  }
}
