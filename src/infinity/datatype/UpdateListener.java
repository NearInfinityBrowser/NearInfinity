// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.util.EventListener;

/**
 * Used in conjunction with infinity.datatype.Editable's updateValue() method.
 * @author argent77
 */
public interface UpdateListener extends EventListener
{
  /**
   * Called whenever the editable item has changed its value.
   * @param event Contains associated data
   * @return true if table data other than the current item has been changed, false otherwise.
   */
  public boolean valueUpdated(UpdateEvent event);
}
