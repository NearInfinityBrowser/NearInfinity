// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import org.infinity.resource.AbstractStruct;

class AreaLinkNorth extends AreaLink
{
  // WMP/AreaLinkNorth-specific field labels
  public static final String WMP_LINK_NORTH = "North link";

  AreaLinkNorth() throws Exception
  {
    super(WMP_LINK_NORTH);
  }

  AreaLinkNorth(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, buffer, offset, WMP_LINK_NORTH + " " + number);
  }
}
