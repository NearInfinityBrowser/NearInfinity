// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import infinity.datatype.HexNumber;
import infinity.datatype.StringRef;
import infinity.resource.AbstractStruct;

public class StrRefEntry2 extends AbstractStruct
{
  public StrRefEntry2() throws Exception
  {
    super(null, "StrRef entry", new byte[8], 0);
  }

  public StrRefEntry2(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "StrRef entry " + nr, buffer, offset);
  }

  public StrRefEntry2(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new StringRef(buffer, offset, "Overridden strref"));
    list.add(new HexNumber(buffer, offset + 4, 4, "Relative override string offset"));
    return offset + 8;
  }
}
