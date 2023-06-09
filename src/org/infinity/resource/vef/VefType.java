// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vef;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

public final class VefType extends Bitmap {
  // VEF/VefType-specific field labels
  public static final String VEF_TYPE           = "Resource type";
  public static final String VEF_TYPE_RESOURCE  = "Resource";

  public static final String[] RES_TYPE_ARRAY = { "WAV", "VVC/BAM", "VEF/VVC/BAM" };

  private static final int BUF_SIZE = 8;

  public VefType(ByteBuffer buffer, int offset, int length) {
    super(buffer, offset, length, VEF_TYPE, RES_TYPE_ARRAY);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public boolean updateValue(AbstractStruct struct) {
    super.updateValue(struct);
    try {
      final List<StructEntry> list = new ArrayList<>();
      readAttributes(struct.removeFromList(this, BUF_SIZE), 0, list);
      for (StructEntry entry : list) {
        entry.setOffset(entry.getOffset() + getOffset() + getSize());
      }
      struct.addFields(this, list);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  // --------------------- End Interface Editable ---------------------

  public int readAttributes(ByteBuffer buffer, int off, List<StructEntry> list) {
    switch (getValue()) {
      case 0:
        list.add(new ResourceRef(buffer, off, VEF_TYPE_RESOURCE, "WAV"));
        break;
      case 1:
        list.add(new ResourceRef(buffer, off, VEF_TYPE_RESOURCE, "VVC", "BAM"));
        break;
      case 2:
        list.add(new ResourceRef(buffer, off, VEF_TYPE_RESOURCE, "VEF", "VVC", "BAM"));
        break;
      default:
        list.add(new TextString(buffer, off, 8, VEF_TYPE_RESOURCE));
        break;
    }
    return off + BUF_SIZE;
  }
}
