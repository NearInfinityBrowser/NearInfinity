// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

public final class Action extends AbstractCode
{
  public static final String FMT_NAME = "Action %1$d";

  public Action()
  {
    super("Action");
  }

  public Action(byte buffer[], int offset, int count)
  {
    super(buffer, offset, String.format(FMT_NAME, count));
  }
}

