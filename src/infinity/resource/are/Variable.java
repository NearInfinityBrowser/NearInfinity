// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.resource.AbstractStruct;
import infinity.resource.AbstractVariable;

public class Variable extends AbstractVariable
{
  Variable() throws Exception
  {
    super();
  }

  Variable(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, buffer, offset, number);
  }
}

