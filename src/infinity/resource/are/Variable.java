// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.FloatNumber;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Variable extends AbstractStruct implements AddRemovable
{
  private static final String s_type[] = {"Integer", "Float", "Script name", "Resource reference",
                                          "String reference", "Double word"};

  Variable() throws Exception
  {
    super(null, "Variable", new byte[84], 0);
  }

  Variable(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Variable " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, "Name"));
    addField(new Bitmap(buffer, offset + 32, 2, "Type", s_type));
    addField(new DecNumber(buffer, offset + 34, 2, "Reference value"));
    addField(new DecNumber(buffer, offset + 36, 4, "Dword value"));
    addField(new DecNumber(buffer, offset + 40, 4, "Integer value"));
    addField(new FloatNumber(buffer, offset + 44, 8, "Double value"));
    addField(new TextString(buffer, offset + 52, 32, "Script name"));
    return offset + 84;
  }
}

