// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;

import org.infinity.datatype.TextEdit;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public class StringEntry2 extends AbstractStruct {
  // TOH/StringEntry2-specific field labels
  public static final String TOH_STRING       = "String entry";
  public static final String TOH_STRING_TEXT  = "Override string";

  public StringEntry2() throws Exception {
    super(null, TOH_STRING, StreamUtils.getByteBuffer(524), 0);
  }

  public StringEntry2(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, TOH_STRING + " " + nr, buffer, offset);
  }

  public StringEntry2(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    int len = 0;
    while ((len < buffer.limit() - offset) && buffer.get(offset + len) != 0) {
      len++;
    }
    TextEdit edit = new TextEdit(buffer, offset, len + 1, TOH_STRING_TEXT);
    edit.setEolType(TextEdit.EOLType.UNIX);
    edit.setCharset("UTF-8");
    edit.setEditable(false);
    edit.setStringTerminated(true);
    addField(edit);
    return offset + len + 1;
  }
}
