// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.gui.layeritem.ShapedLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.are.Ambient;
import infinity.resource.are.AreResource;
import infinity.resource.are.viewer.icon.ViewerIcons;

/**
 * Handles specific layer type: ARE/Ambient Sound and Ambient Sound Range
 * Note: Ambient returns two layer items: 0=icon, 1=range (if available)
 * @author argent77
 */
public class LayerObjectAmbient extends LayerObject
{
  private static final Image[] IconGlobal = new Image[]{Icons.getImage(ViewerIcons.class, "itm_AmbientG1.png"),
                                                        Icons.getImage(ViewerIcons.class, "itm_AmbientG2.png")};
  private static final Image[] IconLocal = new Image[]{Icons.getImage(ViewerIcons.class, "itm_AmbientL1.png"),
                                                       Icons.getImage(ViewerIcons.class, "itm_AmbientL2.png")};
  private static final Point Center = new Point(16, 16);
  final Color[] ColorRange = new Color[]{new Color(0xA0000080, true), new Color(0xA0000080, true),
                                         new Color(0x00204080, true), new Color(0x004060C0, true)};

  private final Ambient ambient;
  private final Point location = new Point();

  private IconLayerItem itemIcon;   // for sound icon
  private ShapedLayerItem itemShape;  // for sound range
  private int radiusLocal, volume;
  private Flag scheduleFlags;


  public LayerObjectAmbient(AreResource parent, Ambient ambient)
  {
    super(ViewerConstants.RESOURCE_ARE, "Sound", Ambient.class, parent);
    this.ambient = ambient;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return ambient;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    if (isLocal()) {
      return new AbstractStruct[]{ambient, ambient};
    } else {
      return new AbstractStruct[]{ambient};
    }
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return itemIcon;
  }

  /**
   * Returns the layer item of specified type.
   * @param type The type of the item to return (either <code>ViewerConstants.AMBIENT_ITEM_ICON</code> or
   *              <code>ViewerConstants.AMBIENT_ITEM_RANGE</code>).
   * @return The layer item of specified type.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    if (type == ViewerConstants.AMBIENT_ITEM_RANGE && isLocal()) {
      return itemShape;
    } else if (type == ViewerConstants.AMBIENT_ITEM_ICON) {
      return itemIcon;
    } else {
      return null;
    }
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    if (isLocal()) {
      return new AbstractLayerItem[]{itemIcon, itemShape};
    } else {
      return new AbstractLayerItem[]{itemIcon};
    }
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    int x = (int)(location.x*zoomFactor + (zoomFactor / 2.0));
    int y = (int)(location.y*zoomFactor + (zoomFactor / 2.0));

    if (itemIcon != null) {
      itemIcon.setItemLocation(x, y);
    }

    if (isLocal()) {
      Shape circle = createShape(zoomFactor);
      Rectangle rect = circle.getBounds();
      itemShape.setItemLocation(x, y);
      itemShape.setCenterPosition(new Point(rect.width / 2, rect.height / 2));
      itemShape.setShape(circle);
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
    return new Point[]{location, location};
  }

  /**
   * Returns whether the ambient sound uses a local sound radius.
   */
  public boolean isLocal()
  {
    return (itemShape != null);
  }

  /**
   * Returns the local radius of the ambient sound (if any).
   */
  public int getRadius()
  {
    return radiusLocal;
  }

  /**
   * Returns the volume of the ambient sound.
   */
  public int getVolume()
  {
    return volume;
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


  private void init()
  {
    if (ambient != null) {
      String msg = "";
      Image[] icon = IconGlobal;
      Shape circle = null;
      Color[] color = new Color[ColorRange.length];
      try {
        location.x = ((DecNumber)ambient.getAttribute("Origin: X")).getValue();
        location.y = ((DecNumber)ambient.getAttribute("Origin: Y")).getValue();
        radiusLocal = ((DecNumber)ambient.getAttribute("Radius")).getValue();
        volume = ((DecNumber)ambient.getAttribute("Volume")).getValue();
        if (((Flag)ambient.getAttribute("Flags")).isFlagSet(2)) {
          icon = IconGlobal;
          radiusLocal = 0;
        } else {
          icon = IconLocal;
        }

        scheduleFlags = ((Flag)ambient.getAttribute("Active at"));

        msg = ((TextString)ambient.getAttribute("Name")).toString();
        if (icon == IconLocal) {
          circle = createShape(1.0);
          double minAlpha = 0.0, maxAlpha = 64.0;
          double alphaF = minAlpha + Math.sqrt((double)volume) / 10.0 * (maxAlpha - minAlpha);
          int alpha = (int)alphaF & 0xff;
          color[0] = ColorRange[0];
          color[1] = ColorRange[1];
          color[2] = new Color(ColorRange[2].getRGB() | (alpha << 24), true);
          color[3] = new Color(ColorRange[3].getRGB() | (alpha << 24), true);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Using cached icons
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(icon[0]),
                                                 SharedResourceCache.createKey(icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.Icon, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.Icon, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon);
      } else {
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      // creating sound item
      itemIcon = new IconLayerItem(location, ambient, msg, icon[0], Center);
      itemIcon.setName(getCategory());
      itemIcon.setToolTipText(msg);
      itemIcon.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      itemIcon.setVisible(isVisible());

      // creating sound range item
      if (icon == IconLocal) {
        itemShape = new ShapedLayerItem(location, ambient, msg, circle, new Point(radiusLocal, radiusLocal));
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
      }
    }
  }

  private Shape createShape(double zoomFactor)
  {
    if (ambient != null && itemShape != null && radiusLocal > 0) {
      float diameter = (float)(radiusLocal*zoomFactor + (zoomFactor / 2.0)) * 2.0f;
      return new Ellipse2D.Float(0.0f, 0.0f, diameter, diameter);
    }
    return null;
  }
}
