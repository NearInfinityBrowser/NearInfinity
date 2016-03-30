// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AbstractVariable;

public class Variable extends AbstractVariable
{
  public Variable() throws Exception
  {
    super();
  }

  public Variable(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, buffer, offset, number);
  }

  public Variable(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }
}

