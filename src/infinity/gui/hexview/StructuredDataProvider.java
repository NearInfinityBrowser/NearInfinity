// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.hexview;

import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;

/**
 * Provides data as byte array from the associated AbstractStruct instance to be used in
 * JHexView components.
 *
 * @author argent77
 */
public class StructuredDataProvider implements IDataProvider
{
  private final ArrayList<IDataChangedListener> listeners = new ArrayList<IDataChangedListener>();
  private final AbstractStruct struct;

  private List<StructEntry> listStructures;
  private int dataSize;

  /** Constructs a new DataProvider object that can be used in JHexView components. */
  public StructuredDataProvider(AbstractStruct struct)
  {
    if (struct == null) {
      throw new NullPointerException("struct is null");
    }
    this.struct = struct;
    this.dataSize = -1;         // mark as uninitialized
    this.listStructures = null; // mark as uninitialized
  }

//--------------------- Begin Interface IDataProvider ---------------------

  @Override
  public void addListener(IDataChangedListener listener)
  {
    if (listener != null && listeners.indexOf(listener) < 0) {
      listeners.add(listener);
    }
  }

  @Override
  public byte[] getData(long offset, int length)
  {
    // checking size
    int extraLength = 0;
    if (offset+length > getDataLength()) {
      extraLength = (int)(offset+length) - getDataLength();
      length = getDataLength() - (int)offset;
    }

    if (length > 0) {
      ArrayList<StructEntry> listEntries = new ArrayList<StructEntry>();
      int entryIndex = findStructureIndex((int)offset);
      if (entryIndex >= 0) {
        // collecting matching entries
        for (int idx = entryIndex; idx < getCachedList().size(); idx++) {
          if (getCachedList().get(idx).getOffset() >= offset+length) {
            break;
          }
          listEntries.add(getCachedList().get(idx));
        }

        // creating byte array to return
        byte[] retVal = new byte[length+extraLength];

        // constructing byte array
        int startOffset = listEntries.get(0).getOffset();
        StructEntry entry = listEntries.get(listEntries.size()-1);
        int fullSize = entry.getOffset()+entry.getSize() - startOffset;
        ByteArrayOutputStream os = new ByteArrayOutputStream(fullSize);
        int curOfs = startOffset;
        for (int idx = 0; idx < listEntries.size(); idx++) {
          entry = listEntries.get(idx);

          // sanity check
          if (entry.getOffset() >= offset+length) {
            continue;
          }

          // filling holes with empty data
          while (curOfs < entry.getOffset()) {
            os.write(0);
            curOfs++;
          }

          // writing actual data
          try {
            entry.write(os);
          } catch (IOException e) {
            e.printStackTrace();
          }
          curOfs += entry.getSize();
        }

        // preparing byte array for output
        try {
          System.arraycopy(os.toByteArray(), (int)offset - startOffset, retVal, 0, length);
        } catch (ArrayIndexOutOfBoundsException e) {
          e.printStackTrace();
        }

        return retVal;
      }
    }
    return null;
  }

  @Override
  public int getDataLength()
  {
    if (dataSize < 0) {
      reset();
    }
    return dataSize;
  }

  @Override
  public boolean hasData(long start, int length)
  {
    return (start >= 0 && start+length <= getDataLength());
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
    if (offset >= 0 && offset < getStruct().getSize() && data != null && data.length > 0) {
      boolean hasChanged = false;

      int length = data.length;
      if (offset+length > getStruct().getSize()) {
        length = getStruct().getSize() - (int)offset;
      }

      if (length > 0) {
        ArrayList<StructEntry> listEntries = new ArrayList<StructEntry>();
        int entryIndex = findStructureIndex((int)offset);
        // collecting matching entries
        if (entryIndex >= 0) {
          int maxEntrySize = 0;   // max. possible size of a single entry
          // collecting matching entries
          for (int idx = entryIndex; idx < getCachedList().size(); idx++) {
            if (getCachedList().get(idx).getOffset() >= offset+length) {
              break;
            }
            maxEntrySize = Math.max(maxEntrySize, getCachedList().get(idx).getSize());
            listEntries.add(getCachedList().get(idx));
          }

          // we need an output stream to initially load original data from the structure because
          // of the possibility to write only partial data into a structure.
          ByteArrayOutputStream os = new ByteArrayOutputStream(maxEntrySize);
          for (int idx = 0; idx < listEntries.size(); idx++) {
            StructEntry entry = listEntries.get(idx);
            os.reset();
            try {
              // pre-initializing byte array with original data
              entry.write(os);

              // writing new data into byte array
              byte[] buffer = os.toByteArray();
              int srcOfs = Math.max(0, entry.getOffset() - (int)offset);
              int dstOfs = Math.max(0, (int)offset - entry.getOffset());
              int len = Math.min((int)offset+length, entry.getOffset()+entry.getSize())
                        - entry.getOffset() - dstOfs;
              if (len > 0) {
                System.arraycopy(data, srcOfs, buffer, dstOfs, len);

                // loading data into the structure
                entry.read(buffer, 0);
                hasChanged = true;
              }
            } catch (IOException ioe) {
              ioe.printStackTrace();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }

      if (hasChanged) {
        fireDataChanged();
      }
    }
  }

//--------------------- End Interface IDataProvider ---------------------

  /** Re-initializes data cache. */
  public void reset()
  {
    if (listStructures != null) {
      listStructures.clear();
      listStructures = null;
      dataSize = -1;
    }
    listStructures = getStruct().getFlatList();
    dataSize = 0;
    for (final StructEntry e: listStructures) {
      dataSize = Math.max(dataSize, e.getOffset()+e.getSize());
    }
  }

  /** Returns the attached AbstractStruct instance. */
  public AbstractStruct getStruct()
  {
    return struct;
  }

  protected void fireDataChanged()
  {
    if (!listeners.isEmpty()) {
      DataChangedEvent event = new DataChangedEvent(this);
      for (int i = listeners.size()-1; i >= 0; i--) {
        listeners.get(i).dataChanged(event);
      }
    }
  }

  // Returns the list of cached top-level StructEntry objects
  private List<StructEntry> getCachedList()
  {
    if (listStructures == null) {
      reset();
    }
    return listStructures;
  }

  // Returns the list index of the StructEntry containing the specified offset. Returns -1 on failure.
  private int findStructureIndex(int offset)
  {
    if (!getCachedList().isEmpty()) {
      int baseIndex = 0;
      int curIndex = Math.max(baseIndex, Math.min(getCachedList().size()-1, getCachedList().size()/2));
      int diff = curIndex;
      while (diff > 0) {
        StructEntry entry = getCachedList().get(curIndex);
        if (offset >= entry.getOffset() && offset < entry.getOffset()+entry.getSize()) {
          break;
        }
        if (offset < entry.getOffset()) {
          // seek before current entry
          int newIndex = Math.max(baseIndex, Math.min(curIndex-1, (curIndex-baseIndex)/2));
          diff = curIndex - newIndex;
          curIndex = newIndex;
        } else {
          // seek after current entry
          baseIndex = curIndex+1;
          int newIndex = Math.max(baseIndex, Math.min(getCachedList().size()-1, baseIndex + (curIndex-baseIndex)/2));
          diff = newIndex - curIndex;
          curIndex = newIndex;
        }
      }
      return curIndex;
    }
    return -1;
  }
}
