// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.EnumMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.infinity.gui.RenderCanvas;
import org.infinity.resource.Viewable;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Represents a game resource structure visually as a bitmap icon.
 */
public class IconLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Image DEFAULT_IMAGE = ColorConvert.createCompatibleImage(1, 1, true);
  private static final Color COLOR_BG_NORMAL = new Color(0x80ffffff, true);
  private static final Color COLOR_BG_HIGHLIGHTED = new Color(0xc0ffffff, true);

  private final EnumMap<ItemState, Image> images = new EnumMap<>(ItemState.class);
  private final EnumMap<ItemState, FrameInfo> frames = new EnumMap<>(ItemState.class);
  private RenderCanvas rcCanvas;
  private JLabel label;

  /**
   * Initialize object with an associated Viewable and an additional text message.
   *
   * @param viewable Associated Viewable object
   * @param message An arbitrary text message
   */
  public IconLayerItem(Viewable viewable, String message)
  {
    this(viewable, message, null, null);
  }

  /**
   * Initialize object with an associated Viewable, an additional text message,
   * an image for the visual representation and a locical center position within the icon.
   *
   * @param viewable Associated Viewable object
   * @param tooltip A short text message shown as tooltip or menu item text
   * @param image The image to display
   * @param center Logical center position within the icon
   */
  public IconLayerItem(Viewable viewable, String tooltip,
                       Image image, Point center)
  {
    super(viewable, tooltip);
    setLayout(new BorderLayout());
    // preparing icon
    rcCanvas = new FrameCanvas(this);
//    rcCanvas.setBorder(BorderFactory.createLineBorder(Color.RED));  // DEBUG
    rcCanvas.setHorizontalAlignment(SwingConstants.CENTER);
    rcCanvas.setVerticalAlignment(SwingConstants.CENTER);
    add(rcCanvas, BorderLayout.CENTER);
    // preparing icon label
    label = new JLabel(tooltip);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    label.setIconTextGap(2);
    label.setBackground(COLOR_BG_NORMAL);
    label.setForeground(Color.BLACK);
    label.setOpaque(true);
    label.setVisible(false);  // labels are disabled by default
    add(label, BorderLayout.NORTH);
    setImage(ItemState.NORMAL, image);

    if (center != null) {
      // adjusting center position
      int gap = label.getIconTextGap();
      int shiftX = gap*2 + label.getPreferredSize().width - image.getWidth(null);
      shiftX = Math.max(0, shiftX / 2);
      int shiftY = gap + label.getPreferredSize().height;
      center = new Point(center.x + shiftX, center.y + shiftY);
    }
    setCenterPosition(center);

    setCurrentImage(getItemState());
    addLayerItemListener(this);
  }

  /**
   * Returns the image of the specified visual state.
   * @return The image of the specified visual state.
   */
  public Image getImage(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    return images.containsKey(state) ? images.get(state) : DEFAULT_IMAGE;
  }

  /**
   * Sets the image for the specified visual state.
   * @param image The image to display in the specified visual state.
   */
  public void setImage(ItemState state, Image image)
  {
    if (state != null) {
      if (image != null) {
        images.put(state, image);
      } else {
        images.remove(state);
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
   * @param show {@code true} if the frame should be displayed for the specified visual state
   *             or {@code false} otherwise.
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
   * @param center The center position within the icon.
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

  public void setLabelEnabled(boolean set)
  {
    label.setVisible(set);
    validate();
  }

  @Override
  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      setCurrentImage(getItemState());
    }
  }

  /** Returns whether the mouse cursor is over the relevant part of the component. */
  @Override
  protected boolean isMouseOver(Point pt)
  {
    BufferedImage image = ColorConvert.toBufferedImage(getCurrentImage(), true);
    if (image != null) {
      Rectangle region = new Rectangle((getSize().width - image.getWidth() + rcCanvas.getX()) / 2,
                                       (getSize().height - image.getHeight() + rcCanvas.getY()) / 2,
                                        image.getWidth(),
                                        image.getHeight());
      if (region.contains(pt)) {
        int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        int color = data[(pt.y - region.y)*image.getWidth() + (pt.x - region.x)];
        // (near) transparent pixels (alpha <= 16) are disregarded
        return ((color >>> 24) > 0x10);
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
      Image image = getImage(state);
      r.width = Math.max(r.width, image.getWidth(null));
      r.height = Math.max(r.height, image.getHeight(null));
    }
    int gap = label.getIconTextGap() * 2;
    r.width = Math.max(r.width, label.getPreferredSize().width + gap);
    r.height += label.getPreferredSize().height + gap;
    setPreferredSize(r.getSize());
    setBounds(r);
  }

  private Image getCurrentImage()
  {
    return rcCanvas.getImage();
  }

  private void setCurrentImage(ItemState state)
  {
    if (state != null) {
      switch (state) {
        case NORMAL: label.setBackground(COLOR_BG_NORMAL); break;
        case HIGHLIGHTED: label.setBackground(COLOR_BG_HIGHLIGHTED); break;
      }
      rcCanvas.setImage(getImage(state));
    } else {
      label.setBackground(new Color(0, true));
      rcCanvas.setImage(null);
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

  /** Extended JLabel to add the feature to show a frame around the component. */
  private static class FrameCanvas extends RenderCanvas
  {
    private final IconLayerItem parent;

    public FrameCanvas(IconLayerItem parent)
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

  /** Stores information required to draw a customized frame around the component. */
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
