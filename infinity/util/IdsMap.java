// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import infinity.resource.key.ResourceEntry;
import infinity.resource.other.PlainTextResource;

import javax.swing.*;
import java.util.*;

public final class IdsMap
{
  private final List<IdsMapEntry> overflow = new ArrayList<IdsMapEntry>();
  private final LongIntegerHashMap<IdsMapEntry> idEntryMap = new LongIntegerHashMap<IdsMapEntry>();
  private final ResourceEntry entry;
  private Map<String, IdsMapEntry> stringEntryMap;

  IdsMap(ResourceEntry entry)
  {
    this.entry = entry;
    StringTokenizer st;
    try {
      st = new StringTokenizer(new PlainTextResource(entry).getText(), "\n");
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    if (entry.getExtension().equalsIgnoreCase("IDS")) {
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        try {
          extractIDS(token);
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(null, "Error interpreting " + entry + ": " + token,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
      if (entry.toString().equalsIgnoreCase("TRIGGER.IDS")
          || entry.toString().equalsIgnoreCase("ACTION.IDS")
          || entry.toString().equalsIgnoreCase("OBJECT.IDS")) {
        stringEntryMap = new HashMap<String, IdsMapEntry>();
        for (final Object newVar : idEntryMap.values()) {
          IdsMapEntry idsEntry = (IdsMapEntry)newVar;
          stringEntryMap.put(idsEntry.getString().toUpperCase(), idsEntry);
        }
        for (int i = 0; i < overflow.size(); i++) {
          IdsMapEntry idsEntry = overflow.get(i);
          if (!stringEntryMap.containsKey(idsEntry.getString().toUpperCase()))
            stringEntryMap.put(idsEntry.getString().toUpperCase(), idsEntry);
        }
      }
    }
    else if (entry.getExtension().equalsIgnoreCase("2DA")) {
      // 3 uninteresting lines
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        try {
          extract2DA(token);
        } catch (NumberFormatException e) {
          JOptionPane.showMessageDialog(null, "Error interpreting " + entry + ": " + token,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

  public String toString()
  {
    if (entry == null)
      return "NULL";
    return entry.toString();
  }

  public List getAllValues()
  {
    List<IdsMapEntry> list = new ArrayList<IdsMapEntry>(idEntryMap.values());
    list.addAll(overflow);
    return list;
  }

  public LongIntegerHashMap<IdsMapEntry> getMap()
  {
    return idEntryMap;
  }

  public IdsMapEntry getOverflowValue(long value)
  {
    if (value < 0)
      value += 4294967296L;
    for (int i = 0; i < overflow.size(); i++) {
      IdsMapEntry idsEntry = overflow.get(i);
      if (idsEntry.getID() == value)
        return idsEntry;
    }
    return null;
  }

  public IdsMapEntry getValue(long value)
  {
    if (value < 0)
      value += 4294967296L;
    return idEntryMap.get(value);
//    return new IdsMapEntry(value, String.valueOf(value), null);
  }

  public IdsMapEntry lookup(String entry) // ToDo: Pretty much all compile time is spent here
  {
    if (entry.length() == 0 || entry.equals("0"))
      return null;
    if (stringEntryMap != null) {
      IdsMapEntry idsEntry = stringEntryMap.get(entry.toUpperCase());
      if (idsEntry != null)
        return idsEntry;
    }
    for (final Object newVar : idEntryMap.values()) {
      IdsMapEntry idsEntry = (IdsMapEntry)newVar;
      if (idsEntry.getString().equalsIgnoreCase(entry))
        return idsEntry;
    }
    for (int i = 0; i < overflow.size(); i++) {
      IdsMapEntry idsEntry = overflow.get(i);
      if (idsEntry.getString().equalsIgnoreCase(entry))
        return idsEntry;
    }
    return null;
  }

  public String lookupID(String entry)
  {
    IdsMapEntry idsEntry = lookup(entry);
    if (idsEntry == null)
      return null;
    long l_value = idsEntry.getID();
    if (l_value >= 2147483648L)
      l_value -= 4294967296L;
    return String.valueOf(l_value);
  }

  private void extract2DA(String line)
  {
    StringTokenizer st = new StringTokenizer(line);
    long id = Long.parseLong(st.nextToken());
    String resource = st.nextToken();
    while (st.hasMoreTokens())
      resource = st.nextToken();
    if (!idEntryMap.containsKey(id))
      idEntryMap.put(id, new IdsMapEntry(id, resource, null));
    else
      overflow.add(new IdsMapEntry(id, resource, null));
  }

  private void extractIDS(String line)
  {
    line = line.trim();
    line = line.replace('\t', ' ');
    int i = line.indexOf((int)' ');
    if (i == -1)
      return;
    String id = line.substring(0, i);
    String val = line.substring(i).trim();
    if (val.equals("") || id.equalsIgnoreCase("IDS"))
      return;

    long iid;
    if (id.length() > 2 && id.substring(0, 2).equalsIgnoreCase("0x"))  // Hex
      iid = Long.parseLong(id.substring(2), 16);
    else  // Dec
      iid = Long.parseLong(id);

    i = val.indexOf("//");
    if (i != -1)
      val = val.substring(0, i - 1);

    String param = null;
    i = val.indexOf((int)'(');
    if ((entry.toString().equalsIgnoreCase("ACTION.IDS") ||
         entry.toString().equalsIgnoreCase("TRIGGER.IDS")) && i != -1) {
      int j = val.indexOf((int)')', i + 1);
      if (j != -1) {
        param = val.substring(i + 1, j);
        val = val.substring(0, i + 1);
      }
    }
    val = val.trim();
    //    if (iid.longValue() < 0)
    //      iid = new Long(iid.longValue() + 4294967296l);
    if (!idEntryMap.containsKey(iid))
      idEntryMap.put(iid, new IdsMapEntry(iid, val, param));
    else
      overflow.add(new IdsMapEntry(iid, val, param));
  }
}

