// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

final class StateTrigger extends AbstractCode
{
  StateTrigger()
  {
    super("State trigger");
  }

  StateTrigger(byte buffer[], int offset, int count)
  {
    super(buffer, offset, "State trigger " + count);
  }
}

