// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.resource.itm.ItmResource;
import org.infinity.util.io.StreamUtils;

public final class Purchases extends Bitmap implements AddRemovable {
  // STO/Purchases-specific field labels
  public static final String STO_PURCHASES = "Store purchases";

  Purchases() {
    super(StreamUtils.getByteBuffer(4), 0, 4, STO_PURCHASES,
        (Profile.getEngine() == Profile.Engine.PST) ? ItmResource.CATEGORIES11_ARRAY : ItmResource.CATEGORIES_ARRAY);
  }

  Purchases(ByteBuffer buffer, int offset, int number) {
    super(buffer, offset, 4, STO_PURCHASES + " " + number,
        (Profile.getEngine() == Profile.Engine.PST) ? ItmResource.CATEGORIES11_ARRAY : ItmResource.CATEGORIES_ARRAY);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------
}
