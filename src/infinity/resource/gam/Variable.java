// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.FloatNumber;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public class Variable extends AbstractStruct implements AddRemovable
{
  // GAM/Variable-specific field labels
  public static final String GAM_VAR              = "Variable";
  public static final String GAM_VAR_NAME         = "Name";
  public static final String GAM_VAR_TYPE         = "Type";
  public static final String GAM_VAR_REFERENCE    = "Reference value (unused)";
  public static final String GAM_VAR_DWORD        = "Dword value (unused)";
  public static final String GAM_VAR_INT          = "Integer value";
  public static final String GAM_VAR_DOUBLE       = "Double value (unused)";
  public static final String GAM_VAR_SCRIPT_NAME  = "Script name (unused)";

  public static final String s_type[] = {"Integer", "Float", "Script name", "Resource reference",
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
    addField(new TextString(buffer, offset, 32, GAM_VAR_NAME));
    addField(new Bitmap(buffer, offset + 32, 2, GAM_VAR_TYPE, s_type));
    addField(new DecNumber(buffer, offset + 34, 2, GAM_VAR_REFERENCE));
    addField(new DecNumber(buffer, offset + 36, 4, GAM_VAR_DWORD));
    addField(new DecNumber(buffer, offset + 40, 4, GAM_VAR_INT));
    addField(new FloatNumber(buffer, offset + 44, 8, GAM_VAR_DOUBLE));
    addField(new TextString(buffer, offset + 52, 32, GAM_VAR_SCRIPT_NAME));
    return offset + 84;
  }
}

