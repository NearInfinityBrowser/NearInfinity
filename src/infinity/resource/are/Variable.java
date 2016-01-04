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
  // ARE/Variable-specific field labels
  public static final String ARE_VAR              = "Variable";
  public static final String ARE_VAR_NAME         = infinity.resource.gam.Variable.GAM_VAR_NAME;
  public static final String ARE_VAR_TYPE         = infinity.resource.gam.Variable.GAM_VAR_TYPE;
  public static final String ARE_VAR_REFERENCE    = infinity.resource.gam.Variable.GAM_VAR_REFERENCE;
  public static final String ARE_VAR_DWORD        = infinity.resource.gam.Variable.GAM_VAR_DWORD;
  public static final String ARE_VAR_INT          = infinity.resource.gam.Variable.GAM_VAR_INT;
  public static final String ARE_VAR_DOUBLE       = infinity.resource.gam.Variable.GAM_VAR_DOUBLE;
  public static final String ARE_VAR_SCRIPT_NAME  = infinity.resource.gam.Variable.GAM_VAR_SCRIPT_NAME;

  Variable() throws Exception
  {
    super(null, ARE_VAR, new byte[84], 0);
  }

  Variable(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, ARE_VAR + " " + number, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, ARE_VAR_NAME));
    addField(new Bitmap(buffer, offset + 32, 2, ARE_VAR_TYPE, infinity.resource.gam.Variable.s_type));
    addField(new DecNumber(buffer, offset + 34, 2, ARE_VAR_REFERENCE));
    addField(new DecNumber(buffer, offset + 36, 4, ARE_VAR_DWORD));
    addField(new DecNumber(buffer, offset + 40, 4, ARE_VAR_INT));
    addField(new FloatNumber(buffer, offset + 44, 8, ARE_VAR_DOUBLE));
    addField(new TextString(buffer, offset + 52, 32, ARE_VAR_SCRIPT_NAME));
    return offset + 84;
  }
}

