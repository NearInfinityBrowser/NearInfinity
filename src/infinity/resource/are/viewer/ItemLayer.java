// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.gui.layeritem.AbstractLayerItem;
import infinity.resource.Viewable;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;

/**
 * Manages a single layer of map structures.
 * @author argent77
 */
public class ItemLayer
{
  /**
   * Identifies the layer of map structures.
   */
  public static enum Type { ACTOR, REGION, ENTRANCE, CONTAINER, AMBIENT, AMBIENTRANGE, DOOR,
                            ANIMATION, AUTOMAP, SPAWNPOINT, PROTRAP, DOORPOLY, WALLPOLY }

  private final AreaViewer viewer;
  private final Type type;
  private final List<AbstractLayerItem> itemList;

  private String name;          // short and unique name that identifies a specific layer
  private String text;          // more descriptive version of 'name'
  private boolean selected;     // the selected state of the layer
  private boolean extended;     // indicates whether the layer uses two items as one logical item

  /**
   * Creas a new empty item layer with default settings. It is strongly advised to set id,
   * buttonText and listener afterwards.
   * @param type The type of the layer. Can not be changed later.
   */
  public ItemLayer(AreaViewer viewer, Type type)
  {
    this(viewer, type, null, null, false);
  }

  public ItemLayer(AreaViewer viewer, Type type, String name, String longName)
  {
    this(viewer, type, name, longName, false);
  }

  /**
   * Creates a new empty item layer with the specified parameters.
   * @param type The type of the layer. Can not be changed later.
   * @param id A short and unique keyword that describes the layer type (e.g. "Ambient" or "Actor").
   * @param buttonText The text that is associated with the layer's checkbox.
   * @param listener A listener that handles the layer's checkbox state changes.
   */
  public ItemLayer(AreaViewer viewer, Type type, String name, String longName, boolean initialState)
  {
    if (viewer == null || type == null)
      throw new NullPointerException();

    this.viewer = viewer;
    this.type = type;
    this.itemList = new Vector<AbstractLayerItem>();

    setName(name);
    setLongName(longName);
    setSelected(initialState);
  }

  /**
   * Returns the number of items stored in the layer.
   * @return Number of items.
   */
  public int itemCount()
  {
    return itemList.size();
  }

  /**
   * Returns whether the items have been associated with the layer.
   * @return <code>true</code> if no items are available, <code>false</code> otherwise.
   */
  public boolean isEmpty()
  {
    return itemList.isEmpty();
  }

  /**
   * Adds a new item to the layer.
   * @param item The item to add (can not be <code>null</code>).
   */
  public void add(AbstractLayerItem item)
  {
    if (item != null) {
      itemList.add(item);
    }
  }

  /**
   * Adds a whole collection of items to the layer.
   * @param list A collection of items to be added.
   */
  public void add(Collection<AbstractLayerItem> list)
  {
    if (list != null && !list.isEmpty()) {
      for (final AbstractLayerItem item: list) {
        if (item != null) {
          itemList.add(item);
        }
      }
    }
  }

  /**
   * Removes the item at the specified index.
   * @param index The index of the item to remove
   * @return The removed item.
   */
  public AbstractLayerItem remove(int index)
  {
    if (index >= 0 && index < itemList.size()) {
      return itemList.remove(index);
    } else {
      return null;
    }
  }

  /**
   * The item object to remove from the layer.
   * @param item The item object to remove.
   * @return <code>true</code> if the item was in the list and has been removed.
   */
  public boolean remove(AbstractLayerItem item)
  {
    if (item != null) {
      return itemList.remove(item);
    } else {
      return false;
    }
  }

  /**
   * Removes all items from the layer.
   */
  public void clear()
  {
    itemList.clear();
  }

  /**
   * Returns the list of layer items.
   * @return The list of layer items.
   */
  public List<AbstractLayerItem> getItemList()
  {
    return itemList;
  }

  /**
   * Returns the item at the specified index.
   * @param index The index of the item to return.
   * @return The item at the specified index, or <code>null</code> otherwise.
   */
  public AbstractLayerItem getItem(int index)
  {
    if (index >= 0 && index < itemList.size()) {
      return itemList.get(index);
    }
    return null;
  }

  /**
   * Returns the first item with the associated viewable.
   * @param viewable The viewable attached to the item in question.
   * @return The matching item, or <code>null</code> otherwise.
   */
  public AbstractLayerItem findItem(Viewable viewable)
  {
    if (viewable != null) {
      for (final AbstractLayerItem item: itemList) {
        if (item.getViewable() == viewable)
          return item;
      }
    }
    return null;
  }

  /**
   * Returns the first item with the associated text message.
   * @param message The text message attached to the item in question.
   * @return The matching item, or <code>null</code> otherwise.
   */
  public AbstractLayerItem findItem(String message)
  {
    if (message != null) {
      for (final AbstractLayerItem item: itemList) {
        if (message.equals(item.getMessage()))
          return item;
      }
    }
    return null;
  }

  /**
   * Returns the first item with the specified map location.
   * @param mapLocation The map location of the item in question.
   * @return The matching item, or <code>null</code> otherwise.
   */
  public AbstractLayerItem findItem(Point mapLocation)
  {
    if (mapLocation != null) {
      for (final AbstractLayerItem item: itemList) {
        if (mapLocation.equals(item.getMapLocation()))
          return item;
      }
    }
    return null;
  }

  /**
   * Returns the type of the layer.
   * @return The type of the layer.
   */
  public Type getType()
  {
    return type;
  }

  /**
   * Returns the checkbox associated with the layer.
   * @return A JCheckBox object.
   */
//  public JCheckBox getCheckBox()
//  {
//    return cbButton;
//  }

