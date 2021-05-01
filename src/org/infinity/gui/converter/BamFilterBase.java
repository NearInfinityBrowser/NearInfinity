// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * The base class for BAM converter effects filters.
 */
public abstract class BamFilterBase
{
  /** Supported filter types. */
  public enum Type {
    /** Specifies an filter type that manipulates on color/pixel level only. (Examples: Brightness, Color balance) */
    COLOR,
    /** Specifies an filter type that manipulates on image level. (Example: Resize) */
    TRANSFORM,
    /** Specifies an filter type that outputs the current BAM structure to disk. (Example: Split BAM) */
    OUTPUT
  }

  private final ConvertToBam converter;
  private final String name, description;
  private final Type type;
  private final List<ChangeListener> listChangeListeners = new ArrayList<>();
  private final JPanel controls;

  protected BamFilterBase(ConvertToBam converter, String name, String desc, Type type)
  {
    if (converter == null) {
      throw new NullPointerException();
    }
    this.converter = converter;
    this.name = (name != null) ? name : "";
    this.description = (desc != null) ? desc : "";
    this.type = type;
    this.controls = loadControls();
  }

  /** Returns the associated {@code ConvertToBam} instance. */
  public ConvertToBam getConverter()
  {
    return converter;
  }

  /** Returns the name of the filter. Will be used to identify the filter in the filters list. */
  public String getName()
  {
    return name;
  }

  /** Returns an optional description of the filter. Will be shown when the filter has been selected. */
  public String getDescription()
  {
    return description;
  }

  /** Returns the filter type/category. Will be used internally. */
  public Type getType()
  {
    return type;
  }

  /**
   * Returns a panel with controls that can be used for setting specific filter parameters.
   */
  public JPanel getControls()
  {
    return controls;
  }

  /** This method is called whenever a change has occured in associated the ConvertToBam instance. */
  public void updateControls()
  {
    // does nothing by default
  }

  /**
   * Returns the current filter configuration as string data.
   * The format string should be in the format <pre>value1;"string1";[complex,"string2",value2];value3</pre>
   */
  public abstract String getConfiguration();

  /**
   * Applies the specified configuration data to the filter.
   * @param config The configuration data as string data.
   * @return {@code true} if configuration have been applied successfully,
   *         {@code false} otherwise.
   */
  public abstract boolean setConfiguration(String config);

  /** Cleans up class-specific resource. Call whenever you want to release this instance from memory. */
  public void close() {}

  /**
   * Modifies the specified BufferedImage object to reflect the current settings of the filter.<br>
   * <b>Note:</b> For optimization purposes, prevent creating a new BufferedImage object if possible.
   * @param frame The PseudoBamFrameEntry object to modify.
   * @return The updated PseudoBamFrameEntry object.
   */
  public abstract PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry frame);


  /**
   * Adds a ChangeListener to the listener list.
   * ChangeListeners will be notified whenever the filter settings change.
   */
  public void addChangeListener(ChangeListener l)
  {
    if (l != null) {
      if (listChangeListeners.indexOf(l) < 0) {
        listChangeListeners.add(l);
      }
    }
  }

  /** Returns an array of all ChangeListeners added to this filter object. */
  public ChangeListener[] getChangeListeners()
  {
    ChangeListener[] retVal = new ChangeListener[listChangeListeners.size()];
    for (int i = 0; i < retVal.length; i++) {
      retVal[i] = listChangeListeners.get(i);
    }
    return retVal;
  }

  /** Removes a ChangeListener from the listener list. */
  public void removeChangeListener(ChangeListener l)
  {
    if (l != null) {
      int idx = listChangeListeners.indexOf(l);
      if (idx >= 0) {
        listChangeListeners.remove(idx);
      }
    }
  }


  @Override
  public String toString()
  {
    return name;
  }

  /** Initializes the control panel that will be returned by {@link #getControls()}. */
  protected abstract JPanel loadControls();

  /**
   * Triggers a ChangeEvent in all registered components.
   * Should be called by subclasses whenever a filter setting has been changed.
   */
  protected void fireChangeListener()
  {
    ChangeEvent event = new ChangeEvent(this);
    for (int i = 0; i < listChangeListeners.size(); i++) {
      listChangeListeners.get(i).stateChanged(event);
    }
  }

  /** Parses a single numeric value from a parameter string in the given limits. Returns defValue on error. */
  protected static int decodeNumber(String param, int min, int max, int defValue)
  {
    if (param != null) {
      try {
        int value = Integer.parseInt(param);
        if (value >= min && value <= max) {
          return value;
        }
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }

  /** Parses a single floating point value from a parameter string in the given limits. Returns defValue on error. */
  protected static double decodeDouble(String param, double min, double max, double defValue)
  {
    if (param != null) {
      try {
        double value = Double.parseDouble(param);
        if (value >= min && value <= max) {
          return value;
        }
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }
}
