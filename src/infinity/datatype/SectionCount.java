// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

public final class SectionCount extends DecNumber
{
  private final Class section;

  public SectionCount(byte buffer[], int offset, int length, String desc, Class section)
  {
    super(buffer, offset, length, desc);
    this.section = section;
  }

  public Class getSection()
  {
    return section;
  }
}

