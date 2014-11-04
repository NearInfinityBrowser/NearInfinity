// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;

public final class SectionOffset extends HexNumber
{
  private final Class<? extends StructEntry> section;

  public SectionOffset(byte buffer[], int offset, String desc, Class<? extends StructEntry> section)
  {
    this(null, buffer, offset, desc, section);
  }

  public SectionOffset(StructEntry parent, byte buffer[], int offset, String desc,
                       Class<? extends StructEntry> section)
  {
    super(parent, buffer, offset, 4, desc);
    this.section = section;
  }

  public Class<? extends StructEntry> getSection()
  {
    return section;
  }
}

