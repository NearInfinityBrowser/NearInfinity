// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Logger;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;

/**
 * Provides data as byte array from the associated ResourceEntry instance to be used in JHexView components.
 */
public class ResourceDataProvider implements IDataProvider {
  private final ArrayList<IDataChangedListener> listeners = new ArrayList<>();
  private final HashMap<Integer, Byte> modifiedMap = new HashMap<>();
  private final ResourceEntry entry;

  private int size;

  public ResourceDataProvider(ResourceEntry entry) {
    if (entry == null) {
      throw new NullPointerException("entry is null");
    }
    this.entry = entry;
    size = -1; // initialized when needed
  }

  // --------------------- Begin Interface IDataProvider ---------------------

  @Override
  public void addListener(IDataChangedListener listener) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public byte[] getData(long offset, int length) {
    if (offset + length > getDataLength()) {
      length = getDataLength() - (int) offset;
    }

    if (length > 0) {
      try {
        ByteBuffer bb = getResourceEntry().getResourceBuffer();
        byte[] retVal = new byte[length];
        for (int i = 0; i < length; i++) {
          if (isModifiedData((int) offset + i)) {
            retVal[i] = getModifiedData((int) offset + i);
          } else {
            retVal[i] = bb.get((int) offset + i);
          }
        }
        return retVal;
      } catch (Exception e) {
        Logger.error(e);
      }
    }
    return null;
  }

  @Override
  public int getDataLength() {
    if (size < 0) {
      size = 0;
      try {
        long resSize = getResourceEntry().getResourceSize();
        if (resSize >= 0) {
          size = (int) resSize;
        }
      } catch (Exception e) {
        Logger.error(e);
      }
    }
    return size;
  }

  @Override
  public boolean hasData(long start, int length) {
    return (start >= 0 && start + length <= getDataLength());
  }

  @Override
  public boolean isEditable() {
    // to be fully implemented
    return true;
  }

  @Override
  public boolean keepTrying() {
    return false;
  }

  @Override
  public void removeListener(IDataChangedListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  @Override
  public void setData(long offset, byte[] data) {
    if (data != null) {
      for (byte element : data) {
        addModifiedData((int) offset, element);
      }
      if (data.length > 0) {
        fireDataChanged();
      }
    }
  }

  // --------------------- End Interface IDataProvider ---------------------

  /** Returns the attached ResourceEntry instance. */
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  /** Removes all modified data information from the provider. */
  public void clear() {
    clearModifiedData();
  }

  protected void fireDataChanged() {
    if (!listeners.isEmpty()) {
      DataChangedEvent event = new DataChangedEvent(this);
      for (int i = listeners.size() - 1; i >= 0; i--) {
        listeners.get(i).dataChanged(event);
      }
    }
  }

  private boolean isModifiedData(int offset) {
    return modifiedMap.containsKey(offset);
  }

  private byte getModifiedData(int offset) {
    Byte b = modifiedMap.get(offset);
    if (b != null) {
      return b;
    } else {
      return 0;
    }
  }

  private void addModifiedData(int offset, byte value) {
    if (offset >= 0 && offset < getDataLength()) {
      modifiedMap.put(offset, value);
    }
  }

  // private void removeModifiedData(int offset)
  // {
  // modifiedMap.remove(Integer.valueOf(offset));
  // }

  // Removes all modified data from the map.
  private void clearModifiedData() {
    modifiedMap.clear();
  }
}
