// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import infinity.datatype.TextEdit;
import infinity.resource.AbstractStruct;

public class StringEntry2 extends AbstractStruct
{
  public StringEntry2() throws Exception
  {
    super(null, "String entry", new byte[524], 0);
  }

  public StringEntry2(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "String entry " + nr, buffer, offset);
  }

  public StringEntry2(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    int len = 0;
    while ((len < buffer.length - offset) && buffer[offset + len] != 0) {
      len++;
    }
    TextEdit edit = new TextEdit(buffer, offset, len + 1, "Override string");
    edit.setEolType(TextEdit.EOLType.UNIX);
    edit.setCharset("UTF-8");
    edit.setEditable(false);
    edit.setStringTerminated(true);
    list.add(edit);
    return offset + len + 1;
  }
}
