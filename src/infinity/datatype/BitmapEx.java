// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.AbstractStruct;

import java.util.Vector;

/**
 * TODO: move functionality to Datatype
 * Adds UpdateListener callback support to the Bitmap class.
 * @author argent77
 */
public class BitmapEx extends Bitmap
{
  private final Vector<UpdateListener> listeners;

  public BitmapEx(byte buffer[], int offset, int length, String name, String[] table)
  {
    super(buffer, offset, length, name, table);
    listeners = new Vector<UpdateListener>();
  }

//--------------------- Begin Interface Editable ---------------------

 public boolean updateValue(AbstractStruct struct)
 {
   boolean result = super.updateValue(struct);
   if (!listeners.isEmpty()) {
     boolean ret = false;
     UpdateEvent event = new UpdateEvent(this, struct);
     for (final UpdateListener l: listeners)
       ret |= l.valueUpdated(event);
     if (ret)
       struct.fireTableDataChanged();
   }
   return result;
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
