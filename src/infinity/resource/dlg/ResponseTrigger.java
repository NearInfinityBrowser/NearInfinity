// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

final class ResponseTrigger extends AbstractCode
{
  ResponseTrigger()
  {
    super("Response trigger");
  }

  ResponseTrigger(byte buffer[], int offset, int count)
  {
    super(buffer, offset, "Response trigger " + count);
  }
}

