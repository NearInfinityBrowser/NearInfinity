// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class Drink extends AbstractStruct implements AddRemovable {
  // STO/Drink-specific field labels
  public static final String STO_DRINK            = "Drink";
  public static final String STO_DRINK_NAME       = "Drink name";
  public static final String STO_DRINK_PRICE      = "Price";
  public static final String STO_DRINK_RUMOR_RATE = "Rumor rate";

  Drink() throws Exception {
    super(null, STO_DRINK, StreamUtils.getByteBuffer(20), 0);
  }

  Drink(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception {
    super(superStruct, STO_DRINK + " " + number, buffer, offset);
  }

  // --------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove() {
    return true;
  }

  // --------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new Unknown(buffer, offset, 8));
    addField(new StringRef(buffer, offset + 8, STO_DRINK_NAME));
    addField(new DecNumber(buffer, offset + 12, 4, STO_DRINK_PRICE));
    addField(new DecNumber(buffer, offset + 16, 4, STO_DRINK_RUMOR_RATE));
    return offset + 20;
  }
}
