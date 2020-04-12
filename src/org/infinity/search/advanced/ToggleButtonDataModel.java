// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import javax.swing.JToggleButton;

/**
 * This class extends {@code JToggleButton.ToggleButtonModel} by an associated data object.
 */
public class ToggleButtonDataModel extends JToggleButton.ToggleButtonModel
{
  private Object data;

  /** Creates a new ToggleButton Model. */
  public ToggleButtonDataModel()
  {
    this(null);
  }

  /** Creates a new ToggleButton Model and associates it with the specified data object. */
  public ToggleButtonDataModel(Object data)
  {
    super();
    this.data = data;
  }

  /** Returns the associated data. */
  public Object getData()
  {
    return data;
  }

  /** Assigns new data to this model instance. */
  public void setData(Object data)
  {
    this.data = data;
  }

  /** Returns whether data has been assigned to this model instance. */
  public boolean hasData()
  {
    return data != null;
  }
}
