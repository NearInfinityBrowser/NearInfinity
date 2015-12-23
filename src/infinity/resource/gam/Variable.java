// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.FloatNumber;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.StructEntry;

public class Variable extends AbstractStruct implements AddRemovable
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

  Variable(AbstractStruct superStruct, String s, byte b[], int o) throws Exception
  {
    super(superStruct, s, b, o);
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
    // using simplified structure format for GAM V1.1  resources
    boolean isV1 = true;
    if (getParent() instanceof AbstractStruct) {
      StructEntry field = ((AbstractStruct)getParent()).getAttribute(4, false);
      if (field instanceof TextString) {
        isV1 = ((TextString)field).toString().equals("V1.1");
      }
    }

    addField(new TextString(buffer, offset, 32, "Name"));
    if (isV1) {
      addField(new Unknown(buffer, offset + 32, 8));
      addField(new DecNumber(buffer, offset + 40, 4, "Value"));
      addField(new Unknown(buffer, offset + 44, 40));
    } else {
      addField(new Bitmap(buffer, offset + 32, 2, "Type", s_type));
      addField(new DecNumber(buffer, offset + 34, 2, "Reference value (unused)"));
      addField(new DecNumber(buffer, offset + 36, 4, "Dword value (unused)"));
      addField(new DecNumber(buffer, offset + 40, 4, "Integer value"));
      addField(new FloatNumber(buffer, offset + 44, 8, "Double value (unused)"));
      addField(new TextString(buffer, offset + 52, 32, "Script name (unused)"));
    }
    return offset + 84;
  }
}

