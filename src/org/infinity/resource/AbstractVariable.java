// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.FloatNumber;
import org.infinity.datatype.TextString;

public class AbstractVariable extends AbstractStruct implements AddRemovable
{
  // Variable-specific field labels
  public static final String VAR              = "Variable";
  public static final String VAR_NAME         = "Name";
  public static final String VAR_TYPE         = "Type";
  public static final String VAR_REFERENCE    = "Reference value (unused)";
  public static final String VAR_DWORD        = "Dword value (unused)";
  public static final String VAR_INT          = "Integer value";
  public static final String VAR_DOUBLE       = "Double value (unused)";
  public static final String VAR_SCRIPT_NAME  = "Script name (unused)";

  public static final String s_type[] = {"Integer", "Float", "Script name", "Resource reference",
                                         "String reference", "Double word"};

  protected AbstractVariable() throws Exception
  {
    super(null, VAR, new byte[84], 0);
  }

  protected AbstractVariable(AbstractStruct superStruct, byte[] buffer, int offset, int nr) throws Exception
  {
    super(superStruct, VAR + " " + nr, buffer, offset);
  }

  protected AbstractVariable(AbstractStruct superStruct, String name, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
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
    addField(new TextString(buffer, offset, 32, VAR_NAME));
    addField(new Bitmap(buffer, offset + 32, 2, VAR_TYPE, s_type));
    addField(new DecNumber(buffer, offset + 34, 2, VAR_REFERENCE));
    addField(new DecNumber(buffer, offset + 36, 4, VAR_DWORD));
    addField(new DecNumber(buffer, offset + 40, 4, VAR_INT));
    addField(new FloatNumber(buffer, offset + 44, 8, VAR_DOUBLE));
    addField(new TextString(buffer, offset + 52, 32, VAR_SCRIPT_NAME));
    return offset + 84;
  }
}
