// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.EnumMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.infinity.resource.Viewable;

/**
 * Represents a game resource structure visually as a shape.
 */
public class ShapedLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Color DEFAULT_COLOR = Color.BLACK;

  private Shape shape;
  private final EnumMap<ItemState, Color> strokeColors = new EnumMap<>(ItemState.class);
  private final EnumMap<ItemState, BasicStroke> strokePen = new EnumMap<>(ItemState.class);
  private final EnumMap<ItemState, Color> fillColors = new EnumMap<>(ItemState.class);
  private final JLabel label;
  private boolean stroked, filled;

  /**
   * Initialize object with an associated Viewable, an additional text message
   * and a shape for the visual representation.
   *
   * @param viewable Associated Viewable object
   * @param tooltip A short text message shown as tooltip or menu item text
   * @param shape The shape to display
   */
  public ShapedLayerItem(Viewable viewable, String tooltip, Shape shape)
  {
    super(viewable, tooltip);
    setLayout(new BorderLayout());
    label = new ShapeLabel(this);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    add(label, BorderLayout.CENTER);
    setShape(shape);
    setCenterPosition(null);
    addLayerItemListener(this);
  }

  /**
   * Sets a new shape.
   * @param shape The new shape
   */
  public void setShape(Shape shape)
  {
    this.shape = (shape != null) ? shape : new Rectangle();
    updateSize();
    updateShape();
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

  /**
   * Returns the polygon's stroke color of the specified visual state.
   * @return Stroke color of the specified visual state.
   */
  public Color getStrokeColor(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    return strokeColors.containsKey(state) ? strokeColors.get(state) : DEFAULT_COLOR;
  }

  /**
   * Sets the stroke color of the polygon for the specified visual state.
   * @param color The stroke color of the polygon for the specified visual state.
   */
  public void setStrokeColor(ItemState state, Color color)
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

  /**
   * Returns the stroke width of the polygon for the specified visual state.
   * @param state The visual state to get the stroke with from.
   * @return The stroke width in pixels.
   */
  public int getStrokeWidth(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    if (strokePen.containsKey(state)) {
      return (int)strokePen.get(state).getLineWidth();
    } else {
      return 1;
    }
  }

  /**
   * Sets the stroke width of the polygon for the specified visual state.
   * @param state The visual state to set the stroke with for.
   * @param width The stroke width in pixels.
   */
  public void setStrokeWidth(ItemState state, int width)
  {
    if (state != null) {
      if (width < 1) {
        width = 1;
      }
      strokePen.put(state, new BasicStroke((float)width));
      updateShape();
    }
  }

  /**
   * Returns the polygon's fill color of the specified visual state.
   * @return Fill color of the specified visual state.
   */
  public Color getFillColor(ItemState state)
  {
    if (state == null) {
      state = ItemState.NORMAL;
    }
    return fillColors.containsKey(state) ? fillColors.get(state) : DEFAULT_COLOR;
  }

  /**
   * Sets the polygon's fill color for the specified visual state.
   * @param color The fill color for the specified visual state.
   */
  public void setFillColor(ItemState state, Color color)
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


  /** Returns whether the mouse cursor is over the relevant part of the component. */
  @Override
  protected boolean isMouseOver(Point pt)
  {
    if (shape != null) {
      return shape.contains(pt);
    } else {
      return getBounds().contains(pt);
    }
  }


  private void updateSize()
  {
    if (shape != null) {
      Rectangle r = getBounds();
      r.setSize(shape.getBounds().getSize());
      setPreferredSize(r.getSize());
      setBounds(r);
    }
  }

  /** Recreates polygons. */
  private void updateShape()
  {
    label.repaint();
  }

  @Override
  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      updateShape();
    }
  }

//----------------------------- INNER CLASSES -----------------------------

  /** Extended JLabel to draw shapes on the fly. */
  private static class ShapeLabel extends JLabel
  {
    private final ShapedLayerItem parent;

    public ShapeLabel(ShapedLayerItem parent)
    {
      super();
      this.parent = parent;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      if (parent != null) {
        ItemState state = parent.getItemState();
        Shape shape = parent.shape;
        if (state != null && shape != null && !shape.getBounds().isEmpty()) {
          Graphics2D g2 = (Graphics2D)g;

          if (parent.filled) {
            g2.setColor(parent.fillColors.get(state));
            g2.fill(shape);
          }

          if (parent.stroked) {
            Object renderHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (parent.strokePen.containsKey(state)) {
              g2.setStroke(parent.strokePen.get(state));
            }
            g2.setColor(parent.strokeColors.get(state));
            g2.draw(shape);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, renderHint);
          }
        }
      }
    }
  }
}
