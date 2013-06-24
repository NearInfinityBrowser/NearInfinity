// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;
import infinity.gui.BrowserMenuBar;
import infinity.gui.StructViewer;
import infinity.resource.are.Actor;
import infinity.resource.cre.CreResource;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.gam.GamResource;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.io.*;
import java.util.*;

public abstract class AbstractStruct extends AbstractTableModel implements StructEntry, Viewable, Closeable
{
  private static final boolean CONSISTENCY_CHECK = false;
  private static final boolean DEBUG_MESSAGES = false;
  protected List<StructEntry> list;
  private AbstractStruct superStruct;
  private Map<Class, SectionCount> countmap;
  private Map<Class, SectionOffset> offsetmap;
  private ResourceEntry entry;
  private String name;
  private StructViewer viewer;
  private boolean structChanged;
  private int startoffset, endoffset, extraoffset;

  private static void adjustEntryOffsets(AbstractStruct superStruct, AbstractStruct modifiedStruct,
                                         AddRemovable datatype, int amount)
  {
    for (int i = 0; i < superStruct.list.size(); i++) {
      StructEntry structEntry = superStruct.list.get(i);
      if (structEntry.getOffset() > datatype.getOffset() ||
          structEntry.getOffset() == datatype.getOffset() && structEntry != datatype &&
          structEntry != modifiedStruct) {
        structEntry.setOffset(structEntry.getOffset() + amount);
        if (DEBUG_MESSAGES && superStruct.getSuperStruct() == null && structEntry instanceof AbstractStruct)
          System.out.println("Adjusting " + structEntry.getName() + " by " + amount);
      }
      if (structEntry instanceof AbstractStruct)
        adjustEntryOffsets((AbstractStruct)structEntry, modifiedStruct, datatype, amount);
    }
  }

  private static void adjustSectionOffsets(AbstractStruct superStruct, AddRemovable datatype, int amount)
  {
    for (int i = 0; i < superStruct.list.size(); i++) {
      Object o = superStruct.list.get(i);
      if (o instanceof SectionOffset) {
        SectionOffset sOffset = (SectionOffset)o;
        if (sOffset.getValue() + superStruct.getExtraOffset() > datatype.getOffset()) {
          sOffset.incValue(amount);
          if (DEBUG_MESSAGES)
            System.out.println("Adjusting section offset " + sOffset.getName() + " by " + amount);
        }
        else if (sOffset.getValue() + superStruct.getExtraOffset() == datatype.getOffset()) {
          if (amount > 0 &&
              !(sOffset.getSection() == datatype.getClass() ||
                ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2 &&
                superStruct instanceof CreResource)) {
            sOffset.incValue(amount);
            if (DEBUG_MESSAGES)
              System.out.println("Adjusting section offset " + sOffset.getName() + " by " + amount);
          }
        }
      }
    }
  }

  private static void getStructsOfClass(AbstractStruct struct, Class cl, List<StructEntry> container)
  {
    for (int i = 0; i < struct.list.size(); i++) {
      if (struct.list.get(i).getClass() == cl)
        container.add(struct.list.get(i));
      else if (struct.list.get(i) instanceof AbstractStruct)
        getStructsOfClass((AbstractStruct)struct.list.get(i), cl, container);
    }
  }

  protected AbstractStruct()
  {
  }

  protected AbstractStruct(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    list = new ArrayList<StructEntry>();
    name = entry.toString();
    byte buffer[] = entry.getResourceData();
    endoffset = read(buffer, 0);
    if (this instanceof HasAddRemovable && list.size() > 0) {// Is this enough?
      Collections.sort(list); // This way we can writeField out in the order in list - sorted by offset
      fixHoles(buffer);
      initAddStructMaps();
    }
  }

  protected AbstractStruct(AbstractStruct superStruct, String name, int startoffset, int listSize)
  {
    this.superStruct = superStruct;
    this.name = name;
    this.startoffset = startoffset;
    list = new ArrayList<StructEntry>(listSize);
  }

  protected AbstractStruct(AbstractStruct superStruct, String name, byte buffer[], int startoffset)
          throws Exception
  {
    this(superStruct, name, buffer, startoffset, 10);
  }

