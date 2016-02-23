// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;

public final class KillVariable extends Variable implements AddRemovable
{
  // GAM/KillVariable-specific field labels
  public static final String GAM_KILLVAR = "Kill variable";

  KillVariable() throws Exception
  {
    super(null, GAM_KILLVAR, new byte[84], 0);
  }

  KillVariable(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, GAM_KILLVAR + " " + number, buffer, offset);
  }
}

