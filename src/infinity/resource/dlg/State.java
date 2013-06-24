// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.dlg;

import infinity.datatype.DecNumber;
import infinity.datatype.StringRef;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class State extends AbstractStruct implements AddRemovable
{
  private int nr;

  State() throws Exception
  {
    super(null, "State", new byte[16], 0);
  }

  State(AbstractStruct superStruct, byte buffer[], int offset, int count) throws Exception
  {
    super(superStruct, "State " + count, buffer, offset);
    nr = count;
  }

  public int getFirstTrans()
  {
    return ((DecNumber)getAttribute("First response index")).getValue();
  }

  public int getNumber()
  {
    return nr;
  }

  public StringRef getResponse()
  {
    return (StringRef)getAttribute("Response");
  }

  public int getTransCount()
  {
    return ((DecNumber)getAttribute("# responses")).getValue();
  }

  public int getTriggerIndex()
  {
    return ((DecNumber)getAttribute("Trigger index")).getValue();
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  public int read(byte buffer[], int offset)
  {
    list.add(new StringRef(buffer, offset, "Response"));
    list.add(new DecNumber(buffer, offset + 4, 4, "First response index"));
    list.add(new DecNumber(buffer, offset + 8, 4, "# responses"));
    list.add(new DecNumber(buffer, offset + 12, 4, "Trigger index"));
    return offset + 16;
  }
}