  protected AbstractStruct(AbstractStruct superStruct, String name, byte buffer[], int startoffset,
                           int listSize) throws Exception
  {
    this(superStruct, name, startoffset, listSize);
    endoffset = read(buffer, startoffset);
    if (this instanceof HasAddRemovable) {
      if (!(this instanceof Actor))  // Is this enough?
        Collections.sort(list); // This way we can writeField out in the order in list - sorted by offset
      initAddStructMaps();
    }
  }

// --------------------- Begin Interface Closeable ---------------------

  // end - extends AbstractTableModel

  // begin - implements Closeable
  public void close() throws Exception
  {
    if (structChanged && viewer != null && this instanceof Resource && superStruct == null) {
      File output;
      if (entry instanceof BIFFResourceEntry)
        output =
        new File(ResourceFactory.getRootDir(),
                 ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
      else
        output = entry.getActualFile();
      String options[] = {"Save changes", "Discard changes", "Cancel"};
      int result = JOptionPane.showOptionDialog(viewer, "Save changes to " + output + '?', "Resource changed",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
      if (result == 0)
        ResourceFactory.getInstance().saveResource((Resource)this, viewer.getTopLevelAncestor());
      else if (result == 2)
        throw new Exception("Save aborted");
    }
    if (viewer != null)
      viewer.close();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Comparable ---------------------

  // begin - implements StructEntry
  public int compareTo(StructEntry o)
  {
    return getOffset() - o.getOffset();
  }

// --------------------- End Interface Comparable ---------------------


// --------------------- Begin Interface StructEntry ---------------------

  public Object clone() throws CloneNotSupportedException
  {
    AbstractStruct newstruct = (AbstractStruct)super.clone();
    newstruct.superStruct = null;
    newstruct.list = new ArrayList<StructEntry>(list.size());
    newstruct.viewer = null;
    for (int i = 0; i < list.size(); i++)
      newstruct.list.add((StructEntry)list.get(i).clone());
//    for (Iterator i = newstruct.list.iterator(); i.hasNext();) {
//      StructEntry sentry = (StructEntry)i.next();
//      if (sentry.getOffset() <= 0)
//        break;
//      sentry.setOffset(sentry.getOffset() - newstruct.getOffset());
//    }
    newstruct.initAddStructMaps();
    return newstruct;
  }

  public void copyNameAndOffset(StructEntry structEntry)
  {
    name = structEntry.getName();
    setOffset(structEntry.getOffset());
  }

  public String getName()
  {
    return name;
  }

  public int getOffset()
  {
    return startoffset;
  }

  public int getSize()
  {
    return endoffset - startoffset;
  }

  public void setOffset(int newoffset)
  {
    if (extraoffset != 0)
      extraoffset += newoffset - startoffset;
    int delta = getSize();
    startoffset = newoffset;
    endoffset = newoffset + delta;
  }

// --------------------- End Interface StructEntry ---------------------


// --------------------- Begin Interface TableModel ---------------------

  // start - extends AbstractTableModel
  public int getRowCount()
  {
    return list.size();
  }

  public int getColumnCount()
  {
    if (BrowserMenuBar.getInstance().showOffsets())
      return 3;
    return 2;
  }

  public Object getValueAt(int row, int column)
  {
    if (list.get(row) instanceof StructEntry) {
      StructEntry data = list.get(row);
      if (column == 0)
        return data.getName();
      if (column == 1)
        return data;
      return Integer.toHexString(data.getOffset()) + " h";
    }
    return "Unknown datatype";
  }

// --------------------- End Interface TableModel ---------------------


// --------------------- Begin Interface Viewable ---------------------

  // end - implements Closeable

  // begin - implements Viewable
  public JComponent makeViewer(ViewableContainer container)
  {
    if (viewer == null)
      viewer = new StructViewer(this);
    return viewer;
  }

// --------------------- End Interface Viewable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  // begin - implements Writeable
  public void write(OutputStream os) throws IOException
  {
    Collections.sort(list); // This way we can writeField out in the order in list - sorted by offset
    for (int i = 0; i < list.size(); i++)
      list.get(i).write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public String getColumnName(int columnIndex)
  {
    if (columnIndex == 0)
      return "Attribute";
    if (columnIndex == 1)
      return "Value";
    return "Offset";
  }

  public boolean isCellEditable(int row, int col)
  {
    if (col == 1) {
      Object o = getValueAt(row, col);
      if (o instanceof InlineEditable && !(o instanceof Editable))
        return true;
    }
    return false;
  }

  public void setValueAt(Object value, int row, int column)
  {
    Object o = getValueAt(row, column);
    if (o instanceof InlineEditable) {
      if (!((InlineEditable)o).update(value))
        JOptionPane.showMessageDialog(viewer, "Error updating value", "Error", JOptionPane.ERROR_MESSAGE);
      else {
        fireTableCellUpdated(row, column);
        setStructChanged(true);
      }
    }
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer(80);
    for (int i = 0; i < list.size() && sb.length() < 80; i++) { // < 80 to speed things up
      StructEntry datatype = list.get(i);
      sb.append(datatype.getName()).append(": ").append(datatype.toString()).append(',');
    }
    return sb.toString();
  }

  public int addDatatype(AddRemovable addedEntry)
  {
    int index = 0;
    // Find place to add
    if (viewer != null && viewer.getSelectedEntry() != null
        && viewer.getSelectedEntry().getClass() == addedEntry.getClass())
      index = viewer.getSelectedRow();
    else if (offsetmap.containsKey(addedEntry.getClass())) {
      int offset = offsetmap.get(addedEntry.getClass()).getValue() + extraoffset;
      while (index < list.size() && list.get(index).getOffset() < offset)
        index++;
      while (index < list.size() && addedEntry.getClass() == (list.get(index)).getClass())
        index++;
      if (index == 0) {
        SectionOffset soffset = offsetmap.get(addedEntry.getClass());
        if (soffset.getValue() == 0) {
          index = list.size();
          soffset.setValue(getSize());
        }
        else
          throw new IllegalArgumentException(
                  "addDatatype: No suitable index found - " + getName() + " adding " + addedEntry.getName());
      }
    }
    else
      index = getAddedPosition();

    return addDatatype(addedEntry, index);
  }

  public int addDatatype(AddRemovable addedEntry, int index)
  {
    // Increase count
    if (countmap.containsKey(addedEntry.getClass()))
      countmap.get(addedEntry.getClass()).incValue(1);

    // Set addedEntry offset
    if (index > 0 && list.get(index - 1).getClass() == addedEntry.getClass()) {
      StructEntry prev = list.get(index - 1);
      addedEntry.setOffset(prev.getOffset() + prev.getSize());
    }
    else if (offsetmap.containsKey(addedEntry.getClass())) {
      addedEntry.setOffset(offsetmap.get(addedEntry.getClass()).getValue() + extraoffset);
    }
    else if (index == 0 && list.size() > 0) {
      StructEntry next = list.get(0);
      addedEntry.setOffset(next.getOffset());
    }
    else {
      setAddRemovableOffset(addedEntry);
      for (int i = 0; i < list.size(); i++) {
        StructEntry structEntry = list.get(i);
        if (structEntry.getOffset() == addedEntry.getOffset()) {
          index = i;
          break;
        }
      }
    }
    if (DEBUG_MESSAGES)
      System.out.println(
              "Added " + addedEntry.getName() + " at " + Integer.toHexString(addedEntry.getOffset()));
    if (addedEntry instanceof AbstractStruct) {
      AbstractStruct addedStruct = (AbstractStruct)addedEntry;
      addedStruct.realignStructOffsets();
      addedStruct.superStruct = this;
    }
    AbstractStruct topStruct = this;
    while (topStruct.superStruct != null) {
      if (topStruct instanceof Resource) {
        topStruct.endoffset += addedEntry.getSize();
        adjustSectionOffsets(topStruct, addedEntry, addedEntry.getSize());
      }
      topStruct = topStruct.superStruct;
    }
    if (topStruct instanceof Resource)
      topStruct.endoffset += addedEntry.getSize();
    adjustEntryOffsets(topStruct, this, addedEntry, addedEntry.getSize());
    adjustSectionOffsets(topStruct, addedEntry, addedEntry.getSize());

    list.add(index, addedEntry);
    datatypeAdded(addedEntry);
    if (superStruct != null)
      superStruct.datatypeAddedInChild(this, addedEntry);
    setStructChanged(true);
    fireTableRowsInserted(index, index);
    if (CONSISTENCY_CHECK && topStruct instanceof Resource)
      topStruct.testStruct();
    return index;
  }

  public void addToList(StructEntry startFromEntry, List<StructEntry> toBeAdded)
  {
    list.addAll(1 + list.indexOf(startFromEntry), toBeAdded);
  }

  public StructEntry getAttribute(int offset)
  {
    List<StructEntry> flatList = getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      StructEntry structEntry = flatList.get(i);
      if (offset >= structEntry.getOffset() && offset < structEntry.getOffset() + structEntry.getSize())
        return structEntry;
    }
    return null;
  }

  public StructEntry getAttribute(String ename)
  {
    for (int i = 0; i < list.size(); i++) {
      StructEntry structEntry = list.get(i);
      if (structEntry.getName().equalsIgnoreCase(ename))
        return structEntry;
    }
    System.err.println("Could not find attribute " + ename + " in " + getName());
    return null;
  }

  public int getEndOffset()
  {
    return endoffset;
  }

  public int getExtraOffset()
  {
    return extraoffset;
  }

  public List<StructEntry> getList()
  {
    return list;
  }

  public List<StructEntry> getFlatList()
  {
    List<StructEntry> flatList = new ArrayList<StructEntry>(2 * list.size());
    addFlatList(flatList);
    Collections.sort(flatList);
    return flatList;
  }

  public int getIndexOf(StructEntry structEntry)
  {
    return list.indexOf(structEntry);
  }

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

  public StructEntry getStructEntryAt(int index)
  {
    return list.get(index);
  }

  public AbstractStruct getSuperStruct()
  {
    return superStruct;
  }

  public AbstractStruct getSuperStruct(StructEntry structEntry)
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o == structEntry)
        return this;
      if (o instanceof AbstractStruct) {
        AbstractStruct result = ((AbstractStruct)o).getSuperStruct(structEntry);
        if (result != null)
          return result;
      }
    }
    return null;
  }

