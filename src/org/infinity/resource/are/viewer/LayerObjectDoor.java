// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Door;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.vertex.ClosedVertex;
import org.infinity.resource.vertex.OpenVertex;

/**
 * Handles specific layer type: ARE/Door
 */
public class LayerObjectDoor extends LayerObject {
  private static final Image[] ICONS_CLOSE = { ViewerIcons.ICON_ITM_DOOR_TARGET_C_1.getIcon().getImage(),
                                               ViewerIcons.ICON_ITM_DOOR_TARGET_C_2.getIcon().getImage() };

  private static final Image[] ICONS_OPEN = { ViewerIcons.ICON_ITM_DOOR_TARGET_O_1.getIcon().getImage(),
                                              ViewerIcons.ICON_ITM_DOOR_TARGET_O_2.getIcon().getImage() };

  private static final Image[] ICONS_LAUNCH = { ViewerIcons.ICON_ITM_DOOR_TARGET_L_1.getIcon().getImage(),
                                                ViewerIcons.ICON_ITM_DOOR_TARGET_L_2.getIcon().getImage() };

  private static final Point CENTER = new Point(13, 29);

  private static final Color[] COLOR = { new Color(0xFF400040, true), new Color(0xFF400040, true),
                                         new Color(0xC0800080, true), new Color(0xC0C000C0, true) };

  private final HashMap<Integer, DoorInfo> doorMap = new HashMap<>(4);
  private final Door door;

  private final IconLayerItem itemIconClose;
  private final Point closePoint = new Point();

  private final IconLayerItem itemIconOpen;
  private final Point openPoint = new Point();

  private final IconLayerItem itemIconLaunch;
  private final Point launchPoint = new Point();

