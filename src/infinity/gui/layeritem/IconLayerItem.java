// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.EnumMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import infinity.gui.layeritem.LayerItemEvent;
import infinity.resource.Viewable;
import infinity.resource.graphics.ColorConvert;

/**
 * Represents a game resource structure visually as a bitmap icon.
 * @author argent77
 */
public class IconLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Icon DefaultIcon = new ImageIcon(ColorConvert.createCompatibleImage(1, 1, true));

  private EnumMap<ItemState, Icon> icons;
  private EnumMap<ItemState, FrameInfo> frames;
  private JLabel label;

  /**
   * Initialize object with default settings.
   */
  public IconLayerItem()
  {
    this(null, null, null, null, null);
  }

  /**
   * Initialize object with the specified map location.
   * @param location Map location
   */
  public IconLayerItem(Point location)
  {
    this(location, null, null, null, null);
  }

  /**
   * Initialize object with a specific map location and an associated viewable object.
   * @param location Map location
   * @param viewable Associated Viewable object
   */
  public IconLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null, null, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable and an additional text message.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   */
  public IconLayerItem(Point location, Viewable viewable, String msg)
  {
    this(location, viewable, msg, null, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable, an additional text message
   * and an icon for the visual representation.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   * @param icon The icon to display
   */
  public IconLayerItem(Point location, Viewable viewable, String msg, Icon icon)
  {
    this(location, viewable, msg, icon, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable, an additional text message,
   * an icon for the visual representation and a locical center position within the icon.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   * @param icon The icon to display
   * @param center Logical center position within the icon
   */
  public IconLayerItem(Point location, Viewable viewable, String msg, Icon icon, Point center)
  {
    super(location, viewable, msg);
    setLayout(new BorderLayout());
    icons = new EnumMap<ItemState, Icon>(ItemState.class);
    frames = new EnumMap<ItemState, FrameInfo>(ItemState.class);
    label = new FrameLabel(this);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    add(label, BorderLayout.CENTER);
    setIcon(ItemState.NORMAL, icon);
    setCenterPosition(center);
    setCurrentIcon(getItemState());
    addLayerItemListener(this);
  }

  /**
   * Returns the icon of the specified visual state.
   * @return The icon of the specified visual state.
   */
  public Icon getIcon(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    switch (state) {
      case HIGHLIGHTED:
        if (icons.containsKey(ItemState.HIGHLIGHTED))
          return icons.get(ItemState.HIGHLIGHTED);
      case NORMAL:
        if (icons.containsKey(ItemState.NORMAL))
          return icons.get(ItemState.NORMAL);
    }
    return DefaultIcon;
  }

  /**
   * Sets the icon for the specified visual state.
   * @param icon The icon to display in the specified visual state.
   */
  public void setIcon(ItemState state, Icon icon)
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

  /**
   * Returns the width of an optional frame around the item.
   * @param state The frame's visual state to get the frame width from.
   * @return The frame's width in pixels.
   */
  public int getFrameWidth(ItemState state)
  {
    return getFrameInfo(state).getLineWidth();
  }

  /**
   * Sets the width of an optional frame around the item.
   * @param state The frame's visual state to set the frame width for.
   * @param size The frame width in pixels.
   */
  public void setFrameWidth(ItemState state, int size)
  {
    if (state != null) {
      if (frames.containsKey(state)) {
        frames.get(state).setLineWidth(size);
      } else {
        frames.put(state, new FrameInfo(size));
      }
    }
  }

  /**
   * Returns the color of an optional frame around the item (Default color is white).
   * @param state The frame's visual state to get the frame color from.
   * @return The frame's color.
   */
  public Color getFrameColor(ItemState state)
  {
    return getFrameInfo(state).getColor();
  }

  /**
   * Sets the color of an optional frame around the item.
   * @param state The frame's visual state to set the frame color for.
   * @param color The frame color to set.
   */
  public void setFrameColor(ItemState state, Color color)
  {
    if (state != null) {
      if (frames.containsKey(state)) {
        frames.get(state).setColor(color);
      } else {
        FrameInfo info = new FrameInfo(FrameInfo.getDefaultLineWidth());
        info.setColor(color);
        frames.put(state, info);
      }
    }
  }

  /**
   * Returns whether a frame around the item will be displayed for the specified visual state.
   * @param state The frame's visual state to show the frame.
   * @return Whether the frame is shown for the specified visual state.
   */
  public boolean getFrameEnabled(ItemState state)
  {
    return getFrameInfo(state).getEnabled();
  }

  /**
   * Specify whether a frame around the item will be displayed for the specified visual state.
   * @param state The frame's visual state to set the display state.
   * @param show <code>true</code> if the frame should be displayed for the specified visual state
   *             or <code>false</code> otherwise.
   */
  public void setFrameEnabled(ItemState state, boolean show)
  {
    if (state != null) {
      if (frames.containsKey(state)) {
        frames.get(state).setEnabled(show);
      } else {
        FrameInfo info = new FrameInfo(FrameInfo.getDefaultLineWidth());
        info.setEnabled(show);
        frames.put(state, info);
      }
    }
  }

  /**
   * Sets the logical center of the icon.
   * @return The logical center of the icon
   */
  public Point getCenterPosition()
  {
    return getLocationOffset();
  }

  /**
   * Sets the logical center of the icon.
   * @param center The center position within the icon
   */
  public void setCenterPosition(Point center)
  {
    if (center == null) {
      center = new Point(0, 0);
    }

    if (!getLocationOffset().equals(center)) {
      Point distance = new Point(getLocationOffset().x - center.x, getLocationOffset().y - center.y);
      setLocationOffset(center);
      // updating component location
      Point loc = super.getLocation();
      setLocation(loc.x + distance.x, loc.y + distance.y);
      validate();
    }
  }

//--------------------- Begin Interface LayerItemListener ---------------------

  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      setCurrentIcon(getItemState());
    }
  }

//--------------------- End Interface LayerItemListener ---------------------

  // Returns whether the mouse cursor is over the relevant part of the component
  @Override
  protected boolean isMouseOver(Point pt)
  {
    ImageIcon icon = (ImageIcon)getCurrentIcon();
    if (icon != null) {
      Rectangle region = new Rectangle((getSize().width - icon.getIconWidth()) / 2,
                                       (getSize().height - icon.getIconHeight()) / 2,
                                        icon.getIconWidth(),
                                        icon.getIconHeight());
      if (region.contains(pt)) {
        BufferedImage image = ColorConvert.toBufferedImage(icon.getImage(), true);
        if (image != null) {
          int color = image.getRGB(pt.x - region.x, pt.y - region.y);
          // (near) transparent pixels (alpha <= 16) are disregarded
          return ((color >>> 24) > 0x10);
        } else {
          return true;
        }
      } else {
        return false;
      }
    } else {
      return super.isMouseOver(pt);
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

  private FrameInfo getFrameInfo(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    switch (state) {
      case HIGHLIGHTED:
        if (frames.containsKey(ItemState.HIGHLIGHTED))
          return frames.get(ItemState.HIGHLIGHTED);
      case NORMAL:
        if (frames.containsKey(ItemState.NORMAL))
          return frames.get(ItemState.NORMAL);
    }
    return new FrameInfo(FrameInfo.getDefaultLineWidth());
  }


//----------------------------- INNER CLASSES -----------------------------

  // Extended JLabel to add the feature to show a frame around the component
  private static class FrameLabel extends JLabel
  {
    private final IconLayerItem parent;

    public FrameLabel(IconLayerItem parent)
    {
      super();
      this.parent = parent;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      if (parent != null) {
        FrameInfo info = parent.getFrameInfo(parent.getItemState());
        if (info.getEnabled()) {
          int ofs  = info.getLineWidth() / 2;
          Shape shp = new Rectangle(ofs, ofs, getWidth() - 2*ofs - 1, getHeight() - 2*ofs - 1);
          Graphics2D g2 = (Graphics2D)g;
          g2.setStroke(info.getStroke());
          g2.setColor(info.getColor());
          g2.draw(shp);
        }
      }
    }
  }

  // Stores information required to draw a customized frame around the component
  private static class FrameInfo
  {
    private boolean enabled;
    private Color color;
    private BasicStroke stroke;

    private static boolean getDefaultEnabled() { return false; }
    private static Color getDefaultColor() { return Color.WHITE; }
    private static int getDefaultLineWidth() { return 1; }

    private FrameInfo(int width)
    {
      setEnabled(getDefaultEnabled());
      setColor(getDefaultColor());
      setLineWidth(width);
    }

    private boolean getEnabled()
    {
      return enabled;
    }

    private void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
    }

    private Color getColor()
    {
      return (color != null) ? color: getDefaultColor();
    }

    private void setColor(Color c)
    {
      this.color = (c != null) ? c : getDefaultColor();
    }

    private int getLineWidth()
    {
      return (stroke != null) ? (int)stroke.getLineWidth() : getDefaultLineWidth();
    }

    private void setLineWidth(int width)
    {
      if (width < 1) {
        width = getDefaultLineWidth();
      }
      if (stroke == null || (int)stroke.getLineWidth() != width) {
          stroke = new BasicStroke((float)width);
      }
    }

    private BasicStroke getStroke()
    {
      if (stroke == null) {
        stroke = new BasicStroke((float)getDefaultLineWidth());
      }
      return stroke;
    }
  }

}
