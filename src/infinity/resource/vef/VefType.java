// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import infinity.datatype.Bitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class VefType extends Bitmap
{
  public static final String[] s_restype = {"WAV", "VVC/BAM", "VEF/VVC/BAM"};
  private static int buf_size = 8;

  public VefType(byte buffer[], int offset, int length)
  {
    this(null, buffer, offset, length);
  }

  public VefType(StructEntry parent, byte buffer[], int offset, int length)
  {
    super(parent, buffer, offset, length, "Resource type", s_restype);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    super.updateValue(struct);
    try {
      List<StructEntry> list = new ArrayList<StructEntry>();
      readAttributes(struct.removeFromList(this, buf_size), 0, list);
      for (int i = 0; i < list.size(); i++) {
        StructEntry entry = list.get(i);
        entry.setOffset(entry.getOffset() + getOffset() + getSize());
      }
      struct.addToList(this, list);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  // --------------------- End Interface Editable ---------------------

  public int readAttributes(byte buffer[], int off, List<StructEntry> list)
  {
    switch (getValue()) {
    case 0:
      list.add(new ResourceRef(buffer, off, 8, "Resource", "WAV"));
      break;
    case 1:
      list.add(new ResourceRef(buffer, off, 8, "Resource", new String[]{"VVC", "BAM"}));
      break;
    case 2:
      list.add(new ResourceRef(buffer, off, 8, "Resource", new String[]{"VEF", "VVC", "BAM"}));
      break;
    default:
      list.add(new TextString(buffer, off, 8, "Resource"));
      break;
    }
    return off + buf_size;
  }
}
