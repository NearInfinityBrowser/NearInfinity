// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import java.io.IOException;
import java.io.OutputStream;

public interface Writeable
{
  void write(OutputStream os) throws IOException;
}

