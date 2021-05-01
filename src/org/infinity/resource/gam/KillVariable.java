// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public final class KillVariable extends Variable
{
  // GAM/KillVariable-specific field labels
  public static final String GAM_KILLVAR = "Kill variable";

  KillVariable() throws Exception
  {
    super(null, GAM_KILLVAR, StreamUtils.getByteBuffer(84), 0);
  }

  KillVariable(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, GAM_KILLVAR + " " + number, buffer, offset);
  }
}

