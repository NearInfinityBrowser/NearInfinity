// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.Unknown;

/**
 * Subclassed from {@code Unknown} to make field type identifiable by reflection.
 */
public class Explored extends Unknown
{

  public Explored(ByteBuffer buffer, int offset, int length)
  {
    super(buffer, offset, length);
  }

  public Explored(ByteBuffer buffer, int offset, int length, String name)
  {
    super(buffer, offset, length, name);
  }

}
