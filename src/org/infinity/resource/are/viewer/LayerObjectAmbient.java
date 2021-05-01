// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.gui.layeritem.ShapedLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.Ambient;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Ambient Sound and Ambient Sound Range
 * Note: Ambient returns two layer items: 0=icon, 1=range (if available)
 */
public class LayerObjectAmbient extends LayerObject
{
  private static final Image[] ICONS_GLOBAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_G_1),
                                               Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_G_2)};
  private static final Image[] ICONS_LOCAL = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_L_1),
                                              Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AMBIENT_L_2)};
  private static final Point CENTER = new Point(16, 16);
  private static final Color[] COLOR_RANGE = {new Color(0xA0000080, true), new Color(0xA0000080, true),
                                              new Color(0x00204080, true), new Color(0x004060C0, true)};

  private final Ambient ambient;
  private final Point location = new Point();

  /** Center of sound emitter. */
  private final IconLayerItem itemIcon;
  /** Area of the local sound. */
  private ShapedLayerItem itemShape;
  private int radiusLocal;
  private int volume;
  private Flag scheduleFlags;


  public LayerObjectAmbient(AreResource parent, Ambient ambient)
  {
    super("Sound", Ambient.class, parent);
    this.ambient = ambient;
    String msg = null;
    boolean isLocal = false;
    final Color[] color = new Color[COLOR_RANGE.length];
    try {
      location.x = ((IsNumeric)ambient.getAttribute(Ambient.ARE_AMBIENT_ORIGIN_X)).getValue();
      location.y = ((IsNumeric)ambient.getAttribute(Ambient.ARE_AMBIENT_ORIGIN_Y)).getValue();
      radiusLocal = ((IsNumeric)ambient.getAttribute(Ambient.ARE_AMBIENT_RADIUS)).getValue();
      volume = ((IsNumeric)ambient.getAttribute(Ambient.ARE_AMBIENT_VOLUME)).getValue();
      // Bit 2 - Ignore radius
      isLocal = !((Flag)ambient.getAttribute(Ambient.ARE_AMBIENT_FLAGS)).isFlagSet(2);

      scheduleFlags = ((Flag)ambient.getAttribute(Ambient.ARE_AMBIENT_ACTIVE_AT));

      msg = ambient.getAttribute(Ambient.ARE_AMBIENT_NAME).toString();
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Using cached icons
    final Image[] icons = getIcons(isLocal ? ICONS_LOCAL : ICONS_GLOBAL);

    // creating sound item
    itemIcon = new IconLayerItem(ambient, msg, icons[0], CENTER);
    itemIcon.setLabelEnabled(Settings.ShowLabelSounds);
    itemIcon.setName(getCategory());
    itemIcon.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    itemIcon.setVisible(isVisible());

    // creating sound range item
    if (isLocal) {
      final double minAlpha = 0.0;
      final double maxAlpha = 64.0;
      final double alphaF = minAlpha + Math.sqrt(volume) / 10.0 * (maxAlpha - minAlpha);
      final int alpha = (int)alphaF & 0xff;
      color[0] = COLOR_RANGE[0];
      color[1] = COLOR_RANGE[1];
      color[2] = new Color(COLOR_RANGE[2].getRGB() | (alpha << 24), true);
      color[3] = new Color(COLOR_RANGE[3].getRGB() | (alpha << 24), true);

      itemShape = new ShapedLayerItem(ambient, msg, createShape(1.0));
      itemShape.setName(getCategory());
      itemShape.setStrokeColor(AbstractLayerItem.ItemState.NORMAL, color[0]);
      itemShape.setStrokeColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[1]);
      itemShape.setFillColor(AbstractLayerItem.ItemState.NORMAL, color[2]);
      itemShape.setFillColor(AbstractLayerItem.ItemState.HIGHLIGHTED, color[3]);
      itemShape.setStrokeWidth(AbstractLayerItem.ItemState.NORMAL, 2);
      itemShape.setStrokeWidth(AbstractLayerItem.ItemState.HIGHLIGHTED, 2);
      itemShape.setStroked(true);
      itemShape.setFilled(true);
      itemShape.setVisible(isVisible());
    } else {
      radiusLocal = 0;
    }
  }

  @Override
  public Viewable getViewable()
  {
    return ambient;
  }

  /**
   * Returns the layer item of specified type.
   * @param type The type of the item to return (either {@code ViewerConstants.AMBIENT_ITEM_ICON} or
   *              {@code ViewerConstants.AMBIENT_ITEM_RANGE}).
   * @return The layer item of specified type.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (type == ViewerConstants.AMBIENT_ITEM_RANGE) {
      return itemShape;
    }
    if (type == ViewerConstants.AMBIENT_ITEM_ICON) {
      return itemIcon;
    }
    return null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    if (itemShape != null) {
      return new AbstractLayerItem[]{itemIcon, itemShape};
    } else {
      return new AbstractLayerItem[]{itemIcon};
    }
  }

  @Override
  public void update(double zoomFactor)
  {
    int x = (int)(location.x*zoomFactor + (zoomFactor / 2.0));
    int y = (int)(location.y*zoomFactor + (zoomFactor / 2.0));

    itemIcon.setItemLocation(x, y);

    if (itemShape != null) {
      Shape circle = createShape(zoomFactor);
      Rectangle rect = circle.getBounds();
      itemShape.setItemLocation(x, y);
      itemShape.setCenterPosition(new Point(rect.width / 2, rect.height / 2));
      itemShape.setShape(circle);
    }
  }

  @Override
  public boolean isScheduled(int schedule)
  {
    if (schedule >= ViewerConstants.TIME_0 && schedule <= ViewerConstants.TIME_23) {
      return (scheduleFlags.isFlagSet(schedule));
    } else {
      return false;
    }
  }

  /**
   * Returns whether the ambient sound uses a local sound radius.
   */
  public boolean isLocal()
  {
    return (itemShape != null);
  }

  private Shape createShape(double zoomFactor)
  {
    if (radiusLocal > 0) {
      float diameter = (float)(radiusLocal*zoomFactor + (zoomFactor / 2.0)) * 2.0f;
      return new Ellipse2D.Float(0.0f, 0.0f, diameter, diameter);
    }
    return null;
  }
}
