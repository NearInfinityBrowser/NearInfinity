// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

/**
 * {@code ComboBoxModel} for the creature animation combo box used in the Creature Animation Browser.
 */
public class CreatureAnimationModel extends AbstractListModel<CreatureAnimationModel.AnimateEntry>
    implements MutableComboBoxModel<CreatureAnimationModel.AnimateEntry>
{
  private final List<AnimateEntry> animationList = new ArrayList<>(65536);

  private Object selectedItem;

  public CreatureAnimationModel()
  {
    super();
    init();
  }

  public void reload()
  {
    init();
  }

  /**
   * Returns the numeric value associated with the selected item.
   * It returns the animation id for a regular entry from the model.
   * It attempts to detect and return a manually entered number. Use prefix "0x" or suffix "h" to indicate that
   * number is entered in hexadecimal notation.
   * @return the numeric value of the selected item. Returns -1 if numeric value could not be determined.
   */
  public int getSelectedValue()
  {
    return parseValue(getSelectedItem());
  }

  /**
   * Returns the index-position of the specified object in the list.
   * @param anItem a {@code AnimateEntry} object, {@code Number} object or {@code String} object.
   * @return an int representing the index position, where 0 is the first position. Returns -1
   *         if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof AnimateEntry) {
      return animationList.indexOf(anItem);
    } else {
      int idx = -1;
      if (anItem instanceof Number) {
        idx = ((Number)anItem).intValue();
      } else {
        idx = parseValue(anItem);
      }
      if (idx != -1) {
        final int aidx = idx;
        return IntStream
            .range(0,  animationList.size())
            .filter(i -> aidx == animationList.get(i).getValue())
            .findAny()
            .orElse(-1);
      }
    }

    if (anItem != null) {
      final String aname = anItem.toString().toUpperCase(Locale.ENGLISH);
      return IntStream
          .range(0,  animationList.size())
          .filter(i -> animationList.get(i).getSymbol().toUpperCase(Locale.ENGLISH).startsWith(aname))
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!animationList.isEmpty()) {
      int oldSize = animationList.size();
      animationList.clear();
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
    return animationList.size();
  }


  @Override
  public AnimateEntry getElementAt(int index)
  {
    if (index >= 0 && index < animationList.size()) {
      return animationList.get(index);
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

//--------------------- Begin Interface MutableComboBoxModel ---------------------

  @Override
  public void addElement(AnimateEntry item)
  {
    animationList.add(Objects.requireNonNull(item));
    fireIntervalAdded(this, animationList.size() - 1, animationList.size() - 1);
    if (animationList.size() == 1 && selectedItem == null) {
      setSelectedItem(item);
    }
  }


  @Override
  public void removeElement(Object obj)
  {
    int index = animationList.indexOf(obj);
    if (index != -1) {
      removeElementAt(index);
    }
  }


  @Override
  public void insertElementAt(AnimateEntry item, int index)
  {
    animationList.add(index, Objects.requireNonNull(item));
    fireIntervalAdded(this, index, index);
  }


  @Override
  public void removeElementAt(int index)
  {
    if (getElementAt(index) == selectedItem) {
      if (index == 0) {
        setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
      } else {
        setSelectedItem(getElementAt(index - 1));
      }
    }
    animationList.remove(index);
    fireIntervalRemoved(this, index, index);
  }

//--------------------- End Interface MutableComboBoxModel ---------------------

  /**
   * Helper method: Attempts to convert the specified argument into a meaningful numeric value.
   * An {@code AnimateEntry} object returns the associted animation id.
   * A string is parsed and returns as decimal or hexadecimal number.
   * Returns -1 if argument could not be converted.
   */
  public int parseValue(Object o)
  {
    int retVal = -1;
    if (o instanceof AnimateEntry) {
      retVal = ((AnimateEntry)o).getValue();
    } else if (o instanceof Number) {
      retVal = ((Number)o).intValue();
    } else if (o != null) {
      String s = o.toString().trim().toLowerCase().replaceAll("\\s+", "");
      try {
        // preserve sign
        int factor = 1;
        if (s.startsWith("-")) {
          factor = -1;
          s = s.substring(1);
        }

        // determine base
        int base = 10;
        if (s.startsWith("0x")) {
          base = 16;
          s = s.substring(2);
        }
        if (s.endsWith("h")) {
          base = 16;
          s = s.substring(0, s.length() - 1);
        }

        retVal = Integer.parseInt(s, base) * factor;
      } catch (Exception e) {
      }
    }
    return retVal;
  }

  private void init()
  {
    removeAllElements();

    IdsMap map = IdsMapCache.get("ANIMATE.IDS");
    if (map != null) {
      for (Iterator<IdsMapEntry> iter = map.getAllValues().iterator(); iter.hasNext(); ) {
        IdsMapEntry entry = iter.next();
        animationList.add(new AnimateEntry((int)entry.getID(), entry.getSymbol()));
      }
    }
    Collections.sort(animationList);
    if (!animationList.isEmpty()) {
      fireIntervalAdded(this, 0, animationList.size() - 1);
    }

    setSelectedItem(getElementAt(0));
  }

//-------------------------- INNER CLASSES --------------------------

  public static class AnimateEntry implements Comparable<AnimateEntry>
  {
    private final int id;
    private final String name;

    public AnimateEntry(int animId, String name)
    {
      this.id = Math.max(0, Math.min(0xffff, animId));
      this.name = (name != null) ? name : "";
    }

    /** Returns the numeric animation id. */
    public int getValue() { return id; }

    /** Returns the symbolic animation id if available. Returns empty string otherwise. */
    public String getSymbol()
    {
      if (name.isEmpty()) {
        String idString = Integer.toString(getValue(), 16).toUpperCase(Locale.ENGLISH);
        switch (idString.length()) {
          case 1:
            idString = "000" + idString;
            break;
          case 2:
            idString = "00" + idString;
            break;
          case 3:
            idString = "0" + idString;
            break;
          default:
        }
        return "UNKNOWN_" + idString;
      } else {
        return name;
      }
    }

    @Override
    public int compareTo(AnimateEntry o)
    {
      // sort by symbolic name
      return toString().compareTo(o.toString());
    }

    @Override
    public String toString()
    {
      String idString = Integer.toString(getValue(), 16).toUpperCase(Locale.ENGLISH);
      switch (idString.length()) {
        case 1:
          idString = "0x000" + idString;
          break;
        case 2:
          idString = "0x00" + idString;
          break;
        case 3:
          idString = "0x0" + idString;
          break;
        default:
          idString = "0x" + idString;
      }

      return getSymbol() + " - " + idString;
    }
  }
}
