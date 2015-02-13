// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.StructEntry;

public final class SectionCount extends DecNumber
{
  private final Class<? extends StructEntry> section;

  public SectionCount(byte buffer[], int offset, int length, String desc, Class<? extends StructEntry> section)
  {
    this(null, buffer, offset, length, desc, section);
  }

  public SectionCount(StructEntry parent, byte buffer[], int offset, int length, String desc,
                      Class<? extends StructEntry> section)
  {
    super(parent, buffer, offset, length, desc);
    this.section = section;
  }

  public Class<? extends StructEntry> getSection()
  {
    return section;
  }
}

