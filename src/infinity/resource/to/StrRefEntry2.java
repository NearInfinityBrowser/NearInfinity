// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import infinity.datatype.HexNumber;
import infinity.datatype.StringRef;
import infinity.resource.AbstractStruct;

public class StrRefEntry2 extends AbstractStruct
{
  // TOH/StrrefEntry2-specific field labels
  public static final String TOH_STRREF               = "StrRef entry";
  public static final String TOH_STRREF_OVERRIDDEN    = "Overridden strref";
  public static final String TOH_STRREF_OFFSET_STRING = "Relative override string offset";

  public StrRefEntry2() throws Exception
  {
    super(null, TOH_STRREF, new byte[8], 0);
  }

  public StrRefEntry2(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, TOH_STRREF + " " + nr, buffer, offset);
  }

  public StrRefEntry2(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, TOH_STRREF_OVERRIDDEN));
    addField(new HexNumber(buffer, offset + 4, 4, TOH_STRREF_OFFSET_STRING));
    return offset + 8;
  }
}
