// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;

/**
 * Subclassed from {@code DecNumber} to make field type identifiable by reflection.
 */
public class IndexNumber extends DecNumber
{
  public IndexNumber(ByteBuffer buffer, int offset, int length, String name)
  {
    super(buffer, offset, length, name);
  }

  public IndexNumber(ByteBuffer buffer, int offset, int length, String name, boolean signed)
  {
    super(buffer, offset, length, name, signed);
  }

}
