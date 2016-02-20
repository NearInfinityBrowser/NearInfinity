// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.var;

import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Entry extends AbstractStruct implements AddRemovable
{
  // GAM-specific field labels
  public static final String VAR_ENTRY        = "Variable";
  public static final String VAR_ENTRY_TYPE   = "Type";
  public static final String VAR_ENTRY_NAME   = "Name";
  public static final String VAR_ENTRY_VALUE  = "Value";

  Entry() throws Exception
  {
    super(null, VAR_ENTRY, new byte[44], 0);
  }

  Entry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, VAR_ENTRY + " " + nr, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 8, VAR_ENTRY_TYPE));
    addField(new TextString(buffer, offset + 8, 32, VAR_ENTRY_NAME));
    addField(new DecNumber(buffer, offset + 40, 4, VAR_ENTRY_VALUE));
    return offset + 44;
  }
}

