// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AbstractVariable;

public class Variable extends AbstractVariable
{
  Variable() throws Exception
  {
    super();
  }

  Variable(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, buffer, offset, number);
  }
}

