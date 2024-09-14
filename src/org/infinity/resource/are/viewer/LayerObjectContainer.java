// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Container;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.vertex.Vertex;
import org.infinity.util.Logger;

/**
 * Handles specific layer type: ARE/Container
 */
public class LayerObjectContainer extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_CONTAINER_TARGET_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_CONTAINER_TARGET_2.getIcon().getImage() };

  private static final Image[] ICONS_LAUNCH = { ViewerIcons.ICON_ITM_CONTAINER_TARGET_L_1.getIcon().getImage(),
                                                ViewerIcons.ICON_ITM_CONTAINER_TARGET_L_2.getIcon().getImage() };

  private static final Point CENTER = new Point(13, 29);

  private static final Color[] COLOR = { new Color(0xFF004040, true), new Color(0xFF004040, true),
                                         new Color(0xC0008080, true), new Color(0xC000C0C0, true) };

  private final Container container;
  private final Point location = new Point();

  private final ShapedLayerItem item;
  private Point[] shapeCoords;

  private final IconLayerItem itemIconAccess;
  private final Point accessPoint = new Point();

  private final IconLayerItem itemIconLaunch;
  private final Point launchPoint = new Point();

  public LayerObjectContainer(AreResource parent, Container container) {
    super("Container", Container.class, parent);
    this.container = container;
    String label = null;
    String msg = null;
    try {
      int type = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_TYPE)).getValue();
      if (type < 0) {
        type = 0;
      } else if (type >= Container.TYPE_ARRAY.length) {
        type = Container.TYPE_ARRAY.length - 1;
      }
      msg = String.format("%s (%s) %s", container.getAttribute(Container.ARE_CONTAINER_NAME).toString(),
          Container.TYPE_ARRAY[type], getAttributes());
      int vNum = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_NUM_VERTICES)).getValue();
      int vOfs = ((IsNumeric) parent.getAttribute(AreResource.ARE_OFFSET_VERTICES)).getValue();
      shapeCoords = loadVertices(container, vOfs, 0, vNum, Vertex.class);

      label = container.getAttribute(Container.ARE_CONTAINER_NAME).toString();
      accessPoint.x = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_LOCATION_X)).getValue();
      accessPoint.y = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_LOCATION_Y)).getValue();
      launchPoint.x = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_LAUNCH_POINT_X)).getValue();
      launchPoint.y = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_LAUNCH_POINT_Y)).getValue();
    } catch (Exception e) {
      Logger.error(e);
    }
    final Polygon poly = createPolygon(shapeCoords, 1.0);
    final Rectangle bounds = normalizePolygon(poly);

    location.x = bounds.x;
    location.y = bounds.y;
    item = new ShapedLayerItem(container, msg, poly);
    item.setName(getCategory());
    item.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, COLOR[0]);
    item.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[1]);
    item.setFillColor(AbstractLayerItem.ItemState.NORMAL, COLOR[2]);
    item.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, COLOR[3]);
    item.setStroked(true);
    item.setFilled(true);
    item.setVisible(isVisible());

    itemIconAccess = createValidatedLayerItem(accessPoint, label, getIcons(ICONS));
    itemIconLaunch = createValidatedLayerItem(launchPoint, label, getIcons(ICONS_LAUNCH));
  }

  @Override
  public Viewable getViewable() {
    return container;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    switch (type) {
      case ViewerConstants.LAYER_ITEM_POLY:
        if (item != null) {
          return new AbstractLayerItem[] { item };
        }
        break;
      case ViewerConstants.LAYER_ITEM_ICON:
        if (itemIconAccess != null && itemIconLaunch != null) {
          return new AbstractLayerItem[] { itemIconAccess, itemIconLaunch };
        } else if (itemIconAccess != null) {
          return new AbstractLayerItem[] { itemIconAccess };
        } else if (itemIconLaunch != null) {
          return new AbstractLayerItem[] { itemIconLaunch };
        }
        break;
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    final AbstractLayerItem[] retVal = new AbstractLayerItem[] { item, itemIconAccess, itemIconLaunch };
    int size = retVal.length;

    if (itemIconAccess == null) {
      retVal[1] = retVal[size - 1];
      size--;
    }

    if (itemIconLaunch == null) {
      size--;
    }

    if (size < 3) {
      return Arrays.copyOf(retVal, size);
    } else {
      return retVal;
    }
  }

  @Override
  public void update(double zoomFactor) {
    if (item != null) {
      item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (location.y * zoomFactor + (zoomFactor / 2.0)));

      Polygon poly = createPolygon(shapeCoords, zoomFactor);
      normalizePolygon(poly);
      item.setShape(poly);
    }

    if (itemIconAccess != null) {
      itemIconAccess.setItemLocation((int) (accessPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (accessPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }

    if (itemIconLaunch != null) {
      itemIconLaunch.setItemLocation((int) (launchPoint.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (launchPoint.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }

  private String getAttributes() {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    final boolean isLocked = ((Flag) container.getAttribute(Container.ARE_CONTAINER_FLAGS)).isFlagSet(0);
    if (isLocked) {
      int v = ((IsNumeric) container.getAttribute(Container.ARE_CONTAINER_LOCK_DIFFICULTY)).getValue();
      if (v > 0) {
        sb.append("Locked (").append(v).append(')');
        addResRefDesc(sb, container, Container.ARE_CONTAINER_KEY, "Key: ");
      }
    }

    addTrappedDesc(sb, container, Container.ARE_CONTAINER_TRAPPED, Container.ARE_CONTAINER_TRAP_REMOVAL_DIFFICULTY,
        Container.ARE_CONTAINER_SCRIPT_TRAP);

    if (sb.length() == 1) {
      sb.append("No Flags");
    }
    sb.append(']');
    return sb.toString();
  }

  private IconLayerItem createValidatedLayerItem(Point pt, String label, Image[] icons) {
    IconLayerItem retVal = null;

    if (pt.x > 0 && pt.y > 0) {
      retVal = new IconLayerItem(container, label, LayerContainer.LAYER_ICONS_TARGET, icons[0], CENTER);
      retVal.setLabelEnabled(Settings.ShowLabelContainerTargets);
      retVal.setName(getCategory());
      retVal.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
      retVal.setVisible(isVisible());
    }

    return retVal;
  }
}
