// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import infinity.gui.layeritem.LayerItemEvent.ItemState;
import infinity.resource.Viewable;

/**
 *
 * @author argent77
 */
public class IconLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Icon DefaultIcon = new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));

  private EnumMap<ItemState, Icon> icons;
  private JLabel label;

  public IconLayerItem()
  {
    this(null, null, null, null, null);
  }

  public IconLayerItem(Point location)
  {
    this(location, null, null, null, null);
  }

  public IconLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null, null, null);
  }

  public IconLayerItem(Point location, Viewable viewable, String msg)
  {
    this(location, viewable, msg, null, null);
  }

  public IconLayerItem(Point location, Viewable viewable, String msg, Icon icon)
  {
    this(location, viewable, msg, icon, null);
  }

  public IconLayerItem(Point location, Viewable viewable, String msg, Icon icon, Point center)
  {
    super(location, viewable, msg);
    setLayout(new BorderLayout());
    icons = new EnumMap<ItemState, Icon>(ItemState.class);
    label = new JLabel();
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    add(label, BorderLayout.CENTER);
    setIcon(ItemState.NORMAL, icon);
    setCenterPosition(center);
    setCurrentIcon(getItemState());
    addLayerItemListener(this);
  }

  /**
   * Returns the icon which is displayed by default.
   * @return The default icon
   */
  public Icon getIcon()
  {
    return getIcon(ItemState.NORMAL);
  }

  /**
   * Sets the icon which is displayed by default. It is used for highlighted and/or selected state
   * as well if no further icons have been defined yet.
   * @param icon The default icon to display.
   */
  public void setIcon(Icon icon)
  {
    setIcon(ItemState.NORMAL, icon);
  }

  /**
   * Returns the icon which is displayed when the mouse cursor is over the component.
   * @return The highlighted icon
   */
  public Icon getHighlightedIcon()
  {
    return getIcon(ItemState.HIGHLIGHTED);
  }

  /**
   * Sets the icon which is displayed when the mouse cursor is over the component.
   * It is used for selected state as well if no further icons have been defined yet.
   * @param icon The highlighted icon to display.
   */
  public void setHighlightedIcon(Icon icon)
  {
    setIcon(ItemState.HIGHLIGHTED, icon);
  }

  public Point getCenterPosition()
  {
    return getLocationOffset();
  }

  public void setCenterPosition(Point center)
  {
    if (center == null)
      center = new Point(0, 0);

    if (!getLocationOffset().equals(center)) {
      Point distance = new Point(getLocationOffset().x - center.x, getLocationOffset().y - center.y);
      setLocationOffset(center);
      // updating component location
      Point loc = super.getLocation();
      setLocation(loc.x + distance.x, loc.y + distance.y);
      validate();
    }
  }

  // Returns whether the mouse cursor is over the relevant part of the component
  protected boolean isMouseOver(Point pt)
  {
    Icon icon = getCurrentIcon();
    if (icon != null) {
      return (new Rectangle((getSize().width - icon.getIconWidth()) / 2,
                            (getSize().height - icon.getIconHeight()) / 2,
                            icon.getIconWidth(),
                            icon.getIconHeight())).contains(pt);
    } else
      return super.isMouseOver(pt);
  }

  private Icon getIcon(ItemState state)
  {
    if (state == null)
      state = ItemState.NORMAL;
    switch (state) {
      case SELECTED:
        if (icons.containsKey(ItemState.SELECTED))
          return icons.get(ItemState.SELECTED);
      case HIGHLIGHTED:
        if (icons.containsKey(ItemState.HIGHLIGHTED))
          return icons.get(ItemState.HIGHLIGHTED);
      case NORMAL:
        if (icons.containsKey(ItemState.NORMAL))
          return icons.get(ItemState.NORMAL);
    }
    return DefaultIcon;
  }

  private void setIcon(ItemState state, Icon icon)
  {
    if (state != null) {
      if (icon != null) {
        icons.put(state, icon);
      } else {
        icons.remove(state);
      }
      updateSize();
    }
  }

  private void updateSize()
  {
    Rectangle r = getBounds();
    r.width = r.height = 0;
    for (final ItemState state: ItemState.values()) {
      Icon icon = getIcon(state);
      r.width = Math.max(r.width, icon.getIconWidth());
      r.height = Math.max(r.height, icon.getIconHeight());
    }
    setPreferredSize(r.getSize());
    setBounds(r);
  }

  private Icon getCurrentIcon()
  {
    return label.getIcon();
  }

  private void setCurrentIcon(ItemState state)
  {
    if (state != null) {
      label.setIcon(getIcon(state));
    } else {
      label.setIcon(null);
    }
  }

//--------------------- Begin Interface Runnable ---------------------

  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      setCurrentIcon(event.getItemState());
    }
  }

//--------------------- End Interface Runnable ---------------------

}
