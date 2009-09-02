// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

public final class SectionOffset extends HexNumber
{
  private final Class section;

  public SectionOffset(byte buffer[], int offset, String desc, Class section)
  {
    super(buffer, offset, 4, desc);
    this.section = section;
  }

  public Class getSection()
  {
    return section;
  }
}

