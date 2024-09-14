// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.nio.ByteBuffer;

import org.infinity.datatype.ItemTypeBitmap;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class Purchases extends ItemTypeBitmap implements AddRemovable {
  // STO/Purchases-specific field labels
  public static final String STO_PURCHASES = "Store purchases";

  Purchases() {
    super(StreamUtils.getByteBuffer(4), 0, 4, STO_PURCHASES);
  }

  Purchases(ByteBuffer buffer, int offset, int number) {
    super(buffer, offset, 4, STO_PURCHASES + " " + number);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------
}
