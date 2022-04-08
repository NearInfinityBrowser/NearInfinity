// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.src;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

final class Entry extends AbstractStruct implements AddRemovable {
  // SRC/Entry-specific field labels
  public static final String SRC_ENTRY        = "Entry";
  public static final String SRC_ENTRY_TEXT   = "Text";
  public static final String SRC_ENTRY_WEIGHT = "Weight";

  Entry() throws Exception {
    super(null, SRC_ENTRY, StreamUtils.getByteBuffer(8), 0);
  }

  Entry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception {
    super(superStruct, SRC_ENTRY + " " + number, buffer, offset);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new StringRef(buffer, offset, SRC_ENTRY_TEXT));
    addField(new DecNumber(buffer, offset + 4, 4, SRC_ENTRY_WEIGHT));
    return offset + 8;
  }
}