  // end - implements Viewable

  public StructViewer getViewer()
  {
    return viewer;
  }

  public void realignStructOffsets()
  {
    int offset = startoffset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry structEntry = list.get(i);
      structEntry.setOffset(offset);
      offset += structEntry.getSize();
      if (structEntry instanceof AbstractStruct)
        ((AbstractStruct)structEntry).realignStructOffsets();
    }
  }

  public List<AddRemovable> removeAllRemoveables()
  {
    List<AddRemovable> removed = new ArrayList<AddRemovable>();
    for (int i = 0; i < list.size(); i++) {
      StructEntry o = list.get(i);
      if (o instanceof AddRemovable) {
        removeDatatype((AddRemovable)o, false);
        removed.add((AddRemovable)o);
        i--;
      }
    }
    return removed;
  }

  public void removeDatatype(AddRemovable removedEntry, boolean removeRecurse)
  {
    if (removeRecurse && removedEntry instanceof HasAddRemovable) { // Recusivly removeTableLine substructures first
      AbstractStruct removedStruct = (AbstractStruct)removedEntry;
      for (int i = 0; i < removedStruct.list.size(); i++) {
        Object o = removedStruct.list.get(i);
        if (o instanceof AddRemovable) {
          removedStruct.removeDatatype((AddRemovable)o, removeRecurse);
          i--;
        }
      }
    }
    int index = list.indexOf(removedEntry);
    list.remove(index);
    // decrease count
    if (countmap != null && countmap.containsKey(removedEntry.getClass()))
      countmap.get(removedEntry.getClass()).incValue(-1);
    // decrease offsets
    AbstractStruct topStruct = this;
    while (topStruct.superStruct != null) {
      if (topStruct instanceof Resource) {
        topStruct.endoffset -= removedEntry.getSize();
        adjustSectionOffsets(topStruct, removedEntry, -removedEntry.getSize());
      }
      topStruct = topStruct.superStruct;
    }
    if (topStruct instanceof Resource)
      topStruct.endoffset -= removedEntry.getSize();
    if (DEBUG_MESSAGES)
      System.out.println("Removing: " + removedEntry.getName());
    adjustEntryOffsets(topStruct, this, removedEntry, -removedEntry.getSize());
    adjustSectionOffsets(topStruct, removedEntry, -removedEntry.getSize());
    datatypeRemoved(removedEntry);
    if (superStruct != null)
      superStruct.datatypeRemovedInChild(this, removedEntry);
    fireTableRowsDeleted(index, index);
    setStructChanged(true);
    if (CONSISTENCY_CHECK && topStruct instanceof Resource)
      topStruct.testStruct();
  }

  public byte[] removeFromList(StructEntry startFromEntry, int numBytes) throws IOException
  {
    int startindex = list.indexOf(startFromEntry) + 1;
    int endindex = startindex;
    int len = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (len < numBytes) {
      StructEntry e = list.get(endindex++);
      len += e.getSize();
      e.write(baos);
    }
    for (int i = endindex - 1; i >= startindex; i--)
      list.remove(i);
    return baos.toByteArray();
  }

  public void setListEntry(int index, StructEntry structEntry)
  {
    list.set(index, structEntry);
    fireTableRowsUpdated(index, index);
  }

  public void setStructChanged(boolean changed)
  {
    structChanged = changed;
    if (superStruct != null)
      superStruct.setStructChanged(changed);
  }

  public void testStruct()
  {
    StringBuffer sb = new StringBuffer(1000);
    sb.append("Testing: ").append(getName()).append('\n');

    sb.append("1: Testing flatList:\n");
    List<StructEntry> flatList = getFlatList();
    StructEntry firstEntry = flatList.get(0);
    for (int i = 1; i < flatList.size(); i++) {
      StructEntry secondEntry = flatList.get(i);
      if (firstEntry.getOffset() + firstEntry.getSize() != secondEntry.getOffset()) {
        sb.append("ERR-> ").append(firstEntry.getName()).append(' ');
        sb.append(Integer.toHexString(firstEntry.getOffset())).append("h + ");
        sb.append(Integer.toHexString(firstEntry.getSize())).append("h = ");
        sb.append(Integer.toHexString(firstEntry.getOffset() + firstEntry.getSize())).append("h != ");
        sb.append(secondEntry.getName()).append(' ').append(Integer.toHexString(secondEntry.getOffset())).append(
                "h\n");
      }
      firstEntry = secondEntry;
    }

    if (this instanceof HasAddRemovable) {
      List<StructEntry> temp = new ArrayList<StructEntry>();
      sb.append("2: Testing SectionCounts:\n");
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) instanceof SectionCount) {
          SectionCount sectionCount = (SectionCount)list.get(i);
          temp.clear();
          if (!(sectionCount.getSection() == Unknown.class))
            getStructsOfClass(this, sectionCount.getSection(), temp);
          if (sectionCount.getValue() != temp.size()) {
            sb.append("ERR-> ").append(sectionCount.getName()).append(' ').append(sectionCount.getValue()).append(
                    " != ");
            sb.append("Actual count: ").append(temp.size()).append('\n');
          }
        }
      }

      sb.append("3: Testing SectionOffsets:\n");
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) instanceof SectionOffset) {
          SectionOffset sectionOffset = (SectionOffset)list.get(i);
          temp.clear();
          if (!(sectionOffset.getSection() == Unknown.class))
            getStructsOfClass(this, sectionOffset.getSection(), temp);
          if (temp.size() > 0) {
            Collections.sort(temp);
            StructEntry se = temp.get(0);
            if (sectionOffset.getValue() + getExtraOffset() != se.getOffset()) {
              sb.append("ERR-> ").append(sectionOffset.getName()).append(' ').append(
                      sectionOffset.toString()).append(" + ");
              sb.append(Integer.toHexString(getExtraOffset())).append("h != ");
              sb.append(se.getName()).append(" @ ").append(Integer.toHexString(se.getOffset())).append("h\n");
            }
          }
        }
      }
    }
    if (this instanceof GamResource) {
      sb.append("4: Testing embedded CREs:\n");
      List<StructEntry> temp = new ArrayList<StructEntry>();
      getStructsOfClass(this, CreResource.class, temp);
      for (int i = 0; i < temp.size(); i++)
        ((CreResource)temp.get(i)).testStruct();
    }

    String s = sb.toString();
    if (s.indexOf("ERR->") != -1)
      System.err.println(s);
  }

  public String toMultiLineString()
  {
    StringBuffer sb = new StringBuffer(30 * list.size());
    for (int i = 0; i < list.size(); i++) {
      StructEntry datatype = list.get(i);
      sb.append(datatype.getName()).append(": ").append(datatype.toString()).append('\n');
    }
    return sb.toString();
  }

  private void addFlatList(List<StructEntry> flatList)
  {
    for (int i = 0; i < list.size(); i++) {
      StructEntry o = list.get(i);
      if (o instanceof AbstractStruct)
        ((AbstractStruct)o).addFlatList(flatList);
      else if (o instanceof AbstractCode)
        ((AbstractCode)o).addFlatList(flatList);
      else
        flatList.add(o);
    }
  }

  // To be overriden by subclasses
  protected void datatypeAdded(AddRemovable datatype)
  {
  }

  // To be overriden by subclasses
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (superStruct != null)
      superStruct.datatypeAddedInChild(child, datatype);
  }

  // To be overriden by subclasses
  protected void datatypeRemoved(AddRemovable datatype)
  {
  }

  // To be overriden by subclasses
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    if (superStruct != null)
      superStruct.datatypeRemovedInChild(child, datatype);
  }

  private void fixHoles(byte buffer[])
  {
    int offset = startoffset;
    List<StructEntry> flatList = getFlatList();
    for (int i = 0; i < flatList.size(); i++) {
      StructEntry se = flatList.get(i);
      int delta = se.getOffset() - offset;
      if (delta > 0) {
        Unknown hole = new Unknown(buffer, offset, delta, "Unused bytes?");
        list.add(hole);
        flatList.add(i, hole);
        System.out.println("Hole: " + name + " off: " + Integer.toHexString(offset) + "h len: " + delta);
        i++;
      }
      offset = se.getOffset() + se.getSize();
    }
    if (endoffset < buffer.length) { // Does this break anything?
      list.add(new Unknown(buffer, endoffset, buffer.length - endoffset, "Unused bytes?"));
      System.out.println("Hole: " + name + " off: " + Integer.toHexString(offset) + "h len: " +
                         (buffer.length - endoffset));
      endoffset = buffer.length;
    }
  }

  // To be overriden by subclasses
  protected int getAddedPosition()
  {
    return list.size(); // Default: Add at end
  }

  private void initAddStructMaps()
  {
    countmap = new HashMap<Class, SectionCount>();
    offsetmap = new HashMap<Class, SectionOffset>();
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof SectionOffset) {
        SectionOffset so = (SectionOffset)o;
        if (so.getSection() != null) {
          offsetmap.put(so.getSection(), so);
        }
      }
      else if (o instanceof SectionCount)
        countmap.put(((SectionCount)o).getSection(), (SectionCount)o);
    }
  }

  // To be overriden by subclasses
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
  }

  // end - implements StructEntry

  protected void setExtraOffset(int offset)
  {
    extraoffset = offset;
  }

  protected void setStartOffset(int offset)
  {
    startoffset = offset;
  }

  // end - implements Writeable

  protected void writeFlatList(OutputStream os) throws IOException
  {
    List<StructEntry> flatList = getFlatList();
    for (int i = 0; i < flatList.size(); i++)
      flatList.get(i).write(os);
  }

  protected abstract int read(byte buffer[], int startoffset) throws Exception;
}

