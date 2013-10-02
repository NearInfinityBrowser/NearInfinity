// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import infinity.gui.layeritem.LayerItemEvent.ItemState;
import infinity.resource.Viewable;

import java.awt.BorderLayout;
import java.awt.Color;
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

/**
 * Represents a game resource structure visually as a shape.
 * @author argent77
 */ public class ShapedLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Color DefaultColor = Color.BLACK;

  private Shape shape;
  private EnumMap<ItemState, Icon> icons;
  private EnumMap<ItemState, Color> strokeColors;
  private EnumMap<ItemState, Color> fillColors;
  private JLabel label;
  private boolean stroked, filled;

  /**
   * Initialize object with default settings.
   */
  public ShapedLayerItem()
  {
    this(null, null, null, null, null);
  }

  /**
   * Initialize object with the specified map location.
   * @param location Map location
   */
  public ShapedLayerItem(Point location)
  {
    this(location, null, null, null, null);
  }

  /**
   * Initialize object with a specific map location and an associated viewable object.
   * @param location Map location
   * @param viewable Associated Viewable object
   */
  public ShapedLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null, null, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable and an additional text message.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   */
  public ShapedLayerItem(Point location, Viewable viewable, String msg)
  {
    this(location, viewable, msg, null, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable, an additional text message
   * and a shape for the visual representation.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   * @param shape The shape to display
   */
  public ShapedLayerItem(Point location, Viewable viewable, String msg, Shape shape)
  {
    this(location, viewable, msg, shape, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable, an additional text message,
   * a shape for the visual representation and a locical center position within the shape.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   * @param shape The shape to display
   * @param center Logical center position within the shape
   */
  public ShapedLayerItem(Point location, Viewable viewable, String msg, Shape shape, Point center)
  {
    super(location, viewable, msg);
    icons = new EnumMap<ItemState, Icon>(ItemState.class);
    strokeColors = new EnumMap<ItemState, Color>(ItemState.class);
    fillColors = new EnumMap<ItemState, Color>(ItemState.class);
    setLayout(new BorderLayout());
    label = new JLabel();
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    add(label, BorderLayout.CENTER);
    setShape(shape);
    setCenterPosition(center);
    addLayerItemListener(this);
  }

  /**
   * Returns the associated shape object.
   * @return The associated shape object.
   */
  public Shape getShape()
  {
    return shape;
  }

  /**
   * Sets a new shape.
   * @param shape The new shape
   */
  public void setShape(Shape shape)
  {
    this.shape = (shape != null) ? shape : new Rectangle();
    updateShape();
  }

  /**
   * Sets the logical center of the icon.
   * @param center The center position within the icon
   */
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

  /**
   * Returns the stroke color.
   * @return Stroke color
   */
  public Color getStrokeColor()
  {
    return getStrokeColor(ItemState.NORMAL);
  }

  /**
   * Returns the stroke color in highlighted state.
   * @return Stroke color in highlighted state
   */
  public Color getHighlightedStrokeColor()
  {
    return getStrokeColor(ItemState.HIGHLIGHTED);
  }

  /**
   * Sets the stroke color of the polygon.
   * @param c The stroke color of the polygon.
   */
  public void setStrokeColor(Color c)
  {
    setStrokeColor(ItemState.NORMAL, c);
  }

  /**
   * Sets the stroke color of the polygon in the highlighted state.
   * @param c The stroke color of the polygon in the highlighted state.
   */
  public void setHighlightedStrokeColor(Color c)
  {
    setStrokeColor(ItemState.HIGHLIGHTED, c);
  }

  /**
   * Returns the fill color of the polygon.
   * @return Fill color
   */
  public Color getFillColor()
  {
    return getFillColor(ItemState.NORMAL);
  }

  /**
   * Returns the fill color of the polygon in the highlighted state.
   * @return The fill color of the polygon in the highlighted state.
   */
  public Color getHighlightedFillColor()
  {
    return getFillColor(ItemState.HIGHLIGHTED);
  }

  /**
   * Sets the fill color of the polygon.
   * @param c The fill color of the polygon.
   */
  public void setFillColor(Color c)
  {
    setFillColor(ItemState.NORMAL, c);
  }

  /**
   * Sets the fill color of the polygon in the highlighted state.
   * @param c The fill color of the polygon in the highlighted state.
   */
  public void setHighlightedFillColor(Color c)
  {
    setFillColor(ItemState.HIGHLIGHTED, c);
  }

  /**
   * Returns whether the polygon should have a stroked outline.
   * @return true if the polygon is drawn with a stroked outline, false otherwise.
   */
  public boolean getStroked()
  {
    return stroked;
  }

  /**
   * Specify whether the polygon should be drawn with a stroked outline.
   * @param b If true, the polygon will be drawn with a stroked outline
   */
  public void setStroked(boolean b)
  {
    if (b != stroked) {
      stroked = b;
      updateShape();
    }
  }

  /**
   * Returns whether the polygon should be filled with a specific color.
   * @return true if the polygon is drawn filled, false otherwise.
   */
  public boolean getFilled()
  {
    return filled;
  }

  /**
   * Specify whether the polygon should be filled with a specific color.
   * @param b If true, the polygon will be filled with a specific color.
   */
  public void setFilled(boolean b)
  {
    if (b != filled) {
      filled = b;
      updateShape();
    }
  }


  // Returns whether the mouse cursor is over the relevant part of the component
  protected boolean isMouseOver(Point pt)
  {
    if (shape != null) {
      return shape.contains(pt);
    } else
      return getBounds().contains(pt);
  }


  private Color getStrokeColor(ItemState state)
  {
    if (state == null)
      state = ItemState.NORMAL;
    switch (state) {
      case SELECTED:
        if (strokeColors.containsKey(ItemState.SELECTED))
          return strokeColors.get(ItemState.SELECTED);
      case HIGHLIGHTED:
        if (strokeColors.containsKey(ItemState.HIGHLIGHTED))
          return strokeColors.get(ItemState.HIGHLIGHTED);
      case NORMAL:
        if (strokeColors.containsKey(ItemState.NORMAL))
          return strokeColors.get(ItemState.NORMAL);
    }
    return DefaultColor;
  }

  private void setStrokeColor(ItemState state, Color color)
  {
    if (state != null) {
      if (color != null) {
        strokeColors.put(state, color);
      } else {
        strokeColors.remove(state);
      }
      updateShape();
    }
  }

  private Color getFillColor(ItemState state)
  {
    if (state == null)
      state = ItemState.NORMAL;
    switch (state) {
      case SELECTED:
        if (fillColors.containsKey(ItemState.SELECTED))
          return fillColors.get(ItemState.SELECTED);
      case HIGHLIGHTED:
        if (fillColors.containsKey(ItemState.HIGHLIGHTED))
          return fillColors.get(ItemState.HIGHLIGHTED);
      case NORMAL:
        if (fillColors.containsKey(ItemState.NORMAL))
          return fillColors.get(ItemState.NORMAL);
    }
    return DefaultColor;
  }

  private void setFillColor(ItemState state, Color color)
  {
    if (state != null) {
      if (color != null) {
        fillColors.put(state, color);
      } else {
        fillColors.remove(state);
      }
      updateShape();
    }
  }

  // generates a graphical representation of the polygon
  private ImageIcon createIcon(ItemState state)
  {
    if (shape != null && !shape.getBounds().isEmpty() && state != null) {
      Rectangle rect = shape.getBounds();
      if (rect.isEmpty())
        rect.width = rect.height = 1;
      BufferedImage img = new BufferedImage(rect.x + rect.width, rect.y + rect.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = img.createGraphics();
      if (graphics != null) {
        if (filled) {
          graphics.setColor(fillColors.get(state));
          graphics.fill(shape);
        }
        if (stroked) {
          graphics.setColor(strokeColors.get(state));
          graphics.draw(shape);
        }
      }
      return new ImageIcon(img);
    } else
      return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
  }

  private void updateSize()
  {
    Rectangle r = getBounds();
    r.width = r.height = 0;
    for (final Icon icon: icons.values()) {
      r.width = Math.max(r.width, icon.getIconWidth());
      r.height = Math.max(r.height, icon.getIconHeight());
    }
    setPreferredSize(r.getSize());
    setBounds(r);
  }

  // Recreates polygons
  private void updateShape()
  {
    updateSize();
    icons.put(ItemState.NORMAL, createIcon(ItemState.NORMAL));
    icons.put(ItemState.HIGHLIGHTED, createIcon(ItemState.HIGHLIGHTED));
    icons.put(ItemState.SELECTED, createIcon(ItemState.SELECTED));
    setCurrentIcon(getItemState());
  }

  private Icon getCurrentIcon()
  {
    return label.getIcon();
  }

  private void setCurrentIcon(ItemState state)
  {
    if (state != null && icons.containsKey(state))
      label.setIcon(icons.get(state));
  }

//--------------------- Begin Interface LayerItemListener ---------------------

  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      setCurrentIcon(event.getItemState());
    }
  }

//--------------------- End Interface LayerItemListener ---------------------

}
