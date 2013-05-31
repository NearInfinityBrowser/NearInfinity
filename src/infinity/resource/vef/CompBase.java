// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

class CompBase extends AbstractStruct implements AddRemovable
{
  private static final String s_type[] = {"WAV", "VVC/BAM", "VEF/VVC/BAM"};
  private static final String s_bool[] = {"Disabled", "Enabled"};

  CompBase(String label) throws Exception
  {
    super(null, label, new byte[224], 0);
  }

  CompBase(AbstractStruct superStruct, byte[] buffer, int offset, String label) throws Exception
  {
    super(superStruct, label, buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 4, "Ticks until start"));
    list.add(new Unknown(buffer, offset + 4, 4));
    list.add(new DecNumber(buffer, offset + 8, 4, "Ticks until loop"));
    list.add(new Bitmap(buffer, offset + 12, 4, "Resource type", s_type));
    list.add(new TextString(buffer, offset + 16, 8, "Resource"));
    list.add(new Bitmap(buffer, offset + 24, 4, "Continuous cycles?", s_bool));
    list.add(new Unknown(buffer, offset + 28, 196));
    return offset + 224;
  }
}
