// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;

public final class NonPartyNPC extends PartyNPC {
  // GAM/NonPartyNPC-specific field labels
  public static final String GAM_EXNPC = "Non-party character";

  NonPartyNPC() throws Exception {
    super(null, GAM_EXNPC, createEmptyBuffer(), 0);
  }

  NonPartyNPC(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception {
    super(superStruct, GAM_EXNPC + " " + nr, buffer, offset);
  }
}