  /**
   * Returns the associated name.
   * @return The associated name.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Sets a new name for the layer. It should be short and unique that describes the layer
   * (e.g. "Ambient" or "Container").
   * @param name The new name of the layer.
   */
  public void setName(String name)
  {
    if (text != null) {
      this.name = name;
    } else {
      this.name = type.toString();
    }
  }

  /**
   * Returns the associated long name.
   * @return The associated long name
   */
  public String getLongName()
  {
    return text;
  }

  /**
   * Sets a new long name for the layer.
   * @param text The new long name.
   */
  public void setLongName(String text)
  {
    if (text != null) {
      this.text = text;
    } else {
      this.text = type.toString();
    }
  }

  /**
   * Returns whether this layer uses two items as one logical item (e.g. doors in opened/closed state).
   * @return <code>true</code> if the layer is in extended state.
   */
  public boolean isExtended()
  {
    return extended;
  }

  /**
   * Sets the extended state of the layer. An extended layer uses two items as one logical item
   * (e.g. doors in opened/closed states).
   * @param set
   */
  public void setExtended(boolean set)
  {
    if (extended != set) {
      extended = set;
    }
  }

  /**
   * Returns whether the item of the specified index is currently active.
   * @param itemIndex The item index
   * @return <code>true</code> if the specified item is currently active.
   */
  public boolean isExtendedItemActive(int itemIndex)
  {
    if (isExtended()) {
      return ((itemIndex & 1) == 1) == viewer.isDoorStateClosed();
    } else {
      return true;
    }
  }

  /**
   * Returns whether the specified listener has been connected with the layer's checkbox.
   * @param listener The lister to query.
   * @return <code>true</code> if the specified listener has been connected, <code>false</code> otherwise.
   */
//  public boolean isConnected(ItemListener listener)
//  {
//    if (listener != null) {
//      for (final ItemListener l: cbButton.getItemListeners()) {
//        if (l == listener)
//          return true;
//      }
//    }
//    return false;
//  }

  /**
   * Connects the layer's checkbox with the specified listener object.
   * @param listener The listener object for the layer's checkbox.
   */
//  public void connect(ItemListener listener)
//  {
//    if (listener != null) {
//      cbButton.addItemListener(listener);
//    }
//  }

  /**
   * Removes the specified listener from the layer's checkbox.
   * @param listener The listener to remove from the layer's checkbox.
   */
//  public void disconnect(ItemListener listener)
//  {
//    if (listener != null) {
//      cbButton.removeItemListener(listener);
//    } else {
//      for (final ItemListener l: cbButton.getItemListeners()) {
//        cbButton.removeItemListener(l);
//      }
//    }
//  }

  /**
   * Enables or disables the specified layer and sets the appropriate visual state of the
   * associated items.
   * @param enable The new layer state.
   */
  public void setEnabled(boolean enable)
  {
    if (type != null) {
      for (int i = 0; i < itemList.size(); i++) {
        AbstractLayerItem item = itemList.get(i);
        if (item != null) {
          item.setVisible(isExtendedItemActive(i) && enable);
        }
      }
      LayerManager.setSelectedState(type, enable);
    }
  }

  /**
   * Returns the selection state of the layer.
   * @return The selection state of the layer
   */
  public boolean isSelected()
  {
    return selected;
  }

  /**
   * Sets the selection state of the layer.
   * @param select The new selection state.
   */
  public void setSelected(boolean select)
  {
    selected = select;
  }

  /**
   * Creates a pre-initialized checkbox based on this layer.
   * @param listener An optional listener to add to the checkbox. Specify <code>null</code> to omit.
   * @return A new JCheckBox object with configured text, selection state and optional listener.
   */
  public JCheckBox createCheckBox(ItemListener listener)
  {
    JCheckBox cb = new JCheckBox(text, selected);
    if (listener != null) {
      cb.addItemListener(listener);
    }
    return cb;
  }

  /**
   * Adds the item of the specified index to the container.
   * @param index The item index in the layer item list
   * @param target The container to add the item to.
   */
  public void addToContainer(int index, Container target)
  {
    addToContainer(getItem(index), target);
  }

  /**
   * Adds the specified item to the container.
   * @param item The item to add.
   * @param target The container to add the item to.
   */
  public void addToContainer(AbstractLayerItem item, Container target)
  {
    if (item != null && target != null) {
      item.setVisible(false);
      target.add(item);
      item.setItemLocation(item.getMapLocation());
    }
  }

  /**
   * Removes the item at the specified index from the container.
   * @param index The item index in the layer item list.
   * @param target The container to remove the item from.
   */
  public void removeFromContainer(int index, Container target)
  {
    removeFromContainer(getItem(index), target);
  }

  /**
   * Removes the specified item from the container.
   * @param item The item to remove.
   * @param target The container to remove the item from.
   */
  public void removeFromContainer(AbstractLayerItem item, Container target)
  {
    if (item != null && target != null) {
      target.remove(item);
    }
  }

  /**
   * Adds all items in the list to the specified container.
   * @param target The container to add the items to.
   */
  public void addAllToContainer(Container target)
  {
    if (target != null) {
      for (final AbstractLayerItem item: itemList) {
        addToContainer(item, target);
      }
    }
  }

  /**
   * Removes all items in the list from the specified container.
   * @param target The container to remove the items from.
   */
  public void removeAllFromContainer(Container target)
  {
    if (target != null) {
      for (final AbstractLayerItem item: itemList) {
        target.remove(item);
      }
    }
  }

  @Override
  public String toString()
  {
    return String.format("Type: %1$s, items: %2$d, name: %3$s, longName: %4$s, selected: %5$b",
                         type.toString(), itemList.size(), name, text, selected);
  }

}
