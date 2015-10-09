// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.Vector;

import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

/**
 * Adds UpdateListener callback support to the Flag class.
 */
public class FlagEx extends Flag
{
  private final Vector<UpdateListener> listeners;

  FlagEx(byte[] buffer, int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  FlagEx(StructEntry parent, byte[] buffer, int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name);
    listeners = new Vector<UpdateListener>();
  }

  public FlagEx(byte[] buffer, int offset, int length, String name, String[] stable)
  {
    this(null, buffer, offset, length, name, stable);
  }

  public FlagEx(StructEntry parent, byte[] buffer, int offset, int length, String name, String[] stable)
  {
    super(parent, buffer, offset, length, name, stable);
    listeners = new Vector<UpdateListener>();
  }

//--------------------- Begin Interface Editable ---------------------

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    boolean retVal = super.updateValue(struct);
    if (!listeners.isEmpty()) {
      boolean ret = false;
      UpdateEvent event = new UpdateEvent(this, struct);
      for (final UpdateListener l: listeners) {
        ret |= l.valueUpdated(event);
      }
      if (ret) {
        struct.fireTableDataChanged();
      }
    }
    return retVal;
  }

//--------------------- End Interface Editable ---------------------

  /**
   * Adds the specified update listener to receive update events from this object.
   * If listener l is null, no exception is thrown and no action is performed.
   * @param l The update listener
   */
  public void addUpdateListener(UpdateListener l)
  {
    if (l != null)
      listeners.add(l);
  }

  /**
   * Returns an array of all update listeners registered on this object.
   * @return All of this object's update listener or an empty array if no listener is registered.
   */
  public UpdateListener[] getUpdateListeners()
  {
    UpdateListener[] ar = new UpdateListener[listeners.size()];
    for (int i = 0; i < listeners.size(); i++)
      ar[i] = listeners.get(i);
    return ar;
  }

  /**
   * Removes the specified update listener, so that it no longer receives update events
   * from this object.
   * @param l The update listener
   */
  public void removeUpdateListener(UpdateListener l)
  {
    if (l != null)
      listeners.remove(l);
  }

}
