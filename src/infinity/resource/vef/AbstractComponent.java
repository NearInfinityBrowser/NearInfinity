// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import java.util.ArrayList;
import java.util.List;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.StructEntry;

public class AbstractComponent extends AbstractStruct implements AddRemovable
{
  // VEF/Component-specific field labels
  public static final String VEF_COMP_TICKS_START = "Ticks until start";
  public static final String VEF_COMP_TICKS_LOOP  = "Ticks until loop";
  public static final String VEF_COMP_CONTINUOUS  = "Continuous cycles?";

  public static final String[] s_noyes = {"No", "Yes"};

  protected AbstractComponent(String label) throws Exception
  {
    super(null, label, new byte[224], 0);
  }

  protected AbstractComponent(AbstractStruct superStruct, byte[] buffer, int offset, String label) throws Exception
  {
    super(superStruct, label, buffer, offset);
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
    addField(new DecNumber(buffer, offset, 4, VEF_COMP_TICKS_START));
    addField(new Unknown(buffer, offset + 4, 4));
    addField(new DecNumber(buffer, offset + 8, 4, VEF_COMP_TICKS_LOOP));
    VefType type = new VefType(buffer, offset + 12, 4);
    addField(type);

    List<StructEntry> list = new ArrayList<StructEntry>();
    offset = type.readAttributes(buffer, offset + 16, list);
    addToList(getList().size() - 1, list);

    addField(new Bitmap(buffer, offset, 4, VEF_COMP_CONTINUOUS, s_noyes));
    addField(new Unknown(buffer, offset + 4, 196));
    offset += 200;
    return offset;
  }
}
