// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IwdRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class Iwd2Song extends AbstractStruct implements AddRemovable {
  // CRE/Iwd2Song-specific field labels
  public static final String CRE_SONG                 = "Song";
  public static final String CRE_SONG_RESREF          = "ResRef";
  public static final String CRE_SONG_NUM_MEMORIZABLE = "# memorizable";
  public static final String CRE_SONG_NUM_REMAINING   = "# remaining";

  public Iwd2Song() throws Exception {
    super(null, CRE_SONG, StreamUtils.getByteBuffer(16), 0);
  }

  public Iwd2Song(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, CRE_SONG, buffer, offset);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new IwdRef(buffer, offset, CRE_SONG_RESREF, "LISTSONG.2DA"));
    addField(new DecNumber(buffer, offset + 4, 4, CRE_SONG_NUM_MEMORIZABLE));
    addField(new DecNumber(buffer, offset + 8, 4, CRE_SONG_NUM_REMAINING));
    addField(new Unknown(buffer, offset + 12, 4));
    return offset + 16;
  }
}
