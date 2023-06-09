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

public final class Iwd2Shape extends AbstractStruct implements AddRemovable {
  // CRE/Iwd2Shape-specific field labels
  public static final String CRE_SHAPE                  = "Shape";
  public static final String CRE_SHAPE_RESREF           = "ResRef";
  public static final String CRE_SHAPE_NUM_MEMORIZABLE  = "# memorizable";
  public static final String CRE_SHAPE_NUM_REMAINING    = "# remaining";

  public Iwd2Shape() throws Exception {
    super(null, CRE_SHAPE, StreamUtils.getByteBuffer(16), 0);
  }

  public Iwd2Shape(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, CRE_SHAPE, buffer, offset);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new IwdRef(buffer, offset, CRE_SHAPE_RESREF, "LISTSHAP.2DA"));
    addField(new DecNumber(buffer, offset + 4, 4, CRE_SHAPE_NUM_MEMORIZABLE));
    addField(new DecNumber(buffer, offset + 8, 4, CRE_SHAPE_NUM_REMAINING));
    addField(new Unknown(buffer, offset + 12, 4));
    return offset + 16;
  }
}
