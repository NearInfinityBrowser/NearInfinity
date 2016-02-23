// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.util.EventObject;

import org.infinity.resource.AbstractStruct;

/**
 * Used in conjunction with infinity.datatype.Editable's updateValue() method.
 * @author argent77
 */
public class UpdateEvent extends EventObject
{
  private final AbstractStruct struct;

  public UpdateEvent(Object source, AbstractStruct struct)
  {
    super(source);
    this.struct = struct;
  }

  public AbstractStruct getStructure()
  {
    return struct;
  }
}
