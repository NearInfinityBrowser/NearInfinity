// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.nio.ByteBuffer;

public final class ResponseTrigger extends AbstractCode
{
  // DLG/ResponseTrigger-specific field labels
  public static final String DLG_RESPONSETRIGGER = "Response trigger";

  private int nr;

  ResponseTrigger()
  {
    super(DLG_RESPONSETRIGGER);
  }

  ResponseTrigger(ByteBuffer buffer, int offset, int count)
  {
    super(buffer, offset, DLG_RESPONSETRIGGER + " " + count);
    this.nr = count;
  }

  public int getNumber()
  {
    return nr;
  }
}

