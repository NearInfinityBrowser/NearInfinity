// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;
import infinity.util.io.FileWriterNI;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public abstract class Datatype implements StructEntry
{
  protected static final Dimension DIM_WIDE = new Dimension(800, 100);
  protected static final Dimension DIM_BROAD = new Dimension(650, 100);
  protected static final Dimension DIM_MEDIUM = new Dimension(450, 100);

  private final List<UpdateListener> listeners = new ArrayList<UpdateListener>();
  private final int length;

  private String name;
  private int offset;
  private StructEntry parent;

  protected Datatype(int offset, int length, String name)
  {
    this(null, offset, length, name);
  }

  protected Datatype(StructEntry parent, int offset, int length, String name)
  {
    this.parent = parent;
    this.offset = offset;
    this.length = length;
    this.name = name;
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(StructEntry o)
  {
    return offset - o.getOffset();
  }

// --------------------- End Interface Comparable ---------------------


// --------------------- Begin Interface StructEntry ---------------------

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    return super.clone();
  }

  @Override
  public void copyNameAndOffset(StructEntry entry)
  {
    name = entry.getName();
    offset = entry.getOffset();
  }

  @Override
  public StructEntry getParent()
  {
    return parent;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public int getOffset()
  {
    return offset;
  }

  @Override
  public int getSize()
  {
    return length;
  }

  @Override
  public List<StructEntry> getStructChain()
  {
    List<StructEntry> list = new Vector<StructEntry>();
    StructEntry e = this;
    while (e != null) {
      list.add(0, e);
      e = e.getParent();
      if (list.contains(e)) {
        // avoid infinite loops
        break;
      }
    }
    return list;
  }

  @Override
  public void setOffset(int newoffset)
  {
    offset = newoffset;
  }

  @Override
  public void setParent(StructEntry parent)
  {
    this.parent = parent;
  }

  @Override
  public byte[] getDataBuffer()
  {
    ByteArrayOutputStream os = new ByteArrayOutputStream(getSize());
    try {
      write(os);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return os.toByteArray();
  }

// --------------------- End Interface StructEntry ---------------------

  /**
   * Adds the specified update listener to receive update events from this object.
   * If listener l is null, no exception is thrown and no action is performed.
   * @param l The update listener
   */
  public void addUpdateListener(UpdateListener l)
  {
    if (l != null) {
      listeners.add(l);
    }
  }

  /**
   * Returns an array of all update listeners registered on this object.
   * @return All of this object's update listener or an empty array if no listener is registered.
   */
  public UpdateListener[] getUpdateListeners()
  {
    UpdateListener[] ar = new UpdateListener[listeners.size()];
    for (int i = 0; i < listeners.size(); i++) {
      ar[i] = listeners.get(i);
    }
    return ar;
  }

  /**
   * Removes the specified update listener, so that it no longer receives update events
   * from this object.
   * @param l The update listener
   */
  public void removeUpdateListener(UpdateListener l)
  {
    if (l != null) {
      listeners.remove(l);
    }
  }

  /**
   * Notifies all listeners that the value of this Datatype object may have changed.
   */
  protected void fireValueUpdated(UpdateEvent event)
  {
    if (event != null) {
      // don't lose current selection
      if (event.getStructure().getViewer() != null) {
        event.getStructure().getViewer().storeCurrentSelection();
      }
      boolean retVal = false;
      for (final UpdateListener l: listeners) {
        retVal |= l.valueUpdated(event);
      }
      if (retVal) {
        event.getStructure().fireTableDataChanged();
      }
      if (event.getStructure().getViewer() != null) {
        event.getStructure().getViewer().restoreCurrentSelection();
      }
    }
  }


  void writeInt(OutputStream os, int value) throws IOException
  {
    if (getSize() == 4)
      FileWriterNI.writeInt(os, value);
    else if (getSize() == 3)
      FileWriterNI.writeInt24(os, value);
    else if (getSize() == 2)
      FileWriterNI.writeShort(os, (short)value);
    else if (getSize() == 1)
      FileWriterNI.writeByte(os, (byte)value);
    else
      throw new IllegalArgumentException();
  }

  void writeLong(OutputStream os, long value) throws IOException
  {
    if (getSize() == 4)
      FileWriterNI.writeInt(os, (int)value);
    else if (getSize() == 3)
      FileWriterNI.writeInt24(os, (int)value);
    else if (getSize() == 2)
      FileWriterNI.writeShort(os, (short)value);
    else if (getSize() == 1)
      FileWriterNI.writeByte(os, (byte)value);
    else
      throw new IllegalArgumentException();
  }
}

