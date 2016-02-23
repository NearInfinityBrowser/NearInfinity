// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.JComponent;

import org.infinity.gui.ViewFrame;
import org.infinity.resource.Viewable;

/**
 * Common base class for visual components, representing parts of a game resource
 * @author argent77
 */
public abstract class AbstractLayerItem extends JComponent implements MouseListener, MouseMotionListener
{
  /**
   * Represents the possible visual states of the component
   */
  public enum ItemState { NORMAL, HIGHLIGHTED }

  private Vector<ActionListener> actionListener;
  private Vector<LayerItemListener> itemStateListener;
  private String actionCommand;
  private Viewable viewable;
  private Object objData;
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
    if (cmd != null) {
      actionCommand = cmd;
    } else {
      actionCommand = new String();
    }
  }

  public void addActionListener(ActionListener l)
  {
    if (l != null) {
      actionListener.add(l);
    }
  }

  public ActionListener[] getActionListeners()
  {
    ActionListener[] array = new ActionListener[actionListener.size()];
    for (int i = 0; i < actionListener.size(); i++) {
      array[i] = actionListener.get(i);
    }
    return array;
  }

  public void removeActionListener(ActionListener l)
  {
    if (l != null) {
      actionListener.remove(l);
    }
  }

  public void addLayerItemListener(LayerItemListener l)
  {
    if (l != null) {
      itemStateListener.add(l);
    }
  }

  public LayerItemListener[] getLayerItemListeners()
  {
    LayerItemListener[] array = new LayerItemListener[itemStateListener.size()];
    for (int i = 0; i < itemStateListener.size(); i++) {
      array[i] = itemStateListener.get(i);
    }
    return array;
  }

  public void removeLayerItemListener(LayerItemListener l)
  {
    if (l != null) {
      itemStateListener.remove(l);
    }
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
    if (p == null) {
      p = new Point(0, 0);
    }

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
    if (msg != null) {
      message = new String(msg);
    } else {
      message = new String();
    }
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
  @Override
  public String toString()
  {
    return getMessage();
  }

  /**
   * Returns the item's current visual state.
   * @return The item's current visual state.
   */
  public ItemState getItemState()
  {
    return itemState;
  }

  /**
   * Attaches a custom data object to this layer item.
   * @param data The data item to attach.
   */
  public void setData(Object data)
  {
    objData = data;
  }

  /**
   * Returns the custom data object that has been attached to this layer item.
   * @return The custom data object attached to this layer item.
   */
  public Object getData()
  {
    return objData;
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
    if (viewable != null && getTopLevelAncestor() != null) {
      new ViewFrame(getTopLevelAncestor(), (Viewable)viewable);
    }
  }

//--------------------- Begin Interface MouseListener ---------------------

  @Override
  public void mouseClicked(MouseEvent event)
  {
  }

  @Override
  public void mouseEntered(MouseEvent event)
  {
    if (isMouseOver(event.getPoint())) {
      setItemState(ItemState.HIGHLIGHTED);
    }
  }

  @Override
  public void mouseExited(MouseEvent event)
  {
    setItemState(ItemState.NORMAL);
  }

  @Override
  public void mousePressed(MouseEvent event)
  {
    if (isMouseOver(event.getPoint())) {
      setMouseClicked(event.getButton());
    }
  }

  @Override
  public void mouseReleased(MouseEvent event)
  {
    if (isMouseOver(event.getPoint())) {
      setItemState(ItemState.HIGHLIGHTED);
    } else {
      setItemState(ItemState.NORMAL);
    }
  }

//--------------------- End Interface MouseListener ---------------------

//--------------------- Begin Interface MouseMotionListener ---------------------

  @Override
  public void mouseDragged(MouseEvent event)
  {
  }

  @Override
  public void mouseMoved(MouseEvent event)
  {
    if (isMouseOver(event.getPoint())) {
      setItemState(ItemState.HIGHLIGHTED);
    } else {
      setItemState(ItemState.NORMAL);
    }
  }

//--------------------- End Interface MouseMotionListener ---------------------

  @Override
  public String getToolTipText(MouseEvent event)
  {
    // Tooltip is only displayed over visible areas of this component
    if (isMouseOver(event.getPoint())) {
      return message;
    } else {
      return null;
    }
  }

  @Override
  public boolean contains(int x, int y)
  {
    // Non-visible parts of the component are disregarded by mouse events
    return isMouseOver(new Point(x, y));
  }

  // Returns whether the mouse cursor is over the relevant part of the component
  protected boolean isMouseOver(Point pt)
  {
    if (pt != null) {
      return getBounds().contains(pt);
    } else {
      return false;
    }
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
        LayerItemEvent ise = new LayerItemEvent(this, actionCommand);
        for (final LayerItemListener l: itemStateListener)
          l.layerItemChanged(ise);
      }
      if (itemState == ItemState.HIGHLIGHTED) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      } else {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  private void setMouseClicked(int button)
  {
    if ((button == MouseEvent.BUTTON1) && !actionListener.isEmpty()) {
      // processing left mouse click event
      ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
      for (final ActionListener l: actionListener) {
        l.actionPerformed(ae);
      }
    } else if (button == MouseEvent.BUTTON2) {
      // processing right mouse click event
    } else if (button == MouseEvent.BUTTON3) {
      // processing middle mouse click event
    }
  }
}