  public LayerObjectDoor(AreResource parent, Door door) {
    super("Door", Door.class, parent);
    this.door = door;
    final DoorInfo doorOpen = new DoorInfo();
    doorMap.put(Integer.valueOf(ViewerConstants.DOOR_OPEN), doorOpen);
    final DoorInfo doorClosed = new DoorInfo();
    doorMap.put(Integer.valueOf(ViewerConstants.DOOR_CLOSED), doorClosed);
    String label = null;
    try {
      String attr = getAttributes();
      final String name = door.getAttribute(Door.ARE_DOOR_NAME).toString();
      final int vOfs = ((IsNumeric) parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();

      // processing opened state door
      doorOpen.setMessage(String.format("%s (Open) %s", name, attr));
      int vNum = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_OPEN)).getValue();
      doorOpen.setCoords(loadVertices(door, vOfs, 0, vNum, OpenVertex.class));

      // processing closed state door
      doorClosed.setMessage(String.format("%s (Closed) %s", name, attr));
      vNum = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_NUM_VERTICES_CLOSED)).getValue();
      doorClosed.setCoords(loadVertices(door, vOfs, 0, vNum, ClosedVertex.class));

      label = door.getAttribute(Door.ARE_DOOR_NAME).toString();
      closePoint.x = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LOCATION_CLOSE_X)).getValue();
      closePoint.y = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LOCATION_CLOSE_Y)).getValue();
      openPoint.x = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LOCATION_OPEN_X)).getValue();
      openPoint.y = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LOCATION_OPEN_Y)).getValue();
      launchPoint.x = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LAUNCH_POINT_X)).getValue();
      launchPoint.y = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LAUNCH_POINT_Y)).getValue();
    } catch (Exception e) {
      e.printStackTrace();
    }

    for (final DoorInfo info: getDoors()) {
      final Polygon poly = createPolygon(info.getCoords(), 1.0);
      final Rectangle bounds = normalizePolygon(poly);

      info.setLocation(new Point(bounds.x, bounds.y));
      final ShapedLayerItem item = new ShapedLayerItem(door, info.getMessage(), poly);
      item.setName(getCategory());
      item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
      item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
      item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
      item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
      item.setStroked(true);
      item.setFilled(true);
      item.setVisible(isVisible());
      info.setItem(item);
    }

    itemIconClose = createValidatedLayerItem(closePoint, label, getIcons(ICONS_CLOSE));
    itemIconOpen = createValidatedLayerItem(openPoint, label, getIcons(ICONS_OPEN));
    itemIconLaunch = createValidatedLayerItem(launchPoint, label, getIcons(ICONS_LAUNCH));
  }

  @Override
  public Viewable getViewable() {
    return door;
  }

  /**
   * Returns the layer item of specified state.
   *
   * @param type The open/closed state of the item.
   * @return The layer item of the specified state.
   */
  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    boolean isClosed = (type & ViewerConstants.DOOR_CLOSED) == ViewerConstants.DOOR_CLOSED;
    boolean isOpen = (type & ViewerConstants.DOOR_OPEN) == ViewerConstants.DOOR_OPEN;
    boolean isIcon = (type & ViewerConstants.LAYER_ITEM_ICON) == ViewerConstants.LAYER_ITEM_ICON;
    boolean isPoly = (type & ViewerConstants.LAYER_ITEM_POLY) == ViewerConstants.LAYER_ITEM_POLY;

    if (Profile.getEngine() == Profile.Engine.PST) {
      // open/closed states are inverted for PST
      boolean tmp = isClosed;
      isClosed = isOpen;
      isOpen = tmp;
    }

    List<AbstractLayerItem> list = new ArrayList<>();

    if (isIcon) {
      if (itemIconClose != null) {
        list.add(itemIconClose);
      }
      if (itemIconOpen != null) {
        list.add(itemIconOpen);
      }
      if (itemIconLaunch != null) {
        list.add(itemIconLaunch);
      }
    }

    if (isPoly) {
      if (isOpen) {
        final DoorInfo info = getDoor(ViewerConstants.DOOR_OPEN);
        if (info != null && info.getItem() != null) {
          list.add(info.getItem());
        }
      }
      if (isClosed) {
        final DoorInfo info = getDoor(ViewerConstants.DOOR_CLOSED);
        if (info != null && info.getItem() != null) {
          list.add(info.getItem());
        }
      }
    }

    return list.toArray(new AbstractLayerItem[0]);
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    List<AbstractLayerItem> list = new ArrayList<>();

    for (final AbstractLayerItem item : getDoorItems()) {
      if (item != null) {
        list.add(item);
      }
    }

    if (itemIconClose != null) {
      list.add(itemIconClose);
    }
    if (itemIconOpen != null) {
      list.add(itemIconOpen);
    }
    if (itemIconLaunch != null) {
      list.add(itemIconLaunch);
    }

    return list.toArray(new AbstractLayerItem[0]);
  }

  @Override
  public void update(double zoomFactor) {
    for (final DoorInfo info : getDoors()) {
      info.getItem().setItemLocation((int) (info.getLocation().x * zoomFactor + (zoomFactor / 2.0)),
          (int) (info.getLocation().y * zoomFactor + (zoomFactor / 2.0)));
      Polygon poly = createPolygon(info.getCoords(), zoomFactor);
      normalizePolygon(poly);
      info.getItem().setShape(poly);
    }

    if (itemIconClose != null) {
      itemIconClose.setItemLocation((int) (closePoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (closePoint.y * zoomFactor + (zoomFactor / 2.0)));
    }
    if (itemIconOpen != null) {
      itemIconOpen.setItemLocation((int) (openPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (openPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }
    if (itemIconLaunch != null) {
      itemIconLaunch.setItemLocation((int) (launchPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (launchPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }

  private String getAttributes() {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    final boolean isLocked = ((Flag) door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(1);
    if (isLocked) {
      int v = ((IsNumeric) door.getAttribute(Door.ARE_DOOR_LOCK_DIFFICULTY)).getValue();
      if (v > 0) {
        sb.append("Locked (").append(v).append(')');
        int bit = (Profile.getEngine() == Profile.Engine.IWD2) ? 14 : 10;
        boolean usesKey = ((Flag) door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(bit);
        if (usesKey) {
          addResRefDesc(sb, door, Door.ARE_DOOR_KEY, "Key: ");
        }
      }
    }

    addTrappedDesc(sb, door, Door.ARE_DOOR_TRAPPED, Door.ARE_DOOR_TRAP_REMOVAL_DIFFICULTY, Door.ARE_DOOR_SCRIPT);

    final boolean isSecret = ((Flag) door.getAttribute(Door.ARE_DOOR_FLAGS)).isFlagSet(7);
    if (isSecret) {
      if (sb.length() > 1) {
        sb.append(", ");
      }
      sb.append("Secret door");
    }

    if (sb.length() == 1) {
      sb.append("No flags");
    }
    sb.append(']');
    return sb.toString();
  }

  private IconLayerItem createValidatedLayerItem(Point pt, String label, Image[] icons) {
    IconLayerItem retVal = null;

    if (pt.x > 0 && pt.y > 0) {
      retVal = new IconLayerItem(door, label, icons[0], CENTER);
      retVal.setLabelEnabled(Settings.ShowLabelDoorTargets);
      retVal.setName(getCategory());
      retVal.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
      retVal.setVisible(isVisible());
    }

    return retVal;
  }

  private Collection<DoorInfo> getDoors() {
    return doorMap.values();
  }

  private Collection<ShapedLayerItem> getDoorItems() {
    return doorMap.values().stream().map(di -> di.getItem()).filter(item -> item != null).collect(Collectors.toList());
  }

  private DoorInfo getDoor(int id) {
    return doorMap.get(Integer.valueOf(id));
  }

  // ----------------------------- INNER CLASSES -----------------------------

  /** Storage for open/close-based door information. */
  private static class DoorInfo {
    private String message;
    private ShapedLayerItem item;
    private Point location;
    private Point[] coords;

    private DoorInfo() {}

    /** Returns a message or label associated with the door. */
    public String getMessage() {
      return message;
    }

    /** Defines a message or label associated with the door. */
    public DoorInfo setMessage(String msg) {
      this.message = msg;
      return this;
    }

    /** Returns the layer item for the door. */
    public ShapedLayerItem getItem() {
      return item;
    }

    /** Defines the layer item for the door. */
    public DoorInfo setItem(ShapedLayerItem newItem) {
      this.item = newItem;
      return this;
    }

    /** Returns the origin of the door shape. */
    public Point getLocation() {
      return location;
    }

    /** Defines the origin of the door shape. */
    public DoorInfo setLocation(Point newLocation) {
      this.location = newLocation;
      return this;
    }

    /** Returns the vertices of the door shape. */
    public Point[] getCoords() {
      return coords;
    }

    /** Defines the vertices of the door shape. */
    public DoorInfo setCoords(Point[] newCoords) {
      this.coords = newCoords;
      return this;
    }
  }
}
