// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;

final class Ambient extends AbstractStruct implements AddRemovable
{
  private static final String[] s_flag = {"Disabled", "Enabled", "Looping",
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

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 32, "Name"));
    list.add(new DecNumber(buffer, offset + 32, 2, "Origin: X"));
    list.add(new DecNumber(buffer, offset + 34, 2, "Origin: Y"));
    list.add(new DecNumber(buffer, offset + 36, 2, "Radius"));
//    list.add(new DecNumber(buffer, offset + 38, 2, "Height (3D)?"));
    list.add(new Unknown(buffer, offset + 38, 2));
    list.add(new DecNumber(buffer, offset + 40, 4, "Pitch variation"));
    list.add(new DecNumber(buffer, offset + 44, 2, "Volume variation"));
    list.add(new DecNumber(buffer, offset + 46, 2, "Volume"));
    if (getSuperStruct() != null)
      for (int i = 0; i < 10; i++)
        list.add(
                new AreResourceRef(buffer, offset + 48 + (i << 3), "Sound " + (i + 1),
                                   (AreResource)getSuperStruct()));
    else
      for (int i = 0; i < 10; i++)
        list.add(new ResourceRef(buffer, offset + 48 + (i << 3), "Sound " + (i + 1), "WAV"));
    list.add(new DecNumber(buffer, offset + 128, 2, "# sounds"));
    list.add(new Unknown(buffer, offset + 130, 2));
    list.add(new DecNumber(buffer, offset + 132, 4, "Base interval"));
    list.add(new DecNumber(buffer, offset + 136, 4, "Interval variation"));
    list.add(new Flag(buffer, offset + 140, 4, "Active at", Actor.s_schedule));
//    list.add(new HexNumber(buffer, offset + 140, 4, "Day/night presence?"));
    list.add(new Flag(buffer, offset + 144, 4, "Flags", s_flag));
    list.add(new Unknown(buffer, offset + 148, 64));
    return offset + 212;
  }
}

