// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;
import tv.porst.jhexview.SimpleDataProvider;

/**
 * A slightly improved version of {@link SimpleDataProvider} which allows to change size
 * of the data buffer with the {@link #setDataLength(int)} method.
 */
public class VariableDataProvider implements IDataProvider
{
  private final List<IDataChangedListener> listeners = new ArrayList<>();

  private byte[] buffer;

  public VariableDataProvider()
  {
    buffer = new byte[0];
  }

  public VariableDataProvider(byte[] data)
  {
    buffer = data;
  }

  @Override
  public void addListener(IDataChangedListener listener)
  {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  @Override
  public byte[] getData(long offset, int length)
  {
    if (offset + length > getDataLength()) {
      length = getDataLength() - (int)offset;
    }
    if (length > 0) {
      return Arrays.copyOfRange(buffer, (int) offset, (int) (offset + length));
    } else {
      return new byte[0];
    }
  }

  @Override
  public int getDataLength()
  {
    return buffer.length;
  }

  /**
   * Resizes the current data buffer to the specified length in bytes.
   * Old data will be retained as best as possible.
   * @param newLength The new size of the data buffer in bytes.
   * @return The old length of the data buffer in bytes.
   */
  public int setDataLength(int newLength)
  {
    newLength = Math.max(0, newLength);
    byte[] temp = new byte[newLength];
    int copySize = Math.min(buffer.length, temp.length);
    System.arraycopy(buffer, 0, temp, 0, copySize);
    int retVal = buffer.length;
    buffer = temp;
    return retVal;
  }

  @Override
  public boolean hasData(long start, int length)
  {
    return ((int)start + length <= getDataLength());
  }

  @Override
  public boolean isEditable()
  {
    return true;
  }

  @Override
  public boolean keepTrying()
  {
    return false;
  }

  @Override
  public void removeListener(IDataChangedListener listener)
  {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  @Override
  public void setData(long offset, byte[] data)
  {
    int length = data.length;
    if (offset + data.length > getDataLength()) {
      length = getDataLength() - (int)offset;
    }
    if (length > 0) {
      System.arraycopy(data, 0, buffer, (int) offset, data.length);
      fireDataChangedListener();
    }
  }

  protected void fireDataChangedListener()
  {
    if (!listeners.isEmpty()) {
      DataChangedEvent event = new DataChangedEvent(this);
      for (final IDataChangedListener l: listeners) {
        l.dataChanged(event);
      }
    }
  }
}
