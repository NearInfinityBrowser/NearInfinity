// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;

import org.infinity.datatype.HexNumber;
import org.infinity.datatype.TextEdit;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class StringEntry extends AbstractStruct {
  // TOT/StringEntry-specific field labels
  public static final String TOT_STRING                     = "String entry";
  public static final String TOT_STRING_OFFSET_FREE_REGION  = "Offset to next free region";
  public static final String TOT_STRING_OFFSET_PREV_ENTRY   = "Offset of preceding entry";
  public static final String TOT_STRING_TEXT                = "Text";
  public static final String TOT_STRING_OFFSET_NEXT_ENTRY   = "Offset of following entry";

  public StringEntry() throws Exception {
    super(null, TOT_STRING, StreamUtils.getByteBuffer(524), 0);
  }

  public StringEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, TOT_STRING + " " + nr, buffer, offset);
  }

  public StringEntry(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new HexNumber(buffer, offset, 4, TOT_STRING_OFFSET_FREE_REGION));
    addField(new HexNumber(buffer, offset + 4, 4, TOT_STRING_OFFSET_PREV_ENTRY));
    addField(new TextEdit(buffer, offset + 8, 512, TOT_STRING_TEXT));
    addField(new HexNumber(buffer, offset + 520, 4, TOT_STRING_OFFSET_NEXT_ENTRY));
    return offset + 524;
  }
}
