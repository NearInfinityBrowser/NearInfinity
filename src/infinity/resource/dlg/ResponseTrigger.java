// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

public final class ResponseTrigger extends AbstractCode
{
  // DLG/ResponseTrigger-specific field labels
  public static final String DLG_RESPONSETRIGGER = "Response trigger";

  private int nr;

  ResponseTrigger()
  {
    super(DLG_RESPONSETRIGGER);
  }

  ResponseTrigger(byte buffer[], int offset, int count)
  {
    super(buffer, offset, DLG_RESPONSETRIGGER + " " + count);
    this.nr = count;
  }

  public int getNumber()
  {
    return nr;
  }
}

