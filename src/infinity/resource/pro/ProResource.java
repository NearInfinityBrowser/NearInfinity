// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class ProResource extends AbstractStruct implements Resource, HasAddRemovable
{
  private static final String[] s_color = {"", "Black", "Blue", "Chromatic", "Gold",
                                           "Green", "Purple", "Red", "White", "Ice",
                                           "Stone", "Magenta", "Orange"};
  private static final String[] s_behave = {"No flags set", "Show sparks", "Use height",
                                            "Loop fire sound", "Loop impact sound", "Ignore center",
                                            "Draw as background"};

  public ProResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

//--------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return null;
  }

//--------------------- End Interface HasAddRemovable ---------------------

  protected int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    ProType projtype = new ProType(buffer, offset + 8);
    list.add(projtype);
    list.add(new DecNumber(buffer, offset + 10, 2, "Speed"));
    list.add(new Flag(buffer, offset + 12, 4, "Behavior", s_behave));
    list.add(new ResourceRef(buffer, offset + 16, "Fire sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 24, "Impact sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 32, "Source animation", new String[]{"VVC", "BAM"}));
    list.add(new Bitmap(buffer, offset + 40, 4, "Particle color", s_color));
    list.add(new Unknown(buffer, offset + 44, 212));
    offset += 256;

    if (projtype.getValue() > 1L) {
      ProSingleType single = new ProSingleType(this, buffer, offset);
      list.add(single);
      offset += single.getSize();
    }
    if (projtype.getValue() > 2L) {
      ProAreaType area = new ProAreaType(this, buffer, offset);
      list.add(area);
      offset += area.getSize();
    }

    return offset;
  }
}
