// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;

import org.infinity.resource.StructEntry;

public final class SectionOffset extends HexNumber
{
  private final Class<? extends StructEntry> section;

  public SectionOffset(ByteBuffer buffer, int offset, String desc, Class<? extends StructEntry> section)
  {
    this(null, buffer, offset, desc, section);
  }

  public SectionOffset(StructEntry parent, ByteBuffer buffer, int offset, String desc,
                       Class<? extends StructEntry> section)
  {
    super(parent, buffer, offset, 4, desc);
    this.section = section;
  }

//--------------------- Begin Interface InlineEditable ---------------------

 @Override
 public boolean update(Object value)
 {
   // should not be modified by the user
   return false;
 }

//--------------------- End Interface InlineEditable ---------------------

  public Class<? extends StructEntry> getSection()
  {
    return section;
  }
}

