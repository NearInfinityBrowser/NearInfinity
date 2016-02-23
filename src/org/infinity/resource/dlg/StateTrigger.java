// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

public final class StateTrigger extends AbstractCode
{
  // DLG/StateTrigger-specific field labels
  public static final String DLG_STATETRIGGER = "State trigger";

  private int nr;

  StateTrigger()
  {
    super(DLG_STATETRIGGER);
  }

  StateTrigger(byte buffer[], int offset, int count)
  {
    super(buffer, offset, DLG_STATETRIGGER + " " + count);
    this.nr = count;
  }

  public int getNumber()
  {
    return nr;
  }
}

