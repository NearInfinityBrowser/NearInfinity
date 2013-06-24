// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vertex;

import infinity.datatype.DecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public class Vertex extends AbstractStruct implements AddRemovable
{
  public Vertex() throws Exception
  {
    super(null, "Vertex", new byte[4], 0, 2);
  }

  Vertex(AbstractStruct superStruct, String name, byte buffer[], int offset) throws Exception
  {
    super(superStruct, name, buffer, offset, 2);
  }

  public Vertex(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Vertex " + nr, buffer, offset, 2);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  public int read(byte buffer[], int offset)
  {
    list.add(new DecNumber(buffer, offset, 2, "X"));
    list.add(new DecNumber(buffer, offset + 2, 2, "Y"));
    return offset + 4;
  }
}

