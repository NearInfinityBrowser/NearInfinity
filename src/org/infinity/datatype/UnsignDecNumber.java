// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import org.infinity.resource.StructEntry;

public class UnsignDecNumber extends DecNumber implements InlineEditable
{
  public UnsignDecNumber(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public UnsignDecNumber(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, false);
  }
}

