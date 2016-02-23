// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

public final class Action extends AbstractCode
{
  // DLG/Action-specific field labels
  public static final String DLG_ACTION = "Action";

  private int nr;

  public Action()
  {
    super(DLG_ACTION);
  }

  public Action(byte buffer[], int offset, int count)
  {
    super(buffer, offset, DLG_ACTION + " " + count);
    this.nr = count;
  }

  public int getNumber()
  {
    return nr;
  }
}

