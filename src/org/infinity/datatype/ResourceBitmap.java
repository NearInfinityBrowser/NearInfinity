// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JButton;

import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

/**
 * Datatype for selecting resource entries, constructed from a predefined list of key/value pairs.
 *
 * <h2>Bean property</h2>
 * When this field is child of {@link AbstractStruct}, then changes of its internal
 * value reported as {@link PropertyChangeEvent}s of the {@link #getParent() parent}
 * struct.
 * <ul>
 * <li>Property name: {@link #getName() name} of this field</li>
 * <li>Property type: {@code long}</li>
 * <li>Value meaning: index of the resource in the predefined list</li>
 * </ul>
 */
public class ResourceBitmap extends AbstractBitmap<ResourceBitmap.RefEntry> implements IsReference, ActionListener
{
  /** Print resource reference together with search name in parentheses and value after hyphen. */
  public static final String FMT_REF_NAME_VALUE   = "%s (%s) - %s";
  /** Print search name together with resource reference in parentheses and value after hyphen. */
  public static final String FMT_NAME_REF_VALUE   = "%2$s (%1$s) - %3$s";
  /** Print resource reference together with its search name after hyphen. */
  public static final String FMT_REF_HYPHEN_NAME  = "%s - %s";
  /** Print search name together with its resource reference after hyphen. */
  public static final String FMT_NAME_HYPHEN_REF  = "%2$s - %1$s";
  /** Print resource reference together with value after hyphen. */
  public static final String FMT_REF_HYPHEN_VALUE = "%1$s - %3$s";
  /** Print resource reference together with its search name. */
  public static final String FMT_REF_NAME         = "%s (%s)";
  /** Print search name together with its resource reference. */
  public static final String FMT_NAME_REF         = "%2$s (%1$s)";
  /** Print resource reference together with value in parentheses. */
  public static final String FMT_REF_VALUE        = "%1$s (%3$s)";
  /** Print resource reference only. */
  public static final String FMT_REF_ONLY         = "%s";
  /** Print only the search name of the resource. */
  public static final String FMT_NAME_ONLY        = "%2$s";
  /** Print resource value only. */
  public static final String FMT_VALUE_ONLY       = "%3$s";

  private final BiFunction<Long, RefEntry, String> formatterResourceBitmap = (value, item) -> {
    String number;
    if (isShowAsHex()) {
      number = getHexValue(value.longValue());
    } else {
      number = value.toString();
    }

    String resName, searchString;
    if (item != null) {
      resName = item.getResourceName();
      searchString = item.getSearchString();
    } else {
      resName = getDefaultLabel();
      searchString = "";
    }

    return String.format(getFormatString(), resName, searchString, number);
  };

  private final String defaultLabel;
  private final String formatString;
  private final JButton bView;

  public ResourceBitmap(ByteBuffer buffer, int offset, int length, String name, List<RefEntry> resources,
                        String defLabel, String fmt)
  {
    this(buffer, offset, length, name, resources, defLabel, fmt, false);
  }

  public ResourceBitmap(ByteBuffer buffer, int offset, int length, String name, List<RefEntry> resources,
                        String defLabel, String fmt, boolean signed)
  {
    super(buffer, offset, length, name, createMap(resources), null, true);
    this.formatString = (fmt != null) ? fmt : FMT_REF_VALUE;
    this.defaultLabel = (defLabel != null) ? defLabel : "Unknown";
    this.setSortByName(true);
    this.setFormatter(formatterResourceBitmap);

    RefEntry curEntry = getDataOf(getLongValue());
    if (curEntry == null) {
      curEntry = getDataOf(0L);
    }
    if (curEntry == null && resources != null && resources.size() > 0) {
      curEntry = resources.get(0);
    }
    this.bView = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
    this.bView.addActionListener(this);
    this.bView.setEnabled(curEntry != null && curEntry.isResource());
    addButtons(this.bView);
  }

  //--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == bView) {
      Long value = getSelectedValue();
      final RefEntry selected = (value != null) ? getDataOf(value.longValue()) : null;
      if (selected != null) {
        final ResourceEntry entry = selected.getResourceEntry();
        new ViewFrame(getUiControl().getTopLevelAncestor(), ResourceFactory.getResource(entry));
      }
    }
  }

  //--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface IsReference ---------------------

  @Override
  public String getResourceName()
  {
    RefEntry entry = getDataOf(getLongValue());
    if (entry != null) {
      return entry.getResourceName();
    } else {
      return "";
    }
  }

//--------------------- End Interface IsReference ---------------------

  @Override
  protected void listItemChanged()
  {
    Long value = getSelectedValue();
    RefEntry selected = (value != null) ? getDataOf(value.longValue()) : null;
    bView.setEnabled(selected != null && selected.isResource());
  }

  protected String getFormatString()
  {
    return formatString;
  }

  protected String getDefaultLabel()
  {
    return defaultLabel;
  }

private static TreeMap<Long, RefEntry> createMap(List<RefEntry> resources)
  {
  TreeMap<Long, RefEntry> retVal = new TreeMap<>();
    if (resources != null) {
      for (final RefEntry entry : resources) {
        if (entry != null) {
          retVal.put(Long.valueOf(entry.getValue()), entry);
        }
      }
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  public static final class RefEntry implements Comparable<RefEntry>
  {
    /** Associated ID. */
    private final long value;
    /** Alternate label if ResourceEntry is empty. */
    private final String name;
    /** Contains resource if available. */
    private final ResourceEntry entry;
    /** Resource-dependent search string. */
    private final String searchString;
    /** Cached textual output for {@link #toString()} method. */
    private String desc;

    public RefEntry(long value, String ref)
    {
      this(value, ref, null, null);
    }

    public RefEntry(long value, String ref, String search)
    {
      this(value, ref, search, null);
    }

    public RefEntry(long value, String ref, String search, List<Path> searchDirs)
    {
      this.value = value;
      this.entry = (ref.lastIndexOf('.') > 0) ? ResourceFactory.getResourceEntry(ref, true, searchDirs) : null;
      if (this.entry != null) {
        this.searchString = (search != null) ? search : entry.getSearchString();
        this.name = null;
      } else {
        this.searchString = (search != null) ? search : "";
        this.name = ref;
      }
      this.desc = String.format(FMT_REF_VALUE, getResourceName(), getSearchString(), Long.toString(value));
    }

    @Override
    public String toString()
    {
      return desc;
    }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 31 * hash + Long.hashCode(value);
      hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
      hash = 31 * hash + ((entry == null) ? 0 : entry.hashCode());
      hash = 31 * hash + ((searchString == null) ? 0 : searchString.hashCode());
      hash = 31 * hash + ((desc == null) ? 0 : desc.hashCode());
      return hash;
    }

    @Override
    public boolean equals(Object o)
    {
      return desc.equalsIgnoreCase(Misc.safeToString(o));
    }

    @Override
    public int compareTo(RefEntry o)
    {
      return desc.compareToIgnoreCase(Misc.safeToString(o));
    }

    public boolean isResource() { return (entry != null); }

    public long getValue() { return value; }

    public ResourceEntry getResourceEntry() { return entry; }

    public String getResourceName()
    {
      if (entry != null) {
        return entry.getResourceName();
      } else {
        return name;
      }
    }

    public String getSearchString()
    {
      return searchString;
    }
  }
}
