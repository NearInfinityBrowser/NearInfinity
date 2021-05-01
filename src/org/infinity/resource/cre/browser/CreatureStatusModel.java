// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.infinity.util.Misc;

/**
 * {@code ComboBoxModel} for the creature allegiance combo box used in the Creature Animation Browser.
 */
public class CreatureStatusModel extends AbstractListModel<CreatureStatusModel.StatusEntry>
    implements ComboBoxModel<CreatureStatusModel.StatusEntry>
{
  /** Available creature status entries. */
  public enum Status {
    /** Indicates green selection circle. */
    FRIENDLY(2),
    /** Indicates cyan selection circle. */
    NEUTRAL(128),
    /** Indicates red selection circle. */
    HOSTILE(255),
    /** Indicates yellow selection circle. */
    PANICKED(-1);

    private final int id;
    private Status(int id) { this.id = id; }

    /** Returns the numeric id associated with the enum. */
    public int getValue() { return id; }
  }

  private final List<StatusEntry> statusList = new ArrayList<>();

  private Object selectedItem;

  public CreatureStatusModel()
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
   * @param anItem a {@code StatusEntry} object, {@code Number} object or {@code String} specifying an allegiance name.
   * @return an int representing the index position, where 0 is the first position. Returns -1
   *         if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof StatusEntry) {
      return statusList.indexOf(anItem);
    } else if (anItem instanceof Number) {
      final int eaValue = ((Number)anItem).intValue();
      return IntStream
          .range(0, statusList.size())
          .filter(i -> statusList.get(i).getStatus().getValue() == eaValue)
          .findAny()
          .orElse(-1);
    } else if (anItem != null) {
      final String eaName = anItem.toString().trim();
      return IntStream
          .range(0, statusList.size())
          .filter(i -> eaName.equalsIgnoreCase(statusList.get(i).getName()))
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!statusList.isEmpty()) {
      int oldSize = statusList.size();
      statusList.clear();
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
    return statusList.size();
  }

  @Override
  public StatusEntry getElementAt(int index)
  {
    if (index >= 0 && index < statusList.size()) {
      return statusList.get(index);
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

    for (final Status status : Status.values()) {
      statusList.add(new StatusEntry(status));
    }
    fireIntervalAdded(this, 0, statusList.size() - 1);

    setSelectedItem(getElementAt(0));
  }

//-------------------------- INNER CLASSES --------------------------

  public static class StatusEntry
  {
    private final Status status;
    private final String name;

    public StatusEntry(Status status)
    {
      this.status = Objects.requireNonNull(status);
      this.name = Misc.prettifySymbol(this.status.toString());
    }

    /** Returns the status enum. */
    public Status getStatus() { return status; }

    /**
     * Returns a typical EA.IDS value representing the status.
     * Returns negative values for non-allegiance status types.
     */
    public int getValue() { return status.getValue(); }

    /** Returns the descriptive name of the status. */
    public String getName() { return name; }

    @Override
    public String toString()
    {
      return getName();
    }
  }
}
