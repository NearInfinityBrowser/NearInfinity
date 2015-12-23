// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

public final class Ambient extends AbstractStruct implements AddRemovable
{
  public static final String[] s_flag = {"Disabled", "Enabled", "Looping",
                                         "Ignore radius", "Play in random order", "High memory ambient"};

  Ambient() throws Exception
  {
    super(null, "Ambient", new byte[212], 0);
  }

  Ambient(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Ambient " + nr, buffer, offset);
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
    addField(new DecNumber(buffer, offset + 32, 2, "Origin: X"));
    addField(new DecNumber(buffer, offset + 34, 2, "Origin: Y"));
    addField(new DecNumber(buffer, offset + 36, 2, "Radius"));
//    addField(new DecNumber(buffer, offset + 38, 2, "Height (3D)?"));
    addField(new Unknown(buffer, offset + 38, 2));
    addField(new DecNumber(buffer, offset + 40, 4, "Pitch variation"));
    addField(new DecNumber(buffer, offset + 44, 2, "Volume variation"));
    addField(new DecNumber(buffer, offset + 46, 2, "Volume"));
    for (int i = 0; i < 10; i++) {
      addField(new ResourceRef(buffer, offset + 48 + (i << 3), "Sound " + (i + 1), "WAV"));
    }
    addField(new DecNumber(buffer, offset + 128, 2, "# sounds"));
    addField(new Unknown(buffer, offset + 130, 2));
    addField(new DecNumber(buffer, offset + 132, 4, "Base interval"));
    addField(new DecNumber(buffer, offset + 136, 4, "Interval variation"));
    addField(new Flag(buffer, offset + 140, 4, "Active at", Actor.s_schedule));
//    addField(new HexNumber(buffer, offset + 140, 4, "Day/night presence?"));
    addField(new Flag(buffer, offset + 144, 4, "Flags", s_flag));
    addField(new Unknown(buffer, offset + 148, 64));
    return offset + 212;
  }
}

