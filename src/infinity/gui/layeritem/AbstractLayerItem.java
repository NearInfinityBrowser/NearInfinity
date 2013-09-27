// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.layeritem;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import infinity.gui.ViewFrame;
import infinity.gui.layeritem.LayerItemEvent.ItemState;
import infinity.resource.Viewable;

import javax.swing.JComponent;

/**
 * Common base class for visual components, representing parts of a game resource
 * @author argent77
 */
public abstract class AbstractLayerItem extends JComponent implements MouseListener, MouseMotionListener
{
  private Vector<ActionListener> actionListener;
  private Vector<LayerItemListener> itemStateListener;
  private String actionCommand;
  private Viewable viewable;
  private String message;
  private ItemState itemState;
  private Point location;
  private Point center;

  /**
   * Initialize object with default settings.
   */
  public AbstractLayerItem()
  {
    this(null);
  }

  /**
   * Initialize object with the specified map location.
   * @param location Map location
   */
  public AbstractLayerItem(Point location)
  {
    this(location, null);
  }

  /**
   * Initialize object with a specific map location and an associated viewable object.
   * @param location Map location
   * @param viewable Associated Viewable object
   */
  public AbstractLayerItem(Point location, Viewable viewable)
  {
    this(location, viewable, null);
  }

  /**
   * Initialize object with a specific map location, associated Viewable and an additional text message.
   * @param location Map location
   * @param viewable Associated Viewable object
   * @param msg An arbitrary text message
   */
  public AbstractLayerItem(Point location, Viewable viewable, String msg)
  {
    this.actionListener = new Vector<ActionListener>();
    this.itemStateListener = new Vector<LayerItemListener>();
    this.viewable = viewable;
    this.itemState = ItemState.NORMAL;
    this.center = new Point();
    setMapLocation(location);
    setMessage(msg);
    setActionCommand(null);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public String getActionCommand()
  {
    return actionCommand;
  }

  public void setActionCommand(String cmd)
  {
    if (cmd != null)
      actionCommand = cmd;
    else
      actionCommand = new String();
  }

  public void addActionListener(ActionListener l)
  {
    if (l != null)
      actionListener.add(l);
  }

  public ActionListener[] getActionListeners()
  {
    return (ActionListener[])actionListener.toArray();
  }

  public void removeActionListener(ActionListener l)
  {
    if (l != null)
      actionListener.remove(l);
  }

  public void addLayerItemListener(LayerItemListener l)
  {
    if (l != null)
      itemStateListener.add(l);
  }

  public LayerItemListener[] getLayerItemListeners()
  {
    return (LayerItemListener[])itemStateListener.toArray();
  }

  public void removeLayerItemListener(LayerItemListener l)
  {
    if (l != null)
      itemStateListener.remove(l);
  }

  /**
   * Moves this component to the specified location. Takes item-specific corrections into account.
   * @param x New x coordinate
   * @param y New y coordinate
   */
  public void setItemLocation(int x, int y)
  {
    setLocation(x - center.x, y - center.y);
  }

  /**
   * Moves this component to the specified location. Takes item-specific corrections into account.
   * @param p New location
   */
  public void setItemLocation(Point p)
  {
    if (p == null)
      p = new Point(0, 0);

    setLocation(new Point(p.x - center.x, p.y - center.y));
  }

  /**
   * Returns the map location of the item.
   * @return Map location of the item.
   */
  public Point getMapLocation()
  {
    return location;
  }

  /**
   * Sets a new map location of the item.
   * @param location New map location of the item.
   */
  public void setMapLocation(Point location)
  {
    if (location != null) {
      this.location = location;
    } else {
      location = new Point(0, 0);
    }
  }

  /**
   * Set a simple text message which can be queried at a given time
   * @param msg The text message
   */
  public void setMessage(String msg)
  {
    if (msg != null)
      message = new String(msg);
    else
      message = new String();
  }

  /**
   * Returns a simple text message associated with the component.
   * @return A text message.
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * Returns a String representation of this object.
   */
  public String toString()
  {
    return getMessage();
  }

  /**
   * Returns the current item state.
   * @return The current item state
   */
  public ItemState getItemState()
  {
    return itemState;
  }

  /**
   * Returns whether the component is in highlighted state, i.e. the mouse cursor has been placed
   * over the component.
   * @return The highlighted state of the component.
   */
  public boolean isHighlighted()
  {
    return itemState == ItemState.HIGHLIGHTED;
  }

  /**
   * Returns whether the component has been clicked on.
   * @return The selected state of the component
   */
  public boolean isSelected()
  {
    return itemState == ItemState.SELECTED;
  }

  /**
   * Associates a new Viewable object with the component
   * @param v The new viewable.
   */
  public void setViewable(Viewable v)
  {
    viewable = v;
  }

  /**
   * Returns the current Viewable object associated with the component.
   * @return The current Viewable object associated with the component.
   */
  public Viewable getViewable()
  {
    return viewable;
  }

  /**
   * Opens the current Viewable object associated with the component, if any.
   */
  public void showViewable()
  {
    if (viewable != null) {
      new ViewFrame(this.getTopLevelAncestor(), (Viewable)viewable);
    }
  }

//--------------------- Begin Interface MouseListener ---------------------

  public void mouseClicked(MouseEvent event)
  {
//    if (event.getButton() == MouseEvent.BUTTON1 &&
//        isMouseOver(new Point(event.getX(), event.getY())))
//      setItemState(ItemState.SELECTED);
  }

  public void mouseEntered(MouseEvent event)
  {
    if (isMouseOver(new Point(event.getX(), event.getY())))
      setItemState(ItemState.HIGHLIGHTED);
  }

  public void mouseExited(MouseEvent event)
  {
    setItemState(ItemState.NORMAL);
  }

  public void mousePressed(MouseEvent event)
  {
    if (event.getButton() == MouseEvent.BUTTON1 &&
        isMouseOver(new Point(event.getX(), event.getY())))
      setItemState(ItemState.SELECTED);
  }

  public void mouseReleased(MouseEvent event)
  {
    if (event.getButton() == MouseEvent.BUTTON1 &&
        isMouseOver(new Point(event.getX(), event.getY())))
      setItemState(ItemState.HIGHLIGHTED);
    else
      setItemState(ItemState.NORMAL);
  }

//--------------------- End Interface MouseListener ---------------------

//--------------------- Begin Interface MouseMotionListener ---------------------

  public void mouseDragged(MouseEvent event)
  {
  }

  public void mouseMoved(MouseEvent event)
  {
    // override
    if (isMouseOver(new Point(event.getX(), event.getY()))) {
      if (itemState != ItemState.SELECTED)
        setItemState(ItemState.HIGHLIGHTED);
    } else
      setItemState(ItemState.NORMAL);
  }

//--------------------- End Interface MouseMotionListener ---------------------

  // Returns whether the mouse cursor is over the relevant part of the component
  protected boolean isMouseOver(Point pt)
  {
    if (pt != null)
      return getBounds().contains(pt);
    else
      return false;
  }

  // Adds an offset to the component's position
  protected void setLocationOffset(Point ofs)
  {
    if (ofs != null) {
      center.x = ofs.x;
      center.y = ofs.y;
    }
  }

  // Returns the offset to the component's position
  protected Point getLocationOffset()
  {
    return center;
  }

  private void setItemState(ItemState newState)
  {
    if (itemState != newState) {
      itemState = newState;
      if (!itemStateListener.isEmpty()) {
        LayerItemEvent ise = new LayerItemEvent(this, itemState, actionCommand);
        for (final LayerItemListener l: itemStateListener)
          l.layerItemChanged(ise);
      }
      if (itemState == ItemState.SELECTED && !actionListener.isEmpty()) {
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
        for (final ActionListener l: actionListener)
          l.actionPerformed(ae);
      }
      if (itemState == ItemState.HIGHLIGHTED || itemState == ItemState.SELECTED) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      } else {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }
}
