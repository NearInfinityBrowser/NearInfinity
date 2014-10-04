// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import infinity.datatype.HexNumber;
import infinity.datatype.TextEdit;
import infinity.resource.AbstractStruct;

public final class StringEntry extends AbstractStruct
{
  public StringEntry() throws Exception
  {
    super(null, "String entry", new byte[524], 0);
  }

  public StringEntry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "String entry " + nr, buffer, offset);
  }

  public StringEntry(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new HexNumber(buffer, offset, 4, "Offset to next free region"));
    list.add(new HexNumber(buffer, offset + 4, 4, "Offset of preceeding entry"));
    list.add(new TextEdit(buffer, offset + 8, 512, "String data"));
    list.add(new HexNumber(buffer, offset + 520, 4, "Offset of following entry"));
    return offset + 524;
  }
}
