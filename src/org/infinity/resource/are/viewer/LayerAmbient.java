// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.ArrayList;
import java.util.List;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.resource.are.Ambient;
import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_AMBIENTS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_AMBIENTS;

/**
 * Manages ambient sound layer objects (including global and local ambient sounds).
 */
public class LayerAmbient extends BasicLayer<LayerObjectAmbient, AreResource>
{
  private static final String[] AvailableFmt = {"Global ambient sounds: %d",
                                                "Local ambient sound: %d",
                                                "Ambient sounds: %d"};

  // stores ambient sound objects with local radius
  private final List<LayerObjectAmbient> listGlobalSounds = new ArrayList<>();
  private final List<LayerObjectAmbient> listLocalSounds = new ArrayList<>();

  private boolean iconEnabled = true;
  private boolean rangeEnabled;

  public LayerAmbient(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.AMBIENT, viewer);
    loadLayer();
  }

  @Override
  public void close()
  {
    super.close();
    listGlobalSounds.clear();
    listLocalSounds.clear();
  }

  @Override
  protected void loadLayer()
  {
    loadLayerItems(ARE_OFFSET_AMBIENTS, ARE_NUM_AMBIENTS, Ambient.class, a -> {
      final LayerObjectAmbient obj = new LayerObjectAmbient(parent, a);
      // putting global/local sounds into separate lists for faster access
      if (obj.isLocal()) {
        listLocalSounds.add(obj);
      } else {
        listGlobalSounds.add(obj);
      }
      return obj;
    });
  }

  /**
   * Sets the visibility state of all items in the layer. Takes enabled states of the different
   * item types into account.
   */
  @Override
  public void setLayerVisible(boolean visible)
  {
    setVisibilityState(visible);
    List<LayerObjectAmbient> list = getLayerObjects();
    boolean state;
    for (int i = 0, size = list.size(); i < size; i++) {
      LayerObjectAmbient obj = list.get(i);
      state = isLayerVisible(ViewerConstants.AMBIENT_ITEM_ICON) && (!isScheduleEnabled() || isScheduled(i));
      AbstractLayerItem item = obj.getLayerItem(ViewerConstants.AMBIENT_ITEM_ICON);
      if (item != null) {
        item.setVisible(state && iconEnabled);
      }
      state = isLayerVisible(ViewerConstants.AMBIENT_ITEM_RANGE) && (!isScheduleEnabled() || isScheduled(i));
      item = obj.getLayerItem(ViewerConstants.AMBIENT_ITEM_RANGE);
      if (item != null) {
        item.setVisible(state && rangeEnabled);
      }
    }
  }

  /**
   * Returns whether the specific layer item type is visible.
   * @param itemType One of {@code ViewerConstants.AMBIENT_ITEM_ICON} and {@code ViewerConstants.AMBIENT_ITEM_RANGE}.
   * @return Whether items of the specified state are visible.
   */
  public boolean isLayerVisible(int itemType)
  {
    if (itemType == ViewerConstants.AMBIENT_ITEM_ICON) {
      return isLayerVisible() && iconEnabled;
    } else if (itemType == ViewerConstants.AMBIENT_ITEM_RANGE) {
      return isLayerVisible() && rangeEnabled;
    } else {
      return false;
    }
  }

  /**
   * Returns whether the items of specified type have been enabled (i.e. will be shown when visibility state
   * is set to {@code true}).
   * @param itemType One of {@code ViewerConstants.AMBIENT_ITEM_ICON} and {@code ViewerConstants.AMBIENT_ITEM_RANGE}.
   * @return The activation state of the item type.
   */
  public boolean isItemTypeEnabled(int itemType)
  {
    if (itemType == ViewerConstants.AMBIENT_ITEM_ICON) {
      return iconEnabled;
    } else if (itemType == ViewerConstants.AMBIENT_ITEM_RANGE) {
      return rangeEnabled;
    } else {
      return false;
    }
  }

  /**
   * Sets the enabled state of the specified item type.
   * @param itemType One of {@code ViewerConstants.AMBIENT_ITEM_ICON} and {@code ViewerConstants.AMBIENT_ITEM_RANGE}.
   * @param enable Whether the item type will be considered when setting the visibility state of the layer items.
   */
  public void setItemTypeEnabled(int itemType, boolean enable)
  {
    boolean hasChanged = false;
    if (itemType == ViewerConstants.AMBIENT_ITEM_ICON) {
      if (enable != iconEnabled) {
        iconEnabled = enable;
        hasChanged = true;
      }
    } else if (itemType == ViewerConstants.AMBIENT_ITEM_RANGE) {
      if (enable != rangeEnabled) {
        rangeEnabled = enable;
        hasChanged = true;
      }
    }
    if (hasChanged) {
      setLayerVisible(isLayerVisible());
    }
  }

  /**
   * Returns the number of layer objects of the specified type.
   * @param ambientType The ambient sound type (either ViewerConstants.AMBIENT_TYPE_GLOBAL,
   *                    ViewerConstants.AMBIENT_TYPE_LOCAL or ViewerConstants.AMBIENT_TYPE_ALL).
   * @return Number of layer objects.
   */
  public int getLayerObjectCount(int ambientType)
  {
    int count = 0;
    if ((ambientType & ViewerConstants.AMBIENT_TYPE_ALL) == ViewerConstants.AMBIENT_TYPE_LOCAL) {
      count += listLocalSounds.size();
    }
    if ((ambientType & ViewerConstants.AMBIENT_TYPE_ALL) == ViewerConstants.AMBIENT_TYPE_GLOBAL) {
      count += listGlobalSounds.size();
    }
    return count;
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return getAvailabilityString(ViewerConstants.AMBIENT_TYPE_ALL, cnt);
  }

  /**
   * Returns the number of objects in this layer for the specified type as a formatted string.
   * @param ambientType The ambient sound type (either ViewerConstants.AMBIENT_TYPE_GLOBAL,
   *                    ViewerConstants.AMBIENT_TYPE_LOCAL or ViewerConstants.AMBIENT_TYPE_ALL).
   * @return A formatted string telling about the number of objects in this layer.
   */
  public String getAvailability(int ambientType)
  {
    ambientType &= ViewerConstants.AMBIENT_TYPE_ALL;
    int cnt = 0;
    if ((ambientType & ViewerConstants.AMBIENT_TYPE_LOCAL) != 0) {
      cnt += getLayerObjectCount(ViewerConstants.AMBIENT_TYPE_LOCAL);
    }
    if ((ambientType & ViewerConstants.AMBIENT_TYPE_GLOBAL) != 0) {
      cnt += getLayerObjectCount(ViewerConstants.AMBIENT_TYPE_GLOBAL);
    }
    return getAvailabilityString(ambientType, cnt);
  }


  // Returns the availability string based on ambient sound type and count
  private String getAvailabilityString(int ambientType, int count)
  {
    ambientType &= ViewerConstants.AMBIENT_TYPE_ALL;
    int idx = 0;
    switch (ambientType) {
      case ViewerConstants.AMBIENT_TYPE_ALL:
        idx++;
      case ViewerConstants.AMBIENT_TYPE_LOCAL:
        idx++;
      case ViewerConstants.AMBIENT_TYPE_GLOBAL:
        return String.format(AvailableFmt[idx], count);
      default:
        return "";
    }
  }
}
