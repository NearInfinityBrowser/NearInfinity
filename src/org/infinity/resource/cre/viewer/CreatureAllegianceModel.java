// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

/**
 * {@code ComboBoxModel} for the creature allegiance combo box used in the Creature Animation Viewer.
 */
public class CreatureAllegianceModel extends AbstractListModel<CreatureAllegianceModel.AllegianceEntry>
    implements ComboBoxModel<CreatureAllegianceModel.AllegianceEntry>
{
  private final List<AllegianceEntry> eaList = new ArrayList<>();

  private Object selectedItem;

  public CreatureAllegianceModel()
  {
    super();
    init();
  }

  public void reload()
  {
    init();
  }

  /**
   * Returns the index-position of the specified object in the list.
   * @param anItem a {@code AllegianceEntry} object, {@code Number} object or {@code String} specifying an allegiance name.
   * @return an int representing the index position, where 0 is the first position. Returns -1
   *         if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof AllegianceEntry) {
      return eaList.indexOf(anItem);
    } else if (anItem instanceof Number) {
      final int eaValue = ((Number)anItem).intValue();
      return IntStream
          .range(0, eaList.size())
          .filter(i -> eaList.get(i).value == eaValue)
          .findAny()
          .orElse(-1);
    } else if (anItem != null) {
      final String eaName = anItem.toString().trim();
      return IntStream
          .range(0, eaList.size())
          .filter(i -> eaName.equalsIgnoreCase(eaList.get(i).name))
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!eaList.isEmpty()) {
      int oldSize = eaList.size();
      eaList.clear();
      selectedItem = null;
      if (oldSize > 0) {
        fireIntervalRemoved(this, 0, oldSize - 1);
      }
    } else {
      selectedItem = null;
    }
  }

//--------------------- Begin Interface ListModel ---------------------

  @Override
  public int getSize()
  {
    return eaList.size();
  }

  @Override
  public AllegianceEntry getElementAt(int index)
  {
    if (index >= 0 && index < eaList.size()) {
      return eaList.get(index);
    } else {
      return null;
    }
  }

//--------------------- End Interface ListModel ---------------------

//--------------------- Begin Interface ComboBoxModel ---------------------

  @Override
  public void setSelectedItem(Object anItem)
  {
    if ((selectedItem != null && !selectedItem.equals(anItem)) ||
        selectedItem == null && anItem != null) {
      selectedItem = anItem;
      fireContentsChanged(this, -1, -1);
    }
  }

  @Override
  public Object getSelectedItem()
  {
    return selectedItem;
  }

//--------------------- End Interface ComboBoxModel ---------------------

  private void init()
  {
    removeAllElements();

    IdsMap map = IdsMapCache.get("EA.IDS");
    if (map != null) {
      for (Iterator<IdsMapEntry> iter = map.getAllValues().iterator(); iter.hasNext(); ) {
        IdsMapEntry entry = iter.next();
        eaList.add(new AllegianceEntry(entry));
      }
      Collections.sort(eaList);
    }
    if (!eaList.isEmpty()) {
      fireIntervalAdded(this, 0, eaList.size() - 1);
    }

    setSelectedItem(getElementAt(0));
  }

//-------------------------- INNER CLASSES --------------------------

  public static class AllegianceEntry implements Comparable<AllegianceEntry>
  {
    private final int value;
    private final String name;

    public AllegianceEntry(IdsMapEntry eaEntry)
    {
      this.value = (int)Objects.requireNonNull(eaEntry).getID();
      this.name = eaEntry.getSymbol();
    }

    public int getValue() { return value; }

    public String getName() { return name; }

    @Override
    public String toString()
    {
      return value + " - " + name;
    }

    @Override
    public int compareTo(AllegianceEntry o)
    {
      return value - o.value;
    }
  }
}
