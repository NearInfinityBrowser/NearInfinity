// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasAddRemovable;
import org.infinity.resource.StructEntry;

public final class StructClipboard
{
  public static final int CLIPBOARD_EMPTY = 0;
  public static final int CLIPBOARD_VALUES = 1;
  public static final int CLIPBOARD_ENTRIES = 2;
  private static StructClipboard clip;
  private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
  private final List<StructEntry> contents = new ArrayList<StructEntry>();
  private Class<? extends StructEntry> contentsClass;
  private boolean hasValues = true;

  public static synchronized StructClipboard getInstance()
  {
    if (clip == null)
      clip = new StructClipboard();
    return clip;
  }

  private static void pasteSubStructures(AbstractStruct targetStruct, List<? extends StructEntry> substructures)
  {
    for (int i = 0; i < substructures.size(); i++) {
      AddRemovable pasteEntry = (AddRemovable)substructures.get(i);
      if (pasteEntry instanceof HasAddRemovable) {
        AbstractStruct pasteStruct = (AbstractStruct)pasteEntry;
        List<AddRemovable> subsubstructures = pasteStruct.removeAllRemoveables();
        targetStruct.addDatatype(pasteEntry);
        pasteSubStructures(pasteStruct, subsubstructures);
      }
      else
        targetStruct.addDatatype(pasteEntry);
    }
  }

  private StructClipboard()
  {
  }

  @Override
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < contents.size(); i++) {
      StructEntry datatype = contents.get(i);
      sb.append(datatype.getName()).append(": ").append(datatype.toString()).append('\n');
    }
    return sb.toString();
  }

  public void addChangeListener(ChangeListener listener)
  {
    listeners.add(listener);
  }

  public void clear()
  {
    contents.clear();
    contentsClass = null;
    fireStateChanged();
  }

  public void copy(AbstractStruct struct, int firstIndex, int lastIndex)
  {
    contents.clear();
    contentsClass = struct.getClass();
    try {
      for (int i = firstIndex; i <= lastIndex; i++) {
        StructEntry entry = struct.getField(i);
        contents.add((StructEntry)entry.clone());
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    hasValues = false;
    fireStateChanged();
  }

  public void copyValue(AbstractStruct struct, int firstIndex, int lastIndex)
  {
    contents.clear();
    hasValues = true;
    contentsClass = struct.getClass();
    try {
      for (int i = firstIndex; i <= lastIndex; i++) {
        StructEntry entry = struct.getField(i);
        contents.add((StructEntry)entry.clone());
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    fireStateChanged();
  }

  public void cut(AbstractStruct struct, int firstIndex, int lastIndex)
  {
    contents.clear();
    contentsClass = struct.getClass();
    try {
      for (int i = firstIndex; i <= lastIndex; i++) {
        StructEntry entry = struct.getField(i);
        contents.add((StructEntry)entry.clone());
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    hasValues = false;
    for (int i = firstIndex; i <= lastIndex; i++)
      struct.removeDatatype((AddRemovable)struct.getField(firstIndex), true);
    fireStateChanged();
  }

  public int getContentType(AbstractStruct struct)
  {
    if (contents.isEmpty())
      return CLIPBOARD_EMPTY;
    if (hasValues) {
      if (struct.getClass().equals(contentsClass))
        return CLIPBOARD_VALUES;
      else
        return CLIPBOARD_EMPTY;
    }
    else {
      if (struct.getClass().equals(contentsClass))
        return CLIPBOARD_ENTRIES;
      AddRemovable[] targetClasses;
      try {
        targetClasses = ((HasAddRemovable)struct).getAddRemovables();
      } catch (Exception e) {
        return CLIPBOARD_EMPTY;
      }
      if (targetClasses == null)
        targetClasses = new AddRemovable[0];
      for (int i = 0; i < contents.size(); i++) {
        Class<? extends StructEntry> c = contents.get(i).getClass();
        boolean found = false;
        for (final AddRemovable targetClass : targetClasses) {
          if (targetClass != null && c.equals(targetClass.getClass()))
            found = true;
        }
        if (!found)
          return CLIPBOARD_EMPTY;
      }
      return CLIPBOARD_ENTRIES;
    }
  }

  public int paste(AbstractStruct targetStruct)
  {
    int lastIndex = 0;
    try {
      for (int i = 0; i < contents.size(); i++) {
        AddRemovable pasteEntry = (AddRemovable)contents.get(i).clone();
        if (pasteEntry instanceof HasAddRemovable) {
          ((HasAddRemovable)pasteEntry).getAddRemovables();
          List<AddRemovable> substructures = ((AbstractStruct)pasteEntry).removeAllRemoveables();
          lastIndex = targetStruct.addDatatype(pasteEntry);
          pasteSubStructures((AbstractStruct)pasteEntry, substructures);
        }
        else
          lastIndex = targetStruct.addDatatype(pasteEntry);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return lastIndex;
  }

  public int pasteValue(AbstractStruct struct, int index)
  {
    for (int i = 0; i < contents.size(); i++) {
      StructEntry oldEntry = struct.getField(index + i);
      StructEntry newEntry = contents.get(i);
      if (oldEntry.getClass() != newEntry.getClass() ||
          oldEntry.getSize() != newEntry.getSize())
        return 0;
    }
    try {
      for (int i = 0; i < contents.size(); i++) {
        StructEntry oldEntry = struct.getField(index + i);
        StructEntry newEntry = (StructEntry)contents.get(i).clone();
        newEntry.copyNameAndOffset(oldEntry);
        struct.setListEntry(index + i, newEntry);
      }
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return 0;
    }
    return contents.size();
  }

  public void removeChangeListener(ChangeListener listener)
  {
    listeners.remove(listener);
  }

  private void fireStateChanged()
  {
    ChangeEvent event = new ChangeEvent(this);
    for (int i = 0; i < listeners.size(); i++)
      listeners.get(i).stateChanged(event);
  }
}

