// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
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
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;

/**
 * Common base class for visual components, representing parts of a game resource
 */
public abstract class AbstractLayerItem extends JComponent implements MouseListener, MouseMotionListener
{
  /**
   * Represents the possible visual states of the component
   */
  public enum ItemState { NORMAL, HIGHLIGHTED }

  private final Vector<ActionListener> actionListener = new Vector<>();
  private final Vector<LayerItemListener> itemStateListener = new Vector<>();
  private final Viewable viewable;
  private Object objData;
  private ItemState itemState;
  private final Point center;

  /**
   * Initialize object with a associated viewable object and message for
   * both info box and quick info.
   *
   * @param viewable Associated Viewable object
   * @param tooltip A short text message shown as tooltip or menu item text
   */
  public AbstractLayerItem(Viewable viewable, String tooltip)
  {
    this.viewable = viewable;
    this.itemState = ItemState.NORMAL;
    this.center = new Point();
    if (viewable instanceof StructEntry) {
      setToolTipText(((StructEntry) viewable).getName() + ": " + tooltip);
    } else {
      setToolTipText(tooltip);
    }
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void addActionListener(ActionListener l)
  {
    if (l != null) {
      actionListener.add(l);
    }
  }

  public ActionListener[] getActionListeners()
  {
    return actionListener.toArray(new ActionListener[actionListener.size()]);
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
    return itemStateListener.toArray(new LayerItemListener[itemStateListener.size()]);
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
   * Returns a String representation of this object.
   */
  @Override
  public String toString()
  {
    return getToolTipText();
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
      new ViewFrame(getTopLevelAncestor(), viewable);
    }
  }

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

  @Override
  public boolean contains(int x, int y)
  {
    // Non-visible parts of the component are disregarded by mouse events
    return isMouseOver(new Point(x, y));
  }

  /** Returns whether the mouse cursor is over the relevant part of the component. */
  protected boolean isMouseOver(Point pt)
  {
    if (pt != null) {
      return getBounds().contains(pt);
    } else {
      return false;
    }
  }

  /** Adds an offset to the component's position. */
  protected void setLocationOffset(Point ofs)
  {
    if (ofs != null) {
      center.x = ofs.x;
      center.y = ofs.y;
    }
  }

  /** Returns the offset to the component's position. */
  protected Point getLocationOffset()
  {
    return center;
  }

  private void setItemState(ItemState newState)
  {
    if (itemState != newState) {
      itemState = newState;
      if (!itemStateListener.isEmpty()) {
        final LayerItemEvent ise = new LayerItemEvent(this, "");
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
      final ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
      for (final ActionListener l: actionListener) {
        l.actionPerformed(ae);
      }
    }
  }
}
