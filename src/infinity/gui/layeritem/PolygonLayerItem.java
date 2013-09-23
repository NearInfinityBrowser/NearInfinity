// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
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
public class PolygonLayerItem extends AbstractLayerItem implements LayerItemListener
{
  private static final Color DefaultColor = Color.BLACK;

  private Polygon polygon;
  private EnumMap<ItemState, Icon> icons;
  private EnumMap<ItemState, Color> strokeColors;
  private EnumMap<ItemState, Color> fillColors;
  private JLabel label;
  private boolean stroked, filled;

  public PolygonLayerItem()
  {
    this(null, null, null, null);
  }

  public PolygonLayerItem(Point location)
  {
    this(location, null, null);
  }

  public PolygonLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null, null);
  }

  public PolygonLayerItem(Point location, Viewable viewable, String msg)
  {
    this(location, viewable, msg, null);
  }

  public PolygonLayerItem(Point location, Viewable viewable, String msg, Polygon poly)
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
    setPolygon(poly);
    addLayerItemListener(this);
  }

  public Polygon getPolygon()
  {
    return polygon;
  }

  public void setPolygon(Polygon poly)
  {
    polygon = (poly != null) ? poly : new Polygon();
    updatePolygon();
  }

  public Color getStrokeColor()
  {
    return getStrokeColor(ItemState.NORMAL);
  }

  public Color getHighlightedStrokeColor()
  {
    return getStrokeColor(ItemState.HIGHLIGHTED);
  }

  public Color getSelectedStrokeColor()
  {
    return getStrokeColor(ItemState.SELECTED);
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
   * Sets the stroke color of the polygon in the selected state.
   * @param c The stroke color of the polygon in the selected state.
   */
  public void setSelectedStrokeColor(Color c)
  {
    setStrokeColor(ItemState.SELECTED, c);
  }

  public Color getFillColor()
  {
    return getFillColor(ItemState.NORMAL);
  }

  public Color getHighlightedFillColor()
  {
    return getFillColor(ItemState.HIGHLIGHTED);
  }

  public Color getSelectedFillColor()
  {
    return getFillColor(ItemState.SELECTED);
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
   * Sets the fill color of the polygon in the selected state.
   * @param c The fill color of the polygon in the selected state.
   */
  public void setSelectedFillColor(Color c)
  {
    setFillColor(ItemState.SELECTED, c);
  }

  public boolean getStroked()
  {
    return stroked;
  }

  public void setStroked(boolean b)
  {
    if (b != stroked) {
      stroked = b;
      updatePolygon();
    }
  }

  public boolean getFilled()
  {
    return filled;
  }

  public void setFilled(boolean b)
  {
    if (b != filled) {
      filled = b;
      updatePolygon();
    }
  }


  // Returns whether the mouse cursor is over the relevant part of the component
  protected boolean isMouseOver(Point pt)
  {
    if (polygon != null) {
      return polygon.contains(pt);
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
      updatePolygon();
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
      updatePolygon();
    }
  }

  // generates a graphical representation of the polygon
  private ImageIcon createIcon(ItemState state)
  {
    if (polygon != null && polygon.npoints > 2 && state != null) {
      Rectangle rect = polygon.getBounds();
      if (rect.isEmpty())
        rect.width = rect.height = 1;
      BufferedImage img = new BufferedImage(rect.x + rect.width, rect.y + rect.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = img.createGraphics();
      if (graphics != null) {
        graphics.setColor(new Color(-1, true));
        if (filled) {
          graphics.setColor(fillColors.get(state));
          graphics.fillPolygon(polygon);
        }
        if (stroked) {
          graphics.setColor(strokeColors.get(state));
          graphics.drawPolygon(polygon);
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
  private void updatePolygon()
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

//--------------------- Begin Interface Runnable ---------------------

  public void layerItemChanged(LayerItemEvent event)
  {
    if (event.getSource() == this) {
      setCurrentIcon(event.getItemState());
    }
  }

//--------------------- End Interface Runnable ---------------------

}
